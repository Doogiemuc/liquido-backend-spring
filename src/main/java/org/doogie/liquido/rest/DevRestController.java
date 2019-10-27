package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.PollService;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.resource.HttpResource;

import javax.validation.constraints.NotNull;
import java.util.SortedSet;

/**
 * This controller is only available for development and testing
 */
@Slf4j
@BasePathAwareController
@Profile({"dev", "test"})			// This controller is only available in dev or test environment
public class DevRestController {
	@Autowired
	LawRepo lawRepo;

	@Autowired
	PollRepo pollRepo;

	@Autowired
	BallotRepo ballotRepo;

	@Autowired
	PollService pollService;

	/**
	 * Manually start the voting phase of a poll via REST call. Poll MUTS be in ELABORATION phase.
	 * This is used in tests, because wraping time is so complicated.
	 * @param poll a poll that must be in ELABORATION phase
	 * @return HTTP 200 and Ok message as JSON
	 * @throws LiquidoException for example when voting phase cannot be started because of wrong status in poll
	 */
	@RequestMapping(value = "/dev/polls/{pollId}/startVotingPhase")
	public @ResponseBody Lson devStartVotingPhase(@PathVariable(name="pollId") PollModel poll) throws LiquidoException {
		if (poll == null)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_START_VOTING_PHASE, "Cannot find poll with that id");
		log.debug("DEV: Starting voting phase of "+poll);
		pollService.startVotingPhase(poll);
		return new Lson().put("ok", "Started voting phase of poll.id="+poll.id);
	}

	/**
	 * Manually finish the voting phase of a poll. This is used in tests.
	 * @param poll a poll that must be in VOTING phase
	 * @return HTTP 200 and a JSON with the winning LawModel
	 * @throws LiquidoException for example when poll is not in correct status
	 */
	@RequestMapping(value = "/dev/polls/{pollId}/finishVotingPhase")
	public @ResponseBody Lson finishVotingPhase(@PathVariable(name="pollId") PollModel poll) throws LiquidoException {
		if (poll == null)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_START_VOTING_PHASE, "Cannot find poll with that id");
		log.debug("DEV: Finish voting phase of "+poll);
		LawModel winner = pollService.finishVotingPhase(poll);
		return new Lson()
			.put("ok", "Finished voting phase of poll.id="+poll.id)
			.put("winner", winner);
	}

	/**
	 * Completely delete poll and all casted ballots in this poll. Will NOT delete any proposals
	 * @param poll an existing poll
	 * @return HTTP 200
	 */
	@RequestMapping(value = "/dev/polls/{pollId}", method = RequestMethod.DELETE)
	@PreAuthorize("hasRole('ROLE_ADMIN')")  // only the admin may delete polls.  See application.properties for admin name and email
	public ResponseEntity deletePoll(@PathVariable(name="pollId") PollModel poll) {
		pollService.deletePoll(poll);
		return ResponseEntity.ok().build();
	}


}
