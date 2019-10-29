package org.doogie.liquido.services.voting;

import lombok.NonNull;
import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.util.Matrix;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Ranked Pairs voting
 * https://en.wikipedia.org/wiki/Ranked_pairs
 *
 * Adapted from https://gist.github.com/asafh/a8e9af7a3e5282cbba27
 */
public class RankedPairVoting {

	/**
	 * Calculate the winning proposal(s) with the Ranked Pairs voting method.
	 * 1. TALLY -   For each pair of proposals in the poll calculate the winner of the direct comparison
	 *              Which proposal has more preferences i&lt;j compared to j&gt;i.
	 * 2. SORT -    Sort these majorities by the number of preferences i over j
	 * 3. LOCK IN - For each of the sorted majorities: add the majority to a directed graph,
	 *              IF this edge does not introduce a circle in the graph.
	 * 4. WINNERS - The source of the tree, ie. the node with no incoming links is the winner of the poll.
	 *              Unless there is a pairwise tie between two sources, then there will only be one winner.
	 * @param poll a poll that finished his voting phase
	 * @return Thw winner of the poll. Unless there is a pairwise tie, then this list will always only contain one winner.
	 * @throws LiquidoException in the exceptional case when there is more than one winner in the poll
	 */
	public static LawModel calcRankedPairsWinner(PollModel poll, Matrix duelMatrix) throws LiquidoException {

		// TALLY
		// one "majority" := (p1,p2) -> n   how often(n) was proposal p1 preferred over proposal p2
		List<int[]> majorities = new ArrayList<>();
		for (int i = 0; i < duelMatrix.getRows()-1; i++) {
			for (int j = i+1; j < duelMatrix.getCols(); j++) {
				int[] maj_ij = new int[] {i,j, duelMatrix.get(i,j)};
				int[] maj_ji = new int[] {j,i, duelMatrix.get(j,i)};
				if (maj_ij[2] != maj_ji[2]) {
					majorities.add(maj_ij[2] > maj_ji[2] ? maj_ij : maj_ji);   // add the winner of this pair to the list of majorities (if there is a winner)
				}
			}
		}

		// SORT   majorities
		majorities.sort(new MajorityComparator(duelMatrix));

		// LOCK IN
		DirectedGraph digraph = new DirectedGraph();
		for (int[] majority : majorities) {
			if (!digraph.reachable(majority[1], majority[0])) {
				digraph.addDirectedEdge(majority[0], majority[1]);
			}
		}

		// WINNERS
		Set<Integer> sourceIndexes = digraph.getSources();
		if (sourceIndexes.size() > 1) throw new LiquidoException(LiquidoException.Errors.CANNOT_CALCULATE_UNIQUE_RANKED_PAIR_WINNER, "Cannot calculate exactly one Ranked Pair winner.");
		int winnerIndex = sourceIndexes.iterator().next();
		int loopIndex = 0;
		for(LawModel prop : poll.getProposals()) {
			if (winnerIndex == loopIndex) return prop;
			loopIndex++;
		}

		throw new RuntimeException("This is mathematically impossible! :-(");
	}





	/**
	 * Calculate the matrix of votes that prefer proposal i over proposal j for every i <= j
	 * (Of course this matrix is mirror-symmetric along its diagonal.)
	 * @param poll a poll where voting was just finished
	 * @return a two dimensional Matrix that contains the number of voters that prefer i over j
	 */
	public static Matrix calcDuelMatrix(@NonNull PollModel poll, @NonNull List<BallotModel> ballots) {
		// Map proposals in the poll to index numbers
		//TODO: the indexes in the returned Matrix depend on the NOT GUARANTEED order of proposals in the poll. Is that a problem? Maybe not as long as the same poll is in memory. Needs to be checked!
		Map<LawModel, Integer> proposalIdx = new HashMap<>();
		int proposalIndex = 0;
		for(LawModel prop : poll.getProposals()) {
			proposalIdx.put(prop, proposalIndex++);
		}

		//----- Fill the duelMatrix that stores the number of preferences i > j
		int n = poll.getNumCompetingProposals();
		Matrix duelMatrix = new Matrix(n, n);

		// Loop over each ballot
		for(BallotModel ballot : ballots) {
			List<LawModel> voteOrder = ballot.getVoteOrder();

			//TODO: optimize:  the notVotedForIndexes can be cached per ballot.voteOrder

			// indexes of proposals in poll that this ballot has NOT voted for at all.
			List<Integer> notVotedForIndexes = poll.getProposals().stream()
				.filter(prop -> !voteOrder.contains(prop))
				.map(prop -> proposalIdx.get(prop))
				.collect(Collectors.toList());

			// for each pair of proposals in the ballot's voteOrder add one preference i>j to the duelMatrix
			for (int i = 0; i < voteOrder.size(); i++) {
				int prefIdx = proposalIdx.get(voteOrder.get(i));
				for (int j = i+1; j < voteOrder.size(); j++) {
					// add a preference i>j
					int lessIdx = proposalIdx.get(voteOrder.get(j));
					duelMatrix.add(prefIdx, lessIdx, 1);
				}
				// and add a preference i>k for each proposal k that was not voted for at all in this voteOrder
				// the notVotedFor proposals do not have any preferences among themselves. They are all just simply "lower".
				for (int k = 0; k < notVotedForIndexes.size(); k++) {
					duelMatrix.add(prefIdx, notVotedForIndexes.get(k), 1);
				}
			}
		}
		return  duelMatrix;
	}


