package org.doogie.liquido.testdata;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.graphql.TeamsGraphQL;
import org.doogie.liquido.model.*;
import org.doogie.liquido.rest.dto.CastVoteRequest;
import org.doogie.liquido.rest.dto.CastVoteResponse;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.*;
import org.doogie.liquido.util.DoogiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.doogie.liquido.model.LawModel.LawStatus;

/**
 * <h1>TestDataCreator</h1>
 *
 * Every test needs data. This testdata is extremely important. Here we create it.
 *
 * <h3>Fixed or random testdata</h3>
 * On the one hand the application of course must be able to handle arbitrary user data.
 * But on the other hand, especially when debugging, then determenistic and  repeatable test conditions are indispensible.
 *
 * For data that need to be fixed we use TestFixtures. For the rest we can create random data (e.g. for loremIpsum text blocks)
 *
 *
 * <h3>How to use TestDataCreator</h3>
 *
 * TestDataCreator can be un in two modes:
 *
 * <h4>Create sample data from scratch with JPA</h4>
 *
 * In application.properties  set  spring.jpa.hibernate.ddl-auto=create   to let Spring-JPA init the DB schema.
 * Then run this app with createSampleData=true
 * TestDataCreator will create test data from scratch. It will call plain spring-data-jpa methods
 * and use service methods very useful.   This whole creation takes around 1-2 minutes.
 * The resulting schema and testdata will be exported into an SQL script  sample-DB.sql
 *
 * <h4>Load schema and testdata from SQL script</h4>
 *
 * In application.properties  set  spring.jpa.hibernate.ddl-auto=none (Then a data.sql is not loaded!)
 * And set the environment variable loadSampleDB=true
 * Then sample-data.sql is loaded. This is quick!
 *
 * You can create a sample-data.sql from the embedded H2 console with the SQL command
 * <pre>SCRIPT TO 'sample-DB.sql'</pre>
 *
 *
 *
 * This is executed right after SpringApplication.run(...)
 * See http://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#boot-features-command-line-runner
 *
 * Other possibilities for initializing a DB with Spring and Hibernate:
 * https://www.baeldung.com/spring-boot-data-sql-and-schema-sql    <=  @Sql annotation can be used on test classes or methods to execute SQL scripts.
 * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#core.repository-populators
 * https://docs.spring.io/spring-boot/docs/current/reference/html/howto-database-initialization.html
 * http://www.sureshpw.com/2014/05/importing-json-with-references-into.html
 * http://www.generatedata.com/
 * https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#testcontext-ctx-management-env-profiles
 * https://stackoverflow.com/questions/8523423/reset-embedded-h2-database-periodically    -- how to drop H2 DB completely
 *
 * I already tried to refoctor this TestDataCreator into its own module. But this is not possible,
 * because this class depends so closely on pretty much every liquido service. And this is a good thing.
 * We want to test everything here. => So TestDataCreator will stay an internal part of the LIQUIDO backend project.
 */

@Slf4j
@Component
@Profile({"dev", "test"})    						// run test data creator only during development or when running tests!
@Order(100)   		                      // seed DB first, then run the other CommandLineRunners
public class TestDataCreator implements CommandLineRunner {

  static final String RECREATE_TEST_DATA     	= "recreateTestData";
  static final String LOAD_TEST_DATA         	= "loadTestData";
	static final String TEST_DATA_FILENAME   		= "sampleDB-H2.sql";
	static final String TEST_DATA_PATH       		= "src/main/resources/";

	// TestDataCreator pretty much depends on any Model, Repo and Service that we have. Which proves that we have a nice code coverage :-)

  @Autowired
  UserRepo userRepo;

  @Autowired
	TeamRepo teamRepo;

  @Autowired
  AreaRepo areaRepo;

  @Autowired
  LawRepo lawRepo;

  @Autowired
  LawService lawService;

  @Autowired
	CommentRepo commentRepo;

  @Autowired
  BallotRepo ballotRepo;

  @Autowired
  PollRepo pollRepo;

  @Autowired
	RightToVoteRepo rightToVoteRepo;

  @Autowired
	TeamsGraphQL teamService;

  @Autowired
  PollService pollService;

  @Autowired
  CastVoteService castVoteService;

  @Autowired
	DelegationRepo delegationRepo;

  @Autowired
	ProxyService proxyService;

