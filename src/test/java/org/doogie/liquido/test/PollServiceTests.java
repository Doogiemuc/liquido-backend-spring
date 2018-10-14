package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
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
	CastVoteService castVoteService;

	@Autowired
	TestDataCreator testDataCreator;

	@Autowired
	Environment springEnv;

	@Autowired
	LiquidoAuditorAware auditor;

	@Test
	public void testSchulzeMethode() throws LiquidoException {
		log.info("=========== testSchulzeMethode");

		// We need 45 voters
		String mailPrefix = "testSchulzeUser";
		Map<String, UserModel> users = testDataCreator.seedUsers(45, mailPrefix);
		AreaModel area = testDataCreator.getArea(0);
		String basePath = springEnv.getProperty("spring.data.rest.base-path");

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

		int count = 1;
		LawModel[] propsArray = poll.getProposals().stream().toArray(LawModel[]::new);

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
				String voterToken = castVoteService.createVoterToken(voter, area, "dummyPasswordHash");
				auditor.setMockAuditor(null);
				CastVoteRequest castVoteRequest = new CastVoteRequest(basePath+"/polls/"+poll.getId(), voteOrderURIs, voterToken);
				castVoteService.castVote(castVoteRequest);
			}

		}

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

		List<LawModel> winningProposals = pollService.calcPotentialWinners(poll);
		log.info("Potenetial Winners:" +winningProposals);

		assertTrue("There should only be one winner in this example", winningProposals.size() == 1);
		assertEquals(propsArray[4]+" should have one the election.", propsArray[4], winningProposals.get(0) );

		log.info("Schulze Method calculations SUCCESSFULL.");

	}
}
