package org.doogie.liquido.testdata;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.graphql.TeamsGraphQL;
import org.doogie.liquido.jwt.AuthUtil;
import org.doogie.liquido.model.*;
import org.doogie.liquido.rest.dto.CastVoteResponse;
import org.doogie.liquido.rest.dto.CreateOrJoinTeamResponse;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.*;
import org.doogie.liquido.util.DoogiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.doogie.liquido.model.LawModel.LawStatus;

/**
 * <h1>TestDataCreator</h1>
 *
 * Every test needs data. This test data is extremely important. Here we create it.
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
 * <h4>Create sample test data from scratch with JPA</h4>
 *
 * In application.properties  set  spring.jpa.hibernate.ddl-auto=create   to let Spring-JPA init the DB schema.
 * Then run this app with liquido.test.recreateTestData=true
 * TestDataCreator will create test data from scratch. It will call plain spring-data-jpa methods
 * and use service methods very useful.   This whole creation takes around 1-2 minutes.
 * The resulting schema and test data will be exported into an SQL script  sample-DB.sql
 *
 * <h4>Load schema and test data from an SQL script</h4>
 *
 * In application.properties  set  spring.jpa.hibernate.ddl-auto=none (Then a data.sql is not loaded!)
 * And set liquido.test.loadTestData=true
 * Then sample-data.sql is loaded. This is quick!
 *
 * You can create a sample-data.sql from the embedded H2 console with the SQL command
 * <pre>SCRIPT TO 'sample-DB.sql'</pre>
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
 * I already tried to refactor this TestDataCreator into its own module. But this is not possible,
 * because this class depends so closely on pretty much every liquido service. And this is a good thing.
 * We want to test everything here. => So TestDataCreator will stay an internal part of the LIQUIDO backend project.
 */

@Slf4j
@Component
//@Profile({"dev", "test", "int"})   			// run test data creator only during development or when running tests!
@Order(100)   		                      // seed DB first, then run the other CommandLineRunners
public class TestDataCreator implements CommandLineRunner {

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
	TeamsGraphQL teamServiceGQL;

  @Autowired
  PollService pollService;

  @Autowired
  CastVoteService castVoteService;

  @Autowired
	DelegationRepo delegationRepo;

  @Autowired
	ProxyService proxyService;

  @Autowired
	LiquidoProperties props;

  @Autowired
	TestDataUtils util;

  @Autowired
	AuthUtil authUtil;

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


	// I thought about this question for a long time:
	// Should TestDataCreator be completely deterministic. Or is it ok if it creates some random data.
	// One the one hand the system MUST be stable enough to handle random data.
	// But on the other hand, when debugging a very complex deeply hidden issue, then 100% repeatable test conditions are a must.
  // => Currently TestDataCreator has some random titles and descriptions.
  Random rand;

  public TestDataCreator() {
    this.rand = new Random(System.currentTimeMillis());
  }

  private AreaModel defaultArea = null;

  /** Lazily load default area */
  public AreaModel getDefaultArea() {
		if (this.defaultArea == null) {
			this.defaultArea = areaRepo.findByTitle(props.getDefaultAreaTitle()).orElseThrow(
				() -> new RuntimeException("ERROR in TestDataCreator: Cannot find default area(title="+props.getDefaultAreaTitle()+")")
			);
		}
		return this.defaultArea;
	}

