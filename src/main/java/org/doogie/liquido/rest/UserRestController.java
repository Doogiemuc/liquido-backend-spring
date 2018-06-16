package org.doogie.liquido.rest;

import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.LiquidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Controller for our RESTfull endpoint for user management.
 *
 * Available endpoints:   This is the same endpoint as published by the SprintRestController for the users repository. We extend it with some additional endpoints:
 *
 *  BASE_URL     /liquido/v2/users
 *  RESOURCE       /{userId}
 *  GET              /getNumVotes   - return the number of votes that this user may cast
 *  GET              /getProxyMap   - return each currently assigned proxy per area of this user
 *  PUT              /saveProxy     - save a new proxy (upsert: updated existing delegation or insert a new one if that user has no delegation in that area yet.)
 *
 */
@BasePathAwareController
//@RestController
//@RepositoryRestController
//@RequestMapping("/users")
public class UserRestController {
  private Logger log = LoggerFactory.getLogger(this.getClass());  // Simple Logging Facade 4 Java

  @Autowired
  UserRepo userRepo;

  @Autowired
  AreaRepo areaRepo;

  @Autowired
  DelegationRepo delegationRepo;

  @Autowired
  LiquidoAuditorAware liquidoAuditorAware;

  /**
   * calculate the number of votes a user may cast (including his own one) because of (transitive) delegation
   * of votes to this proxy.
   *
   * This can also directly be accessed under the delegation
   *
   * @param userId ID of proxy (as 24 char HEX string)
   * @param areaId URL query param: ID of area
   * @return the number of votes this user may cast, including his own one!
   */
  @RequestMapping(value = "/users/{userId}/getNumVotes/{areaId}", method = GET)
  public @ResponseBody long getNumVotes(@PathVariable Long userId, @PathVariable Long areaId) throws Exception {
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
   * @return a map  area.title => proxyUser   with one entry for each proxy of that user in that area
   * @throws LiquidoException when user does not exist
   */
  @RequestMapping(value = "/users/{userId}/getProxyMap", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody Map getProxyMap(@PathVariable Long userId) throws LiquidoException {
    UserModel user = userRepo.findOne(userId);
    if (user == null) throw new LiquidoException(LiquidoException.Errors.USER_DOES_NOT_EXIST, "User with id="+userId+" does not exist.");
    List<DelegationModel> proxies = delegationRepo.findByFromUser(user);
    Map<String, UserModel> proxyMap = new HashMap<>();
    for (DelegationModel delegation : proxies) {
      proxyMap.put(delegation.getArea().getTitle(), delegation.getToProxy());
    }
    return proxyMap;
  }

  /**
   * Save a proxy for the logged in user. This will insert a new delegation or update an existing one in that area.
   * @param delegationResource the new delegation that shall be saved (only 'area' and 'toProxy' need to be filled in the request)
   * @return the created (or updated) delegation as HAL+JSON
   *         If you you need full detailed data of all referenced entities, you can request the delegationProjection.
   *         Or you can send additional requests for example for the "_links.toProxy.href" URI
   */
  @RequestMapping(value = "/saveProxy", method = PUT, consumes="application/json", produces = MediaType.APPLICATION_JSON_VALUE)
  //@ResponseStatus(HttpStatus.OK)
  public
    @ResponseBody PersistentEntityResource   // This returns the DelegationModel as HAL resource
    // HttpEntity<DelegationModel>    This would return the DelegationProjection  (with inlined referenced objects)  but that cannot be used by the client for further updates
    saveProxy(
      @RequestBody Resource<DelegationModel> delegationResource,
      PersistentEntityResourceAssembler resourceAssembler
      // Authentication auth  // not needed anymore - spring-security authentication object could be injected like this
    ) throws LiquidoException {
    log.trace("saveProxy delegation="+delegationResource);
    DelegationModel newDelegation = delegationResource.getContent();

    UserModel currentUser = liquidoAuditorAware.getCurrentAuditor();
    if (currentUser == null) throw new LiquidoException(LiquidoException.Errors.NO_LOGIN, "Cannot save Proxy. Need an authenticated user as fromUser");

    newDelegation.setFromUser(currentUser);     // any user can only add proxies for himself
    UserModel toProxy = newDelegation.getToProxy();
    AreaModel area = newDelegation.getArea();

    // Delegations are important. So we do a lot of sanity checks here.
    if (area == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot save Proxy. Need an area");
    if (toProxy == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot save Proxy: Need a toProxy");
    if (currentUser.getEmail().equals(toProxy.getEmail())) throw new LiquidoException(LiquidoException.Errors.CANNOT_SAVE_PROXY, "Cannot save Proxy. You cannot set yourself as proxy.");

    //TODO: check for circular delegation!

    DelegationModel existingDelegation = delegationRepo.findByAreaAndFromUser(area, currentUser);
    DelegationModel savedDelegation;
    if (existingDelegation != null) {
      existingDelegation.setToProxy(toProxy);
      log.trace("Updating existing delegation to "+existingDelegation);
      savedDelegation = delegationRepo.save(existingDelegation);
    } else {
      log.trace("adding new delegation "+newDelegation);
      savedDelegation = delegationRepo.save(newDelegation);
    }

    //return the complete HATEOAS representation of the saved delegation.
    return resourceAssembler.toResource(savedDelegation);

    // You should not return a DelegationProjection here, as this code would:
    //   return new ResponseEntity<>(savedDelegation, HttpStatus.OK);
    // because that cannot be used for further updates by the client.
    // http://stackoverflow.com/questions/30220333/why-is-an-excerpt-projection-not-applied-automatically-for-a-spring-data-rest-it
  }

  /**
   * delete a proxy of the currently logged in user in one area.
   * @param area the area where to delete the proxy
   * @return HTTP status 204 (NoContent) on success
   */
  @RequestMapping(value = "/deleteProxy/{areaId}", method = DELETE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public @ResponseBody Map deleteProxy(@PathVariable(name="areaId") AreaModel area) throws LiquidoException {
    log.trace("deleteProxy in area="+area);

    UserModel currentUser = liquidoAuditorAware.getCurrentAuditor();
    if (currentUser == null) throw new LiquidoException(LiquidoException.Errors.NO_LOGIN, "Cannot delete Proxy. Need an authenticated user as fromUser");

    DelegationModel existingDelegation = delegationRepo.findByAreaAndFromUser(area, currentUser);
    if (existingDelegation != null) {
      log.trace("Removing proxy. Deleting existing delegation: " + existingDelegation);
      delegationRepo.delete(existingDelegation);
      return Collections.singletonMap("msg", "Successfully removed proxy.");   // cannot return simple string in Spring :-( http://stackoverflow.com/questions/30895286/spring-mvc-how-to-return-simple-string-as-json-in-rest-controller/30895501
    } else {
      return Collections.singletonMap("msg", "You already did not have any proxy in that area.");
    }

  }

}



