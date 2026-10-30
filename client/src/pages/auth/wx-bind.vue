<template>
  <view class="wx-bind-container" :style="pageStyle">
    <view class="header">
      <text class="title">绑定手机号</text>
      <text class="subtitle">微信登录需要绑定手机号，用于账号识别与找回</text>
    </view>

    <view class="form-group">
      <view class="field">
        <text class="field-label">姓名</text>
        <input
          v-model="name"
          class="field-input"
          placeholder="首次使用请填写真实姓名"
          placeholder-class="placeholder-text"
          maxlength="20"
        />
      </view>

      <view class="field">
        <text class="field-label">手机号</text>
        <input
          v-model="phone"
          type="number"
          class="field-input"
          placeholder="请输入 11 位手机号"
          placeholder-class="placeholder-text"
          maxlength="11"
        />
      </view>

      <view class="field">
        <text class="field-label">密码</text>
        <input
          v-model="password"
          password
          class="field-input"
          placeholder="至少 4 位，已有账号请填原密码"
          placeholder-class="placeholder-text"
        />
      </view>
    </view>

    <view class="tip-box">
      <text class="tip-text">手机号已注册过：请输入该账号的原密码，绑定后可用微信一键登录。</text>
      <text class="tip-text">手机号未注册：将直接用该手机号创建账号，密码用于下次登录。</text>
    </view>

    <button class="bind-btn" hover-class="btn-hover" @tap="handleBind">
      完 成 绑 定
    </button>

    <view class="back-row" hover-class="btn-hover" @tap="handleBack">
      <text>← 返回登录</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { useUserStore } from '../../stores/user';
import { useSettingStore } from '../../stores/setting';
import { refreshAfterLogin } from '../../utils/session';

const userStore = useUserStore();
const settingStore = useSettingStore();

const ticket = ref('');
const name = ref('');
const phone = ref('');
const password = ref('');
const submitting = ref(false);

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

onLoad((options) => {
  ticket.value = (options && options.ticket) || '';
  if (!ticket.value) {
    uni.showToast({ title: '微信授权已失效，请重新登录', icon: 'none' });
    setTimeout(() => uni.navigateBack({ delta: 1 }), 1200);
  }
});

const handleBack = () => {
  uni.navigateBack({ delta: 1 });
};

const handleBind = async () => {
  if (submitting.value) return;
  if (!ticket.value) {
    uni.showToast({ title: '微信授权已失效，请重新登录', icon: 'none' });
    return;
  }
  if (phone.value.length !== 11) {
    uni.showToast({ title: '请输入有效的 11 位手机号', icon: 'none' });
    return;
  }
  if (!password.value || password.value.length < 4) {
    uni.showToast({ title: '密码至少 4 位', icon: 'none' });
    return;
  }

  submitting.value = true;
  uni.showLoading({ title: '绑定中...', mask: true });
  try {
    const user = await userStore.bindWeChat({
      ticket: ticket.value,
      phone: phone.value,
      password: password.value,
      name: name.value.trim() || undefined
    });
    // 绑定成功即代表已登录，拉一次业务数据，否则首页是空的
    uni.showLoading({ title: '加载数据中...', mask: true });
    await refreshAfterLogin();
    uni.hideLoading();
    uni.showToast({ title: `绑定成功，${user.name}`, icon: 'none' });
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/index/index' });
    }, 700);
  } catch (e: any) {
    uni.hideLoading();
    uni.showToast({ title: e?.message || '绑定失败', icon: 'none', duration: 2500 });
  } finally {
    submitting.value = false;
  }
};
</script>

<style lang="scss" scoped>
.wx-bind-container {
  min-height: 100vh;
  background-color: #F6F7FB;
  padding: 60rpx 44rpx;
  box-sizing: border-box;
}

.header {
  display: flex;
  flex-direction: column;
  margin-bottom: 48rpx;

  .title {
    font-size: calc(52rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
    letter-spacing: 2rpx;
  }

  .subtitle {
    margin-top: 16rpx;
    font-size: calc(28rpx * var(--font-scale, 1));
    color: #7A8088;
    line-height: 1.5;
  }
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.field {
  background-color: #FFFFFF;
  border-radius: 32rpx;
  padding: 24rpx 36rpx;
  display: flex;
  flex-direction: column;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);

  .field-label {
    font-size: calc(26rpx * var(--font-scale, 1));
    font-weight: 700;
    color: #7A8088;
    margin-bottom: 10rpx;
  }

  .field-input {
    width: 100%;
    height: calc(64rpx * var(--font-scale, 1));
    font-size: calc(36rpx * var(--font-scale, 1));
    font-weight: 800;
    color: #1F2329;
  }
}

.placeholder-text {
  color: #B0B5BD;
  font-size: calc(32rpx * var(--font-scale, 1));
  font-weight: 400;
}

.tip-box {
  margin-top: 28rpx;
  padding: 0 12rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;

  .tip-text {
    font-size: calc(26rpx * var(--font-scale, 1));
    color: #9AA0A8;
    line-height: 1.6;
  }
}

.bind-btn {
  margin-top: 56rpx;
  width: 100%;
  height: calc(112rpx * var(--font-scale, 1));
  line-height: calc(112rpx * var(--font-scale, 1));
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(44rpx * var(--font-scale, 1));
  font-weight: 900;
  border-radius: 36rpx;
  letter-spacing: 8rpx;
  border: none;
  box-shadow: 0 10rpx 28rpx rgba(27, 101, 240, 0.28);

  &::after {
    border: none;
  }
}

.back-row {
  margin-top: 32rpx;
  text-align: center;
  font-size: calc(28rpx * var(--font-scale, 1));
  font-weight: 600;
  color: #7A8088;
}

.btn-hover {
  opacity: 0.88;
}
</style>
