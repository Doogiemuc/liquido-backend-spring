package org.doogie.liquido.test.testUtils;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Just the same as spring's BasicAuthorizationInterceptor. But mine can change teh user :-)
 */
@Deprecated  // not used anymore
public class LiquidoBasicAuthInterceptor implements ClientHttpRequestInterceptor {
	private String username;
	private String password;

	public LiquidoBasicAuthInterceptor(@Nullable String username, @Nullable String password) {
		Assert.doesNotContain(username, ":", "Username must not contain a colon");
		this.username = username != null ? username : "";
		this.password = password != null ? password : "";
	}

	public void login(String username, String password) {
		this.username = username != null ? username : "";
		this.password = password != null ? password : "";
	}

	public void logout() {
		this.username = null;
		this.password = null;
	}

	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		String token = Base64Utils.encodeToString((this.username + ":" + this.password).getBytes(StandardCharsets.UTF_8));
		request.getHeaders().add("Authorization", "Basic " + token);
		return execution.execute(request, body);
	}
}
