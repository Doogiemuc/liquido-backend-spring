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

import java.util.Optional;

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


	@GraphQLMutation(name = "createNewTeam", description = "Create a new team")
	public TeamModel createNewTeam(@GraphQLArgument(name = "teamName") @GraphQLNonNull String teamName) throws LiquidoException {
		//TODO: most likely we will need a CreateNewTeamRequestDTO
		//TODO: add validation logic
		log.info("Create new team "+teamName);
		UserModel admin = new UserModel("admin222@graphql.org", "graphql admin", null, null, null);
		TeamModel newTeam = new TeamModel("teamName222", admin);
		try {
			teamRepo.save(newTeam);
		} catch (DataIntegrityViolationException ex) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_NEW_TEAM, "Cannot create new team: A team with that name ('"+teamName+"') already exists");
		}
		return newTeam;
	}
}
