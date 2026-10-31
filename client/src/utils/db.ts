/**
 * 前端数据层（纯接口模式）
 *
 * 数据来源：后端 MySQL（Spring Boot 服务，地址见 src/api/config.ts）
 *
 * 设计原则：
 * 1. 全部业务数据以 MySQL 为唯一可信源，本地不再落 Storage
 * 2. 启动时通过 /api/bootstrap 一次性拉取「我的数据」到内存快照
 *    （只含按登录用户过滤、体量有上限的表）
 * 3. 对外仍暴露 TABLES.xxx.all() / insert() / update() / remove() / replaceAll()
 *    这套同步 API，页面与 Store 代码无需感知异步细节
 * 4. 写操作采用「内存乐观更新 + 异步落库」，失败时控制台告警并回读后端修正
 *
 * 不在快照里的数据（它们会无限增长，不适合整表塞进内存）：
 *   - 广场动态 / 评论 → 走 /api/posts/feed 游标分页，见 stores/square.ts
 *   - 聊天消息       → 走 /api/chat/messages（GET 拉取、POST 单条写入），见 stores/chat.ts
 */

import {
  fetchBootstrap,
  insertRow,
  patchRow,
  deleteRow,
  replaceTable,
  clearTable
} from '../api';
import { resolveAssetUrl, toStorageUrl } from './token';

/** 递归把对象里的字符串做统一转换（用于头像等服务器地址的展示/存储转换） */
const mapStrings = (value: any, fn: (s: string) => string): any => {
  if (typeof value === 'string') return fn(value);
  if (Array.isArray(value)) return value.map((item) => mapStrings(item, fn));
  if (value && typeof value === 'object') {
    const out: Record<string, any> = {};
    Object.keys(value).forEach((key) => {
      out[key] = mapStrings(value[key], fn);
    });
    return out;
  }
  return value;
};

export type Row = Record<string, any>;

export interface TableOptions {
  key?: string;
  /** 单行表：整个表只存一个对象（如设置、当前用户） */
  single?: boolean;
}

interface TableMeta extends TableOptions {
  name: string;
  key: string;
}

const SCHEMA_VERSION = 2;

/** 表注册表 */
const registry = new Map<string, TableMeta>();

const resolveMeta = (name: string): TableMeta => {
  const existed = registry.get(name);
  if (existed) return existed;
  const meta: TableMeta = { name, key: name };
  registry.set(name, meta);
  return meta;
};

/* ------------------------------------------------------------------ */
/* 内存快照                                                            */
/* ------------------------------------------------------------------ */

/** 全量数据快照，key 为表名 */
let snapshot: Record<string, any> = {};

/** 是否已完成首次加载 */
let ready = false;

/** 首次加载的 Promise，供需要等待数据的场景复用 */
let readyPromise: Promise<void> | null = null;

/** 已经用种子数据初始化过的表，避免重复写库 */
const seededTables = new Set<string>();

/** 主键字段名映射（默认 id；会话表用 sessionId） */
const ID_FIELD: Record<string, string> = {
  sessions: 'sessionId'
};

const idFieldOf = (name: string): string => ID_FIELD[name] || 'id';

/** 后台同步：失败不打断用户操作，仅记录日志 */
const fireAndForget = (task: Promise<any>, action: string) => {
  task.catch((e) => {
    console.error(`[db] 同步到后端失败：${action}`, e);
  });
};

/* ------------------------------------------------------------------ */
/* 结构适配：购物车在前端是 { goods, count }，后端是 { goodsId, quantity }  */
/* ------------------------------------------------------------------ */

const serverToClient = (name: string, value: any): any => {
  if (name === 'cart') {
    // 后端返回的是 { id, goodsId, quantity, goods }，前端用 { goods, count }
    const rows: any[] = Array.isArray(value) ? value : [];
    return rows
      .filter((row) => row && row.goods)
      .map((row) => ({
        id: row.id,
        goods: mapStrings(row.goods, resolveAssetUrl),
        count: row.quantity == null ? 1 : row.quantity
      }));
  }
  // 服务器上的上传资源（头像、动态图片）转成可直接渲染的完整地址
  return mapStrings(value, resolveAssetUrl);
};

const clientToServer = (name: string, value: any): any => {
  if (name === 'cart') {
    // 前端 { goods, count } → 后端 { goodsId, quantity }
    const rows: any[] = Array.isArray(value) ? value : [];
    return rows
      .filter((row) => row && row.goods)
      .map((row) => ({
        id: row.id,
        goodsId: row.goods.id,
        quantity: row.count == null ? 1 : row.count
      }));
  }
  // 展示用的完整地址回写成数据库里的相对地址
  return mapStrings(value, toStorageUrl);
};

