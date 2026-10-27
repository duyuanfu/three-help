<template>
  <view class="msg-page-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部标题 -->
    <view class="page-title-wrap">
      <text class="page-title">{{ activeModule === 'session' ? '消 息' : '联系人' }}</text>
    </view>

    <!-- 搜索栏 (按姓名检索) -->
    <view class="search-box">
      <text class="search-icon">🔍</text>
      <input 
        v-model="chatStore.searchKeyword" 
        class="search-input" 
        placeholder="搜索联系人姓名..." 
        placeholder-class="search-placeholder"
      />
    </view>

    <!-- 顶部两大功能方块按钮 (私信 / 联系人) 严格对齐原型 -->
    <view class="module-toggle-row">
      <view 
        class="module-square-btn btn-orange" 
        :class="{ 'btn-selected': activeModule === 'session' }"
        hover-class="btn-hover"
        @tap="activeModule = 'session'"
      >
        <text class="module-btn-text">私 信</text>
      </view>
      <view 
        class="module-square-btn btn-yellow" 
        :class="{ 'btn-selected': activeModule === 'contact' }"
        hover-class="btn-hover"
        @tap="activeModule = 'contact'"
      >
        <text class="module-btn-text">联系人</text>
      </view>
    </view>

    <!-- 灰色分隔条 -->
    <view class="gray-divider"></view>

    <!-- 1. 私信会话列表模块 (原型消息页1.png) -->
    <view v-if="activeModule === 'session'" class="session-section">
      <view class="section-header">
        <text class="section-title">私信</text>
      </view>

      <scroll-view scroll-y class="session-list-scroll">
        <view 
          v-for="item in sessionList" 
          :key="item.sessionId" 
          class="session-row-item"
          hover-class="item-hover"
          @tap="goToChat(item.sessionId)"
        >
          <image :src="item.targetUser.avatar" mode="aspectFill" class="session-avatar" />
          <view class="session-name-wrap">
            <text class="session-name">
              {{ item.targetUser.name }}{{ item.targetUser.tag || '' }}
            </text>
          </view>
          <view class="session-time-wrap">
            <text class="session-time">{{ item.lastTime }}</text>
          </view>
        </view>

        <view class="elder-no-more">
          <text>——暂无更多——</text>
        </view>
      </scroll-view>
    </view>

    <!-- 2. 联系人通讯录模块 (原型消息页2.png 带右侧 A-Z 索引) -->
    <view v-else class="contact-section">
      <scroll-view 
        scroll-y 
        class="contact-list-scroll" 
        :scroll-into-view="currentAnchor"
      >
        <view 
          v-for="group in contactGroups" 
          :key="group.letter" 
          :id="'anchor_' + group.letter"
          class="letter-group"
        >
          <!-- 灰色字母栏 -->
          <view class="letter-header-bar">
            <text class="letter-title">{{ group.letter }}</text>
          </view>

          <!-- 该字母下的联系人列表 -->
          <view 
            v-for="c in group.list" 
            :key="c.id" 
            class="contact-row-item"
            hover-class="item-hover"
            @tap="handleContactClick(c)"
          >
            <image :src="c.avatar" mode="aspectFill" class="contact-avatar" />
            <text class="contact-name">{{ c.name }}{{ c.tag || '' }}</text>
          </view>
        </view>

        <view class="elder-no-more">
          <text>——暂无更多——</text>
        </view>

        <!-- + 新增联系人大按钮 (原型样式) -->
        <view class="add-contact-btn-wrap">
          <button class="add-contact-btn" hover-class="btn-hover" @tap="goToAddContact">
            + 新增联系人
          </button>
        </view>
      </scroll-view>

      <!-- 右侧 A-Z 首字母快速导航条 (原型样式) -->
      <view class="alphabet-index-bar">
        <text 
          v-for="char in alphabetList" 
          :key="char" 
          class="alpha-char"
          :class="{ 'alpha-char-active': currentLetter === char }"
          @tap="scrollToLetter(char)"
        >
          {{ char }}
        </text>
      </view>
    </view>

    <!-- 新增联系人弹窗 -->
    <view v-if="showAddDialog" class="modal-mask">
      <view class="modal-dialog">
        <text class="modal-title">新增联系人</text>
        <view class="modal-field">
          <text class="modal-label">姓名：</text>
          <input v-model="newContactName" placeholder="如：李建国" class="modal-input" />
        </view>
        <view class="modal-field">
          <text class="modal-label">手机：</text>
          <input v-model="newContactPhone" placeholder="如：13800000000" class="modal-input" type="number" />
        </view>
        <view class="modal-field">
          <text class="modal-label">职业：</text>
          <input v-model="newContactTag" placeholder="如：@退休教师 / @学生" class="modal-input" />
        </view>
        <view class="modal-actions">
          <button class="modal-btn cancel" @tap="showAddDialog = false">取消</button>
          <button class="modal-btn confirm" @tap="confirmAddContact">保存</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useChatStore } from '../../stores/chat';
