<template>
  <view class="error-page-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 中心出错内容卡片 (严格对齐出错页.png) -->
    <view class="error-content-box">
      <!-- 难过的向日葵吉祥物插画 -->
      <view class="error-mascot-wrap">
        <image src="/static/illustrations/error-mascot.png" mode="aspectFit" class="mascot-img" />
      </view>

      <view class="error-text-wrap">
        <text class="error-main-title">哎呀！出错了！</text>
        <text class="error-sub-desc">我需要网络连接</text>
      </view>
    </view>

    <!-- 确定大蓝按钮 (原型样式) -->
    <view class="bottom-action-wrap">
      <button class="confirm-retry-btn" hover-class="btn-hover" @tap="handleRetry">
        确 定
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useSettingStore } from '../../stores/setting';

const settingStore = useSettingStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const handleRetry = () => {
  uni.showLoading({ title: '正在重新连接...' });
  setTimeout(() => {
    uni.hideLoading();
    uni.navigateBack({ delta: 1 });
  }, 600);
};
</script>

<style lang="scss" scoped>
.error-page-container {
  min-height: 100vh;
  background-color: #F6F7FB;
  padding: 0 44rpx 60rpx 44rpx;
  display: flex;
  flex-direction: column;
}

.status-bar {
  height: 60rpx;
}

.error-content-box {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-bottom: 80rpx;
}

.error-mascot-wrap {
  width: 480rpx;
  height: 520rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;

  .mascot-img {
    width: 100%;
    height: 100%;
  }
}

.error-text-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;

  .error-main-title {
    font-size: calc(52rpx * var(--font-scale));
    font-weight: 900;
    color: #000000;
    letter-spacing: 4rpx;
    margin-bottom: 20rpx;
  }

  .error-sub-desc {
    font-size: calc(36rpx * var(--font-scale));
    font-weight: 800;
    color: #1F2329;
  }
}

.bottom-action-wrap {
  margin-bottom: 40rpx;
}

.confirm-retry-btn {
  width: 100%;
  height: calc(112rpx * var(--font-scale));
  line-height: calc(112rpx * var(--font-scale));
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(48rpx * var(--font-scale));
  font-weight: 900;
  border-radius: 36rpx;
  letter-spacing: 12rpx;
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
