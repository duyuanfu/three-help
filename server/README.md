# 帮帮帮 · 后端服务（bbb-server）

小程序的数据层后端，基于 **Spring Boot 3.3.5 + MyBatis-Plus 3.5.7 + MySQL**。

前端通过 REST 接口读写，**全部业务数据持久化在 MySQL**，本地不再保存业务数据。

---

## 一、环境要求

| 项 | 版本 |
|---|---|
| JDK | 17+（本机已装 17.0.18） |
| Maven | 3.6+（本机已装 3.9.16） |
| MySQL | 5.7 / 8.x，监听 3306 |

> 生产环境用 Docker 部署，见 **第十一节**（不需要在服务器上装 JDK / Maven）。

---

## 二、数据库配置

### 1. 先执行初始化脚本（必须）

`src/main/resources/db/init.sql` 是一个**完整可独立执行**的脚本，包含：

- 创建 `bbb` 库
- 创建 13 张表
- 灌入初始数据
- 创建应用专用账号 `bbb / bbb123456`

执行方式（WSL / Linux）：

```bash
mysql -u root -p < /mnt/f/Workspace/three-help/server/src/main/resources/db/init.sql
```

Windows CMD / PowerShell：

```cmd
mysql -u root -p < F:\Workspace\three-help\server\src\main\resources\db\init.sql
```

脚本全部幂等（`CREATE ... IF NOT EXISTS` + `INSERT IGNORE`），重复执行不会丢数据。

> **为什么要单独建 `bbb` 账号？**
> 如果 MySQL 跑在 WSL / Docker 里，Windows 侧连接 3306 时来源 IP 是 WSL 虚拟网卡地址（如 `172.18.0.1`），
> 而默认的 `root@localhost` 只允许本机回环，会报
> `Access denied for user 'root'@'172.18.0.1'`。
> 脚本里创建的 `'bbb'@'%'` 账号可以从任意来源连接，避开这个坑。

### 2. 修改连接配置

配置在 `src/main/resources/application.yml`，全部写成「环境变量优先、留空用默认值」：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:127.0.0.1}:${DB_PORT:3306}/${DB_NAME:bbb}?createDatabaseIfNotExist=true&...
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root}
app:
  upload:
    dir: ${UPLOAD_DIR:uploads}
```

- 本地直接 `mvn spring-boot:run`：不设环境变量，行为与以前完全一致（`root/root@127.0.0.1:3306/bbb`）
- 服务器 / Docker：不改进代码，用环境变量注入（见第十一节 `.env.example`）
- 端口不是 3306 → 设 `DB_PORT`；想用 root → 设 `DB_USER=root` 并确保 MySQL 里存在 `'root'@'%'`
- 改了 `init.sql` 里的账号密码 → 同步改 `.env`（或环境变量）

---

## 三、启动

```bash
cd server
mvn spring-boot:run
```

或打包后运行：

```bash
mvn clean package -DskipTests
java -jar target/bbb-server-1.0.0.jar
```

启动时 Spring Boot 会**自动执行**：

1. `db/schema.sql` —— 建 13 张表（`CREATE TABLE IF NOT EXISTS`，幂等）
2. `db/seed.sql` —— 灌入初始数据（`INSERT IGNORE`，幂等）

这两份脚本不含 `CREATE DATABASE`，因为连上时库已存在，且应用账号也没有全局建库权限。
手动执行过 `init.sql` 之后，这两步就是空操作，不会覆盖你改过的数据。

验证是否就绪：

```bash
curl http://127.0.0.1:8090/api/health
# {"code":0,"message":"ok","data":{"status":"UP",...}}
```

---

## 四、接口一览

统一前缀 `/api`，统一响应体 `{ code, message, data }`，`code === 0` 表示成功。

### 引导接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/health` | 健康检查 |
| GET | `/api/bootstrap` | **一次性拉取全部 13 张表数据**（前端启动时调用） |

### 通用表接口

前端的数据层语义是「表 → 行」，因此后端把 13 张表统一暴露为一套接口：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/tables` | 列出所有表名 |
| GET | `/api/tables/{table}` | 读取整表（单行表返回对象，其余返回数组） |
| POST | `/api/tables/{table}` | 新增一行（单行表自动降级为 upsert） |
| POST | `/api/tables/{table}/{id}` | 局部更新（同 PATCH；小程序不支持 PATCH） |
| PUT | `/api/tables/{table}` | 整表覆盖 |
| DELETE | `/api/tables/{table}/{id}` | 删除一行 |
| DELETE | `/api/tables/{table}` | 清空整表 |

支持的 `{table}`：

```
user  setting  orders  exchangeOrders  contacts  sessions
messages  posts  comments  addresses  helpers  goods  cart
```

示例：

```bash
# 读订单
curl http://127.0.0.1:8090/api/tables/orders

# 新增一条帖子
curl -X POST http://127.0.0.1:8090/api/tables/posts \
  -H "Content-Type: application/json" \
  -d '{"id":"post_test","authorId":"u_zhangdongmei","authorName":"张冬梅","content":"测试"}'

# 把订单标记为已完成
curl -X POST http://127.0.0.1:8090/api/tables/orders/ord_004 \
  -H "Content-Type: application/json" \
  -d '{"status":"completed","statusText":"已完成服务"}'
