<script setup lang="ts">
import { onLaunch, onShow, onHide } from "@dcloudio/uni-app";
import { useSettingStore } from "./stores/setting";
import { useUserStore } from "./stores/user";
import { useOrderStore } from "./stores/order";
import { useChatStore } from "./stores/chat";
import { useSquareStore } from "./stores/square";
import { useMallStore } from "./stores/mall";
import { initDb } from "./utils/db";

onLaunch(() => {
  // 数据层引导：迁移旧版 Storage key，确保所有表可用
  initDb();

  const settingStore = useSettingStore();
  settingStore.initSetting();

  const userStore = useUserStore();
  userStore.initUser();

  const orderStore = useOrderStore();
  orderStore.initOrders();

  const chatStore = useChatStore();
  chatStore.initChat();

  const squareStore = useSquareStore();
  squareStore.initSquare();

  const mallStore = useMallStore();
  mallStore.initMall();
});

onShow(() => {
  // 保持全局字号同步
  const settingStore = useSettingStore();
  settingStore.initSetting();
});

onHide(() => {
  console.log("App Hide");
});
</script>

<style lang="scss">
/* 全局基础配置与适老化基调 */
page {
  background-color: #F6F7FB;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  color: #1F2329;
  box-sizing: border-box;
  -webkit-font-smoothing: antialiased;
}

view, scroll-view, swiper, text, button, input, textarea, label, navigator, image {
  box-sizing: border-box;
}

/* 适老无级/阶梯缩放 CSS 变量体系 */
.scale-wrapper {
  --font-scale: 1;
}

/* 适老卡片样式 */
.elder-card {
  background: #FFFFFF;
  border-radius: 32rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.04);
}

/* 适老操作大按钮 */
.elder-btn-primary {
  background: #1B65F0;
  color: #FFFFFF;
  font-weight: bold;
  border-radius: 24rpx;
  text-align: center;
  box-shadow: 0 8rpx 24rpx rgba(27, 101, 240, 0.25);
  
  &:active {
    opacity: 0.88;
  }
}

.elder-btn-secondary {
  background: #FFFFFF;
  color: #1B65F0;
  border: 3rpx solid #1B65F0;
  font-weight: bold;
  border-radius: 24rpx;
  text-align: center;
  
  &:active {
    background-color: #EBF2FF;
  }
}

/* 暂无更多底线 */
.elder-no-more {
  text-align: center;
  color: #8F959E;
  padding: 36rpx 0;
  letter-spacing: 2rpx;
}
</style>
