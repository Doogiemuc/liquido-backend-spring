package org.doogie.liquido.rest.dto;

import lombok.Data;
import lombok.NonNull;

/**
 * HTTP request payload data for joining a proposal into a poll
 */
@Data
public class JoinPollRequest {
	@NonNull
	public String poll;

	@NonNull
	public String proposal;
}
