<template>
  <view class="my-page-container" :style="pageStyle">
    <!-- 顶部蓝色背景曲线 (原型背景) -->
    <view class="top-blue-curve"></view>

    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 个人资料卡片 (严格对齐个人信息页1.png) -->
    <view class="user-profile-card">
      <view class="profile-left-col">
        <text class="user-display-name">{{ userStore.currentUser.name }}</text>
        <view class="gender-age-row">
          <text class="gender-age-text">{{ userStore.currentUser.gender }}  {{ userStore.currentUser.age }}岁</text>
        </view>
        <view class="realname-badge">
          <view class="green-dot"></view>
          <text class="realname-text">{{ userStore.currentUser.isRealName ? '已实名' : '未实名' }}</text>
          <text v-if="userStore.currentUser.identityTag" class="identity-tag">{{ userStore.currentUser.identityTag }}</text>
        </view>
      </view>

      <view class="profile-right-col">
        <image :src="userStore.currentUser.avatar" mode="aspectFill" class="large-user-avatar" />
        <button class="change-avatar-pill" hover-class="btn-hover" @tap="handleChangeAvatar">
          修改头像
        </button>
      </view>
    </view>

    <!-- 适老功能菜单项列表 (原型白底圆角大卡片) -->
    <view class="menu-items-group">
      <!-- 1. 个人资料 -->
      <view class="menu-card-item" hover-class="menu-hover" @tap="navigateTo('/pages/my/profile')">
        <view class="menu-left-wrap">
          <image src="../../static/common/menu-1.png" mode="aspectFit" class="menu-icon-img" />
          <text class="menu-title">个人资料</text>
        </view>
        <text class="menu-arrow">→</text>
      </view>

      <!-- 2. 实名认证 -->
      <view class="menu-card-item" hover-class="menu-hover" @tap="navigateTo('/pages/my/auth')">
        <view class="menu-left-wrap">
          <image src="/static/common/menu-2.png" mode="aspectFit" class="menu-icon-img" />
          <text class="menu-title">实名认证</text>
        </view>
        <text class="menu-arrow">→</text>
      </view>

      <!-- 3. 我的动态 -->
      <view class="menu-card-item" hover-class="menu-hover" @tap="navigateTo('/pages/my/posts')">
        <view class="menu-left-wrap">
          <image src="/static/common/menu-3.png" mode="aspectFit" class="menu-icon-img" />
          <text class="menu-title">我的动态</text>
        </view>
        <text class="menu-arrow">→</text>
      </view>

      <!-- 4. 我的地址 -->
      <view class="menu-card-item" hover-class="menu-hover" @tap="navigateTo('/pages/my/address')">
        <view class="menu-left-wrap">
          <image src="/static/common/menu-4.png" mode="aspectFit" class="menu-icon-img" />
          <text class="menu-title">我的地址</text>
        </view>
        <text class="menu-arrow">→</text>
      </view>

      <!-- 5. 字体大小 -->
      <view class="menu-card-item" hover-class="menu-hover" @tap="navigateTo('/pages/my/font-size')">
        <view class="menu-left-wrap">
          <image src="/static/common/menu-5.png" mode="aspectFit" class="menu-icon-img" />
          <text class="menu-title">字体大小</text>
        </view>
        <text class="menu-arrow">→</text>
      </view>

      <!-- 6. 常见问题 -->
      <view class="menu-card-item" hover-class="menu-hover" @tap="navigateTo('/pages/my/faq')">
        <view class="menu-left-wrap">
          <image src="/static/common/menu-6.png" mode="aspectFit" class="menu-icon-img" />
          <text class="menu-title">常见问题</text>
        </view>
        <text class="menu-arrow">→</text>
      </view>
    </view>

    <!-- 底部退出登录大蓝按钮 -->
    <view class="logout-action-wrap">
      <button class="logout-btn" hover-class="btn-hover" @tap="handleLogout">
        退出登录
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useUserStore } from '../../stores/user';
import { useSettingStore } from '../../stores/setting';

const userStore = useUserStore();
const settingStore = useSettingStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const navigateTo = (url: string) => {
  uni.navigateTo({ url });
};

const goToMyPosts = () => {
  uni.switchTab({ url: '/pages/square/index' });
};

const handleChangeAvatar = () => {
  uni.showActionSheet({
    itemList: ['从手机相册选择', '拍照', '恢复默认原型头像'],
    title: '修改头像',
    success: (res) => {
      if (res.tapIndex === 2) {
        userStore.updateProfile({ avatar: '/static/avatars/zhangdongmei.png' });
        uni.showToast({ title: '已恢复头像', icon: 'success' });
      } else {
        uni.showToast({ title: '头像已更新', icon: 'success' });
      }
    }
  });
};

