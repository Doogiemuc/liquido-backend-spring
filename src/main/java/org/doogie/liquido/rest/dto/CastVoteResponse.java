package org.doogie.liquido.rest.dto;

import lombok.Data;
import lombok.NonNull;
import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.PollModel;

/**
 * DTO for the response to a castVote request
 */
@Data
public class CastVoteResponse {
	/** The casted ballot, includes its level, checksum and link to poll */
	@NonNull
	BallotModel ballot;

	/**
	 * For how many delegees was this ballot casted.
	 * Some delegees may have already voted for themselves. Then voteCount is smaller
	 * then delegationCount.
	 */
	@NonNull
	Long voteCount;
}
