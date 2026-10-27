import { defineStore } from 'pinia';
import type { AppSetting } from '../types';

const STORAGE_KEY = 'bbb_setting';

export const useSettingStore = defineStore('setting', {
  state: (): AppSetting => ({
    fontScaleLevel: 1, // 0: 小, 1: 标准, 2: 大, 3: 特大
    fontScale: 1.0
  }),
  actions: {
    initSetting() {
      try {
        const cached = uni.getStorageSync(STORAGE_KEY);
        if (cached && typeof cached.fontScaleLevel === 'number') {
          this.fontScaleLevel = cached.fontScaleLevel;
          this.fontScale = cached.fontScale || 1.0;
        }
      } catch (e) {
        console.error('Failed to load setting from storage', e);
      }
    },
    setFontScaleLevel(level: number) {
      this.fontScaleLevel = level;
      const scaleMap = [0.85, 1.0, 1.15, 1.3];
      this.fontScale = scaleMap[level] || 1.0;
      try {
        uni.setStorageSync(STORAGE_KEY, {
          fontScaleLevel: this.fontScaleLevel,
          fontScale: this.fontScale
        });
      } catch (e) {
        console.error('Failed to save setting to storage', e);
      }
    }
  }
});
