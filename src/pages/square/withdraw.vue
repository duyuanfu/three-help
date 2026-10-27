<template>
  <view class="withdraw-page-container" :style="pageStyle">
    <!-- 顶部蓝色导航栏 (严格对齐提现.png) -->
    <view class="blue-header-nav">
      <view class="nav-left" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">提 现</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 到账账户行 (原型样式) -->
    <view class="account-row">
      <text class="account-label">到账</text>
      <view class="wechat-badge">
        <text class="wechat-icon">💬</text>
      </view>
      <text class="account-name">微信零钱</text>
      <button class="modify-account-btn" hover-class="btn-hover" @tap="modifyAccount">
        修改
      </button>
    </view>

    <!-- 灰色分隔条 -->
    <view class="gray-bar"></view>

    <!-- 提现金额表单 -->
    <view class="withdraw-body">
      <text class="field-title">提现金额</text>

      <!-- 提现输入大白卡片 (原型带￥符号) -->
      <view class="amount-card">
        <text class="currency-symbol">￥</text>
        <input 
          v-model="withdrawYuan" 
          type="digit" 
          class="amount-input" 
          placeholder="0.00" 
          placeholder-class="placeholder-amount"
        />
      </view>

      <!-- 合计与剩余双框 (原型样式) -->
      <view class="summary-boxes-row">
        <view class="summary-box">
          <text class="summary-text">合计 ： {{ userStore.currentUser.points }}</text>
        </view>
        <view class="summary-box">
          <text class="summary-text">剩余 ： {{ remainingCoins }}</text>
        </view>
      </view>

      <!-- 提现比例提示 (原型灰字) -->
      <view class="ratio-hint-wrap">
        <text class="ratio-hint-text">当前提现比例100金币=1元现金</text>
      </view>

      <!-- 确定提现大蓝按钮 -->
      <view class="submit-action-wrap">
        <button class="confirm-withdraw-btn" hover-class="btn-hover" @tap="handleWithdraw">
          确定提现
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useUserStore } from '../../stores/user';
import { useSettingStore } from '../../stores/setting';

const userStore = useUserStore();
const settingStore = useSettingStore();

const withdrawYuan = ref('10');

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const needCoins = computed(() => {
  const val = parseFloat(withdrawYuan.value) || 0;
  return Math.round(val * 100);
});

const remainingCoins = computed(() => {
  return Math.max(0, userStore.currentUser.points - needCoins.value);
});

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const modifyAccount = () => {
  uni.showActionSheet({
    itemList: ['微信零钱 (杜** 138****5920)', '支付宝 (du***@yeah.net)'],
    title: '选择提现到账方式',
    success: (res) => {
      uni.showToast({ title: '已切换提现账户', icon: 'success' });
    }
  });
};

const handleWithdraw = () => {
  const coins = needCoins.value;
  if (coins <= 0) {
    uni.showToast({ title: '请输入提现金额', icon: 'none' });
    return;
  }
  if (userStore.currentUser.points < coins) {
    uni.showToast({ title: '金币余额不足', icon: 'none' });
    return;
  }

  userStore.deductPoints(coins);

  // 跳转提现成功页 (原型：提现成功.png)
  uni.redirectTo({
    url: `/pages/square/withdraw-success?amount=${withdrawYuan.value}`
  });
};
</script>

<style lang="scss" scoped>
.withdraw-page-container {
  min-height: 100vh;
  background-color: #F6F7FB;
  display: flex;
  flex-direction: column;
}

/* 顶部蓝色导航栏 (严格对齐提现.png) */
.blue-header-nav {
  height: 140rpx;
  background-color: #1B65F0;
  padding-top: 50rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-left: 28rpx;
  padding-right: 28rpx;
}

.back-text {
  font-size: calc(34rpx * var(--font-scale, 1));
  font-weight: 700;
  color: #FFFFFF;
}

.page-title {
  font-size: calc(48rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #FFFFFF;
  letter-spacing: 4rpx;
}

.nav-right-placeholder {
  width: 90rpx;
}

/* 到账行 */
.account-row {
  background-color: #FFFFFF;
  padding: 24rpx 36rpx;
  display: flex;
  align-items: center;

  .account-label {
    font-size: calc(34rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
    margin-right: 32rpx;
  }

  .wechat-badge {
    width: 68rpx;
    height: 68rpx;
    background-color: #07C160;
    border-radius: 16rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 18rpx;

    .wechat-icon {
      font-size: 40rpx;
    }
  }

  .account-name {
    flex: 1;
    font-size: calc(32rpx * var(--font-scale, 1));
    font-weight: 800;
    color: #1F2329;
  }

  .modify-account-btn {
    background-color: #EBF2FF;
    color: #1B65F0;
    font-size: calc(28rpx * var(--font-scale, 1));
    font-weight: 800;
    border-radius: 30rpx;
    padding: 0 24rpx;
    height: 60rpx;
    line-height: 60rpx;
    border: none;

    &::after {
      border: none;
    }
  }
}

.gray-bar {
  height: 24rpx;
  background-color: #E5E7EB;
}

.withdraw-body {
  padding: 36rpx;
}

.field-title {
  display: block;
  font-size: calc(34rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1F2329;
  margin-bottom: 24rpx;
}

.amount-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 32rpx;
  display: flex;
  align-items: center;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 36rpx;

  .currency-symbol {
    font-size: calc(52rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
    margin-right: 20rpx;
  }

  .amount-input {
    flex: 1;
    font-size: calc(52rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
  }

  .placeholder-amount {
    color: #C4C8D0;
  }
}

.summary-boxes-row {
  display: flex;
  gap: 28rpx;
  margin-bottom: 36rpx;
}

.summary-box {
  flex: 1;
  height: calc(110rpx * var(--font-scale, 1));
  background-color: #FFFFFF;
  border: 4rpx solid #1B65F0;
  border-radius: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);

  .summary-text {
    font-size: calc(36rpx * var(--font-scale, 1));
    font-weight: 900;
    color: #1F2329;
  }
}

.ratio-hint-wrap {
  text-align: center;
  margin-bottom: 50rpx;

  .ratio-hint-text {
    font-size: calc(28rpx * var(--font-scale, 1));
    color: #8F959E;
    font-weight: 600;
  }
}

.confirm-withdraw-btn {
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

.btn-hover {
  opacity: 0.88;
}
</style>
