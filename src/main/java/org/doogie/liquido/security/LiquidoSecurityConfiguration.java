package org.doogie.liquido.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;


/**
 * Configure everything related to Spring Security
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
//@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)  // WebSecurityConfigurerAdapter has @Order(100) by default
public class LiquidoSecurityConfiguration extends WebSecurityConfigurerAdapter {

  @Value("${spring.data.rest.base-path}")   // value from application.properties file
  String basePath;

	@Value("${security.signing-key}")
	private String signingKey;

	@Value("${security.encoding-strength}")
	private Integer encodingStrength;

	@Value("${security.security-realm}")
	private String securityRealm;


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


  /**  This way global configuration could be configured.  Just an example
	public void configure(AuthenticationManagerBuilder builder) throws Exception {
		builder.inMemoryAuthentication()
			.withUser("joe")
			.password("123")
			.roles("ADMIN");
	}
	 */

  /**
   * Configure HttpSecurity. Autentication is handled in {@link org.doogie.liquido.security.oauth2.ResourceServerConfig}
	 * which has higher priority and comes first
   * @param http spring http security
   * @throws Exception
   */
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    log.trace("Configuring HttpSecurity for "+ basePath);
		http
			.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			.and()
				.authorizeRequests().anyRequest().authenticated()
			.and()
				.antMatcher("/h2-console/**").headers().frameOptions().disable()  // needed for H2 DB console
			.and()
				.csrf().disable();												// TODO: re-enable CSRF check
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

  /******************************** for OAUTH 2.0 **********************/

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

	//MAYBE: use Digest Autentication http://stackoverflow.com/questions/33918432/digest-auth-in-spring-security-with-rest-and-javaconfig
}
