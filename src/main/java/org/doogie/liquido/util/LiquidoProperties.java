package org.doogie.liquido.util;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.KeyValueRepo;
import org.doogie.liquido.model.KeyValueModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Global properties that are read from the DB.
 * These properties can be changed at runtime
 * and the new value <strong>can</strong> be persisted in the DB.
 */
@Slf4j
@Component
public class LiquidoProperties implements CommandLineRunner {

  /**
   * List of all KEYs
   * I am using an enum for several reasons
   *  - KEYs as enum values can be autocompleted in an IDE
   *  - I can iterate over all KEYs.
   *  - And each Java enum value of a KEY can have its own keyName (a String) which is used in the DB.
   */
  public enum KEY {
    LIKES_FOR_QUORUM("liquido.likes.for.quorum"),
    SUPPORTERS_FOR_PROPOSAL("liquido.supporters.for.proposal");
    //TODO: BCRYPT_SALT("liquido.bcrypt.salt");

    String keyName;
    KEY(String name) {
      this.keyName = name;
    }
    @Override
    public String toString() {
      return this.keyName;
    }
  }

  private static Map<KEY, String> props;

  @Autowired
  KeyValueRepo keyValueRepo;

  /** Load initial values for all KEYs from the DB */
  @Override
  public void run(String... strings) throws Exception {
    log.debug("Loading properties from DB");
    this.props = new HashMap<>();
    for(KEY key : KEY.values()) {
      KeyValueModel kv = keyValueRepo.findByKey(key.toString());
      if (kv == null) throw new Exception("Cannot find property value in DB for key: "+key.toString());
      props.put(key, kv.getValue());
      log.debug("   "+key+" = "+kv.getValue());
    }
  }

  public static String get(KEY key) {
    return props.get(key.toString());
  }

  public static void set(KEY key, String value) {
    props.put(key, value);
  }

  /**
   * set the value for this key and also persist the new value to the DB.
   */
  public void setAndStore(KEY key, String value) {
    this.set(key, value);
    KeyValueModel kv = new KeyValueModel(key.toString(), value);
    keyValueRepo.save(kv);
  }
}
