package org.doogie.liquido.testdata;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Iterator;

/**
 * Run some jobs for startup
 */
@Slf4j
@Component
@Order(-1000)   // run "very early", before the other command line runners
public class StartupJobs implements CommandLineRunner {
	@Autowired
	UserRepo userRepo;

	@Autowired
	LiquidoProperties prop;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	Environment springEnv;

	/**
	 * Some sanity checks
	 */
	@Override
	public void run(String... args) throws Exception {
		try {
			String dbURL = jdbcTemplate.getDataSource().getConnection().getMetaData().getURL();
			log.info("===== Connecting to DB at "+dbURL);
			//Keep in mind that at this stage the DB might not be filled with a schema or data yet
		} catch (Exception e) {
			log.error("There is something wrong with the database: "+e.toString());
			throw e;
		}

		if (springEnv.acceptsProfiles(Profiles.of("dev", "test"))) {
			log.info("===== Spring environment properties ");
			MutablePropertySources propSrcs = ((AbstractEnvironment) springEnv).getPropertySources();
			Iterator<PropertySource<?>> it = propSrcs.iterator();
			while (it.hasNext()) {
				PropertySource<?> src = it.next();
				log.debug("===== Property Source: " + src.getName());
				if (src instanceof EnumerablePropertySource) {
					String[] propertyNames = ((EnumerablePropertySource<?>) src).getPropertyNames();
					for (int i = 0; i < propertyNames.length; i++) {
						log.debug(propertyNames[i] + "=" + springEnv.getProperty(propertyNames[i]));
					}
				}
			}

			/*
			// https://stackoverflow.com/questions/23506471/spring-access-all-environment-properties-as-a-map-or-properties-object
			StreamSupport.stream(propSrcs.spliterator(), false)
				.forEach(propSrc -> log.debug(propSrc.getName()))
				.filter(ps -> ps instanceof EnumerablePropertySource)
				.map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
				.flatMap(Arrays::<String>stream)
				.forEach(propName -> log.debug(propName + "=" + springEnv.getProperty(propName)));

			 */
		}


	}



}
