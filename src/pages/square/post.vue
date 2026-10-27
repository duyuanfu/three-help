<template>
  <view class="post-page-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航栏 (严格对齐发布页.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">发布帖子</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 帖子内容输入区域 (大白卡片) -->
    <view class="input-card-wrap">
      <textarea 
        v-model="postContent" 
        class="post-textarea" 
        placeholder="请输入内容..." 
        placeholder-class="placeholder-text"
        maxlength="300"
      />
    </view>

    <!-- 蓝色发布大按钮 -->
    <view class="submit-action-wrap">
      <button class="submit-post-btn" hover-class="btn-hover" @tap="handlePublish">
        发 布
      </button>
    </view>

    <!-- 底部语音求助条 (原型样式) -->
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
import { useSquareStore } from '../../stores/square';
import { useSettingStore } from '../../stores/setting';
import { showVoiceUnsupported } from '../../utils/voice';

const squareStore = useSquareStore();
const settingStore = useSettingStore();

const postContent = ref('');

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const handlePublish = () => {
  if (!postContent.value.trim()) {
    uni.showToast({ title: '请输入帖子内容', icon: 'none' });
    return;
  }

  squareStore.createPost(postContent.value.trim());

  // 跳转发布成功页 (原型：发布成功.png)
  uni.redirectTo({ url: '/pages/square/post-success' });
};
</script>

<style lang="scss" scoped>
.post-page-container {
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
  padding: 20rpx 0 30rpx 0;
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

.input-card-wrap {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 32rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 40rpx;
}

.post-textarea {
  width: 100%;
  height: 400rpx;
  font-size: calc(34rpx * var(--font-scale, 1));
  color: #1F2329;
  line-height: 1.6;
}

.placeholder-text {
  color: #B0B5BD;
  font-size: calc(32rpx * var(--font-scale, 1));
}

.submit-action-wrap {
  margin-bottom: auto;
}

.submit-post-btn {
  width: 100%;
  height: calc(108rpx * var(--font-scale, 1));
  line-height: calc(108rpx * var(--font-scale, 1));
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(44rpx * var(--font-scale, 1));
  font-weight: 900;
  border-radius: 32rpx;
  letter-spacing: 8rpx;
  box-shadow: 0 10rpx 28rpx rgba(27, 101, 240, 0.35);
  border: none;

  &::after {
    border: none;
  }
}

.bottom-voice-wrap {
  margin-top: 40rpx;
  margin-bottom: 30rpx;
}

.voice-bar {
  background-color: #EBF2FF;
  border: 3rpx solid #BCD6FF;
  border-radius: 50rpx;
  height: calc(96rpx * var(--font-scale, 1));
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
    font-size: calc(32rpx * var(--font-scale, 1));
    font-weight: 800;
    color: #1B65F0;
  }
}

.btn-hover {
  opacity: 0.88;
}
</style>
