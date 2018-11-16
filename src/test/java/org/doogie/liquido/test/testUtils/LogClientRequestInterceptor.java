package org.doogie.liquido.test.testUtils;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.util.DoogiesUtil;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Map;

/** log requests that the REST client sends during testing */
@Slf4j
public class LogClientRequestInterceptor implements ClientHttpRequestInterceptor {
  private static final String CLIENT = "=CLIENT=";
  @Override
  public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
    log.trace(CLIENT + "=> "+httpRequest.getMethod()+ " " + httpRequest.getURI());
    Map<String, String> headerMap = httpRequest.getHeaders().toSingleValueMap();
    for (String key : headerMap.keySet()) {
      log.trace("  "+key+": "+headerMap.get(key));
    }
    ClientHttpResponse response = clientHttpRequestExecution.execute(httpRequest, bytes);
		log.trace("<=" + CLIENT + " " + httpRequest.getMethod()+ " " + httpRequest.getURI()+" returned "+response.getStatusText()+"("+response.getStatusCode()+")");
		// TODO: log response body, especially in case of errors.
		if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
			String responseBody = DoogiesUtil._stream2String(response.getBody());
			log.warn("   "+responseBody);
		}

		return response;
  }
}
