<template>
  <view class="admin-page" :style="pageStyle">
    <view class="status-bar"></view>

    <!-- 顶部导航 -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">管理后台</text>
      <text class="admin-name">{{ userStore.currentUser.name }}</text>
    </view>

    <!-- 数据表切换 -->
    <scroll-view scroll-x class="table-tabs" :show-scrollbar="false" enhanced>
      <view
        v-for="item in tables"
        :key="item.key"
        class="table-chip"
        :class="{ active: item.key === activeKey }"
        @tap="switchTable(item.key)"
      >
        <text class="chip-label">{{ item.label }}</text>
        <text class="chip-count">{{ item.count }}</text>
      </view>
    </scroll-view>

    <!-- 关键词搜索 -->
    <view class="search-row">
      <input
        class="search-input"
        v-model="keyword"
        placeholder="搜索姓名 / 手机号 / 内容…"
        placeholder-class="search-placeholder"
        confirm-type="search"
        @confirm="doSearch"
      />
      <button class="search-btn" hover-class="btn-hover" @tap="doSearch">搜索</button>
    </view>

    <!-- 列表概览 -->
    <view class="list-summary">
      <text class="summary-text">{{ currentLabel }} · 共 {{ total }} 条</text>
      <text v-if="keyword" class="clear-kw" @tap="clearKeyword">清除搜索</text>
    </view>

    <!-- 记录列表 -->
    <view class="record-list">
      <view
        v-for="row in rows"
        :key="pkOf(row)"
        class="record-card"
        hover-class="card-hover"
        @tap="openEdit(row)"
      >
        <view class="record-main">
          <text class="record-title">{{ summaryOf(row) }}</text>
          <text class="record-id">{{ pkOf(row) }}</text>
        </view>
        <text class="record-edit">编辑 ›</text>
      </view>

      <view v-if="loading" class="empty-tip">加载中…</view>
      <view v-else-if="!rows.length" class="empty-tip">暂无数据</view>
      <button
        v-if="!loading && rows.length < total"
        class="load-more-btn"
        hover-class="btn-hover"
        @tap="loadMore"
      >
        加载更多（{{ rows.length }} / {{ total }}）
      </button>
    </view>

    <!-- 编辑弹层 -->
    <view v-if="editing" class="edit-mask" @tap="closeEdit">
      <view class="edit-panel" @tap.stop>
        <view class="edit-header">
          <view class="edit-title-wrap">
            <text class="edit-title">编辑 · {{ currentLabel }}</text>
            <text class="edit-subtitle">{{ editingId }}</text>
          </view>
          <text class="edit-close" @tap="closeEdit">✕</text>
        </view>

        <scroll-view scroll-y class="edit-body">
          <view v-for="col in editableColumns" :key="col.name" class="field-row">
            <view class="field-head">
              <text class="field-label">{{ col.label }}</text>
              <text class="field-key">{{ col.name }}</text>
            </view>
            <input
              class="field-input"
              :value="draftValue(col.name)"
              :placeholder="col.nullable ? '可留空' : '不可为空'"
              placeholder-class="field-placeholder"
              @input="onFieldInput(col.name, $event)"
            />
          </view>
        </scroll-view>

        <view class="edit-footer">
          <button class="foot-btn danger" hover-class="btn-hover" @tap="confirmDelete">删除</button>
          <button class="foot-btn primary" hover-class="btn-hover" @tap="saveEdit">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import { useUserStore } from '../../stores/user';
import { useSettingStore } from '../../stores/setting';
import { toStorageUrl } from '../../utils/token';
import {
  apiAdminTables,
  apiAdminPage,
  apiAdminUpdate,
  apiAdminDelete,
  type AdminTable,
  type AdminColumn
} from '../../api/admin';

const userStore = useUserStore();
const settingStore = useSettingStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const PAGE_SIZE = 20;

