package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.datarepos.RightToVoteRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.model.*;
import org.doogie.liquido.rest.dto.JoinPollRequest;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LawService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.PollService;
import org.doogie.liquido.util.LiquidoRestUtils;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * REST controller for working with Polls.
 */
@Slf4j
@BasePathAwareController		// expose controller under basePath
//@RepositoryRestController   and    @RequestMapping("postBallot")    Both do not really work:
// https://jira.spring.io/browse/DATAREST-972
// https://faithfull.me/overriding-spring-data-rest-repositories/
public class PollRestController {

	@Autowired
  PollService pollService;

	@Autowired
	CastVoteService castVoteService;

	@Autowired
	LawService lawService;

  @Autowired
  LawRepo lawRepo;

  @Autowired
  PollRepo pollRepo;

  @Autowired
	BallotRepo ballotRepo;

  @Autowired
	RightToVoteRepo rightToVoteRepo;

  @Autowired
  Environment springEnv;

	@Autowired
	private ProjectionFactory factory;

	@Autowired
	EntityLinks entityLinks;

	@Autowired
	LiquidoRestUtils restUtils;

  //see https://docs.spring.io/spring-data/rest/docs/current/reference/html/#customizing-sdr.overriding-sdr-response-handlers

  /**
   * When an idea reaches its quorum then it becomes a proposal and its creator <i>can</i> builder a new poll for this proposal.
   * Other proposals need to join this poll before voting can be started.
   * @param pollResource the new poll with the link to (at least) one proposal, e.g.
   *          <pre>{ title: "Poll Title", "proposals": [ "/liquido/v2/laws/152" ] }</pre>
   * @param resourceAssembler spring's PersistentEntityResourceAssembler that can builder the reply
   * @return the saved poll as HATEOAS resource with all _links
   * @throws LiquidoException when sent LawModel is not in state PROPOSAL or creation of new poll failed
   */
  @RequestMapping(value = "/polls/add", method = RequestMethod.POST)
  public @ResponseBody Resource createNewPoll(
      @RequestBody Resource<PollModel> pollResource,                         // how to convert URI to object? https://github.com/spring-projects/spring-hateoas/issues/292
      PersistentEntityResourceAssembler resourceAssembler
  ) throws LiquidoException
  {
    PollModel pollFromRequest = pollResource.getContent();
    LawModel proposalFromRequest = pollFromRequest.getProposals().iterator().next();             // This proposal is a "detached entity". Cannot simply be saved.
    //jpaContext.getEntityManagerByManagedType(PollModel.class).merge(proposal);      // DOES NOT WORK IN SPRING.  Must handle transaction via a seperate PollService class and @Transactional annotation there.
    PollModel createdPoll = pollService.createPoll(pollFromRequest.getTitle(), proposalFromRequest);

    PersistentEntityResource persistentEntityResource = resourceAssembler.toFullResource(createdPoll);

    log.trace("<= POST /createNewPoll: created Poll "+persistentEntityResource.getLink("self").getHref());

    /*
    Map<String, String> result = new HashMap<>();
    result.putArray("msg", "Created poll successfully");
    result.putArray("", resourceAssembler.getSelfLinkFor(savedPoll).getHref());
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
	}

	/**
	 * Verify that a ballot has been casted and counted correctly in a poll.
	 * @param poll the poll
	 * @param checksum the ballot's checksum that was returned to the voter when casting his ballot
	 * @return true when ballot could be verified
	 */
	@RequestMapping(value = "/polls/{pollId}/verifyChecksum")
	@ResponseBody
	public Lson verifyChecksum(
		@PathVariable("pollId") PollModel poll,
		@RequestParam("checksum") String checksum
	) {
  	boolean valid = pollService.verifyBallot(poll, checksum);
		Link pollLink = entityLinks.linkToSingleResource(PollModel.class, poll.getId());
  	return new Lson()
			.put("checksum", checksum)
			.put("poll", pollLink.getHref())
			.put("valid", valid);
	}

	/**
	 * Get voter's own ballot in a poll. With {@link BallotModel#level} shows
	 * if this ballot was casted by the voter himself, or by a proxy n levels above him.
	 *
	 * @param poll a poll in status VOTING or FINISHED
	 * @param voterToken the users own voterToken
	 * @return voter's own ballot (200)  or HTTP 204 NO_CONTENT if user has no ballot yet
	 */
	@RequestMapping(value = "/polls/{pollId}/myballot")
	@ResponseBody
	public BallotModel getOwnBallot(
			@PathVariable(name="pollId") PollModel poll,			// To use pollId as @PathVariable this needs its own conversionService in LiquidoRepositoryRestConfigurer.java !
			@RequestParam("voterToken") String voterToken
	) throws LiquidoException {
		BallotModel ownBallot = pollService.getBallotForVoterToken(poll, voterToken)
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.NO_BALLOT, "No ballot found. You did not vote in this poll yet."));   // this is not an error. HttpStatus.NO_CONTENT
		//TODO: Having no ballot is quite normal. Do not throw an exception. Instead return HTTP 204 with empty body    => Ok, now needs a test.

		return ownBallot;  // includes some tweaking of JSON serialization  in

