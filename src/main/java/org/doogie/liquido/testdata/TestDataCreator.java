package org.doogie.liquido.testdata;

import net.bytebuddy.utility.RandomString;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.model.*;
import org.doogie.liquido.rest.dto.CastVoteRequest;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LawService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.PollService;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.LiquidoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.*;

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
 * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#core.repository-populators
 * https://docs.spring.io/spring-boot/docs/current/reference/html/howto-database-initialization.html
 * http://www.sureshpw.com/2014/05/importing-json-with-references-into.html
 * http://www.generatedata.com/
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)   // seed DB first, then run the other CommandLineRunners
public class TestDataCreator implements CommandLineRunner {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  public int NUM_USERS = 20;
  public int NUM_AREAS = 10;
  public int NUM_IDEAS = 111;
  public int NUM_PROPOSALS = 50;

  public int NUM_ALTERNATIVE_PROPOSALS = 5;   // proposals in poll
  public int NUM_PROPOSALS_IN_VOTING = 4;     // proposals currently in voting phase

  public int NUM_LAWS = 2;


  //@Autowired
	//EntityManager em;

  @Autowired
  UserRepo userRepo;
  List<UserModel> users = new ArrayList();    // will contain the list of created users that can be used in further repos, eg. as "createdBy" value

  @Autowired
  AreaRepo areaRepo;
  List<AreaModel> areas = new ArrayList();

  @Autowired
  LawRepo lawRepo;
  List<LawModel> lawModels = new ArrayList();    // ideas, proposals and laws

  @Autowired
  LawService lawService;

  @Autowired
	CommentRepo commentRepo;

  @Autowired
  DelegationRepo delegationRepo;

  @Autowired
  BallotRepo ballotRepo;

  @Autowired
  PollRepo pollRepo;

  @Autowired
  PollService pollService;

  @Autowired
  CastVoteService castVoteService;

  @Autowired
  LiquidoProperties props;

  /*
  @Autowired
  KeyValueRepo keyValueRepo;
  */

  @Autowired
  JdbcTemplate jdbcTemplate;

  @Autowired
  LiquidoAuditorAware auditorAware;