const tables = ref<AdminTable[]>([]);
const activeKey = ref('users');
const currentLabel = ref('');
const currentPk = ref('id');
const columns = ref<AdminColumn[]>([]);
const rows = ref<Record<string, any>[]>([]);
const total = ref(0);
const page = ref(1);
const keyword = ref('');
const loading = ref(false);

const editing = ref(false);
const editingId = ref('');
const original = ref<Record<string, any>>({});
const draft = ref<Record<string, any>>({});

/** 可编辑字段：主键不可改 */
const editableColumns = computed(() => columns.value.filter((c) => !c.primary));

const pkOf = (row: Record<string, any>) => String(row[currentPk.value] ?? '');

/** 列表摘要：挑最像"标题"的字段 */
const SUMMARY_KEYS = [
  'name', 'title', 'content', 'last_message', 'goods_name',
  'user_name', 'description', 'phone', 'session_id'
];
const summaryOf = (row: Record<string, any>) => {
  for (const key of SUMMARY_KEYS) {
    const value = row[key];
    if (value !== null && value !== undefined && String(value).trim() !== '') {
      const text = String(value);
      return text.length > 28 ? text.slice(0, 28) + '…' : text;
    }
  }
  return pkOf(row);
};

const draftValue = (name: string) => {
  const value = draft.value[name];
  return value === null || value === undefined ? '' : String(value);
};

const onFieldInput = (name: string, e: any) => {
  draft.value[name] = e.detail.value;
};

/* ------------------------------------------------------------------ */

const loadTables = async () => {
  tables.value = await apiAdminTables();
};

const loadPage = async (reset: boolean) => {
  if (loading.value) return;
  loading.value = true;
  try {
    const targetPage = reset ? 1 : page.value + 1;
    const result = await apiAdminPage(activeKey.value, targetPage, PAGE_SIZE, keyword.value);
    currentLabel.value = result.label;
    currentPk.value = result.pk || 'id';
    columns.value = result.columns || [];
    total.value = result.total;
    page.value = result.page;
    rows.value = reset ? result.rows : rows.value.concat(result.rows);
  } catch (e: any) {
    uni.showToast({ title: e?.message || '加载失败', icon: 'none', duration: 2200 });
  } finally {
    loading.value = false;
  }
};

const switchTable = (key: string) => {
  if (key === activeKey.value) return;
  activeKey.value = key;
  keyword.value = '';
  rows.value = [];
  loadPage(true);
};

const doSearch = () => {
  loadPage(true);
};

const clearKeyword = () => {
  keyword.value = '';
  loadPage(true);
};

const loadMore = () => {
  loadPage(false);
};

/* ------------------------------------------------------------------ */

const openEdit = (row: Record<string, any>) => {
  editingId.value = pkOf(row);
  original.value = { ...row };
  draft.value = { ...row };
  editing.value = true;
};

const closeEdit = () => {
  editing.value = false;
  editingId.value = '';
  original.value = {};
  draft.value = {};
};

const NUMERIC_TYPES = ['int', 'bigint', 'smallint', 'tinyint', 'mediumint', 'decimal', 'float', 'double'];

/** 表单字符串 → 数据库值：数字列转数字，图片地址转回相对路径 */
const normalizeValue = (col: AdminColumn, raw: any) => {
  const text = String(raw ?? '').trim();
  if (text === '') {
    return col.nullable ? null : '';
  }
  if (NUMERIC_TYPES.indexOf(col.type) > -1) {
    const num = Number(text);
    return Number.isNaN(num) ? null : num;
  }
  return toStorageUrl(text);
};

