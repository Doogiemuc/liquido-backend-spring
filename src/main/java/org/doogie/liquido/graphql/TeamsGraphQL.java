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
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.CreateOrJoinTeamResponse;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.LiquidoException.Errors;
import org.doogie.liquido.services.UserService;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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

		//TODO:  Do I need to call  team.getPolls() here?   Can I do this depending on what the client requested in his GraphQL query? That would be cool!

		return team;
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
	@GraphQLMutation(name = "createNewTeam", description = "Create a new team")
	public CreateOrJoinTeamResponse createNewTeam(
		@GraphQLArgument(name = "teamName") @GraphQLNonNull String teamName,
		@GraphQLArgument(name = "adminName") @GraphQLNonNull String adminName,
		@GraphQLArgument(name = "adminEmail") @GraphQLNonNull String adminEmail,
		@GraphQLArgument(name = "mobilephone") String mobilephone,		//TODO: user needs mobilephone for Authy
		@GraphQLArgument(name = "website") String website,
		@GraphQLArgument(name = "picture") String picture
	) throws LiquidoException {
		UserModel admin = new UserModel(adminEmail, adminName, mobilephone, website, picture);
		//TODO: also register user at twilio. This Needs a mobilephone! userService.registerUser(admin);
		TeamModel newTeam = new TeamModel(teamName, admin);
		try {
			newTeam = teamRepo.save(newTeam);
			userRepo.save(admin);
			log.info("Created new team: "+newTeam.toString());
		} catch (DataIntegrityViolationException ex) {
			log.debug(ex.getMessage());
			throw new LiquidoException(Errors.TEAM_WITH_SAME_NAME_EXISTS, "Cannot create new team: A team with that name ('"+teamName+"') already exists");
		}
		String jwt = jwtTokenUtils.generateToken(admin.getId(), newTeam.getId());
		return new CreateOrJoinTeamResponse(newTeam, admin, jwt);

	}

	/**
	 * Join an existing team.
	 * This can be called anonymously.
	 *
	 * Will throw an error, when a member tries to join with an emqail of one of the admins!
	 * Joining more than once is idempotent. Or in simpler words: It is ok to click the invite link more than once :-)
	 *
	 * @param inviteCode valid invite code of the team to join
	 * @param userName new user name
	 * @param userEmail new user's email
	 * @return Info about the joined team and a JsonWebToken
	 * @throws LiquidoException when inviteCode is invalid
	 */
	@GraphQLMutation(name = "joinTeam", description = "Join an existing team")
	public CreateOrJoinTeamResponse joinNewTeam(
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
		// If an admin with that email already exists, then throw error
		boolean adminWithSameEmail = team.getAdmins().stream().anyMatch(admin -> userEmail.equals(admin.email));
		if (adminWithSameEmail) throw new LiquidoException(Errors.CANNOT_JOIN_TEAM, "You must not join the team as a member with this email address!");

		// If a member with that email is already in the team, then check if he has the same attributes. If all match
		UserModel newUser;
		UserModel existingMember = team.getMembers().stream()
			.filter(u -> u.email.equals(userEmail))
			.findFirst().orElse(null);
		//Or that way:   Optional<UserModel> existingMember = DoogiesUtil.doesContain(team.getMembers(), member -> userEmail.equals(member.email));

		if (existingMember != null) {
			if (!ObjectUtils.nullSafeEquals(userName, existingMember.getName()) || !ObjectUtils.nullSafeEquals(mobilephone, existingMember.getMobilephone()))
				throw new LiquidoException(Errors.CANNOT_JOIN_TEAM, "A user with that email but a different name is already in the team!");
			newUser = existingMember;
			log.info("User <" + userEmail + "> already is in team: " + team.toString());
		} else {
			// Otherwise add a new user to the team
			try {
				newUser = new UserModel(userEmail, userName, mobilephone, website, picture);
				team.getMembers().add(newUser);
				userRepo.save(newUser);
				team = teamRepo.save(team);
				log.info("User <" + userEmail + "> joined team: " + team.toString());
			} catch (Exception e) {
				throw new LiquidoException(Errors.CANNOT_JOIN_TEAM, "Error: Cannot join team.", e);
			}
		}
		String jwt = jwtTokenUtils.generateToken(newUser.getId(), team.getId());
		return new CreateOrJoinTeamResponse(team, newUser, jwt);
	}


}
