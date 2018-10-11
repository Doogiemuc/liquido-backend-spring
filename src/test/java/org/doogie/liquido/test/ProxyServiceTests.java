package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.TokenChecksumModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.ProxyService;
import org.doogie.liquido.testdata.TestFixtures;
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
	 *  THEN the new delegation points from V to P
	 *   AND the user's checksumModel is delegatedTo P's checksum
	 *   AND the proxyMap of V contains P in area A
	 */
	@Test
	@WithUserDetails(USER2_EMAIL)
	public void testAssignPublicProxy() throws LiquidoException {
		//GIVEN
		UserModel fromUser = userRepo.findByEmail(USER2_EMAIL);
		UserModel toProxy  = userRepo.findByEmail(USER1_EMAIL);
		AreaModel area     = areaRepo.findByTitle(AREA1_TITLE);

		//make sure that toProxy is a public proxy
		String proxyVoterToken = castVoteService.getVoterToken(toProxy, area, toProxy.getPasswordHash());
		TokenChecksumModel proxyChecksumModel = proxyService.becomePublicProxy(toProxy, area, proxyVoterToken);

		//WHEN
		String userVoterToken = castVoteService.getVoterToken(fromUser, area, fromUser.getPasswordHash());
		DelegationModel newDelegation = proxyService.assignProxy(area, fromUser, toProxy, userVoterToken);

		//THEN
		assertNotNull("newly created delegation must not be null (Is toProxy a public proxy?)", newDelegation);
		assertEquals(newDelegation.getFromUser(), fromUser);
		assertEquals(newDelegation.getToProxy(), toProxy);

		TokenChecksumModel userCheckumModel = castVoteService.isVoterTokenValid(userVoterToken);
		assertEquals("Users checksum must be delegated to the proxy's checksum.", userCheckumModel.getDelegatedTo(), proxyChecksumModel);

		Map<AreaModel, UserModel> proxyMap = proxyService.getProxyMap(fromUser);
		log.info(proxyMap.toString());
		assertEquals(toProxy+"is proxy of "+fromUser+" in area "+area, toProxy, proxyMap.get(area));
	}

	@Test
	@WithUserDetails(USER1_EMAIL)
	public void testGetNumVotes() {
		log.trace("testGetNumVotes");
		AreaModel area  = areaRepo.findByTitle(AREA0_TITLE);
		UserModel proxy = userRepo.findByEmail(USER1_EMAIL);
		int numVotes = proxyService.getNumVotes(area, proxy);
		assertEquals(USER1_EMAIL+" should have "+TestFixtures.USER1_NUM_VOTES+" votes", TestFixtures.USER1_NUM_VOTES, numVotes);
		log.trace("SUCCESS: Proxy "+USER1_EMAIL+" can cast "+TestFixtures.USER1_NUM_VOTES+" votes.");
	}

	/**
	 * GIVEN a voter that delegated to a proxy
	 *   AND a chain of transitive delegations above that
	 *  WHEN we query for the topmost proxy in that chain
	 *  THEN the top most proxy at the end of the delegation chain is returned.
	 */
	@Test
	@WithUserDetails(USER5_EMAIL)
	public void findTopProxy() {
		log.trace("testGetTopmostProxy");
		//GIVEN
		UserModel voter             = userRepo.findByEmail(USER5_EMAIL);
		UserModel expectedTopProxy  = userRepo.findByEmail(USER1_EMAIL);
		AreaModel area      			  = areaRepo.findByTitle(AREA0_TITLE);

		//WHEN
		UserModel topmostProxy = proxyService.findTopProxy(area, voter);

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
	@WithUserDetails(USER2_EMAIL)
	public void testCircularDelegationErrorCases() throws LiquidoException {
		//GIVEN
		UserModel fromUser = userRepo.findByEmail(USER2_EMAIL);
		UserModel toProxy  = userRepo.findByEmail(USER1_EMAIL);
		AreaModel area     = areaRepo.findByTitle(AREA1_TITLE);
		String proxyVoterToken = castVoteService.getVoterToken(toProxy, area, toProxy.getPasswordHash());
		proxyService.becomePublicProxy(toProxy, area, proxyVoterToken);

		//WHEN
		String userVoterToken = castVoteService.getVoterToken(fromUser, area, fromUser.getPasswordHash());
		proxyService.assignProxy(area, fromUser, toProxy, userVoterToken);

		//THEN
		assertFalse("Normal delegation should be allowed", proxyService.thisWouldBeCircularDelegation(area, fromUser, toProxy));
		assertTrue("Circular delegation should be forbidden", proxyService.thisWouldBeCircularDelegation(area, toProxy, fromUser));
		try {
			proxyService.assignProxy(area, toProxy, fromUser, userVoterToken);
			fail("Trying to assign a proxy which leads to a circular delegation should have thrown a LiquidoException!");
		} catch(LiquidoException e) {
			if (e.getError() != LiquidoException.Errors.CANNOT_ASSIGN_CIRCULAR_PROXY)
				fail("Trying to assign a proxy which leads to a circular delegation should have thrown a LiquidoException with Error == CANNOT_ASSIGN_CIRCULAR_PROXY!");
		}
	}

	@Test
	public void testRemoveProxy() throws LiquidoException {
		//GIVEN
		UserModel fromUser = userRepo.findByEmail(USER2_EMAIL);
		UserModel toProxy  = userRepo.findByEmail(USER1_EMAIL);
		AreaModel area     = areaRepo.findByTitle(AREA1_TITLE);

		//WHEN
		String userVoterToken = castVoteService.getVoterToken(fromUser, area, fromUser.getPasswordHash());
		proxyService.removeProxy(area, fromUser, userVoterToken);

		//THEN
		DelegationModel delegation  = delegationRepo.findByAreaAndFromUser(area, fromUser);
		assertNull("Delegation to proxy should not exist anymore", delegation);

		//Cleanup
		proxyService.assignProxy(area, fromUser, toProxy, userVoterToken);
	}
}