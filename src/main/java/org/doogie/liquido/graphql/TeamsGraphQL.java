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
	 * Check if GraphQL part of backend is available.
	 * @see org.doogie.liquido.rest.PingController  for an is-alive ping via HTTP REST
	 * @return some dummy hello world json if everything is ok
	 */
	@GraphQLQuery(name = "ping")
	public Lson graphQLPing() {
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
		TeamModel team = authUtil.getCurrentTeamFromDB()
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
		admin.setEmail(LiquidoRestUtils.cleanEmail(admin.email));

		// IF team with same name exist, then throw error
		if (teamRepo.findByTeamName(teamName).isPresent())
			throw new LiquidoException(Errors.TEAM_WITH_SAME_NAME_EXISTS, "Cannot create new team: A team with that name ('"+teamName+"') already exists");


		Optional<UserModel> currentUserOpt = authUtil.getCurrentUserFromDB();
		boolean emailExists = userRepo.findByEmail(admin.email).isPresent();
		boolean mobilePhoneExists = userRepo.findByMobilephone(admin.mobilephone).isPresent();

		if (!currentUserOpt.isPresent()) {
			/* GIVEN an anonymous request (this is what normally happens when a new team is created)
				 WHEN anonymous user wants to create a new team
					AND another user with that email or mobile-phone already exists,
				 THEN throw an error   */
			if (emailExists) throw new LiquidoException(Errors.USER_EMAIL_EXISTS, "Sorry, another user with that email already exists.");
			if (mobilePhoneExists) throw new LiquidoException(Errors.USER_MOBILEPHONE_EXISTS, "Sorry, another user with that mobile phone number already exists.");
		} else {
			/* GIVEN an authenticated request
				  WHEN an already registered user wants to create a another new team
				   AND he does NOT provide his already registered email and mobile-phone
			  	 AND he does also NOT provide a completely new email and mobilephone
	        THEN throw an error */
			boolean providedOwnData = DoogiesUtil.isEqual(currentUserOpt.get().email, admin.email) && DoogiesUtil.isEqual(currentUserOpt.get().mobilephone, admin.mobilephone);
			if (!providedOwnData &&	(emailExists || mobilePhoneExists)) {
				throw new LiquidoException(Errors.CANNOT_CREATE_TEAM_ALREADY_REGISTERED,
					"Your are already registered as " + currentUserOpt.get().email + ". You must provide your existing user data or a new email and new mobile phone for the admin of the new team!");
			} else {
				admin = currentUserOpt.get();  // with db ID!
			}
		}

		TeamModel newTeam = new TeamModel(teamName, admin);
		newTeam = teamRepo.save(newTeam);
		userRepo.save(admin);
		log.info("Created new team: "+newTeam);
		String jwt = jwtTokenUtils.generateToken(admin.getId(), newTeam.getId());
		return new CreateOrJoinTeamResponse(newTeam, admin, jwt);
	}

	/**
	 * Get information about team by inviteCode
	 * @param inviteCode a valid InviteCode
	 * @return TeamModel or nothing if InviteCode is invalid
	 */
	@GraphQLQuery(name = "getTeamForInviteCode")
	public Optional<TeamModel> getTeamForInviteCode(
		@GraphQLArgument(name = "inviteCode") String inviteCode
	) {
		Optional<TeamModel> team = teamRepo.findByInviteCode(inviteCode);
		return team;
	}


	/**
	 * Join an existing team as a member.
	 *
	 * This should be called anonymously. Then the new member <b>must</b> register with a an email and mobilephone that does not exit in LIQUIDO yet.
	 * When called with JWT, then the already registered user may join this additional team. But he must exactly provide his user data.
	 * Will also throw an error, when email is already admin or member in that team.
	 *
	 * After returning from this method, the user will be logged in.
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
		member.setEmail(LiquidoRestUtils.cleanEmail(member.email));
		TeamModel team = teamRepo.findByInviteCode(inviteCode)
			.orElseThrow(LiquidoException.supply(Errors.CANNOT_JOIN_TEAM_INVITE_CODE_INVALID, "Invalid inviteCode '"+inviteCode+"'"));

		//TODO: make it configurable so that join team requests must be confirmed by an admin first.

		Optional<UserModel> currentUserOpt = authUtil.getCurrentUserFromDB();
		if (currentUserOpt.isPresent()) {
			// IF user is already logged in, then he CAN join another team, but he MUST provide his already registered email and mobilephone.
			if (!DoogiesUtil.isEqual(currentUserOpt.get().email, member.email) ||
			    !DoogiesUtil.isEqual(currentUserOpt.get().mobilephone, member.mobilephone)) {
				throw new LiquidoException(Errors.USER_EMAIL_EXISTS, "Your are already registered. You must provide your email and mobilephone to join another team!");
			}
			member = currentUserOpt.get();  // with db ID!
		} else {
			// Anonymous request. Must provide new email and mobilephone
			Optional<UserModel> userByMail = userRepo.findByEmail(member.email);
			if (userByMail.isPresent()) throw new LiquidoException(Errors.USER_EMAIL_EXISTS, "Sorry, another user with that email already exists.");
			Optional<UserModel> userByMobilephone = userRepo.findByMobilephone(member.mobilephone);
			if (userByMobilephone.isPresent()) throw new LiquidoException(Errors.USER_MOBILEPHONE_EXISTS, "Sorry, another user with that mobile phone number already exists.");
		}

		try {
			team.getMembers().add(member);   // Add to java.util.Set. Will never add duplicate.
			member = userRepo.save(member);
			team = teamRepo.save(team);
			log.info("User <" + member.email + "> joined team: " + team.toString());
			String jwt = jwtTokenUtils.generateToken(member.getId(), team.getId());
			//BUGFIX: Authenticate new user in spring's security context, so that access restricted attributes such as isLikeByCurrentUser can be queried via GraphQL.
			authUtil.authenticateInSecurityContext(member.id, team.id, jwt);
			return new CreateOrJoinTeamResponse(team, member, jwt);
		} catch (Exception e) {
			throw new LiquidoException(Errors.INTERNAL_ERROR, "Error: Cannot join team.", e);
		}
	}


}
