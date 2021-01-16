package org.doogie.liquido.graphql;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.OffsetLimitPageable;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.jwt.JwtTokenProvider;
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.testdata.TestFixtures;
import org.doogie.liquido.util.DoogiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
	JwtTokenProvider jwtTokenProvider;

	@Autowired
	PollRepo pollRepo;


	/**
	 * Login with email and oneTimeToken
	 * @param email user's email
	 * @param token one time token
	 * @return <pre>{ "login": "...jwt..."}</pre> with JWT for next authenticated requests
	 * @throws LiquidoException when email or OTT is invalid
	 */
	@GraphQLQuery(name = "login")
	public String login(
		@GraphQLNonNull @GraphQLArgument(name = "email") String email,
		@GraphQLNonNull @GraphQLArgument(name = "token") String token
	) throws LiquidoException {
		if (!DoogiesUtil.isEmail(email))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_LOGIN_EMAIL_NOT_FOUND, "This does not look like a valid email.");
		TeamModel team = teamRepo.findByMembersEmailEquals(email).orElseThrow(
			LiquidoException.notFound("Could not find your team.")
		);
		String jwt = jwtTokenProvider.generateToken(email);
		return jwt;
	}

	public static final String HAS_ROLE_USER = "hasRole('ROLE_USER')";

	/**
	 * Get information about your own team
	 * @param teamId
	 * @param teamName
	 * @return
	 * @throws LiquidoException
	 */
	@PreAuthorize(HAS_ROLE_USER)
	@GraphQLQuery(name = "team")
	public TeamModel getOwnTeam(
		@GraphQLArgument(name = "id") Long teamId,
		@GraphQLArgument(name = "name") String teamName,
		@GraphQLNonNull @GraphQLArgument(name = "jwt") String jwt
	) throws LiquidoException {


		TeamModel team;
		if (teamId != null) {
			team = teamRepo.findById(teamId).orElseThrow(LiquidoException.notFound("Cannot find team with id="+teamId));
		} else
		if (teamName != null) {
			team = teamRepo.findByTeamName(teamName).orElseThrow(LiquidoException.notFound("Cannot find team with teamName="+teamName));
		} else {
			throw LiquidoException.notFound("Need either teamId or teamName").get();
		}
		return team;
	}

	/**
	 * Search for polls in own team
	 * @param offset
	 * @param limit
	 * @param status
	 * @param jwt
	 * @return

	@GraphQLQuery(name="polls")
	public Iterable<TeamModel> getTeams(
		@GraphQLArgument(name = "offset", defaultValue = "0") int offset,
		@GraphQLArgument(name = "limit", defaultValue = "100") int limit,
		@GraphQLArgument(name = "status", defaultValue = "ELABORATION") String status,
		@GraphQLNonNull @GraphQLArgument(name = "jwt") String jwt
	) {



		OffsetLimitPageable page = new OffsetLimitPageable(offset, limit);

		return pollRepo.find
	}
		*/



	/**
	 * Create a new team. The first user will become the admin of the team.
	 * @param teamName Name of new team. Must be unique.
	 * @param adminName Admin's name
	 * @param adminEmail email of admin. (Must not be unique. One email MAY be the admin of several teams.)
	 * @return The newly created team, incl. ID and inviteCode
	 * @throws LiquidoException when teamName is not unique.
	 */
	@GraphQLMutation(name = "createNewTeam", description = "Create a new team")
	public TeamModel createNewTeam(
		@GraphQLArgument(name = "teamName") @GraphQLNonNull String teamName,
		@GraphQLArgument(name = "adminName") @GraphQLNonNull String adminName,
		@GraphQLArgument(name = "adminEmail") @GraphQLNonNull String adminEmail,
		@GraphQLArgument(name = "adminMobilephone") @GraphQLNonNull String adminMobilephone
	) throws LiquidoException {
		// GraphQLArgument without a default value are automatically checked as REQUIRED.
		UserModel admin = new UserModel(adminEmail, adminName, adminMobilephone, null, null);
		TeamModel newTeam = new TeamModel(teamName, admin);

		try {
			teamRepo.save(newTeam);
			log.info("Created new team: "+newTeam.toString());
		} catch (DataIntegrityViolationException ex) {
			log.debug(ex.getMessage());
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_NEW_TEAM, "Cannot create new team: A team with that name ('"+teamName+"') already exists");
		}

		return newTeam;
	}

	/**
	 * Join an existing team
	 * @param inviteCode valid invite code of the team to join
	 * @param userName new user name
	 * @param userEmail new user's email
	 * @return the team
	 * @throws LiquidoException when inviteCode is invalid
	 */
	@GraphQLMutation(name = "joinTeam", description = "Join an existing team")
	public TeamModel joinNewTeam(
		@GraphQLArgument(name = "inviteCode") @GraphQLNonNull String inviteCode,
		@GraphQLArgument(name = "userName") @GraphQLNonNull String userName,
		@GraphQLArgument(name = "userEmail") @GraphQLNonNull String userEmail,
		@GraphQLArgument(name = "userMobilephone") @GraphQLNonNull String userMobilephone
	) throws LiquidoException {
		TeamModel team = teamRepo.findByInviteCode(inviteCode).orElseThrow(
			() -> new LiquidoException(LiquidoException.Errors.CANNOT_JOIN_TEAM, "Invalid inviteCode '"+inviteCode+"'")
		);
		// If a user with that email is already in the team, then check if he has the same name.
		Optional<UserModel> existingUser = team.getMembers().stream()
			.filter(u -> u.email.equals(userEmail))
			.findFirst();
		if (existingUser.isPresent()) {
			if (!userName.equals(existingUser.get().profile.getName()))
				throw new LiquidoException(LiquidoException.Errors.CANNOT_JOIN_TEAM, "A user with that email but a different name is already in the team!");
			log.info("User <" + userEmail + "> already is in team: " + team.toString());
		} else {
			// Otherwise add a new user to the team
			try {
				UserModel newUser = new UserModel(userEmail, userName, userMobilephone, null, null);
				team.getMembers().add(newUser);
				userRepo.save(newUser);
				team = teamRepo.save(team);
				log.info("User <" + userEmail + "> joined team: " + team.toString());
			} catch (Exception e) {
				throw new LiquidoException(LiquidoException.Errors.CANNOT_JOIN_TEAM, "Error: Cannot join team.", e);
			}
		}
		return team;
	}


}
