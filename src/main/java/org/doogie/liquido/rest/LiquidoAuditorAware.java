package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidioUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

/**
 * AuditorAware that is responsible for fetching the currently logged in user from
 * {@link LiquidioUserDetailsService}
 * It can also set and return a mocked auditor. (This is used in tests.)
 */
@Slf4j
@Component
public class LiquidoAuditorAware implements AuditorAware<UserModel> {

  @Autowired
  LiquidioUserDetailsService userDetailsService;

  UserModel mockAuditor = null;

  /**
   * @return the currently logged in user (may return null!)
   */
  @Override
  public UserModel getCurrentAuditor() {
    try {
      if (mockAuditor != null) {
        log.trace("returning mockAuditor "+mockAuditor);
        return mockAuditor;
      }

      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      if (authentication == null || !authentication.isAuthenticated()) {
        log.warn("Cannot getCurrentAuditor. No one is currently authenticated");
        return null;
      }

      User principal = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
      UserModel currentlyLoggedInUser = userDetailsService.getLiquidoUser(principal.getUsername());
      return currentlyLoggedInUser;
    } catch (Exception e) {
      log.error("Cannot getCurrentAuditor: "+e);
      return null;
    }
  }

  public void setMockAuditor(UserModel userModel) {
    this.mockAuditor = userModel;
  }
}
