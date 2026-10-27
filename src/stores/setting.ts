import { defineStore } from 'pinia';
import type { AppSetting } from '../types';
import { TABLES } from '../utils/db';

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
        } else {
          // 首次启动时写入默认设置，保证表结构完整
          TABLES.setting.replaceAll({ ...DEFAULT_SETTING });
        }
      } catch (e) {
        console.error('Failed to load setting from storage', e);
      }
    },
    setFontScaleLevel(level: number) {
      this.fontScaleLevel = level;
      const scaleMap = [0.85, 1.0, 1.15, 1.3];
      this.fontScale = scaleMap[level] || 1.0;
      TABLES.setting.replaceAll({
        fontScaleLevel: this.fontScaleLevel,
        fontScale: this.fontScale
      });
    }
  }
});
