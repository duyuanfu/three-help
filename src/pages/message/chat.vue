<template>
  <view class="chat-page-container" :style="pageStyle">
    <!-- 顶部蓝色导航栏 (严格对齐聊天页原型) -->
    <view class="blue-header-nav">
      <view class="nav-left" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="chat-title">{{ targetUser.name }}</text>
      <view class="nav-right" @tap="showUserMoreInfo">
        <text class="more-text">更多</text>
      </view>
    </view>

    <!-- 关联订单提示条 (若从订单点入) -->
    <view v-if="relatedOrder" class="related-order-banner" @tap="goToOrderDetail">
      <text class="order-banner-tag">关联订单</text>
      <text class="order-banner-title">{{ relatedOrder.title }} · {{ relatedOrder.appointTime }}</text>
      <text class="order-banner-arrow">→</text>
    </view>

    <!-- 聊天消息滚动区域 -->
    <scroll-view 
      scroll-y 
      class="message-scroll-list" 
      :scroll-top="scrollTop"
      :scroll-into-view="lastMessageId"
    >
      <view class="message-inner-wrap">
        <view 
          v-for="msg in messageList" 
          :key="msg.id" 
          :id="'msg_' + msg.id"
          class="msg-row"
          :class="msg.senderId === userStore.currentUser.id ? 'msg-row-right' : 'msg-row-left'"
        >
          <!-- 对方发的消息：头像在左 -->
          <image 
            v-if="msg.senderId !== userStore.currentUser.id" 
            :src="targetUser.avatar" 
            mode="aspectFill" 
            class="chat-avatar" 
          />

          <!-- 消息气泡 (原型白底黑边圆角框) -->
          <view class="bubble-box">
            <!-- 文本类型 -->
            <text v-if="msg.type === 'text'" class="bubble-text">{{ msg.content }}</text>
            
            <!-- 图片类型 -->
            <image 
              v-else-if="msg.type === 'image'" 
              :src="msg.content" 
              mode="aspectFit" 
              class="bubble-image" 
              @tap="previewImage(msg.content)"
            />

            <!-- 语音消息 -->
            <view v-else-if="msg.type === 'voice'" class="voice-message-wrap" @tap="playVoice(msg.id, msg.audioPath)">
              <image
                src="/static/chat/voice.png"
                mode="aspectFit"
                class="voice-bubble-icon"
                :class="{ playing: playingId === msg.id }"
              />
              <text class="voice-bubble-duration">{{ msg.duration || 1 }}″</text>
            </view>

            <!-- 语音/视频通话提示 -->
            <view v-else-if="msg.type === 'phone_call'" class="media-call-wrap">
              <text class="media-icon">📞</text>
              <text class="media-text">{{ msg.content || '通话已结束' }}</text>
            </view>

            <!-- 名片类型 -->
            <view v-else-if="msg.type === 'card'" class="card-message-wrap">
              <text class="card-title">🪪 个人名片</text>
              <text class="card-desc">{{ msg.content }}</text>
            </view>
          </view>

          <!-- 我发的消息：头像在右 -->
          <image 
            v-if="msg.senderId === userStore.currentUser.id" 
            :src="userStore.currentUser.avatar" 
            mode="aspectFill" 
            class="chat-avatar" 
          />
        </view>
      </view>
    </scroll-view>

    <!-- 底部输入栏 (与线上咨询页一致) -->
    <view class="bottom-input-bar">
      <!-- 语音模式切换 -->
      <view class="voice-toggle-btn" hover-class="btn-hover" @tap="toggleVoiceMode">
        <image
          v-if="!isVoiceMode"
          src="/static/chat/voice.png"
          mode="aspectFit"
          class="voice-toggle-img"
        />
        <view v-else class="keyboard-circle">
          <text class="keyboard-text">键盘</text>
        </view>
      </view>

      <!-- 输入框或按住说话 -->
      <view class="input-center-wrap">
        <input 
          v-if="!isVoiceMode"
          v-model="inputText" 
          class="chat-input" 
          confirm-type="send"
          :adjust-position="false"
          @focus="onInputFocus"
          @confirm="sendTextMessage"
        />
        <view 
          v-else 
          class="voice-hold-btn" 
          :class="{ recording: isRecording }"
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

      <!-- 右侧 + 按钮 (切换展开 6 大扩展功能) -->
      <view class="circle-btn" @tap="togglePlusDrawer">
        <text class="icon-text">+</text>
      </view>
    </view>

    <!-- 扩展多媒体面板 (严格对齐聊天页2.png 6 大图标) -->
    <view v-if="showPlusDrawer" class="expanded-drawer">
      <view class="drawer-grid">
        <view class="drawer-item" @tap="handleCamera">
          <view class="drawer-icon-box">
            <image src="/static/chat/camera.png" mode="aspectFit" class="drawer-icon-img" />
          </view>
          <text class="drawer-label">拍照</text>
        </view>

        <view class="drawer-item" @tap="handleAlbum">
          <view class="drawer-icon-box">
            <image src="/static/chat/album.png" mode="aspectFit" class="drawer-icon-img" />
          </view>
          <text class="drawer-label">相册</text>
        </view>

        <view class="drawer-item" @tap="handlePhoneCall">
          <view class="drawer-icon-box">
            <image src="/static/chat/voice-call.png" mode="aspectFit" class="drawer-icon-img" />
          </view>
          <text class="drawer-label">语音通话</text>
        </view>

        <view class="drawer-item" @tap="handleVideoCall">
          <view class="drawer-icon-box">
            <image src="/static/chat/video-call.png" mode="aspectFit" class="drawer-icon-img" />
          </view>
          <text class="drawer-label">视频通话</text>
        </view>

        <view class="drawer-item" @tap="handleFileSend">
          <view class="drawer-icon-box">
            <image src="/static/chat/file.png" mode="aspectFit" class="drawer-icon-img" />
          </view>
          <text class="drawer-label">文件</text>
        </view>

        <view class="drawer-item" @tap="handleCardSend">
          <view class="drawer-icon-box">
            <image src="/static/chat/card.png" mode="aspectFit" class="drawer-icon-img" />
          </view>
          <text class="drawer-label">名片</text>
        </view>
      </view>

      <!-- 原型底部标点快捷条 (， 。 ？ ！) -->
      <view class="quick-punct-bar">
        <view class="punct-key" @tap="appendPunct('，')">，</view>
        <view class="punct-key" @tap="appendPunct('。')">。</view>
        <view class="punct-key" @tap="appendPunct('？')">？</view>
        <view class="punct-key" @tap="appendPunct('！')">！</view>
        <button class="send-text-btn" @tap="sendTextMessage">发送</button>
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
import { ref, computed, nextTick, watch } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { useChatStore } from '../../stores/chat';
import { useUserStore } from '../../stores/user';
import { useOrderStore } from '../../stores/order';
import { useSettingStore } from '../../stores/setting';
import { useVoiceChat } from '../../composables/useVoiceChat';
import { useKeyboardHeight } from '../../composables/useKeyboardHeight';

