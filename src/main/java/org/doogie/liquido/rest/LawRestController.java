package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.LawProjection;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.LawQuery;
import org.doogie.liquido.rest.dto.LawQueryResult;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.LawService;
import org.doogie.liquido.services.LiquidoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.Link;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Set;

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

	/**
	 * Search for ideas, proposals or laws with advanced search criteria
	 * @param lawQuery search criteria
	 * @return list of matching LawModels as HATEOAS Resource (JSON)
	 */
	@RequestMapping(value = "/laws/search/findByQuery", method = RequestMethod.POST)
	 public @ResponseBody LawQueryResult<LawProjection>                              // MUST have @ResponseBody annotation!
	  searchForLaw(@RequestBody LawQuery lawQuery)
	{
		if (log.isTraceEnabled()) log.trace("/laws/search/findByQuery : "+lawQuery);
		Page<LawModel> resultPage = lawService.findBySearchQuery(lawQuery);
		if (log.isTraceEnabled()) log.trace("findByQuery: got "+resultPage.getTotalElements()+" LawModels");

		// Create an HATEOAS compliant JSON response with _projected_ LawModels and self, next and prev Links.
		// No paging here!. Instead we use offset and limit. Therefore we cannot simply use PagedResourceAssembler because it serializes to PagedResources class, which uses PageMetadata internally.
		Page<LawProjection> projectedPage = resultPage.map(l -> factory.createProjection(LawProjection.class, l));
		LawQueryResult<LawProjection> result = new LawQueryResult<>(projectedPage.getContent(), lawQuery, resultPage.getTotalElements());

		long offset = lawQuery.getOffset();
		long limit  = lawQuery.getLimit();
		long total  = resultPage.getTotalElements();

		result.add(buildLink(offset, limit, Link.REL_SELF));
		result.add(buildLink(0, limit, Link.REL_FIRST));
		if (offset + limit < total) {
			result.add(buildLink(offset+limit, limit, Link.REL_NEXT));
		}
		if (offset - limit >= 0) {
			result.add(buildLink(lawQuery.getOffset() - lawQuery.getLimit(), lawQuery.getLimit(), Link.REL_PREVIOUS));
		}

		//TODO:  RAGE: Scrap all this stupid spring HATEOAS stuff and just simply build JSON by hand and return that!!!
		/*
		Lson.builder()
			.put("_embedded", Lson.builder("laws", projectedPage.getContent()))
			.put("_link.self.href", buildLink(offset, limit, Link.REL_SELF) )
			.put("_link.self.first", buildLink(0, limit, Link.REL_FIRST) )
			.put("query", lawQuery)
			.put("totalElements", resultPage.getTotalElements());
    */

		return result;
	}

	/** Build an HATEOAS Link with rel */
	private Link buildLink(long offset, long limit, String rel) {
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