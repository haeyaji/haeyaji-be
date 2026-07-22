package com.haeyaji.be.user.oauth.oidc;

import com.haeyaji.be.user.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collection;

@Getter
public class CustomOidcUser extends DefaultOidcUser {

    private final User user;
    private final boolean newUser;

    public CustomOidcUser(User user,
                          boolean newUser,
                          Collection<? extends GrantedAuthority> authorities,
                          OidcIdToken idToken,
                          OidcUserInfo userInfo,
                          String nameAttributeKey) {

        super(authorities, idToken, userInfo, nameAttributeKey);

        this.user = user;
        this.newUser = newUser;
    }

    @Override
    public String getName() {
        return String.valueOf(user.getId());
    }
}
