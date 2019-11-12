package org.doogie.liquido.services.voting;

//Implementation note: This class is completely independent of any Liquido data model. It's just the algorithm

import org.doogie.liquido.util.Matrix;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Ranked Pairs voting
 * Ranked pairs (RP) or the Tideman method is an electoral system developed in 1987 by Nicolaus Tideman that selects a single winner using votes that express preferences.
 * If there is a candidate who is preferred over the other candidates, when compared in turn with each of the others, RP guarantees that candidate will win. Because of this property, RP is, by definition, a Condorcet method.
 * https://en.wikipedia.org/wiki/Ranked_pairs
 *
 * The code here is adapted from the JavaScript at https://gist.github.com/asafh/a8e9af7a3e5282cbba27
 */
public class RankedPairVoting {



	/**
	 * Sum up the pairwise comparision of proposals/candidates in every ballot.
	 * How many ballots prefere candidate i over candidate j.
	 * The returned table is "sum symetric" along its diagonal axis. The sum of each pair of cells
	 * along this symetry equals the number of ballots.
	 *
	 * @param allIds all proposal/candidate IDs that can be voted for in this poll.
	 * @param idsInBallots the list of ballots. Each ballot consists of an ordered list of proposal/candidate IDs from allIds
	 *                     A ballot does not necessarily need to contain all candidate IDs. It is also possible that
	 *                     a voter only votes for some of the candidates. This means, that he preferes all the candidates
	 *                     that he sorted into his ballot over the candidates that hi did not vote for at all.
	 *                     <h3>Example</h3>
	 *                     <ul>
	 *                       <li>allIDs = {1,2,3,4}</li>
	 *                     	 <li>idsInBallots[0] = { 2, 1}</li>
	 *                     </ul>
	 *                     This voter prefers candidate 2 over 1. And he prefers these two candidates over all the other ones
	 *                     [ 2, 1 ] > { 3 4}   The candidates 3 and 4 do not have any preferred order within them. They
	 *                     are all just "low".
	 * @throws IllegalArgumentException when one of the required param is null
	 * @return the duelMatrix, which is a pairwise comparision of each preference i > j
	 */
	public static Matrix calcDuelMatrix(List<Long> allIds, List<List<Long>> idsInBallots)  {
		if (allIds == null || idsInBallots == null)
			throw new IllegalArgumentException("id2index and idsInBallots params must not be null!");

		// reverse map IDs to their index in allIds, so that we can use these indexes as row and col numbers in the duelMatrix
		HashMap<Long, Integer> id2index = new HashMap<>();
		int index = 0;
		for (Long id : allIds) {
			id2index.put(id, index++);
		}

		// When there are many ballots, then some will have the same vote order.
		// This cache stores the Set of IDs that have NOT been voted for in a ballot.
		// E.g. for voteOrder A > B  the notVotedFor Set is {C, D}
		Map<List<Long>, Set<Long>> notVotedForMap = new HashMap<>();

		// DuelMatrix is a pairwise comparision of preferences proposal1.id > proposal2.id
		// Proposal IDs are mapped to row/col index in duelMatrix via the passed id2index map.
		Matrix duelMatrix = new Matrix(id2index.size(), id2index.size());

		// For each ballot
		for (List<Long> votedForIds : idsInBallots) {

			// calc (and cache) the proposal ids that were not voted for at all for this voteOrder
			Set<Long> notVotedForIds = notVotedForMap.get(votedForIds);
			if (notVotedForIds == null) {
				notVotedForIds = id2index.keySet().stream().filter(id -> !votedForIds.contains(id)).collect(Collectors.toSet());
				notVotedForMap.put(votedForIds, notVotedForIds);
			}

			// For each pair of votedForIds add one to the cell in the duelMatrix
			for (int i = 0; i < votedForIds.size(); i++) {					//BUGFIX: Must loop over all votedForIds. We could skip the last for preference i > j, but I need this last loop iteration for counting the preferences over notVotedForIds
				int prefIndex = id2index.get(votedForIds.get(i));
				for (int j = i + 1; j < votedForIds.size(); j++) {
					// add count preferences i > j
					duelMatrix.add(prefIndex, id2index.get(votedForIds.get(j)), 1);
				}
				// AND add one preference i > k  for  each notVotedForId[k]
				// the notVotedForIds among themselves do not have any preference. They are all just simply "lower" than the votedForIds.
				for (Long notVotedForId : notVotedForIds) {
					duelMatrix.add(prefIndex, id2index.get(notVotedForId), 1);
				}
			}

		}

		return duelMatrix;
	}



	/**
	 * Calculate the winner of the Ranked Pairs voting method.
	 * 1. TALLY -   For each pair of proposals in the poll calculate the winner of the direct comparison
	 *              Which proposal has more preferences i&lt;j compared to j&gt;i.
	 * 2. SORT -    Sort these majorities by the number of preferences i over j
	 * 3. LOCK IN - For each of the sorted majorities: add the majority to a directed graph,
	 *              IF this edge does not introduce a circle in the graph.
	 * 4. WINNERS - The source of the tree, ie. the node with no incoming links is the winner of the poll.
	 *              Unless there is a pairwise tie between two sources, then there will only be one winner.
	 * @return The (list of) winners of the poll as row/col indexes in duelMatrix.
	 *         In nearly every case, there is only one winner.
	 */
	public static List<Integer> calcRankedPairWinners(Matrix duelMatrix) {
		// TALLY
		// Majority  :=  [i,j,n]  where
		//   i  row index in duelMatrix  (actually an int)
		//   j  col index in duelMatrix
		//   n  number of ballots that prefer i > j   (a long)
		// This list of majorities contains each pair only once, where i is the winner.
		List<long[]> majorities = new ArrayList<>();
		for (int i = 0; i < duelMatrix.getRows()-1; i++) {
			for (int j = i+1; j < duelMatrix.getCols(); j++) {
				long[] maj_ij = new long[] {i,j, duelMatrix.get(i,j)};
				long[] maj_ji = new long[] {j,i, duelMatrix.get(j,i)};
				if (maj_ij[2] != maj_ji[2]) {
					majorities.add(maj_ij[2] > maj_ji[2] ? maj_ij : maj_ji);   // add the winner of this pair to the list of majorities (if there is a winner)
				}
			}
		}

		// SORT  majorities
		// https://en.wikipedia.org/wiki/Ranked_pairs#Sort
		majorities.sort(new MajorityComparator(duelMatrix));

		// LOCK IN
		// The node ids in DirectedGraph are row/col indexes in the duelMatrix (int)
		DirectedGraph<Integer> digraph = new DirectedGraph<>();
		for (long[] majority : majorities) {
			if (!digraph.reachable((int)majority[1], (int)majority[0])) {
				digraph.addDirectedEdge((int)majority[0], (int)majority[1]);
			}
		}

		// WINNERS
		// In nearly every case, there is only one winner/one source.
		Set<Integer> sources = digraph.getSources();
		List<Integer> winningRowIndexes = new ArrayList(sources);
		// TODO: Sort Ranked Pair winners, if there is more than one winner;
		return winningRowIndexes;
	}

}