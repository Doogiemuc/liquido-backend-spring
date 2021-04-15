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
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.StreamSupport;

/**
 * Main entry class for Liquido Backend
 */
@SpringBootApplication
@EnableScheduling
@Slf4j
public class LiquidoBackendSpringApplication {

	@Value("${server.port}")
	String serverPort;

	@Autowired
	Environment env;

	@Autowired
	LiquidoProperties liquidoProps;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	AreaRepo areaRepo;

  /**
   * Main entry point for Liquido Spring backend.
   * @param args command line arguments
   */
  public static void main(String[] args) throws SchedulerException {
  	//This code may be executed twice: https://stackoverflow.com/questions/49527862/spring-boot-application-start-twice
		System.out.println("====================== Starting LIQUIDO ==========================");
		SpringApplication.run(LiquidoBackendSpringApplication.class, args);
	}

	/**
	 * When application started successfully, then perform some sanity checks
	 * and log the most important security configuration.
	 * @throws Exception
	 */
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

		/*
		try {
			Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.start();
		} catch (SchedulerException e) {
			log.error("Cannot start Quartz scheduler", e);
			throw e;
		}
		*/

		if (env.acceptsProfiles(Profiles.of("dev"))) {
			try {
				Class.forName("org.h2.Driver");
			} catch (ClassNotFoundException e) {
				log.error("ERROR: Cannot load org.h2.Driver!");
			}
			log.info("Can load H2 Driver in env=development");
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

		log.info("=====================================================");
		log.info(" LIQUIDO backend API is up and running in " + Arrays.toString(env.getActiveProfiles()));
		log.info(" ServerPort: " + this.serverPort);
		log.info(" Database URL: "+dbUrl);
		log.info(" spring.data.rest.base-path: " + env.getProperty("spring.data.rest.base-path"));
		log.info(" graphiql.endpoint: "+ env.getProperty("graphiql.endpoint"));
		log.info(" spring.jpa.generate-ddl: " + env.getProperty("spring.jpa.generate-ddl"));
		log.info(" spring.jpa.hibernate.ddl-auto: " + env.getProperty("spring.jpa.hibernate.ddl-auto"));
		log.info(" javax.javax.persistence.schema-generation: " + env.getProperty("javax.javax.persistence.schema-generation"));
		log.info("=======================================================");
		if (log.isDebugEnabled()) {
			System.out.println("LiquidoProperties:");
			System.out.println(liquidoProps.toYaml());
			log.info("=======================================================");
		}


		/* This way you could log ALL effective environment properties. But this reveals all passwords!!!
		// https://stackoverflow.com/questions/23506471/spring-access-all-environment-properties-as-a-map-or-properties-object
		MutablePropertySources propSources = ((AbstractEnvironment) env).getPropertySources();
		StreamSupport.stream(propSources.spliterator(), false).forEach(propSrc -> {
			log.debug("=== property source: " + propSrc.getName() + " ===");
			if (propSrc instanceof MapPropertySource) {
				((MapPropertySource)propSrc).getSource().forEach((key, val) -> {
					log.debug(key + "=" + val);
				});
			}
		});
		*/

	}

}
