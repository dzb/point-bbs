# point 配置与部署说明

本文面向单机部署和常规生产部署，覆盖配置分层、启动脚本、数据库、反向代理、云形态与升级回滚。

- 单机快速启动：`./point.sh build --frontend && ./point.sh start`
- 配置文件：`point-boot/src/main/resources/application.json`，按 profile 覆盖
- 运行入口：单机 `PointApp`，云形态 `PointCloudApp`

---

## 1. 环境要求

| 组件 | 要求 | 说明 |
|---|---|---|
| JDK | 25+ | 后端编译和运行都需要 |
| Maven | 3.9+ | 构建后端 fat JAR |
| Node.js | 22+ | 仅构建前端时需要 |
| curl / wget | 可选 | `point.sh` 用它做启动健康检查 |

后端是一个 shaded fat JAR，内置 HTTP 引擎和 H2 驱动；单机部署不需要外部 Web 服务器或数据库。

---

## 2. 快速启动（单机）

```bash
# 首次：构建前端 + 后端
./point.sh build --frontend

# 后台启动（默认 dev profile，8082 端口）
./point.sh start

# 查看状态 / 日志
./point.sh status
./point.sh logs

# 停止 / 重启
./point.sh stop
./point.sh restart
```

启动后访问 `http://localhost:8082`，健康检查为 `GET /healthz`。

只想快速跑后端、不改前端时：

```bash
./point.sh build
./point.sh start
```

前台运行（方便看日志、Ctrl+C 停止）：

```bash
./point.sh run
```

---

## 3. `point.sh` 用法

```text
./point.sh build [--frontend] [--fast]
./point.sh start [--profile dev|prod] [--port 8082] [--cloud|--monolith] [--jar JAR]
./point.sh cloud [start 选项]   以 cloud 模式启动（等价 start --cloud）
./point.sh run   [同上]
./point.sh restart [--yes] [start 选项]
./point.sh stop | status | logs | help
```

| 变量 | 默认值 | 作用 |
|---|---|---|
| `PROFILE` | `dev` | 配置 profile；`none` 表示不激活 profile |
| `PORT` | `8082` | 覆盖 `F2.http.server.port` |
| `JAVA_OPTS` | `-Xms256m -Xmx512m` | JVM 参数 |
| `JWT_SECRET` | 空 | 覆盖 `bbs.jwt.secret`，prod 必须 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | 空 | 覆盖数据库配置 |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | 空 | 首次启动创建管理员，密码至少 8 位 |
| `UPLOAD_DIR` | 空 | 覆盖 `bbs.upload.dir` |
| `STATIC_SERVICES` | 空 | 覆盖 `F2.cloud.discovery.static.services`（cloud 模式） |
| `MODE` | `monolith` | `monolith` / `cloud` 启动模式 |
| `HOST` | 空 | 覆盖 `F2.http.server.host` |
| `JAR` | 自动找最新 JAR | 指定 `point-boot` JAR |
| `PID_FILE` / `LOG_FILE` | `logs/point.pid` / `logs/point.out` | 进程号 / 标准输出日志 |

脚本会检查 JDK 主版本、复用已运行的进程、启动后等待健康检查，并在失败时输出日志尾部。

---

## 4. 配置分层与 `F2` 别名

配置按以下顺序合并（后者覆盖前者）：

1. `point-boot/src/main/resources/application.json`
2. `application-dev.json` / `application-prod.json`（由 `F2.profile` 选择）
3. 环境变量、JVM 系统属性、命令行参数

配置文件里的框架键使用大写别名 `F2.*`，freeway 会在合并和未知键检查前规范化为 `freeway.*`。两种写法等价，但同一个配置源里不要用两种拼法重复同一个键。

```json
{
  "F2": {
    "http": { "server": { "host": "0.0.0.0", "port": 8082 } },
    "db": {
      "url": "jdbc:h2:file:./data/bbs;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE,KEY,LABEL,TYPE,LEVEL,INDEX",
      "username": "sa",
      "password": ""
    }
  }
}
```

### Profile 行为

| Profile | 数据库 | 演示数据 | JWT 占位密钥 |
|---|---|---|---|
| `dev` | H2 文件 | 会写入演示用户/帖子/评论，密码为 `123456` | 仅警告 |
| `prod` | PostgreSQL（默认） | 跳过 | **启动失败**，必须设置真实密钥 |
| 不激活 | 基础配置 | **跳过**（只写入角色/权限与 `bbs.admin.*` 引导管理员） | 仅警告 |

