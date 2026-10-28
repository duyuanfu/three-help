<template>
  <view class="square-page-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部大标题 -->
    <view class="page-title-wrap">
      <text class="page-title">广 场</text>
    </view>

    <!-- 顶部搜索框 (搜索指定人名的帖子) -->
    <view class="search-box">
      <image src="/static/common/search.png" mode="aspectFit" class="search-icon-img" />
      <input 
        v-model="squareStore.searchKeyword" 
        class="search-input" 
        placeholder="搜索人名查看动态..." 
        placeholder-class="search-placeholder"
      />
    </view>

    <!-- 玩转互助 Slogan -->
    <view class="slogan-wrap">
      <text class="slogan-text">玩转互助 ， 生活小帮手</text>
    </view>

    <!-- 两大醒目行动入口卡片 (严格对齐原型尺寸与色彩) -->
    <view class="action-banners-group">
      <!-- 1. 兑换卡片 (黄色大卡) -->
      <view class="banner-card banner-yellow" hover-class="banner-hover" @tap="goToMall">
        <view class="banner-icon-box">
          <image src="/static/square/exchange-banner.png" mode="aspectFit" class="banner-icon-img" />
        </view>
        <view class="banner-pill-btn">
          <text class="pill-btn-text text-yellow">兑 换</text>
        </view>
      </view>

      <!-- 2. 一键发帖卡片 (蓝色大卡) -->
      <view class="banner-card banner-blue" hover-class="banner-hover" @tap="goToPost">
        <view class="banner-icon-box">
          <image src="/static/square/post-banner.png" mode="aspectFit" class="banner-icon-img" />
        </view>
        <view class="banner-pill-btn">
          <text class="pill-btn-text text-blue">一 键 发 帖</text>
        </view>
      </view>
    </view>

    <!-- 全部动态标题 -->
    <view class="section-title-wrap">
      <text class="section-title">全部动态</text>
    </view>

    <!-- 动态帖子滚动流 -->
    <scroll-view scroll-y class="posts-scroll-list">
      <view 
        v-for="post in postList" 
        :key="post.id" 
        class="post-card"
      >
        <!-- 头部作者信息与加好友按钮 -->
        <view class="post-header-row">
          <view class="author-info">
            <image :src="avatarSrc(post.authorAvatar)" mode="aspectFill" class="author-avatar" />
            <view class="author-text-col">
              <text class="author-name">{{ post.authorName }}</text>
              <text class="post-time">{{ post.timeAgo }}</text>
            </view>
          </view>
          <!-- 加好友按钮 (若非本人)：未添加显示【加好友】，已添加显示【已添加】 -->
          <button
            v-if="post.authorId !== userStore.currentUser.id"
            class="add-friend-pill"
            :class="{ 'friend-added': isFriend(post) }"
            hover-class="btn-hover"
            @tap="isFriend(post) ? undefined : handleAddFriend(post)"
          >
            {{ isFriend(post) ? '已添加' : '加好友' }}
          </button>
        </view>

        <!-- 帖子正文 -->
        <view class="post-content-wrap">
          <text class="post-text">{{ post.content }}</text>
        </view>

        <!-- 分割线 -->
        <view class="post-card-divider"></view>

        <!-- 底部互动操作行 (评论 / 删除 / 点赞) -->
        <view class="post-footer-row">
          <view class="comment-trigger" @tap="handleComment(post)">
            <text class="comment-count-text">评论 {{ squareStore.commentCountOf(post.id) }}</text>
          </view>
          <view class="action-right">
            <text 
              v-if="post.authorId === userStore.currentUser.id" 
              class="delete-btn-text" 
              @tap="handleDelete(post.id)"
            >
              删除
            </text>
            <!-- 点赞数也不再展示：老年用户反馈爱心图标易误解，数字同样无感知，直接留白 -->
          </view>
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
import { useChatStore } from '../../stores/chat';
import { useSettingStore } from '../../stores/setting';
import { usePullRefresh } from '../../composables/usePullRefresh';
import { useUserStore } from '../../stores/user';
import { avatarSrc } from '../../utils/avatar';
import type { PostItem } from '../../types';

const squareStore = useSquareStore();
const chatStore = useChatStore();
const settingStore = useSettingStore();

// 下拉刷新：重新拉取全量数据（五个 tab 页统一行为）
usePullRefresh();

const userStore = useUserStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const postList = computed(() => {
  return squareStore.filteredPosts;
});

const goToMall = () => {
  uni.navigateTo({
    url: '/pages/square/mall'
  });
};

const goToPost = () => {
  uni.navigateTo({
    url: '/pages/square/post'
  });
};

// 是否已添加该作者为好友（按对方账号ID判断，重名也不会误判）
const isFriend = (post: PostItem) => {
  if (!post.authorId) return false;
  return chatStore.contacts.some((c) => c.targetUserId === post.authorId);
};

/**
 * 从动态加好友
 *
 * 必须传 targetUserId —— 广场上拿不到对方手机号，
 * 早期版本传的是 "138****1234" 这种假号码，会被服务端
 * 按「请填写 11 位手机号」拒掉。服务端拿到账号ID后会
 * 自动补齐姓名、头像、标签并建立真实关联。
 */
