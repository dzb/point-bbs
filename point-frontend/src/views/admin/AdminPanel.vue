<template>
  <div class="detail-main">
    <v-tabs v-model="tab" density="compact" color="var(--paper-accent)" class="mb-4"
      style="border-bottom:1px solid var(--paper-border)">
      <v-tab value="topics" style="font-size:14px;text-transform:none;letter-spacing:0">帖子</v-tab>
      <v-tab value="users" style="font-size:14px;text-transform:none;letter-spacing:0">用户</v-tab>
      <v-tab value="categories" style="font-size:14px;text-transform:none;letter-spacing:0">分类</v-tab>
      <v-tab value="config" style="font-size:14px;text-transform:none;letter-spacing:0">配置</v-tab>
    </v-tabs>

    <!-- ── Topics ── -->
    <div v-if="tab === 'topics'">
      <div v-for="t in topics" :key="t.id" class="admin-row">
        <div class="flex-grow-1" style="min-width:0">
          <div style="font-size:14px;font-weight:500;color:var(--paper-text);text-overflow:ellipsis;overflow:hidden;white-space:nowrap">
            {{ t.title || (t.content || '').slice(0, 40) }}
          </div>
          <div style="font-size:12px;color:var(--paper-text2)">#{{ t.id }} · {{ t.user?.nickname || t.userId }} · {{ fmt(t.createTime) }}</div>
        </div>
        <div class="d-flex" style="gap:6px;flex-shrink:0">
          <v-btn size="x-small" variant="text" style="color:var(--paper-text2)" @click="toggleRecommend(t)">
            {{ t.recommend ? '取消推荐' : '推荐' }}
          </v-btn>
          <v-btn size="x-small" variant="text" style="color:var(--paper-text2)" @click="toggleSticky(t)">
            {{ t.sticky ? '取消置顶' : '置顶' }}
          </v-btn>
          <v-btn size="x-small" variant="text" style="color:var(--paper-accent)" @click="deleteTopic(t)">删除</v-btn>
        </div>
      </div>
      <LoadMore :has-more="topicsHasMore" :loading="topicsLoadingMore" @load-more="loadTopicsMore" />
      <div v-if="!topics.length && !topicsLoading" class="text-center py-12" style="color:var(--paper-text2)">暂无帖子</div>
    </div>

    <!-- ── Users ── -->
    <div v-if="tab === 'users'">
      <div v-for="u in users" :key="u.id" class="admin-row">
        <UserAvatar :src="u.avatar" :name="u.nickname" :size="32" class="mr-3" />
        <div class="flex-grow-1" style="min-width:0">
          <div style="font-size:14px;font-weight:500;color:var(--paper-text)">
            {{ u.nickname }} <span style="font-weight:400;color:var(--paper-text2)">@{{ u.username || u.id }}</span>
          </div>
          <div style="font-size:12px;color:var(--paper-text2)">#{{ u.id }} · 帖子 {{ u.topicCount || 0 }} · 评论 {{ u.commentCount || 0 }}</div>
        </div>
        <div class="d-flex" style="gap:6px;flex-shrink:0">
          <v-btn v-if="u.forbiddenEndTime > Date.now()" size="x-small" variant="text" @click="unforbid(u)">解禁</v-btn>
          <v-btn v-else size="x-small" variant="text" style="color:var(--paper-accent)" @click="forbid(u)">禁言</v-btn>
        </div>
      </div>
      <LoadMore :has-more="usersHasMore" :loading="usersLoadingMore" @load-more="loadUsersMore" />
    </div>

    <!-- ── Categories ── -->
    <div v-if="tab === 'categories'">
      <div class="d-flex mb-3" style="gap:8px">
        <v-text-field v-model="newCat.name" label="名称" density="compact" variant="outlined" hide-details style="max-width:200px" />
        <v-text-field v-model="newCat.description" label="描述" density="compact" variant="outlined" hide-details style="max-width:280px" />
        <v-select v-model="newCat.type" :items="['free', 'qa']" label="类型" density="compact" variant="outlined" hide-details style="max-width:120px" />
        <v-btn variant="flat" size="small" style="background:var(--paper-accent);color:#fff" @click="createCategory">新建</v-btn>
      </div>
      <div v-for="c in categories" :key="c.id" class="admin-row">
        <div class="flex-grow-1" style="min-width:0">
          <div style="font-size:14px;font-weight:500;color:var(--paper-text)">
            {{ c.name }} <v-chip size="x-small" variant="outlined" class="ml-1">{{ c.type }}</v-chip>
          </div>
          <div style="font-size:12px;color:var(--paper-text2)">{{ c.description }}</div>
        </div>
        <div class="d-flex" style="gap:6px;flex-shrink:0">
          <v-btn size="x-small" variant="text" style="color:var(--paper-text2)" @click="editCategory(c)">编辑</v-btn>
          <v-btn size="x-small" variant="text" style="color:var(--paper-accent)" @click="deleteCategory(c)">删除</v-btn>
        </div>
      </div>
      <div v-if="!categories.length" class="text-center py-12" style="color:var(--paper-text2)">暂无分类</div>
    </div>

    <!-- ── Config ── -->
    <div v-if="tab === 'config'">
      <div v-for="(v, k) in configs" :key="k" class="admin-row">
        <div class="flex-grow-1" style="min-width:0">
          <div style="font-size:13px;font-weight:500;color:var(--paper-text)">{{ k }}</div>
          <div style="font-size:12px;color:var(--paper-text2);word-break:break-all">{{ v }}</div>
        </div>
        <v-btn size="x-small" variant="text" style="color:var(--paper-text2);flex-shrink:0" @click="editConfig(k, v)">编辑</v-btn>
      </div>
      <div v-if="!Object.keys(configs).length" class="text-center py-12" style="color:var(--paper-text2)">暂无配置</div>
    </div>

    <!-- category edit dialog -->
    <v-dialog v-model="catDialog" max-width="420">
      <v-card class="pa-4">
        <div style="font-size:15px;font-weight:600;color:var(--paper-text)" class="mb-3">编辑分类</div>
        <v-text-field v-model="catForm.name" label="名称" variant="outlined" density="compact" hide-details class="mb-2" />
        <v-text-field v-model="catForm.description" label="描述" variant="outlined" density="compact" hide-details class="mb-2" />
        <v-text-field v-model.number="catForm.sortNo" label="排序" type="number" variant="outlined" density="compact" hide-details class="mb-3" />
        <div class="d-flex justify-end" style="gap:8px">
          <v-btn size="small" variant="text" @click="catDialog = false">取消</v-btn>
          <v-btn size="small" variant="flat" style="background:var(--paper-accent);color:#fff" @click="saveCategory">保存</v-btn>
        </div>
      </v-card>
    </v-dialog>

    <!-- config edit dialog -->
    <v-dialog v-model="configDialog" max-width="480">
      <v-card class="pa-4">
        <div style="font-size:15px;font-weight:600;color:var(--paper-text)" class="mb-3">编辑配置</div>
        <div style="font-size:13px;color:var(--paper-text2)" class="mb-2">{{ configKey }}</div>
        <v-textarea v-model="configValue" variant="outlined" density="compact" hide-details rows="3" class="mb-3" />
        <div class="d-flex justify-end" style="gap:8px">
          <v-btn size="small" variant="text" @click="configDialog = false">取消</v-btn>
          <v-btn size="small" variant="flat" style="background:var(--paper-accent);color:#fff" @click="saveConfig">保存</v-btn>
        </div>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import client from '@/api/client'
