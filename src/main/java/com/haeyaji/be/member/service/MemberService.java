package com.haeyaji.be.member.service;

import com.haeyaji.be.common.exception.BusinessException;
import com.haeyaji.be.common.exception.ErrorCode;
import com.haeyaji.be.member.domain.Member;
import com.haeyaji.be.member.domain.MemberStatus;
import com.haeyaji.be.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public Member getMember(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 탈퇴한 회원은 존재하지 않는 것과 동일하게 취급
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        return member;
    }

    @Transactional
    public Member updateNickname(UUID memberId, String nickname) {

        // 1) 사전 중복 체크 (일반적인 케이스에서 바로 명확한 에러 메시지를 주기 위함)
        if (memberRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        member.assignNickname(nickname);

        // 2) 최종 방어선: 사전 체크 이후 동시에 같은 닉네임으로 요청이 들어와도
        // nickname 컬럼의 unique 제약이 막아준다. saveAndFlush로 즉시 반영시켜야
        // 이 메서드 안에서 예외를 catch할 수 있다 (커밋 시점까지 미루면 여기서 못 잡음).
        try {
            return memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    public Member searchMemberByNickname(String nickname) {

        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 탈퇴한 회원은 검색 결과에도 노출하지 않음
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        return member;
    }
}
