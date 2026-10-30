import { request } from './request';
import type { UserProfile } from '../types';

export interface AuthResult {
  token: string;
  user: UserProfile;
}

export interface RegisterPayload {
  name: string;
  age?: number;
  phone: string;
  password: string;
  gender?: string;
  role?: string;
}

/** 登录：手机号 + 密码，成功后拿到令牌与用户资料 */
export const apiLogin = (phone: string, password: string): Promise<AuthResult> => {
  return request<AuthResult>({
    url: '/auth/login',
    method: 'POST',
    data: { phone, password },
    skipAuthRedirect: true
  });
};

/** 注册：直接创建账号并返回登录态 */
export const apiRegister = (payload: RegisterPayload): Promise<AuthResult> => {
  return request<AuthResult>({
    url: '/auth/register',
    method: 'POST',
    data: payload,
    skipAuthRedirect: true
  });
};

/** 退出登录：服务端令牌失效 */
export const apiLogout = (): Promise<void> => {
  return request<void>({ url: '/auth/logout', method: 'POST', silent: true });
};

/** 微信登录结果：已绑定微信的账号直接拿到 token，未绑定则返回一次性票据 */
export interface WxLoginResult {
  needBind: boolean;
  ticket?: string;
  token?: string;
  user?: UserProfile;
}

export interface WxBindPayload {
  ticket: string;
  phone: string;
  password: string;
  /** 首次使用微信登录（手机号未注册）时必填 */
  name?: string;
  age?: number;
}

/** 微信登录：传 uni.login 拿到的 code */
export const apiWxLogin = (code: string): Promise<WxLoginResult> => {
  return request<WxLoginResult>({
    url: '/auth/wx-login',
    method: 'POST',
    data: { code },
    skipAuthRedirect: true
  });
};

/** 微信授权后绑定手机号：手机号未注册即注册，已注册需校验密码 */
export const apiWxBind = (payload: WxBindPayload): Promise<AuthResult> => {
  return request<AuthResult>({
    url: '/auth/wx-bind',
    method: 'POST',
    data: payload,
    skipAuthRedirect: true
  });
};

/** 拉取当前登录用户（用于校验令牌是否仍然有效） */
export const apiMe = (): Promise<UserProfile> => {
  return request<UserProfile>({ url: '/auth/me', silent: true });
};

/** 更新个人资料（服务端只认当前登录用户，id 段无实际意义） */
export const apiUpdateProfile = (patch: Partial<UserProfile>): Promise<UserProfile> => {
  return request<UserProfile>({
    url: '/tables/user/me',
    method: 'POST',
    data: patch
  });
};
