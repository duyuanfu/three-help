import { defineStore } from 'pinia';
import type { UserProfile } from '../types';
import { initialUser } from '../mock/seeds';

const STORAGE_KEY = 'bbb_user';

export const useUserStore = defineStore('user', {
  state: () => ({
    currentUser: { ...initialUser } as UserProfile,
    role: 'elder' as 'elder' | 'volunteer',
    isLoggedIn: false
  }),
  actions: {
    initUser() {
      try {
        const cached = uni.getStorageSync(STORAGE_KEY);
        if (cached && cached.id) {
          this.currentUser = cached;
          this.isLoggedIn = Boolean(cached.isLoggedIn);
        } else {
          this.saveUser();
        }
      } catch (e) {
        console.error('Failed to load user', e);
      }
    },
    saveUser() {
      try {
        uni.setStorageSync(STORAGE_KEY, {
          ...this.currentUser,
          isLoggedIn: this.isLoggedIn
        });
      } catch (e) {
        console.error('Failed to save user', e);
      }
    },
    login() {
      this.isLoggedIn = true;
      this.saveUser();
    },
    logout() {
      this.isLoggedIn = false;
      this.saveUser();
    },
    updateProfile(updates: Partial<UserProfile>) {
      this.currentUser = { ...this.currentUser, ...updates };
      this.saveUser();
    },
    addPoints(amount: number) {
      this.currentUser.points += amount;
      this.saveUser();
    },
    deductPoints(amount: number) {
      if (this.currentUser.points >= amount) {
        this.currentUser.points -= amount;
        this.saveUser();
        return true;
      }
      return false;
    },
    setRealName(realName: string, idCard: string, tag?: string) {
      this.currentUser.isRealName = true;
      this.currentUser.name = realName;
      if (tag) this.currentUser.identityTag = tag;
      this.saveUser();
    },
    switchRole(role: 'elder' | 'volunteer') {
      this.role = role;
    }
  }
});