		/*
		// This was a try to build our response JSON here. But has now been moved to BallotModelPollJsonSerializer.java
		List<PersistentEntityResource> voteOrder = ownBallot.getVoteOrder().stream().map(proposal -> resourceAssembler.toFullResource(proposal)).collect(Collectors.toList());
		Link areaLink = entityLinks.linkToSingleResource(poll.getArea());
		Link pollLink = entityLinks.linkToSingleResource(poll);
		BallotProjection ballotProjection = projectionFactory.createProjection(BallotProjection.class, ownBallot);
		return Lson.builder()
				.put("checksum", ownBallot.getChecksum().getChecksum())
				.put("level", ownBallot.getLevel())
				.put("voteCount", ownBallot.getVoteCount())
				.put("voteOrder", voteOrder)   // inline vote order with full HATEOAS JSON resources, because client needs it
				.put("_links.area.href", areaLink.getHref())
				.put("_links.area.templated", areaLink.isTemplated())
				.put("_links.poll.href", pollLink.getHref())
				.put("_links.poll.templated", pollLink.isTemplated());
		*/
	}

	/**
	 * Find polls by status and area
	 * @param status {@link PollModel.PollStatus}
	 * @param area URI of an AreaModel
	 * @return HATEOAS JSON with _embedded.polls[]
	 * @throws LiquidoException when status String does not match any PollStatus
	 *
	 * Example JSON response
	 * <pre>
	 * {
	 *   "_embedded" : {
	 *     "polls" : [ {
	 *       "id" : 239,
	 *       "createdAt" : "2019-07-17T05:38:45.141+0000",
	 *       "updatedAt" : "2019-07-17T05:38:45.141+0000",
	 *       "title" : null,
	 *       "status" : "ELABORATION",
	 *       "votingStartAt" : "2019-08-07T00:00:00",
	 *       "votingEndAt" : "2019-08-21T00:00:00",
	 *       "duelMatrix" : {
	 *         "rows" : 0,
	 *         "cols" : 0,
	 *         "rawData" : [ ]
	 *       },
	 *       "area" : {
	 *         "id" : 21,
	 *         "createdAt" : "2019-07-24T05:38:29.802+0000",
	 *         "updatedAt" : "2019-07-24T05:38:29.802+0000",
	 *         "title" : "Area 0",
	 *         "description" : "Nice description for test area #0"
	 *       },
	 *       "numCompetingProposals" : 5
	 *     } ]
	 *   },
	 *   "_links" : {
	 *     "self" : {
	 *       "href" : "http://localhost:57253/polls/search/findByStatus?status={status}",
	 *       "templated" : true
	 *     }
	 *   }
	 * }
	 * </pre>
   */
	@RequestMapping("/polls/search/findByStatusAndArea")
	public @ResponseBody Lson findPollsByStatusAndArea(@RequestParam("status") String status, @RequestParam("area")AreaModel area) throws LiquidoException {
		PollModel.PollStatus pollStatus = null;
		try {
			 pollStatus = PollModel.PollStatus.valueOf(status);
		} catch (IllegalArgumentException e) {
			throw new LiquidoException(LiquidoException.Errors.INVALID_POLL_STATUS, "Unknown status for poll '"+status+"'. Must be one of ELABORATION, VOTING or FINISHED");
		}
		List<PollModel> polls = pollRepo.findByStatus(pollStatus);
		List<PollModel> pollsInArea = polls.stream().filter(poll -> poll.getArea().equals(area)).collect(Collectors.toList());

		// Implementation note: PollRepo is deliberately NOT exposed as REST resource. Polls MUST be handled through this custom PollRestController.
		// CODE: return new Resources<>(pollsInArea, linkTo(methodOn(PollRestController.class).findPollsByStatusAndArea(null, null)).withRel("self"));
		// BUG: returning resource does not add _embedded.polls: [] when List is empty. https://stackoverflow.com/questions/30286795/how-to-force-spring-hateoas-resources-to-render-an-empty-embedded-array/30297552
		// FIX: Doogies LSON Builder for the win once again!! :-)
		// LEARNING:  Always fine tune the returned JSON of your API !YOURSELF!  Do NOT rely on auto generated Repos.
		ControllerLinkBuilder controllerLinkBuilder = linkTo(methodOn(PollRestController.class).findPollsByStatusAndArea(null, null));
		return new Lson()
				.put("_embedded.polls", pollsInArea)
				.put("_links.self.href", controllerLinkBuilder.toUri());
	}


	//TODO: Move getRecentlyDiscussed() method to LawRestController (There was something about URL pathes????)
	/**
	 * Get recently discussed proposals.
	 *
	 * @see LawRepo#getRecentlyDiscussed(Date)
	 * @return a sorted list of (max 10) proposals with recent comments.
	 *   The list will contain LawProjections as Spring HATEOAS JSON
	 */
	@RequestMapping("/laws/search/recentlyDiscussed")
	public @ResponseBody Resources<LawProjection> getRecentlyDiscussedProposals() {
		List<LawModel> mostDiscussedProposals = lawService.getRecentlyDiscussed(10);
		// Some more Spring HATEOAS magic:
		// https://stackoverflow.com/questions/28139856/how-can-i-get-spring-mvchateoas-to-encode-a-list-of-resources-into-hal
		List<LawProjection> projected = mostDiscussedProposals.stream().map(l -> factory.createProjection(LawProjection.class, l)).collect(Collectors.toList());
		return new Resources<>(projected, linkTo(methodOn(PollRestController.class).getRecentlyDiscussedProposals()).withRel("self"));
	}

}
