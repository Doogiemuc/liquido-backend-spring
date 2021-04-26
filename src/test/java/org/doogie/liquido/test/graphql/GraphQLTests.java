package org.doogie.liquido.test.graphql;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.OffsetLimitPageable;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.jwt.JwtTokenUtils;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.services.LawService;
import org.doogie.liquido.test.HttpBaseTest;
import org.doogie.liquido.testdata.TestFixtures;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.Lson;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
	JwtTokenUtils jwtTokenUtils;

	@Autowired
	PollRepo pollRepo;

	/** all test cases will run against this team */
	TeamModel team;

	private final String GraphQLPath = "/graphql";


	@PostConstruct
	public void loadTeamTestee() {
		this.team = teamRepo.findAll(new OffsetLimitPageable(0, 1)).iterator().next();
	}

	public void loginTeamAdmin() {
		this.loginUserJWT(team.getAdmins().iterator().next().getId(), team.getId());
	}

	public void loginTeamMember() {
		this.loginUserJWT(team.getMembers().iterator().next().getId(), team.getId());
	}


	/**
	 * Test register and login flow via GraphQL.
	 */
	public void testAuthyLogin() {
		// GIVEN a user with mobilephone
		UserModel user = this.team.getMembers().iterator().next();
		assertTrue("User needs mobilephone", DoogiesUtil.isEmpty(user.getMobilephone()));

		//WHEN requesting an authy token
		String graphQL    = String.format("{ authyToken(mobilephone: \"%s\") }", user.getMobilephone());
		Lson entity = new Lson("query", graphQL);
		ResponseEntity<String> res = this.client.postForEntity(this.GraphQLPath, entity.toJsonHttpEntity(), String.class);
		assertEquals(HttpStatus.ACCEPTED, res.getStatusCode());
		log.info("Successfully requested a one time token");

		/* login with received token



		res = anonymousClient.getForEntity("/auth/loginWithToken?mobile={mobile}&token={smsToken}", String.class, mobile, smsToken);
		String jwt = res.getBody();
		log.debug("received JWT: "+jwt);
		assertTrue("Invalid JWT: "+jwt, jwt != null && jwt.length() > 20);
		log.debug("Logged in successfully. Received JWT: "+jwt);

		// verify that user is logged in
		this.jwtAuthInterceptor.setJwtToken(jwt);
		String userJson = client.getForObject("/my/user", String.class);
		log.debug("Logged in as : "+userJson);
		String receivedMobile = JsonPath.read(userJson, "$.profile.mobilephone");
		assertEquals("Logged in user should have the phone number that we registered with", mobile, receivedMobile);
		*/

	}







	/** Load information about team of currently logged in user */
	@Test
	public void testGetOwnTeam() {
		//GIVEN a query for team of currently logged in user
		this.loginTeamMember();
		String expectedTeamName   = team.getTeamName();
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
		String teamName    = TestFixtures.TEAM_NAME_PREFIX + "_" + now;
		String adminName   = TestFixtures.USER_NAME_PREFIX + "_" + now;
		String adminEmail  = TestFixtures.MAIL_PREFIX+ "_admin_" + now + "@graphql-test.vote";
		String mobilephone = TestFixtures.MOBILEPHONE_PREFIX + now;
		String website     = TestFixtures.DEFAULT_WEBSITE;
		String picture     = TestFixtures.AVATAR_IMG_PREFIX +(now%16)+".png";
		String graphQLMutation = String.format(
			"mutation { createNewTeam(teamName: \"%s\", adminName: \"%s\", adminEmail: \"%s\", adminMobilephone: \"%s\", " +
				"website: \"%s\", picture: \"%s\") { " +
				"team { id teamName inviteCode members { id, email, name, website, picture, mobilephone } } " +
				"user { id email name mobilephone website picture } " +
				"jwt " +
			"}}",
			teamName, adminName, adminEmail, mobilephone, website, picture
		);

		// WHEN we send this mutation
		String actualTeamName = executeGraphQl(graphQLMutation, "$.createNewTeam.team.teamName");

		// THEN we receive info about the new team
		Assert.assertEquals("Expected teamName="+teamName, teamName, actualTeamName);
	}

	/** Join an existing team */
	@Test
	public void testJoinTeam() {
		// GIVEN an inviteCode
		Page<TeamModel> teams = teamRepo.findAll(new OffsetLimitPageable(0, 1));
		TeamModel team = teams.iterator().next();
		Assert.assertNotNull("Need at least one team to testJoinTeam!", team);
		String inviteCode = team.getInviteCode();

		// AND a graphQL mutation to join an existing team
		long now = System.currentTimeMillis() % 10000;
		String userName  = TestFixtures.USER_NAME_PREFIX + "_" + now;
		String userEmail = TestFixtures.MAIL_PREFIX+ "_" + now + "@graphql-test.vote";
		String mobilephone = TestFixtures.MOBILEPHONE_PREFIX+ now;
		String website     = TestFixtures.DEFAULT_WEBSITE;
		String picture     = TestFixtures.AVATAR_IMG_PREFIX +(now%16)+".png";
		String graphQLMutation = String.format(
			"mutation { joinTeam(inviteCode: \"%s\", userName: \"%s\", userEmail: \"%s\", mobilephone: \"%s\"" +
				"website: \"%s\", picture: \"%s\") {" +
			  "team { id teamName inviteCode members { id, email, name, website, picture, mobilephone } } " +
			  "user { id email name mobilephone website picture  } " +
			  "jwt " +
			"}}",
			inviteCode, userName, userEmail, mobilephone, website, picture
		);

		// WHEN we send this mutation
		Lson entity = new Lson("query", graphQLMutation);
		ResponseEntity<String> res = this.client.exchange(this.GraphQLPath, HttpMethod.POST, entity.toJsonHttpEntity(), String.class);

		// THEN user's email is part of team.members
		List<String> members = JsonPath.read(res.getBody(), "$.joinTeam.team.members..email");
		assertTrue("Cannot find userEmail in joinedTeam.members", members.contains(userEmail));
	}

	/** Admin creates a new poll in his team. */
	@Test
	public void testAdminCreatesNewPoll() {
		//GIVEN a team with a logged in admin
		this.loginTeamAdmin();

		// AND a graphQL mutation to createPoll
		String pollTitle = "Poll from test " + System.currentTimeMillis() * 10000;
		String graphQLMutation = "mutation { createPoll(title: \"" + pollTitle + "\") { id, title } }";

		//WHEN the admin creates a new poll
		String actualTitle = executeGraphQl(graphQLMutation, "$.createPoll.title");

		//THEN poll is created
		Assert.assertEquals("Poll title should be returned", pollTitle, actualTitle);
	}

	@Test
	public void testAddProposalToPoll() {
		//GIVEN a logged in Admin
		this.loginTeamAdmin();

		//  AND a poll in ELABORATION
		List<PollModel> polls = pollRepo.findByStatus(PollModel.PollStatus.ELABORATION);
		assertTrue("Need at least one poll in elaboration to testAddProposalToPoll", polls.size() > 0);
		PollModel poll = polls.get(0);

		// AND data for a new proposal
		Long now = System.currentTimeMillis() % 10000;
		String proposalTitle = "Proposal added from Test "+now;
		String description = getLoremIpsum(0, 200);

		// AND a graphQL mutation to add a proposal to this poll
		String addProposalGraphQL = String.format(
			"mutation { addProposal(pollId: \"%s\", title: \"%s\", description: \"%s\") { id, title, proposals { id, title, description } } }",
			poll.getId(), proposalTitle, description
		);

		//WHEN this proposal is added to the poll
		Lson entity = new Lson("query", addProposalGraphQL);
		ResponseEntity<String> res = this.client.exchange(this.GraphQLPath, HttpMethod.POST, entity.toJsonHttpEntity(), String.class);

		//THEN poll is created
		assertTrue("Proposal with that title should have been added", res.getBody().contains(proposalTitle));
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
