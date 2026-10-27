<template>
  <view class="splash-container">
    <!-- 顶部状态栏与跳过倒计时按钮 (严格对齐闪屏页.png) -->
    <view class="top-skip-row">
      <view class="skip-pill-btn" @tap="handleEnterHome">
        <text class="skip-text">跳过 {{ countdown + 2 }}</text>
      </view>
    </view>

    <!-- 闪屏背景大图 (原型截取) -->
    <view class="splash-graphic-wrap">
      <image src="/static/splash-bg.jpg" mode="aspectFill" class="splash-full-img" />
    </view>

    <!-- 底部固定浮层与立即体验大按钮 -->
    <view class="bottom-action-layer">
      <button class="experience-btn" hover-class="btn-hover" @tap="handleEnterHome">
        立 即 体 验
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();

const countdown = ref(1);
let timer: any = null;

onMounted(() => {
  timer = setInterval(() => {
    if (countdown.value > 1) {
      countdown.value -= 1;
    } else {
      clearInterval(timer);
      handleEnterHome();
    }
  }, 1000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});

const handleEnterHome = () => {
  if (timer) clearInterval(timer);
  if (userStore.isLoggedIn) {
    uni.switchTab({
      url: '/pages/index/index'
    });
  } else {
    // 首次使用：依次播放 启动页1 / 启动页2 后再进入登录页
    uni.redirectTo({
      url: '/pages/common/launch-1'
    });
  }
};
</script>

<style lang="scss" scoped>
.splash-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  background-color: #1A6CFF;
  overflow: hidden;
}

.top-skip-row {
  position: absolute;
  top: 70rpx;
  right: 36rpx;
  z-index: 10;
}

.skip-pill-btn {
  background: rgba(0, 0, 0, 0.4);
  padding: 8rpx 24rpx;
  border-radius: 30rpx;

  .skip-text {
    color: #FFFFFF;
    font-size: 26rpx;
    font-weight: 700;
  }
}

.splash-graphic-wrap {
  width: 100%;
  height: 100%;

  .splash-full-img {
    width: 100%;
    height: 100%;
  }
}

.bottom-action-layer {
  position: absolute;
  bottom: calc(10rpx + env(safe-area-inset-bottom));
  left: 44rpx;
  right: 44rpx;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.experience-btn {
  width: 100%;
  height: 112rpx;
  line-height: 112rpx;
  background-color: #FFFFFF;
  color: #1B65F0;
  font-size: 46rpx;
  font-weight: 900;
  border-radius: 40rpx;
  letter-spacing: 8rpx;
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.25);
  border: none;

  &::after {
    border: none;
  }
}

.btn-hover {
  opacity: 0.9;
}

/* 小屏机型 (如 iPhone 6/7/8 375×667) 避免按钮压到背景标语 */
@media screen and (max-height: 700px) {
  .bottom-action-layer {
    bottom: 16rpx;
  }

  .experience-btn {
    height: 88rpx;
    line-height: 88rpx;
    font-size: 38rpx;
  }
}
</style>
