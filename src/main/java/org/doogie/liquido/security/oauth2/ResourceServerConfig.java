package org.doogie.liquido.security.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

/**
 * Spring-security-oauth Resource Server
 * This class configures the http security for our Oauth endpoints.
 *
 * org.springframework.core.annotation.Order: "Lower values have higher priority"
 * ResourceServerConfiguration has order = 3 by default. So it will be checked before LiquidoSecurityConfiguration which has @Order(100)
 */
@Slf4j
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
    @Autowired
    private ResourceServerTokenServices tokenServices;

    @Value("${security.jwt.resource-ids}")
    private String resourceIds;

		@Value("${spring.data.rest.base-path}")   // value from application.properties file
		String basePath;

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources.resourceId(resourceIds).tokenServices(tokenServices);
    }

	/**
	 * Configure access to several endpoints. This configruation has order = 3 in the filter chain. So it comes first!
	 * The rest of our API is configured in {@link org.doogie.liquido.security.LiquidoSecurityConfiguration}
	 *
	 * @param http spring HttpSecurity
	 * @throws Exception
	 */
	@Override
    public void configure(HttpSecurity http) throws Exception {
      http
				.authorizeRequests()
					.antMatchers(basePath +"/_ping").permitAll()       	// is alive  public endpoints must be configured here
					.antMatchers(basePath +"/globalProperties").permitAll() // client needs to load globalProperties, before a user is logged in  => basicAuth?
					.antMatchers(basePath +"/castVote").permitAll()    	// allow anonymous voting
					//Remark: the /oauth/token endpoint is already secured with basic auth by default
					//TODO: .antMatchers("/error").permitAll()
					//.antMatchers("/actuator/**", "/api-docs/**").permitAll()			// why would I need that?????
					.antMatchers(basePath+"/**" ).authenticated()				// our liquido API
					.anyRequest().denyAll();																				// prevent any leaking URL
    }
}
