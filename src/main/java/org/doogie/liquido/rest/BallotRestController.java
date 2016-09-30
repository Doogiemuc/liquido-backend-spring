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
 * Controller for our RESTfull endpoint:
 *
 * Resouces:
 *   POST /ballot  -  post a users vote
 */
@RestController
public class BallotRestController {
  Logger log = LoggerFactory.getLogger(BallotRestController.class);  // Simple Logging Facade 4 Java

  @Autowired
  BallotRepo ballotRepo;

  /**
   * is alive test
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
  @RequestMapping(value = "/ballot", method = POST)
  public BallotModel postBallot(@Valid @RequestBody BallotModel newBallot, BindingResult bindingResult) throws BindException {
    log.trace("=> POST /ballot "+newBallot);
    if (bindingResult.hasErrors()) {
      log.trace("   ballot is invalid: "+bindingResult.getAllErrors());
      throw new BindException(bindingResult);  // this generates a cool error message. Undocumented spring feature :-)
    }
    BallotModel createdBallot = ballotRepo.save(newBallot);
    log.trace("<= POST /ballot returned: "+createdBallot);
    return createdBallot;
  }
}

