package org.doogie.liquido.test.graphql;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.services.LawService;
import org.doogie.liquido.test.HttpBaseTest;
import org.doogie.liquido.testdata.TestFixtures;
import org.doogie.liquido.util.Lson;
import org.h2.api.DatabaseEventListener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * Tests for {@link LawService}
 * Here we mostly test the advanced search capabilities.
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)   // automatically fire up the integraded webserver on a random port
public class GraphQLTests extends HttpBaseTest {

	@Before
	public void beforeEachTest() {
		this.loginUserJWT(TestFixtures.USER1_EMAIL);
	}

	String GraphQLPath = "/graphql";

	/** Make a simple GraphQL query against the backend */
	@Test
	public void testGetAllTeams() {
		// GIVEN a graphQLQuery for all teams
		String graphQLQuery = "{ getAllTeams { teamName, inviteCode, id, createdAt, updatedAt, members { id, email, profile { name } }} }";  // this is not JSON! This is a graphQL query String!

		// WHEN we send this query
		Lson entity = new Lson("query", graphQLQuery);  // must send this as JSON with field "query".
		ResponseEntity<String> res = this.client.exchange(this.GraphQLPath, HttpMethod.POST, entity.toJsonHttpEntity(), String.class);

		// THEN we receive a list of teams
		String teamName = JsonPath.read(res.getBody(), "$.getAllTeams[0].teamName");
		Assert.assertNotNull(teamName);
		Assert.assertTrue("Expected teamName to start with "+TestFixtures.TEAM_NAME_PREFIX, teamName.startsWith(TestFixtures.TEAM_NAME_PREFIX));
	}

	@Test
	public void testCreateNewTeam() {
		// GIVEN a graphQL mutation to create a new team
		long now = System.currentTimeMillis() & 10000;
		String teamName   = TestFixtures.TEAM_NAME_PREFIX + "_" + now;
		String adminName  = TestFixtures.USER_NAME_PREFIX + "_" + now;
		String adminEmail = TestFixtures.MAIL_PREFIX+ "_" + now + "@graphql-test.vote";
		String graphQLMutation = String.format("mutation { createNewTeam(teamName: \"%s\", adminName: \"%s\", adminEmail: \"%s\") " +
			"{ id, teamName, inviteCode, members { id, email } } }", teamName, adminName, adminEmail);

		// WHEN we send this mutation
		Lson entity = new Lson("query", graphQLMutation);
		ResponseEntity<String> res = this.client.exchange(this.GraphQLPath, HttpMethod.POST, entity.toJsonHttpEntity(), String.class);

		// THEN we receive a list of teams
		String actualTeamName = JsonPath.read(res.getBody(), "$.createNewTeam.teamName");
		Assert.assertNotNull(actualTeamName);
		Assert.assertEquals("Expected teamName="+teamName, teamName, actualTeamName);
	}



}
