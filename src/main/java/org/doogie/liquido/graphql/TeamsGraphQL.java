package org.doogie.liquido.graphql;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.jwt.AuthUtil;
import org.doogie.liquido.jwt.JwtTokenUtils;
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.CreateOrJoinTeamResponse;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.LiquidoException.Errors;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.LiquidoRestUtils;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

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
	JwtTokenUtils jwtTokenUtils;

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
	 * @param admin The user that sends the request will become the admin of the new team.
	 * @return The newly created team, incl. ID, inviteCode  and a JsonWebToken
	 * @throws LiquidoException when teamName is not unique. Or user with same email exists.
	 */
	@Transactional
	@GraphQLMutation(name = "createNewTeam", description = "Create a new team")
	public CreateOrJoinTeamResponse createNewTeam(
		@GraphQLArgument(name = "teamName") @GraphQLNonNull String teamName,
		@GraphQLArgument(name = "admin") @GraphQLNonNull UserModel admin
	) throws LiquidoException {
		admin.setMobilephone(LiquidoRestUtils.cleanMobilephone(admin.mobilephone));

		// IF team with same name exist, then throw error
		if (teamRepo.findByTeamName(teamName).isPresent())
			throw new LiquidoException(Errors.TEAM_WITH_SAME_NAME_EXISTS, "Cannot create new team: A team with that name ('"+teamName+"') already exists");

		/*
		  IF request is authorized
		    IF "admin" does NOT exactly have the same attributes as the logged in user THEN throw: not allowed.
		  ELSE // anonymous request
		    IF user with same email exists, THEN throw
		    IF user with same mobilephone exits, THEN throw
		 */
		Optional<UserModel> currentUserOpt = authUtil.getCurrentUser();
		if (currentUserOpt.isPresent()) {
			// IF user is logged and then he CAN create a new team, but he MUST provide his already registered email and mobilephone.
			if (!DoogiesUtil.isEqual(currentUserOpt.get().email, admin.email) ||
					!DoogiesUtil.isEqual(currentUserOpt.get().mobilephone, admin.mobilephone)) {
				throw new LiquidoException(Errors.USER_EMAIL_EXISTS, "Your are already registered. You must provide your user data for the admin of the new team!");
			}
			admin = currentUserOpt.get();  // with db ID!
		} else {
			// Anonymous request. Must provide new email and mobilephone
			Optional<UserModel> userByMail = userRepo.findByEmail(admin.email);
			if (userByMail.isPresent()) throw new LiquidoException(Errors.USER_EMAIL_EXISTS, "Sorry, another user with that email already exists.");
			Optional<UserModel> userByMobilephone = userRepo.findByEmail(admin.getMobilephone());
			if (userByMobilephone.isPresent()) throw new LiquidoException(Errors.USER_MOBILEPHONE_EXISTS, "Sorry, another user with that mobile phone number already exists.");
		}

		TeamModel newTeam = new TeamModel(teamName, admin);
		newTeam = teamRepo.save(newTeam);
		userRepo.save(admin);
		log.info("Created new team: "+newTeam);
		String jwt = jwtTokenUtils.generateToken(admin.getId(), newTeam.getId());
		return new CreateOrJoinTeamResponse(newTeam, admin, jwt);
	}

	/**
	 * Join an existing team as a member.
	 *
	 * This should be called anonymously. Then the new member <b>must</b> register with a an email and mobilephone that does not exit in LIQUIDO yet.
	 * When called with JWT, then the already registered user may join this additional team. But he must exactly provide his user data.
	 *
	 * Will also throw an error, when email is already admin or member in that team.
	 *
	 * TODO: Joining more than once is idempotent. Or in simpler words: It is ok to click the invite link more than once :-)
	 *
	 * @param inviteCode valid invite code of the team to join
	 * @param member new user member, with email and mobilephone
	 * @return Info about the joined team and a JsonWebToken
	 * @throws LiquidoException when inviteCode is invalid, or when this email is already admin or member in team.
	 */
	@Transactional
	@GraphQLMutation(name = "joinTeam", description = "Join an existing team")
	public CreateOrJoinTeamResponse joinTeam(
		@GraphQLArgument(name = "inviteCode") @GraphQLNonNull String inviteCode,
		@GraphQLArgument(name = "member") @GraphQLNonNull UserModel member  //grouped as one argument of type UserModel: https://graphql-rules.com/rules/input-grouping
	) throws LiquidoException {
		member.setMobilephone(LiquidoRestUtils.cleanMobilephone(member.mobilephone));
		TeamModel team = teamRepo.findByInviteCode(inviteCode)
			.orElseThrow(LiquidoException.supply(Errors.CANNOT_JOIN_TEAM_INVITE_CODE_INVALID, "Invalid inviteCode '"+inviteCode+"'"));

		Optional<UserModel> currentUserOpt = authUtil.getCurrentUser();
		if (currentUserOpt.isPresent()) {
			// IF user is logged and then he CAN join another team, but he MUST provide his already registered email and mobilephone.
			if (!DoogiesUtil.isEqual(currentUserOpt.get().email, member.email) ||
			    !DoogiesUtil.isEqual(currentUserOpt.get().mobilephone, member.mobilephone)) {
				throw new LiquidoException(Errors.USER_EMAIL_EXISTS, "Your are already registered. You must provide your email and mobilephone to join another team!");
			}
			member = currentUserOpt.get();  // with db ID!
		} else {
			// Anonymous request. Must provide new email and mobilephone
			Optional<UserModel> userByMail = userRepo.findByEmail(member.email);
			if (userByMail.isPresent()) throw new LiquidoException(Errors.USER_EMAIL_EXISTS, "Sorry, another user with that email already exists.");
			Optional<UserModel> userByMobilephone = userRepo.findByEmail(member.mobilephone);
			if (userByMobilephone.isPresent()) throw new LiquidoException(Errors.USER_MOBILEPHONE_EXISTS, "Sorry, another user with that mobile phone number already exists.");
		}

		try {
			team.getMembers().add(member);   // Add to java.util.Set. Will never add duplicate.
			member = userRepo.save(member);
			team = teamRepo.save(team);
			log.info("User <" + member.email + "> joined team: " + team.toString());
			String jwt = jwtTokenUtils.generateToken(member.getId(), team.getId());
			return new CreateOrJoinTeamResponse(team, member, jwt);
		} catch (Exception e) {
			throw new LiquidoException(Errors.INTERNAL_ERROR, "Error: Cannot join team.", e);
		}
	}


}
