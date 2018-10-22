package org.doogie.liquido.test.testUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Map;

/** log requests that the REST client sends during testing */
@Slf4j
public class LogClientRequestInterceptor implements ClientHttpRequestInterceptor {
  @Override
  public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
    log.trace("=TEST=> "+httpRequest.getMethod()+ " " + httpRequest.getURI());
    Map<String, String> headerMap = httpRequest.getHeaders().toSingleValueMap();
    for (String key : headerMap.keySet()) {
      log.trace("  "+key+": "+headerMap.get(key));
    }
    ClientHttpResponse response = clientHttpRequestExecution.execute(httpRequest, bytes);
		log.trace("<=TEST= "+httpRequest.getMethod()+ " " + httpRequest.getURI()+" "+response.getStatusText()+"("+response.getStatusCode()+")");
		return response;
  }
}
