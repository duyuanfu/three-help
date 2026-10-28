import { ref, onUnmounted } from 'vue';

export interface VoiceRecordResult {
  duration: number;
  audioPath: string;
}

interface UseVoiceChatOptions {
  onSend: (result: VoiceRecordResult) => void;
}

/**
 * 微信式「按住说话」+ 语音播放。
 * 录音使用 RecorderManager，播放使用 InnerAudioContext。
 */
export function useVoiceChat(options: UseVoiceChatOptions) {
  const isVoiceMode = ref(false);
  const isRecording = ref(false);
  const isCancelling = ref(false);
  const recordDuration = ref(0);
  const playingId = ref('');

  let recordTimer: ReturnType<typeof setInterval> | null = null;
  let recordStartY = 0;
  let recordStartAt = 0;
  let touchActive = false;
  let recordStartRequested = false;
  let shouldSend = false;
  let recordCancelled = false;

  let recorderManager: any = null;
  let audioContext: any = null;

  const setupAudio = () => {
    if (audioContext) return;
    const api = uni as any;
    if (typeof api.createInnerAudioContext !== 'function') return;

    audioContext = api.createInnerAudioContext();
    audioContext.obeyMuteSwitch = false;
    audioContext.onEnded(() => {
      playingId.value = '';
    });
    audioContext.onError(() => {
      playingId.value = '';
      uni.showToast({ title: '语音播放失败', icon: 'none' });
    });
  };

  const setupRecorder = () => {
    if (recorderManager) return;
    const api = uni as any;
    if (typeof api.getRecorderManager !== 'function') return;

    recorderManager = api.getRecorderManager();
    recorderManager.onStop((res: any) => {
      const elapsed = Date.now() - recordStartAt;
      const wasRecording = isRecording.value;
      const send = shouldSend || (wasRecording && touchActive && !recordCancelled);
      const durationMs = res && res.duration ? res.duration : elapsed;
      const path = res && res.tempFilePath ? res.tempFilePath : '';
      const cancelled = recordCancelled;
      const tooShort = durationMs < 800;

      stopRecordingUi();
      recordStartRequested = false;

      if (cancelled || !send || tooShort) {
        shouldSend = false;
        recordCancelled = false;
        return;
      }

      shouldSend = false;
      recordCancelled = false;

      if (!path) {
        uni.showToast({ title: '录音失败，请重试', icon: 'none' });
        return;
      }

      options.onSend({
        duration: Math.max(1, Math.round(durationMs / 1000)),
        audioPath: path
      });
    });
    recorderManager.onError(() => {
      stopRecordingUi();
      recordStartRequested = false;
      shouldSend = false;
      recordCancelled = false;
      uni.showToast({ title: '录音失败，请检查麦克风权限', icon: 'none' });
    });
  };

  const startTimer = () => {
    if (recordTimer) clearInterval(recordTimer);
    recordTimer = setInterval(() => {
      recordDuration.value = Math.max(1, Math.floor((Date.now() - recordStartAt) / 1000));
    }, 200);
  };

  const stopRecordingUi = () => {
    isRecording.value = false;
    isCancelling.value = false;
    if (recordTimer) {
      clearInterval(recordTimer);
      recordTimer = null;
    }
  };

  const openRecordSetting = (resolve: (value: boolean) => void) => {
    uni.showModal({
      title: '需要麦克风权限',
      content: '开启麦克风权限后才能发送语音消息',
      confirmText: '去设置',
      success: (r) => {
        if (!r.confirm) {
          resolve(false);
          return;
        }
        uni.openSetting({
          success: (s: any) => resolve(!!(s.authSetting && s.authSetting['scope.record'])),
          fail: () => resolve(false)
        });
      },
      fail: () => resolve(false)
    });
  };

  const requestRecordPermission = (): Promise<boolean> => {
    return new Promise((resolve) => {
      uni.getSetting({
        success: (res: any) => {
          const setting = (res && res.authSetting) || {};
          if (setting['scope.record'] === true) {
            resolve(true);
            return;
          }
          if (setting['scope.record'] === false) {
            openRecordSetting(resolve);
            return;
          }
          uni.authorize({
            scope: 'scope.record',
            success: () => resolve(true),
            fail: () => openRecordSetting(resolve)
          });
        },
        fail: () => resolve(false)
      });
    });
  };

  const toggleVoiceMode = () => {
    if (isRecording.value) return;
    isVoiceMode.value = !isVoiceMode.value;
    uni.hideKeyboard();
  };

  const startVoiceRecord = async (e: any) => {
    if (!isVoiceMode.value || isRecording.value) return;

    const touch = e.touches && e.touches[0];
    recordStartY = touch ? touch.clientY : 0;
    recordStartAt = Date.now();
    recordDuration.value = 0;
    isCancelling.value = false;
    isRecording.value = true;
    touchActive = true;
    shouldSend = false;
    recordCancelled = false;
    recordStartRequested = false;

    startTimer();

    const granted = await requestRecordPermission();
    if (!granted) {
      touchActive = false;
      stopRecordingUi();
      return;
    }
    if (!touchActive) return;

    setupRecorder();
    if (!recorderManager) {
      touchActive = false;
      stopRecordingUi();
      uni.showToast({ title: '当前环境不支持录音', icon: 'none' });
      return;
    }

    recordStartRequested = true;
    try {
      recorderManager.start({
        duration: 60000,
        sampleRate: 16000,
        numberOfChannels: 1,
        encodeBitRate: 48000,
        format: 'mp3'
      });
    } catch (err) {
      recordStartRequested = false;
      touchActive = false;
      stopRecordingUi();
      uni.showToast({ title: '录音启动失败', icon: 'none' });
    }
  };

  const onVoiceMove = (e: any) => {
    if (!isRecording.value) return;
    const touch = e.touches && e.touches[0];
    if (!touch) return;
    // 手指上滑超过 60px 进入取消状态
    isCancelling.value = recordStartY - touch.clientY > 60;
  };

  const endVoiceRecord = () => {
    if (!isRecording.value) return;

    touchActive = false;
    const elapsed = Date.now() - recordStartAt;
    const cancelled = isCancelling.value;
    const tooShort = elapsed < 800;
    recordCancelled = cancelled;
    shouldSend = !cancelled && !tooShort;

    stopRecordingUi();

    if (cancelled) {
      uni.showToast({ title: '已取消', icon: 'none' });
    } else if (tooShort) {
      uni.showToast({ title: '说话时间太短', icon: 'none' });
    }

    if (recordStartRequested && recorderManager) {
      try {
        recorderManager.stop();
      } catch (e) {
        // 录音尚未真正启动时 stop 可能失败，忽略即可
      }
    }
  };

  const cancelVoiceRecord = () => {
    if (!isRecording.value) return;

    touchActive = false;
    recordCancelled = true;
    shouldSend = false;
    stopRecordingUi();

    if (recordStartRequested && recorderManager) {
      try {
        recorderManager.stop();
      } catch (e) {
        // ignore
      }
    }
    uni.showToast({ title: '已取消', icon: 'none' });
  };

  const playVoice = (id: string, audioPath?: string) => {
    if (!audioPath) {
      uni.showToast({ title: '语音文件已失效', icon: 'none' });
      return;
    }

    setupAudio();
    if (!audioContext) return;

    if (playingId.value === id) {
      audioContext.stop();
      playingId.value = '';
      return;
    }

    if (playingId.value) {
      audioContext.stop();
    }
    audioContext.src = audioPath;
    playingId.value = id;
    audioContext.play();
  };

  onUnmounted(() => {
    if (recordTimer) clearInterval(recordTimer);

    if (recorderManager) {
      try {
        if (isRecording.value) {
          recorderManager.stop();
        }
        recorderManager.offStart && recorderManager.offStart();
        recorderManager.offStop && recorderManager.offStop();
        recorderManager.offError && recorderManager.offError();
      } catch (e) {
        // ignore
      }
    }

    if (audioContext) {
      try {
        audioContext.stop();
        audioContext.destroy();
      } catch (e) {
        // ignore
      }
    }
  });

  return {
    isVoiceMode,
    isRecording,
    isCancelling,
    recordDuration,
    playingId,
    toggleVoiceMode,
    startVoiceRecord,
    onVoiceMove,
    endVoiceRecord,
    cancelVoiceRecord,
    playVoice
  };
}
