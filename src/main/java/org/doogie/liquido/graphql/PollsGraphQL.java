package org.doogie.liquido.graphql;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.jwt.AuthUtil;
import org.doogie.liquido.model.*;
import org.doogie.liquido.rest.dto.CastVoteRequest;
import org.doogie.liquido.rest.dto.CastVoteResponse;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.PollService;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.doogie.liquido.jwt.AuthUtil.HAS_ROLE_TEAM_ADMIN;
import static org.doogie.liquido.jwt.AuthUtil.HAS_ROLE_USER;

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
	TeamRepo teamRepo;

	@Autowired
	PollService pollService;

	@Autowired
	CastVoteService castVoteService;

	@Autowired
	LiquidoProperties liquidoProps;

	@Autowired
	AuthUtil authUtil;

	/* Lazily initialized default area. */
	AreaModel defaultArea = null;

	/**
	 * Get one poll by its ID
	 * @param pollId pollId (mandatory)
	 * @return the PollModel
	 */
	@GraphQLQuery(name = "poll")
	@PreAuthorize(HAS_ROLE_USER)
	public PollModel getPollById(@GraphQLNonNull @GraphQLArgument(name = "pollId") Long pollId) throws LiquidoException {
		Optional<PollModel> pollOpt = pollRepo.findById(pollId);
		if (!pollOpt.isPresent())
			throw new LiquidoException(LiquidoException.Errors.CANNOT_FIND_ENTITY, "Poll.id=" + pollId + " not found.");
		return pollOpt.get();
	}

	/**
	 * Get all polls of currently logged in user's team.
	 * @return Set of polls
	 * @throws LiquidoException when no one is logged in.
	 */
	@GraphQLQuery(name = "polls")
	@PreAuthorize(HAS_ROLE_USER)
	public Set<PollModel> getPollsOfTeam() throws LiquidoException {
		TeamModel team = authUtil.getCurrentTeam()
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.UNAUTHORIZED, "Cannot get polls of team: Must be logged into a team!"));
		Set<PollModel> polls = team.getPolls();
		return polls;
	}

	/**
	 * Admin of a team creates a new poll.
	 * The VOTING phase of this poll will be started manually by the admin later.
	 * @param title title of poll
	 * @return the newly created poll
	 */
	@GraphQLMutation(name = "createPoll", description = "Admin creates a new poll")
	@PreAuthorize(HAS_ROLE_TEAM_ADMIN)
	public PollModel createPoll(
		@GraphQLNonNull @GraphQLArgument(name="title") String title
	) throws LiquidoException {
		AreaModel defaultArea = this.getDefaultArea();
		TeamModel team = authUtil.getCurrentTeam()
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.UNAUTHORIZED, "Cannot create poll: Must be logged into a team!"));
		return pollService.createPoll(title, defaultArea, team);
	}

	/**
	 * Create a new proposal and add it to a poll. A user is only allowed to have one proposal in each poll.
	 *
	 * @See {@link PollService#addProposalToPoll(LawModel, PollModel)}
	 *
	 * You must set supportersForProposal = -1 in application.properties for this to work.
	 * Otherwise the idea must first reach its quorum, before it can be added to a poll.
	 *
	 * Creating ideas is currently not supported via the GraphQL endpoints. The workflow for the mobile app is simplified.
	 *
	 * @param pollId the poll, MUST exist
	 * @param title Title of the new proposal. MUST be unique within the poll.
	 * @param description Longer description of the proposal
	 * @param areaId (optional) AreaId. DEFAULT_AREA will be used if not set.
	 * @return The updated poll with the added proposal
	 * @throws LiquidoException when proposal title already exists in this poll.
	 */
	@GraphQLMutation(name = "addProposal", description = "Add new proposal to a poll")
	@PreAuthorize(HAS_ROLE_USER)
	public PollModel addProposalToPoll_GraphQL(
		@GraphQLNonNull @GraphQLArgument(name = "pollId") long pollId,
		@GraphQLNonNull @GraphQLArgument(name = "title") String title,
		@GraphQLNonNull @GraphQLArgument(name = "description") String description,
		@GraphQLArgument(name = "areaId") Long areaId
	) throws LiquidoException {
		// If areaId is given, then lookup that AreaModel. Otherwise lookup defaultArea;
		AreaModel area = this.getAreaOrDefault(areaId);
		// Find the poll
		PollModel poll = pollRepo.findById(pollId)
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Cannot addProposal: There is no poll with id="+pollId));
		// Create a new proposal and add it to the poll (via PollService)
		LawModel proposal = new LawModel(title, description, area);

		// The PWA mobile client has no ideas that need to reach a quorum of supporters before they become a proposal. Mobile client directly creates proposals.
		// Warn if this is configured incorrectly.
		if (liquidoProps.supportersForProposal > 0)
			log.warn("Directly adding a proposal via GraphQL to poll(id="+poll.id+"). But needed supportersForProposal is actually configured to be "+liquidoProps.supportersForProposal);

		proposal.setStatus(LawModel.LawStatus.PROPOSAL);
		pollService.addProposalToPoll(proposal, poll);
		return poll;
	}

	/**
	 * Get a valid voter Token
	 * @param area optional area otherwise default area will be used
	 * @param tokenSecret secret that only the user must know!
	 * @param becomePublicProxy if user's automatically wants to become a public proxy
	 * @return { "voterToken": "$2ADDgg33gva...." }
	 * @throws LiquidoException when user is not logged into a team
	 */
	@GraphQLQuery(name = "voterToken")
	@PreAuthorize(HAS_ROLE_USER)
	public Lson getVoterToken(
		@GraphQLArgument(name = "area") AreaModel area,
		@GraphQLNonNull @GraphQLArgument(name = "tokenSecret") String tokenSecret,
		@GraphQLArgument(name = "becomePublicProxy", defaultValue = "false") Boolean becomePublicProxy
	) throws LiquidoException {
		UserModel voter = authUtil.getCurrentUser()
			.orElseThrow(LiquidoException.unauthorized("Must be logged in to getVoterToken!"));
		String voterToken = castVoteService.createVoterTokenAndStoreRightToVote(voter, area, tokenSecret, becomePublicProxy);
		return new Lson("voterToken", voterToken);
	}

	/**
	 * Cast a vote in a poll
	 * A user may overwrite his previous ballot as long as the poll is still in its VOTING phase.
	 * This request can be sent anonymously!
	 *
	 * @param pollId poll id that must exist
	 * @param voteOrder list of proposals as sorted by the voter in his ballot
	 * @param voterToken a valid voter token
	 * @return CastVoteResponse
	 * @throws LiquidoException when poll.id ist not found, voterToken is invalid or voterOrder is empty.
	 */
	@GraphQLMutation(name = "castVote", description = "Cast a vote in a poll with ballot")
	public CastVoteResponse castVote_GraphQL(
		@GraphQLNonNull @GraphQLArgument(name = "pollId") long pollId,
		@GraphQLNonNull @GraphQLArgument(name = "voteOrder") List<LawModel> voteOrder,
		@GraphQLNonNull @GraphQLArgument(name = "voterToken") String voterToken
	) throws LiquidoException {
		PollModel poll = pollRepo.findById(pollId)
			.orElseThrow(LiquidoException.notFound("Cannot cast vote. Poll(id="+pollId+") not found!"));
		CastVoteRequest req = new CastVoteRequest(poll, voteOrder, voterToken);
		return castVoteService.castVote(req);
	}




	/**
	 * If areaId is null the return default area.
	 * If areaId is given (not null) then return that area.
	 * Default area will be lazily fetched and then cached locally
 	 * @param areaId id of an existing area or null to fetch default area
	 * @return the area
	 * @throws LiquidoException when default area is not found or an area with the passed id does not exist
	 */
	private AreaModel getAreaOrDefault(Long areaId) throws LiquidoException {
		if (areaId == null) {
			return this.getDefaultArea();
		} else {
			return areaRepo.findById(areaId).orElseThrow(LiquidoException.supply(LiquidoException.Errors.CANNOT_FIND_ENTITY, "Cannot find area with id="+areaId));
		}
	}

	private AreaModel getDefaultArea() throws LiquidoException {
		if (this.defaultArea == null) {
			this.defaultArea = areaRepo.findByTitle(liquidoProps.getDefaultAreaTitle())
				.orElseThrow(LiquidoException.supply(LiquidoException.Errors.INTERNAL_ERROR, "Cannot find default area!"));
		}
		return this.defaultArea;
	}
}
