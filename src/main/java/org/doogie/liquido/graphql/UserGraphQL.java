package org.doogie.liquido.graphql;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.OneTimeTokenRepo;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.jwt.AuthUtil;
import org.doogie.liquido.jwt.JwtTokenUtils;
import org.doogie.liquido.model.OneTimeToken;
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.CreateOrJoinTeamResponse;
import org.doogie.liquido.security.TwilioVerifyApiClient;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.MailService;
import org.doogie.liquido.services.UserService;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.doogie.liquido.testdata.TestFixtures;
import org.doogie.liquido.util.DoogiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.doogie.liquido.jwt.AuthUtil.HAS_ROLE_USER;

/**
 * GraphQL endpoint for everything around a User
 * register, login, logout, authy/twilio authTokens
 */
@Slf4j
@Service
public class UserGraphQL {

	@Autowired
	AuthUtil authUtil;

	@Autowired
	UserRepo userRepo;

	@Autowired
	TeamRepo teamRepo;

	@Autowired
	LiquidoProperties props;

	@Autowired
	OneTimeTokenRepo ottRepo;

	@Autowired
	MailService mailService;

	@Autowired
	JwtTokenUtils jwtTokenUtils;

	@Autowired
	UserService userService;

	@Autowired
	TwilioVerifyApiClient verifyApiClient;

	@Autowired
	Environment env;

