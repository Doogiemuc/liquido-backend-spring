package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.OneTimeTokenRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.jwt.JwtTokenProvider;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.OneTimeToken;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.AssignProxyRequest;
import org.doogie.liquido.rest.dto.ProxyMapResponseElem;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.ProxyService;
import org.doogie.liquido.util.DoogiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Rest Controller for handling user data and (voting) proxy
 */
@Slf4j
//@BasePathAwareController
@RestController
@RequestMapping("${spring.data.rest.base-path}")
public class UserRestController {

	private static final int MIN_PASSWORD_LENGTH = 8;

	@Autowired
	Environment springEnv;

  @Autowired
	CastVoteService castVoteService;

  @Autowired
  ProxyService proxyService;

  @Autowired
  LiquidoAuditorAware liquidoAuditorAware;

  @Autowired
	UserRepo userRepo;

  @Autowired
  OneTimeTokenRepo tokenRepo;

  @Autowired
	JwtTokenProvider jwtTokenProvider;

	/**
	 * Register as a new user
	 * @param newUser UserModel with at least username and password.   Profile.phonenumber can also be filled.
	 * @return HTTP OK(200)
	 * @throws LiquidoException when newUser is not ok.
	 */
	@RequestMapping("/auth/register")
	public ResponseEntity registerNewUser(@RequestBody UserModel newUser) throws LiquidoException {
		log.info("register new user "+newUser);
		//----- sanity checks
		if (newUser == null || DoogiesUtil.isEmpty(newUser.getEmail()) || DoogiesUtil.isEmpty(newUser.getPasswordHash()) ||
		    newUser.getPasswordHash().length() < MIN_PASSWORD_LENGTH)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_REGISTER, "Need data for new user");
		UserModel existingUser = userRepo.findByEmail(newUser.getEmail());
		if (existingUser != null)
			throw new LiquidoException(LiquidoException.Errors.CANNOT_REGISTER, "User already exists");

