package com.bangbangbang.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bangbangbang.common.BizException;
import com.bangbangbang.entity.SysUser;
import com.bangbangbang.mapper.SysUserMapper;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 账号服务：注册 / 登录 / 资料更新
 */
@Service
public class AuthService {

    /** 新用户默认头像（服务器上的地址，不是小程序包内静态资源） */
    public static final String DEFAULT_AVATAR = "/uploads/default/avatar.jpg";

    private final SysUserMapper userMapper;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final WxService wxService;

    public AuthService(SysUserMapper userMapper,
                       PasswordService passwordService,
                       TokenService tokenService,
                       WxService wxService) {
        this.userMapper = userMapper;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
        this.wxService = wxService;
    }

    @Data
    public static class RegisterRequest {
        private String name;
        private Integer age;
        private String phone;
        private String password;
        private String gender;
        private String role;
    }

    @Data
    public static class LoginRequest {
        private String phone;
        private String password;
    }

    /** 微信登录：code 由小程序端 uni.login 获取 */
    @Data
    public static class WxLoginRequest {
        private String code;
    }

    /** 微信登录后绑定手机号（首次使用即注册，老账号需验密码） */
    @Data
    public static class WxBindRequest {
        private String ticket;
        private String phone;
        private String password;
        private String name;
        private Integer age;
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest req) {
        String phone = trim(req.getPhone());
        String name = trim(req.getName());
        String password = req.getPassword();

        if (name.isEmpty()) {
            throw BizException.of("请填写姓名");
        }
        if (!phone.matches("\\d{11}")) {
            throw BizException.of("请填写 11 位手机号");
        }
        if (password == null || password.length() < 4) {
            throw BizException.of("密码至少 4 位");
        }
        // 年龄做范围校验：Integer 存不下 11 位手机号（有用户会把手机号误填进年龄框），
        // 不校验的话 Jackson 反序列化直接抛 out of range of int，前端只会看到「服务异常」
        if (req.getAge() != null && (req.getAge() < 1 || req.getAge() > 150)) {
            throw BizException.of("请输入正确的年龄（1-150）");
        }

        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("phone", phone);
        List<SysUser> exists = userMapper.selectList(wrapper);
        if (exists != null && !exists.isEmpty()) {
            throw BizException.of("该手机号已注册，请直接登录");
        }

        SysUser user = new SysUser();
        user.setId("u_" + System.currentTimeMillis());
        user.setName(name);
        user.setPhone(phone);
        user.setAge(req.getAge() == null ? 60 : req.getAge());
        user.setGender(req.getGender() == null || req.getGender().isEmpty() ? "男" : req.getGender());
        user.setRole(req.getRole() == null || req.getRole().isEmpty() ? "elder" : req.getRole());
        user.setAvatar(DEFAULT_AVATAR);
        user.setPoints(0);
        user.setIsRealName(false);
        user.setIsLoggedIn(true);
        user.setPasswordHash(passwordService.hash(password));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);

        return buildAuthResult(user);
    }

    public Map<String, Object> login(LoginRequest req) {
        String phone = trim(req.getPhone());
        if (phone.isEmpty() || req.getPassword() == null) {
            throw BizException.of("请输入手机号和密码");
        }

        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("phone", phone);
        List<SysUser> list = userMapper.selectList(wrapper);
        SysUser user = (list == null || list.isEmpty()) ? null : list.get(0);
        if (user == null) {
            throw BizException.of("手机号未注册，请先注册");
        }
        if (!passwordService.matches(req.getPassword(), user.getPasswordHash())) {
            throw BizException.of("手机号或密码不正确");
        }

        user.setIsLoggedIn(true);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        return buildAuthResult(user);
    }

    /**
     * 微信登录：code → openid。
     * 已绑定过微信的账号直接返回令牌；否则返回 needBind=true + 一次性票据，
     * 由前端引导到绑定手机号页，绑完再签发令牌。
     */
    public Map<String, Object> wxLogin(WxLoginRequest req) {
        String code = trim(req.getCode());
        if (code.isEmpty()) {
            throw BizException.of("微信授权失败，请重新点击微信登录");
        }
        String openid = wxService.code2Openid(code);

        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("openid", openid);
        List<SysUser> list = userMapper.selectList(wrapper);
        if (list != null && !list.isEmpty()) {
            SysUser user = list.get(0);
            user.setIsLoggedIn(true);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);

            Map<String, Object> result = buildAuthResult(user);
            result.put("needBind", false);
            return result;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("needBind", true);
        result.put("ticket", wxService.issueTicket(openid));
        return result;
    }

    /**
     * 微信登录后绑定手机号。
     *
     * 手机号未注册 → 直接注册并绑定（姓名必填，密码作为该账号的登录凭证）
     * 手机号已注册 → 必须校验密码，否则任何人输入他人手机号就能把别人的账号绑到自己微信上
     */
    @Transactional
    public Map<String, Object> wxBind(WxBindRequest req) {
        String openid = wxService.resolveTicket(req.getTicket());
        wxService.invalidateTicket(req.getTicket());

        String phone = trim(req.getPhone());
        String name = trim(req.getName());
        String password = req.getPassword();

        if (!phone.matches("\\d{11}")) {
            throw BizException.of("请填写 11 位手机号");
        }
        if (password == null || password.length() < 4) {
            throw BizException.of("密码至少 4 位");
        }
        if (req.getAge() != null && (req.getAge() < 1 || req.getAge() > 150)) {
            throw BizException.of("请输入正确的年龄（1-150）");
        }

        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("phone", phone);
        List<SysUser> list = userMapper.selectList(wrapper);
        SysUser user = (list == null || list.isEmpty()) ? null : list.get(0);

        if (user == null) {
            if (name.isEmpty()) {
                throw BizException.of("首次使用微信登录，请填写姓名");
            }
            SysUser created = new SysUser();
            created.setId("u_" + System.currentTimeMillis());
            created.setName(name);
            created.setPhone(phone);
            created.setAge(req.getAge() == null ? 60 : req.getAge());
            created.setGender("男");
            created.setRole("elder");
            created.setAvatar(DEFAULT_AVATAR);
            created.setPoints(0);
            created.setIsRealName(false);
            created.setIsLoggedIn(true);
            created.setOpenid(openid);
            created.setPasswordHash(passwordService.hash(password));
            created.setCreatedAt(LocalDateTime.now());
            created.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(created);
            return buildAuthResult(created);
        }

        if (!passwordService.matches(password, user.getPasswordHash())) {
            throw BizException.of("手机号或密码不正确");
        }
        String bound = user.getOpenid();
        if (bound != null && !bound.isEmpty() && !bound.equals(openid)) {
            throw BizException.of("该手机号已绑定其它微信，请使用手机号密码登录");
        }

        user.setOpenid(openid);
        user.setIsLoggedIn(true);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return buildAuthResult(user);
    }

    public void logout(String token) {
        tokenService.revoke(token);
    }

    public SysUser getById(String userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw BizException.of("用户不存在");
        }
        return user;
    }

    private Map<String, Object> buildAuthResult(SysUser user) {
        String token = tokenService.issue(user.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
