package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.IdeaModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidioUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

@Component
public class IdeaUtil {

  @Autowired
  LiquidioUserDetailsService userDetailsService;

  /**
   * check if a given idea is created or supported by the currently logged in user.
   * @param idea an IdeaModel
   * @return  true IF this idea is supported by the currently logged in user
   *            or IF this idea is created by the currently logged in user,
   *         false IF there is no user logged in
   */
  public boolean isSupportedByCurrentUser(IdeaModel idea) {
    try {
      User principal = (org.springframework.security.core.userdetails.User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      UserModel currentlyLoggedInUser = userDetailsService.getLiquidoUser(principal.getUsername());
      return idea.getCreatedBy().equals(currentlyLoggedInUser) || idea.getSupporters().contains(currentlyLoggedInUser);
    } catch (Exception e) {
      return false;
    }
  }

  //see also possible method argument :    (... @AuthenticationPrincipal UserModel userModel ..)
}
