package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.model.*;
import org.doogie.liquido.rest.LiquidoRestUtils;
import org.doogie.liquido.rest.dto.CastVoteRequest;
import org.doogie.liquido.security.LiquidoAnonymizer;
import org.doogie.liquido.util.DoogiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This spring component implements the business logic for {@link org.doogie.liquido.model.BallotModel}
 * I am trying to keep the Models as dumb as possible.
 * All business rules and security checks are implemented here.
 */
@Service
@Slf4j
public class CastVoteService {

	@Autowired
	PollRepo pollRepo;

	@Autowired
	LawRepo lawRepo;

	@Autowired
	BallotRepo ballotRepo;

	@Autowired
	DelegationRepo delegationRepo;

	@Autowired
	TokenChecksumRepo checksumRepo;

	@Autowired
	LiquidoRestUtils restUtils;

	@Autowired
	LiquidoAnonymizer anonymizer;

	/**
	 * A user wants to vote and therefore requests a voter token for this area. Each user has one token per area.
	 * The hash value of the voterToken (and a secret salt) is the checksum. The checksum is stored in the DB.
	 * These tokens can be passed to proxies. A proxy can then cast votes with all his tokens.
	 * @param user the currently logged in user
	 * @param area the area of the poll
	 * @return user's voterToken, that only the user must know, and that will hash to the stored checksum.
	 */
	public String getVoterToken(UserModel user, AreaModel area) throws LiquidoException {
		log.debug("getVoterToken: "+user+" requested voterToken for "+area);
		if (user == null || DoogiesUtil.isEmpty(user.getEmail())) throw new LiquidoException(LiquidoException.Errors.NO_LOGIN, "User must be authenticated to getToken");
		if (area == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_GET_TOKEN, "Need area when requesting a token.");
		String voterToken = anonymizer.getBCrypetHash(user.getEmail(), user.getPasswordHash(), String.valueOf(area.getId()));   // token that only this user must know
		String tokenChecksum  = anonymizer.getBCrypetHash(voterToken);                                           								// token that can only be generated from the users voterToken and only by the server.
 		TokenChecksumModel existingTokenModel = checksumRepo.findByChecksum(tokenChecksum);
		if (existingTokenModel == null) {   // only save new token if it does not exist yet.
			TokenChecksumModel newToken = new TokenChecksumModel(tokenChecksum);
			checksumRepo.save(newToken);
		}
		return voterToken;  // important do not return the tokenChecksum!
	}

	//TODO: When user changes his passwords, then invalidate all his tokens!!!

	public BallotModel castVote(UserModel user, CastVoteRequest castVoteRequest) throws LiquidoException {
		log.trace("User "+user.getEmail()+"(id="+user.getId()+") casts Vote "+ castVoteRequest);

		// load models for URIs in castVoteRequst
		Long pollId = restUtils.getIdFromURI("polls", castVoteRequest.getPoll());
		PollModel poll = pollRepo.findOne(pollId);

		List<LawModel> voteOrder = new ArrayList<>();
		for (String proposalId : castVoteRequest.getVoteOrder()) {
			Long lawId = restUtils.getIdFromURI("laws", proposalId);
			LawModel law = lawRepo.findOne(lawId);
			voteOrder.add(law);
		}

		// Create yet unvalidatedChecksum.
		String unvalidatedChecksum = anonymizer.getBCrypetHash(castVoteRequest.getVoterToken());

		// Create new Ballot and check if its valid (including the checksum and many other checks)
		BallotModel ballot = new BallotModel(poll, true, voteOrder, unvalidatedChecksum);  //MAYBE: BallotModelBuilder.createFromVoterToken(...)   but would be a bit of overengeneering here
		checkBallot(ballot);

		// Check that  voter (if he is a proxy) has as many voter tokens as he has delegations
		AreaModel area = ballot.getPoll().getProposals().iterator().next().getArea();
		long numVotes = delegationRepo.getNumVotes(area, user);
		if (numVotes != user.getVoterTokens().size()+1)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Inconsistency detected: number of voter tokens does not match number of delegations. User '"+user.getEmail()+"' (id="+user.getId()+")");

		// Check if user has already voted. If so, then update the voteOrder in his existing ballot
		BallotModel savedBallot = null;
		BallotModel existingBallot = ballotRepo.findByPollAndChecksum(ballot.getPoll(), ballot.getChecksum());
		if (existingBallot != null) {
			existingBallot.setVoteOrder(ballot.getVoteOrder());
			log.trace("Updating existing "+ballot);
			savedBallot = ballotRepo.save(existingBallot);  // update the existing ballot
		} else {
			log.trace("Inserting new "+ballot);
			savedBallot = ballotRepo.save(ballot);          // insert a new BallotModel
		}

		//=========== If this user is a proxy, then also post a ballot for each of his delegations, except for ownVotes
		for (String delegatedVoterToken : user.getVoterTokens()) {
			String checksumOfDelegee = anonymizer.getBCrypetHash(delegatedVoterToken);
			BallotModel delegeeExistingBallot = ballotRepo.findByPollAndChecksum(ballot.getPoll(), checksumOfDelegee);
			if (delegeeExistingBallot != null && delegeeExistingBallot.isOwnVote()) { continue; }  // Check if delegee already voted for himself.  Never overwrite ballots with ownVote == true
			BallotModel ballotForDelegee = new BallotModel(ballot.getPoll(), false, ballot.getVoteOrder(), checksumOfDelegee);
			checkBallot(ballotForDelegee);
			log.trace("Saving ballot for delegee: "+ballotForDelegee);
			ballotRepo.save(ballotForDelegee);
		}
		return savedBallot;
	}

