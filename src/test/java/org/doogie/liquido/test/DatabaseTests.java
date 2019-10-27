package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.util.LiquidoProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class DatabaseTests extends BaseTest {
	@Autowired
	Environment springEnv;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	LiquidoProperties props;

	@Autowired
	PollRepo pollRepo;

	@Autowired
	LawRepo lawRepo;

	@Test
	public void testDeletePoll() {
		// GIVEN a poll in ELABORATION
		List<PollModel> polls = pollRepo.findByStatus(PollModel.PollStatus.ELABORATION);
		if (polls.size() < 1) throw new RuntimeException("Need at least  one poll for testDeletePoll");
		PollModel poll = polls.get(0);
		Long firstPropId = poll.getProposals().first().getId();

		// WHEN poll is deleted
		for (LawModel prop : poll.getProposals()) {
			prop.setPoll(null);																// Need to unlink proposals from poll first!
			lawRepo.save(prop);
		}
		pollRepo.delete(poll);

		// THEN its proposals should not be deleted
		Optional<LawModel> prop = lawRepo.findById(firstPropId);

		assertTrue("Proposal should not be deleted, after poll has been deleted", prop.isPresent());

		//Remark: For a Poll in VOTING phase, the Ballots are not deleted when a poll is deleted, because they are not linked from the poll.
	}
}