const chatStore = useChatStore();
const userStore = useUserStore();
const orderStore = useOrderStore();
const settingStore = useSettingStore();

const sessionId = ref('sess_li');
const inputText = ref('');
const showPlusDrawer = ref(false);
const scrollTop = ref(0);
const lastMessageId = ref('');

const { keyboardHeight } = useKeyboardHeight();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale,
  paddingBottom: keyboardHeight.value + 'px'
}));

const currentSession = computed(() => {
  return chatStore.sessions.find((s) => s.sessionId === sessionId.value);
});

const targetUser = computed(() => {
  if (currentSession.value) {
    return currentSession.value.targetUser;
  }
  // 兜底：从通讯录读取第一个联系人，避免硬编码
  const first = chatStore.contacts[0];
  if (first) {
    return {
      id: first.id,
      name: first.name,
      avatar: first.avatar
    };
  }
  return { id: '', name: '', avatar: '' };
});

const messageList = computed(() => {
  return chatStore.messages[sessionId.value] || [];
});

const relatedOrder = computed(() => {
  return orderStore.orders.find((o) => o.status === 'in_progress');
});

onLoad((options) => {
  if (options && options.sessionId) {
    sessionId.value = options.sessionId;
    chatStore.currentSessionId = options.sessionId;
  }
  scrollToBottom();
});

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const scrollToBottom = () => {
  nextTick(() => {
    // 交替设置，确保键盘弹起或连续发消息时也会重新滚动到底部
    scrollTop.value = scrollTop.value === 99999 ? 99998 : 99999;
    if (messageList.value.length > 0) {
      lastMessageId.value = 'msg_' + messageList.value[messageList.value.length - 1].id;
    }
  });
};

watch(keyboardHeight, (height) => {
  if (height > 0) {
    setTimeout(scrollToBottom, 80);
  }
});

