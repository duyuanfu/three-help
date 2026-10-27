<template>
  <view class="cart-page-container" :style="pageStyle">
    <!-- 顶部蓝色导航栏 (严格对齐兑换页2.png) -->
    <view class="blue-header-nav">
      <view class="nav-left" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">购物车</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 收货地址条 (原型样式) -->
    <view class="delivery-address-bar">
      <view class="loc-icon-wrap">
        <image src="/static/common/location.png" mode="aspectFit" class="loc-icon-img" />
      </view>
      <text class="address-text">{{ userStore.currentUser.address }}</text>
      <button class="edit-addr-btn" hover-class="btn-hover" @tap="editAddress">
        修改
      </button>
    </view>

    <!-- 灰色分隔条 -->
    <view class="gray-bar"></view>

    <!-- 购物车商品列表 -->
    <scroll-view scroll-y class="cart-scroll-area">
      <view v-if="cartItems.length > 0">
        <view 
          v-for="item in cartItems" 
          :key="item.goods.id" 
          class="cart-goods-row"
        >
          <!-- 左侧勾选圆圈 (原型黑勾) -->
          <view class="cart-check-circle">
            <image src="/static/common/check.png" mode="aspectFit" class="check-img" />
          </view>

          <!-- 商品图片 -->
          <image :src="item.goods.image" mode="aspectFit" class="cart-item-img" />

          <!-- 商品信息 -->
          <view class="cart-info-col">
            <text class="cart-item-name">{{ item.goods.name }}</text>
            <text class="cart-item-price">{{ item.goods.price }}金币</text>
          </view>

          <!-- 数量 -->
          <view class="cart-count-col">
            <text class="cart-count-text">×{{ item.count }}</text>
          </view>
        </view>
      </view>

      <view v-else class="empty-cart-hint">
        <text class="empty-text">购物车是空的，快去兑换生活物资吧！</text>
      </view>

      <view class="elder-no-more">
        <text>——暂无更多——</text>
      </view>
    </scroll-view>

    <!-- 底部合计与剩余金币双框 (严格对齐原型) -->
    <view class="summary-bottom-section">
      <view class="summary-boxes-row">
        <view class="summary-box">
          <text class="summary-text">合计 ： {{ mallStore.totalCartPoints }}</text>
        </view>
        <view class="summary-box">
          <text class="summary-text">剩余 ： {{ remainingPoints }}</text>
        </view>
      </view>

      <!-- 确定兑换大按钮 (原型高饱和蓝) -->
      <button 
        class="confirm-exchange-btn" 
        :class="{ 'btn-disabled': cartItems.length === 0 || remainingPoints < 0 }"
        hover-class="btn-hover" 
        @tap="handleExchange"
      >
        确定兑换
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useMallStore } from '../../stores/mall';
import { useUserStore } from '../../stores/user';
import { useSettingStore } from '../../stores/setting';

const mallStore = useMallStore();
const userStore = useUserStore();
const settingStore = useSettingStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const cartItems = computed(() => {
  return mallStore.cart;
});

const remainingPoints = computed(() => {
  return userStore.currentUser.points - mallStore.totalCartPoints;
});

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const editAddress = () => {
  uni.showModal({
    title: '修改收货地址',
    editable: true,
    placeholderText: '请输入您的详细地址',
    content: userStore.currentUser.address,
    confirmText: '保存',
    confirmColor: '#1B65F0',
    success: (res) => {
      if (res.confirm && res.content) {
        userStore.updateProfile({ address: res.content });
        uni.showToast({ title: '地址已更新', icon: 'success' });
      }
    }
  });
};

const handleExchange = () => {
  if (cartItems.value.length === 0) {
    uni.showToast({ title: '购物车暂无物品', icon: 'none' });
    return;
  }
  if (remainingPoints.value < 0) {
    uni.showToast({ title: '金币余额不足', icon: 'none' });
    return;
  }

  const exOrder = mallStore.confirmExchange();
  if (exOrder) {
    // 跳转兑换成功页 (原型：兑换成功.png)
    uni.redirectTo({
      url: `/pages/square/exchange-success?orderId=${exOrder.id}`
    });
  }
};
</script>