const handleAddFriend = async (post: PostItem) => {
  if (!post.authorId) {
    uni.showToast({ title: '无法识别发布者账号', icon: 'none' });
    return;
  }
  if (post.authorId === userStore.currentUser.id) {
    uni.showToast({ title: '这是你自己发布的动态', icon: 'none' });
    return;
  }
  if (isFriend(post)) {
    uni.showToast({ title: `${post.authorName} 已在通讯录中`, icon: 'none' });
    return;
  }

  uni.showLoading({ title: '添加中...', mask: true });
  try {
    await chatStore.addContact({ targetUserId: post.authorId });
    uni.hideLoading();
    uni.showToast({ title: `已添加 ${post.authorName} 为好友`, icon: 'success' });
  } catch (e: any) {
    uni.hideLoading();
    uni.showToast({ title: e?.message || '添加失败', icon: 'none', duration: 2200 });
  }
};

const handleComment = (post: PostItem) => {
  uni.navigateTo({
    url: `/pages/square/comment?postId=${post.id}`
  });
};

const handleDelete = (postId: string) => {
  uni.showModal({
    title: '提示',
    content: '确定要删除这条动态吗？',
    confirmColor: '#FF4D4F',
    confirmText: '删除',
    cancelText: '取消',
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
.square-page-container {
  min-height: 100vh;
  background-color: #F6F7FB;
  padding: 0 32rpx 140rpx 32rpx;
  display: flex;
  flex-direction: column;
}

.status-bar {
  height: 60rpx;
}

.page-title-wrap {
  text-align: center;
  padding: 10rpx 0 24rpx 0;
}

.page-title {
  font-size: calc(52rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1B65F0;
  letter-spacing: 4rpx;
}

/* 搜索框 */
.search-box {
  background-color: #FFFFFF;
  border-radius: 40rpx;
  height: calc(88rpx * var(--font-scale, 1));
  display: flex;
  align-items: center;
  padding: 0 28rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.05);
  margin-bottom: 28rpx;

  .search-icon-img {
    width: 38rpx;
    height: 38rpx;
    margin-right: 14rpx;
  }

  .search-input {
    flex: 1;
    font-size: calc(32rpx * var(--font-scale, 1));
    color: #1F2329;
  }

  .search-placeholder {
    color: #8F959E;
    font-size: calc(30rpx * var(--font-scale, 1));
  }
}

.slogan-wrap {
  text-align: center;
  margin-bottom: 24rpx;

  .slogan-text {
    font-size: calc(38rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
    letter-spacing: 2rpx;
  }
}

/* 两大入口卡片 (严格对齐原型) */
.action-banners-group {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  margin-bottom: 36rpx;
}

.banner-card {
  height: calc(130rpx * var(--font-scale, 1));
  border-radius: 36rpx;
  padding: 0 36rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.08);

  .banner-icon-box {
    width: 80rpx;
    height: 80rpx;
    display: flex;
    align-items: center;
    justify-content: center;

    .banner-icon-img {
      width: 80rpx;
      height: 80rpx;
    }
  }

  .banner-pill-btn {
    width: 380rpx;
    height: calc(84rpx * var(--font-scale, 1));
    line-height: calc(84rpx * var(--font-scale, 1));
    background-color: #FFFFFF;
    border-radius: 42rpx;
    text-align: center;
    box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);

    .pill-btn-text {
      font-size: calc(44rpx * var(--font-scale, 1));
      font-weight: 900;
      letter-spacing: 6rpx;
    }

    .text-yellow {
      color: #FFC000;
    }

    .text-blue {
      color: #1B65F0;
    }
  }
}

.banner-yellow {
  background-color: #FFC000;
}

.banner-blue {
  background-color: #1B65F0;
}

.section-title-wrap {
  margin-bottom: 20rpx;

  .section-title {
    font-size: calc(34rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
  }
}

.posts-scroll-list {
  flex: 1;
}

/* 动态卡片 (原型白底圆角) */
.post-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
}

.post-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.author-info {
  display: flex;
  align-items: center;
}

.author-avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  margin-right: 20rpx;
  background-color: #F0F2F5;
}

.author-text-col {
  display: flex;
  flex-direction: column;

  .author-name {
    font-size: calc(34rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
  }

  .post-time {
    font-size: calc(26rpx * var(--font-scale, 1));
    color: #8F959E;
    margin-top: 4rpx;
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

  &.friend-added {
    background-color: #F1F2F5;
    color: #8F959E;
  }
}

.post-content-wrap {
  margin-bottom: 24rpx;

  .post-text {
    font-size: calc(36rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
    line-height: 1.5;
  }
}

.post-card-divider {
  height: 2rpx;
  background-color: #E5E7EB;
  margin-bottom: 20rpx;
}

.post-footer-row {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .comment-count-text {
    font-size: calc(32rpx * var(--font-scale, 1));
    font-weight: 800;
    color: #1F2329;
  }

  .delete-btn-text {
    font-size: calc(32rpx * var(--font-scale, 1));
    font-weight: 800;
    color: #1F2329;
  }
}

.banner-hover, .btn-hover {
  opacity: 0.88;
}
</style>
