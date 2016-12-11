package org.doogie.liquido.rest;

import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
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
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Controller for our RESTfull endpoint for user management.
 *
 * REST Resouces:
 *  BASE     /users/{userId}
 *  GET        /getNumVotes   - return the number of votes that this user may cast
 *  PUT        /delegations   - save a new proxy (upsert: updated existing delegation or insert a new one if that user has no delegation in that area yet.)
 */
@RestController
public class UserRestController {
  Logger log = LoggerFactory.getLogger(this.getClass());  // Simple Logging Facade 4 Java

  @Autowired
  UserRepo userRepo;

  @Autowired
  AreaRepo areaRepo;

  @Autowired
  DelegationRepo delegationRepo;  // high level abstraction of repositories in (any) DB.

//  @Autowired
//  MongoTemplate mongoTemplate;    // low level MongoOperations

  /** will validate Delegations for the existence of the references foreign keys */
  /*  BUGFIX:  DelegationValidator cannot Autowire  repos  :-(
  @InitBinder
  protected void initBinder(WebDataBinder binder) {
    binder.addValidators(new DelegationValidator());
  }
  */

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
    // validation did happen in DelegationValidator class
    if (bindingResult.hasErrors()) {
      log.trace("   newDelegation is invalid: " + bindingResult.getAllErrors());
      throw new BindException(bindingResult);  // this generates a cool error message. Undocumented spring feature :-)
    }

    //Do not create the delegation twice if it already exists. (There is also a combined unique contraint in MongoDB on  (area, fromUser, toProxy)
    DelegationModel existingDelegation = delegationRepo.findOne(Example.of(newDelegation));
    if (existingDelegation != null) {
      log.trace("   update existing delegation with id="+existingDelegation.getId());
      newDelegation.setId(existingDelegation.getId());
      delegationRepo.save(newDelegation);
    } else {
      log.trace("   insert new delegation");
      delegationRepo.insert(newDelegation);
    }
    //MAYBE: Screw all this Spring Repository stuff and code by hand with full pure mongo power  mongoTemplate.upsert(query, update, DelegationModel.class);

    log.trace("<= PUT saveProxy successful.");
    return "Proxy saved successfully";
  }

}