```

### 业务接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/register` | 注册（PBKDF2 入库，返回 token） |
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/logout` | 注销令牌 |
| GET | `/api/auth/me` | 当前用户（含 `isAdmin`） |
| POST | `/api/upload?kind=avatar\|image\|voice` | 上传文件，返回 `/uploads/...` |
| GET/POST | `/api/orders` | 订单列表（`scope=mine\|pending`）/ 下单 |
| POST | `/api/orders/{id}/take\|complete\|cancel` | 接单 / 完成 / 取消 |
| GET/POST | `/api/sessions` | 会话列表 / 打开或创建会话 |
| GET/POST | `/api/messages` | 消息列表 / 发送消息 |
| GET/POST/DELETE | `/api/contacts` | 通讯录 |
| GET/POST/DELETE | `/api/posts` | 广场动态 |
| POST | `/api/posts/{id}/like` | 点赞 |
| GET/POST | `/api/posts/{id}/comments` | 评论 |

### 管理后台接口

要求登录账号 `sys_user.is_admin = 1`，否则返回 `需要管理员权限`。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/whoami` | 当前账号是否管理员 |
| GET | `/api/admin/tables` | 13 张表的记录数概览 |
| GET | `/api/admin/tables/{key}?page=&size=&keyword=` | 分页查询，keyword 在所有文本列上 LIKE |
| PUT | `/api/admin/tables/{key}/{id}` | 更新记录（只提交改动字段） |
| DELETE | `/api/admin/tables/{key}/{id}` | 删除记录（删用户会一并作废令牌） |

`{key}` 取值：`users orders exchangeOrders sessions messages contacts posts
comments addresses helpers goods cart setting`

安全边界：表名只能取自静态白名单；列名取自 `information_schema`，
`password_hash` 等隐藏列不返回也不可改；所有值走 `?` 占位符。

---

## 五、数据表说明

| 前端表名 | MySQL 表 | 说明 |
|---|---|---|
| user | `sys_user` | 当前用户（单行） |
| setting | `app_setting` | 适老设置（单行） |
| orders | `help_order` | 互助订单，`helper_*` 平铺为列 |
| exchangeOrders | `exchange_order` | 积分兑换订单，`steps` 存 JSON |
| contacts | `contact` | 通讯录 |
| sessions | `chat_session` | 聊天会话，`target_user_*` 平铺为列 |
| messages | `chat_message` | 聊天消息（**一行一条**，按 session_id + timestamp 索引） |
| posts | `post` | 广场动态，`images` 存 JSON |
| comments | `post_comment` | 帖子评论 |
| addresses | `address` | 收货地址 |
| helpers | `helper` | 志愿者帮手 |
| goods | `goods` | 商城商品 |
| cart | `cart_item` | 购物车（`user_id + goods_id` 唯一） |

设计说明：

- 前端 JSON 里的嵌套结构（`helper`、`review`、`targetUser`）在库中**平铺为独立列**，查询和索引都更方便；
  序列化时由实体上的视图 getter 自动还原成嵌套对象，前端拿到的结构与改造前完全一致
- 确实无固定结构的字段（物流节点、动态图片列表）用 **JSON 文本列**存储
- 每张列表表都有 `sort_no`，数值越小越靠前。新增时由后端自动分配：
  「插到列表头」的表取 `MIN(sort_no) - 1`，「追加到列表尾」的表取 `MAX(sort_no) + 1`

---

## 六、重置数据

想恢复成初始种子数据：

```bash
mysql -u root -p bbb < src/main/resources/db/reset.sql
```

或在代码里执行 `DROP DATABASE bbb;` 后重启服务（会自动重建）。

---

## 八、当前实际生效的配置（2026-10-28 联调验证通过）

| 项 | 值 |
|---|---|
| 后端端口 | `8090`（本机 8080 已被其他项目占用，故改用 8090） |
| 数据库 | WSL 内的 MySQL 8.0.46，Windows 侧经 3306 转发访问 |
| 连接账号 | `root` / `root`（MySQL 侧存在 `root@'%'`，故从 172.18.0.1 可认证） |
| 库名 | `bbb`，共 13 张表，数据与早期种子数据一致 |
| 前端接口地址 | `client/src/api/config.ts` → `http://192.168.1.7:8090/api` |
| 前端工程 | 与后端同级，位于 `../client`（独立目录，自带 package.json 与构建配置） |

> 说明：`db/init.sql` 里额外创建的 `bbb/bbb123456` 账号保留不动，作为备用账号；
> 若改用该账号，只需把 `application.yml` 的 `username/password` 换掉即可。

---

## 九、真实业务流程（2026-10-28 已完成）

早期版本登录、发消息、接单都是前端本地模拟的。现在全部落到后端 + MySQL：

### 1. 账号

| 接口 | 说明 |
|---|---|
| `POST /api/auth/register` | 姓名/年龄/手机号/密码 → 建账号并返回 token |
| `POST /api/auth/login` | 手机号 + 密码 → token + 用户资料 |
| `POST /api/auth/logout` | 令牌作废 |
| `GET /api/auth/me` | 校验当前令牌 |

- 密码用 **PBKDF2-HMAC-SHA256 + 随机盐** 存储（`sys_user.password_hash`），不下发前端
- 令牌随机 32 位，存 `user_token` 表，有效期 30 天
- 除 `login / register / health` 外，`/api/**` 全部需要 `Authorization: Bearer <token>`

演示账号（密码均为 `123456`）：

| 姓名 | 手机号 | 角色 |
|---|---|---|
| 张冬梅 | 13859201234 | 老人（发布求助） |
| 马玲玲 | 13900000002 | 志愿者（@学生） |
| 王秋华 | 13700000003 | 志愿者（@村委会干事） |
| 李老头 | 13800000001 | 老人 |

### 2. 文件上传

| 接口 | 说明 |
|---|---|
| `POST /api/upload?kind=avatar` | multipart，字段名 `file`，返回 `/uploads/avatar/202610/xxx.jpg` |
| `POST /api/upload?kind=image` | 动态图片、聊天图片 |
| `POST /api/upload?kind=voice` | 语音消息 |

