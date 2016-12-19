package org.doogie.liquido.datarepos;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableMongoAuditing  // This annotation will magically make @LastModifiedDate and @CreatedDate work
public class MongoDBConfiguration {
  //EMPTY class
}
