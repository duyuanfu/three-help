<template>
  <view class="profile-page-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航栏 (严格对齐个人信息页2.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">个人资料</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 资料表单列表 (原型白底整行分割线) -->
    <view class="profile-form-body">
      <!-- 1. 头像 -->
      <view class="form-row-item" @tap="changeAvatar">
        <text class="row-label">头像 ：</text>
        <view class="row-val-right">
          <image :src="userStore.currentUser.avatar" mode="aspectFill" class="profile-avatar" />
        </view>
      </view>

      <!-- 2. 姓名 -->
      <view class="form-row-item" @tap="editField('name', '姓名', userStore.currentUser.name)">
        <text class="row-label">姓名 ：</text>
        <text class="row-val-text">{{ userStore.currentUser.name }}</text>
      </view>

      <!-- 3. 性别 -->
      <view class="form-row-item" @tap="toggleGender">
        <text class="row-label">性别 ：</text>
        <text class="row-val-text">{{ userStore.currentUser.gender }}</text>
      </view>

      <!-- 4. 年龄 -->
      <view class="form-row-item" @tap="editAge">
        <text class="row-label">年龄 ：</text>
        <text class="row-val-text">{{ userStore.currentUser.age }}岁</text>
      </view>

      <!-- 5. 职业 -->
      <view class="form-row-item" @tap="editField('occupation', '职业', userStore.currentUser.occupation)">
        <text class="row-label">职业 ：</text>
        <text class="row-val-text">{{ userStore.currentUser.occupation || '退休职工' }}</text>
      </view>

      <!-- 6. 地址 -->
      <view class="form-row-item" @tap="editField('address', '地址', userStore.currentUser.address)">
        <text class="row-label">地址 ：</text>
        <text class="row-val-text text-truncate">{{ userStore.currentUser.address }}</text>
      </view>

      <!-- 7. 组织 -->
      <view class="form-row-item" @tap="editField('organization', '组织', userStore.currentUser.organization)">
        <text class="row-label">组织 ：</text>
        <text class="row-val-text">{{ userStore.currentUser.organization || '群众' }}</text>
      </view>
    </view>

    <!-- 底部蓝色完成大按钮 (原型样式) -->
    <view class="bottom-action-wrap">
      <button class="complete-btn" hover-class="btn-hover" @tap="handleComplete">
        完 成
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useUserStore } from '../../stores/user';
import { useSettingStore } from '../../stores/setting';
import { chooseAvatar } from '../../utils/avatar';

const userStore = useUserStore();
const settingStore = useSettingStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const changeAvatar = () => {
  uni.showActionSheet({
    itemList: ['拍照', '从相册选择'],
    success: async (res) => {
      const avatarPath = await chooseAvatar(res.tapIndex === 0 ? 'camera' : 'album');
      if (avatarPath) {
        userStore.updateProfile({ avatar: avatarPath });
        uni.showToast({ title: '头像更新成功', icon: 'success' });
      }
    }
  });
};

const toggleGender = () => {
  const newGender = userStore.currentUser.gender === '女' ? '男' : '女';
  userStore.updateProfile({ gender: newGender });
  uni.showToast({ title: `已设置为 ${newGender}`, icon: 'none' });
};

const editAge = () => {
  uni.showModal({
    title: '修改年龄',
    editable: true,
    content: String(userStore.currentUser.age),
    placeholderText: '请输入年龄',
    success: (res) => {
      if (res.confirm && res.content) {
        const val = parseInt(res.content) || 65;
        userStore.updateProfile({ age: val });
      }
    }
  });
};

const editField = (key: string, label: string, currentVal: string) => {
  uni.showModal({
    title: `修改${label}`,
    editable: true,
    content: currentVal,
    placeholderText: `请输入${label}`,
    success: (res) => {
      if (res.confirm && res.content) {
        userStore.updateProfile({ [key]: res.content });
        uni.showToast({ title: '已保存', icon: 'success' });
      }
    }
  });
};

const handleComplete = () => {
  uni.showToast({ title: '个人资料保存成功', icon: 'success' });
  setTimeout(() => {
    uni.navigateBack({ delta: 1 });
  }, 600);
};
</script>

<style lang="scss" scoped>
.profile-page-container {
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
  padding: 20rpx 0 28rpx 0;
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

/* 表单列表 (纯白背景横线分割，严格对齐个人信息页2.png) */
.profile-form-body {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 0 36rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 60rpx;
}

.form-row-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: calc(108rpx * var(--font-scale, 1));
  padding: 20rpx 0;
  border-bottom: 2rpx solid #E5E7EB;

  &:last-child {
    border-bottom: none;
  }
}

.row-label {
  font-size: calc(36rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1F2329;
  width: 170rpx;
}

.row-val-text {
  font-size: calc(34rpx * var(--font-scale, 1));
  font-weight: 800;
  color: #1F2329;
  text-align: right;
  flex: 1;
}

.text-truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-avatar {
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  background-color: #F0F2F5;
}

/* 底部完成按钮 */
.bottom-action-wrap {
  margin-top: auto;
  margin-bottom: 40rpx;
}

.complete-btn {
  width: 100%;
  height: calc(108rpx * var(--font-scale, 1));
  line-height: calc(108rpx * var(--font-scale, 1));
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(44rpx * var(--font-scale, 1));
  font-weight: 900;
  border-radius: 36rpx;
  letter-spacing: 8rpx;
  box-shadow: 0 10rpx 28rpx rgba(27, 101, 240, 0.35);
  border: none;

  &::after {
    border: none;
  }
}

.btn-hover {
  opacity: 0.88;
}
</style>
