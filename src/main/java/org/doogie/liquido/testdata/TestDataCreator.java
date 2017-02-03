package org.doogie.liquido.testdata;

import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.model.*;
import org.doogie.liquido.util.DoogiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.doogie.liquido.model.LawModel.LawStatus;

/**
 * Seed the database with test data. This script will upsert entities, ie. it will only insert new rows
 * for entities that do not exist yet.
 *
 * For checking whether an entity already exists, the functional primary key is used, not the ID field!
 *
 * This is executed right after SpringApplication.run(...) when the command line parameter "--seedDB" is passed.
 * See http://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#boot-features-command-line-runner
 */
@Component
public class TestDataCreator implements CommandLineRunner {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  public int NUM_USERS = 10;
  public int NUM_AREAS = 10;
  public int NUM_IDEAS = 10;

  public int NUM_ALTERNATIVE_PROPOSALS = 3;
  public int NUM_ELABORATION = 1;
  // proposals currently in voting phase

  public int NUM_LAWS = 2;

  @Autowired
  UserRepo userRepo;
  List<UserModel> users;    // will contain the list of created users that can be used in further repos, eg. as "createdBy" value

  @Autowired
  AreaRepo areaRepo;
  List<AreaModel> areas;

  @Autowired
  IdeaRepo ideaRepo;
  List<IdeaModel> ideas;

  @Autowired
  LawRepo lawRepo;
  List<LawModel> laws;
  String initialProposalInVotingTitle;
  List<String> alternativeProposalInVotingTitle;

  @Autowired
  DelegationRepo delegationRepo;

  @Autowired
  BallotRepo ballotRepo;

  //@Autowired
  //LiquidoAuditorAware auditorAware;

  @Autowired
  Environment springEnv;   // load settings from application-test.properties

  public TestDataCreator() {

  }

