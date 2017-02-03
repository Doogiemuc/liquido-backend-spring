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
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.websocket.server.PathParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Controller for our RESTfull endpoint for user management.
 *
 * Available endpoints:   This is the same endpoint as published by the SprintRestController for the users repository. We extend it with some additional endpoints:
 *
 *  BASE_URL     /liquido/v2/users
 *  RESOURCE       /{userId}
 *  GET              /getNumVotes   - return the number of votes that this user may cast
 *  PUT              /delegations   - save a new proxy (upsert: updated existing delegation or insert a new one if that user has no delegation in that area yet.)
 *
 */
@RestController
@RequestMapping("/liquido/v2/users")
public class UserRestController {
  Logger log = LoggerFactory.getLogger(this.getClass());  // Simple Logging Facade 4 Java

  @Autowired
  UserRepo userRepo;

  @Autowired
  AreaRepo areaRepo;

  @Autowired
  DelegationRepo delegationRepo;  // high level abstraction of repositories in (any) DB.

  /**
   * calculate the number of votes a user may cast (including his own one) because of (transitive) delegation
   * of votes to this proxy.
   *
   * This can also directly be accessed under the delegation
   *
   * @param userId ID of proxy (as 24 char HEX string)
   * @param areaId URL query param: ID of area
   * @param req HttpServletRequest automatically injected by Spring
   * @return the number of votes this user may cast, including his own one!
   */
  @RequestMapping(value = "/{userId}/getNumVotes", method = GET)
  public long getNumVotes(@PathVariable Long userId, @PathParam("areaId") Long areaId, HttpServletRequest req) throws Exception {
    log.trace("=> GET numVotes(userId=" + userId + ", areaId=" + areaId + ")");
    //check that userId and areaId are correct and exist
    UserModel user = userRepo.findOne(userId);
    AreaModel area = areaRepo.findOne(areaId);
    if (user == null) throw new Exception("User with id="+userId+" does not exist.");
    if (area == null) throw new Exception("Area with id="+areaId+" does not exist.");
    //TODO: get number of votes from cache if possible
    //calculate number of votes from "delegations" in DB
    long numVotes = delegationRepo.getNumVotes(area, user);
    log.trace("<= GET numVotes(userId=" + userId + ", areaId=" + areaId + ") = "+numVotes);
    return numVotes;
  }


  /**
   * get all proxies that this user currently has assigned (per area)
   * @param userId ID of an existing user
   * @return a map  area.title => proxyUser.id   with one entry for each proxy of that user in that area
   * @throws Exception when user does not exist
   */
  @RequestMapping(value = "/{userId}/getProxyMap", method = GET)
  public Map getProxyMap(@PathVariable Long userId) throws Exception {
    UserModel user = userRepo.findOne(userId);
    if (user == null) throw new Exception("User with id="+userId+" does not exist.");
    List<DelegationModel> proxies = delegationRepo.findByFromUser(user);
    Map<String, Long> proxyMap = new HashMap<>();
    for (DelegationModel delegation : proxies) {
      proxyMap.put(delegation.getArea().getTitle(), delegation.getToProxy().getId());
    }
    return proxyMap;
  }




  /**
   * Save a proxy. This will insert a new delegation or update the existing one
   * @param userId
   * @param newDelegation
   * @param bindingResult
   * @param req
   * @return
   * @throws Exception
   */
/*
  @RequestMapping(value = "/{userId}/delegations", method = PUT)
  public String saveProxy(@PathVariable Long userId, @Valid @RequestBody DelegationModel newDelegation, BindingResult bindingResult, HttpServletRequest req) throws Exception {
    log.trace("=> PUT saveProxy: newDelegation="+newDelegation);
    // validation did happen in DelegationValidator.java  This includes checking foreign keys!
    if (bindingResult.hasErrors()) {
      log.trace("   newDelegation is invalid: " + bindingResult.getAllErrors());
      throw new BindException(bindingResult);  // this generates a cool error message. Undocumented spring feature :-)
    }

    //Do not create the delegation twice if it already exists. (There is also a combined unique constraint on  (area, fromUser, toProxy)
    DelegationModel existingDelegation = delegationRepo.findOne(Example.of(newDelegation));
    if (existingDelegation != null) {
      log.trace("   update existing delegation with id="+existingDelegation.getId());
      newDelegation.setId(existingDelegation.getId());
      delegationRepo.save(newDelegation);  //TODO: This is actually not necessary. A delegatino consinsts only of foreign keys. Nothing to update. (except maybe timestamp)
    } else {
      log.trace("   insert new delegation");
      delegationRepo.insert(newDelegation);
    }
    //MAYBE: Screw all this Spring Repository stuff and code by hand with full pure mongo power  mongoTemplate.upsert(query, update, DelegationModel.class);

    log.trace("<= PUT saveProxy successful.");
    return "Proxy saved successfully";
  }
*/
}



