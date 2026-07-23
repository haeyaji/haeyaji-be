package com.haeyaji.be.member.oauth.oauth2;

import com.haeyaji.be.member.domain.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class CustomOAuth2User extends DefaultOAuth2User {

    private final Member member;
    private final boolean newMember;

    public CustomOAuth2User(Member member,
                            boolean newMember,
                            Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String nameAttributeKey) {

        super(authorities, attributes, nameAttributeKey);

        this.member = member;
        this.newMember = newMember;
    }

    @Override
    public String getName() {
        return String.valueOf(member.getId());
    }
}
