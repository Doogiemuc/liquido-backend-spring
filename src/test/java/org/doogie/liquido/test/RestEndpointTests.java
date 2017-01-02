package org.doogie.liquido.test;

import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.*;
import org.doogie.liquido.util.DoogiesUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)  // This is so cool. This automatically sets up everything and starts the server. *like*
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)  // Only run tests. Do not automatically start a server.
public class RestEndpointTests {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  /*
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
  */
  /*   use with WebEnvironment.RANDOM_PORT   (when SpringBootTest starts the server)
  @Autowired
  TestRestTemplate client;    // REST client that is automatically configured with localServerPort of running test server.
                              // BUG: but wit the wrong base url
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

  @Bean
  RestTemplateBuilder restTemplateBuilder() {
    //BUG: This is never called: https://github.com/spring-projects/spring-boot/issues/6465
    log.trace("========== resteTemplateBuilder" + localServerPort);
    return new RestTemplateBuilder();
  }

  /* HTTP body of last error response */
  String lastErrorResponseBody = "";

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
      lastErrorResponseBody = DoogiesUtil._stream2String(clientHttpResponse.getBody());
      log.error("HTTP error: "+lastErrorResponseBody);
    }
  }

  /**
   * this is executed, when the Bean has been created and @Autowired references are injected and ready.
   */
  @PostConstruct
  public void postConstruct() {
    log.info("Spring tests started");
    // Here you can do any one time setup necessary
    log.trace("preloading data from DB");
    for (UserModel userModel : userRepo.findAll()) {
      this.users.add(userModel);
    }
    log.trace("loaded "+this.users.size()+ " users");
    for (AreaModel areaModel : areaRepo.findAll()) {
      this.areas.add(areaModel);
    }
    log.trace("loaded "+this.areas.size()+ " areas");
    for (LawModel lawModel : lawRepo.findAll()) {
      this.laws.add(lawModel);
    }
    log.trace("loaded "+this.laws.size()+ " laws");

    client.getRestTemplate().setErrorHandler(new LiquidoTestErrorHandler());
    //client.getRestTemplate().setDefaultUriVariables();   //TODO:  for API keys
    client.getRestTemplate().setUriTemplateHandler(new RootUriTemplateHandler("http://localhost:"+ localServerPort +TestFixtures.BASE_URL));
  }

  /**
   * pre load some data from the DB.
   * Keep in mind that this will run before every single test case!
   */
  @Before
  public void beforeEachTest() {
    log.trace("This runs before each single test case");
  }

  @Test
  public void testDelegationObjectIdConversion() {
    log.trace("TEST testDelegationObjectIdConversion");

    String result = client.getForObject("/delegations", String.class);

    //log.debug(result);
    //TODO: assert
    log.info("TEST testDelegationObjectIdConversion");
  }

  @Test
  public void testFindMostRecentIdeas() {
    log.trace("TEST testFindMostRecentIdeas");
    List<IdeaModel> recentIdeas = client.getForObject("/getRecentIdeas", List.class);
    log.debug("Got size="+recentIdeas.size());
    assertTrue(recentIdeas.size() > 8);

  }


  @Test
  public void testPostBallot() {
    final String VOTER_HASH = "dummyUserHashFromTest";  // test fixture

    log.trace("TEST postBallot");

    //TODO: find a law that is currently in the voting phase
    //TODO: create a random voteOrder of (some of) its competing proposals
    //TODO: calculate new(!) userHash  (make test repeatable!)

    // I am deliberately not creating a new BallotModel(...) here that I could then   postForEntity like this:
    //   ResponseEntity<BallotModel> createdBallot = client.postForEntity("/ballot", newBallot, BallotModel.class);
    // because I do not want the test to success just because of on spring's very clever serialization and deserialization.
    //
    // Instead I want to post plain JSON as a client would:
    JSONObject newBallotJson = new JSONObject()
        .put("voterHash", VOTER_HASH)
        .put("initialLawId", this.laws.get(0).getId())
        .put("voteOrder", new JSONArray()
                                .put(this.laws.get(1).getId())
                                .put(this.laws.get(2).getId()) );
    log.trace("posting JSON Object:\n"+newBallotJson.toString(2));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(newBallotJson.toString(), headers);

    BallotModel createdBallot = client.postForObject("/ballot", entity, BallotModel.class);  // this actually deserializes the response into a BallotModel. But that's ok. Makes the assertions much easier than digging around in a plain String response.
    assertNotNull(createdBallot);
    assertEquals(VOTER_HASH, createdBallot.getVoterHash());

    log.trace("TEST postBallot successfully created "+createdBallot);
  }

  @Test
  public void testPostInvalidBallot() {
    log.trace("TEST postInvalidBallot");
    BallotModel invalidBallot = new BallotModel(null, new ArrayList<>(), "invalidVoterHash");   // this is an invalid ballot!

    ResponseEntity<BallotModel> response = client.postForEntity("/ballot", invalidBallot, BallotModel.class);

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCodeValue());  // 400
    assertTrue(lastErrorResponseBody.contains("NotNull.initialLawId"));
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
    log.trace("TEST SUCCESS: found expected delegations for "+TestFixtures.USER4_EMAIL);
  }

  @Test
  public void testSaveProxy() {
    log.trace("TEST saveProxy");

    String url = "/users/"+this.users.get(0).getId()+"/delegations";
    //Implementation note: I tried doing this via the auto generated /liquido/v2/delegations endpoint.  But it only support POST a new item. I need an upsert operation here.

    //I am deliberately not using DelegationModel here. This is the JSON as a client would send it.
    JSONObject newDelegationJSON = new JSONObject()
        .put("fromUser", this.users.get(0).getId())
        .put("toProxy",  this.users.get(1).getId())
        .put("area",     this.areas.get(3).getId());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(newDelegationJSON.toString(), headers);

    ResponseEntity<String> result = client.exchange(url, HttpMethod.PUT, entity, String.class);

    assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());

    log.trace("TEST SUCCESS: saved delegation to proxy successfully");
  }

  /**
   * try to save a delegation with an invalid objectID
   */
  @Test
  public void testSaveInvalidProxy() {
    log.trace("TEST saveProxy");

    String url = "/delegations";

    //I am deliberately not using DelegationModel here. This is the JSON as a client would send it.
    JSONObject newDelegationJSON = new JSONObject()
      .put("fromUser", this.users.get(0).getId())
      .put("toProxy",  "ffffffffffffffffffffffff")   // invalid userID  (but still a valid mongo ObjectId
      .put("area",     this.areas.get(3).getId());

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

    String url = "/liquido/v2/areas";
    JSONObject duplicateAreaJson = new JSONObject()
      .put("title", TestFixtures.AREA1_TITLE)           // area with that title already exists.
      .put("description", "duplicate Area from test");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(duplicateAreaJson.toString(), headers);

    ResponseEntity<String> responseEntity = client.postForEntity(url, entity, String.class);  // This will return HTTP status 409(Conflict) because of the duplicate composite key.

    log.trace("responseEntity:\n" + responseEntity);
    assertEquals(responseEntity.getStatusCode(), HttpStatus.CONFLICT);  // status == 409
    log.trace("TEST postDuplicateArea SUCCESS: Did receive expected HttpStatus=409 (Conflict)");
  }
}
