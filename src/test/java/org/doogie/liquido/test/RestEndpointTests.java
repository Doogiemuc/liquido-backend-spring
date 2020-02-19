package org.doogie.liquido.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.jwt.JwtTokenProvider;
import org.doogie.liquido.model.*;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.test.testUtils.JwtAuthInterceptor;
import org.doogie.liquido.test.testUtils.LogClientRequestInterceptor;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.doogie.liquido.testdata.TestDataUtils;
import org.doogie.liquido.testdata.TestFixtures;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.Lson;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.springframework.http.HttpMethod.*;

/**
 * Integration test for Liquiodo REST endpoint.
 *
 * These test cases test the Liquido Java backend via its REST interface.
 * All these tests run against the test data defined in {@link TestFixtures} !
 * Keep in mind, that these tests run from the point of view of an HTTP client.
 * We CAN use Autowired spring components here. But the tests should assert just the HTTP responses.
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)  // This automatically sets up everything and starts the server.
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)       // Theoretically we coul tun tests against an already running server, e.g. PROD   But that's a lot of work. Instead our Cypress E2E tests can do this much better.
public class RestEndpointTests extends BaseTest {

  /** path prefix for REST API from application.properties */
  @Value(value = "${spring.data.rest.base-path}")
  String basePath;

  /** (random) port of local backend under test that spring creates */
  @LocalServerPort
  int localServerPort;

  /** full uri: https://localhost:{port}/{basePath}/ */
  String rootUri;

  // My spring-data-jpa repositories for loading test data directly
  @Autowired
  UserRepo userRepo;

  @Autowired
  AreaRepo areaRepo;

  @Autowired
  LawRepo lawRepo;

  @Autowired
  PollRepo pollRepo;

  @Autowired
	RightToVoteRepo rightToVoteRepo;

  @Autowired
	BallotRepo ballotRepo;

  @Autowired
  CastVoteService castVoteService;

  @Autowired
	LiquidoProperties prop;

  @Autowired
	TestDataUtils testDataUtils;

  @Autowired
  RepositoryEntityLinks entityLinks;

	// preloaded data that most test cases need.
  List<UserModel> users;
  List<AreaModel> areas;
  Map<String, AreaModel> areaMap;  // areas by title
	List<LawModel>  ideas;
	List<LawModel>  proposals;
	List<LawModel>  laws;

  //@Autowired
  //Environment springEnv;

  /** our HTTP REST client */
  RestTemplate client;

  /** anonymous HTTP client without any auth, for testing register, login and cast vote */
	RestTemplate anonymousClient;

	// Json Web Tokens   JWT
	@Autowired
	JwtTokenProvider jwtTokenProvider;

	JwtAuthInterceptor jwtAuthInterceptor = new JwtAuthInterceptor();

	/**
	 * This is executed, when the Bean has been created and @Autowired references are injected and ready.
	 * This runs once for every test!
	 */
	@PostConstruct
	public void postConstruct() {
		initHttpClients();

		// Here you can do any one time setup necessary
		log.trace("PostConstruct: pre-fetching data from DB");

		this.users = new ArrayList<>();
		for (UserModel userModel : userRepo.findAll()) {
			this.users.add(userModel);
		}
		log.trace("loaded "+this.users.size()+ " users");

		this.areas = new ArrayList<>();
		this.areaMap = new HashMap<>();
		for (AreaModel areaModel : areaRepo.findAll()) {
			this.areas.add(areaModel);
			this.areaMap.put(areaModel.getTitle(), areaModel);
		}
		log.trace("loaded "+this.areas.size()+ " areas");

		this.ideas = new ArrayList<>();
		for (LawModel lawModel : lawRepo.findByStatus(LawModel.LawStatus.IDEA, Pageable.unpaged())) {
			this.ideas.add(lawModel);
		}
		log.trace("loaded "+this.ideas.size()+ " ideas");

		this.proposals = new ArrayList<>();
		for (LawModel lawModel : lawRepo.findByStatus(LawModel.LawStatus.PROPOSAL, Pageable.unpaged())) {
			this.ideas.add(lawModel);
		}
		log.trace("loaded "+this.proposals.size()+ " proposals");

		this.laws = new ArrayList<>();
		for (LawModel lawModel : lawRepo.findByStatus(LawModel.LawStatus.LAW, Pageable.unpaged())) {
			this.ideas.add(lawModel);
		}
		log.trace("loaded "+this.laws.size()+ " laws");

	}

	/**
	 * This runs before every test method.
	 * Here we (fake) generation of a JWT token by directly calling jwtTokenProvider
	 * By Default USER1_EMAIL is logged in
	 */
	@Before
	public void beforeEachTest() {
		loginUserJWT(TestFixtures.USER1_EMAIL);
	}

	/**
	 * little helper to quickly login a specific user
	 */
	private void loginUserJWT(String email) {
		// Here we see that advantage of a completely stateless server. We simply generate a JWT and that's it. No login state is stored on the server.
		String jwt = jwtTokenProvider.generateToken(email);
		jwtAuthInterceptor.setJwtToken(jwt);
	}

  /**
   * Configure a HTTP REST client
   *  - USER1 is logged in
   *  - logs the client requetss with {@link LogClientRequestInterceptor}
   *  - has a rootUri
   */
  public void initHttpClients() {
    this.rootUri = "http://localhost:"+localServerPort+basePath;
    log.trace("====== configuring RestTemplate HTTP client for "+rootUri+ " with user "+TestFixtures.USER1_EMAIL);

		// neet to manually configure Encoding of URL query parameters
		// https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/web.html#web-uri-encoding
		DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
		uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);

	  this.client = new RestTemplateBuilder()
      //TODO: .errorHandler(new LiquidoTestErrorHandler())     // the DefaultResponseErrorHandler throws exceptions
		  //TODO: need to add CSRF header https://stackoverflow.com/questions/32029780/json-csrf-interceptor-for-resttemplate
      .additionalInterceptors(new LogClientRequestInterceptor())
			.additionalInterceptors(this.jwtAuthInterceptor)
			//.uriTemplateHandler(uriBuilderFactory)
      .rootUri(rootUri)
      .build();

		this.anonymousClient = new RestTemplateBuilder()
				.additionalInterceptors(new LogClientRequestInterceptor())
				.uriTemplateHandler(uriBuilderFactory)
				.rootUri(rootUri)
				.build();
	}



	//========= Tests HTTP Security =============

	@Test
	public void testPublicPingEndpoint() {
		ResponseEntity<String> res = anonymousClient.exchange("/_ping", HttpMethod.GET, null, String.class);
		assertEquals("/_ping endpoint should be reachable anonymously", HttpStatus.OK, res.getStatusCode());
	}

	@Test
	public void testProposalsUrlShouldNeedAuth() {
		RestTemplate anonymousClient = new RestTemplateBuilder()
				.additionalInterceptors(new LogClientRequestInterceptor())
				.rootUri(rootUri)
				.build();
		try {
			ResponseEntity<String> res = anonymousClient.exchange("/laws", HttpMethod.GET, null, String.class);
			fail("Should have thrown an exception with unauthorized(401)");
		} catch (HttpClientErrorException err) {
			assertEquals("Response should have status unauthorized(401)", HttpStatus.UNAUTHORIZED, err.getStatusCode());
		}
	}

	@Test
  public void testInvalidUrlShouldReturn404() {
    try {
			ResponseEntity<String> res = client.exchange("/invalidUrl", HttpMethod.GET, null, String.class);
			fail("Should have thrown an exception with 404");
		} catch (HttpClientErrorException err) {
			assertEquals("Response should have status 404", HttpStatus.NOT_FOUND, err.getStatusCode());
		}
  }


  //===================== Register ============================

	//@Test    //TODO: test this via email
	public void testRegisterAndLoginViaSms() {
		String mobile = "+4912345"+ DoogiesUtil.randomDigits(5);
		HttpEntity<String> entity = Lson.builder()
				.put("email", "userFromTest" + System.currentTimeMillis()+"@liquido.vote")
				.put("profile", new Lson()
						.put("mobilephone", mobile)
						.put("picture", "/static/img/avatars/Avatar1.png")
				).toJsonHttpEntity();

		// register
		ResponseEntity<String> res = anonymousClient.postForEntity("/auth/register", entity, String.class);
		assertEquals(res.getStatusCode(), HttpStatus.OK);
		log.info("Successfully registered new user");

		// request one time login token
		res = anonymousClient.getForEntity("/auth/requestToken?mobile={mobile}", String.class, mobile);
		assertEquals("Could not requestToken", HttpStatus.OK, res.getStatusCode());

		log.info("Successfully requested a one time token");
		//with reals 2FA tokens, this cannot be automatically tested
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


	//===================== basic requests =================================



  @Test
  public void testPostNewArea() {
    String areaTitle = "This is a newly created Area "+System.currentTimeMillis();  // make test repeatable: Title must be unique!
		String newAreaJSON = new Lson()
      .put("title", areaTitle)
      .put("description", "Very nice description for new area")
			.toString();

    log.trace("posting JSON Object:\n"+newAreaJSON);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(newAreaJSON, headers);

    ResponseEntity<AreaModel> response = client.exchange("/areas", POST, entity, AreaModel.class);

    assertEquals("expected HttpStatus.CREATED(201)", HttpStatus.CREATED, response.getStatusCode());
    AreaModel createdArea = response.getBody();
    assertEquals(areaTitle, createdArea.getTitle());
  }

  @Test
  public void testPatchArea() {
  	String newDescription = "Updated description";
    String newAreaJSON = new Lson()
      .put("description", newDescription)
			.toString();
		log.trace("JSON Payload for PATCH request: "+newAreaJSON);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(newAreaJSON.toString(), headers);

    Long areaId = this.areas.get(0).getId();
    ResponseEntity<AreaModel> response = client.exchange("/areas/"+areaId, PATCH, entity, AreaModel.class);
    AreaModel updatedArea = response.getBody();
    assertEquals(newDescription, updatedArea.getDescription());
  }

  @Test
  //@WithUserDetails(value="testuser0@liquido.de", userDetailsServiceBeanName="liquidoUserDetailsService")
  public void testFindMostRecentIdeas() throws IOException {
    log.trace("TEST testFindMostRecentIdeas");

    String response = client
      //.withBasicAuth(TestFixtures.USER1_EMAIL, TestFixtures.USER1_PWD)
      .getForObject("/laws/search/recentIdeas", String.class);

    log.debug("got response: "+response);

    assertNotNull("Could not get recent ideas", response);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(response);
    JsonNode ideas = node.get("_embedded").get("laws");
    assertTrue(ideas.isArray());
    assertTrue(ideas.size() > 5);

    log.trace("TEST SUCCESSFULL testFindMostRecentIdeas  found "+ideas.size()+" ideas");
  }

  /**
   * Create a new proposal for a proposal. This test case posts an alternative proposal to an already existing proposal.
   */
  @Test
  public void testPostAlternativeProposal() {
    log.trace("TEST postAlternativeProposal");

    // ===== Find a poll that is in VOTING phase
    List<PollModel> polls = pollRepo.findByStatus(PollModel.PollStatus.ELABORATION);
    assertTrue("Need a poll that currently is in PROPOSAL phase for this test", polls != null && polls.size() > 0);

    // I am deliberately not creating a new BallotModel(...) here that I could then   postForEntity like this:
    //   ResponseEntity<BallotModel> createdBallot = client.postForEntity("/ballot", newBallot, BallotModel.class);
    // because I do not want the test to success just because of on spring's very clever serialization and deserialization.
    // Instead I want to post plain JSON as a client would:
    String areaUri  = basePath + "/areas/" + this.areas.get(0).getId();
    String pollUri  = basePath + "/polls/" + polls.get(0).getId();
    String newLawTitle = "Law from test "+System.currentTimeMillis() % 10000;  // proposal.title must be unique!!

    String newLawJson = new Lson()
      .put("title", newLawTitle)
      .put("description", "Dummy description from testPostProposalForLaw")
      //.putArray("status", LawModel.LawStatus.IDEA)     //TODO: Actually the server should decide about the status.
      .put("area", areaUri)
      .put("poll", pollUri)
			.toString();
      // Remark: it is not necessary to send a createdBy user URI
    log.trace("posting JSON Object:\n"+newLawJson);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(newLawJson.toString(), headers);

    LawModel createdLaw = client.postForObject("/laws", entity, LawModel.class);  // this actually deserializes the response into a LawModel. But that's ok. Makes the assertions much easier than digging around in a plain String response.
    assertNotNull("ERROR: could not post proposal for new proposal", createdLaw);   // createdLaw will be null, when there was an error.  (I hate methods that return null instead of throwing exceptions!)
    assertEquals("ERROR: builder proposal title does not match", newLawTitle, createdLaw.getTitle());

    log.trace("TEST postAlternativeProposal successfully created "+createdLaw);
  }

  /*
  //TODO: Test duplicate vote (with differenet faked voterToken)
  @Test
  public void testPostDuplicateVote() throws JSONException {
    log.trace("TEST postDuplicateVote");

    // ===== Find a poll tha is in VOTING phase
    List<PollModel> polls = pollRepo.findByStatus(PollModel.PollStatus.VOTING);
    assertTrue("Need a poll that currently is in VOTING phase for this test", polls != null && polls.limit() > 0);

    String pollUri     = basePath + "/polls/" + polls.get(0).getId();
    String proposalUri = basePath + "/laws/" + this.laws.get(0).getId();

    JSONObject newBallotJson = new JSONObject()
      .putArray("poll", pollUri)
      .putArray("voteOrder", new JSONArray()
        .putArray(proposalUri)
        .putArray(proposalUri));    // <======== try to vote for them same proposal twice
    log.trace("posting JSON Object:\n"+newBallotJson.toString(2));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(newBallotJson.toString(), headers);

    ResponseEntity<String> response = client.exchange("/postBallot", POST, entity, String.class);

    String responseBody = response.getBody();
    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCodeValue());  // 400
    log.trace("response.body (that should contain the error): "+responseBody);
    assertTrue(responseBody.contains("Duplicate vote for proposal"));
    log.trace("TEST postInvalidBallot successful: received correct status and error message in response.");
  }
  */

	/**
	 * Negative test case: User must NOT be able to support his own idea
	 */
	@Test
	public void testSupportOwnIdea() {
		log.trace("TEST supportOwnIdea");
		//GIVEN
		LawModel idea = postNewIdea("Idea from testSupportOwnIdea");
		UserModel currentUser = getCurrentUser();
		String supporterURI = basePath + "/users/" + currentUser.getId();
		try {
			//WHEN
			ResponseEntity<String> res = addSupporterToIdea(supporterURI, idea);
			fail("addSupporterToIdea should have thrown an Exception");
		} catch (HttpClientErrorException e) {
			//THEN
			Assert.assertEquals("Response should have status" + HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST, e.getStatusCode());
			String liquidoErrorName = JsonPath.read(e.getResponseBodyAsString(), "$.liquidoErrorName");
			Assert.assertEquals("LiquidoException.Error should have been CANNOT_ADD_SUPPORTER", LiquidoException.Errors.CANNOT_ADD_SUPPORTER.name(), liquidoErrorName );
			log.trace("TEST supportOwnIdea SUCCESSFUL");
		} catch (Throwable t) {
			fail("Should have thrown a HttpClientErrorException but threw "+t);
		}
	}

	/**
   * Create a new idea. Then add as many supporters, so that the idea reaches its quorum.
   * A new poll will then automatically be created.
   * Test for {@link org.doogie.liquido.services.LawService#checkQuorum(LawModel)}
   */
  @Test
  //With user Details only works with MockMvc:  @WithUserDetails(value=TestFixtures.USER1_EMAIL , userDetailsServiceBeanName="liquidoUserDetailsService")
  public void testIdeaReachesQuorum() {
    log.trace("TEST ideaReachesQuorum");

		loginUserJWT(TestFixtures.USER1_EMAIL);
    LawModel idea = postNewIdea("Idea from testIdeaReachesQuorum");
    log.trace(idea.toString());
    assertEquals(0, idea.getNumSupporters());

    //===== add Supporters via JSON, so that idea reaches its quorum
    Assert.assertTrue("Need at least "+prop.supportersForProposal+" users to run this test", this.users.size() >= prop.supportersForProposal);
    for (int j = 0; j < this.users.size(); j++) {
      if (!this.users.get(j).getEmail().equals(TestFixtures.USER1_EMAIL)) {   // creator is implicitly already a supporter
				String supporterURI = basePath + "/users/" + this.users.get(j).getId();
				loginUserJWT(this.users.get(j).getEmail());
      	addSupporterToIdea(supporterURI, idea);
      }
    }

    //===== idea should now have reached its quorum
    LawModel updatedIdea = client.getForObject("/laws/"+idea.getId(), LawModel.class);
    Assert.assertEquals("Idea should have reached its quorum and be in status PROPOSAL", LawModel.LawStatus.PROPOSAL, updatedIdea.getStatus());

    log.trace("TEST ideaReachesQuorum SUCCESSFUL");
  }

  @Test
	public void testGetMyNewsfeed() {
  	// GIVEN user1 is logged in
		loginUserJWT(TestFixtures.USER1_EMAIL);

		// WHEN fetching current user's newsfeed
		ResponseEntity<String> res = client.getForEntity("/my/newsfeed", String.class);
		log.debug("Newsfeed: "+res.getBody());

		// THEN the newsfeed should not be empty
		assertTrue("Newsfeed should not be empty", res != null);
		DocumentContext ctx = JsonPath.parse(res.getBody());
		Long recentlyDiscussedId = ctx.read("$.supportedByYou[0].id", Long.class);
		assertNotNull(recentlyDiscussedId);

	}

	/**
	 * fetch currently logged in user
	 * @return
	 */
	private UserModel getCurrentUser() {
		ResponseEntity<UserModel> response = client.getForEntity("/my/user", UserModel.class);
		return response.getBody();
	}

  /**
   * Helper to builder a new idea (via REST)
   * @param ideaTitlePrefix the title of the idea. A random number will be added,
   *                        because title MUST be unique.
   * @return the created idea (but without dependant entities such as area and createdBy filled!)
   */
  private LawModel postNewIdea(String ideaTitlePrefix) {
    String ideaTitle = ideaTitlePrefix+" "+System.currentTimeMillis();;  // title must be unique!
    String ideaDesc  = "This idea was created from a test case";
    String areaUri   = basePath + "/areas/" + this.areas.get(0).getId();

		String jsonBody = Lson.builder()
		  .put("title", ideaTitle)
		  .put("description", ideaDesc)
		  .put("area", areaUri)
			.toString();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

		ResponseEntity<LawModel> createdIdea = client.postForEntity("/laws", entity, LawModel.class);

    // Keep in mind that createdIdea.createdBy is not filled, because this is just the serialized idea not the ideaProjection
    assertEquals(HttpStatus.CREATED, createdIdea.getStatusCode());
    return createdIdea.getBody();
  }

	/**
	 * Add a supporter to an idea
	 * @param supporterURI  basePath + "/users/" + supporter.getId();
	 * @param idea
	 */
  private ResponseEntity<String> addSupporterToIdea(String supporterURI, LawModel idea) {
		String supportersURL = "/laws/"+idea.getId()+"/like";
		/*
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(RestMediaTypes.TEXT_URI_LIST);
		HttpEntity<String> entity = new HttpEntity<>(supporterURI, headers);
		*/
		ResponseEntity<String> addSupporterResponse = client.postForEntity(supportersURL, null, String.class);
		assertEquals(HttpStatus.OK, addSupporterResponse.getStatusCode());
		log.debug("Added supporter to idea: "+supporterURI);
		return addSupporterResponse;
	}

  /** helper to get the currently logged in user's voterToken via REST*/
  private String getVoterToken(long areaId) {
		String tokenJson = client.getForObject("/my/voterToken/{areaId}?tokenSecret={tokenSecret}", String.class, areaId, TestFixtures.USER_TOKEN_SECRET);
		return JsonPath.read(tokenJson, "$.voterToken");
		//Or fake REST and call service directly: return castVoteService.createVoterTokenAndStoreChecksum(fromUser, area, TestFixtures.USER_TOKEN_SECRET, true);
	}

  /**
   * User4 should have 5 votes (including his own) in area1
   */
  @Test
  public void testGetVoterToken() {
    AreaModel area = this.areaMap.get(TestFixtures.AREA_FOR_DELEGATIONS);
    String voterToken = getVoterToken(area.getId());
    assertTrue("Voter token is invalid", voterToken != null && voterToken.startsWith("$2") && voterToken.length() > 10);
    log.trace("TEST SUCCESS: found expected "+TestFixtures.USER1_DELEGATIONS +" delegations for "+TestFixtures.USER1_EMAIL + " in area "+TestFixtures.AREA_FOR_DELEGATIONS);
  }

  @Test
  public void testGetAssignableProxies() {
		AreaModel area = areaMap.get(TestFixtures.AREA_FOR_DELEGATIONS);
		loginUserJWT(TestFixtures.USER1_EMAIL);
		String assignableResources = client.getForObject("/my/proxy/{areaId}/assignable", String.class, area.getId());
		JSONArray assignableProxies = JsonPath.read(assignableResources, "$._embedded.users");
		assertTrue("There should be at least one assignable proxy", assignableProxies.size() > 1);
	}

	@Test
	public void testDelegationsCount() {
		AreaModel area = areaMap.get(TestFixtures.AREA_FOR_DELEGATIONS);
		loginUserJWT(TestFixtures.USER1_EMAIL);

		String tokenJson = client.getForObject("/my/voterToken/{areaId}?tokenSecret={tokenSecret}", String.class, area.getId(), TestFixtures.USER_TOKEN_SECRET);
		String voterToken = JsonPath.read(tokenJson, "$.voterToken");
		int delegationCountFromVoterToken = JsonPath.read(tokenJson, "$.delegationCount");

		String delegationsJSON = client.getForObject("/my/delegations/{areaId}?voterToken={voterToken}", String.class, area.getId(), voterToken);
		int delegationCount = JsonPath.read(delegationsJSON, "$.delegationCount");
		assertEquals(TestFixtures.USER1_EMAIL+" should have "+TestFixtures.USER1_DELEGATIONS +" delegated votes in area='"+TestFixtures.AREA_FOR_DELEGATIONS+"'", TestFixtures.USER1_DELEGATIONS, delegationCount);
		assertEquals("DelegationCount from GET /my/voterToken must equal the returned delegation count from GET /my/delegations/{areaId}", delegationCountFromVoterToken, delegationCount);
	}

	@Test
  public void testRequestedDelegation() {
		AreaModel area = areaMap.get(TestFixtures.AREA_FOR_DELEGATIONS);
		String email = TestFixtures.USER2_EMAIL;
		loginUserJWT(email);
		String voterToken = getVoterToken(area.getId());
		String delegationsJSON = client.getForObject("/my/delegations/{areaId}?voterToken={voterToken}", String.class, area.getId(), voterToken);
		Boolean isPublicProxy = JsonPath.read(delegationsJSON, "$.isPublicProxy");
		int delegationCount = JsonPath.read(delegationsJSON, "$.delegationCount");
		assertFalse(email + " should NOT be a public proxy in area " + TestFixtures.AREA_FOR_DELEGATIONS, isPublicProxy);
		assertEquals(email + " should have one delegation request", TestFixtures.USER2_DELEGATIONS, delegationCount);
	}


  /**
   * This updates a delegation and changes the toProxy via PUT to the /saveProxy endpoint
   */
  @Test
  public void testAssignAndRemoveProxy() {
    UserModel fromUser = this.users.get(15);
    UserModel toProxy  = this.users.get(10);
    AreaModel area     = this.areas.get(0);
    String toProxyUri  = basePath + "/users/" + toProxy.getId();

    loginUserJWT(fromUser.getEmail());
    String voterToken  = getVoterToken(area.getId());
    HttpEntity entity = new Lson()
				.put("toProxy",  toProxyUri)
				.put("voterToken", voterToken)
				.put("transitive", true)
				.toJsonHttpEntity();
    ResponseEntity<String> response = client.exchange("/my/proxy/{areaId}?voterToken={voterToken}", PUT, entity, String.class, area.getId(), voterToken);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    String updatedDelegationJson = response.getBody();
		log.debug("received updated delegation: \n"+updatedDelegationJson);
    String actualProxyEmail = JsonPath.read(updatedDelegationJson, "$.toProxy.email");
    assertEquals("expected toProxy to be updated", toProxy.getEmail(), actualProxyEmail);

    client.delete("/my/proxy/{areaId}?voterToken={voterToken}", area.getId(), voterToken);

  }

  @Test
  public void testPostDuplicateArea() {
    String createdByUri  = basePath + "/users/" + this.users.get(0).getId();
    String duplicateAreaJson = new Lson()
      .put("title", TestFixtures.AREA1_TITLE)           // area with that title already exists.
      .put("description", "duplicate Area from test")
      .put("createdBy", createdByUri).toString();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(duplicateAreaJson, headers);

		try {
    	ResponseEntity<String> responseEntity = client.postForEntity("/areas", entity, String.class);  // This will return HTTP status 409(Conflict) because of the duplicate composite key.
			fail("Should have thrown an exception with 409 (Conflict)");
		} catch (HttpClientErrorException err) {
			assertEquals("Response should have status 400", HttpStatus.CONFLICT, err.getStatusCode());
			log.trace("TEST postDuplicateArea SUCCESS: Did receive expected HttpStatus=409 (Conflict)");
		}

  }

  @Test
  public void testGetGlobalProperties() {
    String responseJSON = client.getForObject("/globalProperties", String.class);
    assertNotNull(responseJSON);
  }

  /**
	 * Search a poll by its status.
	 * Precondition: This test expects at least on poll in status ELABORATION (which is created by TestDataCreator)
	 */
	@Test
	public void testSearchPollByStatus() {
  	String res = client.getForObject("/polls/search/findByStatus?status=ELABORATION", String.class);
  	try {
			Map<String, Object> poll = JsonPath.read(res, "$._embedded.polls[0]");
			assertEquals("Found poll should have had status ELABORATION!", PollModel.PollStatus.ELABORATION.name(), poll.get("status"));
		}	catch (PathNotFoundException e) {
  		fail("Expected to find at least one poll in status ELABORATION: "+e.getMessage());
		}
	}


	/**
	 * Cast a vote via real REST requests.
	 * This is THE most important test case in LIQUIDO.
	 * Here we test the basic flow for casting a vote.
	 * Then we also test the use case when two proxies at different levels each cast their vote.
	 * The vote of a lower level proxy must not be "overwritten" by the vote of a higher level proxy.
	 */
  @Test
  public void testCastVote() throws Exception {
		// GIVEN a poll that is in voting (user has not voted in this poll yet)
		List<PollModel> pollsInVoting = pollRepo.findByStatus(PollModel.PollStatus.VOTING);
		assertNotNull("Need at least one poll in voting!", pollsInVoting.size() > 0);
		PollModel poll = pollsInVoting.get(0);
		log.trace("Cast vote in "+poll);

		// -------------------------------------------------------
		// GIVEN user4 with his voterToken
		loginUserJWT(TestFixtures.USER4_EMAIL);
		String user4VoterToken = getVoterToken(poll.getArea().getId());

		// WHEN getting own ballot of user4
		ResponseEntity<String> noBallotYet = client.getForEntity("/polls/" + poll.getId() + "/myballot?voterToken="+user4VoterToken, String.class);
		// THEN user4 does not yet have a ballot (404 is returned)
		assertEquals("User should not yet have a ballot in this poll", HttpStatus.NO_CONTENT, noBallotYet.getStatusCode());

		// WHEN user4 casts his vote
		List<LawModel> user4VoteOrder = TestDataUtils.randVoteOrder(poll);
		ResponseEntity<String> user4CastVoteRes = castVoteRest(poll, user4VoterToken, user4VoteOrder);

		// THEN his ballot is created successfully
		assertEquals("User4's vote is casted successfully via REST", HttpStatus.CREATED, user4CastVoteRes.getStatusCode());

		// AND voteCount is correct
		long user4VoteCount = JsonPath.parse(user4CastVoteRes.getBody()).read("$.voteCount", Long.class);
		assertEquals("Vote by user4 should have been counted for "+TestFixtures.USER4_DELEGATIONS+" delegations.", TestFixtures.USER4_DELEGATIONS, user4VoteCount);

		//  AND ballot can be verified with checksum of user4
		String user4Checksum = JsonPath.read(user4CastVoteRes.getBody(), "$.ballot.checksum");
		ResponseEntity<BallotModel> user4VerifyRes = client.getForEntity("/polls/" + poll.getId() + "/verify?checksum=" + user4Checksum, BallotModel.class);
		assertEquals("Ballot's checksum could be verified ", HttpStatus.OK, user4VerifyRes.getStatusCode());
		assertEquals("Verified ballots's level is 0", 0, (long)user4VerifyRes.getBody().getLevel());
		assertNull("Ballot.rightToVote should not be revealed", user4VerifyRes.getBody().getRightToVote());
		for (int i = 0; i < user4VoteOrder.size(); i++) {
			assertEquals("Verified ballot received with user1Checksum should have correct voteOrder", user4VoteOrder.get(i), user4VerifyRes.getBody().getVoteOrder().get(i));
		}

		//  AND we can retrieve user's ballot with correct level and voteOrder
		ResponseEntity<BallotModel> myBallotRes = client.getForEntity("/polls/" + poll.getId() + "/myballot?voterToken="+user4VoterToken, BallotModel.class);
		assertEquals("User should now have a ballot in this poll", HttpStatus.OK, myBallotRes.getStatusCode());
		assertEquals("Ballot should have level 0", 0L, (long)myBallotRes.getBody().getLevel());
		for (int i = 0; i < user4VoteOrder.size(); i++) {
			assertEquals("Ballot should have correct voteOrder", user4VoteOrder.get(i), myBallotRes.getBody().getVoteOrder().get(i));
		}



		// --------------------------------------------------------
		// WHEN top proxy user1 casts his vote
		loginUserJWT(TestFixtures.USER1_EMAIL);
		String user1VoterToken = getVoterToken(poll.getArea().getId());
		List<LawModel> user1VoteOrder = TestDataUtils.randVoteOrder(poll);  // different voteOrder than above
		ResponseEntity<String> user1CastVoteRes = castVoteRest(poll, user1VoterToken, user1VoteOrder);

		// THEN ballot of user1 is created successfully
		assertEquals("Vote by user1 is casted successfully via REST", HttpStatus.CREATED, user1CastVoteRes.getStatusCode());

		// AND voteCount is correct (smaller than delegationCount because voter4 below has already voted from himself)
		Long user1VoteCount = JsonPath.parse(user1CastVoteRes.getBody()).read("$.voteCount", Long.class);
		assertEquals("Vote by user1 should have been counted for "+TestFixtures.USER1_VOTE_COUNT_WHEN_USER4_VOTED+ " delegations.", TestFixtures.USER1_VOTE_COUNT_WHEN_USER4_VOTED, (long)user1VoteCount);

		//  AND ballot can be verified with checksum of user1
		String user1Checksum = JsonPath.read(user1CastVoteRes.getBody(), "$.ballot.checksum");
		ResponseEntity<BallotModel> user1VerifyRes = client.getForEntity("/polls/" + poll.getId() + "/verify?checksum=" + user1Checksum, BallotModel.class);
		assertEquals("Ballot's checksum could be verified ", HttpStatus.OK, user1VerifyRes.getStatusCode());
		assertEquals("Verified ballots's level is 0", 0, (long)user1VerifyRes.getBody().getLevel());
		assertNull("Ballot.rightToVote should not be revealed", user1VerifyRes.getBody().getRightToVote());
		for (int i = 0; i < user1VoteOrder.size(); i++) {
			assertEquals("Verified ballot received with user1Checksum should have correct voteOrder", user1VoteOrder.get(i), user1VerifyRes.getBody().getVoteOrder().get(i));
		}

		// --------------------------------------------------------
		// GIVEN a user that has already casted his vote
		//  WHEN a proxy above him casts a vote
		// THEN the ballot of this user, who casted his own vote, did not change
		ResponseEntity<BallotModel> verifiedBallot2 = client.getForEntity("/polls/" + poll.getId() + "/verify?checksum=" + user4Checksum, BallotModel.class);
		assertEquals("ballot of user4 can still be verified via his checksum", HttpStatus.OK, verifiedBallot2.getStatusCode());
		assertEquals("level of the ballot of user4 is still 0", 0, (long)verifiedBallot2.getBody().getLevel());
		for (int i = 0; i < user4VoteOrder.size(); i++) {
			assertEquals("Ballot of user4 still has the original voteOrder after user below him voted", user4VoteOrder.get(i), verifiedBallot2.getBody().getVoteOrder().get(i));
		}



		// CLEANUP: remove casted Ballots
		ballotRepo.findByPollAndChecksum(poll, user1Checksum).ifPresent(ballot -> ballotRepo.delete(ballot));
		ballotRepo.findByPollAndChecksum(poll, user4Checksum).ifPresent(ballot -> ballotRepo.delete(ballot));
	}

	/**
	 * Cast a vote with the given voterToken
	 * @param poll a poll in VOTING
	 * @param voterToken user's valid voterToken
	 * @param voteOrder ordered list of proposals
	 * @return castVoter HTTP response
	 */
	private ResponseEntity<String> castVoteRest(PollModel poll, String voterToken, List<LawModel> voteOrder) {
		List<String> voteOrderUris = voteOrder.stream().map(prop -> "/laws/"+prop.getId()).collect(Collectors.toList());
		HttpEntity user4CastVoteEntity = Lson.builder()
			.put("poll", "/polls/"+poll.getId())
			.put("voterToken", voterToken)
			.putArray("voteOrder",  voteOrderUris)
			.toJsonHttpEntity();
		return anonymousClient.postForEntity("/castVote", user4CastVoteEntity, String.class);
	}


}
