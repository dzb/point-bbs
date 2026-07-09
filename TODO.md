# TODO — 待接线功能

以下为后端已就绪、前端尚未接线的功能，按优先级和接入成本分类。

## 可接线（低风险）

### 密码修改
- 后端：`POST /users/edit/{id}` 接受 `SetPasswordRequest`（`oldPassword`, `newPassword`），路由已注册
- 前端：需在 `UserProfileEdit.vue` 或独立页面加密码修改表单
- 风险点：密码修改不可逆，需谨慎处理校验和错误反馈
- 建议：在用户资料编辑页加"修改密码"区块，调用同一路由

### GitHub OAuth bind / unbind（已登录用户绑定 GitHub）
- 后端：`POST /auth/github/bind` + `/github/unbind` 已就绪
- 前端：需在用户设置页加"绑定 GitHub 账号"面板，复用 authorize + callback 流程（但 callback 需带上 `code` 而非 token）
- 注意：当前 callback redirect 只处理"登录"（返回 token），bind 需要改造 callback 或在同一页完成

## 需更多规划

### 附件下载
- 后端：`GET /attachment/download/{id}` 返回文件流，`UploadService` 记录下载计数
- 前端：当前上传走 base64 内联图片，**没有附件实体 UI 和消费场景**（消息 extraData 只含 topic/article/user/comment，无 attachment 类型）
- 接入前提：需定义附件在哪些场景出现（文件消息？文章附件？），才能设计 UI

### GitHub OAuth 管理面板
- 完整 OAuth 管理（绑定/解绑状态展示、切换 GitHub 账号）需要专门的面板页
- 当前 LoginPage 已接"用 GitHub 登录"（仅新用户登录/注册），bind/unbind 需另有入口

### `/auth/signout` 前端调用
- 后端：`POST /auth/signout` 返回 200，**无实质行为**（JWT 无状态，不吊销 token）
- 前端：退出登录走 `auth.logout()`（清 localStorage + 跳首页），不请求后端
- 接入前提：需后端实现 token 黑名单 / 会话管理，否则调 /signout 是无操作

## 明确不接

### `/admin/*` 全部（19 条路由）
- 管理员后台需要完整的权限 UI 和操作面板，不属于前端 SPA 的"接线"范畴
- 后端已提供：AdminTopicRoutes（删除/推荐/置顶/搜索/采纳答案）、AdminUserRoutes（列表/详情/更新/禁言）、AdminCategoryRoutes（CRUD）、AdminConfigRoutes（CRUD）

### `/attachment/download/{id}`
- 见"需更多规划"——无前端消费场景时不硬接 UI
