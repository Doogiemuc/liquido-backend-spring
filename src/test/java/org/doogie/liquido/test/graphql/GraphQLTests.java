package org.doogie.liquido.test.graphql;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.OffsetLimitPageable;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.jwt.JwtTokenProvider;
import org.doogie.liquido.model.PollModel;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)   // automatically fire up the integrated webserver on a random port
public class GraphQLTests extends HttpBaseTest {

	@Autowired
	TeamRepo teamRepo;

	@Autowired
	JwtTokenProvider jwtTokenProvider;

	@Autowired
	PollRepo pollRepo;

	/**
	 * Always login the admin of team0.
	 */
	@Before
	public void beforeEachTest() {
		this.loginUserJWT((String)TestFixtures.teams.get(0).get("adminEmail"));
	}

	String GraphQLPath = "/graphql";

	/** Load information about team of currently logged in user */
	@Test
	public void getOwnTeam() {
		//GIVEN a query for team of currently logged in user
		this.loginUserJWT((String)TestFixtures.teams.get(0).get("adminEmail"));
		String expectedTeamName   = (String)TestFixtures.teams.get(0).get("teamName");
		String graphQL    = "{ team { id teamName } }";

		//WHEN querying for the user's team
		String actualTeamName = executeGraphQl(graphQL, "$.team.teamName");

		//THEN the correct teamName is returned
		Assert.assertEquals("Expected teamName="+expectedTeamName, expectedTeamName, actualTeamName);
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
		String actualTeamName = executeGraphQl(graphQLMutation, "$.createNewTeam.teamName");

		// THEN we receive a list of teams
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
		Assert.assertNotNull("Need at least one team to testAdminCreatesNewPoll!", team);
		UserModel admin = team.getAdmins().stream().findFirst().get();

		// AND a graphQL mutation to createPoll
		String pollTitle = "Poll from test " + System.currentTimeMillis() * 10000;
		String graphQLMutation = "mutation { createPoll(title: \"" + pollTitle + "\") { id, title } }";

		//WHEN the admin creates a new poll
		loginUserJWT(admin.getEmail());
		String actualTitle = executeGraphQl(graphQLMutation, "$.createPoll.title");

		//THEN poll is created
		Assert.assertEquals("Poll title should be returned", pollTitle, actualTitle);
	}

	@Test
	public void testAddProposalToPoll() {
		//GIVEN a poll in ELABORATION
		List<PollModel> polls = pollRepo.findByStatus(PollModel.PollStatus.ELABORATION);
		Assert.assertTrue("Need at least one poll in elaboration to testAddProposalToPoll", polls.size() > 0);
		PollModel poll = polls.get(0);

		// AND data for a new proposal
		Long now = System.currentTimeMillis() % 10000;
		String title = "Proposal added from Test "+now;
		String description = getLoremIpsum(0, 200);

		// AND a graphQL mutation to add a proposal to this poll
		String pollTitle = "Poll from test " + System.currentTimeMillis() * 10000;
		String graphQL = String.format(
			"mutation { addProposal(pollId: \"%s\", title: \"%s\", description: \"%s\") { id, title, proposals { id, title, description } } }",
			poll.getId(), title, description
		);

		//WHEN this proposal is added to the poll
		Lson entity = new Lson("query", graphQL);
		ResponseEntity<String> res = this.client.exchange(this.GraphQLPath, HttpMethod.POST, entity.toJsonHttpEntity(), String.class);

		//THEN poll is created
		Assert.assertTrue("Proposal with that title should have been added", res.getBody().contains(title));
	}


	// ========================= private utility methods ======================

	/**
	 * Execute a GraphQL query or mutation and return the result of a JsonPath expression
	 * @param graphQL GraphQL query or mutation  This will be wrapped in a JSON   { "query" : "[graphQL]" }
	 * @param resJsonPath A JsonPath path
	 * @return the String result of your JsonPath expression. Result must be a string value
	 */
	private String executeGraphQl(String graphQL, String resJsonPath) {
		Lson entity = new Lson("query", graphQL);
		ResponseEntity<String> res = this.client.exchange(this.GraphQLPath, HttpMethod.POST, entity.toJsonHttpEntity(), String.class);
		return JsonPath.read(res.getBody(), resJsonPath);
	}

}
