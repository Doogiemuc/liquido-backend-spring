package org.doogie.liquido.test;

import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.util.DoogiesUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.ResponseErrorHandler;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for Liquiodo REST endpoint and mongoDB.
 *
 * These test cases test the Liquido Java backend together with the mongo database behind it.
 * It would be quite costly to mock the complete DB. So we test both layers here.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)  // This is so cool. This automatically sets up everything and starts the server. *like*
public class RestEndpointTests {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired
  TestRestTemplate client;    // REST client that is automatically configured with port of running test server.

  @Autowired
  UserRepo userRepo;

  @Autowired
  AreaRepo areaRepo;

  @Autowired
  DelegationRepo delegationRepo;

  @LocalServerPort
  int port;

  /* HTTP body of last error response */
  String lastErrorResponseBody = "";

  /** Custom ErrorResponseHandler to catch (and store) the response body in case of an HTTP error. */
  class LiquidoTestErrorHandler implements ResponseErrorHandler {
    @Override
    public boolean hasError(ClientHttpResponse clientHttpResponse) throws IOException {
      return clientHttpResponse.getStatusCode() != HttpStatus.OK;
    }
    @Override
    public void handleError(ClientHttpResponse clientHttpResponse) throws IOException {
      lastErrorResponseBody = DoogiesUtil._stream2String(clientHttpResponse.getBody());
      log.error("HTTP error: "+lastErrorResponseBody);
    }
  }

  @PostConstruct
  public void doLogging() {
    log.info("RestEndpointTests server started on port="+port);
    client.getRestTemplate().setErrorHandler(new LiquidoTestErrorHandler());
  }

  @Test
  public void testPostBallot() {
    log.trace("TEST postBallot");
    BallotModel newBallot = new BallotModel("dummyInitialLawID", Arrays.asList("aaa", "bbb"));
    ResponseEntity<BallotModel> createdBallot = client.postForEntity("/ballot", newBallot, BallotModel.class);
    assertNotNull("Newly posted Ballot must have an ID", createdBallot.getBody().getId());
    assertEquals(200, createdBallot.getStatusCodeValue());
    log.trace("TEST postBallot successful: newly created ballot has ID="+createdBallot.getBody());
  }

  @Test
  public void testPostInvalidBallot() {
    log.trace("TEST postInvalidBallot");
    BallotModel invalidBallot = new BallotModel(null, null);
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
    UserModel fromUser = userRepo.findByEmail(TestFixtures.USER1_EMAIL);
    UserModel toProxy  = userRepo.findByEmail(TestFixtures.USER4_EMAIL);
    AreaModel area     = areaRepo.findByTitle(TestFixtures.AREA1_TITLE);
    DelegationModel newDelegation = new DelegationModel(fromUser.getId(), toProxy.getId(), area.getId());

    String uri = "/users/"+fromUser.getId()+"/delegations";
    client.put(uri, newDelegation);

    List<DelegationModel> delegations = delegationRepo.findAll(Example.of(newDelegation));

    assertEquals("Expected to find exactly one new delegation.", 1, delegations.size());
    assertEquals(fromUser, delegations.get(0).getFromUser());
    log.trace("TEST SUCCESS: saved delegation to proxy successfully");
  }
}
