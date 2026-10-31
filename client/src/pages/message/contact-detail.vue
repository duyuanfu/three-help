<template>
  <view class="contact-detail-page" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航栏 -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">更 多</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 主信息卡片 (头像、姓名、实名状态、是否已添加) -->
    <view class="primary-user-card">
      <view class="user-left-info">
        <image :src="avatarSrc(profile.avatar)" mode="aspectFill" class="large-avatar" />
        <view class="user-text-col">
          <text class="user-name">{{ profile.name || '加载中…' }}</text>
          <view class="realname-row">
            <view class="dot" :class="profile.isRealName ? 'green-dot' : 'gray-dot'"></view>
            <text class="realname-label">{{ profile.isRealName ? '已实名' : '未实名' }}</text>
          </view>
        </view>
      </view>
      <button
        v-if="!profile.isFriend"
        class="added-pill-btn add-pill"
        hover-class="btn-hover"
        @tap="addToContacts"
      >
        加为联系人
      </button>
      <button v-else class="added-pill-btn">已添加</button>
    </view>

    <!-- 详细档案卡片：全部来自对方账号资料，没填就显示「未填写」，不编造 -->
    <view class="detail-archive-card">
      <view class="archive-row" v-for="row in archiveRows" :key="row.label">
        <text class="archive-label">{{ row.label }}</text>
        <text class="archive-val">{{ row.value }}</text>
      </view>
    </view>

    <!-- TA 的动态 -->
    <view class="section-title-wrap">
      <text class="section-title">TA 的动态</text>
    </view>

    <view v-for="post in posts" :key="post.id" class="contact-post-card" hover-class="btn-hover" @tap="openPost(post.id)">
      <view class="post-user-row">
        <image :src="avatarSrc(post.authorAvatar || profile.avatar)" mode="aspectFill" class="post-mini-avatar" />
        <view class="post-name-col">
          <text class="post-author-name">{{ post.authorName || profile.name }}</text>
          <text class="post-time-text">{{ post.timeAgo }}</text>
        </view>
      </view>
      <view class="post-text-box" v-if="post.content">
        <text class="post-body-text">{{ post.content }}</text>
      </view>
      <view class="post-footer-row">
        <text class="post-comment-text">评论 {{ post.commentCount || 0 }}</text>
      </view>
    </view>

    <!-- 首屏加载 / 空态 / 翻页状态 -->
    <view v-if="loadingPosts && posts.length === 0" class="feed-tip">
      <text>加载中...</text>
    </view>
    <view v-else-if="posts.length === 0" class="feed-tip" @tap="loadFailed ? retryPosts : undefined">
      <text>{{ loadFailed ? '动态加载失败，点击重试' : 'TA 还没有发过动态' }}</text>
    </view>
    <view v-else-if="hasMore" class="feed-tip">
      <text>上拉加载更多</text>
    </view>
    <view v-else class="elder-no-more">
      <text>——暂无更多——</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad, onReachBottom } from '@dcloudio/uni-app';
import { apiContactProfile, apiUserPosts, type ContactProfile } from '../../api/business';
import type { PostItem } from '../../types';
import { useChatStore } from '../../stores/chat';
import { useSettingStore } from '../../stores/setting';
import { avatarSrc } from '../../utils/avatar';

const chatStore = useChatStore();
const settingStore = useSettingStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const PAGE_SIZE = 10;

/** 从聊天页进来时传的是对方账号ID（不是通讯录行ID） */
const userId = ref('');
const profile = ref<Partial<ContactProfile>>({});
const posts = ref<PostItem[]>([]);
const cursor = ref<number | null>(null);
const hasMore = ref(true);
const loadingPosts = ref(false);
const loadFailed = ref(false);

const archiveRows = computed(() => [
  { label: '性 别 ：', value: profile.value.gender || '未填写' },
  { label: '年 龄 ：', value: profile.value.age ? `${profile.value.age}岁` : '未填写' },
  { label: '住 址 ：', value: profile.value.address || '未填写' },
  { label: '职 业 ：', value: profile.value.occupation || '未填写' }
]);

onLoad((options) => {  userId.value = (options && options.id) || '';
  if (!userId.value) {
    uni.showToast({ title: '缺少用户信息', icon: 'none' });
    return;
  }

  // 先用通讯录里的快照占位（姓名/头像/标签），不必等接口回来才有内容。
  // 注意：这里必须按 targetUserId 匹配 —— 传进来的是账号ID，通讯录行的 id 是 c_xxx。
  const local = chatStore.contacts.find(
    (c) => c.targetUserId === userId.value || c.id === userId.value
  );
  if (local) {
    profile.value = {
      name: local.name,
      avatar: local.avatar,
      phone: local.phone,
      tag: local.tag,
      isFriend: local.isFriend !== false
    };
  }

  loadProfile();
  loadPosts(true);
});

onReachBottom(() => {
  loadPosts(false);
});

const loadProfile = async () => {
  try {
    profile.value = await apiContactProfile(userId.value);
  } catch (e) {
    // request 层已经弹过提示，这里保留已有快照即可，不把页面清空
  }
};

