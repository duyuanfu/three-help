<template>
  <view class="my-page-container" :style="pageStyle">
    <!-- 顶部蓝色 Hero：蓝色大圆角背景 + 浅蓝内卡 + 溢出头像（对齐截图） -->
    <view class="top-hero">
      <view class="status-bar"></view>

      <!-- 双层内卡：暗色卡偏右上 + 白色卡偏左下，错位重叠 -->
      <view class="hero-card-dark"></view>
      <view class="hero-card-white">
        <view class="profile-info">
          <text class="user-display-name">{{ userStore.currentUser.name }}</text>
          <text class="gender-age-text">{{ userStore.currentUser.gender }}  {{ userStore.currentUser.age }}岁</text>
          <view class="realname-badge">
            <view class="green-dot"></view>
            <text class="realname-text">{{ userStore.currentUser.isRealName ? '已实名' : '未实名' }}</text>
            <text v-if="userStore.currentUser.identityTag" class="identity-tag">{{ userStore.currentUser.identityTag }}</text>
          </view>
        </view>
      </view>

      <!-- 头像列（顶部溢出浅蓝内卡） -->
      <view class="avatar-col">
        <image :src="displayAvatar" mode="aspectFill" class="large-user-avatar" @error="onAvatarError" />
        <button class="change-avatar-pill" hover-class="btn-hover" @tap="handleChangeAvatar">
          修改头像
        </button>
      </view>
    </view>

    <!-- 适老功能菜单 (原型：连续分组卡片 + 细分割线) -->
    <view class="menu-group">
      <!-- 1. 个人资料 -->
      <view class="menu-item" hover-class="menu-hover" @tap="navigateTo('/pages/my/profile')">
        <view class="menu-left-wrap">
          <image src="../../static/common/menu-1.png" mode="aspectFit" class="menu-icon-img" />
          <text class="menu-title">个人资料</text>
        </view>
        <text class="menu-arrow">→</text>
      </view>

      <!-- 2. 实名认证 -->
      <view class="menu-item" hover-class="menu-hover" @tap="navigateTo('/pages/my/auth')">
        <view class="menu-left-wrap">
          <image src="/static/common/menu-2.png" mode="aspectFit" class="menu-icon-img" />
          <text class="menu-title">实名认证</text>
        </view>
        <text class="menu-arrow">→</text>
      </view>

      <!-- 3. 我的动态 -->
      <view class="menu-item" hover-class="menu-hover" @tap="navigateTo('/pages/my/posts')">
        <view class="menu-left-wrap">
          <image src="/static/common/menu-3.png" mode="aspectFit" class="menu-icon-img" />
          <text class="menu-title">我的动态</text>
        </view>
        <text class="menu-arrow">→</text>
      </view>

      <!-- 4. 我的地址 -->
      <view class="menu-item" hover-class="menu-hover" @tap="navigateTo('/pages/my/address')">
        <view class="menu-left-wrap">
          <image src="/static/common/menu-4.png" mode="aspectFit" class="menu-icon-img" />
          <text class="menu-title">我的地址</text>
        </view>
        <text class="menu-arrow">→</text>
      </view>

      <!-- 5. 字体大小 -->
      <view class="menu-item" hover-class="menu-hover" @tap="navigateTo('/pages/my/font-size')">
        <view class="menu-left-wrap">
          <image src="/static/common/menu-5.png" mode="aspectFit" class="menu-icon-img" />
          <text class="menu-title">字体大小</text>
        </view>
        <text class="menu-arrow">→</text>
      </view>

      <!-- 6. 常见问题 -->
      <view class="menu-item" hover-class="menu-hover" @tap="navigateTo('/pages/my/faq')">
        <view class="menu-left-wrap">
          <image src="/static/common/menu-6.png" mode="aspectFit" class="menu-icon-img" />
          <text class="menu-title">常见问题</text>
        </view>
        <text class="menu-arrow">→</text>
      </view>

      <!-- 7. 管理后台：仅管理员可见 -->
      <view
        v-if="userStore.currentUser.isAdmin"
        class="menu-item"
        hover-class="menu-hover"
        @tap="navigateTo('/pages/admin/index')"
      >
        <view class="menu-left-wrap">
          <image src="/static/common/menu-admin.png" mode="aspectFit" class="menu-icon-img" />
          <text class="menu-title">管理后台</text>
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
import { ref, computed } from 'vue';
import { useUserStore } from '../../stores/user';
import { useSettingStore } from '../../stores/setting';
import { usePullRefresh } from '../../composables/usePullRefresh';
import { chooseAvatar, avatarSrc, LOCAL_DEFAULT_AVATAR } from '../../utils/avatar';
import { resetSnapshot } from '../../utils/db';

const userStore = useUserStore();
const settingStore = useSettingStore();

// 下拉刷新：重新拉取全量数据（五个 tab 页统一行为）
usePullRefresh();

