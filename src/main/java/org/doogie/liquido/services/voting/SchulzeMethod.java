package org.doogie.liquido.services.voting;

import lombok.NonNull;
import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.BaseModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.util.Matrix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * dapted from https://github.com/zephyr/schulze-voting/blob/master/src/java/dh/p/schulze/Election.java
 */
public class SchulzeMethod {

	/**
	 * The strongest path from candidate ("proposal") A to B is the path with the best weakest link.
	 * "A chain can only be so strong as its weakest link."
	 * @param poll a poll where voting has just finished
	 * @return a two dimensional Matrix that contains the strength of the weakest links for each stongest path
	 */
	public static Matrix calcStrongestPathMatrix(@NonNull PollModel poll, List<BallotModel> ballots) {
		// Ordered list of proposal IDs in poll.  (Keep in mind that the proposal in a poll are not ordered.)
		List<Long> allIds = poll.getProposals().stream().map(p -> p.getId()).collect(Collectors.toList());

		// map the vote order of each ballot to a List of ids
		List<List<Long>> idsInBallots = ballots.stream().map(
				ballot -> ballot.getVoteOrder().stream().map(BaseModel::getId).collect(Collectors.toList())
		).collect(Collectors.toList());


		Matrix d = RankedPairVoting.calcDuelMatrix(allIds, idsInBallots);										// number of preferences i over j
		Matrix p = new Matrix(d.getRows(), d.getCols());		// strongest path matrix
		int C = poll.getNumCompetingProposals();

		for (int i = 0; i < C; i++) {
			for (int j = 0; j < C; j++) {
				if (i != j) {
					p.set(i, j, (d.get(i, j) > d.get(j, i)) ? d.get(i, j) : 0);
				}
			}
		}

		for (int i = 0; i < C; i++) {
			for (int j = 0; j < C; j++) {
				if (i != j) {
					for (int k = 0; k < C; k++) {
						if ((i != k) && (j != k)) {
							p.set(j, k, Math.max(p.get(j, k), Math.min(p.get(j,i), p.get(i, k))));
						}
					}
				}
			}
		}
		return p;
	}

	/**
	 * Calculate the list of potential winners of this poll.
	 * @param poll a poll where the voting phase is finished
	 * @return the list of potential winners by the Schulze Method
	 */
	public static List<LawModel> calcSchulzeMethodWinners(@NonNull PollModel poll, @NonNull List<BallotModel> ballots) {
		List<LawModel> winnerList = new ArrayList<>();
		Matrix p = SchulzeMethod.calcStrongestPathMatrix(poll, ballots);
		int C = poll.getNumCompetingProposals();

		int i = 0;
		for (LawModel proposal : poll.getProposals()) {
			boolean b = true;
			for (int j = 0; j < C; j++) {
				if (i != j) {
					b = b && (p.get(i, j) >= p.get(j, i));
				}
			}
			if (b) winnerList.add(proposal);
			i++;
		}
		return winnerList;
	}
}
