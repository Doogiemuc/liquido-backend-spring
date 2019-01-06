package org.doogie.liquido.security;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.Nullable;
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

  UserModel mockAuditor = null;

  /**
   * Get the currently logged in user.
   * @return (An Java optional that resolves to) the currently logged in user as a liquido UserModel
   */
  @Override
  public Optional<UserModel> getCurrentAuditor() {
    if (mockAuditor != null) {
    	// warn about mock users, but only if we are not in dev or test
			if (!springEnv.acceptsProfiles(Profiles.of("dev", "test")))
				log.warn("Returning mock auditor "+mockAuditor.getEmail());
      return Optional.of(mockAuditor);
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      log.trace("Cannot getCurrentAuditor. No one is currently authenticated");
      return Optional.empty();
    }

    if (authentication.getPrincipal() != null && authentication.getPrincipal().equals("anonymousUser")) {
      log.trace("Anonymous user");
			return Optional.empty();
    }
		// principal _IS_ a LiquidoAuthUser, because
		// 1. I load one in LiquidoUserDetailsService.java
		// 2. and JwtAuthenticationFilter puts it into the security context
    LiquidoAuthUser authUser = (LiquidoAuthUser)authentication.getPrincipal();
    //log.trace("Returning current auditor " + authUser);

    return Optional.of(authUser.getLiquidoUserModel());

    //BUGFIX:  Must not call userRepo, since this will lead to an endless loop and throw a StackOverflowException
    //https://stackoverflow.com/questions/14223649/how-to-implement-auditoraware-with-spring-data-jpa-and-spring-security
    //http://stackoverflow.com/questions/42315960/stackoverflowexception-in-spring-data-jpa-app-with-spring-security-auditoraware
    //UserModel currentlyLoggedInUser = userRepo.findByEmail(principal.getUsername()) ;

  }

  public void setMockAuditor(@Nullable UserModel mockAuditor) {
    if (springEnv.acceptsProfiles(Profiles.of("dev", "test"))) {
      log.debug("Setting mockauditor to "+mockAuditor);  // mockauditor may be null!!!
    }
    this.mockAuditor = mockAuditor;
  }
}
