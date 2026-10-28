<template>
  <view class="mall-page-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航栏 (严格对齐兑换页1.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">兑 换</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 顶部金币总额与提现按钮 (原型蓝黄双卡) -->
    <view class="coins-summary-row">
      <view class="coin-balance-card">
        <text class="coin-balance-text">金币 ： {{ userStore.currentUser.points }}</text>
      </view>
      <view class="withdraw-card" hover-class="btn-hover" @tap="goToWithdraw">
        <text class="withdraw-text">提 现</text>
      </view>
    </view>

    <!-- 灰色分隔条 -->
    <view class="gray-bar"></view>

    <!-- 商品大厅列表 -->
    <scroll-view scroll-y class="goods-scroll-area">
      <view class="section-title-wrap">
        <text class="section-title">全部</text>
      </view>

      <view class="goods-grid-layout">
        <view 
          v-for="item in displayedGoods" 
          :key="item.id" 
          class="goods-card"
        >
          <!-- 红色加购角标 (原型样式) -->
          <view v-if="getCartCount(item.id) > 0" class="red-count-badge">
            <text class="badge-num">{{ getCartCount(item.id) }}</text>
          </view>

          <!-- 商品图片 -->
          <view class="goods-img-box">
            <image :src="netImage(item.image)" mode="aspectFit" class="goods-img" />
          </view>

          <!-- 名称与所需金币 -->
          <view class="goods-name-row">
            <text class="goods-name-text">{{ item.name }} {{ item.price }}</text>
          </view>

          <!-- 兑换加入购物车按钮 -->
          <button class="exchange-add-btn" hover-class="btn-hover" @tap="handleAddToCart(item.id)">
            兑 换
          </button>
        </view>
      </view>

      <!-- 下拉展开更多提示 (原型样式) -->
      <view
        v-if="mallStore.goods.length > INITIAL_GOODS_COUNT"
        class="expand-more-wrap"
        hover-class="btn-hover"
        @tap="toggleExpand"
      >
        <text class="expand-text">{{ isExpanded ? '收起' : '下拉展开更多' }}</text>
        <text class="expand-arrow">{{ isExpanded ? '︽' : '︾' }}</text>
      </view>
    </scroll-view>

    <!-- 底部固定购物车大按钮 -->
    <view class="bottom-cart-bar">
      <button class="cart-main-btn" hover-class="btn-hover" @tap="goToCart">
        购 物 车 {{ mallStore.totalCartCount > 0 ? `(${mallStore.totalCartCount})` : '' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { netImage } from '../../utils/netImage';
import { ref, computed } from 'vue';
import { useMallStore } from '../../stores/mall';
import { useUserStore } from '../../stores/user';
import { useSettingStore } from '../../stores/setting';

const mallStore = useMallStore();
const userStore = useUserStore();
const settingStore = useSettingStore();

const INITIAL_GOODS_COUNT = 4;
const isExpanded = ref(false);

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const displayedGoods = computed(() => {
  return isExpanded.value
    ? mallStore.goods
    : mallStore.goods.slice(0, INITIAL_GOODS_COUNT);
});

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value;
};

const getCartCount = (goodsId: string) => {
  const item = mallStore.cart.find((i) => i.goods.id === goodsId);
  return item ? item.count : 0;
};

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const goToWithdraw = () => {
  uni.navigateTo({
    url: '/pages/square/withdraw'
  });
};

const goToCart = () => {
  uni.navigateTo({
    url: '/pages/square/cart'
  });
};

const handleAddToCart = (goodsId: string) => {
  mallStore.addToCart(goodsId);
};
</script>

<style lang="scss" scoped>
.mall-page-container {
  min-height: 100vh;
  background-color: #F6F7FB;
  padding: 0 32rpx 40rpx 32rpx;
  display: flex;
  flex-direction: column;
}

.status-bar {
  height: 60rpx;
}

.header-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0 28rpx 0;
}