- 文件落在 `server/uploads/{avatar|image|voice}/yyyyMM/`，通过 `/uploads/**` 静态访问
- 数据库只存**相对地址**，换 IP / 换域名时不用迁数据；前端展示前用 `API_ORIGIN` 拼完整地址
- 单文件上限 8MB，图片仅允许 jpg/png/gif/webp/bmp

### 3. 业务接口（身份由令牌决定，客户端无法伪造）

| 接口 | 说明 |
|---|---|
| `POST /api/orders` | 下单，creator 取当前登录用户 |
| `GET /api/orders?scope=mine\|pending` | 我的订单 / 待接单大厅 |
| `POST /api/orders/{id}/take` | 接单，helper 取当前登录用户；不能接自己的单 |
| `POST /api/orders/{id}/complete` | 完成，仅订单双方可操作 |
| `POST /api/orders/{id}/cancel` | 取消，仅发布者 |
| `POST /api/sessions` | 与某人建立/获取会话（一对用户只有一条会话） |
| `GET /api/messages?sessionId=` | 拉取消息 |
| `POST /api/messages` | 发消息，sender 取当前登录用户 |
| `POST /api/sessions/{id}/read` | 标记已读 |
| `POST /api/contacts` | 加联系人（可按手机号或 `targetUserId`） |
| `POST /api/posts` | 发动态，author 取当前登录用户 |
| `POST /api/posts/{id}/comments` | 评论 |
| `POST /api/posts/{id}/like` | 点赞 |

### 4. 数据隔离

`/api/bootstrap` 按登录用户返回：订单只含「我发布的 / 我接的 / 待接单」，
通讯录、地址、购物车、兑换单按 `owner_id`（购物车用 `user_id`）过滤，
广场动态与商品是全局公开数据。

### 5. 启动时的自动迁移

`config/DataMigration.java` 每次启动幂等执行：
补 `password_hash`、`user_token` 表、各表的 `owner_id`、会话双方字段；
把 `/static/avatars/xxx.jpg` 的头像地址迁到 `/uploads/default/xxx.jpg`；
规范化历史会话 ID，并清理种子脚本重复插入的旧会话。

### 6. 已知未接入项（不伪造）

微信/支付宝一键登录、短信验证码、语音通话、视频通话、文件发送 —— 界面上直接提示「暂未接入」，
不再用假数据模拟成功。

---

## 十、Docker 部署到服务器（生产环境）

适用于**服务器上已经有别的 Docker 服务、并且已经配好 SSL 证书**的情况。
整套方案的原则是：**不抢端口、不抢容器名、不碰你现有的 Nginx 配置结构，只往里面加一个 server 块。**

### 1. 部署后的完整链路

```
微信小程序
   │  https://api.example.com/api/...
   ▼
宿主机 Nginx（已有，复用现有证书，监听 443）
   │  proxy_pass http://127.0.0.1:18090
   ▼
Docker 容器 bbb-server（只监听宿主机回环地址，公网无法直连）
   │
   ├── MySQL（宿主机 3306 或另一个容器）
   └── 数据卷 bbb-uploads → 容器内 /data/uploads
```

为什么不让容器自己处理 HTTPS：

- 证书续期（certbot / acme.sh）已经绑定在宿主机 Nginx 上，容器里再配一份只会多一处过期点
- 443 端口只能有一个进程监听，容器抢占会直接打挂你现有的站点
- 反代层做 TLS 是标准做法，后端只管 HTTP，日志和排错都简单

### 2. 端口与命名规划（避免和现有服务冲突）

| 项 | 取值 | 说明 |
|---|---|---|
| 容器内端口 | `8090` | 固定不变，代码里的默认值 |
| 宿主机映射端口 | `127.0.0.1:18090` | **只绑回环**，公网访问必须经 Nginx；被占用时在 `.env` 改 `HOST_PORT` |
| compose 项目名 | `bbb` | 固定，不与其他 compose 项目混在一起 |
| 容器名 | `bbb-server`、`bbb-mysql` | 全部带前缀，不会撞名 |
| 网络名 | `bbb-net` | 独立 bridge 网络 |
| 数据卷 | `bbb-uploads`、`bbb-mysql-data` | 重建容器不丢数据 |

> 如果你的 18090 也被占了，改 `.env` 里的 `HOST_PORT` 就行，**Nginx 里的 `proxy_pass` 要同步改**。

### 3. 准备数据库（二选一）

#### 方案 A：服务器已有 MySQL（推荐）

在服务器上执行初始化脚本，它会建库、建 13 张表、灌种子数据，并创建应用账号 `bbb / bbb123456`：

```bash
cd /opt/bbb/server
mysql -u root -p < src/main/resources/db/init.sql
```

确认账号可用（必须能看到 `bbb | %` 这一行）：

```sql
mysql -u root -p -e "SELECT user, host FROM mysql.user WHERE user='bbb';"
```

若 MySQL 装在容器里，用 `docker exec -i <mysql容器名> mysql -uroot -p... < init.sql` 导入。

**`DB_HOST` 到底填什么，取决于服务器上 MySQL 的形态**（这一步最容易踩坑）：

| MySQL 的形态 | `DB_HOST` | 注意事项 |
|---|---|---|
| 宿主机直装（apt / yum / 宝塔） | `host.docker.internal` | 需 `bind-address = 0.0.0.0`（或 `172.17.0.1`）；只绑 `127.0.0.1` 时容器连不上 |
| 另一个 compose 起的 MySQL 容器 | 该**容器名**（如 `mysql`） | 让两个容器在同一网络（见下方），**不要**靠端口映射 |
| 本 compose 的 `bbb-mysql` | `bbb-mysql` | 加 `--profile with-mysql` 启动 |

