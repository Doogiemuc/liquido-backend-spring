package org.doogie.liquido.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

  // The famous baseUrL problem: http://stackoverflow.com/questions/32927937/how-to-set-base-url-for-rest-in-spring-boot/41321286#41321286

  /**
   * Simple is alive test
   * @return <pre>{"Hello":"World"}</pre>
   */
  @RequestMapping("${spring.data.rest.base-path}/_ping")
  public String isAlive() {
    return "{\"Hello\":\"World\"}";
  }

}
