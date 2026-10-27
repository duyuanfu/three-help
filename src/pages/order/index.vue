<template>
  <view class="order-page-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部标题 -->
    <view class="page-title-wrap">
      <text class="page-title">订 单</text>
    </view>

    <!-- 搜索栏 (严格对齐原型) -->
    <view class="search-box">
      <image src="/static/common/search.png" mode="aspectFit" class="search-icon-img" />
      <input 
        v-model="orderStore.searchKeyword" 
        class="search-input" 
        placeholder="搜索" 
        placeholder-class="search-placeholder"
      />
    </view>

    <!-- 顶部状态双 Tab (进行中 / 已完成) -->
    <view class="tab-toggle-bar">
      <view 
        class="tab-btn" 
        :class="activeTab === 'in_progress' ? 'tab-btn-active' : 'tab-btn-inactive'"
        @tap="activeTab = 'in_progress'"
      >
        <text class="tab-text">进行中</text>
      </view>
      <view 
        class="tab-btn" 
        :class="activeTab === 'completed' ? 'tab-btn-active' : 'tab-btn-inactive'"
        @tap="activeTab = 'completed'"
      >
        <text class="tab-text">已完成</text>
      </view>
    </view>

    <!-- 订单列表滚动视图 -->
    <scroll-view scroll-y class="orders-scroll-area">
      <!-- 1. 进行中 Tab 列表 -->
      <view v-if="activeTab === 'in_progress'">
        <view 
          v-for="order in inProgressList" 
          :key="order.id" 
          class="progress-card"
          hover-class="order-card-hover"
          @tap="goToDetail(order.id)"
        >
          <view class="card-content-layout">
            <!-- 左侧圆圈图标/头像 -->
            <view class="circle-avatar">
              <image 
                v-if="order.helper && order.helper.avatar" 
                :src="order.helper.avatar" 
                mode="aspectFill" 
                class="inner-avatar" 
              />
            </view>

            <!-- 右侧订单主体信息 -->
            <view class="card-main-info">
              <!-- 时间行 -->
              <view class="info-top-row">
                <text class="info-date">{{ order.appointTime.split(' ')[0] || '今日' }}</text>
                <text class="info-time">{{ order.appointTime.split(' ')[1] || '全天' }}</text>
              </view>

              <!-- 人物与任务简称 -->
              <view class="info-middle-row">
                <text class="helper-name-tag">
                  {{ order.helper ? `${order.helper.name}${order.helper.tag}` : '等待爱心接单' }}
                </text>
                <text class="task-badge-title">{{ order.title }}</text>
              </view>

              <!-- 状态追踪行 -->
              <view class="status-tracking-line">
                <view class="green-dot"></view>
                <text class="status-helper-name" v-if="order.helper">{{ order.helper.name }}</text>
                <text class="status-desc-text">{{ order.statusText || '帮手即将到达' }}</text>
              </view>

              <!-- 底部操作按钮行 -->
              <view class="card-btn-row">
                <button 
                  class="pill-action-btn" 
                  hover-class="pill-hover" 
                  @tap.stop="handleContact(order)"
                >
                  {{ order.helper && order.helper.name.includes('燕') ? '联系她' : '联系他' }}
                </button>
                <button 
                  class="pill-action-btn cancel-btn" 
                  hover-class="pill-hover" 
                  @tap.stop="handleCancel(order.id)"
                >
                  取 消
                </button>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 2. 已完成 Tab 列表 (严格对照订单页2.png) -->
      <view v-else>
        <!-- 互助服务已完成卡片 -->
        <view 
          v-for="order in completedList" 
          :key="order.id" 
          class="completed-dark-card"
          hover-class="order-card-hover"
          @tap="goToDetail(order.id)"
        >
          <view class="card-content-layout">
            <!-- 左侧勾选圆圈 -->
            <view class="checked-circle">
              <image src="/static/common/check.png" mode="aspectFit" class="check-icon-img" />
            </view>

            <!-- 主信息 -->
            <view class="card-main-info">
              <view class="info-top-row">
                <text class="dark-date">{{ order.appointTime.split(' ')[0] }}</text>
                <text class="dark-time">{{ order.appointTime.split(' ')[1] }}</text>
              </view>
              <view class="info-middle-row">
                <text class="dark-helper">{{ order.helper ? `${order.helper.name}${order.helper.tag}` : '志愿者' }}</text>
                <text class="dark-title">{{ order.title }}</text>
              </view>
              <!-- 黑色全宽联系他按钮 (原型样式) -->
              <button 
                class="black-contact-btn" 
                hover-class="black-hover" 
                @tap.stop="handleContact(order)"
              >
                联系他
              </button>
            </view>
          </view>
        </view>

        <!-- 积分兑换商品卡片 (严格对照订单页2.png中兑换鸡蛋一盒卡片) -->
        <view 
          v-for="exOrder in exchangeList" 
          :key="exOrder.id" 
          class="completed-dark-card"
          hover-class="order-card-hover"
          @tap="goToLogistics(exOrder.id)"
        >
          <view class="card-content-layout">
            <view class="checked-circle">
              <text class="check-icon">✔</text>
            </view>
            <view class="card-main-info">
              <view class="info-top-row">
                <text class="dark-date">{{ exOrder.createdAt.split(' ')[0] }}</text>
                <text class="dark-time">下单时间{{ exOrder.createdAt.split(' ')[1] }}</text>
              </view>
              <view class="info-middle-row">
                <text class="dark-title bold-big">{{ exOrder.goodsName }}</text>
              </view>
              <!-- 查看物流按钮 (原型样式) -->
              <button 
                class="black-contact-btn" 
                hover-class="black-hover" 
                @tap.stop="goToLogistics(exOrder.id)"
              >
                查看物流
              </button>
            </view>
          </view>
        </view>
      </view>

      <view class="elder-no-more">
        <text>——暂无更多——</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useOrderStore } from '../../stores/order';