  /**
   * Seed the DB with some default values, IF
   *  - the currently active spring profiles contain "test"   OR
   *  - there is an environment property seedDB==true  OR
   *  - there is a command line parmaeter "--seedDB"
   * @param args
   */
  public void run(String... args) {
    boolean seedDB =   springEnv.acceptsProfiles("test") || "true".equals(springEnv.getProperty("seedDB"));
    for(String arg : args) {
      if ("--seedDB".equals(arg)) { seedDB = true; }
    }
    if (seedDB) {
      log.info("==== Populate test DB ...");
      // order is important here!
      seedUsers();
      //auditorAware.setMockAuditor(this.users.get(0));   // Simulate that user is logged in.  This user will be set as @createdAt
      seedAreas();
      seedDelegations();
      seedIdeas();
      seedLaws();
      seedBallots();
      //auditorAware.setMockAuditor(null);
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

  /**
   * Create some areas with unique titles. All created by user0
   */
  private void seedAreas() {
    log.debug("Seeding Areas ...");
    this.areas = new ArrayList<>();

    UserModel createdBy = this.users.get(0);

    for (int i = 0; i < NUM_AREAS; i++) {
      String areaTitle = "Area " + i;
      AreaModel newArea = new AreaModel(areaTitle, "Nice description for test area #"+i, createdBy);

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
      if (delegationRepo.exists(Example.of(newDele))) {
        log.trace("Delegation already exists: "+newDele);
      } else {
        DelegationModel savedDelegation = delegationRepo.save(newDele);
        log.trace("Created new delegation " + savedDelegation);
      }
    }
  }


  private void seedIdeas() {
    log.debug("Seeding Ideas ...");
    this.ideas = new ArrayList<>();
    for (int i = 0; i < NUM_IDEAS; i++) {
      String ideaTitle = "Idea " + i;
      String ideaDescr = getLoremIpsum(50);
      UserModel createdBy = this.users.get(i % NUM_USERS);
      IdeaModel newIdea = new IdeaModel(ideaTitle, ideaDescr, this.areas.get(0), createdBy);
      if (i > NUM_IDEAS / 2) {
        newIdea.addSupporter(this.users.get(1));
        newIdea.addSupporter(this.users.get(2));
        newIdea.addSupporter(this.users.get(3));
      }

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

    AreaModel area = this.areas.get(0);
    UserModel createdBy = this.users.get(0);

    // ==== One initial new proposal for a law   and some alternatives with and without quorum
    LawModel initialProposal = LawModel.buildInitialLaw("Initial Proposal", "Perfect proposal for a law with quorum. "+getLoremIpsum(100), area, LawStatus.ELABORATION, createdBy);
    upsertLaw(null, initialProposal);

    // and some alternative proposals for this initial proposal that did not yet reach the neccassary quorum
    for (int i = 0; i < NUM_ALTERNATIVE_PROPOSALS; i++) {
      String lawTitle = "Alternative Proposal " + i;
      String lawDesc  = "Alternative proposal #"+i+" for "+initialProposal.getTitle()+" that did not reach quorum yet\n"+getLoremIpsum(100);
      LawModel alternativeProposal = new LawModel(lawTitle, lawDesc, area, initialProposal, LawStatus.NEW_PROPOSAL, createdBy);
      LawModel existingLaw = lawRepo.findByTitle(lawTitle);
      upsertLaw(existingLaw, alternativeProposal);
    }

    // and some alternative proposals for this initial proposal that did already reach the necessary quorum and are in the elaboration phase
    for (int i = 0; i < NUM_ELABORATION; i++) {
      String lawTitle = "Alternative Proposal " + i;
      String lawDesc  = "Alternative proposal #"+i+" for "+initialProposal.getTitle()+" with necessary quorum\n"+getLoremIpsum(100);
      LawModel alternativeProposal = new LawModel(lawTitle, lawDesc, area, initialProposal, LawStatus.ELABORATION, createdBy);
      LawModel existingLaw = lawRepo.findByTitle(lawTitle);
      upsertLaw(existingLaw, alternativeProposal);
    }

    // ==== Some proposals that currently are in the voting phase (one initial, of course)

    initialProposalInVotingTitle = "Initial Proposal in voting phase";
    alternativeProposalInVotingTitle = new ArrayList<>();
    alternativeProposalInVotingTitle.add("Alternative proposal #1 in voting phase");
    alternativeProposalInVotingTitle.add("Alternative proposal #2 in voting phase");

    LawModel initialProposalInVoting = LawModel.buildInitialLaw(initialProposalInVotingTitle, "Initial proposal that currently is in the voting phase.", area, LawStatus.VOTING, createdBy);
    LawModel existingProposal = lawRepo.findByTitle(initialProposalInVotingTitle);
    upsertLaw(existingProposal, initialProposalInVoting);

    for (String alternativeInVotingTitle: alternativeProposalInVotingTitle) {
      LawModel votingLaw = new LawModel(alternativeInVotingTitle, "Alternative proposal that currently is in the voting phase", area, initialProposalInVoting, LawStatus.VOTING, createdBy);
      LawModel existingLaw = lawRepo.findByTitle(alternativeInVotingTitle);
      upsertLaw(existingLaw, votingLaw);
    }

    // === real laws
    for (int i = 0; i < NUM_LAWS; i++) {
      String lawTitle = "Law " + i;
      LawModel realLaw = LawModel.buildInitialLaw(lawTitle, "Complete description of real law #"+i, area, LawStatus.LAW, createdBy);
      realLaw.setCreatedAt(DoogiesUtil.dayAgo(20+i));
      realLaw.setUpdatedAt(DoogiesUtil.dayAgo(20+i));
      LawModel existingLaw = lawRepo.findByTitle(lawTitle);
      upsertLaw(existingLaw, realLaw);
    }
  }

  private LawModel upsertLaw(LawModel existingLaw, LawModel newLaw) {
    if (existingLaw != null) {
      log.trace("Updating existing law with id=" + existingLaw.getId());
      newLaw.setId(existingLaw.getId());
    } else {
      log.trace("Creating new law " + newLaw);
    }
    LawModel savedLaw = lawRepo.save(newLaw);
    this.laws.add(savedLaw);
    return savedLaw;
  }

  public void seedBallots() {
    log.debug("Seeding ballots ...");

    LawModel initialInVoting = lawRepo.findByTitle(initialProposalInVotingTitle);
    LawModel alt0 = lawRepo.findByTitle(alternativeProposalInVotingTitle.get(0));
    LawModel alt1 = lawRepo.findByTitle(alternativeProposalInVotingTitle.get(1));

    List<LawModel> voteOrder = new ArrayList<>();
    voteOrder.add(initialInVoting);
    voteOrder.add(alt0);
    voteOrder.add(alt1);
    BallotModel newBallot = new BallotModel(initialInVoting, voteOrder, "dummyVoterHash");
    ballotRepo.save(newBallot);
  }


  /** @return a dummy text that can be used eg. in descriptions */
  public String getLoremIpsum(int length) {
    String loremIpsum = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
    return loremIpsum.substring(0, length);
  }


}