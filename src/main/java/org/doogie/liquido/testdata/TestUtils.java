package org.doogie.liquido.testdata;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.ChecksumRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.ChecksumModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TestUtils {

	@Autowired
	DelegationRepo delegationRepo;

	@Autowired
	ChecksumRepo checksumRepo;

	public void printDelegationTree(AreaModel area, UserModel proxy) {
		Function<UserModel, List<UserModel>> getChildrenFunc = toProxy -> delegationRepo.findByAreaAndToProxy(area, toProxy)
				.stream().map(del -> del.getFromUser()).collect(Collectors.toList());
		this.printTreeRec("", proxy, getChildrenFunc, true);
	}


	public void printChecksumTree(ChecksumModel checksum) {
		Function<ChecksumModel, List<ChecksumModel>> getChildrenFunc = c -> checksumRepo.findByDelegatedTo(c);
		this.printTreeRec("", checksum, getChildrenFunc, true);
	}

	/**
	 * Print a tree structure in a pretty ASCII fromat.
	 *
	 * I LOVE stackoverflow :-)  https://stackoverflow.com/questions/4965335/how-to-print-binary-tree-diagram
	 *
	 * @param prefix Currnet previx. Use "" in initial call!
	 * @param node The current node. Pass the root node of your tree in initial call.
	 * @param getChildrenFunc A {@link Function} that returns the children of a given node.
	 * @param isTail Is node the last of its sibblings. Use false in initial call. (This is needed for pretty printing.)
	 * @param <T> The type of your nodes. Anything that has a toString can be used.
	 */
	private static <T> void printTreeRec(String prefix, T node, Function<T, List<T>> getChildrenFunc, boolean isTail) {
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
