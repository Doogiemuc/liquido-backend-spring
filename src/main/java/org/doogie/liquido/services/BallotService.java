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
public class BallotService {

	@Autowired
	PollRepo pollRepo;

	@Autowired
	LawRepo lawRepo;

	@Autowired
	BallotRepo ballotRepo;

	@Autowired
	DelegationRepo delegationRepo;

	@Autowired
	TokenRepo tokenRepo;

	@Autowired
	LiquidoRestUtils restUtils;

	@Autowired
	LiquidoAnonymizer anonymizer;


	/**
	 * This salt is only known to the server
	 * It prevents attackers from creating a valid areaToken.
	 */
	String serversSecretSalt = "noOneKnow";

	/**
	 * A user wants to vote and therefore requests a voter token for this area. Each user has one token per area.
	 * The hash value of the voterToken (and a secret salt) is the areaToken. The areaToken is stored in the DB.
	 * These tokens can be passed to proxies. A proxy can then cast votes with all his tokens.
	 * @param user the currently logged in user
	 * @param area the area of the poll
	 * @return user's voterToken, that only the user must know, and that will hash to the stored areaToken.
	 */
	public String getVoterToken(UserModel user, AreaModel area) throws LiquidoException {
		if (user == null || DoogiesUtil.isEmpty(user.getEmail())) throw new LiquidoException(LiquidoException.Errors.NO_LOGIN, "User must be authenticated to getToken");
		if (area == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_GET_TOKEN, "Need area when requesting a token.");
		log.debug("getBallotToken: user="+user.getEmail()+"(id="+user.getId()+") requested areaToken for area="+area.getTitle()+"(id="+area.getId()+")");
		String voterToken = anonymizer.getBCrypetHash(user.getEmail(), user.getPasswordHash(), String.valueOf(area.getId()));   // token that only this user must know
		String areaToken  = anonymizer.getBCrypetHash(voterToken, serversSecretSalt);                                           // token that can only be generated from the users voterToken and only by the server.
 		TokenModel existingTokenModel = tokenRepo.findByAreaToken(areaToken);
		if (existingTokenModel == null) {   // only save new token if it does not exist yet.
			TokenModel newToken = new TokenModel(areaToken);
			tokenRepo.save(newToken);
		}
		return voterToken;  // important do not return the areaToken!            //MAYBE: actually the user wouldn't even need the voterToken. He could later create it on his own. Using BCRYPT on the client!
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

		// Create yet unvalidatedAreaToken.
		String unvalidatedAreaToken = anonymizer.getBCrypetHash(castVoteRequest.getVoterToken(), serversSecretSalt);

		// Create new Ballot and check if its valid (including the areaToken and many other checks)
		BallotModel ballot = new BallotModel(poll, true, voteOrder, unvalidatedAreaToken);  //MAYBE: BallotModelBuilder.createFromVoterToken(...)   but would be a bit of overengeneering here
		checkBallot(ballot);

		// Check that  voter (if he is a proxy) has as many voter tokens as he has delegations
		AreaModel area = ballot.getPoll().getProposals().iterator().next().getArea();
		long numVotes = delegationRepo.getNumVotes(area, user);
		if (numVotes != user.getVoterTokens().size()+1)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Inconsistency detected: number of voter tokens does not match number of delegations. User '"+user.getEmail()+"' (id="+user.getId()+")");

		// Check if user has already voted. If so, then update the voteOrder in his existing ballot
		BallotModel savedBallot = null;
		BallotModel existingBallot = ballotRepo.findByPollAndAreaToken(ballot.getPoll(), ballot.getAreaToken());
		if (existingBallot != null) {
			existingBallot.setVoteOrder(ballot.getVoteOrder());
			log.trace("Updating existing "+ballot);
			savedBallot = ballotRepo.save(existingBallot);  // update the existing ballot
		} else {
			log.trace("Inserting new "+ballot);
			savedBallot = ballotRepo.save(ballot);          // insert a new BallotModel
		}

		//If this user is a proxy, then also post a ballot for each of his delegations
		for (String delegatedVoterToken : user.getVoterTokens()) {
			String delegatedAreaToken = anonymizer.getBCrypetHash(delegatedVoterToken, serversSecretSalt);
			BallotModel delegeeExistingBallot = ballotRepo.findByPollAndAreaToken(ballot.getPoll(), delegatedAreaToken);
			if (delegeeExistingBallot != null && delegeeExistingBallot.isOwnVote()) { continue; }  // Check if delegee already voted for himself.  Never overwrite ballots with ownVote == true
			BallotModel ballotForDelegee = new BallotModel(ballot.getPoll(), false, ballot.getVoteOrder(), delegatedAreaToken);
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
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot post ballot: Poll must be in voting phase.");
		}
		if (poll.getProposals().size() < 2)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot post ballot: Poll must have at least two alternative proposals.");

		// check that voter Order is not empty
		if (ballot.getVoteOrder() == null || ballot.getVoteOrder().size() == 0) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE,"Cannot post ballot: VoteOrder is empty!");
		}

		// check that there is no duplicate vote for any one proposal
		HashSet<Long> proposalIds = new HashSet<>();
		for(LawModel proposal : ballot.getVoteOrder()) {
			if (proposalIds.contains(proposal.getId())) {
				throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot post ballot: Duplicate vote for proposal_id="+proposal.getId());
			} else {
				proposalIds.add(proposal.getId());
			}
		}

		// check that all proposals you wanna vote for are also in voting phase
		for(LawModel proposal : ballot.getVoteOrder()) {
			if (!LawModel.LawStatus.VOTING.equals(proposal.getStatus())) {
				throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot post ballot: proposals must be in voting phase.");
			}
		}

		// check that there is an areaToken
		if (ballot.getAreaToken() == null || ballot.getAreaToken().length() < 5) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot post ballot: Invalid areaToken. Must be at least 5 chars!");
		}

		// check that areaToken is valid, ie. that it already exists in TokenRepo
		TokenModel existingToken = tokenRepo.findByAreaToken(ballot.getAreaToken());
		if (existingToken == null)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "User's voterToken is invalid!");

	}

	// MAYBE also look at JSON-Patch. Could that be used here to post the voteOrder?
	// http://stackoverflow.com/questions/25311978/posting-a-onetomany-sub-resource-association-in-spring-data-rest
	// https://github.com/spring-projects/spring-data-rest/commit/ef3720be11f117bb691edbbf63e38ff72e0eb3dd
	// http://stackoverflow.com/questions/34843297/modify-onetomany-entity-in-spring-data-rest-without-its-repository/34864254#34864254


  /* This is how to manually create a HASH with Java's built-in SHA-256

  try {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(DoogiesUtil.longToBytes(userId));
    md.update(DoogiesUtil.longToBytes(initialPropId));
    md.update(userPassword.getBytes());
    byte[] digest = md.digest();
    String voterTokenSHA256 = DoogiesUtil.bytesToString(digest);
  } catch (NoSuchAlgorithmException e) {
    log.error("FATAL: cannot create SHA-256 MessageDigest: "+e);
    throw new LiquidoRestException("Internal error in backend");
  }
  */
}
