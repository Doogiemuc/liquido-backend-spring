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
import java.util.Map;
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
	 * Calculate the number of votes a voter may cast. If this is a normal voter without any delegations this method will return 1.
	 * If this voter is a proxy, because other checksums were delegated to him, then this method will return
	 * the recursive count of those delegations plus the one vote of the proxy himself.
	 * @param area the area. needed to check for delegation requests.
	 * @param voterToken voterToken of a voter. Number of votes are calculated from real checksum delegations. Therefore we need the voterToken
	 * @return the number of votes this user may cast with this voterToken in an area.
	 *         And also an array of delegationRequests if there are any pending ones.
	 */
	@RequestMapping(value = "/my/numVotes", method = GET)
	public @ResponseBody Lson getNumVotes(@RequestParam("area") AreaModel area, @RequestParam("voterToken")String voterToken) throws LiquidoException {
		log.trace("=> GET /my/numVotes");
		UserModel proxy = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "You must be logged in to get your numVotes!"));
		long numVotes = proxyService.getNumVotes(voterToken);
		List<DelegationModel> delegationRequests = proxyService.findDelegationRequests(area, proxy);
  	Lson response = Lson.builder("numVotes", numVotes);
		if (delegationRequests.size() > 0) response.put("delegationRequests", delegationRequests);
		log.trace("<= GET /my/numVotes(proxy=" + proxy +") returns "+numVotes + " votes and "+delegationRequests.size()+" delegation requests");
		return response;
	}

	/**
	 * When a voter delegates his vote to a proxy, then this is his direct proxy.
	 * When the proxy in turn delegates his vote this is a transitive proxy.
	 * At the end of this chain is the user's top proxy for that area.
	 *
	 * @return a map with one entry per area. Each entry contains the direct proxy(if any) and the top proxy(if any) of voter in that area.
	 */
	@RequestMapping(value = "/my/proxyMap", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Lson getProxyMap(@RequestParam(value="voterToken") String voterToken) throws LiquidoException {
		UserModel proxy = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(()-> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "You must be logged in to get your proxy map!"));
		return proxyService.getProxyMap(proxy, voterToken);
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

		//Implementation note  about different ways of returning data back to the client.

		// Return HATEOS representation of Delegation => does not work correctly, cause delegationRepo is not exposed as spring-data-rest endpoint.
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
	@RequestMapping(value = "/my/proxy/{areaId}", method = DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteProxy(
			@PathVariable("areaId") AreaModel area,
			@RequestParam("voterToken") String voterToken)
			throws LiquidoException
	{
		UserModel currentUser = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Need login to delete proxy!"));
		log.info("deleteProxy(voter="+currentUser+", area="+area+")");
		proxyService.removeProxy(area, currentUser, voterToken);
	}

	@RequestMapping(value = "/my/delegationRequests/{areaId}", method = GET)
	public ResponseEntity getDelegationRequests(@PathVariable("areaId") AreaModel area) throws LiquidoException {
		UserModel proxy = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Need login to get delegation requests"));
		List<DelegationModel> delegationRequests = proxyService.findDelegationRequests(area, proxy);
		return new ResponseEntity(delegationRequests, HttpStatus.OK);
	}

	@RequestMapping(value = "/my/delegationRequests/{areaId}", method = PUT)
	public @ResponseBody Lson acceptDelegationRequests(@PathVariable("areaId") AreaModel area, @RequestParam("voterToken")String voterToken) throws LiquidoException {
		UserModel proxy = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Need login to accept delegation requests."));
		long numVotes = proxyService.acceptDelegationRequests(area, proxy, voterToken);
		return Lson.builder("numVotes", numVotes);
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

}
