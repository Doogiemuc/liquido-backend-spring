package org.doogie.liquido.model;

import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Will validate Delegations for the existence of the references foreign keys
 *
 * See https://github.com/danielolszewski/blog/tree/master/spring-custom-validation
 */
@Component
public class BeforeCreateDelegationValidator implements Validator {
  Logger log = LoggerFactory.getLogger(this.getClass());  // Simple Logging Facade 4 Java

  @Autowired
  UserRepo userRepo;   //FIXME: this is null

  @Autowired
  AreaRepo areaRepo;

  @Override
  public boolean supports(Class<?> aClass) {
    return DelegationModel.class.equals(aClass);
  }

  /**
   * A DelegationModel is valid when all its referenced ObjectIds actually exist in the areaRepo and userRepo.
   * @param o expectes a DelegationModel
   * @param errors list of errors with error message
   */
  @Override
  public void validate(Object o, Errors errors) {
    DelegationModel delegation = (DelegationModel)o;
    log.trace("======== Validating Delegation: "+delegation);
    String areaId      = delegation.getArea().toHexString();
    String fromUserId  = delegation.getFromUser().toHexString();
    String toProxyId   = delegation.getToProxy().toHexString();

    if (!areaRepo.exists(areaId)) errors.rejectValue("area","Area with id="+areaId+" does not exist.");
    if (!userRepo.exists(fromUserId)) errors.rejectValue("fromUser", "fromUser with id="+fromUserId+" does not exist.");
    if (!userRepo.exists(toProxyId)) errors.rejectValue("toProxy", "toProxy with id="+toProxyId+" does not exist.");

  }
}
