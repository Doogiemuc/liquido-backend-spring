package org.doogie.liquido.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.security.LiquidoUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.token.*;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import java.util.Arrays;


/**
 * In Oauth the AuthorisationServer is responsible for issuing access tokens.
 * We use JWT (JSON Web tokens) as token format.
 *
 * This code is adapted from https://github.com/nydiarra/springboot-jwt
 */
@Slf4j
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

	@Value("${security.jwt.client-id}")
	private String clientId;

	@Value("${security.jwt.client-secret}")
	private String clientSecret;

	@Value("${security.jwt.grant-type}")
	private String grantType;

	@Value("${security.jwt.scope-read}")
	private String scopeRead;

	@Value("${security.jwt.scope-write}")
	private String scopeWrite = "write";

	@Value("${security.jwt.resource-ids}")
	private String resourceIds;

	@Autowired
	private TokenStore tokenStore;

	@Autowired
	private JwtAccessTokenConverter jwtAccessTokenConverter;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	LiquidoUserDetailsService liquidoUserDetailsService;

	@Value("${spring.data.rest.base-path}")   // value from application.properties file
	String basePath;


	//FIXME: Do I need to do this?  https://stackoverflow.com/questions/26636465/oauth2-bad-credentials-spring-boot

	//TODO: !!! Login, Logout, Refersh, Forgot Password, EMail token  https://github.com/isopropylcyanide/Jwt-Spring-Security-JPA

	@Override
	public void configure(ClientDetailsServiceConfigurer configurer) throws Exception {
		log.trace("OAuth2 AuthorizationServer.configure(http)");
		configurer
		    .inMemory()
		    .withClient(clientId)
				.secret(passwordEncoder.encode(clientSecret))
		    .authorizedGrantTypes(grantType)
		    .scopes(scopeRead, scopeWrite)
		    .resourceIds(resourceIds);
	}

	/**
	 * Configure sprint-security-oauth endpoints
	 * Here we map the Oauth urls to our API basePath
	 * @param endpoints
	 * @throws Exception
	 */
	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
		TokenEnhancerChain enhancerChain = new TokenEnhancerChain();
		enhancerChain.setTokenEnhancers(Arrays.asList(jwtAccessTokenConverter));

		//BUGFIX: Need set our LiquidoUserDetailsService so that the JWT token is not just converted to the user's email as String but instead to a full blown LiquidoAuthUser with a liquido UserModel inside that was looked up via userDetailsService from the DB.
		// This one took me a while to figure out :-)  JwtAccessTokenConverter -> (Default)AccessTokenConverter -> (Default)UserAuthenticationConverter -> (Liquido)UserDetailsService
		DefaultUserAuthenticationConverter userAuthenticationConverter = new DefaultUserAuthenticationConverter();
		userAuthenticationConverter.setUserDetailsService(liquidoUserDetailsService);

		((DefaultAccessTokenConverter) jwtAccessTokenConverter.getAccessTokenConverter()).setUserTokenConverter(userAuthenticationConverter);

		//endpoints.getFrameworkEndpointHandlerMapping().getPath("/oauth/token");

		endpoints
				//.prefix("oauthPrefix")   // <== NO :-)  https://github.com/spring-projects/spring-security-oauth/issues/214
				.pathMapping("/oauth/token", basePath+"/oauth/token")
				.pathMapping("/oauth/authorize", basePath+"/oauth/authorize")
				.pathMapping("/oauth/check_token", basePath+"/oauth/check_token")
				.pathMapping("/oauth/confirm_access", basePath+"/oauth/confirm_access")
				.pathMapping("/oauth/error", basePath+"/oauth/error")
				.pathMapping("/oauth/token_key", basePath+"/oauth/token_key")
				.tokenStore(tokenStore)
		    .accessTokenConverter(jwtAccessTokenConverter)
		    .tokenEnhancer(enhancerChain)
		    .authenticationManager(authenticationManager);
	}

}
