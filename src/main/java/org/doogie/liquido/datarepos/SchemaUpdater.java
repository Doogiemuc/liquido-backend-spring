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

  @Override
  public void run(String... strings) throws Exception {
    log.info("Adding UNIQUE constraint for delegations.");
    jdbcTemplate.execute("ALTER TABLE delegations ADD CONSTRAINT DELEGATION_COMPOSITE_ID UNIQUE (area_id, from_user_id)");

  }
}
