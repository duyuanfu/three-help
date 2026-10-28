import { API_BASE_URL } from '../api/config';

/** 登录令牌的本地存储 key */
const TOKEN_KEY = 'bbb_token';
const USER_KEY = 'bbb_cached_user_id';

export const getToken = (): string => {
  try {
    return uni.getStorageSync(TOKEN_KEY) || '';
  } catch (e) {
    return '';
  }
};

export const setToken = (token: string) => {
  try {
    uni.setStorageSync(TOKEN_KEY, token);
  } catch (e) {
    console.error('[token] 保存失败', e);
  }
};

export const clearToken = () => {
  try {
    uni.removeStorageSync(TOKEN_KEY);
    uni.removeStorageSync(USER_KEY);
  } catch (e) {
    // 忽略
  }
};

export const setCachedUserId = (id: string) => {
  try {
    uni.setStorageSync(USER_KEY, id);
  } catch (e) {
    // 忽略
  }
};

export const getCachedUserId = (): string => {
  try {
    return uni.getStorageSync(USER_KEY) || '';
  } catch (e) {
    return '';
  }
};

/** 是否已登录（仅判断本地是否有令牌，真正的有效性由服务端校验） */
export const hasToken = (): boolean => Boolean(getToken());

/** 后端地址（用于拼接上传文件地址） */
export const API_ORIGIN = API_BASE_URL.replace(/\/api\/?$/, '');

/**
 * 把数据库里的相对头像地址转成可显示的完整地址
 * - /uploads/xxx → http://host:8090/uploads/xxx（服务器上传目录）
 * - /static/xxx  → 保持原样（小程序包内静态资源）
 * - http(s)://   → 保持原样
 */
export const resolveAssetUrl = (url?: string): string => {
  if (!url) return '';
  if (url.indexOf('http://') === 0 || url.indexOf('https://') === 0) return url;
  if (url.indexOf('/uploads/') === 0) return API_ORIGIN + url;
  return url;
};

/** 反向：把完整地址转回数据库存储的相对地址 */
export const toStorageUrl = (url?: string): string => {
  if (!url) return '';
  if (url.indexOf(API_ORIGIN + '/') === 0) return url.slice(API_ORIGIN.length);
  return url;
};
