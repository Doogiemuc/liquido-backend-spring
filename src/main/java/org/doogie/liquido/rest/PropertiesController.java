package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.data.LiquidoProperties;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * REST service for global configuration values
 */
@Slf4j
@BasePathAwareController                  // only works when methods have the @ResponseBody annotation
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
    return new Lson()
      .put("supportersForProposal", prop.supportersForProposal)
      .put("daysUntilVotingStarts", prop.daysUntilVotingStarts)
      .put("durationOfVotingPhase", prop.durationOfVotingPhase)
      .put("checksumExpirationHours", prop.checksumExpirationHours)
      .toString();

  }

}