import LoadMore from '@/components/LoadMore.vue'
import UserAvatar from '@/components/UserAvatar.vue'

const tab = ref('topics')

// topics
const topics = ref<any[]>([])
const topicsPage = ref(1)
const topicsLoading = ref(false)  // reassigned below via .value
const topicsLoadingMore = ref(false)
const topicsHasMore = ref(false)

// users
const users = ref<any[]>([])
const usersPage = ref(1)
const usersLoadingMore = ref(false)
const usersHasMore = ref(false)

// categories
const categories = ref<any[]>([])
const newCat = ref({ name: '', description: '', type: 'free' })
const catDialog = ref(false)
const catForm = ref({ id: 0, name: '', description: '', sortNo: 0 })

// config
const configs = ref<Record<string, string>>({})
const configDialog = ref(false)
const configKey = ref('')
const configValue = ref('')

watch(tab, (t) => {
  if (t === 'topics' && !topics.value.length) loadTopics(true)
  else if (t === 'users' && !users.value.length) loadUsers(true)
  else if (t === 'categories' && !categories.value.length) loadCategories()
  else if (t === 'config' && !Object.keys(configs.value).length) loadConfigs()
})

onMounted(() => loadTopics(true))

async function loadTopics(reset = false) {
  if (reset) { topicsPage.value = 1; topicsLoading.value = true }
  try {
    const { data } = await client.get('/admin/topic', { params: { page: topicsPage.value, pageSize: 20 } })
    if (data.code === 0) {
      const payload = data.data || {}
      const items = payload.items || []
      topics.value = reset ? items : [...topics.value, ...items]
      topicsHasMore.value = (payload.total ?? 0) > topicsPage.value * 20
    }
  } catch (e) { console.error('admin topics error', e) }
  topicsLoading.value = false
  topicsLoadingMore.value = false
}
async function loadTopicsMore() { topicsPage.value++; topicsLoadingMore.value = true; await loadTopics() }

