package org.doogie.liquido.test.testUtils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
//@Configuration
public class TestClientConfiguration {

  @Value(value = "${spring.data.rest.base-path}")   // get value from from application.properties file
  String basePath;

  //@LocalServerPort
  //int localServerPort;

  /*
  @Bean
  public RestTemplateBuilder restTemplateBuilder() {
    //see: https://github.com/spring-projects/spring-boot/issues/6465
    String rootUri = "http://localhost:"+localServerPort+basePath;
    log.trace("========== configuring RestTemplate for "+rootUri);
    return new RestTemplateBuilder()
      .basicAuthorization(TestFixtures.USER1_EMAIL, TestFixtures.USER1_PWD)
      .errorHandler(new LiquidoTestErrorHandler())
      //.requestFactory(new HttpComponentsClientHttpRequestFactory())
      .additionalInterceptors(new LogClientRequestInterceptor())
      .rootUri(rootUri);
  }

  */


}
