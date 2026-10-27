<template>
  <view class="home-container" :style="pageStyle">
    <!-- 顶部状态栏占位 -->
    <view class="status-bar"></view>

    <!-- 顶部品牌与定位 -->
    <view class="header-section">
      <view class="logo-area">
        <view >
          <image src="../../static/launch/logo.png" mode="aspectFit" class="logo-img" />
        </view>
        <view class="title-wrap">
          <image src="/static/common/brand-blue.png" mode="aspectFit" class="brand-blue-img" />
          <view class="location-bar" @tap="changeCity">
            <image src="/static/common/location.png" mode="aspectFit" class="loc-icon-img" />
            <text class="city-name">{{ currentCity }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 问候语胶囊卡片 -->
    <view class="greeting-capsule">
      <text class="greeting-text">您好 ， 今天需要什么帮助？</text>
    </view>

    <!-- 一键下单大卡片 -->
    <view class="order-hero-card">
      <view class="hero-header">
        <image src="../../static/common/call.png" mode="aspectFit" class="phone-icon" />
        <!-- <view class="phone-icon-wrap">
          <text class="phone-icon">📞</text>
        </view> -->
        <view class="hero-text-wrap">
          <text class="hero-title">一 键 下 单</text>
          <text class="hero-subtitle">请说出您的需求</text>
        </view>
      </view>
      <view class="hero-actions">
        <button class="hero-btn" hover-class="btn-hover" @tap="goToCreate('一键需求下单')">
          立即下单
        </button>
        <button class="hero-btn" hover-class="btn-hover" @tap="goToTake">
          立即接单
        </button>
      </view>
    </view>

    <!-- 6 大服务分类九宫格卡片 -->
    <view class="categories-card">
      <view class="cat-grid">
        <view 
          v-for="cat in categoryList" 
          :key="cat.id" 
          class="cat-item"
          hover-class="cat-item-hover"
          @tap="goToCreate(cat.title, cat.id)"
        >
          <view class="cat-icon-box">
            <image :src="cat.icon" mode="aspectFit" class="cat-img" />
          </view>
          <text class="cat-label">{{ cat.title }}</text>
        </view>
      </view>
    </view>

    <!-- 我的求助进度状态卡片 -->
    <view class="my-request-card" v-if="latestOrder">
      <view class="req-top-row">
        <text class="req-label">我的求助</text>
        <view class="status-badge" :class="{ 'badge-done': latestOrder.status === 'in_progress' }">
          <text class="check-icon">✔</text>
          <text class="badge-text">{{ latestOrder.status === 'in_progress' ? '已有帮手接单' : '等待帮手接单' }}</text>
        </view>
      </view>
      <view class="req-middle-row">
        <text class="req-title">{{ latestOrder.title }}，{{ latestOrder.content || '按时上门服务' }}</text>
      </view>
      <view class="req-bottom-row">
        <button class="progress-btn" hover-class="btn-hover" @tap="goToOrderList">
          查看进度
        </button>
      </view>
    </view>

    <!-- 底部常驻语音求助条 (原型样式) -->
    <view class="voice-bar-wrap">
      <view 
        class="voice-bar" 
        hover-class="btn-hover" 
        @tap="showVoiceUnsupported"
      >
        <image src="/static/common/voice-mic.png" mode="aspectFit" class="mic-img" />
        <text class="voice-text">不方便打字？按住说话</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useOrderStore } from '../../stores/order';
import { useSettingStore } from '../../stores/setting';
import { showVoiceUnsupported } from '../../utils/voice';

const orderStore = useOrderStore();
const settingStore = useSettingStore();

const currentCity = ref('泉州市');

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const categoryList = [
  { id: 'transport', title: '出行接送', icon: '/static/categories/transport.png' },
  { id: 'medical', title: '出行就医', icon: '/static/categories/medical.png' },
  { id: 'shopping', title: '购物陪伴', icon: '/static/categories/shopping.png' },
  { id: 'delivery', title: '物品代买', icon: '/static/categories/delivery.png' },
  { id: 'cleaning', title: '家政服务', icon: '/static/categories/cleaning.png' },
  { id: 'consult', title: '线上咨询', icon: '/static/categories/consult.png' },
];

const latestOrder = computed(() => {
  return orderStore.latestMyRequest;
});

const changeCity = () => {
  currentCity.value = currentCity.value === '泉州市' ? '泉州·丰泽区' : '泉州市';
};

const goToCreate = (title: string, categoryId?: string) => {
  if (categoryId === 'consult' || title.includes('咨询')) {
    uni.navigateTo({
      url: '/pages/index/consult'
    });
    return;
  }
  // 首页分类入口：下单内容默认带入入口名称
  const contentParam = categoryId ? `&content=${encodeURIComponent(title)}` : '';
  uni.navigateTo({
    url: `/pages/order/create?title=${encodeURIComponent(title)}&category=${categoryId || 'delivery'}${contentParam}`
  });
};

const goToTake = () => {
  uni.navigateTo({
    url: '/pages/order/take'
  });
};

const goToOrderList = () => {
  uni.switchTab({
    url: '/pages/order/index'
  });
};

</script>

<style lang="scss" scoped>
.home-container {
  min-height: 100vh;
  background-color: #F6F7FB;
  padding: 0 60rpx 140rpx 60rpx;
  display: flex;
  flex-direction: column;
}

.status-bar {
  height: 60rpx;
}