> 特别注意：MySQL 容器如果写的是 `-p 127.0.0.1:3306:3306`，那么 `host.docker.internal:3306` 是**连不通**的
> —— 容器经 `host-gateway` 访问的是宿主机非回环地址，而端口只绑了回环。
> 这种情况必须走同一 Docker 网络、用容器名直连，别图省事去把端口改成 `0.0.0.0:3306`（等于把数据库暴露到公网）。

把 `bbb-server` 接入已有 MySQL 的网络（在 `docker-compose.yml` 里改）：

```yaml
services:
  bbb-server:
    networks:
      - bbb-net
      - mysql-net          # 你现有 MySQL 所在的网络

networks:
  bbb-net:
    name: bbb-net
    driver: bridge
  mysql-net:
    external: true
    name: mysql_default     # 换成真实名字，docker network ls 查（常见形如 xxx_default）
```

#### 方案 B：服务器还没有 MySQL

**先确认是真的没有**（别凭印象，两条命令十秒钟）：

```bash
ss -lntp | grep 3306                                          # 宿主机有没有进程在听 3306
docker ps --format '{{.Names}}\t{{.Image}}' | grep -i mysql    # 有没有别的 MySQL 容器
```

两条都没输出，才是真的没有 MySQL，走下面这套。
（只要有一条有输出，就说明其实有 MySQL，回到方案 A，别再起一个，白占内存。）

compose 里已经带了 MySQL 8.0 服务，默认不启动（`profiles: ["with-mysql"]`），启用它：

```bash
cd /opt/bbb/server
cp .env.example .env
vim .env
```

`.env` 里这三行必须改对，**缺一个都连不上**：

```ini
DB_HOST=bbb-mysql                      # ← 关键：填容器名，不是 localhost，也不是 host.docker.internal
MYSQL_ROOT_PASSWORD=换成你自己的强密码    # 只给内置 MySQL 的 root 用
DB_PASSWORD=bbb123456                  # 必须和 init.sql 里创建的账号一致
```

> `DB_PASSWORD` 必须和 `src/main/resources/db/init.sql` 末尾那行对上：
> `CREATE USER IF NOT EXISTS 'bbb'@'%' IDENTIFIED BY 'bbb123456';`
> 改了脚本里的密码就要同步改这里，否则日志变成
> `Access denied for user 'bbb'@'172.x.x.x'`——那是「连上了但账号不对」，和现在的 `Connection refused` 不是一回事。

启动：

```bash
docker compose --profile with-mysql up -d --build
```

嫌每次都带 `--profile` 麻烦，可以在 `.env` 里加一行 `COMPOSE_PROFILES=with-mysql`，
之后所有命令自动包含它。用这条验证生效（输出里**必须**有 `bbb-mysql`）：

```bash
docker compose config --services
```

> ⚠️ 不带 profile 时，compose 会**当 bbb-mysql 根本不存在**：
> `docker compose down` 不会停它（它会一直跑着），`logs -f` 也看不到它的日志。
> 所以要么每条命令都带 `--profile with-mysql`，要么就用 `COMPOSE_PROFILES`。

**首次启动的时间线**（全程约 1~3 分钟，中途别 Ctrl+C）：

| 阶段 | 在做什么 |
|---|---|
| 0~40s | MySQL 初始化数据目录，自动执行挂进去的 `init.sql`：建库 `bbb`、建 13 张表、灌种子数据、建账号 `bbb` |
| 40s 起 | `bbb-server` 探测到 3306 可用，启动 Spring，`spring.sql.init` 再跑一遍幂等的建表与迁移 |
| 60~90s | 健康检查通过，`docker compose ps` 显示 `(healthy)` |

镜像里已经带了等待逻辑（`docker/entrypoint.sh`）：MySQL 没就绪时只打印
`[entrypoint] 等待数据库 bbb-mysql:3306 就绪`，**不会再刷一屏 `Communications link failure`**。
最多等 `DB_WAIT_TIMEOUT` 秒（默认 120，`.env` 里可调，填 0 表示不等）。

**验证到底成了没**：

```bash
# 1. 两个容器都在，MySQL 状态是 healthy
docker compose --profile with-mysql ps

# 2. 表建全了（应输出 13）
docker exec bbb-mysql mysql -uroot -p"你的root密码" -N -e \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='bbb';"

# 3. 应用账号能连（不报错即通过）
docker exec bbb-mysql mysql -ubbb -pbbb123456 -e "USE bbb; SELECT COUNT(*) FROM sys_user;"

# 4. 接口通
curl http://127.0.0.1:18090/api/health
```

**如果之前已经起过一次**：`init.sql` **只在数据目录为空时执行一次**。
万一那次是失败的（库建了但表没建全），之后重启不会重放，需要清空卷重来：

```bash
docker compose --profile with-mysql down
docker volume rm bbb-mysql-data        # 只删数据库卷，bbb-uploads 上传目录不受影响
docker compose --profile with-mysql up -d --build
```

> ⚠️ 这会清空数据库里的全部数据，确认里面没有真实数据再做。

**不要给 `bbb-mysql` 加 `ports` 映射**：它在 `bbb-net` 网络里，`bbb-server` 通过容器名直连即可，
3306 不会出现在宿主机端口上，也就没有暴露到公网的风险。要手工操作数据库就
`docker exec -it bbb-mysql mysql -uroot -p`。

### 4. 服务器账号权限（用普通用户部署，免 sudo）

