<script setup lang="ts">
import { onLaunch, onShow, onHide } from "@dcloudio/uni-app";
import { useSettingStore } from "./stores/setting";
import { initDb } from "./utils/db";
import { bootstrapStores } from "./utils/session";
import { API_BASE_URL } from "./api/config";
import { hasToken } from "./utils/token";

/** 页面栈里是否已经是登录页，避免重复跳转 */
const isOnLoginPage = (): boolean => {
  const pages = getCurrentPages();
  if (!pages.length) return false;
  const route = (pages[pages.length - 1] as any).route || "";
  return route.indexOf("auth/login") > -1;
};

onLaunch(() => {
  // 未登录直接去登录页，不拉数据（后端接口也要求令牌）
  if (!hasToken()) {
    // 只初始化本机偏好（字号），业务 store 等登录后拉到数据再初始化。
    // 否则快照为空，setting 会把默认值回写后端并被鉴权拦下。
    useSettingStore().initSetting();
    if (!isOnLoginPage()) {
      uni.reLaunch({ url: "/pages/auth/login" });
    }
    return;
  }

  initDb()
    .then(() => {
      bootstrapStores();
    })
    .catch((e) => {
      console.error("[App] 数据加载失败", e);
      uni.showModal({
        title: "数据加载失败",
        content: `无法连接后端服务，请确认已启动（${API_BASE_URL}）`,
        confirmText: "重试",
        cancelText: "稍后",
        success: (res) => {
          if (res.confirm) {
            uni.reLaunch({ url: "/pages/auth/login" });
          }
        }
      });
    });
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
