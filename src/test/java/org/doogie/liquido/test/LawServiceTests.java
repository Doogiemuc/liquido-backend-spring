package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.LawQuery;
import org.doogie.liquido.services.LawService;
import org.doogie.liquido.testdata.TestFixtures;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

/**
 * Thests for {@link LawService}
 * Here we mostly test the advanced search capabilities.
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class LawServiceTests extends BaseTest {

	@Autowired
	AreaRepo areaRepo;

	@Autowired
	LawRepo lawRepo;

	@Autowired
	UserRepo userRepo;

	@Autowired
	LawService lawService;

	@Test
	public void testFindLaw() {
		// GIVEN
		LawQuery lawQuery = new LawQuery();
		lawQuery.setStatusList(Arrays.asList(LawModel.LawStatus.LAW));

		// WHEN
		Page<LawModel> page = lawService.findBySearchQuery(lawQuery);

		// THEN
		log.debug("========== matched laws");
		page.get().forEach((law) -> {
			log.debug(law.toString());
		});
		Assert.assertTrue(page != null);
		Assert.assertTrue(page.getTotalElements() > 0);
	}

	@Test
	public void testFindEmptyResult() {
		// GIVEN
		LawQuery lawQuery = new LawQuery();
		lawQuery.setUpdatedAfter(new Date(System.currentTimeMillis()+100*24*3600*1000));   // 100 days in the future

		// WHEN
		Page<LawModel> page = lawService.findBySearchQuery(lawQuery);

		// THEN
		Assert.assertTrue(page != null);
		Assert.assertTrue(page.getTotalElements() == 0);
	}

	@Test
	public void testFindBySearchText() {
		// GIVEN
		LawQuery lawQuery = new LawQuery();
		lawQuery.setSearchText(TestFixtures.USER1_NAME);

		// WHEN
		Page<LawModel> page = lawService.findBySearchQuery(lawQuery);

		// THEN
		log.debug("========== matched laws");
		page.get().forEach((law) -> {
			log.debug(law.toString());
		});
		LawModel firstLaw = getFirstResult(page);
		Assert.assertEquals(firstLaw.getCreatedBy().getProfile().getName(), TestFixtures.USER1_NAME);
	}


	@Test
	public void testFindBySearchQueryWithSorting() {
		// GIVEN
		LawQuery lawQuery = new LawQuery();
		lawQuery.setStatusList(Arrays.asList(LawModel.LawStatus.IDEA));
		lawQuery.setSingleSortProperty("title");
		lawQuery.setDirection(Sort.Direction.ASC);

		// WHEN
		Page<LawModel> page = lawService.findBySearchQuery(lawQuery);

		// THEN
		log.debug("========== sorted laws");
		page.get().forEach((law) -> {
			log.debug(law.toString());
		});
		LawModel firstLaw = getFirstResult(page);
		Assert.assertEquals(TestFixtures.IDEA_0_TITLE, firstLaw.getTitle());
	}

	@Test
	public void testFindByCreator() {
		// GIVEN
		LawQuery lawQuery = new LawQuery();
		lawQuery.setStatusList(Arrays.asList(LawModel.LawStatus.IDEA));
		lawQuery.setCreatedByEmail(TestFixtures.USER1_EMAIL);

		// WHEN
		Page<LawModel> page = lawService.findBySearchQuery(lawQuery);
		Optional<UserModel> creator = userRepo.findByEmail(TestFixtures.USER1_EMAIL);

		// THEN
		log.debug("========== laws created by "+TestFixtures.USER1_EMAIL);
		page.get().forEach((law) -> {
			log.debug(law.toString());
		});
		LawModel firstLaw = getFirstResult(page);
		Assert.assertTrue(creator.isPresent());
		Assert.assertEquals(creator.get(), firstLaw.getCreatedBy());
	}

	@Test
	public void testFindBySupporter() {
		// GIVEN
		LawQuery lawQuery = new LawQuery();
		lawQuery.setStatusList(Arrays.asList(LawModel.LawStatus.IDEA));
		lawQuery.setSupportedByEMail(TestFixtures.USER1_EMAIL);

		// WHEN
		Page<LawModel> page = lawService.findBySearchQuery(lawQuery);
		Optional<UserModel> supporter = userRepo.findByEmail(TestFixtures.USER1_EMAIL);

		// THEN
		log.debug("========== laws supported by "+TestFixtures.USER1_EMAIL);
		page.get().forEach((law) -> {
			log.debug(law.toString());
		});
		LawModel firstLaw = getFirstResult(page);

		Assert.assertTrue(supporter.isPresent());
		Assert.assertTrue(firstLaw.getSupporters().contains(supporter.get()));
	}

	private LawModel getFirstResult(Page<LawModel> page) {
		Assert.assertTrue("Expected to get a page", page != null);
		Assert.assertTrue("Expected to get at least one result", page.getTotalElements() > 0);
		Optional<LawModel> firstLaw = page.get().findFirst();
		Assert.assertTrue("Expected to have a first element in page", firstLaw.isPresent());
		return firstLaw.get();
	}


}
