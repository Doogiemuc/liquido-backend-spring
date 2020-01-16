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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.persistence.Table;
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
	LiquidoProperties prop;

	@Autowired
	JdbcTemplate jdbcTemplate;

	Map<String, UserModel> usersMap = new HashMap();
	List<UserModel> users = new ArrayList();

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
	 * Fake the created at date to be n days in the past
	 *
	 * @param model     any domain model class derived from BaseModel
	 * @param ageInDays the number of days to set the createAt field into the past.
	 */
	public void fakeCreateAt(BaseModel model, int ageInDays) {
		if (ageInDays < 0) throw new IllegalArgumentException("ageInDays must be positive");
		Table tableAnnotation = model.getClass().getAnnotation(javax.persistence.Table.class);
		String tableName = tableAnnotation.name();
		//String sql = "UPDATE " + tableName + " SET created_at = DATEADD('DAY', -" + ageInDays + ", NOW()) WHERE id='" + model.getId() + "'";
		String postgreSQL = "UPDATE " + tableName + " SET created_at = clock_timestamp() + interval '" + ageInDays + " day'  WHERE id='" + model.getId() + "'";
		log.trace("Executing sql:" + postgreSQL);
		jdbcTemplate.execute(postgreSQL);
		Date daysAgo = DoogiesUtil.daysAgo(ageInDays);
		model.setCreatedAt(daysAgo);
	}

	/**
	 * Fake the updated at date to be n days in the past. Keep in mind, that updated at should not be before created at.
	 *
	 * @param model     any domain model class derived from BaseModel
	 * @param ageInDays the number of days to set the createAt field into the past.
	 */
	public void fakeUpdatedAt(BaseModel model, int ageInDays) {
		if (ageInDays < 0) throw new IllegalArgumentException("ageInDays must be positive");
		Table tableAnnotation = model.getClass().getAnnotation(javax.persistence.Table.class);
		String tableName = tableAnnotation.name();
		//String sql = "UPDATE " + tableName + " SET updated_at = DATEADD('DAY', -" + ageInDays + ", NOW()) WHERE id='" + model.getId() + "'";
		String postgreSQL = "UPDATE " + tableName + " SET updated_at = clock_timestamp() + interval '" + ageInDays + " day'  WHERE id='" + model.getId() + "'";
		log.trace("Executing sql:" + postgreSQL);
		jdbcTemplate.execute(postgreSQL);
		Date daysAgo = DoogiesUtil.daysAgo(ageInDays);
		model.setUpdatedAt(daysAgo);
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

}