	/**
	 * When the client already has a JWT, then he can query for the team and user details.
	 * @return {@link CreateOrJoinTeamResponse} - the same object as a create or join team query returns.
	 * @throws LiquidoException when JWT is invalid
	 */
	@PreAuthorize(HAS_ROLE_USER)					// Must be authenticated call! Client must have sent JWT in header!
	@GraphQLQuery(name = "loginWithJwt")  // camelCaseJwt ! not "...JWT"
	@Transactional
	public CreateOrJoinTeamResponse loginWithJwt(/*@RequestHeader(name="Authorization") String token*/) throws LiquidoException {
		UserModel user = authUtil.getCurrentUser()
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.UNAUTHORIZED, "JWT invalid. Cannot find user to loginWithJWT"));
		TeamModel team = authUtil.getCurrentTeam()			// This loads the team with admins and members, but NOT YET the polls.
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.UNAUTHORIZED, "JWT invalid. Cannot get team to loginWithJWT."));
		String jwt = jwtTokenUtils.generateToken(user.getId(), team.getId());  // refresh JWT
		return new CreateOrJoinTeamResponse(team, user, jwt);
	}

	/********************** LOGIN via Email ************************/

	/**
	 * User requests his login link via email.
	 * A magic link will be sent to the user's email. With this link the user can login.
	 * If user is member (or admin) in several teams, then client will show a chooser when user clicks on this link.
	 * The link is valid for n hours as configured in liquido.loginLinkExpirationHours
	 * @param email user's email
	 * @return the oneTimeToken object
	 * @throws LiquidoException when email is not invalid or not member of team
	 */
	@GraphQLQuery(name="requestEmailToken")
	public void requestEmailToken(
		@GraphQLNonNull @GraphQLArgument(name="email") String email
	) throws LiquidoException {
		UserModel user = userRepo.findByEmail(email)
			.orElseThrow(() -> {
				log.info("Email "+email+" tried to request email token, but is not registered");  // This needs to be logged on level INFO. (Not warn, but info.)
				return new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_EMAIL_NOT_FOUND, "User with that email is not found.");
			});

		// Create new email login link with a token time token in it.
		UUID tokenUUID = UUID.randomUUID();
		LocalDateTime validUntil = LocalDateTime.now().plusHours(props.loginLinkExpirationHours);
		OneTimeToken oneTimeToken = new OneTimeToken(tokenUUID.toString(), user, validUntil);
		ottRepo.save(oneTimeToken);
		log.info("User " + user.getEmail() + " may login. SendingL code via EMail.");

		try {
			mailService.sendEMail(email, oneTimeToken.getNonce());
		} catch (Exception e) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_INTERNAL_ERROR, "Internal server error: Cannot send Email " + e.toString(), e);
		}
		// SECURITY: This method MUST NOT return anything! This is the whole idea of 2FA login via email.
		// The (correct) user must have access to this email. This _IS_ the second factor.
	}

	/**
	 * User clicked on the link in his email. Verify token and if valid log him in.
	 * @param email user's email that must match the email that requested the token.
	 * @param token one time token that must not be expired.
	 * @return Full login info
	 * @throws LiquidoException when email was not found, does not match or token is expired.
	 */
	@GraphQLQuery(name = "loginWithEmailToken")
	public CreateOrJoinTeamResponse loginWithEmailToken(
		@GraphQLNonNull @GraphQLArgument(name="email") String email,
		@GraphQLNonNull @GraphQLArgument(name="token") String token
	) throws LiquidoException {
		OneTimeToken ott = ottRepo.findByNonce(token)
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "This email token is invalid!"));
		if (LocalDateTime.now().isAfter(ott.getValidUntil())) {
			ottRepo.delete(ott);
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "Token is expired!");
		}
		if (!ObjectUtils.nullSafeEquals(email, ott.getUser().getEmail()))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "This token is not valid for that email!");

		return loginUserIntoTeam(ott.getUser());
	}


	/************************ LOGIN via SMS ************************/

	/**
	 * Request an auth token for login. Will send token via SMS.
	 * @param mobilephone user's mobilephone
	 * @return Twilio API "SID" of the verification request
	 * @throws LiquidoException when there is an error from the authentication API
	 */
	@GraphQLQuery(name="authToken")
	public String requestAuthToken(
		@GraphQLNonNull @GraphQLArgument(name="mobilephone") String mobilephone
	) throws LiquidoException {
		UserModel user = userRepo.findByMobilephone(mobilephone)
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.CANNOT_LOGIN_MOBILE_NOT_FOUND, "Cannot request auth token. There is no liquido user with that mobilephone!"));

		// This is used in testing.
		//TODO: should I require devLoginToken for requesting an SMS authToken?
		if (TestFixtures.TEAM1_ADMIN_MOBILEPHONE.equals(mobilephone)) return "DummySID";

		return verifyApiClient.requestVerificationToken(TwilioVerifyApiClient.CHANNEL_SMS, mobilephone);
	}

	/**
	 * Login with a token that has been sent via SMS or entered from an authentication application.
	 * @param mobilephone must be a mobilephone of an already registered user
	 * @param authToken the authentication token, eg. from the SMS
	 * @return login data when successful
	 * @throws LiquidoException when this mobilephone is not registered
	 */
	@GraphQLQuery(name="loginWithAuthToken")
	public CreateOrJoinTeamResponse loginWithAuthToken(
		@GraphQLNonNull @GraphQLArgument(name="mobilephone") String mobilephone,
		@GraphQLNonNull @GraphQLArgument(name="authToken") String authToken
	) throws LiquidoException {
		UserModel user = userRepo.findByMobilephone(mobilephone)
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.CANNOT_LOGIN_MOBILE_NOT_FOUND, "Cannot login with auth token. There is no liquido user with that mobilephone!"));
		if (DoogiesUtil.isEqual(props.test.devLoginToken, authToken)) {
			// Allow login with devLoginToken. We do allow this in any environment, so that we can also run automated tests against prod.
			// (This will skip the Authy verifyAPI call!)
			return this.loginUserIntoTeam(user);
		} else if (verifyApiClient.tokenIsValid(mobilephone, authToken)) {
			return this.loginUserIntoTeam(user);
		} else {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "Cannot login. Auth Token is invalid");
		}
	}

	/**
	 * Login a user into his team.
	 * If user is member of multiple teams, then he will be logged into the last one he was using,
	 * or otherwise the first team in the list.
	 * @param user a user that want's to log in
	 * @return CreateOrJoinTeamResponse
	 * @throws LiquidoException when user has no teams (which should never happen)
	 */
	public CreateOrJoinTeamResponse loginUserIntoTeam(@NonNull UserModel user) throws LiquidoException {
		// find all teams that this user is a member or admin in.
		List<TeamModel> teams = teamRepo.teamsOfUser(user);
		if (teams.size() == 0) {
			throw new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "User is not member of any team");
		} else if (teams.size() == 1) {
			String jwt = jwtTokenUtils.generateToken(user.getId(), teams.get(0).getId());
			return new CreateOrJoinTeamResponse(teams.get(0), user, jwt);
		} else {
			TeamModel team = teamRepo.findById(user.getLastTeamId()).orElse(teams.get(0));
			String jwt = jwtTokenUtils.generateToken(user.getId(), team.getId());
			return new CreateOrJoinTeamResponse(team, user, jwt);
		}
	}

}
