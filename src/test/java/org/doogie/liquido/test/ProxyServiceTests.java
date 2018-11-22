package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.DelegationRepo;
import org.doogie.liquido.datarepos.TokenChecksumRepo;
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
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

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
	TokenChecksumRepo checksumRepo;


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
		String proxyVoterToken = castVoteService.createVoterTokenAndStoreChecksum(toProxy, area, toProxy.getPasswordHash(), true);
		TokenChecksumModel publicProxyChecksum = proxyService.becomePublicProxy(toProxy, area, proxyVoterToken);

		//WHEN
		String userVoterToken = castVoteService.createVoterTokenAndStoreChecksum(fromUser, area, fromUser.getPasswordHash(), true);
		TokenChecksumModel assignedChecksumModel = proxyService.assignProxy(area, fromUser, toProxy, userVoterToken, true);

		//THEN
		assertNotNull("proxies checksum must not be null", assignedChecksumModel);
		assertEquals("Assigned checksum must be the same as public proxies checksum", publicProxyChecksum, assignedChecksumModel);

		TokenChecksumModel userCheckumModel = castVoteService.isVoterTokenValidAndGetChecksum(userVoterToken);
		assertEquals("Users checksum must be delegated to the proxy's checksum.", userCheckumModel.getDelegatedTo(), publicProxyChecksum);

		Map<AreaModel, UserModel> proxyMap = proxyService.getDirectProxies(fromUser);
		log.info(proxyMap.toString());
		assertEquals(toProxy+"is proxy of "+fromUser+" in area "+area, toProxy, proxyMap.get(area));
	}

	@Test
	@WithUserDetails(USER1_EMAIL)
	public void testGetNumVotes() throws LiquidoException {
		log.trace("testGetNumVotes");
		AreaModel area  = areaRepo.findByTitle(AREA0_TITLE);

		UserModel proxy = userRepo.findByEmail(USER1_EMAIL);
		String voterToken = castVoteService.createVoterTokenAndStoreChecksum(proxy, area, proxy.getPasswordHash(), true);
		TokenChecksumModel proxyChecksumModel = castVoteService.isVoterTokenValidAndGetChecksum(voterToken);
		printProxyTree(proxyChecksumModel, "");
		long numVotes = proxyService.getNumVotes(voterToken);
		assertEquals(USER1_EMAIL+" should have "+TestFixtures.USER1_NUM_VOTES+" votes", TestFixtures.USER1_NUM_VOTES, numVotes);

		proxy = userRepo.findByEmail(USER4_EMAIL);
		voterToken = castVoteService.createVoterTokenAndStoreChecksum(proxy, area, proxy.getPasswordHash(), true);
		proxyChecksumModel = castVoteService.isVoterTokenValidAndGetChecksum(voterToken);
		printProxyTree(proxyChecksumModel, "");
		numVotes = proxyService.getNumVotes(voterToken);
		assertEquals(USER4_EMAIL+" should have "+TestFixtures.USER4_NUM_VOTES+" votes", TestFixtures.USER4_NUM_VOTES, numVotes);

		log.trace("SUCCESS: testGetNumVotes");
	}

	private void printProxyTree(TokenChecksumModel proxyChecksum, String indent) {
		String publicProxyStr = proxyChecksum.getPublicProxy() != null ? "==public proxy: " + proxyChecksum.getPublicProxy().getEmail() : "";
		List<TokenChecksumModel> delegees = checksumRepo.findByDelegatedTo(proxyChecksum);
		if (delegees == null) {
			log.trace(indent+"Voter: " + proxyChecksum + publicProxyStr);
		} else {
			//log.trace(indent+"Proxy: " + proxyChecksum + publicProxyStr);
			for (TokenChecksumModel delegee : delegees) {
				log.trace(indent + delegee.getPublicProxy().getEmail() + " -" + (delegee.isTransitive() ? "transitive" : "") + "-> " + proxyChecksum.getPublicProxy().getEmail()+"    " + delegee.getChecksum() + " -> " + proxyChecksum);
				printProxyTree(delegee, indent + "  ");
			}
		}
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
		//String proxyVoterToken = castVoteService.createVoterTokenAndStoreChecksum(toProxy, area, toProxy.getPasswordHash(), true);

		//WHEN
		String userVoterToken = castVoteService.createVoterTokenAndStoreChecksum(fromUser, area, fromUser.getPasswordHash(), true);
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
		String userVoterToken = castVoteService.createVoterTokenAndStoreChecksum(fromUser, area, fromUser.getPasswordHash(), true);
		proxyService.removeProxy(area, fromUser, userVoterToken);

		//THEN
		DelegationModel delegation  = delegationRepo.findByAreaAndFromUser(area, fromUser);
		assertNull("Delegation to proxy should not exist anymore", delegation);

		//Cleanup
		proxyService.assignProxy(area, fromUser, toProxy, userVoterToken, true);
	}
}