package org.doogie.liquido;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.sql.SQLException;

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

	@Autowired
	JdbcTemplate jdbcTemplate;

  /**
   * Main entry point for Liquido Spring backend.
   * @param args command line arguments (none currently used)
   */
  public static void main(String[] args) throws SchedulerException {
		System.out.println(" _       ___    ___    _   _   ___   ____     ___  ");
		System.out.println("| |     |_ _|  / _ \\  | | | | |_ _| |  _ \\   / _ \\ ");
		System.out.println("| |      | |  | | | | | | | |  | |  | | | | | | | |");
		System.out.println("| |___   | |  | |_| | | |_| |  | |  | |_| | | |_| |");
		System.out.println("|_____| |___|  \\__\\_\\  \\___/  |___| |____/   \\___/ ");


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
		String dbUrl = "unknown";
		try {
			dbUrl = jdbcTemplate.getDataSource().getConnection().getMetaData().getURL();
		} catch (SQLException e) {
			// ignore
		}
		log.info("=====================================================");
		log.info("=== LIQUIDO backend API is up and running under");
		log.info(" BackendBasePath: http://localhost:"+this.serverPort+this.basePath);
		log.info(" DB: "+dbUrl);
		log.info("=======================================================");
		log.info(ppp.toString());
	}

}
