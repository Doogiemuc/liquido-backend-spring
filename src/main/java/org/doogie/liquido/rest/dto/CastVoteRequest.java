package org.doogie.liquido.rest.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.rest.deserializer.PollModelDeserializer;

import java.util.List;

/**
 * Data Transfer Object (DTO) that can hold the payload for a vote that a user wants to cast.
 * POST /castVote
 */
@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class CastVoteRequest {
	/** a poll */
	@NonNull
	@JsonDeserialize(using = PollModelDeserializer.class)
	PollModel poll;

	/** Ordered list of IDs, one for each proposal in VOTING. */
	@NonNull
	//@JsonDeserialize(contentUsing = LawModelDeserializer.class)   		// deserialize list elements with this class
	List<Long> voteOrderIds;

	/**
	 * The voter's own voterToken that MUST hash to a valid checksumModel. */
	@NonNull
	String voterToken;

	@Override
	public String toString() {
		String proposalIds = String.join(",", voteOrderIds.toString());
		//String proposalIds = voteOrder.stream().collect(Collectors.joining(","));   // I love java :-) <sarcasm>   In PERL this would just simply be     $buf = join(",", @arr);
		StringBuilder buf = new StringBuilder();
		buf.append("CastVoteRequest[");
		buf.append("poll="+poll);
		buf.append(", voteOrder(proposalIds)=[" + proposalIds +"]");
		//do not expose secret voterToken in toString!
		return buf.toString();
	}
}
