package org.doogie.liquido.graphql;

import io.leangen.graphql.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.OneTimeTokenRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.jwt.AuthUtil;
import org.doogie.liquido.jwt.JwtTokenUtils;
import org.doogie.liquido.jwt.LiquidoAuthentication;
import org.doogie.liquido.model.OneTimeToken;
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.CreateOrJoinTeamResponse;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.LiquidoException.Errors;
import org.doogie.liquido.services.MailService;
import org.doogie.liquido.services.UserService;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.doogie.liquido.util.Lson;
import org.hibernate.cfg.NotYetImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StreamUtils;

import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.doogie.liquido.jwt.AuthUtil.HAS_ROLE_USER;

/**
 * Create a new Team, join an existing team, get information about existing team and its members.
 * This service is directly exposed as GraphQL.
 */
@Slf4j
@Service
public class TeamsGraphQL {

	@Autowired
	TeamRepo teamRepo;

	@Autowired
	UserRepo userRepo;

	@Autowired
	UserService userService;

	@Autowired
	JwtTokenUtils jwtTokenUtils;

	@Autowired
	PollRepo pollRepo;

	@Autowired
	OneTimeTokenRepo ottRepo;

	@Autowired
	MailService mailService;

	@Autowired
	AuthUtil authUtil;

	@Autowired
	LiquidoProperties props;


	/**
	 * Check if backend is available at all
	 * @return some hello world json if everything is ok
	 */
	@GraphQLQuery(name = "ping")
	public Lson pingApi() {
		return Lson.builder("api", "LIQUIDO API is available");
	}

	/**
	 * Get information about user's own team, including the team's polls.
	 * @return info about user's own team.
	 * @throws LiquidoException
	 */
	@PreAuthorize(HAS_ROLE_USER)
	@GraphQLQuery(name = "team")
	@Transactional
	public TeamModel getOwnTeam() throws LiquidoException {
		TeamModel team = authUtil.getCurrentTeam()
			.orElseThrow(LiquidoException.supply(Errors.UNAUTHORIZED, "Cannot get team. User must be logged into a team!"));

		//TODO:  Do I need to call  team.getPolls() here?   Can I do this depending on what the client requested in his GraphQL query? That would be cool!

		return team;
	}

	// The loginWithJWT, createTeam or joinTeam requests all return exactly the same response format!

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
			.orElseThrow(LiquidoException.supply(Errors.UNAUTHORIZED, "JWT invalid. Cannot find user to loginWithJWT"));
		TeamModel team = authUtil.getCurrentTeam()			// This loads the team with admins and members, but NOT YET the polls.
			.orElseThrow(LiquidoException.supply(Errors.UNAUTHORIZED, "JWT invalid. Cannot get team to loginWithJWT."));
		String jwt = authUtil.getCurrentJwt()
			.orElseThrow(LiquidoException.supply(Errors.UNAUTHORIZED, "No JWT in request. Cannot loginWithJWT"));
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
			.orElseThrow(LiquidoException.supply(Errors.CANNOT_LOGIN_EMAIL_NOT_FOUND, "User with that email is not found."));

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
			.orElseThrow(LiquidoException.supply(Errors.CANNOT_LOGIN_TOKEN_INVALID, "This email token is invalid!"));
		UserModel user = ott.getUser();

		// find all teams that this user is a member or admin in.
		List<TeamModel> teams = null; // teamRepo.findByMembersIdEqualsOrAdminsIdEquals(user.getId());

