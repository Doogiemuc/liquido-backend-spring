package org.doogie.liquido.testdata;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.AreaModel;
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
	// [SECURITY] Exclude sensitive fields from toString in this class!!!

	public Integer supportersForProposal;
	public Integer daysUntilVotingStarts;
	public Integer durationOfVotingPhase;
	public Integer rightToVoteExpirationHours;
	public String  defaultAreaTitle;
	public String  frontendUrl;

	public Integer loginLinkExpirationHours;

	@ToString.Exclude
	public String  devLoginToken;

	public Backend backend = new Backend();

	@Data
	public static class Backend {
		public String version;
	}

	@ToString.Exclude
	public Bcrypt bcrypt = new Bcrypt();
	@Data
	public static class Bcrypt {
		@ToString.Exclude
		public String salt;
		@ToString.Exclude
		public String secret;
	}

	public Smtp smtp = new Smtp();
	@Data
	public static class Smtp {
		public String host;
		public String port;
		public String from;
		public String fromName;
		@ToString.Exclude
		public String username;
		@ToString.Exclude
		public String pass;
	}

	@ToString.Exclude
	public Admin admin = new Admin();
	@Data
	public static class Admin {
		public String email;
		public String name;
		public String mobilephone;
		public String picture;
	}

	@ToString.Exclude
	public Authy authy = new Authy();
	@Data
	public static class Authy {
		@ToString.Exclude
		public String apiUrl;
		@ToString.Exclude
		public String apiKey;
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
