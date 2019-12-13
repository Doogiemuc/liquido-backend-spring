package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.model.RightToVoteModel;
import org.doogie.liquido.rest.VoteRestController;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

/**
 * Database abstraction for "ballots".
 *
 * Ballots are not exposed as RepositoryRestResource at all.
 * Posting a ballot (ie. cast a vote) is handled in our custom {@link VoteRestController}
 */
public interface BallotRepo extends CrudRepository<BallotModel, Long> {

	/**
	 * find all ballots in a poll
	 * @param poll
	 * @return list of ballots that have been casted so far
	 */
  List<BallotModel> findByPoll(PollModel poll);

	/**
	 * Count number of ballots casted in this poll
	 * @param poll
	 * @return
	 */
  Long countByPoll(PollModel poll);

	/**
	 * Find all ballots that a user has casted with this rightToVote.
	 * These are all his ballots in one area.
	 * @param rightToVote a voters rightToVote
	 * @return list of ballots that were casted with this rightToVote (in one area)
	 */
  List<BallotModel> findByRightToVote(RightToVoteModel rightToVote);

  /**
   * Find the ballot in a poll that was casted from a voter
	 * who's secret voterToken hashes to this rightToVote.
   * @param poll a PollModel
   * @param rightToVote a user's right to vote
   * @return the ballot that was casted with this this (still anonymous) rightToVote
   */
  Optional<BallotModel> findByPollAndRightToVote(PollModel poll, RightToVoteModel rightToVote);
}
