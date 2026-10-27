<template>
  <view class="launch-container">
    <!-- 右上角跳过倒计时 (严格对齐启动页1.png) -->
    <view class="skip-row">
      <view class="skip-pill" hover-class="skip-hover" @tap="goNext">
        <text class="skip-text">跳过 {{ countdown + 1 }}</text>
      </view>
    </view>

    <!-- 白色圆角 Logo 卡片 -->
    <view class="logo-card">
      <image src="/static/launch/logo.png" mode="aspectFit" class="logo-img" />
    </view>

    <!-- 帮帮帮 白色艺术字 (官方切图) -->
    <image src="/static/launch/text-white.png" mode="aspectFit" class="brand-text-img" />

    <!-- 品牌标语 -->
    <view class="slogan-wrap">
      <text class="slogan-text">[全民互助 更有温度]</text>
    </view>

    <!-- 底部吉祥物场景大插图 (官方切图) -->
    <view class="illustration-wrap">
      <image
        src="../../static/launch/launch1-illustration.png"
        mode="aspectFit"
        class="illustration-img"
      />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';

const countdown = ref(1);
let timer: any = null;

const goNext = () => {
  if (timer) clearInterval(timer);
  uni.redirectTo({ url: '/pages/common/launch-2' });
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

/* 白色圆角 Logo 卡片 */
.logo-card {
  margin-top: 40rpx;
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

/* 帮帮帮 白色艺术字 */
.brand-text-img {
  width: 480rpx;
  height: 160rpx;
  margin-top: 48rpx;
}

.slogan-wrap {
  margin-top: 60rpx;
}

.slogan-text {
  font-size: 40rpx;
  font-weight: 900;
  color: #FFFFFF;
  letter-spacing: 2rpx;
}

/* 底部吉祥物插图 */
.illustration-wrap {
  // flex: 1;
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
</style>
