<template>
  <view class="consult-page-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航栏 (严格对齐线上咨询.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">线上咨询</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 消息对话区 -->
    <scroll-view scroll-y class="consult-scroll" :scroll-top="scrollTop">
      <!-- 初始官方助手问候卡片 (原型样式) -->
      <view class="welcome-row">
        <image src="/static/consult/service-avatar.png" mode="aspectFill" class="chat-avatar" />
        <view class="assistant-welcome-card">
          <text class="welcome-text">
            您好，我是帮帮帮小助手，能够为您解答在使用帮帮帮过程中出现的问题，快来和我对话吧！
          </text>
        </view>
      </view>

      <!-- 转接人工服务灰色提示 (原型样式) -->
      <view class="service-transfer-tip">
        <text class="tip-text">正在为您转接人工服务...</text>
      </view>

      <!-- 对话流 -->
      <view 
        v-for="msg in messageList" 
        :key="msg.id" 
        class="msg-item"
        :class="msg.isUser ? 'msg-user' : 'msg-bot'"
      >
        <image
          v-if="!msg.isUser"
          src="/static/consult/service-avatar.png"
          mode="aspectFill"
          class="chat-avatar"
        />

        <view
          class="msg-bubble"
          :class="{ 'voice-bubble': msg.type === 'voice', playing: playingId === msg.id }"
          @tap="onMessageTap(msg)"
        >
          <view v-if="msg.type === 'voice'" class="voice-msg">
            <image src="/static/consult/voice.png" mode="aspectFit" class="voice-msg-icon" />
            <text class="voice-msg-duration">{{ msg.duration }}″</text>
          </view>
          <text v-else class="msg-text">{{ msg.text }}</text>
        </view>

        <image
          v-if="msg.isUser"
          :src="userStore.currentUser.avatar"
          mode="aspectFill"
          class="chat-avatar"
        />
      </view>
    </scroll-view>

    <!-- 底部输入栏 (语音交互参考微信) -->
    <view class="bottom-input-bar">
      <view class="voice-toggle-btn" hover-class="btn-hover" @tap="toggleVoiceMode">
        <image
          v-if="!isVoiceMode"
          src="/static/consult/voice.png"
          mode="aspectFit"
          class="voice-toggle-img"
        />
        <view v-else class="keyboard-circle">
          <text class="keyboard-text">键盘</text>
        </view>
      </view>

      <view class="input-center-wrap">
        <input 
          v-if="!isVoiceMode"
          v-model="inputText" 
          class="chat-input" 
          placeholder="请输入您要咨询的问题..."
          confirm-type="send"
          :adjust-position="false"
          @focus="onInputFocus"
          @confirm="sendConsultMessage"
        />
        <view
          v-else
          class="voice-hold-btn"
          :class="{ 'recording': isRecording }"
          @touchstart="startVoiceRecord"
          @touchmove.stop.prevent="onVoiceMove"
          @touchend="endVoiceRecord"
          @touchcancel="cancelVoiceRecord"
        >
          <text class="voice-hold-text">
            {{ isRecording ? (isCancelling ? '松开取消' : '松开发送') : '按住 说话' }}
          </text>
        </view>
      </view>

      <view class="circle-btn" @tap="sendConsultMessage">
        <text class="icon-text">+</text>
      </view>
    </view>

    <!-- 录音浮层 (微信风格) -->
    <view v-if="isRecording" class="recording-panel" :class="{ 'cancel-state': isCancelling }">
      <view class="wave-row">
        <view v-for="n in 5" :key="n" class="wave-bar"></view>
      </view>
      <text class="recording-text">{{ isCancelling ? '松开取消' : '松开发送，上滑取消' }}</text>
      <text class="recording-time">{{ recordDuration }}″</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue';
import { useSettingStore } from '../../stores/setting';
import { useUserStore } from '../../stores/user';
import { useVoiceChat } from '../../composables/useVoiceChat';
import { useKeyboardHeight } from '../../composables/useKeyboardHeight';

const settingStore = useSettingStore();
const userStore = useUserStore();
const inputText = ref('');
const scrollTop = ref(0);

interface ConsultMessage {
  id: string;
  isUser: boolean;
  type: 'text' | 'voice';
  text?: string;
  duration?: number;
  audioPath?: string;
}

const messageList = ref<ConsultMessage[]>([]);

