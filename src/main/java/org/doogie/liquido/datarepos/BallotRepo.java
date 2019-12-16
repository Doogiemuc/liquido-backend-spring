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
	 * Find all ballots in a poll.
	 * @param poll a poll
	 * @return list of ballots that have been casted in this ppoll (so far)
	 */
  List<BallotModel> findByPoll(PollModel poll);

	/**
	 * Find a casted ballot by its checksum
	 * @param checksum a ballots checksum, ie. its hashCode
	 * @param poll the poll with that ballot
	 * @return the ballot or Optional.emtpy() if not found
	 */
  Optional<BallotModel> findByPollAndChecksum(PollModel poll, String checksum);

	/**
	 * Count number of ballots casted in this poll
	 * @param poll a poll
	 * @return number of ballots in this poll
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
   * @return the ballot that was casted with this this (still anonymous) rightToVote or
	 *         Optional.empty() when this voter did not cast a vote in this poll yet.
   */
  Optional<BallotModel> findByPollAndRightToVote(PollModel poll, RightToVoteModel rightToVote);
}