import { useSettingStore } from '../../stores/setting';
import type { ContactItem } from '../../types';

const chatStore = useChatStore();
const settingStore = useSettingStore();

const activeModule = ref<'session' | 'contact'>('session');
const currentLetter = ref('L');
const currentAnchor = ref('');

const showAddDialog = ref(false);
const newContactName = ref('');
const newContactPhone = ref('');
const newContactTag = ref('');

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const alphabetList = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');

const sessionList = computed(() => {
  return chatStore.filteredSessions;
});

const contactGroups = computed(() => {
  return chatStore.groupedContacts;
});

const scrollToLetter = (letter: string) => {
  currentLetter.value = letter;
  currentAnchor.value = 'anchor_' + letter;
};

const goToChat = (sessionId: string) => {
  uni.navigateTo({
    url: `/pages/message/chat?sessionId=${sessionId}`
  });
};

const handleContactClick = (contact: ContactItem) => {
  const session = chatStore.openOrCreateSession({
    id: contact.id,
    name: contact.name,
    avatar: contact.avatar,
    tag: contact.tag
  });
  goToChat(session.sessionId);
};

const goToAddContact = () => {
  uni.navigateTo({
    url: '/pages/message/add-contact'
  });
};

const confirmAddContact = () => {
  if (!newContactName.value.trim()) {
    uni.showToast({ title: '请输入联系人姓名', icon: 'none' });
    return;
  }
  chatStore.addContact({
    name: newContactName.value.trim(),
    phone: newContactPhone.value.trim() || '138****0000',
    tag: newContactTag.value.trim()
  });
  showAddDialog.value = false;
  newContactName.value = '';
  newContactPhone.value = '';
  newContactTag.value = '';
  uni.showToast({ title: '新增联系人成功', icon: 'success' });
};
</script>

<style lang="scss" scoped>
.msg-page-container {
  min-height: 100vh;
  background-color: #F6F7FB;
  padding: 0 32rpx 140rpx 32rpx;
  display: flex;
  flex-direction: column;
  position: relative;
}

.status-bar {
  height: 60rpx;
}

.page-title-wrap {
  text-align: center;
  padding: 10rpx 0 24rpx 0;
}

.page-title {
  font-size: calc(52rpx * var(--font-scale));
  font-weight: 900;
  color: #1B65F0;
  letter-spacing: 4rpx;
}

/* 搜索框 */
.search-box {
  background-color: #FFFFFF;
  border-radius: 40rpx;
  height: calc(88rpx * var(--font-scale));
  display: flex;
  align-items: center;
  padding: 0 28rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.05);
  margin-bottom: 30rpx;

  .search-icon {
    font-size: 34rpx;
    margin-right: 14rpx;
  }

  .search-input {
    flex: 1;
    font-size: calc(32rpx * var(--font-scale));
    color: #1F2329;
  }

  .search-placeholder {
    color: #8F959E;
    font-size: calc(30rpx * var(--font-scale));
  }
}

/* 顶部两大功能按钮 (橙色私信 + 黄色联系人) 严格对齐原型尺寸 */
.module-toggle-row {
  display: flex;
  justify-content: center;
  gap: 36rpx;
  margin-bottom: 32rpx;
}

.module-square-btn {
  width: 260rpx;
  height: 200rpx;
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.08);
  transition: transform 0.15s;

  .module-btn-text {
    font-size: calc(50rpx * var(--font-scale));
    font-weight: 900;
    color: #FFFFFF;
    letter-spacing: 4rpx;
  }
}

.btn-orange {
  background-color: #FFA940;
}

.btn-yellow {
  background-color: #FFC000;
}

.btn-selected {
  transform: scale(1.03);
  box-shadow: 0 12rpx 30rpx rgba(0, 0, 0, 0.16);
}

