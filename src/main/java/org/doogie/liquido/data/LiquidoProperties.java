package org.doogie.liquido.data;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Type save properties for Liquido
 * These params are read from <pre>application.properties</pre> file.
 *
 * https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config-typesafe-configuration-properties
 * https://www.mkyong.com/spring-boot/spring-boot-configurationproperties-example/
 */
@Data   // Lombok is just plain simply the coolest Java library ever!
@Slf4j
@Component
@ConfigurationProperties("liquido")
public class LiquidoProperties {

	public Integer supportersForProposal;
	public Integer daysUntilVotingStarts;
	public Integer durationOfVotingPhase;
	public Integer checksumExpirationHours;
	public String  frontendUrl;
	public String  devLoginSmsToken;

	public Backend backend = new Backend();
	@Data
	static class Backend {	String version;	}

	public Bcrypt bcrypt = new Bcrypt();
	@Data
	public static class Bcrypt {
		public String salt;
		public String secret;
		/** Security: Do NOT expose secret and salt in toString() !!! */
		@Override
		public String toString() {
			return "Bcrypt{secret, salt}";
		}
	}

	public Smtp smtp = new Smtp();
	@Data
	public static class Smtp {
		public String host;
		public String port;
		public String from;
		public String fromName;
		public String username;
		public String pass;
		/** Security: No username and password in toString !!!! */
		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer("Smtp{");
			sb.append("host='").append(host).append('\'');
			sb.append(", port='").append(port).append('\'');
			sb.append(", from='").append(from).append('\'');
			sb.append(", fromName='").append(fromName).append('\'');
			sb.append('}');
			return sb.toString();
		}
	}

	public Admin admin = new Admin();
	@Data
	public static class Admin {
		public String email;
		public String name;
		public String mobilephone;
	}



	/*
	 * Load some properties from DB instead of application.properties file.
	 *
	@PostConstruct
	public void initProperties() {
		// ... not yet implemented ...
	}
	*/




}