const nextMessageId = () => {
  return 'consult_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6);
};

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const scrollToBottom = () => {
  nextTick(() => {
    // 交替设置，确保键盘弹起或连续发消息时也会重新滚动到底部
    scrollTop.value = scrollTop.value === 99999 ? 99998 : 99999;
  });
};

const { keyboardHeight } = useKeyboardHeight((height) => {
  if (height > 0) {
    setTimeout(scrollToBottom, 80);
  }
});

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale,
  paddingBottom: keyboardHeight.value + 'px'
}));

const onInputFocus = () => {
  setTimeout(scrollToBottom, 100);
};

const {
  isVoiceMode,
  isRecording,
  isCancelling,
  recordDuration,
  playingId,
  toggleVoiceMode,
  startVoiceRecord,
  onVoiceMove,
  endVoiceRecord,
  cancelVoiceRecord,
  playVoice
} = useVoiceChat({
  onSend: ({ duration, audioPath }) => {
    messageList.value.push({
      id: nextMessageId(),
      isUser: true,
      type: 'voice',
      duration,
      audioPath
    });
    scrollToBottom();

    setTimeout(() => {
      messageList.value.push({
        id: nextMessageId(),
        isUser: false,
        type: 'text',
        text: '已收到您的语音，正在为您转接人工服务，请稍候...'
      });
      scrollToBottom();
    }, 800);
  }
});

const onMessageTap = (msg: ConsultMessage) => {
  if (msg.type === 'voice') {
    playVoice(msg.id, msg.audioPath);
  }
};

const sendConsultMessage = () => {
  if (!inputText.value.trim()) return;
  const userText = inputText.value.trim();
  messageList.value.push({
    id: nextMessageId(),
    isUser: true,
    type: 'text',
    text: userText
  });
  inputText.value = '';

  scrollToBottom();

  setTimeout(() => {
    let reply = '您好，帮帮帮人工客服已为您接入！请问有什么具体问题可以帮到您？';
    if (userText.includes('金币') || userText.includes('鸡蛋') || userText.includes('兑换')) {
      reply = '阿姨您好！您可以在底部点击【广场】，然后点击黄色大卡片【兑换】，选中鸡蛋点击兑换加入购物车，最后在购物车点击“确定兑换”就可以包邮送到家啦！';
    } else if (userText.includes('下单') || userText.includes('就医')) {
      reply = '叔叔阿姨您好！在首页点击【出行就医】或者直接按住底部的语音条说出需求，附近的青年志愿者就会接单上门陪同您看病哦！';
    }
    messageList.value.push({
      id: nextMessageId(),
      isUser: false,
      type: 'text',
      text: reply
    });
    scrollToBottom();
  }, 800);
};
</script>

<style lang="scss" scoped>
.consult-page-container {
  height: 100vh;
  box-sizing: border-box;
  overflow: hidden;
  background-color: #F6F7FB;
  display: flex;
  flex-direction: column;
}

.status-bar {
  height: 60rpx;
  flex-shrink: 0;
}

.header-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 36rpx 28rpx 36rpx;
  flex-shrink: 0;
}

.back-btn {
  padding: 10rpx 0;
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

.consult-scroll {
  flex: 1;
  height: 0;
  min-height: 0;
  box-sizing: border-box;
  padding: 24rpx 36rpx;
}

/* 客服头像 */
.chat-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  flex-shrink: 0;
  background-color: #E8EEF9;
}

.welcome-row {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

/* 官方助手欢迎大卡片 (严格对齐线上咨询.png) */
.assistant-welcome-card {
  flex: 1;
  max-width: 560rpx;
  background-color: #FFFFFF;
  border: 3rpx solid #E5E7EB;
  border-radius: 36rpx;
  padding: 36rpx 32rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);

  .welcome-text {
    font-size: calc(34rpx * var(--font-scale));
    font-weight: 800;
    color: #1F2329;
    line-height: 1.6;
  }
}

.service-transfer-tip {
  text-align: center;
  margin-bottom: 36rpx;

  .tip-text {
    font-size: calc(28rpx * var(--font-scale));
    color: #8F959E;
    font-weight: 600;
  }
}

.msg-item {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  margin-bottom: 28rpx;

  &.msg-user {
    justify-content: flex-end;
    .msg-bubble {
      background-color: #1B65F0;
      color: #FFFFFF;
      border-color: #1B65F0;
      .msg-text {
        color: #FFFFFF;
      }
    }
  }

  &.msg-bot {
    justify-content: flex-start;
    .msg-bubble {
      background-color: #FFFFFF;
      color: #1F2329;
      border: 3rpx solid #000000;
    }
  }
}

