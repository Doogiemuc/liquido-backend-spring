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
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.LiquidoRestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
	TwilioVerifyApiClient twilioApiClient;

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
		UserModel user = authUtil.getCurrentUserFromDB()
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.UNAUTHORIZED, "JWT invalid. Cannot find user to loginWithJWT"));
		TeamModel team = authUtil.getCurrentTeamFromDB()			// This loads the team with admins and members, but NOT YET the polls.
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
		String emailLowerCase = LiquidoRestUtils.cleanEmail(email);
		UserModel user = userRepo.findByEmail(emailLowerCase)
			.orElseThrow(() -> {
				log.info("Email "+ emailLowerCase +" tried to request email token, but is not registered");
				return new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_EMAIL_NOT_FOUND, "User with that email <" + emailLowerCase + "> is not found.");
			});

		// Create new email login link with a token time token in it.
		UUID tokenUUID = UUID.randomUUID();
		LocalDateTime validUntil = LocalDateTime.now().plusHours(props.loginLinkExpirationHours);
		OneTimeToken oneTimeToken = new OneTimeToken(tokenUUID.toString(), user, validUntil);
		ottRepo.save(oneTimeToken);
		log.info("User " + user.getEmail() + " may login via email link.");

		// This link is parsed in a cypress test case. Must update test if you change this.
		String loginLink = "<a id='loginLink' style='font-size: 20pt;' href='"+props.frontendUrl+"/login?email="+user.getEmail()+"&emailToken="+oneTimeToken.getNonce()+"'>Login " + user.getName() +  "</a>";
		String body = String.join(
			System.getProperty("line.separator"),
			"<html><h1>Liquido Login Token</h1>",
			"<h3>Hello " + user.getName() + "</h3>",
			"<p>With this link you can login to Liquido.</p>",
			"<p>&nbsp;</p>",
			"<b>" + loginLink + "</b>",
			"<p>&nbsp;</p>",
			"<p>This login link can only be used once!</p>",
			"<p style='color:grey; font-size:10pt;'>You received this email, because  a login token for the <a href='https://www.liquido.net'>LIQUIDO</a> eVoting webapp was requested. If you did not request a login yourself, than you may simply ignore this message.</p>",
			"</html>"
		);

		try {
			mailService.sendEMail(user.getName(), emailLowerCase, body);
		} catch (Exception e) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_INTERNAL_ERROR, "Internal server error: Cannot send Email " + e.toString(), e);
		}
		// SECURITY: This method MUST NOT return anything! This is the whole idea of 2FA login via email.
		// The (correct) user must have access to his email. This _IS_ the second factor.
	}

	/**
	 * User clicked on the link in his email. Verify token and if valid log him in.
	 * @param email user's email that must match the email that requested the token.
	 * @param authToken one time token that must not be expired.
	 * @return Full login info
	 * @throws LiquidoException when email was not found, does not match or token is expired.
	 */
	@GraphQLQuery(name = "loginWithEmailToken")
	public CreateOrJoinTeamResponse loginWithEmailToken(
		@GraphQLNonNull @GraphQLArgument(name="email") String email,
		@GraphQLNonNull @GraphQLArgument(name="authToken") String authToken
	) throws LiquidoException {
		email = LiquidoRestUtils.cleanEmail(email);
		OneTimeToken ott = ottRepo.findByNonce(authToken)
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "This email token is invalid!"));
		if (LocalDateTime.now().isAfter(ott.getValidUntil())) {
			ottRepo.delete(ott);
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "Token is expired!");
		}
		if (!DoogiesUtil.isEqual(email, ott.getUser().getEmail()))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "This token is not valid for that email!");

		return loginUserIntoTeam(ott.getUser(), null);
	}


	/************************ LOGIN via SMS ************************/

	/**
	 * Request an auth token for login. Will send token via SMS.
	 * @param mobilephone user's mobilephone
	 * @param devLoginToken (optional) token for development login. Used during testing.
	 * @return Twilio API "SID" of the verification request
	 * @throws LiquidoException when user with that mobile is not found. Or if there is a downstream error from the authentication API
	 */
	@GraphQLQuery(name="authToken")
	public String requestAuthToken(
		@GraphQLNonNull @GraphQLArgument(name="mobilephone") String mobilephone,
		@GraphQLArgument(name="devLoginToken") Optional<String> devLoginToken   // This is used for testing the login flow
	) throws LiquidoException {
		mobilephone = LiquidoRestUtils.cleanMobilephone(mobilephone);
		UserModel user = userRepo.findByMobilephone(mobilephone)
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.CANNOT_LOGIN_MOBILE_NOT_FOUND, "Cannot request auth token. There is no user with that mobilephone!"));

		// When user passes correct devLoginToken, then do NOT send and SMS and return dummySID. This is used in testing.
		//BUGFIX: graphql-spqr passes null here: https://github.com/leangen/graphql-spqr/issues/197
		if (devLoginToken != null && devLoginToken.isPresent() && DoogiesUtil.isEqual(props.test.devLoginToken, devLoginToken.get()) &&
		    mobilephone.equals(props.testUser.mobilephone)) {
			log.debug("requestAuthToken: DevLogin: returning valid dummy token for testUser");
			return "DummyAuthToken";
		}

		try {
			return twilioApiClient.requestVerificationToken(TwilioVerifyApiClient.CHANNEL_SMS, mobilephone);
		} catch (Throwable t) {
			log.debug("Cannot request authToken: ", t.toString());
			throw new LiquidoException(LiquidoException.Errors.CANNOT_REQUEST_SMS_TOKEN, t.getMessage());
		}
	}

	/**
	 * Login with a one time token that the user has received via SMS or from an authentication application.
	 * If user is a member of several teams, then he will be logged into his last team.
	 * When authToken is the devLoginToken then the login is granted automatically. This is used in tests.
	 *
	 * @param mobilephone must be a mobilephone of an already registered user
	 * @param authToken the authentication token, eg. from the SMS
	 * @return login data when successful
	 * @throws LiquidoException when this mobilephone is not registered or last team cannot be found
	 */
	@GraphQLQuery(name="loginWithAuthToken")
	public CreateOrJoinTeamResponse loginWithAuthToken(
		@GraphQLNonNull @GraphQLArgument(name="mobilephone") String mobilephone,
		@GraphQLNonNull @GraphQLArgument(name="authToken") String authToken
		//TODO: @GraphQLArgument(name="authToken") Long teamId   // when user is member of several teams   see#changeTeam()
	) throws LiquidoException {
		mobilephone = LiquidoRestUtils.cleanMobilephone(mobilephone);
		UserModel user = userRepo.findByMobilephone(mobilephone)
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.CANNOT_LOGIN_MOBILE_NOT_FOUND, "Cannot login with auth token. There is no liquido user with that mobilephone!"));

		if (DoogiesUtil.isEqual(props.test.devLoginToken, authToken)) {
			// Allow login with devLoginToken. We do allow this in any environment, so that we can also run automated tests against prod.
			// (This will skip the Authy verifyAPI call!)
			return this.loginUserIntoTeam(user, null);
		} else if (twilioApiClient.tokenIsValid(mobilephone, authToken)) {
			return this.loginUserIntoTeam(user, null);
		} else {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TOKEN_INVALID, "Cannot login. Auth Token is invalid");
		}
	}

	/********************** Change team ************************/


	/**
	 * A user can be member (or admin) in several teams. Here he can switch and login into another team.
	 * This call must be authenticated with the JWT of the <b>old</b> team. And of course the target team must exist.
	 * User must of course be a member or admin in the target team.
	 *
	 * @param teamName name of the other team, that he want's to switch to
	 * @return Login information with new JWT for other team.
	 * @throws LiquidoException when call is not authenticated, JWT is expired, target team does not exist or user is not member or admin in target team.
	 */
	@GraphQLQuery(name="changeTeam")
	@PreAuthorize(HAS_ROLE_USER)
	public CreateOrJoinTeamResponse changeTeam(
		@GraphQLNonNull @GraphQLArgument(name="teamName") String teamName
	) throws LiquidoException {
		String jwt = authUtil.getCurrentJwt()
			.orElseThrow(LiquidoException.unauthorized("Need JWT to switch into new team"));     // this should never throw, because of @PreAuthorize() annotation
		jwtTokenUtils.validateToken(jwt);  // throws detailed LiquidoException, eg. "token expired"
		UserModel user = authUtil.getCurrentUserFromDB()
			.orElseThrow(LiquidoException.unauthorized("Need to be logged in to change team"));
		TeamModel team = teamRepo.findByTeamName(teamName)
			.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_TEAM_NOT_FOUND, "Cannot change team. Target team not found."));
		if (!team.isAdmin(user) && !team.isMember(user))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_USER_NOT_MEMBER_OF_TEAM, "Cannot change team. User is not member or admin of new team");

		return this.loginUserIntoTeam(user, team);
	}



	/**
	 * Login a user into his team.
	 * If team is not given and user is member of multiple teams, then he will be logged into the last one he was using,
	 * or otherwise the first team in his list.
	 * @param user a user that want's to log in
	 * @return CreateOrJoinTeamResponse
	 * @throws LiquidoException when user has no teams (which should never happen)
	 */
	private CreateOrJoinTeamResponse loginUserIntoTeam(@NonNull UserModel user, @Nullable TeamModel team) throws LiquidoException {
		if (team == null) {
			List<TeamModel> teams = teamRepo.teamsOfUser(user);
			if (teams.size() == 0) {
				throw new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "User is not member of any team");
			} else if (teams.size() == 1) {
				team = teams.get(0);
			} else {
				team = teamRepo.findById(user.getLastTeamId()).orElse(teams.get(0));
			}
		}

		//TODO: team.getPolls();  // Do I need to lazily fetch polls; Or is graphql-spqr even able to handle this dynamically from the client request. Test the SQL- loot of work

		log.debug("Login " + user.toStringShort() + " into team '" + team.getTeamName()+"'");
		String jwt = jwtTokenUtils.generateToken(user.getId(), team.getId());
		authUtil.authenticateInSecurityContext(user.getId(), team.getId(), jwt);
		return new CreateOrJoinTeamResponse(team, user, jwt);
	}

}
