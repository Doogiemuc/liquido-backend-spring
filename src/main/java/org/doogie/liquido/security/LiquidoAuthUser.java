package org.doogie.liquido.security;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.model.UserModel;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Adapter between my Liquido-{@link UserModel} and Spring-security's {@link org.springframework.security.core.userdetails.User}
 * The spring User can be retrieved as the HTTP Principal. With this adapter you can get our custom Liquido UserModel from that.
 */
@Slf4j
public class LiquidoAuthUser extends User {  // org.springframework.security.core.userdetails.User implements UserDetails, CredentialsContainer
  private UserModel liquidoUserModel;

  /**
   * Create a new Liquido authentication user that contains a custom liquidoUserModel.
   * No password here!
   * @param username
   * @param authorities
   * @param liquidoUserModel
   */
  public LiquidoAuthUser(String username, Collection<? extends GrantedAuthority> authorities, @NonNull UserModel liquidoUserModel) {
    super(username, "", authorities);
    this.liquidoUserModel = liquidoUserModel;
  }

  public UserModel getLiquidoUserModel() {
  	return liquidoUserModel;
  }


  /**
   * There are no passwords in LIQUIDO. Instead we use JsonWebTokens with a 2-factor-authentication.
   * So we throw an exception when someone calls this!
   * @throws RuntimeException whenever someone calls this!
   */
  @Override
  public String getPassword() {
    log.warn("SEC TRACE: LiquidoAuthUser.java: Some code is getting the password of "+this.getUsername()+ "!!! There are no passwords in LIQUIDO. This call should never happen!");
    throw new RuntimeException("There are no passwords in LIQUIDO!");
  }
}
