package org.doogie.liquido.testdata;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.RightToVoteRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.*;
import org.doogie.liquido.util.DoogiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TestDataUtils {

	@Autowired
	DelegationRepo delegationRepo;

	@Autowired
	UserRepo userRepo;

	@Autowired
	RightToVoteRepo rightToVoteRepo;

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

	public UserModel upsert(UserModel user) {
		Optional<UserModel> existingUser = userRepo.findByEmail(user.getEmail());
		if (existingUser.isPresent()) {
			user.setId(existingUser.get().getId());
		}
		return userRepo.save(user);
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

}
