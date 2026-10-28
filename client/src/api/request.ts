import { API_BASE_URL, API_TIMEOUT } from './config';
import { clearToken, getToken, resolveAssetUrl } from '../utils/token';

/** 递归把响应里的 /uploads/xxx 转成可渲染的完整地址（幂等） */
const resolveDeep = (value: any): any => {
  if (typeof value === 'string') return resolveAssetUrl(value);
  if (Array.isArray(value)) return value.map(resolveDeep);
  if (value && typeof value === 'object') {
    const out: Record<string, any> = {};
    Object.keys(value).forEach((key) => {
      out[key] = resolveDeep(value[key]);
    });
    return out;
  }
  return value;
};

/** 后端统一响应体 */
export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

/**
 * 微信小程序的 wx.request 不支持 PATCH，
 * 因此局部更新统一走 POST /api/tables/{table}/{id}
 */
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE';

export interface RequestOptions {
  url: string;
  method?: HttpMethod;
  data?: any;
  /** 静默模式：失败时不弹 toast（后台同步用） */
  silent?: boolean;
  /** 401 时不跳登录页（例如登录接口本身） */
  skipAuthRedirect?: boolean;
}

/** 401 统一处理：清令牌 + 去登录页（防抖，避免并发请求弹多次） */
let redirecting = false;
const handleUnauthorized = (skip?: boolean) => {
  clearToken();
  if (skip || redirecting) return;
  redirecting = true;
  uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
  setTimeout(() => {
    redirecting = false;
    uni.reLaunch({ url: '/pages/auth/login' });
  }, 800);
};

/**
 * 统一请求封装
 * - 自动带上 Authorization: Bearer <token>
 * - 后端约定 code === 0 为成功
 * - code === 401 视为登录失效
 */
export const request = <T = any>(options: RequestOptions): Promise<T> => {
  return new Promise<T>((resolve, reject) => {
    const token = getToken();
    const header: Record<string, string> = {
      'Content-Type': 'application/json'
    };
    if (token) {
      header.Authorization = 'Bearer ' + token;
    }

    uni.request({
      url: API_BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      timeout: API_TIMEOUT,
      header,
      success: (res) => {
        const status = res.statusCode;
        const body = res.data as ApiResult<T>;

        if (status >= 200 && status < 300 && body && body.code === 0) {
          resolve(resolveDeep(body.data));
          return;
        }

        if (body && body.code === 401) {
          handleUnauthorized(options.skipAuthRedirect);
          reject(new Error(body.message || '请先登录'));
          return;
        }

        const message = (body && body.message) || `请求失败（${status}）`;
        if (!options.silent) {
          uni.showToast({ title: message, icon: 'none', duration: 2500 });
        }
        reject(new Error(message));
      },
      fail: (err) => {
        const message = '无法连接服务器，请确认后端已启动';
        if (!options.silent) {
          uni.showToast({ title: message, icon: 'none', duration: 2500 });
        }
        console.error('[api] request failed', options.url, err);
        reject(new Error(err.errMsg || message));
      }
    });
  });
};

/**
 * 上传文件到服务器（头像、动态图片、语音）
 * 返回服务器上的相对地址，例如 /uploads/avatar/202610/xxx.jpg
 */
export const uploadFile = (
  filePath: string,
  kind: 'avatar' | 'image' | 'voice' = 'image'
): Promise<{ url: string; size: number; kind: string }> => {
  return new Promise((resolve, reject) => {
    const token = getToken();
    uni.uploadFile({
      url: API_BASE_URL + '/upload?kind=' + kind,
      filePath,
      name: 'file',
      header: token ? { Authorization: 'Bearer ' + token } : {},
      success: (res) => {
        try {
          const body = JSON.parse(res.data);
          if (body.code === 0 && body.data && body.data.url) {
            resolve(body.data);
            return;
          }
          if (body.code === 401) {
            handleUnauthorized();
          }
          uni.showToast({ title: body.message || '上传失败', icon: 'none' });
          reject(new Error(body.message || '上传失败'));
        } catch (e) {
          uni.showToast({ title: '上传返回格式异常', icon: 'none' });
          reject(new Error('上传返回格式异常'));
        }
      },
      fail: (err) => {
        uni.showToast({ title: '上传失败，请检查网络', icon: 'none' });
        reject(new Error(err.errMsg || '上传失败'));
      }
    });
  });
};
