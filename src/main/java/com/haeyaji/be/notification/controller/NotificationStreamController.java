package com.haeyaji.be.notification.controller;

import com.haeyaji.be.member.oauth.CustomUserDetails;
import com.haeyaji.be.notification.sse.SseConnectionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationStreamController {

    private final SseConnectionRegistry sseConnectionRegistry;

    @GetMapping("/stream")
    public SseEmitter stream(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return sseConnectionRegistry.register(userDetails.getMemberId());
    }
}
