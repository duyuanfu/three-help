import { request } from './request';

/** 可管理的表 */
export interface AdminTable {
  key: string;
  label: string;
  table: string;
  pk: string;
  count: number;
}

/** 字段元信息（服务端从 information_schema 读出） */
export interface AdminColumn {
  name: string;
  label: string;
  type: string;
  nullable: boolean;
  primary: boolean;
  comment: string;
}

/** 一页数据 */
export interface AdminPage {
  key: string;
  label: string;
  pk: string;
  page: number;
  size: number;
  total: number;
  columns: AdminColumn[];
  rows: Record<string, any>[];
}

export const apiAdminTables = () => request<AdminTable[]>({ url: '/admin/tables' });

export const apiAdminPage = (key: string, page = 1, size = 20, keyword = '') =>
  request<AdminPage>({
    url:
      `/admin/tables/${key}?page=${page}&size=${size}` +
      (keyword ? `&keyword=${encodeURIComponent(keyword)}` : '')
  });

/** 更新一条记录（只提交改动过的字段） */
export const apiAdminUpdate = (key: string, id: string, body: Record<string, any>) =>
  request<{ id: string; updated: number }>({
    url: `/admin/tables/${key}/${encodeURIComponent(id)}`,
    method: 'PUT',
    data: body
  });

export const apiAdminDelete = (key: string, id: string) =>
  request<void>({
    url: `/admin/tables/${key}/${encodeURIComponent(id)}`,
    method: 'DELETE'
  });
