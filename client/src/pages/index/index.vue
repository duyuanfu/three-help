<template>
  <view class="home-container" :style="pageStyle">
    <!-- 顶部状态栏占位 -->
    <view class="status-bar"></view>

    <!-- 顶部品牌与定位 -->
    <view class="header-section">
      <view class="logo-area">
        <view >
          <image src="../../static/launch/logo.png" mode="aspectFit" class="logo-img" />
        </view>
        <view class="title-wrap">
          <image src="/static/common/brand-blue.png" mode="aspectFit" class="brand-blue-img" />
          <view class="location-bar" @tap="handleLocationTap">
            <image src="/static/common/location.png" mode="aspectFit" class="loc-icon-img" />
            <text class="city-name">{{ locating ? '定位中…' : currentCity }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 问候语胶囊卡片 -->
    <view class="greeting-capsule">
      <text class="greeting-text">您好 ， 今天需要什么帮助？</text>
    </view>

    <!-- 一键下单大卡片 -->
    <view class="order-hero-card">
      <view class="hero-header">
        <image src="../../static/common/call.png" mode="aspectFit" class="phone-icon" />
        <!-- <view class="phone-icon-wrap">
          <text class="phone-icon">📞</text>
        </view> -->
        <view class="hero-text-wrap">
          <text class="hero-title">一 键 下 单</text>
          <text class="hero-subtitle">请说出您的需求</text>
        </view>
      </view>
      <view class="hero-actions">
        <button class="hero-btn" hover-class="btn-hover" @tap="goToCreate('一键需求下单')">
          立即下单
        </button>
        <button class="hero-btn" hover-class="btn-hover" @tap="goToTake">
          立即接单
        </button>
      </view>
    </view>

    <!-- 6 大服务分类九宫格卡片 -->
    <view class="categories-card">
      <view class="cat-grid">
        <view 
          v-for="cat in categoryList" 
          :key="cat.id" 
          class="cat-item"
          hover-class="cat-item-hover"
          @tap="goToCreate(cat.title, cat.id)"
        >
          <view class="cat-icon-box">
            <image :src="netImage(cat.icon)" mode="aspectFit" class="cat-img" />
          </view>
          <text class="cat-label">{{ cat.title }}</text>
        </view>
      </view>
    </view>

    <!-- 我的求助进度状态卡片 -->
    <view class="my-request-card" v-if="latestOrder">
      <view class="req-top-row">
        <text class="req-label">我的求助</text>
        <view class="status-badge" :class="{ 'badge-done': latestOrder.status === 'in_progress' }">
          <text class="check-icon">✔</text>
          <text class="badge-text">{{ latestOrder.status === 'in_progress' ? '已有帮手接单' : '等待帮手接单' }}</text>
        </view>
      </view>
      <view class="req-middle-row">
        <text class="req-title">{{ latestOrder.title }}，{{ latestOrder.content || '按时上门服务' }}</text>
      </view>
      <view class="req-bottom-row">
        <button class="progress-btn" hover-class="btn-hover" @tap="goToOrderList">
          查看进度
        </button>
      </view>
    </view>

    <!-- 底部常驻语音求助条 (原型样式) -->
    <view class="voice-bar-wrap">
      <view 
        class="voice-bar" 
        hover-class="btn-hover" 
        @tap="showVoiceUnsupported"
      >
        <image src="/static/common/voice-mic.png" mode="aspectFit" class="mic-img" />
        <text class="voice-text">不方便打字？按住说话</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { netImage } from '../../utils/netImage';
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useOrderStore } from '../../stores/order';
import { useSettingStore } from '../../stores/setting';
import { usePullRefresh } from '../../composables/usePullRefresh';
import { showVoiceUnsupported } from '../../utils/voice';
import { apiReverseGeocode } from '../../api/location';

const orderStore = useOrderStore();
const settingStore = useSettingStore();

// 下拉刷新：重新拉取全量数据（五个 tab 页统一行为）
usePullRefresh();

const currentCity = ref('泉州市');

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const categoryList = [
  { id: 'transport', title: '出行接送', icon: '/static/categories/transport.png' },
  { id: 'medical', title: '出行就医', icon: '/static/categories/medical.png' },
  { id: 'shopping', title: '购物陪伴', icon: '/static/categories/shopping.png' },
  { id: 'delivery', title: '物品代买', icon: '/static/categories/delivery.png' },
  { id: 'cleaning', title: '家政服务', icon: '/static/categories/cleaning.png' },
  { id: 'consult', title: '线上咨询', icon: '/static/categories/consult.png' },
];

const latestOrder = computed(() => {
  return orderStore.latestMyRequest;
});

// ---------------- 定位 ----------------
const CITY_STORAGE_KEY = 'bbb:selected-city';
const locating = ref(false);
/** 本次启动是否已尝试过自动定位，避免每次切 tab 都触发一次定位 */
let autoLocated = false;

