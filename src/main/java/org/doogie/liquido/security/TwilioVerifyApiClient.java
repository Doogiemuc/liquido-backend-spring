package org.doogie.liquido.security;

import com.jayway.jsonpath.JsonPath;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Set;

/**
 * Client for the new Twilio/Authy Verify 2.0 API
 * https://www.twilio.com/docs/verify/api
 */
@Slf4j
@Service
public class TwilioVerifyApiClient {

	@Autowired
	LiquidoProperties props;

	private RestTemplate client;

	private static final String Services = "/Services";

	public static final String CHANNEL_SMS  = "sms";
	public static final String CHANNEL_MAIL = "mail";
	public static final String CHANNEL_CALL = "call";

	@PostConstruct
	public void initHttpClients() {
		log.debug("configuring Twilio Verify HTTP client:");
		log.debug("liquido.twilio.verifyUrl  = " + props.twilio.verifyUrl);
		log.debug("liquido.twilio.accountSID = " + props.twilio.accountSID);
		log.debug("liquido.twilio.serviceSID = " + props.twilio.serviceSID);


		if (DoogiesUtil.isEmpty(props.twilio.accountSID) || DoogiesUtil.isEmpty(props.twilio.authToken) || DoogiesUtil.isEmpty(props.twilio.verifyUrl))
			throw new RuntimeException("Need config for Twilio! Must set liquido.twilio.accountSID, .authToken and .verifyUrl in application-dev.yml");

		this.client = new RestTemplateBuilder()
			//.additionalInterceptors(new LogClientRequestInterceptor())
			.basicAuthentication(props.twilio.accountSID, props.twilio.authToken)
			//.uriTemplateHandler(uriBuilderFactory)
			.rootUri(props.twilio.verifyUrl + Services)
			.build();
	}

	/**
	 * Request a verification code via SMS.
	 * https://www.twilio.com/docs/verify/api/verification#start-new-verification
	 *
	 * @return SID of the verification request. URL where the verification can be checked: https://verify.twilio.com/v2/Services/#VA..ServiceSID....#/Verifications/#VE...verificationSid...#
	 */
	public String requestVerificationToken(@NonNull String channel, @NonNull String phoneNumberOrEmail) {
		if (!Set.of(CHANNEL_SMS, CHANNEL_CALL, CHANNEL_MAIL).contains(channel))
			throw new IllegalArgumentException("Channel must be one of sms|email|call !");
		Lson entity = Lson.builder()
			.put("To", phoneNumberOrEmail)
			//.put("CustomMessage", localize("Here is your LIQUIDO login code: "))
			//.put("CustomCode", "998877");
			.put("Channel", "sms");
		ResponseEntity<String> res = this.client.postForEntity("/" + props.twilio.serviceSID + "/Verifications", entity.toUrlEncodedFormHttpEntity(), String.class);
		return JsonPath.read(res.getBody(), "sid");
	}

	/**
	 * Check/Verify if a one time token that was previously sent via SMS is valid.
	 *
	 * https://www.twilio.com/docs/verify/api/verification-check#check-a-verification
	 *
	 * @return true if authToken is valid (the one sent to this mobilephone)
	 */
	public boolean tokenIsValid(@NonNull String mobilephone, @NonNull String authToken) {
		if (authToken.length() != props.liquidoTokenLength)
			throw new IllegalArgumentException("authToken must have length "+props.liquidoTokenLength);
		Lson entity = Lson.builder()
			//.put("VerificationSid", verificationSid)  // Would need to pass the Sid through the client. We're stateless in here!  What is more secure?
			.put("To", mobilephone)
			.put("Code", authToken);
		ResponseEntity<String> res = this.client.postForEntity("/" + props.twilio.serviceSID + "/VerificationCheck", entity.toUrlEncodedFormHttpEntity(), String.class);
		String status = JsonPath.read(res.getBody(), "status");
		return "approved".equals(status);
	}


	// https://github.com/AuthySE/VerifyV2-API-Samples/blob/master/createVerificationSMSCustomCode.sh   => Premium Feature :-)  Would be nice for testing.
	//https://www.twilio.com/docs/verify/api/customization-options#code-start-a-verification-with-custom-code

}
