package org.doogie.liquido.services;

import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.LiquidoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * This spring component provides some utility methods for {@link org.doogie.liquido.model.PollModel}
 */
@Component
public class PollService {
  //Implementation note: Actually these are just some getters for PollModel. But I do not want PollModel
  // to depend on LiquidoProperties. So I refactored these getters into their own Service class.

  @Autowired
  LiquidoProperties props;

  /**
   * The voting phase of a poll starts n days after the initial proposal has reached its quorum.
   * The value of n is one of the {@link LiquidoProperties} in the DB.
   * @param poll a PollModel
   * @return The date when voting starts for this proposal or
   *         <b>null</b> if you pass in null or the poll's initial proposal did not yet reach its quorum.
   */
  public Date getVotingStartsAt(PollModel poll) {
    if (poll == null || poll.getInitialProposal() == null || poll.getInitialProposal().getReachedQuorumAt() == null) return null;
    int daysUntilVotingStarts = props.getInt(LiquidoProperties.KEY.DAYS_UNTIL_VOTING_STARTS);
    return DoogiesUtil.addDays(poll.getInitialProposal().getReachedQuorumAt(), daysUntilVotingStarts);
  }

  /**
   * The duration of the voting phase is configured via @{@link LiquidoProperties}
   * @param poll a PollModel
   * @return the date when the voting phase of this poll ends or
   *         <b>null</b> when you pass in null
   */
  public Date getVotingEndsAt(PollModel poll) {
    Date votingStartsAt = getVotingStartsAt(poll);
    if (votingStartsAt == null) return null;
    int durationOfVotingPhase = props.getInt(LiquidoProperties.KEY.DURATION_OF_VOTING_PHASE);
    return DoogiesUtil.addDays(votingStartsAt, durationOfVotingPhase);
  }

  //TODO: getCurrentResult()   sum up current votes
}