  @Autowired
	LiquidoProperties prop;

  @Autowired
	TestDataUtils util;

  @Autowired
  JdbcTemplate jdbcTemplate;

  @Autowired
  LiquidoAuditorAware auditorAware;

	// value from application.properties file
	@Value("${spring.data.rest.base-path}")
	String basePath;

	@Autowired
	LiquidoProperties liquidoProps;

	@Autowired
  Environment springEnv;

	@Autowired
	ApplicationContext appContext;

	@Autowired
	TestDataUtils utils;

	/* Cache default area. Will be initialized in seedAreas() */
	private AreaModel defaultArea;


	// I thought about this question for a long time:
	// Should TestDataCreator be completely deterministic. Or is it ok if it creates some random data.
	// One the one hand the system MUST be stable enough to handle random data.
	// But on the other hand, when debugging a very complex deeply hidden issue, then 100% repeatable test conditions are a must.
  // => Currently TestDataCreator has some random titles and descriptions.
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
  public void run(String... args) throws LiquidoException {
		boolean seedDB = "true".equalsIgnoreCase(springEnv.getProperty(RECREATE_TEST_DATA));
    for(String arg : args) {
      if (("--"+ RECREATE_TEST_DATA).equalsIgnoreCase(arg)) { seedDB = true; }
    }
    if (seedDB) {
        log.info("===== START TestDataCreator");
        // Sanity check: Is there a schema with tables?
        try {
            List<UserModel> users = jdbcTemplate.queryForList("SELECT * FROM USERS LIMIT 10", UserModel.class);
        } catch (Exception e) {
            log.error("Cannot find table USERS! Did you create a DB schema at all?");
            throw e;
        }

        log.debug("Create test data from scratch via spring-data for DB: "+ jdbcTemplate.getDataSource().toString());
      // The order of these methods is very important here!
      seedUsers(TestFixtures.NUM_USERS, TestFixtures.MAIL_PREFIX);
      util.seedAdminUser();
      auditorAware.setMockAuditor(util.user(TestFixtures.USER1_EMAIL));   // Simulate that user is logged in.  This user will be set as @createdAt
      seedTeams();
			seedAreas();

      seedIdeas();
      seedProposals();
			seedProxies(TestFixtures.delegations);
			seedPollInElaborationPhase(this.defaultArea, TestFixtures.NUM_ALTERNATIVE_PROPOSALS);
			seedPollInVotingPhase(this.defaultArea, TestFixtures.NUM_ALTERNATIVE_PROPOSALS);						      // seed one poll in voting
			PollModel poll = seedPollInVotingPhase(this.defaultArea, TestFixtures.NUM_ALTERNATIVE_PROPOSALS);
			seedVotes(poll, TestFixtures.NUM_VOTES);
			seedPollFinished(this.defaultArea, TestFixtures.NUM_ALTERNATIVE_PROPOSALS);
      seedLaws();
      auditorAware.setMockAuditor(null);

			log.info("===== TestDataCreator: Store sample data in file: "+ TEST_DATA_PATH + TEST_DATA_FILENAME);
			jdbcTemplate.execute("SCRIPT TO '"+ TEST_DATA_PATH + TEST_DATA_FILENAME +"'");				//TODO: export schema only with  SCRIPT NODATA TO ...   and export MySQL compatible script!!!
			adjustDbInitializationScript();
			log.info("===== TestDataCreator: Sample data stored successfully in file: "+ TEST_DATA_PATH + TEST_DATA_FILENAME);
    }

    boolean loadSampleDataFromSqlScript = Boolean.parseBoolean(springEnv.getProperty(LOAD_TEST_DATA));
    if (loadSampleDataFromSqlScript) {
			try {
				log.info("===== TestDataCreator: Loading schema and sample data from "+ TEST_DATA_FILENAME);
				Resource resource = appContext.getResource(ResourceLoader.CLASSPATH_URL_PREFIX + TEST_DATA_FILENAME);
				ScriptUtils.executeSqlScript(jdbcTemplate.getDataSource().getConnection(), resource);

				// Fill userMap as cache
				util.reloadUsersCache();
				log.info("Loaded {} teams", teamRepo.count());
				log.info("Loaded {} users", userRepo.count());
				log.info("Loaded {} areas", areaRepo.count());
				log.info("Loaded {} ideas, proposals and laws.", lawRepo.count());
				log.info("Loaded {} polls", pollRepo.count());
				log.info("Loaded {} delegations", delegationRepo.count());
				log.info("Loaded {} checksums", rightToVoteRepo.count());
				log.info("Loaded {} comments", commentRepo.count());

				log.info("===== TestDataCreator: Successfully loaded schema and sample data from "+ TEST_DATA_FILENAME +" => DONE");
			} catch (SQLException e) {
				String errMsg = "ERROR: Cannot load schema and sample data from "+ TEST_DATA_FILENAME;
				log.error(errMsg);
				throw new RuntimeException(errMsg, e);
			}
		}

    // log proxy structure in default area if tracing is enabled
    if ((seedDB || loadSampleDataFromSqlScript) && log.isTraceEnabled()) {
			AreaModel area = this.defaultArea;
			Optional<UserModel> topProxyOpt = userRepo.findByEmail(TestFixtures.USER1_EMAIL);      // user1 is the topmost proxy in TestFixtures.java
			if (topProxyOpt.isPresent()) {
				UserModel topProxy = topProxyOpt.get();
				log.debug("====== TestDataCreator: Proxy tree =====");
				utils.printProxyTree(area, topProxy);

				log.debug("====== TestDataCreator: Tree of delegations =====");
				utils.printDelegationTree(area, topProxy);

				try {
					String voterToken = castVoteService.createVoterTokenAndStoreRightToVote(topProxy, area, TestFixtures.USER_TOKEN_SECRET, false);
					RightToVoteModel rightToVote = castVoteService.isVoterTokenValid(voterToken);
					log.debug("====== TestDataCreator: RightToVotes =====");
					utils.printRightToVoteTree(rightToVote);
				} catch (LiquidoException e) {
					log.error("Cannot get rightToVote of " + topProxy + ": " + e.getMessage());
				}
			}

		}

  }

