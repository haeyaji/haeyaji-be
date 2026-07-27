package com.haeyaji.be.notification.mail;

import com.haeyaji.be.member.domain.Member;
import com.haeyaji.be.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 회원 → 받을 메일 주소. 소셜 동의 거부·미입력으로 이메일이 없는 회원은 목록에서 빠진다
 * (카카오는 검수 전이면 이메일을 안 내려줘 온보딩에서 직접 받는다).
 */
@Component
@RequiredArgsConstructor
public class MemberMails {

    private final MemberRepository memberRepository;

    public List<String> of(Collection<UUID> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }
        return memberRepository.findAllById(memberIds).stream()
                .map(Member::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .toList();
    }

    /** 한 명의 주소. 없으면 {@code null} — 호출부가 발송을 건너뛴다. */
    public String of(UUID memberId) {
        if (memberId == null) {
            return null;
        }
        return memberRepository.findById(memberId)
                .map(Member::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .orElse(null);
    }
}