	/**
	 * Check if a ballot is valid for casting a vote.
	 * @param ballot a casted vote
	 * @throws LiquidoException when something inside ballot is invalid
	 */
	public void checkBallot(BallotModel ballot) throws LiquidoException {
		// check that poll is actually in voting phase and has at least two alternative proposals
		PollModel poll = ballot.getPoll();
		if (poll == null || !PollModel.PollStatus.VOTING.equals(poll.getStatus())) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot cast vote: Poll must be in voting phase.");
		}
		if (poll.getProposals().size() < 2)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot cast vote: Poll must have at least two alternative proposals.");

		// check that voter Order is not empty
		if (ballot.getVoteOrder() == null || ballot.getVoteOrder().size() == 0) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE,"Cannot cast vote: VoteOrder is empty!");
		}

		// check that there is no duplicate vote for any one proposal
		HashSet<Long> proposalIds = new HashSet<>();
		for(LawModel proposal : ballot.getVoteOrder()) {
			if (proposalIds.contains(proposal.getId())) {
				throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot cast vote: Duplicate vote for proposal_id="+proposal.getId());
			} else {
				proposalIds.add(proposal.getId());
			}
		}

		// check that all proposals you wanna vote for are in this poll and that they are also in voting phase
		for(LawModel proposal : ballot.getVoteOrder()) {
			if (!proposal.getPoll().equals(ballot.getPoll()))
				throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot cast vote: A proposal(id="+proposal.getId()+") is not part of poll(id="+ballot.getPoll().getId()+")!");
			if (!LawModel.LawStatus.VOTING.equals(proposal.getStatus())) {
				throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot cast vote: proposals must be in voting phase.");
			}
		}

		// check that there is an checksum
		if (ballot.getChecksum() == null || ballot.getChecksum().length() < 5) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot cast vote: Invalid voterToken. It's checksum must be at least 5 chars!");
		}

		// Passed checksum in ballot must already exists in TokenChecksumRepo to be valid
		TokenChecksumModel existingChecksum = checksumRepo.findByChecksum(ballot.getChecksum());
		if (existingChecksum == null)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "User's voterToken is invalid! Cannot find checksum to validate.");

	}

	// MAYBE also look at JSON-Patch. Could that be used here to post the voteOrder?
	// http://stackoverflow.com/questions/25311978/posting-a-onetomany-sub-resource-association-in-spring-data-rest
	// https://github.com/spring-projects/spring-data-rest/commit/ef3720be11f117bb691edbbf63e38ff72e0eb3dd
	// http://stackoverflow.com/questions/34843297/modify-onetomany-entity-in-spring-data-rest-without-its-repository/34864254#34864254


}
