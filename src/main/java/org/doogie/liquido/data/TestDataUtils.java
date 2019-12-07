package org.doogie.liquido.data;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.ChecksumRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.ChecksumModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.util.DoogiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TestDataUtils {

	@Autowired
	DelegationRepo delegationRepo;

	@Autowired
	ChecksumRepo checksumRepo;

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

	public void printChecksumTree(ChecksumModel checksum) {
		if (checksum == null) return;
		Function<ChecksumModel, List<ChecksumModel>> getChildrenFunc = c -> checksumRepo.findByDelegatedTo(c);
		DoogiesUtil.printTreeRec(checksum, getChildrenFunc);
	}

}