一开始用 `sudo` 部署完全没问题，但会留下两个后遗症：`/opt/three-help` 下的文件归 root 所有
（普通用户改不了 `.env`，`git pull` 还会报 `detected dubious ownership`），
以及每条 docker 命令都得带 `sudo`。一次性理顺，只需要两件事：

**① 把项目目录交给部署账号**

```bash
sudo chown -R dyf:dyf /opt/three-help
```

这一步同时解决三件事：`.env` 可写、`git pull` 不再报 dubious ownership、日志与上传目录可读写。

**② 把账号加入 docker 组**

```bash
sudo usermod -aG docker dyf

# 让新组在当前 shell 立即生效（或直接退出重登 SSH）
newgrp docker
```

**验证**（三条都要通过）：

```bash
id -nG                                     # 输出里应有 docker
docker ps                                  # 不报 permission denied
cd /opt/three-help/server && docker compose ps   # 能看到 bbb-server / bbb-mysql
```

> 之前用 `sudo docker compose` 起的容器**不用重建**。Docker 是同一个 daemon，
> compose 文件里写死了 `name: bbb`，换用户执行 `docker compose ps` 看到的就是同一批容器、
> 同一个 `bbb-mysql-data` 卷。

**`.env` 权限收紧**：里面有数据库密码、地图 key，只给属主读写：

```bash
chmod 600 /opt/three-help/server/.env
```

**安全边界必须知道**：`docker` 组的权限约等于 root——组内用户可以把 `/` 挂进容器再改任何文件。
如果这台服务器还有别人在用，更稳妥的替代方案是不进 docker 组，改用 sudoers 精确放行。

```bash
# /etc/sudoers.d/bbb-deploy
dyf ALL=(root) NOPASSWD: /usr/bin/docker
```

```bash
sudo chmod 440 /etc/sudoers.d/bbb-deploy
sudo visudo -c                                    # 语法自检，必须显示 parsed OK
```

这样 dyf 执行 `sudo docker ...` 不再要密码，而 `sudo` 之外的动作仍受限制、且留有审计记录。
缺点是命令还得带 `sudo`；要彻底免 sudo 就只能用上面的 docker 组。

### 5. 构建与启动

> 以下命令假定以 `root` 或已加入 `docker` 组的用户执行；普通用户请在命令前加 `sudo`，
> 否则会报 `permission denied while trying to connect to the Docker daemon`。

```bash
# 1. 代码放到服务器（示例路径 /opt/bbb）
#    git clone <你的仓库> /opt/bbb   或   scp -r 本地目录 root@服务器:/opt/bbb
cd /opt/bbb/server

# 2. 准备环境变量
cp .env.example .env
vim .env                 # 至少改 DB_PASSWORD，别用示例值

# 3. 构建镜像并后台启动
#    走方案 B（用内置 MySQL）的话，这里要加 --profile with-mysql
docker compose up -d --build

# 4. 看状态与日志
docker compose ps
docker compose logs -f bbb-server
```

镜像构建分两阶段（`Dockerfile`）：第一阶段用 Maven 打出 fat jar，第二阶段只保留 JRE + jar，
最终镜像约 180MB，服务器上不需要装 JDK 和 Maven。

**容器内验证**（启动约需 30~60 秒，`spring.sql.init` 会跑建表与迁移）：

```bash
docker compose ps                       # STATUS 应为 (healthy)
curl http://127.0.0.1:18090/api/health  # {"code":0,...,"status":"UP"}
```

启动顺序是：容器起来 → `docker/entrypoint.sh` 探测 `DB_HOST:DB_PORT` 直到可连接
（MySQL 首次初始化要 30~60s，这段时间日志只有一行「等待数据库 ... 就绪」）→ 拉起 jar。
所以看到日志停在这一行不是卡死，是在等 MySQL。

国内服务器构建卡在拉依赖时可启用阿里云镜像：把 `Dockerfile` 阶段一里这行的注释去掉

```dockerfile
COPY docker/maven-settings.xml /root/.m2/settings.xml
```

同理，镜像第二阶段的 `apk add`（tzdata / netcat-openbsd）默认已把 Alpine 源换成阿里云镜像
（`Dockerfile` 里的 `APK_MIRROR`）。不换的话，连 `dl-cdn.alpinelinux.org` 经常报
`TLS: unspecified error`，community 仓库索引拉不下来时会误报
`netcat-openbsd (no such package)`——看起来像包名错了，其实是网络问题。
特殊网络环境换别的源：

```bash
docker build --build-arg APK_MIRROR=https://mirrors.tuna.tsinghua.edu.cn -t bbb-server:1.0.0 .
```

### 6. 配置 Nginx HTTPS 反向代理

新开一个子域名（推荐 `api.example.com`），**不要**在原来的站点里加路径前缀，
否则前端地址容易出现 `/help/api` 这种双前缀错误。

#### 5.1 宿主机 Nginx（最常见）

先加 DNS 的 A 记录指向服务器 IP。配置文件放哪取决于你 Nginx 的目录结构，先花十秒确认：

```bash
ls /etc/nginx/sites-enabled/ 2>/dev/null   # 有输出 → Debian/Ubuntu 结构，走路径 A
ls /etc/nginx/conf.d/      2>/dev/null     # 有 .conf  → 通用结构，走路径 B
```

**路径 A（Debian/Ubuntu 的 `sites-available` / `sites-enabled`）**

新建 `/etc/nginx/sites-available/bbb`（内容用下面那段 server 配置），然后软链启用：

```bash
sudo vim /etc/nginx/sites-available/bbb
sudo ln -s /etc/nginx/sites-available/bbb /etc/nginx/sites-enabled/bbb
```

