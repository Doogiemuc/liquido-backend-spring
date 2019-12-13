package org.doogie.liquido.testdata;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.model.*;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.*;
import org.doogie.liquido.util.DoogiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.persistence.Table;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Create nice real world example data about a school where pupils can vote for certain things.
 * Wouldn't that be a nice world in pupils could take part in democratic decisions at their own school!
 */
@Slf4j
@Component
@Order(2000)   // DB Schema must be created, then we can seed data into it.
public class SchoolExampleData implements CommandLineRunner {

	private static final String SEED_DB_PARAM = "seedSchoolExampleData";

	@Autowired
	LiquidoProperties prop;

	@Autowired
	UserRepo userRepo;
	private List<UserModel> users = new ArrayList<>();
	private Map<String, UserModel> usersMap = new HashMap<>();

	@Autowired
	AreaRepo areaRepo;
	private Map<String, AreaModel> areaMap = new HashMap<>();
	private AreaModel areaTimetable;
	private AreaModel areaExcursions;
	private AreaModel areaCanteen;

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
	PollService pollService;

	@Autowired
	CastVoteService castVoteService;

	@Autowired
	DelegationRepo delegationRepo;

	@Autowired
	ProxyService proxyService;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	LiquidoAuditorAware auditorAware;

	@Autowired
	Environment springEnv;

	@Autowired
	ApplicationContext appContext;

	/**
	 * When the command line parameter <pre>seedSchoolExampleData</pre> is given,
	 * then add data into the db.
	 * @param args command line args passed from CommandLineRunner
	 * @throws LiquidoException when creation of data fails
	 */
	@Override
	public void run(String... args) throws LiquidoException {
		boolean seedDB = "true".equalsIgnoreCase(springEnv.getProperty(SEED_DB_PARAM));;
		for(String arg : args) {
			if (("--"+SEED_DB_PARAM).equalsIgnoreCase(arg)) { seedDB = true; }
		}
		if (seedDB) {
			this.seedSchoolExampleData();
		}
	}

	/**
	 * Seed some real live example data. The examples are from the context of a school
	 * where pupils may take part in the organization of the school.
	 * @throws LiquidoException
	 */
	public void seedSchoolExampleData() throws LiquidoException {
		log.info("===== Create school example data: START");
		seedUsers();
		seedAreas();
		seedIdeas();
		seedProposals();
		seedPollInElaboration();
		seedPollInVoting();
		auditorAware.setMockAuditor(null);			// Important: After creating test data, no one is logged in!
		log.info("===== Create school example data: DONE");
	}

	void seedUsers() {
		log.debug("seedUsers");
		UserModel user1 = upsert(new UserModel("fritz@liquido.de",   "Fritz Cool",      "+49 22234501", "http://www.our-school.edu", TestFixtures.AVATAR_PREFIX+"1.png"));
		auditorAware.setMockAuditor(user1);
		upsert(new UserModel("peter@liido.de",     "Peter Newman",    "+49 22234502", "http://www.our-school.edu", TestFixtures.AVATAR_PREFIX+"2.png"));
		upsert(new UserModel("sarah@liquido.de",   "Sarah Connor",    "+49 22234503", "http://www.our-school.edu", TestFixtures.AVATAR_PREFIX+"3.png"));
		upsert(new UserModel("susan@liquido.de",   "Susan Johnson",   "+49 22234504", "http://www.our-school.edu", TestFixtures.AVATAR_PREFIX+"4.png"));
		upsert(new UserModel("john@liquido.de",    "Johan Appleseed", "+49 22234505", "http://www.our-school.edu", TestFixtures.AVATAR_PREFIX+"5.png"));
		upsert(new UserModel("jane@liido.de",      "Jane Doe",        "+49 22234506", "http://www.our-school.edu", TestFixtures.AVATAR_PREFIX+"6.png"));
		upsert(new UserModel("linda@liquido.de",   "Linda Maine",     "+49 22234507", "http://www.our-school.edu", TestFixtures.AVATAR_PREFIX+"7.png"));
		upsert(new UserModel("dug@liquido.de",     "Dug Branston",    "+49 22234508", "http://www.our-school.edu", TestFixtures.AVATAR_PREFIX+"8.png"));
		upsert(new UserModel("charles@liquido.de", "Charles Kirk",    "+49 22234509", "http://www.our-school.edu", TestFixtures.AVATAR_PREFIX+"9.png"));
		upsert(new UserModel("laurent@liido.de",   "Laurent Marnier", "+49 22234510", "http://www.our-school.edu", TestFixtures.AVATAR_PREFIX+"10.png"));
		upsert(new UserModel("angel@liquido.de",   "Angel Strickland","+49 22234511", "http://www.our-school.edu", TestFixtures.AVATAR_PREFIX+"11.png"));
		upsert(new UserModel("brenda@liquido.de",  "Brenda Lee",      "+49 22234512", "http://www.our-school.edu", TestFixtures.AVATAR_PREFIX+"12.png"));
		upsert(new UserModel("sharon@liquido.de",  "Sharon Jensen",   "+49 22234513", "http://www.our-school.edu", TestFixtures.AVATAR_PREFIX+"13.png"));
		upsert(new UserModel("nick@liido.de",      "Nick Colodeon",   "+49 22234514", "http://www.our-school.edu", TestFixtures.AVATAR_PREFIX+"14.png"));
		upsert(new UserModel("jody@liquido.de",    "Jody Hanson",     "+49 22234515", "http://www.our-school.edu", TestFixtures.AVATAR_PREFIX+"15.png"));
		upsert(new UserModel("mark@liquido.de",    "Mark Hardy",      "+49 22234516", "http://www.our-school.edu", TestFixtures.AVATAR_PREFIX+"16.png"));

		userRepo.findAll().forEach(user -> {
			this.usersMap.put(user.getEmail(), user);
			this.users.add(user);
		});
	}

