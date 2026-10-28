import { request } from './request';

/** 全部业务数据（13 张表） */
export type BootstrapData = Record<string, any>;

/** 一次性拉取全量数据 */
export const fetchBootstrap = (): Promise<BootstrapData> => {
  return request<BootstrapData>({ url: '/bootstrap' });
};

/** 读取整表 */
export const fetchTable = (table: string): Promise<any> => {
  return request({ url: `/tables/${table}` });
};

/** 新增一行 */
export const insertRow = (table: string, row: any, silent = true): Promise<any> => {
  return request({ url: `/tables/${table}`, method: 'POST', data: row, silent });
};

/** 整表覆盖 */
export const replaceTable = (table: string, rows: any[], silent = true): Promise<any> => {
  return request({ url: `/tables/${table}`, method: 'PUT', data: rows, silent });
};

/** 局部更新（小程序不支持 PATCH，故用 POST /{id}） */
export const patchRow = (table: string, id: string, patch: any, silent = true): Promise<any> => {
  return request({ url: `/tables/${table}/${encodeURIComponent(id)}`, method: 'POST', data: patch, silent });
};

/** 删除一行 */
export const deleteRow = (table: string, id: string, silent = true): Promise<any> => {
  return request({ url: `/tables/${table}/${encodeURIComponent(id)}`, method: 'DELETE', silent });
};

/** 清空整表 */
export const clearTable = (table: string, silent = true): Promise<any> => {
  return request({ url: `/tables/${table}`, method: 'DELETE', silent });
};

/** 健康检查 */
export const fetchHealth = (): Promise<any> => {
  return request({ url: '/health', silent: true });
};