> 软链而不是复制：以后 `certbot --nginx` 改证书路径时改的是 sites-available 里的源文件，
> 复制的话两边会不一致。
>
> 证书路径最省事的取法：打开 `sites-available` 里随便一个在用的站点，
> 把它的 `ssl_certificate` / `ssl_certificate_key` 两行**原样抄过来**——
> 已有泛域名证书（`*.example.com`）的话直接就能复用。

**路径 B（`conf.d`）**

新建 `/etc/nginx/conf.d/bbb.conf`，内容相同，不需要软链。

**两种路径通用的 server 配置**（替换域名与证书路径）：

```nginx
server {
    listen 80;
    server_name api.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    # nginx < 1.25.1 请改成：listen 443 ssl http2;
    http2 on;
    server_name api.example.com;

    # 证书：按服务器上实际位置填（见上面的取法）
    ssl_certificate     /etc/letsencrypt/live/example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_session_cache   shared:SSL:10m;

    # ★ 关键：头像/动态图片最大 8MB，Nginx 默认只放行 1MB，不改会 413
    client_max_body_size 12m;

    location / {
        proxy_pass http://127.0.0.1:18090;

        proxy_http_version 1.1;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 上传大图 + 写库，留够时间
        proxy_connect_timeout 10s;
        proxy_read_timeout    60s;
        proxy_send_timeout    60s;
    }

    # 上传的文件是静态资源，让浏览器缓存起来（可选但建议）
    location /uploads/ {
        proxy_pass http://127.0.0.1:18090;
        proxy_set_header Host $host;
        expires 30d;
        add_header Cache-Control "public, max-age=2592000";
    }
}
```

生效：

```bash
sudo nginx -t && sudo systemctl reload nginx
```

验证（在任意外网机器上）：

```bash
curl https://api.example.com/api/health
```

顺便验一下证书链和 TLS 版本（小程序只接受 TLS 1.2+，证书必须由受信任 CA 签发）：

```bash
curl -vI https://api.example.com/api/health 2>&1 | grep -E "SSL|TLS|subject|expire"
```

> 如果你用 certbot 申请新子域名证书：`sudo certbot --nginx -d api.example.com`，
> 它会自动定位到对应的站点文件（sites-available 里的源文件）改证书路径，就不用手填了。

#### 5.2 Nginx 跑在容器里

把 Nginx 容器接入本项目网络，然后 proxy_pass 用**容器名 + 容器内端口**，不能用 `127.0.0.1:18090`：

```yaml
# 在你现有 nginx 的 compose 里加
services:
  nginx:
    networks:
      - default
      - bbb-net

networks:
  bbb-net:
    external: true      # 由本项目的 compose 创建
```

```nginx
location / {
    proxy_pass http://bbb-server:8090;   # 容器内端口，不是 18090
}
```

先 `docker compose up -d` 起一次本项目，`bbb-net` 才会存在，否则外部网络声明会报 not found。

#### 5.3 宝塔 / 1Panel 面板

- 网站 → 添加站点 `api.example.com` → 开启 SSL（面板申请 Let's Encrypt）
- 该站点 → 反向代理 → 目标 URL 填 `http://127.0.0.1:18090`
- 面板的「配置文件」里补一句 `client_max_body_size 12m;`

### 7. 迁移本地已有的上传文件

数据库里只存相对地址（`/uploads/avatar/202610/xxx.jpg`），换服务器不用改数据，
但**文件本身要搬过去**。容器以 uid 1000 运行，直接 `docker cp` 会因属主是 root 而写入失败，
用下面这种方式最稳：

```bash
# 本地：打包
cd /path/to/three-help/server
tar czf uploads.tar.gz uploads/

# 上传
scp uploads.tar.gz root@服务器:/tmp/

# 服务器：解包 → 灌进数据卷 → 修正属主
cd /tmp && tar xzf uploads.tar.gz
docker run --rm \
  -v bbb-uploads:/data \
  -v /tmp/uploads:/src:ro \
  alpine sh -c "mkdir -p /data/uploads && cp -a /src/. /data/uploads/ && chown -R 1000:1000 /data/uploads"
rm -rf /tmp/uploads /tmp/uploads.tar.gz
```

验证：

```bash
curl -I https://api.example.com/uploads/default/avatar.jpg   # 200
docker run --rm -v bbb-uploads:/data alpine ls /data/uploads  # avatar image voice default
```

### 8. 前端配套修改（不做这步，小程序连不上）

#### 7.1 改接口地址

`client/src/api/config.ts`：

```ts
export const API_BASE_URL = 'https://api.example.com/api';
```

**必须满足**：HTTPS、已备案域名、只走 443（小程序不允许自定义端口）、证书由受信任 CA 签发（自签名不行）。

改完重新构建：

```bash
cd /path/to/three-help/client
npx uni build -p mp-weixin
```

#### 7.2 微信公众平台配置服务器域名

「开发管理 → 开发设置 → 服务器域名」，三项都要加 `https://api.example.com`：

| 域名类型 | 为什么需要 |
|---|---|
| request 合法域名 | 所有 `/api/**` 接口 |
| uploadFile 合法域名 | 换头像、发动态图片、语音 |
| downloadFile 合法域名 | `<image>` 加载网络图；本项目 `utils/netImage.ts` 也走它 |

不加的后果：开发者工具勾了「不校验合法域名」时一切正常，**真机上接口全 403、图片全空白**。

> 好消息：切到 HTTPS 后，`utils/netImage.ts` 里的 HTTP 中转会自动失效
> （它只拦 `http://`，https 原样返回），不再依赖 `downloadFile` 兜底，性能和稳定性都更好。

