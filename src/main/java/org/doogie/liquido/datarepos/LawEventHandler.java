package org.doogie.liquido.datarepos;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.jwt.AuthUtil;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.services.LawService;
import org.doogie.liquido.services.LiquidoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleBeforeLinkSave;
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
  AuthUtil authUtil;

  //BUGFIX:  All of this is only called for REST operations.   SOLUTION: Also added same logic directly into LawService.addSupporter()
  //BUGFIX 2: https://jira.spring.io/browse/DATAREST-1241
  /**
   * This is called when a supporter is added via Spring data rest directly.
   * Then we'll check if the quorum has been reached.
   *
   * POST /laws/{id}/supporters
   * content-type: text/uri-list
   * body:   /liquido-api/v3/user/1   *
   *
   * @param idea a supported has been added to this idea
   * @param supporters the <b>new</b> set of supporters
   */
  @HandleBeforeLinkSave
  public void handleLawLinkSave(LawModel idea, Set<UserModel> supporters) throws LiquidoException {
    UserModel currentUser = authUtil.getCurrentUser()
      .orElseThrow(() -> new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "MUST be logged in to add a supporter"));
    if (supporters.contains(idea.getCreatedBy())) {
      log.debug(idea.getCreatedBy().toStringShort()+" must not support his own proposal! "+idea);
      throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_SUPPORTER, "You cannot support your own idea or proposal!");
    }
    log.debug("handleLawLinkSave: "+currentUser.toStringShort()+" now supports "+idea);
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