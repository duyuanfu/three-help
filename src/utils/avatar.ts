export type AvatarSource = 'album' | 'camera';

/**
 * 选择头像并把临时文件保存到小程序本地持久目录。
 * 微信的 chooseMedia 返回的是临时路径，重启后可能失效，所以保存后再写入用户资料。
 */
const persistAvatar = (tempFilePath: string): Promise<string> => {
  return new Promise((resolve) => {
    // #ifdef MP-WEIXIN
    uni.saveFile({
      tempFilePath,
      success: (res) => resolve(res.savedFilePath || tempFilePath),
      fail: () => resolve(tempFilePath)
    });
    // #endif

    // #ifndef MP-WEIXIN
    resolve(tempFilePath);
    // #endif
  });
};

export const chooseAvatar = (source: AvatarSource = 'album'): Promise<string | null> => {
  return new Promise((resolve) => {
    uni.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: [source],
      sizeType: ['compressed'],
      success: async (res) => {
        const tempFilePath = res.tempFiles && res.tempFiles[0]
          ? res.tempFiles[0].tempFilePath
          : '';
        if (!tempFilePath) {
          resolve(null);
          return;
        }
        resolve(await persistAvatar(tempFilePath));
      },
      fail: () => resolve(null)
    });
  });
};
