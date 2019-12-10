package org.doogie.liquido.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Spring security configuration to allow access to the H2 database web console
 * The H2 console also needs to be enabled in application.properties:
 * <pre>spring.h2.console.enabled=true</pre>
 */
@Slf4j
@Configuration
@Order(1)  // low value => very high priority!
public class LiquidoH2ConsoleSecurityConf extends WebSecurityConfigurerAdapter {
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		log.debug("Configuring security for H2 DB console");
		http
			.antMatcher("/h2-console/**")   // everything below this path:
				.authorizeRequests().anyRequest().permitAll()
				.and()
				.headers().frameOptions().disable()  // needed for H2 DB console
				.and()
				.csrf().ignoringAntMatchers("/h2-console/**")   // H2 console does not support CSRF protection
		;
	}
}
