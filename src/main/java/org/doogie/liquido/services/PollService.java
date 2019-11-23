package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.model.*;
import org.doogie.liquido.services.scheduler.FinishPollJob;
import org.doogie.liquido.services.voting.RankedPairVoting;
import org.doogie.liquido.util.LiquidoProperties;
import org.doogie.liquido.util.Lson;
import org.doogie.liquido.util.Matrix;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * This spring component implements the business logic for {@link org.doogie.liquido.model.PollModel}
 * I am trying to keep the Models as dumb as possible.
 * All business rules and security checks are implemented here.
 */
@Slf4j
@Service
public class PollService {

  @Autowired
  LiquidoProperties props;

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
	CastVoteService castVoteService;

  //@Autowired
	//TaskScheduler scheduler;


	/**
   * Start a new poll from an already existing proposal.
   * @param proposal an idea that reached its quorum and became a proposal. MUST be in status proposal and MUST NO already be part of another poll.
   * @return the newly created poll
   * @throws LiquidoException if passed LawModel is not in state PROPOSAL.
   */
  @Transactional    // run inside a transaction (all or nothing)
  public PollModel createPoll(@NonNull String title, @NonNull LawModel proposal) throws LiquidoException {
    //===== sanity checks
    if (proposal == null)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_POLL, "Proposal must not be null");
    // The proposal must be in status PROPOSAL
    if (!LawModel.LawStatus.PROPOSAL.equals(proposal.getStatus()))
      throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_POLL, "Need proposal with quorum for creating a poll!");
    // that proposal must not already be linked to another poll
    if (proposal.getPoll() != null)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_POLL, "Proposal (id="+proposal.getId()+") is already part of another poll!");

    //===== builder new Poll with one initial proposal
    log.info("Create new poll from proposal "+proposal.toStringShort());
    PollModel poll = new PollModel();
    poll.setTitle(title);
    // voting starts n days in the future (at midnight)
    LocalDateTime votingStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).plusDays(props.getInt(LiquidoProperties.KEY.DAYS_UNTIL_VOTING_STARTS));
    poll.setVotingStartAt(votingStart);
    poll.setVotingEndAt(votingStart.plusDays(props.getInt(LiquidoProperties.KEY.DURATION_OF_VOTING_PHASE)));
    //LawModel proposalInDB = lawRepo.findByTitle(proposal.getTitle());  //BUGFIX: I have to load the proposal from DB, otherwise: exception "Detached entity passed to persist" => Fixed by setting cascade = CascadeType.MERGE in PollModel
    poll = addProposalToPoll(proposal, poll);
    PollModel savedPoll = pollRepo.save(poll);
    return savedPoll;
  }

  /**
   * Add a proposals (ie. an ideas that reached its quorum) to an existing poll and save the poll.
   * @param proposal a proposal (in status PROPOSAL)
   * @param poll a poll in status ELABORATION
   * @return the newly created poll
   * @throws LiquidoException if area or status of proposal or poll is wrong. And also when user already has a proposal in this poll.
   */
  public PollModel addProposalToPoll(@NonNull LawModel proposal, @NonNull PollModel poll) throws LiquidoException {
    if (proposal.getStatus() != LawModel.LawStatus.PROPOSAL)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Cannot add proposal(id="+proposal.getId()+") to poll(id="+poll.getId()+", because proposal is not in state PROPOSAL.");
    if (poll.getStatus() != PollModel.PollStatus.ELABORATION)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Cannot add proposal, because poll id="+poll.getId()+" is not in ELABORATION phase");
    if (poll.getProposals().size() > 0 && !proposal.getArea().equals(poll.getArea()))
      throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Added proposal must be in the same area as the other proposals in this poll.");
		if(poll.getProposals().contains(proposal))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Poll.id="+poll.getId()+" already contains proposal.id="+proposal.getId());
    for (LawModel p : poll.getProposals()) {
			if (p.getCreatedBy().equals(proposal.getCreatedBy()))
				throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, p.getCreatedBy().getEmail()+"(id="+p.getCreatedBy().getId()+") already have a proposal in poll(id="+poll.getId()+")");
    }

    log.debug("addProposalToPoll(proposal.id="+proposal.getId()+", poll.id="+poll.getId()+")");
    proposal.setStatus(LawModel.LawStatus.ELABORATION);
    poll.getProposals().add(proposal);
    proposal.setPoll(poll);
    return pollRepo.save(poll);
  }

  /**
   * Start the voting phase of the given poll.
   * Poll must be in elaboration phase and must have at least two proposals
   * @param poll a poll in elaboration phase with at least two proposals
   */
  @Transactional
  public void startVotingPhase(@NonNull PollModel poll) throws LiquidoException {
    log.info("startVotingPhase of "+poll.toString());
    if (poll.getStatus() != PollModel.PollStatus.ELABORATION)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_START_VOTING_PHASE, "Poll(id=\"+poll.id+\") must be in status ELABORATION");
    if (poll.getProposals().size() < 2)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_START_VOTING_PHASE, "Poll(id="+poll.id+") must have at least two alternative proposals");

    for (LawModel proposal : poll.getProposals()) {
      proposal.setStatus(LawModel.LawStatus.VOTING);
    }
    poll.setStatus(PollModel.PollStatus.VOTING);
    LocalDateTime votingStart = LocalDateTime.now();			// LocalDateTime is without a timezone
    poll.setVotingStartAt(votingStart);   //record the exact datetime when the voting phase started.
    poll.setVotingEndAt(votingStart.truncatedTo(ChronoUnit.DAYS).plusDays(props.getInt(LiquidoProperties.KEY.DURATION_OF_VOTING_PHASE)));     //voting ends in n days at midnight
		pollRepo.save(poll);

		//----- schedule a Quartz Job that will finish the voting phase at poll.votingEndAt() date
	  try {
			Date votingEndAtDate = Date.from(poll.getVotingEndAt().atZone(ZoneId.systemDefault()).toInstant());
			scheduleJobToFinishPoll(poll, votingEndAtDate);
	  } catch (SchedulerException e) {
	  	String msg = "Cannot start voting phase, because of scheduler error";
		  log.error(msg, e);
		  throw new LiquidoException(LiquidoException.Errors.CANNOT_START_VOTING_PHASE, msg, e);
	  }
  }

  /** An auto-configured spring bean that gives us access to the Quartz scheduler */
  @Autowired
	SchedulerFactoryBean schedulerFactoryBean;

	/**
	 * Schedule a Quartz job that will end the voting phase of this poll
	 * @param poll a poll in voting phase
	 * @throws SchedulerException
	 */
	private void scheduleJobToFinishPoll(@NonNull PollModel poll, Date votingEndAtDate) throws SchedulerException {
		JobKey finishPollJobKey = new JobKey("finishVoting_pollId="+poll.getId(), "finishPollJobGroup");

		JobDetail jobDetail = newJob(FinishPollJob.class)
				.withIdentity(finishPollJobKey)
				.withDescription("Finish voting phase of poll.id="+poll.getId())
				.usingJobData("poll.id", poll.getId())
				.storeDurably()
				.build();

		Trigger trigger = newTrigger()
				.withIdentity("finishVotingTrigger_poll.id="+poll.getId(), "finishPollTriggerGroup")
				.withDescription("Finish voting phase of poll.id="+poll.getId())
				.startAt(votingEndAtDate )
				.withSchedule(SimpleScheduleBuilder.simpleSchedule()
						.withRepeatCount(0)
						.withIntervalInMinutes(1)
						.withMisfireHandlingInstructionFireNow()		// If backend was down at the time when the Job would have been scheduled, then fire the job immidiately when the app is back up
				)
				.build();

		try {
			//Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
			Scheduler scheduler = schedulerFactoryBean.getScheduler();
			if (!scheduler.isStarted())
				log.warn("Quartz job scheduler is not started. It should be started!");
			scheduler.scheduleJob(jobDetail, trigger);
		} catch (SchedulerException e) {
			log.error("Cannot schedule task to finish poll.", e);
			throw e;
		}
	}

	/**
	 * Finish the voting phase of a poll and calculate the winning proposal.
	 * @param poll A poll in voting phase
	 * @return Winning proposal of this poll that now is a proposal.
	 * @throws LiquidoException When poll is not in voting phase
	 */
	@Transactional
  public LawModel finishVotingPhase(@NonNull PollModel poll) throws LiquidoException {
		log.debug("finishVotingPhase(poll.id="+poll.getId()+")");
    if (!PollModel.PollStatus.VOTING.equals(poll.getStatus()))
      throw new LiquidoException(LiquidoException.Errors.CANNOT_FINISH_POLL, "Cannot finishVotingPhase: Poll must be in status VOTING.");

    poll.setStatus(PollModel.PollStatus.FINISHED);
		poll.setVotingEndAt(LocalDateTime.now());
		poll.getProposals().forEach(p -> p.setStatus(LawModel.LawStatus.LOST));

		//----- calc winner of poll
		List<BallotModel> ballots = ballotRepo.findByPoll(poll);
		LawModel winningProposal = calcWinnerOfPoll(poll, ballots);
		log.info("Winner of Poll(id="+poll.getId()+") is "+winningProposal);

		//----- save results
		if (winningProposal != null) {
			winningProposal.setStatus(LawModel.LawStatus.LAW);
			poll.setWinner(winningProposal);
			lawRepo.save(winningProposal);
		}
		pollRepo.save(poll);
    return winningProposal;
  }

	/**
	 * Calculate the pairwise comparision of every pair of proposals in every ballot's voteOrder.
	 *
	 * This method just extracts all the IDs from poll and ballots and the forwards to the
	 * {@link RankedPairVoting#calcRankedPairWinners(Matrix)} method.
	 *
	 * @param poll a poll that just finished its voting phase
	 * @param ballots the ballots casted in this poll
	 * @return the duelMatrix, which counts the number of preferences for each pair of proposals.
	 * @throws LiquidoException When poll is not in status FINISHED
	 */
	@Transactional
	public LawModel calcWinnerOfPoll(@NonNull PollModel poll, @NonNull List<BallotModel> ballots) throws LiquidoException {
		if (!PollModel.PollStatus.FINISHED.equals(poll.getStatus()))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_FINISH_POLL, "Poll must be in status finished to calcDuelMatrix!");

		// Ordered list of proposal IDs in poll.  (Keep in mind that the proposal in a poll are not ordered.)
		List<Long> allIds = poll.getProposals().stream().map(p -> p.getId()).collect(Collectors.toList());

		// map the vote order of each ballot to a List of ids
		List<List<Long>> idsInBallots = ballots.stream().map(
				ballot -> ballot.getVoteOrder().stream().map(BaseModel::getId).collect(Collectors.toList())
		).collect(Collectors.toList());

		// wizardry mathematical magic :-)
		Matrix duelMatrix = RankedPairVoting.calcDuelMatrix(allIds, idsInBallots);
		poll.setDuelMatrix(duelMatrix);

		List<Integer> winnerIndexes = RankedPairVoting.calcRankedPairWinners(duelMatrix);
		if (winnerIndexes.size() == 0) {
			log.warn("There is no winner in poll "+poll);  // This may for example happen when there are no votes at all.
			return null;
		}
		if (winnerIndexes.size() > 1) log.warn("There is more than one winner in "+poll);
		long firstWinnerId = allIds.get(winnerIndexes.get(0));
		for(LawModel prop: poll.getProposals()) {
			if (prop.getId().longValue() == firstWinnerId)	return prop;
		}
		throw new RuntimeException("Couldn't find winning Id in poll.");  // This should mathematically never happen!
	}


  public Lson calcPollResults(PollModel poll) {
	  Long ballotCount = ballotRepo.countByPoll(poll);
		Lson lson = Lson.builder()
				.put("winner", poll.getWinner())
				.put("numBallots", ballotCount)
				.put("duelMatrix", poll.getDuelMatrix())
				;
		return lson;
	}

	/**
	 * Find the ballot of an anonymous voterToken
	 * @param poll a poll at least in voting phase
	 * @param voterToken user's own voterToken. This will be validated. voterToken's checksum must already exist!
	 * @return (optionally) the ballot of voter in that poll or Optional.empty() if there is not ballot for that voterToken
	 * @thows LiquidoException when poll is in status elaboration or voterToken or is invalid or unknown
	 */
	public Optional<BallotModel> getBallotForVoterToken(PollModel poll, String voterToken) throws LiquidoException {
		ChecksumModel checksum = castVoteService.isVoterTokenValidAndGetChecksum(voterToken);
	 	if (PollModel.PollStatus.ELABORATION.equals(poll.getStatus()))
  		throw new LiquidoException(LiquidoException.Errors.INVALID_POLL_STATUS, "Cannot get ballot of poll in ELABORATION");
		return ballotRepo.findByPollAndChecksum(poll, checksum);
	}

	/**
	 * When a user wants to check how his direct proxy has  voted for him.
	 * @param poll a poll
	 * @param voterChecksum the voter's checksum
	 * @return (optionally) the ballot of the vote's direct proxy in this poll, if voter has a direct proxy
	 * @throws LiquidoException when this voter did not delegate his checksum to any proxy in this area
	 */
	public Optional<BallotModel> getBallotOfDirectProxy(PollModel poll, ChecksumModel voterChecksum) throws LiquidoException {
		if (PollModel.PollStatus.ELABORATION.equals(poll.getStatus()))
			throw new LiquidoException(LiquidoException.Errors.INVALID_POLL_STATUS, "Cannot get ballot of poll in ELABORATION");
		return ballotRepo.findByPollAndChecksum(poll, voterChecksum.getDelegatedTo());
	}

	//TODO: actually I need getBallotOfEffectiveProxy
	public Optional<BallotModel> getBallotOfTopProxy(PollModel poll, ChecksumModel voterChecksum) throws LiquidoException {
		if (PollModel.PollStatus.ELABORATION.equals(poll.getStatus()))
			throw new LiquidoException(LiquidoException.Errors.INVALID_POLL_STATUS, "Cannot get ballot of poll in ELABORATION");
		if (voterChecksum.getDelegatedTo() == null) return Optional.empty();
		ChecksumModel topChecksum = findTopChecksumRec(voterChecksum);
		return ballotRepo.findByPollAndChecksum(poll, topChecksum);
	}


	private ChecksumModel findTopChecksumRec(ChecksumModel checksum) {
		if (checksum.getDelegatedTo() == null) return checksum;
		return findTopChecksumRec(checksum.getDelegatedTo());
	}

	/**
	 * Checks if a given checksum was counted in that poll
	 * @param poll SHOULD have status == FINISHED
	 * @param checksumStr a checksum string
	 * @return true if a ballot with that checksum was counted in that poll
	 */
	public boolean verifyChecksum(@NotNull PollModel poll, @NotNull String checksumStr) {
		Optional<ChecksumModel> checksumOpt = checksumRepo.findByChecksum(checksumStr);
		if (!checksumOpt.isPresent()) return false;
		Optional<BallotModel> ballotOpt = ballotRepo.findByPollAndChecksum(poll, checksumOpt.get());
		return ballotOpt.isPresent() && ballotOpt.get().getPoll().equals(poll);
	}

	/**
	 * Find the proxy that casted the vote in this poll.
	 * This recursive method walks up the tree of DelegationModels and Checksum delegations in parallel
	 * until it reaches a ballot with level == 0. That is the ballot casted by the effective proxy.
	 *
	 * This may be the voter himself, if he voted himself.
	 * This may be the voters direct proxy
	 * Or this may be any other proxy up in the tree, not necessarily the top proxy.
	 * Or there might be no effective proxy yet, when not the voter nor his proxies voted yet in this poll.
	 * *
	 * @param poll a poll in voting or finished
	 * @param voter The voter to check who may have delegated his right to vote to a proxy.
	 * @param voterToken This voter's token that must be valid and match to a known checksum
	 * @return Optional.empty() IF there is no ballot for this checksum, ie. user has not voted yet at all.
	 *         Optional.of(voter) IF voter has not delegated his checksum to any proxy. (Or maybe only requested a delegation)
	 *				 Optional.of(effectiveProxy) where effective proxy is the one who actually voted (ballot.level == 0) for the voter
	 * @throws LiquidoException When poll is in status ELABORATION or
	 */
	public Optional<UserModel> findEffectiveProxy(PollModel poll, UserModel voter, String voterToken) throws LiquidoException {
		if (PollModel.PollStatus.ELABORATION.equals(poll.getStatus()))
			throw new LiquidoException(LiquidoException.Errors.INVALID_POLL_STATUS, "Cannot find effective proxy, because poll is not in voting phase or finished");

		//----- get checksum from voterToken
		ChecksumModel voterChecksum = castVoteService.isVoterTokenValidAndGetChecksum(voterToken);
		return findEffectiveProxyRec(poll, voter, voterChecksum);
	}

  /* private recursive part of finding the effective proxy. Passing checksums along the way up the tree. */
	private Optional<UserModel> findEffectiveProxyRec(PollModel poll, UserModel voter, ChecksumModel voterChecksum) {
		if (voterChecksum.getChecksum() == null)
			throw new RuntimeException("This does not look like a valid checksum: "+voterChecksum);
		if (voterChecksum.getPublicProxy() != null &&	!voterChecksum.getPublicProxy().equals(voter))
			throw new RuntimeException("Data inconsistency: " + voterChecksum + " is not the checksum of public proxy="+voter);

		//----- Check if there is a ballot for this checksum. If not this voter did not vote yet.
		Optional<BallotModel> ballot = ballotRepo.findByPollAndChecksum(poll, voterChecksum);
		if (!ballot.isPresent()) return Optional.empty();

		//----- If ballot has level 0, then this voter voted for himself. He is the effective proxy.
		if (ballot.get().getLevel() == 0) return Optional.of(voter);

		//----- If voter's checksum is not delegated, (although he has a ballot that has level >0 then he is his own effective proxy.
		// This may happen when the voter removed his proxy, after his proxy voted for him.
		if (voterChecksum.getDelegatedTo() == null) return Optional.of(voter);

		//----- Get voters direct proxy, which must exist because the voters checksum is delegated
		DelegationModel delegation = delegationRepo.findByAreaAndFromUser(poll.getArea(), voter)
				.orElseThrow(() -> new RuntimeException("Data inconsistency: Voter has a delegated checksum but no direct proxy! "+voter+", "+voterChecksum));

		//----- if delegation is non-transitive, only check if direct proxy has voted for himself or not. No recursion.
		if (delegation.isTransitive() == false) {
			Optional<BallotModel> ballotOfNonTransitiveProxy = ballotRepo.findByPollAndChecksum(poll, voterChecksum.getDelegatedTo());
			if (ballotOfNonTransitiveProxy.isPresent() && ballotOfNonTransitiveProxy.get().getLevel() == 0) return Optional.of(delegation.getToProxy());
			return Optional.empty();
		}

		//----- at last recursively check for that proxy up in the tree.
		return findEffectiveProxyRec(poll, delegation.getToProxy(), voterChecksum.getDelegatedTo());

		//of course the order of all the IF statements in this method is extremely important. Managed to do it without any "else" !! :-)
	}

	/**
	 * Delete a poll and all ballots casted in it.
	 * This will not delete any Proposals.
	 * @param poll The poll to delete
	 */
	@PreAuthorize("hasRole('ROLE_ADMIN')")  // only the admin may delete polls.  See application.properties for admin name and email
	@Transactional
	public void deletePoll(@NonNull PollModel poll) {
		log.info("DELETE "+poll);
		if (poll == null) return;

		// unlink proposals/laws from poll
		for (LawModel prop : poll.getProposals()) {
			prop.setPoll(null);
			lawRepo.save(prop);
		}

		// Delete casted Ballots in poll
		for (BallotModel ballot : ballotRepo.findByPoll(poll)) {
			ballotRepo.delete(ballot);
		}

		// Delete the poll
		pollRepo.delete(poll);
	}
}

