package org.doogie.liquido.services.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.PollService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quartz job for finishing the voting phase of a poll.
 */
@Slf4j
@Component
public class FinishPollJob implements Job {
	@Autowired
	PollService pollService;

	@Autowired
	PollRepo pollRepo;

	/** Qwartz needs an empty constructor :-) Must set params via jobExecutionContext*/
	public FinishPollJob() {	}

	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		Long pollId = jobExecutionContext.getJobDetail().getJobDataMap().getLong("poll.id");
		log.info("Quartz Job: Finish voting phase of poll.id="+pollId);
		PollModel poll = pollRepo.findById(pollId)
				.orElseThrow(() -> new JobExecutionException("Cannot find poll with id="+pollId));
		try {
			pollService.finishVotingPhase(poll);
		} catch (LiquidoException lqe) {
			String errorMsg = "Cannot finish voting phase of poll.id="+pollId;
			log.error(errorMsg, lqe);
			throw new JobExecutionException(errorMsg, lqe);
		}
	}
}
