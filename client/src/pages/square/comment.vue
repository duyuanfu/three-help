<template>
  <view class="comment-page-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航栏 (严格对齐评论页1.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">评 论</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 帖子与评论大卡片 (严格对齐评论页1.png) -->
    <view class="post-comment-box">
      <!-- 帖子作者信息 -->
      <view class="post-top-row">
        <view class="author-left">
          <image :src="avatarSrc(post.authorAvatar)" mode="aspectFill" class="author-avatar" />
          <view class="author-meta">
            <text class="author-name">{{ post.authorName }}</text>
            <text class="author-time">{{ post.timeAgo }}</text>
          </view>
        </view>
        <button v-if="!isMine && !isFriend(post.authorName)" class="add-friend-pill" hover-class="btn-hover" @tap="handleAddAuthor">
          加好友
        </button>
        <button v-else-if="!isMine && isFriend(post.authorName)" class="add-friend-pill friend-added" disabled>
          已添加
        </button>
        <view v-else class="delete-post-btn" hover-class="btn-hover" @tap="handleDeletePost">
          <view class="delete-icon"></view>
          <text class="delete-text">删除</text>
        </view>
      </view>

      <!-- 帖子正文 -->
      <view class="post-content-wrap">
        <text class="post-content-text">{{ post.content }}</text>
      </view>

      <!-- 卡片内部分割线 -->
      <view class="card-divider-line"></view>

      <!-- 评论列表 (真实数据：按 postId 从评论表读取) -->
      <view class="comments-list">
        <view
          v-for="(cmt, idx) in commentList"
          :key="cmt.id || idx"
          class="comment-item-row"
        >
          <image :src="avatarSrc(cmt.avatar)" mode="aspectFill" class="comment-avatar" />
          <view class="comment-main-col">
            <view class="comment-content-line">
              <text class="comment-user-name">{{ cmt.userName }} ：</text>
              <text class="comment-text-val">{{ cmt.content }}</text>
            </view>
          </view>
        </view>
        <view v-if="commentList.length === 0" class="comments-empty">
          <text class="comments-empty-text">暂无评论，快来说两句吧</text>
        </view>
      </view>
    </view>

    <view class="elder-no-more">
      <text>——暂无更多——</text>
    </view>

    <!-- 底部评论输入条：文字评论 + 发送（语音仍暂未接入，保留入口提示） -->
    <view class="bottom-comment-bar-wrap">
      <view class="comment-input-box">
        <input
          v-model="draft"
          class="comment-input"
          placeholder="说点什么吧…"
          placeholder-class="comment-placeholder"
          confirm-type="send"
          :maxlength="200"
          :adjust-position="true"
          @confirm="handleSendComment"
        />
        <view class="voice-btn" hover-class="btn-hover" @tap="showVoiceUnsupported">
          <image src="/static/common/voice-mic.png" mode="aspectFit" class="mic-img" />
        </view>
      </view>
      <button
        class="send-btn"
        hover-class="btn-hover"
        :disabled="sending"
        @tap="handleSendComment"
      >
        发送
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { useSquareStore } from '../../stores/square';
import { useChatStore } from '../../stores/chat';
import { useSettingStore } from '../../stores/setting';
import { useUserStore } from '../../stores/user';
import { showVoiceUnsupported } from '../../utils/voice';
import { avatarSrc } from '../../utils/avatar';
import type { CommentItem } from '../../types';

const squareStore = useSquareStore();
const chatStore = useChatStore();
const settingStore = useSettingStore();
const userStore = useUserStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const postId = ref('');
const post = ref({
  authorId: '',
  authorName: '',
  authorAvatar: '',
  timeAgo: '',
  content: ''
});

// 真实评论数据：按 postId 从评论表读取
const commentList = computed<CommentItem[]>(() => {
  if (!postId.value) return [];
  return squareStore.commentsByPost(postId.value);
});

// 当前登录用户视角：作者是本人时展示【删除】(对应评论页2)
const isMine = computed(() => post.value.authorId === userStore.currentUser.id);

// 是否已添加该作者为好友
const isFriend = (authorName: string) => {
  return chatStore.contacts.some(
    (c) => c.targetUserId === post.value.authorId || c.name === authorName
  );
};

onLoad((options) => {
  if (options && options.postId) {
    postId.value = options.postId;
    // 列表里已加载的直接用；没有就按 id 拉一次详情
    // （从「我的动态」点进来、或广场已经翻过这一条时，列表里可能没有它）
    squareStore
      .fetchPost(options.postId)
      .then((found) => {
        if (!found) return;
        post.value.authorId = found.authorId;
        post.value.authorName = found.authorName;
        post.value.authorAvatar = found.authorAvatar;
        post.value.timeAgo = found.timeAgo;
        post.value.content = found.content;
      })
      .catch((e) => console.error('[comment] 动态详情加载失败', e));

    // 从后端拉取该动态的真实评论
    squareStore.loadComments(options.postId).catch((e) => {
      console.error('[comment] 评论加载失败', e);
    });
  }
});

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

// ---------- 发表评论 ----------
const draft = ref('');
const sending = ref(false);

