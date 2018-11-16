package org.doogie.liquido.config;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.jwt.JwtAuthenticationFilter;
import org.doogie.liquido.security.LiquidoUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;


/**
 * Configure spring Security.
 *
 * Here we configure authentication for our api endpoints.A
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
//@Order(...)  // WebSecurityConfigurerAdapter has @Order(100) by default
public class LiquidoWebSecurityConfiguration extends WebSecurityConfigurerAdapter {

	// REST base path from application.properties
  @Value("${spring.data.rest.base-path}")
  String basePath;

  @Autowired
	JwtAuthenticationFilter jwtAuthenticationFilter;

  /*
  /*  FOR TESTING: Simplest possible in memory authentication with static default users.
  @Autowired
  public void configureAuth(AuthenticationManagerBuilder auth) throws Exception {
    log.debug("Adding users to inMemoryAuthentication");
    auth.inMemoryAuthentication()
      .withUser("user").password("user").roles("USER").and()
      .withUser("admin").password("admin").roles("USER", "ADMIN");
  }
  */

  /**
   * Configure HttpSecurity. This config has the highest priority and comes first.
   * @param http spring http security
   * @throws Exception when request is unauthorized
   */
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    log.trace("Configuring HttpSecurity for "+ basePath);
	  http
			//.antMatcher(basePath).authenticationProvider(new LiquidoTokenAuthProvider()) // can I add my token auth that way?
			.authorizeRequests()
			  .antMatchers(basePath+"/_ping").permitAll()        // allow is alive
			  .antMatchers(basePath+"/auth/**").permitAll()      // allow login via one time token
			  .anyRequest().authenticated()
			.and()
				.httpBasic()
			.and()
			  .csrf().disable();   //TODO: reenable CSRF

		log.trace("Adding JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter");
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
  }

	//see http://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#jc-authentication-userdetailsservice
	@Bean
	public LiquidoUserDetailsService liquidoUserDetailsService() {
		log.debug("creating LiquidoUserDetailsService");
		return new LiquidoUserDetailsService();
	}


	/**
	 * Bean for default Password encoder. Needed since
	 * <a href="https://spring.io/blog/2017/11/01/spring-security-5-0-0-rc1-released#password-encoding">Sprint-securty 5.0</a>
	 * @return a default delegatingPasswordEncoder
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

  /**
   * Configure CORS so that access from localhost:3001 is allowed.
   * http://stackoverflow.com/a/31748398/122441 until https://jira.spring.io/browse/DATAREST-573
   * https://spring.io/blog/2015/06/08/cors-support-in-spring-framework#filter-based-cors-support
   * https://spring.io/guides/gs/rest-service-cors/
   * @return the FilterRegistrationBean
   */
  @Bean
  public FilterRegistrationBean corsFilter() {
    log.trace("Configuring CORS from spring blog");
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    config.addAllowedOrigin("*");
    //config.addAllowedOrigin("http://localhost:8080");
    //config.addAllowedOrigin("http://localhost:3001");
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");
    source.registerCorsConfiguration("/**", config);
    FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return bean;
  }



}
