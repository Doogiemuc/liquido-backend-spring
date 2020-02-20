package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.OffsetLimitPageable;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.LawProjection;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.LawQuery;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.LawService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@RepositoryRestController  // with all spring magic
@RequestMapping("${spring.data.rest.base-path}")
public class LawRestController {

	@Autowired
	LawService lawService;

	@Autowired
	LiquidoAuditorAware liquidoAuditorAware;

	@Autowired
	ProjectionFactory factory;

	/**
	 * Add current user as a supporter for the given idea, proposal or law.
	 * Will check quorum if idea can become a proposal.
	 * @param idea id of an idea, proposal or law
	 * @return 200
	 * @throws LiquidoException When current user is the creator of that idea
	 */
	@RequestMapping(value = "/laws/{ideaId}/like", method = RequestMethod.POST)
	public @ResponseBody ResponseEntity<?> addSupporter(@PathVariable(name="ideaId") LawModel idea) throws LiquidoException {
		UserModel currentUser = liquidoAuditorAware.getCurrentAuditor().orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "MUST be logged in to add a supporter"));
		lawService.addSupporter(currentUser, idea);
		return ResponseEntity.ok("Thank you for supporting this idea.");
	}

	@Autowired
	LawRepo lawRepo;

	@Autowired
	AreaRepo areaRepo;

	@Autowired
	DelegationRepo delegationRepo;

	/*  DOES NOT WORK

	  HEre I tried to return the list of LawModels "supportedByYou" as HATEOAS Resources.   But did not yet manage to do so.
	  Error: Infinite recursion from Jackson => which field???

	@RequestMapping(path = "/my/newsfeed2", produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Lson getMyNewsfeed2(PersistentEntityResourceAssembler assembler) throws LiquidoException {
		UserModel currentUser = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Must be logged in to get newsfeed!"));

		LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);

		// users ideas that recently reached their quorum and became a proposal
		List<LawModel> reachedQuorum = lawRepo.findByReachedQuorumAtGreaterThanEqualAndCreatedBy(twoWeeksAgo, currentUser);

		// own proposals that are in a poll which is in voting phase
		List<LawModel> ownProposalsInVoting = lawRepo.findDistinctByStatusAndCreatedBy(LawModel.LawStatus.VOTING, currentUser);
		List<LawProjection> ownPropsInVotingProjected = ownProposalsInVoting.stream().map(p -> factory.createProjection(LawProjection.class, p)).collect(Collectors.toList());

		// ideas and proposals that are supported by current user
		List<LawModel> supportedByYou = new ArrayList<>();
		supportedByYou.addAll(lawRepo.findDistinctByStatusAndSupportersContains(LawModel.LawStatus.IDEA, currentUser));
		supportedByYou.addAll(lawRepo.findDistinctByStatusAndSupportersContains(LawModel.LawStatus.PROPOSAL, currentUser));
		supportedByYou.addAll(lawRepo.findDistinctByStatusAndSupportersContains(LawModel.LawStatus.ELABORATION, currentUser));
		supportedByYou.addAll(lawRepo.findDistinctByStatusAndSupportersContains(LawModel.LawStatus.VOTING, currentUser));
		List<PersistentEntityResource> resourceList = supportedByYou.stream()
				.sorted((p1, p2) -> (int) (p1.getUpdatedAt().getTime() - p2.getUpdatedAt().getTime()))
				.limit(10)
				.map(p -> assembler.toFullResource(p))
				.collect(Collectors.toList());

		Resources<PersistentEntityResource> ddd = new Resources<>(resourceList, linkTo(methodOn(LawRestController.class).getMyNewsfeed2(null)).withRel("self"));

		// Own proposals that have recent comments
		List<LawModel> recentlyDiscussed = lawRepo.getRecentlyDiscussed(java.sql.Timestamp.valueOf(twoWeeksAgo), currentUser);

		// Delegation requests in all areas
		List<DelegationModel> delegationRequests = new ArrayList<>();
		for (AreaModel area: areaRepo.findAll()) {
			delegationRequests.addAll(delegationRepo.findDelegationRequests(area, currentUser));
		}

		return new Lson()
				// users own stuff
				.put("delegationRequests", delegationRequests)
				.put("reachedQuorum", reachedQuorum)
				.put("supportedByYouResources", ddd)
				.put("ownProposalsInVoting", ownPropsInVotingProjected)   // return LawProjection with info about poll
				.put("recentlyDiscussedProposals", recentlyDiscussed)
				// recently won polls :-)
				//stuff by others


				;
	}
  */

	/**
	 * Search for ideas, proposals or laws with advanced search criteria
	 * @param lawQuery search criteria
	 * @return list of matching LawModels as HATEOAS Resource (JSON)
	 */
	@RequestMapping(value = "/laws/search/findByQuery", method = RequestMethod.POST)
	public @ResponseBody Lson searchForLaw(@RequestBody LawQuery lawQuery)
	{
		if (log.isTraceEnabled()) log.trace("/laws/search/findByQuery : "+lawQuery);
		Page<LawModel> resultPage = lawService.findBySearchQuery(lawQuery);
		if (log.isTraceEnabled()) log.trace("findByQuery: got "+resultPage.getTotalElements()+" LawModels");

		//Scrap all this STUPID spring HATEOAS stuff FOREVER and just simply build JSON by hand and return that!!!
		long offset = ((OffsetLimitPageable)resultPage.getPageable()).getOffset();
		long limit  = ((OffsetLimitPageable)resultPage.getPageable()).getLimit();
		Lson result = Lson.builder()
			.put("_embedded", Lson.builder("laws", resultPage))
			.put("_link.self.href", buildLink(offset, limit, IanaLinkRelations.SELF))
			.put("_link.self.first", buildLink(0, limit, IanaLinkRelations.FIRST))
			.put("query", lawQuery)
			.put("totalElements", resultPage.getTotalElements());

		return result;
	}

	/** Build an HATEOAS Link with rel */
	private Link buildLink(long offset, long limit, LinkRelation rel) {
		String URI = ServletUriComponentsBuilder.fromCurrentRequest()
			.queryParam("offset", offset)
			.queryParam("limit", limit)
			.build().toUriString();
		return new Link(URI, rel);
	}







}


/*
 Example Request

 curl -X POST \
  http://localhost:8080/liquido/v2/laws/search/findByQuery \
  -H 'Authorization: Bearer eyJhbGciOi.......' \
  -H 'Content-Type: application/json' \
  -d '{
	  "status": "IDEA",
	  "searchText": "Idea 2"
  }'


 */