### 9. 日常运维

```bash
# 用内置 MySQL（方案 B）的话，下面每条命令都要带 --profile with-mysql；
# 在 .env 里设了 COMPOSE_PROFILES=with-mysql 就不用了。

# 更新代码后重新部署
cd /opt/bbb && git pull
cd server && docker compose up -d --build     # 只重建有变化的层

# 查看日志（容器日志已限制 10MB×3，不会撑爆磁盘）
docker compose logs -f --tail=200 bbb-server
docker compose logs -f --tail=200 bbb-mysql

# 重启 / 停止
docker compose restart bbb-server
docker compose down            # 停止并删除容器，数据卷保留
docker compose down -v         # ⚠ 连数据卷一起删，图片和数据库都没了

# 进容器排查
docker exec -it bbb-server sh
```

备份（建议加进 crontab，每天一次）：

```bash
# 数据库（MySQL 在宿主机）
mysqldump -u bbb -p'密码' --single-transaction --default-character-set=utf8mb4 bbb \
  | gzip > /backup/bbb-$(date +%F).sql.gz

# 数据库（MySQL 是 bbb-mysql 容器：mysqldump 在容器里，输出重定向在宿主机）
docker exec bbb-mysql mysqldump -u bbb -p'密码' --single-transaction \
  --default-character-set=utf8mb4 bbb \
  | gzip > /backup/bbb-$(date +%F).sql.gz

# 上传文件（命名卷打成一个 tar）
docker run --rm -v bbb-uploads:/data -v /backup:/backup alpine \
  tar czf /backup/uploads-$(date +%F).tar.gz -C /data uploads
```

恢复上传文件：把上面的 `cp -a` 命令反过来做一次（`-v /backup/uploads.tar.gz` 解包到 `/data`）。

### 10. 故障排查

#### 先定位：`Communications link failure`

这个报错只说明「TCP 没连上」，具体是哪一步断的要看 `Caused by` 后面那句：

| `Caused by` | 含义 | 下一步 |
|---|---|---|
| `java.net.ConnectException: Connection refused` | 找到主机了，但 3306 没人应答 | 按下面三步查 |
| `java.net.SocketTimeoutException: connect timed out` | 包被防火墙丢弃（不回 RST） | 查 firewalld / iptables / 云安全组 |
| `java.net.UnknownHostException` | `DB_HOST` 这个名字根本解析不到 | 名字填错，或容器不在同一网络 |

**`Connection refused` 的三步定位**（在服务器上依次执行，把输出贴出来就能定性）：

```bash
# ① 容器实际拿到的数据库配置（确认 .env 有没有生效）
docker exec bbb-server sh -c 'env | grep -E "^(DB_|SERVER_PORT)"'

# ② host.docker.internal 解析成了哪个 IP（无输出 = extra_hosts 没生效）
docker exec bbb-server sh -c 'getent hosts host.docker.internal'

# ③ 3306 到底是谁在听 —— 这一条最关键
ss -lntp | grep 3306        # 机器上没有 ss 就用 netstat -lntp | grep 3306
```

看 ③ 的输出对号入座：

| ③ 的输出 | 病因 | 修复 |
|---|---|---|
| `127.0.0.1:3306` | MySQL 只绑回环，容器经 `host-gateway`（172.17.0.1）访问被拒 | **改 `bind-address`**，见下方 A |
| `0.0.0.0:3306` 或 `*:3306` | 监听没问题，是防火墙在拦 | 放行 docker0，见下方 C |
| 一条都没有 | MySQL 根本没启动 | `systemctl start mysqld`（或起 MySQL 容器），再确认 `systemctl enable mysqld` 已设 |
| 别的容器占用了 3306 映射 | MySQL 是容器且端口只映射给 127.0.0.1 | 走同一网络用容器名，见下方 B |

**A. MySQL 装宿主机，只绑了 127.0.0.1（最常见）**

```bash
# 找出配置写在哪个文件
grep -rn "bind-address\|skip-networking" /etc/mysql/ /etc/my.cnf /etc/my.cnf.d/ 2>/dev/null

# 改成 0.0.0.0（或 docker0 的网关 172.17.0.1），有 skip-networking 就注释掉
# 然后重启
systemctl restart mysqld        # MariaDB 是 mariadb，Ubuntu 24.04+ 是 mysql

# 复查，必须看到 0.0.0.0:3306 才说明改成功
ss -lntp | grep 3306
```

改完务必确认 3306 **没有暴露到公网**：云平台安全组不开 3306，`ufw status` / `firewall-cmd --list-ports` 里也不要有 3306。

**B. MySQL 是另一个 Docker 容器**

不要走端口映射，把两边放进同一个网络用容器名直连（配置写法见第 3 节方案 A 末）。

**C. 防火墙拦了 docker0 → 宿主端口**

```bash
firewall-cmd --permanent --zone=trusted --add-interface=docker0
firewall-cmd --reload
# 或临时验证：firewall-cmd --zone=trusted --add-interface=docker0
```

> 补充：`--profile with-mysql` 模式下 MySQL 首次启动要几十秒才建完库。
> 新版镜像会在启动前先等端口就绪（日志是 `[entrypoint] 等待数据库 bbb-mysql:3306 就绪`），
> 所以正常情况下不会再出现这个错。如果仍然出现，说明等的不是真正的 MySQL 地址——
> 回到上面 ② 确认 `host.docker.internal` 解析到的 IP、③ 确认 3306 到底是谁在听。

#### 其他常见问题

