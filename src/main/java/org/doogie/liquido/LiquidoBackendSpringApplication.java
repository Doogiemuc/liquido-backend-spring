package org.doogie.liquido;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Main entry class for Liquido Backend
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
	LiquidoProperties liquidoProps;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	AreaRepo areaRepo;

  /**
   * Main entry point for Liquido Spring backend.
   * @param args command line arguments (none currently used)
   */
  public static void main(String[] args) throws SchedulerException {
  	//BUGFIX: The code in here may be executed twice: https://stackoverflow.com/questions/49527862/spring-boot-application-start-twice
		System.out.println("====================== Starting LIQUIDO ==========================");
		SpringApplication.run(LiquidoBackendSpringApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void applicationReady() throws Exception {
		System.out.println();
		System.out.println("=====================================================");
		System.out.println(" _       ___    ___    _   _   ___   ____     ___  ");
		System.out.println("| |     |_ _|  / _ \\  | | | | |_ _| |  _ \\   / _ \\ ");
		System.out.println("| |      | |  | | | | | | | |  | |  | | | | | | | |");
		System.out.println("| |___   | |  | |_| | | |_| |  | |  | |_| | | |_| |");
		System.out.println("|_____| |___|  \\__\\_\\  \\___/  |___| |____/   \\___/ ");
		System.out.println("=====================================================");
		System.out.println();

		log.info("=====================================================");
		log.info(" LIQUIDO backend API is up and running.");
		log.info(" Running some sanity checks ...");

		/*
		try {
			Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.start();
		} catch (SchedulerException e) {
			log.error("Cannot start Quartz scheduler", e);
			throw e;
		}
		*/

		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
			log.error("ERROR: Cannot load org.h2.Driver!");
		}
		String dbUrl = "[unknown]";
		try {
			dbUrl = jdbcTemplate.getDataSource().getConnection().getMetaData().getURL();
		} catch (SQLException e) {
			log.error("Cannot get dbUrl: "+e.getMessage());
			throw e;
		}

		AreaModel defaultArea = areaRepo.findByTitle(liquidoProps.defaultAreaTitle).orElse(null);
		if (defaultArea == null) {
			String errMsg = "Cannot find default area with title '"+ liquidoProps.defaultAreaTitle+"'";
			log.error(errMsg);
			throw new Exception(errMsg);
		}
		liquidoProps.setDefaultArea(defaultArea);

		log.info(" ... sanity checks: successful.");

		log.info(" BackendBasePath: http://localhost:"+this.serverPort+this.basePath);
		log.info(" DB: "+dbUrl);
		try {
			ResultSet resultSet = jdbcTemplate.getDataSource().getConnection().getMetaData().getTables(null, null, "AREAS", null);
			if (resultSet.next()) {
				log.info(" Liquido table AREAS exists.");
			}
		} catch (Exception e) {
			// ignore
		}
		log.info(" "+ liquidoProps.toString());
		log.info("=======================================================");
	}

}
