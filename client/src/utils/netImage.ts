import { reactive } from 'vue';

/**
 * HTTP 网络图片中转。
 *
 * 背景：微信基础库 3.17 起，`<image>` 组件的 src 不再支持 HTTP 协议，
 * 直连会报「图片链接 http://... 不再支持 HTTP 协议，请升级到 HTTPS」。
 * 而 `uni.downloadFile` 仍允许 HTTP，所以这里把 http 图片下载成
 * 本地临时文件（wxfile://tmp_xxx）再交给 image 渲染。
 *
 * 用法：模板里 `:src="netImage(url)"`。函数同时承担两个职责：
 *  1. 命中缓存 → 返回本地路径
 *  2. 未命中 → 入队下载，本次返回空串（调用方用默认图兜底）
 * 缓存是 reactive 的，下载完成后会触发用到它的组件重新渲染。
 */

/** 远程地址 → 本地临时文件路径 */
const localMap = reactive<Record<string, string>>({});

/** 下载失败的地址 → 失败时间戳（毫秒），冷却一段时间后允许重试 */
const failedMap = reactive<Record<string, number>>({});

/** 失败冷却时间：网络抖动导致的一次失败，不该让这张图整个生命周期都不再加载 */
const RETRY_AFTER_MS = 60 * 1000;

const pending = new Set<string>();
const queue: string[] = [];

/** 微信 downloadFile 并发上限是 10，这里保守取 4，避免列表页瞬间打满 */
const MAX_CONCURRENT = 4;
let running = 0;

const pump = () => {
  while (running < MAX_CONCURRENT && queue.length) {
    const url = queue.shift() as string;
    running += 1;
    uni.downloadFile({
      url,
      success: (res: any) => {
        if (res.statusCode === 200 && res.tempFilePath) {
          localMap[url] = res.tempFilePath;
        } else {
          failedMap[url] = Date.now();
        }
      },
      fail: () => {
        failedMap[url] = Date.now();
      },
      complete: () => {
        running -= 1;
        pending.delete(url);
        pump();
      }
    });
  }
};

/**
 * 把图片地址转成当前可渲染的地址。
 * - 空值 / https / 包内路径 / wxfile / 网络图片已下载好 → 直接返回可用地址
 * - http 且未下载 → 触发下载，本次返回空串
 */
export const netImage = (url?: string | null): string => {
  const u = (url || '').trim();
  if (!u) return '';

  // 只有 http 明文需要中转；https、包内静态资源、本地临时文件都能直接渲染
  if (u.indexOf('http://') !== 0) return u;

  const local = localMap[u];
  if (local) return local;

  // 冷却期内的失败地址不再重试，避免渲染一次就发一次请求
  const failedAt = failedMap[u] || 0;
  const cooling = failedAt > 0 && Date.now() - failedAt < RETRY_AFTER_MS;

  if (!cooling && !pending.has(u)) {
    pending.add(u);
    queue.push(u);
    pump();
  }
  return '';
};

/** 带兜底的版本：下载中或失败时返回 fallback */
export const netImageOr = (url: string | null | undefined, fallback: string): string => {
  return netImage(url) || fallback;
};

/** 是否处于失败冷却中（调试用） */
export const isImageFailed = (url?: string | null): boolean => {
  const u = (url || '').trim();
  const failedAt = failedMap[u] || 0;
  return failedAt > 0 && Date.now() - failedAt < RETRY_AFTER_MS;
};

/** 手动清除失败标记并立即重试（调试用） */
export const retryImage = (url?: string | null) => {
  const u = (url || '').trim();
  if (!u || u.indexOf('http://') !== 0) return;
  delete failedMap[u];
  if (!pending.has(u)) {
    pending.add(u);
    queue.push(u);
    pump();
  }
};