const saveEdit = async () => {
  const body: Record<string, any> = {};
  editableColumns.value.forEach((col) => {
    const before = original.value[col.name];
    const after = draft.value[col.name];
    const beforeText = before === null || before === undefined ? '' : String(before);
    const afterText = after === null || after === undefined ? '' : String(after);
    if (beforeText === afterText) return;
    body[col.name] = normalizeValue(col, after);
  });

  if (!Object.keys(body).length) {
    uni.showToast({ title: '没有改动任何字段', icon: 'none' });
    return;
  }

  uni.showLoading({ title: '保存中...', mask: true });
  try {
    await apiAdminUpdate(activeKey.value, editingId.value, body);
    uni.hideLoading();
    uni.showToast({ title: '已保存', icon: 'success' });
    closeEdit();
    await loadTables();
    await loadPage(true);
  } catch (e: any) {
    uni.hideLoading();
    uni.showToast({ title: e?.message || '保存失败', icon: 'none', duration: 2400 });
  }
};

const confirmDelete = () => {
  uni.showModal({
    title: '删除记录',
    content: `确定要删除 ${editingId.value} 吗？此操作不可撤销。`,
    confirmText: '删除',
    confirmColor: '#FF4D4F',
    cancelText: '取消',
    success: async (res) => {
      if (!res.confirm) return;
      uni.showLoading({ title: '删除中...', mask: true });
      try {
        await apiAdminDelete(activeKey.value, editingId.value);
        uni.hideLoading();
        uni.showToast({ title: '已删除', icon: 'success' });
        closeEdit();
        await loadTables();
        await loadPage(true);
      } catch (e: any) {
        uni.hideLoading();
        uni.showToast({ title: e?.message || '删除失败', icon: 'none', duration: 2400 });
      }
    }
  });
};

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};

/* ------------------------------------------------------------------ */

onLoad(() => {
  if (!userStore.currentUser.isAdmin) {
    uni.showModal({
      title: '无权访问',
      content: '当前账号不是管理员，无法进入管理后台。',
      showCancel: false,
      success: () => uni.navigateBack({ delta: 1 })
    });
    return;
  }
  (async () => {
    uni.showLoading({ title: '加载中...', mask: true });
    try {
      await loadTables();
      const first = tables.value[0];
      if (first) {
        activeKey.value = first.key;
      }
      await loadPage(true);
    } catch (e: any) {
      uni.showToast({ title: e?.message || '加载失败', icon: 'none', duration: 2200 });
    } finally {
      uni.hideLoading();
    }
  })();
});
</script>

<style lang="scss" scoped>
.admin-page {
  min-height: 100vh;
  background-color: #F6F7FB;
  padding: 0 28rpx 60rpx 28rpx;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.status-bar {
  height: 60rpx;
}

.header-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0 24rpx 0;
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

.admin-name {
  font-size: calc(26rpx * var(--font-scale, 1));
  color: #1B65F0;
  font-weight: 700;
  max-width: 160rpx;
  overflow: hidden;
  white-space: nowrap;
}

/* 表切换 */
.table-tabs {
  white-space: nowrap;
  margin-bottom: 20rpx;
}

.table-chip {
  display: inline-flex;
  align-items: center;
  padding: 14rpx 26rpx;
  margin-right: 16rpx;
  border-radius: 999rpx;
  background-color: #FFFFFF;
  box-shadow: 0 4rpx 14rpx rgba(0, 0, 0, 0.04);

  &.active {
    background-color: #1B65F0;

    .chip-label,
    .chip-count {
      color: #FFFFFF;
    }
  }
}

.chip-label {
  font-size: calc(28rpx * var(--font-scale, 1));
  font-weight: 700;
  color: #1F2329;
}

.chip-count {
  font-size: calc(22rpx * var(--font-scale, 1));
  color: #8F959E;
  margin-left: 10rpx;
}

/* 搜索 */
.search-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 18rpx;
}

.search-input {
  flex: 1;
  height: 84rpx;
  background-color: #FFFFFF;
  border-radius: 20rpx;
  padding: 0 24rpx;
  font-size: calc(28rpx * var(--font-scale, 1));
  color: #1F2329;
}

.search-placeholder {
  color: #B0B4BA;
}

.search-btn {
  width: 140rpx;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 20rpx;
  background-color: #1B65F0;
  color: #FFFFFF;
  font-size: calc(28rpx * var(--font-scale, 1));
  font-weight: 700;
  padding: 0;

  &::after {
    border: none;
  }
}

