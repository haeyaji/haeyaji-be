package com.haeyaji.be.user.oauth.oauth2;

import com.haeyaji.be.user.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class CustomOAuth2User extends DefaultOAuth2User {

    private final User user;
    private final boolean newUser;

    public CustomOAuth2User(User user,
                            boolean newUser,
                            Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String nameAttributeKey) {

        super(authorities, attributes, nameAttributeKey);

        this.user = user;
        this.newUser = newUser;
    }

    @Override
    public String getName() {
        return String.valueOf(user.getId());
    }
}