// 上次手动选择/定位成功的城市，进页面先显示它
try {
  const cached = uni.getStorageSync(CITY_STORAGE_KEY);
  if (cached) currentCity.value = cached;
} catch (e) {
  // 读取缓存失败不影响默认值
}

const applyCity = (name: string) => {
  const value = (name || '').trim();
  if (!value) return;
  currentCity.value = value;
  try {
    uni.setStorageSync(CITY_STORAGE_KEY, value);
  } catch (e) {
    // 缓存写入失败只是下次不记住，不阻断流程
  }
};

/** 从「福建省泉州市丰泽区…」这类地址里截出城市 */
const pickCity = (address: string) => {
  const matched = (address || '').match(/[\u4e00-\u9fa5]{2,10}?市/);
  return matched ? matched[0] : '';
};

/** 手动选择位置：微信原生地图选点，不需要任何地图 key */
const manualLocate = () => {
  uni.chooseLocation({
    success: (res: any) => {
      // address 是完整地址，name 是地点名；优先取城市，取不到就用地点名
      const city = pickCity(res.address) || res.name || '';
      if (city) {
        applyCity(city);
        uni.showToast({ title: `已设置位置：${city}`, icon: 'none' });
      }
    },
    fail: (err: any) => {
      // 用户主动取消不提示
      if (err && /cancel/.test(err.errMsg || '')) return;
      uni.showToast({ title: '未选择位置', icon: 'none' });
    }
  });
};

/**
 * 自动定位：getLocation 拿经纬度 → 后端反查城市。
 * silent=true 时失败不打扰用户（首次进页面用），只保留默认/缓存城市。
 */
const autoLocate = (silent = false) => {
  if (locating.value) return;
  locating.value = true;
  uni.getLocation({
    type: 'gcj02',
    success: async (res) => {
      try {
        const geo = await apiReverseGeocode(res.latitude, res.longitude);
        const city = (geo?.city || geo?.district || '').trim();
        if (city) {
          applyCity(city);
          if (!silent) uni.showToast({ title: `已定位到${city}`, icon: 'none' });
        } else if (!silent) {
          throw new Error('未能解析出城市');
        }
      } catch (e) {
        if (!silent) {
          uni.showToast({ title: '自动定位失败，请手动选择位置', icon: 'none' });
          manualLocate();
        }
      } finally {
        locating.value = false;
      }
    },
    fail: () => {
      locating.value = false;
      if (!silent) {
        uni.showToast({ title: '未获得定位权限，请手动选择位置', icon: 'none' });
        manualLocate();
      }
    }
  });
};

/** 点击定位条：自动定位 / 手动选择位置 */
const handleLocationTap = () => {
  uni.showActionSheet({
    itemList: ['自动定位', '手动选择位置'],
    success: (res) => {
      if (res.tapIndex === 0) autoLocate(false);
      if (res.tapIndex === 1) manualLocate();
    }
  });
};

onShow(() => {
  // 首次进首页静默自动定位一次；用户手动选过的城市会被缓存覆盖
  if (autoLocated) return;
  autoLocated = true;
  autoLocate(true);
});

const goToCreate = (title: string, categoryId?: string) => {
  if (categoryId === 'consult' || title.includes('咨询')) {
    uni.navigateTo({
      url: '/pages/index/consult'
    });
    return;
  }
  // 首页分类入口：下单内容默认带入入口名称
  const contentParam = categoryId ? `&content=${encodeURIComponent(title)}` : '';
  uni.navigateTo({
    url: `/pages/order/create?title=${encodeURIComponent(title)}&category=${categoryId || 'delivery'}${contentParam}`
  });
};

const goToTake = () => {
  uni.navigateTo({
    url: '/pages/order/take'
  });
};

const goToOrderList = () => {
  uni.switchTab({
    url: '/pages/order/index'
  });
};

</script>

<style lang="scss" scoped>
.home-container {
  min-height: 100vh;
  background-color: #F6F7FB;
  padding: 0 60rpx 140rpx 60rpx;
  display: flex;
  flex-direction: column;
}

.status-bar {
  height: 60rpx;
}

/* 顶部品牌与定位 */
.header-section {
  padding: 20rpx 0 16rpx 0;
}

.logo-area {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 白色圆角 Logo 卡片（与启动页1完全一致） */
.logo-card {
  width: 182rpx;
  height: 182rpx;
  background-color: #FFFFFF;
  border-radius: 46rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12rpx 32rpx rgba(0, 0, 0, 0.12);
}

.logo-img {
  width: 236rpx;
  height: 200rpx;
}

.title-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.brand-blue-img {
  width: 308rpx;
  height: 102rpx;
}

.location-bar {
  display: flex;
  align-items: center;
  margin-top: 6rpx;
  padding: 4rpx 16rpx;
  border-radius: 30rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);

  .loc-icon-img {
    width: 68rpx;
    height: 80rpx;
    margin-right: 8rpx;
  }

  .city-name {
    font-size: calc(26rpx * var(--font-scale, 1));
    color: #1F2329;
    font-weight: 600;
  }
}

