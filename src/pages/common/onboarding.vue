<template>
  <view class="onboarding-page">
    <!-- 闪屏页 -->
    <view class="onboarding-screen splash-screen" :class="{ 'is-active': step === 0 }">
      <view class="splash-skip-row">
        <view class="splash-skip-btn" hover-class="skip-hover" @tap="goToLaunchOne">
          <text class="splash-skip-text">跳过 {{ countdown }}</text>
        </view>
      </view>

      <image src="/static/splash-bg.jpg" mode="aspectFill" class="splash-bg-img" />

      <view class="splash-bottom-layer">
        <button class="experience-btn" hover-class="btn-hover" @tap="goToLaunchOne">
          立 即 体 验
        </button>
      </view>
    </view>

    <!-- 启动页 1 -->
    <view class="onboarding-screen launch-screen launch-one" :class="{ 'is-active': step === 1 }">
      <view class="skip-row">
        <view class="skip-pill" hover-class="skip-hover" @tap="goToLaunchTwo">
          <text class="skip-text">跳过 {{ countdown }}</text>
        </view>
      </view>

      <view class="logo-card launch-one-logo">
        <image src="/static/launch/logo.png" mode="aspectFit" class="logo-img" />
      </view>

      <image src="/static/launch/text-white.png" mode="aspectFit" class="brand-text-img launch-one-brand" />

      <view class="slogan-wrap launch-one-slogan">
        <text class="slogan-text">[全民互助 更有温度]</text>
      </view>

      <view class="illustration-wrap">
        <image
          src="/static/launch/launch1-illustration.png"
          mode="aspectFit"
          class="illustration-img"
        />
      </view>
    </view>

    <!-- 启动页 2 -->
    <view class="onboarding-screen launch-screen launch-two" :class="{ 'is-active': step === 2 }">
      <view class="skip-row">
        <view class="skip-pill" hover-class="skip-hover" @tap="finish">
          <text class="skip-text">跳过 {{ countdown }}</text>
        </view>
      </view>

      <view class="brand-block">
        <view class="logo-card">
          <image src="/static/launch/logo.png" mode="aspectFit" class="logo-img" />
        </view>

        <image src="/static/launch/text-white.png" mode="aspectFit" class="brand-text-img launch-two-brand" />

        <view class="slogan-wrap launch-two-slogan">
          <text class="slogan-text">[你帮我 我帮你 轻松搭伴]</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();

// 三个视图共用同一个连续倒计时：总时长 3 秒
// 剩余 3~2 显示在闪屏页，2~1 显示在启动页 1，1~0 显示在启动页 2
const TOTAL_SECONDS = 3;
const STEP1_AT = 2; // 剩余 3 秒时切换到启动页 1
const STEP2_AT = 1; // 剩余 1 秒时切换到启动页 2

const step = ref(0);
const countdown = ref(TOTAL_SECONDS);
let timer: ReturnType<typeof setInterval> | null = null;

const clearTimer = () => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
};

const startCountdown = () => {
  clearTimer();
  countdown.value = TOTAL_SECONDS;
  step.value = 0;
  timer = setInterval(() => {
    if (countdown.value <= 0) {
      clearTimer();
      finish();
      return;
    }
    countdown.value -= 1;
    if (countdown.value === STEP1_AT) {
      step.value = 1;
    } else if (countdown.value === STEP2_AT) {
      step.value = 2;
    }
  }, 1000);
};

const goToLaunchOne = () => {
  step.value = 1;
};

const goToLaunchTwo = () => {
  step.value = 2;
};

const finish = () => {
  clearTimer();
  if (userStore.isLoggedIn) {
    uni.switchTab({ url: '/pages/index/index' });
  } else {
    uni.reLaunch({ url: '/pages/auth/login' });
  }
};

onMounted(() => {
  startCountdown();
});

onUnmounted(() => {
  clearTimer();
});
</script>

<style lang="scss" scoped>
.onboarding-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background-color: #125EF0;
}

/* 三层页面放在同一个页面内，用透明度过渡，避免 redirectTo 的原生切页闪烁 */
.onboarding-screen {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.35s ease;
  overflow: hidden;
  z-index: 1;

  &.is-active {
    opacity: 1;
    pointer-events: auto;
    z-index: 2;
  }
}

/* ---------------- 闪屏页 ---------------- */
.splash-screen {
  background-color: #0268F1;
}

.splash-bg-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.splash-skip-row {
  position: absolute;
  top: 70rpx;
  right: 36rpx;
  z-index: 10;
}

.splash-skip-btn {
  background: rgba(0, 0, 0, 0.4);
  padding: 8rpx 24rpx;
  border-radius: 30rpx;
}

.splash-skip-text {
  color: #FFFFFF;
  font-size: 26rpx;
  font-weight: 700;
}

.splash-bottom-layer {
  position: absolute;
  bottom: calc(30rpx + env(safe-area-inset-bottom));
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

/* ---------------- 启动页公共 ---------------- */
.launch-screen {
  background-color: #125EF0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.skip-row {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  padding: 70rpx 32rpx 0 32rpx;
}

.skip-pill {
  min-width: 140rpx;
  height: 60rpx;
  padding: 0 24rpx;
  background-color: rgba(0, 0, 0, 0.28);
  border-radius: 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.skip-hover {
  opacity: 0.75;
}

.skip-text {
  font-size: 28rpx;
  font-weight: 700;
  color: #FFFFFF;
  letter-spacing: 2rpx;
}

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
  width: 162rpx;
  height: 136rpx;
}

.brand-text-img {
  width: 480rpx;
  height: 160rpx;
}

.slogan-text {
  font-size: 40rpx;
  font-weight: 900;
  color: #FFFFFF;
  letter-spacing: 2rpx;
}

/* ---------------- 启动页 1 ---------------- */
.launch-one-logo {
  margin-top: 40rpx;
}

.launch-one-brand {
  margin-top: 48rpx;
}

.launch-one-slogan {
  margin-top: 60rpx;
}

.illustration-wrap {
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 30rpx;
}

.illustration-img {
  width: 750rpx;
  height: 750rpx;
}

/* ---------------- 启动页 2 ---------------- */
.brand-block {
  width: 100%;
  margin-top: 120rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.launch-two-brand {
  width: 460rpx;
  height: 152rpx;
  margin-top: 36rpx;
}

.launch-two-slogan {
  margin-top: 8rpx;
}

.btn-hover {
  opacity: 0.9;
}

/* 小屏机型避免按钮压到背景标语 */
@media screen and (max-height: 700px) {
  .splash-bottom-layer {
    bottom: 16rpx;
  }

  .experience-btn {
    height: 88rpx;
    line-height: 88rpx;
    font-size: 38rpx;
  }
}
</style>

<style lang="scss">
/* 页面级底色：避免页面首帧露出全局浅灰底色 */
page {
  background-color: #125EF0;
}
</style>
