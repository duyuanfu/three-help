<template>
  <view class="verify-page-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部返回 (严格对齐信息验证.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">← 返回</text>
      </view>
    </view>

    <!-- 人脸识别中心视觉区 (严格对齐信息验证.png 的黑色人像与对焦框) -->
    <view class="face-verify-main">
      <view class="scanner-frame-box" :class="{ 'scanning-active': isScanning }">
        <!-- 四角取景器线框 -->
        <view class="corner corner-tl"></view>
        <view class="corner corner-tr"></view>
        <view class="corner corner-bl"></view>
        <view class="corner corner-br"></view>

        <!-- 黑色人脸轮廓剪影 -->
        <view class="silhouette-head"></view>
        <view class="silhouette-body"></view>

        <!-- 扫描线动画 -->
        <view v-if="isScanning" class="scan-line-animation"></view>
      </view>

      <view class="prompt-text-wrap">
        <text class="prompt-text">{{ isVerified ? '人脸识别已完成，信息核验通过' : '请进行人脸识别' }}</text>
      </view>
    </view>

    <!-- 蓝色完成大按钮 (原型样式) -->
    <view class="bottom-action-wrap">
      <button class="complete-verify-btn" hover-class="btn-hover" @tap="handleCompleteVerify">
        完 成
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useSettingStore } from '../../stores/setting';

const settingStore = useSettingStore();

const isScanning = ref(false);
const isVerified = ref(false);

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const handleCompleteVerify = () => {
  isScanning.value = true;
  uni.showLoading({ title: '人脸信息校验中...' });
  setTimeout(() => {
    uni.hideLoading();
    isScanning.value = false;
    isVerified.value = true;

    // 信息验证页仅做展示，暂不写入实名认证结果
    uni.showToast({ title: '人脸识别通过', icon: 'success' });

    setTimeout(() => {
      uni.navigateBack({ delta: 1 });
    }, 700);
  }, 1000);
};
</script>

<style lang="scss" scoped>
.verify-page-container {
  min-height: 100vh;
  background-color: #F6F7FB;
  padding: 0 44rpx 60rpx 44rpx;
  display: flex;
  flex-direction: column;
}

.status-bar {
  height: 60rpx;
}

.header-nav {
  display: flex;
  align-items: center;
  padding: 24rpx 0 40rpx 0;
}

.back-btn {
  padding: 10rpx 0;
}

.back-text {
  font-size: calc(36rpx * var(--font-scale));
  color: #000000;
  font-weight: 800;
}

.face-verify-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-bottom: 120rpx;
}

/* 取景框 (严格对齐信息验证.png) */
.scanner-frame-box {
  position: relative;
  width: 440rpx;
  height: 440rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-bottom: 50rpx;

  /* 四角取景器 */
  .corner {
    position: absolute;
    width: 60rpx;
    height: 60rpx;
    border-color: #000000;
    border-style: solid;
    border-width: 0;
  }

  .corner-tl {
    top: 0;
    left: 0;
    border-top-width: 12rpx;
    border-left-width: 12rpx;
  }

  .corner-tr {
    top: 0;
    right: 0;
    border-top-width: 12rpx;
    border-right-width: 12rpx;
  }

  .corner-bl {
    bottom: 0;
    left: 0;
    border-bottom-width: 12rpx;
    border-left-width: 12rpx;
  }

  .corner-br {
    bottom: 0;
    right: 0;
    border-bottom-width: 12rpx;
    border-right-width: 12rpx;
  }

  /* 黑色人脸轮廓剪影 */
  .silhouette-head {
    width: 160rpx;
    height: 160rpx;
    background-color: #000000;
    border-radius: 50%;
    margin-bottom: 16rpx;
  }

  .silhouette-body {
    width: 240rpx;
    height: 120rpx;
    background-color: #000000;
    border-top-left-radius: 120rpx;
    border-top-right-radius: 120rpx;
  }
}

.scan-line-animation {
  position: absolute;
  top: 20rpx;
  left: 20rpx;
  right: 20rpx;
  height: 6rpx;
  background-color: #1B65F0;
  box-shadow: 0 0 16rpx #1B65F0;
  animation: scanMove 1.5s infinite linear;
}

@keyframes scanMove {
  0% { transform: translateY(0); }
  50% { transform: translateY(380rpx); }
  100% { transform: translateY(0); }
}

.prompt-text-wrap {
  .prompt-text {
    font-size: calc(40rpx * var(--font-scale));
    font-weight: 900;
    color: #1F2329;
    letter-spacing: 2rpx;
  }
}

.bottom-action-wrap {
  margin-bottom: 40rpx;
}

.complete-verify-btn {
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
