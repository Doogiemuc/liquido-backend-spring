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
import java.util.stream.Collectors;

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

  //@Autowired
	//TaskScheduler scheduler;


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
  @Transactional
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
    LocalDateTime votingStart = LocalDateTime.now();			// LocalDateTime is without a timezone
    poll.setVotingStartAt(votingStart);   //record the exact datetime when the voting phase started.
    poll.setVotingEndAt(votingStart.truncatedTo(ChronoUnit.DAYS).plusDays(props.getInt(LiquidoProperties.KEY.DURATION_OF_VOTING_PHASE)));     //voting ends in n days at midnight
		pollRepo.save(poll);

		//----- schedule a Task in Spring that will end the voting phase.
		PollService that = this;
		Runnable endVotingPhase = new Runnable() {
			public void run() {
				try {
					that.finishVotingPhase(poll);
				} catch (LiquidoException e) {
					log.error("Could not finishVotingPhase of "+poll);
				}
			}
		};
		//TODO: scheduler.schedule(endVotingPhase, Date.from(poll.getVotingEndAt().atZone(ZoneId.systemDefault()).toInstant()));  // 		new CronTrigger("0 15 9-17 * * MON-FRI")
  }

	/**
	 * Finish the voting phase of a poll and calculate the winning proposal.
	 * @param poll A poll in voting phase
	 * @return Winnin proposal of this poll that now is a law.
	 * @throws LiquidoException When poll is not in voting phase
	 */
	@Transactional
  public LawModel finishVotingPhase(@NonNull PollModel poll) throws LiquidoException {
    if (!poll.getStatus().equals(PollModel.PollStatus.VOTING))
      throw new LiquidoException(LiquidoException.Errors.CANNOT_FINISH_POLL, "Poll must be in status VOTING.");

    poll.setStatus(PollModel.PollStatus.FINISHED);
    for(LawModel proposal : poll.getProposals()) {
      proposal.setStatus(LawModel.LawStatus.DROPPED);
    }
    poll.setVotingEndAt(LocalDateTime.now());

		//calc winner(s) of poll
		LawModel winningProposal = calcSchulzeMethodWinners(poll).get(0);
		winningProposal.setStatus(LawModel.LawStatus.LAW);
		poll.setWinner(winningProposal);
		pollRepo.save(poll);
    return winningProposal;
  }

  



	//========= Ranked Pairs voting   https://en.wikipedia.org/wiki/Ranked_pairs  ==================================
	// adapted from https://gist.github.com/asafh/a8e9af7a3e5282cbba27

	/**
	 * Compare two majorities which one is "better" and wins.
	 * A Majority is how often a candidate i was preferred to candidate j.
	 * 		int[3] = { i, j, numPreferences_I_over_J }
	 */
	public class MajorityComparator implements Comparator<int[]> {
		Matrix duelMatrix;

		public MajorityComparator(Matrix duelMatrix) {
			this.duelMatrix = duelMatrix;
		}

		/**
		 * Compare two majorities m1 and m2
		 * (1) The majority having more support for its alternative is ranked first.
		 * (2) Where the majorities are equal, the majority with the smaller minority opposition is ranked first.
		 * @param m1 majority one
		 * @param m2 majority two
		 * @return a negative number IF m1 < m2   OR
		 *         a positive number IF m2 > m1
		 */
		@Override
		public int compare(int[] m1, int[] m2) {
			if (m1 == null && m2 == null) return 0;
			if (m1.equals(m2)) return 0;
			if (m1 == null) return -1;
			if (m2 == null) return  1;
			int diff = m2[2] - m1[2];  // (1)
			if (diff == 0) {
				return duelMatrix.get(m2[1], m2[0]) - duelMatrix.get(m1[1], m1[0]);	// (2)
			} else {
				return diff;
			}
		}
	}

	/**
	 * A directed graph with nodes. (Without node weights)
	 */
	public class DirectedGraph extends HashMap<Integer, Set<Integer>> {
		/** add an edge from a node to another node */
		public boolean addDirectedEdge(int from, int to) {
			if (from == to) throw new IllegalArgumentException("cannot add a circular edge from a node to itself");
			if (this.get(from) == null) this.put(from, new HashSet<>());  // lazily create HashSet
			return this.get(from).add(to);
		}

		/** @return true if there is a path from node A to node B along the directed edges */
		public boolean reachable(int from, int to) {
			Set<Integer> neighbors = this.get(from);
			if (neighbors == null) return false;				// from is a leave
			if (neighbors.contains(to)) return true;		// to can be directly reached as a neighbor
			for(int neighbor: neighbors) {							// recursively check from all neighbors
				if (reachable(neighbor, to)) return true;
			}
			return false;
		}

		/**
		 * A "source" is a node that is not reachable from any other node.
		 * @return all sources, ie. nodes with no incoming links.
		 */
		public Set<Integer> getSources() {
			Set<Integer> sources = new HashSet(this.keySet());   // clone! of all nodes in this graph
			for(int nodeKey : this.keySet()) {
				Set<Integer> neighbors = this.get(nodeKey);
				for (int neighbor: neighbors) sources.remove(neighbor);
			}
			return sources;
		}

		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("DirectedGraph[");
			Iterator<Integer> it = this.keySet().iterator();
			while (it.hasNext()) {
				Integer key = it.next();
				sb.append("["+key+"->[");
				String neighborIDs = this.get(key).stream().map(id -> String.valueOf(id)).collect(Collectors.joining(","));
				sb.append(neighborIDs);
				sb.append("]");
				if (it.hasNext()) sb.append(", ");
			}
			//if (this.keySet().size() > 0) sb.delete(sb.length()-2, sb.length()-1);
			sb.append("]");
			return sb.toString();
		}
	}

	/**
	 * Calculate the winning proposal(s) with the Ranked Pairs voting method.
	 * 1. TALLY -   For each pair of proposals in the poll calculate the winner of the direct comparision
	 *              Which proposal has more preferences i&lt;j compared to j&gt;i.
	 * 2. SORT -    Sort these majorities by the number of preferences i over j
	 * 3. LOCK IN - For each of the sorted majorities: add the majority to a directed graph,
	 *              IF this edge does not introduce a circle in the graph.
	 * 4. WINNERS - The source of the tree, ie. the node with no incoming links is the winner of the poll.
	 * @param poll a poll that finished his voting phase
	 * @return the sorted list of winners. Best winner to least.
	 */
	public List<LawModel> calcRankedPairsWinners(PollModel poll) {
		Matrix duelMatrix = calcDuelMatrix(poll);

		// TALLY
		// one "majority"       := how often was candidate i preferred over j
		// list of "majorities" := sorted list of majorities i>j  with n votes
		List<int[]> majorities = new ArrayList<>();
		for (int i = 0; i < duelMatrix.getRows()-1; i++) {
			for (int j = i+1; j < duelMatrix.getCols(); j++) {
				int[] maj_ij = new int[] {i,j, duelMatrix.get(i,j)};
				int[] maj_ji = new int[] {j,i, duelMatrix.get(j,i)};
				majorities.add(maj_ij[2] > maj_ji[2] ? maj_ij : maj_ji);   // add the winner of this pair to the list of majorities
			}
		}

		// SORT
		majorities.sort(new MajorityComparator(duelMatrix));

		// LOCK IN
		DirectedGraph digraph = new DirectedGraph();
		for (int[] majority : majorities) {
			if (!digraph.reachable(majority[1], majority[0])) {
				digraph.addDirectedEdge(majority[0], majority[1]);
			}
		}
		// WINNERS
		Set<Integer> sourceIds = digraph.getSources();

		List<LawModel> winningProposals = new ArrayList<>();
		int i = 0;
		for(LawModel prop : poll.getProposals()) {
			if (sourceIds.contains(i)) winningProposals.add(prop);  i++;
		}

		return winningProposals;
	}



  //========= adapted from https://github.com/zephyr/schulze-voting/blob/master/src/java/dh/p/schulze/Election.java

  /**
   * Calculate the number of voters that prefer proposal i over proposal j for every i != j
   * @param poll a poll where voting was just finished
   * @return a two dimensional Matrix that contains the number of voters that prefer i over j
   */
  public Matrix calcDuelMatrix(@NonNull PollModel poll) {
    // First of all each proposal in the poll gets an array index (poll.proposals is a SortedSet)
		// Map: proposal -> array index of this proposal
		Map<LawModel, Integer> proposalIdx = new HashMap<>();
		int proposalIndex = 0;
		for(LawModel prop : poll.getProposals()) {
			proposalIdx.put(prop, proposalIndex++);
		}

		// For each different voteOrder that appears in all ballots we now fill three maps.
		// The map's key is always the unique hashCode of that voteOrder.

		// number of ballots with this specific vote order. Mapped by hashCode of voteOrder
		Map<Integer, Integer> numberOfBallots = new HashMap<>();

		// proposals that have been voted for. Each array contains indexes of proposals (from proposalIdx)
		Map<Integer, Integer[]> votedForMap = new HashMap<>();

		// proposals that nave NOT been voted for at all in this voteOrder.
		Map<Integer, Integer[]> notVotedForMap = new HashMap<>();

		//----- collect all ballots into buckets by their voteOrder
    for (BallotModel ballot : ballotRepo.findByPoll(poll)) {
			int key = ballot.getVoteOrder().hashCode();
			Integer numBallots = numberOfBallots.get(key);
			// when a voteOrder appears for the first time, then calc voteForIndexes and notVotedForIndexes
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

		//----- Fill the duelMatrix that stores the number of preferences i > j
		int n = poll.getNumCompetingProposals();
		Matrix duelMatrix = new Matrix(n, n);

		// for each type of voteOrder that was casted
		for(Integer key : votedForMap.keySet()) {
    	Integer[] idx = votedForMap.get(key);
    	// for each pair candidate i > candidate j in the voteOrder of these ballots
			for (int i = 0; i < idx.length-1; i++) {
				for (int j = i+1; j < idx.length; j++) {
					// add a preference i>j for that number of ballots
					duelMatrix.add(idx[i], idx[j], numberOfBallots.get(key));
				}
				// and add a preference i>k for each proposal k that was not voted for at all in this voteOrder
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
  public List<LawModel> calcSchulzeMethodWinners(@NonNull PollModel poll) {
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