	/**
	 * We need to "massage" the DB generation script a bit:
	 *
	 * (1) We prepend the command <pre>DROP ALL OBJECTS</pre> so that the database is cleaned completely!
	 *
	 * (2) And we need a crude hack for nasty race condition:
	 *
	 * My nice SQL script contains the schema (CREATE TABLE ...) and data (INSERT INTO...) That way I can
	 * very quickly init a DB from scratch.  But TestDataCreator runs after my SpringApp has started.
	 * Our Quartz scheduler is started earlier. It can be configured to create or not create its own
	 * schema. But when I tell it to not create its own schema TestDataCreator runs too late to
	 * create the schema for Quartz.
	 * So I let Quartz create its own stuff and remove any Quarts related lines from my DB script
	 *
	 * The alternative would be do copy the Quartz lines into schema.sql and data.sql
	 * Then I could also recreate Quartz sample data such as jobs.
	 */
	private void adjustDbInitializationScript() {
		log.trace("removeQartzSchema from SQL script: start");
		try {
			File sqlScript = new File(TEST_DATA_PATH + TEST_DATA_FILENAME);
			BufferedReader reader = new BufferedReader(new FileReader(sqlScript));
			List<String> lines = new ArrayList<>();
			String currentLine;
			Boolean removeBlock = false;
			while ((currentLine = reader.readLine()) != null) {
				currentLine = currentLine.trim();
				//log.trace("Checking line "+currentLine);
				if (currentLine.matches("(ALTER|CREATE).*TABLE \"PUBLIC\"\\.\"QRTZ.*\\(")) removeBlock = true;
				if (currentLine.matches("INSERT INTO \"PUBLIC\"\\.\"QRTZ.*VALUES")) removeBlock = true;
				if (removeBlock && currentLine.matches(".*\\); *")) {
					//log.trace("Remove end of block      );");
					removeBlock = false;
					continue;
				}
				if (removeBlock) {
					//log.trace("Removing line from block "+currentLine);
					continue;
				}
				if (currentLine.matches("(ALTER|CREATE).*TABLE \"PUBLIC\"\\.\"QRTZ.*;")) {
					//log.trace("Removing single line:    "+currentLine);
					continue;
				}
				lines.add(currentLine);
			}
			reader.close();

			BufferedWriter writer = new BufferedWriter(new FileWriter(sqlScript));
			writer.write("-- LIQUIDO  H2 Database initialization script\n");
			writer.write("-- This script contains the SCHEMA and TEST DATA\n");
			writer.write("-- BE CAREFULL: This script completely DROPs and RE-CREATES the DB !!!!!\n");
			writer.write("DROP ALL OBJECTS;\n");
			for(String line : lines) {
				writer.write(line);
				writer.newLine();		//  + System.getProperty("line.separator")
			}
			writer.close();
			log.trace("removeQuartzSchema from SQL script successfull: "+sqlScript.getAbsolutePath());

		} catch (Exception e) {
			log.error("Could not remove Quarts statements from Schema: "+e.getMessage());
  		throw new RuntimeException("Could not remove Quarts statements from Schema: "+e.getMessage(), e);
		}
	}

