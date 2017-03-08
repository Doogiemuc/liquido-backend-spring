package org.doogie.liquido.test;

import org.doogie.liquido.datarepos.IdeaRepo;
import org.doogie.liquido.model.IdeaModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.testdata.TestDataCreator;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.UserModel;
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

import static org.doogie.liquido.test.matchers.UserMatcher.userWithEMail;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;

/**
 * Unit Test for UserService class.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableJpaAuditing(auditorAwareRef = "liquidoAuditorAware")
@ActiveProfiles("test")  // this will also load the settings  from  application-test.properties
public class RepoTests {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired
  LiquidoAuditorAware auditorAware;

  @Autowired
  TestDataCreator testDataCreator;

  @Autowired
  UserRepo userRepo;

  @Autowired
  DelegationRepo delegationRepo;

  @Autowired
  AreaRepo areaRepo;

  @Autowired
  IdeaRepo ideaRepo;

//  @PostConstruct
//  public void populateTestDB() {
//    //Test data was automatically injected by the CommandLineRunner, but maybe sometime we'll need to do it here.
//  }


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
  @WithUserDetails("testuser1@liquido.de")  // http://docs.spring.io/spring-security/site/docs/4.2.1.RELEASE/reference/htmlsingle/#test-method-withuserdetails
  public void testCreateIdeaWithMockAuditor() {
    UserModel user1 = userRepo.findByEmail(TestFixtures.USER1_EMAIL);
    AreaModel area1 = areaRepo.findByTitle(TestFixtures.AREA1_TITLE);
    IdeaModel newIdea = new IdeaModel("Idea from test"+System.currentTimeMillis(), "Very nice description from test", area1, user1);

    //auditorAware.setMockAuditor(user1);     //not necessary anymore. Replaced by @WithUserDetails annotation.     // have to mock the currently logged in user for the @CreatedBy annotation in IdeaModel to work
    IdeaModel insertedIdea = ideaRepo.save(newIdea);
    //auditorAware.setMockAuditor(null);
    log.trace("saved Idea "+insertedIdea);

    Iterable<IdeaModel> ideas = ideaRepo.findAll();
    for(IdeaModel idea : ideas) {
      log.debug(idea.toString());
    }
  }

  @Test
  public void getNumVotes() {
    log.trace("TEST getNumVotes of "+ TestFixtures.USER4_EMAIL +" in area.title='"+ TestFixtures.AREA1_TITLE +"'");
    UserModel user4 = userRepo.findByEmail(TestFixtures.USER4_EMAIL);
    AreaModel area1 = areaRepo.findByTitle(TestFixtures.AREA1_TITLE);
    long numVotes = delegationRepo.getNumVotes(area1, user4);
    assertEquals(TestFixtures.USER4_NUM_VOTES, numVotes);
    log.info("TEST SUCCESS: "+ TestFixtures.USER4_EMAIL +" is proxy for "+ TestFixtures.USER4_NUM_VOTES +" delegates (including himself).");
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
}
