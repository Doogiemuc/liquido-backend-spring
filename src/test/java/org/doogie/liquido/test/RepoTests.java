package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.test.testUtils.WithMockTeamUser;
import org.doogie.liquido.testdata.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test for Spring DB Repositories
 */
@Slf4j
@SpringBootTest
//@DataJpaTest
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)  //BUGFIX: must make sure that spring application context is reloaded after all tests in this class have run http://www.javarticles.com/2016/03/spring-dirtiescontext-annotation-example.html
public class RepoTests extends BaseTest {
  @Autowired
  LiquidoAuditorAware auditorAware;

  @Autowired
  UserRepo userRepo;

  @Autowired
  LawRepo lawRepo;

  @Autowired
  AreaRepo areaRepo;

  @Test
  public void findUserByEmail() {
    Optional<UserModel> foundUser = userRepo.findByEmail(TestFixtures.USER1_EMAIL);
    assertTrue(foundUser.isPresent(), TestFixtures.USER1_EMAIL +" could not be found");
    assertEquals(TestFixtures.USER1_EMAIL, foundUser.get().getEmail());
    log.info("TEST SUCCESS: "+ TestFixtures.USER1_EMAIL +" found by email.");
  }

  @Test
  @WithMockTeamUser(email = TestFixtures.USER2_EMAIL)  // http://docs.spring.io/spring-security/site/docs/4.2.1.RELEASE/reference/htmlsingle/#test-method-withuserdetails
  public void testCreateIdeaWithMockAuditor() {
    auditorAware.setMockAuditor(null);    // BUGFIX: Must remove any previously set mock auditor

    Optional<UserModel> currentAuditor = auditorAware.getCurrentAuditor();
    assertTrue(currentAuditor.isPresent());
    UserModel user = currentAuditor.get();
    assertEquals(TestFixtures.USER2_EMAIL, user.getEmail());

    LawModel newIdea = new LawModel("Idea from test"+System.currentTimeMillis(), "Very nice description from test", getDefaultArea());

    //auditorAware.setMockAuditor(user1);     //not necessary anymore. Replaced by @WithUserDetails annotation.     // have to mock the currently logged in user for the @CreatedBy annotation in LawModel to work
    LawModel insertedIdea = lawRepo.save(newIdea);
    //auditorAware.setMockAuditor(null);
    log.trace("saved Idea "+insertedIdea);

    assertEquals(user, insertedIdea.getCreatedBy(), "Expected idea to be created by "+user);
    log.trace("TEST CreateIdeaWithMockAuditor SUCCESSFULL");
  }

  @Test
  @WithMockTeamUser(email = TestFixtures.USER1_EMAIL)
  public void testUpdate() {
    long count1 = areaRepo.count();
    Optional<AreaModel> areaOpt = areaRepo.findByTitle(TestFixtures.AREA1_TITLE);
    assertTrue(areaOpt.isPresent(), "Did not find area 1 by title");
    AreaModel area = areaOpt.get();
    area.setDescription("Description from Test");
    areaRepo.save(area);

    long count2 = areaRepo.count();
    assertEquals(count1, count2, "Should not have inserted duplicate");
    log.trace("TEST update SUCCESS");
  }

  @Test
  public void testSaveDuplicateArea() {
    Optional<AreaModel> areaOpt = areaRepo.findByTitle(TestFixtures.AREA1_TITLE);
    assertTrue(areaOpt.isPresent(), "Did not find area 1 by title");

    AreaModel areaDuplicate = new AreaModel(areaOpt.get().getTitle(), "This is a duplicate", areaOpt.get().getCreatedBy());
    try {
      areaRepo.save(areaDuplicate);  // should throw DuplicateKeyException
      fail("Did not receive expected DuplicateKeyException");
    } catch ( Exception e) {
      log.trace("TEST testSaveDuplicateArea SUCCESS: did receive expected exception: "+e);
    }
  }

  @Test
  public void testFindSupportedProposals() {
    UserModel supporter = userRepo.findByEmail(TestFixtures.USER1_EMAIL).get();
    List<LawModel> supportedLaws = lawRepo.findDistinctByStatusAndSupportersContains(LawModel.LawStatus.PROPOSAL, supporter);
    assertTrue(supportedLaws.size() >= 2, "Expected at least 2 proposals that user "+TestFixtures.USER1_EMAIL+" supports");
    log.debug("User "+supporter.getEmail()+" supports "+supportedLaws.size()+" proposals.");
  }

}
