package org.doogie.liquido.model;

import org.springframework.data.rest.core.config.Projection;

import java.util.List;

/**
 * Projection for a ballot that contains the voteOrder as a list of proposals (LawModels)
 */
@Projection(name = "ballotProjection", types = { BallotModel.class })
public interface BallotProjection {
	//Remember that all default fields must be listed here!
	Long getId();

	//PollModel getPoll();

	Integer getLevel();

	Long getVoteCount();

	List<LawModel> getVoteOrder();

	ChecksumModel getChecksum();

}


