package org.doogie.liquido.rest;

import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.websocket.server.PathParam;

import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Controller for our RESTfull endpoint:
 *
 * Resouces:
 *   POST /ballot  -  post a users vote
 */
@RestController
public class UserRestController {
  Logger log = LoggerFactory.getLogger(this.getClass());  // Simple Logging Facade 4 Java

  @Autowired
  UserRepo userRepo;

  @Autowired
  AreaRepo areaRepo;

  @Autowired
  DelegationRepo delegationRepo;

  /** will validate Delegations for the existence of the references foreign keys */
  @InitBinder
  protected void initBinder(WebDataBinder binder) {
    binder.addValidators(new DelegationValidator());
  }


  /**
   * calculate the number of votes a user may cast (including his own one) because of (transitive) delegation
   * of votes to this proxy.
   * @param userId ID of proxy (as 24 char HEX string)
   * @param areaId URL query param: ID of area
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

  @RequestMapping(value = "/users/{userId}/delegations", method = PUT)
  public String saveProxy(@PathVariable String userId, @Valid @RequestBody DelegationModel newDelegation, BindingResult bindingResult, HttpServletRequest req) throws Exception {
    log.trace("=> PUT saveProxy: newDelegation="+newDelegation);
    if (bindingResult.hasErrors()) {
      log.trace("   newDelegation is invalid: " + bindingResult.getAllErrors());
      throw new BindException(bindingResult);  // this generates a cool error message. Undocumented spring feature :-)
    }

    //TODO: should already be validated in DelegationValidator  (do I need to call validate manually?)
    String areaId = newDelegation.getArea().toHexString();
    String toProxyId = newDelegation.getToProxy().toHexString();
    UserModel fromUser = userRepo.findOne(userId);
    UserModel toProxy  = userRepo.findOne(toProxyId);
    AreaModel area     = areaRepo.findOne(areaId);
    if (fromUser == null) throw new Exception("User with id="+userId+" does not exist.");
    if (toProxy  == null) throw new Exception("User with id="+toProxyId+" does not exist.");
    if (area     == null) throw new Exception("Area with id="+areaId+" does not exist.");

    delegationRepo.save(newDelegation);

    log.trace("<= PUT saveProxy successful.");
    return "Proxy saved successfully";
  }

}



