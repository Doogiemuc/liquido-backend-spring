package org.doogie.liquido.security;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Client for the twilio.com REST API. Liquido uses Authy's TOTP to authenticate users.
 *
 * This class uses configuration parameters from <pre>application.properties</pre>
 * We have proxy support.
 *
 * https://www.twilio.com/docs/authy/api
 */
@Slf4j
@Service
public class TwilioAuthyClient {

	@Autowired
	LiquidoProperties prop;

	/** Do we need to connect through a proxy. You can configure this in your local application-dev.yml. (Default is "false") */
	@Value("${PROXY_ACTIVE:false}")
	private boolean proxyActive;

	@Value("${PROXY_HOST:''}")
	private String PROXY_HOST;
	@Value("${PROXY_PORT:8080}")
	private int    PROXY_PORT;
	@Value("${PROXY_USER:''}")
	private String PROXY_USER;
	@Value("${PROXY_PASS:''}")
	private String PROXY_PASS;

	RestTemplate restClient = null;

	/**
	 * Lazily create and return a rest client that always sends X-Authy-API-Key in header
	 * @return
	 */
	RestTemplate getRestClient() {
		if (this.restClient != null) return this.restClient;

		log.debug("Creating new RestClient for " + prop.authy.apiUrl + (proxyActive ? " with proxy "+PROXY_USER + "@" + PROXY_HOST+":"+PROXY_PORT : ""));

		if (prop.authy.apiKey == null || prop.authy.apiKey.length() < 3)
			throw new RuntimeException("Need prop.authy.apiKey in application-dev.yml !");

		List<Header> headers = new ArrayList<>();
		headers.add(new BasicHeader("X-Authy-API-Key", prop.authy.apiKey));
		//headers.add(new BasicHeader("Content-Type", "application/json");
		//headers.add(new BasicHeader("Accept", "*/*");

		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
 		httpClientBuilder
				.setDefaultHeaders(headers)
				//MAYBE: .addInterceptorLast()    could also use a request interceptor for the default header and base URL
				.disableCookieManagement();

		if (this.proxyActive) {
			HttpHost myProxy = new HttpHost(PROXY_HOST, PROXY_PORT);
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(
					new AuthScope(PROXY_HOST, PROXY_PORT),
					new UsernamePasswordCredentials(PROXY_USER, PROXY_PASS));
			httpClientBuilder
				.setProxy(myProxy)
				.setDefaultCredentialsProvider(credsProvider);
		}

		HttpClient httpClient = httpClientBuilder.build();

		/*  MAYBE we could do this without springs RestTemplate and instead just use apaches HttpClient ...
		// https://mkyong.com/java/apache-httpclient-examples/
		HttpGet httpGet = new HttpGet("http://www.google.de");
		httpGet.addHeader("name", "value");
		try {
			HttpResponse res = httpClient.execute(httpGet);
			res.getStatusLine().getStatusCode()
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/


		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
		factory.setHttpClient(httpClient);

		this.restClient = new RestTemplate(factory);
		return this.restClient;
	}

	/**
	 * Fetch general Authy application details
	 * @return JSON
	 */
	public String getAppDetails() {
		String url = prop.authy.apiUrl + "/protected/json/app/details";
		ResponseEntity<String> responseEntity = this.getRestClient().getForEntity(url, String.class);
		return responseEntity.getBody();
	}


	/**
	 * Register a new user at twilio
	 * @param email user's email adress
	 * @param mobilephone user's mobile phone number (that SMS will be sent to)
	 * @param countryCode two digit mobile country code. (No leading plus sign!)
	 * @return authy's user id
	 */
	public long createTwilioUser(@NotNull String email, @NotNull String mobilephone, String countryCode) {
		String url = prop.authy.apiUrl + "/protected/json/users/new";

		try {
			HttpEntity<String> requestEntity = Lson.builder()
				.put("user[email]", email)
				.put("user[cellphone]", mobilephone)
				.put("user[country_code]", countryCode)  		// mobile phone prefix without leading plus, e.g. 49
				.toUrlEncodedFormHttpEntity();

			log.debug("=> Register new Twilio User: POST "+requestEntity);
			ResponseEntity<String> responseEntity = this.getRestClient().postForEntity(url, requestEntity, String.class);
			log.debug("<= "+responseEntity);
			assert HttpStatus.OK.equals(responseEntity.getStatusCode());
			//Boolean success = JsonPath.read(responseEntity.getBody(), "$.success");
			int userId = JsonPath.read(responseEntity.getBody(), "$.user.id");
			return (long) userId;
		} catch (RestClientResponseException e) {
			log.error("Exception: "+ e.toString());
			log.error("Error Response: "+ e.getResponseBodyAsString());
			throw e;
		}
	}

	/**
	 * If the user has the authy app intalled, then this will send an push notifications to his mobile phone.
	 * Then the user can open the authy app and enter a timed one time password (TOTP).
	 * Otherwise an SMS with a OTP will be sent to the user as fallback.
	 * @param userAuthyId authy's user id   (mobile phone number is already stored with that user at authy)
	 * @return JSON response from authy
	 */
	public String sendSmsOrPushNotification(long userAuthyId) throws LiquidoException {
		try {
			String url = prop.authy.apiUrl + "/protected/json/sms/"+userAuthyId;
			ResponseEntity<String> response = this.getRestClient().getForEntity(url, String.class);
			log.debug("Sent authentication request to userAuthyId="+userAuthyId, response);
			return response.getBody();
		} catch (RestClientResponseException e) {
			// Spring RestTemplate throws RuntimeExceptions when request fails!   (see HttpClientErrorException)
			if (e.getRawStatusCode() == 404) {
				throw new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "No Authy user with id="+userAuthyId, e);
			} else {
				String msg = "Cannot request token for userAuthyId="+userAuthyId;
				log.warn(msg);
				throw new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, msg, e);
			}
		}
	}

	/**
	 * Verify a TOTP that a user has entered.
	 * https://www.twilio.com/docs/authy/api/one-time-passwords#verify-a-one-time-password
	 *
	 * @param userAuthyId authy user
	 * @param otp the one time password entered by the user from the authy app
	 * @return JSON response if authentication is successful with device information.
	 * @throws LiquidoException if authentication was denied
	 */
	public String verifyOneTimePassword(long userAuthyId, String otp) throws LiquidoException {
		log.debug("OTP authentication request for userAuthyId="+userAuthyId);
		if (userAuthyId <= 0)
			throw new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Cannot verifyOneTimePassword. Invalid userAuthyId="+userAuthyId);
		if (otp == null || otp.length() < 3)
			throw new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Cannot verifyOneTimePassword. Invalid oneTimePassword");
		try {
			String url = prop.authy.apiUrl + "/protected/json/verify/"+otp+"/"+userAuthyId;
			ResponseEntity<String> response = this.getRestClient().getForEntity(url, String.class);
			if (!HttpStatus.OK.equals(response.getStatusCode()))
				throw new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Authy Token is invalid (userAuthyId="+userAuthyId+")");
			log.debug("AUTHY: userAuthyId="+userAuthyId+" authenticated successfully with valid OTP.");
			return response.getBody();
		} catch (RestClientResponseException err) {		// Authy returns HTTP 401 when OTP is invalid => Spring RestTemplate then throws a RuntimeExceptions!
			if (err.getRawStatusCode() == 401) {
				log.debug("Invalid Authy OTP provided. verifyOneTimePassword(userAuthIy=" + userAuthyId+")");
				throw new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Invalid Authy OTP provided. (userAuthyId=" + userAuthyId + ")", err);
			} else {
				log.warn("ERROR verifyOneTimePassword userAuthIy=" + userAuthyId, err);
				throw new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "ERROR: Cannot verifyOneTimePassword from Authy (userAuthyId=" + userAuthyId + ")", err);
			}
		}
	}

	/**
	 * Remove an authy user at twilio
	 * @param userAuthyId authy's user ID
	 * @return JSON response when user was successfully removed
	 */
	public String removeUser(long userAuthyId) {
		String url = prop.authy.apiUrl + "/protected/json/users/"+userAuthyId+"/remove";
		ResponseEntity<String> response = this.getRestClient().postForEntity(url, null, String.class);
		return response.getBody();
	}
}