/* 问候语胶囊 */
.greeting-capsule {
  margin: 18rpx 0 24rpx 0;
  background-color: #FFFFFF;
  border-radius: 40rpx;
  padding: 16rpx 28rpx;
  text-align: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);

  .greeting-text {
    font-size: calc(30rpx * var(--font-scale, 1));
    font-weight: 700;
    color: #1F2329;
    letter-spacing: 2rpx;
  }
}

/* 一键下单大蓝卡 */
.order-hero-card {
  background: #1B65F0;
  border-radius: 36rpx;
  padding: 36rpx 32rpx;
  box-shadow: 0 10rpx 30rpx rgba(27, 101, 240, 0.35);
  margin-bottom: 28rpx;
}

.hero-header {
  display: flex;
  align-items: center;
  margin-bottom: 32rpx;
}

.phone-icon-wrap {
  width: 90rpx;
  height: 90rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;


}

.phone-icon {
  width: 130rpx;
  height: 130rpx;
}

.hero-text-wrap {
  display: flex;
  flex-direction: column;
  margin-left: 60rpx;
}

.hero-title {
  font-size: calc(54rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #FFFFFF;
  letter-spacing: 6rpx;
}

.hero-subtitle {
  font-size: calc(28rpx * var(--font-scale, 1));
  color: rgba(255, 255, 255, 0.9);
  margin-top: 6rpx;
  letter-spacing: 10rpx;
}

.hero-actions {
  display: flex;
  justify-content: space-between;
  gap: 20rpx;
  letter-spacing: 10rpx;
}

.hero-btn {
  flex: 1;
  height: calc(88rpx * var(--font-scale, 1));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  margin: 0;
  background-color: #FFFFFF;
  color: #1B65F0;
  font-size: calc(34rpx * var(--font-scale, 1));
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
  border-radius: 34rpx;
  border: none;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);

  &::after {
    border: none;
  }
}

/* 6 大分类卡片 */
.categories-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 24rpx 16rpx;
  box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 28rpx;
  border: 2px solid  #18529B;
}

.cat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  row-gap: 24rpx;
}

.cat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14rpx 0;
  margin: 0 24rpx;
  background: #F2F7FD;
  border-radius: 32rpx;
}

.cat-item-hover {
  opacity: 0.8;
  transform: scale(0.97);
}

.cat-icon-box {
  width: 130rpx;
  height: 100rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cat-img {
  width: 110rpx;
  height: 90rpx;
}

.cat-label {
  font-size: calc(28rpx * var(--font-scale, 1));
  font-weight: 800;
  color: #1F2329;
  margin-top: 8rpx;
}

/* 我的求助卡片 */
.my-request-card {
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 28rpx 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 30rpx;
}

.req-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.req-label {
  font-size: calc(28rpx * var(--font-scale, 1));
  color: #1F2329;
  font-weight: 600;
}

.status-badge {
  display: flex;
  align-items: center;
  color: #52C41A;
  font-size: calc(26rpx * var(--font-scale, 1));
  font-weight: 700;

  .check-icon {
    margin-right: 8rpx;
    font-size: 28rpx;
  }
}

.req-middle-row {
  margin-bottom: 20rpx;
  text-align: center;
}

.req-title {
  font-size: calc(34rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1F2329;
  letter-spacing: 2rpx;
}

.req-bottom-row {
  display: flex;
  justify-content: center;
}

.progress-btn {
  width: 100%;
  height: calc(80rpx * var(--font-scale, 1));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  margin: 0;
  background-color: #EBF2FF;
  color: #1B65F0;
  font-size: calc(32rpx * var(--font-scale, 1));
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
  border-radius: 40rpx;
  border: none;

  &::after {
    border: none;
  }
}

/* 底部语音求助条 */
.voice-bar-wrap {
  margin-top: auto;
  padding-top: 10rpx;
}

.voice-bar {
  background-color: #EBF2FF;
  border: 3rpx solid #BCD6FF;
  border-radius: 50rpx;
  height: calc(96rpx * var(--font-scale, 1));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24rpx;
  box-sizing: border-box;
  box-shadow: 0 4rpx 16rpx rgba(27, 101, 240, 0.1);
  transition: opacity 0.2s;

  .mic-img {
    width: 44rpx;
    height: 44rpx;
    margin-right: 16rpx;
  }

  .voice-text {
    font-size: calc(32rpx * var(--font-scale, 1));
    font-weight: 800;
    color: #1B65F0;
    letter-spacing: 2rpx;
    white-space: nowrap;
  }
}

.btn-hover {
  opacity: 0.85;
}
</style>
