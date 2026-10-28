package com.bangbangbang.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bangbangbang.common.BizException;
import com.bangbangbang.entity.ChatMessage;
import com.bangbangbang.entity.ChatSession;
import com.bangbangbang.entity.SysUser;
import com.bangbangbang.mapper.ChatMessageMapper;
import com.bangbangbang.mapper.ChatSessionMapper;
import com.bangbangbang.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话与消息服务
 *
 * 会话采用「一对用户只有一条会话」的模型：
 *   sessionId = sess_{较小ID}__{较大ID}
 * 双方看到的是同一条会话、同一批消息，只是 targetUser 各自不同。
 * 这样聊天是真正的双人对话，而不是各存各的假数据。
 */
@Service
public class ChatService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH : mm");

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final SysUserMapper userMapper;

    public ChatService(ChatSessionMapper sessionMapper,
                       ChatMessageMapper messageMapper,
                       SysUserMapper userMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
    }

    /* ------------------------------------------------------------------ */
    /* 会话                                                               */
    /* ------------------------------------------------------------------ */

    @Transactional
    public Map<String, Object> ensureSession(String meId, String targetUserId) {
        if (targetUserId == null || targetUserId.isEmpty()) {
            throw BizException.of("请选择聊天对象");
        }
        if (meId.equals(targetUserId)) {
            throw BizException.of("不能和自己发起会话");
        }
        SysUser target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw BizException.of("对方账号不存在");
        }

        String sessionId = buildSessionId(meId, targetUserId);
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            session = new ChatSession();
            session.setSessionId(sessionId);
            session.setUserAId(min(meId, targetUserId));
            session.setUserBId(max(meId, targetUserId));
            session.setUnreadA(0);
            session.setUnreadB(0);
            session.setLastMessage("新建立了私信沟通");
            session.setLastTime(now());
            session.setSortNo(nextSortNo());
            // 兼容旧字段：记录 userA 视角的对方信息
            SysUser userA = session.getUserAId().equals(meId) ? target : userMapper.selectById(session.getUserAId());
            if (userA != null) {
                session.setTargetUserId(userA.getId());
                session.setTargetUserName(userA.getName());
                session.setTargetUserAvatar(userA.getAvatar());
                session.setTargetUserTag(userA.getIdentityTag());
            }
            sessionMapper.insert(session);
        }
        return toSessionView(session, meId);
    }

    public List<Map<String, Object>> listSessions(String meId) {
        QueryWrapper<ChatSession> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w.eq("user_a_id", meId).or().eq("user_b_id", meId));
        wrapper.orderByAsc("sort_no");
        List<ChatSession> list = sessionMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        if (list == null) {
            return result;
        }
        for (ChatSession session : list) {
            result.add(toSessionView(session, meId));
        }
        return result;
    }

    public void markRead(String meId, String sessionId) {
        ChatSession session = requireSession(meId, sessionId);
        if (meId.equals(session.getUserAId())) {
            session.setUnreadA(0);
        } else {
            session.setUnreadB(0);
        }
        sessionMapper.updateById(session);
    }

    /* ------------------------------------------------------------------ */
    /* 消息                                                               */
    /* ------------------------------------------------------------------ */

    public List<ChatMessage> listMessages(String meId, String sessionId) {
        requireSession(meId, sessionId);
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId).orderByAsc("timestamp");
        List<ChatMessage> list = messageMapper.selectList(wrapper);
        return list == null ? new ArrayList<>() : list;
    }

    @Transactional
    public ChatMessage sendMessage(String meId, String sessionId, String type, String content,
                                   Integer duration, String audioPath) {
        ChatSession session = requireSession(meId, sessionId);
        String receiverId = meId.equals(session.getUserAId()) ? session.getUserBId() : session.getUserAId();

        ChatMessage message = new ChatMessage();
        message.setId("msg_" + System.nanoTime());
        message.setSessionId(sessionId);
        message.setSenderId(meId);
        message.setReceiverId(receiverId);
        String msgType = (type == null || type.isEmpty()) ? "text" : type;
        message.setType(msgType);
        if ("text".equals(msgType)) {
            // 私信不保留标点：，。？！
            String cleaned = stripPunctuation(content);
            if (cleaned.isEmpty()) {
                throw BizException.of("消息内容不能全是标点符号");
            }
            message.setContent(cleaned);
        } else {
            message.setContent(content == null ? "" : content);
        }
        message.setDuration(duration);
        message.setAudioPath(audioPath);
        message.setCreatedAt(now());
        message.setTimestamp(System.currentTimeMillis());
        messageMapper.insert(message);

        session.setLastMessage(preview(message));
        session.setLastTime(message.getCreatedAt());
        if (receiverId.equals(session.getUserAId())) {
            session.setUnreadA((session.getUnreadA() == null ? 0 : session.getUnreadA()) + 1);
            session.setTargetUserId(meId);
        } else {
            session.setUnreadB((session.getUnreadB() == null ? 0 : session.getUnreadB()) + 1);
        }
        sessionMapper.updateById(session);
        return message;
    }

    /* ------------------------------------------------------------------ */
    /* 内部工具                                                            */
    /* ------------------------------------------------------------------ */

    public ChatSession requireSession(String meId, String sessionId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw BizException.of("会话不存在");
        }
        boolean member = meId.equals(session.getUserAId()) || meId.equals(session.getUserBId());
        if (!member) {
            throw BizException.of("无权访问该会话");
        }
        return session;
    }

    private Map<String, Object> toSessionView(ChatSession session, String meId) {
        String otherId = meId.equals(session.getUserAId()) ? session.getUserBId() : session.getUserAId();
        SysUser other = otherId == null ? null : userMapper.selectById(otherId);

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("sessionId", session.getSessionId());
        view.put("lastMessage", session.getLastMessage());
        view.put("lastTime", session.getLastTime());
        view.put("unreadCount", meId.equals(session.getUserAId())
                ? nvl(session.getUnreadA()) : nvl(session.getUnreadB()));
        view.put("relatedOrderId", session.getRelatedOrderId());
        view.put("targetUser", other == null ? null : toUserBrief(other));
        view.put("sortNo", session.getSortNo());
        return view;
    }

    private Map<String, Object> toUserBrief(SysUser user) {
        Map<String, Object> brief = new LinkedHashMap<>();
        brief.put("id", user.getId());
        brief.put("name", user.getName());
        brief.put("avatar", user.getAvatar());
        brief.put("tag", user.getIdentityTag());
        brief.put("phone", user.getPhone());
        return brief;
    }

    /**
     * 去掉中文标点「，。？！」以及对应的英文标点
     *
     * 这是刻意的产品规则：私信只保留文字内容本身。
     * 前端发送前也会清理一次，这里是服务端的兜底（防止绕过前端直接调接口）。
     *
     * 英文句点 `.` 刻意保留：它同时是小数点、域名、版本号的分隔符，
     * 无差别删除会把「3.5」变成「35」、把网址拼坏。
     */
    private String stripPunctuation(String content) {
        if (content == null) {
            return "";
        }
        String cleaned = content.replaceAll("[，。？！,?!]", "");
        return cleaned.trim();
    }

    private String preview(ChatMessage message) {
        String type = message.getType();
        if ("voice".equals(type)) {
            return "[语音]";
        }
        if ("image".equals(type)) {
            return "[图片]";
        }
        if (!"text".equals(type)) {
            return "[" + type + "]";
        }
        String content = message.getContent() == null ? "" : message.getContent();
        return content.length() > 60 ? content.substring(0, 60) : content;
    }

    public String buildSessionId(String a, String b) {
        return "sess_" + min(a, b) + "__" + max(a, b);
    }

    private String min(String a, String b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private String max(String a, String b) {
        return a.compareTo(b) <= 0 ? b : a;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private String now() {
        return LocalTime.now().format(TIME_FMT);
    }

    private long nextSortNo() {
        QueryWrapper<ChatSession> wrapper = new QueryWrapper<>();
        wrapper.select("MIN(sort_no) AS agg");
        List<Map<String, Object>> maps = sessionMapper.selectMaps(wrapper);
        if (maps != null && !maps.isEmpty() && maps.get(0) != null) {
            Object agg = maps.get(0).get("agg");
            if (agg instanceof Number number) {
                return number.longValue() - 1;
            }
        }
        return 0L;
    }
}
