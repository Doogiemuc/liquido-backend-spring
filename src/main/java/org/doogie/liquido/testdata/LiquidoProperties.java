package org.doogie.liquido.testdata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Data;
import lombok.ToString;
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
	// [SECURITY] Exclude sensitive fields from toString in this class!!!

	public Integer supportersForProposal;
	public Integer daysUntilVotingStarts;
	public Integer durationOfVotingPhase;
	public Integer rightToVoteExpirationHours;
	public String  defaultAreaTitle;
	public String  frontendUrl;
	public Integer loginLinkExpirationHours;
	public Integer liquidoTokenLength;

	public Backend backend = new Backend();

	@Data
	public static class Backend {
		public String version;
	}

	@ToString.Exclude
	@JsonIgnore
	public Bcrypt bcrypt = new Bcrypt();
	@Data
	public static class Bcrypt {
		@ToString.Exclude
		@JsonIgnore
		public String salt;
		@ToString.Exclude
		@JsonIgnore
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
		@JsonIgnore
		public String username;
		@ToString.Exclude
		@JsonIgnore
		public String pass;
	}

	public Test test = new Test();
	@Data
	public static class Test {
		public Boolean recreateTestData = false;
		public Boolean loadTestData = false;
		public String sampleDbFile = "sampleDB-H2.sql";
		@ToString.Exclude
		@JsonIgnore
		public String devLoginToken;
	}

	@ToString.Exclude
	public Admin admin = new Admin();
	@Data
	public static class Admin {
		public String email;
		public String name;
		public String mobilephone;
		public String website;
		public String picture;
	}

	public Authy authy = new Authy();
	@Data
	public static class Authy {
		public String apiUrl;
		@ToString.Exclude
		@JsonIgnore
		public String apiKey;
	}

	public Twilio twilio = new Twilio();
	@Data
	public static class Twilio {
		public String verifyUrl;
		public String accountSID;
		public String serviceSID;
		@ToString.Exclude
		@JsonIgnore
		public String authToken;
	}

	public static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

	public String toYaml() throws JsonProcessingException {
		return mapper.writeValueAsString(this);
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
