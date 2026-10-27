<template>
  <view class="font-size-page-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航栏 (严格对齐字体大小.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">字体大小</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 对话预览区域 (实时响应字号滑块变化) -->
    <view class="preview-chat-section">
      <!-- 对方发的消息 -->
      <view class="preview-msg-row left">
        <image :src="previewOtherAvatar" mode="aspectFill" class="preview-avatar" />
        <view class="preview-bubble">
          <text class="bubble-content">杨老太，明天晚上做我的舞伴去跳广场舞啊。</text>
        </view>
      </view>

      <!-- 我发的消息 -->
      <view class="preview-msg-row right">
        <view class="preview-bubble">
          <text class="bubble-content">好啊，先这样约好</text>
        </view>
        <image :src="previewMyAvatar" mode="aspectFill" class="preview-avatar" />
      </view>
    </view>

    <!-- 字体大小滑动调节卡片 (严格对齐原型) -->
    <view class="slider-control-card">
      <!-- 6 个递增的 B 字母标识 -->
      <view class="ticks-letter-row">
        <text 
          v-for="(tick, idx) in fontLevels" 
          :key="idx" 
          class="tick-b"
          :style="{ fontSize: tick.bSize }"
          :class="{ 'tick-b-active': currentLevel === idx }"
          @tap="selectLevel(idx)"
        >
          B
        </text>
      </view>

      <!-- 原生滑块组件 -->
      <view class="slider-bar-wrap">
        <slider 
          :value="currentLevel" 
          :min="0" 
          :max="3" 
          :step="1"
          activeColor="#B0B5BD" 
          backgroundColor="#E5E7EB" 
          block-color="#FFFFFF"
          block-size="28"
          @change="onSliderChange"
        />
      </view>

      <!-- 底部档位标签 (小 / 标准 / 大) -->
      <view class="ticks-label-row">
        <text class="label-item" @tap="selectLevel(0)">小</text>
        <text class="label-item label-center" @tap="selectLevel(1)">标准</text>
        <text class="label-item label-right" @tap="selectLevel(2)">大</text>
      </view>
    </view>

    <!-- 底部蓝色完成大按钮 -->
    <view class="bottom-action-wrap">
      <button class="complete-btn" hover-class="btn-hover" @tap="handleSave">
        完 成
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useSettingStore } from '../../stores/setting';
import { useUserStore } from '../../stores/user';
import { useChatStore } from '../../stores/chat';

const settingStore = useSettingStore();
const userStore = useUserStore();
const chatStore = useChatStore();
const currentLevel = ref(settingStore.fontScaleLevel);

// 预览头像：本人头像从 db 读取，对方头像取通讯录第一人（避免硬编码）
const previewMyAvatar = computed(() => userStore.currentUser.avatar);
const previewOtherAvatar = computed(() => chatStore.contacts[0]?.avatar || '');

const fontLevels = [
  { bSize: '32rpx', scale: 0.85 },
  { bSize: '38rpx', scale: 1.0 },
  { bSize: '46rpx', scale: 1.15 },
  { bSize: '54rpx', scale: 1.3 }
];

const pageStyle = computed(() => {
  const scale = fontLevels[currentLevel.value]?.scale || 1.0;
  return {
    '--font-scale': scale
  };
});

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const selectLevel = (level: number) => {
  currentLevel.value = level;
};

const onSliderChange = (e: any) => {
  currentLevel.value = e.detail.value;
};

const handleSave = () => {
  settingStore.setFontScaleLevel(currentLevel.value);
  uni.showToast({ title: '字号设置已生效', icon: 'success' });
  setTimeout(() => {
    uni.navigateBack({ delta: 1 });
  }, 500);
};
</script>

<style lang="scss" scoped>
.font-size-page-container {
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

/* 预览对话 (严格对齐原型) */
.preview-chat-section {
  flex: 1;
  padding-top: 40rpx;
  display: flex;
  flex-direction: column;
  gap: 36rpx;
}

.preview-msg-row {
  display: flex;
  align-items: flex-start;

  &.left {
    justify-content: flex-start;
  }

  &.right {
    justify-content: flex-end;
  }
}

.preview-avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

.preview-bubble {
  background-color: #FFFFFF;
  border: 3rpx solid #000000;
  border-radius: 32rpx;
  padding: 24rpx 28rpx;
  max-width: 480rpx;
  margin: 0 16rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.04);

  .bubble-content {
    font-size: calc(34rpx * var(--font-scale, 1));
    color: #000000;
    font-weight: 700;
    line-height: 1.5;
  }
}

/* 滑块卡片 (原型白底圆角) */
.slider-control-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 28rpx 36rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 50rpx;
}

.ticks-letter-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 0 20rpx;
  margin-bottom: 10rpx;

  .tick-b {
    font-weight: 900;
    color: #1F2329;
    cursor: pointer;
  }

  .tick-b-active {
    color: #1B65F0;
  }
}

.slider-bar-wrap {
  margin: 0 -10rpx;
}

.ticks-label-row {
  display: flex;
  justify-content: space-between;
  padding: 0 20rpx;

  .label-item {
    font-size: calc(30rpx * var(--font-scale, 1));
    font-weight: 800;
    color: #1F2329;
  }

  .label-center {
    margin-left: 8rpx;
  }
}

/* 底部完成按钮 */
.bottom-action-wrap {
  margin-bottom: 30rpx;
}

.complete-btn {
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
