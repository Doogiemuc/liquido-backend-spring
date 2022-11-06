package org.doogie.liquido.services;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Backend service for sending EMails via SMTP
 */
@Slf4j
@Service
public class MailService {

	//MAYBE: this could also be done via spring https://www.baeldung.com/spring-email   SimpleMailMessage.java

	@Autowired
	LiquidoProperties prop;

	String SUBJECT = "[LIQUIDO] One time login token";

	public void sendEMail(String name, String recipientEMail, String emailToken) throws Exception {

		String BODY = String.join(
			System.getProperty("line.separator"),
			"<html><h1>Liquido Login Token</h1>",
			"<h3>Hello "+name+"</h3>",
			"<p>With this link you can login to Liquido.</p>",
			"<p>&nbsp;</p>",
			"<b><a style='font-size: 20pt;' href='"+prop.frontendUrl+"/login?email="+recipientEMail+"&token="+emailToken+"'>Login " + name +  "</a></b>",
			"<p>&nbsp;</p>",
			"<p>This login link can only be used once!</p>",
			"<p style='color:grey; font-size:10pt;'>You received this email, because you (or someone) requested a login token for the <a href='https://www.liquido.net'>LIQUIDO</a> eVoting webapp. If you did not request a login yourself, than you may simply ignore this message.</p>",
			"</html>"
		);

		// Create a Properties object to contain connection configuration information.
		Properties props = new Properties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.auth", true);
		props.put("mail.smtp.starttls.enable", prop.smtp.startttls);
		props.put("mail.smtp.host", prop.smtp.host);
		props.put("mail.smtp.port", prop.smtp.port);
		props.put("mail.smtp.ssl.trust", prop.smtp.host);
		props.put("mail.smtp.ssl.protocols", "TLSv1.2");  //BUGFIX: Java and its version chaos :-(   https://stackoverflow.com/questions/67899129/postfix-and-openjdk-11-no-appropriate-protocol-protocol-is-disabled-or-cipher

		// Create a Session object to represent a mail session with the specified properties.
		Session session = Session.getInstance(props, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(prop.smtp.username, prop.smtp.pass);
			}
		});

		// Create a message with the specified information.
		MimeMessage msg = new MimeMessage(session);
		msg.setFrom(new InternetAddress(prop.smtp.from, prop.smtp.fromName));
		msg.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEMail));
		msg.setSubject(SUBJECT);
		msg.setContent(BODY,"text/html");

		// Add a configuration set header. Comment or delete the
		// next line if you are not using a configuration set
		//msg.setHeader("X-SES-CONFIGURATION-SET", CONFIGSET);

		Transport transport = session.getTransport();
		try	{
			log.debug("Sending email via SMTP to "+name + "<" + recipientEMail + ">");
			Transport.send(msg);
		}	catch (Exception ex) {
			log.error("Cannot send email: "+ex.toString());
			throw new Exception("Cannot send email! "+ex.toString(), ex);
		}	finally	{
			transport.close();
		}

	}

	/**
	 * Send an SMS message to the given mobile phone.
	 * @param mobilephone a valid mobile phone number
	 * @param smsText the text of the message. Max 140 ASCII chars!

	public void sendSms(String mobilephone, String smsText) {
			if (smsText.length() > 140)
				throw new RuntimeException("Cannot send SMS. Text is too long. Longer than 140 ASCII chars.");

			AmazonSNSClient snsClient = new AmazonSNSClient();
			Map<String, MessageAttributeValue> smsAttributes = new HashMap<String, MessageAttributeValue>();
			smsAttributes.put("AWS.SNS.SMS.SenderID", new MessageAttributeValue()
				.withStringValue("LIQUDIO") //The sender ID shown on the device.
				.withDataType("String"));
			smsAttributes.put("AWS.SNS.SMS.MaxPrice", new MessageAttributeValue()
				.withStringValue("0.50") //Sets the max price to 0.50 USD.
				.withDataType("Number"));
			smsAttributes.put("AWS.SNS.SMS.SMSType", new MessageAttributeValue()
				.withStringValue("Promotional") //Sets the type to promotional.
				.withDataType("String"));

			PublishResult result = snsClient.publish(new PublishRequest()
				.withMessage(message)
				.withPhoneNumber(mobilephone)
				.withMessageAttributes(smsAttributes));

			System.out.println(result); // Prints the message ID.

	}
	*/

}