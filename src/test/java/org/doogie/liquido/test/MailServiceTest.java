package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.services.MailService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
@Disabled // currently disabled
//@ActiveProfiles("test")   // Needed to manually set test profile for tests, so that application-test.yml is loaded ???
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