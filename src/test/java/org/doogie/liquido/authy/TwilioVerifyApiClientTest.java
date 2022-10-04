package org.doogie.liquido.authy;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.security.TwilioVerifyApiClient;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.test.BaseTest;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Happy Flow test for new Twilio Verify 2.0 API client
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@Slf4j
@Ignore    // This test is ignored to NOT spam my Twillio API account.  But test is working fine.
public class TwilioVerifyApiClientTest extends BaseTest {

	@Autowired
	TwilioVerifyApiClient verifyApiClient;

	@Autowired
	LiquidoProperties props;

	/**
	 * Register a new user, send an authentication request to him.
	 * (Sadly we cannot verify the TOTP in an automated test case. The OTP is on the users hardware device. => Which actually is the whole reason behind 2FA :-)
	 * Then delete the newly created user.
	 */
	@Test
	public void testTwilioAuthentication() throws LiquidoException {
		String email = null;
		String mobilephone = props.admin.mobilephone;
		long userAuthyId;

		//----- create new user
		/*
		try {
			long rand5digits = System.currentTimeMillis() & 10000;
			email = "userFromTest" + rand5digits + "@liquido.vote";
			mobilephone = "+49111" + rand5digits;
			String countryCode = "49";
			userAuthyId = client.createTwilioUser(email, mobilephone, countryCode);
			log.info("Created new twilio user[mobilephone=" + mobilephone + ", authyId=" + userAuthyId + "]");
		} catch (RestClientException e) {
			log.error("Cannot create twilio user "+email, e.toString());
			throw e;
		}
		*/


		//----- send authentication request (via push or SMS)
		log.debug("Send SMS or push notification to mobilephone="+mobilephone);
		String res = verifyApiClient.requestVerificationToken("sms", mobilephone);
		log.debug(" => SMS sent successfully. "+res);

		/*
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
		*/

	}


	/*
	 * This test cannot be run automatically. But still we provide it. You must manually enter the otp.
	 * @throws LiquidoException

	//@Test
	public void verifyOneTimePassword() throws LiquidoException {
		long userAuthyId = 118906633;
		String otp = "2079048";
		try {
			String res = verifyApiClient.verifyOneTimePassword(userAuthyId, otp);
			log.info(" => "+res);
			log.info("Authy OneTimePassword verified SUCCESSFULLY. User "+userAuthyId+" is authentic.");
		} catch (LiquidoException e) {
			log.error("Cannot verify OTP", e);
			throw e;
		}
	}
	*/

}