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

    public AuthService(SysUserMapper userMapper,
                       PasswordService passwordService,
                       TokenService tokenService) {
        this.userMapper = userMapper;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
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
