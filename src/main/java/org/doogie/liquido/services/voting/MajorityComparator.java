package org.doogie.liquido.services.voting;


import lombok.NonNull;
import org.doogie.liquido.util.LongMatrix;

import java.util.Comparator;

/**
 * Compare two majorities which one is "better" and wins.
 * A Majority is how often a candidate i was preferred to candidate j.
 * int[3] = { i, j, numPreferences_I_over_J }
 *
 */
class MajorityComparator implements Comparator<long[]> {
	LongMatrix duelMatrix;

	public MajorityComparator(@NonNull LongMatrix duelMatrix) {
		this.duelMatrix = duelMatrix;
	}

	/**
	 * Compare two majorities m1 and m2
	 * (1) The majority having more support for its alternative is ranked first.
	 * (2) Where the majorities are equal, the majority with the smaller minority opposition is ranked first.
	 *
	 * @param m1 majority one
	 * @param m2 majority two
	 * @return a negative number IF m1 < m2   OR
	 * a positive number IF m2 > m1
	 */
	@Override
	public int compare(long[] m1, long[] m2) {
		if (m1 == null && m2 == null) return 0;
		if (m1.equals(m2)) return 0;
		if (m1 == null) return -1;
		if (m2 == null) return 1;
		int diff = (int)(m2[2] - m1[2]);  // (1)
		if (diff == 0) {
			return duelMatrix.get((int)m2[1], (int)m2[0]) - duelMatrix.get((int)m1[1], (int)m1[0]);  // (2)
		} else {
			return diff;
		}
	}
}