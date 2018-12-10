package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.ChecksumRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.ChecksumModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.ProxyService;
import org.doogie.liquido.testdata.TestFixtures;
import org.doogie.liquido.testdata.TestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import java.util.Optional;

import static org.doogie.liquido.testdata.TestFixtures.*;
import static org.junit.Assert.*;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")  // this will also load the settings  from  application-test.properties
public class ProxyServiceTests {
	@Autowired
	UserRepo userRepo;

	@Autowired
	AreaRepo areaRepo;

	@Autowired
	CastVoteService castVoteService;

	@Autowired
	ProxyService proxyService;

	@Autowired
	DelegationRepo delegationRepo;

	@Autowired
	ChecksumRepo checksumRepo;

	@Autowired
	TestUtils utils;

	@PostConstruct
	public void beforeClass() throws LiquidoException {
		log.info("=========== ProxyServiceTest.postConstruct (once beforeClass) ========");
		UserModel topProxy = userRepo.findByEmail(USER1_EMAIL);
		AreaModel area     = areaRepo.findByTitle(AREA_FOR_DELEGATIONS);
		ChecksumModel proxyChecksum = castVoteService.getExistingChecksum(topProxy, USER_TOKEN_SECRET, area);
		log.info("=========== PROXY TREE (checksums) ========");
		utils.printChecksumTree(proxyChecksum);
		log.info("=========== PROXY TREE (delegations) ========");
		utils.printDelegationTree(area, topProxy);
		log.info("============================================");
	}

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
	public void testAssignProxy() throws LiquidoException {
		//GIVEN
		UserModel fromUser = userRepo.findByEmail(USER2_EMAIL);
		UserModel toProxy  = userRepo.findByEmail(USER1_EMAIL);
		AreaModel area     = areaRepo.findByTitle(AREA1_TITLE);

		//Make sure that toProxy is a public proxy
		String proxyVoterToken = castVoteService.createVoterTokenAndStoreChecksum(toProxy, area, USER_TOKEN_SECRET, true);
		ChecksumModel publicProxyChecksum = proxyService.becomePublicProxy(toProxy, area, proxyVoterToken);

		//WHEN
		String userVoterToken = castVoteService.createVoterTokenAndStoreChecksum(fromUser, area, USER_TOKEN_SECRET, true);
		ChecksumModel votersChecksum = proxyService.assignProxy(area, fromUser, toProxy, userVoterToken, true);

		//THEN
		assertNotNull("Voter's checksum must not be null", votersChecksum);
		assertEquals("Voter's checksum must be delegated to proxies checksum", publicProxyChecksum, votersChecksum.getDelegatedTo());

		Optional<DelegationModel> delegation = delegationRepo.findByAreaAndFromUser(area, fromUser);
		assertTrue("DelegationModel from user in that area must exist", delegation.isPresent());
		assertEquals("Delegation must point from voter ", fromUser, delegation.get().getFromUser());
		assertEquals("Delegation must point to proxy", toProxy, delegation.get().getToProxy());

		Optional<UserModel> directProxy = proxyService.getDirectProxy(area, fromUser);
		assertEquals(toProxy+"is direct proxy of "+fromUser+" in area "+area, toProxy, directProxy.get());
	}

	@Test
	@WithUserDetails(USER1_EMAIL)
	public void testGetNumVotes() throws LiquidoException {
		log.trace("ENTER: testGetNumVotes");
		AreaModel area  = areaRepo.findByTitle(AREA0_TITLE);

		UserModel proxy = userRepo.findByEmail(USER1_EMAIL);
		String voterToken = castVoteService.createVoterTokenAndStoreChecksum(proxy, area, USER_TOKEN_SECRET, true);
		ChecksumModel proxyChecksum = castVoteService.isVoterTokenValidAndGetChecksum(voterToken);
		utils.printChecksumTree(proxyChecksum);
		long numVotes = proxyService.getNumVotes(voterToken);
		assertEquals(USER1_EMAIL+" should have "+TestFixtures.USER1_NUM_VOTES+" votes", TestFixtures.USER1_NUM_VOTES, numVotes);

		proxy = userRepo.findByEmail(USER4_EMAIL);
		voterToken = castVoteService.createVoterTokenAndStoreChecksum(proxy, area, USER_TOKEN_SECRET, true);
		proxyChecksum = castVoteService.isVoterTokenValidAndGetChecksum(voterToken);
		utils.printChecksumTree(proxyChecksum);
		numVotes = proxyService.getNumVotes(voterToken);
		assertEquals(USER4_EMAIL+" should have "+TestFixtures.USER4_NUM_VOTES+" votes", TestFixtures.USER4_NUM_VOTES, numVotes);

		log.trace("SUCCESS: testGetNumVotes");
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
		AreaModel area      			  = areaRepo.findByTitle(AREA0_TITLE);
		UserModel voter;
		UserModel expectedTopProxy;
		Optional<UserModel> topProxy;

		//GIVEN   - proxy 5 -> 3 -> 1    see TestFixtures.java
		voter             = userRepo.findByEmail(USER5_EMAIL);
		expectedTopProxy  = userRepo.findByEmail(USER1_EMAIL);
		//WHEN
		topProxy = proxyService.findTopProxy(area, voter);
		//THEN
		assertTrue(topProxy.isPresent());
		assertEquals(expectedTopProxy, topProxy.get());

		//GIVEN   - proxy 6 -> 4 because delegation is non transitive
		voter             = userRepo.findByEmail(USER6_EMAIL);
		expectedTopProxy  = userRepo.findByEmail(USER4_EMAIL);
		//WHEN
		topProxy = proxyService.findTopProxy(area, voter);
		//THEN
		assertTrue(topProxy.isPresent());
		assertEquals(expectedTopProxy, topProxy.get());

		//GIVEN   - voter 7 -> no proxy, because delegation is only requested
		voter             = userRepo.findByEmail(USER7_EMAIL);
		//WHEN
		topProxy = proxyService.findTopProxy(area, voter);
		//THEN
		assertFalse(topProxy.isPresent());

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
		//String proxyVoterToken = castVoteService.createVoterTokenAndStoreChecksum(toProxy, area, toProxy.getPasswordHash(), true);

		//WHEN
		String userVoterToken = castVoteService.createVoterTokenAndStoreChecksum(fromUser, area, USER_TOKEN_SECRET, true);
		proxyService.assignProxy(area, fromUser, toProxy, userVoterToken, true);

		//THEN
		assertFalse("Normal delegation should be allowed", proxyService.thisWouldBeCircularDelegation(area, fromUser, toProxy));
		assertTrue("Circular delegation should be forbidden", proxyService.thisWouldBeCircularDelegation(area, toProxy, fromUser));
		try {
			proxyService.assignProxy(area, toProxy, fromUser, userVoterToken, true);
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
		String userVoterToken = castVoteService.createVoterTokenAndStoreChecksum(fromUser, area, USER_TOKEN_SECRET, true);
		proxyService.removeProxy(area, fromUser, userVoterToken);

		//THEN
		Optional<DelegationModel> delegation  = delegationRepo.findByAreaAndFromUser(area, fromUser);
		assertFalse("Delegation to proxy should not exist anymore", delegation.isPresent());

		//Cleanup
		proxyService.assignProxy(area, fromUser, toProxy, userVoterToken, true);
	}
}