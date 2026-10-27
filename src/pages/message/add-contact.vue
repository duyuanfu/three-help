<template>
  <view class="add-contact-page" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航栏 (严格对齐新建.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">新建联系人</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 表单字段组 (严格对齐新建.png) -->
    <view class="form-body">
      <!-- 1. 姓名 -->
      <view class="field-item">
        <text class="field-label">姓名</text>
        <view class="input-card">
          <input 
            v-model="name" 
            class="elder-input" 
            placeholder="请输入姓名" 
            placeholder-class="placeholder-text"
          />
        </view>
      </view>

      <!-- 2. 年龄 -->
      <view class="field-item">
        <text class="field-label">年龄</text>
        <view class="input-card">
          <input 
            v-model="age" 
            type="number" 
            class="elder-input" 
            placeholder="请输入年龄" 
            placeholder-class="placeholder-text"
          />
        </view>
      </view>

      <!-- 3. 电话号码 -->
      <view class="field-item">
        <text class="field-label">电话号码</text>
        <view class="input-card">
          <input 
            v-model="phone" 
            type="number" 
            class="elder-input" 
            placeholder="请输入电话号码" 
            placeholder-class="placeholder-text"
            maxlength="11"
          />
        </view>
      </view>

      <!-- 确定大蓝按钮 (原型样式) -->
      <view class="submit-action-wrap">
        <button class="confirm-btn" hover-class="btn-hover" @tap="handleConfirm">
          确 定
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useChatStore } from '../../stores/chat';
import { useSettingStore } from '../../stores/setting';

const chatStore = useChatStore();
const settingStore = useSettingStore();

const name = ref('');
const age = ref('');
const phone = ref('');

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const handleConfirm = () => {
  if (!name.value.trim()) {
    uni.showToast({ title: '请输入联系人姓名', icon: 'none' });
    return;
  }
  if (!phone.value.trim()) {
    uni.showToast({ title: '请输入电话号码', icon: 'none' });
    return;
  }

  chatStore.addContact({
    name: name.value.trim(),
    phone: phone.value.trim(),
    tag: '@邻里互助'
  });

  uni.showToast({ title: '联系人添加成功', icon: 'success' });
  setTimeout(() => {
    uni.navigateBack({ delta: 1 });
  }, 500);
};
</script>

<style lang="scss" scoped>
.add-contact-page {
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
  padding: 24rpx 0 40rpx 0;
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

.form-body {
  margin-top: 10rpx;
  display: flex;
  flex-direction: column;
  gap: 36rpx;
}

.field-item {
  display: flex;
  flex-direction: column;
}

.field-label {
  font-size: calc(34rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1F2329;
  margin-bottom: 16rpx;
}

.input-card {
  background-color: #FFFFFF;
  border: 3rpx solid #1B65F0;
  border-radius: 40rpx;
  height: calc(108rpx * var(--font-scale, 1));
  padding: 0 36rpx;
  display: flex;
  align-items: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);

  .elder-input {
    width: 100%;
    font-size: calc(34rpx * var(--font-scale, 1));
    font-weight: 700;
    color: #1F2329;
  }
}

.placeholder-text {
  color: #B0B5BD;
  font-size: calc(32rpx * var(--font-scale, 1));
}

.submit-action-wrap {
  margin-top: 40rpx;
}

.confirm-btn {
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
