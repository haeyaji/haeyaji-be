package com.haeyaji.be.user.oauth.oidc;

import com.haeyaji.be.user.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collection;

public class CustomOidcUser extends DefaultOidcUser {

    private final User user;

    public CustomOidcUser(User user,
                          Collection<? extends GrantedAuthority> authorities,
                          OidcIdToken idToken,
                          OidcUserInfo userInfo,
                          String nameAttributeKey) {
        super(authorities, idToken, userInfo, nameAttributeKey);
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String getName() {
        return String.valueOf(user.getId());
    }
}
