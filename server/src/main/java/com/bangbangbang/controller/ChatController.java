package com.bangbangbang.controller;

import com.bangbangbang.common.ApiResult;
import com.bangbangbang.common.BizException;
import com.bangbangbang.common.UserContext;
import com.bangbangbang.entity.ChatMessage;
import com.bangbangbang.service.ChatService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 私信：会话列表、拉取消息、发送消息
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Data
    public static class CreateSessionRequest {
        private String targetUserId;
    }

    @Data
    public static class SendMessageRequest {
        private String sessionId;
        private String type;
        private String content;
        private Integer duration;
        private String audioPath;
    }

    @GetMapping("/sessions")
    public ApiResult<List<Map<String, Object>>> sessions() {
        return ApiResult.ok(chatService.listSessions(UserContext.require().id()));
    }

    /** 打开或创建与某人的会话 */
    @PostMapping("/sessions")
    public ApiResult<Map<String, Object>> createSession(@RequestBody CreateSessionRequest req) {
        return ApiResult.ok(chatService.ensureSession(UserContext.require().id(), req.getTargetUserId()));
    }

    @PostMapping("/sessions/{id}/read")
    public ApiResult<Void> markRead(@PathVariable String id) {
        chatService.markRead(UserContext.require().id(), id);
        return ApiResult.ok();
    }

    @GetMapping("/messages")
    public ApiResult<List<ChatMessage>> messages(@RequestParam String sessionId) {
        return ApiResult.ok(chatService.listMessages(UserContext.require().id(), sessionId));
    }

    @PostMapping("/messages")
    public ApiResult<ChatMessage> send(@RequestBody SendMessageRequest req) {
        String meId = UserContext.require().id();
        if (req.getSessionId() == null || req.getSessionId().isEmpty()) {
            throw BizException.of("缺少会话ID");
        }
        if ("text".equals(req.getType()) && (req.getContent() == null || req.getContent().trim().isEmpty())) {
            throw BizException.of("消息内容不能为空");
        }
        return ApiResult.ok(chatService.sendMessage(
                meId,
                req.getSessionId(),
                req.getType(),
                req.getContent(),
                req.getDuration(),
                req.getAudioPath()
        ));
    }
}
