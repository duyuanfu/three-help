import { ref, onMounted, onUnmounted } from 'vue';

/**
 * 监听微信键盘高度。
 * 页面根节点使用 box-sizing: border-box + padding-bottom 键盘高度，
 * 可让底部输入栏跟随键盘上移，同时避免整页内容被系统推上去。
 */
export function useKeyboardHeight(onChange?: (height: number) => void) {
  const keyboardHeight = ref(0);

  const handler = (res: any) => {
    const height = res && typeof res.height === 'number' ? res.height : 0;
    keyboardHeight.value = height;
    onChange?.(height);
  };

  onMounted(() => {
    const api = uni as any;
    if (typeof api.onKeyboardHeightChange === 'function') {
      api.onKeyboardHeightChange(handler);
    }
  });

  onUnmounted(() => {
    const api = uni as any;
    if (typeof api.offKeyboardHeightChange === 'function') {
      api.offKeyboardHeightChange(handler);
    }
  });

  return { keyboardHeight };
}
