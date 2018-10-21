package org.doogie.liquido.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.util.LiquidoProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * REST service for global configuration values that are loaded from the DB
 * (and not from environment)
 */
@Slf4j
@BasePathAwareController  // only works when methods have the @ResponseBody annotation
public class PropertiesController {

  @Autowired
  LiquidoProperties props;

  /**
   * @return all properties as JSON
   */
  @RequestMapping(value = "/globalProperties", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('ROLE_USER')")  //  => doesn't work.  Controll will not be mapped => must be configured in RecourseServerConfig
  public @ResponseBody ObjectNode getGlobalProperties() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode json = mapper.createObjectNode().putObject("properties");
    for (LiquidoProperties.KEY key: LiquidoProperties.KEY.values()) {
      json.put(key.toString(), props.get(key));
    }
    return json;
  }

}
