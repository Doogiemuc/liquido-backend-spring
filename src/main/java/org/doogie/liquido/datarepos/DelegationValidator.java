package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.rest.RepositoryRestConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Will validate Delegations for the existence of the references foreign keys
 * This Validator is manually registered in {@link RepositoryRestConfigurer}
 *
 * See
 * http://www.baeldung.com/spring-data-rest-validators
 * https://github.com/danielolszewski/blog/tree/master/spring-custom-validation
 * http://blog.trifork.com/2009/08/04/bean-validation-integrating-jsr-303-with-spring/
 * http://stackoverflow.com/questions/35323974/spring-data-rest-validation-confusion    This looks very close to my problem
 */
@Component
public class DelegationValidator implements Validator {
  Logger log = LoggerFactory.getLogger(this.getClass());  // Simple Logging Facade 4 Java

  @Autowired
  UserRepo userRepo;

  @Autowired
  AreaRepo areaRepo;

  public DelegationValidator() {
    //log.info("==== DelegationValidator constructor");
  }

  @Override
  public boolean supports(Class<?> aClass) {
    return DelegationModel.class.equals(aClass);
  }

  /**
   * A DelegationModel is valid when all its referenced ObjectIds actually exist in the areaRepo and userRepo.
   * @param o expectes a DelegationModel
   * @param errors list of errors with keys for error message
   */
  @Override
  public void validate(Object o, Errors errors) {
    DelegationModel delegation = (DelegationModel)o;
    log.trace("Validating Delegation: "+delegation);
    Long areaId      = delegation.getArea().getId();
    Long fromUserId  = delegation.getFromUser().getId();
    Long toProxyId   = delegation.getToProxy().getId();

    if (!areaRepo.exists(areaId)) errors.rejectValue("area", "objectId.mustExist", new Long[]{areaId}, "Referenced areaId does not exist: "+areaId);
    if (!userRepo.exists(fromUserId)) errors.rejectValue("fromUser", "objectId.mustExist", new Long[]{fromUserId}, "Referenced userId does not exist: "+fromUserId);
    if (!userRepo.exists(toProxyId)) errors.rejectValue("toProxy", "objectId.mustExist", new Long[]{toProxyId}, "Referenced userId does not exist: "+toProxyId);

    if (errors.hasErrors()) { log.trace("Validation of Delegation failed: "+ errors); }
  }
}
