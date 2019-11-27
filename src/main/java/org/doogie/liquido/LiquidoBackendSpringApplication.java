package org.doogie.liquido;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.data.LiquidoProperties;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry class for Liquido Backend
 *
 * Starts the SpringApplication.
 */
@SpringBootApplication
@EnableScheduling
@Slf4j
public class LiquidoBackendSpringApplication {

	@Value("${server.port}")
	private String serverPort;

	/** path prefix for REST API from application.properties */
	@Value(value = "${spring.data.rest.base-path}")
	String basePath;

	@Autowired
	LiquidoProperties ppp;

  /**
   * Main entry point for Liquido Spring backend.
   * @param args command line arguments (none currently used)
   */
  public static void main(String[] args) throws SchedulerException {
		System.out.println("====== Starting Liquido Backend =====");
		/*
		try {
			Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.start();
		} catch (SchedulerException e) {
			log.error("Cannot start Quartz scheduler", e);
			throw e;
		}
		*/
		SpringApplication.run(LiquidoBackendSpringApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void applicationStarted() {

		log.info(ppp.toString());

		log.info("=====================================================");
		log.info("=== LIQUIDO backend is up and running at");
		log.info("=== http://localhost:"+this.serverPort+this.basePath);
		log.info("=======================================================");
	}

  //TODO: package-by-feature  http://www.javapractices.com/topic/TopicAction.do?Id=205
}
