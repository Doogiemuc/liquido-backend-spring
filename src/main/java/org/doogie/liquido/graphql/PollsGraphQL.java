package org.doogie.liquido.graphql;

import io.leangen.graphql.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.jwt.AuthUtil;
import org.doogie.liquido.model.*;
import org.doogie.liquido.rest.dto.CastVoteResponse;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.PollService;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.doogie.liquido.jwt.AuthUtil.HAS_ROLE_TEAM_ADMIN;
import static org.doogie.liquido.jwt.AuthUtil.HAS_ROLE_USER;

/**
 * GraphQL queries and mutations for LIQUIDO polls.
 * This endpoint is used by the LIQUIDO mobile app.
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
	LawRepo lawRepo;

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
		return pollRepo.findById(pollId)
			.orElseThrow(LiquidoException.notFound("Poll.id=" + pollId + " not found."));
	}

	/**
	 * GraphQL client can request the number of already casted ballots for a poll in voting.
	 * This is not an attribute in PollModel, but a calculated attribute "numBallots" that can then be
	 * returned to the GraphQL query.
	 * @param poll the poll of the current GraphQL context
	 * @return number of already casted ballots in this poll
	 */
	@GraphQLQuery(name = "numBallots")
	public Long getNumBallots(@GraphQLContext PollModel poll) {
		return pollService.getNumCastedBallots(poll);
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
		return team.getPolls();
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
		poll = pollService.addProposalToPoll(proposal, poll);
		return poll;
	}

	/**
	 * Get a valid voter Token
	 * @param areaId optional area otherwise default area will be used
	 * @param tokenSecret secret that only the user must know!
	 * @param becomePublicProxy if user's automatically wants to become a public proxy
	 * @return { "voterToken": "$2ADDgg33gva...." }
	 * @throws LiquidoException when user is not logged into a team
	 */
	@GraphQLQuery(name = "voterToken", description = "Get voter's the secret voterToken")
	@PreAuthorize(HAS_ROLE_USER)
	public String getVoterToken(
		@GraphQLArgument(name = "areaId") Long areaId,
		@GraphQLNonNull @GraphQLArgument(name = "tokenSecret") String tokenSecret,
		@GraphQLArgument(name = "becomePublicProxy", defaultValue = "false") Boolean becomePublicProxy
	) throws LiquidoException {
		AreaModel area = getAreaOrDefault(areaId);
		UserModel voter = authUtil.getCurrentUser()
			.orElseThrow(LiquidoException.unauthorized("Must be logged in to getVoterToken!"));
		return castVoteService.createVoterTokenAndStoreRightToVote(voter, area, tokenSecret, becomePublicProxy);
	}

	/**
	 * Cast a vote in a poll
	 * A user may overwrite his previous ballot as long as the poll is still in its VOTING phase.
	 * This request can be sent anonymously!
	 *
	 * @param pollId poll id that must exist
	 * @param voteOrderIds list of proposals IDs as sorted by the voter in his ballot
	 * @param voterToken a valid voter token
	 * @return CastVoteResponse
	 * @throws LiquidoException when poll.id ist not found, voterToken is invalid or voterOrder is empty.
	 */
	@GraphQLMutation(name = "castVote", description = "Cast a vote in a poll with ballot")
	public CastVoteResponse castVote_GraphQL(
		@GraphQLNonNull @GraphQLArgument(name = "pollId") long pollId,
		@GraphQLNonNull @GraphQLArgument(name = "voteOrderIds") List<Long> voteOrderIds,
		@GraphQLNonNull @GraphQLArgument(name = "voterToken") String voterToken
	) throws LiquidoException {
		PollModel poll = pollRepo.findById(pollId)
			.orElseThrow(LiquidoException.notFound("Cannot cast vote. Poll(id="+pollId+") not found!"));
		return castVoteService.castVote(voterToken, poll, voteOrderIds);
	}

	/**
	 * Start the voting Phase of a poll
	 * @param pollId poll.id
	 * @return the poll in VOTING
	 * @throws LiquidoException when voting phase cannot yet be started
	 */
	@GraphQLMutation(name = "startVotingPhase", description = "Start voting phase of a poll")
	@PreAuthorize(HAS_ROLE_TEAM_ADMIN)
	public PollModel startVotingPhase_GraphQL(
		@GraphQLArgument(name = "pollId") @GraphQLNonNull long pollId
	) throws LiquidoException {
		PollModel poll = pollRepo.findById(pollId)
			.orElseThrow(LiquidoException.notFound("Cannot start voting phase. Poll(id="+pollId+") not found!"));
		return pollService.startVotingPhase(poll);
	}

	/**
	 * Finish the voting phase of a poll
	 * @param pollId poll.id
	 * @return the winning law
	 * @throws LiquidoException when poll is not in status voting
	 */
	@GraphQLMutation(name = "finishVotingPhase", description = "Finish voting phase of a poll")
	@PreAuthorize(HAS_ROLE_TEAM_ADMIN)
	public LawModel finishVotingPhase_GraphQL(
		@GraphQLArgument(name = "pollId") @GraphQLNonNull long pollId
	) throws LiquidoException {
		PollModel poll = pollRepo.findById(pollId)
			.orElseThrow(LiquidoException.notFound("Cannot start voting phase. Poll(id="+pollId+") not found!"));
		return pollService.finishVotingPhase(poll);
	}

	/**
	 * Get the ballot of a voter in a poll, if the voter has already casted one.
	 * @param voterToken voter's secret voterToken
	 * @param pollId poll.id
	 * @return the voter's ballot if there is one
	 * @throws LiquidoException when voterToken is invalid
	 */
	@GraphQLQuery(name = "ballot", description = "Get the ballot of a voter in a poll, if the voter has already casted one.")
	@PreAuthorize(HAS_ROLE_USER)
	public Optional<BallotModel> getBallot(
		@GraphQLArgument(name = "voterToken") String voterToken,
		@GraphQLArgument(name = "pollId") @GraphQLNonNull long pollId
	) throws LiquidoException {
		PollModel poll = pollRepo.findById(pollId)
			.orElseThrow(LiquidoException.notFound("Cannot start voting phase. Poll(id="+pollId+") not found!"));
		return pollService.getBallotForVoterToken(poll, voterToken);
	}

	/**
	 * Verify a voter's ballot with its checksum. When the checksum is valid, the
	 * ballot with the correct voteOrder will be returned.
	 * @param pollId a poll
	 * @param checksum checksum of a ballot in that poll
	 * @return the voter's ballot if it matches the checksum.
	 * @throws LiquidoException when poll cannot be found
	 */
	@GraphQLQuery(name= "verifyBallot", description = "Verify a ballot with its checksum.")
	@PreAuthorize(HAS_ROLE_USER)
	public Optional<BallotModel> verifyBallot(
		@GraphQLArgument(name = "pollId") @GraphQLNonNull long pollId,
		@GraphQLNonNull @GraphQLArgument(name = "checksum") String checksum
	) throws LiquidoException {
		PollModel poll = pollRepo.findById(pollId)
			.orElseThrow(LiquidoException.notFound("Cannot verify checksum! Poll(id="+pollId+") not found!"));
		return pollService.getBallotForChecksum(poll, checksum);
	}

	// ============= Utility methods ========


	/**
	 * If areaId is null the return (lazily fetched) default area.
	 * If areaId is given (not null) then return that area.
	 *
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
			this.defaultArea = areaRepo.findByTitle(liquidoProps.defaultAreaTitle)
				.orElseThrow(LiquidoException.supply(LiquidoException.Errors.INTERNAL_ERROR, "Cannot find default area!"));
		}
		return this.defaultArea;
	}
}
