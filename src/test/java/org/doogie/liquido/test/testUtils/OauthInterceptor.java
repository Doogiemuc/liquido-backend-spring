package org.doogie.liquido.test.testUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

/**
 * Http request interceptor that automatically fetches an Oauth token
 * if the request does not contain one yet.
 * (Singleton)
 */
@Slf4j
@Data
public class OauthInterceptor implements ClientHttpRequestInterceptor {

	// if you would want to inject things, then this class must be a spring bean (@Component etc.)
	//@Value(value = "${spring.data.rest.base-path}")
	//String basePath;

	private final String OAUTH_TOKEN_PATH ="/oauth/token";

	private String grandType = "password";
	private String clientId;
	private String clientSecret;
	private String username;
	private String password;
	private String rootUri;

	private String cachedToken;

	public OauthInterceptor(String rootUri, String clientId, String clientSecret, String username, String password) {
		this.rootUri = rootUri;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.username = username;
		this.password = password;
	}

	public String getNewOauthToken(String username, String password) {
		this.cachedToken = null;
		this.username = username;
		this.password = password;
		return getOauthAccessToken();
	}

	public String getOauthAccessToken() {
		log.debug("Intercepting unauthorized request. getOauthAccesToken for "+username+" from "+rootUri+OAUTH_TOKEN_PATH);

		if (cachedToken != null) return cachedToken;

		RestTemplate basicAuthClient = new RestTemplateBuilder()
				.basicAuthorization(clientId, clientSecret)
				.additionalInterceptors(new LogClientRequestInterceptor())
				.rootUri(rootUri)
				.build();
		// POST request with url encoded form parameters in the body
		String formParams = null;
		try {
			formParams = EntityUtils.toString(new UrlEncodedFormEntity(Arrays.asList(
					new BasicNameValuePair("grant_type", grandType),
					new BasicNameValuePair("username", username),
					new BasicNameValuePair("password", password)
			)));
		} catch (IOException e) {
			String msg = "Cannot get Oauth token: Error while encoding form parameters: "+e.getMessage();
			log.error(msg);
			throw new RuntimeException(msg, e);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_FORM_URLENCODED);
		HttpEntity entity = new HttpEntity(formParams, headers);

		ResponseEntity<String> tokenRes = basicAuthClient.exchange(OAUTH_TOKEN_PATH, HttpMethod.POST, entity, String.class);

		if (!tokenRes.getStatusCode().equals(HttpStatus.OK)) throw new RuntimeException("Cannot get Oauth token.");

		JacksonJsonParser jsonParser = new JacksonJsonParser();
		String accessToken = jsonParser.parseMap(tokenRes.getBody()).get("access_token").toString();
		log.trace("received Oauth access_token: "+accessToken);
		return accessToken;
	}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
			if (!request.getHeaders().containsKey("Authorization") && !request.getHeaders().containsKey("authorization")) {
				String access_token = getOauthAccessToken();
				request.getHeaders().set("Authorization", "Bearer "+access_token);
			}

			ClientHttpResponse response = execution.execute(request, body);
			//response.getHeaders().add("Foo", "bar");
			return response;
		}


}
