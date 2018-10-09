package org.doogie.liquido;

import org.doogie.liquido.util.DoogiesRequestLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Main entry class for Liquido
 *
 * Starts the SpringApplication.
 */
@SpringBootApplication
@EnableWebSecurity
public class LiquidoBackendSpringApplication {
  static Logger log = LoggerFactory.getLogger(LiquidoBackendSpringApplication.class);

  /**
   * main method when run without an application container.
   * spring provides its owen internal app server (jetty). This can be used for testing.
   * @param args command line arguments (none currently used)
   */
  public static void main(String[] args) {
		log.trace("====== Starting Liquido");
		SpringApplication.run(LiquidoBackendSpringApplication.class, args);
  }

  /**
   * configure super advanced and elaborate logging of HTTP requests and responses
   * Log each request with ReqID, full Request URI and its duration in ms.
   * @return DoogiesRequestLogger
   */
  @Bean
  public OncePerRequestFilter requestLoggingFilter() {
		return new DoogiesRequestLogger();
	}


  //TODO: package-by-feature  http://www.javapractices.com/topic/TopicAction.do?Id=205
}
