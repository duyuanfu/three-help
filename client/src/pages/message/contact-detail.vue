<template>
  <view class="contact-detail-page" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航栏 (严格对齐更多.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">更 多</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 主信息卡片 (头像、姓名、已实名、已添加) -->
    <view class="primary-user-card">
      <view class="user-left-info">
        <image :src="avatarSrc(contactInfo.avatar)" mode="aspectFill" class="large-avatar" />
        <view class="user-text-col">
          <text class="user-name">{{ contactInfo.name }}</text>
          <view class="realname-row">
            <view class="green-dot"></view>
            <text class="realname-label">已实名</text>
          </view>
        </view>
      </view>
      <button class="added-pill-btn">已添加</button>
    </view>

    <!-- 详细档案卡片 (性别、年龄、住址、相识) -->
    <view class="detail-archive-card">
      <view class="archive-row">
        <text class="archive-label">性 别 ：</text>
        <text class="archive-val">{{ contactInfo.gender || '男' }}</text>
      </view>
      <view class="archive-row">
        <text class="archive-label">年 龄 ：</text>
        <text class="archive-val">{{ contactInfo.age || '56' }}岁</text>
      </view>
      <view class="archive-row">
        <text class="archive-label">住 址 ：</text>
        <text class="archive-val">{{ contactInfo.address || '福建省泉州市' }}</text>
      </view>
      <view class="archive-row">
        <text class="archive-label">相 识 ：</text>
        <text class="archive-val">{{ contactInfo.metDate || '2024/03/02' }}</text>
      </view>
    </view>

    <!-- 他的动态栏目 -->
    <view class="section-title-wrap">
      <text class="section-title">他的动态</text>
    </view>

    <!-- 动态卡片 (原型样式) -->
    <view class="contact-post-card">
      <view class="post-user-row">
        <image :src="avatarSrc(contactInfo.avatar)" mode="aspectFill" class="post-mini-avatar" />
        <view class="post-name-col">
          <text class="post-author-name">{{ contactInfo.name }}</text>
          <text class="post-time-text">1分钟前</text>
        </view>
      </view>
      <view class="post-text-box">
        <text class="post-body-text">马玲玲那孩子脾气好又有耐心</text>
      </view>
    </view>

    <view class="elder-no-more">
      <text>——暂无更多——</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { useChatStore } from '../../stores/chat';
import { useSettingStore } from '../../stores/setting';
import { avatarSrc } from '../../utils/avatar';

const chatStore = useChatStore();
const settingStore = useSettingStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const contactInfo = ref({
  name: '',
  avatar: '',
  gender: '男',
  age: 56,
  address: '福建省泉州市',
  metDate: '2024/03/02'
});

onLoad((options) => {
  let found = null;
  if (options && options.id) {
    found = chatStore.contacts.find((c) => c.id === options.id);
  }
  // 兜底：从通讯录读取第一个联系人，避免硬编码
  if (!found && chatStore.contacts.length > 0) {
    found = chatStore.contacts[0];
  }
  if (found) {
    contactInfo.value.name = found.name;
    contactInfo.value.avatar = found.avatar;
  }
});

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
}

.large-avatar {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  margin-right: 24rpx;
  background-color: #F0F2F5;
}

.user-text-col {
  display: flex;
  flex-direction: column;

  .user-name {
    font-size: calc(40rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
    margin-bottom: 12rpx;
  }

  .realname-row {
    display: flex;
    align-items: center;

    .green-dot {
      width: 22rpx;
      height: 22rpx;
      border-radius: 50%;
      background-color: #52C41A;
      margin-right: 12rpx;
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

  &::after {
    border: none;
  }
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
  }

  .archive-val {
    font-size: calc(34rpx * var(--font-scale, 1));
    font-weight: 800;
    color: #1F2329;
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

.btn-hover {
  opacity: 0.88;
}
</style>
