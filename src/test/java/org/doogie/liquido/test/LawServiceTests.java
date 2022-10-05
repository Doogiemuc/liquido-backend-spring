package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.datarepos.OffsetLimitPageable;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.dto.LawQuery;
import org.doogie.liquido.services.LawService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.testdata.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link LawService}
 * Here we mostly test the advanced search capabilities.
 */
@Slf4j
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
		assertTrue(page != null);
		assertTrue(page.getTotalElements() > 0);
	}

	@Test
	public void testFindEmptyResult() {
		// GIVEN
		LawQuery lawQuery = new LawQuery();
		lawQuery.setUpdatedAfter(new Date(System.currentTimeMillis()+100*24*3600*1000));   // 100 days in the future

		// WHEN
		Page<LawModel> page = lawService.findBySearchQuery(lawQuery);

		// THEN
		assertTrue(page != null);
		assertTrue(page.getTotalElements() == 0);
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
		assertEquals(firstLaw.getCreatedBy().getName(), TestFixtures.USER1_NAME);
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
		assertEquals(TestFixtures.IDEA_0_TITLE, firstLaw.getTitle());
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
		assertTrue(creator.isPresent());
		assertEquals(creator.get(), firstLaw.getCreatedBy());
	}

	@Test
	//MAYBE:  https://www.baeldung.com/spring-boot-data-sql-and-schema-sql    <=  @Sql annotation can be used on test classes or methods to execute SQL scripts.
	public void testFindBySupporter() throws LiquidoException {
		// GIVEN an idea that is supported by user1
		UserModel supporter = userRepo.findByEmail(TestFixtures.USER1_EMAIL)
			.orElseThrow(() -> new LiquidoException(LiquidoException.Errors.CANNOT_ADD_SUPPORTER, "TEST: Cannot find "+TestFixtures.USER1_EMAIL));
		LawModel idea = getFirstResult(lawRepo.findByStatus(LawModel.LawStatus.PROPOSAL, new OffsetLimitPageable(0,1)));
		lawService.addSupporter(supporter, idea);

		// AND a query for this idea
		LawQuery lawQuery = new LawQuery();
		lawQuery.setStatusList(Arrays.asList(LawModel.LawStatus.PROPOSAL));
		lawQuery.setSupportedByEMail(TestFixtures.USER1_EMAIL);

		// WHEN
		Page<LawModel> page = lawService.findBySearchQuery(lawQuery);

		// THEN
		log.debug("========== Proposals supported by "+TestFixtures.USER1_EMAIL);
		page.get().forEach((law) -> {
			log.debug(law.toString());
		});
		LawModel firstLaw = getFirstResult(page);

		assertTrue(firstLaw.getSupporters().contains(supporter));
	}

	private LawModel getFirstResult(Page<LawModel> page) {
		assertTrue(page != null, "Expected to get a page");
		assertTrue(page.getTotalElements() > 0, "Expected to get at least one result");
		Optional<LawModel> firstLaw = page.get().findFirst();
		assertTrue(firstLaw.isPresent(), "Expected to have a first element in page");
		return firstLaw.get();
	}


}
