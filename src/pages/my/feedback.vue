<template>
  <view class="feedback-page-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航栏 -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">意见反馈</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 反馈表单 -->
    <view class="feedback-body">
      <view class="input-card">
        <textarea 
          v-model="feedbackContent" 
          class="feedback-textarea" 
          placeholder="请写下您在使用帮帮帮过程中的意见、建议或遇到的问题..." 
          placeholder-class="placeholder-text"
          maxlength="300"
        />
      </view>

      <view class="contact-card">
        <input 
          v-model="contactInfo" 
          class="contact-input" 
          placeholder="留下您的手机号（选填，方便工作人员向您致电回访）" 
          placeholder-class="placeholder-text"
        />
      </view>

      <!-- 提交按钮 -->
      <view class="action-wrap">
        <button class="submit-btn" hover-class="btn-hover" @tap="handleSubmit">
          提 交 反 馈
        </button>
      </view>
    </view>

    <!-- 底部语音入口 (原型样式，暂未开放) -->
    <view class="bottom-voice-wrap">
      <view class="voice-bar" hover-class="btn-hover" @tap="showVoiceUnsupported">
        <image src="/static/common/voice-mic.png" mode="aspectFit" class="mic-img" />
        <text class="voice-text">不方便打字？按住说话</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useSettingStore } from '../../stores/setting';
import { showVoiceUnsupported } from '../../utils/voice';

const settingStore = useSettingStore();
const feedbackContent = ref('');
const contactInfo = ref('');

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const handleSubmit = () => {
  if (!feedbackContent.value.trim()) {
    uni.showToast({ title: '请填写反馈内容', icon: 'none' });
    return;
  }
  uni.showModal({
    title: '感谢您的反馈',
    content: '您的意见我们已仔细记录，我们将持续优化适老化与互助体验！',
    showCancel: false,
    confirmText: '确定',
    confirmColor: '#1B65F0',
    success: () => {
      uni.navigateBack({ delta: 1 });
    }
  });
};
</script>

<style lang="scss" scoped>
.feedback-page-container {
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

.back-text {
  font-size: calc(34rpx * var(--font-scale));
  color: #1B65F0;
  font-weight: 700;
}

.page-title {
  font-size: calc(52rpx * var(--font-scale));
  font-weight: 900;
  color: #1B65F0;
  letter-spacing: 4rpx;
}

.nav-right-placeholder {
  width: 90rpx;
}

.feedback-body {
  margin-top: 20rpx;
}

.input-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 30rpx;
}

.feedback-textarea {
  width: 100%;
  height: 360rpx;
  font-size: calc(32rpx * var(--font-scale));
  color: #1F2329;
  line-height: 1.6;
}

.contact-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 0 32rpx;
  height: calc(100rpx * var(--font-scale));
  display: flex;
  align-items: center;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 50rpx;
}

.contact-input {
  width: 100%;
  font-size: calc(30rpx * var(--font-scale));
  color: #1F2329;
}

.placeholder-text {
  color: #B0B5BD;
  font-size: calc(30rpx * var(--font-scale));
}

.submit-btn {
  width: 100%;
  height: calc(108rpx * var(--font-scale));
  line-height: calc(108rpx * var(--font-scale));
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(44rpx * var(--font-scale));
  font-weight: 900;
  border-radius: 36rpx;
  letter-spacing: 6rpx;
  box-shadow: 0 10rpx 28rpx rgba(27, 101, 240, 0.35);
  border: none;

  &::after {
    border: none;
  }
}

.bottom-voice-wrap {
  margin-top: auto;
  padding-top: 40rpx;
  padding-bottom: 20rpx;
}

.voice-bar {
  background-color: #EBF2FF;
  border: 3rpx solid #BCD6FF;
  border-radius: 50rpx;
  height: calc(96rpx * var(--font-scale));
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(27, 101, 240, 0.1);

  .mic-img {
    width: 44rpx;
    height: 44rpx;
    margin-right: 14rpx;
  }

  .voice-text {
    font-size: calc(32rpx * var(--font-scale));
    font-weight: 800;
    color: #1B65F0;
  }
}

.btn-hover {
  opacity: 0.88;
}
</style>
