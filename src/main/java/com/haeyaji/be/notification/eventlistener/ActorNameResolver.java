package com.haeyaji.be.notification.eventlistener;

import com.haeyaji.be.member.domain.Member;
import com.haeyaji.be.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 알림 문구에 쓸 행위자 이름을 구한다.
 *
 * <p>알림 본문은 "누가 무엇을 했다"가 핵심이라 닉네임이 필요한데, 이벤트마다 닉네임을 실어 나르면
 * 발행 측이 알림 문구 사정을 알아야 한다. 그래서 이벤트는 id만 옮기고 이름 조회는 알림 쪽에서 한다.
 *
 * <p>닉네임은 온보딩 전이면 비어 있을 수 있다 — 이때는 대체 이름을 써서 문장이 깨지지 않게 한다.
 */
@Component
@RequiredArgsConstructor
public class ActorNameResolver {

    private static final String UNKNOWN = "누군가";

    private final MemberRepository memberRepository;

    /** 닉네임, 없으면 {@code "누군가"}. 문장에 그대로 끼워 넣어도 어색하지 않은 값만 돌려준다. */
    public String nicknameOf(UUID memberId) {
        if (memberId == null) {
            return UNKNOWN;
        }
        return memberRepository.findById(memberId)
                .map(Member::getNickname)
                .filter(name -> name != null && !name.isBlank())
                .orElse(UNKNOWN);
    }
}
