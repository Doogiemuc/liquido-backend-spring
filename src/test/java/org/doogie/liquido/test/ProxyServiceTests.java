package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.AreaModel;
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

import static org.doogie.liquido.test.TestFixtures.*;
import static org.junit.Assert.assertEquals;

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
		UserModel fromUser = userRepo.findByEmail(USER1_EMAIL);
		UserModel toProxy  = userRepo.findByEmail(USER0_EMAIL);
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

}