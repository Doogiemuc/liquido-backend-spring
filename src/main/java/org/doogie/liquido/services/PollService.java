package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.util.LiquidoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * This spring component implements the business logic for {@link org.doogie.liquido.model.PollModel}
 * I am trying to keep the Models as dumb as possible.
 * All business rules and security checks are implemented here.
 */
@Slf4j
@Service
public class PollService {

  @Autowired
  LiquidoProperties props;

  @Autowired
  PollRepo pollRepo;

  @Autowired
  LawRepo lawRepo;


  /**
   * Start a new poll with (at least) one already existing proposal.
   * @param proposal an idea that reached its quorum and became a proposal. MUST be in status proposal!
   * @param resourceAssembler
   * @return the newly created poll
   * @throws IllegalArgumentException if passed LawModel is not in state PROPOSAL.
   */
  @Transactional    // This should run inside a transaction (all or nothing)
  public PollModel createPoll(@NotNull LawModel proposal, PersistentEntityResourceAssembler resourceAssembler) throws LiquidoException {
    //===== sanity check
    if (proposal == null)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_POLL, "Proposal must not be null");
    if (!LawModel.LawStatus.PROPOSAL.equals(proposal.getStatus()))
      throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_POLL, "Need proposal with quorum for creating a poll!");
    if (proposal.getPoll() != null)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_POLL, "Proposal (id="+proposal.getId()+") is already part of another poll!");
    //TODO: All proposal within a poll must be in the same area!

    //===== create new Poll with one initial proposal
    log.debug("Will create new poll. InitialProposal (id={}): {}", proposal.getId(), proposal.getTitle());
    PollModel poll = new PollModel();
    LawModel proposalInDB = lawRepo.findByTitle(proposal.getTitle());  // I have to load the proposal from DB, otherwise: exception "Detached entity passed to persist"
    poll.addProposal(proposalInDB);
    PollModel savedPoll = pollRepo.save(poll);
    return savedPoll;
  }

  /**
   * Add a proposals (ie. an ideas that reached its quorum) to an existing poll and save the poll.
   * @param proposal a proposal (in status PROPOSAL)
   * @param poll a poll in status ELABORATION
   * @return the newly created poll
   * @throws LiquidoException if any status is wrong
   */
  public PollModel addProposalToPoll(@NotNull LawModel proposal, @NotNull PollModel poll) throws LiquidoException {
    poll.addProposal(proposal);  // may throw LiquidoException
    return pollRepo.save(poll);
  }

  /**
   * Start the voting phase of the given poll.
   * Poll must be in elaboration phase and must have at least two proposals
   * @param poll a poll in elaboration phase with at least two proposals
   */
  public void startVotingPhase(@NotNull PollModel poll) throws LiquidoException {
    if (poll.getStatus() != PollModel.PollStatus.ELABORATION)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_START_VOTING_PHASE, "Poll must be in status ELABORATION");
    if (poll.getProposals().size() < 2)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_START_VOTING_PHASE, "Poll must have at least two alternative proposals");
    for (LawModel proposal : poll.getProposals()) {
      proposal.setStatus(LawModel.LawStatus.VOTING);
    }
    poll.setStatus(PollModel.PollStatus.VOTING);
    poll.setVotingStartedAt(new Date());
    pollRepo.save(poll);
  }

  //TODO: getCurrentResult()   sum up current votes

  //TODO: endVotingPhase(): winning proposal becomes a law, and all others fall back to status=proposal  (then can join further polls later on)
}
