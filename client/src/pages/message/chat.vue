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
            :src="av(targetUser.avatar)" 
            mode="aspectFill" 
            class="chat-avatar" 
            @error="onAvatarError(targetUser.avatar)"
          />

          <!-- 消息气泡 (原型白底黑边圆角框) -->
          <view class="bubble-box">
            <!-- 文本类型 -->
            <text v-if="msg.type === 'text'" class="bubble-text">{{ msg.content }}</text>
            
            <!-- 图片类型 -->
            <image 
              v-else-if="msg.type === 'image'" 
              :src="netImage(resolveAssetUrl(msg.content))" 
              mode="aspectFit" 
              class="bubble-image" 
              @tap="previewImage(resolveAssetUrl(msg.content))"
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
            :src="av(userStore.currentUser.avatar)" 
            mode="aspectFill" 
            class="chat-avatar" 
            @error="onAvatarError(userStore.currentUser.avatar)"
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

      <!-- 抽屉底部发送按钮（原标点快捷键已移除） -->
      <view class="quick-punct-bar">
        <button class="send-text-btn" @tap="sendTextMessage">发 送</button>
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
import { netImage } from '../../utils/netImage';
import { ref, computed, nextTick, watch } from 'vue';
import { onLoad, onUnload, onShow, onHide } from '@dcloudio/uni-app';
import { useChatStore, stripPunctuation } from '../../stores/chat';
import { useUserStore } from '../../stores/user';
import { useOrderStore } from '../../stores/order';
import { useSettingStore } from '../../stores/setting';
import { useVoiceChat } from '../../composables/useVoiceChat';
import { chooseImages, avatarSrc, LOCAL_DEFAULT_AVATAR } from '../../utils/avatar';
import { resolveAssetUrl } from '../../utils/token';
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

/**
 * 头像加载失败兜底：记录失败过的地址，改渲染包内默认图。
 * 真机网络不稳定或头像文件缺失时不至于留一片空白。
 */
const failedAvatars = ref<Record<string, boolean>>({});
const av = (url?: string): string =>
  failedAvatars.value[url || ''] ? LOCAL_DEFAULT_AVATAR : avatarSrc(url);
const onAvatarError = (url?: string) => {
  failedAvatars.value = { ...failedAvatars.value, [url || '']: true };
};

const { keyboardHeight } = useKeyboardHeight();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale,
  paddingBottom: keyboardHeight.value + 'px'
}));

const currentSession = computed(() => {
  return chatStore.sessions.find((s) => s.sessionId === sessionId.value);
});

const targetUser = computed(() => {
  // 注意：会话的 targetUser 可能为 null（对方账号已不存在），
  // 不能只判断 currentSession 本身，否则模板读 targetUser.name 会报错
  if (currentSession.value && currentSession.value.targetUser) {
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
  return {
    id: '',
    name: '聊天',
    avatar: resolveAssetUrl('/uploads/default/avatar.jpg')
  };
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

/** 拉取当前会话消息（进入页面 / 轮询） */
const refreshMessages = async () => {
  try {
    await chatStore.loadMessages(sessionId.value);
    scrollToBottom();
  } catch (e) {
    console.error('[chat] 消息加载失败', e);
  }
};

/** 对方发来的新消息靠轮询获取（小程序无长连接，3 秒一次足够） */
let pollTimer: ReturnType<typeof setInterval> | null = null;
const startPolling = () => {
  stopPolling();
  pollTimer = setInterval(async () => {
    if (!sessionId.value) return;
    try {
      // 只拉增量：带 since 后每次只传回新增的几条，不再整段历史重传
      const added = await chatStore.pollNewMessages(sessionId.value);
      if (added > 0) {
        chatStore.markRead(sessionId.value);
        scrollToBottom();
      }
    } catch (e) {
      // 轮询失败静默
    }
  }, 3000);
};

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
};

onUnload(() => {
  stopPolling();
});

/** 页面被其他页面盖住/切到后台时暂停轮询，回来再续上，避免后台空跑耗电耗流量 */
onHide(() => {
  stopPolling();
});

/** 首次进入也会触发，正好承担「拉消息 + 标已读 + 开始轮询」的全部工作 */
onShow(() => {
  if (!sessionId.value) return;
  refreshMessages();
  chatStore.markRead(sessionId.value);
  startPolling();
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
    chatStore
      .sendMessage({
        type: 'voice',
        content: '',
        duration,
        audioPath,
        sessionId: sessionId.value
      })
      .catch((e: any) => {
        uni.showToast({ title: e?.message || '发送失败', icon: 'none' });
      });
    scrollToBottom();
  }
});

const sendTextMessage = async () => {
  // 私信不保留标点：，。？！ 发送前先清掉
  const text = stripPunctuation(inputText.value);
  if (!text) {
    if (inputText.value.trim()) {
      uni.showToast({ title: '消息内容不能全是标点符号', icon: 'none', duration: 1800 });
    }
    return;
  }
  inputText.value = '';
  showPlusDrawer.value = false;
  uni.hideKeyboard();
  try {
    await chatStore.sendMessage({
      type: 'text',
      content: text,
      sessionId: sessionId.value
    });
    scrollToBottom();
  } catch (e: any) {
    // 失败时把内容还给输入框，避免用户白打字
    inputText.value = text;
    uni.showToast({ title: e?.message || '发送失败', icon: 'none', duration: 2000 });
  }
};

const togglePlusDrawer = () => {
  uni.hideKeyboard();
  showPlusDrawer.value = !showPlusDrawer.value;
};

/** 拍照 / 相册：真实上传到服务器后发送图片消息 */
const sendImageMessage = async (source: 'camera' | 'album') => {
  showPlusDrawer.value = false;
  const urls = await chooseImages(1);
  if (!urls.length) return;
  try {
    await chatStore.sendMessage({
      type: 'image',
      content: urls[0],
      sessionId: sessionId.value
    });
    scrollToBottom();
  } catch (e: any) {
    uni.showToast({ title: e?.message || '图片发送失败', icon: 'none' });
  }
};

const handleCamera = () => sendImageMessage('camera');

const handleAlbum = () => sendImageMessage('album');

const handlePhoneCall = () => {
  uni.showToast({ title: '语音通话暂未接入', icon: 'none', duration: 2000 });
};

const handleVideoCall = () => {
  uni.showToast({ title: '视频通话暂未接入', icon: 'none', duration: 2000 });
};

const handleFileSend = () => {
  uni.showToast({ title: '文件发送暂未接入', icon: 'none', duration: 2000 });
  showPlusDrawer.value = false;
};

const handleCardSend = () => {
  chatStore
    .sendMessage({
      type: 'card',
      content: `${userStore.currentUser.name} · ${userStore.currentUser.address}`,
      sessionId: sessionId.value
    })
    .catch((e: any) => {
      uni.showToast({ title: e?.message || '发送失败', icon: 'none' });
    });
  showPlusDrawer.value = false;
  scrollToBottom();
};

const previewImage = (url: string) => {
  // 大图预览同样不能直连 http，优先用已下载到本地的副本
  uni.previewImage({ urls: [netImage(url) || url] });
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

/* 抽屉底部发送条 */
.quick-punct-bar {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 10rpx 12rpx;
  background-color: #E2E4E8;
  border-radius: 20rpx;
}

.send-text-btn {
  flex: 1;
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(32rpx * var(--font-scale, 1));
  font-weight: 900;
  height: 80rpx;
  line-height: 80rpx;
  padding: 0 24rpx;
  border-radius: 14rpx;
  letter-spacing: 4rpx;

  &::after {
    border: none;
  }
}
</style>
