package org.doogie.liquido.test;

import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.doogie.liquido.test.matchers.UserMatcher.userWithEMail;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;

/**
 * Unit Test for UserService class.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class RepoTests {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private UserRepo userRepo;

  @Autowired
  private DelegationRepo delegationRepo;

  @Autowired
  private AreaRepo areaRepo;


  @Test
  public void findAllUsers() {
    log.trace("TEST findAllUsers");
    List<UserModel> allUsers = userRepo.findAll();
    assertThat(allUsers, hasItem(userWithEMail(TestFixtures.USER1_EMAIL)));
    log.info("TEST SUCCESS: "+ TestFixtures.USER1_EMAIL +" found in list of all users.");
  }

  @Test
  public void findUserByEmail() {
    UserModel foundUser = userRepo.findByEmail(TestFixtures.USER1_EMAIL);
    assertNotNull(TestFixtures.USER1_EMAIL +" could not be found", foundUser);
    assertEquals(TestFixtures.USER1_EMAIL, foundUser.getEMail());
    log.info("TEST SUCCESS: "+ TestFixtures.USER1_EMAIL +" found by email.");
  }

  @Test
  public void getNumVotes() {
    log.trace("TEST getNumVotes of "+ TestFixtures.USER4_EMAIL +" in area.title='"+ TestFixtures.AREA1_TITLE +"'");
    UserModel user4 = userRepo.findByEmail(TestFixtures.USER4_EMAIL);
    AreaModel area1 = areaRepo.findByTitle(TestFixtures.AREA1_TITLE);
    int numVotes = delegationRepo.getNumVotes(user4.getId(), area1.getId());
    assertEquals(TestFixtures.USER4_NUM_VOTES, numVotes);
    log.info("TEST SUCCESS: "+ TestFixtures.USER4_EMAIL +" is proxy for "+ TestFixtures.USER4_NUM_VOTES +" delegates (including himself).");
  }
}
