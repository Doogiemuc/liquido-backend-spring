package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.testdata.TestFixtures;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Unit Test for LIQUIDO Teams
 * see {@link org.doogie.liquido.model.TeamModel}
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class TeamUnitTests extends BaseTest {

	@Autowired
	TeamRepo teamRepo;

	@Autowired
	UserRepo userRepo;

	@Test
	public void testUserInTwoTeams() throws LiquidoException {
		UserModel user = userRepo.findByEmail(TestFixtures.TWO_TEAM_USER_EMAIL)
			.orElseThrow(LiquidoException.notFound("Cannot find test user which is in two teams (email="+TestFixtures.TWO_TEAM_USER_EMAIL+")"));
		List<TeamModel> twoTeams = teamRepo.teamsOfUser(user);
		assertEquals("expected user to be in exactly two teams", 2, twoTeams.size());

		UserModel admin = twoTeams.get(0).getAdmins().stream().findFirst()
			.orElseThrow(LiquidoException.notFound("Team has no admin???"));
		List<TeamModel> oneTeam = teamRepo.teamsOfUser(admin);
		assertEquals("expected this admin to be in exactly one team", 1, oneTeam.size());
	}

}
