package org.doogie.liquido.services.voting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A directed graph with nodes. (Without node weights)
 */
class DirectedGraph extends HashMap<Integer, Set<Integer>> {
	public DirectedGraph() {}

	/**
	 * add an edge from a node to another node
	 */
	public boolean addDirectedEdge(int from, int to) {
		if (from == to) throw new IllegalArgumentException("cannot add a circular edge from a node to itself");
		if (this.get(from) == null) this.put(from, new HashSet<Integer>());  // lazily create HashSet
		return this.get(from).add(to);
	}

	/**
	 * @return true if there is a path from node A to node B along the directed edges
	 */
	public boolean reachable(int from, int to) {
		Set<Integer> neighbors = this.get(from);
		if (neighbors == null) return false;        // from is a leave
		if (neighbors.contains(to)) return true;    // to can be directly reached as a neighbor
		for (int neighbor : neighbors) {              // recursively check from all neighbors
			if (this.reachable(neighbor, to)) return true;
		}
		return false;
	}

	/**
	 * A "source" is a node that is not reachable from any other node.
	 *
	 * @return all sources, ie. nodes with no incoming links.
	 */
	public Set<Integer> getSources() {
		Set<Integer> sources = new HashSet(this.keySet());   // clone! of all nodes in this graph
		for (int nodeKey : this.keySet()) {
			Set<Integer> neighbors = this.get(nodeKey);
			for (int neighbor : neighbors) sources.remove(neighbor);
		}
		return sources;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("DirectedGraph[");
		Iterator<Integer> it = this.keySet().iterator();
		while (it.hasNext()) {
			Integer key = it.next();
			sb.append("[" + key + "->[");
			String neighborIDs = this.get(key).stream().map(id -> String.valueOf(id)).collect(Collectors.joining(","));
			sb.append(neighborIDs);
			sb.append("]");
			if (it.hasNext()) sb.append(", ");
		}
		//if (this.keySet().limit() > 0) sb.delete(sb.length()-2, sb.length()-1);
		sb.append("]");
		return sb.toString();
	}
}