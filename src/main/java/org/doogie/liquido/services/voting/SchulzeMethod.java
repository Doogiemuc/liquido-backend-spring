package org.doogie.liquido.services.voting;

import lombok.NonNull;
import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.util.LongMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * dapted from https://github.com/zephyr/schulze-voting/blob/master/src/java/dh/p/schulze/Election.java
 */
public class SchulzeMethod {

	/**
	 * The strongest path from candidate ("proposal") A to B is the path with the best weakest link.
	 * "A chain can only be so strong as its weakest link."
	 * @param poll a poll where voting has just finished
	 * @return a two dimensional LongMatrix that contains the strength of the weakest links for each stongest path
	 */
	public static LongMatrix calcStrongestPathMatrix(@NonNull PollModel poll, List<BallotModel> ballots) {
		LongMatrix d = RankedPairVoting.calcDuelMatrix(poll, ballots);										// number of preferences i over j
		LongMatrix p = new LongMatrix(d.getRows(), d.getCols());		// strongest path matrix
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
		LongMatrix p = SchulzeMethod.calcStrongestPathMatrix(poll, ballots);
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
