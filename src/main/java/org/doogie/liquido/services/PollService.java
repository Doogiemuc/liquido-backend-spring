package org.doogie.liquido.services;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.util.LiquidoProperties;
import org.doogie.liquido.util.Matrix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

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

  @Autowired
	BallotRepo ballotRepo;


	/**
   * Start a new poll from an already existing proposal.
   * @param proposal an idea that reached its quorum and became a proposal. MUST be in status proposal and MUST NO already be part of another poll.
   * @return the newly created poll
   * @throws LiquidoException if passed LawModel is not in state PROPOSAL.
   */
  @Transactional    // run inside a transaction (all or nothing)
  public PollModel createPoll(@NonNull LawModel proposal) throws LiquidoException {
    //===== sanity checks: There must be at least one proposal (in status PROPOSAL)
    if (proposal == null)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_POLL, "Proposal must not be null");
    if (!LawModel.LawStatus.PROPOSAL.equals(proposal.getStatus()))
      throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_POLL, "Need proposal with quorum for creating a poll!");
    // that proposal must not already be linked to another poll
    if (proposal.getPoll() != null)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_CREATE_POLL, "Proposal (id="+proposal.getId()+") is already part of another poll!");

    //===== create new Poll with one initial proposal
    log.info("Create new poll. InitialProposal (id={}): {}", proposal.getId(), proposal.getTitle());
    PollModel poll = new PollModel();
    // voting starts n days in the future (at midnight)
    LocalDateTime votingStart = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).plusDays(props.getInt(LiquidoProperties.KEY.DAYS_UNTIL_VOTING_STARTS));
    poll.setVotingStartAt(votingStart);
    poll.setVotingEndAt(votingStart.plusDays(props.getInt(LiquidoProperties.KEY.DURATION_OF_VOTING_PHASE)));
    //LawModel proposalInDB = lawRepo.findByTitle(proposal.getTitle());  //BUGFIX: I have to load the proposal from DB, otherwise: exception "Detached entity passed to persist" => Fixed by setting cascade = CascadeType.MERGE in PollModel
    poll = addProposalToPoll(proposal, poll);
    PollModel savedPoll = pollRepo.save(poll);
    return savedPoll;
  }

  /**
   * Add a proposals (ie. an ideas that reached its quorum) to an existing poll and save the poll.
   * @param proposal a proposal (in status PROPOSAL)
   * @param poll a poll in status ELABORATION
   * @return the newly created poll
   * @throws LiquidoException if area or status of proposal or poll is wrong. And also when user already has a proposal in this poll.
   */
  public PollModel addProposalToPoll(@NonNull LawModel proposal, @NonNull PollModel poll) throws LiquidoException {
    if (proposal.getStatus() != LawModel.LawStatus.PROPOSAL)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Cannot add proposal(id="+proposal.getId()+") to poll(id="+poll.getId()+", because proposal is not in state PROPOSAL.");
    if (poll.getStatus() != PollModel.PollStatus.ELABORATION)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Cannot add proposal, because poll id="+poll.getId()+" is not in ELABORATION phase");
    if (poll.getProposals().size() > 0 && !proposal.getArea().equals(poll.getArea()))
      throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Added proposal must be in the same area as the other proposals in this poll.");
		if(poll.getProposals().contains(proposal))
			throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Poll.id="+poll.getId()+" already contains proposal.id="+proposal.getId());
    for (LawModel p : poll.getProposals()) {
			if (p.getCreatedBy().equals(proposal.getCreatedBy()))
				throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, p.getCreatedBy().getEmail()+"(id="+p.getCreatedBy().getId()+") already have a proposal in poll(id="+poll.getId()+")");
    }

    log.debug("addProposalToPoll(proposal.id="+proposal.getId()+", poll.id="+poll.getId()+")");
    proposal.setStatus(LawModel.LawStatus.ELABORATION);
    poll.getProposals().add(proposal);
    proposal.setPoll(poll);
    return pollRepo.save(poll);
  }

  /**
   * Start the voting phase of the given poll.
   * Poll must be in elaboration phase and must have at least two proposals
   * @param poll a poll in elaboration phase with at least two proposals
   */
  public void startVotingPhase(@NonNull PollModel poll) throws LiquidoException {
    log.info("startVotingPhase of "+poll.toString());
    if (poll.getStatus() != PollModel.PollStatus.ELABORATION)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_START_VOTING_PHASE, "Poll must be in status ELABORATION");
    if (poll.getProposals().size() < 2)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_START_VOTING_PHASE, "Poll must have at least two alternative proposals");
    for (LawModel proposal : poll.getProposals()) {
      proposal.setStatus(LawModel.LawStatus.VOTING);
    }
    poll.setStatus(PollModel.PollStatus.VOTING);
    LocalDateTime votingStart = LocalDateTime.now();
    poll.setVotingStartAt(votingStart);   //record the exact datetime when the voting phase started.
    poll.setVotingEndAt(votingStart.truncatedTo(ChronoUnit.DAYS).plusDays(props.getInt(LiquidoProperties.KEY.DURATION_OF_VOTING_PHASE)));     //voting ends in n days at midnight
    pollRepo.save(poll);
  }




  public void finishVotingPhase(@NonNull PollModel poll) throws LiquidoException {
    if (!poll.getStatus().equals(PollModel.PollStatus.VOTING))
      throw new LiquidoException(LiquidoException.Errors.CANNOT_FINISH_POLL, "Poll must be in status VOTING.");

    poll.setStatus(PollModel.PollStatus.FINISHED);
    for(LawModel proposal : poll.getProposals()) {
      proposal.setStatus(LawModel.LawStatus.DROPPED);
    }

		//TODO: calc winner(s) of poll with Schulz method
  }

  //========= adapted from https://github.com/zephyr/schulze-voting/blob/master/src/java/dh/p/schulze/Election.java

  /**
   * Calculate the number of voters that prefer proposal i over proposal j
   * @param poll a poll where voting was just finished
   * @return a two dimensional Matrix that contains the number of voters that prefere i over j
   */
  public Matrix calcDuelMatrix(@NonNull PollModel poll) {
    // First of all each proposal in the poll gets and array index (in poll.getProposals, which is only a Set)
		Map<LawModel, Integer> proposalIdx = new HashMap<>();
		int proposalIndex = 0;
		for(LawModel prop : poll.getProposals()) {
			proposalIdx.put(prop, proposalIndex++);
		}

		// For each different voteOrder that appears in all ballots we now fill three maps.
		// The map's key is always the unique hashCode of that voteOrder.

		// number of ballots with this specific vote order. Mapped by hashCode of voteOrder
		Map<Integer, Integer> numberOfBallots = new HashMap<>();

		// array of proposals that have been voted for. Mapped by hashCode of voteOrder.
		Map<Integer, Integer[]> votedForMap = new HashMap<>();

		// array of proposals that nave NOT been voted for at all in this voteOrder. Mapped by hashCode of voteOrder.
		Map<Integer, Integer[]> notVotedForMap = new HashMap<>();

    for (BallotModel ballot : ballotRepo.findByPoll(poll)) {
			int key = ballot.getVoteOrder().hashCode();
			Integer numBallots = numberOfBallots.get(key);
			// when a voteOrder appears for the first time, then calc:
			if (numBallots == null) {
				numberOfBallots.put(key, 1);

    		// array of proposal indexes for the proposals in this voteOrder
				Integer[] votedForIndexes = ballot.getVoteOrder().stream().map(p -> proposalIdx.get(p)).toArray(Integer[]::new);
				votedForMap.put(key, votedForIndexes);

				// array of proposal indexes for the proposals that were not voted for at all in this ballot
				Set<LawModel> notVotedFor = new HashSet<>(poll.getProposals());
				notVotedFor.removeAll(ballot.getVoteOrder());
				Integer[] notVotedForIndexes = notVotedFor.stream().map(p -> proposalIdx.get(p)).toArray(Integer[]::new);
				notVotedForMap.put(key, notVotedForIndexes);
			} else {
				numberOfBallots.put(key, numBallots+1);
			}
		}

		//----- Fill the duelMatrix that stores the number of preferences i <-> j
		int n = poll.getNumCompetingProposals();
		Matrix duelMatrix = new Matrix(n, n);

		// for each type of voteOrder that was casted
		for(Integer key : votedForMap.keySet()) {
    	Integer[] idx = votedForMap.get(key);
			for (int i = 0; i < idx.length-1; i++) {

				// add a preference i<j for each other proposal sorted below for that number of ballots
				for (int j = i+1; j < idx.length; j++) {
					duelMatrix.add(idx[i], idx[j], numberOfBallots.get(key));
				}

				// and add a preference i<j for each proposal that was not voted for at all in this voteOrder
				for (int k = 0; k < notVotedForMap.get(key).length; k++) {
					duelMatrix.add(idx[i], notVotedForMap.get(key)[k], numberOfBallots.get(key));
				}

			}
		}
    return  duelMatrix;
  }

	/**
	 * The strongest path from candidate ("proposal") A to B is the path with the best weakest link.
	 * "A chain can only be so strong as its weakest link."
	 * @param poll a poll where voting has just finished
	 * @return a two dimensional Matrix that contains the strength of the weakest links for each stongest path
	 */
  public Matrix calcStrongestPathMatrix(@NonNull PollModel poll) {
		Matrix d = calcDuelMatrix(poll);										// number of preferences i over j
		Matrix p = new Matrix(d.getRows(), d.getCols());		// strongest path matrix
		int C = poll.getNumCompetingProposals();

		for (int i = 0; i < C; i++) {
			for (int j = 0; j < C; j++) {
				if (i != j) {
					p.set(i, j, (d.get(i, j) > d.get(j, i)) ? d.get(i, j) : 0);
				}
			}
		}

		for (int i = 0; i < C; i++) {
			for (int j = 0; j < C; j++) {
				if (i != j) {
					for (int k = 0; k < C; k++) {
						if ((i != k) && (j != k)) {
							p.set(j, k, Math.max(p.get(j, k), Math.min(p.get(j,i), p.get(i, k))));
						}
					}
				}
			}
		}
		return p;
  }

	/**
	 * Calculate the list of potential winners of this poll.
	 * @param poll a poll where the voting phase is finished
	 * @return the list of potential winners by the Schulze Method
	 */
  public List<LawModel> calcPotentialWinners(@NonNull PollModel poll) {
    List<LawModel> winnerList = new ArrayList<>();
    Matrix p = calcStrongestPathMatrix(poll);
    int C = poll.getNumCompetingProposals();

    int i = 0;
		for (LawModel proposal : poll.getProposals()) {
      boolean b = true;
      for (int j = 0; j < C; j++) {
        if (i != j) {
          b = b && (p.get(i, j) >= p.get(j, i));
        }
      }
      if (b) winnerList.add(proposal);
      i++;
    }
    return winnerList;
  }


}