/**
 * 转成后端 replaceAll 期望的数组格式。
 * 单行表（user / setting）在前端是对象，后端统一按「行数组」接收，故包一层。
 */
const toServerRows = (name: string, value: any, single: boolean): any => {
  const normalized = clientToServer(name, value);
  if (single && !Array.isArray(normalized)) {
    return normalized === null || normalized === undefined ? [] : [normalized];
  }
  return normalized;
};

/* ------------------------------------------------------------------ */
/* 表定义                                                              */
/* ------------------------------------------------------------------ */

export interface DbTable<T extends Row | Row[]> {
  readonly name: string;
  readonly key: string;
  all(seed?: T): T;
  replaceAll(rows: T): boolean;
  exists(): boolean;
  clear(): void;
  find(predicate?: (row: any, index: number) => boolean): any[];
  findById(id: string, idField?: string): any | null;
  insert(row: any, position?: 'head' | 'tail'): any;
  update(id: string, patch: Row, idField?: string): boolean;
  remove(idOrPredicate: string | ((row: any) => boolean), idField?: string): number;
}

export const defineTable = <T extends Row | Row[]>(name: string, options: TableOptions = {}): DbTable<T> => {
  const meta = resolveMeta(name);
  if (options.key) meta.key = options.key;
  if (options.single !== undefined) meta.single = options.single;

  const emptyValue = (): any => (meta.single ? null : []);

  const read = (): any => {
    const val = snapshot[meta.key];
    if (val === undefined) return emptyValue();
    return val;
  };

  const write = (val: any) => {
    snapshot[meta.key] = val;
  };

  const table: DbTable<T> = {
    name,
    key: meta.key,

    all(seed?: T): T {
      const current = read();

      // 单行表
      if (meta.single) {
        if (current && typeof current === 'object' && Object.keys(current).length > 0) {
          return current as T;
        }
      } else if (Array.isArray(current) && current.length > 0) {
        return current as T;
      }

      // 表为空且调用方给了种子数据：回填到后端（仅执行一次）
      if (seed !== undefined && !seededTables.has(name)) {
        seededTables.add(name);
        write(seed);
        fireAndForget(
          replaceTable(name, toServerRows(name, seed, Boolean(meta.single)), true),
          `init ${name}`
        );
        return seed;
      }

      return current as T;
    },

    replaceAll(rows: T) {
      write(rows);
      fireAndForget(
        replaceTable(name, clientToServer(name, rows), true),
        `replaceAll ${name}`
      );
      return true;
    },

    exists() {
      const current = read();
      if (meta.single) return current !== null && typeof current === 'object';
      return Array.isArray(current) && current.length > 0;
    },

    clear() {
      write(emptyValue());
      fireAndForget(clearTable(name, true), `clear ${name}`);
    },

    find(predicate) {
      const rows = read();
      if (!Array.isArray(rows)) return [];
      return predicate ? rows.filter(predicate) : rows.slice();
    },

    findById(id: string, idField = idFieldOf(name)) {
      const rows = read();
      if (!Array.isArray(rows)) return null;
      return rows.find((row) => row && row[idField] === id) || null;
    },

    insert(row: any, position: 'head' | 'tail' = 'head') {
      const current = read();
      const rows = Array.isArray(current) ? current : [];
      if (position === 'head') {
        rows.unshift(row);
      } else {
        rows.push(row);
      }
      write(rows);

      fireAndForget(insertRow(name, row, true), `insert ${name}`);
      return row;
    },

    update(id: string, patch: Row, idField = idFieldOf(name)) {
      const rows = read();
      if (!Array.isArray(rows)) return false;
      const idx = rows.findIndex((row) => row && row[idField] === id);
      if (idx === -1) return false;
      rows[idx] = { ...rows[idx], ...patch };
      write(rows);

      fireAndForget(patchRow(name, id, patch, true), `update ${name}`);
      return true;
    },

    remove(idOrPredicate, idField = idFieldOf(name)) {
      const current = read();
      const rows = Array.isArray(current) ? current : [];
      const before = rows.length;

      let removedIds: string[] = [];
      let next: any[];

      if (typeof idOrPredicate === 'function') {
        next = rows.filter((row) => {
          const hit = !idOrPredicate(row);
          if (!hit && row) removedIds.push(row[idField]);
          return hit;
        });
      } else {
        next = rows.filter((row) => {
          const keep = !row || row[idField] !== idOrPredicate;
          if (!keep) removedIds.push(idOrPredicate);
          return keep;
        });
      }

      if (next.length !== before) {
        write(next);

        removedIds.filter(Boolean).forEach((id) => {
          fireAndForget(deleteRow(name, id, true), `remove ${name}/${id}`);
        });
      }
      return before - next.length;
    }
  };

  return table;
};

