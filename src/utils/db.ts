/**
 * 轻量前端数据层：为 uni Storage 增加「表结构」语义
 *
 * 设计原则：
 * 1. 不改变底层存储方式 —— 仍然使用 uni.getStorageSync / uni.setStorageSync
 * 2. 每个「表」对应一个 Storage key，表内是扁平数组（行）或单条对象（单行表）
 * 3. 提供 find / findOne / insert / update / remove / replaceAll 等类数据库 API
 * 4. 支持整库导出 JSON、导入恢复、单表重置、整库清空
 *
 * 后续接入真实后端时，只需替换各 Store 内调用本模块的位置即可。
 */

export type Row = Record<string, any>;

export interface TableOptions {
  /** Storage key，默认 bbb_table_{name} */
  key?: string;
  /** 单行表：整个表只存一个对象，而非数组（如设置、当前用户） */
  single?: boolean;
}

/** 表的元信息，供导出/调试使用 */
interface TableMeta extends TableOptions {
  name: string;
  key: string;
}

const KEY_PREFIX = 'bbb_table_';
const SCHEMA_VERSION = 1;

/** 表注册表 */
const registry = new Map<string, TableMeta>();

const resolveMeta = (name: string): TableMeta => {
  const existed = registry.get(name);
  if (existed) return existed;
  const meta: TableMeta = { name, key: KEY_PREFIX + name };
  registry.set(name, meta);
  return meta;
};

const readRaw = (key: string): any => {
  try {
    const val = uni.getStorageSync(key);
    return val === '' || val === undefined ? null : val;
  } catch (e) {
    console.error(`[db] read failed: ${key}`, e);
    return null;
  }
};

const writeRaw = (key: string, value: any) => {
  try {
    uni.setStorageSync(key, value);
    return true;
  } catch (e) {
    console.error(`[db] write failed: ${key}`, e);
    uni.showToast({ title: '本地存储写入失败', icon: 'none' });
    return false;
  }
};

const removeRaw = (key: string) => {
  try {
    uni.removeStorageSync(key);
  } catch (e) {
    console.error(`[db] remove failed: ${key}`, e);
  }
};

/* ------------------------------------------------------------------ */
/* 表定义                                                              */
/* ------------------------------------------------------------------ */

export interface DbTable<T extends Row | Row[]> {
  readonly name: string;
  readonly key: string;
  /** 读取整表；若为空可用 seed 初始化 */
  all(seed?: T): T;
  /** 覆盖整表 */
  replaceAll(rows: T): boolean;
  /** 是否已有数据 */
  exists(): boolean;
  /** 清空（恢复为默认空态） */
  clear(): void;
  /** 数组表：按条件筛选 */
  find(predicate?: (row: any, index: number) => boolean): any[];
  /** 数组表：按 id 查找单行 */
  findById(id: string, idField?: string): any | null;
  /** 数组表：插入（默认插到头部） */
  insert(row: any, position?: 'head' | 'tail'): any;
  /** 数组表：按 id 更新，返回是否命中 */
  update(id: string, patch: Row, idField?: string): boolean;
  /** 数组表：按 id 或其他条件删除 */
  remove(idOrPredicate: string | ((row: any) => boolean), idField?: string): number;
}

/**
 * 定义一张表
 * - 默认数组表：存 Row[]
 * - single: true 时存单条对象（用于设置、当前用户等单行表）
 */
export const defineTable = <T extends Row | Row[]>(name: string, options: TableOptions = {}): DbTable<T> => {
  const meta = resolveMeta(name);
  if (options.key) meta.key = options.key;
  if (options.single !== undefined) meta.single = options.single;

  const emptyValue = (): any => (meta.single ? null : []);

  const read = (): any => readRaw(meta.key);

  const table: DbTable<T> = {
    name,
    key: meta.key,

    all(seed?: T): T {
      const cached = read();
      if (cached !== null && cached !== '') {
        return cached as T;
      }
      if (seed !== undefined) {
        writeRaw(meta.key, seed);
        return seed;
      }
      return emptyValue() as T;
    },

    replaceAll(rows: T) {
      return writeRaw(meta.key, rows);
    },

    exists() {
      const cached = read();
      if (meta.single) return cached !== null && typeof cached === 'object';
      return Array.isArray(cached) && cached.length > 0;
    },

    clear() {
      writeRaw(meta.key, emptyValue());
    },

    find(predicate) {
      const rows = (read() || []) as any[];
      if (!Array.isArray(rows)) return [];
      return predicate ? rows.filter(predicate) : rows.slice();
    },

    findById(id: string, idField = 'id') {
      const rows = (read() || []) as any[];
      if (!Array.isArray(rows)) return null;
      return rows.find((row) => row && row[idField] === id) || null;
    },

    insert(row: any, position: 'head' | 'tail' = 'head') {
      const rows = Array.isArray(read()) ? (read() as any[]) : [];
      if (position === 'head') {
        rows.unshift(row);
      } else {
        rows.push(row);
      }
      writeRaw(meta.key, rows);
      return row;
    },

    update(id: string, patch: Row, idField = 'id') {
      const rows = Array.isArray(read()) ? (read() as any[]) : [];
      const idx = rows.findIndex((row) => row && row[idField] === id);
      if (idx === -1) return false;
      rows[idx] = { ...rows[idx], ...patch };
      writeRaw(meta.key, rows);
      return true;
    },

    remove(idOrPredicate, idField = 'id') {
      const rows = Array.isArray(read()) ? (read() as any[]) : [];
      const before = rows.length;
      const next =
        typeof idOrPredicate === 'function'
          ? rows.filter((row) => !idOrPredicate(row))
          : rows.filter((row) => !row || row[idField] !== idOrPredicate);
      if (next.length !== before) {
        writeRaw(meta.key, next);
      }
      return before - next.length;
    }
  };

  return table;
};

