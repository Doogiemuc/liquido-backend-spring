package org.doogie.liquido;

import org.doogie.liquido.testdata.TestDataCreator;
import org.doogie.liquido.util.DoogiesRequestLogger;
import org.h2.server.web.WebServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Main entry class for Liquido
 *
 * Starts the SpringApplication.
 */
@SpringBootApplication
@EnableJpaAuditing   // this is necessary so that UpdatedAt and CreatedAt are handled.
public class LiquidoBackendSpringApplication {
	static Logger log = LoggerFactory.getLogger(LiquidoBackendSpringApplication.class);

	@Autowired
	TestDataCreator testDataCreator;

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
	 * Initialize the H2 DB web console
	 * https://springframework.guru/using-the-h2-database-console-in-spring-boot-with-spring-security/
	 * @return
	 // NOT NECESSARY in spring boot this can be done in application.properties
	@Bean
	ServletRegistrationBean h2servletRegistration(){
		log.trace("======= init H2 console WebServlet");
		ServletRegistrationBean registrationBean = new ServletRegistrationBean( new WebServlet());
		registrationBean.addUrlMappings("/h2-console/*");
		return registrationBean;
	}
	*/

  /**
   * configure logging of HTTP requests
   * Log each request with ReqID, full Request URI and its duration in ms.
   * @return DoogiesRequestLogger
   */
	@Bean
	public OncePerRequestFilter requestLoggingFilter() {
		return new DoogiesRequestLogger();
	}

  /*
	@Bean
	public Validator beforeCreateDelegationModelValidator() {
		log.info("====== creating BeforeCreateDelegationValidator");
		return new BeforeCreateDelegationValidator();
	}
	*/
}
