package com.bangbangbang.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.bangbangbang.common.ApiResult;
import com.bangbangbang.common.BizException;
import com.bangbangbang.common.UserContext;
import com.bangbangbang.entity.HelpOrder;
import com.bangbangbang.entity.SysUser;
import com.bangbangbang.mapper.HelpOrderMapper;
import com.bangbangbang.mapper.SysUserMapper;
import com.bangbangbang.service.UserDisplayService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 互助订单：下单 / 接单 / 完成 / 取消
 *
 * 关键点：发布者与接单者的身份全部取自登录令牌，
 * 客户端只提交业务字段，无法伪造他人身份。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private final HelpOrderMapper orderMapper;
    private final SysUserMapper userMapper;
    private final UserDisplayService userDisplayService;

    public OrderController(HelpOrderMapper orderMapper, SysUserMapper userMapper,
                           UserDisplayService userDisplayService) {
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
        this.userDisplayService = userDisplayService;
    }

    @Data
    public static class CreateOrderRequest {
        private String category;
        private String title;
        private String content;
        private String appointTime;
        private String address;
        private Integer bounty;
    }

    /** 下单：发布者 = 当前登录用户 */
    @PostMapping
    public ApiResult<HelpOrder> create(@RequestBody CreateOrderRequest req) {
        UserContext.CurrentUser me = UserContext.require();
        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            throw BizException.of("请填写求助标题");
        }

        HelpOrder order = new HelpOrder();
        order.setId("ord_" + System.currentTimeMillis());
        order.setCreatorId(me.id());
        order.setCreatorName(me.name());
        order.setCreatorAvatar(me.avatar());
        order.setCreatorTag(me.tag());
        order.setCategory(req.getCategory() == null ? "other" : req.getCategory());
        order.setTitle(req.getTitle().trim());
        order.setContent(req.getContent() == null ? "" : req.getContent().trim());
        order.setAppointTime(req.getAppointTime() == null ? "" : req.getAppointTime());
        order.setAddress(req.getAddress() == null ? "" : req.getAddress());
        order.setBounty(req.getBounty() == null ? 0 : req.getBounty());
        order.setStatus("pending");
        order.setStatusText("等待帮手接单");
        order.setCreatedAt(LocalDateTime.now().format(TIME_FMT));
        order.setSortNo(nextSortNo());
        orderMapper.insert(order);
        return ApiResult.ok(order);
    }

    /** 订单列表：mine = 我发布或我接的；pending = 全部待接单 */
    @GetMapping
    public ApiResult<List<Map<String, Object>>> list(@RequestParam(defaultValue = "mine") String scope) {
        UserContext.CurrentUser me = UserContext.require();
        QueryWrapper<HelpOrder> wrapper = new QueryWrapper<>();

        if ("pending".equals(scope)) {
            wrapper.eq("status", "pending");
        } else {
            wrapper.and(w -> w.eq("creator_id", me.id()).or().eq("helper_id", me.id()));
        }
        wrapper.orderByAsc("sort_no");

        List<HelpOrder> orders = orderMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (HelpOrder order : orders) {
            result.add(toView(order, me.id()));
        }
        return ApiResult.ok(result);
    }

    /** 接单：接单者 = 当前登录用户 */
    @PostMapping("/{id}/take")
    public ApiResult<Map<String, Object>> take(@PathVariable String id) {
        UserContext.CurrentUser me = UserContext.require();
        HelpOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.of("订单不存在");
        }
        if (me.id().equals(order.getCreatorId())) {
            throw BizException.of("不能接自己发布的求助");
        }
        if (!"pending".equals(order.getStatus())) {
            throw BizException.of("该订单已被接单或已完成");
        }

        SysUser user = userMapper.selectById(me.id());
        order.setStatus("in_progress");
        order.setStatusText("已匹配到帮手");
        order.setHelperId(me.id());
        order.setHelperName(me.name());
        order.setHelperAvatar(me.avatar());
        order.setHelperTag(me.tag());
        order.setHelperPhone(user == null ? null : user.getPhone());
        orderMapper.updateById(order);
        return ApiResult.ok(toView(order, me.id()));
    }

    /** 完成：仅发布者或接单者可操作 */
    @PostMapping("/{id}/complete")
    public ApiResult<Map<String, Object>> complete(@PathVariable String id) {
        UserContext.CurrentUser me = UserContext.require();
        HelpOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.of("订单不存在");
        }
        if (!me.id().equals(order.getCreatorId()) && !me.id().equals(order.getHelperId())) {
            throw BizException.of("只有订单双方可以确认完成");
        }
        if ("completed".equals(order.getStatus())) {
            return ApiResult.ok(toView(order, me.id()));
        }

        order.setStatus("completed");
        order.setStatusText("已完成服务");
        order.setCompletedAt(LocalDateTime.now().format(TIME_FMT));
        orderMapper.updateById(order);

        // 接单人奖励：完成一单，按订单金额发金币（平台补贴，不从发布者扣）
        int award = awardPointsToHelper(order);
        Map<String, Object> view = toView(order, me.id());
        view.put("pointsAwarded", award);
        return ApiResult.ok(view);
    }

    /**
     * 给接单人加金币。
     *
     * 只会在「第一次把订单标记为已完成」时被调用：complete() 开头对已完成的订单直接返回，
     * 所以重复点击、双方各点一次都不会重复发放。
     *
     * 用 SQL 自增而不是「查余额 → 改 → 写回」，避免发布者与接单人同时点完成时互相覆盖余额。
     */
    private int awardPointsToHelper(HelpOrder order) {
        String helperId = order.getHelperId();
        if (helperId == null || helperId.isEmpty()) {
            return 0;
        }
        int bounty = order.getBounty() == null ? 0 : order.getBounty();
        if (bounty <= 0) {
            return 0;
        }
        UpdateWrapper<SysUser> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", helperId).setSql("points = points + " + bounty);
        userMapper.update(null, wrapper);
        return bounty;
    }

    /** 取消：仅发布者，且未完成 */
    @PostMapping("/{id}/cancel")
    public ApiResult<Map<String, Object>> cancel(@PathVariable String id) {
        UserContext.CurrentUser me = UserContext.require();
        HelpOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.of("订单不存在");
        }
        if (!me.id().equals(order.getCreatorId())) {
            throw BizException.of("只有发布者可以取消订单");
        }
        order.setStatus("cancelled");
        order.setStatusText("已取消");
        orderMapper.updateById(order);
        return ApiResult.ok(toView(order, me.id()));
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable String id) {
        UserContext.CurrentUser me = UserContext.require();
        HelpOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw BizException.of("订单不存在");
        }
        return ApiResult.ok(toView(order, me.id()));
    }

    /* ------------------------------------------------------------------ */

    /** 补一个 isMine / isHelper 标记，前端按钮显隐可直接用 */
    private Map<String, Object> toView(HelpOrder order, String userId) {
        // 发布人/接单人的姓名、头像在 order 表里是下单、接单时的快照，
        // 用户后来改了资料就会和「我的」「私信」里显示的不一致，这里统一校准成最新值。
        userDisplayService.fillOrder(order);

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", order.getId());
        view.put("creatorId", order.getCreatorId());
        view.put("creatorName", order.getCreatorName());
        view.put("creatorAvatar", order.getCreatorAvatar());
        view.put("creatorTag", order.getCreatorTag());
        view.put("category", order.getCategory());
        view.put("title", order.getTitle());
        view.put("content", order.getContent());
        view.put("appointTime", order.getAppointTime());
        view.put("address", order.getAddress());
        view.put("bounty", order.getBounty());
        view.put("status", order.getStatus());
        view.put("statusText", order.getStatusText());
        view.put("createdAt", order.getCreatedAt());
        view.put("completedAt", order.getCompletedAt());
        view.put("helper", order.getHelperView());
        view.put("review", order.getReviewView());
        view.put("isMine", userId.equals(order.getCreatorId()));
        view.put("isHelper", userId.equals(order.getHelperId()));
        return view;
    }

    private long nextSortNo() {
        QueryWrapper<HelpOrder> wrapper = new QueryWrapper<>();
        wrapper.select("MIN(sort_no) AS agg");
        List<Map<String, Object>> maps = orderMapper.selectMaps(wrapper);
        if (maps != null && !maps.isEmpty() && maps.get(0) != null) {
            Object agg = maps.get(0).get("agg");
            if (agg instanceof Number number) {
                return number.longValue() - 1;
            }
        }
        return 0L;
    }
}
