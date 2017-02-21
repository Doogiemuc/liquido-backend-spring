package org.doogie.liquido.datarepos;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SchemaUpdater implements CommandLineRunner {

  @Autowired
  JdbcTemplate jdbcTemplate;

  /**
   * A user can only delegate to exactly one proxy in one give area.
   * Therefore {@link org.doogie.liquido.model.DelegationModel} will get a combined unique constraint on area and fromUser.
   * This CommandLineRunner runs right after the app has started and creates that unique constraint.
   * @param strings not used
   * @throws Exception when the JDBC operation fails
   */
  @Override
  public void run(String... strings) throws Exception {
    log.info("Adding UNIQUE constraint for delegations.");
    jdbcTemplate.execute("ALTER TABLE delegations ADD CONSTRAINT DELEGATION_COMPOSITE_ID UNIQUE (area_id, from_user_id)");

  }
}
