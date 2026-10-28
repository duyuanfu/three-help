package com.bangbangbang.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bangbangbang.entity.UserToken;
import com.bangbangbang.mapper.UserTokenMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 登录令牌管理：签发 / 校验 / 失效
 */
@Service
public class TokenService {

    /** 令牌有效期：30 天（适老场景下不希望频繁重新登录） */
    private static final int EXPIRE_DAYS = 30;

    private final UserTokenMapper tokenMapper;

    public TokenService(UserTokenMapper tokenMapper) {
        this.tokenMapper = tokenMapper;
    }

    public String issue(String userId) {
        UserToken token = new UserToken();
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setUserId(userId);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusDays(EXPIRE_DAYS));
        tokenMapper.insert(token);
        return token.getToken();
    }

    /** 校验令牌并返回 userId；无效或过期返回 null */
    public String resolveUserId(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        UserToken stored = tokenMapper.selectById(token);
        if (stored == null) {
            return null;
        }
        if (stored.getExpiresAt() != null && stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenMapper.deleteById(token);
            return null;
        }
        return stored.getUserId();
    }

    public void revoke(String token) {
        if (token != null && !token.isEmpty()) {
            tokenMapper.deleteById(token);
        }
    }

    public void revokeAll(String userId) {
        QueryWrapper<UserToken> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        tokenMapper.delete(wrapper);
    }
}