import { useChatStore } from '../../stores/chat';
import { useSettingStore } from '../../stores/setting';
import type { HelpOrder } from '../../types';

const orderStore = useOrderStore();
const chatStore = useChatStore();
const settingStore = useSettingStore();

const activeTab = ref<'in_progress' | 'completed'>('in_progress');

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const inProgressList = computed(() => {
  return orderStore.inProgressOrders;
});

const completedList = computed(() => {
  return orderStore.completedOrders;
});

const exchangeList = computed(() => {
  return orderStore.exchangeOrders;
});

const handleContact = (order: HelpOrder) => {
  const targetUser = order.helper ? {
    id: order.helper.id,
    name: order.helper.name,
    avatar: order.helper.avatar,
    tag: order.helper.tag
  } : {
    id: 'u_wuxiaoying',
    name: '吴小应',
    avatar: '/static/avatars/malingling.png',
    tag: '@学生'
  };

  const session = chatStore.openOrCreateSession(targetUser);
  uni.navigateTo({
    url: `/pages/message/chat?sessionId=${session.sessionId}&orderId=${order.id}`
  });
};

const handleCancel = (orderId: string) => {
  uni.showModal({
    title: '提示',
    content: '确定要取消该求助订单吗？',
    confirmColor: '#FF4D4F',
    cancelText: '点错了',
    confirmText: '确认取消',
    success: (res) => {
      if (res.confirm) {
        orderStore.cancelOrder(orderId);
        uni.showToast({ title: '已取消订单', icon: 'success' });
      }
    }
  });
};

const goToLogistics = (orderId: string) => {
  uni.navigateTo({
    url: `/pages/order/logistics?orderId=${orderId}`
  });
};

const goToDetail = (orderId: string) => {
  uni.navigateTo({
    url: `/pages/order/detail?orderId=${orderId}`
  });
};
</script>

<style lang="scss" scoped>
.order-page-container {
  min-height: 100vh;
  background-color: #F6F7FB;
  padding: 0 32rpx 140rpx 32rpx;
  display: flex;
  flex-direction: column;
}

.status-bar {
  height: 60rpx;
}

.page-title-wrap {
  text-align: center;
  padding: 10rpx 0 24rpx 0;
}

.page-title {
  font-size: calc(52rpx * var(--font-scale));
  font-weight: 900;
  color: #1B65F0;
  letter-spacing: 4rpx;
}

/* 搜索框 */
.search-box {
  background-color: #FFFFFF;
  border-radius: 40rpx;
  height: calc(88rpx * var(--font-scale));
  display: flex;
  align-items: center;
  padding: 0 28rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.05);
  margin-bottom: 28rpx;

  .search-icon-img {
    width: 38rpx;
    height: 38rpx;
    margin-right: 14rpx;
  }

  .search-input {
    flex: 1;
    font-size: calc(32rpx * var(--font-scale));
    color: #1F2329;
  }

  .search-placeholder {
    color: #8F959E;
    font-size: calc(30rpx * var(--font-scale));
  }
}

