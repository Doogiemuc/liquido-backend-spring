package org.doogie.liquido.graphql;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.jwt.AuthUtil;
import org.doogie.liquido.jwt.JwtTokenUtils;
import org.doogie.liquido.jwt.LiquidoAuthentication;
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.CreateOrJoinTeamResponse;
import org.doogie.liquido.security.TwilioVerifyApiClient;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.LiquidoException.Errors;
import org.doogie.liquido.services.UserService;
import org.doogie.liquido.util.LiquidoRestUtils;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.transaction.Transactional;
import java.util.Optional;

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
	TwilioVerifyApiClient twilio;

	@Autowired
	AuthUtil authUtil;

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
		return team;
	}

	// small side note: The loginWithJWT, createTeam or joinTeam requests all return exactly the same response format! I like!


	/**
	 * Create a new team. The first user will become the admin of the team.
	 * This can be called anonymously.
	 *
	 * @param teamName Name of new team. Must be unique.
	 * @param admin Admin of new team
	 * @return The newly created team, incl. ID, inviteCode  and a JsonWebToken
	 * @throws LiquidoException when teamName is not unique. Or user with same email exists.
	 */
	@Transactional
	@GraphQLMutation(name = "createNewTeam", description = "Create a new team")
	public CreateOrJoinTeamResponse createNewTeam(
		@GraphQLArgument(name = "teamName") @GraphQLNonNull String teamName,
		@GraphQLArgument(name = "adminName") @GraphQLNonNull String adminName,
		@GraphQLArgument(name = "adminEmail") @GraphQLNonNull String adminEmail,
		@GraphQLArgument(name = "adminMobilephone") @GraphQLNonNull String adminMobilephone,
		@GraphQLArgument(name = "adminWebsite") String adminWebsite,
		@GraphQLArgument(name = "adminPicture") String adminPicture
	) throws LiquidoException {
		adminMobilephone = LiquidoRestUtils.cleanMobilephone(adminMobilephone);
		Optional<LiquidoAuthentication> auth = authUtil.getLiquidoAuthentication();
		Optional<UserModel> existingUser = userRepo.findByEmail(adminEmail);

		// IF team with same name exist, then throw error
		if (teamRepo.findByTeamName(teamName).isPresent())
			throw new LiquidoException(Errors.TEAM_WITH_SAME_NAME_EXISTS, "Cannot create new team: A team with that name ('"+teamName+"') already exists");

		//TODO: write a test for this

		//                | user with same | no other user
		//                | email exists   | with same e-mail
		// --------------:|----------------|----------------------------------
		// not authorized | NO             | OK
		//     authorized | Ok when match  | Register a 2nd time with new email? Or forward to login?

		if (!auth.isPresent()) {              // Anonymous request
			if (existingUser.isPresent()) {     // and user with same e-mail exists, THEN throw
				throw new LiquidoException(Errors.USER_EMAIL_EXISTS, "Another user with that email already exists");
			}
		} else {         // authorized but with different ID than existing email, THEN throw
			if (existingUser.isPresent() && !ObjectUtils.nullSafeEquals(auth.get().getUserId(), existingUser.get().getId())) {
				throw new LiquidoException(Errors.USER_EMAIL_EXISTS, "Another user with that email already exists");
			}
		}

		UserModel admin = existingUser.orElse(new UserModel(adminEmail, adminName, adminMobilephone, adminWebsite, adminPicture));
		TeamModel newTeam = new TeamModel(teamName, admin);
		newTeam = teamRepo.save(newTeam);
		userRepo.save(admin);

		log.info("Created new team: "+newTeam);
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
		//TODO: group this into one argument of type UserModel: https://graphql-rules.com/rules/input-grouping
		@GraphQLArgument(name = "userName") @GraphQLNonNull String userName,
		@GraphQLArgument(name = "userEmail") @GraphQLNonNull String userEmail,
		@GraphQLArgument(name = "mobilephone") String mobilephone,
		@GraphQLArgument(name = "website") String website,
		@GraphQLArgument(name = "picture") String picture
	) throws LiquidoException {
		TeamModel team = teamRepo.findByInviteCode(inviteCode).orElseThrow(
			() -> new LiquidoException(Errors.CANNOT_JOIN_TEAM_INVITE_CODE_INVALID, "Invalid inviteCode '"+inviteCode+"'")
		);
		mobilephone = LiquidoRestUtils.cleanMobilephone(mobilephone);
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
			throw new LiquidoException(Errors.INTERNAL_ERROR, "Error: Cannot join team.", e);
		}
	}


}
