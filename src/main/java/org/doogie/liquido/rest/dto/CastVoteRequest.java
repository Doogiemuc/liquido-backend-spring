package org.doogie.liquido.rest.dto;

import lombok.Data;
import lombok.NonNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Data Transfer Object (DTO) that can hold the payload for a vote that a user wants to cast.
 * POST /castVote
 */
@Data
public class CastVoteRequest {
	/** URI of a poll */
	@NonNull
	String poll;

	/** Ordered list of URIs, one for each proposal in VOTING. */
	@NonNull
	List<String> voteOrder;

	/**
	 * The voter's own voterToken that MUST hash to a valid checksumModel. */
	@NonNull
	String voterToken;

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("CastVoteRequest[");
		buf.append("poll="+poll);
		buf.append(", voteOrder=[");
		buf.append(voteOrder.stream().collect(Collectors.joining(",")));    // I love java :-) <sarcasm>   In PERL this would just simply be     $buf = join(",", @arr);
		buf.append("]");
		//do not expose secret voterToken in toString!
		return buf.toString();
	}
}
