package com.haeyaji.be.notification.controller;

import com.haeyaji.be.common.exception.ResponseCode;
import com.haeyaji.be.common.response.ApiResponse;
import com.haeyaji.be.common.response.CursorPageResponse;
import com.haeyaji.be.common.response.SuccessCode;
import com.haeyaji.be.member.oauth.CustomUserDetails;
import com.haeyaji.be.notification.domain.Notification;
import com.haeyaji.be.notification.domain.NotificationType;
import com.haeyaji.be.notification.dto.NotificationResponse;
import com.haeyaji.be.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 알림.
 *
 * <pre>
 * GET    /api/notifications                 목록 조회 (커서 페이지네이션, type 필터)
 * GET    /api/notifications/unread-count    미읽음 개수 조회
 * POST   /api/notifications/{id}/read       단건 읽음 처리
 * POST   /api/notifications/read-all        전체 읽음 처리
 * DELETE /api/notifications/{id}            삭제
 * </pre>
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<CursorPageResponse<NotificationResponse, UUID>> getNotifications(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                                          @RequestParam(required = false) NotificationType type,
                                                                                          @RequestParam(required = false) UUID cursor,
                                                                                          @RequestParam(defaultValue = "20") int size) {

        CursorPageResponse<Notification, UUID> pageResponse = notificationService.getNotifications(userDetails.getMemberId(), type, cursor, size);

        return ApiResponse.of(pageResponse.map(NotificationResponse::from), SuccessCode.GET_SUCCESS);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> getUnreadCount(@AuthenticationPrincipal CustomUserDetails userDetails) {

        long unreadNotiCount = notificationService.getUnreadCount(userDetails.getMemberId());

        return ApiResponse.of(unreadNotiCount, SuccessCode.GET_SUCCESS);
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@AuthenticationPrincipal CustomUserDetails userDetails,
                                         @PathVariable UUID id) {

        notificationService.markAsRead(id, userDetails.getMemberId());

        return ApiResponse.of(null, SuccessCode.PUT_SUCCESS);
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {

        notificationService.markAllAsRead(userDetails.getMemberId());

        return ApiResponse.of(null, SuccessCode.PUT_SUCCESS);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                 @PathVariable UUID id) {

        notificationService.deleteNotification(id, userDetails.getMemberId());

        return ApiResponse.of(null, SuccessCode.DELETE_SUCCESS);
    }
}
