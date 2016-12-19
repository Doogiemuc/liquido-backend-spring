package org.doogie.liquido.rest;

import org.doogie.liquido.datarepos.BallotRepo;
import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.model.BallotModel;
import org.doogie.liquido.model.LawModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.Date;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Controller for our RESTfull endpoint: /laws
 *
 * Resouces:
 *   POST /laws -  add a new idea
 */
@RestController
@RequestMapping("/liquido/v2")
public class LawController {
  Logger log = LoggerFactory.getLogger(this.getClass());  // Simple Logging Facade 4 Java

  @Autowired
  LawRepo lawRepo;


  //only handle very specia cases in this class, like


  /**   THIS IS ALREADY HANDLED BY Spring's    RepositoryRestResource    DONE


   **
   * post a new idea.  Will check that the title is unique
   * @param newIdea the new idea (createdAt and updatedAt timestamps will be set by the backend)
   * @param bindingResult result of validation
   * @return the newly created idea (incl. timestamps and its new ID)
   * @throws BindException when validation fails. Exception will contain detailed error information (returned a JSON to the client)
  @RequestMapping(value = "/laws", method = POST)
  public LawModel createNewIdea(@Valid @RequestBody LawModel newIdea, BindingResult bindingResult) throws BindException {
    log.trace("=> POST /laws "+newIdea);

    if (bindingResult.hasErrors()) {
      log.trace("   newIdea is invalid: "+bindingResult.getAllErrors());
      throw new BindException(bindingResult);  // this generates a cool error message. Undocumented spring feature :-)
    }

    LawModel createdIdea = lawRepo.insert(newIdea);   //TODO: catch DuplicateKeyException

    log.trace("<= POST /laws created: "+createdIdea);
    return createdIdea;
  }

  */

}



