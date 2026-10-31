package com.bangbangbang.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bangbangbang.common.UserContext;
import com.bangbangbang.entity.*;
import com.bangbangbang.mapper.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动引导：一次性返回「当前登录用户可见」的数据。
 *
 * 只包含按用户过滤、体量有上限的表；
 * 动态、评论、聊天消息因为会无限增长，改走各自的分页/增量接口。
 */
@Service
public class BootstrapService {

    /*
     * 这里只放「按登录用户过滤、体量有上限」的表。
     *
     * 三类会随时间无限增长的数据都不在启动快照里：
     *   动态 / 评论 → /api/posts/feed 游标分页、/api/posts/{id}/comments 按需加载
     *   聊天消息   → /api/chat/messages（GET 拉取、POST 单条写入）
     * 因为 /api/bootstrap 是冷启动时阻塞整个 App 的单个请求（前端 15s 超时），
     * 一旦体积随数据量增长，最终会在某天直接打不开，而不是慢慢变卡。
     */

    private final AppSettingMapper settingMapper;
    private final SysUserMapper userMapper;
    private final HelpOrderMapper orderMapper;
    private final ExchangeOrderMapper exchangeOrderMapper;
    private final ContactMapper contactMapper;
    private final AddressMapper addressMapper;
    private final HelperMapper helperMapper;
    private final GoodsMapper goodsMapper;
    private final CartItemMapper cartMapper;
    private final ChatService chatService;
    private final UserDisplayService userDisplayService;

    public BootstrapService(AppSettingMapper settingMapper,
                            SysUserMapper userMapper,
                            HelpOrderMapper orderMapper,
                            ExchangeOrderMapper exchangeOrderMapper,
                            ContactMapper contactMapper,
                            AddressMapper addressMapper,
                            HelperMapper helperMapper,
                            GoodsMapper goodsMapper,
                            CartItemMapper cartMapper,
                            ChatService chatService,
                            UserDisplayService userDisplayService) {
        this.settingMapper = settingMapper;
        this.userMapper = userMapper;
        this.orderMapper = orderMapper;
        this.exchangeOrderMapper = exchangeOrderMapper;
        this.contactMapper = contactMapper;
        this.addressMapper = addressMapper;
        this.helperMapper = helperMapper;
        this.goodsMapper = goodsMapper;
        this.cartMapper = cartMapper;
        this.chatService = chatService;
        this.userDisplayService = userDisplayService;
    }

    public Map<String, Object> load() {
        UserContext.CurrentUser me = UserContext.require();
        String meId = me.id();
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("user", userMapper.selectById(meId));
        data.put("setting", loadSetting());
        data.put("orders", loadOrders(meId));
        data.put("exchangeOrders", loadOwnedExchangeOrders(meId));
        data.put("contacts", loadContacts(meId));
        data.put("sessions", chatService.listSessions(meId));
        data.put("addresses", loadAddresses(meId));
        data.put("helpers", helperMapper.selectList(orderBy("sort_no", "ASC")));
        data.put("goods", goodsMapper.selectList(orderBy("sort_no", "ASC")));
        data.put("cart", loadCart(meId));
        return data;
    }

    /* ------------------------------------------------------------------ */

    private <T> QueryWrapper<T> orderBy(String column, String direction) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        wrapper.last("ORDER BY " + column + " " + direction);
        return wrapper;
    }

    private AppSetting loadSetting() {
        List<AppSetting> list = settingMapper.selectList(null);
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    /** 我发布的 + 我接的 + 全部待接单（接单大厅需要看到别人的求助） */
    private List<HelpOrder> loadOrders(String meId) {
        QueryWrapper<HelpOrder> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w.eq("creator_id", meId)
                .or().eq("helper_id", meId)
                .or().eq("status", "pending"));
        wrapper.orderByAsc("sort_no");
        List<HelpOrder> list = orderMapper.selectList(wrapper);
        if (list == null) {
            return new ArrayList<>();
        }
        // 前端靠这两个标记区分"我是发单人"还是"我是接单人"，
        // 决定订单详情里该展示谁、该联系谁
        for (HelpOrder order : list) {
            order.setIsMine(meId.equals(order.getCreatorId()));
            order.setIsHelper(meId.equals(order.getHelperId()));
        }
        // 订单里的姓名/头像是下单时的快照，统一校准成账号最新资料
        userDisplayService.fillOrders(list);
        return list;
    }

    private List<ExchangeOrder> loadOwnedExchangeOrders(String meId) {
        QueryWrapper<ExchangeOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("owner_id", meId).orderByAsc("sort_no");
        List<ExchangeOrder> list = exchangeOrderMapper.selectList(wrapper);
        return list == null ? new ArrayList<>() : list;
    }

    private List<Contact> loadContacts(String meId) {
        QueryWrapper<Contact> wrapper = new QueryWrapper<>();
        wrapper.eq("owner_id", meId).orderByAsc("sort_no");
        List<Contact> list = contactMapper.selectList(wrapper);
        if (list == null) {
            return new ArrayList<>();
        }
        userDisplayService.fillContacts(list);
        return list;
    }

    private List<Address> loadAddresses(String meId) {
        QueryWrapper<Address> wrapper = new QueryWrapper<>();
        wrapper.eq("owner_id", meId).orderByAsc("sort_no");
        List<Address> list = addressMapper.selectList(wrapper);
        return list == null ? new ArrayList<>() : list;
    }

    /** 购物车行需要回填商品明细；商品已下架的行直接丢弃 */
    private List<CartItem> loadCart(String meId) {
        QueryWrapper<CartItem> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", meId);
        List<CartItem> rawCart = cartMapper.selectList(wrapper);
        List<Goods> rawGoods = goodsMapper.selectList(null);
        if (rawCart == null || rawCart.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Goods> goodsById = new HashMap<>();
        if (rawGoods != null) {
            for (Goods goods : rawGoods) {
                goodsById.put(goods.getId(), goods);
            }
        }

        List<CartItem> result = new ArrayList<>();
        for (CartItem cartItem : rawCart) {
            Goods goods = goodsById.get(cartItem.getGoodsId());
            if (goods == null) {
                continue;
            }
            cartItem.setGoods(goods);
            result.add(cartItem);
        }
        return result;
    }
}
