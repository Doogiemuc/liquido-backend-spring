package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.LiquidoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * This spring component implements the business logic for {@link org.doogie.liquido.model.PollModel}
 * I am trying to keep the Models as dumb as possible.
 * All business rules and security checks are implemented here.
 */
@Slf4j
@Component
public class PollService {

  @Autowired
  LiquidoProperties props;

  @Autowired
  PollRepo pollRepo;


  /**
   * Start a new poll
   * @param proposal an idea that reached its quorum and became a proposal. MUST be in status proposal!
   * @throws IllegalArgumentException if passed LawModel is not in state PROPOSAL.
   */
  public void createPoll(@NotNull LawModel proposal) throws LiquidoException {
    //===== sanity check
    if (proposal == null) throw new IllegalArgumentException("Proposal must not be null");
    if (proposal.getStatus() != LawModel.LawStatus.PROPOSAL)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_POLL, "Need proposal with quorum for creating a poll!");

    //===== create new Poll with initial proposal
    log.debug("Will create new poll. InitialProposal (id={}): {}", proposal.getId(), proposal.getTitle());
    PollModel poll = new PollModel();
    poll.addProposal(proposal);
    pollRepo.save(poll);
  }

  /**
   * Add a proposals (ie. an ideas that reached its quorum) to an existing poll and save the poll.
   * @param proposal a proposal (in status PROPOSAL)
   * @param poll a poll in status ELABORATION
   * @throws LiquidoException if any status is wrong
   */
  public void addProposalToPoll(@NotNull LawModel proposal, @NotNull PollModel poll) throws LiquidoException {
    poll.addProposal(proposal);
    pollRepo.save(poll);
  }

  /**
   * Start the voting phase of the given poll.
   * Poll must contain at least two proposals
   * @param poll
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
  }

  //TODO: getCurrentResult()   sum up current votes

  //TODO: endVotingPhase(): winning proposal becomes a law, and all others fall back to status=proposal  (then can join further polls later on)
}
