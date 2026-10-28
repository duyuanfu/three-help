import { uploadFile } from '../api/request';
import { resolveAssetUrl } from './token';
import { netImage } from './netImage';

export type AvatarSource = 'album' | 'camera';

/** 服务器上的默认头像（新注册用户、对方账号已注销等情况都用它） */
export const DEFAULT_AVATAR = '/uploads/default/avatar.jpg';

/** 包内默认头像：没有网络、或服务器图加载失败时用它，保证不出现空白方块 */
export const LOCAL_DEFAULT_AVATAR = '/static/common/avatar-default.jpg';

/**
 * 头像地址统一出口（仅做地址补全，不做下载）。
 *
 * 数据库里存的是相对地址（/uploads/avatar/202610/xxx.jpg），
 * 小程序 <image src="/uploads/..."> 会被当成包内资源，加载不到就是一片空白，
 * 所以渲染前必须过一遍 resolveAssetUrl；地址为空时退回包内默认头像。
 */
export const avatarUrl = (url?: string | null): string => {
  const resolved = resolveAssetUrl(url || '');
  return resolved || LOCAL_DEFAULT_AVATAR;
};

/**
 * 渲染专用的头像地址：在 avatarUrl 的基础上再处理 HTTP 图片。
 *
 * 微信基础库 3.17 起 <image> 不再支持 HTTP 直连（会报
 * 「图片链接不再支持 HTTP 协议，请升级到 HTTPS」），所以 http 头像
 * 要先 downloadFile 落到本地临时文件再渲染。下载完成前显示包内默认图，
 * 不会出现空白，也不会闪回原型。
 */
export const avatarSrc = (url?: string | null): string => {
  return netImage(avatarUrl(url)) || LOCAL_DEFAULT_AVATAR;
};

/**
 * 选择头像并上传到服务器。
 *
 * 返回的是服务器上的相对地址（/uploads/avatar/202610/xxx.jpg），
 * 会直接写进 MySQL 的 avatar 字段，而不是小程序本地临时路径。
 */
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
        uni.showLoading({ title: '上传中', mask: true });
        try {
          const result = await uploadFile(tempFilePath, 'avatar');
          uni.hideLoading();
          resolve(result.url);
        } catch (e) {
          uni.hideLoading();
          resolve(null);
        }
      },
      fail: () => resolve(null)
    });
  });
};

/**
 * 选择多张图片并逐张上传到服务器，返回服务器地址数组
 */
export const chooseImages = (count = 3): Promise<string[]> => {
  return new Promise((resolve) => {
    uni.chooseMedia({
      count,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      sizeType: ['compressed'],
      success: async (res) => {
        const files = (res.tempFiles || []).map((f: any) => f.tempFilePath).filter(Boolean);
        if (!files.length) {
          resolve([]);
          return;
        }
        uni.showLoading({ title: '上传中', mask: true });
        const urls: string[] = [];
        for (const file of files) {
          try {
            const result = await uploadFile(file, 'image');
            urls.push(result.url);
          } catch (e) {
            console.error('[upload] 图片上传失败', e);
          }
        }
        uni.hideLoading();
        resolve(urls);
      },
      fail: () => resolve([])
    });
  });
};
