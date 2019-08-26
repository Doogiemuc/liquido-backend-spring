package org.doogie.liquido.util;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.KeyValueRepo;
import org.doogie.liquido.model.KeyValueModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

import static org.doogie.liquido.util.LiquidoProperties.KEY.BCRYPT_SALT;

/**
 * Global properties that are persisted in the DB.
 * These properties can be changed at runtime
 * and new values <strong>can</strong> be persisted in the DB.
 */
@Slf4j
@Component
//TODO: replace LiquidoProperties this with Type safe configuration properties https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html#boot-features-external-config-typesafe-configuration-properties
public class LiquidoProperties extends HashMap<LiquidoProperties.KEY, String> /*implements CommandLineRunner*/ {
  //Implementation note: A CommandLine runner makes this run AFTER TestDataCreator.
  // But LiquidoProperties must be initialized BEFORE TestDataCreator. This is now possible with the @PostConstruct annotation below.

  @Autowired
  KeyValueRepo keyValueRepo;

  @Autowired
  Environment springEnv;   // load settings from application[-<env>].properties

  /**
   * List of KEYs. All values are mandatory! Each key must have a default value in application.properties!
   * I am using an enum for several reasons
   *  - KEYs as enum values can be autocompleted in an IDE
   *  - You can easily see where a key is used.
   *  - I can iterate over all KEYs.
   */
  public enum KEY {
    LIQUIDO_BACKEND_VERSION("liquido.backend.version"),
    SUPPORTERS_FOR_PROPOSAL("liquido.supporters.for.proposal"),
    DAYS_UNTIL_VOTING_STARTS("liquido.days.until.voting.starts"),
    DURATION_OF_VOTING_PHASE("liquido.duration.of.voting.phase"),
    BCRYPT_SALT("liquido.bcrypt.salt");

    String keyName;
    KEY(String name) {
      this.keyName = name;
    }
    @Override
    public String toString() {
      return this.keyName;
    }
  }


  /**
   * Initialize properties. All properties are mandatory. If there is no value in the DB
   * we try to get an initial value from application.properties
   * This runs, after the bean has been constructed completely, ie. all Autowired attributes are injected and ready.
   * Keep in mind that this runs BEFORE the TestDataCreator!

  @PostConstruct
  public void postConstruct() {
    log.info("Loading properties from DB");
    for(KEY key : KEY.values()) {
      KeyValueModel kv = keyValueRepo.findByKey(key.toString());
      if (kv == null)  {
        String property = springEnv.getProperty(key.toString());
				if (BCRYPT_SALT.equals(key) && property == null) {
					String salt = BCrypt.gensalt();
					throw new RuntimeException("Need BCRYPT_SALT in application.properties. You may for example use\nliquido.bcrypt.salt="+salt);
				}
				if (property == null) throw new RuntimeException("Need initial value for "+key+" in application.properties");
        kv = new KeyValueModel(key.toString(), property);
        this.put(key, kv.getValue());  // put and store
      } else {
        super.put(key, kv.getValue()); // only put, already stored
      }
      log.debug("   "+key+" = "+kv.getValue());
    }
  }
  */

  /**
   * 1. return the locally cached value
   * 2. return the value from the DB and cache it
   * 3. try to load the initial value from application.properties
   * 4. throw a RuntimeException
   * @param key liquido key
   * @return property value
   */
  @Override
  public String get(Object key) {
    if (super.containsKey(key)) return super.get(key);
    KeyValueModel kv = keyValueRepo.findByKey(key.toString());
    if (kv != null) return kv.getValue();
    String springProp = springEnv.getProperty(key.toString());
    if (springProp != null) {
      super.put((KEY)key, springProp);
      keyValueRepo.save(new KeyValueModel(key.toString(), springProp));
      return springProp;
    }
    throw new RuntimeException("No property value for liqudio key="+key);
  }

  public Integer getInt(KEY key) {
    return Integer.valueOf(this.get(key));
  }

  /**
   * set the value for this key and also persist the new value to the DB.
   */
  public String put(KEY key, String value) {
    KeyValueModel kv = new KeyValueModel(key.toString(), value);
    keyValueRepo.save(kv);
    return super.put(key, value);
  }
}
