package org.doogie.liquido.datarepos;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.services.LawService;
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

  //BUGFIX:  All of this is only called for REST operations.   SOLUTION: added logic directly into LawService.addSupporter
  /**
   * This is called when a supporter is added from an idea.
   * Then we'll check if the quorum has been reached.
   * @param idea a supported has been added to this idea
   * @param supporters the new set of supporters
   */
  @HandleAfterLinkSave
  public void handleLawLinkSave(LawModel idea, Set<UserModel> supporters) {
    log.debug("handleLawLinkSave: supporter added: "+supporters);
    lawService.checkQuorum(idea);
  }



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




}