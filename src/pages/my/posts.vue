<template>
  <view class="my-posts-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航栏 (严格对齐我的动态.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">我的动态</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 动态卡片列表 (严格对齐原型) -->
    <scroll-view scroll-y class="posts-scroll-list">
      <view 
        v-for="post in myPostList" 
        :key="post.id" 
        class="post-detail-card"
      >
        <view class="card-header-row">
          <view class="author-meta">
            <image :src="post.authorAvatar" mode="aspectFill" class="my-avatar" />
            <view class="name-date-col">
              <text class="author-name">{{ post.authorName }}</text>
              <text class="post-date">{{ post.timeAgo }}</text>
            </view>
          </view>
          <view class="delete-trigger" @tap="handleDelete(post.id)">
            <view class="delete-circle-radio"></view>
            <text class="delete-label">删除</text>
          </view>
        </view>

        <view class="post-body-wrap">
          <text class="post-body-text">{{ post.content }}</text>
        </view>

        <view class="card-divider"></view>

        <view class="card-footer-row">
          <text class="comment-count-label">评论 {{ post.commentCount }}</text>
          <button class="view-more-pill" hover-class="btn-hover" @tap="goToComments(post.id)">
            查看更多
          </button>
        </view>
      </view>

      <view class="elder-no-more">
        <text>——暂无更多——</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useSquareStore } from '../../stores/square';
import { useSettingStore } from '../../stores/setting';

const squareStore = useSquareStore();
const settingStore = useSettingStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const myPostList = computed(() => {
  return squareStore.myPosts;
});

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const goToComments = (postId: string) => {
  uni.navigateTo({
    url: `/pages/square/comment?postId=${postId}`
  });
};

const handleDelete = (postId: string) => {
  uni.showModal({
    title: '提示',
    content: '确定要删除这条动态吗？',
    confirmColor: '#FF4D4F',
    cancelText: '取消',
    confirmText: '确认删除',
    success: (res) => {
      if (res.confirm) {
        squareStore.deletePost(postId);
        uni.showToast({ title: '已删除', icon: 'success' });
      }
    }
  });
};
</script>

<style lang="scss" scoped>
.my-posts-container {
  min-height: 100vh;
  background-color: #F6F7FB;
  padding: 0 36rpx 60rpx 36rpx;
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

.posts-scroll-list {
  flex: 1;
}

/* 动态卡片 (严格对齐我的动态.png) */
.post-detail-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 36rpx 32rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 30rpx;
}

.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.author-meta {
  display: flex;
  align-items: center;
}

.my-avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  margin-right: 20rpx;
  background-color: #F0F2F5;
}

.name-date-col {
  display: flex;
  flex-direction: column;

  .author-name {
    font-size: calc(36rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
  }

  .post-date {
    font-size: calc(26rpx * var(--font-scale, 1));
    color: #8F959E;
    font-weight: 600;
    margin-top: 6rpx;
  }
}

.delete-trigger {
  display: flex;
  align-items: center;

  .delete-circle-radio {
    width: 38rpx;
    height: 38rpx;
    border-radius: 50%;
    background-color: #E2E4E8;
    margin-right: 12rpx;
  }

  .delete-label {
    font-size: calc(32rpx * var(--font-scale, 1));
    font-weight: 800;
    color: #1F2329;
  }
}

.post-body-wrap {
  margin-bottom: 28rpx;

  .post-body-text {
    font-size: calc(38rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
    line-height: 1.5;
  }
}

.card-divider {
  height: 2rpx;
  background-color: #E5E7EB;
  margin-bottom: 24rpx;
}

.card-footer-row {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .comment-count-label {
    font-size: calc(34rpx * var(--font-scale, 1));
    font-weight: 800;
    color: #1F2329;
  }

  .view-more-pill {
    background-color: #EBF2FF;
    color: #1B65F0;
    font-size: calc(30rpx * var(--font-scale, 1));
    font-weight: 800;
    border-radius: 34rpx;
    padding: 0 32rpx;
    height: 64rpx;
    line-height: 64rpx;
    border: none;

    &::after {
      border: none;
    }
  }
}

.btn-hover {
  opacity: 0.88;
}
</style>