演示数据只在**显式激活 `dev` profile** 时写入。裸跑 `java -jar`（无 profile）不再创建演示账号——它曾经看起来像正常部署，却在空库上写入 `123456` 账号和演示内容。

**任何对外可访问的部署都必须使用 `prod` profile。** `dev` 数据只用于本地开发。

---

## 5. 配置项

### HTTP

| 键 | 默认值 | 说明 |
|---|---|---|
| `F2.http.server.host` | `0.0.0.0` | 监听地址 |
| `F2.http.server.port` | `8082` | HTTP 端口 |
| `F2.http.cors.allowed-origins` | `*`（base） / 域名（prod） | 前后端不同源时必改 |
| `F2.http.cors.allowed-headers` | `Content-Type, Authorization, X-User-Token` | CORS 允许头 |
| `F2.http.cors.max-age` | `86400` | 预检缓存秒数 |

### 数据库

| 键 | 说明 |
|---|---|
| `F2.db.url` | JDBC URL；H2 文件或 PostgreSQL |
| `F2.db.username` / `F2.db.password` | 数据库账号 |
| `F2.db.pool.max-size` | 连接池上限（base 20，prod 30） |
| `F2.db.pool.min-idle` | 最小空闲连接（prod 5） |

H2 文件默认在 `./data/bbs.mv.db`，随 JAR 工作目录变化。H2 文件同一时间只能被一个进程打开；多实例部署请使用 PostgreSQL。

### 认证与上传

| 键 | 默认值 | 说明 |
|---|---|---|
| `bbs.jwt.secret` | 开发占位值 | HMAC 密钥，生产至少 32 位随机串 |
| `bbs.jwt.expire-days` | `7`（dev 30） | 会话有效天数 |
| `bbs.jwt.cookie-domain` | 空 | 需要跨子域共享会话时设置 |
| `bbs.jwt.cookie-secure` | 空（按 profile 推导） | 会话 Cookie 是否带 `Secure`；未设置时 `prod` 为 true、`dev` 为 false |
| `bbs.upload.dir` | `./uploads` | 上传文件目录 |
| `bbs.upload.max-size` | `10485760` | 单文件上限（字节，10 MB） |

会话只从 Cookie 或 `Authorization: Bearer` 读取；`?token=` 查询参数已不再接受（URL 里的凭据会进入访问日志、Referer 和浏览器历史）。

### 密钥来源

不要把密钥放在 `-D` 启动参数里：`ps` 对所有本机用户可见。freeway 会加载 `freeway.config.file`（环境变量 `FREEWAY_CONFIG_FILE`）指向的文件，并把它排在配置文件的最高优先级。

`point.sh` 已按此处理：它把 `JWT_SECRET`、`DB_*`、`ADMIN_*`、`UPLOAD_DIR`、`STATIC_SERVICES` 写入 `logs/point.runtime.json`（权限 0600，`stop` 时删除），再通过 `FREEWAY_CONFIG_FILE` 传给进程。想自己管理这份文件时：

```bash
install -m 600 /dev/null /etc/point/secrets.json
# 写入 "bbs.jwt.secret"、"freeway.db.password" 等键（点号分隔或嵌套均可）
POINT_CONFIG=/etc/point/secrets.json ./point.sh start --profile prod
```

systemd 的 `EnvironmentFile=` 仍然可用：变量名不变，脚本会把它们转进运行时配置文件。

生成密钥：

```bash
openssl rand -base64 48
```

### 管理员初始化

| 键 | 说明 |
|---|---|
| `bbs.admin.username` | 首次启动创建的管理员用户名 |
| `bbs.admin.password` | 管理员密码，至少 8 位 |

只有当系统里还没有任何管理员、且该用户名不存在时才会创建；重复启动不会重置密码。创建后建议删除配置中的明文密码。

### GitHub OAuth（可选）

| 键 | 说明 |
|---|---|
| `bbs.oauth.github.client-id` | GitHub OAuth App Client ID |
| `bbs.oauth.github.client-secret` | Client Secret |
| `bbs.oauth.github.redirect-uri` | 回调地址，例如 `https://forum.example.com/api/auth/github/callback` |

---

## 6. 单机 H2 部署

默认 `application.json` 已配置 H2 文件数据库，适合单机、小流量或内网部署。

