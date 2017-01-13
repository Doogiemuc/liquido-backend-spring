package org.doogie.liquido.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;


@Slf4j
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
//@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
public class LiquidoSecurityConfiguration extends WebSecurityConfigurerAdapter {


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


  @Override
  protected void configure(HttpSecurity http) throws Exception {
    log.trace("Configuring HttpSecurity");
    //http.authorizeRequests().anyRequest().authenticated().and().formLogin().and().httpBasic();

    http.authorizeRequests()
      .antMatchers("/h2-console").permitAll()   // Allow access to H2 DB web console
      .anyRequest().authenticated()
      .and()
      .httpBasic()
      .and()
      .headers().disable()   // TODO: remove  temporary necessary to make /h2-console working
      .csrf().disable(); // Disable CSRF since it's not critical for the scope of testing.
  }

}