const handleSendComment = async () => {
  const content = draft.value.trim();
  if (!content) {
    uni.showToast({ title: '请输入评论内容', icon: 'none' });
    return;
  }
  if (sending.value) return;
  sending.value = true;
  try {
    // 真实写库：POST /api/square/{id}/comments，成功后 store 会同步评论数与广场列表
    await squareStore.addComment(postId.value, content);
    draft.value = '';
    uni.showToast({ title: '评论成功', icon: 'success' });
  } catch (e: any) {
    uni.showToast({ title: e?.message || '评论失败，请重试', icon: 'none', duration: 2000 });
  } finally {
    sending.value = false;
  }
};

const handleAddAuthor = async () => {
  try {
    const saved = await chatStore.addContact({ targetUserId: post.value.authorId });
    uni.showToast({ title: `已添加 ${saved.name} 为好友`, icon: 'none' });
  } catch (e: any) {
    uni.showToast({ title: e?.message || '添加失败', icon: 'none', duration: 2000 });
  }
};

const handleDeletePost = () => {
  uni.showModal({
    title: '删除动态',
    content: '删除后该动态及评论将无法恢复，确定删除吗？',
    confirmText: '删除',
    confirmColor: '#FF4D4F',
    cancelText: '取消',
    success: async (res) => {
      if (!res.confirm) return;
      try {
        // 真实删除帖子及其评论，同步广场列表与评论计数
        await squareStore.deletePost(postId.value);
        uni.showToast({ title: '已删除', icon: 'success' });
        setTimeout(() => {
          uni.navigateBack({ delta: 1 });
        }, 600);
      } catch (e: any) {
        uni.showToast({ title: e?.message || '删除失败', icon: 'none', duration: 2000 });
      }
    }
  });
};

</script>

<style lang="scss" scoped>
.comment-page-container {
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

/* 帖子与评论卡片 (严格对齐评论页1.png) */
.post-comment-box {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 36rpx 32rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 30rpx;
}

.post-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.author-left {
  display: flex;
  align-items: center;
}

.author-avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  margin-right: 20rpx;
}

.author-meta {
  display: flex;
  flex-direction: column;

  .author-name {
    font-size: calc(34rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
  }

  .author-time {
    font-size: calc(26rpx * var(--font-scale, 1));
    color: #8F959E;
  }
}

.add-friend-pill {
  height: calc(68rpx * var(--font-scale, 1));
  line-height: calc(68rpx * var(--font-scale, 1));
  background-color: #EBF2FF;
  color: #1B65F0;
  font-size: calc(30rpx * var(--font-scale, 1));
  font-weight: 800;
  border-radius: 34rpx;
  padding: 0 28rpx;
  border: none;

  &::after {
    border: none;
  }
}

/* 本人动态的删除入口 (对应评论页2)：圆形图标与文字垂直居中 */
.delete-post-btn {
  display: flex;
  align-items: center;
  padding: 8rpx 4rpx;
}

.delete-icon {
  width: 38rpx;
  height: 38rpx;
  border-radius: 50%;
  background-color: #E2E4E8;
  margin-right: 12rpx;
  flex-shrink: 0;
}

.delete-text {
  font-size: calc(32rpx * var(--font-scale, 1));
  font-weight: 800;
  color: #1F2329;
  line-height: 38rpx;
  display: flex;
  align-items: center;
}

.add-friend-pill.friend-added {
  background-color: #F1F2F5;
  color: #8F959E;
}

.comments-empty {
  padding: 30rpx 0;
  text-align: center;

  .comments-empty-text {
    font-size: calc(28rpx * var(--font-scale, 1));
    color: #8F959E;
  }
}

.post-content-wrap {
  margin-bottom: 28rpx;

  .post-content-text {
    font-size: calc(38rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
    line-height: 1.5;
  }
}

.card-divider-line {
  height: 2rpx;
  background-color: #E5E7EB;
  margin-bottom: 28rpx;
}

/* 评论项 */
.comments-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.comment-item-row {
  display: flex;
  align-items: flex-start;
}

.comment-avatar {
  width: 90rpx;
  height: 90rpx;
  border-radius: 50%;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.comment-main-col {
  flex: 1;
}

.comment-content-line {
  .comment-user-name {
    font-size: calc(34rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
    margin-right: 12rpx;
  }

  .comment-text-val {
    font-size: calc(34rpx * var(--font-scale, 1));
    font-weight: 700;
    color: #1F2329;
  }
}

/* 底部语音条 */
/* 底部评论输入条 */
.bottom-comment-bar-wrap {
  margin-top: auto;
  margin-bottom: 30rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.comment-input-box {
  flex: 1;
  display: flex;
  align-items: center;
  background-color: #FFFFFF;
  border: 3rpx solid #BCD6FF;
  border-radius: 50rpx;
  height: calc(96rpx * var(--font-scale, 1));
  padding: 0 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(27, 101, 240, 0.1);
}

.comment-input {
  flex: 1;
  font-size: calc(32rpx * var(--font-scale, 1));
  color: #1F2329;
}

.comment-placeholder {
  color: #9AA3AF;
  font-size: calc(30rpx * var(--font-scale, 1));
}

.voice-btn {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;

  .mic-img {
    width: 44rpx;
    height: 44rpx;
  }
}

.send-btn {
  width: 150rpx;
  height: calc(96rpx * var(--font-scale, 1));
  line-height: calc(96rpx * var(--font-scale, 1));
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(32rpx * var(--font-scale, 1));
  font-weight: 800;
  border-radius: 50rpx;
  border: none;
  padding: 0;

  &::after {
    border: none;
  }

  &[disabled] {
    opacity: 0.6;
  }
}

.btn-hover {
  opacity: 0.88;
}
</style>
