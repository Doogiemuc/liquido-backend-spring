package org.doogie.liquido.test;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.services.MailService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class MailServiceTest extends BaseTest {

	@Autowired
	MailService mailService;

	/**
	 * This can for example send a mail to https://mailtrap.io/ when configured in your application-local.yml
	 * @throws Exception when sending of mail fails eg. due to a SMTP misconfiguration or error
	 */
	@Test
	public void testSendEMail() throws Exception {
		//GIVEN an emailToken
		String emailToken = "testDummyEmailToken";

		//WHEN sending an Email
		mailService.sendEMail("DummyUserName", "liquido_test@nowhere.sdf", emailToken);

		//THEN no exception is thrown
		log.info("Email sent successfully");
	}
}