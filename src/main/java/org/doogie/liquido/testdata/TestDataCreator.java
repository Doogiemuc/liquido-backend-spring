package org.doogie.liquido.testdata;

import lombok.NonNull;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.model.*;
import org.doogie.liquido.rest.dto.CastVoteRequest;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.*;
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
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Table;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
//TODO: @Profile("dev")    // run test data creator only during development
@Order(Ordered.HIGHEST_PRECEDENCE)   // seed DB first, then run the other CommandLineRunners
public class TestDataCreator implements CommandLineRunner {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  public int NUM_USERS = 20;
  public int NUM_AREAS = 10;
  public int NUM_IDEAS = 111;
  public int NUM_PROPOSALS = 50;

  public int NUM_ALTERNATIVE_PROPOSALS = 5;   // proposals in poll

  public int NUM_LAWS = 2;

	//@Value("${spring.data.rest.base-path}")   // value from application.properties file
	//String restBasePath;

  @Autowired
  UserRepo userRepo;
  Map<String, UserModel> users = new HashMap<>();  // users by their email adress

  @Autowired
  AreaRepo areaRepo;
  List<AreaModel> areas = new ArrayList<>();
  Map<String, AreaModel> areaMap = new HashMap<>();

  @Autowired
  LawRepo lawRepo;
  List<LawModel> lawModels = new ArrayList<>();    // ideas, proposals and laws

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
	ProxyService proxyService;

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
    boolean seedDB = springEnv.acceptsProfiles("dev", "test") || "true".equalsIgnoreCase(springEnv.getProperty("seedDB"));
    for(String arg : args) {
      if ("--seedDB".equalsIgnoreCase(arg)) { seedDB = true; }
    }
    if (seedDB) {
			log.info("===== START TestDataCreator");
      log.debug("Populate test DB: "+ jdbcTemplate.getDataSource().toString());
      // The order of these methods is very important here!
      seedUsers(NUM_USERS, TestFixtures.MAIL_PREFIX, TestFixtures.TESTUSER_PASSWORD);
      auditorAware.setMockAuditor(this.users.get(TestFixtures.USER1_EMAIL));   // Simulate that user is logged in.  This user will be set as @createdAt
      //seedGlobalProperties();
      seedAreas();
      seedProxies(TestFixtures.delegations);
      seedIdeas();
      seedProposals();
      seedPollInElaborationPhase(NUM_ALTERNATIVE_PROPOSALS);
      seedPollInVotingPhase(NUM_ALTERNATIVE_PROPOSALS);
			seedPollFinished(NUM_ALTERNATIVE_PROPOSALS);
      seedLaws();
      seedVotes();

      auditorAware.setMockAuditor(null);

      log.info("===== TestDataCreator FINISHED");
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

  public Map<String, UserModel> seedUsers(int numUsers, String mailPrefix, String password) {
    log.info("Seeding Users ... this will bring up some 'Cannot getCurrentAuditor' WARNings that you can ignore.");
    this.users = new HashMap<>();

    /** Hashing a password takes time. So all test users have the same password to speed TestDataCreator up alot. */
	  PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();   // Springs default password encoder uses "bcrypt"
		String hashedPassword = passwordEncoder.encode(password);           // Password encoding takes a second! And it should take a second, for security reasons!

    for (int i = 0; i < numUsers; i++) {
      String email = mailPrefix + (i+1) + "@liquido.de";    // Remember that DB IDs start at 1. Testuser1 has ID=1 in DB. And there is no testuser0
      UserModel newUser = new UserModel(email, hashedPassword);

      UserProfileModel profile = new UserProfileModel();
      profile.setName("Test User" + (i+1));
      profile.setPicture("/static/img/photos/"+((i%3)+1)+".png");
      profile.setWebsite("http://www.liquido.de");
      profile.setPhonenumber(randomDigits(10));
      if (i==0) profile.setPhonenumber("1234567890");  // make sure that there is one user with that phonenumber
      newUser.setProfile(profile);

      UserModel existingUser = userRepo.findByEmail(email);
      if (existingUser != null) {
        log.debug("Updating existing user with id=" + existingUser.getId());
        newUser.setId(existingUser.getId());
      } else {
        log.debug("Creating new user " + newUser);
      }

      UserModel savedUser = userRepo.save(newUser);
      this.users.put(savedUser.getEmail(), savedUser);
      if (i==0) auditorAware.setMockAuditor(savedUser);   // prevent some warnings
    }
    return this.users;
  }

  /**
   * Create some areas with unique titles. All created by user0
   */
  private void seedAreas() {
    log.info("Seeding Areas ...");
    this.areas = new ArrayList<>();

    UserModel createdBy = this.users.get(TestFixtures.USER1_EMAIL);

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
      this.areaMap.put(savedArea.getTitle(), savedArea);
    }
  }


