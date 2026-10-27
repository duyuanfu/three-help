import { defineStore } from 'pinia';
import type { HelpOrder, ExchangeOrder, OrderCategory } from '../types';
import { initialOrders, initialExchangeOrders } from '../mock/seeds';

const ORDERS_KEY = 'bbb_orders';
const EXCHANGE_ORDERS_KEY = 'bbb_exchange_orders';

export const useOrderStore = defineStore('order', {
  state: () => ({
    orders: [] as HelpOrder[],
    exchangeOrders: [] as ExchangeOrder[],
    searchKeyword: '',
    activeTab: 'in_progress' as 'in_progress' | 'completed'
  }),
  getters: {
    // 进行中订单：包括老人发起的待接单、已接单进行中的订单
    inProgressOrders: (state) => {
      const list = state.orders.filter(
        (o) => o.status === 'in_progress' || o.status === 'accepted' || o.status === 'pending'
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
    // 可接订单（待接单列表）
    pendingOrders: (state) => {
      return state.orders.filter((o) => o.status === 'pending');
    },
    // 当前首页展示的最新求助卡片
    latestMyRequest: (state) => {
      // 优先取进行中，其次取待接单
      return state.orders.find((o) => o.status === 'in_progress' || o.status === 'pending') || null;
    }
  },
  actions: {
    initOrders() {
      try {
        const cachedOrders = uni.getStorageSync(ORDERS_KEY);
        if (Array.isArray(cachedOrders) && cachedOrders.length > 0) {
          this.orders = cachedOrders;
        } else {
          this.orders = [...initialOrders];
          this.saveOrders();
        }

        const cachedEx = uni.getStorageSync(EXCHANGE_ORDERS_KEY);
        if (Array.isArray(cachedEx) && cachedEx.length > 0) {
          this.exchangeOrders = cachedEx;
        } else {
          this.exchangeOrders = [...initialExchangeOrders];
          this.saveExchangeOrders();
        }
      } catch (e) {
        console.error('Failed to init orders', e);
      }
    },
    saveOrders() {
      try {
        uni.setStorageSync(ORDERS_KEY, this.orders);
      } catch (e) {
        console.error('Failed to save orders', e);
      }
    },
    saveExchangeOrders() {
      try {
        uni.setStorageSync(EXCHANGE_ORDERS_KEY, this.exchangeOrders);
      } catch (e) {
        console.error('Failed to save exchange orders', e);
      }
    },
    // 下单
    createOrder(params: {
      category: OrderCategory;
      title: string;
      content: string;
      appointTime: string;
      address: string;
      bounty: number;
      creatorName?: string;
      creatorAvatar?: string;
    }) {
      const newOrder: HelpOrder = {
        id: 'ord_' + Date.now(),
        creatorId: 'u_zhangdongmei',
        creatorName: params.creatorName || '张冬梅',
        creatorAvatar: params.creatorAvatar || '/static/avatars/zhangdongmei.png',
        category: params.category,
        title: params.title,
        content: params.content,
        appointTime: params.appointTime,
        address: params.address,
        bounty: params.bounty,
        status: 'pending',
        statusText: '等待帮手接单',
        createdAt: new Date().toLocaleString()
      };
      this.orders.unshift(newOrder);
      this.saveOrders();
      return newOrder;
    },
    // 接单
    takeOrder(orderId: string, helperInfo?: { id: string; name: string; tag: string; avatar: string; phone: string }) {
      const idx = this.orders.findIndex((o) => o.id === orderId);
      if (idx !== -1) {
        this.orders[idx].status = 'in_progress';
        this.orders[idx].statusText = '已匹配到帮手';
        this.orders[idx].helper = helperInfo || {
          id: 'u_wuxiaoying',
          name: '吴小应',
          tag: '@学生',
          avatar: '/static/avatars/malingling.png',
          phone: '13912345678'
        };
        this.saveOrders();
        return this.orders[idx];
      }
      return null;
    },
    // 取消订单
    cancelOrder(orderId: string) {
      const idx = this.orders.findIndex((o) => o.id === orderId);
      if (idx !== -1) {
        this.orders[idx].status = 'cancelled';
        this.orders[idx].statusText = '已取消';
        this.saveOrders();
      }
    },
    // 完成订单
    completeOrder(orderId: string) {
      const idx = this.orders.findIndex((o) => o.id === orderId);
      if (idx !== -1) {
        this.orders[idx].status = 'completed';
        this.orders[idx].statusText = '已完成服务';
        this.orders[idx].completedAt = new Date().toLocaleString();
        this.saveOrders();
      }
    },
    // 新增兑换订单
    addExchangeOrder(order: ExchangeOrder) {
      this.exchangeOrders.unshift(order);
      this.saveExchangeOrders();
    }
  }
});