const showAddressManager = () => {
  uni.showModal({
    title: '我的地址',
    content: `当前默认地址：\n${userStore.currentUser.address}\n收件人：${userStore.currentUser.name} (138****5920)`,
    confirmText: '修改地址',
    cancelText: '知道了',
    confirmColor: '#1B65F0',
    success: (res) => {
      if (res.confirm) {
        uni.showModal({
          title: '输入新地址',
          editable: true,
          content: userStore.currentUser.address,
          success: (editRes) => {
            if (editRes.confirm && editRes.content) {
              userStore.updateProfile({ address: editRes.content });
              uni.showToast({ title: '地址修改成功', icon: 'success' });
            }
          }
        });
      }
    }
  });
};

const handleLogout = () => {
  uni.showModal({
    title: '退出登录',
    content: '退出后您仍可以随时使用手机号或微信一键重新登录。',
    confirmText: '确认退出',
    confirmColor: '#FF4D4F',
    cancelText: '取消',
    success: (res) => {
      if (res.confirm) {
        userStore.logout();
        uni.reLaunch({
          url: '/pages/auth/login'
        });
      }
    }
  });
};
</script>

<style lang="scss" scoped>
.my-page-container {
  min-height: 100vh;
  background-color: #F6F7FB;
  padding: 0 32rpx 140rpx 32rpx;
  display: flex;
  flex-direction: column;
  position: relative;
}

.top-blue-curve {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 280rpx;
  background-color: #1B65F0;
  border-bottom-left-radius: 60rpx;
  border-bottom-right-radius: 60rpx;
  z-index: 1;
}

.status-bar {
  height: 60rpx;
  position: relative;
  z-index: 2;
}

/* 个人资料大卡片 (原型重叠样式) */
.user-profile-card {
  position: relative;
  z-index: 2;
  margin-top: 60rpx;
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 36rpx 40rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.08);
  margin-bottom: 36rpx;
}

.profile-left-col {
  display: flex;
  flex-direction: column;
}

.user-display-name {
  font-size: calc(52rpx * var(--font-scale));
  font-weight: 900;
  color: #1F2329;
  letter-spacing: 2rpx;
  margin-bottom: 12rpx;
}

.gender-age-row {
  margin-bottom: 16rpx;

  .gender-age-text {
    font-size: calc(34rpx * var(--font-scale));
    font-weight: 800;
    color: #1F2329;
  }
}

.realname-badge {
  display: flex;
  align-items: center;

  .green-dot {
    width: 24rpx;
    height: 24rpx;
    border-radius: 50%;
    background-color: #52C41A;
    margin-right: 12rpx;
  }

  .realname-text {
    font-size: calc(32rpx * var(--font-scale));
    font-weight: 900;
    color: #1F2329;
  }

  .identity-tag {
    margin-left: 14rpx;
    background-color: #EBF2FF;
    color: #1B65F0;
    font-size: calc(26rpx * var(--font-scale));
    font-weight: 800;
    padding: 4rpx 14rpx;
    border-radius: 12rpx;
  }
}

.profile-right-col {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.large-user-avatar {
  width: 170rpx;
  height: 170rpx;
  border-radius: 50%;
  margin-bottom: 12rpx;
  background-color: #F0F2F5;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.1);
}

.change-avatar-pill {
  height: calc(56rpx * var(--font-scale));
  line-height: calc(56rpx * var(--font-scale));
  background-color: #EBF2FF;
  color: #1B65F0;
  font-size: calc(26rpx * var(--font-scale));
  font-weight: 800;
  border-radius: 28rpx;
  padding: 0 20rpx;
  border: none;

  &::after {
    border: none;
  }
}

/* 菜单项大卡片列表 (原型大圆角、右箭头) */
.menu-items-group {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  margin-bottom: 40rpx;
}

.menu-card-item {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  height: calc(108rpx * var(--font-scale));
  padding: 0 36rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.menu-left-wrap {
  display: flex;
  align-items: center;

  .menu-icon-img {
    width: 48rpx;
    height: 48rpx;
    margin-right: 28rpx;
  }

  .menu-title {
    font-size: calc(36rpx * var(--font-scale));
    font-weight: 800;
    color: #1F2329;
    letter-spacing: 2rpx;
  }
}

.menu-arrow {
  font-size: 38rpx;
  color: #1F2329;
  font-weight: 900;
}

/* 退出登录大蓝按钮 */
.logout-action-wrap {
  position: relative;
  z-index: 2;
  margin-top: 10rpx;
  margin-bottom: 30rpx;
}

.logout-btn {
  width: 100%;
  height: calc(108rpx * var(--font-scale));
  line-height: calc(108rpx * var(--font-scale));
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(44rpx * var(--font-scale));
  font-weight: 900;
  border-radius: 36rpx;
  letter-spacing: 6rpx;
  box-shadow: 0 10rpx 28rpx rgba(27, 101, 240, 0.35);
  border: none;

  &::after {
    border: none;
  }
}

.menu-hover, .btn-hover {
  opacity: 0.88;
}
</style>
