package org.doogie.liquido.testdata;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Make sure that there is an admin user
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
		UserModel adminUser = testDataUtils.upsert(admin);
		log.info("Created Admin User in DB. "+admin.toStringShort());
	}

}
