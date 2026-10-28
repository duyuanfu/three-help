package com.bangbangbang.controller;

import com.bangbangbang.common.ApiResult;
import com.bangbangbang.common.BizException;
import com.bangbangbang.common.UserContext;
import com.bangbangbang.service.TableService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通用表接口：与前端 src/utils/db.ts 的 TABLES 语义一一对应
 *
 * 仅用于「结构简单、无复杂业务规则」的表：
 *   读：全部 13 张表都可以读（私有表按当前用户过滤）
 *   写：user / setting / addresses / exchangeOrders / cart
 *
 * 订单、消息、动态、评论等表必须走各自的业务接口，
 * 这样身份字段由服务端填充，客户端无法伪造。
 */
@RestController
@RequestMapping("/api/tables")
public class TableController {

    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    /** 列出全部可用表名，便于排查 */
    @GetMapping
    public ApiResult<List<String>> tables() {
        return ApiResult.ok(tableService.tableNames());
    }

    /** 读取整表（单行表返回对象，其余返回数组） */
    @GetMapping("/{table}")
    public ApiResult<Object> all(@PathVariable String table) {
        return ApiResult.ok(tableService.all(table, UserContext.require().id()));
    }

    /** 新增一行（单行表自动降级为 upsert） */
    @PostMapping("/{table}")
    public ApiResult<Object> insert(@PathVariable String table, @RequestBody Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            throw BizException.of("请求体不能为空");
        }
        return ApiResult.ok(tableService.insert(table, body, UserContext.require().id()));
    }

    /** 整表覆盖（对应前端 replaceAll）；兼容单行表传对象的情况 */
    @PutMapping("/{table}")
    @SuppressWarnings("unchecked")
    public ApiResult<List<Object>> replaceAll(@PathVariable String table,
                                              @RequestBody Object body) {
        List<Map<String, Object>> rows;
        if (body instanceof List) {
            rows = (List<Map<String, Object>>) body;
        } else if (body instanceof Map) {
            rows = List.of((Map<String, Object>) body);
        } else {
            rows = List.of();
        }
        return ApiResult.ok(tableService.replaceAll(table, rows, UserContext.require().id()));
    }

    /** 局部更新；同时接受 POST 与 PATCH（小程序端 wx.request 不支持 PATCH） */
    @RequestMapping(value = "/{table}/{id}", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ApiResult<Object> patch(@PathVariable String table,
                                   @PathVariable String id,
                                   @RequestBody Map<String, Object> patch) {
        return ApiResult.ok(tableService.patch(table, id, patch, UserContext.require().id()));
    }

    /** 删除一行 */
    @DeleteMapping("/{table}/{id}")
    public ApiResult<Void> delete(@PathVariable String table, @PathVariable String id) {
        tableService.delete(table, id, UserContext.require().id());
        return ApiResult.ok();
    }

    /** 清空整表（只清当前用户自己的数据） */
    @DeleteMapping("/{table}")
    public ApiResult<Void> clear(@PathVariable String table) {
        tableService.clear(table, UserContext.require().id());
        return ApiResult.ok();
    }
}
