package org.doogie.liquido.test.testUtils;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Simple HTTP request interceptor that can add a bearer token to the request's header for authorization:
 *
 * <pre>Authorization: Bearer 23gg34terg345</pre>
 */
public class JwtAuthInterceptor implements ClientHttpRequestInterceptor {
	private String jwtToken = null;

	public JwtAuthInterceptor() {}

	public JwtAuthInterceptor(String token) {
		this.jwtToken = token;
	}

	public void setJwtToken(String jwtToken) {
		this.jwtToken = jwtToken;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		if (this.jwtToken != null) {
			request.getHeaders().setBearerAuth(this.jwtToken);
		}
		return execution.execute(request, body);
	}
}
