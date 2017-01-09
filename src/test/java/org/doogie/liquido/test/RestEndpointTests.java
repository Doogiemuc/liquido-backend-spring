package org.doogie.liquido.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.*;
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
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.ResponseErrorHandler;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

  /*

  THESE WERE SOME TRIES TO CONFIGRE TestRestTemplate  WHEN RUNNING WITH WebEnvironment.NONE
  private TestRestTemplate client = new TestRestTemplate();

  @TestConfiguration
  static class Config {

    @Bean
    public RestTemplateBuilder restTemplateBuilder() {
      return new RestTemplateBuilder()
          .additionalMessageConverters(...)
				.customizers(...);
    }

  }

  @Bean
  RestTemplateBuilder restTemplateBuilder() {
    //BUG: This is never called: https://github.com/spring-projects/spring-boot/issues/6465
    log.trace("========== resteTemplateBuilder" + localServerPort);
    return new RestTemplateBuilder();
  }

  */

  /**
   * REST client that is automatically configured with localServerPort of running test server.
   * use with WebEnvironment.RANDOM_PORT   (when SpringBootTest starts the server)
   */
  @Autowired
  TestRestTemplate client;

  @LocalServerPort
  int localServerPort;

  @Autowired
  UserRepo userRepo;

  @Autowired
  AreaRepo areaRepo;

  @Autowired
  LawRepo lawRepo;

  @Autowired
  DelegationRepo delegationRepo;

  @Value(value = "${spring.data.rest.base-path}")   // from application.properties
  String basePath;

  @Autowired
  Environment springEnv;


  // preloaded data that most test cases need.
  List<UserModel> users;
  List<AreaModel> areas;
  List<LawModel>  laws;

  /** Custom ErrorResponseHandler to catch (and store) the response body in case of an HTTP error. */
  class LiquidoTestErrorHandler implements ResponseErrorHandler {
    @Override
    public boolean hasError(ClientHttpResponse clientHttpResponse) throws IOException {
      return clientHttpResponse.getStatusCode() != HttpStatus.OK &&
             clientHttpResponse.getStatusCode() != HttpStatus.CREATED;
    }
    @Override
    public void handleError(ClientHttpResponse clientHttpResponse) throws IOException {
      //Cannot read the body, cause InputStream can only be read once lastErrorResponseBody = DoogiesUtil._stream2String(clientHttpResponse.getBody());
      log.error("HTTP error: ("+clientHttpResponse.getRawStatusCode() + ") "+clientHttpResponse.getStatusText());
    }
  }

  /**
   * this is executed, when the Bean has been created and @Autowired references are injected and ready.
   */
  @PostConstruct
  public void postConstruct() {
    // Here you can do any one time setup necessary
    log.trace("PostConstruct: pre-fetching data from DB");

    //client.getRestTemplate().setErrorHandler(new LiquidoTestErrorHandler());

    //BUGFIX: TestRestClient does not take application.properties  spring.data.rest.base-path  into account. We must manually configure this.
    // https://jira.spring.io/browse/DATAREST-968
    // https://github.com/spring-projects/spring-boot/issues/7816
    String basePath = springEnv.getProperty("spring.data.rest.base-path");
    client.getRestTemplate().setUriTemplateHandler(new RootUriTemplateHandler("http://localhost:"+ localServerPort + basePath));

    //TODO:  for API keys
    //client.getRestTemplate().setDefaultUriVariables();

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
   * Keep in mind that this will run before every single test case!
   */
  @Before
  public void beforeEachTest() {
    log.trace("This runs before each single test case");
    //TODO: handle login  (maybe use a special static APP_ID for tests.
  }

  @Test
  public void testFindMostRecentIdeas() throws IOException {
    log.trace("TEST testFindMostRecentIdeas");
    String response = client.getForObject("/ideas/search/recentIdeas", String.class);

    log.debug(response);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(response);
    JsonNode ideas = node.get("_embedded").get("ideas");
    assertTrue(ideas.isArray());
    assertTrue(ideas.size() > 5);

    log.trace("TEST SUCCESSFULL testFindMostRecentIdeas  found "+ideas.size()+" ideas");
  }

  /*    Post a new entity with RestTempalte => Do I need this when using TestRestTemplate or are the mappers already ocnfigured?

        // http://stackoverflow.com/questions/27414922/unable-to-post-new-entity-with-relationship-using-resttemplate-and-spring-data-r?rq=1
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJacksonHttpMessageConverter());
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());

        String uri = new String("url");

        Bar b= new Bar();
        bar.setName("newWgetBar");

        rt.postForObject(uri, b, Bar.class);

   */

  @Test
  public void testPostProposalForALaw() {
    log.trace("TEST postBallot");

    String newLawTitle = "Law from test "+System.currentTimeMillis() % 10000;  // law.title must be unique!!

    String initialLawUri = basePath + "/laws/" + this.laws.get(0).getId();
    String createdByUri = basePath + "/users/" + this.users.get(0).getId();

    // I am deliberately not creating a new BallotModel(...) here that I could then   postForEntity like this:
    //   ResponseEntity<BallotModel> createdBallot = client.postForEntity("/ballot", newBallot, BallotModel.class);
    // because I do not want the test to success just because of on spring's very clever serialization and deserialization.
    // Instead I want to post plain JSON as a client would:
    JSONObject newLawJson = new JSONObject()
      .put("title", newLawTitle)
      .put("description", "Dummy description from testPostProposalForLaw")
      .put("initialLaw", initialLawUri)
      .put("createdBy", createdByUri);
    log.trace("posting JSON Object:\n"+newLawJson.toString(2));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(newLawJson.toString(), headers);

    LawModel createdLaw = client.postForObject("/laws", entity, LawModel.class);  // this actually deserializes the response into a BallotModel. But that's ok. Makes the assertions much easier than digging around in a plain String response.
    assertNotNull(createdLaw);   // createdLaw will be null, when there was an error.
    assertEquals(newLawTitle, createdLaw.getTitle());

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

  @Test
  public void testGetNumVotes() {
    log.trace("TEST getNumVotes");
    UserModel user4 = userRepo.findByEmail(TestFixtures.USER4_EMAIL);
    AreaModel area1 = areaRepo.findByTitle(TestFixtures.AREA1_TITLE);
    String uri = "/users/"+user4.getId()+"/getNumVotes?areaId="+area1.getId();

    Integer numVotes = client.getForObject(uri, Integer.class);

    assertNotNull(numVotes);
    assertEquals("User "+TestFixtures.USER4_EMAIL+" should have "+TestFixtures.USER4_NUM_VOTES+" delegated votes", TestFixtures.USER4_NUM_VOTES, numVotes.intValue());
    log.trace("TEST SUCCESS: found expected "+TestFixtures.USER4_NUM_VOTES+" delegations for "+TestFixtures.USER4_EMAIL);
  }

  @Test
  public void testPostDelegation() {
    log.trace("TEST postDelegation");

    //Implementation note: I tried doing this via the auto generated /liquido/v2/delegations endpoint.  But it only support POST a new item. I need an upsert operation here.
    //String url = "/users/"+this.users.get(0).getId()+"/delegations";
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

    JSONObject duplicateAreaJson = new JSONObject()
      .put("title", TestFixtures.AREA1_TITLE)           // area with that title already exists.
      .put("description", "duplicate Area from test");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(duplicateAreaJson.toString(), headers);

    ResponseEntity<String> responseEntity = client.postForEntity("/areas", entity, String.class);  // This will return HTTP status 409(Conflict) because of the duplicate composite key.

    log.trace("responseEntity:\n" + responseEntity);
    assertEquals(responseEntity.getStatusCode(), HttpStatus.CONFLICT);  // status == 409
    log.trace("TEST postDuplicateArea SUCCESS: Did receive expected HttpStatus=409 (Conflict)");
  }
}
