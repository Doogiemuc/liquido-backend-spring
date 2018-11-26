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
 * Global properties that are read from the DB.
 * These properties can be changed at runtime
 * and new values <strong>can</strong> be persisted in the DB.
 */
@Slf4j
@Component
public class LiquidoProperties /*implements CommandLineRunner*/ {
  //Implementation note: A CommandLine runner makes this run AFTER TestDataCreator.
  // But LiquidoProperties must be initialized BEFORE TestDataCreator. This is now possible with the @PostConstruct annotation below.

  /**
   * List of KEYs. All values are mandatory! Each key must have a default value in application.properties!
   * I am using an enum for several reasons
   *  - KEYs as enum values can be autocompleted in an IDE
   *  - You can easily see where a key is used.
   *  - I can iterate over all KEYs.
   */
  public enum KEY {
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

  //TODO: Cache liquido properties  (same as in LiquidoUserDetailsService)
  /*See:
   @Cacheable("authenticedUsers")
   https://spring.io/guides/gs/caching/
   https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-caching.html
   https://docs.spring.io/spring/docs/current/spring-framework-reference/html/cache.html#cache-store-configuration-caffeine

   Do not forget to @CacheEvict the cache elem, when a user or its roles & rights change
  */

  private Map<KEY, String> props;

  @Autowired
  KeyValueRepo keyValueRepo;

  @Autowired
  Environment springEnv;   // load settings from application[-<env>].properties


  /**
   * Load values for all KEYs.
   * Sources are in this order:
   * 1) load from the DB   if not found, then try
   * 2) load from spring properties (application.properties or application-env.properties)
   * 3) Use fixed default value.

  @Override
  public void run(String... args) throws Exception {
    log.info("running LiquidoProperties command line runner");
  }
  */

  /**
   * Initialize properties. Currently all properties are mandatory.
   * This runs, after the bean has been constructed completely, ie. all Autowired attributes are injected and ready.
   * Keep in mind that this runs BEFORE the TestDataCreator!
   */
  @PostConstruct
  public void postConstruct() {
    log.info("Loading properties from DB");
    this.props = new HashMap<>();
    for(KEY key : KEY.values()) {
      KeyValueModel kv = null; //keyValueRepo.findByKey(key.toString());

      //If there is no value in the DB, then we load initial values from property file.
      if (kv == null)  {
        String property = springEnv.getProperty(key.toString());
				if (BCRYPT_SALT.equals(key) && property == null) {
					String salt = BCrypt.gensalt();
					throw new RuntimeException("Need BCRYPT_SALT in application.properties. You may for example use\nliquido.bcrypt.salt="+salt);
				}
				if (property == null) throw new RuntimeException("Need default value for "+key+" in application.properties");
        kv = new KeyValueModel(key.toString(), property);
      }


      props.put(key, kv.getValue());
      log.debug("   "+key+" = "+kv.getValue());
    }
  }


  public String get(KEY key) {
    return props.get(key);
  }
  public Integer getInt(KEY key) {
    return Integer.valueOf(props.get(key));
  }

  /**
   * Set a value.  If you also want to persist that value in the DB, then use
   * {@link #setAndStore(KEY, String)}
   * @param key {@link KEY}
   * @param value the new string value
   */
  public void set(KEY key, String value) {
    props.put(key, value);
  }

  /**
   * set the value for this key and also persist the new value to the DB.
   */
  public KeyValueModel setAndStore(KEY key, String value) {
    this.set(key, value);
    KeyValueModel kv = new KeyValueModel(key.toString(), value);
    return keyValueRepo.save(kv);
  }
}
