package org.doogie.liquido.testdata;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.model.*;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.LawService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.util.DoogiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TestDataUtils {

	@Autowired
	AreaRepo areaRepo;

	@Autowired
	UserRepo userRepo;

	@Autowired
	LawRepo lawRepo;

	@Autowired
	DelegationRepo delegationRepo;

	@Autowired
	RightToVoteRepo rightToVoteRepo;

	@Autowired
	LawService lawService;

	@Autowired
	LiquidoAuditorAware auditorAware;

	@Autowired
	Environment springEnv;

	@Autowired
	LiquidoProperties prop;

	@Autowired
	JdbcTemplate jdbcTemplate;

	Random rand = new Random();

	public Map<String, UserModel> usersMap = new HashMap();
	public List<UserModel> users = new ArrayList();

	public void reloadUsersCache() {
		this.usersMap = new HashMap();
		this.users = new ArrayList();
		userRepo.findAll().forEach(user -> {
			this.usersMap.put(user.getEmail(), user);
			this.users.add(user);
		});
	}

	public UserModel user(int index) {
		return this.users.get(index);
	}

	public UserModel user(String email) {
		return this.usersMap.get(email);
	}

	public UserModel randUser() {
		return this.users.get(rand.nextInt(this.users.size()));
	}

	public void printProxyTree(AreaModel area, UserModel proxy) {
		if (proxy == null) return;
		Function<UserModel, List<UserModel>> getChildrenFunc = toProxy -> delegationRepo.findByAreaAndToProxy(area, toProxy)
				.stream().map(del -> del.getFromUser()).collect(Collectors.toList());
		DoogiesUtil.printTreeRec(proxy, getChildrenFunc);
	}

	public void printDelegationTree(AreaModel area, UserModel proxy) {
		if (proxy == null) return;
		UserModel dummyUser = new UserModel("aboveTopProxy@dummy.org");
		DelegationModel dummyTopProxyDel = new DelegationModel(area, proxy, dummyUser);
		Function<DelegationModel, List<DelegationModel>> getChildrenFunc = del -> delegationRepo.findByAreaAndToProxy(area, del.getFromUser())
				.stream().collect(Collectors.toList());
		DoogiesUtil.printTreeRec(dummyTopProxyDel, getChildrenFunc);
	}

	public void printRightToVoteTree(RightToVoteModel rightToVote) {
		if (rightToVote == null) return;
		Function<RightToVoteModel, List<RightToVoteModel>> getChildrenFunc = c -> rightToVoteRepo.findByDelegatedTo(c);
		DoogiesUtil.printTreeRec(rightToVote, getChildrenFunc);
	}

	/** create a random vote Order with at least one proposal */
	public static List<LawModel> randVoteOrder(PollModel poll) {
		Random rand = new Random(System.currentTimeMillis());
		List<LawModel> voteOrder = poll.getProposals().stream()
			.filter(p -> rand.nextInt(10) > 0)					// keep 90% of the candidates
			.sorted((p1, p2) -> rand.nextInt(2)*2 - 1)  // compare randomly  -1 or +1
			.collect(Collectors.toList());
		if (voteOrder.size() == 0) voteOrder.add(poll.getProposals().iterator().next());
		return voteOrder;
	}

	public UserModel upsert(UserModel user) {
		Optional<UserModel> existingUser = userRepo.findByEmail(user.getEmail());
		if (existingUser.isPresent()) {
			user.setId(existingUser.get().getId());
		}
		return userRepo.save(user);
	}

	public AreaModel upsert(AreaModel area) {
		Optional<AreaModel> existingArea = areaRepo.findByTitle(area.getTitle());
		if (existingArea.isPresent()) {
			area.setId(existingArea.get().getId());
		}
		return areaRepo.save(area);
	}

	public LawModel upsert(LawModel p) {
		Optional<LawModel> existing = lawRepo.findByTitle(p.getTitle());
		if (existing.isPresent()) {
			p.setId(existing.get().getId());
		}
		return lawRepo.save(p);
	}

	/**
	 * Fake the created_at date to be n days in the past
	 *
	 * @param model     any domain model class derived from BaseModel
	 * @param ageInDays the number of days to set the createAt field into the past.
	 */
	public void fakeCreateAt(BaseModel model, int ageInDays) {
		updateDateField(model, "created_at", ageInDays, model.getId());
	}

	/**
	 * Fake the updated_at date of the given model to be n days in the past
	 * @param model a liquido model, eg. a LawModel
	 * @param ageInDays the number of days to se the updated_at field into the past.
	 */
	public void fakeUpdatedAt(BaseModel model, int ageInDays) {
		updateDateField(model, "updated_at", ageInDays, model.getId());
	}

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * Fake the value of a dateTime field in the DB with nasty proprietary SQL syntax. This is a hack.
	 */
	public void updateDateField(BaseModel model, String field, int ageInDays, long id) {
		if (ageInDays < 0) throw new IllegalArgumentException("ageInDays must be positive");
		Table tableAnnotation = model.getClass().getAnnotation(javax.persistence.Table.class);
		String tableName = tableAnnotation.name();
		String hybernateDialect = (String)entityManager.getEntityManagerFactory().getProperties().getOrDefault("hibernate.dialect", "");
		String sql = "";
		log.debug("Using hybernateDialect="+hybernateDialect);
		if(hybernateDialect.contains("H2Dialect")) {
			sql = "UPDATE " + tableName + " SET "+field+" = DATEADD('DAY', -" + ageInDays + ", NOW()) WHERE id=" + id;
		} else { // MySQL
			sql = "UPDATE " + tableName + " SET "+field+" = CURRENT_TIMESTAMP() - interval " + ageInDays + " day WHERE id=" + id;
		}
		log.trace("Executing sql:" + sql);
		jdbcTemplate.execute(sql);
		Date daysAgo = DoogiesUtil.daysAgo(ageInDays);
		model.setCreatedAt(daysAgo);

	}

	/**
	 * Add num supporters to idea/proposal.   This only adds the references. It does not save/persist anything.
	 * This is actually a quite interesting algorithm, because the initial creator of the idea must not be added as supporter
	 * ,supporters must not be added twice but we want random supporters given these restrictions
	 *
	 * @param idea the idea to add to
	 * @param num  number of new supporters to add.
	 * @return the idea with the added supporters. The idea might have reached its quorum and now be a proposal
	 */
	public LawModel addSupportersToIdea(@NonNull LawModel idea, int num) {
		if (num >= usersMap.size() - 1)
			throw new RuntimeException("Cannot at " + num + " supporters to idea. There are not enough usersMap.");
		if (idea.getId() == null)
			throw new RuntimeException(("Idea must must be saved to DB before you can add supporter to it. IdeaModel must have an Id"));

		// https://stackoverflow.com/questions/8378752/pick-multiple-random-elements-from-a-list-in-java
		LinkedList<UserModel> otherUsers = new LinkedList<>();
		for (UserModel user : this.usersMap.values()) {
			if (!user.equals(idea.getCreatedBy())) otherUsers.add(user);
		}
		Collections.shuffle(otherUsers);
		//LawModel ideaFromDB = lawRepo.findById(idea.getId())  // Get JPA "attached" entity
		// .orElseThrow(()->new RuntimeException("Cannot find idea with id="+idea.getId()));

		List<UserModel> newSupporters = otherUsers.subList(0, num);
		for (UserModel supporter : newSupporters) {
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
	public LawModel createProposal(String title, String description, AreaModel area, UserModel createdBy, int ageInDays, int reachedQuorumDaysAgo) {
		if (ageInDays < reachedQuorumDaysAgo)
			throw new RuntimeException("Proposal cannot reach its quorum before it was created.");
		auditorAware.setMockAuditor(createdBy);
		LawModel proposal = upsert(new LawModel(title, description, area));

		proposal = addSupportersToIdea(proposal, prop.supportersForProposal);
		LocalDateTime reachQuorumAt = LocalDateTime.now().minusDays(reachedQuorumDaysAgo);
		proposal.setReachedQuorumAt(reachQuorumAt);      // fake reachQuorumAt date to be in the past

		lawRepo.save(proposal);
		fakeCreateAt(proposal, ageInDays);
		fakeUpdatedAt(proposal, ageInDays > 1 ? ageInDays - 1 : 0);
		return proposal;
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
