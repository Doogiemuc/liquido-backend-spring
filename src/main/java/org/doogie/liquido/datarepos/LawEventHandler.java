package org.doogie.liquido.datarepos;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.services.LawService;
import org.doogie.liquido.services.LiquidoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleAfterLinkSave;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RepositoryEventHandler(LawModel.class)
public class LawEventHandler {

  @Autowired
  LawService lawService;

  @Autowired
  LiquidoAuditorAware liquidoAuditorAware;

  //BUGFIX:  All of this is only called for REST operations.   SOLUTION: added logic directly into LawService.addSupporter
  /**
   * This is called when a supporter is added from an idea.
   * Then we'll check if the quorum has been reached.
   * @param idea a supported has been added to this idea
   * @param supporters the new set of supporters
   */
  @HandleAfterLinkSave
  public void handleLawLinkSave(LawModel idea, Set<UserModel> supporters) throws LiquidoException {
     UserModel currentUser = liquidoAuditorAware.getCurrentAuditor().orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "MUST be logged in to add a supporter"));
    if (supporters.contains(currentUser)) throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_SUPPORTER, "User must not support his own proposal!");
    log.debug("handleLawLinkSave: adding supporters: "+supporters+" to idea "+idea);
    lawService.checkQuorum(idea);
  }


  /*

  // called only for PUT !!!
  @HandleAfterSave
  public void handleIdeaSave(LawModel idea) {
    log.debug("handleIdeaSave ");
  }


  // called when POST
  @HandleBeforeCreate
  public void handleBeforeCreateIdea(LawModel idea) {
    log.debug("handleBeforeCreateIdea");
  }

  */

}