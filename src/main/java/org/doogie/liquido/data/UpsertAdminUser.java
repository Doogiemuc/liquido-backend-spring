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
@Order(1000)   // run "late", after DB schema has been created
public class UpsertAdminUser implements CommandLineRunner {
	@Autowired
	UserRepo userRepo;

	@Autowired
	LiquidoProperties prop;

	@Autowired
	TestDataUtils testDataUtils;

	/**
	 * Make sure that there always is an admin user in the DB.
	 */
	@Override
	public void run(String... args) throws Exception {
		UserModel admin = new UserModel(prop.admin.email, prop.admin.name, prop.admin.mobilephone, "", prop.admin.picture);
		log.info("Creating Admin User in DB. "+admin.toStringShort());
		testDataUtils.upsert(admin);
	}

}
