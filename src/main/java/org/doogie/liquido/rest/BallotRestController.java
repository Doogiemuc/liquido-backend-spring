package org.doogie.liquido.rest;

import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.websocket.server.PathParam;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
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
  UserRepo userRepo;

  @Autowired
  AreaRepo areaRepo;

  @Autowired
  BallotRepo ballotRepo;

  @Autowired
  DelegationRepo delegationRepo;


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

  /**
   * calculate the number of votes a user may cast (including his own one) because of (transitive) delegation
   * of votes to this proxy.
   * @param userId ID of proxy (as 24 char HEX string)
   * @param areaId ID of area
   * @param req HttpServletRequest automatically injected by Spring
   * @return the number of votes this user may cast, including his own one!
   */
  @RequestMapping(value = "/users/{userId}/getNumVotes", method = GET)
  public int getNumVotes(@PathVariable String userId, @PathParam("areaId") String areaId, HttpServletRequest req) throws Exception {
    log.trace("=> GET numVotes "+req.getRequestURI());
    //check that userId and areaId are correct and exist
    UserModel user = userRepo.findOne(userId);
    AreaModel area = areaRepo.findOne(areaId);
    if (user == null) throw new Exception("User with id="+userId+" does not exist.");
    if (area == null) throw new Exception("Area with id="+areaId+" does not exist.");
    //TODO: get number of votes from cache if possible
    //calculate number of votes from "delegations" in DB
    int numVotes = delegationRepo.getNumVotes(userId, areaId);
    log.trace("<= GET numVotes = "+numVotes);
    return numVotes;
  }
}

