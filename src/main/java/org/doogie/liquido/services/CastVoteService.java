package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.model.*;
import org.doogie.liquido.rest.dto.CastVoteRequest;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.LiquidoRestUtils;
import org.hibernate.annotations.Check;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * This service contains all the voting logic for casting a vote.
 * Here we implement all the security around voterTokens and their checksums.
 */
@Service
@Slf4j
public class CastVoteService {

	@Autowired
	PollRepo pollRepo;

	@Autowired
	DelegationRepo delegationRepo;

	@Autowired
	LawRepo lawRepo;

	@Autowired
	BallotRepo ballotRepo;

	@Autowired
	ChecksumRepo checksumRepo;

	@Autowired
	ProxyService proxyService;

	@Autowired
	LiquidoRestUtils restUtils;

	/**
	 * This salt is initialized from application.properties
	 * and then stored into the DB by {@link org.doogie.liquido.util.LiquidoProperties}.
	 * We must use the same salt for re-generating tokens and checksums
	 */
	@Value("${liquido.bcrypt.salt}")
	String bcryptSalt;

	/**
	 * This secret is only known to the liquido server. That way we ensure
	 * that only this class can create voterTokens and checksums.
	 */
	@Value("${liquido.bcrypt.secret}")
	String serverSecret;

	@Value("${liquido.checksum.expiration.hours}")
	int checksumExpirationHours;

	//TODO:  RSA Tokens  https://stackoverflow.com/questions/37722090/java-jwt-with-public-private-keys
	//TODO: create really secure voterTokens like this: U2F  https://blog.trezor.io/why-you-should-never-use-google-authenticator-again-e166d09d4324