/* ------------------------------------------------------------------ */
/* 全局表注册（供导出/重置遍历使用）                                    */
/* ------------------------------------------------------------------ */

export const TABLES = {
  user: defineTable<any>('user', { single: true }),
  setting: defineTable<any>('setting', { single: true }),
  orders: defineTable<any[]>('orders'),
  exchangeOrders: defineTable<any[]>('exchange_orders'),
  contacts: defineTable<any[]>('contacts'),
  sessions: defineTable<any[]>('sessions'),
  messages: defineTable<any>('messages', { single: true }),
  posts: defineTable<any[]>('posts'),
  comments: defineTable<any[]>('comments'),
  addresses: defineTable<any[]>('addresses'),
  helpers: defineTable<any[]>('helpers'),
  goods: defineTable<any[]>('goods'),
  cart: defineTable<any[]>('cart')
} as const;

export type TableName = keyof typeof TABLES;

/** 导出包结构 */
export interface DbDump {
  version: number;
  exportedAt: string;
  tables: Record<string, any>;
}

/* ------------------------------------------------------------------ */
/* 导出 / 导入 / 重置                                                   */
/* ------------------------------------------------------------------ */

/** 导出整库为 JSON 对象 */
export const exportAll = (): DbDump => {
  const tables: Record<string, any> = {};
  Object.values(TABLES).forEach((table: any) => {
    const val = readRaw(table.key);
    if (val !== null && val !== '') {
      tables[table.name] = val;
    }
  });
  return {
    version: SCHEMA_VERSION,
    exportedAt: new Date().toLocaleString(),
    tables
  };
};

/** 导出整库为格式化 JSON 字符串 */
export const exportAllAsString = (pretty = true): string => {
  const dump = exportAll();
  return pretty ? JSON.stringify(dump, null, 2) : JSON.stringify(dump);
};

/**
 * 从导出包恢复整库
 * @param dump 导出对象或其 JSON 字符串
 * @param mode replace = 先清空再写入；merge = 只覆盖包中存在的表
 */
export const importAll = (dump: DbDump | string, mode: 'replace' | 'merge' = 'replace'): boolean => {
  let parsed: DbDump;
  try {
    parsed = typeof dump === 'string' ? JSON.parse(dump) : dump;
  } catch (e) {
    console.error('[db] import parse failed', e);
    return false;
  }
  if (!parsed || typeof parsed !== 'object' || !parsed.tables) return false;

  if (mode === 'replace') {
    Object.values(TABLES).forEach((table: any) => removeRaw(table.key));
  }

  Object.entries(parsed.tables).forEach(([name, value]) => {
    const table = (TABLES as any)[name];
    const key = table ? table.key : KEY_PREFIX + name;
    writeRaw(key, value);
  });
  return true;
};

/** 清空整库（不影响已注册表结构） */
export const clearAll = () => {
  Object.values(TABLES).forEach((table: any) => removeRaw(table.key));
};

/** 导出数据到系统剪贴板，便于粘贴保存 */
export const exportToClipboard = (): Promise<boolean> => {
  return new Promise((resolve) => {
    const text = exportAllAsString();
    uni.setClipboardData({
      data: text,
      success: () => {
        uni.showToast({ title: '数据已复制到剪贴板', icon: 'success' });
        resolve(true);
      },
      fail: (err) => {
        console.error('[db] clipboard failed', err);
        uni.showToast({ title: '复制失败', icon: 'none' });
        resolve(false);
      }
    });
  });
};

/**
 * 导出到本地文件（仅 H5 / App 端可用；小程序端会优雅降级为剪贴板）
 * 小程序没有任意文件写入权限，因此这里调用平台条件编译
 */
