<template>
  <view class="create-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航标题栏 (严格对齐下单页.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">下 单</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 表单卡片区域 -->
    <view class="form-body">
      <!-- 1. 下单内容 -->
      <view class="field-group">
        <text class="field-label">下单内容</text>
        <view class="textarea-box">
          <textarea 
            v-model="content" 
            class="elder-textarea" 
            placeholder="请输入内容..." 
            placeholder-class="placeholder-text"
            maxlength="200"
          />
        </view>
      </view>

      <!-- 2. 约定时间（时间选择器） -->
      <view class="field-group">
        <text class="field-label">约定时间</text>
        <picker
          class="picker-full"
          mode="multiSelector"
          :range="timeRange"
          :value="timeIndex"
          @change="onTimeChange"
        >
          <view class="input-box">
            <text class="value-text" :class="{ 'value-placeholder': !appointTime }">
              {{ appointTime || '请选择时间...' }}
            </text>
            <text class="field-arrow">›</text>
          </view>
        </picker>
      </view>

      <!-- 3. 约定地点（支持手动输入 + 地图定位） -->
      <view class="field-group">
        <text class="field-label">约定地点</text>
        <view class="input-box">
          <input 
            v-model="address" 
            class="elder-input" 
            placeholder="请输入地点..." 
            placeholder-class="placeholder-text"
          />
          <view class="loc-btn" hover-class="loc-btn-hover" @tap.stop="chooseLocation">
            <image src="/static/common/location.png" mode="aspectFit" class="loc-icon" />
            <text class="loc-btn-text">定位</text>
          </view>
        </view>
      </view>

      <!-- 4. 下单金额 -->
      <view class="field-group">
        <text class="field-label">下单金额</text>
        <view class="input-box">
          <input 
            v-model="bounty" 
            type="number"
            class="elder-input" 
            placeholder="请输入金额..." 
            placeholder-class="placeholder-text"
          />
        </view>
      </view>

      <!-- 确定大按钮 -->
      <view class="action-wrap">
        <button class="submit-btn" hover-class="btn-hover" @tap="handleSubmit">
          确 定
        </button>
      </view>
    </view>

    <!-- 底部语音输入条（暂未开放，点击给出友好提示） -->
    <view class="bottom-voice-wrap">
      <view class="voice-bar" hover-class="btn-hover" @tap="showVoiceUnsupported">
        <image src="/static/common/voice-mic.png" mode="aspectFit" class="mic-img" />
        <text class="voice-text">不方便打字？按住说话</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { useOrderStore } from '../../stores/order';
import { useSettingStore } from '../../stores/setting';
import { showVoiceUnsupported } from '../../utils/voice';
import type { OrderCategory } from '../../types';

const orderStore = useOrderStore();
const settingStore = useSettingStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const category = ref<OrderCategory>('delivery');
const orderTitle = ref('互助服务');
const content = ref('');
const appointTime = ref('');
const address = ref('');
const bounty = ref('30');

// 约定时间：未来 14 天 + 08:00~20:00 每半小时
const WEEK_LABELS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
const dateOptions = (() => {
  const list: string[] = [];
  const base = new Date();
  for (let i = 0; i < 14; i++) {
    const d = new Date(base.getFullYear(), base.getMonth(), base.getDate() + i);
    const prefix = i === 0 ? '今天' : i === 1 ? '明天' : i === 2 ? '后天' : WEEK_LABELS[d.getDay()];
    list.push(`${prefix} ${d.getMonth() + 1}月${d.getDate()}日`);
  }
  return list;
})();
const timeOptions = (() => {
  const list: string[] = [];
  for (let h = 8; h <= 20; h++) {
    list.push(`${String(h).padStart(2, '0')}:00`);
    if (h < 20) list.push(`${String(h).padStart(2, '0')}:30`);
  }
  return list;
})();
const timeRange = [dateOptions, timeOptions];
const timeIndex = ref<[number, number]>([1, 2]);

const applySelectedTime = () => {
  appointTime.value = `${dateOptions[timeIndex.value[0]]} ${timeOptions[timeIndex.value[1]]}`;
};

const parseIncomingTime = (text: string) => {
  const match = text.match(/(\d{1,2}):(\d{2})/);
  if (match) {
    const target = `${match[1].padStart(2, '0')}:${match[2]}`;
    const idx = timeOptions.indexOf(target);
    if (idx >= 0) timeIndex.value[1] = idx;
  }
  if (text.includes('今天')) timeIndex.value[0] = 0;
  else if (text.includes('后天')) timeIndex.value[0] = 2;
  else if (text.includes('明天')) timeIndex.value[0] = 1;
};

onLoad((options) => {
  if (options) {
    if (options.title) orderTitle.value = decodeURIComponent(options.title);
    if (options.category) category.value = options.category as OrderCategory;
    if (options.content) content.value = decodeURIComponent(options.content);
    if (options.time) parseIncomingTime(decodeURIComponent(options.time));
    if (options.address) address.value = decodeURIComponent(options.address);
    if (options.bounty) bounty.value = options.bounty;
  }
  applySelectedTime();
  // 默认提供亲切的示范地址
  if (!address.value) {
    address.value = '凤阳小区8号楼302';
  }
});

