package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.LawProjection;
import org.doogie.liquido.rest.dto.LawQuery;
import org.doogie.liquido.services.LawService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@RepositoryRestController  // with all spring magic
@RequestMapping("${spring.data.rest.base-path}")
public class LawSearchController {

	@Autowired
	LawService lawService;


	@Autowired
	ProjectionFactory factory;

	@Autowired
	PagedResourcesAssembler<LawProjection> resourcesAssembler;

	/**
	 * Search for ideas, proposals or laws with advanced search criteria
	 * @param lawQuery search criteria
	 * @return list of matching LawModels as HATEOAS Resource (JSON)
	 */
	@RequestMapping(value = "/laws/search/findByQuery", method = RequestMethod.POST)
	public
	  @ResponseBody
	PagedResources<Resource<LawProjection>>    // MUST have @ResponseBody annotation!
	  searchForLaw(@RequestBody LawQuery lawQuery)
	{
		log.trace("/laws/search/findByQuery : "+lawQuery);
		Page<LawModel> resultPage = lawService.findBySearchQuery(lawQuery);
    log.trace("findByQuery: got "+resultPage.getTotalElements()+" LawModels");

		// create an HATEOAS compliant JSON response with _projected_ LawModels
		Page<LawProjection> projectedPage = resultPage.map(l -> factory.createProjection(LawProjection.class, l));
		return resourcesAssembler.toResource(projectedPage);
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