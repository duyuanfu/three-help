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
          <image :src="post.authorAvatar" mode="aspectFill" class="author-avatar" />
          <view class="author-meta">
            <text class="author-name">{{ post.authorName }}</text>
            <text class="author-time">{{ post.timeAgo }}</text>
          </view>
        </view>
        <button v-if="!isMine" class="add-friend-pill" hover-class="btn-hover" @tap="handleAddAuthor">
          加好友
        </button>
        <view v-else class="delete-post-btn" hover-class="btn-hover" @tap="handleDeletePost">
          <text class="delete-icon">🗑</text>
          <text class="delete-text">删除</text>
        </view>
      </view>

      <!-- 帖子正文 -->
      <view class="post-content-wrap">
        <text class="post-content-text">{{ post.content }}</text>
      </view>

      <!-- 卡片内部分割线 -->
      <view class="card-divider-line"></view>

      <!-- 评论列表 (原型展示：张冬梅 ： 下次介绍给我，带已添加按钮) -->
      <view class="comments-list">
        <view 
          v-for="(cmt, idx) in comments" 
          :key="idx" 
          class="comment-item-row"
        >
          <image :src="cmt.avatar" mode="aspectFill" class="comment-avatar" />
          <view class="comment-main-col">
            <view class="comment-content-line">
              <text class="comment-user-name">{{ cmt.userName }} ：</text>
              <text class="comment-text-val">{{ cmt.content }}</text>
            </view>
            <view class="comment-btn-wrap">
              <button class="added-badge-btn">已添加</button>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="elder-no-more">
      <text>——暂无更多——</text>
    </view>

    <!-- 底部语音评论条 (原型样式) -->
    <view class="bottom-voice-bar-wrap">
      <view class="voice-bar" hover-class="btn-hover" @tap="showVoiceUnsupported">
        <image src="/static/common/voice-mic.png" mode="aspectFit" class="mic-img" />
        <text class="voice-text">不方便打字？按住说话</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { useSquareStore } from '../../stores/square';
import { useChatStore } from '../../stores/chat';
import { useSettingStore } from '../../stores/setting';
import { showVoiceUnsupported } from '../../utils/voice';

const squareStore = useSquareStore();
const chatStore = useChatStore();
const settingStore = useSettingStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const post = ref({
  authorName: '王奶奶',
  authorAvatar: '/static/avatars/wangnainai.png',
  timeAgo: '1分钟前',
  content: '马玲玲那孩子脾气好又有耐心'
});

const comments = ref([
  {
    userName: '张冬梅',
    avatar: '/static/avatars/zhangdongmei.png',
    content: '下次介绍给我'
  }
]);

// 当前登录用户视角：作者是本人时展示【删除】(对应评论页2)
const isMine = computed(() => post.value.authorName === '张冬梅');

onLoad((options) => {
  if (options && options.postId) {
    const found = squareStore.posts.find((p) => p.id === options.postId);
    if (found) {
      post.value.authorName = found.authorName;
      post.value.authorAvatar = found.authorAvatar;
      post.value.timeAgo = found.timeAgo;
      post.value.content = found.content;
    }
  }
});

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const handleAddAuthor = () => {
  chatStore.addContact({
    name: post.value.authorName,
    phone: '138****8888',
    tag: '@社区邻居'
  });
  uni.showToast({ title: '已成功添加好友', icon: 'success' });
};

const handleDeletePost = () => {
  uni.showModal({
    title: '删除动态',
    content: '删除后该动态及评论将无法恢复，确定删除吗？',
    confirmText: '删除',
    confirmColor: '#FF4D4F',
    cancelText: '取消',
    success: (res) => {
      if (res.confirm) {
        uni.showToast({ title: '已删除', icon: 'success' });
        setTimeout(() => {
          uni.navigateBack({ delta: 1 });
        }, 600);
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
    font-size: calc(34rpx * var(--font-scale));
    font-weight: 900;
    color: #1F2329;
  }

  .author-time {
    font-size: calc(26rpx * var(--font-scale));
    color: #8F959E;
  }
}

.add-friend-pill {
  height: calc(68rpx * var(--font-scale));
  line-height: calc(68rpx * var(--font-scale));
  background-color: #EBF2FF;
  color: #1B65F0;
  font-size: calc(30rpx * var(--font-scale));
  font-weight: 800;
  border-radius: 34rpx;
  padding: 0 28rpx;
  border: none;

  &::after {
    border: none;
  }
}

/* 本人动态的删除入口 (对应评论页2) */
.delete-post-btn {
  display: flex;
  align-items: center;
  padding: 8rpx 4rpx;
}

.delete-icon {
  font-size: calc(30rpx * var(--font-scale));
  margin-right: 8rpx;
}

.delete-text {
  font-size: calc(32rpx * var(--font-scale));
  font-weight: 800;
  color: #1F2329;
}

.post-content-wrap {
  margin-bottom: 28rpx;

  .post-content-text {
    font-size: calc(38rpx * var(--font-scale));
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
  margin-bottom: 12rpx;

  .comment-user-name {
    font-size: calc(34rpx * var(--font-scale));
    font-weight: 900;
    color: #1F2329;
    margin-right: 12rpx;
  }

  .comment-text-val {
    font-size: calc(34rpx * var(--font-scale));
    font-weight: 700;
    color: #1F2329;
  }
}

.added-badge-btn {
  display: inline-block;
  background-color: #EBF2FF;
  color: #1B65F0;
  font-size: calc(28rpx * var(--font-scale));
  font-weight: 800;
  border-radius: 30rpx;
  padding: 0 24rpx;
  height: 56rpx;
  line-height: 56rpx;
  border: none;

  &::after {
    border: none;
  }
}

/* 底部语音条 */
.bottom-voice-bar-wrap {
  margin-top: auto;
  margin-bottom: 30rpx;
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
