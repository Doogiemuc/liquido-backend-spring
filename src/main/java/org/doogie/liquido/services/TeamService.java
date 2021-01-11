package org.doogie.liquido.services;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Optional;

/**
 * Create a new Team, join an existing team, get information about existing team and its members.
 * This service is directly exposed as GraphQL.
 */
@Slf4j
@Service
public class TeamService {

	@Autowired
	TeamRepo teamRepo;

	@GraphQLQuery(name="getAllTeams")
	public Iterable<TeamModel> getAllTeams() {
		return teamRepo.findAll();
	}

	@GraphQLQuery(name = "getTeamById")
	public Optional<TeamModel> getById(@GraphQLArgument(name = "id") Long id) {
		return teamRepo.findById(id);
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
			@GraphQLArgument(name = "adminEmail") @GraphQLNonNull String adminEmail
	) throws LiquidoException {
		Assert.hasText(teamName, "need teamName");
		Assert.hasText(adminName, "need adminName");
		Assert.hasText(adminEmail, "need adminEmail");

		log.info("Create new team "+teamName);
		UserModel admin = new UserModel(adminEmail, adminName, null, null, null);
		TeamModel newTeam = new TeamModel(teamName, admin);

		try {
			teamRepo.save(newTeam);
		} catch (DataIntegrityViolationException ex) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_NEW_TEAM, "Cannot create new team: A team with that name ('"+teamName+"') already exists");
		}

		return newTeam;
	}
}
