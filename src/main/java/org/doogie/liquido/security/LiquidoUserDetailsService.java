package org.doogie.liquido.security;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Arrays;
import java.util.Collection;

/**
 * This class loads users from the DB via {@link UserRepo} and grants them roles.
 *
 * @see {https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#tech-userdetailsservice}
 */
@Slf4j
public class LiquidoUserDetailsService implements UserDetailsService {

  @Autowired
  UserRepo userRepo;

  /**
   * load a user by its email and return it as a LiquidoAuthUser class. This class will be used as
   * principal / authenticatin object throughout the application.
   *
   * Remark: LiquidoAuthUser contains the Liquido specific  {@link UserModel}
   *
   * @param email Liquido uses email adress as username
   * @return the currently logged in {@link LiquidoAuthUser} or null if no user is currently logged in
   * @throws UsernameNotFoundException if email could not be found in the user DB.
   */
  @Override
  public LiquidoAuthUser loadUserByUsername(String email) throws UsernameNotFoundException {
    log.debug("loading user "+email+" from DB for authentication");

    //Just for testting
    /*
    if ("admin".equals(email)) {
      log.debug("==== ADMIN LOGIN ===");
      return new LiquidoAuthUser(email, "adminpwd", Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")), adminFromDB);
    }
    */

    UserModel userModel = userRepo.findByEmail(email);
    if (userModel == null) throw new UsernameNotFoundException("Could not find user '"+email+"'");

    return new LiquidoAuthUser(userModel.getEmail(), userModel.getPassword(), getGrantedAuthorities(userModel), userModel);
  }

  public UserModel getLiquidoUser(String email) throws Exception {
    UserModel liquidoUser = userRepo.findByEmail(email);  // may return null!
    if (liquidoUser == null) throw new Exception("Could not find liquidoUser with email "+email);
    return liquidoUser;
  }

  private Collection<GrantedAuthority> getGrantedAuthorities(UserModel userModel) {
    // if user.email == admin then return "ROLE_ADMIN" else ...
    return Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
  }
}
