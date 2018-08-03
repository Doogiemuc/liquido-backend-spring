package org.doogie.liquido.rest.dto;

import lombok.Data;
import lombok.NonNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Data Transfero Object (DTO) that can hold the payload for a vote that a user wants to cast.
 * POST /castVote
 */
@Data
public class CastVoteRequest {
	@NonNull
	String poll;

	@NonNull
	List<String> voteOrder;

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
		return buf.toString();
	}
}
