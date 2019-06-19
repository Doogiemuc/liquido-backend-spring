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
import org.doogie.liquido.util.LiquidoRestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

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
// https://jira.spring.io/browse/DATAREST-1323  (by me)
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
  OneTimeTokenRepo ottRepo;

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
		Optional<UserModel> existingByEmail = userRepo.findByEmail(newUser.getEmail());
		if (existingByEmail.isPresent()) throw new LiquidoException(LiquidoException.Errors.USER_EXISTS, "User with that email already exists");
		Optional<UserModel> existingByMobile = userRepo.findByProfileMobilephone(newUser.getProfile().getMobilephone());
		if (existingByMobile.isPresent()) throw new LiquidoException(LiquidoException.Errors.USER_EXISTS, "User with that mobile phone number already exists");

		//----- save new user (mobile phone number is automatically cleaned in UserProfileModel.java
		userRepo.save(newUser);
		return ResponseEntity.ok().build();
	}

	/**
	 * SMS login flow - step 1 - user requests login code via SMS
	 * @param mobile users mobile phone number. MUST be a known user. Mobile numer will be cleaned.
	 * @return HttpStatus.OK, when code was sent via SMS
	 *        OR HttpStatus.NOT_FOUND when cleaned mobile phone number was not found and user must register first.
	 */
  @RequestMapping(value = "/auth/requestSmsCode", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity requestSmsCode(@RequestParam("mobile") String mobile) throws LiquidoException {
		if (DoogiesUtil.isEmpty(mobile)) throw new LiquidoException(LiquidoException.Errors.MOBILE_NOT_FOUND,  "Need mobile phone number!");
		final String cleanMobile = LiquidoRestUtils.cleanMobilephone(mobile);
  	log.info("request SMS login code for mobile="+cleanMobile);
	  UserModel user = userRepo.findByProfileMobilephone(cleanMobile)
		  .orElseThrow(() -> new LiquidoException(LiquidoException.Errors.MOBILE_NOT_FOUND,  "No user found with mobile number "+cleanMobile+". You must register first."));

	  // Create new SMS token: six random digits between [100000...999999]
	  String smsCode = DoogiesUtil.randomDigits(6);
	  LocalDateTime validUntil = LocalDateTime.now().plusHours(1);  // login token via SMS is valid for one hour. That should be enough!
		OneTimeToken oneTimeToken = new OneTimeToken(smsCode, user, OneTimeToken.TOKEN_TYPE.SMS, validUntil);
		ottRepo.save(oneTimeToken);
		log.info("User "+user.getEmail()+" may login. Sending code via SMS.");
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
	  	UserModel user = userRepo.findByProfileMobilephone(mobile)
					.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN, "DevLogin: user for mobile phone "+mobile+" not found."));
	  	log.info("DEV Login as "+mobile+" => "+user);
	  	user.setLastLogin(LocalDateTime.now());
	  	userRepo.save(user);
			return jwtTokenProvider.generateToken(user.getEmail());
		}

	  OneTimeToken oneTimeToken = ottRepo.findByToken(code);
	  if (oneTimeToken == null || mobile == null ||
        !mobile.equals(oneTimeToken.getUser().getProfile().getMobilephone()) ||
	      !OneTimeToken.TOKEN_TYPE.SMS.equals(oneTimeToken.getTokenType())  )
	  {
	  	// We deliberately DO NOT tell the user the exact error reason.
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN, "Invalid SMS login code.");
		}

	  if (LocalDateTime.now().isAfter(oneTimeToken.getValidUntil())) {
	  	ottRepo.delete(oneTimeToken);
		  throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN, "This sms login code is expired.");
	  }

	  //---- delete used one time token
		UserModel user = oneTimeToken.getUser();
		ottRepo.delete(oneTimeToken);

    // return JWT token for this email
		String jwt = jwtTokenProvider.generateToken(user.getEmail());
		oneTimeToken.getUser().setLastLogin(LocalDateTime.now());
		userRepo.save(user);
		log.info("User "+user+ "logged in with valid SMS code.");
		return jwt;
  }

  //TODO: login via E-Mail magic link

	//TODO: clean logout: delete all pending OTPs and voterTokens

}



