<template>
  <view class="logistics-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航栏 (严格对照物流页.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">查看物流</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 运单头部卡片 (原型样式) -->
    <view class="tracking-summary-card">
      <view class="express-company-row">
        <view class="blue-dot"></view>
        <text class="company-name">{{ currentExchange?.expressCompany || '中通快递' }}</text>
      </view>

      <view class="cities-route-row">
        <view class="city-col">
          <text class="city-name">{{ currentExchange?.senderCity.split(' ')[0] || '泉州市' }}</text>
          <text class="party-name">{{ currentExchange?.senderCity.split(' ')[1] || '帮帮超市' }}</text>
        </view>

        <view class="route-line-bar"></view>

        <view class="city-col right-align">
          <text class="city-name">{{ currentExchange?.receiverCity.split(' ')[0] || '泉州市' }}</text>
          <text class="party-name">{{ currentExchange?.receiverCity.split(' ')[1] || '张冬梅' }}</text>
        </view>
      </view>

      <view class="tracking-number-row">
        <text class="label-text">运单号</text>
        <text class="num-text">{{ currentExchange?.orderNo || '75124398600122' }}</text>
      </view>
    </view>

    <!-- 垂直时间轴节点 (原型绿色大圆点与实线) -->
    <view class="timeline-section">
      <view class="timeline-list">
        <!-- 节点 1: 已发货 -->
        <view class="timeline-item">
          <view class="node-left">
            <view class="green-circle-node"></view>
            <view class="vertical-connecting-bar"></view>
          </view>
          <view class="node-right">
            <text class="node-title">已发货</text>
            <text class="node-time">2025/08/23  20:00</text>
          </view>
        </view>

        <!-- 节点 2: 转站中 华大接驳点 -->
        <view class="timeline-item">
          <view class="node-left">
            <view class="green-circle-node"></view>
            <view class="vertical-connecting-bar"></view>
          </view>
          <view class="node-right">
            <text class="node-title">转站中  华大接驳点</text>
            <text class="node-time">2025/08/24  8:00</text>
          </view>
        </view>

        <!-- 节点 3: 已到达 -->
        <view class="timeline-item">
          <view class="node-left">
            <view class="green-circle-node"></view>
            <view class="vertical-connecting-bar"></view>
          </view>
          <view class="node-right">
            <text class="node-title">已到达</text>
            <text class="node-time">2025/08/24  12:00</text>
          </view>
        </view>

        <!-- 节点 4: 取件码 -->
        <view class="timeline-item no-line">
          <view class="node-left">
            <view class="green-circle-node"></view>
          </view>
          <view class="node-right">
            <text class="node-title code-highlight">取件码  70-2-0029</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { useOrderStore } from '../../stores/order';
import { useSettingStore } from '../../stores/setting';
import type { ExchangeOrder } from '../../types';

const orderStore = useOrderStore();
const settingStore = useSettingStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const orderId = ref('');

onLoad((options) => {
  if (options && options.orderId) {
    orderId.value = options.orderId;
  }
});

const currentExchange = computed<ExchangeOrder | undefined>(() => {
  if (orderId.value) {
    return orderStore.exchangeOrders.find((item) => item.id === orderId.value);
  }
  return orderStore.exchangeOrders[0];
});

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};
</script>

<style lang="scss" scoped>
.logistics-container {
  min-height: 100vh;
  background-color: #F6F7FB;
  padding: 0 36rpx 60rpx 36rpx;
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
  padding: 20rpx 0 30rpx 0;
}

.back-btn {
  padding: 10rpx 0;
}

.back-text {
  font-size: calc(34rpx * var(--font-scale));
  color: #1B65F0;
  font-weight: 700;
}

.page-title {
  font-size: calc(52rpx * var(--font-scale));
  font-weight: 900;
  color: #1B65F0;
  letter-spacing: 4rpx;
}

.nav-right-placeholder {
  width: 90rpx;
}

/* 运单卡片 */
.tracking-summary-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 50rpx;
}

.express-company-row {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;

  .blue-dot {
    width: 32rpx;
    height: 32rpx;
    background-color: #1B65F0;
    border-radius: 50%;
    margin-right: 16rpx;
  }

  .company-name {
    font-size: calc(32rpx * var(--font-scale));
    font-weight: 800;
    color: #1F2329;
  }
}

.cities-route-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.city-col {
  display: flex;
  flex-direction: column;

  .city-name {
    font-size: calc(42rpx * var(--font-scale));
    font-weight: 900;
    color: #1F2329;
  }

  .party-name {
    font-size: calc(28rpx * var(--font-scale));
    color: #8F959E;
    font-weight: 600;
    margin-top: 6rpx;
  }
}

.right-align {
  align-items: flex-end;
}

.route-line-bar {
  width: 120rpx;
  height: 12rpx;
  background-color: #1B65F0;
  border-radius: 6rpx;
}

.tracking-number-row {
  font-size: calc(30rpx * var(--font-scale));

  .label-text {
    color: #1F2329;
    font-weight: 700;
    margin-right: 20rpx;
  }

  .num-text {
    color: #1F2329;
    font-weight: 800;
    letter-spacing: 1rpx;
  }
}

/* 时间轴 */
.timeline-section {
  padding: 10rpx 20rpx;
}

.timeline-list {
  display: flex;
  flex-direction: column;
}

.timeline-item {
  display: flex;
  min-height: 140rpx;
}

.node-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 80rpx;
  margin-right: 30rpx;
}

.green-circle-node {
  width: 60rpx;
  height: 60rpx;
  background-color: #7DB95C;
  border-radius: 50%;
  flex-shrink: 0;
}

.vertical-connecting-bar {
  width: 14rpx;
  flex: 1;
  background-color: #7DB95C;
  margin: 8rpx 0;
}

.node-right {
  flex: 1;
  padding-top: 6rpx;
  display: flex;
  flex-direction: column;

  .node-title {
    font-size: calc(40rpx * var(--font-scale));
    font-weight: 900;
    color: #1F2329;
  }

  .code-highlight {
    font-size: calc(40rpx * var(--font-scale));
    letter-spacing: 2rpx;
  }

  .node-time {
    font-size: calc(30rpx * var(--font-scale));
    color: #1F2329;
    font-weight: 600;
    margin-top: 10rpx;
  }
}

.no-line {
  min-height: 80rpx;
}

.btn-hover {
  opacity: 0.85;
}
</style>
