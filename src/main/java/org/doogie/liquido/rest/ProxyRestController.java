package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.ChecksumModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.AssignProxyRequest;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.ProxyService;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Liquido REST endpoint for working with proxies.
 */
@Slf4j
@BasePathAwareController  //ProxyRestController MUST be a BasePathAwareController!!! Otherwise the deserialization of URIs does not work in POST /my/proxy
public class ProxyRestController {

	@Autowired
	ProxyService proxyService;

	@Autowired
	LiquidoAuditorAware liquidoAuditorAware;

	/**
	 * Get own user information as HATEOAS
	 * @return Full info about the currently logged in user
	 * @throws LiquidoException when no JWT was sent in header
	 */
	@RequestMapping(value = "/my/user", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody PersistentEntityResource getMyUser(PersistentEntityResourceAssembler resourceAssembler) throws LiquidoException {
		log.trace("GET /my/user");
		UserModel currentUser = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "You must be logged in to get your own user info."));
		log.trace("GET /my/user returns "+currentUser);
		return resourceAssembler.toResource(currentUser);
	}

	/**
	 * When a voter delegates his vote to a proxy, then this is his direct proxy.
	 * When the proxy in turn delegates his vote this is a transitive proxy.
	 * At the end of this chain is the user's top proxy for that area.
	 *
	 * @param area an area id or URI
	 * @return all the information about the proxies of this user in that area. And delegation requests to that user.
	 */
	@RequestMapping(value = "/my/proxy", method = RequestMethod.GET)
	public @ResponseBody Lson getProxyInfo(@RequestParam("area") AreaModel area, @RequestParam(value="voterToken") String voterToken) throws LiquidoException {
		UserModel proxy = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(()-> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "You must be logged in to get your proxy map!"));
		return proxyService.getProxyInfo(area, proxy, voterToken);
	}

	/**
	 * Save a proxy for the logged in user. This will insert a new delegation or update an existing one in that area.
	 * @param assignProxyRequest assign proxy toProxy in area with voterToken
	 * @return the created (or updated) delegation as HAL+JSON
	 *         If you you need full detailed data of all referenced entities, you can request the delegationProjection.
	 *         Or you can send additional requests for example for the "_links.toProxy.href" URI
	 */
	@RequestMapping(value = "/my/proxy", method = PUT)
	@ResponseStatus(value = HttpStatus.CREATED)
	public
	ResponseEntity<?>                			// Return normal HTTP response with savedDelegation
	//PersistentEntityResource 						// Return HAL representation of Model
	//HttpEntity<DelegationProjection>    // This way one could return the DelegationProjection  (with inlined referenced objects)  but that cannot be used by the client for further updates
	assignProxy(
			@RequestBody AssignProxyRequest assignProxyRequest
			//PersistentEntityResourceAssembler resourceAssembler
			//Authentication auth  // not needed anymore - spring-security authentication object could be injected like this
	) throws LiquidoException {
		log.info("assignProxy("+assignProxyRequest+")");
		UserModel currentUser = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(()-> new  LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Cannot save Proxy. Need an authenticated user."));

		ChecksumModel proxiesChecksumModel = proxyService.assignProxy(assignProxyRequest.getArea(), currentUser, assignProxyRequest.getToProxy(), assignProxyRequest.getVoterToken(), assignProxyRequest.isTransitive());
		if (proxiesChecksumModel == null) {
			log.info("Proxy is not yet assigned. Proxy must still confirm.");
			return new ResponseEntity(HttpStatus.ACCEPTED);  // 202
		} else {
			log.info("Assigned new proxy");
			return new ResponseEntity<>(proxiesChecksumModel, HttpStatus.CREATED);  // 201
		}
	}

	/**
	 * delete a proxy of the currently logged in user in one area.
	 * @param area the area where to delete the proxy
	 * @return HTTP status 204 (NoContent) on success
	 */
	@RequestMapping(value = "/my/proxy/{areaId}", method = DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteProxy(@PathVariable("areaId") AreaModel area,	@RequestParam("voterToken") String voterToken) throws LiquidoException {
		UserModel currentUser = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Need login to delete proxy!"));
		log.info("deleteProxy(voter="+currentUser+", area="+area+")");
		proxyService.removeProxy(area, currentUser, voterToken);
	}


	/**
	 * Calculate the number of delegations to this proxy. If this voter is not yet a proxy, this method will return
	 * a delegationCount of zero.
	 *
	 * We need the voterToken, because real delegations are calculated from the tree of checksums.
	 *
	 * @param area the area. needed to check for delegation requests.
	 * @param voterToken voterToken of a voter. Number of votes are calculated from real checksum delegations. Therefore we need the voterToken.
	 * @return the number of votes this user may cast with this voterToken in an area.
	 *         And also an array of delegationRequests if there are any pending ones.
	 */
	@RequestMapping(value = "/my/delegationCount", method = GET)
	public @ResponseBody Lson getDelegations(@RequestParam("area") AreaModel area, @RequestParam("voterToken")String voterToken) throws LiquidoException {
		log.trace("=> GET /my/delegations");
		UserModel proxy = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "You must be logged in to get your numVotes!"));
		long delegationCount = proxyService.getDelegationCount(voterToken);
		List<DelegationModel> delegationRequests = proxyService.findDelegationRequests(area, proxy);
		Lson response = Lson.builder()
				.put("delegationCount", delegationCount)
				.put("delegationRequests", delegationRequests.size());
		log.info("<= GET /my/delegations?area="+area.getId()+" for proxy " + proxy.toStringShort() + " returns "+response);
		return response;
	}


	@RequestMapping(value = "/my/delegations", method = GET)
	public @ResponseBody Lson getDelegationRequests(@RequestParam("area") AreaModel area) throws LiquidoException {
		UserModel proxy = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Need login to get delegation requests"));
		List<DelegationModel> acceptedDelegations = proxyService.findAcceptedDelegations(area, proxy);
		List<DelegationModel> delegationRequests = proxyService.findDelegationRequests(area, proxy);
		return Lson.builder()
				.put("acceptedDelegations", acceptedDelegations)
				.put("delegationRequests", delegationRequests);
	}

	@RequestMapping(value = "/my/delegationRequests/{areaId}", method = PUT)
	public @ResponseBody Lson acceptDelegationRequests(@PathVariable("area") AreaModel area, @RequestParam("voterToken")String voterToken) throws LiquidoException {
		UserModel proxy = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Need login to accept delegation requests."));
		long delegationCount = proxyService.acceptDelegationRequests(area, proxy, voterToken);
		return Lson.builder("delegationCount", delegationCount);
	}

	/**
	 * Get the checksum of a public proxy, so that we can delegate to it.
	 * With this request a client can also check if a user already is a public proxy.
	 */
	@RequestMapping("/users/{userId}/publicChecksum")
	public ResponseEntity getPublicChecksum(@RequestParam("area") AreaModel area, @PathVariable("userId") UserModel proxy) throws LiquidoException {
		if (proxy == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_FIND_ENTITY, "User with that id not found.");
		if (area == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_FIND_ENTITY, "Area with that id not found.");
		log.trace("=> /users/{userId}/publicChecksum?area="+area+"&proxy="+proxy);
		Optional<ChecksumModel> checksumOfPublicProxy = proxyService.getChecksumOfPublicProxy(area, proxy);
		if (!checksumOfPublicProxy.isPresent())
			throw new LiquidoException(LiquidoException.Errors.PUBLIC_CHECKSUM_NOT_FOUND, "User is not yet a public proxy.");
		return ResponseEntity.of(checksumOfPublicProxy);  // This would also return 404 when publicChecksum is not present.  But without any error message
	}

	//Implementation note  about different ways of returning data back to the client.

	// Return HATEOS representation of Delegation => does not work correctly, cause delegationRepo is not exposed as spring-data-rest endpoint.
	// with param (PersistentEntityResourceAssembler resourceAssembler)  and then
	// return resourceAssembler.toResource(savedDelegation);
	// https://stackoverflow.com/questions/31758862/enable-hal-serialization-in-spring-boot-for-custom-controller-method#31782016

	// You should not return a DelegationProjection here, as this code would:
	//   return new ResponseEntity<>(savedDelegation, HttpStatus.OK);
	// because that cannot be used for further updates by the client.
	// http://stackoverflow.com/questions/30220333/why-is-an-excerpt-projection-not-applied-automatically-for-a-spring-data-rest-it

  /*  *sic*
	@PathVariable (Spring based)  --- equivalent ---  @PathParam (JAX-RS)
	@RequestParam (Spring based)  --- equivalent ---  @QueryParam (JAX-RS)
	*/

}
