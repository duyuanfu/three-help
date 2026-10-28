import { defineStore } from 'pinia';
import type { HelpOrder, ExchangeOrder, OrderCategory } from '../types';
import { TABLES } from '../utils/db';
import {
  apiCreateOrder,
  apiTakeOrder,
  apiCompleteOrder,
  apiCancelOrder,
  apiListOrders
} from '../api/business';

export const useOrderStore = defineStore('order', {
  state: () => ({
    orders: [] as HelpOrder[],
    exchangeOrders: [] as ExchangeOrder[],
    searchKeyword: '',
    activeTab: 'in_progress' as 'in_progress' | 'completed'
  }),
  getters: {
    // 进行中订单：我发布的待接单 + 我已接的进行中订单（别人的待接单去「接单大厅」）
    inProgressOrders: (state) => {
      const list = state.orders.filter(
        (o) =>
          o.status === 'in_progress' ||
          o.status === 'accepted' ||
          (o.status === 'pending' && (o.isMine || o.isHelper))
      );
      if (!state.searchKeyword.trim()) return list;
      const kw = state.searchKeyword.trim().toLowerCase();
      return list.filter(
        (o) =>
          o.title.toLowerCase().includes(kw) ||
          o.creatorName.toLowerCase().includes(kw) ||
          (o.helper && o.helper.name.toLowerCase().includes(kw))
      );
    },
    // 已完成订单
    completedOrders: (state) => {
      const list = state.orders.filter((o) => o.status === 'completed');
      if (!state.searchKeyword.trim()) return list;
      const kw = state.searchKeyword.trim().toLowerCase();
      return list.filter(
        (o) =>
          o.title.toLowerCase().includes(kw) ||
          o.creatorName.toLowerCase().includes(kw) ||
          (o.helper && o.helper.name.toLowerCase().includes(kw))
      );
    },
    // 可接订单（别人发布的待接单）
    pendingOrders: (state) => {
      return state.orders.filter((o) => o.status === 'pending' && !o.isMine);
    },
    // 当前首页展示的最新求助卡片
    latestMyRequest: (state) => {
      return state.orders.find(
        (o) => o.isMine && (o.status === 'in_progress' || o.status === 'pending')
      ) || null;
    }
  },
  actions: {
    /** 启动时从后端快照读取（bootstrap 已按当前用户过滤） */
    initOrders() {
      try {
        this.orders = TABLES.orders.all();
        this.exchangeOrders = TABLES.exchangeOrders.all();
      } catch (e) {
        console.error('[order] 初始化失败', e);
      }
    },

    /** 重新从服务端拉取订单列表（我的 + 待接单大厅） */
    async reloadOrders() {
      const [mine, pending] = await Promise.all([
        apiListOrders('mine'),
        apiListOrders('pending')
      ]);
      const merged = new Map<string, HelpOrder>();
      mine.forEach((o) => merged.set(o.id, o));
      pending.forEach((o) => {
        if (!merged.has(o.id)) merged.set(o.id, o);
      });
      this.orders = Array.from(merged.values());
    },

    /** 下单：发布者由服务端按令牌确定 */
    async createOrder(params: {
      category: OrderCategory;
      title: string;
      content: string;
      appointTime: string;
      address: string;
      bounty: number;
    }) {
      const order = await apiCreateOrder({
        category: params.category,
        title: params.title,
        content: params.content,
        appointTime: params.appointTime,
        address: params.address,
        bounty: params.bounty
      });
      this.orders.unshift(order);
      return order;
    },

    /** 接单：接单者由服务端按令牌确定 */
    async takeOrder(orderId: string) {
      const order = await apiTakeOrder(orderId);
      this.replaceLocal(order);
      return order;
    },

    /** 完成订单：仅订单双方可操作 */
    async completeOrder(orderId: string) {
      const order = await apiCompleteOrder(orderId);
      this.replaceLocal(order);
      return order;
    },

    /** 取消订单：仅发布者可操作 */
    async cancelOrder(orderId: string) {
      const order = await apiCancelOrder(orderId);
      this.replaceLocal(order);
      return order;
    },

    replaceLocal(order: HelpOrder) {
      const idx = this.orders.findIndex((o) => o.id === order.id);
      if (idx === -1) {
        this.orders.unshift(order);
      } else {
        this.orders[idx] = { ...this.orders[idx], ...order };
      }
    },

    /** 兑换订单（走通用表接口，归属当前用户） */
    addExchangeOrder(order: ExchangeOrder) {
      this.exchangeOrders.unshift(order);
      TABLES.exchangeOrders.insert(order, 'head');
    }
  }
});
