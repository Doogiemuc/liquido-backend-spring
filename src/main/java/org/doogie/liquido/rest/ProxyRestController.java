package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.jwt.AuthUtil;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.RightToVoteModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.AssignProxyRequest;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.ProxyService;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
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
@RepositoryRestController
@RequestMapping("${spring.data.rest.base-path}")   // MUST have Request Mapping on class level https://stackoverflow.com/questions/38607421/spring-data-rest-controllers-behaviour-and-usage-of-basepathawarecontroller
																									 //https://jira.spring.io/browse/DATAREST-1327
public class ProxyRestController {

	@Autowired
	ProxyService proxyService;

	@Autowired
	AuthUtil authUtil;

	/**
	 * Get own user information as HATEOAS
	 * @return Full info about the currently logged in user
	 * @throws LiquidoException when no JWT was sent in header
	 */
	@RequestMapping(value = "/my/user", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody
	PersistentEntityResource getMyUser(PersistentEntityResourceAssembler resourceAssembler) throws LiquidoException {
		// This method MUST be implemented in a @RepositoryRestController when we want to return the user as a HATEOAS resource! Therefore it isi in ProxyRestController and not in UserRestController
		log.trace("GET /my/user");
		UserModel currentUser = authUtil.getCurrentUserFromDB()
			.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "You must be logged in to get your own user info."));
		log.trace("GET /my/user returns "+currentUser);
		return resourceAssembler.toFullResource(currentUser);
	}

	/**
	 * When a voter delegates his vote to a proxy, then this is his direct proxy in this area.
	 * If the proxy is not yet a public proxy, then the delegation is only requested.
	 *
	 * When that proxy in turn delegates his vote to another proxy, then this is a transtivie delegation.
	 * At the end of this chain is the user's top proxy.
	 *
	 * @param area an area id or URI
	 * @return all the information about the proxy of this user
	 */
	@RequestMapping(value = "/my/proxy/{areaId}", method = RequestMethod.GET)
	public @ResponseBody Lson getProxyInfo(@PathVariable("areaId") AreaModel area) throws LiquidoException {
		UserModel proxy = authUtil.getCurrentUserFromDB()
				.orElseThrow(()-> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "You must be logged in to get your proxy map!"));

		Optional<DelegationModel> directProxy = proxyService.getDelegationToDirectProxy(area, proxy);
		Optional<UserModel> topProxy = proxyService.findTopProxy(area, proxy);
		//Optional<ChecksumModel> checksumOfPublicProxy = proxyService.getChecksumOfPublicProxy(area, proxy);

		Lson proxyInfo = Lson.builder()
				.put("area", area)																						// also directly inline the full area JSON, because client needs it
				.put("directProxyDelegation", directProxy.orElse(null))		// may also be a requested delegation
				.put("topProxy", topProxy.orElse(null));  									// this inlines the topProxy information, because client needs it

				//MAYBE: could add some more info, that this makes the response quite slow ...
				//.put("isPublicProxy", checksumOfPublicProxy.isPresent())
				//.put("acceptedDelegations", proxyService.findAcceptedDirectDelegations(area, proxy))   // these are only the direct delegations. See  getRealDelegationCount(voterToken)
				//.put("delegationRequests", proxyService.findDelegationRequests(area, proxy))

		return proxyInfo;
	}

	/**
	 * Get all users that the currently logged in user could assign as his proxy in this area.
	 * Assignable users are all users that would not form a circle in the tree of delegations.
	 * @param area
	 * @return
	 * @throws LiquidoException
	 */
	@RequestMapping("/my/proxy/{areaId}/assignable")
	public @ResponseBody List<UserModel> getAssignableProxies(@PathVariable("areaId") AreaModel area) throws LiquidoException {
		List<UserModel> assignableProxies = proxyService.getAssignableProxies(area);
		return assignableProxies;
	}

	/**
	 * Save a proxy for the logged in user. This will insert a new delegation or update an existing one in that area.
	 * @param assignProxyRequest proxy and voterToken
	 * @return the created (or updated) delegation as HAL+JSON
	 *         If you you need full detailed data of all referenced entities, you can request the delegationProjection.
	 *         Or you can send additional requests for example for the "_links.toProxy.href" URI
	 */
	@RequestMapping(value = "/my/proxy/{areaId}", method = PUT)
	@ResponseStatus(value = HttpStatus.CREATED)
	public ResponseEntity assignProxy(@PathVariable("areaId") AreaModel area,	@RequestBody AssignProxyRequest assignProxyRequest) throws LiquidoException {
		log.info("assignProxy " + assignProxyRequest + " in area(id=" + area.getId() + ")");
		UserModel currentUser = authUtil.getCurrentUserFromDB()
				.orElseThrow(()-> new  LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Cannot save Proxy. Need an authenticated user."));
		DelegationModel delegation = proxyService.assignProxy(area, currentUser, assignProxyRequest.getToProxy(), assignProxyRequest.getVoterToken());
		if (delegation.isDelegationRequest()) {
			log.info("Proxy is not yet assigned. Proxy must still confirm.");
			return new ResponseEntity(delegation, HttpStatus.ACCEPTED);  // 202
		} else {
			log.info("Assigned new proxy");
			return new ResponseEntity(delegation, HttpStatus.CREATED);  // 201
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
		UserModel currentUser = authUtil.getCurrentUserFromDB()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Need login to delete proxy!"));
		log.info("deleteProxy(voter="+currentUser+", area="+area+")");
		proxyService.removeProxy(area, currentUser, voterToken);
	}

	/**
	 * Get all information about delegations TO this voter as a proxy.
	 * @param area an area
	 * @param voterToken we need to the voter token to calculate the recursive delegationCount
	 * @return JSON with accepted and requested delgations and the full recursive delegationCount
	 * @throws LiquidoException
	 */
	@RequestMapping(value = "/my/delegations/{areaId}", method = GET)
	public @ResponseBody Lson getDelegations(@PathVariable("areaId") AreaModel area, @RequestParam("voterToken") String voterToken) throws LiquidoException {
		UserModel proxy = authUtil.getCurrentUserFromDB()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Need login to get delegation requests"));
		List<DelegationModel> acceptedDelegations = proxyService.findAcceptedDirectDelegations(area, proxy);
		List<DelegationModel> delegationRequests = proxyService.findDelegationRequests(area, proxy);
		Optional<RightToVoteModel> checksumOfPublicProxy = proxyService.getRightToVoteOfPublicProxy(area, proxy);
		long delegationCount = proxyService.getRecursiveDelegationCount(voterToken);
		return Lson.builder()
				.put("acceptedDirectDelegations", acceptedDelegations)
				.put("isPublicProxy", checksumOfPublicProxy.isPresent())
				.put("delegationRequests", delegationRequests)
				.put("delegationCount", delegationCount);		// number of already existing delegations to this proxy. This is also returned by getVoterToken
	}

	@RequestMapping(value = "/my/delegations/{areaId}/accept", method = PUT)
	public @ResponseBody Lson acceptDelegationRequests(@PathVariable("areaId") AreaModel area, @RequestBody Map bodyMap) throws LiquidoException {
		UserModel proxy = authUtil.getCurrentUserFromDB()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Need login to accept delegation requests."));
		String voterToken = (String)(bodyMap.get("voterToken"));    // We must pass JSON as body because of RepositoryRestController annotaion
		if (voterToken == null) throw new IllegalArgumentException("Need voter token to acceptDelegationRequests");
		long delegationCount = proxyService.acceptDelegationRequests(area, proxy, voterToken);
		return Lson.builder("delegationCount", delegationCount);
	}

	/**
	 * Become a public proxy in this area. This will automatically accept any pending delegation requests
	 *
	 * @param area an area
	 * @body  JSON with voterToken
	 * @return the rightToVote of the proxy in that area which is now public, ie. linked to this user
	 * @throws LiquidoException if voterToken is not valid
	 */
	@RequestMapping(value = "/my/delegations/{areaId}/becomePublicProxy", method = PUT)   // PUT is idempotent, so if you PUT an object twice, it has no additional effect. And that is what we need here.
	public ResponseEntity becomePublicProxy(@PathVariable("areaId") AreaModel area, @RequestBody Map bodyMap) throws LiquidoException {
		UserModel proxy = authUtil.getCurrentUserFromDB()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Need login to become a public proxy."));
		String voterToken = (String)(bodyMap.get("voterToken"));
		if (voterToken == null) throw new IllegalArgumentException("Need voter token to become a public proxy");
		RightToVoteModel rightToVote = proxyService.becomePublicProxy(proxy, area, voterToken);
		// ChecksomRepo is not exposed as REST endpoint. So we build our own response
		return ResponseEntity.ok(rightToVote);
	}

	/**
	 * Get the checksum of a public proxy, so that we can delegate to it.
	 * With this request a client can also check if a user already is a public proxy.
	 */
	@RequestMapping("/users/{userId}/publicChecksum")
	public ResponseEntity getPublicChecksum(@PathVariable("userId") UserModel proxy, @RequestParam("area") AreaModel area) throws LiquidoException {
		if (proxy == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_FIND_ENTITY, "User with that id not found.");
		if (area == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_FIND_ENTITY, "Area with that id not found.");
		log.trace("=> /users/{userId}/publicChecksum?area="+area+"&proxy="+proxy);
		Optional<RightToVoteModel> checksumOfPublicProxy = proxyService.getRightToVoteOfPublicProxy(area, proxy);
		if (!checksumOfPublicProxy.isPresent())
			throw new LiquidoException(LiquidoException.Errors.PUBLIC_CHECKSUM_NOT_FOUND, "User is not yet a public proxy.");
		return ResponseEntity.of(checksumOfPublicProxy);  // This would also return 404 when publicChecksum is not present.  But without any error message
	}


	// Some collected infos...

	//Implementation note  about different ways of returning data back to the client.

	//PersistentEntityResourceAssembler resourceAssembler
	//Authentication auth  // spring-security authentication object could be injected like this

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
