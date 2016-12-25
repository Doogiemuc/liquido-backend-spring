package org.doogie.liquido.rest;

import org.doogie.liquido.datarepos.IdeaRepo;
import org.doogie.liquido.model.IdeaModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Controller for our RESTfull endpoint
 *
 * Resouces:
 *  BASE_URL     /liquido/v2/
 *  GET            /recentIdeasa    returns the ten most recently created ideas
 *
 */
@RestController
@RequestMapping("/liquido/v2/ideas")
public class IdeaController {
  Logger log = LoggerFactory.getLogger(this.getClass());  // Simple Logging Facade 4 Java


  //@Autowired
  //IdeaRepo ideaRepo;

  /**
   * get the ten most recent ideas ordered by date created at.
   * This is already exposed by Spring HATEOAS under /ideas/search/getRecent
   *
   * @return list of IdeaModels

  @RequestMapping(value = "/getRecent", method = GET)
  public List<IdeaModel> getRecentIdeas() {
    log.trace("=> GET /getRecentIdeas ");

    List<IdeaModel> recentIdeas = ideaRepo.findFirst10ByOrderByCreatedAtDesc();

    log.trace("<= GET /getRecentIdeas: "+recentIdeas);
    return recentIdeas;
  }
  */

}