	void seedAreas() {
		log.debug("seedAreas");
		UserModel admin = this.usersMap.get(prop.admin.email);
		if (admin == null) throw new RuntimeException("ERROR: Need admin user to seed areas!");
		this.areaTimetable  = upsert(new AreaModel("Timetable", "Planning the timetable for all classes", admin));
		this.areaExcursions = upsert(new AreaModel("Excursions", "Planning our adventurous excursions", admin));
		this.areaCanteen    = upsert(new AreaModel("Canteen", "Healthy food in our school", admin));
		areaRepo.findAll().forEach(area -> {
			this.areaMap.put(area.getTitle(), area);
		});
	}

	void seedIdeas() {
		log.debug("seedIdeas");
		auditorAware.setMockAuditor(this.users.get(0));
		upsert(new LawModel("More vegetarian meals", "We need more vegetarian meals in our canteen.", areaCanteen));
		auditorAware.setMockAuditor(this.users.get(1));
		upsert(new LawModel("More burgers", "I want to have hamburgers in our canteen!", areaCanteen));
		auditorAware.setMockAuditor(this.users.get(3));
		upsert(new LawModel("More Salad", "The canteen should offer a healthy salad more often.", areaCanteen));
	}

	void seedProposals() {
		log.debug("seedPollInElaboration");
		LawModel prop1 = createProposal("No classes on friday", "I want no classes on friday! Yeaah long weekend!", areaTimetable, this.users.get(4), 10, 5);   // prop1 by user4 can join poll in elaboration
		LawModel prop2 = createProposal("Hiking", "We should do more sports. The next excursion should go into the hills and we should climb a mountain.", areaExcursions, this.users.get(1), 20, 3);

		auditorAware.setMockAuditor(users.get(5));
		CommentModel comment = new CommentModel(prop1, "I really like this idea, but who will pay for this?", null);
		CommentModel reply   = new CommentModel(prop1, "We could request financial support from the parent council", comment);
		//commentRepo.save(reply);
		prop1.getComments().add(comment);

		auditorAware.setMockAuditor(this.users.get(0));
		upsert(prop1);
		auditorAware.setMockAuditor(this.users.get(1));
		upsert(prop2);

	}

	void seedPollInElaboration() throws LiquidoException {
		log.debug("seedPollInElaboration");
		LawModel sports1 = createProposal("No Sport in the morning", "Sport classes should not be in the morning. Just think about having math after sports. It is not possible to concentrate at all", areaTimetable, this.users.get(0), 10, 5);
		LawModel sports2 = createProposal("No sports on friday", "We should not schedule sports on a friday, because on that day there is no bus, so we have to carry our sport bags the whole long way.", areaTimetable, this.users.get(1), 20, 3);
		LawModel sports3 = createProposal("More sports classes", "There should be sports classes two times a day", areaTimetable, this.users.get(2), 8, 7);

		PollModel poll = new PollModel("When should we do sports classes?");
		pollService.addProposalToPoll(sports1, poll);
		pollService.addProposalToPoll(sports2, poll);
		pollService.addProposalToPoll(sports3, poll);

		PollModel savedPoll = pollRepo.save(poll);
		fakeCreateAt(savedPoll, prop.daysUntilVotingStarts/2);
		fakeUpdatedAt(savedPoll, prop.daysUntilVotingStarts/2);
		log.debug("Created poll in elaboration phase: "+poll);
	}

