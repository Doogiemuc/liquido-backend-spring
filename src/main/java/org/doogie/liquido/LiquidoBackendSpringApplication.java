package org.doogie.liquido;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry class for Liquido Backend
 *
 * Starts the SpringApplication.
 */
@SpringBootApplication
@EnableScheduling
public class LiquidoBackendSpringApplication {
  static Logger log = LoggerFactory.getLogger(LiquidoBackendSpringApplication.class);

  /**
   * Main entry point for Liquido Spring backend.
   * @param args command line arguments (none currently used)
   */
  public static void main(String[] args) throws SchedulerException {
		log.trace("====== Starting Liquido");
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

		System.out.println("=======================================================");
		System.out.println("======== LIQUIDO backend is up and running! ===========");
		System.out.println("=======================================================");
	}

  //TODO: package-by-feature  http://www.javapractices.com/topic/TopicAction.do?Id=205
}
