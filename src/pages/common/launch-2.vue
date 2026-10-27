<template>
  <view class="launch-container">
    <!-- 右上角跳过倒计时 (严格对齐启动页2.png) -->
    <view class="skip-row">
      <view class="skip-pill" hover-class="skip-hover" @tap="goNext">
        <text class="skip-text">跳过 {{ countdown }}</text>
      </view>
    </view>

    <!-- 居中品牌区 -->
    <view class="brand-block">
      <!-- 白色圆角 Logo 卡片 -->
      <view class="logo-card">
        <image src="/static/launch/logo.png" mode="aspectFit" class="logo-img" />
      </view>

      <!-- 帮帮帮 白色艺术字 (官方切图) -->
      <image src="/static/launch/text-white.png" mode="aspectFit" class="brand-text-img" />

      <!-- 品牌标语 -->
      <view class="slogan-wrap">
        <text class="slogan-text">[你帮我 我帮你 轻松搭伴]</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();

const countdown = ref(1);
let timer: any = null;

const goNext = () => {
  if (timer) clearInterval(timer);
  if (userStore.isLoggedIn) {
    uni.switchTab({ url: '/pages/index/index' });
  } else {
    uni.reLaunch({ url: '/pages/auth/login' });
  }
};

onMounted(() => {
  timer = setInterval(() => {
    if (countdown.value > 1) {
      countdown.value -= 1;
    } else {
      goNext();
    }
  }, 1000);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<style lang="scss" scoped>
.launch-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  background-color: #125EF0;
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow: hidden;
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

/* 居中品牌区 (原型 Logo 位于屏幕上方约 1/4 处) */
.brand-block {
  width: 100%;
  margin-top: 120rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
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
  width: 460rpx;
  height: 152rpx;
  margin-top: 36rpx;
}

.slogan-wrap {
  margin-top: 8rpx;
}

.slogan-text {
  font-size: 40rpx;
  font-weight: 900;
  color: #FFFFFF;
  letter-spacing: 2rpx;
}
</style>
