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
 * Adapter between my [@link {@link UserModel} and the {@link org.springframework.security.core.userdetails.User}
 * This class loads users from the DB via {@link UserRepo} and grants them roles.
 */
@Slf4j
public class LiquidioUserDetailsService implements UserDetailsService {

  @Autowired
  UserRepo userRepo;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    //TODO: Remove default admin login!
    /*
    if ("admin".equals(email)) {
      log.debug("==== ADMIN LOGIN ===");
      return new User(email, "adminpwd", Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
    }
    */

    //log.debug("Security Loading user "+email+ " for granting access");
    UserModel user = userRepo.findByEmail(email);
    if (user == null) throw new UsernameNotFoundException("Could not find user '"+email+"'");
    return new User(user.getEmail(), user.getPassword(), getGrantedAuthorities(user));
  }

  private Collection<GrantedAuthority> getGrantedAuthorities(UserModel user) {
    // if user.email == admin then return "ROLE_ADMIN" else ...
    return Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
  }
}
