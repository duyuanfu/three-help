import { defineStore } from 'pinia';
import type { MallGoods, CartItem, ExchangeOrder } from '../types';
import { initialGoods } from '../mock/seeds';
import { useUserStore } from './user';
import { useOrderStore } from './order';

const GOODS_KEY = 'bbb_goods';
const CART_KEY = 'bbb_cart';
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
        const cachedGoods = uni.getStorageSync(GOODS_KEY);
        const cachedList: MallGoods[] = Array.isArray(cachedGoods)
          ? cachedGoods.filter((item) => !OBSOLETE_GOODS_IDS.includes(item.id))
          : [];
        const cachedIds = new Set(cachedList.map((item) => item.id));

        // 合并新增的 mock 商品，并清理旧版占位商品
        this.goods = [
          ...cachedList,
          ...initialGoods.filter((item) => !cachedIds.has(item.id))
        ];
        uni.setStorageSync(GOODS_KEY, this.goods);

        const cachedCart = uni.getStorageSync(CART_KEY);
        const cartList: CartItem[] = Array.isArray(cachedCart) ? cachedCart : [];
        const validGoodsIds = new Set(this.goods.map((item) => item.id));
        this.cart = cartList.filter((item) => item?.goods && validGoodsIds.has(item.goods.id));
        if (this.cart.length !== cartList.length) {
          uni.setStorageSync(CART_KEY, this.cart);
        }
      } catch (e) {
        console.error('Failed to init mall', e);
      }
    },
    saveCart() {
      try {
        uni.setStorageSync(CART_KEY, this.cart);
      } catch (e) {
        console.error('Failed to save cart', e);
      }
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
      this.saveCart();
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
