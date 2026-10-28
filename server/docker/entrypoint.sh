#!/bin/sh
# =====================================================================
# 容器启动入口：先等数据库端口可用，再启动 jar
#
# 为什么要等？
#   MySQL 首次启动要几十秒做初始化（建库 + 执行 docker-entrypoint-initdb.d
#   里的 init.sql）。这期间 TCP 3306 根本不监听，Java 一上来就建连接会直接
#   抛 Communications link failure，容器退出、被 restart 策略拉起、再崩，
#   日志里就是好几屏堆栈。等端口就绪再启动，日志干净，首次部署也更稳。
#
# 不需要等的情况：DB_WAIT_TIMEOUT=0 可跳过等待。
# =====================================================================
set -e

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
WAIT_TIMEOUT="${DB_WAIT_TIMEOUT:-120}"

# 等待失败不终止启动：交给 Java 自己报错，
# 再由 compose 的 restart: unless-stopped 重试，诊断信息不会被这里的循环吞掉。
if ! command -v nc >/dev/null 2>&1; then
    # 构建时 apk 装包失败会出现这种情况（入口脚本不因此卡死），
    # 后果只是回到「Java 直接连、连不上崩、restart 拉起重试」的老行为。
    echo "[entrypoint] 容器里没有 nc，跳过数据库等待"
elif [ "$WAIT_TIMEOUT" -gt 0 ] 2>/dev/null; then
    echo "[entrypoint] 等待数据库 ${DB_HOST}:${DB_PORT} 就绪（最多 ${WAIT_TIMEOUT}s，DB_WAIT_TIMEOUT=0 可跳过）..."
    waited=0
    while [ "$waited" -lt "$WAIT_TIMEOUT" ]; do
        if nc -z -w 2 "$DB_HOST" "$DB_PORT" 2>/dev/null; then
            echo "[entrypoint] 数据库已就绪（等待了 ${waited}s）"
            break
        fi
        waited=$((waited + 2))
        sleep 2
    done
    if [ "$waited" -ge "$WAIT_TIMEOUT" ]; then
        echo "[entrypoint] 警告：${WAIT_TIMEOUT}s 内未连上 ${DB_HOST}:${DB_PORT}，"
        echo "[entrypoint]       继续启动，失败后由 restart 策略重试。请检查 DB_HOST / DB_PORT 与 MySQL 是否已启动。"
    fi
fi

# exec：让 java 成为 PID 1，docker stop 时能收到 SIGTERM 做优雅停机
exec java $JAVA_OPTS -jar /app/app.jar