	/** Seed teams and their admin users. */
	public void seedTeams() throws LiquidoException {
		log.info("Seeding Teams ...");
		for (int i = 0; i < TestFixtures.NUM_TEAMS; i++) {
			String teamName    = TestFixtures.TEAM_NAMES.get(i);
			String adminName   = "Admin " + teamName;
			String adminEmail  = TestFixtures.TEAM_ADMIN_EMAILS.get(i);
			String adminMobilephone = TestFixtures.MOBILEPHONE_PREFIX+"555"+(i+1);
			teamService.createNewTeam(teamName, adminName, adminEmail, adminMobilephone);
		}
	}

	/**
	 * Seed some users. This can be called multiple times! Uses will be stored in this.userMap
	 * @param numUsers
	 * @param mailPrefix
	 */
  public void seedUsers(long numUsers, String mailPrefix) {
    log.info("Seeding Users ... this may bring up a 'Cannot getCurrentAuditor' warning, that you can ignore.");
		long countUsers = userRepo.count();
    for (int i = 0; i < numUsers; i++) {
      String email 				= mailPrefix + (i+1) + "@liquido.de";
			String name  			 	= (i == 0) ? "Test User" + (i+1) : TestFixtures.USER1_NAME;           // user1 has a special fixed name.
			String mobilephone 	= TestFixtures.MOBILEPHONE_PREFIX+(countUsers+i+1);
			String website     	= "http://www.liquido.de";
			String picture     	= TestFixtures.AVATAR_PREFIX+((i%16)+1)+".png";
      UserModel newUser  	= new UserModel(email, name, mobilephone, website, picture);
      //TODO: newUser.setAuthyId();
			UserModel savedUser = util.upsert(newUser);
			if (i==0) auditorAware.setMockAuditor(savedUser);   // This prevents some warnings
    }
    util.reloadUsersCache();
  }

	/**
   * Create some areas with unique titles. All created by user0
	 * The first one is the default area.
   */
  private void seedAreas() {
    log.info("Seeding Areas ...");
    UserModel createdBy = util.user(TestFixtures.USER1_EMAIL);

    // Seed default area
		this.defaultArea = new AreaModel(liquidoProps.defaultAreaTitle, "Default Area", createdBy);
		defaultArea = areaRepo.save(defaultArea);

		// Seed more areas
    for (int i = 0; i < TestFixtures.NUM_AREAS; i++) {
      String areaTitle =  "Area " + i;
      AreaModel newArea = new AreaModel(areaTitle, "Nice description for test area #"+i, createdBy);

      Optional<AreaModel> existingArea = areaRepo.findByTitle(areaTitle);
      if (existingArea.isPresent()) {
        log.debug("Updating existing area with id=" + existingArea.get().getId());
        newArea.setId(existingArea.get().getId());
      } else {
        log.debug("Creating new area " + newArea);
      }
      AreaModel savedArea = areaRepo.save(newArea);
    }
  }


