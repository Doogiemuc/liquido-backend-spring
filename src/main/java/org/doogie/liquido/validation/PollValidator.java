package org.doogie.liquido.validation;

import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.PollModel;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * When a new poll is created, check that its proposal is in status PROPOSAL.

@Component("beforeCreatePollModelValidator")
public class PollValidator implements Validator {
  @Override
  public boolean supports(Class<?> aClass) {
    return PollModel.class.equals(aClass);
  }

  @Override
  public void validate(Object o, Errors errors) {
    PollModel poll = (PollModel)o;
    for (LawModel proposal : poll.getProposals()) {
      if (proposal.getStatus() != LawModel.LawStatus.PROPOSAL) {
        errors.rejectValue(proposal.title, "not in status PROPOSAL");
      }
    }
  }
}
*/