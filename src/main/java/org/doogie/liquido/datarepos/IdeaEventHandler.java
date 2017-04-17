package org.doogie.liquido.datarepos;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.IdeaModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.services.IdeaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleAfterLinkSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RepositoryEventHandler(IdeaModel.class)
public class IdeaEventHandler {

  @Autowired
  IdeaUtil ideaUtil;

  @HandleAfterLinkSave
  public void handleIdeaLinkSave(IdeaModel idea, Set<UserModel> supporters) {
    //log.debug("handleIdeaLinkSave");
    ideaUtil.checkQuorum(idea);
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