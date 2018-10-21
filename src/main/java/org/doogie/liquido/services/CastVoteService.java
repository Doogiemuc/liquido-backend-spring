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
	TokenChecksumRepo checksumRepo;

	@Autowired
	DelegationRepo delegationRepo;

	@Autowired
	LiquidoRestUtils restUtils;

	@Autowired
	LiquidoAnonymizer anonymizer;

	//TOOD: we need more fine grained access
	// 1. calcVoterToken
	// 2. calcChecksum
	// 3. upsertVoterToken (and also checksum)
	// 4. validate voterToken against stored checksum
	// ProxyService only calls 1. and 2.

	/**
	 * A user wants to vote and therefore requests a voter token for this area. Each user has one token per area.
	 * The hash value of the voterToken is its checksum. The checksumModel is stored in the DB.
	 *
	 * @param user the currently logged in and correctly authenticated user
	 * @param area the area of the poll  (must be passed as numerical area.ID in the request. (Not an URI)
	 * @return user's voterTokens, that only the user must know, and that will hash to the stored checksumModel.
	 */
	public String createVoterToken(UserModel user, AreaModel area, String passwordHash) throws LiquidoException {
		log.debug("createVoterToken: for "+user+" in "+area);
		if (user == null || DoogiesUtil.isEmpty(user.getEmail()) || area == null ||passwordHash == null)
			throw new LiquidoException(LiquidoException.Errors.NO_LOGIN, "Need user, area and passwordHash to create a voterToken!");
		String voterToken = anonymizer.getBCryptHash(user.getId()+"", passwordHash, area.getId()+"");   // token that only this user must know
		String tokenChecksum = calcChecksumFromVoterToken(voterToken);                            						// token that can only be generated from the users voterToken and only by the server.
		TokenChecksumModel existingChecksumModel = checksumRepo.findByChecksum(tokenChecksum);
		if (existingChecksumModel == null) {
			TokenChecksumModel newChecksumModel = new TokenChecksumModel(tokenChecksum, area);
			checksumRepo.save(newChecksumModel);
		}
		return voterToken;
	}

	/**
	 * Very thoroughly check if the passed voterToken is valid, ie. its checksum=hash(voterToken) is already known
	 * @param voterToken the token to check
	 * @return the existing TokenChecksumModel  OR <b><NULL</b> if voterToken is invalid
	 */
	public TokenChecksumModel isVoterTokenValid(String voterToken) {
		if (voterToken == null || !voterToken.startsWith("$2") || voterToken.length() < 10) return null;  // BCRYPT hashes start with $2$ or $2
		String tokenChecksum = calcChecksumFromVoterToken(voterToken);
		TokenChecksumModel existingTokenModel = checksumRepo.findByChecksum(tokenChecksum);
		if (existingTokenModel == null) log.warn("VoterToken '"+voterToken+"' is INVALID!");
		return existingTokenModel;
	}

	//TODO: When user changes his passwords, then invalidate all his tokens!!!


	/**
	 * User casts own vote.
	 * If that user is a proxy for other voters, then their ballots will also be added automatically.
	 * @param castVoteRequest which contains the poll that we want to vote for and
	 *                        a list of voterTokens. The first token is the voter's own one.
	 *                        Then there might be more tokens from delegees if that user is a proxy.
	 * @return BallotModel the casted ballot
	 * @throws LiquidoException when something is wrong with the ballot
	 */
	public BallotModel castVote(CastVoteRequest castVoteRequest) throws LiquidoException {
		log.debug("castVote: "+ castVoteRequest);

		TokenChecksumModel checksumModel = isVoterTokenValid(castVoteRequest.getVoterToken());
		if (checksumModel == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Your voter token seems to be invalid!");

		// load models for URIs in castVoteRequst
		//TODO: !!! Should I move loading of models up into the REST controller? As I did it for PollRestController? Should a REST controller directly handle repos? Or should only the service handle repos?  See: RestUtils.class
		Long pollId = restUtils.getIdFromURI("polls", castVoteRequest.getPoll());
		PollModel poll = pollRepo.findById(pollId)
				.orElseThrow(()->new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot find poll with poll.id="+pollId));

		List<LawModel> voteOrder = new ArrayList<>();
		for (String proposalId : castVoteRequest.getVoteOrder()) {
			Long lawId = restUtils.getIdFromURI("laws", proposalId);
			LawModel law = lawRepo.findById(lawId)
					.orElseThrow(()-> new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot find proposal with proposal.id="+lawId));
			voteOrder.add(law);
		}

		BallotModel newBallot = new BallotModel(poll, 0, voteOrder, checksumModel);  //MAYBE: BallotModelBuilder.createFromVoteRequest(...)   or would that be a bit of overengeneering
		BallotModel savedBallot = storeBallot(newBallot);		// checksum will be validated inside storeBallot() -> checkBallot()

		return savedBallot;
	}

	/**
	 * This method calls itself recursivly. The <b>upsert</b> algorithm for storing a ballot:
	 *
	 * 1) Check the integrity of the passed newBallot. Especially check the validity of its TokenChecksumModel.
	 *    The checksum must be known.
	 *
	 * 2) IF there is NO existing ballot for this poll with that checksum yet,
	 *    THEN save a new ballot
	 *    ELSE // ballot with that checksum already exists
	 *      IF the level of the existing ballot is SMALLER then the passed newBallot.level
	 *      THEN do NOT update the existing ballot, because it was casted by a lower proxy or the user himself
	 *      ELSE update the existing ballot's level and vote order
	 *
	 *  3) FOR EACH directly delegated TokenChecksumModel
	 *              create a childBallot and recursively try to store this childBallot.
	 *
	 *  Remark: The child ballot might not be stored when there alrady is one with a smaller level. This is
	 *          our recursion limit.
	 *
	 * @param newBallot the ballot that shall be stored. The ballot will be checked very thoroughly. Especially if the ballot's checksum is valid.
	 * @return the newly created or updated existing ballot  OR
	 *         null if the ballot wasn't stored due to an already existing ballot with a smaller level.
	 */
	public BallotModel storeBallot(BallotModel newBallot) throws LiquidoException {
		log.trace("storeBallot("+newBallot+") checksum="+newBallot.getChecksum().getChecksum());

		//----- check validity of the ballot
		checkBallot(newBallot);

		//----- If there is no existing ballot yet with that checksum, then create a completely new one.
		BallotModel existingBallot = ballotRepo.findByPollAndChecksum(newBallot.getPoll(), newBallot.getChecksum());
		if (existingBallot == null) {
			log.trace("  Inserting new ballot");
			existingBallot = ballotRepo.save(newBallot);
		} else {
			//----- Proxy must not overwrite a voter's own vote  OR  a vote from a proxy below
			if (existingBallot.getLevel() < newBallot.getLevel()) {
				log.trace("  Will not overwrite existing ballot with smaller level " + existingBallot);
				return null;
			}

			log.trace("  Updating existing ballot");
			existingBallot.setVoteOrder(newBallot.getVoteOrder());
			existingBallot.setLevel(newBallot.getLevel());
			if (!existingBallot.getChecksum().equals(newBallot.getChecksum()))
				throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Something is really wrong here!!! Checksums do not match");  // This should never happen
		}

		//----- recursively cast a vote for each delegated checksumModel
		int voteCount = 1;   // first vote is for the ballot itself.
		List<TokenChecksumModel> delegatedChecksums = checksumRepo.findByDelegatedTo(existingBallot.getChecksum());
		for (TokenChecksumModel delegatedChecksum : delegatedChecksums) {
			BallotModel childBallot = new BallotModel(newBallot.getPoll(), newBallot.getLevel() + 1, newBallot.getVoteOrder(), delegatedChecksum);
			//log.trace("checking delegated childBallot "+childBallot);
			BallotModel savedChildBallot = storeBallot(childBallot);  // will return null when childBallot level is to large and a smaller one was found  => then we stop this recursion tree
			if (savedChildBallot != null) voteCount = voteCount + savedChildBallot.getVoteCount();
		}
		existingBallot.setVoteCount(voteCount);
		return ballotRepo.save(existingBallot);
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
				throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot cast vote: Proposal(id="+proposal.getId()+") is not part of poll(id="+ballot.getPoll().getId()+")!");
			if (!LawModel.LawStatus.VOTING.equals(proposal.getStatus())) {
				throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot cast vote: proposals must be in voting phase.");
			}
		}

		// check that there is a checksum
		if (ballot.getChecksum() == null || ballot.getChecksum().getChecksum().length() < 5) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot cast vote: Invalid voterToken. It's checksum must be at least 5 chars!");
		}

		// Passed checksumModel in ballot MUST already exists in TokenChecksumRepo to be valid
		TokenChecksumModel existingChecksum = checksumRepo.findByChecksum(ballot.getChecksum().getChecksum());
		if (existingChecksum == null)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot cast vote: Ballot's checksum is invalid. (It cannot be found in TokenChecksumModel.)");

	}

	/**
	 * Calculate the checksum value for the given voterToken.
	 * The checksum is not stored or validated! This is just the mathematical calculation
	 * Checksum must only be handled on the server. Do not return it to the voter!
	 *
	 * @param voterToken token passed from user
	 * @return checksum = hash(voterToken, seed)
	 */
	public String calcChecksumFromVoterToken(String voterToken) {
		return anonymizer.getBCryptHash(voterToken);
	}

}