const onTimeChange = (e: any) => {
  const [dateIdx, timeIdx] = e.detail.value as number[];
  timeIndex.value = [dateIdx, timeIdx];
  applySelectedTime();
};

const chooseLocation = () => {
  uni.chooseLocation({
    success: (res) => {
      const name = (res.name || '').trim();
      const addr = (res.address || '').trim();
      if (name || addr) {
        address.value = name || addr;
      }
    },
    fail: (err) => {
      const msg = (err && err.errMsg) || '';
      console.error('[chooseLocation] fail:', msg);
      if (msg.includes('cancel')) return;
      if (msg.includes('auth deny') || msg.includes('authorize') || msg.includes('permission')) {
        uni.showModal({
          title: '需要位置权限',
          content: '请允许使用位置信息，才能在地图上选择上门服务地点。',
          confirmText: '去设置',
          cancelText: '取消',
          confirmColor: '#1B65F0',
          success: (r) => {
            if (r.confirm) uni.openSetting();
          }
        });
        return;
      }
      uni.showToast({ title: '地图打开失败，请检查位置权限后重试', icon: 'none' });
    }
  });
};

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const handleSubmit = () => {
  if (!content.value.trim()) {
    uni.showToast({ title: '请填写下单内容', icon: 'none' });
    return;
  }
  if (!appointTime.value.trim()) {
    uni.showToast({ title: '请选择约定时间', icon: 'none' });
    return;
  }
  if (!address.value.trim()) {
    uni.showToast({ title: '请选择或输入约定地点', icon: 'none' });
    return;
  }

  const order = orderStore.createOrder({
    category: category.value,
    title: orderTitle.value || '互助需求',
    content: content.value.trim(),
    appointTime: appointTime.value.trim(),
    address: address.value.trim(),
    bounty: Number(bounty.value) || 20
  });

  // 跳转到下单成功页面
  uni.redirectTo({
    url: `/pages/order/success?type=order&orderId=${order.id}`
  });
};
</script>

<style lang="scss" scoped>
.create-container {
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
  padding: 20rpx 0 30rpx 0;
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

.form-body {
  flex: 1;
}

.field-group {
  margin-bottom: 30rpx;
}

.field-label {
  display: block;
  font-size: calc(34rpx * var(--font-scale));
  font-weight: 800;
  color: #1F2329;
  margin-bottom: 16rpx;
}

.textarea-box {
  background-color: #FFFFFF;
  border: 3rpx solid #1B65F0;
  border-radius: 36rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);
}

.elder-textarea {
  width: 100%;
  height: 200rpx;
  font-size: calc(32rpx * var(--font-scale));
  color: #1F2329;
  line-height: 1.5;
}

.input-box {
  background-color: #FFFFFF;
  border: 3rpx solid #1B65F0;
  border-radius: 36rpx;
  height: calc(100rpx * var(--font-scale));
  padding: 0 28rpx;
  display: flex;
  align-items: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);
}

.elder-input {
  width: 100%;
  height: 100%;
  font-size: calc(32rpx * var(--font-scale));
  color: #1F2329;
}

.picker-full {
  width: 100%;
}

.value-text {
  flex: 1;
  font-size: calc(32rpx * var(--font-scale));
  color: #1F2329;
  font-weight: 400;
}

.value-placeholder {
  color: #B0B5BD;
  font-size: calc(30rpx * var(--font-scale));
  font-weight: 400;
}

.field-arrow {
  margin-left: 12rpx;
  font-size: 44rpx;
  line-height: 1;
  color: #9AA3AF;
}

.loc-btn {
  display: flex;
  align-items: center;
  margin-left: 16rpx;
  padding-left: 20rpx;
  border-left: 2rpx solid #E5E7EB;
  flex-shrink: 0;
}

.loc-btn-hover {
  opacity: 0.7;
}

.loc-icon {
  width: 34rpx;
  height: 34rpx;
  margin-right: 6rpx;
}

.loc-btn-text {
  font-size: calc(28rpx * var(--font-scale));
  font-weight: 800;
  color: #1B65F0;
}

.placeholder-text {
  color: #B0B5BD;
  font-size: calc(30rpx * var(--font-scale));
}

.action-wrap {
  margin-top: 40rpx;
}

.submit-btn {
  width: 100%;
  height: calc(108rpx * var(--font-scale));
  line-height: calc(108rpx * var(--font-scale));
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(44rpx * var(--font-scale));
  font-weight: 900;
  border-radius: 32rpx;
  letter-spacing: 6rpx;
  box-shadow: 0 10rpx 28rpx rgba(27, 101, 240, 0.35);
  border: none;

  &::after {
    border: none;
  }
}

.bottom-voice-wrap {
  margin-top: 36rpx;
  margin-bottom: 30rpx;
}

.voice-bar {
  background-color: #EBF2FF;
  border: 3rpx solid #BCD6FF;
  border-radius: 50rpx;
  height: calc(96rpx * var(--font-scale));
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(27, 101, 240, 0.1);

  .mic-img {
    width: 44rpx;
    height: 44rpx;
    margin-right: 14rpx;
  }

  .voice-text {
    font-size: calc(32rpx * var(--font-scale));
    font-weight: 800;
    color: #1B65F0;
  }
}

.btn-hover {
  opacity: 0.88;
}
</style>
