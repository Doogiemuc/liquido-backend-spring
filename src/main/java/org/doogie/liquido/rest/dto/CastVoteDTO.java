package org.doogie.liquido.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Data Transfero Object (DTO) that can hold the payload for a vote that a user wants to cast.
 * POST /castVote
 */
@Data
@AllArgsConstructor
public class CastVoteDTO {
	@NonNull
	PollModel poll;

	@NonNull
	List<LawModel> voteOrder;

	@NonNull
	String voterToken;

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("CastVoteDTO[");
		buf.append("poll.id="+poll.id);
		buf.append(", voteOrder=[");
		buf.append(voteOrder.stream().map(Object::toString).collect(Collectors.joining(",")));    // I love java :-) <sarcasm>   In PERL this would just simply be     $buf = join(",", @arr);
		buf.append("]");
		return buf.toString();
	}
}