async function toggleRecommend(t: any) {
  try { await client.post(`/admin/topic/recommend/${t.id}`, null, { params: { recommend: !t.recommend } }); t.recommend = !t.recommend }
  catch (e) { console.error(e) }
}
async function toggleSticky(t: any) {
  try { await client.post(`/admin/topic/sticky/${t.id}`, null, { params: { sticky: !t.sticky } }); t.sticky = !t.sticky }
  catch (e) { console.error(e) }
}
async function deleteTopic(t: any) {
  if (!confirm(`确定删除帖子 #${t.id}？`)) return
  try { await client.post(`/admin/topic/delete/${t.id}`); topics.value = topics.value.filter(x => x.id !== t.id) }
  catch (e) { console.error(e) }
}

async function loadUsers(reset = false) {
  if (reset) { usersPage.value = 1 }
  try {
    const { data } = await client.get('/admin/user', { params: { page: usersPage.value, pageSize: 20 } })
    if (data.code === 0) {
      const payload = data.data || {}
      const items = payload.items || []
      users.value = reset ? items : [...users.value, ...items]
      usersHasMore.value = (payload.total ?? 0) > usersPage.value * 20
    }
  } catch (e) { console.error('admin users error', e) }
  usersLoadingMore.value = false
}
async function loadUsersMore() { usersPage.value++; usersLoadingMore.value = true; await loadUsers() }

async function forbid(u: any) {
  if (!confirm(`确定禁言 ${u.nickname}（一年）？`)) return
  try { await client.post(`/admin/user/forbidden/${u.id}`); u.forbiddenEndTime = Date.now() + 365 * 86400 * 1000 }
  catch (e) { console.error(e) }
}
async function unforbid(u: any) {
  try { await client.post(`/admin/user/unforbidden/${u.id}`); u.forbiddenEndTime = 0 }
  catch (e) { console.error(e) }
}

async function loadCategories() {
  try {
    const { data } = await client.get('/admin/category')
    if (data.code === 0) categories.value = flatten(data.data || [])
  } catch (e) { console.error('admin categories error', e) }
}
function flatten(tree: any[]): any[] {
  const out: any[] = []
  for (const node of tree || []) {
    out.push({ id: node.id, name: node.name, type: node.type, description: node.description })
    out.push(...flatten(node.children || []))
  }
  return out
}
async function createCategory() {
  if (!newCat.value.name.trim()) return
  try {
    await client.post('/admin/category', {
      name: newCat.value.name.trim(),
      type: newCat.value.type,
      description: newCat.value.description,
      sortNo: 0,
    })
    newCat.value = { name: '', description: '', type: 'free' }
    await loadCategories()
  } catch (e) { console.error(e) }
}
function editCategory(c: any) {
  catForm.value = { id: c.id, name: c.name, description: c.description, sortNo: 0 }
  catDialog.value = true
}
async function saveCategory() {
  try {
    await client.post(`/admin/category/${catForm.value.id}`, { name: catForm.value.name, description: catForm.value.description, sortNo: catForm.value.sortNo })
    catDialog.value = false
    await loadCategories()
  } catch (e) { console.error(e) }
}
async function deleteCategory(c: any) {
  if (!confirm(`确定删除分类「${c.name}」？`)) return
  try { await client.post(`/admin/category/delete/${c.id}`); await loadCategories() }
  catch (e) { console.error(e) }
}

async function loadConfigs() {
  try {
    const { data } = await client.get('/admin/sys-config')
    if (data.code === 0) configs.value = data.data || {}
  } catch (e) { console.error('admin config error', e) }
}
function editConfig(k: string, v: string) {
  configKey.value = k
  configValue.value = v ?? ''
  configDialog.value = true
}
async function saveConfig() {
  try {
    await client.post(`/admin/sys-config/${encodeURIComponent(configKey.value)}`, { value: configValue.value })
    configDialog.value = false
    await loadConfigs()
  } catch (e) { console.error(e) }
}

function fmt(ts: number) { return ts ? new Date(ts).toLocaleDateString('zh-CN') : '' }
</script>

<style scoped>
.admin-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--paper-border);
  border-radius: 8px;
  margin-bottom: 8px;
  background: var(--paper-bg);
}
</style>
