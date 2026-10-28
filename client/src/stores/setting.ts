import { defineStore } from 'pinia';
import type { AppSetting } from '../types';
import { TABLES } from '../utils/db';
import { hasToken } from '../utils/token';

const DEFAULT_SETTING: AppSetting = {
  fontScaleLevel: 1,
  fontScale: 1.0
};

export const useSettingStore = defineStore('setting', {
  state: (): AppSetting => ({ ...DEFAULT_SETTING }),
  actions: {
    initSetting() {
      try {
        const cached = TABLES.setting.all();
        if (cached && typeof cached.fontScaleLevel === 'number') {
          this.fontScaleLevel = cached.fontScaleLevel;
          this.fontScale = cached.fontScale || 1.0;
          return;
        }
        // 服务端还没有设置记录时，首次启动写入默认值。
        // 未登录不写：写接口需要令牌，否则会被拦成「请先登录」。
        // 此时保持内存里的默认值即可，登录后会随 bootstrap 一起同步。
        if (hasToken()) {
          TABLES.setting.replaceAll(this.syncPayload());
        }
      } catch (e) {
        console.error('Failed to load setting from storage', e);
      }
    },
    setFontScaleLevel(level: number) {
      this.fontScaleLevel = level;
      const scaleMap = [0.85, 1.0, 1.15, 1.3];
      this.fontScale = scaleMap[level] || 1.0;
      if (!hasToken()) {
        // 未登录时只改本机显示，不打扰后端
        return;
      }
      TABLES.setting.replaceAll(this.syncPayload());
    },
    /** 提交给服务端的载荷：带上已有主键，避免后端再补一次 */
    syncPayload() {
      const payload: AppSetting = {
        fontScaleLevel: this.fontScaleLevel,
        fontScale: this.fontScale
      };
      try {
        const cached = TABLES.setting.all();
        if (cached && typeof cached.id === 'number') {
          payload.id = cached.id;
        }
      } catch (e) {
        // 快照还没就绪时忽略，后端会用默认主键补齐
      }
      return payload;
    }
  }
});
