import { defineStore } from 'pinia';
import type { MallGoods, CartItem, ExchangeOrder } from '../types';

import { TABLES } from '../utils/db';
import { useUserStore } from './user';
import { useOrderStore } from './order';

// 上一版临时增加的 4 个占位商品，现已由 2 个文创商品替代
const OBSOLETE_GOODS_IDS = ['g_egg_two', 'g_bag_two', 'g_paste_two', 'g_tissue_two'];

export const useMallStore = defineStore('mall', {
  state: () => ({
    goods: [] as MallGoods[],
    cart: [] as CartItem[]
  }),
  getters: {
    totalCartCount: (state) => {
      return state.cart.reduce((sum, item) => sum + item.count, 0);
    },
    totalCartPoints: (state) => {
      return state.cart.reduce((sum, item) => sum + item.goods.price * item.count, 0);
    }
  },
  actions: {
    initMall() {
      try {
        // 商品与购物车都以 MySQL 为准（商品是全局数据，购物车按当前用户过滤）
        const list = TABLES.goods.all();
        this.goods = list.filter((item) => !OBSOLETE_GOODS_IDS.includes(item.id));

        // 购物车里引用了已下架商品的行直接丢弃（只在内存中过滤，不主动写库）
        const validGoodsIds = new Set(this.goods.map((item) => item.id));
        this.cart = TABLES.cart.all().filter(
          (item) => item?.goods && validGoodsIds.has(item.goods.id)
        );
      } catch (e) {
        console.error('Failed to init mall', e);
      }
    },
    saveCart() {
      TABLES.cart.replaceAll(this.cart);
    },
    addToCart(goodsId: string) {
      const g = this.goods.find((item) => item.id === goodsId);
      if (!g) return;

      const existing = this.cart.find((item) => item.goods.id === goodsId);
      if (existing) {
        existing.count += 1;
      } else {
        this.cart.push({
          goods: g,
          count: 1
        });
      }
      this.saveCart();
      uni.showToast({
        title: '已加入购物车',
        icon: 'success'
      });
    },
    updateCartCount(goodsId: string, count: number) {
      if (count <= 0) {
        this.cart = this.cart.filter((item) => item.goods.id !== goodsId);
      } else {
        const item = this.cart.find((i) => i.goods.id === goodsId);
        if (item) item.count = count;
      }
      this.saveCart();
    },
    clearCart() {
      this.cart = [];
      TABLES.cart.replaceAll([]);
    },
    // 确认兑换下单
    confirmExchange(deliveryAddress?: string) {
      const userStore = useUserStore();
      const orderStore = useOrderStore();
      const totalCost = this.totalCartPoints;

      if (userStore.currentUser.points < totalCost) {
        uni.showToast({
          title: '金币余额不足',
          icon: 'none'
        });
        return false;
      }

      // 扣减金币
      userStore.deductPoints(totalCost);

      const firstItem = this.cart[0];
      const goodsSummary = this.cart.length === 1
        ? `${firstItem.goods.name} × ${firstItem.count}`
        : `${firstItem.goods.name}等${this.totalCartCount}件物资`;

      const now = new Date();
      const pad = (n: number) => String(n).padStart(2, '0');
      const timeStr = `${now.getFullYear()}/${pad(now.getMonth() + 1)}/${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`;

      const newExchangeOrder: ExchangeOrder = {
        id: 'ex_' + Date.now(),
        orderNo: '7512' + Math.floor(1000000000 + Math.random() * 9000000000),
        goodsName: `兑换${goodsSummary}`,
        goodsImage: firstItem ? firstItem.goods.image : '/static/goods/egg.jpg',
        quantity: this.totalCartCount,
        pointsCost: totalCost,
        address: deliveryAddress || userStore.currentUser.address,
        expressCompany: '中通快递',
        senderCity: '泉州市 帮帮超市',
        receiverCity: `泉州市 ${userStore.currentUser.name}`,
        createdAt: timeStr,
        steps: [
          {
            time: timeStr,
            title: '已发货',
            subtitle: '泉州分拨中心已揽收打包',
            isCurrent: false
          },
          {
            time: '预计次日 08:00',
            title: '转站中',
            subtitle: '华大接驳点',
            isCurrent: false
          },
          {
            time: '预计次日 12:00',
            title: '已到达',
            subtitle: '丰泽区服务站',
            code: '取件码 70-2-00' + Math.floor(10 + Math.random() * 90),
            isCurrent: true
          }
        ]
      };

      orderStore.addExchangeOrder(newExchangeOrder);
      this.clearCart();
      return newExchangeOrder;
    }
  }
});
