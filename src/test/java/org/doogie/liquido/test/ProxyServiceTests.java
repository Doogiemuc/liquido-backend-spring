package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.ProxyService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.doogie.liquido.testdata.TestFixtures.*;
import static org.junit.Assert.*;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableJpaAuditing(auditorAwareRef = "liquidoAuditorAware")
@ActiveProfiles("test")  // this will also load the settings  from  application-test.properties
public class ProxyServiceTests {
	@Autowired
	UserRepo userRepo;

	@Autowired
	AreaRepo areaRepo;

	@Autowired
	DelegationRepo delegationRepo;

	@Autowired
	CastVoteService castVoteService;

	@Autowired
	ProxyService proxyService;


	/**
	 * GIVEN a public proxy P
	 *   AND a voter V
	 *  WHEN voter V assigns P as proxy in area A
	 *  THEN the proxyMap of V contains P in area A
	 */
	@Test
	@WithUserDetails(USER1_EMAIL)
	public void testAssignProxy() throws LiquidoException {
		//GIVEN
		UserModel fromUser = userRepo.findByEmail(USER2_EMAIL);
		UserModel toProxy  = userRepo.findByEmail(USER1_EMAIL);
		AreaModel area     = areaRepo.findByTitle(AREA1_TITLE);
		proxyService.becomePublicProxy(toProxy, area);
		//assert: check for checksum in DB,  or  receive it via getChecksumOfPublicProxy ?   or via repo?

		//WHEN
		String voterToken  = castVoteService.getVoterToken(fromUser, area);
		proxyService.assignProxy(area, fromUser, toProxy, voterToken);

		//THEN
		Map<AreaModel, UserModel> proxyMap = proxyService.getProxyMap(fromUser);
		log.info(proxyMap.toString());

		assertEquals(toProxy+"is proxy of "+fromUser+" in area "+area, toProxy, proxyMap.get(area));
	}

	/**
	 * GIVEN a voter that delegeated to a proxy
	 *   AND a chain of transitive delegations above that
	 *  WHEN we query for the topmost proxy in that chain
	 *  THEN the top most proxy at the end of the delegation chain is returned.
	 */
	@Test
	public void testGetTopmostProxy() {
		log.trace("testGetTopmostProxy");
		//GIVEN
		UserModel voter             = userRepo.findByEmail(USER5_EMAIL);
		UserModel expectedTopProxy  = userRepo.findByEmail(USER1_EMAIL);
		AreaModel area      			  = areaRepo.findByTitle(AREA0_TITLE);
		DelegationModel delegation  = delegationRepo.findByAreaAndFromUser(area, voter);

		//WHEN
		UserModel topmostProxy = proxyService.findTopmostProxy(delegation);

		//THEN
		assertEquals(expectedTopProxy, topmostProxy);
		log.trace("SUCCESS: topmost proxy was found correctly.");
	}


	/**
	 * GIVEN a public proxy P
	 *   AND a voter V
	 *  WHEN voter V assigned P as his proxy
	 *  THEN voter V should be allowed to assign proxy P (again)
	 *   AND proxy P must NOT be allowed to assign voter V as his proxy,
	 *       because this would lead to a circular delegation.
	 * @throws LiquidoException
	 */
	@Test
	public void testCircularDelegationErrorCases() throws LiquidoException {
		//GIVEN
		UserModel fromUser = userRepo.findByEmail(USER2_EMAIL);
		UserModel toProxy  = userRepo.findByEmail(USER1_EMAIL);
		AreaModel area     = areaRepo.findByTitle(AREA1_TITLE);
		proxyService.becomePublicProxy(toProxy, area);

		//WHEN
		String voterToken  = castVoteService.getVoterToken(fromUser, area);
		proxyService.assignProxy(area, fromUser, toProxy, voterToken);

		//THEN
		assertFalse("Normal delegation should be allowed", proxyService.thisWouldBeCircularDelegation(area, fromUser, toProxy));
		assertTrue("Circular delegation should be forbidden", proxyService.thisWouldBeCircularDelegation(area, toProxy, fromUser));
		try {
			proxyService.assignProxy(area, toProxy, fromUser, voterToken);
			fail("Trying to assign a proxy which leads to a circular delegation should have thrown a LiquidoException!");
		} catch(LiquidoException e) {
			if (e.getError() != LiquidoException.Errors.CANNOT_ASSIGN_CIRCULAR_PROXY)
				fail("Trying to assign a proxy which leads to a circular delegation should have thrown a LiquidoException with Error == CANNOT_ASSIGN_CIRCULAR_PROXY!");
		}

	}
}