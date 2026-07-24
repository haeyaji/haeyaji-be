package com.haeyaji.be.member.controller;

import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.member.domain.Member;
import com.haeyaji.be.member.dto.MemberResponse;
import com.haeyaji.be.member.dto.MemberSearchResponse;
import com.haeyaji.be.member.dto.NicknameUpdateRequest;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import com.haeyaji.be.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = memberService.getMember(userDetails.getMemberId());
        return ApiResponse.of(MemberResponse.from(member), SuccessCode.GET_SUCCESS);
    }

    @PatchMapping("/me/nickname")
    public ApiResponse<Void> updateNickname(@AuthenticationPrincipal CustomUserDetails userDetails,
                                             @Valid @RequestBody NicknameUpdateRequest request) {
        memberService.updateNickname(userDetails.getMemberId(), request.nickname());
        return ApiResponse.of(null, SuccessCode.PUT_SUCCESS);
    }

    @GetMapping("/search/{nickname}")
    public ApiResponse<MemberSearchResponse> searchMember(@PathVariable String nickname) {
        Member member = memberService.searchMemberByNickname(nickname);

        return ApiResponse.of(MemberSearchResponse.from(member), SuccessCode.GET_SUCCESS);
    }
}
