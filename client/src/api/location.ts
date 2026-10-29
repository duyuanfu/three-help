import { request } from './request';

export interface ReverseGeoResult {
  city?: string;
  district?: string;
  province?: string;
  address?: string;
}

/**
 * 经纬度反查城市（服务端走腾讯位置服务，key 由后端配置，前端不接触密钥）。
 * 失败时抛错，调用方据此退回「手动选择位置」。
 */
export const apiReverseGeocode = (lat: number, lng: number): Promise<ReverseGeoResult> => {
  return request<ReverseGeoResult>({
    url: `/location/reverse?lat=${lat}&lng=${lng}`,
    silent: true
  });
};
