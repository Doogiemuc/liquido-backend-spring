package org.doogie.liquido.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.model.UserModel;

/**
 * GraphQL response for the createNewTeam mutation
 * {@link org.doogie.liquido.graphql.TeamsGraphQL#createNewTeam(String, String, String, String, String, String)}
 */
@Data
@AllArgsConstructor
public class CreateOrJoinTeamResponse {
	TeamModel team;

	UserModel user; 		// newly joined user or admin when new Team was created

	String    jwt;
}