.back-btn {
  padding: 10rpx 0;
}

.back-text {
  font-size: calc(34rpx * var(--font-scale, 1));
  color: #1B65F0;
  font-weight: 700;
}

.page-title {
  font-size: calc(52rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1B65F0;
  letter-spacing: 4rpx;
}

.nav-right-placeholder {
  width: 90rpx;
}

/* 顶部蓝黄双卡 (严格对齐原型尺寸) */
.coins-summary-row {
  display: flex;
  gap: 24rpx;
  margin-bottom: 24rpx;
}

.coin-balance-card {
  flex: 3;
  height: calc(108rpx * var(--font-scale, 1));
  background-color: #1B65F0;
  border-radius: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 20rpx rgba(27, 101, 240, 0.3);

  .coin-balance-text {
    font-size: calc(44rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #FFFFFF;
    letter-spacing: 2rpx;
  }
}

.withdraw-card {
  flex: 2;
  height: calc(108rpx * var(--font-scale, 1));
  background-color: #FFC000;
  border-radius: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 20rpx rgba(255, 192, 0, 0.3);

  .withdraw-text {
    font-size: calc(44rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #FFFFFF;
    letter-spacing: 6rpx;
  }
}

.gray-bar {
  height: 24rpx;
  background-color: #E5E7EB;
  margin: 0 -32rpx 24rpx -32rpx;
}

.goods-scroll-area {
  flex: 1;
}

.section-title-wrap {
  margin-bottom: 24rpx;

  .section-title {
    font-size: calc(36rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
  }
}

/* 2x2 网格 */
.goods-grid-layout {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 28rpx;
  margin-bottom: 30rpx;
}

.goods-card {
  position: relative;
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
}

.red-count-badge {
  position: absolute;
  top: 16rpx;
  right: 16rpx;
  width: 48rpx;
  height: 48rpx;
  background-color: #D9001B;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;

  .badge-num {
    color: #FFFFFF;
    font-size: 26rpx;
    font-weight: 900;
  }
}

.goods-img-box {
  width: 180rpx;
  height: 180rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16rpx;

  .goods-img {
    width: 100%;
    height: 100%;
    border-radius: 16rpx;
  }
}

.goods-name-row {
  width: 100%;
  border-bottom: 2rpx solid #E5E7EB;
  padding-bottom: 14rpx;
  margin-bottom: 16rpx;
  text-align: center;

  .goods-name-text {
    font-size: calc(32rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
  }
}

.exchange-add-btn {
  width: 100%;
  height: calc(76rpx * var(--font-scale, 1));
  line-height: calc(76rpx * var(--font-scale, 1));
  background-color: #EBF2FF;
  color: #1B65F0;
  font-size: calc(34rpx * var(--font-scale, 1));
  font-weight: 800;
  border-radius: 38rpx;
  border: none;

  &::after {
    border: none;
  }
}

.expand-more-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 0 40rpx 0;

  .expand-text {
    font-size: calc(30rpx * var(--font-scale, 1));
    color: #1F2329;
    font-weight: 700;
    margin-bottom: 6rpx;
  }

  .expand-arrow {
    font-size: 32rpx;
    color: #1F2329;
    font-weight: 900;
  }
}

/* 底部购物车大按钮 */
.bottom-cart-bar {
  padding-top: 10rpx;
  margin-bottom: 20rpx;
}

.cart-main-btn {
  width: 100%;
  height: calc(108rpx * var(--font-scale, 1));
  line-height: calc(108rpx * var(--font-scale, 1));
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(44rpx * var(--font-scale, 1));
  font-weight: 900;
  border-radius: 36rpx;
  letter-spacing: 8rpx;
  box-shadow: 0 10rpx 28rpx rgba(27, 101, 240, 0.35);
  border: none;

  &::after {
    border: none;
  }
}

.btn-hover {
  opacity: 0.88;
}
</style>
