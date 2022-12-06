package org.doogie.liquido.util;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.graphql.TeamsGraphQL;
import org.doogie.liquido.jwt.AuthUtil;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.CreateOrJoinTeamResponse;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.doogie.liquido.testdata.TestFixtures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@Order(1001)  // run after TestDataCreator
public class LiquidoInitializer implements CommandLineRunner {

	@Autowired
	LiquidoProperties props;

	@Autowired
	TeamsGraphQL teamsGraphQL;

	@Autowired
	AreaRepo areaRepo;

	@Autowired
	AuthUtil authUtil;

	/**
	 * This runs when application has started successfully
   * and after TestDataCreator
	 */
	@Override
	public void run(String... args) throws LiquidoException {
		// Create a default test user
		authUtil.logoutOfSecurityContext();  //BUGFIX: Make sure no one is logged in before creating a new team for test.
		UserModel testUser = new UserModel(props.testUser.email, props.testUser.name, props.testUser.mobilephone, props.testUser.website, props.testUser.picture);
		try {
			CreateOrJoinTeamResponse res = teamsGraphQL.createNewTeam(props.testUser.teamName, testUser);
			log.info("Created default user: " + res.getTeam().toString());
			testUser = res.getUser();  // withID
		} catch (LiquidoException e) {
			log.error("Cannot create default user", e.getMessage());
			throw e;
		}

		// Create a default area.
		try {
			Optional<AreaModel> defaultAreaOpt = areaRepo.findByTitle(props.defaultAreaTitle);
			if (!defaultAreaOpt.isPresent()) {
				AreaModel defaultArea = new AreaModel(props.defaultAreaTitle, "This is an automatically created default area", testUser);
				areaRepo.save(defaultArea);
				log.info("Created new default area: "+defaultArea);
			}
		} catch (Exception e) {
			log.error("Cannot find or create default area!", e.toString());
			throw e;
		}
	}




}