.list-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6rpx 8rpx 16rpx 8rpx;
}

.summary-text {
  font-size: calc(26rpx * var(--font-scale, 1));
  color: #8F959E;
}

.clear-kw {
  font-size: calc(26rpx * var(--font-scale, 1));
  color: #1B65F0;
}

/* 记录列表 */
.record-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.record-card {
  background-color: #FFFFFF;
  border-radius: 24rpx;
  padding: 24rpx 26rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.card-hover {
  background-color: #F0F5FF;
}

.record-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.record-title {
  font-size: calc(30rpx * var(--font-scale, 1));
  font-weight: 700;
  color: #1F2329;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.record-id {
  font-size: calc(22rpx * var(--font-scale, 1));
  color: #B0B4BA;
  margin-top: 8rpx;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.record-edit {
  font-size: calc(26rpx * var(--font-scale, 1));
  color: #1B65F0;
  font-weight: 700;
  margin-left: 16rpx;
  flex-shrink: 0;
}

.empty-tip {
  text-align: center;
  color: #B0B4BA;
  font-size: calc(28rpx * var(--font-scale, 1));
  padding: 60rpx 0;
}

.load-more-btn {
  margin-top: 12rpx;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 22rpx;
  background-color: #FFFFFF;
  color: #1B65F0;
  font-size: calc(28rpx * var(--font-scale, 1));
  font-weight: 700;

  &::after {
    border: none;
  }
}

/* 编辑弹层 */
.edit-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.45);
  z-index: 100;
  display: flex;
  align-items: flex-end;
}

.edit-panel {
  width: 100%;
  max-height: 86vh;
  background-color: #FFFFFF;
  border-radius: 32rpx 32rpx 0 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.edit-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 30rpx 30rpx 20rpx 30rpx;
  border-bottom: 1rpx solid #F0F1F5;
}

.edit-title-wrap {
  display: flex;
  flex-direction: column;
}

.edit-title {
  font-size: calc(32rpx * var(--font-scale, 1));
  font-weight: 900;
  color: #1F2329;
}

.edit-subtitle {
  font-size: calc(22rpx * var(--font-scale, 1));
  color: #B0B4BA;
  margin-top: 8rpx;
}

.edit-close {
  font-size: 36rpx;
  color: #8F959E;
  padding: 0 10rpx;
}

.edit-body {
  flex: 1;
  max-height: 58vh;
  padding: 16rpx 30rpx 0 30rpx;
  box-sizing: border-box;
}

.field-row {
  padding: 14rpx 0;
  border-bottom: 1rpx solid #F5F6F8;
}

.field-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10rpx;
}

.field-label {
  font-size: calc(26rpx * var(--font-scale, 1));
  font-weight: 700;
  color: #4E5969;
}

.field-key {
  font-size: calc(20rpx * var(--font-scale, 1));
  color: #C4C8CE;
}

.field-input {
  width: 100%;
  min-height: 76rpx;
  background-color: #F6F7FB;
  border-radius: 16rpx;
  padding: 14rpx 20rpx;
  font-size: calc(28rpx * var(--font-scale, 1));
  color: #1F2329;
  box-sizing: border-box;
}

.field-placeholder {
  color: #C4C8CE;
}

.edit-footer {
  display: flex;
  gap: 20rpx;
  padding: 24rpx 30rpx calc(24rpx + env(safe-area-inset-bottom)) 30rpx;
  border-top: 1rpx solid #F0F1F5;
}

.foot-btn {
  flex: 1;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 24rpx;
  font-size: calc(30rpx * var(--font-scale, 1));
  font-weight: 800;
  padding: 0;

  &::after {
    border: none;
  }

  &.primary {
    background-color: #1B65F0;
    color: #FFFFFF;
  }

  &.danger {
    background-color: #FFF1F0;
    color: #FF4D4F;
  }
}

.btn-hover {
  opacity: 0.85;
}
</style>
