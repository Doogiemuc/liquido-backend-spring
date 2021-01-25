package org.doogie.liquido.security;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.util.DoogiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.doogie.liquido.security.LiquidoAuthUser.ROLE_TEAM_ADMIN;
import static org.doogie.liquido.security.LiquidoAuthUser.ROLE_USER;

/**
 * This class loads users from the DB via {@link UserRepo} and grants them roles.
 *
 * @see {https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#tech-userdetailsservice}
 */
@Slf4j
@Component
public class LiquidoUserDetailsService implements UserDetailsService {

  @Autowired
  UserRepo userRepo;

  @Autowired
  LiquidoProperties prop;

  @Autowired
  TeamRepo teamRepo;

  @Autowired
  LiquidoAuditorAware auditorAware;

  // TODO: Cache authenticated users, because loadUserByUsername is called very often! MAYBE simply extend CachingUserDetailsService ?
  /*See:
   @Cacheable("authenticatedUsers")
   https://spring.io/guides/gs/caching/
   https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-caching.html
   https://docs.spring.io/spring/docs/current/spring-framework-reference/html/cache.html#cache-store-configuration-caffeine
   Do not forget to @CacheEvict the cache elem, when a user or its roles & rights change
  */

  /**
   * load a user by its email and return it as a LiquidoAuthUser class. This class will be used as
   * principal / authentication object throughout the application.
   *
   * Remark: LiquidoAuthUser contains the Liquido specific {@link UserModel}
   *
   * @param email Liquido uses email addresses as "usernames"
   * @return the currently logged in {@link LiquidoAuthUser} or null if no user is currently logged in
   * @throws UsernameNotFoundException if email could not be found in the user DB.
   */
  @Override
  public LiquidoAuthUser loadUserByUsername(String email) throws UsernameNotFoundException {
    log.trace("loading user "+email+" from DB for authentication");
    UserModel userModel = userRepo.findByEmail(email)
     .orElseThrow(()-> new UsernameNotFoundException("Could not find user '"+email+"'"));
    // NO PASSWORD!   We authenticate voters with voterTokens! Yeah!
    return new LiquidoAuthUser(userModel.getEmail(), getGrantedAuthorities(userModel), userModel);
  }

  /**
   * Translate from liquido UserModel.roles to spring's SimpleGrantedAuthority
   * @param userModel a liquido user model with roles.
   * @return set of GrantedAuthorities for spring.
   */
  private Set<GrantedAuthority> getGrantedAuthorities(UserModel userModel) {
    if (userModel == null) throw new RuntimeException("Cannot getGrantedAuthorities for null user!");
    Set<GrantedAuthority> authorities = new HashSet<>();
    authorities.add(new SimpleGrantedAuthority(ROLE_USER));
    if (userModel.roles.contains(ROLE_TEAM_ADMIN))
      authorities.add(new SimpleGrantedAuthority(ROLE_TEAM_ADMIN));
    return authorities;
  }

  /**
   * Get the currently authenticated user from the SecurityContext
   * @return the LIQUIDO UserModel from within the authenticated principal (see LiquidoAuthUser)
   * @throws LiquidoException if there is no currently authenticated principal or user
   */
  public UserModel getAuthenticatedUser() throws LiquidoException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Not logged in. No authentication");
    }
    if (authentication.getPrincipal() != null && authentication.getPrincipal().equals("anonymousUser")) {
      throw new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, "Not logged in. No principal");
    }
    // principal _IS_ a LiquidoAuthUser, because
    // 1. I load one in LiquidoUserDetailsService.java
    // 2. and JwtAuthenticationFilter puts it into the security context
    LiquidoAuthUser authUser = (LiquidoAuthUser)authentication.getPrincipal();
    return authUser.getLiquidoUserModel();
  }

  /**
   * Find the team of the currently logged in user
   * @return
   * @throws LiquidoException
   */
  public TeamModel getTeam() throws LiquidoException {
    UserModel currentUser = getAuthenticatedUser();
    //TODO: cache team
    return teamRepo.findById(currentUser.getTeamId())
      .orElseThrow(LiquidoException.supply(LiquidoException.Errors.UNAUTHORIZED, "Cannot find users team."));
  }
}
