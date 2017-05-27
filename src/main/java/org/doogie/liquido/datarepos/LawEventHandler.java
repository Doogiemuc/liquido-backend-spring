package org.doogie.liquido.datarepos;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.LawModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.services.LawService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleAfterLinkSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RepositoryEventHandler(LawModel.class)
public class LawEventHandler {

  @Autowired
  LawService lawService;

  @HandleAfterLinkSave
  public void handleLawLinkSave(LawModel idea, Set<UserModel> supporters) {
    log.debug("handleLawLinkSave: supporter added: "+supporters);
    lawService.checkQuorum(idea);
  }

  /*

  // called only for PUT !!!
  @HandleAfterSave
  public void handleIdeaSave(IdeaModel idea) {
    log.debug("handleIdeaSave ");
  }


  // called when POST
  @HandleBeforeCreate
  public void handleBeforeCreateIdea(IdeaModel idea) {
    log.debug("handleBeforeCreateIdea");
  }
  */



}