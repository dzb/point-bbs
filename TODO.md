# TODO — 待接线功能

以下为后端已就绪、前端尚未接线的功能（2026-11-02 审计后更新）。

## 可接线（低风险）

### GitHub OAuth bind / unbind（已登录用户绑定 GitHub）
- 后端：`POST /api/auth/github/bind` + `/unbind` 已就绪；bind 需要 `code`（query 参数），前端需先走 `GET /api/auth/github/authorize` 再回填。
- 前端：需在用户设置页加"绑定 GitHub 账号"面板；当前 LoginPage 只接了"用 GitHub 登录"（authorize + callback），无绑定入口。

## 需更多规划

### 附件下载
- 后端：`GET /api/attachment/download/{id}` 已就绪（`UploadService` 维护附件与下载计数）。
- 前端：上传走 `POST /api/upload` 的 base64 图片内联，**没有附件实体 UI 和消费场景**（消息 extraData 只含 topic/article/comment/user，无 attachment 类型）。
- 接入前提：需定义附件在哪些场景出现（文件消息？文章附件？），才能设计 UI。

## 已接线（保留记录，勿重复实现）

### GitHub OAuth 登录
- LoginPage 已接 `GET /api/auth/github/authorize` + callback，仅登录/注册（bind 另见上）。

### `/api/auth/signout`
- 后端 `SignoutHandler` 清除 HttpOnly 会话 cookie；前端 `src/stores/auth.ts` 退出时已调用 `POST /api/auth/signout`。

### `/api/admin/*`
- 前端 `AdminPanel.vue` 已接：topic 列表/推荐/置顶/删除、user 列表/禁言/解除、category 增删改查、sys-config 读/写（`/admin` 路由，`requiresAdmin` 守卫）。
- 仍未接：topic 搜索（`GET /admin/topic/search`）、采纳答案（`POST /admin/topic/accept_answer/{id}`）、user 详情（`GET /admin/user/{id}`）、user 更新（`POST /admin/user/update/{id}`）。

### `/api/attachment/download/{id}`
- 同"附件下载"：无前端消费场景时不硬接 UI。

### 密码修改
- 后端：`POST /api/users/password`（body `oldPassword` / `newPassword`，当前用户来自会话，不信任 body 中的用户 id）。Service 层校验旧密码、新密码 ≥ 6 位且不得与原密码相同；改密成功后清除 HttpOnly 会话 cookie（`Set-Cookie: point_token=; Max-Age=0`），要求用新密码重新登录。
- 前端：`UserProfileEdit.vue` 新增"修改密码"区块（当前密码/新密码/确认新密码，前端校验长度与一致性，成功后清会话并跳转登录页）。
