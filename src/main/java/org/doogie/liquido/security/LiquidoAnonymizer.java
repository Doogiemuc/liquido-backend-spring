package org.doogie.liquido.security;

import org.doogie.liquido.datarepos.KeyValueRepo;
import org.doogie.liquido.model.KeyValueModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.util.LiquidoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * Then this spring component can be used to deterministically generate BCRYPT hashes.
 *
 * BCRYPT uses a salt. The inital value for this salt can be set in application.properties
 * Then it will be stored and loaded from the DB via {@link LiquidoProperties}
 */
@Component
public class LiquidoAnonymizer {
  private Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	LiquidoProperties props;

  @Autowired
  KeyValueRepo keyValueRepo;

	/**
	 * Concatenate the passed strings and return the BCRYPT hashvalue.
	 * This will use the BCRYPT_SALT
	 * @param str one or more input strings for the calculation
	 * @return hash(str1+str2+str3+...+strN) using salt
	 */
  public String getBCryptHash(String... str) {
    StringBuffer inputStr = new StringBuffer();
    for(String s: str) {
      inputStr.append(s);
    }
    String salt = props.get(LiquidoProperties.KEY.BCRYPT_SALT);

    return BCrypt.hashpw(inputStr.toString(), salt);
  }

	/**
	 * Check if plaintext hashes to the given hashed password
	 * @param plainText
	 * @param hashed
	 * @return
	 */
  public boolean checkpw(String plainText, String hashed) {
  	return BCrypt.checkpw(plainText, hashed);
	}

}


  /* This is how to manually create a HASH with Java's built-in SHA-256

  try {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(DoogiesUtil.longToBytes(userId));
    md.update(DoogiesUtil.longToBytes(initialPropId));
    md.update(userPassword.getBytes());
    byte[] digest = md.digest();
    String voterTokenSHA256 = DoogiesUtil.bytesToString(digest);
  } catch (NoSuchAlgorithmException e) {
    log.error("FATAL: cannot create SHA-256 MessageDigest: "+e);
    throw new LiquidoRestException("Internal error in backend");
  }
  */