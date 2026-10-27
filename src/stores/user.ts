import { defineStore } from 'pinia';
import type { UserProfile } from '../types';
import { initialUser } from '../mock/seeds';
import { TABLES } from '../utils/db';

export const useUserStore = defineStore('user', {
  state: () => ({
    currentUser: { ...initialUser } as UserProfile,
    role: 'elder' as 'elder' | 'volunteer',
    isLoggedIn: false
  }),
  actions: {
    initUser() {
      try {
        const cached = TABLES.user.all();
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
        TABLES.user.replaceAll({
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
