package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.services.MailService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
//@ActiveProfiles("test")   // Need to manually set test profile for tests, so that application-test.properties is loaded
public class MailServiceTest extends BaseTest {

	@Autowired
	MailService mailService;

	@Test
	public void testSendEMail() throws Exception {
		//GIVEN an emailToken
		String emailToken = "testDummyEmailToken";

		//WHEN sending an Email
		mailService.sendEMail("liquido_test@doogie.de", emailToken);

		//THEN no exception is thrown
		log.info("Email sent successfully");
	}
}