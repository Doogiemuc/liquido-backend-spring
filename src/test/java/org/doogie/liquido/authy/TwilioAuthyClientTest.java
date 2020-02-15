package org.doogie.liquido.authy;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.security.TwilioAuthyClient;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.test.BaseTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestClientException;

/**
 * Happy Flow test for authy API client
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
public class TwilioAuthyClientTest extends BaseTest {

	@Autowired
	TwilioAuthyClient client;

	@Test
	public void testGetAppDetails() {
		log.info(client.getAppDetails());
	}

	/**
	 * Register a new user, send an authentication request to him.
	 * (Sadly we cannot verify the TOTP in an automated test case. The OTP is on the users hardware device. => Which actually is the whole reason behind 2FA :-)
	 * Then delete the newly created user.
	 */
	@Test
	public void testTwilioAuthentication() throws LiquidoException {
		String email = null;
		String mobilephone;
		long userAuthyId;

		//----- create new user
		try {
			long rand5digits = System.currentTimeMillis() & 10000;
			email = "userFromTest" + rand5digits + "@liquido.vote";
			//mobilephone = "+49111" + rand5digits;
			mobilephone = "+4915162963154";
			String countryCode = "49";
			userAuthyId = client.createTwilioUser(email, mobilephone, countryCode);
			log.info("Created new twilio user[mobilephone=" + mobilephone + ", authyId=" + userAuthyId + "]");
		} catch (RestClientException e) {
			log.error("Cannot create twilio user "+email, e.toString());
			throw e;
		}

		//----- send authentication request (via push or SMS)
		try {
			log.debug("Send SMS or push notification to mobilephone="+mobilephone);
			String res = client.sendSmsOrPushNotification(userAuthyId);
			log.debug(" => "+res);
			if (res.contains("ignored")) {
				log.info("Sent push authentication request to userAuthyId=" + userAuthyId + "   Response:\n" + res);
			} else {
				log.info("Sent Sms to userAuthyId=" + userAuthyId + "   Response:\n" + res);
			}
		} catch (LiquidoException e) {
			log.error("Cannot send SMS to twilio userAuthIy="+userAuthyId, e);
			throw e;
		}

		//----- validate user's token (this cannot be automated.)
		//String otp = <otp that user entered from his mobile phone> ;
		//client.verifyOneTimePassword(userAuthyId, otp);

		//----- remove user
		try {
			String res = client.removeUser(userAuthyId);
			log.info("Removed user userAuthyId=" + userAuthyId + "   Respone:\n" + res);
		} catch (RestClientException e) {
			log.error("Cannot remove userAuthIy="+userAuthyId, e);
			throw e;
		}
	}


	/**
	 * This test cannot be run automatically. But still we provide it. You must manually enter the otp.
	 * @throws LiquidoException
	 */
	//@Test
	public void verifyOneTimePassword() throws LiquidoException {
		long userAuthyId = 118906633;
		String otp = "2079048";
		try {
			String res = client.verifyOneTimePassword(userAuthyId, otp);
			log.info(" => "+res);
			log.info("Authy OneTimePassword verified SUCCESSFULLY. User "+userAuthyId+" is authentic.");
		} catch (LiquidoException e) {
			log.error("Cannot verify OTP", e);
			throw e;
		}
	}

}