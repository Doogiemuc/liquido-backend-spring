package org.doogie.liquido.services.voting;

import lombok.NonNull;
import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.services.voting.DirectedGraph;
import org.doogie.liquido.services.voting.MajorityComparator;
import org.doogie.liquido.util.Matrix;

import java.util.*;

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
	 * @param poll a poll that finished his voting phase
	 * @return the sorted list of winners. Best winner to least.
	 */
	public static List<LawModel> calcRankedPairsWinners(PollModel poll, Matrix duelMatrix) {
		// TALLY
		// one "majority"       := how often was candidate i preferred over j
		// list of "majorities" := sorted list of majorities i>j  with n votes
		List<int[]> majorities = new ArrayList<>();
		for (int i = 0; i < duelMatrix.getRows()-1; i++) {
			for (int j = i+1; j < duelMatrix.getCols(); j++) {
				int[] maj_ij = new int[] {i,j, duelMatrix.get(i,j)};
				int[] maj_ji = new int[] {j,i, duelMatrix.get(j,i)};
				majorities.add(maj_ij[2] > maj_ji[2] ? maj_ij : maj_ji);   // add the winner of this pair to the list of majorities
			}
		}

		// SORT
		majorities.sort(new MajorityComparator(duelMatrix));

		// LOCK IN
		DirectedGraph digraph = new DirectedGraph();
		for (int[] majority : majorities) {
			if (!digraph.reachable(majority[1], majority[0])) {
				digraph.addDirectedEdge(majority[0], majority[1]);
			}
		}

		// WINNERS
		Set<Integer> sourceIds = digraph.getSources();
		List<LawModel> winningProposals = new ArrayList<>();
		int i = 0;
		for(LawModel prop : poll.getProposals()) {
			if (sourceIds.contains(i)) winningProposals.add(prop);  i++;
		}

		return winningProposals;
	}



	/**
	 * Calculate the number of voters that prefer proposal i over proposal j for every i != j
	 * @param poll a poll where voting was just finished
	 * @return a two dimensional Matrix that contains the number of voters that prefer i over j
	 */
	public static Matrix calcDuelMatrixBAK(@NonNull PollModel poll, @NonNull List<BallotModel> ballots) {
		// First of all each proposal in the poll gets an array index (poll.proposals is a SortedSet)
		// Map: proposal -> array index of this proposal
		Map<LawModel, Integer> proposalIdx = new HashMap<>();
		int proposalIndex = 0;
		for(LawModel prop : poll.getProposals()) {
			proposalIdx.put(prop, proposalIndex++);
		}

		// For each different voteOrder that appears in all ballots we now fill three maps.
		// The map's key is always the unique hashCode of that voteOrder.

		// number of ballots with this specific vote order. Mapped by hashCode of voteOrder
		Map<Integer, Integer> numberOfBallots = new HashMap<>();

		// proposals that have been voted for. Each array contains indexes of proposals (from proposalIdx)
		Map<Integer, Integer[]> votedForMap = new HashMap<>();

		// proposals that nave NOT been voted for at all in this voteOrder.
		Map<Integer, Integer[]> notVotedForMap = new HashMap<>();

		//----- collect all ballots into buckets by their voteOrder
		for (BallotModel ballot : ballots) {
			int key = ballot.getVoteOrder().hashCode();
			Integer numBallots = numberOfBallots.get(key);
			// when a voteOrder appears for the first time, then calc voteForIndexes and notVotedForIndexes
			if (numBallots == null) {
				numberOfBallots.put(key, 1);

				// array of proposal indexes for the proposals in this voteOrder
				Integer[] votedForIndexes = ballot.getVoteOrder().stream().map(p -> proposalIdx.get(p)).toArray(Integer[]::new);
				votedForMap.put(key, votedForIndexes);

				// array of proposal indexes for the proposals that were not voted for at all in this ballot
				Set<LawModel> notVotedFor = new HashSet<>(poll.getProposals());
				notVotedFor.removeAll(ballot.getVoteOrder());
				Integer[] notVotedForIndexes = notVotedFor.stream().map(p -> proposalIdx.get(p)).toArray(Integer[]::new);
				notVotedForMap.put(key, notVotedForIndexes);
			} else {
				numberOfBallots.put(key, numBallots+1);
			}
		}

		//----- Fill the duelMatrix that stores the number of preferences i > j
		int n = poll.getNumCompetingProposals();
		Matrix duelMatrix = new Matrix(n, n);

		// for each type of voteOrder that was casted
		for(Integer key : votedForMap.keySet()) {
			Integer[] idx = votedForMap.get(key);
			// for each pair candidate i > candidate j in the voteOrder of these ballots
			for (int i = 0; i < idx.length-1; i++) {
				for (int j = i+1; j < idx.length; j++) {
					// add a preference i>j for that number of ballots
					duelMatrix.add(idx[i], idx[j], numberOfBallots.get(key));
				}
				// and add a preference i>k for each proposal k that was not voted for at all in this voteOrder
				for (int k = 0; k < notVotedForMap.get(key).length; k++) {
					duelMatrix.add(idx[i], notVotedForMap.get(key)[k], numberOfBallots.get(key));
				}
			}
		}
		return  duelMatrix;
	}

	public static Matrix calcDuelMatrix(@NonNull PollModel poll, @NonNull List<BallotModel> ballots) {
		// First of all each proposal in the poll gets an array index (poll.proposals is a SortedSet for very complex reasons ... :-)  )
		// Map: proposal -> array index of this proposal
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

			List<Integer> notVotedForIndexes = new ArrayList(poll.getProposals());
			notVotedForIndexes.removeAll(voteOrder);     // indexes of proposals in poll that this ballot has NOT voted for at all.

			// for each pair of proposals in the ballot's voteOrder add one preference i>j to the duelMatrix
			for (int i = 0; i < voteOrder.size(); i++) {
				for (int j = i+1; j < voteOrder.size(); j++) {
					// add a preference i>j
					duelMatrix.add(i, j, 1);
				}
				// and add a preference i>k for each proposal k that was not voted for at all in this voteOrder
				// the notVotedFor proposals do not have any preferences among themselves. They are all just simply "lower".
				for (int k = 0; k < notVotedForIndexes.size(); k++) {
					duelMatrix.add(i, notVotedForIndexes.get(k), 1);
				}
			}
		}
		return  duelMatrix;
	}

}