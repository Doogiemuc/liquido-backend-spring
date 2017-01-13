package org.doogie.liquido.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;


@Slf4j
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
//@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
public class LiquidoSecurityConfiguration extends WebSecurityConfigurerAdapter {

  @Value("${spring.data.rest.base-path}")   // value from application.properties file
  String restBasePath;

  @Bean
  public LiquidioUserDetailsService liquidoUserDetailsService() {
    log.debug("getting LiquidioUserDetailsService");
    return new LiquidioUserDetailsService();
  }

  /*
  /*  WORKING: Simplest possible in memory authentication with static default users.
  @Autowired
  public void configureAuth(AuthenticationManagerBuilder auth) throws Exception {
    log.debug("Adding users to inMemoryAuthentication");
    auth.inMemoryAuthentication()
      .withUser("user").password("user").roles("USER").and()
      .withUser("admin").password("admin").roles("USER", "ADMIN");
  }
  */


  /**
   * Configure HttpSecurity:
   *   - Allow access to H2 DB web console under /h2-console
   *   - allowe authentication with HTTP basic auth
   *   - TODO: digest authentication   http://stackoverflow.com/questions/33918432/digest-auth-in-spring-security-with-rest-and-javaconfig
   * @param http
   * @throws Exception
   */
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    log.trace("Configuring HttpSecurity for "+restBasePath);
    //http.authorizeRequests().anyRequest().authenticated().and().formLogin().and().httpBasic();

    http
      .authorizeRequests()
        .antMatchers("/h2-console").permitAll()  // Allow access to H2 DB web console
        .antMatchers(restBasePath+"/_ping").permitAll()        // is alive
        .anyRequest().authenticated()
      .and()
        .httpBasic()
      .and()
        .headers().frameOptions().disable()   // TODO: temporary necessary to make /h2-console working
      .and()
        .csrf().disable(); // Disable CSRF since it's not critical for the scope of testing.
  //  .and().cors().disabled();

  }

  /**
   * http://stackoverflow.com/a/31748398/122441 until https://jira.spring.io/browse/DATAREST-573
   * https://spring.io/blog/2015/06/08/cors-support-in-spring-framework#filter-based-cors-support
   * @return
   */
  @Bean
  public FilterRegistrationBean corsFilter() {
    log.trace("Configuring CORS from spring blog");
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    config.addAllowedOrigin("http://localhost");
    config.addAllowedOrigin("http://localhost:3001");
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");
    source.registerCorsConfiguration("/**", config);
    FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return bean;
  }
/*
  @Bean
  public CorsFilter corsFilter() {
    log.trace("Creating corsFilter in LiquidoSecurityConfiguration");
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues();

    config.
    //config.setAllowCredentials(true);
    //config.addAllowedOrigin("http://localhost");
    //config.addAllowedOrigin("http://localhost:8080");
    //config.addAllowedHeader("*");
    //config.addAllowedMethod("*");

    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
  }
  */
}
