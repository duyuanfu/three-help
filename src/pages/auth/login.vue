<template>
  <view class="login-page-wrapper">
    <!-- 5_登录页 底图 + 25% Hard Light 蓝色叠层（已烘焙进图片，与原型 1:1） -->
    <image 
      src="../../static/login/bg-clean.jpg" 
      mode="aspectFill" 
      class="login-full-bg" 
    />

    <!-- 前台界面内容 (严格按照 docs/ui/登录页.png 布局) -->
    <view class="login-foreground-content">
      <!-- 状态栏占位 -->
      <view class="status-bar"></view>

      <!-- 顶部 Logo 与 品牌标题 -->
      <view class="app-brand-section">
        <view class="logo-box">
          <image src="../../static/launch/logo.png" mode="aspectFit" class="logo-img" />
        </view>
        <text class="brand-title">帮帮帮</text>
      </view>

      <!-- 手机号与密码输入框 (白底大圆角卡片) -->
      <view class="form-inputs-group">
        <view class="input-card">
          <input 
            v-model="phone" 
            type="number" 
            class="elder-input" 
            placeholder="请输入手机号" 
            placeholder-class="placeholder-text"
            placeholder-style="font-weight: 400;"
            maxlength="11"
          />
        </view>
        <view class="input-card">
          <input 
            v-model="password" 
            password 
            class="elder-input" 
            placeholder="请输入密码" 
            placeholder-class="placeholder-text"
            placeholder-style="font-weight: 400;"
          />
        </view>
      </view>

      <!-- 黑色大胶囊按钮 (原型样式) -->
      <view class="action-buttons-group">
        <button class="black-pill-btn login-btn" hover-class="btn-hover" @tap="handleLogin">
          登   录
        </button>
        <button class="black-pill-btn register-btn" hover-class="btn-hover" @tap="goToRegister">
          没有账号？去注册
        </button>
      </view>

      <!-- 辅助链接 -->
      <view class="helper-links-row">
        <text class="link-item" @tap="handleForgot">忘记密码？</text>
        <text class="link-item" @tap="goToFeedback">帮助与反馈</text>
      </view>

      <!-- 其它登录方式 (需求文档明确要求：UI中银联图标改成支付宝) -->
      <view class="third-party-section">
        <view class="divider-line">
          <text class="divider-text">——其它登录方式——</text>
        </view>

        <view class="third-icons-row">
          <!-- 微信 (来自 5_登录页/登录页_微信.png) -->
          <view class="third-icon-item" hover-class="icon-hover" @tap="quickWeChatLogin">
            <image src="../../static/login/wechat-cut.png" mode="aspectFit" class="third-icon-img" />
            <text class="third-label">微信</text>
          </view>

          <!-- 支付宝 (来自 5_登录页/登录页-支付宝支付 (1).png) -->
          <view class="third-icon-item" hover-class="icon-hover" @tap="quickAlipayLogin">
            <image src="/static/login/alipay-cut.png" mode="aspectFit" class="third-icon-img" />
            <text class="third-label">支付宝</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useUserStore } from '../../stores/user';

const userStore = useUserStore();

const phone = ref('13859201234');
const password = ref('123456');

const handleLogin = () => {
  if (!phone.value || phone.value.length < 11) {
    uni.showToast({ title: '请输入有效的11位手机号', icon: 'none' });
    return;
  }
  if (!password.value) {
    uni.showToast({ title: '请输入密码', icon: 'none' });
    return;
  }

  uni.showLoading({ title: '登录中...' });
  setTimeout(() => {
    uni.hideLoading();
    userStore.login();
    uni.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/index/index' });
    }, 500);
  }, 600);
};

const goToRegister = () => {
  uni.navigateTo({ url: '/pages/auth/register' });
};

const handleForgot = () => {
  uni.showModal({
    title: '忘记密码',
    content: '您可以通过已绑定的手机号验证码进行密码重置，或直接选择微信一键快捷登录。',
    confirmText: '微信登录',
    cancelText: '取消',
    confirmColor: '#07C160',
    success: (res) => {
      if (res.confirm) {
        quickWeChatLogin();
      }
    }
  });
};

