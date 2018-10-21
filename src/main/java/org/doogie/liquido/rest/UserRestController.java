package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.DelegationProjection;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.ProxyMapResponseElem;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.ProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Rest Controller for handling user data and proxies
 */
@Slf4j
@BasePathAwareController
public class UserRestController {

  @Autowired
	CastVoteService castVoteService;

  @Autowired
  ProxyService proxyService;

  @Autowired
  LiquidoAuditorAware liquidoAuditorAware;

  /**
   * Calculate the number of votes a proxy may cast (including his own one) because of (transitive) delegation
   * of votes to this proxy.
   * @param area ID of area
   * @return the number of votes this user may cast in this area, including his own one!
   */
  @RequestMapping(value = "/my/numVotes", method = GET)    // GET /my/numVotes?area=/uri/of/area  ?
  public @ResponseBody long getNumVotes(@RequestParam("area")AreaModel area) throws Exception {
    log.trace("=> GET /my/numVotes  area=" + area);
    UserModel proxy = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.NO_LOGIN, "You must be logged in to get your numVotes!"));
		int numVotes = proxyService.getNumVotes(area, proxy);
    log.trace("<= GET numVotes(proxy=" + proxy + ", area=" + area + ") = "+numVotes);
    return numVotes;
  }


  /**
   * get all proxies that this user currently has assigned (per area)
   * @return a Map with directProxy and topProxy per area
	 */
  @RequestMapping(value = "/my/proxyMap", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody Map getProxyMap() throws LiquidoException {
    UserModel user = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(()-> new LiquidoException(LiquidoException.Errors.NO_LOGIN, "You must be logged in to get your proxy map!"));
    Map<AreaModel, UserModel> proxyMap = proxyService.getProxyMap(user);
		HashMap<String, Object> result = new HashMap<>();
		for(AreaModel area: proxyMap.keySet()) {
			UserModel directProxy = proxyMap.get(area);
			UserModel topProxy = proxyService.findTopProxy(area, user);
			ProxyMapResponseElem elem = new ProxyMapResponseElem(directProxy, topProxy);
			result.put(area.getTitle(), elem);
		}
		return result;
  }

  /**
   * Save a proxy for the logged in user. This will insert a new delegation or update an existing one in that area.
   * @param delegationResource the new delegation that shall be saved (only 'area' and 'toProxy' need to be filled in the request)
   * @return the created (or updated) delegation as HAL+JSON
   *         If you you need full detailed data of all referenced entities, you can request the delegationProjection.
   *         Or you can send additional requests for example for the "_links.toProxy.href" URI
   */
  @RequestMapping(value = "/assignProxy", method = PUT, consumes="application/json", produces = MediaType.APPLICATION_JSON_VALUE)
  public
    @ResponseBody
	  ResponseEntity<?>   							// Return normal HTTP response
		//PersistentEntityResource        Return HAL representation of Model
    // HttpEntity<DelegationModel>    This way one could return the DelegationProjection  (with inlined referenced objects)  but that cannot be used by the client for further updates
    assignProxy(
			@RequestBody Resource<DelegationModel> delegationResource,
			PersistentEntityResourceAssembler resourceAssembler
      //Authentication auth  // not needed anymore - spring-security authentication object could be injected like this
    ) throws LiquidoException {
    DelegationModel newDelegation = delegationResource.getContent();
		log.info("assignProxy(delegation="+newDelegation+")");

		UserModel currentUser = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(()-> new  LiquidoException(LiquidoException.Errors.NO_LOGIN, "Cannot save Proxy. Need an authenticated user."));

		DelegationModel savedDelegation = proxyService.assignProxy(newDelegation.getArea(), currentUser, newDelegation.getToProxy(), currentUser.getPasswordHash());

		if (savedDelegation == null) {
			log.info("Proxy is not yet assigned. Proxy must still confirm.");
			return new ResponseEntity(HttpStatus.ACCEPTED);  // 202
		} else {
			log.info("Assigned new proxy");
			return new ResponseEntity<>(savedDelegation, HttpStatus.CREATED);
		}



		//return the complete HATEOAS representation of the saved delegation.
    //return resourceAssembler.toResource(savedDelegation);

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
  @RequestMapping(value = "/deleteProxy/{areaId}", method = DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteProxy(@PathVariable(name="areaId") AreaModel area) throws LiquidoException {
		UserModel currentUser = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.NO_LOGIN, "Need login to delete proxy!"));
		log.info("deleteProxy(voter="+currentUser+", area="+area+")");
    proxyService.removeProxy(area, currentUser, currentUser.getPasswordHash());
  }


  //TODO: change a user's password => delete all tokens and checksums

}



