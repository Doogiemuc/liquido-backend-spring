package org.doogie.liquido.testdata;

import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seed the database with test data. This script will upsert entities, ie it will only insert new rows
 * for entities that do not exist yet.
 *
 * For checking whether an entity already exists, the functional primary key is used, not the ID field!
 *
 * This is executed right after SpringApplication.run(...) when the command line parameter "--seedDB" is passed.
 *
 * See http://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#boot-features-command-line-runner
 */
@Component
public class TestDataCreator implements CommandLineRunner {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  public int NUM_USERS = 10;
  public int NUM_AREAS = 10;
  public int NUM_IDEAS = 10;
  public int NUM_LAWS  = 10;

  @Autowired
  UserRepo userRepo;
  List<UserModel> users;

  @Autowired
  AreaRepo areaRepo;
  List<AreaModel> areas;

  @Autowired
  IdeaRepo ideaRepo;
  List<IdeaModel> ideas;

  @Autowired
  LawRepo lawRepo;
  List<LawModel> laws;

  @Autowired
  DelegationRepo delegationRepo;

  public TestDataCreator() {
    // EMPTY
  }

  public void run(String... args) {
    for(String arg : args) {
      if ("--seedDB".equals(arg)) {
        log.debug("Populate test DB ...");
        // order is important here!
        seedUsers();
        seedAreas();
        seedDelegations();
        seedIdeas();
        seedLaws();
      }
    }
  }

  private void seedUsers() {
    log.debug("Seeding Users ...");
    this.users = new ArrayList<>();

    for (int i = 0; i < NUM_USERS; i++) {
      String email = "testuser" + i + "@liquido.de";
      UserModel newUser = new UserModel(email, "dummyPasswordHash");

      UserProfileModel profile = new UserProfileModel();
      profile.setName("Test User" + i);
      profile.setPicture("/static/img/Avatar_32x32.jpeg");
      profile.setWebsite("http://www.liquido.de");
      newUser.setProfile(profile);

      UserModel existingUser = userRepo.findByEmail(email);
      if (existingUser != null) {
        log.trace("Updating existing user with id=" + existingUser.getId());
        newUser.setId(existingUser.getId());
      } else {
        log.trace("Creating new user " + newUser);
      }

      UserModel savedUser = userRepo.save(newUser);
      this.users.add(savedUser);
    }
  }

  private void seedAreas() {
    log.debug("Seeding Areas ...");
    this.areas = new ArrayList<>();
    for (int i = 0; i < NUM_AREAS; i++) {
      String areaTitle = "Area " + i;
      AreaModel newArea = new AreaModel(areaTitle, "Nice description for test area");

      AreaModel existingArea = areaRepo.findByTitle(areaTitle);
      if (existingArea != null) {
        log.trace("Updating existing area with id=" + existingArea.getId());
        newArea.setId(existingArea.getId());
      } else {
        log.trace("Creating new area " + newArea);
      }

      AreaModel savedArea = areaRepo.save(newArea);
      this.areas.add(savedArea);
    }
  }

  private void seedDelegations() {
    log.debug("Seeding delegations ...");


    /*   THIS WAS FOR TESTING.  LEARNED A LOT
    DelegationModel delegation1 = new DelegationModel(areas.get(0), users.get(0), users.get(1));
    DelegationModel delegation2 = new DelegationModel(areas.get(0), users.get(2), users.get(1));


    DelegationModel savedDelegation1 = delegationRepo.save(delegation1);   // save will add id to passed object
    log.trace("save1 "+savedDelegation1);

    DelegationModel savedDelegation2 = delegationRepo.save(delegation2);
    log.trace("save2 "+savedDelegation2);

    DelegationModel existingDelegation = delegationRepo.findByAreaAndFromUserAndToProxy(areas.get(0), users.get(0), users.get(1));
    log.info("existing:*********** "+existingDelegation);
    */


    List<DelegationModel> delegations = new ArrayList();
    // User0  is proxy  for users 1,2,3 and 4    in "Area 0"
    // and  user 0  then also delegated to user 4  (who now has five votes including his own)
    delegations.add(new DelegationModel(areas.get(1), users.get(1), users.get(0)));
    delegations.add(new DelegationModel(areas.get(1), users.get(2), users.get(0)));
    delegations.add(new DelegationModel(areas.get(1), users.get(3), users.get(0)));
    delegations.add(new DelegationModel(areas.get(1), users.get(0), users.get(4)));

    // User0 is proxy for users 1 and 2 in  "Area 2"
    delegations.add(new DelegationModel(areas.get(2), users.get(1), users.get(0)));
    delegations.add(new DelegationModel(areas.get(2), users.get(2), users.get(0)));

    for (DelegationModel newDele : delegations) {
      List<DelegationModel> existingDelegations = delegationRepo.findAll(Example.of(newDele));  // will at least return an empty list
      if (existingDelegations.size() > 0) {
        log.trace("Delegation already exists : " + existingDelegations.get(0));
      } else {
        log.trace("Creating new delegation " + newDele);
        DelegationModel savedDelegation = delegationRepo.save(newDele);
      }
    }
  }


  private void seedIdeas() {
    log.debug("Seeding Ideas ...");
    this.ideas = new ArrayList<>();
    for (int i = 0; i < NUM_IDEAS; i++) {
      String ideaTitle = "Idea " + i;
      IdeaModel newIdea = new IdeaModel(ideaTitle, "Very nice idea description", this.areas.get(0), this.users.get(i % NUM_USERS));

      IdeaModel existingIdea = ideaRepo.findByTitle(ideaTitle);
      if (existingIdea != null) {
        log.trace("Updating existing idea with id=" + existingIdea.getId());
        newIdea.setId(existingIdea.getId());
      } else {
        log.trace("Creating new idea " + newIdea);
      }

      IdeaModel savedIdea = ideaRepo.save(newIdea);
      this.ideas.add(savedIdea);
    }
  }

  private void seedLaws() {
    log.debug("Seeding Laws ...");
    this.laws = new ArrayList<>();
    for (int i = 0; i < NUM_LAWS; i++) {
      String lawTitle = "Law " + i;
      LawModel newLaw = new LawModel(lawTitle, "Perfect suggestion for a law", this.users.get(0));
      newLaw.setInitialLaw(newLaw);

      LawModel existingLaw = lawRepo.findByTitle(lawTitle);
      if (existingLaw != null) {
        log.trace("Updating existing law with id=" + existingLaw.getId());
        newLaw.setId(existingLaw.getId());
      } else {
        log.trace("Creating new law " + newLaw);
      }

      LawModel savedLaw = lawRepo.save(newLaw);
      this.laws.add(savedLaw);
    }
  }



}