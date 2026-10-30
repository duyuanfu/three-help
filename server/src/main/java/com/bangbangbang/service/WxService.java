package com.bangbangbang.service;

import com.bangbangbang.common.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微信小程序登录。
 *
 * 流程（个人主体可用，不依赖「手机号快捷登录」）：
 *   1. 小程序端 uni.login 拿到 code
 *   2. 服务端用 code 调 jscode2session 换 openid（AppSecret 只在服务端，绝不下发）
 *   3. openid 已绑定账号 → 直接签发令牌
 *      未绑定 → 返回一次性票据，前端引导绑定手机号，绑定成功后再签发令牌
 *
 * 为什么不直接把 openid 返回给前端：openid 一旦可被前端任意填写，
 * 别人就能把自己的微信绑到别人的账号上。票据是随机串 + 5 分钟有效期 + 一次性使用。
 */
@Slf4j
@Service
public class WxService {

    private static final String JSCODE_URL = "https://api.weixin.qq.com/sns/jscode2session";

    /** 绑定票据有效期：5 分钟（够用户填手机号，又不会长期可重放） */
    private static final long TICKET_TTL_SECONDS = 300;

    private final String appid;
    private final String secret;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** ticket -> openid + 过期时间。单机内存足够：重启后用户重新点一次微信登录即可 */
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    private record Ticket(String openid, Instant expireAt) {}

    public WxService(@Value("${app.wx.appid:}") String appid,
                     @Value("${app.wx.secret:}") String secret,
                     ObjectMapper objectMapper) {
        this.appid = appid;
        this.secret = secret;
        this.objectMapper = objectMapper;
    }

    /** 是否配置了 AppID / AppSecret */
    public boolean isConfigured() {
        return appid != null && !appid.isBlank() && secret != null && !secret.isBlank();
    }

    /** code 换 openid */
    public String code2Openid(String code) {
        if (!isConfigured()) {
            throw BizException.of("服务端尚未配置微信登录（WX_APPID / WX_SECRET）");
        }

        String url = JSCODE_URL
                + "?appid=" + appid
                + "&secret=" + secret
                + "&js_code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            String openid = root.path("openid").asText("");
            if (!openid.isEmpty()) {
                return openid;
            }

            int errcode = root.path("errcode").asInt(-1);
            String errmsg = root.path("errmsg").asText("");
            // code 是敏感凭证，日志里只记错误码，不打印 code / secret
            log.warn("[微信登录] jscode2session 失败 errcode={} errmsg={}", errcode, errmsg);
            throw BizException.of(describeError(errcode));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[微信登录] 调用微信接口异常：{}", e.getMessage());
            throw BizException.of("微信登录服务暂时不可用，请稍后再试");
        }
    }

    /** 为未绑定账号的 openid 签发一次性绑定票据 */
    public String issueTicket(String openid) {
        cleanExpired();
        String ticket = UUID.randomUUID().toString().replace("-", "");
        tickets.put(ticket, new Ticket(openid, Instant.now().plusSeconds(TICKET_TTL_SECONDS)));
        return ticket;
    }

    /** 校验票据并取出 openid（不销毁，校验失败不必让用户重新授权） */
    public String resolveTicket(String ticket) {
        Ticket stored = (ticket == null) ? null : tickets.get(ticket);
        if (stored == null || stored.expireAt().isBefore(Instant.now())) {
            tickets.remove(ticket == null ? "" : ticket);
            throw BizException.of("微信授权已过期，请重新点击微信登录");
        }
        return stored.openid();
    }

    /** 绑定成功后销毁票据，防止重放 */
    public void invalidateTicket(String ticket) {
        if (ticket != null) {
            tickets.remove(ticket);
        }
    }

    private void cleanExpired() {
        Instant now = Instant.now();
        tickets.entrySet().removeIf(entry -> entry.getValue().expireAt().isBefore(now));
    }

    private String describeError(int errcode) {
        return switch (errcode) {
            case 40029 -> "微信授权码无效，请重新点击微信登录";
            case 40163 -> "微信授权码已被使用，请重新点击微信登录";
            case 45011 -> "操作过于频繁，请稍后再试";
            case 40013 -> "服务端 AppID 配置有误，请联系管理员";
            case 40125 -> "服务端 AppSecret 配置有误，请联系管理员";
            default -> "微信登录失败（错误码 " + errcode + "），请稍后再试";
        };
    }
}
