package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.starter.model.dto.ApiResponse;
import com.openquartz.easyarchive.starter.model.dto.InAppNotificationListItem;
import com.openquartz.easyarchive.starter.model.dto.InAppNotificationUnreadCountView;
import com.openquartz.easyarchive.starter.service.InAppNotificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/in-app-notifications")
@RequiredArgsConstructor
public class InAppNotificationController {

    private final InAppNotificationQueryService queryService;

    @GetMapping("/unread-count")
    public ApiResponse<InAppNotificationUnreadCountView> unreadCount() {
        return ApiResponse.success(queryService.getUnreadCount());
    }

    @GetMapping
    public ApiResponse<List<InAppNotificationListItem>> list(@RequestParam(defaultValue = "20") Integer limit) {
        return ApiResponse.success(queryService.listLatest(limit == null ? 20 : limit));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<?> markRead(@PathVariable Long id) {
        queryService.markRead(id);
        return ApiResponse.success();
    }

    @PostMapping("/read-all")
    public ApiResponse<?> markAllRead() {
        queryService.markAllRead();
        return ApiResponse.success();
    }
}