		if (teams.size() == 0) {
			throw new LiquidoException(Errors.CANNOT_LOGIN_TOKEN_INVALID, "User (in token) is not member of any team");
		} else {
			String jwt = jwtTokenUtils.generateToken(user.getId(), teams.get(0).getId());
			return new CreateOrJoinTeamResponse(teams.get(0), user, jwt);
		}
	}

	/**
	 * Create a new team. The first user will become the admin of the team.
	 * This can be called anonymously.
	 *
	 * @param teamName Name of new team. Must be unique.
	 * @param adminName Admin's name
	 * @param adminEmail email of admin. (Must not be unique. One email MAY be the admin of several teams.)
	 * @return The newly created team, incl. ID, inviteCode  and a JsonWebToken
	 * @throws LiquidoException when teamName is not unique.
	 */
	@Transactional
	@GraphQLMutation(name = "createNewTeam", description = "Create a new team")
	public CreateOrJoinTeamResponse createNewTeam(
		@GraphQLArgument(name = "teamName") @GraphQLNonNull String teamName,
		@GraphQLArgument(name = "adminName") @GraphQLNonNull String adminName,
		@GraphQLArgument(name = "adminEmail") @GraphQLNonNull String adminEmail,
		@GraphQLArgument(name = "mobilephone") String mobilephone,		//TODO: user needs mobilephone for Authy
		@GraphQLArgument(name = "website") String website,
		@GraphQLArgument(name = "picture") String picture
	) throws LiquidoException {
		Optional<LiquidoAuthentication> auth = authUtil.getLiquidoAuthentication();
		Optional<UserModel> existingUser = userRepo.findByEmail(adminEmail);

		if (auth.isPresent()) {
			if (!existingUser.isPresent() || !ObjectUtils.nullSafeEquals(auth.get().getUserId(), existingUser.get().getId()))
				throw new LiquidoException(Errors.INTERNAL_ERROR, "Something is wrong.");   // this should never happen
		}
		if (teamRepo.findByTeamName(teamName).isPresent())
			throw new LiquidoException(Errors.TEAM_WITH_SAME_NAME_EXISTS, "Cannot create new team: A team with that name ('"+teamName+"') already exists");

		UserModel admin = existingUser.orElse(new UserModel(adminEmail, adminName, mobilephone, website, picture));
		//TODO: also register user at twilio. This Needs a mobilephone! userService.registerUser(admin);
		TeamModel newTeam = new TeamModel(teamName, admin);
		newTeam = teamRepo.save(newTeam);
		userRepo.save(admin);
		log.info("Created new team: "+newTeam.toString());
		String jwt = jwtTokenUtils.generateToken(admin.getId(), newTeam.getId());
		return new CreateOrJoinTeamResponse(newTeam, admin, jwt);
	}

	/**
	 * Join an existing team.
	 *
	 * When called anonymously, then the email to register must not yet exist in LIQUIDO. User email is globaly unique.
	 * An existing user can call this, but then the call must be authenticated with his JWT.
	 *
	 * Will also throw an error, when email is already admin or member in that team.
	 *
	 * Joining more than once is idempotent. Or in simpler words: It is ok to click the invite link more than once :-)
	 *
	 * @param inviteCode valid invite code of the team to join
	 * @param userName new user name
	 * @param userEmail new user's email
	 * @return Info about the joined team and a JsonWebToken
	 * @throws LiquidoException when inviteCode is invalid
	 */
	@Transactional
	@GraphQLMutation(name = "joinTeam", description = "Join an existing team")
	public CreateOrJoinTeamResponse joinTeam(
		@GraphQLArgument(name = "inviteCode") @GraphQLNonNull String inviteCode,
		@GraphQLArgument(name = "userName") @GraphQLNonNull String userName,
		@GraphQLArgument(name = "userEmail") @GraphQLNonNull String userEmail,
		@GraphQLArgument(name = "mobilephone") String mobilephone,
		@GraphQLArgument(name = "website") String website,
		@GraphQLArgument(name = "picture") String picture
	) throws LiquidoException {
		TeamModel team = teamRepo.findByInviteCode(inviteCode).orElseThrow(
			() -> new LiquidoException(Errors.CANNOT_JOIN_TEAM, "Invalid inviteCode '"+inviteCode+"'")
		);

		Optional<LiquidoAuthentication> auth = authUtil.getLiquidoAuthentication();
		Optional<UserModel> existingUser = userRepo.findByEmail(userEmail);

		// if call is correctly authenticated with a JWT, then be nice.
		if (auth.isPresent()) {
			if (!existingUser.isPresent() || !ObjectUtils.nullSafeEquals(auth.get().getUserId(), existingUser.get().getId()))
				throw new LiquidoException(Errors.INTERNAL_ERROR, "Something is wrong.");   // this should never happen!

			// IF email is already admin/member of the team and the call is correctly authenticated, THEN allow idempotent joinTeam call
			// Let users use their join team link als login link when they've authenticated before
			Optional<UserModel> userOpt = team.getAdminOrMemberByEmail(userEmail);
			if (userOpt.isPresent())
				return new CreateOrJoinTeamResponse(team, userOpt.get(), auth.get().getJwt());
		} else {
			// If not authenticated, but user with that email already exists, then user must login first, to join another team.
			if (existingUser.isPresent())
				throw new LiquidoException(Errors.USER_EMAIL_EXISTS, "There is already a LIQUIDO user with that email address! Please login first to join another team");
		}

		// If a user with that email is already a member or admin in that team, then throw error.
		// We cannot upsert the existing user with an anonymous call. This would allow a security attack.
		if (team.isMember(userEmail))
			throw new LiquidoException(Errors.CANNOT_JOIN_TEAM_ALREADY_MEMBER, "There is already a member with this email address in the team! Please login.");
		if (team.isAdmin(userEmail))
			throw new LiquidoException(Errors.CANNOT_JOIN_TEAM_ALREADY_ADMIN, "You already are admin of this team! Please login.");

		// Upsert user and add to new team
		UserModel newUser = existingUser.orElse(new UserModel(userEmail, userName, mobilephone, website, picture));
		try {
			team.getMembers().add(newUser);
			userRepo.save(newUser);
			team = teamRepo.save(team);
			log.info("User <" + userEmail + "> joined team: " + team.toString());
			String jwt = jwtTokenUtils.generateToken(newUser.getId(), team.getId());
			return new CreateOrJoinTeamResponse(team, newUser, jwt);
		} catch (Exception e) {
			throw new LiquidoException(Errors.CANNOT_JOIN_TEAM, "Error: Cannot join team.", e);
		}
	}


}
