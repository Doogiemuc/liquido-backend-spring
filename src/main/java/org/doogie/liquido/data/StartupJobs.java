package org.doogie.liquido.data;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Optional;

/**
 * Run some jobs for startup
 *  - Make sure that there is an admin user
 *  - Log stuff in DEV
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
	 * Make sure that there always is an admin user in the DB.
	 */
	@Override
	public void run(String... args) throws Exception {
		try {
			String dbURL = jdbcTemplate.getDataSource().getConnection().getMetaData().getURL();
			log.info("===== Connecting to DB at "+dbURL);
		} catch (SQLException e) {
			e.printStackTrace();
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

		UserModel admin = new UserModel(prop.admin.email, prop.admin.name, prop.admin.mobilephone, "", prop.admin.picture);
		log.info("Creating Admin User in DB. "+admin.toStringShort());
		upsert(admin);
	}


	private UserModel upsert(UserModel user) {
		Optional<UserModel> existingUser = userRepo.findByEmail(user.getEmail());
		if (existingUser.isPresent()) {
			user.setId(existingUser.get().getId());
		}
		return userRepo.save(user);
	}
}