watch(
  () => messageList.value.length,
  () => {
    scrollToBottom();
  }
);

const onInputFocus = () => {
  showPlusDrawer.value = false;
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
    chatStore.sendMessage({
      type: 'voice',
      content: '',
      duration,
      audioPath,
      sessionId: sessionId.value
    });
    scrollToBottom();
  }
});

const sendTextMessage = () => {
  if (!inputText.value.trim()) return;
  chatStore.sendMessage({
    type: 'text',
    content: inputText.value.trim(),
    sessionId: sessionId.value
  });
  inputText.value = '';
  showPlusDrawer.value = false;
  uni.hideKeyboard();
  scrollToBottom();
};

const appendPunct = (char: string) => {
  inputText.value += char;
};

const togglePlusDrawer = () => {
  uni.hideKeyboard();
  showPlusDrawer.value = !showPlusDrawer.value;
};

const handleCamera = () => {
  chatStore.sendMessage({
    type: 'image',
    content: '/static/goods/egg.jpg',
    sessionId: sessionId.value
  });
  showPlusDrawer.value = false;
  scrollToBottom();
};

const handleAlbum = () => {
  chatStore.sendMessage({
    type: 'image',
    content: '/static/goods/tissue.jpg',
    sessionId: sessionId.value
  });
  showPlusDrawer.value = false;
  scrollToBottom();
};

const handlePhoneCall = () => {
  uni.showModal({
    title: '模拟语音通话',
    content: `正在向 ${targetUser.value.name} 发起语音呼叫...`,
    confirmText: '挂断',
    showCancel: false,
    success: () => {
      chatStore.sendMessage({
        type: 'phone_call',
        content: '语音通话 01分24秒',
        sessionId: sessionId.value
      });
      scrollToBottom();
    }
  });
};

const handleVideoCall = () => {
  uni.showModal({
    title: '模拟视频通话',
    content: `已接通与 ${targetUser.value.name} 的视频通话`,
    confirmText: '完成通话',
    showCancel: false,
    success: () => {
      chatStore.sendMessage({
        type: 'phone_call',
        content: '视频通话 03分50秒',
        sessionId: sessionId.value
      });
      scrollToBottom();
    }
  });
};

const handleFileSend = () => {
  chatStore.sendMessage({
    type: 'text',
    content: '📄 [门诊挂号病历卡.pdf] (已成功发送)',
    sessionId: sessionId.value
  });
  showPlusDrawer.value = false;
  scrollToBottom();
};

const handleCardSend = () => {
  chatStore.sendMessage({
    type: 'card',
    content: `${userStore.currentUser.name} · ${userStore.currentUser.address}`,
    sessionId: sessionId.value
  });
  showPlusDrawer.value = false;
  scrollToBottom();
};

const previewImage = (url: string) => {
  uni.previewImage({ urls: [url] });
};

const showUserMoreInfo = () => {
  uni.navigateTo({
    url: `/pages/message/contact-detail?id=${targetUser.value.id}`
  });
};

const goToOrderDetail = () => {
  uni.switchTab({ url: '/pages/order/index' });
};
</script>

<style lang="scss" scoped>
.chat-page-container {
  height: 100vh;
  box-sizing: border-box;
  overflow: hidden;
  background-color: #F6F7FB;
  display: flex;
  flex-direction: column;
}

/* 蓝色顶部栏 (原型蓝底白字) */
.blue-header-nav {
  height: 140rpx;
  flex-shrink: 0;
  background-color: #1B65F0;
  padding-top: 50rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-left: 28rpx;
  padding-right: 28rpx;
  color: #FFFFFF;
}

.back-text, .more-text {
  font-size: calc(34rpx * var(--font-scale, 1));
  font-weight: 700;
  color: #FFFFFF;
}