.msg-bubble {
  border-radius: 32rpx;
  padding: 24rpx 30rpx;
  max-width: 500rpx;
  box-shadow: 0 4rpx 14rpx rgba(0, 0, 0, 0.04);

  .msg-text {
    font-size: calc(32rpx * var(--font-scale));
    font-weight: 700;
    line-height: 1.5;
  }
}

/* 语音消息气泡 */
.voice-msg {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.voice-msg-icon {
  width: 42rpx;
  height: 42rpx;
  background-color: #FFFFFF;
  border-radius: 50%;
  flex-shrink: 0;
}

.voice-msg-duration {
  font-size: calc(32rpx * var(--font-scale));
  font-weight: 800;
}

.msg-bubble.playing .voice-msg-icon {
  animation: voicePulse 0.8s infinite ease-in-out;
}

@keyframes voicePulse {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.16);
  }
}

/* 底部输入栏 */
.bottom-input-bar {
  flex-shrink: 0;
  background-color: #F0F2F5;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom)) 24rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  border-top: 2rpx solid #E5E7EB;
}

/* 语音/键盘切换按钮 */
.voice-toggle-btn {
  width: 76rpx;
  height: 76rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.voice-toggle-img {
  width: 76rpx;
  height: 76rpx;
}

.keyboard-circle {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  border: 3rpx solid #000000;
  background-color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
}

.keyboard-text {
  font-size: 24rpx;
  font-weight: 800;
  color: #000000;
}

.circle-btn {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  border: 3rpx solid #000000;
  background-color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;

  .icon-text {
    font-size: 38rpx;
    font-weight: 900;
    color: #000000;
  }
}

.input-center-wrap {
  flex: 1;

  .chat-input {
    background-color: #FFFFFF;
    border-radius: 36rpx;
    height: 76rpx;
    padding: 0 24rpx;
    font-size: calc(30rpx * var(--font-scale));
    color: #1F2329;
  }
}

/* 微信风格「按住说话」 */
.voice-hold-btn {
  width: 100%;
  height: 76rpx;
  background-color: #FFFFFF;
  border: 2rpx solid #E5E7EB;
  border-radius: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;

  &.recording {
    background-color: #E8F0FF;
    border-color: #1B65F0;
  }
}

.voice-hold-text {
  font-size: calc(30rpx * var(--font-scale));
  font-weight: 800;
  color: #1F2329;
}

/* 录音浮层 */
.recording-panel {
  position: fixed;
  left: 50%;
  bottom: 240rpx;
  transform: translateX(-50%);
  z-index: 999;
  width: 360rpx;
  padding: 30rpx 24rpx;
  background-color: rgba(0, 0, 0, 0.78);
  border-radius: 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 12rpx 36rpx rgba(0, 0, 0, 0.25);
  transition: background-color 0.15s;

  &.cancel-state {
    background-color: rgba(200, 40, 40, 0.88);
  }
}

.wave-row {
  height: 56rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-bottom: 16rpx;
}

.wave-bar {
  width: 8rpx;
  height: 44rpx;
  border-radius: 4rpx;
  background-color: #FFFFFF;
  animation: wave 0.9s infinite ease-in-out;

  &:nth-child(1) {
    height: 20rpx;
    animation-delay: 0s;
  }

  &:nth-child(2) {
    height: 34rpx;
    animation-delay: 0.12s;
  }

  &:nth-child(3) {
    height: 48rpx;
    animation-delay: 0.24s;
  }

  &:nth-child(4) {
    height: 34rpx;
    animation-delay: 0.36s;
  }

  &:nth-child(5) {
    height: 20rpx;
    animation-delay: 0.48s;
  }
}

@keyframes wave {
  0%,
  100% {
    transform: scaleY(0.45);
  }
  50% {
    transform: scaleY(1);
  }
}

.recording-text {
  font-size: calc(30rpx * var(--font-scale));
  font-weight: 800;
  color: #FFFFFF;
  text-align: center;
}

.recording-time {
  margin-top: 8rpx;
  font-size: calc(26rpx * var(--font-scale));
  color: rgba(255, 255, 255, 0.85);
}

/* 快捷标点已移除，底部只保留输入栏 */
.btn-hover {
  opacity: 0.88;
}
</style>
