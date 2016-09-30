package org.doogie.liquido;

import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.UserModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.doogie.liquido.matchers.UserMatcher.userWithEMail;
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

  private static final String USER1_EMAIL = "testuser1@liquido.de";
  private static final int    USER1_NUM_VOTES = 5;

  @Test
  public void findAllUsers() {
    List<UserModel> allUsers = userRepo.findAll();
    System.out.println("TEST findAllUsers: got " + allUsers.size()+" users.");
    assertThat(allUsers, hasItem(userWithEMail("testuser1@liquido.de")));
    log.info("TEST SUCCESS: "+USER1_EMAIL+" found in list of all users.");
  }

  @Test
  public void findUserByEmail() {
    UserModel foundUser = userRepo.findByEmail(USER1_EMAIL);
    assertNotNull(USER1_EMAIL+" could not be found", foundUser);
    assertEquals(USER1_EMAIL, foundUser.getEMail());
    log.info("TEST SUCCESS: "+USER1_EMAIL+" found by email.");
  }

  @Test
  public void getNumVotes() {
    UserModel user1 = userRepo.findByEmail(USER1_EMAIL);
    int numVotes = delegationRepo.getNumVotes(user1.getId());
    assertEquals(USER1_NUM_VOTES, numVotes);
    log.info("TEST SUCCESS: user1 is proxy for "+USER1_NUM_VOTES+" delegates");
  }
}