<style lang="scss" scoped>
.cart-page-container {
  height: 100vh;
  background-color: #F6F7FB;
  display: flex;
  flex-direction: column;
}

/* 顶部蓝色导航栏 (严格对齐兑换页2.png) */
.blue-header-nav {
  height: 140rpx;
  background-color: #1B65F0;
  padding-top: 50rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-left: 28rpx;
  padding-right: 28rpx;
}

.back-text {
  font-size: calc(34rpx * var(--font-scale));
  font-weight: 700;
  color: #FFFFFF;
}

.page-title {
  font-size: calc(48rpx * var(--font-scale));
  font-weight: 900;
  color: #FFFFFF;
  letter-spacing: 4rpx;
}

.nav-right-placeholder {
  width: 90rpx;
}

/* 地址条 */
.delivery-address-bar {
  background-color: #FFFFFF;
  padding: 24rpx 32rpx;
  display: flex;
  align-items: center;

  .loc-icon-wrap {
    margin-right: 14rpx;
    display: flex;
    align-items: center;
    .loc-icon-img {
      width: 32rpx;
      height: 32rpx;
    }
  }

  .address-text {
    flex: 1;
    font-size: calc(30rpx * var(--font-scale));
    font-weight: 800;
    color: #1F2329;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .edit-addr-btn {
    background-color: #EBF2FF;
    color: #1B65F0;
    font-size: calc(28rpx * var(--font-scale));
    font-weight: 800;
    border-radius: 30rpx;
    padding: 0 24rpx;
    height: 60rpx;
    line-height: 60rpx;
    margin-left: 16rpx;
    border: none;

    &::after {
      border: none;
    }
  }
}

.gray-bar {
  height: 24rpx;
  background-color: #E5E7EB;
}

.cart-scroll-area {
  flex: 1;
  padding: 24rpx 32rpx;
}

.cart-goods-row {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 28rpx;
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.cart-check-circle {
  width: 68rpx;
  height: 68rpx;
  border-radius: 50%;
  border: 4rpx solid #1F2329;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;

  .check-img {
    width: 36rpx;
    height: 36rpx;
  }
}

.cart-item-img {
  width: 140rpx;
  height: 140rpx;
  border-radius: 20rpx;
  margin-right: 24rpx;
}

.cart-info-col {
  flex: 1;
  display: flex;
  flex-direction: column;

  .cart-item-name {
    font-size: calc(36rpx * var(--font-scale));
    font-weight: 900;
    color: #1F2329;
    margin-bottom: 12rpx;
  }

  .cart-item-price {
    font-size: calc(32rpx * var(--font-scale));
    font-weight: 700;
    color: #1F2329;
  }
}

.cart-count-col {
  .cart-count-text {
    font-size: calc(40rpx * var(--font-scale));
    font-weight: 900;
    color: #1F2329;
  }
}

.empty-cart-hint {
  text-align: center;
  padding: 80rpx 0;

  .empty-text {
    font-size: calc(32rpx * var(--font-scale));
    color: #8F959E;
  }
}

/* 底部合计与剩余双框 */
.summary-bottom-section {
  padding: 20rpx 36rpx 40rpx 36rpx;
  background-color: #F6F7FB;
}

.summary-boxes-row {
  display: flex;
  gap: 28rpx;
  margin-bottom: 30rpx;
}

.summary-box {
  flex: 1;
  height: calc(110rpx * var(--font-scale));
  background-color: #FFFFFF;
  border: 4rpx solid #1B65F0;
  border-radius: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);

  .summary-text {
    font-size: calc(36rpx * var(--font-scale));
    font-weight: 900;
    color: #1F2329;
  }
}

.confirm-exchange-btn {
  width: 100%;
  height: calc(108rpx * var(--font-scale));
  line-height: calc(108rpx * var(--font-scale));
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(44rpx * var(--font-scale));
  font-weight: 900;
  border-radius: 36rpx;
  letter-spacing: 6rpx;
  box-shadow: 0 10rpx 28rpx rgba(27, 101, 240, 0.35);
  border: none;

  &::after {
    border: none;
  }
}

.btn-disabled {
  background-color: #B0B5BD;
  box-shadow: none;
}

.btn-hover {
  opacity: 0.88;
}
</style>
