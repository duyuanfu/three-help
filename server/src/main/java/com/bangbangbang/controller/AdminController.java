package com.bangbangbang.controller;

import com.bangbangbang.common.ApiResult;
import com.bangbangbang.common.BizException;
import com.bangbangbang.common.UserContext;
import com.bangbangbang.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理后台接口
 *
 * 全部路由都要求登录用户具备管理员身份（sys_user.is_admin = 1），
 * 否则直接拒绝。前端「我的」页据此决定是否显示管理后台入口。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** 可管理的表清单与各自记录数 */
    @GetMapping("/tables")
    public ApiResult<List<Map<String, Object>>> tables() {
        requireAdmin();
        return ApiResult.ok(adminService.listTables());
    }

    /** 分页查询某张表，keyword 会在所有文本列上做模糊匹配 */
    @GetMapping("/tables/{key}")
    public ApiResult<Map<String, Object>> page(@PathVariable String key,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(required = false) String keyword) {
        requireAdmin();
        return ApiResult.ok(adminService.page(key, page, size, keyword));
    }

    /** 更新一条记录的若干字段 */
    @PutMapping("/tables/{key}/{id}")
    public ApiResult<Map<String, Object>> update(@PathVariable String key,
                                                 @PathVariable String id,
                                                 @RequestBody Map<String, Object> body) {
        requireAdmin();
        return ApiResult.ok(adminService.update(key, id, body));
    }

    /** 删除一条记录 */
    @DeleteMapping("/tables/{key}/{id}")
    public ApiResult<Void> delete(@PathVariable String key, @PathVariable String id) {
        requireAdmin();
        adminService.delete(key, id);
        return ApiResult.ok();
    }

    /** 当前账号是否为管理员（前端也可直接用 /auth/me 里的 isAdmin 字段） */
    @GetMapping("/whoami")
    public ApiResult<Map<String, Object>> whoami() {
        UserContext.CurrentUser me = UserContext.require();
        return ApiResult.ok(Map.of(
                "id", me.id(),
                "name", me.name(),
                "isAdmin", me.admin()
        ));
    }

    private void requireAdmin() {
        UserContext.CurrentUser me = UserContext.require();
        if (!me.admin()) {
            throw BizException.of("需要管理员权限");
        }
    }
}