	/**
	 * Seed delegations
	 * @param delegations list of email[0] -> email[1] pairs where the second element will become the proxy
	 */
  private void seedProxies(List<String[]> delegations) {
		log.info("Seeding Proxies ...");

		AreaModel area = this.defaultArea;
    for(String[] delegationData: delegations) {
      UserModel fromUser   = util.user(delegationData[0]);
      UserModel toProxy    = util.user(delegationData[1]);
      try {
	      if (TestFixtures.shouldBePublicProxy(area, toProxy)) {
					castVoteService.createVoterTokenAndStoreRightToVote(toProxy, area, TestFixtures.USER_TOKEN_SECRET, true);
					log.debug("publicProxyChecksum = ", proxyService.getRightToVoteOfPublicProxy(area, toProxy));
				}

				String userVoterToken = castVoteService.createVoterTokenAndStoreRightToVote(fromUser, area, TestFixtures.USER_TOKEN_SECRET, TestFixtures.shouldBePublicProxy(area, fromUser));
	      DelegationModel delegation = proxyService.assignProxy(area, fromUser, toProxy, userVoterToken);
      } catch (LiquidoException e) {
        log.error("Cannot seedProxies: error Assign Proxy fromUser.id="+fromUser.getId()+ " toProxy.id="+toProxy.getId()+": "+e);
      }
    }
    UserModel topProxy = util.user(TestFixtures.TOP_PROXY_EMAIL);
    utils.printProxyTree(area, topProxy);
  }

  private void seedIdeas() {
    log.info("Seeding Ideas ...");
    for (int i = 0; i < TestFixtures.NUM_IDEAS; i++) {
      String ideaTitle = "Idea " + i + " that suggest that we definitely need a longer title for ideas";
      if (i == 0) ideaTitle = TestFixtures.IDEA_0_TITLE;   // special fixed title for first idea
      StringBuffer ideaDescr = new StringBuffer();
      ideaDescr.append(DoogiesUtil.randToken(8));    // prepend with some random chars to test sorting
      ideaDescr.append(" ");
      ideaDescr.append(util.getLoremIpsum(0,400));

      UserModel createdBy = util.randUser();
      auditorAware.setMockAuditor(createdBy);
      AreaModel area = this.defaultArea;
      LawModel newIdea = new LawModel(ideaTitle, ideaDescr.toString(), area);
      lawRepo.save(newIdea);

	    // add some supporters, but not enough to become a proposal
      int numSupporters = rand.nextInt(prop.supportersForProposal-1);
      //log.debug("adding "+numSupporters+" supporters to idea "+newIdea);
      addSupportersToIdea(newIdea, numSupporters);

      //LawModel savedIdea = lawRepo.save(newIdea);
      util.fakeCreateAt(newIdea, i+1);
      util.fakeUpdatedAt(newIdea, i);
    }
  }



  private LawModel createProposal(String title, String description, AreaModel area, UserModel createdBy, int ageInDays, int reachedQuorumDaysAgo) {
  	if (ageInDays < reachedQuorumDaysAgo) throw new RuntimeException("Proposal cannot reach its quorum before it was created.");
    auditorAware.setMockAuditor(createdBy);
    LawModel proposal = new LawModel(title, description, area);
		lawRepo.save(proposal);

    proposal = addSupportersToIdea(proposal, prop.supportersForProposal);
		LocalDateTime reachQuorumAt = LocalDateTime.now().minusDays(reachedQuorumDaysAgo);
    proposal.setReachedQuorumAt(reachQuorumAt);			// fake reachQuorumAt date to be in the past

		lawRepo.save(proposal);

    util.fakeCreateAt(proposal,  ageInDays);
    util.fakeUpdatedAt(proposal, ageInDays > 1 ? ageInDays - 1 : 0);
    return proposal;
  }

  private LawModel createRandomProposal(String title) {
    StringBuffer description = new StringBuffer();
    description.append(DoogiesUtil.randToken(8));    // prepend with some random chars to test sorting
    description.append(" ");
    description.append(util.getLoremIpsum(0,400));
    UserModel createdBy = this.util.randUser();
    AreaModel area = this.defaultArea;
    int ageInDays = rand.nextInt(10);
    int reachQuorumDaysAgo = (int)(ageInDays*rand.nextFloat());
    LawModel proposal = createProposal(title, description.toString(), area, createdBy, ageInDays, reachQuorumDaysAgo);
    return proposal;

  }

  /** seed proposals, ie. ideas that have already reached their quorum */
  private void seedProposals() {
    log.info("Seeding Proposals ...");
    for (int i = 0; i < TestFixtures.NUM_PROPOSALS; i++) {
      String title = "Proposal " + i + " that reached its quorum";
      LawModel proposal = createRandomProposal(title);
      log.debug("Created proposal "+proposal);
    }
    // make sure, that testuser0 has at least 5 proposals
    for (int i = 0; i < 5; i++) {
			UserModel createdBy = util.user(TestFixtures.USER1_EMAIL);
    	String title = "Proposal " + i + " for user "+createdBy.getEmail();
      String description = util.getLoremIpsum(100,400);
      AreaModel area = this.defaultArea;
      int ageInDays = rand.nextInt(10);
      int reachedQuorumDaysAgo = (int)(ageInDays*rand.nextFloat());
      LawModel proposal = createProposal(title, description, area, createdBy, ageInDays, reachedQuorumDaysAgo);
			addCommentsToProposal(proposal);
      log.debug("Created proposal for user "+createdBy.getEmail());
    }
  }


