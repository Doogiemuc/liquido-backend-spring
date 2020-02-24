package org.doogie.liquido.rest.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.rest.deserializer.LawModelDeserializer;

/**
 * HTTP request payload DTO for joining a proposal into a poll.
 */
@Data
@NoArgsConstructor
public class JoinPollRequest {
	@NonNull
	@JsonDeserialize(using = LawModelDeserializer.class)
	public LawModel proposal;
}
