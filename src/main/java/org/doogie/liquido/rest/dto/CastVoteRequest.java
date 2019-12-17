package org.doogie.liquido.rest.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.NonNull;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.rest.deserializer.LawModelDeserializer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Data Transfer Object (DTO) that can hold the payload for a vote that a user wants to cast.
 * POST /castVote
 */
@Data
public class CastVoteRequest {
	/** a poll */
	@NonNull
	PollModel poll;

	/** Ordered list of URIs, one for each proposal in VOTING. */
	@NonNull
	@JsonDeserialize(contentUsing = LawModelDeserializer.class)   // deserialize list elements with this class
	List<LawModel> voteOrder;

	/**
	 * The voter's own voterToken that MUST hash to a valid checksumModel. */
	@NonNull
	String voterToken;

	@Override
	public String toString() {
		String proposalIds = voteOrder.stream().map(law->law.getId().toString()).collect(Collectors.joining(","));   // I love java :-) <sarcasm>   In PERL this would just simply be     $buf = join(",", @arr);
		StringBuilder buf = new StringBuilder();
		buf.append("CastVoteRequest[");
		buf.append("poll="+poll);
		buf.append(", voteOrder(proposalIds)=[" + proposalIds +"]");
		//do not expose secret voterToken in toString!
		return buf.toString();
	}
}
