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
	LiquidoRestUtils restUtils;

	@Autowired
	LiquidoAnonymizer anonymizer;

	/**
	 * A user wants to vote and therefore requests a voter token for this area. Each user has one token per area.
	 * The hash value of the voterToken is the checksumModel. The checksumModel is stored in the DB.
	 * If user is a proxy, then he will receive his own token and one token for each of his delegees.
	 * @param user the currently logged in user
	 * @param area the area of the poll  (must be passed as numerical area.ID in the request. (Not an URI)
	 * @return user's voterTokens, that only the user must know, and that will hash to the stored checksumModel.
	 *         The first token in the list is the users own one. The list will always have at least this one token.
	 *         If the user is a proxy, then there will be additional tokens. One for each delegee of this proxy.
	 */
	public String getVoterToken(UserModel user, AreaModel area) throws LiquidoException {
		log.trace("getVoterToken: "+user+" requests voterToken for "+area);
		if (user == null || DoogiesUtil.isEmpty(user.getEmail())) throw new LiquidoException(LiquidoException.Errors.NO_LOGIN, "User must be authenticated to getToken");
		if (area == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_GET_TOKEN, "Need area when requesting a token.");

		String voterToken = upsertVoterToken(user.getId(), user.getPasswordHash(), area.getId());

		return voterToken;  // Important: secret voterToken is returned to user.  tokenChecksum was stored in the local DB.
	}

	/**
	 * Create a voter token for the passed user.
	 * Create a token checksumModel from that voterToken.
	 * Store the checksumModel in the DB, if it doesn't exist yet. (And only store the checksumModel. Not the voterToken.)
	 * Return the voterToken to the user. (And only return the voterToken, not the checksumModel.)
	 *
	 * @param userId a voter
	 * @param passwordHash voters password hash
	 * @param areaId voter can get one voter token per area
	 * @return the voter token of user for this area. the checksumModel to validate this voter token was stored.
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
	 * User casts own vote.
	 * If that user is a proxy for other voters, then their ballots will also be added automatically.
	 * @param castVoteRequest which contains the poll that we want to vote for and
	 *                        a list of voterTokens. The first token is the voter's own one.
	 *                        Then there might be more tokens from delegees if that user is a proxy.
	 * @return checksums of the user's own casted ballot.
	 * @throws LiquidoException
	 */
	public String castVote(CastVoteRequest castVoteRequest) throws LiquidoException {
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

		//----- cast users own vote
		String unvalidatedChecksum = anonymizer.getBCrypetHash(castVoteRequest.getVoterToken());
		TokenChecksumModel existingChecksum = checksumRepo.findByChecksum(unvalidatedChecksum);
		if (existingChecksum == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "voterToken '"+castVoteRequest.getVoterToken()+"' is invalid. Does not match checksumModel.");

		BallotModel ownBallot = storeBallot(poll, voteOrder, 0, existingChecksum);

		return ownBallot.getChecksumModel().getChecksum();
	}

	/**
	 * The upsert algorithm for ballots:
	 *
	 * 1) Create a new (unvalidated) checksumModel from the passed voterToken
	 * 2) Create a new BallotModel from the passed parameters with that checksumModel
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
	 * @param level how directly did the user vote
	 * @param validChecksum a {@link TokenChecksumModel} that was stored previously
	 */
	public BallotModel storeBallot(PollModel poll, List<LawModel> voteOrder, int level, TokenChecksumModel validChecksum) throws LiquidoException {
		BallotModel newBallot = new BallotModel(poll, level, voteOrder, validChecksum);  //MAYBE: BallotModelBuilder.createFromVoterToken(...)   or would that be a bit of overengeneering
		log.trace("storeBallot("+newBallot+")");
		checkBallot(newBallot);

		BallotModel savedBallot = null;
		BallotModel existingBallot = ballotRepo.findByPollAndChecksumModel(newBallot.getPoll(), newBallot.getChecksumModel());
		if (existingBallot == null) {
			log.trace("Inserting new ballot");
			savedBallot = ballotRepo.save(newBallot);
		} else {
			if (existingBallot.getLevel() < level) {
				return existingBallot;   // Proxy must not overwrite a voter's own vote  OR  a vote from a proxy below
			}
			log.trace("Updating existing ballot");
			existingBallot.setVoteOrder(voteOrder);
			existingBallot.setLevel(level);
			if (!existingBallot.getChecksumModel().equals(validChecksum)) throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Something is really wrong here!!! Checksums do not match");  // This should never happen

			//----- recursively cast a vote for each delegated checksumModel
			for(TokenChecksumModel delegatedChecksum: existingBallot.getChecksumModel().getProxyFor()) {
				storeBallot(poll, voteOrder, level+1, delegatedChecksum);
			}

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

		// check that there is an checksumModel
		if (ballot.getChecksumModel() == null || ballot.getChecksumModel().getChecksum().length() < 5) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot cast vote: Invalid voterToken. It's checksumModel must be at least 5 chars!");
		}

		// Passed checksumModel in ballot must already exists in TokenChecksumRepo to be valid
		TokenChecksumModel existingChecksum = checksumRepo.findByChecksum(ballot.getChecksumModel().getChecksum());
		if (existingChecksum == null)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "User's voterToken is invalid! Cannot find checksumModel to validate.");

	}

}