  /**
   * See
	 * <pre>
	 * liquidoProperties.test.createTestData
	 * liquidoProperties.test.loadTestData
	 * </pre>
   * @param args command line args
   */
  public void run(String... args) throws LiquidoException {
		log.info("===== TestDataCreator START");
		log.debug(props.test.toString());
   	if (props.test.recreateTestData) {

			log.info("[TestDataCreator] Recreate test data from scratch. DB: "+ jdbcTemplate.getDataSource().toString());

			// Sanity check: Is there a schema with tables?
			try {
				List<UserModel> users = jdbcTemplate.queryForList("SELECT * FROM users LIMIT 10", UserModel.class);
			} catch (Exception e) {
				log.error("Cannot recreateTestData. There is no 'users' table! Did you create a DB schema at all?");
				throw e;
			}

      // ======== The order of these seed*() methods is very important! =====
      seedUsers(TestFixtures.NUM_USERS, TestFixtures.MAIL_PREFIX);
      util.seedAdminUser();
      auditorAware.setMockAuditor(util.user(TestFixtures.USER1_EMAIL));   // Simulate that user is logged in.  This user will be set as @createdAt
			seedAreas();

			// See some teams for mobile app. With an admin, users, proposals and polls.
			TeamModel team1 = seedTeam(TestFixtures.TEAM1_NAME, TestFixtures.TEAM1_ADMIN_EMAIL, TestFixtures.TEAM1_ADMIN_MOBILEPHONE);
			for (int i = 1; i < TestFixtures.NUM_TEAMS; i++) {
				// create unique admin data
				String teamName   = TestFixtures.TEAM_NAME_PREFIX + i;
				String adminEmail = TestFixtures.TEAM_ADMIN_EMAIL_PREFIX + "_" + teamName + "@liquido.me";
				String adminMobilephone = TestFixtures.MOBILEPHONE_PREFIX + i + DoogiesUtil.randomDigits(5);
				seedTeam(teamName, adminEmail, adminMobilephone);
			}
			seedMemberInTwoTeams(TestFixtures.TWO_TEAM_USER_EMAIL, TestFixtures.TWO_TEAM_USER_MOBILEPHONE, TestFixtures.TEAM1_NAME, TestFixtures.TEAM_NAME_PREFIX+"1");
			seedPollInElaborationInTeam(team1);
			PollModel pollInTeam = seedPollInVotingInTeam(team1);
			seedVotes(pollInTeam, team1.getMembers(), TestFixtures.NUM_TEAM_MEMBERS);

			// Further Test Data for web app
			seedIdeas();
			seedProposals();
			seedProxies(TestFixtures.delegations);
			seedPollInElaborationPhase(TestFixtures.NUM_ALTERNATIVE_PROPOSALS);
			seedPollInVotingPhase(TestFixtures.NUM_ALTERNATIVE_PROPOSALS);						      // seed one poll in voting
			PollModel poll = seedPollInVotingPhase(TestFixtures.NUM_ALTERNATIVE_PROPOSALS);
			seedVotes(poll, util.usersMap.values(), TestFixtures.NUM_VOTES);
			seedPollFinished(TestFixtures.NUM_ALTERNATIVE_PROPOSALS);
      seedLaws();
      auditorAware.setMockAuditor(null);

			/* Try to dump the created data into a .sql file. But this depends on the database driver being used */
      try {
				if (jdbcTemplate.getDataSource().getConnection().getMetaData().getURL().contains("h2")) {
					// The `SCRIPT TO` command only works for H2 in-memory DB
					jdbcTemplate.execute("SCRIPT TO '" + props.test.sampleDbFile + "'");        //There would also be a "SCRIPT NODATA TO ...", but Hibernate can already export the schema.
					adjustDbInitializationScript();
					log.info("===== Successfully stored test data in file: " + props.test.sampleDbFile);
				}
			} catch (Exception e) {
      	String errMsg = "Could not store test data file: " + e.getMessage();
      	log.error(errMsg);
      	//throw new LiquidoException(LiquidoException.Errors.INTERNAL_ERROR, errMsg, e);
			}
    }

    if (props.test.loadTestData) {
			log.info("===== TestDataCreator: Loading schema and sample data from file: " + props.test.sampleDbFile);
    	try {
				Resource fileResource = appContext.getResource(props.test.sampleDbFile);
				FileInputStream fis = new FileInputStream(props.test.sampleDbFile);
				InputStreamResource resource = new InputStreamResource(fis);
				//Resource resource = new ClassPathResource(props.test.sampleDbFile);
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
				log.info("===== TestDataCreator: Successfully loaded schema and sample data from "+ props.test.sampleDbFile +" => DONE");
			} catch (Exception e) {
				String errMsg = "ERROR: Cannot load schema and sample data from "+ props.test.sampleDbFile;
				log.error(errMsg);
				throw new RuntimeException(errMsg, e);
			}
		}

    // log proxy structure in default area if debug logging is enabled
    if (log.isDebugEnabled()) {
			Optional<UserModel> topProxyOpt = userRepo.findByEmail(TestFixtures.USER1_EMAIL);      // user1 is the topmost proxy in TestFixtures.java
			if (topProxyOpt.isPresent()) {
				UserModel topProxy = topProxyOpt.get();
				log.debug("====== TestDataCreator: Proxy tree =====");
				utils.printProxyTree(getDefaultArea(), topProxy);

				log.debug("====== TestDataCreator: Tree of delegations =====");
				utils.printDelegationTree(getDefaultArea(), topProxy);

				try {
					String voterToken = castVoteService.createVoterTokenAndStoreRightToVote(topProxy, getDefaultArea(), TestFixtures.USER_TOKEN_SECRET, false);
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
			File sqlScript = new File(props.test.sampleDbFile);
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
			writer.write("-- BE CAREFUL: This script completely DROPs and RE-CREATES the DB !!!!!\n");
			writer.write("DROP ALL OBJECTS;\n");
			for(String line : lines) {
				writer.write(line);
				writer.newLine();		//  + System.getProperty("line.separator")
			}
			writer.close();
			log.trace("removeQuartzSchema from SQL script successful: "+sqlScript.getAbsolutePath());

		} catch (Exception e) {
			log.error("Could not remove Quarts statements from Schema: "+e.getMessage());
  		throw new RuntimeException("Could not remove Quarts statements from Schema: "+e.getMessage(), e);
		}
	}

	/**
	 * Seed a new team with defaults
	 * @param teamName name of team
	 * @param adminEmail email of teams admin (mobilephone will be created randomly)
	 * @return JoinTeamResponse for last member in team (not the one from the admin!)
	 * @throws LiquidoException
	 */

	/**
	 * Seed a team with its admin users. And let some users join each team.
	 * All params must be new and globally unique!
	 * @param teamName the name of the team.
	 * @param adminEmail email address of the team's new admin
	 * @param adminMobilephone mobilephone of admin
	 * @return JoinTeamResponse for last member in team
	 */
	public TeamModel seedTeam(@NonNull String teamName, @NonNull String adminEmail, @NonNull String adminMobilephone) throws LiquidoException {
		log.info("Seeding a Team with members ...");
		String digits = DoogiesUtil.randomDigits(5);
		String adminName        = TestFixtures.TEAM_ADMIN_NAME_PREFIX + " of " + teamName;
		//if (adminEmail == null) adminEmail = TestFixtures.TEAM_ADMIN_EMAIL_PREFIX + "@" + teamName + ".org";
		//if (adminMobilephone == null) adminMobilephone = TestFixtures.MOBILEPHONE_PREFIX + digits;               // MUST create unique mobile phone numbers!
		String adminWebsite     = "www.liquido.vote";
		String adminPicture     = TestFixtures.AVATAR_IMG_PREFIX + "0.png";
		UserModel admin = new UserModel(adminEmail, adminName, adminMobilephone, adminWebsite, adminPicture);
		CreateOrJoinTeamResponse res = teamServiceGQL.createNewTeam(teamName, admin);
		CreateOrJoinTeamResponse joinTeamRes = null;
		for (int j = 0; j < TestFixtures.NUM_TEAM_MEMBERS; j++) {
			UserModel member = new UserModel(
				TestFixtures.TEAM_MEMBER_EMAIL_PREFIX + j + "@" + teamName + ".org",
				TestFixtures.TEAM_MEMBER_NAME_PREFIX + j + " " + teamName,
				TestFixtures.MOBILEPHONE_PREFIX + digits + j,
				"www.liquido.vote",
				TestFixtures.AVATAR_IMG_PREFIX + (j%16) + ".png"
			);
			joinTeamRes = teamServiceGQL.joinTeam(res.getTeam().getInviteCode(), member);
		}
		return joinTeamRes.getTeam(); // most up to date firstTeam entity, with all members, is from last joinNewTeam response
	}

	/**
	 * Seed a user that is member of two teams
	 * @param memberEmail user's email
	 * @param teamName1 team1 to join
	 * @param teamName2 team2 to join
	 * @return the CreateOrJoinTeam response from the <b>second</b> team
	 * @throws LiquidoException when team1 or team2 cannot by found.
	 */
	public UserModel seedMemberInTwoTeams(String memberEmail, String mobilephone, String teamName1, String teamName2) throws LiquidoException {
	 	log.info("Seeding user that is member in two teams");
	 	TeamModel team1 = teamRepo.findByTeamName(teamName1).orElseThrow(LiquidoException.notFound("Cannot find team.teamName="+teamName1));
		TeamModel team2 = teamRepo.findByTeamName(teamName2).orElseThrow(LiquidoException.notFound("Cannot find team.teamName="+teamName2));
		String digits = DoogiesUtil.randomDigits(5);
		UserModel member = new UserModel(
			memberEmail,
			TestFixtures.TEAM_MEMBER_NAME_PREFIX + digits + "_2teams",
			mobilephone,
			"www.liquido.vote",
			TestFixtures.AVATAR_IMG_PREFIX + "1.png"
		);
		CreateOrJoinTeamResponse res = teamServiceGQL.joinTeam(team1.getInviteCode(), member);
		// MUST authenticate for the second join team call!
		authUtil.authenticateInSecurityContext(res.getUser().getId(), res.getTeam().getId(), res.getJwt());
		res = teamServiceGQL.joinTeam(team2.getInviteCode(), member);
		return res.getUser();
	}

	/**
	 * Create on poll in the team with two proposals
	 * @param team
	 * @return the poll in status ELABORATION
	 * @throws LiquidoException
	 */
	public PollModel seedPollInElaborationInTeam(@NonNull TeamModel team) throws LiquidoException {
		log.info("Seeding a poll with two proposals in a team");
		long now = System.currentTimeMillis() % 1000;
		UserModel admin = team.getAdmins().stream().findFirst()
			.orElseThrow(LiquidoException.notFound("need a team admin to seedPollInTeam"));
		authUtil.authenticateInSecurityContext(admin.getId(), team.getId(), null);  // fake login admin
		String title = "Poll " + now +  " in Team "+team.getTeamName();
		PollModel poll = pollService.createPoll(title, getDefaultArea(), team);
		LawModel proposal = this.createProposal("Proposal " + now + " in Team "+team.getTeamName(), util.getLoremIpsum(30,100), getDefaultArea(), admin, 2, 1);
		pollService.addProposalToPoll(proposal, poll);
		UserModel member = team.getMembers().stream().findFirst()
			.orElseThrow(LiquidoException.notFound("need a team member to seedPollInTeam"));
		authUtil.authenticateInSecurityContext(member.getId(), team.getId(), null);  // fake login member
		LawModel proposal2 = this.createProposal("Another prop " + now + " in Team "+team.getTeamName(), util.getLoremIpsum(30,100), getDefaultArea(), member, 2, 1);
		poll = pollService.addProposalToPoll(proposal2, poll);
		return poll;
	}

	/**
	 * Create a poll in elaboration in team and start its voting phase
	 * @param team
	 * @return the poll in status VORING
	 * @throws LiquidoException
	 */
	public PollModel seedPollInVotingInTeam(@NonNull TeamModel team) throws LiquidoException {
		PollModel poll = this.seedPollInElaborationInTeam(team);
		UserModel admin = team.getAdmins().stream().findFirst()
			.orElseThrow(LiquidoException.notFound("Need a team admin to seedPollInVotingInTeam"));
		authUtil.authenticateInSecurityContext(admin.getId(), team.getId(), null);  // fake login admin
		poll = pollService.startVotingPhase(poll);
		return poll;
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
			String picture     	= TestFixtures.AVATAR_IMG_PREFIX +((i%16)+1)+".png";
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
		this.defaultArea = areaRepo.save(defaultArea);

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

		AreaModel area = this.getDefaultArea();
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

	/**
	 * Seed some initial ideas.
	 * If prop.supportersForProposal is <= 0 then these ideas normally immediately would become proposals.
	 */
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
      LawModel newIdea = new LawModel(ideaTitle, ideaDescr.toString(), getDefaultArea());
      lawRepo.save(newIdea);

	    // add some supporters, but not enough to become a proposal
      if (props.supportersForProposal > 0) {
				int numSupporters = rand.nextInt(props.supportersForProposal - 1);
				//log.debug("adding "+numSupporters+" supporters to idea "+newIdea);
				addSupportersToIdea(newIdea, numSupporters);
			}
      //LawModel savedIdea = lawRepo.save(newIdea);
      util.fakeCreateAt(newIdea, i+1);
      util.fakeUpdatedAt(newIdea, i);
    }
  }



  private LawModel createProposal(String title, String description, AreaModel area, UserModel createdBy, int ageInDays, int reachedQuorumDaysAgo) {
  	if (ageInDays < reachedQuorumDaysAgo) throw new RuntimeException("Proposal cannot reach its quorum before it was created.");
    auditorAware.setMockAuditor(createdBy);
    LawModel proposal = new LawModel(title, description, getDefaultArea());
		lawRepo.save(proposal);

		// add enough supporters so that the idea becomes a proposal. (Or add some random supporters.)
		int numSupporters = props.supportersForProposal > 0 ? props.supportersForProposal : rand.nextInt(5)+1;
		proposal = addSupportersToIdea(proposal, numSupporters);

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
    int ageInDays = rand.nextInt(10);
    int reachQuorumDaysAgo = (int)(ageInDays*rand.nextFloat());
    LawModel proposal = createProposal(title, description.toString(), getDefaultArea(), createdBy, ageInDays, reachQuorumDaysAgo);
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
      AreaModel area = this.getDefaultArea();
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
  	if (num <= 0) return idea;
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
  private PollModel seedPollInElaborationPhase(int numProposals) {
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
      LawModel initialProposal = createProposal(title, desc, getDefaultArea(), createdBy, 10, 7);
			initialProposal = addCommentsToProposal(initialProposal);
			String pollTitle = "Poll in ELABORATION from TestDataCreator "+System.currentTimeMillis() % 10000;
      PollModel newPoll = pollService.createPollWithProposal(pollTitle, initialProposal);
      newPoll.setTitle("Poll from TestDataCreator "+System.currentTimeMillis() % 10000);

      //===== add alternative proposals
      for (int i = 1; i < numProposals; i++) {
        title = "Alternative Proposal" + i + " in a poll that is in elaboration"+System.currentTimeMillis();
        desc = util.getLoremIpsum(100, 400);
        createdBy = util.user(i);
        LawModel altProp = createProposal(title, desc, getDefaultArea(), createdBy, 20, 18);
				altProp = addCommentsToProposal(altProp);
        newPoll = pollService.addProposalToPoll(altProp, newPoll);
      }

      util.fakeCreateAt(newPoll, props.daysUntilVotingStarts/2);
      util.fakeUpdatedAt(newPoll, props.daysUntilVotingStarts/2);
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
      util.fakeCreateAt(savedPoll, props.daysUntilVotingStarts+1);
      util.fakeUpdatedAt(savedPoll, props.daysUntilVotingStarts/2);

      //===== Start the voting phase of this poll
      pollService.startVotingPhase(savedPoll);
      LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
      savedPoll.setVotingStartAt(yesterday);
			savedPoll.setVotingEndAt(yesterday.truncatedTo(ChronoUnit.DAYS).plusDays(props.durationOfVotingPhase));     //voting ends in n days at midnight
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
	 * @param numProposals the number of proposals that the poll should have.
	 */
  public void seedPollFinished(int numProposals) {
		log.info("Seeding one finished poll  ...");
		try {
			PollModel poll = seedPollInVotingPhase(numProposals);

			//---- fake some dates to be in the past
			int daysFinished = 5;  // poll was finished 5 days ago
			int daysVotingStarts = props.daysUntilVotingStarts;
			int durationVotingPhase = props.durationOfVotingPhase;
			LocalDateTime votingStartAt = LocalDateTime.now().minusDays(durationVotingPhase+daysFinished);
			poll.setVotingStartAt(votingStartAt);
			LocalDateTime votingEndAt = LocalDateTime.now().minusDays(daysFinished).truncatedTo(ChronoUnit.DAYS);  // voting ends at midnight
			poll.setVotingEndAt(votingEndAt);
			poll.setTitle("Finished Poll "+System.currentTimeMillis()%10000);

			pollRepo.save(poll);
			util.fakeCreateAt(poll, daysVotingStarts+durationVotingPhase+daysFinished);
			util.fakeUpdatedAt(poll, 1);

			//----- seed Votes
			seedVotes(poll, util.usersMap.values(), TestFixtures.NUM_VOTES);

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
      LawModel realLaw = createProposal(lawTitle, util.getLoremIpsum(100,400), getDefaultArea(), createdBy, 12, 10);
			realLaw = addSupportersToIdea(realLaw, props.supportersForProposal+2);
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
  public void seedVotes(PollModel pollInVoting, Collection<UserModel> voters, int numVotes) {
    log.info("Seeding votes for "+pollInVoting);
    if (voters.size() < numVotes)
    	throw new RuntimeException("Cannot seed "+numVotes+" votes, because there are only "+TestFixtures.NUM_USERS+" test usersMap.");
    if (!PollModel.PollStatus.VOTING.equals(pollInVoting.getStatus()))
			throw new RuntimeException("Cannot seed votes. Poll must be in status "+ PollModel.PollStatus.VOTING);
  	if (pollInVoting.getNumCompetingProposals() < 2) throw new RuntimeException("Cannot seed votes. Poll in voting must have at least two proposals.");

  	// for the first n voters
		voters.stream().limit(numVotes).forEach(voter -> {
			// we use CastVoteService to get a voterToken and cast our vote with a random voteOrder
			try {
				auditorAware.setMockAuditor(voter);
				String voterToken = castVoteService.createVoterTokenAndStoreRightToVote(voter, pollInVoting.getArea(), TestFixtures.USER_TOKEN_SECRET, TestFixtures.shouldBePublicProxy(pollInVoting.getArea(), voter));
				auditorAware.setMockAuditor(null); // MUST cast vote anonymously!
				List<Long> voteOrderIds = TestDataUtils.randVoteOrderIds(pollInVoting);
				CastVoteResponse castVoteResponse = castVoteService.castVote(voterToken, pollInVoting, voteOrderIds);
				log.trace("Vote casted "+castVoteResponse);
			} catch (LiquidoException e) {
				log.error("Cannot seed a vote: " + e);
			}
  	});
  }


}