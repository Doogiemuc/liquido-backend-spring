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

@Slf4j
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
    @Autowired
    private ResourceServerTokenServices tokenServices;

    @Value("${security.jwt.resource-ids}")
    private String resourceIds;

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources.resourceId(resourceIds).tokenServices(tokenServices);
    }

		@Override
    public void configure(HttpSecurity http) throws Exception {
      http
				//.requestMatchers()
				//.and()
					.authorizeRequests().anyRequest().authenticated()
						//.antMatchers("/oauth/token").permitAll()
						//.antMatchers("/actuator/**", "/api-docs/**").permitAll()
						//.antMatchers("/liquido/v2/**" ).authenticated()
				//.and()		// needed for H2 DB concole to work
				//	.antMatcher("/h2-console/**").headers().frameOptions().disable()
				;
    }
}
