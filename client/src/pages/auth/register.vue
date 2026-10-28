<template>
  <view class="register-page-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航与大标题 (严格对齐注册.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">注 册</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 注册表单输入区域 (原型双椭圆输入框) -->
    <view class="register-form-body">
      <!-- 1. 名字输入框 -->
      <view class="register-input-card">
        <input 
          v-model="name" 
          class="register-input" 
          placeholder="请输入您的名字" 
          placeholder-class="placeholder-text"
        />
      </view>

      <!-- 2. 年龄输入框 -->
      <view class="register-input-card">
        <input 
          v-model="age" 
          type="number" 
          class="register-input" 
          placeholder="请输入您的年龄" 
          placeholder-class="placeholder-text"
        />
      </view>

      <!-- 蓝色下一步大按钮 (原型样式) -->
      <view class="submit-action-wrap">
        <button class="next-step-btn" hover-class="btn-hover" @tap="handleNext">
          下 一 步
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useSettingStore } from '../../stores/setting';

const settingStore = useSettingStore();

const name = ref('');
const age = ref('');

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const handleNext = () => {
  if (!name.value.trim()) {
    uni.showToast({ title: '请输入您的名字', icon: 'none' });
    return;
  }
  if (!age.value.trim()) {
    uni.showToast({ title: '请输入您的年龄', icon: 'none' });
    return;
  }
  // 年龄必须在合理范围：曾有用户把 11 位手机号填进年龄框，
  // 原样传给后端会超出 Integer 范围直接报「服务异常」
  const ageNum = parseInt(age.value);
  if (!ageNum || ageNum < 1 || ageNum > 150) {
    uni.showToast({ title: '请输入正确的年龄（1-150）', icon: 'none' });
    return;
  }

  // 姓名与年龄只是注册资料，随下一步一起提交给后端，暂不写库
  uni.navigateTo({
    url: `/pages/auth/phone-reg?name=${encodeURIComponent(name.value.trim())}&age=${ageNum}`
  });
};
</script>

<style lang="scss" scoped>
.register-page-container {
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
  justify-content: space-between;
  padding: 30rpx 0 60rpx 0;
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
  font-size: calc(56rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1B65F0;
  letter-spacing: 6rpx;
}

.nav-right-placeholder {
  width: 90rpx;
}

.register-form-body {
  margin-top: 30rpx;
  display: flex;
  flex-direction: column;
  gap: 40rpx;
}

.register-input-card {
  background-color: #FFFFFF;
  border: 3rpx solid #1B65F0;
  border-radius: 44rpx;
  height: calc(116rpx * var(--font-scale, 1));
  padding: 0 36rpx;
  display: flex;
  align-items: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);

  .register-input {
    width: 100%;
    font-size: calc(36rpx * var(--font-scale, 1));
    font-weight: 800;
    color: #1F2329;
  }
}

.placeholder-text {
  color: #B0B5BD;
  font-size: calc(34rpx * var(--font-scale, 1));
}

.submit-action-wrap {
  margin-top: 24rpx;
}

.next-step-btn {
  width: 100%;
  height: calc(112rpx * var(--font-scale, 1));
  line-height: calc(112rpx * var(--font-scale, 1));
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(48rpx * var(--font-scale, 1));
  font-weight: 900;
  border-radius: 36rpx;
  letter-spacing: 10rpx;
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
