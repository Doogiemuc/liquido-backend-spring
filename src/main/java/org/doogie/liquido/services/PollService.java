package org.doogie.liquido.services;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.services.scheduler.FinishPollJob;
import org.doogie.liquido.services.voting.RankedPairVoting;
import org.doogie.liquido.util.LiquidoProperties;
import org.doogie.liquido.util.Lson;
import org.doogie.liquido.util.Matrix;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

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
  LawRepo lawRepo;

  @Autowired
	BallotRepo ballotRepo;

  //@Autowired
	//TaskScheduler scheduler;


	/**
   * Start a new poll from an already existing proposal.
   * @param proposal an idea that reached its quorum and became a proposal. MUST be in status proposal and MUST NO already be part of another poll.
   * @return the newly created poll
   * @throws LiquidoException if passed LawModel is not in state PROPOSAL.
   */
  @Transactional    // run inside a transaction (all or nothing)
  public PollModel createPoll(@NonNull LawModel proposal) throws LiquidoException {
    //===== sanity checks: There must be at least one proposal (in status PROPOSAL)
    if (proposal == null)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_POLL, "Proposal must not be null");
    if (!LawModel.LawStatus.PROPOSAL.equals(proposal.getStatus()))
      throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_POLL, "Need proposal with quorum for creating a poll!");
    // that proposal must not already be linked to another poll
    if (proposal.getPoll() != null)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_POLL, "Proposal (id="+proposal.getId()+") is already part of another poll!");

    //===== builder new Poll with one initial proposal
    log.info("Create new poll. InitialProposal (id={}): {}", proposal.getId(), proposal.getTitle());
    PollModel poll = new PollModel();
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
  public void startVotingPhase(@NonNull PollModel poll) throws LiquidoException, SchedulerException {
    log.info("startVotingPhase of "+poll.toString());
    if (poll.getStatus() != PollModel.PollStatus.ELABORATION)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_START_VOTING_PHASE, "Poll must be in status ELABORATION");
    if (poll.getProposals().size() < 2)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_START_VOTING_PHASE, "Poll must have at least two alternative proposals");
    for (LawModel proposal : poll.getProposals()) {
      proposal.setStatus(LawModel.LawStatus.VOTING);
    }
    poll.setStatus(PollModel.PollStatus.VOTING);
    LocalDateTime votingStart = LocalDateTime.now();			// LocalDateTime is without a timezone
    poll.setVotingStartAt(votingStart);   //record the exact datetime when the voting phase started.
    poll.setVotingEndAt(votingStart.truncatedTo(ChronoUnit.DAYS).plusDays(props.getInt(LiquidoProperties.KEY.DURATION_OF_VOTING_PHASE)));     //voting ends in n days at midnight
		pollRepo.save(poll);

		//----- schedule a Quartz Job that will finish the voting phase at poll.votingEndAt() date
		scheduleJobToFinishPoll(poll);
  }

  /** An auto-configured spring bean that gives us access to the Quartz scheduler */
  @Autowired
	SchedulerFactoryBean schedulerFactoryBean;

	/**
	 * Schedule a Quartz job that will end the voting phase of this poll
	 * @param poll a poll in voting phase
	 * @throws SchedulerException
	 */
	private void scheduleJobToFinishPoll(@NonNull PollModel poll) throws SchedulerException {
		JobDetail jobDetail = newJob(FinishPollJob.class)
				.withIdentity("finishVoting_pollId="+poll.getId(), "pollJobs")
				.withDescription("Finish voting phase of poll.id="+poll.getId())
				.usingJobData("poll.id", poll.getId())
				.storeDurably()
				.build();

		Date votingEndAtDate = Date.from(poll.getVotingEndAt().atZone(ZoneId.systemDefault()).toInstant());

		Trigger trigger = newTrigger()
				.withIdentity("finishVotingTrigger_poll.id="+poll.getId(), "pollTrigger")
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
	 * @return Winnin proposal of this poll that now is a law.
	 * @throws LiquidoException When poll is not in voting phase
	 */
	@Transactional
  public LawModel finishVotingPhase(@NonNull PollModel poll) throws LiquidoException {
		log.debug("finishVotingPhase(poll.id="+poll.getId()+")");
    if (!poll.getStatus().equals(PollModel.PollStatus.VOTING))
      throw new LiquidoException(LiquidoException.Errors.CANNOT_FINISH_POLL, "Cannot finishVotingPhase: Poll must be in status VOTING.");

    poll.setStatus(PollModel.PollStatus.FINISHED);
    for(LawModel proposal : poll.getProposals()) {
      proposal.setStatus(LawModel.LawStatus.DROPPED);
    }
    poll.setVotingEndAt(LocalDateTime.now());

		//----- calc winner of poll
		//TODO: make the method of calculating a winner configurable: LawModel winningProposal = calcSchulzeMethodWinners(poll).get(0);

		List<BallotModel> ballots = ballotRepo.findByPoll(poll);
		LawModel winningProposal = RankedPairVoting.calcRankedPairsWinners(poll, ballots).get(0);
		winningProposal.setStatus(LawModel.LawStatus.LAW);
		poll.setWinner(winningProposal);
		lawRepo.save(winningProposal);
		pollRepo.save(poll);
    return winningProposal;
  }


  public Lson calcPollResults(PollModel poll) {
		List<BallotModel> ballots = ballotRepo.findByPoll(poll);
		Matrix duelMatrix = RankedPairVoting.calcDuelMatrix(poll, ballots);
		Lson lson = Lson.builder()
				.put("winner", poll.getWinner())
				.put("numBallots", ballots.size())
				.put("duelMatrix", duelMatrix.getRawData())
				;
		return lson;
	}

  



}

