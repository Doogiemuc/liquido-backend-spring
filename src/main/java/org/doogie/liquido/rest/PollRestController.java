package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.datarepos.ChecksumRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.ChecksumModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.rest.dto.JoinPollRequest;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.PollService;
import org.doogie.liquido.util.LiquidoRestUtils;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for working with Polls.
 *   /createNewPoll   add a proposal to a new poll
 *   /joinPoll        add a proposal to an existing poll that is (and must be) in elaboration
 */
@Slf4j
@BasePathAwareController
//@RepositoryRestController   and    @RequestMapping("postBallot")    Both do not really work  See https://jira.spring.io/browse/DATAREST-972
public class PollRestController {

	@Autowired
  PollService pollService;

	@Autowired
	CastVoteService castVoteService;

  @Autowired
  LawRepo lawRepo;

  @Autowired
  PollRepo pollRepo;

  @Autowired
	BallotRepo ballotRepo;

  @Autowired
	ChecksumRepo checksumRepo;

  @Autowired
	LiquidoRestUtils restUtils;

  //see https://docs.spring.io/spring-data/rest/docs/current/reference/html/#customizing-sdr.overriding-sdr-response-handlers

  /**
   * When an idea reaches its quorum then it becomes a law and its creator <i>can</i> builder a new poll for this proposal.
   * Other proposals need to join this poll before voting can be started.
   * @param pollResource the new poll with the link to (at least) one proposal, e.g.
   *          <pre>{ "proposals": [ "/liquido/v2/laws/152" ] }</pre>
   * @param resourceAssembler spring's PersistentEntityResourceAssembler that can builder the reply
   * @return the saved poll as HATEOAS resource with all _links
   * @throws LiquidoException when sent LawModel is not in state PROPOSAL or creation of new poll failed
   */
  @RequestMapping(value = "/createNewPoll", method = RequestMethod.POST)
  public @ResponseBody Resource createNewPoll(
      @RequestBody Resource<PollModel> pollResource,                         // how to convert URI to object? https://github.com/spring-projects/spring-hateoas/issues/292
      PersistentEntityResourceAssembler resourceAssembler
  ) throws LiquidoException
  {
    PollModel pollFromRequest = pollResource.getContent();
    LawModel proposalFromRequest = pollFromRequest.getProposals().iterator().next();             // This proposal is a "detached entity". Cannot simply be saved.
    //jpaContext.getEntityManagerByManagedType(PollModel.class).merge(proposal);      // DOES NOT WORK IN SPRING.  Must handle transaction via a seperate PollService class and @Transactional annotation there.
    PollModel createdPoll = pollService.createPoll(proposalFromRequest);

    PersistentEntityResource persistentEntityResource = resourceAssembler.toFullResource(createdPoll);

    log.trace("<= POST /createNewPoll: created Poll "+persistentEntityResource.getLink("self").getHref());

    /*
    Map<String, String> result = new HashMap<>();
    result.put("msg", "Created poll successfully");
    result.put("", resourceAssembler.getSelfLinkFor(savedPoll).getHref());
    */

    return persistentEntityResource;
  }

	/**
	 * Join a proposal into an existing poll (that must be in its ELABORATION phase)
	 * @param joinPollRequest with poll and proposal uri
	 * @throws LiquidoException if poll is not in its ELABORATION phase or proposal did not reach its quorum yet
	 */
  @RequestMapping(value = "/joinPoll",   //TODO: refactor to POST /polls/{pollId}/join?proposal=proposalURI
      method = RequestMethod.POST,
      consumes = "application/json")
  public @ResponseBody Resource joinPoll(@RequestBody JoinPollRequest joinPollRequest, PersistentEntityResourceAssembler resourceAssembler) throws LiquidoException {
		log.info("Proposal joins poll: "+joinPollRequest);

		// Now we would need to map the Spring Data Rest HATEOAS Uri to the actual entities.  But the clean solution is a bigger effort
		// https://stackoverflow.com/questions/37186417/resolving-entity-uri-in-custom-controller-spring-hateoas
		// https://stackoverflow.com/questions/49458567/mapping-hal-uri-in-requestbody-to-a-spring-data-rest-managed-entity
	  // TODO: use the new Deserializers as in JoinPollRequest.java or the @JsonComponent

		Long pollId = restUtils.getIdFromURI("polls", joinPollRequest.poll);
		PollModel poll = pollRepo.findById(pollId)
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.CANNOT_JOIN_POLL, "Cannot find poll with id="+pollId));

		Long proposalId = restUtils.getIdFromURI("laws", joinPollRequest.proposal);
		LawModel proposal = lawRepo.findById(proposalId)
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.CANNOT_JOIN_POLL, "Cannot find proposal with id="+pollId));

		PollModel updatedPoll = pollService.addProposalToPoll(proposal, poll);

		return resourceAssembler.toResource(updatedPoll);
	}

	@RequestMapping(value = "/polls/{pollId}/result", produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Lson getPollResult(@PathVariable(name="pollId") PollModel poll) throws LiquidoException {
  	if (poll == null)
  		throw new LiquidoException(LiquidoException.Errors.CANNOT_FIND_ENTITY, "Cannot find poll with that id");
  	if (!PollModel.PollStatus.FINISHED.equals(poll.getStatus()))
			throw new LiquidoException(LiquidoException.Errors.INVALID_POLL_STATUS, "Poll.id="+poll.getId()+" is not in status FINISHED");

		Lson pollResultsJson = pollService.calcPollResults(poll);

		return pollResultsJson;

		/*
		List<BallotModel> ballots = ballotRepo.findByPoll(poll);

		// For each place (1st, 2nd, ,3rd, ...) this list contains a mapping from the proposal.id to the
		// count how often this proposal was ordered in that place.
		List<Map<Long, Long>> counts = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			Map<Long, Long> counts_i = new HashMap<>();
			counts.add(counts_i);
			for (BallotModel ballot: ballots) {
				if (i < ballot.getVoteOrder().size()) {											// if voteOrder has that many places
					LawModel proposal = ballot.getVoteOrder().get(i);
					counts_i.merge(proposal.getId(), 1L, Long::sum);   // add 1 to the count for this proposal
				}
			}
		}

		return counts;

		*/
	}

	/**
	 * Get the ballot of the user himself, his direct proxy and the top proxy (if present)
	 * @param poll
	 * @param checksumId ID of the voter's checksum (that must exist)
	 * @return ownBallot, ballotOf DirectProxy and ballotOfTopProxy
	 * @throws LiquidoException when checksum with that ID does not exist
	 */
	@RequestMapping(value = "/polls/{pollId}/ballot/my")
	public @ResponseBody
	PersistentEntityResource getOwnBallot(
			@PathVariable(name="pollId") PollModel poll,
			@RequestParam("checksum") String checksumId,
			PersistentEntityResourceAssembler resourceAssembler
	) throws LiquidoException {
		//TODO: Make it possible to get ballots anonymously
		//TODO: Should it ONLY be possible to get ballots from voterToken? ChecksumModel checksum = castVoteService.isVoterTokenValidAndGetChecksum(voterToken);
		//      Or should everyone be able to see every ballot? Checksums are public! =>  But then I'd need a get ballot of direct/effective Proxy
		ChecksumModel checksum = checksumRepo.findByChecksum(checksumId)
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.CANNOT_FIND_ENTITY, "Cannot find checksum "+checksumId));
		BallotModel ownBallot = pollService.getBallotForChecksum(poll, checksum).orElse(null);
		return resourceAssembler.toFullResource(ownBallot);  // will return 404 when not found
	}

}