/** 头像统一走 avatarUrl（相对地址转完整地址），加载失败再退回包内默认图 */
const avatarFailed = ref(false);
const displayAvatar = computed(() =>
  avatarFailed.value ? LOCAL_DEFAULT_AVATAR : avatarSrc(userStore.currentUser.avatar)
);
const onAvatarError = () => {
  avatarFailed.value = true;
};

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
    itemList: ['从手机相册选择', '拍照', '恢复默认头像'],
    title: '修改头像',
    success: async (res) => {
      if (res.tapIndex === 2) {
        // 默认头像同样是服务器上的地址
        const ok = await userStore.updateProfile({ avatar: '/uploads/default/avatar.jpg' });
        uni.showToast({
          title: ok ? '已恢复头像' : '保存失败，请重试',
          icon: ok ? 'success' : 'none'
        });
        return;
      }

      // chooseAvatar 内部会把图片上传到服务器，返回 /uploads/... 地址
      const avatarPath = await chooseAvatar(res.tapIndex === 1 ? 'camera' : 'album');
      if (avatarPath) {
        // 换了新图就清掉上次的加载失败标记，否则会一直显示兜底图
        avatarFailed.value = false;
        const ok = await userStore.updateProfile({ avatar: avatarPath });
        uni.showToast({
          title: ok ? '头像已更新' : '保存失败，请重试',
          icon: ok ? 'success' : 'none'
        });
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
    content: '退出后需要重新输入手机号和密码才能登录。',
    confirmText: '确认退出',
    confirmColor: '#FF4D4F',
    cancelText: '取消',
    success: async (res) => {
      if (res.confirm) {
        uni.showLoading({ title: '退出中...', mask: true });
        await userStore.logout();
        // 清掉内存快照，避免下个账号登录前短暂看到上个账号的数据
        resetSnapshot();
        uni.hideLoading();
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

/* 顶部蓝色 Hero：全宽蓝色 + 底部大圆角，内嵌浅蓝内容卡 */
.top-hero {
  position: relative;
  margin: 0 -32rpx;
  height: 560rpx;
  background-color: #1B65F0;
  border-radius: 0 0 60rpx 60rpx;
  overflow: hidden;
}

.status-bar {
  height: 60rpx;
  position: relative;
  z-index: 2;
}

/* 双层内卡：暗色卡在下层偏右上，白色卡在上层偏左下，错位重叠 */
.hero-card-dark {
  position: absolute;
  left: 110rpx;
  right: 32rpx;
  top: 205rpx;
  bottom: 0;
  background-color: #CCDDFA;
  border-radius: 40rpx;
}

.hero-card-white {
  position: absolute;
  left: 56rpx;
  right: 86rpx;
  top: 245rpx;
  bottom: 0;
  background-color: #F2F7FD;
  border-radius: 40rpx;
}

.profile-info {
  padding: 45rpx 0 0 56rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.user-display-name {
  font-size: 64rpx;
  font-weight: 900;
  color: #1F2329;
  letter-spacing: 2rpx;
}

.gender-age-text {
  font-size: 34rpx;
  font-weight: 700;
  color: #1F2329;
  margin-top: 14rpx;
}

.realname-badge {
  display: flex;
  align-items: center;
  margin-top: 52rpx;

  .green-dot {
    width: 26rpx;
    height: 26rpx;
    border-radius: 50%;
    background-color: #52C41A;
    margin-right: 14rpx;
  }

  .realname-text {
    font-size: 32rpx;
    font-weight: 800;
    color: #1F2329;
  }

  .identity-tag {
    margin-left: 14rpx;
    background-color: #FFFFFF;
    color: #1B65F0;
    font-size: 26rpx;
    font-weight: 800;
    padding: 4rpx 14rpx;
    border-radius: 12rpx;
  }
}

/* 头像列：绝对定位，顶部溢出浅蓝内卡压在蓝色上 */
.avatar-col {
  position: absolute;
  top: 160rpx;
  right: 160rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 3;
}

.large-user-avatar {
  width: 210rpx;
  height: 210rpx;
  border-radius: 50%;
  background-color: #F0F2F5;
  display: block;
}

/* 修改头像按钮：头像下方，浅底蓝字 */
.change-avatar-pill {
  margin-top: 44rpx;
  height: 72rpx;
  line-height: 72rpx;
  background-color: #EAF2FF;
  color: #1B65F0;
  font-size: 30rpx;
  font-weight: 800;
  border-radius: 36rpx;
  padding: 0 36rpx;
  border: none;

  &::after {
    border: none;
  }
}

/* 菜单分组卡片 (原型：连续白卡 + 细分割线) */
.menu-group {
  position: relative;
  z-index: 2;
  margin-top: 30rpx;
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 0 36rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 40rpx;
}

.menu-item {
  height: calc(104rpx * var(--font-scale, 1));
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1rpx solid #F0F1F5;

  &:last-child {
    border-bottom: none;
  }
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
    font-size: calc(36rpx * var(--font-scale, 1));
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
  height: calc(108rpx * var(--font-scale, 1));
  line-height: calc(108rpx * var(--font-scale, 1));
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(44rpx * var(--font-scale, 1));
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
