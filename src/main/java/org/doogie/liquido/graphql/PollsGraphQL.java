package org.doogie.liquido.graphql;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.PollService;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * GraphQL queries and mutations for Liquido posts.
 * These are used by the mobile app.
 */
@Slf4j
@Service
public class PollsGraphQL {

	@Autowired
	AreaRepo areaRepo;

	@Autowired
	PollRepo pollRepo;

	@Autowired
	LiquidoProperties liquidoProps;

	@Autowired
	PollService pollService;


	/**
	 * Get one poll by its ID
	 * @param pollId pollId (mandatory)
	 * @return the PollModel
	 */
	@GraphQLQuery(name = "poll")
	public PollModel getPollById(@GraphQLNonNull @GraphQLArgument(name = "pollId") Long pollId) throws LiquidoException {
		Optional<PollModel> pollOpt = pollRepo.findById(pollId);
		if (!pollOpt.isPresent())
			throw new LiquidoException(LiquidoException.Errors.CANNOT_FIND_ENTITY, "Poll.id=" + pollId + " not found.");
		return pollOpt.get();
	}

	/**
	 * Admin of a team creates a new poll.
	 * The VOTING phase of this poll will be started manually by the admin later.
	 * @param title title of poll
	 * @return the newly created poll
	 */
	@GraphQLMutation(name = "createPoll", description = "Admin creates a new poll")
	public PollModel createPoll(
		@GraphQLNonNull @GraphQLArgument(name="title") String title
	) throws LiquidoException {
		PollModel poll = new PollModel(title);
		try {
			poll = pollRepo.save(poll);
		} catch (DataIntegrityViolationException e) {
			throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_POLL, "A Poll with the title '"+title+"' already exists in your team!");
		}
		return poll;
	}

	/**
	 * Add a proposal to a poll. One user is only allowed to have one proposal in each poll.
	 * @param pollId the poll, MUST exist
	 * @param title Title of the new proposal. MUST be unique within the poll.
	 * @param description Longer description of the proposal
	 * @param areaId AreaID. DEFAULT_AREA will be used if not set.
	 * @return The updated poll with the added proposal
	 * @throws LiquidoException when proposal title already exists in this poll.
	 */
	@GraphQLMutation(name = "addProposal", description = "Add new proposal to a poll")
	public PollModel addProposalToPoll_GraphQL(
		@GraphQLNonNull @GraphQLArgument(name = "pollId") long pollId,
		@GraphQLNonNull @GraphQLArgument(name = "title") String title,
		@GraphQLNonNull @GraphQLArgument(name = "description") String description,
		@GraphQLArgument(name = "areaId") Long areaId
	) throws LiquidoException {

		AreaModel area = areaRepo.findById(areaId).orElse(liquidoProps.defaultArea);
		PollModel poll = pollRepo.findById(pollId).orElseThrow(
			() -> new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Cannot find poll with id="+pollId)
		);
		LawModel proposal = new LawModel(title, description, area);
		pollService.addProposalToPoll(proposal, poll);
		return poll;
	}

}
