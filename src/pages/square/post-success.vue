<template>
  <view class="success-container" :style="pageStyle">
    <!-- 整页底图：发布成功 -->
    <image src="/static/success/post-success-bg.jpg" mode="scaleToFill" class="page-bg" />

    <view class="page-content">
      <!-- 底图上半部分已经包含吉祥物和“发布成功”文案 -->
      <view class="hero-spacer"></view>

      <!-- 倒计时提示 -->
      <view class="countdown-text">
        <text>{{ countdownText }} 后返回</text>
      </view>

      <!-- 底部两大操作按钮 -->
      <view class="actions-group">
        <button class="action-btn" hover-class="btn-hover" @tap="goToMyPosts">
          查看帖子
        </button>
        <button class="action-btn" hover-class="btn-hover" @tap="goBack">
          返 回
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useSettingStore } from '../../stores/setting';

const settingStore = useSettingStore();
const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const countdown = ref(5);
let timer: any = null;

const countdownText = computed(() => `0${countdown.value}s`);

const clearTimer = () => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
};

const goToMyPosts = () => {
  clearTimer();
  uni.redirectTo({ url: '/pages/my/posts' });
};

const goBack = () => {
  clearTimer();
  uni.switchTab({ url: '/pages/square/index' });
};

onMounted(() => {
  timer = setInterval(() => {
    if (countdown.value > 1) {
      countdown.value -= 1;
    } else {
      clearTimer();
      goBack();
    }
  }, 1000);
});

onUnmounted(() => {
  clearTimer();
});
</script>

<style lang="scss" scoped>
.success-container {
  position: relative;
  min-height: 100vh;
  padding: 0 44rpx;
  background-color: #FFFFFF;
}

.page-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 1624rpx;
  z-index: 0;
  pointer-events: none;
}

.page-content {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  padding-bottom: 60rpx;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.hero-spacer {
  width: 100%;
  height: 860rpx;
  flex-shrink: 0;
}

.countdown-text {
  margin-top: 0;
  font-size: calc(34rpx * var(--font-scale, 1));
  font-weight: 600;
  color: #1F2329;
  letter-spacing: 2rpx;
}

.actions-group {
  width: 100%;
  margin-top: 80rpx;
  display: flex;
  flex-direction: column;
  gap: 36rpx;
}

.action-btn {
  width: 100%;
  height: calc(108rpx * var(--font-scale, 1));
  line-height: calc(108rpx * var(--font-scale, 1));
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(44rpx * var(--font-scale, 1));
  font-weight: 900;
  border-radius: 24rpx;
  box-shadow: 0 10rpx 28rpx rgba(27, 101, 240, 0.3);
  border: none;
  letter-spacing: 4rpx;

  &::after {
    border: none;
  }
}

.btn-hover {
  opacity: 0.88;
}
</style>
