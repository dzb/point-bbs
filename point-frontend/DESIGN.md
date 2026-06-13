# 黑墨暖纸 / Black Ink · Warm Paper

> 书斋美学 — 松烟墨色为字，朱砂一点为印，宣纸三色可择，夜读墨池相伴。

---

## 宣纸三色 + 夜读

| 角色 | 蝉翼 (冷灰) | 象牙 · 玉白 | 金粟 (暖黄) | 夜读 · 墨池 |
|------|------------|-----------|----------|----------|
| 页底 | `#f5f4f1` | `#fdfaf6` | `#fcf7ee` | `#1e1b18` |
| 卡片 | `#ffffff` | `#ffffff` | `#fffdf8` | `#252220` |
| 主文字 | `#1e1d1a` | `#2c2416` | `#2c2416` | `#e8e0d5` |
| 次文字 | `#7d7a73` | `#8b7e6a` | `#8b7e6a` | `#9b8e7e` |
| 边框 | `#e6e3dd` | `#e8e0d5` | `#ece3d2` | `#3a3530` |
| 气质 | 理性安静 | 典雅均衡 | 温暖怀旧 | 深邃专注 |

切换方式：导航栏调色盘图标 → 四色选一 → 偏好存 localStorage。

实现机制：`<html>` 加 `class="dark"` + CSS 变量 + Vuetify `useTheme()` 全局注入。

---

## 朱砂印记 (四色共用)

```
#c43d3d  朱砂 — 全页只此一红
#a83434  深朱砂 — hover
#8e2626  暗朱砂 — active
```

### 朱砂十触

1. 分类 chip 激活态
2. 标签页下划线
3. 引用块左边框
4. 文中链接
5. 点赞/收藏/关注激活
6. 通知徽章
7. 主按钮背景（注册/发布）
8. 代码行内高亮
9. 登录卡片顶部色条
10. 登录按钮边框圈

---

## 字体

| 角色 | 栈 |
|------|-----|
| 标题 / Logo | `'Noto Serif SC', 'Source Han Serif SC', Georgia, serif` |
| 正文 | 系统默认 sans-serif |

字号：正文 16px / 文章 17px / h2 1.4em / Logo 1.1em

---

## 排版

- 正文行高 `1.8—1.9`
- 文章最大宽 `680px` 居中
- 卡片圆角 `8px`，边框 `1px solid`，无阴影
- 按钮圆角 `6px`，去大写，自然字重

---

## 按钮交互

### 主按钮（注册/发布 — 朱砂实底）

| 状态 | 效果 |
|------|------|
| 默认 | `bg:#c43d3d` 朱砂底白字 |
| 悬停 | `bg:#a83434` 深朱砂 |
| 按下 | `bg:#8e2626` 暗朱砂 + `scale(.97)` 微缩 |
| 聚焦 | `box-shadow: inset 0 0 0 2px rgba(255,255,255,.5), 0 0 0 1px #c43d3d` 向内收敛 |

### 次按钮（登录 — 朱砂边框圈）

| 状态 | 效果 |
|------|------|
| 默认 | 1px 朱砂边框圈 |
| 悬停 | `bg:rgba(196,61,61,.04)` 淡朱砂底 |
| 聚焦 | border 消失 → `box-shadow: inset 0 0 0 2px #c43d3d` 圈向内变粗 1px |

---

## 暗色模式覆盖

```css
html.dark .v-application { background: var(--paper-bg) !important; }
html.dark .v-card        { background: #252220 !important; border-color: #3a3530 !important; }
html.dark .v-list        { background: #252220 !important; }
html.dark .v-field       { color: var(--paper-text) !important; }
html.dark .v-tab--selected { color: #c43d3d !important; }
html.dark .v-pagination__item--active { background: #c43d3d !important; }
html.dark .v-divider     { border-color: #3a3530 !important; }
```

页面内所有 `color`/`border` 使用 `var(--paper-*)` CSS 变量，切主题自动联动。

---

## Vuetify 配置

```ts
colors: {
  primary: '#2c2416', secondary: '#8b7e6a', accent: '#c43d3d',
  error: '#c62828', surface: '#ffffff', background: '#fdfaf6',
}
defaults: {
  VCard: { elevation: 0, style: 'border: 1px solid #e8e0d5; border-radius: 8px;' },
  VBtn: { style: 'text-transform: none; letter-spacing: 0; font-weight: 400;' },
  VTextField: { variant: 'outlined', density: 'comfortable', color: '#c43d3d' },
}
```

---

## CSS 变量体系

```css
:root {
  --paper-bg: #fdfaf6;
  --paper-text: #2c2416;
  --paper-text2: #8b7e6a;
  --paper-border: #e8e0d5;
  --vermilion: #c43d3d;
}
```

四种主题通过 JS 覆写 `--paper-*` 变量值 + `<html>` class + `useTheme()` 三重机制联动。

---

## 文章排版参考

```css
.article-body { font-size: 17px; line-height: 1.9; max-width: 680px; margin: 0 auto; }
.article-body h2 { font-family: 'Noto Serif SC', Georgia, serif; font-size: 1.4em; font-weight: 700; }
.article-body blockquote { border-left: 2px solid #c43d3d; color: var(--paper-text2); background: rgba(196,61,61,.04); }
.article-body a { color: #c43d3d; }
.article-body code { background: #f5f0ea; color: #c43d3d; }
```
