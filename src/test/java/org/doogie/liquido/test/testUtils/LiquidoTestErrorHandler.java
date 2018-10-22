package org.doogie.liquido.test.testUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

/** Custom ErrorResponseHandler to catch (and store) the response body in case of an HTTP error. */
@Slf4j
public class LiquidoTestErrorHandler implements ResponseErrorHandler {
  @Override
  public boolean hasError(ClientHttpResponse clientHttpResponse) throws IOException {
    return clientHttpResponse.getStatusCode().is4xxClientError() ||
           clientHttpResponse.getStatusCode().is5xxServerError();
  }
  @Override
  public void handleError(ClientHttpResponse clientHttpResponse) throws IOException {
    //Cannot read the body here, cause InputStream can only be read once lastErrorResponseBody = DoogiesUtil._stream2String(clientHttpResponse.getBody());
    log.error("HTTP error: " + clientHttpResponse.getStatusCode().getReasonPhrase() + "(" + clientHttpResponse.getRawStatusCode() + ")");
  }
}