/* 顶部状态双 Tab */
.tab-toggle-bar {
  display: flex;
  gap: 28rpx;
  margin-bottom: 28rpx;
}

.tab-btn {
  flex: 1;
  height: calc(108rpx * var(--font-scale));
  border-radius: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.08);
  transition: all 0.2s;

  .tab-text {
    font-size: calc(44rpx * var(--font-scale));
    font-weight: 900;
    letter-spacing: 4rpx;
  }
}

.tab-btn-active {
  background-color: #FFA940;
  .tab-text {
    color: #FFFFFF;
  }
}

.tab-btn-inactive {
  background-color: #1B65F0;
  .tab-text {
    color: #FFFFFF;
  }
}

/* 订单卡片列表 */
.orders-scroll-area {
  flex: 1;
}

/* 进行中卡片 (纯白大圆角) */
.progress-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 28rpx 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
}

.card-content-layout {
  display: flex;
}

.circle-avatar {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  border: 4rpx solid #E5E6EB;
  background-color: #F6F7FB;
  margin-right: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  .inner-avatar {
    width: 100%;
    height: 100%;
  }
}

.card-main-info {
  flex: 1;
}

.info-top-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8rpx;

  .info-date, .info-time {
    font-size: calc(28rpx * var(--font-scale));
    color: #1F2329;
    font-weight: 700;
  }
}

.info-middle-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 12rpx;

  .helper-name-tag {
    font-size: calc(34rpx * var(--font-scale));
    font-weight: 800;
    color: #1F2329;
  }

  .task-badge-title {
    font-size: calc(40rpx * var(--font-scale));
    font-weight: 900;
    color: #1F2329;
  }
}

.status-tracking-line {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;

  .green-dot {
    width: 22rpx;
    height: 22rpx;
    border-radius: 50%;
    background-color: #52C41A;
    margin-right: 12rpx;
  }

  .status-helper-name {
    font-size: calc(28rpx * var(--font-scale));
    color: #52C41A;
    font-weight: 700;
    margin-right: 14rpx;
  }

  .status-desc-text {
    font-size: calc(28rpx * var(--font-scale));
    color: #52C41A;
    font-weight: 700;
  }
}

.card-btn-row {
  display: flex;
  gap: 20rpx;
}

.pill-action-btn {
  flex: 1;
  height: calc(84rpx * var(--font-scale));
  line-height: calc(84rpx * var(--font-scale));
  background-color: #EBF2FF;
  color: #1B65F0;
  font-size: calc(34rpx * var(--font-scale));
  font-weight: 800;
  border-radius: 42rpx;
  border: none;

  &::after {
    border: none;
  }
}

.cancel-btn {
  background-color: #EBF2FF;
  color: #1B65F0;
}

/* 已完成 Tab 卡片 (灰色底卡片，原型视觉) */
.completed-dark-card {
  background-color: #8C8E91;
  border-radius: 36rpx;
  padding: 28rpx 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.08);

  .checked-circle {
    width: 76rpx;
    height: 76rpx;
    border-radius: 50%;
    border: 4rpx solid #FFFFFF;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 20rpx;

    .check-icon-img {
      width: 44rpx;
      height: 44rpx;
    }
  }

  .dark-date, .dark-time {
    font-size: calc(28rpx * var(--font-scale));
    color: #1F2329;
    font-weight: 700;
  }

  .dark-helper {
    font-size: calc(34rpx * var(--font-scale));
    font-weight: 800;
    color: #1F2329;
  }

  .dark-title {
    font-size: calc(40rpx * var(--font-scale));
    font-weight: 900;
    color: #1F2329;
  }

  .bold-big {
    font-size: calc(44rpx * var(--font-scale));
  }

  .black-contact-btn {
    margin-top: 18rpx;
    width: 100%;
    height: calc(88rpx * var(--font-scale));
    line-height: calc(88rpx * var(--font-scale));
    background-color: #000000;
    color: #FFFFFF;
    font-size: calc(36rpx * var(--font-scale));
    font-weight: 800;
    border-radius: 44rpx;
    border: none;

    &::after {
      border: none;
    }
  }
}

.pill-hover, .black-hover {
  opacity: 0.85;
}

.order-card-hover {
  opacity: 0.92;
}
</style>
