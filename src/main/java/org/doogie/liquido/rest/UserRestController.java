package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.OneTimeTokenRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.jwt.JwtTokenProvider;
import org.doogie.liquido.model.OneTimeToken;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.ProxyService;
import org.doogie.liquido.util.DoogiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Random;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Rest Controller for registration, login and user data
 */
@Slf4j
// ******************************************************************************************
// If you want to return text/plain from any controller method, then you MUST use a @RestController
// A @BasePathAwareController has all the spring-data-jpa magic, but it cannot return plain string.
// See
// https://stackoverflow.com/questions/38607421/spring-data-rest-controllers-behaviour-and-usage-of-basepathawarecontroller
// https://stackoverflow.com/questions/33687722/spring-data-rest-custom-method-return-string
// ******************************************************************************************
// I want this to be a normal Spring Rest controller.   No spring-dta-jpa magic.
// Since this a normal RestController, we do not need the @ResponseBody annotation on every method.
@RestController
@RequestMapping("${spring.data.rest.base-path}")
public class UserRestController {
	private static final int MIN_PASSWORD_LENGTH = 8;

	@Autowired
	Environment springEnv;

  @Autowired
  LiquidoAuditorAware liquidoAuditorAware;

  @Autowired
	UserRepo userRepo;

  @Autowired
	ProxyService proxyService;

  @Autowired
  OneTimeTokenRepo tokenRepo;

  @Autowired
	JwtTokenProvider jwtTokenProvider;

	/**
	 * Register as a new user
	 * @param newUser UserModel with at least email and profile.mobilePhone.
	 * @return HTTP OK(200)   or error 400 when data is missing or user already exists
	 * @throws LiquidoException when newUser is not ok.
	 */
	@RequestMapping(path = "/auth/register", method = RequestMethod.POST)
	public ResponseEntity registerNewUser(@RequestBody UserModel newUser) throws LiquidoException {
		log.info("register new user "+newUser);
		//----- sanity checks
		if (newUser == null || newUser.getProfile() == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_REGISTER, "Need data for new user");
		if (DoogiesUtil.isEmpty(newUser.getEmail())) throw new LiquidoException(LiquidoException.Errors.CANNOT_REGISTER, "Need email for new user");
		if (DoogiesUtil.isEmpty(newUser.getProfile().getMobilephone())) throw new LiquidoException(LiquidoException.Errors.CANNOT_REGISTER, "Need mobile phone for new user");
		UserModel existingUser = userRepo.findByEmail(newUser.getEmail());
		if (existingUser != null)	throw new LiquidoException(LiquidoException.Errors.CANNOT_REGISTER, "User already exists");

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
  @RequestMapping(value = "/auth/requestSmsCode", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity requestSmsCode(@RequestParam("mobile") String mobile) throws LiquidoException {
		if (mobile == null) throw new LiquidoException(LiquidoException.Errors.MOBILE_NOT_FOUND,  "Need mobile phone number!");
		//mobile = cleanMobilephone(mobile);
  	log.info("request SMS login code for mobile="+mobile);
	  UserModel user = userRepo.findByProfileMobilephone(mobile);
	  if (user == null) throw new LiquidoException(LiquidoException.Errors.MOBILE_NOT_FOUND,  "No user found with mobile number "+mobile+". You must register first.");

	  // Create new SMS token: four random digits between [1000...9999]
	  String smsCode = String.valueOf(new Random().nextInt(9000)+ 1000);
	  LocalDateTime validUntil = LocalDateTime.now().plusHours(1);  // login token via SMS is valid for one hour. That should be enough!
		OneTimeToken oneTimeToken = new OneTimeToken(smsCode, user, OneTimeToken.TOKEN_TYPE.SMS, validUntil);
		tokenRepo.save(oneTimeToken);
		log.debug("User "+user.getEmail()+" may login. Sending code via SMS.");
	  if (springEnv.acceptsProfiles(Profiles.of("dev", "test"))) {
		  return ResponseEntity.ok().header("code", smsCode).build();  // when debugging, return the code in the header
	  } else {
	  	return ResponseEntity.ok().build();
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
	  log.debug("Request to login with sms code="+code);

	  // in DEV or TEST environment allow login as any use with a special code
	  if (springEnv.acceptsProfiles(Profiles.of("dev", "test")) && code.equals(springEnv.getProperty("liquido.dev.dummySmsLoginCode"))) {
	  	UserModel user = userRepo.findByProfileMobilephone(mobile);
	  	if (user == null) throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN, "DevLogin: user for mobile phone "+mobile+" not found.");
	  	log.info("DEV Login as "+mobile+" => "+user);
			return jwtTokenProvider.generateToken(user.getEmail());
		}

	  OneTimeToken oneTimeToken = tokenRepo.findByToken(code);
	  if (oneTimeToken == null || mobile == null ||
        !mobile.equals(oneTimeToken.getUser().getProfile().getMobilephone()) ||
	      !OneTimeToken.TOKEN_TYPE.SMS.equals(oneTimeToken.getTokenType())  )
	  {
	  	// We deliberately DO NOT tell the user the exact error reason.
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN, "Invalid SMS login code.");
		}

	  if (LocalDateTime.now().isAfter(oneTimeToken.getValidUntil())) {
	  	tokenRepo.delete(oneTimeToken);
		  throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN, "This sms login code is expired.");
	  }

	  //---- delete used one time token
		tokenRepo.delete(oneTimeToken);

    // return JWT token for this email
		log.info("User "+oneTimeToken.getUser()+ "logged in with valid SMS code.");
		return jwtTokenProvider.generateToken(oneTimeToken.getUser().getEmail());
  }

	/**
	 * Calculate the number of votes a voter may cast. If this is a normal voter without any delegations this method will return 1.
	 * If this voter is a proxy, because other checksums were delegated to him, then this method will return
	 * the recursive count of those delegations plus the one vote of the proxy himself.
	 *
	 * @param voterToken voterToken of a voter
	 * @return the number of votes this user may cast with this voterToken in an area.
	 */
	@RequestMapping(value = "/my/numVotes", method = GET)
	public long getNumVotes(@RequestParam("voterToken")String voterToken) throws LiquidoException {
		log.trace("=> GET /my/numVotes");
		UserModel proxy = liquidoAuditorAware.getCurrentAuditor()
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "You must be logged in to get your numVotes!"));
		long numVotes = proxyService.getNumVotes(voterToken);
		log.trace("<= GET numVotes(proxy=" + proxy +") = "+numVotes);
		return numVotes;
	}


	//TODO: change a user's password => delete all tokens and checksums

	//TODO: @ExceptionHandler(LiquidoRestException.class)  => handle liquido exception. Return JSON with Error Code

}



