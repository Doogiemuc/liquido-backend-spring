package org.doogie.liquido.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.test.testUtils.LiquidoTestErrorHandler;
import org.doogie.liquido.test.testUtils.LogClientRequestInterceptor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration test for Liquiodo REST endpoint and mongoDB.
 *
 * These test cases test the Liquido Java backend together with the mongo database behind it.
 * It would be quite costly to mock the complete DB. So we test both layers here.
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)  // This is so cool. This automatically sets up everything and starts the server. *like*
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)  // Only run tests. Do not automatically start a server.
public class RestEndpointTests {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  /** base URL from application.properties */
  @Value(value = "${spring.data.rest.base-path}")
  String basePath;

  /** (random) port of local backend under test */
  @LocalServerPort
  int localServerPort;

  // My spring-data-jpa repositories for loading test data directly
  @Autowired
  UserRepo userRepo;

  @Autowired
  AreaRepo areaRepo;

  @Autowired
  LawRepo lawRepo;

  @Autowired
  DelegationRepo delegationRepo;


  // preloaded data that most test cases need.
  List<UserModel> users;
  List<AreaModel> areas;
  List<LawModel>  laws;

  //@Autowired
  //Environment springEnv;

  /** our HTTP REST client */
  RestTemplate client;

  /** configure HTTP REST client */
  public void initRestTemplateClient() {
    String rootUri = "http://localhost:"+localServerPort+basePath;
    log.trace("====== configuring REST client for "+rootUri);
    this.client = new RestTemplateBuilder()
      .basicAuthorization(TestFixtures.USER1_EMAIL, TestFixtures.USER1_PWD)
      .errorHandler(new LiquidoTestErrorHandler())
      //.requestFactory(new HttpComponentsClientHttpRequestFactory())
      .additionalInterceptors(new LogClientRequestInterceptor())
      .rootUri(rootUri)
      .build();
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


  /**
   * this is executed, when the Bean has been created and @Autowired references are injected and ready.
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
    for (AreaModel areaModel : areaRepo.findAll()) {
      this.areas.add(areaModel);
    }
    log.trace("loaded "+this.areas.size()+ " areas");

    this.laws = new ArrayList<>();
    for (LawModel lawModel : lawRepo.findAll()) {
      this.laws.add(lawModel);
    }
    log.trace("loaded "+this.laws.size()+ " laws");

  }

  /**
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




  @Test
  public void testPostNewArea() {
    log.trace("TEST POST new area");

    String areaTitle = "This is a newly created Area "+System.currentTimeMillis() % 10000;  // make test repeatable: Title must be unique!

    JSONObject newAreaJSON = new JSONObject()
      .put("title", areaTitle)
      .put("description", "Very nice description for new area");

    log.trace("posting JSON Object:\n"+newAreaJSON.toString(2));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(newAreaJSON.toString(), headers);

    ResponseEntity<AreaModel> response = client.exchange("/areas", HttpMethod.POST, entity, AreaModel.class);

    assertEquals("expected HttpStatus.CREATED(201)", HttpStatus.CREATED, response.getStatusCode());
    AreaModel createdArea = response.getBody();
    assertEquals(areaTitle, createdArea.getTitle());

    log.trace("TEST SUCCESSFUL: new area created: "+createdArea);
  }

  @Test
  public void testPatchArea() {
    log.trace("TEST PATCH area");

    String newDescription = "Updated description";
    JSONObject newAreaJSON = new JSONObject()
      .put("description", newDescription);

    log.trace("JSON Payload for PATCH request: "+newAreaJSON.toString());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(newAreaJSON.toString(), headers);

    Long areaId = this.areas.get(0).getId();
    ResponseEntity<AreaModel> response = client.exchange("/areas/"+areaId, HttpMethod.PATCH, entity, AreaModel.class);

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
      .getForObject("/ideas/search/recentIdeas", String.class);

    log.debug("got response: "+response);

    assertNotNull("Could not get recent ideas", response);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(response);
    JsonNode ideas = node.get("_embedded").get("ideas");
    assertTrue(ideas.isArray());
    assertTrue(ideas.size() > 5);

    log.trace("TEST SUCCESSFULL testFindMostRecentIdeas  found "+ideas.size()+" ideas");
  }

  @Test
  public void testPostProposalForALaw() {
    log.trace("TEST postBallot");

    String newLawTitle = "Law from test "+System.currentTimeMillis() % 10000;  // law.title must be unique!!

    String areaUri       = basePath + "/areas/" + this.areas.get(0).getId();
    String initialLawUri = basePath + "/laws/"  + this.laws.get(0).getId();
    String createdByUri  = basePath + "/users/" + this.users.get(0).getId();

    // I am deliberately not creating a new BallotModel(...) here that I could then   postForEntity like this:
    //   ResponseEntity<BallotModel> createdBallot = client.postForEntity("/ballot", newBallot, BallotModel.class);
    // because I do not want the test to success just because of on spring's very clever serialization and deserialization.
    // Instead I want to post plain JSON as a client would:
    JSONObject newLawJson = new JSONObject()
      .put("title", newLawTitle)
      .put("description", "Dummy description from testPostProposalForLaw")
      .put("status", LawModel.LawStatus.NEW_ALTERNATIVE_PROPOSAL)
      .put("area", areaUri)
      .put("initialLaw", initialLawUri)
      .put("createdBy", createdByUri);

    log.trace("posting JSON Object:\n"+newLawJson.toString(2));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(newLawJson.toString(), headers);

    LawModel createdLaw = client.postForObject("/laws", entity, LawModel.class);  // this actually deserializes the response into a BallotModel. But that's ok. Makes the assertions much easier than digging around in a plain String response.
    assertNotNull("ERROR: could not post proposal for new law", createdLaw);   // createdLaw will be null, when there was an error.
    assertEquals("ERROR: create law title does not match", newLawTitle, createdLaw.getTitle());

    log.trace("TEST postBallot successfully created "+createdLaw);
  }


  @Test
  public void testPostBallot() {
    log.trace("TEST postBallot");

    //find a proposal  that is currently in the voting phase and its alternatives
    List<LawModel> inVotingPhase = lawRepo.findByStatus(LawModel.LawStatus.VOTING);
    assertTrue("Need a proposal that currently is in voting phase", inVotingPhase != null && inVotingPhase.size() > 0);
    List<LawModel> alternativeProposals = lawRepo.findByInitialLaw(inVotingPhase.get(0));

    //TODO: create a random voteOrder of (some of) its competing proposals
    //TODO: calculate new(!) userHash  (make test repeatable!)

    // I am deliberately not creating a new BallotModel(...) here that I could then   postForEntity like this:
    //   ResponseEntity<BallotModel> createdBallot = client.postForEntity("/ballot", newBallot, BallotModel.class);
    // because I do not want the test to success just because of on spring's very clever serialization and deserialization.
    // Instead I want to post plain JSON as a client would:

    //Problems I had
    // http://stackoverflow.com/questions/40986738/spring-data-rest-no-string-argument-constructor-factory-method-to-deserialize/40986739
    // https://jira.spring.io/browse/DATAREST-687
    // https://jira.spring.io/browse/DATAREST-884

    String voterHash = "dummyUserHashFromTest";
    String initialLawUri = basePath + "/laws/" + inVotingPhase.get(0).getId();
    String voteOrderUri1 = basePath + "/laws/" + alternativeProposals.get(0).getId();
    String voteOrderUri2 = basePath + "/laws/" + alternativeProposals.get(1).getId();

    JSONObject newBallotJson = new JSONObject()
        .put("voterHash", voterHash)
        .put("initialProposal", initialLawUri)
        .put("voteOrder", new JSONArray()
                                .put(voteOrderUri1)
                                .put(voteOrderUri2));
    log.trace("posting JSON Object:\n"+newBallotJson.toString(2));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(newBallotJson.toString(), headers);

    //do not use client.postForObject   it does not return any error. It simply returns null instead!
    // correct endpoint is my own /postBallot implementation.    /ballots are not exposed via HATEOAS !!
    ResponseEntity<String> response = client.exchange("/postBallot", HttpMethod.POST, entity, String.class);

    log.debug("Response body:\n"+response.getBody());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody().contains(voterHash));

    log.trace("TEST SUCCESSFUL: new ballot successfully posted.");
  }

  @Test
  public void testPostInvalidBallot() {
    log.trace("TEST postInvalidBallot");

    String voterHash = "dummyUserHashFromTest";
    String initialLawUri = basePath + "/laws/4711"; // This is a nonexistant ID !
    String voteOrderUri1 = basePath + "/laws/" + this.laws.get(1).getId();
    String voteOrderUri2 = basePath + "/laws/" + this.laws.get(2).getId();

    JSONObject newBallotJson = new JSONObject()
      .put("voterHash", voterHash)
      .put("initialProposal", initialLawUri)
      .put("voteOrder", new JSONArray()
        .put(voteOrderUri1)
        .put(voteOrderUri2));
    log.trace("posting JSON Object:\n"+newBallotJson.toString(2));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(newBallotJson.toString(), headers);

    ResponseEntity<String> response = client.exchange("/postBallot", HttpMethod.POST, entity, String.class);

    String responseBody = response.getBody();
    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCodeValue());  // 400
    log.trace("response.body (that should contain the error): "+responseBody);
    assertTrue(responseBody.contains("initialProposal"));
    log.trace("TEST postInvalidBallot successful: received correct status and error message in response.");
  }

  /**
   * User4 should have 5 votes (including his own
   */
  @Test
  public void testGetNumVotes() {
    log.trace("TEST getNumVotes");
    long user4_id = this.users.get(4).getId();
    long area1_id = this.areas.get(1).getId();
    String uri = "/users/"+user4_id+"/getNumVotes/"+area1_id;

    long numVotes = client.getForObject(uri, Long.class);

    assertEquals("User "+TestFixtures.USER4_EMAIL+" should have "+TestFixtures.USER4_NUM_VOTES+" delegated votes", TestFixtures.USER4_NUM_VOTES, numVotes);
    log.trace("TEST SUCCESS: found expected "+TestFixtures.USER4_NUM_VOTES+" delegations for "+TestFixtures.USER4_EMAIL);
  }

  /**
   * User0 should have delegated his vote to User4 in Area1
   */
  @Test
  public void testGetProxyMap() {
    log.trace("TEST getProxyMap");
    long user0_id = this.users.get(0).getId();
    String uri = "/users/"+user0_id+"/getProxyMap";

    String proxyMapJson = client.getForObject(uri, String.class);
    String proxyInArea1_email = JsonPath.read(proxyMapJson, "$['"+TestFixtures.AREA1_TITLE+"'].email");

    assertEquals("User "+TestFixtures.USER0_EMAIL+" should have "+TestFixtures.USER4_EMAIL+" as proxy", TestFixtures.USER4_EMAIL, proxyInArea1_email);
    log.trace("TEST SUCCESS: found expected "+TestFixtures.USER4_NUM_VOTES+" delegations for "+TestFixtures.USER4_EMAIL);
  }


  /**
   * This creates NEW delegation directly by POSTing to the /delegations rest endpoint.
   */
  @Test
  public void testPostNewDelegation() {
    log.trace("TEST postDelegation");

    String url = "/delegations";
    String fromUserUri = basePath + "/users/" + this.users.get(0).getId();
    String toProxyUri  = basePath + "/users/" + this.users.get(1).getId();
    String areaUri     = basePath + "/areas/" + this.areas.get(0).getId();

    //I am deliberately not using DelegationModel here. This is the JSON as a client would send it.
    JSONObject newDelegationJSON = new JSONObject()
        .put("fromUser", fromUserUri)
        .put("toProxy",  toProxyUri)
        .put("area",     areaUri);
    log.trace("posting JSON Object:\n"+newDelegationJSON.toString(2));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(newDelegationJSON.toString(), headers);

    ResponseEntity<String> response = client.exchange(url, HttpMethod.POST, entity, String.class);

    log.debug("Response body:\n"+response.getBody());
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    log.trace("TEST SUCCESS: saved delegation to proxy successfully");
  }

  /**
   * This updates an existing delegation and changes the toProxy via PUT to the /saveProxy endpoint
   */
  @Test
  public void testSaveProxy() throws IOException {
    log.trace("TEST saveProxy");

    String url = "/saveProxy";
    UserModel toProxy  = this.users.get(2);
    String toProxyUri  = basePath + "/users/" + toProxy.getId();
    String areaUri     = basePath + "/areas/" + this.areas.get(0).getId();

    //I am deliberately not using DelegationModel here. This is the plain JSON as any client might send it.
    JSONObject newDelegationJSON = new JSONObject()
      //.put("fromUser", fromUserUri)    fromUser is implicitly the currently logged in user!
      .put("toProxy",  toProxyUri)
      .put("area",     areaUri);
    log.trace("posting JSON Object:\n"+newDelegationJSON.toString(2));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(newDelegationJSON.toString(), headers);

    // send PUT that will create the new Delegation
    ResponseEntity<String> response = client.exchange(url, HttpMethod.PUT, entity, String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String updatedDelegationJson = response.getBody();

    // extract URI of toProxy and make an additional request for the full userDetails
    String proxyURI = JsonPath.read(updatedDelegationJson, "$._links.toProxy.href");
    log.trace("fetching proxy user information from "+proxyURI);
    ResponseEntity<UserModel> responseEntity = client.getForEntity(proxyURI, UserModel.class);

    assertEquals("expected toProxy ID to be updated", responseEntity.getBody().getEmail(), toProxy.getEmail());
    log.trace("TEST SUCCESS: updated proxy successfully to "+toProxy.getEmail());
  }


  /**
   * try to save a delegation with an invalid objectID
   */
  @Test
  public void testPostInvalidDelegation() {
    log.trace("TEST postInvalidDelegation");

    String url = "/delegations";

    String fromUserUri = basePath + "/users/" + this.users.get(0).getId();
    String toProxyUri  = basePath + "/users/" + this.users.get(1).getId();
    String areaUri     = basePath + "/areas/4711";   // INVALID !

    //I am deliberately not using DelegationModel here. This is the JSON as a client would send it.
    JSONObject newDelegationJSON = new JSONObject()
      .put("fromUser", fromUserUri)
      .put("toProxy",  toProxyUri)
      .put("area",     areaUri);
    log.trace("posting JSON Object:\n"+newDelegationJSON.toString(2));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(newDelegationJSON.toString(), headers);

    ResponseEntity<String> result = client.exchange(url, HttpMethod.POST, entity, String.class);

    assertEquals(result.getStatusCode(), HttpStatus.BAD_REQUEST);

    log.trace("TEST SUCCESS: invalid delegation was rejected as expected");
  }

  @Test
  public void testPostDuplicateArea() {
    log.trace("TEST postDuplicateArea");

    String createdByUri  = basePath + "/users/" + this.users.get(0).getId();

    JSONObject duplicateAreaJson = new JSONObject()
      .put("title", TestFixtures.AREA1_TITLE)           // area with that title already exists.
      .put("description", "duplicate Area from test")
      .put("createdBy", createdByUri);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(duplicateAreaJson.toString(), headers);

    ResponseEntity<String> responseEntity = client.postForEntity("/areas", entity, String.class);  // This will return HTTP status 409(Conflict) because of the duplicate composite key.

    log.trace("responseEntity:\n" + responseEntity);
    assertEquals("Expected HTTP error code 409 == conflict", responseEntity.getStatusCode(), HttpStatus.CONFLICT);  // status == 409
    log.trace("TEST postDuplicateArea SUCCESS: Did receive expected HttpStatus=409 (Conflict)");
  }


}