/* 顶部品牌与定位 */
.header-section {
  padding: 20rpx 0 16rpx 0;
}

.logo-area {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 白色圆角 Logo 卡片（与启动页1完全一致） */
.logo-card {
  width: 182rpx;
  height: 182rpx;
  background-color: #FFFFFF;
  border-radius: 46rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12rpx 32rpx rgba(0, 0, 0, 0.12);
}

.logo-img {
  width: 236rpx;
  height: 200rpx;
}

.title-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.brand-blue-img {
  width: 308rpx;
  height: 102rpx;
}

.location-bar {
  display: flex;
  align-items: center;
  margin-top: 6rpx;
  padding: 4rpx 16rpx;
  border-radius: 30rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);

  .loc-icon-img {
    width: 68rpx;
    height: 80rpx;
    margin-right: 8rpx;
  }

  .city-name {
    font-size: calc(26rpx * var(--font-scale));
    color: #1F2329;
    font-weight: 600;
  }
}

/* 问候语胶囊 */
.greeting-capsule {
  margin: 18rpx 0 24rpx 0;
  background-color: #FFFFFF;
  border-radius: 40rpx;
  padding: 16rpx 28rpx;
  text-align: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);

  .greeting-text {
    font-size: calc(30rpx * var(--font-scale));
    font-weight: 700;
    color: #1F2329;
    letter-spacing: 2rpx;
  }
}

/* 一键下单大蓝卡 */
.order-hero-card {
  background: #1B65F0;
  border-radius: 36rpx;
  padding: 36rpx 32rpx;
  box-shadow: 0 10rpx 30rpx rgba(27, 101, 240, 0.35);
  margin-bottom: 28rpx;
}

.hero-header {
  display: flex;
  align-items: center;
  margin-bottom: 32rpx;
}

.phone-icon-wrap {
  width: 90rpx;
  height: 90rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;


}

.phone-icon {
  width: 130rpx;
  height: 130rpx;
}

.hero-text-wrap {
  display: flex;
  flex-direction: column;
  margin-left: 60rpx;
}

.hero-title {
  font-size: calc(54rpx * var(--font-scale));
  font-weight: 900;
  color: #FFFFFF;
  letter-spacing: 6rpx;
}

.hero-subtitle {
  font-size: calc(28rpx * var(--font-scale));
  color: rgba(255, 255, 255, 0.9);
  margin-top: 6rpx;
  letter-spacing: 10rpx;
}

.hero-actions {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  letter-spacing: 10rpx;
}

.hero-btn {
  flex: 1;
  height: calc(88rpx * var(--font-scale));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  margin: 0;
  background-color: #FFFFFF;
  color: #1B65F0;
  font-size: calc(34rpx * var(--font-scale));
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
  border-radius: 34rpx;
  border: none;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);

  &::after {
    border: none;
  }
}

/* 6 大分类卡片 */
.categories-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 24rpx 16rpx;
  box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 28rpx;
  border: 2px solid  #18529B;
}

.cat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  row-gap: 24rpx;
}

.cat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12rpx 0;
}

.cat-item-hover {
  opacity: 0.8;
  transform: scale(0.97);
}

.cat-icon-box {
  width: 130rpx;
  height: 100rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cat-img {
  width: 110rpx;
  height: 90rpx;
}

.cat-label {
  font-size: calc(28rpx * var(--font-scale));
  font-weight: 800;
  color: #1F2329;
  margin-top: 8rpx;
}

/* 我的求助卡片 */
.my-request-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 28rpx 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 30rpx;
}

.req-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.req-label {
  font-size: calc(28rpx * var(--font-scale));
  color: #1F2329;
  font-weight: 600;
}

.status-badge {
  display: flex;
  align-items: center;
  color: #52C41A;
  font-size: calc(26rpx * var(--font-scale));
  font-weight: 700;

  .check-icon {
    margin-right: 8rpx;
    font-size: 28rpx;
  }
}

.req-middle-row {
  margin-bottom: 20rpx;
  text-align: center;
}

.req-title {
  font-size: calc(34rpx * var(--font-scale));
  font-weight: 900;
  color: #1F2329;
  letter-spacing: 2rpx;
}

.req-bottom-row {
  display: flex;
  justify-content: center;
}

.progress-btn {
  width: 100%;
  height: calc(80rpx * var(--font-scale));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  margin: 0;
  background-color: #EBF2FF;
  color: #1B65F0;
  font-size: calc(32rpx * var(--font-scale));
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
  border-radius: 40rpx;
  border: none;

  &::after {
    border: none;
  }
}

/* 底部语音求助条 */
.voice-bar-wrap {
  margin-top: auto;
  padding-top: 10rpx;
}

.voice-bar {
  background-color: #EBF2FF;
  border: 3rpx solid #BCD6FF;
  border-radius: 50rpx;
  height: calc(96rpx * var(--font-scale));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24rpx;
  box-sizing: border-box;
  box-shadow: 0 4rpx 16rpx rgba(27, 101, 240, 0.1);
  transition: opacity 0.2s;

  .mic-img {
    width: 44rpx;
    height: 44rpx;
    margin-right: 16rpx;
  }

  .voice-text {
    font-size: calc(32rpx * var(--font-scale));
    font-weight: 800;
    color: #1B65F0;
    letter-spacing: 2rpx;
    white-space: nowrap;
  }
}

.btn-hover {
  opacity: 0.85;
}
</style>
