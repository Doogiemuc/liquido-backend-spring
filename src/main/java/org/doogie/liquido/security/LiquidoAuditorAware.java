package org.doogie.liquido.security;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.UserModel;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * AuditorAware implementation that returns the currently logged in user.
 * This is for example used to fill @CreatedBy attributes in models.
 * It can also set and return a mocked auditor. (This is used in tests.)
 */
@Slf4j
@Component
public class LiquidoAuditorAware implements AuditorAware<UserModel> {

  UserModel mockAuditor = null;

  /**
   * @return the currently logged in user (may return null!)
   */
  @Override
  public UserModel getCurrentAuditor() {
    if (mockAuditor != null) {
      log.trace("Returning mock auditor "+mockAuditor.getEmail());
      return mockAuditor;
    }
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null || !authentication.isAuthenticated()) {
        log.warn("Cannot getCurrentAuditor. No one is currently authenticated");
        return null;
      }
      LiquidoAuthUser authUser = (LiquidoAuthUser)authentication.getPrincipal();
      return authUser.getUserModel();

      //BUGFIX:  Must not do this, since this will leed to an endless loop StackOverflowException
      //http://stackoverflow.com/questions/42315960/stackoverflowexception-in-spring-data-jpa-app-with-spring-security-auditoraware
      //UserModel currentlyLoggedInUser = userRepo.findByEmail(principal.getUsername()) ;
    } catch (Exception e) {
      log.error("Cannot getCurrentAuditor: "+e);
      return null;
    }
  }

  public void setMockAuditor(UserModel mockAuditor) {
    this.mockAuditor = mockAuditor;
  }
}
