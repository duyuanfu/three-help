<template>
  <view class="data-page-container" :style="pageStyle">
    <view class="status-bar"></view>

    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">数据管理</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 概览卡片 -->
    <view class="summary-card">
      <view class="summary-row">
        <text class="summary-label">数据表</text>
        <text class="summary-value">{{ stats.length }} 张</text>
      </view>
      <view class="summary-row">
        <text class="summary-label">总记录数</text>
        <text class="summary-value">{{ totalCount }} 条</text>
      </view>
      <view class="summary-row">
        <text class="summary-label">占用空间</text>
        <text class="summary-value">{{ formatSize(totalSize) }}</text>
      </view>
      <view class="summary-hint">
        <text class="hint-text">数据保存在本机 Storage，换设备或清除小程序缓存后会丢失，建议定期导出备份。</text>
      </view>
    </view>

    <!-- 表列表 -->
    <view class="section-title">
      <text class="section-title-text">数据表明细</text>
    </view>

    <view class="table-list">
      <view v-for="item in stats" :key="item.name" class="table-row">
        <view class="table-main">
          <text class="table-name">{{ item.label }}</text>
          <text class="table-key">{{ item.name }}</text>
        </view>
        <view class="table-meta">
          <text class="table-count">{{ item.count }}{{ item.single ? ' 项' : ' 条' }}</text>
          <text class="table-size">{{ formatSize(item.size) }}</text>
        </view>
      </view>
    </view>

    <!-- 操作区 -->
    <view class="section-title">
      <text class="section-title-text">数据操作</text>
    </view>

    <view class="action-list">
      <button class="action-btn primary" hover-class="btn-hover" @tap="handleExport">
        导出数据（JSON）
      </button>
      <button class="action-btn primary" hover-class="btn-hover" @tap="handleExportFile">
        导出为文件
      </button>
      <button class="action-btn secondary" hover-class="btn-hover" @tap="handleImport">
        从剪贴板导入
      </button>
      <button class="action-btn warning" hover-class="btn-hover" @tap="handleClear">
        重置全部数据
      </button>
    </view>

    <view class="footer-tip">
      <text class="footer-tip-text">重置后会清空所有表，并在下次启动时重新注入示例数据。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { useSettingStore } from '../../stores/setting';
import {
  exportToClipboard,
  exportToFile,
  importAll,
  clearAll,
  getStats,
  type TableStat
} from '../../utils/db';

const settingStore = useSettingStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const stats = ref<TableStat[]>([]);

const totalCount = computed(() => stats.value.reduce((sum, item) => sum + item.count, 0));
const totalSize = computed(() => stats.value.reduce((sum, item) => sum + item.size, 0));

const refresh = () => {
  stats.value = getStats();
};

const formatSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`;
};

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

const handleExport = async () => {
  await exportToClipboard();
};

const handleExportFile = async () => {
  await exportToFile();
};

const handleImport = () => {
  uni.showModal({
    title: '从剪贴板导入',
    content: '请先复制导出的 JSON 文本，确认后将覆盖当前全部数据。',
    confirmText: '开始导入',
    cancelText: '取消',
    confirmColor: '#1B65F0',
    success: (res) => {
      if (!res.confirm) return;
      uni.getClipboardData({
        success: (clip) => {
          const ok = importAll(clip.data, 'replace');
          if (ok) {
            uni.showToast({ title: '导入成功，请重启小程序', icon: 'none' });
            refresh();
          } else {
            uni.showToast({ title: '数据格式不正确', icon: 'none' });
          }
        },
        fail: () => {
          uni.showToast({ title: '读取剪贴板失败', icon: 'none' });
        }
      });
    }
  });
};

const handleClear = () => {
  uni.showModal({
    title: '重置全部数据',
    content: '将清空所有本地数据并恢复为示例数据，此操作不可撤销。',
    confirmText: '确认重置',
    cancelText: '取消',
    confirmColor: '#FF4D4F',
    success: (res) => {
      if (!res.confirm) return;
      clearAll();
      refresh();
      uni.showToast({ title: '已重置，重启后生效', icon: 'none' });
    }
  });
};

onShow(() => {
  refresh();
});
</script>

<style lang="scss" scoped>
.data-page-container {
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
  padding: 24rpx 0 32rpx 0;
}

.back-text {
  font-size: calc(30rpx * var(--font-scale, 1));
  color: #1F2329;
  font-weight: 700;
}

.page-title {
  font-size: calc(36rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1F2329;
}

.nav-right-placeholder {
  width: 100rpx;
}

.summary-card {
  background: #FFFFFF;
  border-radius: 32rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.04);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;
}

.summary-label {
  font-size: calc(30rpx * var(--font-scale, 1));
  color: #8F959E;
}

.summary-value {
  font-size: calc(32rpx * var(--font-scale, 1));
  font-weight: 800;
  color: #1B65F0;
}

.summary-hint {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #F0F1F5;
}

.hint-text {
  font-size: calc(24rpx * var(--font-scale, 1));
  color: #8F959E;
  line-height: 1.6;
}

.section-title {
  padding: 36rpx 8rpx 20rpx 8rpx;
}

.section-title-text {
  font-size: calc(30rpx * var(--font-scale, 1));
  font-weight: 800;
  color: #1F2329;
}

.table-list {
  background: #FFFFFF;
  border-radius: 32rpx;
  padding: 8rpx 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.04);
}

.table-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #F0F1F5;

  &:last-child {
    border-bottom: none;
  }
}

.table-main {
  display: flex;
  flex-direction: column;
}

.table-name {
  font-size: calc(30rpx * var(--font-scale, 1));
  font-weight: 700;
  color: #1F2329;
}

.table-key {
  font-size: calc(22rpx * var(--font-scale, 1));
  color: #B0B4BA;
  margin-top: 6rpx;
}

.table-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.table-count {
  font-size: calc(28rpx * var(--font-scale, 1));
  font-weight: 700;
  color: #1B65F0;
}

.table-size {
  font-size: calc(22rpx * var(--font-scale, 1));
  color: #8F959E;
  margin-top: 6rpx;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.action-btn {
  width: 100%;
  height: 100rpx;
  line-height: 100rpx;
  border-radius: 28rpx;
  font-size: calc(32rpx * var(--font-scale, 1));
  font-weight: 800;
  border: none;

  &::after {
    border: none;
  }

  &.primary {
    background-color: #1B65F0;
    color: #FFFFFF;
    box-shadow: 0 8rpx 24rpx rgba(27, 101, 240, 0.25);
  }

  &.secondary {
    background-color: #FFFFFF;
    color: #1B65F0;
    border: 3rpx solid #1B65F0;
  }

  &.warning {
    background-color: #FFFFFF;
    color: #FF4D4F;
    border: 3rpx solid #FFD6D6;
  }
}

.btn-hover {
  opacity: 0.85;
}

.footer-tip {
  padding: 32rpx 8rpx;
}

.footer-tip-text {
  font-size: calc(24rpx * var(--font-scale, 1));
  color: #8F959E;
  line-height: 1.6;
}
</style>
