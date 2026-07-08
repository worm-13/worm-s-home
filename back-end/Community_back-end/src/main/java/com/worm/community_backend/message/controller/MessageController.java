package com.worm.community_backend.message.controller;

import com.worm.community_backend.common.ApiResponse;
import com.worm.community_backend.message.service.MessageService;
import com.worm.community_backend.message.vo.ConversationVO;
import com.worm.community_backend.message.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public ApiResponse<MessageVO> sendMessage(@RequestBody Map<String, Object> body, Authentication auth) {
        Long receiverId = Long.valueOf(body.get("receiverId").toString());
        String content = (String) body.get("content");
        MessageVO vo = messageService.sendMessage((Long) auth.getPrincipal(), receiverId, content);
        return ApiResponse.success(vo);
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationVO>> getConversations(Authentication auth) {
        return ApiResponse.success(messageService.getConversations((Long) auth.getPrincipal()));
    }

    @GetMapping("/conversations/{userId}")
    public ApiResponse<List<MessageVO>> getConversation(@PathVariable Long userId,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "50") int size,
                                                         Authentication auth) {
        return ApiResponse.success(messageService.getConversation((Long) auth.getPrincipal(), userId, page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Integer>> getUnreadCount(Authentication auth) {
        int count = messageService.countUnread((Long) auth.getPrincipal());
        return ApiResponse.success(Map.of("count", count));
    }

    @PutMapping("/conversations/{userId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long userId, Authentication auth) {
        messageService.markAsRead((Long) auth.getPrincipal(), userId);
        return ApiResponse.success(null);
    }
}
