<template>
  <view class="take-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航标题栏 (严格对齐接单页.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">接 单</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 待接单列表 -->
    <scroll-view scroll-y class="order-list-scroll">
      <view 
        v-for="order in pendingList" 
        :key="order.id" 
        class="take-card"
        @tap="toggleSelect(order.id)"
      >
        <view class="card-top-row">
          <view class="user-info">
            <image :src="order.creatorAvatar" mode="aspectFill" class="avatar-img" />
            <text class="user-name">{{ order.creatorName }}</text>
          </view>
          <view class="check-box" :class="{ 'check-box-active': selectedIds.includes(order.id) }">
            <image v-if="selectedIds.includes(order.id)" src="/static/common/check.png" mode="aspectFit" class="check-mark-img" />
          </view>
        </view>

        <view class="task-title-wrap">
          <text class="task-title">{{ order.title || order.content }}</text>
        </view>

        <view class="detail-rows">
          <view class="detail-line">
            <text class="detail-label">时间 ：</text>
            <text class="detail-val">{{ order.appointTime }}</text>
          </view>
          <view class="detail-line">
            <text class="detail-label">地点 ：</text>
            <text class="detail-val">{{ order.address }}</text>
          </view>
          <view class="detail-line">
            <text class="detail-label">金额 ：</text>
            <text class="detail-val bounty-val">{{ order.bounty }}</text>
          </view>
        </view>
      </view>

      <view class="elder-no-more">
        <text>——暂无更多——</text>
      </view>
    </scroll-view>

    <!-- 底部接单主按钮 -->
    <view class="bottom-action-bar">
      <button 
        class="take-action-btn" 
        :class="{ 'take-action-btn-disabled': selectedIds.length === 0 }"
        hover-class="btn-hover" 
        @tap="handleConfirmTake"
      >
        接 单（{{ selectedIds.length }}）
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useOrderStore } from '../../stores/order';
import { useSettingStore } from '../../stores/setting';

const orderStore = useOrderStore();
const settingStore = useSettingStore();

const selectedIds = ref<string[]>([]);

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const pendingList = computed(() => {
  return orderStore.pendingOrders;
});

onMounted(() => {
  // 默认选中第一条待接单任务
  if (pendingList.value.length > 0) {
    selectedIds.value = [pendingList.value[0].id];
  }
});

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const toggleSelect = (id: string) => {
  const idx = selectedIds.value.indexOf(id);
  if (idx > -1) {
    selectedIds.value.splice(idx, 1);
  } else {
    selectedIds.value.push(id);
  }
};

const handleConfirmTake = () => {
  if (selectedIds.value.length === 0) {
    uni.showToast({ title: '请至少勾选一个求助任务', icon: 'none' });
    return;
  }

  // 批量/逐一接单（接单人统一走默认志愿者，数据来自 helpers 表）
  selectedIds.value.forEach((id) => {
    orderStore.takeOrder(id);
  });

  // 跳转接单成功
  uni.redirectTo({
    url: '/pages/order/success?type=take'
  });
};
</script>

<style lang="scss" scoped>
.take-container {
  min-height: 100vh;
  background-color: #F6F7FB;
  padding: 0 36rpx 40rpx 36rpx;
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

.order-list-scroll {
  flex: 1;
}

.take-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
}

.card-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.user-info {
  display: flex;
  align-items: center;
}

.avatar-img {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  margin-right: 20rpx;
  background-color: #F0F2F5;
}

.user-name {
  font-size: calc(34rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1F2329;
}

.check-box {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  border: 4rpx solid #C4C8D0;
  background-color: #F0F2F5;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.check-box-active {
  background-color: #FFFFFF;
  border-color: #1F2329;
  
  .check-mark-img {
    width: 36rpx;
    height: 36rpx;
  }
}

.task-title-wrap {
  margin-bottom: 20rpx;
}

.task-title {
  font-size: calc(40rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1F2329;
}

.detail-rows {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.detail-line {
  display: flex;
  font-size: calc(32rpx * var(--font-scale, 1));
}

.detail-label {
  color: #1F2329;
  font-weight: 800;
  width: 130rpx;
}

.detail-val {
  color: #1F2329;
  font-weight: 600;
  flex: 1;
}

.bounty-val {
  font-weight: 900;
}

.bottom-action-bar {
  padding-top: 20rpx;
  margin-bottom: 30rpx;
}

.take-action-btn {
  width: 100%;
  height: calc(108rpx * var(--font-scale, 1));
  line-height: calc(108rpx * var(--font-scale, 1));
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(44rpx * var(--font-scale, 1));
  font-weight: 900;
  border-radius: 32rpx;
  box-shadow: 0 10rpx 28rpx rgba(27, 101, 240, 0.35);
  border: none;

  &::after {
    border: none;
  }
}

.take-action-btn-disabled {
  background-color: #B0B5BD;
  box-shadow: none;
}

.btn-hover {
  opacity: 0.88;
}
</style>
