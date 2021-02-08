package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.RightToVoteRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.RightToVoteModel;
import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.services.CastVoteService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.ProxyService;
import org.doogie.liquido.test.testUtils.WithMockTeamUser;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.doogie.liquido.testdata.TestFixtures;
import org.doogie.liquido.testdata.TestDataUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static org.doogie.liquido.testdata.TestFixtures.*;
import static org.junit.Assert.*;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ProxyServiceTests extends BaseTest {
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
	RightToVoteRepo rightToVoteRepo;

	@Autowired
	LiquidoProperties props;

	@Autowired
	TestDataUtils utils;

	/**
	 * GIVEN a public proxy P
	 *   AND a voter V
	 *  WHEN voter V assigns P as proxy in area A
	 *  THEN the new delegation points from V to P
	 *   AND the user's checksumModel is delegatedTo P's checksum
	 *   AND the proxyMap of V contains P in area A
	 */
	@Test
	@WithMockTeamUser(email = USER13_EMAIL)
	public void testAssignProxy() throws LiquidoException {
		//GIVEN
		UserModel fromUser = userRepo.findByEmail(USER13_EMAIL).get();   // use other users as in the delegation tests
		UserModel toProxy  = userRepo.findByEmail(USER14_EMAIL).get();
		AreaModel area = getDefaultArea();

		//Make sure that toProxy is a public proxy
		String proxyVoterToken = castVoteService.createVoterTokenAndStoreRightToVote(toProxy, area, USER_TOKEN_SECRET, true);
		RightToVoteModel proxiesRightToVote = proxyService.becomePublicProxy(toProxy, area, proxyVoterToken);

		//WHEN
		String userVoterToken = castVoteService.createVoterTokenAndStoreRightToVote(fromUser, area, USER_TOKEN_SECRET, true);
		DelegationModel delegation = proxyService.assignProxy(area, fromUser, toProxy, userVoterToken);

		//THEN
		RightToVoteModel usersRightToVote = castVoteService.isVoterTokenValid(userVoterToken);
		assertEquals("Voter's rightToVote must be delegated to proxy", proxiesRightToVote, usersRightToVote.getDelegatedTo());
		assertEquals(toProxy.toStringShort()+" should now be the proxy of "+fromUser, toProxy, delegation.getToProxy());

		Optional<DelegationModel> delegationOpt = delegationRepo.findByAreaAndFromUser(area, fromUser);
		assertTrue("DelegationModel from user in that area must exist", delegationOpt.isPresent());
		assertEquals("Delegation must point from voter ", fromUser, delegationOpt.get().getFromUser());
		assertEquals("Delegation must point to proxy", toProxy, delegationOpt.get().getToProxy());

		Optional<DelegationModel> directProxy = proxyService.getDelegationToDirectProxy(area, fromUser);
		assertTrue(fromUser.toStringShort()+" should have a delegation", directProxy.isPresent());
		assertEquals(toProxy+"is direct proxy of "+fromUser+" in area "+area, toProxy, directProxy.get().getToProxy());
	}

	@Test
	@WithMockTeamUser(email = USER1_EMAIL)
	public void testGetNumVotes() throws LiquidoException {
		log.trace("ENTER: testGetNumVotes");
		AreaModel area = getDefaultArea();

		UserModel proxy = userRepo.findByEmail(USER1_EMAIL).get();
		String voterToken = castVoteService.createVoterTokenAndStoreRightToVote(proxy, area, USER_TOKEN_SECRET, true);
		RightToVoteModel proxyChecksum = castVoteService.isVoterTokenValid(voterToken);
		utils.printRightToVoteTree(proxyChecksum);
		long delegationCount = proxyService.getRecursiveDelegationCount(voterToken);
		assertEquals(USER1_EMAIL+" should have "+TestFixtures.USER1_DELEGATIONS +" delegations", TestFixtures.USER1_DELEGATIONS, delegationCount);

		proxy = userRepo.findByEmail(USER4_EMAIL).get();
		voterToken = castVoteService.createVoterTokenAndStoreRightToVote(proxy, area, USER_TOKEN_SECRET, true);
		proxyChecksum = castVoteService.isVoterTokenValid(voterToken);
		utils.printRightToVoteTree(proxyChecksum);
		delegationCount = proxyService.getRecursiveDelegationCount(voterToken);
		assertEquals(USER4_EMAIL+" should have "+TestFixtures.USER4_DELEGATIONS +" delegations", TestFixtures.USER4_DELEGATIONS, delegationCount);

		log.trace("SUCCESS: testGetNumVotes");
	}

	/**
	 * GIVEN a voter that delegated to a proxy
	 *   AND a chain of transitive delegations above that
	 *  WHEN we query for the topmost proxy in that chain
	 *  THEN the top most proxy at the end of the delegation chain is returned.
	 */
	@Test
	//Deprecated: @WithUserDetails(USER5_EMAIL)
	@WithMockTeamUser(email = USER5_EMAIL)
	public void findTopProxy() {
		log.trace("testGetTopmostProxy");
		// all this data must match TestFixtures.java !!!
		AreaModel area = getDefaultArea();
		UserModel voter;
		UserModel expectedTopProxy;
		Optional<UserModel> topProxy;

		//GIVEN   - proxy 5 -> 3 -> 1    see TestFixtures.java
		voter             = userRepo.findByEmail(USER10_EMAIL).get();
		expectedTopProxy  = userRepo.findByEmail(USER1_EMAIL).get();
		//WHEN
		topProxy = proxyService.findTopProxy(area, voter);
		//THEN
		assertTrue(topProxy.isPresent());
		assertEquals(expectedTopProxy, topProxy.get());

		//GIVEN   - voter 5 -> no proxy, because delegation is only requested
		voter             = userRepo.findByEmail(USER5_EMAIL).get();
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
	@WithMockTeamUser(email = USER2_EMAIL)
	public void testCircularDelegationErrorCases() throws LiquidoException {
		//GIVEN
		UserModel fromUser = userRepo.findByEmail(USER8_EMAIL).get();
		UserModel toProxy  = userRepo.findByEmail(USER9_EMAIL).get();
		AreaModel area     = getDefaultArea();
		//String proxyVoterToken = castVoteService.createVoterTokenAndStoreChecksum(toProxy, area, toProxy.getPasswordHash(), true);

		//WHEN
		String userVoterToken = castVoteService.createVoterTokenAndStoreRightToVote(fromUser, area, USER_TOKEN_SECRET, true);
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
	@WithMockTeamUser(email = USER14_EMAIL)
	public void testRemoveProxy() throws LiquidoException {
		//GIVEN
		UserModel fromUser = userRepo.findByEmail(USER14_EMAIL).get();
		UserModel toProxy  = userRepo.findByEmail(USER15_EMAIL).get();
		AreaModel area     = getDefaultArea();

		//WHEN
		String userVoterToken = castVoteService.createVoterTokenAndStoreRightToVote(fromUser, area, USER_TOKEN_SECRET, true);
		proxyService.removeProxy(area, fromUser, userVoterToken);

		//THEN
		Optional<DelegationModel> delegation  = delegationRepo.findByAreaAndFromUser(area, fromUser);
		assertFalse("Delegation to proxy should not exist anymore", delegation.isPresent());

		//Cleanup
		proxyService.assignProxy(area, fromUser, toProxy, userVoterToken);
	}
}