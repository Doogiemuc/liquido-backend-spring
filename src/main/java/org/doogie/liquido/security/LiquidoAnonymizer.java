package org.doogie.liquido.security;

import org.doogie.liquido.datarepos.KeyValueRepo;
import org.doogie.liquido.model.KeyValueModel;
import org.doogie.liquido.model.PollModel;
import org.doogie.liquido.model.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * If there already is an initial salt value in the DB, then we load it.
 * Otherwise a new BCRYPT salt is generated and stored in the key=value table.
 *
 * Then this spring component can be used to deterministically generate BCRYPT hashes.
 */
@Component
public class LiquidoAnonymizer implements CommandLineRunner {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  public static final String SALT_KEY = "liquido.config.salt";

  private String salt;

  @Autowired
  KeyValueRepo keyValueRepo;

  @Override
  public void run(String... strings) throws Exception {
    //TODO: load salt if there already is one in the DB.

    // else
    log.info("Creating BCRYPT salt ...");
    this.salt = BCrypt.gensalt();
    KeyValueModel kv = new KeyValueModel(SALT_KEY, salt);
    keyValueRepo.save(kv);
  }


  /**
   * create a bcrypt (hash) token for this user and this poll.
   * @param votingUser the user that casts the vote
   * @param poll the poll that the vote is casted for
   * @return a bcrypt hash calculated from the userId, pollId and the user's password.
   */
  public String getBCryptVoterToken(UserModel votingUser, String userPassword, PollModel poll) {
    StringBuffer inputStr = new StringBuffer();
    inputStr.append(votingUser.getId());
    inputStr.append(poll.getId());
    inputStr.append(userPassword);
    String voterToken = BCrypt.hashpw(inputStr.toString(), this.salt);
    //TODO: a bcrypt hash contains its salt. MAYBE split string and store hash part seperately
    return voterToken;
  }

	/**
	 * Concatenate the passed strings and return the hashvalue
	 * @param str one or more strings
	 * @return hash(str1+str2+str3+...+strN)
	 */
  public String getBCrypetHash(String... str) {
    StringBuffer inputStr = new StringBuffer();
    for(String s: str) {
      inputStr.append(s);
    }
    return BCrypt.hashpw(inputStr.toString(), this.salt);
  }

}