	PollModel seedPollInVoting() throws LiquidoException {
		log.debug("seedPollInVoting");
		LawModel prop1 = createProposal("Go to Disneyland", "We want our next excursion to go to Disneyland", areaExcursions, this.users.get(0), 2, 1);
		LawModel prop2 = createProposal("City Library", "We want our next excursion to go to the city library, so that we can read more books", areaExcursions, this.users.get(1), 9, 7);
		LawModel prop3 = createProposal("Theater", "Go to the theater. Spring ballet is in town. I heard it is awesome!!!", areaExcursions, this.users.get(2), 10, 2);

		PollModel poll = new PollModel("Where should we travel in our next excursion?");
		pollService.addProposalToPoll(prop1, poll);
		pollService.addProposalToPoll(prop2, poll);
		pollService.addProposalToPoll(prop3, poll);

		PollModel savedPoll = pollRepo.save(poll);
		fakeCreateAt(savedPoll, prop.daysUntilVotingStarts/2);
		fakeUpdatedAt(savedPoll, prop.daysUntilVotingStarts/2);

		//===== Start the voting phase of this poll
		pollService.startVotingPhase(savedPoll);
		LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
		savedPoll.setVotingStartAt(yesterday);
		savedPoll.setVotingEndAt(yesterday.truncatedTo(ChronoUnit.DAYS).plusDays(prop.durationOfVotingPhase));     // voting ends in n days at midnight
		savedPoll = pollRepo.save(savedPoll);

		log.debug("Created poll in voting phase: "+poll);
		return savedPoll;
	}




	//================ utility methods ==============

	private UserModel upsert(UserModel user) {
		Optional<UserModel> existingUser = userRepo.findByEmail(user.getEmail());
		if (existingUser.isPresent()) {
			user.setId(existingUser.get().getId());
		}
		return userRepo.save(user);
	}

	private AreaModel upsert(AreaModel area) {
		Optional<AreaModel> existingArea = areaRepo.findByTitle(area.getTitle());
		if (existingArea.isPresent()) {
			area.setId(existingArea.get().getId());
		}
		return areaRepo.save(area);
	}

	private LawModel upsert(LawModel p) {
		Optional<LawModel> existing = lawRepo.findByTitle(p.getTitle());
		if (existing.isPresent()) {
			p.setId(existing.get().getId());
		}
		return lawRepo.save(p);
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
		if (num >= usersMap.size()-1) throw new RuntimeException("Cannot at "+num+" supporters to idea. There are not enough usersMap.");
		if (idea.getId() == null) throw new RuntimeException(("Idea must must be saved to DB before you can add supporter to it. IdeaModel must have an Id"));

		// https://stackoverflow.com/questions/8378752/pick-multiple-random-elements-from-a-list-in-java
		LinkedList<UserModel> otherUsers = new LinkedList<>();
		for (UserModel user: this.usersMap.values()) {
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


	/**
	 * Create a new idea, then add enough supporters, so that it becomes a proposal. Then fake the createdAt and updatedAt dates
	 * so that they lie in the past.
	 */
	private LawModel createProposal(String title, String description, AreaModel area, UserModel createdBy, int ageInDays, int reachedQuorumDaysAgo) {
		if (ageInDays < reachedQuorumDaysAgo) throw new RuntimeException("Proposal cannot reach its quorum before it was created.");
		auditorAware.setMockAuditor(createdBy);
		LawModel proposal = upsert(new LawModel(title, description, area));

		proposal = addSupportersToIdea(proposal, prop.supportersForProposal);
		LocalDateTime reachQuorumAt = LocalDateTime.now().minusDays(reachedQuorumDaysAgo);
		proposal.setReachedQuorumAt(reachQuorumAt);			// fake reachQuorumAt date to be in the past

		lawRepo.save(proposal);
		fakeCreateAt(proposal,  ageInDays);
		fakeUpdatedAt(proposal, ageInDays > 1 ? ageInDays - 1 : 0);
		return proposal;
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
}