```bash
# 本地开发（会写入演示数据）
./point.sh start --profile dev

# 单机但使用 prod 行为：不写演示数据，JWT 必须真实
DB_URL='jdbc:h2:file:./data/bbs;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE,KEY,LABEL,TYPE,LEVEL,INDEX' \
DB_USERNAME=sa \
DB_PASSWORD='' \
JWT_SECRET="$(openssl rand -base64 48)" \
ADMIN_USERNAME=admin \
ADMIN_PASSWORD='ChangeMe-Str0ng!' \
./point.sh start --profile prod
```

注意：

- 同一个 `data/bbs.mv.db` 只能被一个 point 进程打开。
- 备份时先 `./point.sh stop`，再复制 `data/` 和 `uploads/`。
- 不建议把 H2 文件放到网络盘。

---

## 7. PostgreSQL 生产部署

```bash
DB_URL='jdbc:postgresql://127.0.0.1:5432/bbs' \
DB_USERNAME='point' \
DB_PASSWORD='数据库密码' \
JWT_SECRET="$(openssl rand -base64 48)" \
ADMIN_USERNAME=admin \
ADMIN_PASSWORD='ChangeMe-Str0ng!' \
./point.sh start --profile prod
```

启动时自动执行 schema 迁移（`Schema.ensure`，幂等）。生产建议：

- 数据库只允许应用主机访问，使用独立账号和强密码。
- 连接池 `max-size` 按数据库上限调整。
- 定期 `pg_dump`，并单独备份 `uploads/`。

---

## 8. 前端构建与静态资源

前端产物通过 `tools/sync-frontend.sh` 同步到 `point-boot/src/main/resources/static/`，再打进 JAR。

```bash
cd point-frontend
npm install        # 首次
cd ..
./point.sh build --frontend
./point.sh restart
```

只改后端时：`./point.sh build`。

只改前端、想复用现有 JAR 时：仍然需要重新打包，因为静态资源在 JAR 内。

---

## 9. 反向代理与 HTTPS

推荐让 Nginx/Caddy 终止 TLS，point 只监听 `127.0.0.1:8082`。Nginx 示例：

```nginx
server {
    listen 443 ssl http2;
    server_name forum.example.com;

    ssl_certificate     /etc/letsencrypt/live/forum.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/forum.example.com/privkey.pem;

    client_max_body_size 10m;

    location /ws/ {
        proxy_pass http://127.0.0.1:8082;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
    }

    location / {
        proxy_pass http://127.0.0.1:8082;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

要点：

- `/ws/` 必须转发 Upgrade 头，否则 WebSocket 通知不可用。
- `client_max_body_size` 不要小于 `bbs.upload.max-size`。
- 同域部署时 `bbs.jwt.cookie-domain` 留空即可；跨子域时再设置。
- 前后端分域部署时，修改 `F2.http.cors.allowed-origins`，并确认 Cookie 策略。

---

## 10. 云形态（静态发现）

`PointCloudApp` 在单机模块之上加入 `CloudModule` 和框架 `StaticModule`，对外提供 `/rpc/auth/validateToken`。cloud 模式只能显式指定启动，默认始终是单机。

```bash
STATIC_SERVICES='point=http://point.default.svc.cluster.local:8080' \
./point.sh cloud --profile prod --port 8080

# 等价写法
STATIC_SERVICES='point=http://point.default.svc.cluster.local:8080' \
./point.sh start --cloud --profile prod --port 8080
```

`F2.cloud.discovery.static.services` 的格式：

```text
serviceId=scheme://host:port[/basePath]
```

多个实例用逗号分隔，重复 service id 表示同一服务的多个实例。静态发现没有注册、心跳、驱逐或热更新，成员关系由平台 Service/DNS 或外部 VIP 负责。

cloud 模式改端口时，`F2.cloud.discovery.static.services` 里的端口要同步一致，或用 `STATIC_SERVICES` 覆盖。

Kubernetes 中通常使用：

```text
point=http://point.default.svc.cluster.local:8080
```

云形态健康检查：

- `GET /health/live`：进程存活
- `GET /health/ready`：依赖就绪。返回 JSON，`cloud.point-db.healthy` 是数据库检查（`CloudHealthModule` 贡献的 `SELECT 1`）；数据库不可达时整个探针返回 503

`point.sh` 在 cloud 模式等待 `/health/ready`（而不是 `/health/live`），因此进程起来但数据库不可用不会报"启动完成"。单机模式仍使用 `/healthz`。

需要动态成员时，改用 `MemberModule` + freeway-ext 的 Fleet/Consul 适配器，替换静态发现配置。

---

## 11. 运维

| 操作 | 命令 |
|---|---|
| 启动 | `./point.sh start` |
| 前台运行 | `./point.sh run` |
| 停止 | `./point.sh stop` |
| 重启 | `./point.sh restart` |
| 状态 | `./point.sh status`（从运行进程自动识别模式和端口，选择 `/healthz` 或 `/health/ready`） |
| 应用输出 | `./point.sh logs`（`logs/point.out`） |
| JUL 文件日志 | `logs/point.0.log`（50 MB 滚动，保留 30 个） |
| 健康检查 | `curl http://127.0.0.1:8082/healthz` |
| API 冒烟 | `bash tools/smoke-api.sh http://127.0.0.1:8082` |
| cloud 模式启动 | `./point.sh cloud --profile dev` |

