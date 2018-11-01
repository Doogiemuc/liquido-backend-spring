package org.doogie.liquido.security;

import lombok.extern.slf4j.Slf4j;
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
//@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)  // WebSecurityConfigurerAdapter has @Order(100) by default
public class LiquidoSecurityConfiguration extends WebSecurityConfigurerAdapter {

	// REST base path from application.properties
  @Value("${spring.data.rest.base-path}")
  String basePath;

  /*  DEPRECATED old Oauth stuff
	@Value("${security.signing-key}")
	private String signingKey;

	@Value("${security.encoding-strength}")
	private Integer encodingStrength;

	@Value("${security.security-realm}")
	private String securityRealm;
  */

  //see http://docs.spring.io/spring-security/site/docs/4.2.1.RELEASE/reference/htmlsingle/#jc-authentication-userdetailsservice
  @Bean
  public LiquidoUserDetailsService liquidoUserDetailsService() {
     log.debug("creating LiquidoUserDetailsService");
    return new LiquidoUserDetailsService();
  }

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
			 //TODO: .antMatcher(basePath).authenticationProvider(new LiquidoTokenAuthProvider())  can I add my token auth that way?
			.authorizeRequests()
			  .antMatchers(basePath+"/_ping").permitAll()        // is alive
			  .antMatchers(basePath+"/login/**").permitAll()        // login via one time token
			  .anyRequest().authenticated()
			.and()
			  .httpBasic();
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

  /*  ******************************* DEPRECATED stuff for  OAUTH 2.0 ******************

	@Bean
	@Override
	protected AuthenticationManager authenticationManager() throws Exception {
		return super.authenticationManager();
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public JwtAccessTokenConverter accessTokenConverter() {
		JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
		converter.setSigningKey(signingKey);
		return converter;
	}

	@Bean
	public TokenStore tokenStore() {
		return new JwtTokenStore(accessTokenConverter());
	}

	@Bean
	@Primary
	//Making this primary to avoid any accidental duplication with another token service instance of the same name
	public DefaultTokenServices tokenServices() {
		DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
		defaultTokenServices.setTokenStore(tokenStore());
		defaultTokenServices.setSupportRefreshToken(true);
		return defaultTokenServices;
	}

	*/

  /*
  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurerAdapter() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        log.debug("adding CORS mapping for path="+basePath);
        registry.addMapping(basePath).allowedOrigins("*").allowedMethods("*");
      }
    };
  }
  */


}