const goToFeedback = () => {
  uni.navigateTo({
    url: '/pages/my/feedback'
  });
};

const quickWeChatLogin = () => {
  uni.showLoading({ title: '微信授权登录中...' });
  setTimeout(() => {
    uni.hideLoading();
    userStore.login();
    uni.showToast({ title: '微信一键登录成功', icon: 'success' });
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/common/onboarding' });
    }, 500);
  }, 600);
};

const quickAlipayLogin = () => {
  uni.showLoading({ title: '支付宝授权登录中...' });
  setTimeout(() => {
    uni.hideLoading();
    userStore.login();
    uni.showToast({ title: '支付宝一键登录成功', icon: 'success' });
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/common/onboarding' });
    }, 500);
  }, 600);
};
</script>

<style lang="scss" scoped>
.login-page-wrapper {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background-color: #1A6CFF;
}

.login-full-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.login-foreground-content {
  position: relative;
  z-index: 2;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0 72rpx 50rpx 72rpx;
}

.status-bar {
  height: 60rpx;
}

/* 顶部 Logo 与 品牌标题 */
.app-brand-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 86rpx;
  margin-bottom: 80rpx;
}

.logo-box {
  width: 160rpx;
  height: 160rpx;
  background-color: #FFFFFF;
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.15);
  margin-bottom: 20rpx;

  .logo-img {
    width: 120rpx;
    height: 120rpx;
  }
}

.brand-title {
  font-size: 56rpx;
  font-weight: 900;
  color: #000000;
  letter-spacing: 4rpx;
}

/* 输入框 */
.form-inputs-group {
  display: flex;
  flex-direction: column;
  gap: 30rpx;
  margin-bottom: 34rpx;
}

.input-card {
  width: 100%;
  height: 112rpx;
  background-color: #FFFFFF;
  border-radius: 40rpx;
  padding: 0 44rpx;
  display: flex;
  align-items: center;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.08);

  .elder-input {
    width: 100%;
    font-size: 34rpx;
    font-weight: 700;
    color: #1F2329;
  }
}

.placeholder-text {
  color: #B5B9C2;
  font-size: 34rpx;
  font-weight: 500;
}

/* 黑色大胶囊按钮 */
.action-buttons-group {
  display: flex;
  flex-direction: column;
  gap: 30rpx;
  margin-bottom: 24rpx;
}

.black-pill-btn {
  width: 100%;
  height: 112rpx;
  line-height: 112rpx;
  background-color: #000000;
  color: #FFFFFF;
  font-weight: 900;
  border-radius: 56rpx;
  box-shadow: 0 10rpx 28rpx rgba(0, 0, 0, 0.35);
  border: none;
  text-align: center;

  &.login-btn {
    font-size: 48rpx;
    letter-spacing: 16rpx;
  }

  &.register-btn {
    font-size: 40rpx;
    letter-spacing: 4rpx;
  }

  &::after {
    border: none;
  }
}

/* 辅助链接 */
.helper-links-row {
  display: flex;
  justify-content: space-between;
  padding: 0 20rpx;
  margin-bottom: 60rpx;

  .link-item {
    font-size: 30rpx;
    font-weight: 400;
    color: rgba(255, 255, 255, 0.85);
    letter-spacing: 1rpx;
  }
}

/* 其它登录方式 */
.third-party-section {
  // margin-top: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.divider-line {
  margin-bottom: 28rpx;

  .divider-text {
    font-size: 28rpx;
    color: rgba(255, 255, 255, 0.85);
    letter-spacing: 2rpx;
    font-weight: 400;
  }
}

.third-icons-row {
  display: flex;
  justify-content: center;
  gap: 160rpx;
}

.third-icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.third-icon-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: 30rpx;
  margin-bottom: 12rpx;
}

.third-label {
  font-size: 30rpx;
  font-weight: 400;
  color: #FFFFFF;
}

.btn-hover {
  opacity: 0.86;
  transform: scale(0.985);
}

.icon-hover {
  opacity: 0.85;
}
</style>
