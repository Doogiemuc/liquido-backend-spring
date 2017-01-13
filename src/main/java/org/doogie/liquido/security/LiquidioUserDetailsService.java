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
 *
 * It loads users from the DB with {@link UserRepo}
 */
@Slf4j
public class LiquidioUserDetailsService implements UserDetailsService {

  @Autowired
  UserRepo userRepo;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    if ("admin".equals(email)) {
      log.debug("==== ADMIN LOGIN ===");
      return new User(email, "admin", Arrays.asList(new SimpleGrantedAuthority("ROLE_USER")));
    }


    log.debug("Loading user "+email);
    UserModel user = userRepo.findByEmail(email);
    if (user == null) throw new UsernameNotFoundException("Could not find user '"+email+"'");
    return new User(user.getEmail(), user.getPassword(), getGrantedAuthorities(user));
  }

  private Collection<GrantedAuthority> getGrantedAuthorities(UserModel user) {
    return Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
  }
}
