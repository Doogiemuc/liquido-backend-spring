package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.rest.dto.JoinPollRequest;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.PollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

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
  LawRepo lawRepo;

  @Autowired
  PollRepo pollRepo;

  @Autowired
  LiquidoRestUtils restUtils;

  //see https://docs.spring.io/spring-data/rest/docs/current/reference/html/#customizing-sdr.overriding-sdr-response-handlers

  /**
   * When an idea reaches its quorum then it becomes a law and its creator <i>can</i> create a new poll for this proposal.
   * Other proposals need to join this poll before voting can be started.
   * @param pollResource the new poll with the link to (at least) one proposal, e.g.
   *          <pre>{ "proposals": [ "/liquido/v2/laws/152" ] }</pre>
   * @param resourceAssembler spring's PersistentEntityResourceAssembler that can create the reply
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
  @RequestMapping(value = "/joinPoll",
      method = RequestMethod.POST,
      consumes = "application/json")
  public @ResponseBody Resource joinPoll(@RequestBody JoinPollRequest joinPollRequest, PersistentEntityResourceAssembler resourceAssembler) throws LiquidoException {
		log.info("Proposal joins poll: "+joinPollRequest);

		// Now we would need to map the Spring Data Rest HATEOAS Uri to the actual entities.  But the clean solution is a bigger effort
		// https://stackoverflow.com/questions/37186417/resolving-entity-uri-in-custom-controller-spring-hateoas
		// https://stackoverflow.com/questions/49458567/mapping-hal-uri-in-requestbody-to-a-spring-data-rest-managed-entity

		Long pollId = restUtils.getIdFromURI("polls", joinPollRequest.poll);
		PollModel poll = pollRepo.findOne(pollId);

		Long proposalId = restUtils.getIdFromURI("laws", joinPollRequest.proposal);
		LawModel proposal = lawRepo.findOne(proposalId);

		PollModel updatedPoll = pollService.addProposalToPoll(proposal, poll);

		return resourceAssembler.toResource(updatedPoll);
	}

}