.chat-title {
  font-size: calc(44rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #FFFFFF;
  letter-spacing: 2rpx;
}

/* 订单卡片挂载栏 */
.related-order-banner {
  flex-shrink: 0;
  background-color: #EBF2FF;
  border-bottom: 2rpx solid #BCD6FF;
  padding: 12rpx 28rpx;
  display: flex;
  align-items: center;

  .order-banner-tag {
    background-color: #1B65F0;
    color: #FFFFFF;
    font-size: 22rpx;
    padding: 4rpx 10rpx;
    border-radius: 8rpx;
    margin-right: 14rpx;
    font-weight: 700;
  }

  .order-banner-title {
    flex: 1;
    font-size: calc(26rpx * var(--font-scale, 1));
    font-weight: 700;
    color: #1F2329;
  }

  .order-banner-arrow {
    font-size: 28rpx;
    color: #1B65F0;
    font-weight: 900;
  }
}

/* 消息流 */
.message-scroll-list {
  flex: 1;
  height: 0;
  min-height: 0;
  box-sizing: border-box;
  padding: 24rpx;
}

.message-inner-wrap {
  display: flex;
  flex-direction: column;
  gap: 36rpx;
}

.msg-row {
  display: flex;
  align-items: flex-start;
}

.msg-row-left {
  justify-content: flex-start;
}

.msg-row-right {
  justify-content: flex-end;
}

.chat-avatar {
  width: 90rpx;
  height: 90rpx;
  border-radius: 50%;
  flex-shrink: 0;
}

/* 气泡 (严格对齐原型白底黑边圆角框) */
.bubble-box {
  background-color: #FFFFFF;
  border: 3rpx solid #000000;
  border-radius: 32rpx;
  padding: 20rpx 28rpx;
  max-width: 500rpx;
  margin: 0 16rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.04);

  .bubble-text {
    font-size: calc(34rpx * var(--font-scale, 1));
    color: #000000;
    font-weight: 700;
    line-height: 1.5;
  }

  .bubble-image {
    width: 240rpx;
    height: 240rpx;
    border-radius: 16rpx;
  }

  .media-call-wrap {
    display: flex;
    align-items: center;

    .media-icon {
      font-size: 36rpx;
      margin-right: 10rpx;
    }

    .media-text {
      font-size: calc(30rpx * var(--font-scale, 1));
      font-weight: 700;
    }
  }

  .card-message-wrap {
    display: flex;
    flex-direction: column;

    .card-title {
      font-size: calc(30rpx * var(--font-scale, 1));
      font-weight: 900;
      color: #1B65F0;
      margin-bottom: 6rpx;
    }

    .card-desc {
      font-size: calc(28rpx * var(--font-scale, 1));
      color: #333333;
    }
  }
}

/* 语音消息气泡 */
.voice-message-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;
  min-width: 150rpx;
}

.voice-bubble-icon {
  width: 42rpx;
  height: 42rpx;
  background-color: #FFFFFF;
  border-radius: 50%;
  flex-shrink: 0;
}

.voice-bubble-icon.playing {
  animation: voicePulse 0.8s infinite ease-in-out;
}

.voice-bubble-duration {
  font-size: calc(32rpx * var(--font-scale, 1));
  font-weight: 800;
  color: #1F2329;
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

/* 底部操作条 */
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
    font-size: 40rpx;
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
    font-size: calc(32rpx * var(--font-scale, 1));
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
  font-size: calc(32rpx * var(--font-scale, 1));
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
  font-size: calc(30rpx * var(--font-scale, 1));
  font-weight: 800;
  color: #FFFFFF;
  text-align: center;
}

.recording-time {
  margin-top: 8rpx;
  font-size: calc(26rpx * var(--font-scale, 1));
  color: rgba(255, 255, 255, 0.85);
}

.btn-hover {
  opacity: 0.88;
}

/* 扩展面板 (6 大图标) */
.expanded-drawer {
  flex-shrink: 0;
  background-color: #F0F2F5;
  padding: 30rpx 20rpx;
  border-top: 2rpx solid #E5E7EB;
}

.drawer-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  row-gap: 30rpx;
  margin-bottom: 30rpx;
}

.drawer-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.drawer-icon-box {
  width: 140rpx;
  height: 140rpx;
  background-color: #FFFFFF;
  border-radius: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.05);

  .drawer-icon-img {
    width: 80rpx;
    height: 80rpx;
  }
}

.drawer-label {
  margin-top: 12rpx;
  font-size: calc(28rpx * var(--font-scale, 1));
  font-weight: 800;
  color: #1F2329;
}

/* 快捷标点条 */
.quick-punct-bar {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 10rpx 12rpx;
  background-color: #E2E4E8;
  border-radius: 20rpx;
}

.punct-key {
  flex: 1;
  height: 70rpx;
  background-color: #FFFFFF;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 38rpx;
  font-weight: 900;
  box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.08);
}

.send-text-btn {
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(30rpx * var(--font-scale, 1));
  font-weight: 900;
  height: 70rpx;
  line-height: 70rpx;
  padding: 0 24rpx;
  border-radius: 14rpx;

  &::after {
    border: none;
  }
}
</style>
