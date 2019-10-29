package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.*;
import org.doogie.liquido.model.*;
import org.doogie.liquido.rest.dto.CastVoteRequest;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.PollService;
import org.doogie.liquido.services.ProxyService;
import org.doogie.liquido.services.voting.RankedPairVoting;
import org.doogie.liquido.services.voting.SchulzeMethod;
import org.doogie.liquido.testdata.TestDataCreator;
import org.doogie.liquido.testdata.TestFixtures;
import org.doogie.liquido.util.LiquidoRestUtils;
import org.doogie.liquido.util.Matrix;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

/**
 * Tests for {@link PollService}
 *
 * These tests rely on the test data (test fixtures) that is created by the {@link TestDataCreator}.
 * So you must load that data before running these tests.
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class PollServiceTests  extends BaseTest {

	@Autowired
	PollService pollService;

	@Autowired
	ProxyService proxyService;

	@Autowired
	CastVoteService castVoteService;

	@Autowired
	TestDataCreator testDataCreator;

	@Autowired
	AreaRepo areaRepo;

	@Autowired
	LawRepo lawRepo;

	@Autowired
	PollRepo pollRepo;

	@Autowired
	BallotRepo ballotRepo;

	@Autowired
	ChecksumRepo checksumRepo;

	@Autowired
	LiquidoRestUtils restUtils;

	@Autowired
	Environment springEnv;

	@Value("${liquido.admin.email}")
	String adminEmail;

	@Value("spring.data.rest.base-path")
	public String basePath;

	/**
	 * This test creates a new poll. This makes it a bit slow, but we need a given combination of ballots.
	 * @throws LiquidoException
	 */
	@Test
	public void testSchulzeMethode() throws LiquidoException {
		log.info("=========== testSchulzeMethode");

		// We need 45 voters
		AreaModel area = areaRepo.findByTitle(TestFixtures.AREA1_TITLE);
		PollModel poll = testDataCreator.seedPollInVotingPhase(area, 5);

		// These numbers are from the example on wikipedia https://de.wikipedia.org/wiki/Schulze-Methode
		// The last proposal (ID = 4) will win the election as the sole winner.
		int[][] voteOrderIndexes = new int[8][5];
		voteOrderIndexes[0] = new int[] { 0, 2, 1, 4, 3 };		// voteOrder:  A > C > B > E > D
		voteOrderIndexes[1] = new int[] { 0, 3, 4, 2, 1 };
		voteOrderIndexes[2] = new int[] { 1, 4, 3, 0, 2 };
		voteOrderIndexes[3] = new int[] { 2, 0, 1, 4, 3 };
		voteOrderIndexes[4] = new int[] { 2, 0, 4, 1, 3 };
		voteOrderIndexes[5] = new int[] { 2, 1, 0, 3, 4 };
		voteOrderIndexes[6] = new int[] { 3, 2, 4, 1, 0 };
		voteOrderIndexes[7] = new int[] { 4, 1, 0, 3, 2 };

		int[] numBallots = new int[] { 5, 5, 8, 3, 7, 2, 7, 8 };

		LawModel[] propsArray = poll.getProposals().stream().toArray(LawModel[]::new);
		List<BallotModel> ballots = seedBallotsQuickly(poll, voteOrderIndexes, numBallots);

		Matrix duel = RankedPairVoting.calcDuelMatrix(poll, ballots);
		log.info("Duel matrix:\n"+duel.toString());

		// Some checks for random positions of the result
		assertEquals( 0, duel.get(0, 0));
		assertEquals(25, duel.get(1, 0));
		assertEquals(19, duel.get(2, 0));
		assertEquals(15, duel.get(3, 0));
		assertEquals(23, duel.get(4, 0));

		assertEquals(30, duel.get(0, 3));
		assertEquals(33, duel.get(1, 3));
		assertEquals(17, duel.get(2, 3));
		assertEquals( 0, duel.get(3, 3));
		assertEquals(31, duel.get(4, 3));

		Matrix strong = SchulzeMethod.calcStrongestPathMatrix(poll, ballots);
		log.info("Strongest path matrix:\n"+strong.toString());

		List<LawModel> winningProposals = SchulzeMethod.calcSchulzeMethodWinners(poll, ballots);
		log.info("Potential Winners:" +winningProposals);

		assertTrue("There should only be one winner in this example", winningProposals.size() == 1);
		assertEquals(propsArray[4]+" should have one the election.", propsArray[4], winningProposals.get(0) );

		log.info("Schulze Method calculations SUCCESSFUL.");

	}


	/**
	 * Quick and dirty hack to QUICKLY cast a vote.  NO CHECKS are performed at all.
	 * VoterToken and Checksum will not be real BCRYPT values but static dummies.
	 * Because BCRYPT hashing is the slow part of casting a vote.
	 *
	 * The correct call for all this would be:   castVoteService.castVote(castVoteRequest);
	 *
	 * @param voterToken
	 * @param poll
	 * @param voteOrder
	 * @return
	 * @throws LiquidoException
	 */
	BallotModel quickNdirtyCastVote(String voterToken, PollModel poll, List<LawModel> voteOrder) {
		String proposalIds = voteOrder.stream().map(law->law.getId().toString()).collect(Collectors.joining(","));
		log.debug("quickNDirtyCastVote(voterToken="+voterToken+", poll.id="+poll.getId()+" voteOrder(proposal.ids)=["+proposalIds+"]");
		String tokenChecksum = "dummyChecksumFor "+voterToken;   //castVoteService.calcChecksumFromVoterToken(voterToken);
  	AreaModel area = areaRepo.findByTitle(TestFixtures.AREA1_TITLE);
		ChecksumModel checksumModel = new ChecksumModel(tokenChecksum, area);
		checksumRepo.save(checksumModel);   // must save
		int level = 0;
		BallotModel ballot = new BallotModel(poll, level, voteOrder, checksumModel);
		return ballotRepo.save(ballot);
		//
	}

	/**
	 * Quickly create many ballots. This is only used for testing. It does not check or validate any voterTokens.
	 * We simply create dummy voter tokens
	 *
	 * @param poll a new poll in voting phase
	 * @param voteOrderIndexes list of voteOrders (inner index is index of proposal in poll.getProposals() starting at 0)
	 * @param numBallots How many times each voteOrder should be casted
	 * @return the list of casted ballots
	 * @throws LiquidoException
	 */
	List<BallotModel> seedBallotsQuickly(PollModel poll, int[][] voteOrderIndexes, int[] numBallots) throws LiquidoException {
		int countBallots = Arrays.stream(numBallots).sum();
		long numMissingUsers = countBallots - testDataCreator.countUsers();
		if (numMissingUsers > 0) {
			log.debug("Seeding "+numMissingUsers + " more users to seed ballots quickly");
			testDataCreator.seedUsers(numMissingUsers, "poll"+poll.getId()+"TestUser");   // we need seperate users for each poll
		}

		if (testDataCreator.countUsers() < countBallots)
			throw new RuntimeException("Cannot seed " + countBallots + " ballots because we only have "+testDataCreator.countUsers()+ " users");

		LawModel[] propsArray = poll.getProposals().stream().toArray(LawModel[]::new);
		List<BallotModel> ballots = new ArrayList<>();
		int count = 0;
		for (int i = 0; i < voteOrderIndexes.length; i++) {
			List<LawModel> voteOrder = new ArrayList<>();
			for (int j = 0; j < voteOrderIndexes[i].length; j++) {
				voteOrder.add(propsArray[voteOrderIndexes[i][j]]);
			}
			String proposalIds = voteOrder.stream().map(law->law.getId().toString()).collect(Collectors.joining(","));
			log.debug("----- seeding "+numBallots[i]+" ballots with voteOrder(proposal.ids)=["+proposalIds+"]");
			for (int k = 0; k < numBallots[i]; k++) {
				UserModel voter = testDataCreator.getUser(count);
				auditor.setMockAuditor(voter);

				// correct but slow:
				//String voterToken = castVoteService.createVoterTokenAndStoreChecksum(voter, poll.getArea(), "dummyPasswordHash");
				//auditor.setMockAuditor(null);
				//CastVoteRequest castVoteRequest = new CastVoteRequest(basePath+"/polls/"+poll.getId(), voteOrderURIs, voterToken);
				//castVoteService.castVote(castVoteRequest);

				//crude quick'n'dirty hack to make it QUICK!
				BallotModel ballot = quickNdirtyCastVote("$2dummyVoterToken" + count, poll, voteOrder);// voterTokens must start with $2 !!!
				ballots.add(ballot);
				count++;
			}

		}
		return ballots;
	}

	/**
	 * Test calculating of winner with "Ranked Pair" method.
	 * These cities and numbers in this methods are from the example on wikipedia https://en.wikipedia.org/wiki/Ranked_pairs
	 * @throws LiquidoException
	 */
	@Test
	public void testRankedPairs() throws LiquidoException {
		String[] cities = new String[] {"Memphis", "Nashville", "Knoxville", "Chattanooga"};
		AreaModel area = areaRepo.findByTitle(TestFixtures.AREA1_TITLE);
		PollModel poll = testDataCreator.seedPollInVotingPhase(area, cities.length);

		int ll = 0;
		for(LawModel prop : poll.getProposals()) {
			prop.setTitle(cities[ll++]);
		}
		pollRepo.save(poll);

		// Example data for 100 ballots
		int[][] voteOrderIndexes = new int[4][4];
		voteOrderIndexes[0] = new int[]{0, 1, 3, 2};  //index  0 is first proposal !
		voteOrderIndexes[1] = new int[]{1, 3, 2, 0};
		voteOrderIndexes[2] = new int[]{3, 2, 1, 0};
		voteOrderIndexes[3] = new int[]{2, 3, 1, 0};
		int[] numBallots = new int[] { 42, 26, 15, 17 };   // 100 ballots == 100%

		/* This is data for the edge case when there is only one ballot
		voteOrderIndexes[0] = new int[]{0};
		numBallots = new int[] { 1, 0, 0, 0 };
    */

		List<BallotModel> ballots = seedBallotsQuickly(poll, voteOrderIndexes, numBallots);
		Matrix duelMatrix = RankedPairVoting.calcDuelMatrix(poll, ballots);
		LawModel winner = RankedPairVoting.calcRankedPairsWinner(poll, duelMatrix);

		log.debug("===== Votes ");
		for (int i = 0; i < voteOrderIndexes.length; i++) {
			StringBuffer sb = new StringBuffer();
			sb.append(numBallots[i]+" ballots voted ");
			for (int j = 0; j < voteOrderIndexes[i].length; j++) {
				sb.append(cities[voteOrderIndexes[i][j]]);
				sb.append(" > ");
			}
			log.debug(sb.toString());
		}

		log.debug("===== duelMatrix");
		log.debug(duelMatrix.toString());
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < duelMatrix.getRows(); i++) {
			sb.append(cities[i] + ": [");
			for (int j = 0; j < duelMatrix.getCols(); j++) {
				sb.append(duelMatrix.get(i,j));
				if (j < duelMatrix.getCols()-1) sb.append(",");
			}
			sb.append("]\n");
		}
		System.out.println(sb.toString());

		log.debug("Winner: "+winner);
		assertEquals(cities[1]+" should have won the poll", cities[1], winner.getTitle());

		// We could delete the created dummy poll now.

		log.info("testRankedPairs SUCCESSFUL.");
	}

	@Test
	public void testRankedPairs2() throws LiquidoException {
		String[] cities = new String[] {"Memphis", "Nashville", "Knoxville", "Chattanooga"};
		AreaModel area = areaRepo.findByTitle(TestFixtures.AREA1_TITLE);
		PollModel poll = testDataCreator.seedPollInVotingPhase(area, cities.length);

		int ll = 0;
		Map<Integer, Long> mapIndexToId = new HashMap();
		HashMap<Long, String> mapId2Name = new HashMap<>();
		for(LawModel prop : poll.getProposals()) {
			prop.setTitle(cities[ll]);
			mapIndexToId.put(ll, prop.getId());
			mapId2Name.put(prop.getId(), cities[ll]);
			ll++;
		}
		pollRepo.save(poll);

		// Example data for 100 ballots
		int[][] voteOrderIndexes = new int[4][4];
		voteOrderIndexes[0] = new int[]{0, 1, 3, 2};  //index  0 is first proposal !
		voteOrderIndexes[1] = new int[]{1, 3, 2, 0};
		voteOrderIndexes[2] = new int[]{3, 2, 1, 0};
		voteOrderIndexes[3] = new int[]{2, 3, 1, 0};
		int[] numBallots = new int[] { 42, 26, 15, 17 };   // 100 ballots == 100%

		List<BallotModel> ballots = seedBallotsQuickly(poll, voteOrderIndexes, numBallots);
		List<Long> allIds = poll.getProposals().stream().map(p -> p.getId()).collect(Collectors.toList());
		Long winnerId = RankedPairVoting.calcRankedPairWinner2(allIds, ballots);

		log.debug("===== Votes ");
		for (int i = 0; i < voteOrderIndexes.length; i++) {
			StringBuffer sb = new StringBuffer();
			sb.append(numBallots[i]+" ballots voted    ");
			for (int j = 0; j < voteOrderIndexes[i].length; j++) {
				sb.append(cities[voteOrderIndexes[i][j]]);
				sb.append("(id=");
				sb.append(mapIndexToId.get(voteOrderIndexes[i][j]));
				sb.append(") > ");
			}
			log.debug(sb.toString());
		}

		log.debug("Winner : "+mapId2Name.get(winnerId)+"(id="+winnerId+")");
		assertEquals(cities[1]+" should have won the poll", cities[1], mapId2Name.get(winnerId));

		log.info("testRankedPairs SUCCESSFUL.");
	}

	/**
	 * Test effective proxy in combination with transitive and non-transitive delegations
	 * @throws LiquidoException
	 */
	@Test
	public void testFindEffectiveProxy() throws LiquidoException {
		String voterToken;
		CastVoteRequest castVoteRequest;
		UserModel voter;
		Optional<UserModel> effectiveProxy;
		UserModel expectedProxy;

		// GIVEN a poll in voting
		AreaModel area = areaRepo.findByTitle(TestFixtures.AREA_FOR_DELEGATIONS);
		PollModel poll = testDataCreator.seedPollInVotingPhase(area, 3);
		String pollURI = basePath + "/polls/" + poll.getId();

    //  AND a dummy voteOrder
		List<String> voteOrder = new ArrayList<>();
		Iterator<LawModel> iterator = poll.getProposals().iterator();
		voteOrder.add(basePath + "/laws/" + iterator.next().getId());
		voteOrder.add(basePath + "/laws/" + iterator.next().getId());

		// WHEN USER1_EMAIL casts his vote with a dummy voteOrder (the topProxy)
		voter = testDataCreator.getUser(TestFixtures.USER1_EMAIL);
		voterToken = castVoteService.createVoterTokenAndStoreChecksum(voter, area, TestFixtures.USER_TOKEN_SECRET, false);
		castVoteRequest = new CastVoteRequest(pollURI, voteOrder, voterToken);
		castVoteService.castVote(castVoteRequest);

		//  AND a USER4_EMAIL casts his vote with a dummy voteOrder (a proxy in the middle)
		voter = testDataCreator.getUser(TestFixtures.USER4_EMAIL);
		voterToken = castVoteService.createVoterTokenAndStoreChecksum(voter, area, TestFixtures.USER_TOKEN_SECRET, false);
		castVoteRequest = new CastVoteRequest(pollURI, voteOrder, voterToken);
		castVoteService.castVote(castVoteRequest);

		// THEN the effective proxy of USER10_EMAIL should be USER4_EMAIL (this proxy in the middle)
		voter = testDataCreator.getUser(TestFixtures.USER10_EMAIL);
		expectedProxy = testDataCreator.getUser(TestFixtures.USER4_EMAIL);
		voterToken= castVoteService.createVoterTokenAndStoreChecksum(voter, area, TestFixtures.USER_TOKEN_SECRET, false);
		effectiveProxy = pollService.findEffectiveProxy(poll, voter, voterToken);
		assertTrue(voter + " should have an effective proxy", effectiveProxy.isPresent());
		assertEquals(expectedProxy.toStringShort() + "should be the effective proxy of "+voter.toStringShort(), expectedProxy, effectiveProxy.get());
		log.info("SUCCESS: " + expectedProxy.toStringShort() + " is the effective proxy of "+ voter.toStringShort()  + " in poll.id="+poll.getId());

		// AND USER12_EMAIL should NOT have an effective proxy (because his delegation is non-transitive)
		voter = testDataCreator.getUser(TestFixtures.USER12_EMAIL);
		voterToken= castVoteService.createVoterTokenAndStoreChecksum(voter, area, TestFixtures.USER_TOKEN_SECRET, false);
		effectiveProxy = pollService.findEffectiveProxy(poll, voter, voterToken);
		assertFalse(voter + " should not have an effective proxy", effectiveProxy.isPresent());
		log.info("SUCCESS: " + voter  + " should not yet have an effective proxy, because his non-transitive direct proxy did not vote yet in poll.id="+poll.getId());

		// WHEN VOTER7_EMAIL casts his vote with a dummy voteOrder
		voter = testDataCreator.getUser(TestFixtures.USER7_EMAIL);
		voterToken = castVoteService.createVoterTokenAndStoreChecksum(voter, area, TestFixtures.USER_TOKEN_SECRET, false);
		castVoteRequest = new CastVoteRequest(pollURI, voteOrder, voterToken);
		castVoteService.castVote(castVoteRequest);

		// THEN the effective proxy of USER12_EMAIL should be USER7_EMAIL (his non-transitive direct proxy)
		voter = testDataCreator.getUser(TestFixtures.USER12_EMAIL);
		expectedProxy = testDataCreator.getUser(TestFixtures.USER7_EMAIL);
		voterToken = castVoteService.createVoterTokenAndStoreChecksum(voter, area, TestFixtures.USER_TOKEN_SECRET, false);
		effectiveProxy = pollService.findEffectiveProxy(poll, voter, voterToken);
		assertTrue(voter + " should now have an effective proxy", effectiveProxy.isPresent());
		assertEquals(expectedProxy.toStringShort() + "should be the effective proxy of "+voter.toStringShort(), expectedProxy, effectiveProxy.get());
		log.info("SUCCESS: " + expectedProxy.toStringShort() + " now is the effective proxy of "+ voter.toStringShort()  + " in poll.id="+poll.getId());
		log.info("===== testFindEffectiveProxy SUCCESS");
	}

	/** When there is no vote, then there must not be a winner! (came from a BUGFIX) */
	@Test
	public void testFinishVotingPhaseWithoutVotes() throws LiquidoException {
		// GIVEN a poll in voting phase
		AreaModel area = areaRepo.findByTitle(TestFixtures.AREA0_TITLE);
		PollModel poll = testDataCreator.seedPollInVotingPhase(area, 2);

		// WHEN we finish the voting phase of this poll
		LawModel winner = pollService.finishVotingPhase(poll);

		// THEN there should not be any winner of this poll.
		assertNull("There should not be any winner in a poll without votes", winner);
	}


	/**
	 * Test for {@link PollService#deletePoll(PollModel)} that a poll can be deleted.
	 * WHEN a poll is deleted, THEN its casted ballots are also removed, but not the proposals in it.
	 * @throws LiquidoException when admin user is not available in DB
	 */
	@Test
	public void deletePollTest() throws LiquidoException {
		// GIVEN a poll with votes in it
		AreaModel area = areaRepo.findByTitle(TestFixtures.AREA0_TITLE);
		PollModel poll = testDataCreator.seedPollInVotingPhase(area, 2);
		testDataCreator.seedVotes(poll, 3);

		Long firstProposalId = poll.getProposals().iterator().next().getId();

		// WHEN login as ADMIN
		this.loginUser(adminEmail);

		//  AND poll is deleted
		pollService.deletePoll(poll);

		// THEN the proposals from the former poll still exist
		Optional<LawModel> prop = lawRepo.findById(firstProposalId);
		assertTrue("Proposal from former poll should still extist.", prop.isPresent());
	}

}
