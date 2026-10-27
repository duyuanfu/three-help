<template>
  <view class="address-page-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航栏 (严格对齐个人信息页3.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">我的地址</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 地址列表 (严格对齐原型白底/灰底卡片) -->
    <scroll-view scroll-y class="address-scroll-list">
      <view 
        v-for="(addr, idx) in addressList" 
        :key="idx" 
        class="address-item-card"
        :class="{ 'card-inactive': !addr.isDefault }"
      >
        <view class="address-meta-row">
          <text class="user-info-text">{{ addr.name }}  {{ addr.phone }}</text>
        </view>
        <view class="address-detail-row">
          <text class="address-detail-text">{{ addr.detail }}</text>
        </view>

        <!-- 底部操作单选框 (使用 / 删除) -->
        <view class="address-actions-row">
          <view class="radio-item" @tap="setDefaultAddress(idx)">
            <view class="custom-radio-box" :class="{ 'radio-checked': addr.isDefault }">
              <image v-if="addr.isDefault" src="/static/common/check.png" mode="aspectFit" class="check-img" />
            </view>
            <text class="radio-label">使用</text>
          </view>

          <view class="radio-item" @tap="deleteAddress(idx)">
            <view class="custom-radio-box delete-box">
              <text class="delete-icon">✖</text>
            </view>
            <text class="radio-label">删除</text>
          </view>
        </view>
      </view>

      <!-- + 新增地址大按钮 (原型白底圆角样式) -->
      <view class="add-addr-btn-wrap">
        <button class="add-address-btn" hover-class="btn-hover" @tap="addNewAddress">
          + 新增地址
        </button>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useUserStore } from '../../stores/user';
import { useSettingStore } from '../../stores/setting';

const userStore = useUserStore();
const settingStore = useSettingStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const addressList = ref([
  {
    name: '张冬梅',
    phone: '12345678987',
    detail: '福建省泉州市丰泽区凤阳小区8号楼302',
    isDefault: true
  },
  {
    name: '张晓梅',
    phone: '12345678987',
    detail: '福建省厦门市集美区杏林湾路***',
    isDefault: false
  }
]);

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const setDefaultAddress = (idx: number) => {
  addressList.value.forEach((item, i) => {
    item.isDefault = i === idx;
  });
  userStore.updateProfile({ address: addressList.value[idx].detail });
  uni.showToast({ title: '已设为默认地址', icon: 'success' });
};

const deleteAddress = (idx: number) => {
  if (addressList.value.length <= 1) {
    uni.showToast({ title: '至少保留一个常用地址', icon: 'none' });
    return;
  }
  addressList.value.splice(idx, 1);
  uni.showToast({ title: '地址已删除', icon: 'success' });
};

const addNewAddress = () => {
  uni.showModal({
    title: '新增收货地址',
    editable: true,
    placeholderText: '请输入详细地址，如“泉州市鲤城区西街...”',
    success: (res) => {
      if (res.confirm && res.content) {
        addressList.value.push({
          name: userStore.currentUser.name,
          phone: '138****5920',
          detail: res.content,
          isDefault: false
        });
        uni.showToast({ title: '新增成功', icon: 'success' });
      }
    }
  });
};
</script>

<style lang="scss" scoped>
.address-page-container {
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

.address-scroll-list {
  flex: 1;
}

/* 地址卡片 (原型白底/暗底大圆角) */
.address-item-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 36rpx 32rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 30rpx;
}

.card-inactive {
  background-color: #9A9C9F;
  color: #FFFFFF;

  .user-info-text, .address-detail-text, .radio-label {
    color: #1F2329 !important;
  }
}

.address-meta-row {
  margin-bottom: 14rpx;

  .user-info-text {
    font-size: calc(36rpx * var(--font-scale));
    font-weight: 900;
    color: #1F2329;
  }
}

.address-detail-row {
  margin-bottom: 28rpx;

  .address-detail-text {
    font-size: calc(32rpx * var(--font-scale));
    font-weight: 800;
    color: #1F2329;
    line-height: 1.4;
  }
}

.address-actions-row {
  display: flex;
  align-items: center;
  gap: 80rpx;
}

.radio-item {
  display: flex;
  align-items: center;

  .custom-radio-box {
    width: 48rpx;
    height: 48rpx;
    border-radius: 50%;
    border: 3rpx solid #1F2329;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 14rpx;
    background-color: #FFFFFF;

    .check-img {
      width: 32rpx;
      height: 32rpx;
    }
  }

  .delete-box {
    border-color: #A0A5AE;
    background-color: #E2E4E8;

    .delete-icon {
      font-size: 24rpx;
      color: #666;
    }
  }

  .radio-checked {
    background-color: #FFFFFF;
  }

  .radio-label {
    font-size: calc(34rpx * var(--font-scale));
    font-weight: 900;
    color: #1F2329;
  }
}

/* + 新增地址按钮 (原型样式) */
.add-addr-btn-wrap {
  margin-top: 40rpx;
  margin-bottom: 40rpx;
}

.add-address-btn {
  background-color: #FFFFFF;
  color: #1F2329;
  border: 3rpx solid #E5E7EB;
  border-radius: 40rpx;
  height: calc(108rpx * var(--font-scale));
  line-height: calc(108rpx * var(--font-scale));
  font-size: calc(38rpx * var(--font-scale));
  font-weight: 900;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);

  &::after {
    border: none;
  }
}

.btn-hover {
  opacity: 0.88;
}
</style>
