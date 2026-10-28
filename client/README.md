# 帮帮帮 · 前端小程序（client）

适老化社区互助微信小程序，基于 **uni-app + Vue 3 + TypeScript + Pinia**。

所有业务数据通过 REST 接口读写后端（`../server`），**仓库内不含任何假数据**。

---

## 一、目录结构

```
client/
├── src/                      # 源码
│   ├── api/                  # 接口层
│   │   ├── config.ts         # ★ 后端地址配置（真机调试要改这里）
│   │   ├── request.ts        # 请求封装：带令牌、401 跳登录、资源地址转换
│   │   ├── auth.ts           # 登录 / 注册 / 登出 / 个人资料
│   │   ├── business.ts       # 订单 / 会话 / 联系人 / 广场
│   │   └── admin.ts          # 管理后台：表清单 / 分页 / 更新 / 删除
│   ├── pages/                # 36 个页面（按业务分目录，含 admin/ 管理后台）
│   ├── stores/               # Pinia：user / order / chat / square / mall / setting
│   ├── composables/          # usePullRefresh（下拉刷新）等组合式函数
│   ├── utils/
│   │   ├── db.ts             # 数据层：内存快照 + 异步落库
│   │   ├── session.ts        # 启动 / 登录 / 注册 / 下拉刷新 的数据装载
│   │   ├── token.ts          # 令牌存取、资源 URL 拼接
│   │   ├── avatar.ts         # 选图 + 上传服务器
│   │   └── voice.ts          # 语音录制与播放
│   ├── components/           # 通用组件
│   ├── types/                # TypeScript 类型定义
│   ├── App.vue               # 应用入口（启动装配）
│   └── main.ts
├── dist/                     # 构建产物（不入库）
│   ├── build/mp-weixin/      # 生产包 ← 微信开发者工具导入这个
│   └── dev/mp-weixin/        # 开发包（watch 模式自动生成）
├── scripts/                  # 图片素材处理脚本（Python）
├── assets_original_backup/   # 头像原图备份（不入库）
├── index.html                # H5 入口
├── vite.config.ts
├── tsconfig.json
├── shims-uni.d.ts            # uni-app 类型补充
├── project.config.json       # 微信开发者工具项目配置
└── package.json
```

---

## 二、环境要求

| 项 | 版本 |
|---|---|
| Node.js | 18+（本机 22.22.2） |
| 微信开发者工具 | 稳定版 |
| 后端服务 | 需先启动 `../server`（端口 8090） |

---

## 三、安装依赖

```bash
cd client
npm install
```

---

## 四、配置后端地址

打开 `src/api/config.ts`：

```ts
export const API_BASE_URL = 'http://192.168.1.7:8090/api';
```

- **微信开发者工具模拟器**：用 `127.0.0.1` 或局域网 IP 都行
- **手机真机预览**：必须用电脑的局域网 IP（手机与电脑同一 WiFi），`ipconfig` 可查
- 电脑 IP 变化后要改这里并重新构建

---

## 五、开发与构建

### 开发（watch 模式，改代码自动重编译）

```bash
npm run dev:mp-weixin
```

产物在 `dist/dev/mp-weixin/`，微信开发者工具导入该目录。

### 生产构建

```bash
npx uni build -p mp-weixin
```

产物在 `dist/build/mp-weixin/`。

> **注意**：`npm run build:mp-weixin` 会先删除整个产物目录，如果你的环境对批量删除有安全限制，
> 可能被拦下。直接用上面的 `npx uni build -p mp-weixin` 覆盖构建即可。

### 类型检查

```bash
npx vue-tsc --noEmit
```

---

## 六、微信开发者工具

1. 导入项目，目录选 `client`（`project.config.json` 已指向 `dist/build/mp-weixin/`）
2. **详情 → 本地设置**，勾选：
   - 「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」
3. 改了代码要重新 `npx uni build -p mp-weixin`，然后在工具里点「编译」
4. 若图片（头像）显示空白，先做一次「清缓存 → 全部清除」再编译

---

## 七、数据流说明

页面不直接调接口，统一走 `utils/db.ts` 暴露的同步 API：

