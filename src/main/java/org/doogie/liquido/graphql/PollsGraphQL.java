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
import org.doogie.liquido.services.LawService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.PollService;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.doogie.liquido.util.DoogiesUtil;
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
	LawService lawService;

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
		UserModel user = authUtil.getCurrentUser().orElseThrow(LiquidoException.unauthorized("Must be logged in to like a proposal!"));
		TeamModel team = authUtil.getCurrentTeam()
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.UNAUTHORIZED, "Cannot create poll: Must be logged into a team!"));
		PollModel newPoll = pollService.createPoll(title, defaultArea, team);
		log.info("createPoll: Admin " + user.toStringShort() + " creates new poll '" + newPoll.getTitle() + "' in team "+team.getTeamName());
		return newPoll;
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
	 * @return The updated poll with the added proposal
	 * @throws LiquidoException when proposal title already exists in this poll.
	 */
	@GraphQLMutation(name = "addProposal", description = "Add new proposal to a poll")
	@PreAuthorize(HAS_ROLE_USER)
	public PollModel addProposalToPoll(
		@GraphQLNonNull @GraphQLArgument(name = "pollId") long pollId,
		@GraphQLNonNull @GraphQLArgument(name = "title") String title,
		@GraphQLNonNull @GraphQLArgument(name = "description") String description
	) throws LiquidoException {
		UserModel user = authUtil.getCurrentUser().orElseThrow(LiquidoException.unauthorized("Must be logged in to add a proposal!"));

		// Find the poll
		PollModel poll = pollRepo.findById(pollId)
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Cannot addProposal: There is no poll with id="+pollId));
		// Create a new proposal and add it to the poll (via PollService)
		LawModel proposal = new LawModel(title, description, poll.getArea());

		// The PWA mobile client has no ideas that need to reach a quorum of supporters before they become a proposal. Mobile client directly creates proposals.
		// Warn if this is configured incorrectly.
		if (liquidoProps.supportersForProposal > 0)
			log.warn("Directly adding a proposal via GraphQL to poll(id="+poll.id+"). But needed supportersForProposal is actually configured to be "+liquidoProps.supportersForProposal);

		proposal.setStatus(LawModel.LawStatus.PROPOSAL);
		poll = pollService.addProposalToPoll(proposal, poll);
		//BUGFIX: proposal.getCreatedBy() is not yet filled here
		log.info("addProposalToPoll: " + user.toStringShort() + " adds proposal '" + proposal.getTitle() + "' to poll.id="+poll.id);
		return poll;
	}

	/**
	 * Like a proposal in a poll.
	 * Poll must be in status ELABORATION
	 *
	 * @param pollId a poll
	 * @param proposalId a proposal of that poll
	 * @return the poll
	 * @throws LiquidoException when poll is not in status ELABORATION or when passed proposalId is not part of that poll.
	 */
	@GraphQLMutation(name = "likeProposal", description = "Add a like to a proposal in a poll")
	@PreAuthorize(HAS_ROLE_USER)
	public PollModel likeProposal(
		@GraphQLNonNull @GraphQLArgument(name = "pollId") long pollId,
		@GraphQLNonNull @GraphQLArgument(name = "proposalId") long proposalId
	) throws LiquidoException {
		UserModel user = authUtil.getCurrentUser().orElseThrow(LiquidoException.unauthorized("Must be logged in to like a proposal!"));

		// Find the poll and check that poll is in status ELABORATION
		PollModel poll = pollRepo.findById(pollId)
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.CANNOT_ADD_SUPPORTER, "Cannot supportProposal: There is no poll with id=" + pollId));
		if (!poll.getStatus().equals(PollModel.PollStatus.ELABORATION))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_SUPPORTER, "Cannot supportProposal: Poll is not in status ELABORATION");

		// Find the proposal in this poll
		LawModel proposal = DoogiesUtil.find(poll.getProposals(), prop -> prop.getId() == proposalId)
			.orElseThrow(LiquidoException.supply(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Cannot supportProposal: There is no proposal,id=" + proposalId + " in poll.id=" + pollId));

		lawService.addSupporter(user, proposal);
		log.info("likeProposal: " + user.toStringShort() + " likes proposal.id=" + proposalId + " in poll.id="+poll.id);
		return poll;
	}

	/**
	 * Get a valid voter Token for casting a ballot.
	 * @param areaId optional area otherwise default area will be used
	 * @param tokenSecret secret that only the user must know!
	 * @param becomePublicProxy if user's automatically wants to become a public proxy (default=false)
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
	 * Is a proposal already liked by the currently logged in user?
	 *
	 * @param proposal GraphQL context: the LawModel
	 * @return true, if currently logged in user is already a supporter of this proposal
	 */
	@GraphQLQuery(name = "isLikedByCurrentUser", description = "Is a proposal already liked by the currently logged in user?")
	@PreAuthorize(HAS_ROLE_USER)
	public boolean isLikedByCurrentUser(@GraphQLContext LawModel proposal) {
		// This adds the new boolean field "likedByCurrentUser" to the GraphQL representation of a proposal(LawModel) that can now be queried by the client.  graphql-spqr I like
		return lawService.isSupportedByCurrentUser(proposal);
	}

	/**
	 * Is a proposal created by the currently logged in user
	 * This of course assumes that there is a currently logged in user. But polls and propos can only be fetched by authenticated users.
	 *
	 * @param proposal A proposal in a poll.
	 * @return true if proposal was created by the currently logged in user.
	 */
	@GraphQLQuery(name = "isCreatedByCurrentUser", description = "Is a proposal created by the currently logged in user?")
	@PreAuthorize(HAS_ROLE_USER)
	public boolean isCreatedByCurrentUser(@GraphQLContext LawModel proposal) {
		Optional<UserModel> user = authUtil.getCurrentUser();
		return proposal != null && user.isPresent() && user.get().equals(proposal.getCreatedBy());
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
		CastVoteResponse res = castVoteService.castVote(voterToken, poll, voteOrderIds);
		log.info("castVote: poll.id=" + pollId);		//TODO: log all user actions into seperate file
		return res;
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
