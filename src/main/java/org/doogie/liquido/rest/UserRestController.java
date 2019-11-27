package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.data.LiquidoProperties;
import org.doogie.liquido.datarepos.OneTimeTokenRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.jwt.JwtTokenProvider;
import org.doogie.liquido.model.OneTimeToken;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.MailService;
import org.doogie.liquido.services.ProxyService;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.LiquidoRestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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
	LiquidoProperties prop;

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

  @Autowired
	MailService mailService;

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
	 * @param mobile users mobile phone number. MUST be a known user. Mobile number will be cleaned.
	 * @return HttpStatus.OK, when code was sent via SMS
	 *        OR HttpStatus.NOT_FOUND when cleaned mobile phone number was not found and user must register first.
	 */
  @RequestMapping(value = "/auth/requestSmsToken", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity requestSmsToken(@RequestParam("mobile") String mobile) throws LiquidoException {
		if (DoogiesUtil.isEmpty(mobile)) throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_MOBILE_NOT_FOUND,  "Need mobile phone number!");
		final String cleanMobile = LiquidoRestUtils.cleanMobilephone(mobile);
  	log.info("request SMS login code for mobile="+cleanMobile);
	  UserModel user = userRepo.findByProfileMobilephone(cleanMobile)
		  .orElseThrow(() -> new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_MOBILE_NOT_FOUND,  "No user found with mobile number "+cleanMobile+". You must register first."));

	  // Create new SMS token: six random digits between [100000...999999]
	  String smsToken = DoogiesUtil.randomDigits(6);
	  LocalDateTime validUntil = LocalDateTime.now().plusHours(1);  // login token via SMS is valid for one hour. That should be enough!
		OneTimeToken oneTimeToken = new OneTimeToken(smsToken, user, OneTimeToken.TOKEN_TYPE.SMS, validUntil);
		ottRepo.save(oneTimeToken);
		log.info("User "+user.getEmail()+" may login. Sending code via SMS.");

		//TODO: send one time token via SMS

	  if (springEnv.acceptsProfiles(Profiles.of("dev", "test"))) {
		  return ResponseEntity.ok().header("token", smsToken).build();  // when debugging, return the code in the header
	  } else {
	  	return ResponseEntity.ok().build();
	  }
  }

	/**
	 * SMS login flow - step 2 - login with received SMS code
	 * This endpoint must be public.
	 * @param token the 4 digit code from the SMS
	 * @return Json Web Token (JWT) with user data encoded within it.
	 */
  @RequestMapping(path = "/auth/loginWithSmsToken", produces = MediaType.TEXT_PLAIN_VALUE)
  public String loginWithSmsToken(
		  @RequestParam("mobile") String mobile,
  		@RequestParam("token") String token
  ) throws LiquidoException {
	  log.debug("Request to login with sms code="+token);

	  /* moved to DevRestController
	  // in DEV or TEST environment allow login as any use with a special code
	  if (springEnv.acceptsProfiles(Profiles.of("dev", "test")) && token.equals(prop.devLoginSmsToken)) {
	  	UserModel user = userRepo.findByProfileMobilephone(mobile)
					.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_MOBILE_NOT_FOUND, "DevLogin: user for mobile phone "+mobile+" not found."));
	  	log.info("DEV Login as "+mobile+" => "+user);
	  	user.setLastLogin(LocalDateTime.now());
	  	userRepo.save(user);
			return jwtTokenProvider.generateToken(user.getEmail());
		}
		*/


	  OneTimeToken oneTimeToken = ottRepo.findByToken(token);
	  if (oneTimeToken == null || mobile == null ||
        !mobile.equals(oneTimeToken.getUser().getProfile().getMobilephone()) ||
	      !OneTimeToken.TOKEN_TYPE.SMS.equals(oneTimeToken.getTokenType())  )
	  {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "Invalid SMS login token.");
		}

	  if (LocalDateTime.now().isAfter(oneTimeToken.getValidUntil())) {
	  	ottRepo.delete(oneTimeToken);
		  throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "This sms login token is expired.");
	  }

	  //---- delete used one time token
		UserModel user = oneTimeToken.getUser();
		ottRepo.delete(oneTimeToken);

    // return JWT token for this email
		String jwt = jwtTokenProvider.generateToken(user.getEmail());
		user.setLastLogin(LocalDateTime.now());
		userRepo.save(user);
		log.info(user+ "logged in with valid SMS token.");
		return jwt;
  }


	/**
	 * Request a login token via email. When passed email is valid, ie. user is registered and known
	 * then an email with a login link will be sent to his email account.
	 * @param email must be a valid, previously registered email adress
	 * @return HTTP OK
	 * @throws LiquidoException when this email is not yet registered (or mail cannot be sent via SMTP)
	 */
	@RequestMapping(value = "/auth/requestEmailToken")
	public ResponseEntity requestEmailToken(@RequestParam("email") String email) throws LiquidoException {
		if (DoogiesUtil.isEmpty(email)) throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_EMAIL_NOT_FOUND,  "Need email!");
		log.info("request email login code for email="+email);
		UserModel user = userRepo.findByEmail(email)
			.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_EMAIL_NOT_FOUND,  "No user found with email "+email+". You must register first."));

		// Create new email login link with a token time token in it.
		UUID emailToken = UUID.randomUUID();
		LocalDateTime validUntil = LocalDateTime.now().plusHours(1);  // login token via SMS is valid for one hour. That should be enough!
		OneTimeToken oneTimeToken = new OneTimeToken(emailToken.toString(), user, OneTimeToken.TOKEN_TYPE.EMAIL, validUntil);
		ottRepo.save(oneTimeToken);
		log.info("User "+user.getEmail()+" may login. Sending code via EMail.");

		try {
			mailService.sendEMail(email, oneTimeToken.getToken());
		} catch (Exception e) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_INTERNAL_ERROR, "Internal server error: Cannot send Email "+e.toString(), e);
		}

		if (springEnv.acceptsProfiles(Profiles.of("dev", "test"))) {
			return ResponseEntity.ok().header("token", emailToken.toString()).build();  // when debugging, return the code in the header
		} else {
			return ResponseEntity.ok().build();
		}
	}

	/**
	 * Login via link in email.
	 * @param email user's email that MUST match the email stored together with the token
	 * @param token must be a valid login token
	 * @return Json Web Token as plain text
	 * @throws LiquidoException when token is invalid or expired
	 */
	@RequestMapping(path = "/auth/loginWithEmailToken", produces = MediaType.TEXT_PLAIN_VALUE)
	public String loginWithEmailToken(
		@RequestParam("email") String email,
		@RequestParam("token") String token
	) throws LiquidoException {
		log.debug("Request to login with email token="+token);
		if (DoogiesUtil.isEmpty(email)) throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_EMAIL_NOT_FOUND, "Need email to login!");
		if (DoogiesUtil.isEmpty(token)) throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "Need login token!");

		OneTimeToken oneTimeToken = ottRepo.findByToken(token);
		if (oneTimeToken == null || email == null ||
			!email.equals(oneTimeToken.getUser().getEmail()) ||
			!OneTimeToken.TOKEN_TYPE.EMAIL.equals(oneTimeToken.getTokenType())  )
		{
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "Invalid email login token.");
		}

		if (LocalDateTime.now().isAfter(oneTimeToken.getValidUntil())) {
			ottRepo.delete(oneTimeToken);
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "This email login token is expired.");
		}

		//---- delete used one time token
		UserModel user = oneTimeToken.getUser();
		ottRepo.delete(oneTimeToken);

		// return JWT token for this email
		String jwt = jwtTokenProvider.generateToken(user.getEmail());
		oneTimeToken.getUser().setLastLogin(LocalDateTime.now());
		userRepo.save(user);
		log.info(user+ "logged in with valid email code.");
		return jwt;
	}



	// There is no logout here. The server is stateless! When a user want's to "log out", then he simply has to throw away his JWT
}



