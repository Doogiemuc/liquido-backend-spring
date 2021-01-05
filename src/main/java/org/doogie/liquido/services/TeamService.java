package org.doogie.liquido.services;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.model.TeamModel;
import org.springframework.beans.factory.annotation.Autowired;
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
}