export const exportToFile = (): Promise<boolean> => {
  // #ifdef H5
  try {
    const text = exportAllAsString();
    const blob = new Blob([text], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    const stamp = new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-');
    a.href = url;
    a.download = `bangbangbang-db-${stamp}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    uni.showToast({ title: '已导出 JSON 文件', icon: 'success' });
    return Promise.resolve(true);
  } catch (e) {
    console.error('[db] export file failed', e);
    return Promise.resolve(false);
  }
  // #endif

  // #ifndef H5
  // 小程序 / App 端无任意文件写入权限，降级为剪贴板
  return exportToClipboard();
  // #endif
};

/** 统计各表行数与占用体积，供数据管理页展示 */
export interface TableStat {
  name: string;
  label: string;
  count: number;
  size: number;
  single: boolean;
}

const TABLE_LABELS: Record<string, string> = {
  user: '用户信息',
  setting: '适老设置',
  orders: '互助订单',
  exchangeOrders: '兑换订单',
  contacts: '通讯录',
  sessions: '会话列表',
  messages: '聊天消息',
  posts: '广场动态',
  comments: '帖子评论',
  addresses: '收货地址',
  helpers: '志愿者',
  goods: '商城商品',
  cart: '购物车'
};

export const getStats = (): TableStat[] => {
  return Object.values(TABLES).map((table: any) => {
    const val = readRaw(table.key);
    let count = 0;
    if (Array.isArray(val)) {
      count = val.length;
    } else if (val && typeof val === 'object') {
      count = Object.keys(val).length;
    }
    let size = 0;
    try {
      size = val === null ? 0 : JSON.stringify(val).length;
    } catch (e) {
      size = 0;
    }
    return {
      name: table.name,
      label: TABLE_LABELS[table.name] || table.name,
      count,
      size,
      single: Boolean((table as any).single)
    };
  });
};

/**
 * 兼容旧版 Storage key 的一次性迁移
 * 早期版本直接使用 bbb_user / bbb_orders 等 key，这里在启动时搬到新表结构
 */
const LEGACY_KEY_MAP: Record<string, string> = {
  bbb_user: 'user',
  bbb_setting: 'setting',
  bbb_orders: 'orders',
  bbb_exchange_orders: 'exchange_orders',
  bbb_contacts: 'contacts',
  bbb_sessions: 'sessions',
  bbb_messages: 'messages',
  bbb_posts: 'posts',
  bbb_goods: 'goods',
  bbb_cart: 'cart'
};

export const migrateLegacyStorage = () => {
  Object.entries(LEGACY_KEY_MAP).forEach(([legacyKey, tableName]) => {
    const legacyVal = readRaw(legacyKey);
    if (legacyVal === null) return;
    const table = (TABLES as any)[tableName];
    if (!table) return;
    // 新表已有数据则跳过，避免覆盖用户最新数据
    if (readRaw(table.key) === null) {
      writeRaw(table.key, legacyVal);
    }
    removeRaw(legacyKey);
  });
};

/**
 * 递归修正头像扩展名：早期头像为 .png，现统一为 .jpg。
 * 用于修复手机上残留的旧缓存数据（否则头像会空白）。
 */
const replaceAvatarExt = (value: any): any => {
  if (typeof value === 'string') {
    if (value.indexOf('/static/avatars/') === 0 && /\.png$/i.test(value)) {
      return value.replace(/\.png$/i, '.jpg');
    }
    return value;
  }
  if (Array.isArray(value)) {
    return value.map((item) => replaceAvatarExt(item));
  }
  if (value && typeof value === 'object') {
    const out: any = {};
    Object.keys(value).forEach((key) => {
      out[key] = replaceAvatarExt(value[key]);
    });
    return out;
  }
  return value;
};

/** 遍历所有表，修正头像路径扩展名 */
export const migrateAvatarExtensions = () => {
  Object.values(TABLES).forEach((table: any) => {
    const raw = readRaw(table.key);
    if (raw === null) return;
    const fixed = replaceAvatarExt(raw);
    if (JSON.stringify(fixed) !== JSON.stringify(raw)) {
      writeRaw(table.key, fixed);
    }
  });
};

/** 首次启动初始化：迁移旧 key、修正头像路径，确保所有表可用 */
export const initDb = () => {
  migrateLegacyStorage();
  migrateAvatarExtensions();
};

/* ------------------------------------------------------------------ */
/* 统一业务数据读取入口（姓名 / 头像等均从此处获取，避免散落硬编码）      */
/* ------------------------------------------------------------------ */

/**
 * 读取当前登录用户（单行 user 表）。
 * 注意：返回的是 Storage 快照；如需实时内存态，请优先使用 userStore.currentUser。
 */
export const getCurrentUser = (): any | null => {
  return TABLES.user.all();
};

/** 读取当前用户的姓名 */
export const getCurrentUserName = (): string => {
  const user = getCurrentUser();
  return user?.name || '';
};

/** 读取当前用户的头像 */
export const getCurrentUserAvatar = (): string => {
  const user = getCurrentUser();
  return user?.avatar || '';
};

/** 读取当前用户 id */
export const getCurrentUserId = (): string => {
  const user = getCurrentUser();
  return user?.id || '';
};

/** 读取默认帮手（志愿者列表第一个），接单等场景的兜底数据 */
export const getDefaultHelper = (): any | null => {
  const helpers = TABLES.helpers.find();
  return helpers[0] || null;
};

export default {
  table: defineTable,
  TABLES,
  initDb,
  exportAll,
  exportAllAsString,
  importAll,
  clearAll,
  exportToClipboard,
  exportToFile,
  getStats,
  migrateLegacyStorage,
  getCurrentUser,
  getCurrentUserName,
  getCurrentUserAvatar,
  getCurrentUserId,
  getDefaultHelper
};
