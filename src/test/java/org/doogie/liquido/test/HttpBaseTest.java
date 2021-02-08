package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.jwt.JwtTokenUtils;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.test.testUtils.JwtAuthInterceptor;
import org.doogie.liquido.test.testUtils.LogClientRequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import javax.annotation.PostConstruct;

@Slf4j
public class HttpBaseTest extends BaseTest {
	/** path prefix for REST API from application.properties */
	@Value(value = "${spring.data.rest.base-path}")
	public String basePath;

	/** (random) port of local backend under test that spring creates */
	@LocalServerPort
	int localServerPort;

	/** full uri: https://localhost:{port}/{basePath}/ */
	public String rootUri;

	/** This provider can create Json Web Tokens (JWT) */
	@Autowired
	JwtTokenUtils jwtTokenUtils;

	/** HTTP interceptor that adds JWT to request header */
	JwtAuthInterceptor jwtAuthInterceptor = new JwtAuthInterceptor();

	/**
	 * Authenticated HTTP REST client
	 * You MUST call this.loginUserJWT(email)
	 */
	public RestTemplate client;

	/** Anonymous, not authenticated HTTP REST client */
	public RestTemplate anonymousClient;

	/**
	 * Configure a HTTP REST client:
	 *  - log the client requests with {@link LogClientRequestInterceptor}
	 *  - support authentication via JWT
	 *  - has a rootUri
	 */
	@PostConstruct
	public void initHttpClients() {
		this.rootUri = "http://localhost:" + localServerPort + basePath;
		log.trace("====== configuring RestTemplate HTTP client for " + rootUri);

		// Bugfix: Need to manually configure Encoding of URL query parameters
		// https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/web.html#web-uri-encoding
		DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
		uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);


		this.client = new RestTemplateBuilder()
			.additionalInterceptors(new LogClientRequestInterceptor())
			.additionalInterceptors(this.jwtAuthInterceptor)
			//.uriTemplateHandler(uriBuilderFactory)
			.rootUri(rootUri)
			.build();

		this.anonymousClient = new RestTemplateBuilder()
			.additionalInterceptors(new LogClientRequestInterceptor())
			.uriTemplateHandler(uriBuilderFactory)
			.rootUri(rootUri)
			.build();
	}

	public void loginUserJWT(String userEmail) {
		UserModel user = userRepo.findByEmail(userEmail).orElseThrow(() -> new RuntimeException("Cannot loginUserJWT. Cannot find user with email="+userEmail));
		this.loginUserJWT(user.getId(), null);
	}

	/**
	 * little helper to quickly login a specific user
	 */
	public void loginUserJWT(Long userId, Long teamId) {
		// Here we see that advantage of a completely stateless server. We simply generate a JWT and that's it. No login state is stored on the server.
		String jwt = jwtTokenUtils.generateToken(userId, teamId);
		jwtAuthInterceptor.setJwtToken(jwt);
	}
}
