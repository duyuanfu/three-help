<template>
  <view class="auth-page-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航栏 (严格对齐实名认证.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">实名认证</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 认证输入表单 (原型双圆角卡片) -->
    <view class="auth-form-body">
      <!-- 1. 真实姓名 -->
      <view class="auth-input-card">
        <input 
          v-model="realName" 
          class="auth-input" 
          placeholder="请输入您的真实姓名" 
          placeholder-class="placeholder-text"
        />
      </view>

      <!-- 2. 身份证号码 -->
      <view class="auth-input-card">
        <input 
          v-model="idCard" 
          type="idcard" 
          class="auth-input" 
          placeholder="请输入您的身份证号码" 
          placeholder-class="placeholder-text"
          maxlength="18"
        />
      </view>

      <!-- 蓝色下一步大按钮 (原型样式) -->
      <view class="action-submit-wrap">
        <button class="next-step-btn" hover-class="btn-hover" @tap="handleSubmitAuth">
          下 一 步
        </button>
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

const realName = ref(userStore.currentUser.name);
const idCard = ref('35050219610815****');

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const handleSubmitAuth = () => {
  if (!realName.value.trim()) {
    uni.showToast({ title: '请输入真实姓名', icon: 'none' });
    return;
  }
  if (!idCard.value.trim() || idCard.value.length < 15) {
    uni.showToast({ title: '请输入合法的身份证号码', icon: 'none' });
    return;
  }

  // 实名认证页不设置身份标签，下一步进入信息验证页
  uni.navigateTo({ url: '/pages/auth/verify' });
};
</script>

<style lang="scss" scoped>
.auth-page-container {
  min-height: 100vh;
  background-color: #F6F7FB;
  padding: 0 36rpx 40rpx 36rpx;
  display: flex;
  flex-direction: column;
}

.status-bar {
  height: 60rpx;
}

.header-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0 36rpx 0;
}

.back-btn {
  padding: 10rpx 0;
}

.back-text {
  font-size: calc(34rpx * var(--font-scale, 1));
  color: #1B65F0;
  font-weight: 700;
}

.page-title {
  font-size: calc(52rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1B65F0;
  letter-spacing: 4rpx;
}

.nav-right-placeholder {
  width: 90rpx;
}

.auth-form-body {
  margin-top: 20rpx;
}

/* 输入框圆角卡片 (严格对齐实名认证.png) */
.auth-input-card {
  background-color: #FFFFFF;
  border: 3rpx solid #1B65F0;
  border-radius: 40rpx;
  height: calc(112rpx * var(--font-scale, 1));
  padding: 0 36rpx;
  display: flex;
  align-items: center;
  margin-bottom: 36rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);

  .auth-input {
    width: 100%;
    font-size: calc(34rpx * var(--font-scale, 1));
    color: #1F2329;
    font-weight: 700;
  }
}

.placeholder-text {
  color: #B0B5BD;
  font-size: calc(32rpx * var(--font-scale, 1));
}

.action-submit-wrap {
  margin-top: 30rpx;
}

.next-step-btn {
  width: 100%;
  height: calc(108rpx * var(--font-scale, 1));
  line-height: calc(108rpx * var(--font-scale, 1));
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(44rpx * var(--font-scale, 1));
  font-weight: 900;
  border-radius: 36rpx;
  letter-spacing: 8rpx;
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
