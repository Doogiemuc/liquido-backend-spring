package org.doogie.liquido.security;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * AuditorAware implementation that returns the currently logged in user.
 * This is for example used to fill @CreatedBy attributes in models.
 * It can also set and return a mocked auditor. (This is used in tests.)
 */
@Slf4j
@Component
public class LiquidoAuditorAware implements AuditorAware<UserModel> {

	@Autowired
	Environment springEnv;

	boolean warnIfMockAuditor = false;
  UserModel mockAuditor = null;



  /**
   * @return the currently logged in user (may return null!)
   */
  @Override
  public Optional<UserModel> getCurrentAuditor() {
    if (mockAuditor != null) {
    	//TODO: only warn when not --seedDB
			if (!springEnv.acceptsProfiles("test") && warnIfMockAuditor)
				log.warn("Returning mock auditor "+mockAuditor.getEmail());
      return Optional.of(mockAuditor);
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      log.warn("Cannot getCurrentAuditor. No one is currently authenticated");
      return Optional.empty();
    }

    if (authentication.getPrincipal() != null && authentication.getPrincipal().equals("anonymousUser")) {
      log.trace("Anonymous user");
			return Optional.empty();
    }
		// principal _IS_ a LiquidoAuthUser, because
		// 1. I put one in there   in LiquidoUserDetailsService.java  AND
		// 2. I configured the Oauth AuthorizationServerConfig.java to use the LiquidoUSerDetailsService.java
    LiquidoAuthUser authUser = (LiquidoAuthUser)authentication.getPrincipal();
    log.trace("Returning authUser" + authUser);

    return Optional.of(authUser.getLiquidoUserModel());

    //BUGFIX:  Must not do this, since this will lead to an endless loop StackOverflowException
    //https://stackoverflow.com/questions/14223649/how-to-implement-auditoraware-with-spring-data-jpa-and-spring-security
    //http://stackoverflow.com/questions/42315960/stackoverflowexception-in-spring-data-jpa-app-with-spring-security-auditoraware

    //UserModel currentlyLoggedInUser = userRepo.findByEmail(principal.getUsername()) ;

  }

  public void setMockAuditor(UserModel mockAuditor) {
    this.mockAuditor = mockAuditor;
  }
}
