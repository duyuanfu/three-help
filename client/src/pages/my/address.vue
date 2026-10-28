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
        <button class="add-address-btn" hover-class="btn-hover" @tap="openAddDialog">
          + 新增地址
        </button>
      </view>
    </scroll-view>

    <!-- 新增地址弹窗 (姓名 + 手机号 + 详细地址) -->
    <view v-if="showAddDialog" class="dialog-mask" @tap="closeAddDialog">
      <view class="dialog-card" @tap.stop>
        <text class="dialog-title">新增收货地址</text>

        <view class="dialog-field">
          <text class="field-label">姓名</text>
          <input
            v-model="form.name"
            class="field-input"
            placeholder="请输入收件人姓名"
            placeholder-class="field-placeholder"
          />
        </view>

        <view class="dialog-field">
          <text class="field-label">手机号</text>
          <input
            v-model="form.phone"
            class="field-input"
            type="number"
            maxlength="11"
            placeholder="请输入11位手机号"
            placeholder-class="field-placeholder"
          />
        </view>

        <view class="dialog-field">
          <text class="field-label">详细地址</text>
          <input
            v-model="form.detail"
            class="field-input"
            placeholder="如：泉州市鲤城区西街..."
            placeholder-class="field-placeholder"
          />
        </view>

        <view class="dialog-btn-row">
          <button class="dialog-btn cancel" hover-class="btn-hover" @tap="closeAddDialog">取消</button>
          <button class="dialog-btn confirm" hover-class="btn-hover" @tap="confirmAddAddress">确定</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useUserStore } from '../../stores/user';
import { useSettingStore } from '../../stores/setting';
import { TABLES } from '../../utils/db';

const userStore = useUserStore();
const settingStore = useSettingStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

interface AddressItem {
  id: string;
  name: string;
  phone: string;
  detail: string;
  isDefault: boolean;
}

const DEFAULT_ADDRESSES: AddressItem[] = [
  {
    id: 'addr_default_1',
    name: '',
    phone: '12345678987',
    detail: '福建省泉州市丰泽区凤阳小区8号楼302',
    isDefault: true
  },
  {
    id: 'addr_default_2',
    name: '张晓梅',
    phone: '12345678987',
    detail: '福建省厦门市集美区杏林湾路***',
    isDefault: false
  }
];

// 默认地址收件人从 db 当前用户读取，避免硬编码姓名
const addressSeeds: AddressItem[] = DEFAULT_ADDRESSES.map((a, i) =>
  i === 0 ? { ...a, name: userStore.currentUser.name } : a
);

// 地址表落库：首次进入注入默认地址，后续增删改全部持久化
const persistingList: AddressItem[] = TABLES.addresses.all(addressSeeds);

const addressList = ref<AddressItem[]>(persistingList);

const saveAddresses = () => {
  TABLES.addresses.replaceAll(addressList.value);
};

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const setDefaultAddress = (idx: number) => {
  addressList.value.forEach((item, i) => {
    item.isDefault = i === idx;
  });
  saveAddresses();
  userStore.updateProfile({ address: addressList.value[idx].detail });
  uni.showToast({ title: '已设为默认地址', icon: 'success' });
};

const deleteAddress = (idx: number) => {
  if (addressList.value.length <= 1) {
    uni.showToast({ title: '至少保留一个常用地址', icon: 'none' });
    return;
  }
  addressList.value.splice(idx, 1);
  saveAddresses();
  uni.showToast({ title: '地址已删除', icon: 'success' });
};

// 新增地址弹窗：支持录入姓名、手机号、详细地址
const showAddDialog = ref(false);
const form = ref({
  name: '',
  phone: '',
  detail: ''
});

const openAddDialog = () => {
  form.value = {
    name: userStore.currentUser.name,
    phone: '',
    detail: ''
  };
  showAddDialog.value = true;
};

const closeAddDialog = () => {
  showAddDialog.value = false;
};

const confirmAddAddress = () => {
  const name = form.value.name.trim();
  const phone = form.value.phone.trim();
  const detail = form.value.detail.trim();

  if (!name) {
    uni.showToast({ title: '请输入收件人姓名', icon: 'none' });
    return;
  }
  if (!phone || phone.length !== 11) {
    uni.showToast({ title: '请输入有效的11位手机号', icon: 'none' });
    return;
  }
  if (!detail) {
    uni.showToast({ title: '请输入详细地址', icon: 'none' });
    return;
  }

  addressList.value.push({
    id: 'addr_' + Date.now(),
    name,
    phone,
    detail,
    isDefault: false
  });
  saveAddresses();
  showAddDialog.value = false;
  uni.showToast({ title: '新增成功', icon: 'success' });
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
    font-size: calc(36rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
  }
}

.address-detail-row {
  margin-bottom: 28rpx;

  .address-detail-text {
    font-size: calc(32rpx * var(--font-scale, 1));
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
    font-size: calc(34rpx * var(--font-scale, 1));
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
  height: calc(108rpx * var(--font-scale, 1));
  line-height: calc(108rpx * var(--font-scale, 1));
  font-size: calc(38rpx * var(--font-scale, 1));
  font-weight: 900;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);

  &::after {
    border: none;
  }
}

.btn-hover {
  opacity: 0.88;
}

/* 新增地址弹窗 */
.dialog-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.45);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dialog-card {
  width: 620rpx;
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 40rpx 36rpx 32rpx 36rpx;
  box-shadow: 0 12rpx 40rpx rgba(0, 0, 0, 0.18);
}

.dialog-title {
  display: block;
  text-align: center;
  font-size: calc(36rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1F2329;
  margin-bottom: 32rpx;
}

.dialog-field {
  display: flex;
  align-items: center;
  background-color: #F6F7FB;
  border-radius: 24rpx;
  height: calc(96rpx * var(--font-scale, 1));
  padding: 0 28rpx;
  margin-bottom: 24rpx;

  .field-label {
    width: 150rpx;
    font-size: calc(30rpx * var(--font-scale, 1));
    font-weight: 800;
    color: #1F2329;
    flex-shrink: 0;
  }

  .field-input {
    flex: 1;
    font-size: calc(30rpx * var(--font-scale, 1));
    color: #1F2329;
  }
}

.field-placeholder {
  color: #B0B4BA;
  font-size: calc(28rpx * var(--font-scale, 1));
}

.dialog-btn-row {
  display: flex;
  gap: 24rpx;
  margin-top: 12rpx;
}

.dialog-btn {
  flex: 1;
  height: calc(92rpx * var(--font-scale, 1));
  line-height: calc(92rpx * var(--font-scale, 1));
  border-radius: 40rpx;
  font-size: calc(32rpx * var(--font-scale, 1));
  font-weight: 800;
  border: none;

  &::after {
    border: none;
  }

  &.cancel {
    background-color: #F1F2F5;
    color: #1F2329;
  }

  &.confirm {
    background-color: #1B65F0;
    color: #FFFFFF;
    box-shadow: 0 8rpx 24rpx rgba(27, 101, 240, 0.25);
  }
}
</style>
