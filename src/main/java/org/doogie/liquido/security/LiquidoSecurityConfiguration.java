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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;


/**
 * Configure everything related to Spring Security
 */
@Slf4j
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
//@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
public class LiquidoSecurityConfiguration extends WebSecurityConfigurerAdapter {

  @Value("${spring.data.rest.base-path}")   // value from application.properties file
  String restBasePath;

  //see http://docs.spring.io/spring-security/site/docs/4.2.1.RELEASE/reference/htmlsingle/#jc-authentication-userdetailsservice
  @Bean
  public LiquidoUserDetailsService liquidoUserDetailsService() {
    log.debug("creating LiquidioUserDetailsService");
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


  //MAYBE: use Digest Autentication http://stackoverflow.com/questions/33918432/digest-auth-in-spring-security-with-rest-and-javaconfig

  /**
   * Configure HttpSecurity:
   *   - Allow access to H2 DB web console under /h2-console
   *   - allow authentication with HTTP basic auth
   *   - TODO: digest authentication   http://stackoverflow.com/questions/33918432/digest-auth-in-spring-security-with-rest-and-javaconfig
   * @param http
   * @throws Exception
   */
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    log.trace("Configuring HttpSecurity for "+restBasePath);
    //http.authorizeRequests().anyRequest().authenticated().and().formLogin().and().httpBasic();

    http
      .cors().disable()   //TODO: Turn CSRF back on an implement it on the client.
      .csrf().disable()   //TODO: Turn CORS back on
      .authorizeRequests()
        .antMatchers("/h2-console/**").permitAll()            // Allow access to H2 DB web console
        .antMatchers(restBasePath+"/_ping").permitAll()       // is alive
        .antMatchers(restBasePath+"/globalProperties").permitAll()
        .antMatchers(restBasePath+"/castVote").permitAll()    // allow anonymous voting
        .anyRequest().authenticated()
      .and()
        .httpBasic()
      .and()
        .headers().frameOptions().disable();   // TODO: temporary necessary to make /h2-console working

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

  /*
  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurerAdapter() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        log.debug("adding CORS mapping for path="+restBasePath);
        registry.addMapping(restBasePath).allowedOrigins("*").allowedMethods("*");
      }
    };
  }
  */

}
