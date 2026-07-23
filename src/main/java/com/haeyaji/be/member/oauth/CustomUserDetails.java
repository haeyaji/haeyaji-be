package com.haeyaji.be.member.oauth;

import com.haeyaji.be.member.domain.MemberRole;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {

    private final UUID memberId;
    private final MemberRole role;

    public CustomUserDetails(UUID memberId, MemberRole role) {
        this.memberId = memberId;
        this.role = role;
    }

    public UUID getMemberId() {
        return memberId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    // jwt는 비밀번호가 없음
    @Override
    public @Nullable String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return String.valueOf(memberId);
    }

    // 현재는 form login 없는 jwt전용이라 다른 메서드 true로 유지
}
