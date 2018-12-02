package org.doogie.liquido.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.jwt.JwtTokenProvider;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.test.testUtils.JwtAuthInterceptor;
import org.doogie.liquido.test.testUtils.LogClientRequestInterceptor;
import org.doogie.liquido.testdata.TestFixtures;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.LiquidoProperties;
import org.doogie.liquido.util.LiquidoRestUtils;
import org.doogie.liquido.util.Lson;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.springframework.http.HttpMethod.*;



/**
 * Integration test for Liquiodo REST endpoint.
 *
 * These test cases test the Liquido Java backend via its REST interface.
 */
@Slf4j
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)  // This is so cool. This automatically sets up everything and starts the server. *like*
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)  // Only run tests. Do not automatically start a server.
public class RestEndpointTests {

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
  CastVoteService castVoteService;

  @Autowired
  LiquidoProperties props;

  @Autowired
  RepositoryEntityLinks entityLinks;

	// preloaded data that most test cases need.
  List<UserModel> users;
  List<AreaModel> areas;
  Map<String, AreaModel> areaMap;  // areas by title
  List<LawModel>  laws;

  //@Autowired
  //Environment springEnv;

  /** our HTTP REST client */
  RestTemplate client;

  /** anonymous HTTP client without any auth, for testing register, login and cast vote */
	RestTemplate anonymousClient;

	/**
	 * HTTP request interceptor that sends basic auth header
	 * AND can change the logged in user.
	 */
	//LiquidoBasicAuthInterceptor basicAuthInterceptor;

	// Json Web Tokens   JWT

	@Autowired
	JwtTokenProvider jwtTokenProvider;

	JwtAuthInterceptor jwtAuthInterceptor = new JwtAuthInterceptor();

  /** for JSON parsing */
	ObjectMapper jsonMapper = new ObjectMapper();

	/**
	 * This is executed, when the Bean has been created and @Autowired references are injected and ready.
	 * This runs once for all tests
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

		this.laws = new ArrayList<>();
		for (LawModel lawModel : lawRepo.findAll()) {
			this.laws.add(lawModel);
		}
		log.trace("loaded "+this.laws.size()+ " laws");
	}

	/**
	 * This runs before every test method
	 * By Default USER1_EMAIL is logged in
	 */
	@Before
	public void beforeEachTest() {
		//basicAuthInterceptor.login(TestFixtures.USER1_EMAIL, TestFixtures.TESTUSER_PASSWORD);
		String jwt = jwtTokenProvider.generateToken(TestFixtures.USER1_EMAIL);
		jwtAuthInterceptor.setJwtToken(jwt);
	}


	/**
	 * Entry and exit logging for <b>all</b> test cases. Jiipppiiee. Did I already mention that I am a logging fanatic *G*
	 */
	@Rule
	public TestWatcher slf4jTestWatcher = new TestWatcher() {
		@Override
		protected void starting(Description descr) {
			log.trace("===== TEST STARTING "+descr.getDisplayName());
		}

		@Override
		protected void failed(Throwable e, Description descr) {
			log.error("===== TEST FAILED "+descr.getDisplayName()+ ": "+e.toString());
		}

		@Override
		protected void succeeded(Description descr) {
			log.trace("===== TEST SUCCESS "+descr.getDisplayName());
		}
	};

