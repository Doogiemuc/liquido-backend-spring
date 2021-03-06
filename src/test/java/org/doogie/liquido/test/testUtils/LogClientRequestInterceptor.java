package org.doogie.liquido.test.testUtils;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.util.DoogiesUtil;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Map;

/** log requests that the REST client <b>sends</b> during testing */
@Slf4j
public class LogClientRequestInterceptor implements ClientHttpRequestInterceptor {

	private static final String CLIENT = "C";

  @Override
  public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
    log.debug("=" + CLIENT + "=> "+httpRequest.getMethod()+ " " + httpRequest.getURI());
    /* could print headers. But server does this already. Keep in mind, that other interceptors might still add more headers after us. */
    Map<String, String> headerMap = httpRequest.getHeaders().toSingleValueMap();
    for (String key : headerMap.keySet()) {
      log.trace("  "+key+": "+headerMap.get(key));
    }

    ClientHttpResponse response = clientHttpRequestExecution.execute(httpRequest, bytes);
		log.debug("<=" + CLIENT + "= " + httpRequest.getMethod()+ " " + httpRequest.getURI()+" returned "+response.getStatusText()+"("+response.getStatusCode()+")");
		//Keep in mind: CANNOT simply log response body, because this slurps up the input buffer and client cannot read it anymore.  There are ways around this but this is complicated.   See DoogiesRequestLogger

		return response;
  }
}