	// This new approach DOES NOT rely on the order of proposals in the poll  (which is a Set!!)
	// Instead it maps preferences (= Pair of proposal Ids) to their counts.



	//TODO: it doesn't  matter if you first sum up equal voteOrders and then calculate preferences
	//      OR if you loop through each individual ballot and that way count every single preference step by step.
	//      Its the same amount of operations!  But that way we can only work with IDs.
	//      => Sorting should happen AFTER the preferences have been summed up.
	public static Long calcRankedPairWinner2(@NonNull List<Long> allIds, @NonNull List<BallotModel> ballots) throws LiquidoException {

		// Count how many time each voteOrder appears in the ballots.
		// The keys in this map are the ordered list of proposal ids of a ballot's voteOrder.
		// Keep in mind that these keys may be of different length.
		// E.g.  10 voters may have voted A > B > C > D  and 20 voters may have voted C > A > B
		Map<List<Long>, Long> voteOrderCount = new HashMap<>();

		// Map voteOrders to proposal.ids that have not been voted for in that ballot
		// E.g. for voteOrder A > B  the notVotedFor Set is {C, D}
		Map<List<Long>, Set<Long>> notVotedForMap = new HashMap<>();

		// For each ballot
		for (BallotModel ballot: ballots) {
			// map the voteOrder in this ballot to an ordered list of proposals ids that the user votedFor
			List<Long> votedForIds = ballot.getVoteOrder().stream().map(prop -> prop.getId()).collect(Collectors.toList());

			// increment the counter for this votedForIds
			Long count = voteOrderCount.getOrDefault(votedForIds, 0L);
			voteOrderCount.put(votedForIds, count+1);

			// calc and cache the proposal ids that were not for at all in this type of ballot
			if (notVotedForMap.get(votedForIds) == null) {
				Set<Long> notVotedForIds = allIds.stream().filter(id -> !votedForIds.contains(id)).collect(Collectors.toSet());
				notVotedForMap.put(votedForIds, notVotedForIds);
			}
		}
		long winnerId = calcPreferences(voteOrderCount, notVotedForMap);

		return winnerId;
	}

	public static long calcPreferences(Map<List<Long>, Long> voteOrderCount, Map<List<Long>, Set<Long>> notVotedForMap) throws LiquidoException {
		// ---------- from here on down we are not using any Liquido specific Models anymore. It's just IDs

		// TALLY

		// For each ballot sum up the pairwise preferences how often proposal_1.id was preferred over proposal_2.id
		// "preferences" is actually a table with proposal.ids as names for rows and cols
		Map<Pair<Long>, Long> preferences = new HashMap<>();
		for (List<Long> votedForIds : voteOrderCount.keySet()) {
			Long count = voteOrderCount.get(votedForIds);
			// for each pair of voteOrderIds  (but only once for each pair:  i < j)
			for (int i = 0; i < votedForIds.size()-1; i++) {
				for (int j = i + 1; j < votedForIds.size(); j++) {
					// add count preferences id[i] > id[j]
					inc(preferences, votedForIds.get(i), votedForIds.get(j), count);
				}
				// and add one preference id[i] > id[k]  for  each notVotedForId[k]
				for (Long notVotedForId : notVotedForMap.get(votedForIds)) {
					inc(preferences, votedForIds.get(i), notVotedForId, count);
				}
			}
		}

		// SORT
		// https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values
		List<Map.Entry<Pair<Long>, Long>> majorities =
			preferences.entrySet().stream()
				.sorted(Collections.reverseOrder(	Map.Entry.comparingByValue())	)
				.collect(Collectors.toList());

		System.out.println("======= sorted");
		for(Map.Entry<Pair<Long>, Long> mapEntry : majorities) {
			System.out.println(mapEntry.getKey() + "=> "+mapEntry.getValue());
		}

		// LOCK IN
		DirectedGraph<Long> digraph = new DirectedGraph();
		for (Map.Entry<Pair<Long>, Long> majority : majorities) {
			if (!digraph.reachable(majority.getKey().getVal2(), majority.getKey().getVal1())) {
				digraph.addDirectedEdge(majority.getKey().getVal1(), majority.getKey().getVal2());
			}
		}

		// WINNER(s)
		Set<Long> sourceIds = digraph.getSources();
		if (sourceIds.size() > 1) throw new LiquidoException(LiquidoException.Errors.CANNOT_CALCULATE_UNIQUE_RANKED_PAIR_WINNER, "Cannot calculate exactly one Ranked Pair winner.");
		return sourceIds.iterator().next();
	}

	private static void inc(Map<Pair<Long>, Long> preferences, long id1, long id2, Long count) {
		Pair<Long> key = new Pair<>(id1, id2);
		Long numVotes = preferences.getOrDefault(key, 0L);
		preferences.put(key, numVotes + count);
	}

}