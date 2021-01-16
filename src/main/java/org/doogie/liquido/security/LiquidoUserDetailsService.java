package org.doogie.liquido.security;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.util.DoogiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.*;

/**
 * This class loads users from the DB via {@link UserRepo} and grants them roles.
 *
 * @see {https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#tech-userdetailsservice}
 */
@Slf4j
public class LiquidoUserDetailsService implements UserDetailsService {

  @Autowired
  UserRepo userRepo;

  @Autowired
  LiquidoProperties prop;

  // TODO: Cache autenticated users, because loadUserByUsername is called very often! MAYBE simply extend CachingUserDetailsService ?
  /*See:
   @Cacheable("authenticedUsers")
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

  private List<GrantedAuthority> getGrantedAuthorities(UserModel userModel) {
    if (userModel == null) throw new RuntimeException("Cannot getGrantedAuthorities for null user!");
    List<GrantedAuthority> authorities = new ArrayList<>();
    if (prop.admin != null &&
        prop.admin.email != null && prop.admin.email.equals(userModel.getEmail()) &&
        prop.admin.name  != null && prop.admin.name.equals(userModel.getProfile().getName()) &&
        prop.admin.mobilephone != null && prop.admin.mobilephone.equals(userModel.getProfile().getMobilephone())
    ) {
      authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
      log.info("The ADMIN logged in!");
    }
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    return authorities;
  }
}
