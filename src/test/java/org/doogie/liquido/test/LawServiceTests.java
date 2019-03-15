package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.rest.dto.LawQuery;
import org.doogie.liquido.services.LawService;
import org.doogie.liquido.testdata.TestFixtures;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class LawServiceTests extends BaseTest {

	@Autowired
	AreaRepo areaRepo;

	@Autowired
	LawRepo lawRepo;

	@Autowired
	LawService lawService;

	@Test
	public void testFindBySearchQuery() {
		// GIVEN
		LawQuery lawQuery = new LawQuery();
		lawQuery.setSearchText(Optional.of(TestFixtures.LAW_TITLE));
		lawQuery.setStatus(Optional.of(LawModel.LawStatus.LAW));

		// WHEN
		Page<LawModel> page = lawService.findBySearchQuery(lawQuery);

		// THEN
		Assert.assertTrue(page != null);
		Assert.assertTrue(page.getTotalElements() > 0);
		log.debug("========== matched laws");
		page.get().forEach((law) -> {
			log.debug(law.toString());
		});
	}

	@Test
	public void testFindBySearchQueryNegative() {
		// GIVEN
		LawQuery lawQuery = new LawQuery();
		lawQuery.setUpdatedAfter(Optional.of(new Date(System.currentTimeMillis()+100*24*3600*1000)));   // 100 days in the future

		// WHEN
		Page<LawModel> page = lawService.findBySearchQuery(lawQuery);

		// THEN
		Assert.assertTrue(page != null);
		Assert.assertTrue(page.getTotalElements() == 0);

	}


}
