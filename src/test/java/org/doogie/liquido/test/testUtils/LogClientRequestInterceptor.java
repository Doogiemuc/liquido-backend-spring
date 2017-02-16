package org.doogie.liquido.test.testUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/** log requests that this REST client sends */
@Slf4j
public class LogClientRequestInterceptor implements ClientHttpRequestInterceptor {
  @Override
  public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
    log.trace("Client Request: "+httpRequest.getMethod()+ " " + httpRequest.getURI());
    return clientHttpRequestExecution.execute(httpRequest, bytes);
  }
}