  /**
   * Add num supporters to idea/proposal.   This only adds the references. It does not save/persist anything.
   * This is actually a quite interesting algorithm, because the initial creator of the idea must not be added as supporter
   * ,supporters must not be added twice but we want random supporters given these restrictions
   * @param idea the idea to add to
   * @param num number of new supporters to add.
   * @return the idea with the added supporters. The idea might have reached its quorum and now be a proposal
   */
  private LawModel addSupportersToIdea(@NonNull LawModel idea, int num) {
    if (num >= util.users.size()-1) throw new RuntimeException("Cannot at "+num+" supporters to idea. There are not enough usersMap.");
    if (idea.getId() == null) throw new RuntimeException(("Idea must must be saved to DB before you can add supporter to it. IdeaModel must have an Id"));

    // https://stackoverflow.com/questions/8378752/pick-multiple-random-elements-from-a-list-in-java
    LinkedList<UserModel> otherUsers = new LinkedList<>();
    for (UserModel user: util.usersMap.values()) {
      if (!user.equals(idea.getCreatedBy()))   otherUsers.add(user);
    }
    Collections.shuffle(otherUsers);
    //LawModel ideaFromDB = lawRepo.findById(idea.getId())  // Get JPA "attached" entity
    // .orElseThrow(()->new RuntimeException("Cannot find idea with id="+idea.getId()));

    List<UserModel> newSupporters = otherUsers.subList(0, num);
    for (UserModel supporter: newSupporters) {
			try {
				idea = lawService.addSupporter(supporter, idea);   //Remember: Don't just do idea.getSupporters().add(supporter);
			} catch (LiquidoException e) {
				// should not happen, can be ignored.
			}
		}
    return idea;
  }