.gray-divider {
  height: 36rpx;
  background-color: #E5E7EB;
  margin: 0 -32rpx 20rpx -32rpx;
}

/* 私信流 */
.session-section {
  flex: 1;
}

.section-header {
  padding: 10rpx 0 16rpx 0;
  
  .section-title {
    font-size: calc(36rpx * var(--font-scale));
    font-weight: 900;
    color: #1F2329;
  }
}

.session-list-scroll {
  flex: 1;
}

.session-row-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 2rpx solid #E5E7EB;
}

.session-avatar {
  width: 90rpx;
  height: 90rpx;
  border-radius: 50%;
  margin-right: 24rpx;
  background-color: #F0F2F5;
}

.session-name-wrap {
  flex: 1;

  .session-name {
    font-size: calc(36rpx * var(--font-scale));
    font-weight: 800;
    color: #1F2329;
  }
}

.session-time-wrap {
  .session-time {
    font-size: calc(30rpx * var(--font-scale));
    color: #1F2329;
    font-weight: 600;
    letter-spacing: 2rpx;
  }
}

/* 联系人通讯录 */
.contact-section {
  flex: 1;
  position: relative;
}

.contact-list-scroll {
  height: calc(100vh - 460rpx);
  padding-right: 48rpx;
}

.letter-header-bar {
  background-color: #E5E7EB;
  margin: 0 -32rpx;
  padding: 6rpx 32rpx;

  .letter-title {
    font-size: calc(34rpx * var(--font-scale));
    font-weight: 900;
    color: #1F2329;
  }
}

.contact-row-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 2rpx solid #E5E7EB;
}

.contact-avatar {
  width: 90rpx;
  height: 90rpx;
  border-radius: 50%;
  margin-right: 24rpx;
  background-color: #F0F2F5;
}

.contact-name {
  font-size: calc(36rpx * var(--font-scale));
  font-weight: 800;
  color: #1F2329;
}

.add-contact-btn-wrap {
  padding: 20rpx 0 40rpx 0;
}

.add-contact-btn {
  background-color: #FFFFFF;
  color: #1F2329;
  border: 3rpx solid #E5E7EB;
  border-radius: 40rpx;
  height: calc(96rpx * var(--font-scale));
  line-height: calc(96rpx * var(--font-scale));
  font-size: calc(36rpx * var(--font-scale));
  font-weight: 900;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);

  &::after {
    border: none;
  }
}

/* A-Z 首字母悬浮条 */
.alphabet-index-bar {
  position: absolute;
  right: -10rpx;
  top: 10rpx;
  bottom: 80rpx;
  width: 44rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: center;

  .alpha-char {
    font-size: 22rpx;
    font-weight: 700;
    color: #1F2329;
  }

  .alpha-char-active {
    color: #FFFFFF;
    background-color: #1B65F0;
    border-radius: 50%;
    width: 32rpx;
    height: 32rpx;
    text-align: center;
    line-height: 32rpx;
  }
}

/* 弹窗 */
.modal-mask {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-dialog {
  width: 620rpx;
  background-color: #FFFFFF;
  border-radius: 36rpx;
  padding: 40rpx;
  box-shadow: 0 12rpx 36rpx rgba(0, 0, 0, 0.2);
}

.modal-title {
  display: block;
  text-align: center;
  font-size: calc(40rpx * var(--font-scale));
  font-weight: 900;
  color: #1B65F0;
  margin-bottom: 30rpx;
}

.modal-field {
  margin-bottom: 24rpx;
}

.modal-label {
  display: block;
  font-size: calc(30rpx * var(--font-scale));
  font-weight: 700;
  color: #1F2329;
  margin-bottom: 8rpx;
}

.modal-input {
  background-color: #F6F7FB;
  border: 2rpx solid #E5E7EB;
  border-radius: 20rpx;
  height: 80rpx;
  padding: 0 20rpx;
  font-size: calc(30rpx * var(--font-scale));
}

.modal-actions {
  display: flex;
  gap: 24rpx;
  margin-top: 36rpx;
}

.modal-btn {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 24rpx;
  font-size: calc(32rpx * var(--font-scale));
  font-weight: 800;

  &.cancel {
    background-color: #F0F2F5;
    color: #666;
  }

  &.confirm {
    background-color: #1B65F0;
    color: #FFFFFF;
  }

  &::after {
    border: none;
  }
}

.item-hover, .btn-hover {
  opacity: 0.85;
}
</style>