| 现象 | 原因与处理 |
|---|---|
| 日志有 `Communications link failure` | 连不上 MySQL，按上面「先定位」小节处理 |
| 服务器上压根没有 MySQL（`ss -lntp \| grep 3306` 无输出） | 这是 `Connection refused` 最常见的原因：**3306 上没有人**。按方案 B 启用内置 `bbb-mysql` |
| 页面中文乱码（商品名、种子贴子、用户名等，**新发的内容正常**） | 旧版 `init.sql` 没有 `SET NAMES utf8mb4`：MySQL 容器首次初始化时在 LANG=C 环境里跑脚本，中文按 latin1 解析成双重编码乱码；seed 全是 `INSERT IGNORE`，应用启动时用正确编码重放也覆盖不掉。`init.sql` 已修复，**已初始化的库必须清卷重建**：`down` → `docker volume rm bbb-mysql-data` → `up -d --build`（会清空库内全部数据）。验证：`docker exec bbb-mysql mysql -ubbb -p... -e "SELECT name FROM bbb.goods;"` 应显示正常中文 |
| 日志一直停在 `[entrypoint] 等待数据库 ... 就绪` | 在等 MySQL，不是卡死。查 `docker compose --profile with-mysql ps`；若忘了带 `--profile`，MySQL 根本没起，永远不会就绪 |
| `curl 127.0.0.1:18090` refused 或 `Recv failure: Connection reset by peer`，但 `bbb-mysql` 正常 | 成因相同：**应用还没监听 8090**（还在等 MySQL / Spring 启动中）。18090 的端口代理先收下连接、再被上游拒掉，所以表现为 reset。刚 `up` 完的 1~2 分钟内出现属正常，`docker compose logs -f bbb-server` 看到 `Started BangbangbangApplication` 再 curl。超过 3 分钟还不行：`ps` 看 STATUS，`logs --tail=80 bbb-server` 看卡在哪 |
| 日志停在 `等待数据库 host.docker.internal:3306 就绪` | 用内置 MySQL 时 `.env` 里 `DB_HOST` 没改成 `bbb-mysql`——entrypoint 等的地址就是容器拿到的 `DB_HOST`，一眼定性。改完 `docker compose --profile with-mysql up -d` 重建容器（`restart` 不会更新环境变量），`docker exec bbb-server env \| grep DB_HOST` 复核 |
| 改了 `.env` 的 `DB_HOST` 但没生效 | 环境变量在容器创建时注入，`restart` 不会更新。`docker compose --profile with-mysql up -d` 重建容器 |
| `docker compose down` 之后 MySQL 还在跑 | 没带 `--profile`，本次命令的服务列表里没有它。补上再 down，或在 `.env` 里设 `COMPOSE_PROFILES` |
| 日志 `Access denied for user 'bbb'@'172.x.x.x'` | 账号不存在或只允许 localhost。重跑 `init.sql`，确认存在 `'bbb'@'%'`；同时核对 `.env` 的 `DB_PASSWORD` 与脚本里的密码一致 |
| 构建时 `apk` 报 `TLS: unspecified error` 或 `netcat-openbsd (no such package)` | 不是包名错，是连 `dl-cdn.alpinelinux.org` 拉不动索引。Dockerfile 已默认换阿里云镜像（`APK_MIRROR`）；仍失败就 `--build-arg APK_MIRROR` 换清华等其它源 |
| 日志 `Unknown database 'bbb'` | 没执行初始化脚本；`createDatabaseIfNotExist` 需要建库权限，普通账号没有 |
| `bind: address already in use` | 宿主机 18090 被占。`ss -lntp \| grep 18090` 查占用者，改 `.env` 的 `HOST_PORT` |
| Nginx 502 | 容器没起或端口不匹配。先 `curl http://127.0.0.1:18090/api/health`，通了说明是 Nginx 配置问题 |
| 上传头像报 413 | Nginx 的 `client_max_body_size` 没改，默认只有 1MB |
| 接口正常但图片全部 404 | 上传卷没挂或文件没迁。`docker exec bbb-server ls /data/uploads` |
| 图片 403 / 完全加载不出来，接口正常 | 微信「服务器域名」没加 downloadFile 合法域名 |
| 时间差 8 小时、上传目录月份不对 | 容器时区没生效。确认 `TZ=Asia/Shanghai` 且镜像装了 tzdata（Dockerfile 已处理） |
| 健康检查一直 unhealthy，但接口能访问 | `start_period` 不够。首次启动要跑建表和迁移，等 90 秒再看 |

### 11. 上线前安全清单

- [ ] **改掉管理员默认密码**：`u_admin / 13800000000 / admin123` 是仓库里的公开值，必须改
- [ ] **改掉数据库应用账号密码**：`init.sql` 里的 `bbb / bbb123456` 也是公开值。
      改法：先 `ALTER USER 'bbb'@'%' IDENTIFIED BY '新密码';`（或直接改 `init.sql` 再执行），
      然后把 `.env` 里的 `DB_PASSWORD` 同步改掉并 `docker compose up -d`
- [ ] 演示账号（张冬梅、马玲玲等，密码 `123456`）在正式环境应删除或改密
- [ ] `.env` 里不要用示例密码，且 `chmod 600 .env`（已在 `.gitignore` 中，不会误提交）
- [ ] 8090 不对外暴露（本项目已固定绑 `127.0.0.1`），不要改成 `0.0.0.0:8090:8090`
- [ ] MySQL 端口不要对公网开放，只允许本机或 Docker 内网访问
- [ ] 确认 `https://api.example.com/api/health` 从外网可访问，`http://` 会 301 跳到 443
- [ ] 备份任务已加进 crontab，并**实际恢复过一次**（没验证过的备份不算备份）
