package org.doogie.liquido.test.graphql;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.OffsetLimitPageable;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.services.LawService;
import org.doogie.liquido.test.HttpBaseTest;
import org.doogie.liquido.testdata.TestFixtures;
import org.doogie.liquido.util.Lson;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * Tests for {@link LawService}
 * Here we mostly test the advanced search capabilities.
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)   // automatically fire up the integraded webserver on a random port
public class GraphQLTests extends HttpBaseTest {

	@Autowired
	TeamRepo teamRepo;

	@Before
	public void beforeEachTest() {
		this.loginUserJWT(TestFixtures.USER1_EMAIL);
	}

	String GraphQLPath = "/graphql";

	/** Make a simple GraphQL query against the backend */
	@Test
	public void testGetAllTeams() {
		// GIVEN a graphQLQuery for all teams
		String graphQLQuery = "{ teams { teamName, inviteCode, id, createdAt, updatedAt, members { id, email, profile { name } }} }";  // this is not JSON! This is a graphQL query String!

		// WHEN we send this query
		Lson entity = new Lson("query", graphQLQuery);  // must send this as JSON with field "query".
		ResponseEntity<String> res = this.client.exchange(this.GraphQLPath, HttpMethod.POST, entity.toJsonHttpEntity(), String.class);

		// THEN we receive a list of teams
		String teamName = JsonPath.read(res.getBody(), "$.teams[0].teamName");
		Assert.assertNotNull(teamName);
		Assert.assertTrue("Expected teamName to start with "+TestFixtures.TEAM_NAME_PREFIX, teamName.startsWith(TestFixtures.TEAM_NAME_PREFIX));
	}

	/** Create a new team */
	@Test
	public void testCreateNewTeam() {
		// GIVEN a graphQL mutation to create a new team
		long now = System.currentTimeMillis() % 10000;
		String teamName   = TestFixtures.TEAM_NAME_PREFIX + "_" + now;
		String adminName  = TestFixtures.USER_NAME_PREFIX + "_" + now;
		String adminEmail = TestFixtures.MAIL_PREFIX+ "_admin_" + now + "@graphql-test.vote";
		String adminMobilephone = TestFixtures.MOBILEPHONE_PREFIX+ now;
		String graphQLMutation = String.format("mutation { createNewTeam(teamName: \"%s\", adminName: \"%s\", adminEmail: \"%s\", adminMobilephone: \"%s\") " +
			"{ id, teamName, inviteCode, members { id, email } } }", teamName, adminName, adminEmail, adminMobilephone);

		// WHEN we send this mutation
		Lson entity = new Lson("query", graphQLMutation);
		ResponseEntity<String> res = this.client.exchange(this.GraphQLPath, HttpMethod.POST, entity.toJsonHttpEntity(), String.class);

		// THEN we receive a list of teams
		String actualTeamName = JsonPath.read(res.getBody(), "$.createNewTeam.teamName");
		Assert.assertNotNull(actualTeamName);
		Assert.assertEquals("Expected teamName="+teamName, teamName, actualTeamName);
	}

	/** Join an existing team */
	@Test
	public void testJoinTeamTeam() {
		// GIVEN an inviteCode
		Page<TeamModel> teams = teamRepo.findAll(new OffsetLimitPageable(0, 1));
		TeamModel team = teams.iterator().next();
		Assert.assertNotNull("Need at least one team to testJoinTeam!", team);
		String inviteCode = team.getInviteCode();

		// AND a graphQL mutation to join an existing team
		long now = System.currentTimeMillis() % 10000;
		String userName  = TestFixtures.USER_NAME_PREFIX + "_" + now;
		String userEmail = TestFixtures.MAIL_PREFIX+ "_" + now + "@graphql-test.vote";
		String userMobilephone = TestFixtures.MOBILEPHONE_PREFIX+ now;
		String graphQLMutation = String.format("mutation { joinTeam(inviteCode: \"%s\", userName: \"%s\", userEmail: \"%s\", userMobilephone: \"%s\") " +
			"{ id, teamName, inviteCode, members { email } } }", inviteCode, userName, userEmail, userMobilephone);

		// WHEN we send this mutation
		Lson entity = new Lson("query", graphQLMutation);
		ResponseEntity<String> res = this.client.exchange(this.GraphQLPath, HttpMethod.POST, entity.toJsonHttpEntity(), String.class);

		// THEN user's email is part of team.members
		List<String> members = JsonPath.read(res.getBody(), "$.joinTeam.members..email");
		Assert.assertTrue("Cannot find userEmail in joinedTeam.members", members.contains(userEmail));
	}

	/** Admin creates a new poll in his team. */
	@Test
	public void testAdminCreatesNewPoll() {
		//GIVEN a team with an admin
		Page<TeamModel> teams = teamRepo.findAll(new OffsetLimitPageable(0, 1));
		TeamModel team = teams.iterator().next();
		Assert.assertNotNull("Need at least one team to testCreatePoll!", team);
		UserModel admin = team.getAdmin();

		// AND a graphQL mutation to createPoll
		String title = "Poll from test " + System.currentTimeMillis() * 10000;
		String graphQLMutation = "mutation { createPoll(title: \"" + title + "\") { id, title, }";

		//WHEN the admin creates a new poll
		Lson entity = new Lson("query", graphQLMutation);
		ResponseEntity<String> res = this.client.exchange(this.GraphQLPath, HttpMethod.POST, entity.toJsonHttpEntity(), String.class);

		//THEN poll is created
		String actualTitle = JsonPath.read(res.getBody(), "$.title");
		Assert.assertEquals("Poll title should be returned", title, actualTitle);
	}



}
