package org.doogie.liquido.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.doogie.liquido.model.TeamModel;

/**
 * GraphQL response for the createNewTeam mutation
 * {@link org.doogie.liquido.graphql.TeamsGraphQL#createNewTeam(String, String, String, String)}
 */
@Data
@AllArgsConstructor
public class CreateNewTeamResponse {
	TeamModel team;
	String jwt;
}
