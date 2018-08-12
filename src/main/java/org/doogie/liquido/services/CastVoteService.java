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
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

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
	 * The hash value of the voterToken is the checksum. The checksum is stored in the DB.
	 * If user is a proxy, then he will receive his own token and one token for each of his delegees.
	 * @param user the currently logged in user
	 * @param area the area of the poll  (must be passed as numerical area.ID in the request. (Not an URI)
	 * @return user's voterTokens, that only the user must know, and that will hash to the stored checksum.
	 *         The first token in the list is the users own one. The list will always have at least this one token.
	 *         If the user is a proxy, then there will be additional tokens. One for each delegee of this proxy.
	 */
	public List<String> getVoterTokens(UserModel user, AreaModel area) throws LiquidoException {
		log.debug("getVoterTokens: "+user+" requests voterTokens for "+area);
		if (user == null || DoogiesUtil.isEmpty(user.getEmail())) throw new LiquidoException(LiquidoException.Errors.NO_LOGIN, "User must be authenticated to getToken");
		if (area == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_GET_TOKEN, "Need area when requesting a token.");

		// First token in list is voter's own one.
		List<String> voterTokens = new ArrayList<>();
		voterTokens.add(upsertVoterToken(user.getId(), user.getPasswordHash(), area.getId()));

		// If this user is a proxy, than add a voterToken for each of his delegees.

		/*
		List<DelegationModel> delegations = delegationRepo.findByAreaAndToProxy(area, user);
		for (DelegationModel delegation: delegations) {
			UserModel delegee = delegation.getFromUser();
			voterTokens.add(upsertVoterToken(delegee.getId(), delegee.getPasswordHash(), area.getId()));
		}
		*/
		// cool java 8 way with streams :-)
		voterTokens.addAll(
			delegationRepo.findByAreaAndToProxy(area, user).stream()
					.map(DelegationModel::getFromUser)
					.map(fromUser -> upsertVoterToken(fromUser.getId(), fromUser.getPasswordHash(), area.getId()))
					.collect(toList())
		);

		return voterTokens;  // Important: secret voterToken is returned to user.  tokenChecksum was stored in the local DB.
	}

	/**
	 * Create a voter token for the passed user.
	 * Create a token checksum from that voterToken.
	 * Store the checksum in the DB, if it doesn't exist yet. (And only store the checksum. Not the voterToken.)
	 * Return the voterToken to the user. (And only return the voterToken, not the checksum.)
	 *
	 * @param userId a voter
	 * @param passwordHash voters password hash
	 * @param areaId voter can get one voter token per area
	 * @return the voter token of user for this area. the checksum to validate this voter token was stored.
	 */
	public String upsertVoterToken(Long userId, String passwordHash, Long areaId) {
		String voterToken = anonymizer.getBCrypetHash(userId+"", passwordHash, areaId+"");   // token that only this user must know
		String tokenChecksum  = anonymizer.getBCrypetHash(voterToken);                             // token that can only be generated from the users voterToken and only by the server.
		TokenChecksumModel existingTokenModel = checksumRepo.findByChecksum(tokenChecksum);
		if (existingTokenModel == null) {
			TokenChecksumModel newToken = new TokenChecksumModel(tokenChecksum);
			checksumRepo.save(newToken);
		}
		return voterToken;
	}

	//TODO: When user changes his passwords, then invalidate all his tokens!!!


	/**
	 * User casts a vote.
	 * @param castVoteRequest which contains the poll that we want to vote for and
	 *                        a list of voterTokens. The first token is the voter's own one.
	 *                        Then there might be more tokens from delegees if that user is a proxy.
	 * @return the list of checksums
	 * @throws LiquidoException
	 */
	public List<String> castVote(CastVoteRequest castVoteRequest) throws LiquidoException {
		log.trace("castVote: "+ castVoteRequest);

		// load models for URIs in castVoteRequst
		//TODO: Should I move loading of models into the REST controller? As I did it for PollRestController? Should a REST controller directly handle repos? Or should only the service handle repos?
		Long pollId = restUtils.getIdFromURI("polls", castVoteRequest.getPoll());
		PollModel poll = pollRepo.findOne(pollId);

		List<LawModel> voteOrder = new ArrayList<>();
		for (String proposalId : castVoteRequest.getVoteOrder()) {
			Long lawId = restUtils.getIdFromURI("laws", proposalId);
			LawModel law = lawRepo.findOne(lawId);
			voteOrder.add(law);
		}


		/*
		// Create new Ballot and check if its valid (including the checksum and many other checks)
		String unvalidatedChecksum = anonymizer.getBCrypetHash(castVoteRequest.getVoterToken());
		BallotModel ballot = new BallotModel(poll, true, voteOrder, unvalidatedChecksum);  //MAYBE: BallotModelBuilder.createFromVoterToken(...)   but would be a bit of overengeneering here
		checkBallot(ballot);
		*/

		// Cast a vote for each voterToken that was passed.
		List<String> checksums = new ArrayList<>();
		for (int i = 0; i < castVoteRequest.getVoterTokens().size(); i++) {
			String voterToken = castVoteRequest.getVoterTokens().get(i);
			boolean ownVote = (i == 0);
			BallotModel savedBallot = upsertBallot(poll, voteOrder, ownVote, voterToken);
			checksums.add(savedBallot.getChecksum());
		}

		/*
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
		*/

		/*
		//=========== If this user is a proxy, then also post a ballot for each of his delegations, except for ownVotes
		for (String delegatedVoterToken : castVoteRequest.getVoterTokens()) {
			String checksumOfDelegee = anonymizer.getBCrypetHash(delegatedVoterToken);
			BallotModel delegeeExistingBallot = ballotRepo.findByPollAndChecksum(ballot.getPoll(), checksumOfDelegee);
			if (delegeeExistingBallot != null && delegeeExistingBallot.isOwnVote()) { continue; }  // Check if delegee already voted for himself.  Never overwrite ballots with ownVote == true
			BallotModel ballotForDelegee = new BallotModel(ballot.getPoll(), false, ballot.getVoteOrder(), checksumOfDelegee);
			checkBallot(ballotForDelegee);
			log.trace("Saving ballot for delegee: "+ballotForDelegee);
			ballotRepo.save(ballotForDelegee);
		}

		*/
		return checksums;
	}

	/**
	 * The upsert algorithm for ballots:
	 *
	 * 1) Create a new (unvalidated) checksum from the passed voterToken
	 * 2) Create a new BallotModel from the passed parameters with that checksum
	 * 3) Check integrity of the new BallotModel
	 * 4) Then:
	 *
	 * passed   | existingBallot | update voteOrder of |
	 * .ownVote | .ownVote       | existing ballot?    |
	 * ---------+----------------+------------------+----------------
	 * any      | no existing    | /                | Insert a new ballot with passed ownVote value
	 * true     | true           | yes              | Voter updates his own vote order.
	 * true     | false          | yes              | Voter overwrites previous vote of a proxy.
	 * false    | true           | no               | Proxy must not overwrite voters own vote.
	 * false    | false          | yes              | Proxy updated his vote order which is distributed to delegee.
	 *
	 * We MUST first check if that ballot already exists. And than proceed accordingly.
	 * If we'd just do   ballotRepo.save(ballot)   then we'd get a JdbcSQLException:
	 *   Unique index or primary key violation: "UKJ4ELP2BMUSJ2YX6FYDKUTEDS4_INDEX_1 ON PUBLIC.BALLOTS(POLL_ID, CHECKSUM) ...
	 *
	 * @param poll The poll that the user want's to vote for
	 * @param voteOrder Voters preferred order of proposals
	 * @param ownVote TRUE means a user casts a vote for himself
	 *                FALSE means a proxy casts a vote for one of his delegees.
	 * @param voterToken The secret token of the voter from which the ballot's checksum will be calculated.
	 *                   May be the voters own token, or a token that a proxy has received from one of his delgees.
	 */
	public BallotModel upsertBallot(PollModel poll, List<LawModel> voteOrder, boolean ownVote, String voterToken) throws LiquidoException {
		String unvalidatedChecksum = anonymizer.getBCrypetHash(voterToken);
		BallotModel newBallot = new BallotModel(poll, ownVote, voteOrder, unvalidatedChecksum);  //MAYBE: BallotModelBuilder.createFromVoterToken(...)   but would be a bit of overengeneering here

		checkBallot(newBallot);

		BallotModel savedBallot = null;
		BallotModel existingBallot = ballotRepo.findByPollAndChecksum(newBallot.getPoll(), newBallot.getChecksum());
		if (existingBallot == null) {
			log.trace("Inserting new ballot");
			savedBallot = ballotRepo.save(newBallot);
		} else {
			if (!ownVote && existingBallot.isOwnVote()) return existingBallot;   // Proxy must not overwrite ownVote
			log.trace("Updating existing ballot");
			existingBallot.setVoteOrder(voteOrder);
			existingBallot.setOwnVote(ownVote);
			if (!existingBallot.getChecksum().equals(unvalidatedChecksum)) throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Something is really wrong here!!! Checksums do not match");  // This should never happen
			savedBallot = ballotRepo.save(existingBallot);
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