		//----- save new user
		userRepo.save(newUser);
		return ResponseEntity.ok("");
	}

	/**
	 * SMS login flow - step 1 - user requests login code via SMS
	 * @param mobile users mobile phone number. MUST be a known user
	 * @return HttpStatus.OK, when code was sent via SMS
	 *        OR HttpStatus.NOT_FOUND when mobile phone number was not found and user must register first.
	 */
  @RequestMapping("/auth/requestSmsCode")
  public String requestSmsCode(@RequestParam("mobile") String mobile) throws LiquidoException {
  	log.info("request SMS login code for mobile="+mobile);
	  UserModel user = userRepo.findByProfilePhonenumber(mobile);
	  if (user == null) throw new LiquidoException(LiquidoException.Errors.MUST_REGISTER,  "No user found with mobile "+mobile+". You must register first.");  //TODO: return 404

	  // Create new SMS token: four random digits between [1000...9999]
	  String smsCode = String.valueOf(new Random().nextInt(9000)+ 1000);
	  LocalDateTime validUntil = LocalDateTime.now().plusHours(1);  // login token via SMS is valid for one hour. That should be enough!
		OneTimeToken oneTimeToken = new OneTimeToken(smsCode, user, OneTimeToken.TOKEN_TYPE.SMS, validUntil);
		tokenRepo.save(oneTimeToken);
		log.debug("User "+user.getEmail()+" may login. Sending code via SMS.");
	  if (springEnv.acceptsProfiles(Profiles.of("dev", "test"))) {
		  return smsCode;  // when debugging, return the code
	  } else {
	  	return "";
	  }
  }

	/**
	 * SMS login flow - step 2 - login with received SMS code
	 * This endpoint must be public.
	 * @param code the 4 digit code from the SMS
	 * @return Oauth access token
	 */
  @RequestMapping(path = "/auth/loginWithSmsCode", produces = MediaType.TEXT_PLAIN_VALUE)
  public String loginWithSmsCode(
		  @RequestParam("mobile") String mobile,
  		@RequestParam("code") String code
  ) throws LiquidoException {
	  log.debug("login with sms code="+code);
	  OneTimeToken oneTimeToken = tokenRepo.findByToken(code);
	  if (oneTimeToken == null)
	  	throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN, "Invalid/Unknown sms login code.");
	  if (!mobile.equals(oneTimeToken.getUser().getProfile().getPhonenumber()))
		  throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN, "Invalid/Unknown sms login code.");
	  if (!oneTimeToken.getToken_type().equals(OneTimeToken.TOKEN_TYPE.SMS))
		  throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN, "This is not an SMS login token.");
	  if (LocalDateTime.now().isAfter(oneTimeToken.getValidUntil())) {
	  	tokenRepo.delete(oneTimeToken);
		  throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN, "This sms login token is expired.");
	  }

	  //---- delete used one time token
		tokenRepo.delete(oneTimeToken);

    // return JWT token for this email
		return jwtTokenProvider.generateToken(oneTimeToken.getUser().getEmail());
  }

	/**
	 * get own user information
	 * @return UserModel of the currently logged in user
	 * @throws LiquidoException
	 */
  @RequestMapping("/my/user")
	public UserModel getOwnUser() throws LiquidoException {
  	log.trace("GET /my/user");
  	UserModel currentUser = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.NO_LOGIN, "You must be logged in to get your own user info."));
  	return currentUser;
	}

  /**
   * Calculate the number of votes a proxy may cast (including his own one) because of (transitive) delegation
   * of votes to this proxy.
   * @param area ID of area
   * @return the number of votes this user may cast in this area, including his own one!
   */
  @RequestMapping(value = "/my/numVotes", method = GET)    // GET /my/numVotes?area=/uri/of/area  ?
  public long getNumVotes(@RequestParam("area")AreaModel area) throws LiquidoException {
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
   * @param assignProxyRequest assign proxy toProxy in area with voterToken
   * @return the created (or updated) delegation as HAL+JSON
   *         If you you need full detailed data of all referenced entities, you can request the delegationProjection.
   *         Or you can send additional requests for example for the "_links.toProxy.href" URI
   */
  @RequestMapping(value = "/my/proxy", method = PUT)
	@ResponseStatus(value = HttpStatus.CREATED)
  public
    @ResponseBody ResponseEntity<?>   							// Return normal HTTP response with savedDelegation
		//PersistentEntityResource 						// Return HAL representation of Model
    //HttpEntity<DelegationProjection>    // This way one could return the DelegationProjection  (with inlined referenced objects)  but that cannot be used by the client for further updates
    assignProxy(
			@RequestBody AssignProxyRequest assignProxyRequest,
			PersistentEntityResourceAssembler resourceAssembler
      //Authentication auth  // not needed anymore - spring-security authentication object could be injected like this
    ) throws LiquidoException {
    	log.info("assignProxy("+assignProxyRequest+")");
			UserModel currentUser = liquidoAuditorAware.getCurrentAuditor()
					.orElseThrow(()-> new  LiquidoException(LiquidoException.Errors.NO_LOGIN, "Cannot save Proxy. Need an authenticated user."));

			DelegationModel savedDelegation = proxyService.assignProxy(assignProxyRequest.getArea(), currentUser, assignProxyRequest.getToProxy(), assignProxyRequest.getVoterToken());
			if (savedDelegation == null) {
				log.info("Proxy is not yet assigned. Proxy must still confirm.");
				return new ResponseEntity(HttpStatus.ACCEPTED);  // 202
			} else {
				log.info("Assigned new proxy");
				return new ResponseEntity<>(savedDelegation, HttpStatus.CREATED);  // 201
			}



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
  public void deleteProxy(@PathVariable(name="areaId") AreaModel area) throws LiquidoException {
		UserModel currentUser = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.NO_LOGIN, "Need login to delete proxy!"));
		log.info("deleteProxy(voter="+currentUser+", area="+area+")");
    proxyService.removeProxy(area, currentUser, currentUser.getPasswordHash());
  }


  //TODO: change a user's password => delete all tokens and checksums

	//TODO: @ExceptionHandler(LiquidoRestException.class)  => handle liquido exception. Return JSON with Error Code

}



