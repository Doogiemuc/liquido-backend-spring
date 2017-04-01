package org.doogie.liquido.testdata;

import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.model.*;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.LiquidoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.doogie.liquido.model.LawModel.LawStatus;

/**
 * Seed the database with test data. This script will upsert entities, ie. it will only insert new rows
 * for entities that do not exist yet.
 *
 * For checking whether an entity already exists, the functional primary key is used, not the ID field!
 *
 * This is executed right after SpringApplication.run(...) when the command line parameter "--seedDB" is passed.
 * See http://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#boot-features-command-line-runner
 *
 * Other possibilities for initializing a DB with Spring and Hibernate:
 * https://docs.spring.io/spring-boot/docs/current/reference/html/howto-database-initialization.html
 * http://www.sureshpw.com/2014/05/importing-json-with-references-into.html
 * http://www.generatedata.com/
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)   // seed DB first, then run the other CommandLineRunners
public class TestDataCreator implements CommandLineRunner {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  public int NUM_USERS = 10;
  public int NUM_AREAS = 10;
  public int NUM_IDEAS = 10;

  public int NUM_ALTERNATIVE_PROPOSALS = 2;   // alternative proposals that did not yet reach their quorum.
  public int NUM_ELABORATION = 4;             // alternative proposals that did already reach their quorum and are in their ELABORATION PHASE.
  public int NUM_PROPOSALS_IN_VOTING = 4;     // proposals currently in voting phase

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

  @Autowired
  DelegationRepo delegationRepo;

  @Autowired
  BallotRepo ballotRepo;

  @Autowired
  PollRepo pollRepo;

  @Autowired
  KeyValueRepo keyValueRepo;

  @Autowired
  JdbcTemplate jdbcTemplate;

  @Autowired
  LiquidoAuditorAware auditorAware;

  @Autowired
  Environment springEnv;   // load settings from application-test.properties

  @Value("${liquido.likes.for.quorum}")
  public int likesForQuorum;

  // very simple random number generator
  Random rand;

  public TestDataCreator() {
    log.trace("=== ENTER TestDataCreator");
    this.rand = new Random(System.currentTimeMillis());
  }

  /**
   * Seed the DB with some default values, IF
   *  - the currently active spring profiles contain "test"   OR
   *  - there is an environment property seedDB==true  OR
   *  - there is a command line parmaeter "--seedDB"
   * @param args command line args
   */
  public void run(String... args) {
    log.trace("=== running TestDataCreator");
    boolean seedDB =   springEnv.acceptsProfiles("test") || "true".equals(springEnv.getProperty("seedDB"));
    for(String arg : args) {
      if ("--seedDB".equals(arg)) { seedDB = true; }
    }
    if (seedDB) {
      log.info("Populate test DB: "+ jdbcTemplate.getDataSource().toString());
      // The order of these methods is very important here!
      seedUsers();
      auditorAware.setMockAuditor(this.users.get(0));   // Simulate that user is logged in.  This user will be set as @createdAt
      seedGlobalProperties();
      seedAreas();
      seedDelegations();
      seedIdeas();
      seedPollInElaborationPhase();
      seedPollInVotingPhase();
      seedLaws();
      seedBallots();
      auditorAware.setMockAuditor(null);
    }
  }

  private void seedUsers() {
    log.trace("Seeding Users ...");
    this.users = new ArrayList<>();

    for (int i = 0; i < NUM_USERS; i++) {
      String email = "testuser" + i + "@liquido.de";    // Remember that DB IDs start at 1.   testuser4 has ID==5 in DB
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

  public void seedGlobalProperties() {
    log.trace("Seeding global properties ...");
    List<KeyValueModel> propKV = new ArrayList<>();
    propKV.add(new KeyValueModel(LiquidoProperties.KEY.LIKES_FOR_QUORUM.toString(), "10"));
    propKV.add(new KeyValueModel(LiquidoProperties.KEY.SUPPORTERS_FOR_PROPOSAL.toString(), "5"));
    propKV.add(new KeyValueModel(LiquidoProperties.KEY.DAYS_UNTIL_VOTING_STARTS.toString(), "7"));
    propKV.add(new KeyValueModel(LiquidoProperties.KEY.DURATION_OF_VOTING_PHASE.toString(), "7"));
    keyValueRepo.save(propKV);
  }

  /**
   * Create some areas with unique titles. All created by user0
   */
  private void seedAreas() {
    log.trace("Seeding Areas ...");
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
    // User0  is proxy  for users 1,2,3    in "Area 1"
    // and  user 0  then also delegated to user 4  (who now has five votes including his own)
    delegations.add(new DelegationModel(areas.get(1), users.get(1), users.get(0)));
    delegations.add(new DelegationModel(areas.get(1), users.get(2), users.get(0)));
    delegations.add(new DelegationModel(areas.get(1), users.get(3), users.get(0)));
    delegations.add(new DelegationModel(areas.get(1), users.get(0), users.get(4)));

    // User0 is proxy for users 1 and 2 in  "Area 2"
    delegations.add(new DelegationModel(areas.get(2), users.get(1), users.get(0)));
    delegations.add(new DelegationModel(areas.get(2), users.get(2), users.get(0)));

    // upsert delegations
    for (DelegationModel newDele : delegations) {
      DelegationModel existingDele = delegationRepo.findByAreaAndFromUser(newDele.getArea(), newDele.getFromUser());
      if (existingDele != null) {
        log.trace("Updating existing delegation: "+existingDele+" with new toProxy="+newDele.getToProxy());
        existingDele.setToProxy(newDele.getToProxy());
        delegationRepo.save(existingDele);
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
      String ideaTitle = "Idea " + i + " that suggest that we defenitely need a longer title for ideas";
      String ideaDescr = getLoremIpsum(100,+ 400);
      UserModel createdBy = this.users.get(i % NUM_USERS);
      IdeaModel newIdea = new IdeaModel(ideaTitle, ideaDescr, this.areas.get(0), createdBy);
      // add 0 to 3 supporters != createdBy
      for (int j = 0; j < rand.nextInt(4); j++) {
        int supporterNo = rand.nextInt(NUM_USERS);
        if (supporterNo != (i % NUM_USERS)) {           // creator is implicitly aready a supporter
          newIdea.addSupporter(users.get(supporterNo));
        }
      }

      IdeaModel existingIdea = ideaRepo.findByTitle(ideaTitle);
      if (existingIdea != null) {
        log.trace("Updating existing idea with id=" + existingIdea.getId());
        newIdea.setId(existingIdea.getId());
      } else {
        log.trace("Creating new idea " + newIdea);
      }

      auditorAware.setMockAuditor(createdBy);
      IdeaModel savedIdea = ideaRepo.save(newIdea);
      this.ideas.add(savedIdea);
    }
  }

  private void seedPollInElaborationPhase() {
    log.debug("Seeding poll in elaboration phase ...");
    this.laws = new ArrayList<>();

    AreaModel area = this.areas.get(0);
    UserModel createdBy = this.users.get(0);
    auditorAware.setMockAuditor(createdBy);

    //==== A poll in ELABORATION with some first alternatives proposals. Some with and some without quorum yet.
    PollModel poll = new PollModel();
    try {
      poll.setStatus(PollModel.PollStatus.ELABORATION);
      String descr = "Initial proposal for a law with quorum. " + getLoremIpsum(100, 400);
      LawModel initialProposal = new LawModel("Initial Proposal of course with quorum", descr, area, poll, LawStatus.ELABORATION, createdBy);
      //TODO: add supporters to initial proposal.
      poll.addProposal(initialProposal);
      //upsertLaw(initialProposal, 30);

      //===== add some alternative proposals for this initial proposal that did NOT yet reach the neccassary quorum
      for (int i = 0; i < NUM_ALTERNATIVE_PROPOSALS; i++) {
        String lawTitle = "New Alternative Proposal (without quorum)" + i;
        String lawDesc = "Alternative proposal #" + i + " for " + initialProposal.getTitle() + " that did NOT reach quorum yet\n" + getLoremIpsum(100, 400);
        LawModel alternativeProposal = new LawModel(lawTitle, lawDesc, area, poll, LawStatus.NEW_PROPOSAL, createdBy);
        poll.addProposal(alternativeProposal);
        //upsertLaw(alternativeProposal, 30-i);
      }

      //===== and also add some alternative proposals for this initial proposal that DID already reach the necessary quorum and are in the elaboration phase
      for (int i = 0; i < NUM_ELABORATION; i++) {
        String lawTitle = "Alternative Proposal in elaboration (with quorum) " + i;
        String lawDesc = "Alternative proposal #" + i + " for " + initialProposal.getTitle() + " with necessary quorum\n" + getLoremIpsum(100, 400);
        LawModel alternativeProposal = new LawModel(lawTitle, lawDesc, area, poll, LawStatus.ELABORATION, createdBy);
        alternativeProposal.setReachedQuorumAt(DoogiesUtil.daysAgo(i));
        poll.addProposal(alternativeProposal);
        //upsertLaw(alternativeProposal, 30-i);
      }

      //===== save poll. This will automatically also save all proposals
      log.trace("saving poll in elaboration");
      pollRepo.save(poll);
    } catch (Exception e) {
      log.error("Cannot seed Poll: " + e);
      return;
    }

    // ===== fake the createdAt date of the poll and all proposals to be in the past.
    fakeCreateAt(poll, 10);
    for (LawModel proposal : poll.getProposals()) {
      fakeCreateAt(proposal, 10);
    }

  }

  public void seedPollInVotingPhase() {
    log.debug("Seeding poll in voting phase ...");
    this.laws = new ArrayList<>();

    AreaModel area = this.areas.get(0);
    UserModel createdBy = this.users.get(0);
    auditorAware.setMockAuditor(createdBy);

    // Create a poll with some alternative proposals. All of them reached their quorum in time.
    try {
      PollModel poll = new PollModel();

      //===== add proposals to this poll that are in VOTING phase
      for (int i = 0; i < NUM_PROPOSALS_IN_VOTING; i++) {
        String lawTitle = "Proposal in voting " + i;
        String lawDesc = "Proposal #" + i + " in voting phase\n" + getLoremIpsum(100, 400);
        LawModel alternativeProposal = new LawModel(lawTitle, lawDesc, area, poll, LawStatus.VOTING, createdBy);
        alternativeProposal.setReachedQuorumAt(DoogiesUtil.daysAgo(i));
        poll.addProposal(alternativeProposal);
      }

      //===== save poll. This will automatically also save all proposals
      log.trace("saving poll that is in voting phase");
      poll.setStatus(PollModel.PollStatus.VOTING);
      PollModel savedPoll = pollRepo.save(poll);
      log.debug("savedPoll: "+savedPoll);
      //TODO: fake createAt dates
    } catch (Exception e) {
      log.error("Cannot seed Poll: " + e);
      return;
    }
  }


  public void seedLaws() {
    log.trace("Seeding laws");
    AreaModel area = this.areas.get(0);
    UserModel createdBy = this.users.get(0);
    auditorAware.setMockAuditor(createdBy);
    PollModel poll = new PollModel();
    pollRepo.save(poll);
    for (int i = 0; i < NUM_LAWS; i++) {
      String lawTitle = "Law " + i;
      LawModel realLaw = new LawModel(lawTitle, "Complete description of real law #"+i, area, poll, LawStatus.LAW, createdBy);
      realLaw.setReachedQuorumAt(DoogiesUtil.daysAgo(24));
      upsertLaw(realLaw, 20+i);
    }
  }

  /**
   * Will create a new law or update an existing one with matching title.
   * And will set the createdAt date to n days ago
   * @param lawModel the new law to create (or update)
   * @param ageInDays will setCreatedAt to so many days ago (measured from now)
   * @return the saved law
   */
  private LawModel upsertLaw(LawModel lawModel, int ageInDays) {
    LawModel existingLaw = lawRepo.findByTitle(lawModel.getTitle());  // may return null!
    if (existingLaw != null) {
      log.trace("Updating existing law with id=" + existingLaw.getId());
      lawModel.setId(existingLaw.getId());
    } else {
      log.trace("Creating new law " + lawModel);
    }
    LawModel savedLaw = lawRepo.save(lawModel);
    this.laws.add(savedLaw);
    fakeCreateAt(savedLaw, ageInDays);
    return savedLaw;
  }

  private void fakeCreateAt(LawModel lawModel, int ageInDays) {
    if (ageInDays < 0) throw new IllegalArgumentException("ageInDays must be positive");
    Table tableAnnotation = LawModel.class.getAnnotation(javax.persistence.Table.class);
    String tableName = tableAnnotation.name();
    String sql = "UPDATE " + tableName + " SET created_at = DATEADD('DAY', -" + ageInDays + ", NOW()) WHERE id='" + lawModel.getId() + "'";
    log.trace(sql);
    jdbcTemplate.execute(sql);
    Date daysAgo = DoogiesUtil.daysAgo(ageInDays);
    lawModel.setCreatedAt(daysAgo);
    //MAYBE: setUpdatedAt(...)
  }

  private void fakeCreateAt(PollModel poll, int ageInDays) {
    if (ageInDays < 0) throw new IllegalArgumentException("ageInDays must be positive");
    Table tableAnnotation = PollModel.class.getAnnotation(javax.persistence.Table.class);
    String tableName = tableAnnotation.name();
    String sql = "UPDATE " + tableName + " SET created_at = DATEADD('DAY', -" + ageInDays + ", NOW()) WHERE id='" + poll.getId() + "'";
    log.trace(sql);
    jdbcTemplate.execute(sql);
    Date daysAgo = DoogiesUtil.daysAgo(ageInDays);
    poll.setCreatedAt(daysAgo);
    //MAYBE: setUpdatedAt(...)
  }

  public void seedBallots() {
    log.debug("Seeding ballots ...");

    List<PollModel> polls = pollRepo.findByStatus(PollModel.PollStatus.VOTING);
    if (polls.size() == 0) throw new RuntimeException("cannot seed Ballots. There is no poll in voting phase.");  //MAYBE: create one
    PollModel pollInVoting = polls.get(0);
    if (pollInVoting.getNumCompetingProposals() < 2) throw new RuntimeException("Cannot seed ballots. Need at least two alternative proposals in VOTING phase");

    List<LawModel> voteOrder = new ArrayList<>();
    voteOrder.add(pollInVoting.getProposals().get(0));
    voteOrder.add(pollInVoting.getProposals().get(1));
    //TODO: create real BCRYPT token in TestDataCreator   => Can TestDataCreator depend on LiquidoAnonymizer?  => Yes, but need to seed   salt value first!
    //String voterTokenBCrypt = anonymizer.getBCryptVoterToken(currentUser, currentUser.getPasswordHash(), poll);
    BallotModel newBallot = new BallotModel(pollInVoting, voteOrder, "dummyVoterToken");
    ballotRepo.save(newBallot);
  }


  /** @return a dummy text that can be used eg. in descriptions */
  public String getLoremIpsum(int minLength, int maxLength) {
    String loremIpsum = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, nam urna. Vitae aenean velit, voluptate velit rutrum. Elementum integer rhoncus rutrum morbi aliquam metus, morbi nulla, nec est phasellus dolor eros in libero. Volutpat dui feugiat, non magna, parturient dignissim lacus ipsum in adipiscing ut. Et quis adipiscing perferendis et, id consequat ac, dictum dui fermentum ornare rhoncus lobortis amet. Eveniet nulla sollicitudin, dolore nullam massa tortor ullamcorper mauris. Lectus ipsum lacus.\n" +
      "Vivamus placerat a sodales est, vestibulum nec cursus eros fermentum. Felis orci nunc quis suspendisse dignissim justo, sed proin metus, nunc elit ac aliquam. Sed tellus ante ipsum erat platea nulla, enim bibendum gravida condimentum, imperdiet in vitae faucibus ultrices, aenean fringilla at. Rhoncus et sint volutpat, bibendum neque arcu, posuere viverra in, imperdiet duis. Eget erat condimentum congue ipsam. Tortor nostra, adipiscing facilisis donec elit pellentesque natoque integer. Ipsum id. Aenean suspendisse et eros hymenaeos in auctor, porttitor amet id pellentesque tempor, praesent aliquam rhoncus convallis vel, tempor fusce wisi enim aliquam ut nisl, nullam dictum etiam. Nisi accumsan augue sapiente dui, pulvinar cras sapien mus quam nonummy vivamus, in vitae, sociis pede, convallis mollis id mauris. Vestibulum ac quis scelerisque magnis pede in, duis ullamcorper a ipsum ante ornare.\n" +
      "Quam amet. Risus lorem nibh consequat volutpat. Bibendum lorem, mauris sed quisque. Pellentesque augue eros nibh, iaculis maecenas facilisis amet. Nam laoreet elit litora justo, morbi in vitae nisl nulla vestibulum maecenas. Scelerisque lacinia id eget pede nunc in, id a nullam nunc velit mauris class. Duis dui ullamcorper vestibulum, turpis mi eu, arcu pellentesque sit. Arcu nibh elit. Vitae magna magna auctor, class pariatur, tortor eget amet mi pede accumsan, ut quam ut ante nibh vivamus quisque. Magna praesent tortor praesent.";
    int length = minLength + rand.nextInt(maxLength - minLength);
    if (length >= loremIpsum.length()) length = loremIpsum.length()-1;
    return loremIpsum.substring(0, length);
  }


}