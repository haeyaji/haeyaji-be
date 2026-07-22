package com.haeyaji.be.user.oauth.oauth2;

import com.haeyaji.be.user.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

public class CustomOAuth2User extends DefaultOAuth2User {

    private final User user;

    public CustomOAuth2User(User user,
                            Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String nameAttributeKey) {
        super(authorities, attributes, nameAttributeKey);
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