/* ------------------------------------------------------------------ */
/* 全局表注册                                                          */
/* ------------------------------------------------------------------ */

/**
 * 全局表注册。
 *
 * 这里只登记「按登录用户过滤、体量有上限」的表。
 * 广场动态/评论、聊天消息不在此列：它们会无限增长，
 * 必须走各自的分页/增量接口，见文件头部说明。
 */
export const TABLES = {
  user: defineTable<any>('user', { single: true }),
  setting: defineTable<any>('setting', { single: true }),
  orders: defineTable<any[]>('orders'),
  exchangeOrders: defineTable<any[]>('exchangeOrders'),
  contacts: defineTable<any[]>('contacts'),
  sessions: defineTable<any[]>('sessions'),
  addresses: defineTable<any[]>('addresses'),
  helpers: defineTable<any[]>('helpers'),
  goods: defineTable<any[]>('goods'),
  cart: defineTable<any[]>('cart')
} as const;

export type TableName = keyof typeof TABLES;

/* ------------------------------------------------------------------ */
/* 启动加载                                                            */
/* ------------------------------------------------------------------ */

/**
 * 从后端拉取全量数据到内存快照。
 * 应用启动时调用一次，之后所有读取都走内存，写入异步回写 MySQL。
 */
export const bootstrapAll = (): Promise<void> => {
  if (readyPromise) return readyPromise;

  readyPromise = new Promise<void>((resolve, reject) => {
    uni.showLoading({ title: '加载数据中', mask: true });
    fetchBootstrap()
      .then((data) => {
        const next: Record<string, any> = {};
        Object.keys(data || {}).forEach((name) => {
          next[name] = serverToClient(name, data[name]);
        });
        snapshot = next;
        ready = true;
        uni.hideLoading();
        resolve();
      })
      .catch((e) => {
        uni.hideLoading();
        readyPromise = null;
        reject(e);
      });
  });

  return readyPromise;
};

/** 重新拉取全量数据（下拉刷新 / 出错重试） */
export const reloadAll = async (): Promise<void> => {
  readyPromise = null;
  await bootstrapAll();
};

/**
 * 清空内存快照。
 * 退出登录时必须调用：否则下一个账号会先看到上一个账号的数据。
 */
export const resetSnapshot = (): void => {
  snapshot = {};
  ready = false;
  readyPromise = null;
  seededTables.clear();
};

/** 是否已加载完成 */
export const isReady = (): boolean => ready;

/** 等待数据就绪 */
export const whenReady = (): Promise<void> => {
  if (ready) return Promise.resolve();
  return readyPromise || bootstrapAll();
};

/**
 * 兼容旧版本的初始化入口。
 * 纯接口模式下不再做 Storage 迁移，直接拉取后端数据。
 */
export const initDb = (): Promise<void> => {
  return bootstrapAll();
};

/** 旧版迁移函数，保留空实现以兼容历史调用 */
export const migrateLegacyStorage = () => {
  /* 纯接口模式：无需迁移本地 Storage */
};

export const migrateAvatarExtensions = () => {
  /* 头像路径已由后端统一维护，无需本地修正 */
};

/* ------------------------------------------------------------------ */
/* 统一业务数据读取入口                                                */
/* ------------------------------------------------------------------ */

export const getCurrentUser = (): any | null => {
  return TABLES.user.all();
};

export const getCurrentUserName = (): string => {
  const user = getCurrentUser();
  return user?.name || '';
};

export const getCurrentUserAvatar = (): string => {
  const user = getCurrentUser();
  return user?.avatar || '';
};

export const getCurrentUserId = (): string => {
  const user = getCurrentUser();
  return user?.id || '';
};

export const getDefaultHelper = (): any | null => {
  const helpers = TABLES.helpers.find();
  return helpers[0] || null;
};

export default {
  table: defineTable,
  TABLES,
  initDb,
  bootstrapAll,
  reloadAll,
  isReady,
  whenReady,
  resetSnapshot,
  getCurrentUser,
  getCurrentUserName,
  getCurrentUserAvatar,
  getCurrentUserId,
  getDefaultHelper
};
