package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST service for global configuration values.
 * This is a plain rest controller. No Sprint JPA magic here. So that we can return plain JSON.
 */
@Slf4j
@RestController
@RequestMapping("${spring.data.rest.base-path}")
public class PropertiesController {

  @Autowired
  LiquidoProperties prop;

  /**
   * Get the global Liquido Properties that the client needs
   * This is a subset from the values in application.properties
   *
   * This endpoint is public and can be accessed without authentication!
   *
   * @return Properties for client as JSON
   */
  @RequestMapping(value = "/globalProperties", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public @ResponseBody String getGlobalProperties() {
    //SECURITY: Do NOT simply return the whole prop object! It contains sensitive secrets!
    return new Lson()
      .put("supportersForProposal", prop.supportersForProposal)
      .put("daysUntilVotingStarts", prop.daysUntilVotingStarts)
      .put("durationOfVotingPhase", prop.durationOfVotingPhase)
      .put("checksumExpirationHours", prop.rightToVoteExpirationHours)
      .put("backendVersion", prop.backend.version)
      .toString();

  }

}