### 重启

```bash
./point.sh restart
./point.sh restart --profile prod --port 8082
./point.sh restart --cloud
./point.sh restart --cloud --yes
./point.sh restart --monolith
```

`restart` 等价于先 `stop` 再 `start`：

- 先发送 SIGTERM 优雅停止，20 秒内未退出则强制结束；
- 再按本次命令带的参数和环境变量启动，并等待健康检查通过；
- 进程未运行时，`restart` 直接启动；
- `restart` 默认按单机模式重启；cloud 模式必须再次显式传 `--cloud`（或用 `./point.sh cloud` 启动）。如果当前运行的是 cloud 而本次未指定 `--cloud`，脚本会提示并按单机模式重启。
- 如果当前运行的是单机、执行 `restart --cloud`，会先询问确认；回答 `y` 才切换，`n` 或直接回车会取消并保持当前进程。非交互环境必须加 `--yes`，否则脚本中止，不会自动切换。

`restart` 不继承上一次的模式、端口、profile 或环境变量。prod/cloud 部署请把 `JWT_SECRET`、`DB_URL`、`--profile`、`--cloud` 等再次传入，或使用 systemd 的 `EnvironmentFile` + `systemctl restart point`。

systemd 示例（生产建议用 `run` 前台模式）：

```ini
[Unit]
Description=point forum
After=network.target postgresql.service

[Service]
User=point
WorkingDirectory=/opt/point
Environment=JWT_SECRET=CHANGE_ME_RANDOM_SECRET
Environment=DB_URL=jdbc:postgresql://127.0.0.1:5432/bbs
Environment=DB_USERNAME=point
Environment=DB_PASSWORD=CHANGE_ME_DB_PASSWORD
ExecStart=/opt/point/point.sh run --profile prod
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

---

## 12. 升级与回滚

1. 备份数据库和 `uploads/`。
2. 构建新 JAR：`./point.sh build --frontend`。
3. `./point.sh restart`。
4. 运行 `bash tools/smoke-api.sh` 验证。
5. 出问题时换回旧 JAR，`./point.sh restart`。

数据库 schema 由实体自动迁移，升级前仍应备份；回滚旧版本时如果新版本已写入新结构，需要同时恢复数据库备份。

---

## 13. 安全清单

- `bbs.jwt.secret` 使用 32 位以上随机值，生产禁用占位值。
- `bbs.admin.password` 使用强密码，初始化后从配置中移除。
- prod 的 `F2.http.cors.allowed-origins` 不要保留 `*`。
- 使用 HTTPS；在代理层设置 HSTS。
- 限制 `data/`、`uploads/` 的文件权限；不要把 H2 文件或上传目录暴露为静态目录。
- 单机 H2 只运行一个实例；多实例使用 PostgreSQL。
- 定期备份数据库、上传文件和配置中的密钥。

---

## 14. 故障排查

| 现象 | 排查 |
|---|---|
| `需要 JDK 25+` | `java -version`，切换 JDK 25 |
| `未找到 JAR` | `./point.sh build` |
| 端口占用 | `lsof -i :8082`，或 `./point.sh start --port 9000` |
| prod 启动失败提示 JWT | 设置 `JWT_SECRET` 或 `bbs.jwt.secret` |
| H2 数据库被锁 | 确认没有第二个 point 进程；多实例改用 PostgreSQL |
| cloud 启动失败提示 static.services | 设置 `F2.cloud.discovery.static.services`，或使用带该配置的 dev/prod profile |
| 页面是旧版本 | 重新执行 `./point.sh build --frontend` 并重启 |
| WebSocket 不工作 | 检查代理是否转发 `/ws/` 的 Upgrade 头 |
| 401 但刚登录 | 检查 Cookie 域、HTTPS 和反向代理的 Host/Proto 头 |

日志优先级：`logs/point.out`（启动输出）→ `logs/point.0.log`（JUL 应用日志）。
