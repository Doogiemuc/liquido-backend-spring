package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.datarepos.RightToVoteRepo;
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
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;


/**
 * REST controller for working with Polls.
 */
@Slf4j
//@RestController
//@RequestMapping("${spring.data.rest.base-path}")
//@RepositoryRestController
@BasePathAwareController
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
	LiquidoRestUtils restUtils;

  //see https://docs.spring.io/spring-data/rest/docs/current/reference/html/#customizing-sdr.overriding-sdr-response-handlers
  /*
   * When an idea reaches its quorum then it becomes a proposal and its creator <i>can</i> builder a new poll for this proposal.
   * Other proposals need to join this poll before voting can be started.
   * @param newPoll the poll to create with the link to exactly one proposal, e.g.
   *          <pre>{ title: "Poll Title", "proposals": [ "/liquido/v2/laws/152" ] }</pre>
   * @return the saved poll as HATEOAS resource with all _links
   * @throws LiquidoException when sent LawModel is not in state PROPOSAL or creation of new poll failed
   */
	@RequestMapping(
		value = "/polls",
		method = RequestMethod.POST,
		consumes = MediaType.APPLICATION_JSON_VALUE
	)
	public @ResponseBody ResponseEntity<PollModel> createNewPoll(@RequestBody EntityModel<PollModel> newPollEntity) throws LiquidoException {
		//BUGFIX: for no-string argument constructor: The old HATEOAS class "Resource" now is EntityModel.   Cannot just simply deserialize from a PollModel. There is a problem with creating the poll.proposals[] -> LawModel.  I really tried hard. But no it works like this.
		PollModel newPoll = newPollEntity.getContent();
		if (newPoll.getProposals().size() != 1)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_POLL, "Must pass exactly one proposal to create a new poll!");
		LawModel proposalFromRequest = newPoll.getProposals().iterator().next();             // This proposal is a "detached entity". Cannot simply be saved here. This will all be done inside pollService.
		PollModel createdPoll = pollService.createPollWithProposal(newPoll.getTitle(), proposalFromRequest);
		log.info("Created new poll "+createdPoll);
		return new ResponseEntity(createdPoll, HttpStatus.CREATED);
	}



	/**
	 * Join a proposal into an existing poll (that must be in its ELABORATION phase)
	 * POST request body must contain <b>one</b> URI of a proposal
	 * @param poll an existing poll which must be in status == elaboration
	 * @body URI of proposal in elaboration that wants to join
	 * @throws LiquidoException if poll is not in its ELABORATION phase or proposal did not reach its quorum yet
	 */
  @RequestMapping(
  		value = "/polls/{pollId}/join",						// (A) Client should not now know entity IDs <=> (B) I like the structure of API pathes like  /polls/{id}
			method = RequestMethod.POST,							// This is a POST request, becasue POST is _not_ idempotent. Can join a poll only once!
			consumes = MediaType.APPLICATION_JSON_VALUE
	)
  public @ResponseBody PollModel joinPoll(
			@PathVariable(name="pollId") PollModel poll,
			@RequestBody JoinPollRequest joinPollRequest
			) throws LiquidoException
	{
		log.info("Proposal wants to join poll: "+joinPollRequest.getProposal()+" -> "+poll);

  	/*
  	//https://stackoverflow.com/questions/49458567/mapping-hal-uri-in-requestbody-to-a-spring-data-rest-managed-entity
  	//https://stackoverflow.com/questions/37186417/resolving-entity-uri-in-custom-controller-spring-hateoas

  	I tried so that client can simply pass the proposal's URI as the body of the request
  	But having text/uri-list as body of a POST request is not easily possible with spring-data-rest and HATEOAS


		Long lawId = -1L;
		try {
			lawId = LiquidoRestUtils.getIdFromURI("law", proposalURI);
		} catch (IllegalArgumentException e) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_JOIN_POLL, "Cannot join poll. Invalid URI of proposal: "+proposalURI);
		}
		Optional<LawModel> proposal = lawRepo.findById(lawId);

		if (proposal.isPresent()) throw new LiquidoException(LiquidoException.Errors.CANNOT_JOIN_POLL, "Cannot join poll. Cannot find proposal: "+proposalURI);

  	 */

  	// sanity checks are all inside the service method:
		PollModel updatedPoll = pollService.addProposalToPoll(joinPollRequest.getProposal(), poll);
		log.info("Proposal(id="+joinPollRequest.getProposal().getId()+") joined poll "+updatedPoll);
		return updatedPoll;
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
	 * @return the verified ballot
	 * @throws LiquidoException with HttpStatus 404 when checksum could not be verified because no ballot for that checksum could be found.
	 */
	@RequestMapping(value = "/polls/{pollId}/verify")
	@ResponseBody
	public BallotModel verifyChecksum(
		@PathVariable("pollId") PollModel poll,
		@RequestParam("checksum") String checksum
	) throws LiquidoException {
  	Optional<BallotModel> ballot = pollService.getBallotForChecksum(poll, checksum);
  	if (!ballot.isPresent()) throw new LiquidoException(LiquidoException.Errors.CANNOT_VERIFY_CHECKSUM, "No ballot for that checksum.");
		//Link pollLink = entityLinks.linkToSingleResource(PollModel.class, poll.getId());
  	return ballot.get();
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
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.NO_BALLOT, "No ballot found. You did not vote in this poll yet."));   // this is not an error. returns HttpStatus.NO_CONTENT

		return ownBallot;
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

	//TODO: deprecate this. Has been superseeded by findPolls below.   Need to adapt client.

	@RequestMapping("/polls/search/findByStatusAndArea")
	public @ResponseBody Lson findPollsByStatusAndArea(@RequestParam("status") String status, @RequestParam("area")AreaModel area) throws LiquidoException {
		PollModel.PollStatus pollStatus = null;
		try {
			 pollStatus = PollModel.PollStatus.valueOf(status);
		} catch (IllegalArgumentException e) {
			throw new LiquidoException(LiquidoException.Errors.INVALID_POLL_STATUS, "Unknown status for poll '"+status+"'. Must be one of ELABORATION, VOTING or FINISHED");
		}
		List<PollModel> polls = pollRepo.findByStatusAndArea(pollStatus, area);

		// Implementation note: PollRepo is deliberately NOT exposed as REST resource. Polls MUST be handled through this custom PollRestController.
		// CODE: return new Resources<>(pollsInArea, linkTo(methodOn(PollRestController.class).findPollsByStatusAndArea(null, null)).withRel("self"));
		// BUG: returning resource does not add _embedded.polls: [] when List is empty. https://stackoverflow.com/questions/30286795/how-to-force-spring-hateoas-resources-to-render-an-empty-embedded-array/30297552
		// FIX: Doogies LSON Builder for the win once again!! :-)
		// LEARNING:  Always fine tune the returned JSON of your API !YOURSELF!  Do NOT rely on auto generated Repos.
		WebMvcLinkBuilder webMvcLinkBuilder = linkTo(methodOn(PollRestController.class).findPollsByStatusAndArea(null, null));
		return new Lson()
				.put("_embedded.polls", polls)
				.put("_links.self.href", webMvcLinkBuilder.toUri());
	}

	/**
	 * Flexible search for polls that can search for polls by status and/or area.
	 * You can also search for polls that you have already voted in by passing your voterToken.
	 *
	 * @param status (optionally) only return polls with that status
	 * @param area (optionally) filter by that area
	 * @param voterToken (optionally) only return polls, that the user has a ballot in, casted with that voter token
	 * @return list of polls
	 * @throws LiquidoException when no search criteria is given at all or voterToken is invalid
	 */
	@RequestMapping("/polls/search/find")
	public @ResponseBody Lson findPolls(
			@RequestParam("status") Optional<PollModel.PollStatus> status,
			@RequestParam("area") Optional<AreaModel> area,
			@RequestParam("voterToken") Optional<String> voterToken) throws LiquidoException
	{
		if (!status.isPresent() && !area.isPresent() && !voterToken.isPresent())
			throw new LiquidoException(LiquidoException.Errors.CANNOT_FIND_ENTITY, "You must pass at least one search criteria for a poll");

		List<PollModel> polls = new ArrayList<>();
		// If voterToken is given, then first search polls with ballots created with that voter token.
		// because this will most probably be smalles number of polls.
		// Keep in mind that a voterToken already is per area!
		if (voterToken.isPresent()) {
			RightToVoteModel rightToVote = castVoteService.isVoterTokenValid(voterToken.get());
			if (area.isPresent() && rightToVote.getArea().equals(area.get()))
				throw new LiquidoException(LiquidoException.Errors.CANNOT_FIND_ENTITY, "voterToken's area and passed area do not match!");
			polls = ballotRepo.findByRightToVote(rightToVote).stream()
					.map(ballot -> ballot.getPoll())
					.filter(poll -> !status.isPresent() || poll.getStatus().equals(status.get()))  // if status is given, then filter by status
					.collect(Collectors.toList());
		} else if (status.isPresent() && area.isPresent()) {
			polls = pollRepo.findByStatusAndArea(status.get(), area.get());
		} else if (status.isPresent()) {
			polls = pollRepo.findByStatus(status.get());
		} else if (area.isPresent()) {
			polls = pollRepo.findByArea(area.get());
		}

		// Implementation note: PollRepo is deliberately NOT exposed as REST resource. Polls MUST be handled through this custom PollRestController.
		// CODE: return new Resources<>(pollsInArea, linkTo(methodOn(PollRestController.class).findPollsByStatusAndArea(null, null)).withRel("self"));
		// BUG: returning resource does not add _embedded.polls: [] when List is empty. https://stackoverflow.com/questions/30286795/how-to-force-spring-hateoas-resources-to-render-an-empty-embedded-array/30297552
		// FIX: Doogies LSON Builder for the win once again!! :-)
		// LEARNING:  Always fine tune the returned JSON of your API !YOURSELF!  Do NOT rely on auto generated Repos.
		WebMvcLinkBuilder webMvcLinkBuilder = linkTo(methodOn(PollRestController.class).findPolls(null, null, null));
		return new Lson()
			.put("_embedded.polls", polls)
			.put("_links.self.href", webMvcLinkBuilder.toUri());
	}

}
