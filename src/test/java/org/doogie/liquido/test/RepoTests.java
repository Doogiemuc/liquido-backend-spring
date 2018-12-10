package org.doogie.liquido.test;

import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.testdata.TestDataCreator;
import org.doogie.liquido.testdata.TestFixtures;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.doogie.liquido.test.matchers.UserMatcher.userWithEMail;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;

/**
 * Unit Test for Spring DB Repositories
 */
@RunWith(SpringRunner.class)
@SpringBootTest
//@EnableJpaAuditing(auditorAwareRef = "liquidoAuditorAware")  => fails becasue of famous Mr. Drotbohm https://jira.spring.io/browse/DATAJPA-367
@ActiveProfiles("test")  // this will also load the settings  from  application-test.properties
public class RepoTests {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired
  LiquidoAuditorAware auditorAware;

  @Autowired
  UserRepo userRepo;

  @Autowired
  LawRepo lawRepo;

  @Autowired
  AreaRepo areaRepo;

  @Test
  public void findAllUsers() {
    log.trace("TEST findAllUsers");
    Iterable<UserModel> allUsers = userRepo.findAll();
    assertThat(allUsers, hasItem(userWithEMail(TestFixtures.USER1_EMAIL)));
    log.info("TEST SUCCESS: "+ TestFixtures.USER1_EMAIL +" found in list of all users.");
  }

  @Test
  public void findUserByEmail() {
    UserModel foundUser = userRepo.findByEmail(TestFixtures.USER1_EMAIL);
    assertNotNull(TestFixtures.USER1_EMAIL +" could not be found", foundUser);
    assertEquals(TestFixtures.USER1_EMAIL, foundUser.getEmail());
    log.info("TEST SUCCESS: "+ TestFixtures.USER1_EMAIL +" found by email.");
  }

  @Test
  @WithUserDetails(TestFixtures.USER1_EMAIL)  // http://docs.spring.io/spring-security/site/docs/4.2.1.RELEASE/reference/htmlsingle/#test-method-withuserdetails
  public void testCreateIdeaWithMockAuditor() {
    log.trace("TEST CreateIdeaWithMockAuditor");
    UserModel user1 = userRepo.findByEmail(TestFixtures.USER1_EMAIL);
    AreaModel area1 = areaRepo.findByTitle(TestFixtures.AREA1_TITLE);
    LawModel newIdea = new LawModel("Idea from test"+System.currentTimeMillis(), "Very nice description from test", area1, user1);

    //auditorAware.setMockAuditor(user1);     //not necessary anymore. Replaced by @WithUserDetails annotation.     // have to mock the currently logged in user for the @CreatedBy annotation in LawModel to work
    LawModel insertedIdea = lawRepo.save(newIdea);
    //auditorAware.setMockAuditor(null);
    log.trace("saved Idea "+insertedIdea);

    Assert.assertEquals("Expected idea to be created by "+TestFixtures.USER1_EMAIL, TestFixtures.USER1_EMAIL, insertedIdea.getCreatedBy().getEmail());
    log.trace("TEST CreateIdeaWithMockAuditor SUCCESSFULL");
  }

  @Test
  @WithUserDetails("testuser1@liquido.de")
  public void testUpdate() {
    log.trace("TEST update");
    long count1 = areaRepo.count();
    AreaModel area = areaRepo.findByTitle(TestFixtures.AREA1_TITLE);
    assertNotNull("Did not find area 1 by title", area);
    area.setDescription("Description from Test");

    areaRepo.save(area);

    long count2 = areaRepo.count();
    assertEquals("Should not have inserted duplicate", count1, count2);
    log.trace("TEST update SUCCESS");
  }

  @Test
  public void testSaveDuplicateArea() {
    log.trace("TEST testSaveDuplicateArea");
    long count1 = areaRepo.count();
    AreaModel area = areaRepo.findByTitle(TestFixtures.AREA1_TITLE);
    assertNotNull("Did not find area 1 by title", area);

    AreaModel areaDuplicate = new AreaModel(area.getTitle(), "This is a duplicate", area.getCreatedBy());
    try {
      areaRepo.save(areaDuplicate);  // should throw DuplicateKeyException
      fail("Did not receive expected DuplicateKeyException");
    } catch ( Exception e) {
      log.trace("TEST testSaveDuplicateArea SUCCESS: did receive expected exception: "+e);
    }
  }

  @Test
  public void testFindSupportedIdeas() {
    UserModel supporter = userRepo.findByEmail(TestFixtures.USER1_EMAIL);
    List<LawModel> supportedLaws = lawRepo.findDistinctByStatusAndSupportersContains(LawModel.LawStatus.IDEA, supporter);
    assertTrue("Expected at least 2 ideas that user "+TestFixtures.USER1_EMAIL+" supports", supportedLaws.size() >= 2);
    log.debug("User "+supporter.getEmail()+" supports "+supportedLaws.size()+" ideas.");
  }
}
