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
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;
import java.util.Optional;

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

		log.info("=======================================================");
		log.info(" ServerPort: " + this.serverPort);
		log.info(" spring.data.rest.base-path: " + env.getProperty("spring.data.rest.base-path"));
		log.info(" graphiql.endpoint: "+ env.getProperty("graphiql.endpoint"));
		log.info(" spring.jpa.generate-ddl: " + env.getProperty("spring.jpa.generate-ddl"));
		log.info(" spring.jpa.hibernate.ddl-auto: " + env.getProperty("spring.jpa.hibernate.ddl-auto"));
		log.info(" javax.javax.persistence.schema-generation: " + env.getProperty("javax.javax.persistence.schema-generation"));
		log.info(" Database URL: " + jdbcTemplate.getDataSource().getConnection().getMetaData().getURL());  // may throw SQL Exception
		log.info("=======================================================");
		if (log.isDebugEnabled()) {
			System.out.println();
			System.out.println("LiquidoProperties:");
			System.out.println(liquidoProps.toYaml());
			System.out.println();
		}

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

		//BUGFIX: Make sure H2 driver can be loaded. Otherwise, things may break later at runtime.
		if (env.acceptsProfiles(Profiles.of("dev"))) {
			try {
				Class.forName("org.h2.Driver");
			} catch (ClassNotFoundException e) {
				log.error("ERROR: Cannot load org.h2.Driver!");
			}
			log.info(" Can load H2 Driver in dev");
		}

		try {
			Optional<AreaModel> defaultArea = areaRepo.findByTitle(liquidoProps.defaultAreaTitle);
			if (!defaultArea.isPresent()) {
				String errMsg = "Cannot find default area with title '"+ liquidoProps.defaultAreaTitle+"'";
				log.error(errMsg);
				throw new Exception(errMsg);
			} else {
				log.info(" Ok, default area exists.");
			}
		} catch (Exception e) {
			log.error(e.toString());
			throw e;
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

		log.info("=====================================================");
		log.info("LIQUIDO backend API is up and running in " + Arrays.toString(env.getActiveProfiles()));
		log.info("=====================================================");

	}

}
