/**
 * 登录态与数据装载
 *
 * 小程序的 App.onLaunch 只在冷启动时执行一次，登录页 reLaunch 回首页
 * **不会**重新触发 onLaunch。因此登录/注册成功后必须在这里主动
 * 重新拉取 bootstrap 并初始化各 store，否则首页会是空的。
 */

import { reloadAll } from './db';
import { useSettingStore } from '../stores/setting';
import { useUserStore } from '../stores/user';
import { useOrderStore } from '../stores/order';
import { useChatStore } from '../stores/chat';
import { useSquareStore } from '../stores/square';
import { useMallStore } from '../stores/mall';

/** 用当前内存快照初始化所有 store（快照必须先加载完） */
export const bootstrapStores = (): void => {
  useSettingStore().initSetting();
  useUserStore().initUser();
  useOrderStore().initOrders();
  useChatStore().initChat();
  // 广场动态/评论不在快照里，这里只清状态，真正拉取由页面 onShow 触发
  useSquareStore().resetSquare();
  useMallStore().initMall();
};

/**
 * 登录/注册成功后调用：带上刚拿到的令牌重新拉数据，再初始化 store。
 * 失败只告警，不阻断跳转（用户至少能进首页看到空态与错误提示）。
 */
export const refreshAfterLogin = async (): Promise<void> => {
  try {
    await reloadAll();
  } catch (e) {
    console.error('[session] 登录后数据加载失败', e);
  }
  bootstrapStores();
};

/**
 * 下拉刷新：重新拉取全部业务数据并同步到各 store。
 *
 * 与 refreshAfterLogin 的区别只是不做异常兜底 —— 调用方需要知道失败，
 * 以便提示"刷新失败"并结束下拉动画。
 */
export const refreshAll = async (): Promise<void> => {
  await reloadAll();
  bootstrapStores();
};
