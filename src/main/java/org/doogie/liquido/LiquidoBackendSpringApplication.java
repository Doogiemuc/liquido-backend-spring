package org.doogie.liquido;

import org.doogie.liquido.util.DoogiesRequestLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Main entry class for Liquido
 *
 * Starts the SpringApplication.
 */
@SpringBootApplication
@EnableScheduling
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

  //TODO: package-by-feature  http://www.javapractices.com/topic/TopicAction.do?Id=205
}
