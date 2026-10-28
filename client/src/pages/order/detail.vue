<template>
  <view class="detail-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航栏 -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">订单详情</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <scroll-view v-if="order" scroll-y class="detail-scroll">
      <!-- 状态卡片 -->
      <view class="status-card">
        <view class="status-dot" :class="order.status"></view>
        <view class="status-text-wrap">
          <text class="status-title">{{ statusLabel }}</text>
          <text class="status-sub">{{ order.statusText || statusHint }}</text>
        </view>
      </view>

      <!-- 任务信息卡片 -->
      <view class="info-card">
        <view class="task-head">
          <text class="task-title">{{ order.title }}</text>
          <text class="task-bounty">{{ order.bounty }} 金币</text>
        </view>
        <text class="task-content">{{ order.content }}</text>

        <view class="divider"></view>

        <view class="info-row">
          <text class="info-label">约定时间</text>
          <text class="info-value">{{ order.appointTime }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">约定地点</text>
          <text class="info-value">{{ order.address }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">订单编号</text>
          <text class="info-value small">{{ order.id }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">下单时间</text>
          <text class="info-value small">{{ order.createdAt }}</text>
        </view>
        <view v-if="order.completedAt" class="info-row">
          <text class="info-label">完成时间</text>
          <text class="info-value small">{{ order.completedAt }}</text>
        </view>
      </view>

      <!-- 对方卡片：我是发单人时显示接单人，我是接单人时显示发单人 -->
      <view v-if="counterpart" class="helper-card">
        <text class="section-title">{{ counterpart.roleTitle }}</text>
        <view class="helper-row">
          <image
            :src="avatarSrc(counterpart.avatar)"
            mode="aspectFill"
            class="helper-avatar"
          />
          <view class="helper-info">
            <text class="helper-name">{{ counterpart.name }}</text>
            <text class="helper-tag">{{ counterpart.tag }}</text>
          </view>
          <button class="contact-mini-btn" hover-class="btn-hover" @tap="handleContact">
            联 系
          </button>
        </view>
      </view>

      <!-- 评价卡片 -->
      <view v-if="order.review" class="info-card review-card">
        <text class="section-title">我的评价</text>
        <text class="review-stars">{{ reviewStars }}</text>
        <text class="review-comment">{{ order.review.comment }}</text>
      </view>

      <!-- 操作按钮 -->
      <view class="action-area">
        <button
          v-if="order.status === 'in_progress' || order.status === 'accepted'"
          class="primary-btn"
          hover-class="btn-hover"
          @tap="handleContact"
        >
          {{ contactLabel }}
        </button>
        <button
          v-if="order.status === 'completed' && counterpart"
          class="primary-btn"
          hover-class="btn-hover"
          @tap="handleContact"
        >
          {{ contactLabel }}
        </button>
        <button
          v-if="order.status === 'completed' || order.status === 'cancelled'"
          class="secondary-btn"
          hover-class="btn-hover"
          @tap="reorder"
        >
          再次下单
        </button>
        <button
          v-if="canCancel"
          class="danger-btn"
          hover-class="btn-hover"
          @tap="handleCancel"
        >
          取消订单
        </button>
      </view>
    </scroll-view>

    <!-- 订单不存在 -->
    <view v-else class="empty-state">
      <text class="empty-text">订单不存在或已删除</text>
      <button class="secondary-btn empty-back-btn" hover-class="btn-hover" @tap="goOrderList">
        返回订单列表
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { useOrderStore } from '../../stores/order';
import { useChatStore } from '../../stores/chat';
import { useSettingStore } from '../../stores/setting';
import { useUserStore } from '../../stores/user';
import { avatarSrc } from '../../utils/avatar';
import type { OrderStatus } from '../../types';

const orderStore = useOrderStore();
const chatStore = useChatStore();
const settingStore = useSettingStore();
const userStore = useUserStore();

const orderId = ref('');

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const order = computed(() => {
  if (!orderId.value) return undefined;
  return orderStore.orders.find((item) => item.id === orderId.value);
});

/** 当前用户在这单里的身份 */
const isHelperOfOrder = computed(() => Boolean(order.value && order.value.isHelper));

/**
 * 该展示、该联系的"对方"。
 * - 我是接单人 → 对方是发单人（要联系下单的老人）
 * - 我是发单人 → 对方是接单人
 * 之前不分身份一律展示接单人，导致接单人点"联系"变成联系自己。
 */
const counterpart = computed(() => {
  const current = order.value;
  if (!current) return null;

  if (current.isHelper) {
    if (!current.creatorId && !current.creatorName) return null;
    return {
      roleTitle: '发单人',
      id: current.creatorId || '',
      name: current.creatorName || '',
      avatar: current.creatorAvatar || '',
      tag: current.creatorTag || ''
    };
  }

  const helper = current.helper;
  if (!helper) return null;
  return {
    roleTitle: '接单人',
    id: helper.id || '',
    name: helper.name || '',
    avatar: helper.avatar || '',
    tag: helper.tag || ''
  };
});

/** 联系按钮文案随身份变化 */
const contactLabel = computed(() => {
  const current = order.value;
  if (!current) return '联 系';
  const isDone = current.status === 'completed';
  if (current.isHelper) {
    return isDone ? '再次联系发单人' : '联系发单人';
  }
  return isDone ? '再次联系帮手' : '联系帮手';
});

const STATUS_LABEL: Record<OrderStatus, string> = {
  pending: '等待帮手接单',
  accepted: '帮手已接单',
  in_progress: '服务进行中',
  completed: '订单已完成',
  cancelled: '订单已取消'
};

const statusLabel = computed(() => {
  return order.value ? STATUS_LABEL[order.value.status] : '';
});

const statusHint = computed(() => {
  if (!order.value) return '';
  if (order.value.status === 'pending') return '正在为您匹配附近的爱心帮手';
  if (order.value.status === 'completed') return '感谢您的使用，期待再次为您服务';
  if (order.value.status === 'cancelled') return '该订单已取消';
  return '帮手正在为您服务';
});

const canCancel = computed(() => {
  return order.value
    ? ['pending', 'accepted', 'in_progress'].includes(order.value.status)
    : false;
});

const reviewStars = computed(() => {
  const stars = order.value?.review?.stars || 0;
  return '★'.repeat(stars);
});

onLoad((options) => {
  if (options && options.orderId) {
    orderId.value = options.orderId;
  }
});

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const goOrderList = () => {
  uni.switchTab({ url: '/pages/order/index' });
};

const handleContact = async () => {
  const target = counterpart.value;
  if (!order.value || !target) {
    uni.showToast({ title: '暂无可联系的对象', icon: 'none' });
    return;
  }
  if (!target.id) {
    uni.showToast({ title: `${target.roleTitle}账号信息缺失`, icon: 'none' });
    return;
  }
  // 防御：绝不对自己发起会话（否则会话列表里会出现无对方的空白条目）
  if (target.id === userStore.currentUser.id) {
    uni.showToast({ title: '这是您自己的订单，无需联系自己', icon: 'none' });
    return;
  }
  uni.showLoading({ title: '打开会话...', mask: true });
  try {
    const session = await chatStore.openOrCreateSession(target.id);
    uni.hideLoading();
    uni.navigateTo({
      url: `/pages/message/chat?sessionId=${session.sessionId}&orderId=${order.value.id}`
    });
  } catch (e: any) {
    uni.hideLoading();
    uni.showToast({ title: e?.message || '打开会话失败', icon: 'none', duration: 2200 });
  }
};

const handleCancel = () => {
  if (!order.value) return;
  uni.showModal({
    title: '提示',
    content: '确定要取消该求助订单吗？',
    confirmColor: '#FF4D4F',
    cancelText: '点错了',
    confirmText: '确认取消',
    success: async (res) => {
      if (!res.confirm || !order.value) return;
      try {
        await orderStore.cancelOrder(order.value.id);
        uni.showToast({ title: '已取消订单', icon: 'none' });
      } catch (e: any) {
        uni.showToast({ title: e?.message || '取消失败', icon: 'none', duration: 2200 });
      }
    }
  });
};

const reorder = () => {
  if (!order.value) return;
  const o = order.value;
  uni.navigateTo({
    url: `/pages/order/create?title=${encodeURIComponent(o.title)}&category=${o.category}&content=${encodeURIComponent(o.content)}&address=${encodeURIComponent(o.address)}&bounty=${o.bounty}`
  });
};
</script>

<style lang="scss" scoped>
.detail-container {
  height: 100vh;
  background-color: #F6F7FB;
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
  padding: 20rpx 36rpx 30rpx 36rpx;
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
  font-size: calc(48rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1B65F0;
  letter-spacing: 2rpx;
}

.nav-right-placeholder {
  width: 90rpx;
}

.detail-scroll {
  flex: 1;
  padding: 0 32rpx 40rpx 32rpx;
}

/* 状态卡片 */
.status-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 30rpx 32rpx;
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
}

.status-dot {
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  margin-right: 20rpx;
  flex-shrink: 0;

  &.pending {
    background-color: #FFA940;
  }

  &.accepted,
  &.in_progress {
    background-color: #1B65F0;
  }

  &.completed {
    background-color: #52C41A;
  }

  &.cancelled {
    background-color: #FF4D4F;
  }
}

.status-text-wrap {
  display: flex;
  flex-direction: column;
}

.status-title {
  font-size: calc(36rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1F2329;
}

.status-sub {
  margin-top: 6rpx;
  font-size: calc(28rpx * var(--font-scale, 1));
  color: #8F959E;
  font-weight: 600;
}

/* 信息卡片 */
.info-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
}

.task-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.task-title {
  flex: 1;
  font-size: calc(42rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1F2329;
  line-height: 1.4;
}

.task-bounty {
  flex-shrink: 0;
  font-size: calc(32rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #FF8A00;
}

.task-content {
  display: block;
  margin-top: 16rpx;
  font-size: calc(32rpx * var(--font-scale, 1));
  color: #4E5969;
  line-height: 1.6;
}

.divider {
  height: 2rpx;
  background-color: #EEF0F4;
  margin: 28rpx 0;
}

.info-row {
  display: flex;
  margin-bottom: 20rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

.info-label {
  width: 150rpx;
  flex-shrink: 0;
  font-size: calc(30rpx * var(--font-scale, 1));
  color: #8F959E;
  font-weight: 600;
}

.info-value {
  flex: 1;
  font-size: calc(30rpx * var(--font-scale, 1));
  color: #1F2329;
  font-weight: 700;
  line-height: 1.5;
  word-break: break-all;
}

.info-value.small {
  font-size: calc(28rpx * var(--font-scale, 1));
  font-weight: 500;
  color: #4E5969;
}

/* 接单人卡片 */
.helper-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
}

.section-title {
  display: block;
  font-size: calc(32rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1F2329;
  margin-bottom: 20rpx;
}

.helper-row {
  display: flex;
  align-items: center;
}

.helper-avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  margin-right: 20rpx;
  background-color: #F2F4F7;
}

.helper-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.helper-name {
  font-size: calc(34rpx * var(--font-scale, 1));
  font-weight: 800;
  color: #1F2329;
}

.helper-tag {
  margin-top: 6rpx;
  font-size: calc(26rpx * var(--font-scale, 1));
  color: #8F959E;
  font-weight: 600;
}

.contact-mini-btn {
  width: 140rpx;
  height: calc(68rpx * var(--font-scale, 1));
  line-height: calc(68rpx * var(--font-scale, 1));
  background-color: #EBF2FF;
  color: #1B65F0;
  border-radius: 34rpx;
  font-size: calc(28rpx * var(--font-scale, 1));
  font-weight: 800;
  border: none;

  &::after {
    border: none;
  }
}

/* 评价 */
.review-stars {
  display: block;
  font-size: 40rpx;
  color: #FFB800;
  letter-spacing: 4rpx;
  margin-bottom: 12rpx;
}

.review-comment {
  font-size: calc(30rpx * var(--font-scale, 1));
  color: #4E5969;
  line-height: 1.6;
}

/* 操作区 */
.action-area {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  padding-bottom: 40rpx;
}

.primary-btn,
.secondary-btn,
.danger-btn {
  width: 100%;
  height: calc(104rpx * var(--font-scale, 1));
  line-height: calc(104rpx * var(--font-scale, 1));
  border-radius: 52rpx;
  font-size: calc(40rpx * var(--font-scale, 1));
  font-weight: 900;
  border: none;

  &::after {
    border: none;
  }
}

.primary-btn {
  background-color: #1B65F0;
  color: #FFFFFF;
  box-shadow: 0 10rpx 28rpx rgba(27, 101, 240, 0.3);
}

.secondary-btn {
  background-color: #FFFFFF;
  color: #1B65F0;
  border: 3rpx solid #1B65F0;
}

.danger-btn {
  background-color: #FFFFFF;
  color: #FF4D4F;
  border: 3rpx solid #FF4D4F;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 60rpx;
}

.empty-text {
  font-size: calc(34rpx * var(--font-scale, 1));
  color: #8F959E;
  margin-bottom: 36rpx;
}

.empty-back-btn {
  width: 100%;
}

.btn-hover {
  opacity: 0.86;
}
</style>
