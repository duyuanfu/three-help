<template>
  <view class="faq-page-container" :style="pageStyle">
    <!-- 顶部状态栏 -->
    <view class="status-bar"></view>

    <!-- 顶部导航栏 (严格对齐关于账号\帮助.png) -->
    <view class="header-nav">
      <view class="back-btn" hover-class="btn-hover" @tap="goBack">
        <text class="back-text">←返回</text>
      </view>
      <text class="page-title">常见问题</text>
      <view class="nav-right-placeholder"></view>
    </view>

    <!-- 问答内容列表 (严格对齐原型蓝色竖条与高对比大字) -->
    <scroll-view scroll-y class="faq-scroll-body">
      <view 
        v-for="(item, index) in faqList" 
        :key="index" 
        class="faq-qa-item"
      >
        <view class="faq-question-row">
          <view class="blue-vertical-bar"></view>
          <text class="faq-q-text">{{ item.question }}</text>
        </view>
        <view class="faq-answer-wrap">
          <text class="faq-a-text">{{ item.answer }}</text>
        </view>
      </view>

      <view class="elder-no-more">
        <text>——生活好帮手，遇事有帮手——</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useSettingStore } from '../../stores/setting';

const settingStore = useSettingStore();

const pageStyle = computed(() => ({
  '--font-scale': settingStore.fontScale
}));

const faqList = [
  {
    question: '如何使用帮帮帮 ？',
    answer: '家里有事，您可以使用帮帮帮下单，点首页的“一键求助”，买选菜、修东西、陪伴等，写清楚时间地点，按“发布”，附近能帮忙的人会联系您。'
  },
  {
    question: '金币怎么积累的 ？',
    answer: '通过您下单金额和接单金额进行累计金币，当前比例100金币=1元现金。'
  },
  {
    question: '来帮忙的人可靠吗 ？',
    answer: '帮帮帮要求身份证填写以及人脸识别实名认证，您可以看到对方有无实名。帮帮帮建议您第一次最好白天见面，感觉不对，请立即取消。'
  },
  {
    question: '如何联系客服 ？',
    answer: '进入帮帮帮，点首页的“线上咨询”即可联系到客服，可以发消息和打电话，人工时间：每天8:00-20:00，有紧急情况请拨打110。'
  }
];

const goBack = () => {
  uni.navigateBack({ delta: 1 });
};
</script>

<style lang="scss" scoped>
.faq-page-container {
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

.faq-scroll-body {
  flex: 1;
}

.faq-qa-item {
  margin-bottom: 48rpx;
}

.faq-question-row {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;

  .blue-vertical-bar {
    width: 14rpx;
    height: 44rpx;
    background-color: #1B65F0;
    border-radius: 4rpx;
    margin-right: 18rpx;
  }

  .faq-q-text {
    font-size: calc(40rpx * var(--font-scale));
    font-weight: 900;
    color: #1F2329;
    letter-spacing: 1rpx;
  }
}

.faq-answer-wrap {
  padding-left: 32rpx;

  .faq-a-text {
    font-size: calc(32rpx * var(--font-scale));
    font-weight: 600;
    color: #1F2329;
    line-height: 1.6;
    letter-spacing: 1rpx;
  }
}

.btn-hover {
  opacity: 0.88;
}
</style>
