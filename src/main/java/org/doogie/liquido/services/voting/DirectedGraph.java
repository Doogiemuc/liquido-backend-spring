package org.doogie.liquido.services.voting;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A directed graph with nodes. (Without node weights)
 * Each node has an ID of type T (e.g. Long).
 * Each node has a set of outgoing links to other nodes.
 */
class DirectedGraph<T> extends HashMap<T, Set<T>> {
	public DirectedGraph() {}

	/**
	 * add an edge from a node to another node
	 */
	public boolean addDirectedEdge(T from, T to) {
		if (Objects.equals(from, to)) throw new IllegalArgumentException("cannot add a circular edge from a node to itself");
		if (this.get(from) == null) this.put(from, new HashSet<>());  // lazily create HashSet
		return this.get(from).add(to);
	}

	/**
	 * @return true if there is a path from node A to node B along the directed edges
	 */
	public boolean reachable(T from, T to) {
		Set<T> neighbors = this.get(from);
		if (neighbors == null) return false;        // from is a leave
		if (neighbors.contains(to)) return true;    // to can be directly reached as a neighbor
		for (T neighbor : neighbors) {              // recursively check from all neighbors
			if (this.reachable(neighbor, to)) return true;
		}
		return false;
	}

	/**
	 * A "source" is a node that is not reachable from any other node.
	 *
	 * @return all sources, ie. nodes with no incoming links.
	 */
	public Set<T> getSources() {
		Set<T> sources = new HashSet(this.keySet());   // clone! of all nodes in this graph
		for (T nodeKey : this.keySet()) {
			Set<T> neighbors = this.get(nodeKey);
			for (T neighbor : neighbors) sources.remove(neighbor);
		}
		return sources;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("DirectedGraph[");
		Iterator<T> it = this.keySet().iterator();
		while (it.hasNext()) {
			T key = it.next();
			sb.append("[" + key + "->[");
			String neighborIDs = this.get(key).stream().map(id -> id.toString()).collect(Collectors.joining(","));
			sb.append(neighborIDs);
			sb.append("]");
			if (it.hasNext()) sb.append(", ");
		}
		//if (this.keySet().limit() > 0) sb.delete(sb.length()-2, sb.length()-1);
		sb.append("]");
		return sb.toString();
	}
}