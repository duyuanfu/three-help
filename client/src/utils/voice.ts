/**
 * 非聊天场景的语音入口统一提示。
 * 线上咨询、私信聊天页的语音输入不受此限制。
 */
export const showVoiceUnsupported = () => {
  uni.showModal({
    title: '语音输入',
    content: '语音输入功能暂不支持，请等待后续版本升级',
    showCancel: false,
    confirmText: '我知道了',
    confirmColor: '#1B65F0'
  });
};
