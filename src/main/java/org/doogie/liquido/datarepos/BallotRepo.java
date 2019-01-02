package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.model.ChecksumModel;
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
//We do not expose Ballots as rest resource. Clients must use VoteRestController!
public interface BallotRepo extends CrudRepository<BallotModel, Long> {

	/**
	 * find all ballots in a poll
	 * @param poll
	 * @return list of ballots that have been casted so far
	 */
  List<BallotModel> findByPoll(PollModel poll);

  /**
   * Find a ballot for a poll with a given checksumModel, ie. that was casted from a voter
	 * who's secret voterToken hashes to this checksumModel.
   * @param poll a PollModel
   * @param checksum a valid and stored checksum
   * @return the ballot for this (still anonymous) checksum
   */
  Optional<BallotModel> findByPollAndChecksum(PollModel poll, ChecksumModel checksum);
}
