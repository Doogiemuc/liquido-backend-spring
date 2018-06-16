package org.doogie.liquido.rest;

import lombok.Data;
import lombok.NonNull;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;

/**
 * HTTP request payload data for joining a proposal into a poll
 */
@Data
public class JoinPollRequest {
	@NonNull
	public String poll;
	public String proposal;
}
