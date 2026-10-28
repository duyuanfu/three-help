# =====================================================================
# 帮帮帮后端 · 多阶段构建
#
#   构建：docker build -t bbb-server:1.0.0 .
#   运行：见 docker-compose.yml
#
# 第一阶段产出 fat jar，第二阶段只保留 JRE + jar，
# 最终镜像约 180MB，不含 Maven 与源码。
# =====================================================================

# ---------- 阶段一：编译 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先单独拷 pom 并预热依赖：只要 pom.xml 没改，这一层就命中缓存，
# 后续改代码重新构建时不用再下一遍依赖。
COPY pom.xml .
# 中国大陆服务器拉 Maven 依赖慢时，取消下面这行的注释改用阿里云镜像
# COPY docker/maven-settings.xml /root/.m2/settings.xml
# 预热依赖。个别插件解析不到时不该阻断构建（后面 package 会再拉一次），故允许失败
RUN mvn -B dependency:go-offline || true

COPY src ./src
RUN mvn -B clean package -DskipTests \
    && cp target/bbb-server-*.jar /build/app.jar

# ---------- 阶段二：运行 ----------
FROM eclipse-temurin:17-jre-alpine

# tzdata：容器默认 UTC，没有它 -Duser.timezone=Asia/Shanghai 不生效，
#         上传目录的 yyyyMM 与时间字段会全部偏 8 小时
# uid 固定 1000：以后若把上传目录改成宿主机 bind mount，chown 1000:1000 就能对上
RUN apk add --no-cache tzdata \
    && adduser -D -u 1000 bbb

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    SERVER_PORT=8090 \
    UPLOAD_DIR=/data/uploads \
    JAVA_OPTS="-Xms256m -Xmx512m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

WORKDIR /app
COPY --from=build /build/app.jar /app/app.jar

# 上传目录必须在镜像里先建好并给足属主，
# 否则挂载空数据卷时 Docker 会用 root 建目录，非 root 进程写不进去。
RUN mkdir -p /data/uploads && chown -R bbb:bbb /app /data

USER bbb
EXPOSE 8090

# /api/health 在鉴权白名单里，不需要令牌
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
    CMD wget -qO- "http://127.0.0.1:${SERVER_PORT}/api/health" > /dev/null 2>&1 || exit 1

# 用 sh -c 包一层，JAVA_OPTS 才能在运行时被环境变量覆盖；
# exec 让 java 成为 PID 1，docker stop 时能正常收到 SIGTERM 优雅停机。
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