```
页面 / store
   ↓  TABLES.orders.all() / insert() / update()
内存快照（启动时由 /api/bootstrap 一次性灌满）
   ↓  写操作 fire-and-forget
后端 REST 接口 → MySQL
```

这样 36 个页面保持同步写法，不用到处 `await`；写入失败只在控制台告警并回读修正。

**登录态相关的两个关键点**：

- 登录 / 注册成功后必须调 `refreshAfterLogin()`（`utils/session.ts`），
  因为小程序的 `App.onLaunch` 只在冷启动执行一次，`reLaunch` 回首页不会重新拉数据
- 退出登录要调 `resetSnapshot()`，否则下一个账号会先看到上一个账号的数据

---

## 八、演示账号

密码均为 `123456`：

| 姓名 | 手机号 | 身份 |
|---|---|---|
| 张冬梅 | 13859201234 | 退休教师（elder，有 4 单求助） |
| 李老头 | 13800000001 | 居民（elder） |
| 马玲玲 | 13900000002 | 学生（volunteer，接了「陪散心」） |
| 王秋华 | 13700000003 | 村委会干事（volunteer，**管理员**） |
| 王奶奶 | 13800000004 | 居民（elder，「陪散心」的发单人） |
| 吴小应 | 13900000005 | 学生（volunteer，接了「医院陪诊」） |
| 刘小燕 | 13900000006 | 学生（volunteer，接了「帮抢鸡蛋」） |
| 蓝贵萍 | 13900000007 | 社区居民（volunteer，接了「教会手机」） |

> 后四位是订单里出现的帮手/发单人，早期只有名字没有账号，
> 现已补成真实账号，否则接单人点「联系发单人」会报「对方账号不存在」。

**管理后台专用账号**：

| 姓名 | 手机号 | 密码 |
|---|---|---|
| 系统管理员 | 13800000000 | `admin123` |

管理员登录后，「我的」页会多出一项「管理后台」；普通用户看不到，直接
访问页面也会被拦下。

## 九、订单里的"对方"规则

同一个订单，不同身份看到的人不一样：

| 我的身份 | 展示与联系的对象 | 按钮文案 |
|---|---|---|
| 发单人（`isMine`） | 接单人 | 联系帮手 |
| 接单人（`isHelper`） | **发单人** | 联系发单人 |

这两个标记由后端按令牌计算，`bootstrap` 与 `/api/orders` 都会返回。
前端禁止对自己发起会话，避免会话列表出现无对方的空白条目。

## 十、管理后台

入口：`pages/admin/index`，只对 `sys_user.is_admin = 1` 的账号开放。

| 能力 | 说明 |
|---|---|
| 查看 | 13 张业务表横向切换，显示记录数；支持关键词模糊搜索（在所有文本列上 LIKE） |
| 编辑 | 点任意记录打开底部弹层，逐字段修改后保存，只提交改动过的字段 |
| 删除 | 弹层内二次确认后删除；删除用户会同时作废其登录令牌 |

安全约束（后端 `AdminService`）：

- 表名只能来自静态白名单，前端传什么都不可能拼出其他表
- 列名取自 `information_schema`，`password_hash` 等隐藏列永远不返回、不可改
- 所有值走 `?` 占位符，表名/列名本身也只在白名单内拼接

原来的「数据管理」页（本地 Storage 的导入/导出/重置）已删除，
业务数据全部只存在 MySQL，前端内存里仅保留一次 bootstrap 的快照。

## 十一、私信与下拉刷新

**私信标点清理**：发送文本时自动去掉 `，。？！`（以及 `, . ? !`）。
前端 `stores/chat.ts` 的 `stripPunctuation` 先清一次，服务端
`ChatService` 再清一次；整条消息只有标点时会被拒发并提示。

**下拉刷新**：五个 tab 页（首页 / 消息 / 订单 / 广场 / 我的）均已开启，
统一走 `composables/usePullRefresh.ts` → `refreshAll()`，
重新拉 bootstrap 快照并同步所有 store，失败会提示「检查后端是否已启动」。

## 十二、未接入的功能

微信登录、支付宝登录、短信验证码、语音 / 视频通话、聊天发送文件 ——
均需额外资质或服务，界面上已给出「暂未接入」提示，不会伪造成功。
