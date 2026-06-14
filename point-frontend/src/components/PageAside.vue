<template>
  <aside class="page-aside">
    <p class="aside-tagline">记录思考，分享见闻</p>
    <div class="aside-card"><div class="aside-card-title">社区公告</div>
      <div class="aside-card-text">欢迎来到 point — 一个安静的思考角落。记录随想，发表文章，关注有趣的人。</div>
    </div>
    <div class="aside-card"><div class="aside-card-title">推荐阅读</div>
      <div class="aside-card-text">暂无推荐</div>
    </div>
  </aside>
</template>

<style scoped>
.page-aside { width: 366px; flex-shrink: 0; padding: 20px 24px; background: var(--paper-nav); transition: width .2s ease, padding .2s ease, opacity .2s ease; }
.aside-tagline { font-size: 14px; color: var(--paper-text2); line-height: 1.8; margin-bottom: 20px; }
.aside-card { border: 1px solid var(--paper-border); border-radius: 6px; padding: 12px; margin-bottom: 12px; }
.aside-card-title { font-size: 16px; color: var(--paper-text); font-weight: 500; margin-bottom: 2px; }
.aside-card-text { font-size: 14px; color: var(--paper-text2); line-height: 1.6; }
@media (max-width: 1300px) { .page-aside { width: 320px; padding: 18px 20px; } }
@media (max-width: 1200px) { .page-aside { width: 280px; padding: 16px 16px; } }
@media (max-width: 1100px) { .page-aside { display: none; } }
</style>
