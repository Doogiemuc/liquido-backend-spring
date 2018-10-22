package org.doogie.liquido.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.test.testUtils.LiquidoTestErrorHandler;
import org.doogie.liquido.test.testUtils.LogClientRequestInterceptor;
import org.doogie.liquido.test.testUtils.OauthInterceptor;
import org.doogie.liquido.testdata.TestFixtures;
import org.doogie.liquido.util.LiquidoProperties;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
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
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

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

  /** FQDN */
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

  //@Autowired
  //DelegationRepo delegationRepo;

  @Autowired
  RepositoryEntityLinks entityLinks;

  // parameters for Oauth
	@Value(value = "${security.jwt.client-id}")
	private String CLIENT_ID;

	@Value(value = "${security.jwt.client-secret}")
	private String CLIENT_SECRET;

	@Value(value = "${security.jwt.grant-type}")
	private String GRANT_TYPE;

	@Value(value = "${security.jwt.resource-ids}")
	private String RESOURCE_IDs;



	// preloaded data that most test cases need.
  List<UserModel> users;
  List<AreaModel> areas;
  Map<String, AreaModel> areaMap;  // areas by title
  List<LawModel>  laws;

  //@Autowired
  //Environment springEnv;

  /** our HTTP REST client */
  RestTemplate client;

  /** the oauth interceptor can be configured for specific users */
	OauthInterceptor oauthInterceptor;

	/**
	 * This is executed, when the Bean has been created and @Autowired references are injected and ready.
	 * This runs before each test.
	 */
	@PostConstruct
	public void postConstruct() {
		initRestTemplateClient();

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

  /** configure HTTP REST client */
  public void initRestTemplateClient() {
    this.rootUri = "http://localhost:"+localServerPort+basePath;
    log.trace("====== configuring RestTemplate HTTP client for "+rootUri);
    this.oauthInterceptor = new OauthInterceptor("http://localhost:"+localServerPort, CLIENT_ID, CLIENT_SECRET, TestFixtures.USER1_EMAIL, TestFixtures.TESTUSER_PASSWORD);
    this.client = new RestTemplateBuilder()
      //.basicAuthorization(TestFixtures.USER1_EMAIL, TestFixtures.USER1_PWD)
      .errorHandler(new LiquidoTestErrorHandler())
      .additionalInterceptors(this.oauthInterceptor)
      .additionalInterceptors(new LogClientRequestInterceptor())
      .rootUri(rootUri)
      .build();
	}




	/*==================
	// MockMvc is nice. But it only mocks the HTTP requests (via a mocked DispatcherSrvlet)
	// Rest Template does really send requests (via network)
	// https://stackoverflow.com/questions/25901985/difference-between-mockmvc-and-resttemplate-in-integration-tests

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private FilterChainProxy springSecurityFilterChain;

	// HTTP client configured for Oauth
	private MockMvc mockMvc;


	@Before
	public void setupMockMvc() {

		this.mockMvc = MockMvcBuilders
				.webAppContextSetup(wac)
				.addFilter(springSecurityFilterChain)
				.build();
	}



	/**
	 * get an Oauth access token for this user
	 * @param username email adress
	 * @param password password
	 * @return the Oauth access token for this user
	 * @throws Exception when HTTP request fails

	private String obtainAccessToken(String username, String password) throws Exception {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "password");
		params.add("client_id", "fooClientIdPassword");
		params.add("username", username);
		params.add("password", password);

		ResultActions result = mockMvc
				.perform(post("/oauth/token")
				.params(params)
				.with(httpBasic(CLIENT_ID, CLIENT_SECRET))
				.accept(CONTENT_TYPE))
				.andExpect(status().isOk())
				.andExpect(content().contentType(CONTENT_TYPE));

		String resultString = result.andReturn().getResponse().getContentAsString();
		JacksonJsonParser jsonParser = new JacksonJsonParser();
		return jsonParser.parseMap(resultString).get("access_token").toString();

	}

	@Test
	public void givenNoToken_whenGetSecureRequest_thenUnauthorized() throws Exception {
		mockMvc.perform(get("/areas")).andExpect(status().isUnauthorized());
	}

	*/

	//========= Tests with real REST calls =============

	@Test
	public void testOauthInterceptor() throws Exception {
		OauthInterceptor oauthInterceptor = new OauthInterceptor("http://localhost:"+localServerPort, CLIENT_ID, CLIENT_SECRET, TestFixtures.USER1_EMAIL, TestFixtures.TESTUSER_PASSWORD);
		String oauthAccessToken = oauthInterceptor.getOauthAccessToken();
		assertNotNull("Oauth access_token must not be null", oauthAccessToken);
		assertTrue("Oauth access_token must be at least 5 chars long", oauthAccessToken.length() > 5);
	}


  /*
   * I tried A LOT with this auto injected TestRestTemplate. But it doesn't work.
   * - First of all it doesn't take basePath into account :-(
   * - It doesn't support PATCH requests
   * -
   *
   * REST client that is automatically configured with localServerPort of running test server.
   * use with WebEnvironment.RANDOM_PORT   (when SpringBootTest starts the server)
   * client is further configured in method.

  TestRestTemplate testRestTemplate;

  @Autowired
  public void setTestRestTemplate(TestRestTemplate testRestTemplate) {
    log.debug("configuring TestRestTemplate for " + basePath);
    //BUGFIX: TestRestClient does not take application.properties  spring.data.rest.base-path  into account. We must manually configure this.
    // https://jira.spring.io/browse/DATAREST-968
    // https://github.com/spring-projects/spring-boot/issues/7816

    //client.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());  // must replace  default SimpleClientHttpRequestFactory because it cannot handle PATCH requests http://stackoverflow.com/questions/29447382/resttemplate-patch-request
    testRestTemplate.getRestTemplate().setUriTemplateHandler(new RootUriTemplateHandler("http://localhost:"+ localServerPort + basePath));
    testRestTemplate.getRestTemplate().getInterceptors().add(new LogClientRequestInterceptor());
    testRestTemplate.getRestTemplate().getInterceptors().add(new BasicAuthorizationInterceptor(TestFixtures.USER1_EMAIL, TestFixtures.USER1_PWD));
    //TODO:  for API keys
    //testRestTemplate.getRestTemplate().setDefaultUriVariables();
    this.testRestTemplate = testRestTemplate;
  }
  */


 /*
  THESE WERE SOME TRIES TO CONFIGURE TestRestTemplate  WHEN RUNNING WITH WebEnvironment.NONE
  // http://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#boot-features-rest-templates-test-utility

  private TestRestTemplate client = new TestRestTemplate();

  @TestConfiguration
  static class Config {

    @Bean
    public RestTemplateBuilder restTemplateBuilder() {
      String rootUri = "http://localhost:"+localServerPort+basePath;
      log.trace("Creating and configuring RestTemplate for "+rootUri);
      return new RestTemplateBuilder()
        .basicAuthorization(TestFixtures.USER1_EMAIL, TestFixtures.USER1_PWD)
        .errorHandler(new LiquidoTestErrorHandler())
        //.requestFactory(new HttpComponentsClientHttpRequestFactory())
        .additionalInterceptors(new LogRequestInterceptor())
        .rootUri(rootUri);
    }
  }

  */




  /*
   * This will run before every single test case!
   *
  @Before
  public void beforeEachTest() {
    log.trace("Setting up JacksonTester for JSON assertions");
    ObjectMapper objectMapper = new ObjectMapper();
    // Possibly configure the mapper
    JacksonTester.initFields(this, objectMapper);
  }
   */

  /**
   * Entry and exit logging for <b>all</b> test cases. Jiipppiiee. Did I already mention that I am a logging fanatic *G*
   */
  @Rule
  public TestWatcher slf4jTestWatcher = new TestWatcher() {
    @Override
    protected void starting(Description descr) {
      log.trace("=========== TEST STARTING "+descr.getClassName()+"."+descr.getMethodName()+": "+descr.getDisplayName());
    }

    @Override
    protected void failed(Throwable e, Description descr) {
      log.error("=========== TEST FAILED "+descr.getClassName()+"."+descr.getMethodName()+": "+descr.getDisplayName());
      log.error(e.getMessage());
    }

    @Override
    protected void succeeded(Description descr) {
      log.trace("=========== TEST SUCCEDED "+descr.getClassName()+"."+descr.getMethodName()+": "+descr.getDisplayName());
    }


  };


  @Test
  public void testPostNewArea() throws JSONException {
    log.trace("TEST POST new area");

    String areaTitle = "This is a newly created Area "+System.currentTimeMillis();  // make test repeatable: Title must be unique!

    JSONObject newAreaJSON = new JSONObject()
      .put("title", areaTitle)
      .put("description", "Very nice description for new area");

    log.trace("posting JSON Object:\n"+newAreaJSON.toString(2));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(newAreaJSON.toString(), headers);

    ResponseEntity<AreaModel> response = client.exchange("/areas", POST, entity, AreaModel.class);

    assertEquals("expected HttpStatus.CREATED(201)", HttpStatus.CREATED, response.getStatusCode());
    AreaModel createdArea = response.getBody();
    assertEquals(areaTitle, createdArea.getTitle());

    log.trace("TEST SUCCESSFUL: new area created: "+createdArea);
  }

  @Test
  public void testPatchArea() throws JSONException {
    log.trace("TEST PATCH area");

    String newDescription = "Updated description";
    JSONObject newAreaJSON = new JSONObject()
      .put("description", newDescription);

    log.trace("JSON Payload for PATCH request: "+newAreaJSON.toString());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(newAreaJSON.toString(), headers);

    Long areaId = this.areas.get(0).getId();
    ResponseEntity<AreaModel> response = client.exchange("/areas/"+areaId, PATCH, entity, AreaModel.class);

    AreaModel updatedArea = response.getBody();
    assertEquals(newDescription, updatedArea.getDescription());

    log.trace("TEST SUCCESSFUL: updated area : "+updatedArea);
  }

  @Test
  //@WithUserDetails(value="testuser0@liquido.de", userDetailsServiceBeanName="liquidoUserDetailsService")    HTTP BasicAuth is already configured above
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
  public void testPostAlternativeProposal() throws JSONException {
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

    JSONObject newLawJson = new JSONObject()
      .put("title", newLawTitle)
      .put("description", "Dummy description from testPostProposalForLaw")
      .put("status", LawModel.LawStatus.IDEA)     //TODO: Actually the server should decide about the status.
      .put("area", areaUri)
      .put("poll", pollUri);
      // Remark: it is not necessary to send a createdBy user URI

    log.trace("posting JSON Object:\n"+newLawJson.toString(2));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(newLawJson.toString(), headers);

    LawModel createdLaw = client.postForObject("/laws", entity, LawModel.class);  // this actually deserializes the response into a LawModel. But that's ok. Makes the assertions much easier than digging around in a plain String response.
    assertNotNull("ERROR: could not post proposal for new law", createdLaw);   // createdLaw will be null, when there was an error.  (I hate methods that return null instead of throwing exceptions!)
    assertEquals("ERROR: create law title does not match", newLawTitle, createdLaw.getTitle());

    log.trace("TEST postAlternativeProposal successfully created "+createdLaw);
  }

  //Problems I had when implementing this test.
  // http://stackoverflow.com/questions/40986738/spring-data-rest-no-string-argument-constructor-factory-method-to-deserialize/40986739
  // https://jira.spring.io/browse/DATAREST-687
  // https://jira.spring.io/browse/DATAREST-884

  @Test
  public void testPostBallot() throws JSONException {
    log.trace("TEST postBallot");

    // ===== Find a poll tha is in VOTING phase
    List<PollModel> polls = pollRepo.findByStatus(PollModel.PollStatus.VOTING);
    assertTrue("Need a poll that currently is in VOTING phase for this test", polls != null && polls.size() > 0);
    PollModel pollInVoting = polls.get(0);
    assertTrue("Need a poll that has at least two alternative proposals", pollInVoting.getProposals().size() >= 2);

    // ===== Create a ballot with a voteOrder
    Iterator<LawModel> alternativeProposals = pollInVoting.getProposals().iterator();
    String pollUri       = basePath + "/polls/" + pollInVoting.getId();
    String voteOrderUri1 = basePath + "/laws/" + alternativeProposals.next().getId();
    String voteOrderUri2 = basePath + "/laws/" + alternativeProposals.next().getId();

    JSONObject newBallotJson = new JSONObject()
        .put("poll", pollUri)
        .put("voteOrder", new JSONArray()
                                .put(voteOrderUri1)
                                .put(voteOrderUri2));
    log.trace("posting JSON Object:\n"+newBallotJson.toString(2));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(newBallotJson.toString(), headers);

    // Do not use client.postForObject here. It does not return any error. It simply returns null instead!
    // Endpoint is /postBallot    /ballots are not exposed as @RepositoryRestResource for writing!
    ResponseEntity<String> response = client.exchange("/postBallot", POST, entity, String.class);
    log.debug("Response body:\n"+response.getBody());

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    ReadContext ctx = JsonPath.parse(response.getBody());
    String returnedVoterToken = ctx.read("$.ballotToken");
    assertTrue("Expected a voter token", returnedVoterToken != null && returnedVoterToken.length() > 10);
    Long delegees = ctx.read("$.delegees", Long.class);
    assertTrue("Expteded delegees to be a positive number.", delegees > 0);

    log.trace("TEST SUCCESSFUL: new ballot successfully posted.");
  }

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


  /**
   * Create a new idea. Then add as many supporters, so that the idea reaches its quorum.
   * A new poll will then automatically be created.
   * Test for {@link org.doogie.liquido.services.LawService#checkQuorum(LawModel)}
   */
  @Test
  // USER1 is logged in via HTTP client.
  // This does not work for REST tests: @WithUserDetails(value=TestFixtures.USER0_EMAIL, userDetailsServiceBeanName="liquidoUserDetailsService")
  public void testIdeaReachesQuorum() throws JSONException {
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
   * Helper to create a new idea (via REST)
   * @param ideaTitlePrefix the title of the idea. A random number will be added,
   *                        because title MUST be unique.
   * @return the created idea (but without dependant entities such  as area and createdBy filled!)
   */
  private LawModel postNewIdea(String ideaTitlePrefix) throws JSONException {
    String ideaTitle = ideaTitlePrefix+" "+System.currentTimeMillis();;  // title must be unique!
    String ideaDesc  = "This idea was created from a test case";
    String areaUri   = basePath + "/areas/" + this.areas.get(0).getId();

    JSONObject ideaJson = new JSONObject()
            .put("title", ideaTitle)
            .put("description", ideaDesc)
            .put("area", areaUri);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(ideaJson.toString(), headers);

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
  public void testGetProxyMap() {
    log.trace("TEST getProxyMap");
    String uri = "/my/proxyMap";
		String delegeeEMail = TestFixtures.delegations.get(0)[0];
		String proxyEMail   = TestFixtures.delegations.get(0)[1];

		// send request as the delegee who assigned his vote to a proxy
		this.oauthInterceptor.setUsername(delegeeEMail);
    String proxyMapJson = client.getForObject(uri, String.class);

    log.trace("got Proxy Map:\n"+proxyMapJson);
    String actualProxyInArea = JsonPath.read(proxyMapJson, "$['"+TestFixtures.AREA_FOR_DELEGATIONS+"'].directProxy.email");
    assertEquals(delegeeEMail+" should have "+proxyEMail+" as proxy", proxyEMail, actualProxyInArea);
  }

  /**
   * This updates a delegation and changes the toProxy via PUT to the /saveProxy endpoint
   */
  @Test
  public void testAssignProxy() throws JSONException, LiquidoException {
    String url = "/assignProxy";
    UserModel fromUser = this.users.get(10);
    UserModel toProxy  = this.users.get(11);
    AreaModel area     = this.areas.get(0);
    String toProxyUri  = basePath + "/users/" + toProxy.getId();
    String areaUri     = basePath + "/areas/" + area.getId();
    String voterToken  = castVoteService.createVoterToken(fromUser, area, fromUser.getPasswordHash());

    //TODO: delete delegation if it exists:  proxyServcie.removeProxy(...)



    JSONObject newDelegationJSON = new JSONObject()
      //.put("fromUser", fromUserUri)    fromUser is implicitly the currently logged in user!
      .put("toProxy",  toProxyUri)
      .put("area",     areaUri)
      .put("voterToken", voterToken);
    log.trace("posting JSON Object:\n"+newDelegationJSON.toString(2));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(newDelegationJSON.toString(), headers);

    // send PUT that will assign the new proxy
    ResponseEntity<String> response = client.exchange(url, PUT, entity, String.class);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    String updatedDelegationJson = response.getBody();

    String actualProxyEmail = JsonPath.read(updatedDelegationJson, "$.toProxy.email");
    assertEquals("expected toProxy ID to be updated", toProxy.getEmail(), actualProxyEmail);
  }

  @Test
  public void testPostDuplicateArea() throws JSONException {
    log.trace("TEST postDuplicateArea");

    String createdByUri  = basePath + "/users/" + this.users.get(0).getId();

    JSONObject duplicateAreaJson = new JSONObject()
      .put("title", TestFixtures.AREA1_TITLE)           // area with that title already exists.
      .put("description", "duplicate Area from test")
      .put("createdBy", createdByUri);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<>(duplicateAreaJson.toString(), headers);

    ResponseEntity<String> responseEntity = client.postForEntity("/areas", entity, String.class);  // This will return HTTP status 409(Conflict) because of the duplicate composite key.

    log.trace("responseEntity:\n" + responseEntity);
    assertEquals("Expected HTTP error code 409 == conflict", responseEntity.getStatusCode(), HttpStatus.CONFLICT);  // status == 409
    log.trace("TEST postDuplicateArea SUCCESS: Did receive expected HttpStatus=409 (Conflict)");
  }

  @Test
  public void testGetGlobalProperties() {
    log.trace("TEST getGlobalProperties");

    String responseJSON = client.getForObject("/globalProperties", String.class);

    assertNotNull(responseJSON);
  }

  /**
   * GIVEN an idea that reached its quorum and became a proposal
   * WHEN  author of this idea creates a new poll
   * THEN  the poll is in state ELABORATION
   * AND   the idea is the initial proposal in this poll.
   */
  @Test
  public void testCreateNewPoll() {

  }

}
