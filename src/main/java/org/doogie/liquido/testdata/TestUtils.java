package org.doogie.liquido.testdata;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.ChecksumRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.ChecksumModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Slf4j
@Component
public class TestUtils {

	@Autowired
	DelegationRepo delegationRepo;

	@Autowired
	ChecksumRepo checksumRepo;

	public void printDelegationTree(AreaModel area, UserModel proxy) {
		this.printDelegationTreeRec("", area, proxy, false, null);
	}

	/**
	 * Recursively pretty print the tree of proxies.  I LOVE stackoverflow :-)
	 * https://stackoverflow.com/questions/4965335/how-to-print-binary-tree-diagram
	 */
	private void printDelegationTreeRec(String prefix, AreaModel area, UserModel proxy, boolean isTail, DelegationModel delegation) {
		String nodeName = proxy.getEmail();
		if (delegation != null && delegation.isDelegationRequest()) nodeName += " requested from "+delegation.getRequestedDelegationFromChecksum().getChecksum();
		String nodeConnection = isTail ? "└" : "├";
		nodeConnection += delegation != null && delegation.isTransitive() ? "T─ " : "── ";
		log.debug(prefix + nodeConnection + nodeName);

		List<DelegationModel> delegations = delegationRepo.findByAreaAndToProxy(area, proxy);  // this also finds delegation requests
		for (int i = 0; i < delegations.size(); i++) {
			String newPrefix = prefix + (isTail ? "    " : "│   ");
			printDelegationTreeRec(newPrefix, area, delegations.get(i).getFromUser(), i == delegations.size()-1, delegations.get(i));
		}
	}

	public void printChecksumTree(ChecksumModel checksum) {
		//this.printChecksumTreeRec("", checksum, false);
		Function<ChecksumModel, List<ChecksumModel>> getChildrenFunc = c -> checksumRepo.findByDelegatedTo(c);
		this.printTreeRec("", checksum, getChildrenFunc, false);
	}

	/**
	 * Recursively pretty print the tree of checksums
	 */
	private void printChecksumTreeRec(String prefix, ChecksumModel checksum, boolean isTail) {
		String nodeName = checksum.getChecksum();
		if (checksum.getPublicProxy() != null) nodeName += " - "+checksum.getPublicProxy().getEmail();
		String nodeConnection = isTail ? "└── " : "├── ";
		log.debug(prefix + nodeConnection + nodeName);

		List<ChecksumModel> delegations = checksumRepo.findByDelegatedTo(checksum);
		for (int i = 0; i < delegations.size(); i++) {
			String newPrefix = prefix + (isTail ? "    " : "│   ");
			printChecksumTreeRec(newPrefix, delegations.get(i), i == delegations.size()-1);
		}
	}

	/**
	 * Print a tree structure in a pretty ASCII fromat.
	 * @param prefix Currnet previx. Use "" in initial call!
	 * @param node The current node. Pass the root node of your tree in initial call.
	 * @param getChildrenFunc A {@link Function} that returns the children of a given node.
	 * @param isTail Is node the last of its sibblings. Use false in initial call. (This is needed for pretty printing.)
	 * @param <T> The type of your nodes. Anything that has a toString can be used.
	 */
	private <T> void printTreeRec(String prefix, T node, Function<T, List<T>> getChildrenFunc, boolean isTail) {
		String nodeName = node.toString();
		String nodeConnection = isTail ? "└── " : "├── ";
		log.debug(prefix + nodeConnection + nodeName);
		List<T> children = getChildrenFunc.apply(node);
		for (int i = 0; i < children.size(); i++) {
			String newPrefix = prefix + (isTail ? "    " : "│   ");
			printTreeRec(newPrefix, children.get(i), getChildrenFunc, i == children.size()-1);
		}
	}
}