  @Autowired
  Environment springEnv;   // load settings from application-test.properties


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
    boolean seedDB = springEnv.acceptsProfiles("test") || "true".equals(springEnv.getProperty("seedDB"));
    for(String arg : args) {
      if ("--seedDB".equals(arg)) { seedDB = true; }
    }
    if (seedDB) {
      log.trace("=== running TestDataCreator with --seedDB");
      log.info("Populate test DB: "+ jdbcTemplate.getDataSource().toString());
      // The order of these methods is very important here!
      seedUsers();
      auditorAware.setMockAuditor(this.users.get(0));   // Simulate that user is logged in.  This user will be set as @createdAt
      //seedGlobalProperties();
      seedAreas();
      seedDelegations();
      seedIdeas();
      seedProposals();
      seedPollInElaborationPhase();
      seedPollInVotingPhase();
      seedLaws();
      seedVotes();

      auditorAware.setMockAuditor(null);
    }
  }

  /*   Global properties are automatically initialized from application.properties file
  private void seedGlobalProperties() {
    log.trace("Seeding global properties ...");
    List<KeyValueModel> propKV = new ArrayList<>();
    propKV.add(new KeyValueModel(LiquidoProperties.KEY.SUPPORTERS_FOR_PROPOSAL.toString(), "5"));
    propKV.add(new KeyValueModel(LiquidoProperties.KEY.DAYS_UNTIL_VOTING_STARTS.toString(), "14"));
    propKV.add(new KeyValueModel(LiquidoProperties.KEY.DURATION_OF_VOTING_PHASE.toString(), "14"));
    keyValueRepo.save(propKV);
  }
  */


  private String getGlobalProperty(LiquidoProperties.KEY key) {
    return props.get(key);

    /*
    KeyValueModel kv = keyValueRepo.findByKey(key.toString());
    return kv.getValue();
    */
  }

  private Integer getGlobalPropertyAsInt(LiquidoProperties.KEY key) {
    return props.getInt(key);

    //return Integer.valueOf(getGlobalProperty(key));
  }

  private void seedUsers() {
    log.info("Seeding Users ... this will bring up some 'Cannot getCurrentAuditor' WARNings that you can ignore.");
    this.users = new ArrayList<>();

    for (int i = 0; i < NUM_USERS; i++) {
      String email = "testuser" + i + "@liquido.de";    // Remember that DB IDs start at 1.   testuser4 has ID==5 in DB
      UserModel newUser = new UserModel(email, "dummyPasswordHash");

      UserProfileModel profile = new UserProfileModel();
      profile.setName("Test User" + i);
      profile.setPicture("/static/img/photos/"+((i%3)+1)+".png");
      profile.setWebsite("http://www.liquido.de");
      newUser.setProfile(profile);

      UserModel existingUser = userRepo.findByEmail(email);
      if (existingUser != null) {
        log.debug("Updating existing user with id=" + existingUser.getId());
        newUser.setId(existingUser.getId());
      } else {
        log.debug("Creating new user " + newUser);
      }

      UserModel savedUser = userRepo.save(newUser);
      this.users.add(savedUser);
      if (i==0) auditorAware.setMockAuditor(this.users.get(0));   // prevent some warnings
    }
  }

  /**
   * Create some areas with unique titles. All created by user0
   */
  private void seedAreas() {
    log.info("Seeding Areas ...");
    this.areas = new ArrayList<>();

    UserModel createdBy = this.users.get(0);

    for (int i = 0; i < NUM_AREAS; i++) {
      String areaTitle = "Area " + i;
      AreaModel newArea = new AreaModel(areaTitle, "Nice description for test area #"+i, createdBy);

      AreaModel existingArea = areaRepo.findByTitle(areaTitle);
      if (existingArea != null) {
        log.debug("Updating existing area with id=" + existingArea.getId());
        newArea.setId(existingArea.getId());
      } else {
        log.debug("Creating new area " + newArea);
      }

      AreaModel savedArea = areaRepo.save(newArea);
      this.areas.add(savedArea);
    }
  }

  private void seedDelegations() {
    log.info("Seeding delegations ...");

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
    log.info("Seeding Ideas ...");
    for (int i = 0; i < NUM_IDEAS; i++) {
      String ideaTitle = "Idea " + i + " that suggest that we definitely need a longer title for ideas";
      StringBuffer ideaDescr = new StringBuffer();
      ideaDescr.append(RandomString.make(8));    // prepend with some random chars to test sorting
      ideaDescr.append(" ");
      ideaDescr.append(getLoremIpsum(0,400));

      UserModel createdBy = this.users.get(i % NUM_USERS);
      auditorAware.setMockAuditor(createdBy);
      AreaModel area = this.areas.get(i % this.areas.size());
      LawModel newIdea = new LawModel(ideaTitle, ideaDescr.toString(), area, createdBy);
      lawRepo.save(newIdea);

      int numSupporters = rand.nextInt(getGlobalPropertyAsInt(LiquidoProperties.KEY.SUPPORTERS_FOR_PROPOSAL)-1);
      //log.debug("adding "+numSupporters+" supporters to idea "+newIdea);
      addSupportersToIdea(newIdea, numSupporters);   // add some supporters, but not enough to become a proposal

      /*
      LawModel existingIdea = lawRepo.findByTitle(ideaTitle);
      if (existingIdea != null) {
        log.trace("Updating existing idea with id=" + existingIdea.getId());
        newIdea.setId(existingIdea.getId());
      } else {
        log.trace("Creating new idea " + newIdea);
      }
      */

      //LawModel savedIdea = lawRepo.save(newIdea);
      fakeCreateAt(newIdea, i+1);
      fakeUpdatedAt(newIdea, i);
      this.lawModels.add(newIdea);
    }
  }



  private LawModel createProposal(String title, String description, AreaModel area, UserModel createdBy, int ageInDays) {
    auditorAware.setMockAuditor(createdBy);
    LawModel proposal = new LawModel(title, description, area, createdBy);
    lawRepo.save(proposal);
    proposal = addSupportersToIdea(proposal, getGlobalPropertyAsInt(LiquidoProperties.KEY.SUPPORTERS_FOR_PROPOSAL));
    //proposal.setStatus(LawStatus.PROPOSAL);
    //LawModel savedProposal = lawRepo.save(proposal);
    fakeCreateAt(proposal,  ageInDays);
    fakeUpdatedAt(proposal, ageInDays > 1 ? ageInDays - 1 : 0);
    this.lawModels.add(proposal);
    return proposal;
  }

  private LawModel createRandomProposal(String title) {
    StringBuffer description = new StringBuffer();
    description.append(RandomString.make(8));    // prepend with some random chars to test sorting
    description.append(" ");
    description.append(getLoremIpsum(0,400));
    UserModel createdBy = this.users.get(rand.nextInt(NUM_USERS));
    AreaModel area = this.areas.get(rand.nextInt(NUM_AREAS));
    LawModel proposal = createProposal(title, description.toString(), area, createdBy, rand.nextInt(10));
    return proposal;

  }

  /** seed proposals, ie. ideas that have already reached their quorum */
  private void seedProposals() {
    log.info("Seeding Proposals ...");
    for (int i = 0; i < NUM_PROPOSALS; i++) {
      String title = "Proposal " + i + " that reached its quorum";
      LawModel proposal = createRandomProposal(title);
      log.debug("Created proposal "+proposal);
    }
    // make sure, that testuser0 has at least 5 proposals
    for (int i = 0; i < 5; i++) {
			UserModel createdBy = this.users.get(0);
    	String title = "Proposal " + i + " for user "+createdBy.getEmail();
      String description = getLoremIpsum(100,400);
      AreaModel area = this.areas.get(rand.nextInt(NUM_AREAS));
      int ageInDays = rand.nextInt(10);
      LawModel proposal = createProposal(title, description, area, createdBy, ageInDays);
      log.debug("Created proposal for user "+createdBy.getEmail());
    }
  }


  /**
   * Add num supporters to idea/proposal.   This only adds the references. It does not save/persist anything.
   * This is actually a quite interesting algorithm, because the initial creator of the idea must not be added as supporter
   * and supporters must not be added twice.
   * @param idea the idea to add to
   * @param num number of new supporters to add.
   * @return the idea with the added supporters. The idea might have reached its quorum and now be a proposal
   */
  private LawModel addSupportersToIdea(LawModel idea, int num) {
    if (num >= users.size()-1) throw new RuntimeException("Cannot at "+num+" supporters to idea. There are not enough users.");
    // https://stackoverflow.com/questions/8378752/pick-multiple-random-elements-from-a-list-in-java
    LinkedList<UserModel> otherUsers = new LinkedList<>();
    for (UserModel user: this.users) {
      if (!user.equals(idea.getCreatedBy()))   otherUsers.add(user);
    }
    Collections.shuffle(otherUsers);
    LawModel ideaFromDB = lawRepo.findOne(idea.getId());  // Get JPA "attached" entity
    List<UserModel> newSupporters = otherUsers.subList(0, num);
    for (UserModel supporter: newSupporters) {
      ideaFromDB = lawService.addSupporter(supporter, ideaFromDB);   //Remember: Don't just do idea.getSupporters().add(supporter);
    }
    return ideaFromDB;
  }

  @Transactional
  private LawModel addCommentsToProposal(LawModel proposal) {
  	UserModel randUser = users.get(rand.nextInt(NUM_USERS));
		auditorAware.setMockAuditor(randUser);
		CommentModel rootComment = new CommentModel("Comment on root level. I really like this idea, but needs to be improved.", null);
		for (int i = 0; i < rand.nextInt(10); i++) {
			rootComment.getUpVoters().add(randUser());
		}
		for (int j = 0; j < rand.nextInt(10); j++) {
			rootComment.getDownVoters().add(randUser());
		}
		// Must save CommentModel immediately, to prevent "TransientPropertyValueException: object references an unsaved transient instance"
    // Could also add @Cascade(org.hibernate.annotations.CascadeType.ALL) on LawModel.comments but this would always overwrite and save the full list of all comments on every save of a LawModel.
    commentRepo.save(rootComment);
		for (int i = 0; i < 5; i++) {
			auditorAware.setMockAuditor(randUser());
			CommentModel reply = new CommentModel("Reply "+i+" "+getLoremIpsum(10, 100), rootComment);
			for (int k = 0; k < rand.nextInt(10); k++) {
				reply.getUpVoters().add(randUser());
			}
			for (int l = 0; l < rand.nextInt(10); l++) {
				reply.getDownVoters().add(randUser());
			}
			commentRepo.save(reply);
      rootComment.getReplies().add(reply);
		}
		proposal.getComments().add(rootComment);
  	return lawRepo.save(proposal);
	}

  /**
   * Seed a poll that has some alternative proposals and is still in its elaboration phase,
   * ie. voting has not yet started
   * @return the poll in elaboration as it has been stored into the DB.
   */
  @Transactional
  private PollModel seedPollInElaborationPhase() {
    log.info("Seeding one poll in elaboration phase ...");
    try {
      AreaModel area = this.areas.get(0);
      String title, desc;
      UserModel createdBy;

      //===== create Poll from initial Proposal
      title = "Initial Proposal in a poll that is in elaboration "+System.currentTimeMillis();
      desc = getLoremIpsum(100, 400);
      createdBy = this.users.get(rand.nextInt(NUM_USERS));
      LawModel initialProposal = createProposal(title, desc, area, createdBy, 10);
			addCommentsToProposal(initialProposal);
      PollModel newPoll = pollService.createPoll(initialProposal);

      //===== add alternative proposals
      for (int i = 0; i < NUM_ALTERNATIVE_PROPOSALS; i++) {
        title = "Alternative Proposal" + i + " in a poll that is in elaboration"+System.currentTimeMillis();
        desc = getLoremIpsum(100, 400);
        createdBy = this.users.get(rand.nextInt(NUM_USERS));
        LawModel altProp = createProposal(title, desc, area, createdBy, 20);
				altProp = addCommentsToProposal(altProp);
        pollService.addProposalToPoll(altProp, newPoll);
      }

      fakeCreateAt(newPoll, getGlobalPropertyAsInt(LiquidoProperties.KEY.DAYS_UNTIL_VOTING_STARTS)/2);
      fakeUpdatedAt(newPoll, getGlobalPropertyAsInt(LiquidoProperties.KEY.DAYS_UNTIL_VOTING_STARTS)/2);
      log.trace("Created poll in elaboration phase: "+newPoll);
      return newPoll;
    } catch (Exception e) {
      log.error("Cannot seed Poll in elaboration: " + e);
      throw new RuntimeException("Cannot seed Poll in elaboration", e);
    }

  }

  /**
   * Seed a poll that already is in its voting phase.
   *   Will build upon a seedPollInElaborationPhase and then start the voting phase via pollService.
   */ 
  public void seedPollInVotingPhase() {
    log.info("Seeding one poll in voting phase ...");
    try {
      PollModel poll = seedPollInElaborationPhase();
      int i = 1;
      for(LawModel proposal: poll.getProposals()) {
        i++;
        proposal.setTitle("Proposal "+i+" in voting phase");
      }
      PollModel savedPoll = pollRepo.save(poll);
      fakeCreateAt(savedPoll, getGlobalPropertyAsInt(LiquidoProperties.KEY.DAYS_UNTIL_VOTING_STARTS));
      fakeUpdatedAt(savedPoll, getGlobalPropertyAsInt(LiquidoProperties.KEY.DAYS_UNTIL_VOTING_STARTS)/2);

      //===== Start the voting phase of this poll
      pollService.startVotingPhase(savedPoll);
      savedPoll.setVotingStartAt(LocalDateTime.now().minusDays(1));
      pollRepo.save(savedPoll);
    } catch (Exception e) {
      log.error("Cannot seed Poll in voting phase: " + e);
      throw new RuntimeException("Cannot seed Poll in voting phase", e);
    }
  }


  public void seedLaws() {
    log.info("Seeding laws");
    AreaModel area = this.areas.get(0);
    UserModel createdBy = this.users.get(0);
    auditorAware.setMockAuditor(createdBy);

    // These laws are not linked to a poll.      At the moment I do not need that yet...
    //PollModel poll = new PollModel();
    //pollRepo.save(poll);

    for (int i = 0; i < NUM_LAWS; i++) {
      String lawTitle = "Law " + i;
      LawModel realLaw = createProposal(lawTitle, getLoremIpsum(100,400), area, createdBy, 12);
      addSupportersToIdea(realLaw, getGlobalPropertyAsInt(LiquidoProperties.KEY.SUPPORTERS_FOR_PROPOSAL)+5);
      //TODO: reaLaw actually needs to have been part of a (finished) poll
      realLaw.setReachedQuorumAt(DoogiesUtil.daysAgo(24));
      realLaw.setStatus(LawStatus.LAW);
      upsertLawModel(realLaw, 20+i);
    }
  }

  /**
   * Will create a new law or update an existing one with matching title.
   * And will set the createdAt date to n days ago
   * @param lawModel the new law to create (or update)
   * @param ageInDays will setCreatedAt to so many days ago (measured from now)
   * @return the saved law
   */
  private LawModel upsertLawModel(LawModel lawModel, int ageInDays) {
    LawModel existingLaw = lawRepo.findByTitle(lawModel.getTitle());  // may return null!
    if (existingLaw != null) {
      log.trace("Updating existing law with id=" + existingLaw.getId());
      lawModel.setId(existingLaw.getId());
    } else {
      log.trace("Creating new law " + lawModel);
    }
    LawModel savedLaw = lawRepo.save(lawModel);
    this.lawModels.add(savedLaw);
    fakeCreateAt(savedLaw, ageInDays);
    return savedLaw;
  }


  /**
   * Fake the created at date to be n days in the past
   * @param model any domain model class derived from BaseModel
   * @param ageInDays the number of days to set the createAt field into the past.
   */
  private void fakeCreateAt(BaseModel model, int ageInDays) {
    if (ageInDays < 0) throw new IllegalArgumentException("ageInDays must be positive");
    Table tableAnnotation = model.getClass().getAnnotation(javax.persistence.Table.class);
    String tableName = tableAnnotation.name();
    String sql = "UPDATE " + tableName + " SET created_at = DATEADD('DAY', -" + ageInDays + ", NOW()) WHERE id='" + model.getId() + "'";
    log.trace(sql);
    jdbcTemplate.execute(sql);
    Date daysAgo = DoogiesUtil.daysAgo(ageInDays);
    model.setCreatedAt(daysAgo);
  }

  /**
   * Fake the updated at date to be n days in the past. Keep in mind, that updated at should not be before created at.
   * @param model any domain model class derived from BaseModel
   * @param ageInDays the number of days to set the createAt field into the past.
   */
  private void fakeUpdatedAt(BaseModel model, int ageInDays) {
    if (ageInDays < 0) throw new IllegalArgumentException("ageInDays must be positive");
    Table tableAnnotation = model.getClass().getAnnotation(javax.persistence.Table.class);
    String tableName = tableAnnotation.name();
    String sql = "UPDATE " + tableName + " SET updated_at = DATEADD('DAY', -" + ageInDays + ", NOW()) WHERE id='" + model.getId() + "'";
    log.trace(sql);
    jdbcTemplate.execute(sql);
    Date daysAgo = DoogiesUtil.daysAgo(ageInDays);
    model.setUpdatedAt(daysAgo);
  }

  public void seedVotes() {
    log.info("Seeding votes ...");

    List<PollModel> polls = pollRepo.findByStatus(PollModel.PollStatus.VOTING);
    if (polls.size() == 0) throw new RuntimeException("cannot seed Ballots. There is no poll in voting phase.");  //MAYBE: create one
    PollModel pollInVoting = polls.get(0);
    if (pollInVoting.getNumCompetingProposals() < 2) throw new RuntimeException("Cannot seed ballots. Need at least two alternative proposals in VOTING phase");

		String basePath = springEnv.getProperty("spring.data.rest.base-path");

		String pollURI = basePath+"/polls/"+pollInVoting.getId();

		Iterator<LawModel> proposals = pollInVoting.getProposals().iterator();
    List<String> voteOrder = new ArrayList<>();
    voteOrder.add(basePath+"/laws/"+proposals.next().getId());		// CastVoteRequest contains URIs not full LawModels
    voteOrder.add(basePath+"/laws/"+proposals.next().getId());

    // Now we use the original CastVoteService to get a voterToken and cast our vote.
    try {
			String voterToken = castVoteService.getVoterToken(users.get(1), pollInVoting.getArea());
			CastVoteRequest castVoteRequest = new CastVoteRequest(pollURI, voteOrder, voterToken);
			castVoteService.castVote(castVoteRequest);
		} catch (LiquidoException e) {
    	log.error("Cannot seedVotes: "+e);
		}
  }








  //-------------------------------- UTILITY methods ------------------------------



	private UserModel randUser() {
		return users.get(rand.nextInt(NUM_USERS));
	}

  private static final String loremIpsum = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. Lorem ipsum dolor sit amet, nam urna. Vitae aenean velit, voluptate velit rutrum. Elementum integer rhoncus rutrum morbi aliquam metus, morbi nulla, nec est phasellus dolor eros in libero. Volutpat dui feugiat, non magna, parturient dignissim lacus ipsum in adipiscing ut. Et quis adipiscing perferendis et, id consequat ac, dictum dui fermentum ornare rhoncus lobortis amet. Eveniet nulla sollicitudin, dolore nullam massa tortor ullamcorper mauris. Lectus ipsum lacus.\n" +
      "Vivamus placerat a sodales est, vestibulum nec cursus eros fermentum. Felis orci nunc quis suspendisse dignissim justo, sed proin metus, nunc elit ac aliquam. Sed tellus ante ipsum erat platea nulla, enim bibendum gravida condimentum, imperdiet in vitae faucibus ultrices, aenean fringilla at. Rhoncus et sint volutpat, bibendum neque arcu, posuere viverra in, imperdiet duis. Eget erat condimentum congue ipsam. Tortor nostra, adipiscing facilisis donec elit pellentesque natoque integer. Ipsum id. Aenean suspendisse et eros hymenaeos in auctor, porttitor amet id pellentesque tempor, praesent aliquam rhoncus convallis vel, tempor fusce wisi enim aliquam ut nisl, nullam dictum etiam. Nisi accumsan augue sapiente dui, pulvinar cras sapien mus quam nonummy vivamus, in vitae, sociis pede, convallis mollis id mauris. Vestibulum ac quis scelerisque magnis pede in, duis ullamcorper a ipsum ante ornare.\n" +
      "Quam amet. Risus lorem nibh consequat volutpat. Bibendum lorem, mauris sed quisque. Pellentesque augue eros nibh, iaculis maecenas facilisis amet. Nam laoreet elit litora justo, morbi in vitae nisl nulla vestibulum maecenas. Scelerisque lacinia id eget pede nunc in, id a nullam nunc velit mauris class. Duis dui ullamcorper vestibulum, turpis mi eu, arcu pellentesque sit. Arcu nibh elit. Vitae magna magna auctor, class pariatur, tortor eget amet mi pede accumsan, ut quam ut ante nibh vivamus quisque. Magna praesent tortor praesent.";

  /** @return a dummy text that can be used eg. in descriptions */
  public String getLoremIpsum(int minLength, int maxLength) {
    int endIndex = minLength + rand.nextInt(maxLength - minLength);
    if (endIndex >= loremIpsum.length()) endIndex = loremIpsum.length()-1;
    return loremIpsum.substring(0, endIndex);
  }


}