package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.TokenChecksumRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.TokenChecksumModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.AssignProxyRequest;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.ProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
	TokenChecksumRepo checksumRepo;


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
	 * get all proxies that this user currently has assigned (per area)
	 * @return a Map with directProxy and topProxy per area
	 */
	@RequestMapping(value = "/my/proxyMap", method = GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Map getProxyMap() throws LiquidoException {
		UserModel user = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(()-> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "You must be logged in to get your proxy map!"));
		return proxyService.getDirectProxies(user);
		/*
		Map<AreaModel, UserModel> proxyMap = proxyService.getDirectProxies(user);
		HashMap<String, Object> result = new HashMap<>();
		for(AreaModel area: proxyMap.keySet()) {
			UserModel directProxy = proxyMap.get(area);
			UserModel topProxy = proxyService.findTopProxy(area, user);
			ProxyMapResponseElem elem = new ProxyMapResponseElem(directProxy, topProxy);
			result.put(area.getTitle(), elem);
		}
		return result;
		*/
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

		TokenChecksumModel proxiesChecksumModel = proxyService.assignProxy(assignProxyRequest.getArea(), currentUser, assignProxyRequest.getToProxy(), assignProxyRequest.getVoterToken(), assignProxyRequest.isTransitive());
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

	@RequestMapping("/checksumOfPublicProxy")
	public ResponseEntity getChecksumOfPublicProxy(@RequestParam("area") AreaModel area, @RequestParam("proxy") UserModel proxy) throws LiquidoException {
		if (area == null) return ResponseEntity.badRequest().body("Need area");
		if (proxy == null) return ResponseEntity.badRequest().body("Need proxy");
		log.trace("=> GET /checksumOfPublicProxy?area="+area+"&proxy="+proxy);

		TokenChecksumModel checksum = checksumRepo.findByAreaAndPublicProxy(area, proxy);
		if (checksum == null) ResponseEntity.notFound();  // user is not a public proxy in that area

		log.trace("<= GET GET /checksumOfPublicProxy?area="+area.getId()+"&proxy="+proxy.getId()+ " returns checksum="+checksum.getChecksum());
		return ResponseEntity.ok(checksum);
	}

}
