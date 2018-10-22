package org.doogie.liquido.test.testUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

/**
 * Http request interceptor that automatically fetches an Oauth token
 * if the request does not contain one yet.
 */
@Slf4j
@Data
public class OauthInterceptor implements ClientHttpRequestInterceptor {

	private String grandType = "password";
	private String clientId;
	private String clientSecret;
	private String username;
	private String password;
	private String rootUri;

	//TODO: private String cachedToken   reset token when user changes!

	public OauthInterceptor(String rootUri, String clientId, String clientSecret, String username, String password) {
		this.rootUri = rootUri;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.username = username;
		this.password = password;
	}

	public String getOauthAccessToken() {
		log.debug("Intercepting unauthorized request. getOauthAccesToken for "+username);

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

		ResponseEntity<String> tokenRes = basicAuthClient.exchange("/oauth/token", HttpMethod.POST, entity, String.class);

		assertEquals(HttpStatus.OK, tokenRes.getStatusCode());

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
			response.getHeaders().add("Foo", "bar");
			return response;
		}


}
