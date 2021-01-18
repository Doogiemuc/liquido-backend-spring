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
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.UserService;
import org.doogie.liquido.testdata.TestFixtures;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.LiquidoRestUtils;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.Optional;

import static org.doogie.liquido.security.LiquidoAuthUser.HAS_ROLE_USER;

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

	@Autowired
	LiquidoAuditorAware liquidoAuditorAware;

	@PersistenceContext
	private EntityManager entityManager;

	/*

	   @Deprecated: Login is done via REST

	 * Login with email and oneTimeToken
	 * @param mobile user's mobile phone number
	 * @param loginToken one time token
	 * @return <pre>{ "login": "...jwt..."}</pre> with JWT for next authenticated requests
	 * @throws LiquidoException when email or OTT is invalid

	@GraphQLQuery(name = "loginWithToken")
	public String login(
		@GraphQLNonNull @GraphQLArgument(name = "mobile") String mobile,
		@GraphQLNonNull @GraphQLArgument(name = "token") String loginToken
	) throws LiquidoException {
		return userService.verifyOneTimePassword(mobile, mobile);
	}
	*/


	/**
	 * Get information about user's own team, including the team's polls.
	 * @return info about user's own team.
	 * @throws LiquidoException
	 */
	//@PreAuthorize(HAS_ROLE_USER)
	@GraphQLQuery(name = "team")
	@Transactional  //BUGFIX for error: No EntityManager with actual transaction available for current thread - cannot reliably process 'merge' call"
	public TeamModel getOwnTeam() throws LiquidoException {
		UserModel currentUser = liquidoAuditorAware.getCurrentAuditor()
			.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "You must be logged in to query for your own team's info."));

		currentUser = entityManager.merge(currentUser);


		TeamModel team = currentUser.getTeam();

		//TeamModel team = teamRepo.findById(currentUser.teamId)
	  //	.orElseThrow(LiquidoException.notFound("Cannot find team with id="+currentUser.teamId));

		return team;
	}

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
		UserModel admin = UserModel.asTeamAdmin(adminEmail, adminName, adminMobilephone, null, null);
		TeamModel newTeam = new TeamModel(teamName, admin);
		try {
			//admin.setTeamId(newTeam.id);			// link admin user to team. This MUST be done manually here. After save(newTeam)
			admin.setTeam(newTeam);
			newTeam = teamRepo.save(newTeam);
			//userRepo.save(admin);
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
				//newUser.setTeamId(team.id);
				newUser.setTeam(team);
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
