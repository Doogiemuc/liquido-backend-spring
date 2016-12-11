package org.doogie.liquido.test;

import org.bson.types.ObjectId;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.*;
import org.doogie.liquido.util.DoogiesUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Example;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;

import javax.annotation.PostConstruct;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
  /*   use with WebEnvironment.RANDOM_PORT   (when SpringBootTest starts the server) */
  @Autowired
  TestRestTemplate client;    // REST client that is automatically configured with port of running test server.


  @LocalServerPort
  int port;

  @Autowired
  UserRepo userRepo;

  @Autowired
  AreaRepo areaRepo;

  @Autowired
  LawRepo lawRepo;

  @Autowired
  DelegationRepo delegationRepo;


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

  @PostConstruct
  public void doLogging() {
    log.info("Spring tests started");
    client.getRestTemplate().setErrorHandler(new LiquidoTestErrorHandler());
  }

  /**
   * pre load some data from the DB.
   * Keep in mind that this will run before every single test case!
   */
  @Before
  public void preloadData() {
    log.trace("preloading data from DB");
    this.users = userRepo.findAll();
    this.areas = areaRepo.findAll();
    this.laws  = lawRepo.findAll();
  }

  @Test
  public void testDelegationObjectIdConversion() {
    log.trace("TEST testDelegationObjectIdConversion");

    String uri = "/liquido/v2/delegations";
    String result = client.getForObject(uri, String.class);

    log.debug(result);

    log.info("TEST testDelegationObjectIdConversion");
  }


  @Test
  public void testPostBallot() {
    final String VOTER_HASH = "dummyUserHashFromTest";  // test fixture

    log.trace("TEST postBallot");

    //TODO: find a law that is currently in the voting phase
    //TODO: create a random voteOrder of (some of) its competing proposals
    //TODO: calculate userHash

    // I am deliberately not createing a new BallotModel(...) here that I could then   postForEntity like this:
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
    BallotModel invalidBallot = new BallotModel("voterHash", null, new ArrayList<ObjectId>());   // this is an invalid ballot!

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

    String fromUserId = userRepo.findByEmail(TestFixtures.USER1_EMAIL).getId();

    String url = "/users/"+fromUserId+"/delegations";
    //Implementation note: I tried doing this via the auto generated /liquido/v2/delegations endpoint.  But it only support POST a new item. I need an upsert operation here.

    //I am deliberately not using DelegationModel here. This is the JSON as a client would send it.
    JSONObject newDelegationJSON = new JSONObject()
        .put("fromUser", this.users.get(0).getId())
        .put("toProxy",  this.users.get(1).getId())
        .put("area",     this.areas.get(3).getId());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(newDelegationJSON.toString(), headers);

    client.put(url, entity);    //TODO: this is stupid that PUT does not return anything. Maybe I have to switch to     URI upsertedDelegationURI = client.postForLocation(url, entity);

    /*
    URI upsertedDelegationURI = client.postForLocation(url, entity);
    assertNotNull(upsertedDelegationURI);
    */

    log.trace("TEST SUCCESS: saved delegation to proxy successfully");
  }

  @Test
  public void testPostDuplicateArea() {
    log.trace("TEST postDuplicateArea");
    AreaModel existingArea = areaRepo.findByTitle(TestFixtures.AREA1_TITLE);

    String url = "/liquido/v2/areas";
    JSONObject duplicateAreaJson = new JSONObject()
      .put("title", existingArea.getTitle())
      .put("description", "duplicate Area from test");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> entity = new HttpEntity<String>(duplicateAreaJson.toString(), headers);

    ResponseEntity<String> responseEntity = client.postForEntity(url, entity, String.class);  // This will return HTTP status 409(Conflict) becasue of the duplicate composite key.

    log.trace("responseEntity:\n" + responseEntity);
    assertEquals(responseEntity.getStatusCode(), HttpStatus.CONFLICT);  // status == 409
    log.trace("TEST postDuplicateArea SUCCESS: Did receive expected HttpStatus=409 (Conflict)");
  }
}
