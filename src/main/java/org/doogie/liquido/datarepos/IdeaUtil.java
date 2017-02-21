package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.IdeaModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.LiquidoRestException;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.doogie.liquido.security.LiquidoAuthUser;
import org.doogie.liquido.security.LiquidoUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

@Component
public class IdeaUtil {

  @Autowired
  LiquidoAuditorAware liquidoAuditorAware;

  /**
   * Check if a given idea is already supported by the currently logged in user.
   * Remark: Of course one could say that an idea is implicitly supported by its creator. But that is not counted in the list of supporters,
   * because an idea needs "external" supporters, to become a suggestion for a law.
   * @param idea an IdeaModel
   * @return  true IF this idea is supported by the currently logged in user
   *         false IF there is no user logged in
   */
  public boolean isSupportedByCurrentUser(IdeaModel idea) {
    //this is used in IdeaProjection.java
    try {
      UserModel currentlyLoggedInUser = liquidoAuditorAware.getCurrentAuditor();
      return idea.getSupporters().contains(currentlyLoggedInUser);
    } catch (Exception e) {
      return false;
    }
  }

}
