package org.doogie.liquido;

import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.util.DoogiesUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.ResponseErrorHandler;
import sun.nio.ch.IOUtil;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for Liquiodo REST endpoint
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)  // This is so cool. This automatically sets up everything and starts the server. *like*
public class RestEndpointTests {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired
  TestRestTemplate template;    // REST client that is automatically configured with port of running test server.

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
    log.info("Test server started on port="+port);
    template.getRestTemplate().setErrorHandler(new LiquidoTestErrorHandler());
  }

  @Test
  public void testPostBallot() {
    log.trace("TEST postBallot");
    BallotModel newBallot = new BallotModel("dummyInitialLawID", Arrays.asList("aaa", "bbb"));
    ResponseEntity<BallotModel> createdBallot = template.postForEntity("/ballot", newBallot, BallotModel.class);
    assertNotNull("Newly posted Ballot must have an ID", createdBallot.getBody().getId());
    assertEquals(200, createdBallot.getStatusCodeValue());
    log.trace("TEST postBallot successful: newly created ballot has ID="+createdBallot.getBody());
  }

  @Test
  public void testPostInvalidBallot() {
    log.trace("TEST postInvalidBallot");
    BallotModel invalidBallot = new BallotModel(null, null);
    ResponseEntity<BallotModel> response = template.postForEntity("/ballot", invalidBallot, BallotModel.class);
    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCodeValue());  // 400
    assertTrue(lastErrorResponseBody.contains("NotNull.initialLawId"));
    log.trace("TEST postInvalidBallot successful: received correct status and error message in response.");
  }


}
