package com.haeyaji.be.member.oauth.oidc;

import com.haeyaji.be.member.domain.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collection;

@Getter
public class CustomOidcUser extends DefaultOidcUser {

    private final Member member;
    private final boolean newMember;

    public CustomOidcUser(Member member,
                          boolean newMember,
                          Collection<? extends GrantedAuthority> authorities,
                          OidcIdToken idToken,
                          OidcUserInfo userInfo,
                          String nameAttributeKey) {

        super(authorities, idToken, userInfo, nameAttributeKey);

        this.member = member;
        this.newMember = newMember;
    }

    @Override
    public String getName() {
        return String.valueOf(member.getId());
    }
}
