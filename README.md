# 帮帮帮 · 适老化社区互助小程序

面向老年人的社区互助平台：老人发布求助（陪诊、代买、陪聊等），志愿者接单，
配套通讯录、聊天、社区广场与积分商城。

## 目录结构

```
three-help/
├── client/          # 前端：uni-app + Vue 3 + TypeScript 微信小程序
│   ├── src/         # 源码
│   ├── dist/        # 构建产物（不入库）
│   └── README.md    # ★ 前端说明：开发、构建、真机调试
│
├── server/          # 后端：Spring Boot 3.3.5 + MyBatis-Plus + MySQL
│   ├── src/         # 源码（含 db/schema.sql、seed.sql、init.sql）
│   ├── uploads/     # 上传的头像 / 图片 / 语音
│   ├── Dockerfile            # 多阶段构建（Maven 编译 + JRE 运行）
│   ├── docker-compose.yml    # 生产部署编排（含可选 MySQL）
│   └── README.md    # ★ 后端说明：数据库初始化、接口清单、Docker 部署
│
├── docs/            # 需求文档与设计稿
├── openspec/        # 规格与变更记录
├── .workbuddy/      # 项目记忆与工作区数据
├── 项目总结.md                    # 早期整体总结（改造前）
└── 功能实现与假数据盘点.md         # 早期假数据盘点（改造前，已全部落地为真实接口）
```

> 两份根级 md 是「前端接后端」改造**之前**的盘点记录，提到的 `src/mock/seeds.ts` 已删除。
> 当前真实实现请以 `client/README.md` 与 `server/README.md` 为准。

## 快速启动

### 1. 数据库

MySQL 监听 3306，执行一次初始化脚本（幂等，可重复跑）：

```bash
mysql -u root -p < server/src/main/resources/db/init.sql
```

### 2. 后端（端口 8090）

```bash
cd server
mvn spring-boot:run
```

Windows 下也可直接双击 `server/start.bat`。

### 3. 前端

```bash
cd client
npm install          # 首次
npx uni build -p mp-weixin
```

微信开发者工具导入 `client/dist/build/mp-weixin`，并在
**详情 → 本地设置** 勾选「不校验合法域名、web-view、TLS 版本以及 HTTPS 证书」。

后端地址在 `client/src/api/config.ts`，默认 `http://192.168.1.7:8090/api`。
真机调试需改成电脑的局域网 IP，且手机与电脑在同一 WiFi。

## 部署到服务器（Docker）

后端已带 `Dockerfile` 与 `docker-compose.yml`，服务器上不需要装 JDK 和 Maven：

```bash
cd server
cp .env.example .env          # 改数据库密码
docker compose up -d --build
```

容器只监听宿主机回环地址（默认 `127.0.0.1:18090`），公网访问统一由服务器上**已有的 Nginx 做 HTTPS 反向代理**，
不占用 443、不与现有 Docker 服务抢端口和容器名。

完整步骤（数据库准备、Nginx 配置、上传文件迁移、前端改 HTTPS 域名、备份与排错）见
**[`server/README.md` 第十一节 · Docker 部署到服务器](server/README.md#十一docker-部署到服务器生产环境)**。

## 演示账号

密码均为 `123456`。

| 姓名 | 手机号 | 身份 |
|---|---|---|
| 张冬梅 | 13859201234 | 退休教师（elder） |
| 李老头 | 13800000001 | 居民（elder） |
| 马玲玲 | 13900000002 | 学生（volunteer） |
| 王秋华 | 13700000003 | 村委会干事（volunteer） |

## 技术要点

- **认证**：手机号 + 密码（PBKDF2-HMAC-SHA256，12 万次迭代），
  服务端会话令牌存 `user_token` 表，30 天有效期，`Authorization: Bearer <token>`
- **数据隔离**：所有身份字段（creatorId / helperId / senderId / authorId / ownerId）
  由令牌决定，前端无法伪造；通用表接口对业务表只读
- **图片**：`POST /api/upload?kind=avatar|image|voice`，落盘到 `server/uploads/`，
  库里存相对地址 `/uploads/...`，前端展示时拼上域名
- **前端数据层**：启动一次性拉 `/api/bootstrap` 到内存快照，
  页面用同步 API 读写，写操作异步落库
