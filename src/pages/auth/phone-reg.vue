<template>
  <view class="phone-reg-container" :style="pageStyle">
    <!-- 整页底图：引导页1/2/3 -->
    <image :src="bgImage" mode="scaleToFill" class="page-bg" />

    <view class="page-content">
      <!-- 步骤 1: 手机号录入 (严格对齐引导页1.png) -->
      <view v-if="currentStep === 1" class="step-content-box">
        <view class="hero-spacer hero-spacer-step1"></view>

        <view class="input-card-wrap">
          <view class="phone-prefix">
            <text class="prefix-text">+86</text>
            <view class="vertical-bar"></view>
          </view>
          <input
            v-model="phoneNumber"
            type="number"
            class="phone-input"
            placeholder="请输入手机号"
            placeholder-class="placeholder-text"
            maxlength="11"
          />
        </view>

        <view class="submit-wrap">
          <button class="step-btn" hover-class="btn-hover" @tap="goToStep2">
            下 一 步
          </button>
        </view>
      </view>

      <!-- 步骤 2: 4位验证码 (严格对齐引导页2.png) -->
      <view v-else-if="currentStep === 2" class="step-content-box step2-content">
        <view class="hero-spacer hero-spacer-step2"></view>

        <view class="verify-code-section">
          <text class="verify-title">请输入4位验证码</text>
          <view class="code-boxes-row">
            <view class="code-box" :class="{ 'active-box': code[0] }"><text class="code-num">{{ code[0] || '' }}</text></view>
            <view class="code-box" :class="{ 'active-box': code[1] }"><text class="code-num">{{ code[1] || '' }}</text></view>
            <view class="code-box" :class="{ 'active-box': code[2] }"><text class="code-num">{{ code[2] || '' }}</text></view>
            <view class="code-box" :class="{ 'active-box': code[3] }"><text class="code-num">{{ code[3] || '' }}</text></view>
          </view>

          <view class="resend-countdown-wrap">
            <text class="countdown-text">{{ countdown }}s 后重发</text>
          </view>
        </view>

        <view class="submit-wrap step2-submit">
          <button class="step-btn" hover-class="btn-hover" @tap="goToStep3">
            下 一 步
          </button>
        </view>
      </view>

      <!-- 步骤 3: 设置密码 (严格对齐引导页3.png) -->
      <view v-else-if="currentStep === 3" class="step-content-box">
        <view class="hero-spacer hero-spacer-step3"></view>

        <view class="input-card-wrap">
          <input
            v-model="newPassword"
            password
            class="phone-input"
            placeholder="请设置新密码"
            placeholder-class="placeholder-text"
          />
        </view>

        <view class="submit-wrap">
          <button class="step-btn" hover-class="btn-hover" @tap="handleCompleteReg">
            完 成
          </button>
        </view>
      </view>

      <!-- 步骤 2/3 保留返回上一步入口，避免整页底图覆盖后无法回退 -->
      <view
        v-if="currentStep > 1"
        class="step-back"
        hover-class="btn-hover"
        @tap="handleBack"
      >
        <text>← 返回上一步</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useUserStore } from '../../stores/user';
import { useSettingStore } from '../../stores/setting';

const userStore = useUserStore();
const settingStore = useSettingStore();

const currentStep = ref(1);
const phoneNumber = ref('13859208888');
const code = ref(['8', '6', '1', '9']);
const countdown = ref(50);
const newPassword = ref('');

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const bgImage = computed(() => {
  const map: Record<number, string> = {
    1: '/static/onboarding/guide-1-bg.jpg',
    2: '/static/onboarding/guide-2-bg.jpg',
    3: '/static/onboarding/guide-3-bg.jpg'
  };
  return map[currentStep.value] || map[1];
});

const handleBack = () => {
  if (currentStep.value > 1) {
    currentStep.value -= 1;
  }
};

const goToStep2 = () => {
  if (!phoneNumber.value || phoneNumber.value.length < 11) {
    uni.showToast({ title: '请输入有效的11位手机号', icon: 'none' });
    return;
  }
  currentStep.value = 2;
};

const goToStep3 = () => {
  currentStep.value = 3;
};

const handleCompleteReg = () => {
  userStore.updateProfile({ phone: phoneNumber.value });
  userStore.login();
  uni.showToast({ title: '注册完成，欢迎来到帮帮帮！', icon: 'success' });
  setTimeout(() => {
    uni.reLaunch({ url: '/pages/common/onboarding' });
  }, 600);
};
</script>

<style lang="scss" scoped>
.phone-reg-container {
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
}

.step-content-box {
  display: flex;
  flex-direction: column;
}

.hero-spacer {
  width: 100%;
  flex-shrink: 0;
}

/* 底图上半部分已经包含插画，这里只负责把表单推到原型位置 */
.hero-spacer-step1,
.hero-spacer-step3 {
  height: 1000rpx;
}

.hero-spacer-step2 {
  height: 960rpx;
}

.input-card-wrap {
  background-color: #FFFFFF;
  border-radius: 40rpx;
  height: calc(116rpx * var(--font-scale, 1));
  padding: 0 36rpx;
  display: flex;
  align-items: center;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 80rpx;
}

.phone-prefix {
  display: flex;
  align-items: center;
  margin-right: 20rpx;

  .prefix-text {
    font-size: calc(36rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
    margin-right: 18rpx;
  }

  .vertical-bar {
    width: 3rpx;
    height: 44rpx;
    background-color: #1F2329;
  }
}

.phone-input {
  flex: 1;
  font-size: calc(36rpx * var(--font-scale, 1));
  font-weight: 800;
  color: #1F2329;
}

.placeholder-text {
  color: #B0B5BD;
  font-size: calc(34rpx * var(--font-scale, 1));
}

.verify-code-section {
  display: flex;
  flex-direction: column;
  align-items: center;

  .verify-title {
    font-size: calc(44rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #000000;
    letter-spacing: 2rpx;
    margin-bottom: 30rpx;
  }
}

.code-boxes-row {
  display: flex;
  gap: 24rpx;
  margin-bottom: 24rpx;
}

.code-box {
  width: 130rpx;
  height: 130rpx;
  background-color: #FFFFFF;
  border-radius: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.05);

  .code-num {
    font-size: 56rpx;
    font-weight: 900;
    color: #1F2329;
  }
}

.resend-countdown-wrap {
  margin-top: 110rpx;
  margin-bottom: 20rpx;

  .countdown-text {
    font-size: calc(30rpx * var(--font-scale, 1));
    color: #1F2329;
    font-weight: 700;
  }
}

.submit-wrap {
  margin-top: 10rpx;
}

.step2-submit {
  margin-top: 40rpx;
}

.step-btn {
  width: 100%;
  height: calc(112rpx * var(--font-scale, 1));
  line-height: calc(112rpx * var(--font-scale, 1));
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(48rpx * var(--font-scale, 1));
  font-weight: 900;
  border-radius: 36rpx;
  letter-spacing: 12rpx;
  box-shadow: 0 10rpx 28rpx rgba(27, 101, 240, 0.35);
  border: none;

  &::after {
    border: none;
  }
}

.step-back {
  margin-top: 28rpx;
  padding: 10rpx 0;
  text-align: center;
  font-size: calc(28rpx * var(--font-scale, 1));
  font-weight: 600;
  color: #7A8088;
}

.btn-hover {
  opacity: 0.88;
}
</style>