const loadPosts = async (reset: boolean) => {
  if (loadingPosts.value) return;
  if (!reset && !hasMore.value) return;

  loadingPosts.value = true;
  if (reset) {
    loadFailed.value = false;
  }
  try {
    const page = await apiUserPosts(userId.value, {
      cursor: reset ? null : cursor.value,
      limit: PAGE_SIZE
    });
    const items = page.items || [];
    if (reset) {
      posts.value = items;
    } else {
      // 同一页可能因为新动态插入而重复，按 id 去重
      const seen = new Set(posts.value.map((p) => p.id));
      posts.value = posts.value.concat(items.filter((p) => !seen.has(p.id)));
    }
    cursor.value = page.nextCursor;
    hasMore.value = !!page.hasMore;
  } catch (e) {
    if (reset) {
      posts.value = [];
      loadFailed.value = true;
    }
  } finally {
    loadingPosts.value = false;
  }
};

const retryPosts = () => {
  hasMore.value = true;
  loadPosts(true);
};

const addToContacts = async () => {
  if (!userId.value) return;
  try {
    await chatStore.addContact({ targetUserId: userId.value });
    profile.value.isFriend = true;
    uni.showToast({ title: '已加入通讯录', icon: 'success' });
  } catch (e: any) {
    uni.showToast({ title: e?.message || '添加失败', icon: 'none' });
  }
};

const openPost = (postId: string) => {
  uni.navigateTo({ url: `/pages/square/comment?postId=${encodeURIComponent(postId)}` });
};

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};
</script>

<style lang="scss" scoped>
.contact-detail-page {
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

/* 顶部主要信息卡片 */
.primary-user-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 32rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 30rpx;
}

.user-left-info {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.large-avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  margin-right: 24rpx;
  background-color: #F0F2F5;
  flex-shrink: 0;
}

.user-text-col {
  display: flex;
  flex-direction: column;
  min-width: 0;

  .user-name {
    font-size: calc(40rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
    margin-bottom: 12rpx;
  }

  .realname-row {
    display: flex;
    align-items: center;

    .dot {
      width: 22rpx;
      height: 22rpx;
      border-radius: 50%;
      margin-right: 12rpx;
    }

    .green-dot {
      background-color: #52C41A;
    }

    .gray-dot {
      background-color: #C9CDD4;
    }

    .realname-label {
      font-size: calc(30rpx * var(--font-scale, 1));
      font-weight: 800;
      color: #1F2329;
    }
  }
}

.added-pill-btn {
  background-color: #EBF2FF;
  color: #1B65F0;
  font-size: calc(30rpx * var(--font-scale, 1));
  font-weight: 800;
  border-radius: 36rpx;
  padding: 0 32rpx;
  height: 68rpx;
  line-height: 68rpx;
  border: none;
  flex-shrink: 0;

  &::after {
    border: none;
  }
}

.add-pill {
  background-color: #1B65F0;
  color: #FFFFFF;
}

/* 档案详情卡片 */
.detail-archive-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 10rpx 36rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 36rpx;
}

.archive-row {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 2rpx solid #E5E7EB;

  &:last-child {
    border-bottom: none;
  }

  .archive-label {
    font-size: calc(34rpx * var(--font-scale, 1));
    font-weight: 800;
    color: #1F2329;
    width: 170rpx;
    flex-shrink: 0;
  }

  .archive-val {
    font-size: calc(34rpx * var(--font-scale, 1));
    font-weight: 800;
    color: #1F2329;
    flex: 1;
  }
}

.section-title-wrap {
  margin-bottom: 20rpx;

  .section-title {
    font-size: calc(36rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
  }
}

/* 动态卡片 */
.contact-post-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 30rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 30rpx;
}

.post-user-row {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.post-mini-avatar {
  width: 90rpx;
  height: 90rpx;
  border-radius: 50%;
  margin-right: 20rpx;
  background-color: #F0F2F5;
}

.post-name-col {
  display: flex;
  flex-direction: column;

  .post-author-name {
    font-size: calc(34rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
  }

  .post-time-text {
    font-size: calc(26rpx * var(--font-scale, 1));
    color: #8F959E;
  }
}

.post-text-box {
  .post-body-text {
    font-size: calc(36rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
    line-height: 1.5;
  }
}

.post-footer-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 2rpx solid #F0F2F5;

  .post-comment-text {
    font-size: calc(28rpx * var(--font-scale, 1));
    color: #8F959E;
    font-weight: 700;
  }
}

.feed-tip {
  padding: 30rpx 0;
  text-align: center;

  text {
    font-size: calc(30rpx * var(--font-scale, 1));
    color: #8F959E;
    font-weight: 700;
  }
}

.elder-no-more {
  padding: 24rpx 0;
  text-align: center;

  text {
    font-size: calc(28rpx * var(--font-scale, 1));
    color: #A8ADB5;
    font-weight: 700;
  }
}

.btn-hover {
  opacity: 0.88;
}
</style>
