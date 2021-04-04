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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

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
		String jwt = authUtil.getCurrentJwt()
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.UNAUTHORIZED, "No JWT in request. Cannot loginWithJWT"));
		return new CreateOrJoinTeamResponse(team, user, jwt);
	}

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
	public OneTimeToken requestEmailToken(
		@GraphQLNonNull @GraphQLArgument(name="email") String email
	) throws LiquidoException {
		UserModel user = userRepo.findByEmail(email)
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.CANNOT_LOGIN_EMAIL_NOT_FOUND, "User with that email is not found."));

		// Create new email login link with a token time token in it.
		UUID emailToken = UUID.randomUUID();
		LocalDateTime validUntil = LocalDateTime.now().plusHours(props.loginLinkExpirationHours);
		OneTimeToken oneTimeToken = new OneTimeToken(emailToken.toString(), user, validUntil);
		ottRepo.save(oneTimeToken);
		log.info("User " + user.getEmail() + " may login. Sending code via EMail.");

		try {
			mailService.sendEMail(email, oneTimeToken.getToken());
		} catch (Exception e) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_INTERNAL_ERROR, "Internal server error: Cannot send Email " + e.toString(), e);
		}

		return oneTimeToken;
	}

	@GraphQLQuery(name = "loginWithEmailToken")
	public CreateOrJoinTeamResponse loginWithEmailToken(
		@GraphQLNonNull @GraphQLArgument(name="token") String token
	) throws LiquidoException {
		OneTimeToken ott = ottRepo.findByToken(token)
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "This email token is invalid!"));
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
		return verifyApiClient.requestVerificationToken(TwilioVerifyApiClient.CHANNEL_SMS, mobilephone);
	}

	@GraphQLQuery(name="loginWithAuthToken")
	public CreateOrJoinTeamResponse loginWithAuthToken(
		@GraphQLNonNull @GraphQLArgument(name="mobilephone") String mobilephone,
		@GraphQLNonNull @GraphQLArgument(name="authToken") String authToken
	) throws LiquidoException {
		UserModel user = userRepo.findByMobilephone(mobilephone)
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.CANNOT_LOGIN_MOBILE_NOT_FOUND, "Cannot login with auth token. There is no liquido user with that mobilephone!"));
		if (verifyApiClient.tokenIsValid(mobilephone, authToken)) {
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
