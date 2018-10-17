package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.CastVoteRequest;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.PollService;
import org.doogie.liquido.testdata.TestDataCreator;
import org.doogie.liquido.util.Matrix;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableJpaAuditing(auditorAwareRef = "liquidoAuditorAware")
@ActiveProfiles("test")  // this will also load the settings  from  application-test.properties
public class PollServiceTests {

	@Autowired
	PollService pollService;

	@Autowired
	PollRepo pollRepo;

	@Autowired
	CastVoteService castVoteService;

	@Autowired
	TestDataCreator testDataCreator;

	@Autowired
	Environment springEnv;

	@Autowired
	LiquidoAuditorAware auditor;

	public static final int NUM_USERS = 100;
	public static final String mailPrefix = "pollTestUser";
	public static Map<String, UserModel> users;

	public String basePath ;

	@Before
	public void seedUsersForThisTest() {
		if (users == null) {
			log.debug("Seeding " + NUM_USERS + " users");
			PollServiceTests.users = testDataCreator.seedUsers(NUM_USERS, mailPrefix);
		} else {
			log.debug("Using existing "+users.size()+" users ");
		}
		basePath = springEnv.getProperty("spring.data.rest.base-path");
	}

	@Test
	public void testSchulzeMethode() throws LiquidoException {
		log.info("=========== testSchulzeMethode");

		// We need 45 voters
		PollModel poll = testDataCreator.seedPollInVotingPhase(5);

		// These numbers are from the example on wikipedia https://de.wikipedia.org/wiki/Schulze-Methode
		// The last proposal (ID = 5) will win the election as the sole winner.
		int[][] voteOrderIndexes = new int[8][5];
		voteOrderIndexes[0] = new int[] { 1, 3, 2, 5, 4 };		// voteOrder:  A > C > B > E > D
		voteOrderIndexes[1] = new int[] { 1, 4, 5, 3, 2 };
		voteOrderIndexes[2] = new int[] { 2, 5, 4, 1, 3 };
		voteOrderIndexes[3] = new int[] { 3, 1, 2, 5, 4 };
		voteOrderIndexes[4] = new int[] { 3, 1, 5, 2, 4 };
		voteOrderIndexes[5] = new int[] { 3, 2, 1, 4, 5 };
		voteOrderIndexes[6] = new int[] { 4, 3, 5, 2, 1 };
		voteOrderIndexes[7] = new int[] { 5, 2, 1, 4, 3 };

		int[] numBallots = new int[] { 5, 5, 8, 3, 7, 2, 7, 8 };

		LawModel[] propsArray = poll.getProposals().stream().toArray(LawModel[]::new);
		seedBallots(poll, voteOrderIndexes, numBallots);

		Matrix duel = pollService.calcDuelMatrix(poll);
		log.info("Duel matrix:\n"+duel.toFormattedString());

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

		Matrix strong = pollService.calcStrongestPathMatrix(poll);
		log.info("Strongest path matrix:\n"+strong.toFormattedString());

		List<LawModel> winningProposals = pollService.calcSchulzeMethodWinners(poll);
		log.info("Potenetial Winners:" +winningProposals);

		assertTrue("There should only be one winner in this example", winningProposals.size() == 1);
		assertEquals(propsArray[4]+" should have one the election.", propsArray[4], winningProposals.get(0) );

		log.info("Schulze Method calculations SUCCESSFULL.");

	}

	private LawModel[] seedBallots(PollModel poll, int[][] voteOrderIndexes, int[] numBallots) throws LiquidoException {
		LawModel[] propsArray = poll.getProposals().stream().toArray(LawModel[]::new);
		int count = 1;
		for (int i = 0; i < voteOrderIndexes.length; i++) {
			List<String> voteOrderURIs = new ArrayList<>();
			for (int j = 0; j < voteOrderIndexes[i].length; j++) {
				voteOrderURIs.add(basePath+"/laws/"+propsArray[voteOrderIndexes[i][j]-1].getId());
			}
			log.debug("----- seeding "+numBallots[i]+" ballots with voteOrder "+voteOrderIndexes[i]);
			for (int k = 0; k < numBallots[i]; k++) {
				UserModel voter = users.get(mailPrefix+count+"@liquido.de");
				count++;
				auditor.setMockAuditor(voter);
				//TODO: create dummy vote tooken more quickly for testing. Do not forget to also create valid checksum
				String voterToken = castVoteService.createVoterToken(voter, poll.getArea(), "dummyPasswordHash");
				auditor.setMockAuditor(null);
				CastVoteRequest castVoteRequest = new CastVoteRequest(basePath+"/polls/"+poll.getId(), voteOrderURIs, voterToken);
				castVoteService.castVote(castVoteRequest);
			}

		}
		return propsArray;
	}

	@Test
	public void testRankedPairs() throws LiquidoException {
		log.info("=========== testRankedPairs");

		// These cities and numbers are from the example on wikipedia https://en.wikipedia.org/wiki/Ranked_pairs
		String[] cities = new String[] {"Memphis", "Nashville", "Knoxville", "Chattanooga"};

		PollModel poll = testDataCreator.seedPollInVotingPhase(cities.length);

		int ll = 0;
		for(LawModel prop : poll.getProposals()) {
			prop.setTitle(cities[ll++]);
		}
		pollRepo.save(poll);

		// Example data for 100 ballots
		int[][] voteOrderIndexes = new int[4][4];
		voteOrderIndexes[0] = new int[]{1, 2, 4, 3};  //indexes starting at 1 !
		voteOrderIndexes[1] = new int[]{2, 4, 3, 1};
		voteOrderIndexes[2] = new int[]{4, 3, 2, 1};
		voteOrderIndexes[3] = new int[]{3, 4, 2, 1};
		int[] numBallots = new int[] { 42, 26, 15, 17 };   // 100 ballots == 100%

		LawModel[] lawModels = seedBallots(poll, voteOrderIndexes, numBallots);

		List<LawModel> winners = pollService.calcRankedPairsWinners(poll);

		log.debug("===== Votes ");
		for (int i = 0; i < voteOrderIndexes.length; i++) {
			StringBuffer sb = new StringBuffer();
			sb.append(numBallots[i]+" ballots voted ");
			for (int j = 0; j < voteOrderIndexes[i].length; j++) {
				sb.append(cities[voteOrderIndexes[i][j]-1]);
				sb.append(" > ");
			}
			log.debug(sb.toString());
		}


		assertTrue("There should be one winner with this example data", winners.size() == 1);
		assertEquals("Nashville should have won the poll", lawModels[1], winners.get(0));

		log.info("Winner is "+winners.get(0).getTitle());

		log.info("testRankedPairs SUCCESSFUL.");
	}
}