	/**
	 * seed delegations from email[0] to email[1] so that email[1] becomes a proxy
	 * @param delegations list of email[0] -> email[1] pairs where the second element will become the proxy
	 */
  private void seedProxies(List<String[]> delegations) {
		log.info("Seeding Proxies ...");

		AreaModel area = areaMap.get(TestFixtures.AREA_FOR_DELEGATIONS);
    for(String[] delegation: delegations) {
      UserModel fromUser = users.get(delegation[0]);
      UserModel toProxy  = users.get(delegation[1]);
			log.debug("Assign Proxy fromUser.id="+fromUser.getId()+ " toProxy.id="+toProxy.getId());
      try {
				//String proxyVoterToken = castVoteService.createVoterToken(toProxy, area, toProxy.getPasswordHash());
        //proxyService.becomePublicProxy(toProxy, area, proxyVoterToken);
				String userVoterToken = castVoteService.createVoterToken(fromUser, area, fromUser.getPasswordHash());
				proxyService.assignProxy(area, fromUser, toProxy, userVoterToken);
			} catch (LiquidoException e) {
        log.error("Cannot seedProxies: error Assign Proxy fromUser.id="+fromUser.getId()+ " toProxy.id="+toProxy.getId()+": "+e);
      }
    }

  }

  private void seedIdeas() {
    log.info("Seeding Ideas ...");
    for (int i = 0; i < NUM_IDEAS; i++) {
      String ideaTitle = "Idea " + i + " that suggest that we definitely need a longer title for ideas";
      StringBuffer ideaDescr = new StringBuffer();
      ideaDescr.append(randString(8));    // prepend with some random chars to test sorting
      ideaDescr.append(" ");
      ideaDescr.append(getLoremIpsum(0,400));

      UserModel createdBy = this.randUser();
      auditorAware.setMockAuditor(createdBy);
      AreaModel area = this.areas.get(i % this.areas.size());
      LawModel newIdea = new LawModel(ideaTitle, ideaDescr.toString(), area, createdBy);
      lawRepo.save(newIdea);

	    // add some supporters, but not enough to become a proposal
      int numSupporters = rand.nextInt(props.getInt(LiquidoProperties.KEY.SUPPORTERS_FOR_PROPOSAL)-1);
      //log.debug("adding "+numSupporters+" supporters to idea "+newIdea);
      addSupportersToIdea(newIdea, numSupporters);

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
    proposal = addSupportersToIdea(proposal, props.getInt(LiquidoProperties.KEY.SUPPORTERS_FOR_PROPOSAL));
    //proposal.setStatus(LawStatus.PROPOSAL);
    //LawModel savedProposal = lawRepo.save(proposal);
    fakeCreateAt(proposal,  ageInDays);
    fakeUpdatedAt(proposal, ageInDays > 1 ? ageInDays - 1 : 0);
    this.lawModels.add(proposal);
    return proposal;
  }

  private LawModel createRandomProposal(String title) {
    StringBuffer description = new StringBuffer();
    description.append(randString(8));    // prepend with some random chars to test sorting
    description.append(" ");
    description.append(getLoremIpsum(0,400));
    UserModel createdBy = this.randUser();
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
			UserModel createdBy = this.users.get(TestFixtures.USER1_EMAIL);
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
  private LawModel addSupportersToIdea(@NonNull LawModel idea, int num) {
    if (num >= users.size()-1) throw new RuntimeException("Cannot at "+num+" supporters to idea. There are not enough users.");
    // https://stackoverflow.com/questions/8378752/pick-multiple-random-elements-from-a-list-in-java
    LinkedList<UserModel> otherUsers = new LinkedList<>();
    for (UserModel user: this.users.values()) {
      if (!user.equals(idea.getCreatedBy()))   otherUsers.add(user);
    }
    Collections.shuffle(otherUsers);
    LawModel ideaFromDB = lawRepo.findById(idea.getId())  // Get JPA "attached" entity
     .orElseThrow(()->new RuntimeException("Cannot find idea with id="+idea.getId()));

    List<UserModel> newSupporters = otherUsers.subList(0, num);
    for (UserModel supporter: newSupporters) {
      ideaFromDB = lawService.addSupporter(supporter, ideaFromDB);   //Remember: Don't just do idea.getSupporters().add(supporter);
    }
    return ideaFromDB;
  }

  @Transactional
  private LawModel addCommentsToProposal(LawModel proposal) {
  	UserModel randUser = this.randUser();
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
   * ie. voting has not yet started. Also add some comments to each proposal in that poll.
   * @return the poll in elaboration as it has been stored into the DB.
   */
  @Transactional
  private PollModel seedPollInElaborationPhase(int numProposals) {
    log.info("Seeding one poll in elaboration phase ...");
    if (numProposals > this.users.size())
    	throw new RuntimeException("Cannot seedPollInElaborationPhase. Need at least "+NUM_ALTERNATIVE_PROPOSALS+" distinct users");

    try {
      AreaModel area = this.areas.get(0);
      String title, desc;
      UserModel createdBy;

      //===== builder Poll from initial Proposal
      title = "Initial Proposal in a poll that is in elaboration "+System.currentTimeMillis();
      desc = getLoremIpsum(100, 400);
      createdBy = getUser(0);
      LawModel initialProposal = createProposal(title, desc, area, createdBy, 10);
			initialProposal = addCommentsToProposal(initialProposal);
      PollModel newPoll = pollService.createPoll(initialProposal);

      //===== add alternative proposals
      for (int i = 1; i < numProposals; i++) {
        title = "Alternative Proposal" + i + " in a poll that is in elaboration"+System.currentTimeMillis();
        desc = getLoremIpsum(100, 400);
        createdBy = getUser(i);
        LawModel altProp = createProposal(title, desc, area, createdBy, 20);
				altProp = addCommentsToProposal(altProp);
        newPoll = pollService.addProposalToPoll(altProp, newPoll);
      }

      fakeCreateAt(newPoll, props.getInt(LiquidoProperties.KEY.DAYS_UNTIL_VOTING_STARTS)/2);
      fakeUpdatedAt(newPoll, props.getInt(LiquidoProperties.KEY.DAYS_UNTIL_VOTING_STARTS)/2);
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
  public PollModel seedPollInVotingPhase(int numProposals) {
    log.info("Seeding one poll in voting phase ...");
    try {
      PollModel poll = seedPollInElaborationPhase(numProposals);
      int i = 1;
      for(LawModel proposal: poll.getProposals()) {
        proposal.setTitle("Proposal "+i+" in voting phase "+System.currentTimeMillis());
        i++;
      }
      PollModel savedPoll = pollRepo.save(poll);
      fakeCreateAt(savedPoll, props.getInt(LiquidoProperties.KEY.DAYS_UNTIL_VOTING_STARTS)+1);
      fakeUpdatedAt(savedPoll, props.getInt(LiquidoProperties.KEY.DAYS_UNTIL_VOTING_STARTS)/2);

      //===== Start the voting phase of this poll
      pollService.startVotingPhase(savedPoll);
      LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
      savedPoll.setVotingStartAt(yesterday);
			savedPoll.setVotingEndAt(yesterday.truncatedTo(ChronoUnit.DAYS).plusDays(props.getInt(LiquidoProperties.KEY.DURATION_OF_VOTING_PHASE)));     //voting ends in n days at midnight
      PollModel finalPoll = pollRepo.save(savedPoll);
      return finalPoll;
    } catch (Exception e) {
      log.error("Cannot seed Poll in voting phase: " + e);
      throw new RuntimeException("Cannot seed Poll in voting phase", e);
    }
  }

  public void seedPollFinished(int numProposals) {
		log.info("Seeding one finished poll  ...");
		try {
			PollModel poll = seedPollInVotingPhase(numProposals);

			//---- fake some dates to be in the past
			int daysFinished = 5;  // poll was finished 5 days ago
			int daysVotingStarts = props.getInt(LiquidoProperties.KEY.DAYS_UNTIL_VOTING_STARTS);
			int durationVotingPhase = props.getInt(LiquidoProperties.KEY.DURATION_OF_VOTING_PHASE);
			LocalDateTime votingStartAt = LocalDateTime.now().minusDays(durationVotingPhase+daysFinished);
			poll.setVotingStartAt(votingStartAt);
			LocalDateTime votingEndAt = LocalDateTime.now().minusDays(daysFinished).truncatedTo(ChronoUnit.DAYS);  // voting ends at midnight
			poll.setVotingEndAt(votingEndAt);

			pollRepo.save(poll);
			fakeCreateAt(poll, daysVotingStarts+durationVotingPhase+daysFinished);
			fakeUpdatedAt(poll, 1);

			//----- end voting Phase
			LawModel winner = pollService.finishVotingPhase(poll);

			log.info("Created finished poll (id="+poll.getId()+" with winning proposal.id="+winner.getId());
		} catch (Exception e) {
			log.error("Cannot seed finished poll", e);
			throw new RuntimeException("Cannot seed finished poll", e);
		}
	}

  public void seedLaws() {
    log.info("Seeding laws");
    AreaModel area = this.areas.get(0);
    UserModel createdBy = this.users.get(TestFixtures.USER1_EMAIL);
    auditorAware.setMockAuditor(createdBy);

    // These laws are not linked to a poll.      At the moment I do not need that yet...
    //PollModel poll = new PollModel();
    //pollRepo.save(poll);

    for (int i = 0; i < NUM_LAWS; i++) {
      String lawTitle = "Law " + i;
      LawModel realLaw = createProposal(lawTitle, getLoremIpsum(100,400), area, createdBy, 12);
      addSupportersToIdea(realLaw, props.getInt(LiquidoProperties.KEY.SUPPORTERS_FOR_PROPOSAL)+5);
      //TODO: reaLaw actually needs to have been part of a (finished) poll with alternative proposals
			//realLaw.setPoll(poll);
      realLaw.setReachedQuorumAt(DoogiesUtil.daysAgo(24));
      realLaw.setStatus(LawStatus.LAW);
      upsertLawModel(realLaw, 20+i);
    }
  }

  /**
   * Will builder a new law or update an existing one with matching title.
   * And will set the createdAt date to n days ago
   * @param lawModel the new law to builder (or update)
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
    //log.trace(sql);
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
    //log.trace(sql);
    jdbcTemplate.execute(sql);
    Date daysAgo = DoogiesUtil.daysAgo(ageInDays);
    model.setUpdatedAt(daysAgo);
  }

  public void seedVotes() {
    log.info("Seeding votes ...");

    List<PollModel> polls = pollRepo.findByStatus(PollModel.PollStatus.VOTING);
    if (polls.size() == 0) throw new RuntimeException("cannot seed votes. There is no poll in voting phase.");  //MAYBE: builder one
    PollModel pollInVoting = polls.get(0);
    if (pollInVoting.getNumCompetingProposals() < 2) throw new RuntimeException("Cannot seed votes. Poll in voting must have at least two proposals.");

    log.debug(pollInVoting.toString());

		String basePath = springEnv.getProperty("spring.data.rest.base-path");
    UserModel voter = users.get(TestFixtures.USER1_EMAIL);
		String pollURI = basePath+"/polls/"+pollInVoting.getId();
		Iterator<LawModel> proposals = pollInVoting.getProposals().iterator();
    List<String> voteOrder = new ArrayList<>();
    voteOrder.add(basePath+"/laws/"+proposals.next().getId());		// CastVoteRequest contains URIs not full LawModels
    voteOrder.add(basePath+"/laws/"+proposals.next().getId());

    // Now we use the original CastVoteService to get a voterToken and cast our vote.
    try {
    	auditorAware.setMockAuditor(voter);
			String voterToken = castVoteService.createVoterToken(voter, pollInVoting.getArea(), voter.getPasswordHash());
			auditorAware.setMockAuditor(null);
			CastVoteRequest castVoteRequest = new CastVoteRequest(pollURI, voteOrder, voterToken);
			castVoteService.castVote(castVoteRequest);
		} catch (LiquidoException e) {
    	log.error("Cannot seedVotes: "+e);
		}
  }








  //-------------------------------- UTILITY methods ------------------------------

	/**
	 * get one random UserModel
	 * @return a random user
	 */
	public UserModel randUser() {
		Object[] entries = users.values().toArray();
		return (UserModel)entries[rand.nextInt(entries.length)];
	}

	public UserModel getUser(int i) {
		Object[] entries = users.values().toArray();
		return (UserModel)entries[i];
	}

	public AreaModel getArea(int i) {
		return areas.get(i);
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


	private static final char[] EASY_CHARS = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

	/**
	 * Simply generate some random characters
	 * @param len number of chars to generate
	 * @return a String of length len with "easy" random characters and numbers
	 */
	public String randString(int len) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < len; i++) {
			buf.append(EASY_CHARS[rand.nextInt(EASY_CHARS.length)]);
		}
		return buf.toString();
	}

	public String randomDigits(long len) {          // Example: len = 3
		long max = (long) Math.pow(len, 10);          // 10^3  = 1000
		long min = (long) Math.pow(len-1, 10);        // 10^2  =  100
		long number = min + (rand.nextLong() % (max-min));  //  100 + [0...899]  = [100...999]
		return String.valueOf(number);
	}
}