package org.doogie.liquido.rest;

import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.model.BallotModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Controller for our RESTfull endpoint: /ballot
 *
 * Resouces:
 *   POST /ballot  -  post a users vote
 */
@RestController
@RequestMapping("/liquido/v2")
public class BallotRestController {
  Logger log = LoggerFactory.getLogger(this.getClass());  // Simple Logging Facade 4 Java

  @Autowired
  BallotRepo ballotRepo;

  /**
   * Simple is alive test
   * @return <pre>{"Hello":"World"}</pre>
   */
  @RequestMapping("/ping")
  public String greeting() {
    log.trace("=> GET /ping");
    return "{\"Hello\":\"World\"}";
  }

  /**
   * post a ballot, ie. a vote from a user
   * @param newBallot the users ballot
   * @param bindingResult result of validation
   * @return the stored ballot (incl. its new ID)
   * @throws BindException when validation fails. Exception will contain detailed error information (returned a JSON to the client)
   */
  @RequestMapping(value = "/postBallot", method = POST)   // POST to resource /liquido/v2/ballots
  public BallotModel postBallot(@Valid @RequestBody BallotModel newBallot, BindingResult bindingResult) throws BindException {
    // Look at JSON-Patch. Could that be used here to post the voteOrder?
    // http://stackoverflow.com/questions/25311978/posting-a-onetomany-sub-resource-association-in-spring-data-rest
    // https://github.com/spring-projects/spring-data-rest/commit/ef3720be11f117bb691edbbf63e38ff72e0eb3dd
    // http://stackoverflow.com/questions/34843297/modify-onetomany-entity-in-spring-data-rest-without-its-repository/34864254#34864254

    log.trace("=> POST /ballot "+newBallot);

    if (bindingResult.hasErrors()) {
      log.trace("   ballot is invalid: "+bindingResult.getAllErrors());
      throw new BindException(bindingResult);  // this generates a cool error message. Undocumented spring feature :-)
    }

    BallotModel createdBallot = ballotRepo.save(newBallot);
    log.trace("<= POST /ballot created: "+createdBallot);
    return createdBallot;
  }

}



