package org.doogie.liquido;

import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.service.UserService;
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
import static org.junit.Assert.assertThat;

/**
 * Unit Test for UserService class.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class UserServiceTest {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private UserService userService;

  @Test
  public void findAllUsers() {
    List<UserModel> allUsers = userService.findAll();
    System.out.println("TEST findAllUsers: got" + allUsers.size()+" users.");
    assertThat(allUsers, hasItem(userWithEMail("testuser1@liquido.de")));
    log.info("TEST SUCCESS: testuser1@liquido.de found.");
  }
}
