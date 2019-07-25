package org.doogie.liquido;

import org.quartz.SchedulerException;
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
public class LiquidoBackendSpringApplication {

	@Value("${server.port}")
	private String serverPort;

	/** path prefix for REST API from application.properties */
	@Value(value = "${spring.data.rest.base-path}")
	String basePath;

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
		System.out.println("=====================================================");
		System.out.println("=== LIQUIDO backend is up and running at");
		System.out.println("=== http://localhost:"+this.serverPort+this.basePath);
		System.out.println("=======================================================");
	}

  //TODO: package-by-feature  http://www.javapractices.com/topic/TopicAction.do?Id=205
}
