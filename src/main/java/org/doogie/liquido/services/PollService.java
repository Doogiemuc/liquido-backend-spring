package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.jwt.AuthUtil;
import org.doogie.liquido.model.*;
import org.doogie.liquido.services.scheduler.FinishPollJob;
import org.doogie.liquido.services.voting.RankedPairVoting;
import org.doogie.liquido.testdata.LiquidoProperties;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;
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
	LiquidoProperties prop;

  @Autowired
  PollRepo pollRepo;

  @Autowired
	DelegationRepo delegationRepo;

  @Autowired
  LawRepo lawRepo;

  @Autowired
	BallotRepo ballotRepo;

  @Autowired
	RightToVoteRepo rightToVoteRepo;

  @Autowired
	ProxyService proxyService;

  @Autowired
	CastVoteService castVoteService;

  //@Autowired
	//TaskScheduler scheduler;

	/**
	 * Create a new poll. Then proposals can be added to this poll.
	 * @param title Title of the poll
	 * @return the saved PollModel
	 */
	public PollModel createPoll(@NonNull String title, AreaModel area) {
		//TODO: only allow creating a poll inside a team
		log.info("Create new poll. Title='"+title+"'");
		PollModel poll = new PollModel(title, area);

		// Automatically schedule voting phase if configured.
		if (prop.daysUntilVotingStarts > 0) {
			LocalDateTime votingStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).plusDays(prop.daysUntilVotingStarts);
			poll.setVotingStartAt(votingStart);
			poll.setVotingEndAt(votingStart.plusDays(prop.durationOfVotingPhase));
		}

		PollModel savedPoll = pollRepo.save(poll);
		return savedPoll;
	}

	/**
	 * Create a new poll inside a team. Only the admin is allowed to create a poll in a team
	 * @param title Title of the new poll
	 * @param area Area of the poll. All proposals will also need to be in this area.
	 * @param team The admin's team.
	 * @return the new poll
	 */
	@PreAuthorize(AuthUtil.HAS_ROLE_TEAM_ADMIN)
	public PollModel createPoll(@NonNull String title, AreaModel area, TeamModel team) {
		PollModel poll = this.createPoll(title, area);
		poll.setTeam(team);
		return pollRepo.save(poll);
	}


	/**
   * Start a new poll from an already existing proposal.
   * @param proposal an idea that reached its quorum and became a proposal. MUST be in status proposal and MUST NO already be part of another poll.
   * @return the newly created poll
   * @throws LiquidoException if passed LawModel is not in state PROPOSAL.
   */
  @Transactional    // run inside a transaction (all or nothing)
  public PollModel createPollWithProposal(@NonNull String title, @NonNull LawModel proposal) throws LiquidoException {
    PollModel poll = this.createPoll(title, proposal.getArea());
    poll = this.addProposalToPoll(proposal, poll);
    return poll;
  }

  /**
   * Add a proposals (ie. an ideas that reached its quorum) to an already existing poll and save the poll.
	 *
	 * Preconditions
	 * <ol>
	 *   <li>Proposal must be in status PROPOSAL</li>
	 *   <li>Poll must be in ELABORATION phase.</li>
	 *   <li>Proposal must be in the same area as the poll.</li>
	 *   <li>Proposal must not already be part of another poll.</li>
	 *   <li>Poll must not yet contain this proposal.</li>
	 *   <li>Poll must not yet contain a proposal with the same title.</li>
	 *   <li>User must not yet have a proposal in this poll.</li>
	 * </ol>
	 *
   * @param proposal a proposal (in status PROPOSAL)
   * @param poll a poll in status ELABORATION
   * @return the newly created poll
   * @throws LiquidoException if area or status of proposal or poll is wrong. And also when user already has a proposal in this poll.
   */
  public PollModel addProposalToPoll(@NonNull LawModel proposal, @NonNull PollModel poll) throws LiquidoException {
  	if (proposal.getStatus() != LawModel.LawStatus.PROPOSAL)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Cannot addProposalToPoll: poll(id="+poll.getId()+"): Proposal(id="+proposal.getId()+") is not in state PROPOSAL.");
    if (poll.getStatus() != PollModel.PollStatus.ELABORATION)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Cannot addProposalToPoll: Poll(id="+poll.getId()+") is not in ELABORATION phase");
    if (poll.getProposals().size() > 0 && !proposal.getArea().equals(poll.getArea()))
      throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Cannot addProposalToPoll: Proposal must be in the same area as the other proposals in poll(id="+poll.getId()+")");
    if (proposal.getPoll() != null)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Cannot addProposalToPoll: proposal(id="+proposal.getId()+") is already part of another poll.");
		if(poll.getProposals().contains(proposal))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Poll.id="+poll.getId()+" already contains proposal.id="+proposal.getId());
		if (poll.getProposals().stream().anyMatch(prop -> prop.title.equals(proposal.title)))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Poll.id="+poll.getId()+" already contains a proposal with the same title="+proposal.getTitle());
		//TODO: admin is allowed to add more than one proposal
    if (poll.getProposals().stream().anyMatch(prop -> {
			UserModel u1 = prop.getCreatedBy();
			UserModel u2 = proposal.getCreatedBy();
			boolean result = u1.equals(u2);
			return result;
		}))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, proposal.getCreatedBy().toStringShort() + " already has a proposal in poll(id="+poll.getId()+")");

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
	 * @return
	 */
  @Transactional
  public PollModel startVotingPhase(@NonNull PollModel poll) throws LiquidoException {
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
    poll.setVotingEndAt(votingStart.truncatedTo(ChronoUnit.DAYS).plusDays(prop.durationOfVotingPhase));     //voting ends in n days at midnight
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

	  return poll;
  }

  /** An auto-configured spring bean that gives us access to the Quartz scheduler */
  @Autowired
	SchedulerFactoryBean schedulerFactoryBean;

  /**
	 * Schedule a Quartz job that will end the voting phase of this poll
	 * @param poll a poll in voting phase
	 * @throws SchedulerException when job cannot be scheduled
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
			Scheduler scheduler =  schedulerFactoryBean.getScheduler();
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
	 * @return Winning proposal of this poll that now is a law.
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
		List<Long> allIds = poll.getProposals().stream().map(BaseModel::getId).collect(Collectors.toList());

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
			if (prop.getId() == firstWinnerId)	return prop;
		}
		throw new RuntimeException("Couldn't find winning Id in poll.");  // This should mathematically never happen!
	}


  public Lson calcPollResults(PollModel poll) {
	  Long ballotCount = ballotRepo.countByPoll(poll);
		return Lson.builder()
				.put("winner", poll.getWinner())
				.put("numBallots", ballotCount)
				.put("duelMatrix", poll.getDuelMatrix());
	}

	/**
	 * Get the number of already casted ballots of a currently running poll in VOTING.
	 * @param poll a poll in VOTING
	 * @return the number of casted ballots.
	 */
	public long getNumCastedBallots(PollModel poll) {
		return ballotRepo.countByPoll(poll);
	}

	/**
	 * Find the ballot that a user has casted in a poll. Since every ballot is anonymous, we need the user's voterToken to find the ballot.
	 * The voterToken will be verified.
	 *
	 * @param poll a poll at least in voting phase
	 * @param voterToken user's own voterToken. This will be validated against stored RightToVotes!
	 * @return (optionally) the ballot of voter in that poll or Optional.empty() if there is not ballot for that voterToken in that poll
	 * @thows LiquidoException when poll is in status elaboration or voterToken is invalid.
	 */
	public Optional<BallotModel> getBallotForVoterToken(PollModel poll, String voterToken) throws LiquidoException {
		if (PollModel.PollStatus.ELABORATION.equals(poll.getStatus()))
			throw new LiquidoException(LiquidoException.Errors.INVALID_POLL_STATUS, "Cannot get ballot of poll in ELABORATION");
		RightToVoteModel rightToVote = castVoteService.isVoterTokenValid(voterToken);
		Optional<BallotModel> ballot = ballotRepo.findByPollAndRightToVote(poll, rightToVote);
		return ballot;
	}

	/**
	 * Checks if the ballot with that checksum was counted correctly in the poll.
	 *
	 * The difference between this and {@link #getBallotForVoterToken(PollModel, String)} is that
	 * this verification also checks the voteOrder which is encoded into the ballot's checksum.
	 *
	 * @param poll the poll where the ballot was casted in
	 * @param checksum a ballot's checksum as returned by /castVote
	 * @return The ballot when checksum is correct or Optional.emtpy() if no ballot with that checksum could be found.
	 */
	public Optional<BallotModel> getBallotForChecksum(@NotNull PollModel poll, String checksum) {
		return ballotRepo.findByPollAndChecksum(poll, checksum);
	}

	/**
	 * When a user wants to check how his direct proxy has voted for him.
	 * @param poll a poll
	 * @param voterChecksum the voter's checksum
	 * @return (optionally) the ballot of the vote's direct proxy in this poll, if voter has a direct proxy
	 * @throws LiquidoException when this voter did not delegate his checksum to any proxy in this area
	 */
	public Optional<BallotModel> getBallotOfDirectProxy(PollModel poll, RightToVoteModel voterChecksum) throws LiquidoException {
		if (PollModel.PollStatus.ELABORATION.equals(poll.getStatus()))
			throw new LiquidoException(LiquidoException.Errors.INVALID_POLL_STATUS, "Cannot get ballot of poll in ELABORATION");
		return ballotRepo.findByPollAndRightToVote(poll, voterChecksum.getDelegatedTo());
	}

	public Optional<BallotModel> getBallotOfTopProxy(PollModel poll, RightToVoteModel voterChecksum) throws LiquidoException {
		if (PollModel.PollStatus.ELABORATION.equals(poll.getStatus()))
			throw new LiquidoException(LiquidoException.Errors.INVALID_POLL_STATUS, "Cannot get ballot of poll in ELABORATION");
		if (voterChecksum.getDelegatedTo() == null) return Optional.empty();
		RightToVoteModel topChecksum = findTopChecksumRec(voterChecksum);
		return ballotRepo.findByPollAndRightToVote(poll, topChecksum);
	}


	private RightToVoteModel findTopChecksumRec(RightToVoteModel checksum) {
		if (checksum.getDelegatedTo() == null) return checksum;
		return findTopChecksumRec(checksum.getDelegatedTo());
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
		RightToVoteModel voterChecksum = castVoteService.isVoterTokenValid(voterToken);
		return findEffectiveProxyRec(poll, voter, voterChecksum);
	}

  /* private recursive part of finding the effective proxy. Passing checksums along the way up the tree. */
	private Optional<UserModel> findEffectiveProxyRec(PollModel poll, UserModel voter, RightToVoteModel voterChecksum) {
		if (voterChecksum.getHashedVoterToken() == null)
			throw new RuntimeException("This does not look like a valid checksum: "+voterChecksum);
		if (voterChecksum.getPublicProxy() != null &&	!voterChecksum.getPublicProxy().equals(voter))
			throw new RuntimeException("Data inconsistency: " + voterChecksum + " is not the checksum of public proxy="+voter);

		//----- Check if there is a ballot for this checksum. If not this voter did not vote yet.
		Optional<BallotModel> ballot = ballotRepo.findByPollAndRightToVote(poll, voterChecksum);
		if (!ballot.isPresent()) return Optional.empty();

		//----- If ballot has level 0, then this voter voted for himself. He is the effective proxy.
		if (ballot.get().getLevel() == 0) return Optional.of(voter);

		//----- If voter's checksum is not delegated, (although he has a ballot that has level >0 then he is his own effective proxy.
		// This may happen when the voter removed his proxy, after his proxy voted for him.
		if (voterChecksum.getDelegatedTo() == null) return Optional.of(voter);

		//----- Get voters direct proxy, which must exist because the voters checksum is delegated
		DelegationModel delegation = delegationRepo.findByAreaAndFromUser(poll.getArea(), voter)
				.orElseThrow(() -> new RuntimeException("Data inconsistency: Voter has a delegated checksum but no direct proxy! "+voter+", "+voterChecksum));

		//----- at last recursively check for that proxy up in the tree.
		return findEffectiveProxyRec(poll, delegation.getToProxy(), voterChecksum.getDelegatedTo());

		//of course the order of all the IF statements in this method is extremely important. Managed to do it without any "else" !! :-)
	}

	/**
	 * Delete a poll and all ballots casted in it.
	 * @param poll The poll to delete
	 * @param deleteProposals wether to delete the porposals in the poll
	 */
	@PreAuthorize(AuthUtil.HAS_ROLE_TEAM_ADMIN)  // only the admin may delete polls.  See application.properties for admin name and email
	@Transactional
	public void deletePoll(@NonNull PollModel poll, boolean deleteProposals) {
		log.info("DELETE "+poll);
		if (poll == null) return;

		//TODO: !!! check that poll is in current user's team !!!


		// unlink proposals/laws from poll and then (optionally) delete them
		for (LawModel prop : poll.getProposals()) {
			prop.setPoll(null);
			if (deleteProposals) {
				lawRepo.delete(prop);
			} else {
				lawRepo.save(prop);
			}
		}

		// Delete casted Ballots in poll
		for (BallotModel ballot : ballotRepo.findByPoll(poll)) {
			ballotRepo.delete(ballot);
		}

		// Delete the poll
		pollRepo.delete(poll);
	}
}

