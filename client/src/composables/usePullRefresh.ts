import { onPullDownRefresh } from '@dcloudio/uni-app';
import { refreshAll } from '../utils/session';

/**
 * 页面下拉刷新
 *
 * 统一走 refreshAll：重新拉 bootstrap 快照并同步所有 store，
 * 因此首页、订单、消息、广场、我的都能拿到同一时刻的数据。
 *
 * onPullDownRefresh 必须在 setup 期间同步注册，所以这是个组合式函数，
 * 不能在异步回调里调用。
 *
 * @param after 刷新成功后的额外处理（例如重新计算列表滚动位置）
 */
export const usePullRefresh = (after?: () => void) => {
  onPullDownRefresh(async () => {
    try {
      await refreshAll();
      if (after) after();
    } catch (e: any) {
      console.error('[pull-refresh] 刷新失败', e);
      uni.showToast({
        title: e?.message || '刷新失败，请检查后端是否已启动',
        icon: 'none',
        duration: 2200
      });
    } finally {
      // 无论如何都要收起下拉动画，否则会一直转圈
      uni.stopPullDownRefresh();
    }
  });
};
