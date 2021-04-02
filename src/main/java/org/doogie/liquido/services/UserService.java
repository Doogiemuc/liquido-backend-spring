package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.jwt.JwtTokenUtils;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.TwilioAuthyClient;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.LiquidoRestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

	@Autowired
	UserRepo userRepo;

	@Autowired
	TwilioAuthyClient twilio;

	@Autowired
	LiquidoProperties prop;

	@Autowired
	JwtTokenUtils jwtTokenUtils;

	/**
	 * Create an new user.
	 * @param newUser MUST at least contain an e-mail adress and a mobilephone
	 * @return UserModel with authyId from twilio.com
	 * @throws LiquidoException when some data is missing or registration at twilio fails.
	 */
	public UserModel registerUser(UserModel newUser) throws LiquidoException {
		//----- sanity checks
		if (DoogiesUtil.hasText(newUser.getEmail())) throw new LiquidoException(LiquidoException.Errors.CANNOT_REGISTER_NEED_EMAIL, "Need email for new user");
		if (DoogiesUtil.hasText(newUser.getMobilephone())) throw new LiquidoException(LiquidoException.Errors.CANNOT_REGISTER_NEED_MOBILEPHONE, "Need mobile phone for new user");
		Optional<UserModel> existingByEmail = userRepo.findByEmail(newUser.getEmail());
		if (existingByEmail.isPresent()) throw new LiquidoException(LiquidoException.Errors.USER_EMAIL_EXISTS, "User with that email already exists");
		Optional<UserModel> existingByMobile = userRepo.findByMobilephone(newUser.getMobilephone());
		if (existingByMobile.isPresent()) throw new LiquidoException(LiquidoException.Errors.USER_MOBILEPHONE_EXISTS, "User with that mobile phone number already exists");

		//----- save new user and register him at TWILIO.com  (mobile phone number is automatically cleaned in UserProfileModel.java)
		try {
			long authyId = twilio.createTwilioUser(newUser.getEmail(), newUser.getMobilephone(), "49");
			newUser.setAuthyId(authyId);
			return userRepo.save(newUser);
		} catch (Exception e) {
			log.error("Cannot create twilio user", e);
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_TWILIO_USER, "Cannot create twilio user", e);
		}
	}

	/**
	 * Send push authentication request via Twilio
	 * @param mobilephone MUST be an existing user's mobilephone
	 * @throws LiquidoException when no user with that mobilephone exists
	 * @return
	 */
	public String requestPushAuthentication(String mobilephone) throws LiquidoException {
		if (DoogiesUtil.hasText(mobilephone)) throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_MOBILE_NOT_FOUND,  "Need mobile phone number!");
		final String cleanMobile = LiquidoRestUtils.cleanMobilephone(mobilephone);
		UserModel user = userRepo.findByMobilephone(cleanMobile)
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_MOBILE_NOT_FOUND,  "No user found with mobile number "+cleanMobile+" in LIQUIDO. You must register first."));
		return twilio.sendSmsOrPushNotification(user.getAuthyId());
	}

	/**
	 * Verify a timed one time password (TOTP) that the user entered from his AUTHY app from his mobile phone
	 * @param mobile user's mobile phone number
	 * @param authyToken the 6-digit OTP that the user has entered
	 * @return JsonWebToken when OTP was valid
	 * @throws LiquidoException when no user with that mobile phone is found or OTP was invalid
	 */
	public UserModel verifyOneTimePassword(String mobile, String authyToken) throws LiquidoException {
		if (DoogiesUtil.hasText(mobile)) throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_MOBILE_NOT_FOUND,  "Need mobile phone number!");
		final String cleanMobile = LiquidoRestUtils.cleanMobilephone(mobile);
		UserModel user = userRepo.findByMobilephone(cleanMobile)
				.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_MOBILE_NOT_FOUND,  "No user found with mobile number "+cleanMobile+". You must register first."));

		// The admin, and only the admin is allowed to login with a secret static devLoginToken
		if (DoogiesUtil.equals(prop.devLoginToken, authyToken) &&
				DoogiesUtil.equals(prop.admin.mobilephone, mobile) &&
				DoogiesUtil.equals(prop.admin.email, user.getEmail()) &&
				DoogiesUtil.equals(prop.admin.name, user.getName())
		) {
			log.info("[DEV] Admin login as "+user);
		} else {
			//----- verify Authy OTP at twilio.com
			twilio.verifyOneTimePassword(user.getAuthyId(), authyToken);  // may throw LiquidoException
			log.info("User logged in with OTP: "+user.toStringShort());
		}

		user.setLastLogin(LocalDateTime.now());
		userRepo.save(user);
		return user;
	}
}