  /**
   * Configure a HTTP REST client
   *  - USER1 is logged in
   *  - logs the client requetss with {@link LogClientRequestInterceptor}
   *  - has a rootUri
   */
  public void initHttpClients() {
    this.rootUri = "http://localhost:"+localServerPort+basePath;
    log.trace("====== configuring RestTemplate HTTP client for "+rootUri+ " with user "+TestFixtures.USER1_EMAIL);

	  //this.basicAuthInterceptor = new LiquidoBasicAuthInterceptor(TestFixtures.USER1_EMAIL, TestFixtures.TESTUSER_PASSWORD);

	  this.client = new RestTemplateBuilder()
      //.basicAuthorization(TestFixtures.USER1_EMAIL, TestFixtures.TESTUSER_PASSWORD)
		  //.additionalInterceptors(basicAuthInterceptor)
      //TODO:  .errorHandler(new LiquidoTestErrorHandler())     // the DefaultResponseErrorHandler throws exceptions
		  //TODO: need to add CSRF header https://stackoverflow.com/questions/32029780/json-csrf-interceptor-for-resttemplate
      //.additionalInterceptors(this.getOauthInterceptor())
      .additionalInterceptors(new LogClientRequestInterceptor())
			.additionalInterceptors(this.jwtAuthInterceptor)
      .rootUri(rootUri)
      .build();

		this.anonymousClient = new RestTemplateBuilder()
				.additionalInterceptors(new LogClientRequestInterceptor())
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


  //===================== Register and login ============================


	@Test
	public void testRegisterAndLoginViaSms() {
		String phonenumber = "00491511234567";
		HttpEntity<String> entity = Lson.builder()
				.put("email", "userFromTest-" + System.currentTimeMillis())
				.put("passwordHash", "dummyPasswordHashFromTest")
				.put("profile", Lson.builder("mobilePhone", phonenumber))
				.toHttpEntity();

		// register
		ResponseEntity<String> res = anonymousClient.postForEntity("/auth/register", entity, String.class);
		assertEquals(res.getStatusCode(), HttpStatus.OK);
		log.debug("Successfully registered new user");

		// request SMS login code
		res = anonymousClient.getForEntity("/auth/requestSmsCode?mobile="+phonenumber, String.class);
		String smsCode = res.getBody();  // when spring profile is TEST then backend returns code. that would normally be sent via SMS
		assertFalse("Did not receive code.", DoogiesUtil.isEmpty(smsCode));
		log.debug("Received login code via SMS: "+smsCode);

		// login with received SMS code
		res = anonymousClient.getForEntity("/auth/loginWithSmsCode?mobile="+phonenumber+"&code="+smsCode, String.class);
		String jwtToken = res.getBody();
		log.debug("received JWT: "+jwtToken);
		assertTrue("Invalid JWT: "+jwtToken, jwtToken != null && jwtToken.length() > 20);
		log.debug("Logged in successfully. Received JWT: "+jwtToken);

		// verify that user is logged in with that token
		this.jwtAuthInterceptor.setJwtToken(jwtToken);
		String userJson = client.getForObject("/my/user", String.class);
		log.debug("Logged in as : "+userJson);

		try {
			UserModel receivedUser = jsonMapper.readValue(userJson, UserModel.class);
			assertEquals("Logged in user should have the phone number that we registered with", phonenumber, receivedUser.getProfile().getMobilephone());
		} catch (IOException e) {
			String errMsg = "Cannot read read JSON returned from GET /my/user: "+e;
			log.error(errMsg);
			fail(errMsg);
		}


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
   * Create a new proposal for a law. This test case posts an alternative proposal to an already existing proposal.
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
    String newLawTitle = "Law from test "+System.currentTimeMillis() % 10000;  // law.title must be unique!!

    String newLawJson = new Lson()
      .put("title", newLawTitle)
      .put("description", "Dummy description from testPostProposalForLaw")
      //.put("status", LawModel.LawStatus.IDEA)     //TODO: Actually the server should decide about the status.
      .put("area", areaUri)
      .put("poll", pollUri)
			.toString();
      // Remark: it is not necessary to send a createdBy user URI
    log.trace("posting JSON Object:\n"+newLawJson);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(newLawJson.toString(), headers);

    LawModel createdLaw = client.postForObject("/laws", entity, LawModel.class);  // this actually deserializes the response into a LawModel. But that's ok. Makes the assertions much easier than digging around in a plain String response.
    assertNotNull("ERROR: could not post proposal for new law", createdLaw);   // createdLaw will be null, when there was an error.  (I hate methods that return null instead of throwing exceptions!)
    assertEquals("ERROR: builder law title does not match", newLawTitle, createdLaw.getTitle());

    log.trace("TEST postAlternativeProposal successfully created "+createdLaw);
  }

  /* TODO
  @Test
  public void testPostDuplicateVote() throws JSONException {
    log.trace("TEST postDuplicateVote");

    // ===== Find a poll tha is in VOTING phase
    List<PollModel> polls = pollRepo.findByStatus(PollModel.PollStatus.VOTING);
    assertTrue("Need a poll that currently is in VOTING phase for this test", polls != null && polls.size() > 0);

    String pollUri     = basePath + "/polls/" + polls.get(0).getId();
    String proposalUri = basePath + "/laws/" + this.laws.get(0).getId();

    JSONObject newBallotJson = new JSONObject()
      .put("poll", pollUri)
      .put("voteOrder", new JSONArray()
        .put(proposalUri)
        .put(proposalUri));    // <======== try to vote for them same proposal twice
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
   * Create a new idea. Then add as many supporters, so that the idea reaches its quorum.
   * A new poll will then automatically be created.
   * Test for {@link org.doogie.liquido.services.LawService#checkQuorum(LawModel)}
   */
  @Test
  //This would only work with MockMvc:  @WithUserDetails(value=TestFixtures.USER1_EMAIL , userDetailsServiceBeanName="liquidoUserDetailsService")
  public void testIdeaReachesQuorum() {
    log.trace("TEST ideaReachesQuorum");

    LawModel idea = postNewIdea("Idea from testIdeaReachesQuorum");
    log.trace(idea.toString());
    assertEquals(0, idea.getNumSupporters());

    //===== add Supporters via JSON, so that idea reaches its quorum
    int supportersForProposal = props.getInt(LiquidoProperties.KEY.SUPPORTERS_FOR_PROPOSAL);
    Assert.assertTrue("Need at least "+supportersForProposal+" users to run this test", this.users.size() >= supportersForProposal);

    String supportersURL = "/laws/"+idea.getId()+"/supporters";
    for (int j = 0; j < this.users.size(); j++) {
      if (!this.users.get(j).getEmail().equals(TestFixtures.USER1_EMAIL)) {   // creator is implicitly already a supporter
        String userURI = basePath + "/users/" + this.users.get(j).getId();
        HttpHeaders headers2 = new HttpHeaders();
        headers2.setContentType(RestMediaTypes.TEXT_URI_LIST);
        HttpEntity<String> entity2 = new HttpEntity<>(userURI, headers2);
        ResponseEntity<String> addSupporterResponse = client.postForEntity(supportersURL, entity2, String.class);
        assertEquals(HttpStatus.NO_CONTENT, addSupporterResponse.getStatusCode());   // 204
        log.debug("Added supporter to idea: "+userURI);
      }
    }

    //===== idea should now have reached its quorum
    LawModel updatedIdea = client.getForObject("/laws/"+idea.getId(), LawModel.class);
    Assert.assertEquals("Idea should have reached its quorum and be in status PROPOSAL", LawModel.LawStatus.PROPOSAL, updatedIdea.getStatus());

    log.trace("TEST ideaReachesQuorum SUCCESSFULL");
  }

  /**
   * Helper to builder a new idea (via REST)
   * @param ideaTitlePrefix the title of the idea. A random number will be added,
   *                        because title MUST be unique.
   * @return the created idea (but without dependant entities such  as area and createdBy filled!)
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

    // Keep in mind that createdIdea.createdBy is not filled, because this is just the idea not the ideaProjection
    assertEquals(HttpStatus.CREATED, createdIdea.getStatusCode());
    return createdIdea.getBody();

  }


  /**
   * User4 should have 5 votes (including his own) in area1
   */
  @Test
  public void testGetNumVotes() {
    log.trace("TEST getNumVotes");
    AreaModel area = this.areaMap.get(TestFixtures.AREA_FOR_DELEGATIONS);
    String uri = "/my/numVotes?area="+area.getId();

    long numVotes = client.getForObject(uri, Long.class);

    assertEquals("User "+TestFixtures.USER1_EMAIL+" should have "+TestFixtures.USER1_NUM_VOTES+" delegated votes in area='"+TestFixtures.AREA_FOR_DELEGATIONS+"'", TestFixtures.USER1_NUM_VOTES, numVotes);
    log.trace("TEST SUCCESS: found expected "+TestFixtures.USER1_NUM_VOTES+" delegations for "+TestFixtures.USER1_EMAIL + " in area "+TestFixtures.AREA_FOR_DELEGATIONS);
  }

  /**
   * User0 should have delegated his vote to User4 in Area1
   */
  @Test
  //TODO: How to send requests with that user? @WithUserDetails("testuser1@liquido.de")
  public void testGetProxyMap() {
    log.trace("TEST getDirectProxies");
    String uri = "/my/proxyMap";
		String delegeeEMail = TestFixtures.delegations.get(0)[0];
		String proxyEMail   = TestFixtures.delegations.get(0)[1];

		log.trace("Checking if "+delegeeEMail+" has proxy "+proxyEMail);

		//Login via basic auth: basicAuthInterceptor.login(delegeeEMail, TestFixtures.TESTUSER_PASSWORD);
		//Login via oauth: send request as the delegee who assigned his vote to a proxy
		//getOauthInterceptor().setUsername(delegeeEMail);

		// login with jwtToken that we can create very simple
		String jwt = jwtTokenProvider.generateToken(delegeeEMail);
		this.jwtAuthInterceptor.setJwtToken(jwt);

    String proxyMapJson = client.getForObject(uri, String.class);

    log.trace("got Proxy Map:\n"+proxyMapJson);
    String actualProxyInArea = JsonPath.read(proxyMapJson, "$['"+TestFixtures.AREA_FOR_DELEGATIONS+"'].directProxy.email");
    assertEquals(delegeeEMail+" should have "+proxyEMail+" as proxy", proxyEMail, actualProxyInArea);
  }

  /**
   * This updates a delegation and changes the toProxy via PUT to the /saveProxy endpoint
   */
  @Test
  public void testAssignProxy() throws LiquidoException {
    String url = "/my/proxy";
    UserModel fromUser = this.users.get(10);
    UserModel toProxy  = this.users.get(11);
    AreaModel area     = this.areas.get(0);
    String toProxyUri  = basePath + "/users/" + toProxy.getId();
    String areaUri     = basePath + "/areas/" + area.getId();
    String voterToken  = castVoteService.createVoterTokenAndStoreChecksum(fromUser, area, fromUser.getPasswordHash(), true);

    //TODO: delete delegation if it exists:  proxyServcie.removeProxy(...)

		HttpEntity entity = new Lson()
				.put("toProxy",  toProxyUri)
				.put("area",     areaUri)
				.put("voterToken", voterToken)
				.toHttpEntity();

    // send PUT that will assign the new proxy
    ResponseEntity<String> response = client.exchange(url, PUT, entity, String.class);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    String updatedDelegationJson = response.getBody();

    String actualProxyEmail = JsonPath.read(updatedDelegationJson, "$.toProxy.email");
    assertEquals("expected toProxy ID to be updated", toProxy.getEmail(), actualProxyEmail);
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
    log.trace("TEST getGlobalProperties");

    String responseJSON = client.getForObject("/globalProperties", String.class);

    assertNotNull(responseJSON);
  }

	/**
	 * cast a vote via real REST requests
	 */
  @Test
  public void testCastVote() {
		//----- find poll that is in voting
		String pollsJson = client.getForObject("/polls/search/findByStatus?status=VOTING", String.class);
		DocumentContext ctx = JsonPath.parse(pollsJson);
		String pollURI       = ctx.read("$._embedded.polls[0]._links.self.href", String.class);
		String proposal1_URI = ctx.read("$._embedded.polls[0]._embedded.proposals[0]_links.self.href", String.class);
		String proposal2_URI = ctx.read("$._embedded.polls[0]._embedded.proposals[1]_links.self.href", String.class);
		String areaId        = ctx.read("$._embedded.polls[0].area.id", String.class);
		proposal1_URI = LiquidoRestUtils.cleanURI(proposal1_URI);
		proposal2_URI = LiquidoRestUtils.cleanURI(proposal2_URI);

		log.trace("casting vote in poll.id="+pollURI);

		//----- get voterToken
		//Mock: String voterToken  = castVoteService.createVoterTokenAndStoreChecksum(fromUser, area, fromUser.getPasswordHash());
		String voterTokenJson = client.getForObject("/my/voterToken?area="+areaId, String.class);
		assertNotNull(voterTokenJson);
		String voterToken = JsonPath.read(voterTokenJson, "voterToken");
		log.trace("with voterToken: "+voterToken);



		//----- cast vote
		String body = Lson.builder()
		  .put("poll", pollURI)
		  .put("voterToken", voterToken)
		  .put("voteOrder",  proposal1_URI, proposal2_URI)
			.toString();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> castVoteEntity = new HttpEntity<>(body, headers);

		ResponseEntity<String> castVoteRes = client.postForEntity("/castVote", castVoteEntity, String.class);

		assertEquals(HttpStatus.CREATED, castVoteRes.getStatusCode());
		ctx = JsonPath.parse(castVoteRes.getBody());
		Long voteCount = ctx.read("voteCount", Long.class);
		assertTrue(voteCount > 0);
	}












	/*==================
	// MockMvc is nice. But it only mocks the HTTP requests (via a mocked DispatcherSrvlet)
	// Rest Template does really send requests (via network)
	// https://stackoverflow.com/questions/25901985/difference-between-mockmvc-and-resttemplate-in-integration-tests

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private FilterChainProxy springSecurityFilterChain;

	// Moch HTTP client configured for Oauth
	private MockMvc mockMvc;

	@Before
	public void setupMockMvc() {

		this.mockMvc = MockMvcBuilders
				.webAppContextSetup(wac)
				.addFilter(springSecurityFilterChain)
				.build();
	}
  */

}
