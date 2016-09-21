package org.doogie.liquido;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry class for Liquido
 *
 * Starts the SpringApplication.
 */
@SpringBootApplication
public class LiquidoBackendSpringApplication {
	static Logger log = LoggerFactory.getLogger(LiquidoBackendSpringApplication.class);

	public static void main(String[] args) {
		log.trace("====== Starting Liquido");
		SpringApplication.run(LiquidoBackendSpringApplication.class, args);
	}

}