	/**
	 * A user wants to vote and therefore requests a voter token for this area. Each user has one token per area.
	 * The hash value of the voterToken is its checksum. The checksumModel is stored in the DB.
	 *
	 * <h3>Implementation notes</h3>
	 * <pre>
	 *   voterToken = hash(user.id + voterTokenSecret + area.id + serverSecret)
	 *   checksum   = hash(voterToken + serverSecret)
	 * </pre>
	 *
	 * When creating new voterTokens, then we do not calculate a new BCrypt salt. Because a voter might request his voterToken
	 * multiple times. And we have to return the same voterToken every time.
	 *
	 * @param voter the currently logged in and correctly authenticated user
	 * @param area an area that the user's want's to vote in
	 * @param voterTokenSecret  a secret that only the user knows. So no one else can create this voterToken. May be emtpy string, but then with less security.
	 * @param becomePublicProxy true if voter wants to become (or stay) a public proxy. Voter can also decide later with {@link ProxyService#becomePublicProxy
	 * @return  users voterToken, that only the user must know, and that will hash to the stored checksumModel.
	 */
	@Transactional
	public String createVoterTokenAndStoreChecksum(UserModel voter, AreaModel area, String voterTokenSecret, boolean becomePublicProxy) throws LiquidoException {
		log.debug("createVoterTokenAndStoreChecksum: for "+voter+" in "+area + ", becomePublicProxy="+becomePublicProxy);
 		if (voter == null || DoogiesUtil.isEmpty(voter.getEmail()))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_GET_TOKEN, "Need voter to build a voterToken!");
		if (area == null)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_GET_TOKEN, "Cannot build a voterToken. Could not find area.");
		if (DoogiesUtil.isEmpty(voterTokenSecret))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_GET_TOKEN, "Need your tokenSecret to build a voterToken!");

		// Create a new voterToken for this user in that area with the BCRYPT hashing algorithm
		// Remark: Bcrypt prepends the salt into the returned token value!!!
		String voterToken =    calcVoterToken(voter.getId(), voterTokenSecret, area.getId());   // voterToken that only this user must know
		String tokenChecksum = calcChecksumFromVoterToken(voterToken);                          // checksum of voterToken that can only be generated from the users voterToken and only by the server.

		//   IF there is a an already existing checksum for that voter as public proxy
		//  AND there is NO existing checksum for that voterToken OR one that does not match the public proxies checksum,
		// THEN the user has changed his voterTokenSecret
		//  AND we must invalidate (delete) the old checksum.
		 //  AND we must delete all delegations.
		Optional<ChecksumModel> checksumOpt = checksumRepo.findByChecksum(tokenChecksum);
		Optional<ChecksumModel> existingChecksumOfPublicProxyOpt = checksumRepo.findByAreaAndPublicProxy(area, voter);
		if (existingChecksumOfPublicProxyOpt.isPresent() &&
				!existingChecksumOfPublicProxyOpt.equals(checksumOpt)) {
			log.trace("Voter changed his voterTokenSecret. Replacing old checksum of former public proxy with new one.");

			throw new RuntimeException("Change of voterTokenSecret is NOT YET IMPLEMENTED!");
			/*
			List<DelegationModel> delegations = delegationRepo.findByAreaAndToProxy(area, voter);
			List<ChecksumModel> delegees = checksumRepo.findByDelegatedTo(existingChecksumOfPublicProxy);
			if (becomePublicProxy) {
				for(ChecksumModel delegatedChecksum : delegees) {
					delegatedChecksum.setDelegatedTo(checksumModel);
					checksumRepo.save(delegatedChecksum);
				}
			} else {
				for(ChecksumModel delegatedChecksum : delegees) {
					delegatedChecksum.setDelegatedTo(null);
					checksumRepo.save(delegatedChecksum);
				}
				delegationRepo.deleteAll(delegations);
			}
			checksumRepo.delete(existingChecksumOfPublicProxy);
			*/
		}

		if (checksumOpt.isPresent()) { log.trace("  Update existing checksum");	}
		ChecksumModel checksum = checksumOpt.orElse(new ChecksumModel(tokenChecksum, area));

		//   IF wants to become a public proxy
		//  AND voter is not yet the public proxy of his checksum
		// THEN stores his username with his checksum
		//  AND automatically accept all pending delegations
		if (becomePublicProxy && !voter.equals(checksum.getPublicProxy())) {
			proxyService.becomePublicProxy(voter, area, voterToken);
		}
		refreshChecksum(checksum);
		checksumRepo.save(checksum);

		return voterToken;		// IMPORTANT! return the voterToken and store the checksum.
	}

	/**
	 * refresh the expiration time of this valid checksum. You must save the update yourself!
	 * @param checksum
	 */
	public void refreshChecksum(ChecksumModel checksum) {
		checksum.setExpiresAt(LocalDateTime.now().plusHours(checksumExpirationHours));
	}

	/**
	 * Very thoroughly check if the passed voterToken is valid, ie. its checksum=hash(voterToken) is already known.
	 * This method will not create a checksum.
	 * @param voterToken the token to check
	 * @return the existing ChecksumModel
	 * @throws LiquidoException when voterToken is invalid itself or its checksum is not known.
	 */
	public ChecksumModel isVoterTokenValidAndGetChecksum(String voterToken) throws LiquidoException {
		if (voterToken == null || !voterToken.startsWith("$2") || voterToken.length() < 10)
			throw new LiquidoException(LiquidoException.Errors.INVALID_VOTER_TOKEN, "Voter token is empty or has wrong format");  // BCRYPT hashes start with $2$ or $2
		String tokenChecksum = calcChecksumFromVoterToken(voterToken);
		ChecksumModel existingChecksum = checksumRepo.findByChecksum(tokenChecksum)
				.orElseThrow(() -> (new LiquidoException(LiquidoException.Errors.INVALID_VOTER_TOKEN, "Voter token is invalid. It's checksum is not known as valid.")));
		return existingChecksum;
	}

	/**
	 * Get the already existing checksum of this user in that area.
	 * This method will not create a new voterToken or store any checksum.
	 *
	 * This is mainly used for tests
	 *
	 * @param user a voter
	 * @param area an area
	 * @return the existing checksum of that user in that area
	 * @throws LiquidoException when user does not yet have checksum in that area
	 */
	public ChecksumModel getExistingChecksum(UserModel user, String userTokenSecret, AreaModel area) throws LiquidoException {
		String voterToken = calcVoterToken(user.getId(), userTokenSecret, area.getId());
		return isVoterTokenValidAndGetChecksum(voterToken);
	}


	/**
	 * User casts own vote. Keep in mind, that this method is called anonymously. No UserModel involved.
	 * If that user is a proxy for other voters, then their ballots will also be added automatically.
	 * @param castVoteRequest which contains the poll that we want to vote for and
	 *                        the anonymous voterToken and the preferred voteOrder
	 * @return BallotModel the casted ballot
	 * @throws LiquidoException when something is wrong with the ballot
	 */
	public BallotModel castVote(CastVoteRequest castVoteRequest) throws LiquidoException {
		log.debug("castVote: "+ castVoteRequest);
		if (castVoteRequest.getVoteOrder() == null || castVoteRequest.getVoteOrder().size() == 0)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Empty voteOrder");

		ChecksumModel checksumModel = isVoterTokenValidAndGetChecksum(castVoteRequest.getVoterToken());
		if (checksumModel == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Your voter token seems to be invalid!");

		// load models for URIs in castVoteRequst
		//TODO: use *Deserializers as in AssignProxyRequest
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
	 * 1) Check the integrity of the passed newBallot. Especially check the validity of its ChecksumModel.
	 *    The checksum must be known.
	 *
	 * 2) IF there is NO existing ballot for this poll with that checksum yet,
	 *    THEN save a new ballot
	 *    ELSE // a ballot with that checksum already exists
	 *      IF the level of the existing ballot is SMALLER then the passed newBallot.level
	 *      THEN do NOT update the existing ballot, because it was casted by a lower proxy or the user himself
	 *      ELSE update the existing ballot's level and vote order
	 *
	 *  3) FOR EACH directly delegated ChecksumModel
	 *              builder a childBallot and recursively try to store this childBallot.
	 *
	 *  Remark: The child ballot might not be stored when there already is one with a smaller level. This is
	 *          our recursion limit.
	 *
	 * @param newBallot the ballot that shall be stored. The ballot will be checked very thoroughly. Especially if the ballot's checksum is valid.
	 * @return the newly created or updated existing ballot  OR
	 *         null if the ballot wasn't stored due to an already existing ballot with a smaller level.
	 */
	@Transactional
	public BallotModel storeBallot(BallotModel newBallot) throws LiquidoException {
		log.debug("storeBallot("+newBallot+") checksum="+newBallot.getChecksum().getChecksum());

		//----- check validity of the ballot
		checkBallot(newBallot);

		//----- If there is no existing ballot yet with that checksum, then builder a completely new one.
		Optional<BallotModel> existingBallotOpt = ballotRepo.findByPollAndChecksum(newBallot.getPoll(), newBallot.getChecksum());
		BallotModel existingBallot;
		if (!existingBallotOpt.isPresent()) {
			log.trace("  Insert new ballot");
			existingBallot = ballotRepo.save(newBallot);
		} else {
			//----- Proxy must not overwrite a voter's own vote  OR  a vote from a proxy below
			existingBallot = existingBallotOpt.get();
			if (existingBallot.getLevel() < newBallot.getLevel()) {
				log.trace("  Will not overwrite existing ballot with smaller level " + existingBallot);
				return null;
			}

			log.trace("  Update existing ballot "+existingBallot.getId());
			existingBallot.setVoteOrder(newBallot.getVoteOrder());
			existingBallot.setLevel(newBallot.getLevel());
			if (!existingBallot.getChecksum().equals(newBallot.getChecksum()))
				throw new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Something is really wrong here!!! Checksums do not match");  // This should never happen
		}

		//----- recursively cast a vote for each delegated checksumModel
		int voteCount = 1;   // first vote is for the ballot itself.
		List<ChecksumModel> delegatedChecksums = checksumRepo.findByDelegatedTo(existingBallot.getChecksum());
		for (ChecksumModel delegatedChecksum : delegatedChecksums) {
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

		// Passed checksumModel in ballot MUST already exists in ChecksumRepo to be valid
		ChecksumModel existingChecksum = checksumRepo.findByChecksum(ballot.getChecksum().getChecksum())
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.CANNOT_CAST_VOTE, "Cannot cast vote: Ballot's checksum is invalid. (It cannot be found in ChecksumModel.)"));

	}

	//====================== private methods ========================

	private String calcVoterToken(Long userId, String userTokenSecret, Long areaId) {
		return BCrypt.hashpw(userId + userTokenSecret + areaId + serverSecret, bcryptSalt);
	}

	/**
	 * Calculate the checksum value for the given voterToken.
	 * The checksum is not stored or validated! This is just the mathematical calculation.
	 *
	 * @param voterToken token passed from user
	 * @return checksum = BCrypt.hashpw(voterToken + serverSecret, bcryptSalt)
	 */
	private String calcChecksumFromVoterToken(String voterToken) {
		//log.trace("calcChecksumFromVoterToken");
		return BCrypt.hashpw(voterToken + serverSecret, bcryptSalt);
	}

}
