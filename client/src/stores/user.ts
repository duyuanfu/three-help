import { defineStore } from 'pinia';
import type { UserProfile } from '../types';
import { TABLES } from '../utils/db';
import { getToken, setToken, clearToken, setCachedUserId, resolveAssetUrl } from '../utils/token';
import { apiLogin, apiRegister, apiLogout, apiMe, apiUpdateProfile } from '../api/auth';
import type { RegisterPayload } from '../api/auth';

/** 未登录时的空资料，保证页面不会因为字段缺失而报错 */
const emptyUser = (): UserProfile => ({
  id: '',
  name: '',
  avatar: '',
  gender: '男',
  age: 60,
  phone: '',
  isRealName: false,
  identityTag: '',
  occupation: '',
  address: '',
  organization: '',
  points: 0
});

export const useUserStore = defineStore('user', {
  state: () => ({
    currentUser: emptyUser(),
    role: 'elder' as 'elder' | 'volunteer',
    isLoggedIn: false,
    loading: false
  }),
  getters: {
    /** 是否已登录（令牌 + 用户资料都存在） */
    hasSession: (state) => Boolean(state.currentUser.id) && Boolean(getToken()),
    displayName: (state) => state.currentUser.name || '未登录'
  },
  actions: {
    /** 启动时从后端快照读取当前用户（bootstrap 已按令牌过滤） */
    initUser() {
      try {
        const cached = TABLES.user.all();
        if (cached && cached.id) {
          // 头像统一转成可渲染的完整地址，避免 /uploads/xxx 被当成包内资源而显示空白
          this.currentUser = {
            ...emptyUser(),
            ...cached,
            avatar: resolveAssetUrl(cached.avatar)
          };
          this.role = (cached.role === 'volunteer' ? 'volunteer' : 'elder');
          this.isLoggedIn = true;
        } else {
          this.currentUser = emptyUser();
          this.isLoggedIn = false;
        }
      } catch (e) {
        console.error('[user] 初始化失败', e);
      }
    },

    /** 真实登录：手机号 + 密码 */
    async login(phone: string, password: string) {
      this.loading = true;
      try {
        const result = await apiLogin(phone, password);
        setToken(result.token);
        setCachedUserId(result.user.id);
        this.currentUser = {
          ...emptyUser(),
          ...result.user,
          avatar: resolveAssetUrl(result.user.avatar)
        };
        this.role = (result.user as any).role === 'volunteer' ? 'volunteer' : 'elder';
        this.isLoggedIn = true;
        return result.user;
      } finally {
        this.loading = false;
      }
    },

    /** 真实注册：创建账号后直接进入登录态 */
    async register(payload: RegisterPayload) {
      this.loading = true;
      try {
        const result = await apiRegister(payload);
        setToken(result.token);
        setCachedUserId(result.user.id);
        this.currentUser = {
          ...emptyUser(),
          ...result.user,
          avatar: resolveAssetUrl(result.user.avatar)
        };
        this.role = (result.user as any).role === 'volunteer' ? 'volunteer' : 'elder';
        this.isLoggedIn = true;
        return result.user;
      } finally {
        this.loading = false;
      }
    },

    /** 退出登录：服务端令牌作废 + 清理本地 */
    async logout() {
      try {
        await apiLogout();
      } catch (e) {
        // 网络异常也要能退出
      }
      clearToken();
      this.currentUser = emptyUser();
      this.isLoggedIn = false;
    },

    /** 校验本地令牌是否仍然有效 */
    async refreshMe() {
      if (!getToken()) {
        this.isLoggedIn = false;
        return null;
      }
      try {
        const user = await apiMe();
        this.currentUser = {
          ...emptyUser(),
          ...user,
          avatar: resolveAssetUrl(user.avatar)
        };
        this.isLoggedIn = true;
        return user;
      } catch (e) {
        this.isLoggedIn = false;
        return null;
      }
    },

    /** 更新资料：先落库，成功后再更新本地 */
    /**
     * 更新资料：先本地乐观更新（界面立刻响应），再提交后端。
     *
     * 返回是否真正保存成功。头像保存失败时会回滚本地值 ——
     * 否则界面显示一张服务器上并不存在的图，用户下次进来又变回旧头像，
     * 看起来就像「换了头像但没生效」。
     */
    async updateProfile(updates: Partial<UserProfile>): Promise<boolean> {
      // 本地展示用完整地址，提交给后端用相对地址
      const displayPatch: Partial<UserProfile> = { ...updates };
      if (updates.avatar) {
        displayPatch.avatar = resolveAssetUrl(updates.avatar);
      }
      const previousAvatar = this.currentUser.avatar;
      this.currentUser = { ...this.currentUser, ...displayPatch };
      try {
        const saved = await apiUpdateProfile(updates);
        this.currentUser = {
          ...this.currentUser,
          ...saved,
          avatar: resolveAssetUrl(saved.avatar || this.currentUser.avatar)
        };
        return true;
      } catch (e) {
        console.error('[user] 资料保存失败', e);
        if (updates.avatar) {
          this.currentUser = { ...this.currentUser, avatar: previousAvatar };
        }
        return false;
      }
    },

    async addPoints(amount: number) {
      return this.updateProfile({ points: this.currentUser.points + amount });
    },

    async deductPoints(amount: number) {
      if (this.currentUser.points < amount) {
        return false;
      }
      return this.updateProfile({ points: this.currentUser.points - amount });
    },

    async setRealName(realName: string, idCard: string, tag?: string) {
      const updates: Partial<UserProfile> = { isRealName: true, name: realName };
      if (tag) updates.identityTag = tag;
      await this.updateProfile(updates);
    },

    switchRole(role: 'elder' | 'volunteer') {
      this.role = role;
    }
  }
});
