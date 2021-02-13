package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.OffsetLimitPageable;
import org.doogie.liquido.jwt.AuthUtil;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
	AuthUtil authUtil;

	@Autowired
	LawRepo lawRepo;

	@Autowired
	AreaRepo areaRepo;

	@Autowired
	DelegationRepo delegationRepo;

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
	@PreAuthorize(AuthUtil.HAS_ROLE_USER)
	public @ResponseBody ResponseEntity<?> addSupporter(@PathVariable(name="ideaId") LawModel idea) throws LiquidoException {
		UserModel currentUser = authUtil.getCurrentUser()
			.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "MUST be logged in to add a supporter"));
		lawService.addSupporter(currentUser, idea);
		return ResponseEntity.ok("Thank you for supporting this idea.");
	}

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

		long offset = ((OffsetLimitPageable)resultPage.getPageable()).getOffset();
		long limit  = ((OffsetLimitPageable)resultPage.getPageable()).getLimit();
		//  Spring-data-rest correctly serializes the stream of LawModels into JSON, with the projected entities as values of the array.
		Lson result = Lson.builder()
			.put("_embedded.laws", resultPage.get())
			.put("_links.self.href", buildLink(offset, limit, IanaLinkRelations.SELF))
			.put("_links.self.first", buildLink(0, limit, IanaLinkRelations.FIRST))
			.put("_page.offset", offset)
			.put("_page.limit", limit)
			.put("_page.totalElements", resultPage.getTotalElements())	// totalElement is the total size of the query result without any limit
			.put("_query", lawQuery);
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