  @Transactional
  private LawModel addCommentsToProposal(LawModel proposal) {
  	UserModel randUser = util.randUser();
		auditorAware.setMockAuditor(randUser);
		CommentModel rootComment = new CommentModel(proposal, "Comment on root level. I really like this idea, but needs to be improved. "+System.currentTimeMillis(), null);
		for (int i = 0; i < rand.nextInt(10); i++) {
			rootComment.getUpVoters().add(util.randUser());
		}
		for (int j = 0; j < rand.nextInt(10); j++) {
			rootComment.getDownVoters().add(util.randUser());
		}
		// Must save CommentModel immediately, to prevent "TransientPropertyValueException: object references an unsaved transient instance"
    // Could also add @Cascade(org.hibernate.annotations.CascadeType.ALL) on LawModel.comments but this would always overwrite and save the full list of all comments on every save of a LawModel.
    commentRepo.save(rootComment);
		for (int i = 0; i < rand.nextInt(8)+2; i++) {
			auditorAware.setMockAuditor(util.randUser());
			CommentModel reply = new CommentModel(proposal, "Reply "+i+" "+util.getLoremIpsum(10, 100) , rootComment);
			for (int k = 0; k < rand.nextInt(10); k++) {
				reply.getUpVoters().add(util.randUser());
			}
			for (int l = 0; l < rand.nextInt(10); l++) {
				reply.getDownVoters().add(util.randUser());
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
  private PollModel seedPollInElaborationPhase(AreaModel area, int numProposals) {
    log.info("Seeding one poll in elaboration phase ...");
    if (numProposals > util.usersMap.size())
    	throw new RuntimeException("Cannot seedPollInElaborationPhase. Need at least "+TestFixtures.NUM_ALTERNATIVE_PROPOSALS+" distinct usersMap");

    try {
      String title, desc;
      UserModel createdBy;

      //===== builder Poll from initial Proposal
      title = "Initial Proposal in a poll that is in elaboration "+System.currentTimeMillis();
      desc = util.getLoremIpsum(100, 400);
      createdBy = util.user(0);
      LawModel initialProposal = createProposal(title, desc, area, createdBy, 10, 7);
			initialProposal = addCommentsToProposal(initialProposal);
			String pollTitle = "Poll in ELABORATION from TestDataCreator "+System.currentTimeMillis() % 10000;
      PollModel newPoll = pollService.createPollWithProposal(pollTitle, initialProposal);
      newPoll.setTitle("Poll from TestDataCreator "+System.currentTimeMillis() % 10000);

      //===== add alternative proposals
      for (int i = 1; i < numProposals; i++) {
        title = "Alternative Proposal" + i + " in a poll that is in elaboration"+System.currentTimeMillis();
        desc = util.getLoremIpsum(100, 400);
        createdBy = util.user(i);
        LawModel altProp = createProposal(title, desc, area, createdBy, 20, 18);
				altProp = addCommentsToProposal(altProp);
        newPoll = pollService.addProposalToPoll(altProp, newPoll);
      }

      util.fakeCreateAt(newPoll, prop.daysUntilVotingStarts/2);
      util.fakeUpdatedAt(newPoll, prop.daysUntilVotingStarts/2);
      log.trace("Created poll in elaboration phase: "+newPoll);
      return newPoll;
    } catch (Exception e) {
      log.error("Cannot seed Poll in elaboration: " + e);
      throw new RuntimeException("Cannot seed Poll in elaboration", e);
    }

  }

  /**
   * Seed a poll that already is in its voting phase.
   * Will build upon a seedPollInElaborationPhase and then start the voting phase via pollService.
	 * This will NOT yet seedVotes(poll, numVotes);
   */ 
  public PollModel seedPollInVotingPhase(AreaModel area, int numProposals) {
    log.info("Seeding one poll in voting phase ...");
    try {
      PollModel poll = seedPollInElaborationPhase(area, numProposals);
      int i = 1;
      for(LawModel proposal: poll.getProposals()) {
        proposal.setTitle("Proposal "+i+" in voting phase "+System.currentTimeMillis());
        i++;
      }
      PollModel savedPoll = pollRepo.save(poll);
      util.fakeCreateAt(savedPoll, prop.daysUntilVotingStarts+1);
      util.fakeUpdatedAt(savedPoll, prop.daysUntilVotingStarts/2);

      //===== Start the voting phase of this poll
      pollService.startVotingPhase(savedPoll);
      LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
      savedPoll.setVotingStartAt(yesterday);
			savedPoll.setVotingEndAt(yesterday.truncatedTo(ChronoUnit.DAYS).plusDays(prop.durationOfVotingPhase));     //voting ends in n days at midnight
			savedPoll.setTitle("Poll in voting phase "+System.currentTimeMillis()%10000);
      savedPoll = pollRepo.save(savedPoll);

      return savedPoll;
    } catch (Exception e) {
      log.error("Cannot seed Poll in voting phase: " + e);
      throw new RuntimeException("Cannot seed Poll in voting phase", e);
    }
  }

	/**
	 * Seed one poll where the voting phase is already finished and we have a winner.
	 * This WILL also seedVotes, so that we can finish the poll and calculate the winner via the normal service call.
	 * @param area any area
	 * @param numProposals the number of proposals that the poll should have.
	 */
  public void seedPollFinished(AreaModel area, int numProposals) {
		log.info("Seeding one finished poll  ...");
		try {
			PollModel poll = seedPollInVotingPhase(area, numProposals);

			//---- fake some dates to be in the past
			int daysFinished = 5;  // poll was finished 5 days ago
			int daysVotingStarts = prop.daysUntilVotingStarts;
			int durationVotingPhase = prop.durationOfVotingPhase;
			LocalDateTime votingStartAt = LocalDateTime.now().minusDays(durationVotingPhase+daysFinished);
			poll.setVotingStartAt(votingStartAt);
			LocalDateTime votingEndAt = LocalDateTime.now().minusDays(daysFinished).truncatedTo(ChronoUnit.DAYS);  // voting ends at midnight
			poll.setVotingEndAt(votingEndAt);
			poll.setTitle("Finished Poll "+System.currentTimeMillis()%10000);

			pollRepo.save(poll);
			util.fakeCreateAt(poll, daysVotingStarts+durationVotingPhase+daysFinished);
			util.fakeUpdatedAt(poll, 1);

			//----- seed Votes
			seedVotes(poll, TestFixtures.NUM_VOTES);

			//----- end voting Phase
			LawModel winner = pollService.finishVotingPhase(poll);

			winner.setTitle("This winning Proposal is now a Law");
			lawRepo.save(winner);

			log.info("Created finished poll (id="+poll.getId()+" with winning proposal.id="+winner.getId());
		} catch (Exception e) {
			log.error("Cannot seed finished poll", e);
			throw new RuntimeException("Cannot seed finished poll", e);
		}
	}

  public void seedLaws() {
    log.info("Seeding laws");
    UserModel createdBy = util.user(TestFixtures.USER1_EMAIL);
    auditorAware.setMockAuditor(createdBy);

		//TODO: a real law actually needs to have been part of a (finished) poll with alternative proposals
    //PollModel poll = new PollModel();
    //pollRepo.save(poll);

    for (int i = 0; i < TestFixtures.NUM_LAWS; i++) {
      String lawTitle = "Law " + i;
      LawModel realLaw = createProposal(lawTitle, util.getLoremIpsum(100,400), this.defaultArea, createdBy, 12, 10);
			realLaw = addSupportersToIdea(realLaw, prop.supportersForProposal+2);
			//realLaw.setPoll(poll);
			LocalDateTime reachQuorumAt = LocalDateTime.now().minusDays(10);
      realLaw.setReachedQuorumAt(reachQuorumAt);
      realLaw.setStatus(LawStatus.LAW);
      upsertLawModel(realLaw, 20+i);
    }
  }

  /**
   * Will builder a new proposal or update an existing one with matching title.
   * And will set the createdAt date to n days ago
   * @param lawModel the new proposal to builder (or update)
   * @param ageInDays will setCreatedAt to so many days ago (measured from now)
   * @return the saved proposal
   */
  private LawModel upsertLawModel(LawModel lawModel, int ageInDays) {
    Optional<LawModel> existingLaw = lawRepo.findByTitle(lawModel.getTitle());  // may return null!
    if (existingLaw.isPresent()) {
      log.trace("Updating existing proposal with id=" + existingLaw.get().getId());
      lawModel.setId(existingLaw.get().getId());
    } else {
      log.trace("Creating new proposal " + lawModel);
    }
    LawModel savedLaw = lawRepo.save(lawModel);
    util.fakeCreateAt(savedLaw, ageInDays);
    return savedLaw;
  }

	/**
	 * Randomly cast some votes in the given poll. This will use the normal getVoterToken and castVote calls
	 * which are not that fast.
	 * @param pollInVoting
	 * @param numVotes
	 */
  public void seedVotes(PollModel pollInVoting, int numVotes) {
    log.info("Seeding votes for "+pollInVoting);
    if (TestFixtures.NUM_USERS < numVotes)
    	throw new RuntimeException("Cannot seed "+numVotes+" votes, because there are only "+TestFixtures.NUM_USERS+" test usersMap.");
    if (!PollModel.PollStatus.VOTING.equals(pollInVoting.getStatus()))
			throw new RuntimeException("Cannot seed votes. Poll must be in status "+ PollModel.PollStatus.VOTING);
  	if (pollInVoting.getNumCompetingProposals() < 2) throw new RuntimeException("Cannot seed votes. Poll in voting must have at least two proposals.");

  	// for the first n users
		util.usersMap.values().stream().limit(numVotes).forEach(voter -> {
			// we use CastVoteService to get a voterToken and cast our vote with a random voteOrder
			try {
				auditorAware.setMockAuditor(voter);
				String voterToken = castVoteService.createVoterTokenAndStoreRightToVote(voter, pollInVoting.getArea(), TestFixtures.USER_TOKEN_SECRET, TestFixtures.shouldBePublicProxy(pollInVoting.getArea(), voter));
				auditorAware.setMockAuditor(null); // MUST cast vote anonymously!
				List<LawModel> voteOrder = TestDataUtils.randVoteOrder(pollInVoting);
				CastVoteRequest castVoteRequest = new CastVoteRequest(pollInVoting, voteOrder, voterToken);
				CastVoteResponse castVoteResponse = castVoteService.castVote(castVoteRequest);
				log.trace("Vote casted "+castVoteResponse);
			} catch (LiquidoException e) {
				log.error("Cannot seed a vote: " + e);
			}
  	});
  }


}