<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { http } from "@/utils/http";

const route = useRoute();
const router = useRouter();
const resource = ref<Record<string, any> | null>(null);
const attachments = ref<Record<string, any>[]>([]);
const metadata = ref<Record<string, any>[]>([]);
const metadataCandidates = ref<Record<string, any>[]>([]);
const metadataDialog = ref(false);
const metadataForm = ref({ fieldKey: "", value: "" });
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const editDialog = ref(false);
const form = ref({ primaryTitle: "", summary: "" });
const titleDialog = ref(false);
const titleForm = ref({ locale: "en-US", title: "", primary: false, kind: "TITLE" as "TITLE" | "ALIAS" });
const identityDialog = ref(false);
const identityForm = ref({ provider: "", type: "", value: "" });
const relations = ref<Record<string, any>[]>([]);
const relationDialog = ref(false);
const relationForm = ref({ targetResourceId: "", type: "RELATED_TO", position: 0 });
const userState = ref<Record<string, any> | null>(null);
const tags = ref<Record<string, any>[]>([]);
const activities = ref<Record<string, any>[]>([]);
const tagDialog = ref(false);
const tagForm = ref({ name: "", color: "" });

async function load() {
  loading.value = true; error.value = "";
  try {
    const [result, files, metadataResult, relationResult] = await Promise.all([http.get(`/resources/${route.params.resourceId}`), http.get(`/resources/${route.params.resourceId}/attachments`), http.get(`/resources/${route.params.resourceId}/metadata`), http.get(`/resources/${route.params.resourceId}/relations`)]);
    const [stateResult, tagResult, activityResult, candidateResult] = await Promise.all([http.get(`/resources/${route.params.resourceId}/user-state`).catch(() => null), http.get(`/resources/${route.params.resourceId}/tags`), http.get(`/activity?limit=200`), http.get(`/ingestion/resources/${route.params.resourceId}/metadata-candidates`).catch(() => [])]);
    resource.value = result; attachments.value = Array.isArray(files) ? files : []; metadata.value = Array.isArray(metadataResult) ? metadataResult : []; metadataCandidates.value = Array.isArray(candidateResult) ? candidateResult : []; relations.value = Array.isArray(relationResult) ? relationResult : []; userState.value = stateResult || { favorite: false, rating: null, statusCode: "", progressValue: null, progressUnit: "" }; tags.value = Array.isArray(tagResult) ? tagResult : []; activities.value = (Array.isArray(activityResult) ? activityResult : []).filter((item: any) => String(item.resourceId) === String(route.params.resourceId));
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "资源详情加载失败"; }
  finally { loading.value = false; }
}
function beginEdit() { form.value = { primaryTitle: resource.value?.primaryTitle || resource.value?.title || "", summary: resource.value?.summary || "" }; editDialog.value = true; }
function beginMetadata(row: Record<string, any>) { metadataForm.value = { fieldKey: row.fieldKey || "", value: row.value || "" }; metadataDialog.value = true; }
function useMetadataCandidate(row: Record<string, any>) { metadataForm.value = { fieldKey: row.fieldKey || "", value: row.value || "" }; metadataDialog.value = true; }
async function saveMetadata() { if (!resource.value || !metadataForm.value.fieldKey.trim()) { error.value = "字段名不能为空"; return; } try { await http.request("put", `/resources/${resource.value.id}/metadata/${encodeURIComponent(metadataForm.value.fieldKey.trim())}`, { data: { value: metadataForm.value.value } }); metadataDialog.value = false; await load(); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "手动覆盖保存失败"; } }
function beginTitle(kind: "TITLE" | "ALIAS" = "TITLE") { titleForm.value = { locale: "en-US", title: "", primary: false, kind }; titleDialog.value = true; }
async function saveTitle() {
  if (!resource.value || !titleForm.value.title.trim()) { error.value = "标题不能为空"; return; }
  try { await http.request("put", `/resources/${resource.value.id}/titles`, { data: { locale: titleForm.value.locale, title: titleForm.value.title.trim(), primary: titleForm.value.kind === "TITLE" && titleForm.value.primary, kind: titleForm.value.kind } }); titleDialog.value = false; await load(); }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "多语言标题保存失败"; }
}
function beginIdentity() { identityForm.value = { provider: "", type: "", value: "" }; identityDialog.value = true; }
async function saveIdentity() {
  if (!resource.value || !identityForm.value.provider || !identityForm.value.type || !identityForm.value.value) { error.value = "Provider、类型和外部 ID 不能为空"; return; }
  try { await http.post(`/resources/${resource.value.id}/external-identities`, { data: identityForm.value }); identityDialog.value = false; await load(); }
  catch (e: any) { error.value = e?.response?.status === 409 ? "该外部身份已绑定到其他资源。" : e?.response?.data?.detail || e?.message || "外部身份绑定失败"; }
}
function beginRelation() { relationForm.value = { targetResourceId: "", type: "RELATED_TO", position: relations.value.length }; relationDialog.value = true; }
async function saveRelation() {
  if (!resource.value || !relationForm.value.targetResourceId.trim()) { error.value = "目标 Resource ID 不能为空"; return; }
  try { await http.post(`/resources/${resource.value.id}/relations`, { data: { targetResourceId: relationForm.value.targetResourceId.trim(), type: relationForm.value.type, position: relationForm.value.position } }); relationDialog.value = false; await load(); }
  catch (e: any) { error.value = e?.response?.status === 409 ? "关系重复或目标不能是当前资源。" : e?.response?.data?.detail || e?.message || "资源关系创建失败"; }
}
async function removeRelation(id: string) { if (!resource.value || !window.confirm("确认移除这条资源关系吗？两端 Resource 不会被删除。")) return; try { await http.request("delete", `/resources/${resource.value.id}/relations/${id}`); await load(); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "资源关系移除失败"; } }
function beginTag() { tagForm.value = { name: "", color: "" }; tagDialog.value = true; }
async function saveTag() { if (!resource.value || !tagForm.value.name.trim()) { error.value = "标签名称不能为空"; return; } try { await http.post(`/resources/${resource.value.id}/tags`, { data: { name: tagForm.value.name.trim(), color: tagForm.value.color || null } }); tagDialog.value = false; await load(); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "标签添加失败"; } }
async function removeTag(id: string) { if (!resource.value || !window.confirm("确认移除这个个人标签吗？")) return; try { await http.request("delete", `/resources/${resource.value.id}/tags/${id}`); await load(); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "标签移除失败"; } }
async function toggleFavorite() { if (!resource.value) return; try { const favorite = Boolean(userState.value?.favorite); if (favorite) await http.request("delete", `/resources/${resource.value.id}/favorite`); else await http.post(`/resources/${resource.value.id}/favorite`); await load(); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "收藏状态更新失败"; } }
async function saveUserState() { if (!resource.value || !userState.value) return; try { const result = await http.request("put", `/resources/${resource.value.id}/user-state`, { data: { favorite: Boolean(userState.value.favorite), rating: userState.value.rating ?? null, statusCode: userState.value.statusCode || null, progressValue: userState.value.progressValue ?? null, progressUnit: userState.value.progressUnit || null }, headers: { "If-Match": userState.value.version == null ? undefined : `"${userState.value.version}"` } }); userState.value = result; } catch (e: any) { error.value = e?.response?.status === 409 ? "用户状态版本已变化，请刷新后重试。" : e?.response?.data?.detail || e?.message || "用户状态保存失败"; } }
async function save() {
  if (!resource.value) return;
  saving.value = true; error.value = "";
  try {
    resource.value = await http.request("patch", `/resources/${resource.value.id}`, { data: { primary_title: form.value.primaryTitle, summary: form.value.summary }, headers: { "Content-Type": "application/merge-patch+json", "If-Match": `"${resource.value.version ?? 0}"` } });
    editDialog.value = false;
  } catch (e: any) {
    error.value = e?.response?.status === 409 ? "资源已被其他请求修改，请刷新后重新编辑。" : e?.response?.data?.detail || e?.message || "资源保存失败";
  } finally { saving.value = false; }
}
async function archive() {
  if (!resource.value || !window.confirm("确认归档这个资源吗？附件和 Blob 不会被删除。")) return;
  try { resource.value = await http.post(`/resources/${resource.value.id}/actions/archive`, { headers: { "If-Match": `"${resource.value.version ?? 0}"` } }); }
  catch (e: any) { error.value = e?.response?.status === 409 ? "资源当前状态不允许归档，请刷新后重试。" : e?.response?.data?.detail || e?.message || "资源归档失败"; }
}
async function trash() {
  if (!resource.value || !window.confirm("确认将这个资源移入回收站吗？附件和 Blob 不会被物理删除。")) return;
  try { await http.post(`/resources/${resource.value.id}/actions/trash`, { headers: { "If-Match": `"${resource.value.version ?? 0}"` } }); await router.back(); }
  catch (e: any) { error.value = e?.response?.status === 409 ? "资源版本已变化，请刷新后重试。" : e?.response?.data?.detail || e?.message || "移入回收站失败"; }
}
onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start mb-6"><div><p class="text-sm text-[var(--el-text-color-secondary)]">资源中心 / 资源详情</p><h1 class="mt-2 text-2xl font-semibold">{{ resource?.primaryTitle || resource?.title || route.params.resourceId }}</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">查看资源元数据、附件关系和生命周期状态。</p></div><div class="flex gap-2"><el-button @click="$router.back()">返回</el-button><el-button :loading="loading" @click="load">刷新</el-button><el-button v-if="resource && resource.lifecycle === 'ACTIVE'" type="warning" @click="archive">归档</el-button><el-button v-if="resource && resource.lifecycle !== 'TRASH'" type="danger" @click="trash">移入回收站</el-button><el-button v-if="resource" type="primary" @click="beginEdit">编辑元数据</el-button><el-button v-if="resource" @click="$router.push(`/storage-center/attachments?resourceId=${route.params.resourceId}`)">查看附件</el-button></div></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
    <el-skeleton v-if="loading" :rows="8" animated /><el-card v-else-if="resource" shadow="never" class="mt-4"><el-tabs><el-tab-pane label="概览"><el-descriptions :column="1" border><el-descriptions-item label="Resource ID">{{ resource.id }}</el-descriptions-item><el-descriptions-item label="标题">{{ resource.primaryTitle || resource.title || "-" }}</el-descriptions-item><el-descriptions-item label="摘要">{{ resource.summary || "-" }}</el-descriptions-item><el-descriptions-item label="类型">{{ resource.type || resource.resourceType || "-" }}</el-descriptions-item><el-descriptions-item label="生命周期"><el-tag>{{ resource.lifecycle || resource.status || "ACTIVE" }}</el-tag></el-descriptions-item><el-descriptions-item label="版本">{{ resource.version ?? 0 }}</el-descriptions-item><el-descriptions-item label="更新时间">{{ resource.updatedAt || "-" }}</el-descriptions-item></el-descriptions></el-tab-pane><el-tab-pane label="多语言标题"><div class="flex justify-end mb-3 gap-2"><el-button type="primary" @click="beginTitle('TITLE')">添加标题</el-button><el-button @click="beginTitle('ALIAS')">添加别名</el-button></div><el-empty v-if="!resource.titles?.length" description="暂无多语言标题或别名" /><el-table v-else :data="resource.titles" stripe><el-table-column prop="locale" label="语言/地区" width="140" /><el-table-column prop="value" label="标题/别名" min-width="280" /><el-table-column prop="kind" label="类型" width="120" /><el-table-column label="主标题" width="100"><template #default="{ row }"><el-tag v-if="row.primary" type="success">是</el-tag></template></el-table-column></el-table></el-tab-pane><el-tab-pane label="外部身份"><div class="flex justify-end mb-3"><el-button type="primary" @click="beginIdentity">绑定外部身份</el-button></div><el-empty v-if="!resource.externalIdentities?.length" description="暂无外部身份绑定" /><el-table v-else :data="resource.externalIdentities" stripe><el-table-column prop="provider" label="Provider" width="160" /><el-table-column prop="type" label="类型" width="160" /><el-table-column prop="value" label="外部 ID" min-width="260" /></el-table></el-tab-pane><el-tab-pane label="Attachment 关联"><el-empty v-if="!attachments.length" description="暂无附件关联" /><el-table v-else :data="attachments" stripe><el-table-column prop="fileName" label="文件名" min-width="220" /><el-table-column prop="kind" label="角色" width="130" /><el-table-column prop="blobId" label="Blob" min-width="220" /><el-table-column prop="sizeBytes" label="大小（Bytes）" width="140" /><el-table-column prop="availability" label="可用性" width="130" /></el-table></el-tab-pane><el-tab-pane label="活动"><el-empty description="暂无资源活动" /></el-tab-pane></el-tabs></el-card><el-empty v-else description="暂无资源数据" />
    <el-dialog v-model="editDialog" title="编辑资源元数据" width="520px"><el-form label-position="top"><el-form-item label="主标题" required><el-input v-model="form.primaryTitle" maxlength="512" /></el-form-item><el-form-item label="摘要"><el-input v-model="form.summary" type="textarea" maxlength="4000" :rows="5" /></el-form-item></el-form><template #footer><el-button @click="editDialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template></el-dialog><el-dialog v-model="titleDialog" :title="titleForm.kind === 'ALIAS' ? '添加别名' : '添加多语言标题'" width="460px"><el-form label-position="top"><el-form-item label="语言/地区" required><el-input v-model="titleForm.locale" placeholder="例如 zh-CN、en-US" /></el-form-item><el-form-item :label="titleForm.kind === 'ALIAS' ? '别名' : '标题'" required><el-input v-model="titleForm.title" maxlength="512" /></el-form-item><el-checkbox v-if="titleForm.kind === 'TITLE'" v-model="titleForm.primary">设为主标题</el-checkbox></el-form><template #footer><el-button @click="titleDialog = false">取消</el-button><el-button type="primary" @click="saveTitle">保存</el-button></template></el-dialog><el-dialog v-model="identityDialog" title="绑定外部平台身份" width="460px"><el-form label-position="top"><el-form-item label="Provider" required><el-input v-model="identityForm.provider" placeholder="例如 tmdb" /></el-form-item><el-form-item label="类型" required><el-input v-model="identityForm.type" placeholder="例如 movie" /></el-form-item><el-form-item label="外部 ID" required><el-input v-model="identityForm.value" /></el-form-item></el-form><template #footer><el-button @click="identityDialog = false">取消</el-button><el-button type="primary" @click="saveIdentity">绑定</el-button></template></el-dialog>
    <el-card v-if="resource" shadow="never" class="mt-4"><template #header><div class="flex justify-between items-center"><span>资源关系</span><el-button type="primary" @click="beginRelation">建立关系</el-button></div></template><el-empty v-if="!relations.length" description="暂无出向资源关系" /><el-table v-else :data="relations" stripe><el-table-column prop="targetResourceId" label="目标 Resource ID" min-width="280" /><el-table-column prop="type" label="关系类型" width="150" /><el-table-column prop="position" label="位置" width="100" /><el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="danger" @click="removeRelation(row.id)">移除</el-button></template></el-table-column></el-table></el-card>
    <el-card v-if="resource" shadow="never" class="mt-4"><template #header><div class="flex justify-between items-center"><span>我的资源状态</span><div class="flex gap-2"><el-button :type="userState?.favorite ? 'warning' : 'default'" @click="toggleFavorite">{{ userState?.favorite ? '取消收藏' : '收藏' }}</el-button><el-button type="primary" @click="saveUserState">保存状态</el-button></div></div></template><el-form inline><el-form-item label="评分"><el-input-number v-model="userState.rating" :min="0" :max="10" :step="0.5" /></el-form-item><el-form-item label="状态"><el-input v-model="userState.statusCode" placeholder="例如 IN_PROGRESS" /></el-form-item><el-form-item label="进度"><el-input-number v-model="userState.progressValue" :min="0" /><el-input v-model="userState.progressUnit" placeholder="页 / 秒 / %" class="ml-2 w-28" /></el-form-item></el-form></el-card>
    <el-card v-if="resource" shadow="never" class="mt-4"><template #header><div class="flex justify-between items-center"><span>个人标签</span><el-button type="primary" @click="beginTag">添加标签</el-button></div></template><el-empty v-if="!tags.length" description="暂无个人标签" /><div v-else class="flex flex-wrap gap-2"><el-tag v-for="tag in tags" :key="tag.id" closable @close="removeTag(tag.id)" :color="tag.color || undefined">{{ tag.name }}</el-tag></div></el-card>
    <el-card v-if="resource" shadow="never" class="mt-4"><template #header>近期活动</template><el-empty v-if="!activities.length" description="暂无该资源活动" /><el-table v-else :data="activities" stripe><el-table-column prop="type" label="类型" width="130" /><el-table-column prop="details" label="详情" min-width="240" /><el-table-column prop="occurredAt" label="发生时间" min-width="180" /></el-table></el-card>
    <el-card v-if="resource" shadow="never" class="mt-4"><template #header>字段来源</template><el-empty v-if="!metadata.length" description="暂无扩展字段" /><el-table v-else :data="metadata" stripe><el-table-column prop="fieldKey" label="字段" width="180" /><el-table-column prop="value" label="值" min-width="260" /><el-table-column prop="source" label="来源" width="140" /><el-table-column prop="sourceReference" label="来源引用" min-width="220" /><el-table-column label="手动覆盖" width="110"><template #default="{ row }"><el-tag :type="row.userOverride ? 'success' : 'info'">{{ row.userOverride ? '是' : '否' }}</el-tag></template></el-table-column></el-table></el-card>
    <el-card v-if="resource" shadow="never" class="mt-4"><template #header>外部元数据候选</template><el-empty v-if="!metadataCandidates.length" description="暂无待处理候选" /><el-table v-else :data="metadataCandidates" stripe><el-table-column prop="fieldKey" label="字段" width="160" /><el-table-column prop="value" label="候选值" min-width="240" /><el-table-column prop="source" label="来源" width="130" /><el-table-column prop="confidence" label="置信度" width="100" /><el-table-column prop="status" label="状态" width="120" /><el-table-column label="操作" width="140"><template #default="{ row }"><el-button link type="primary" @click="useMetadataCandidate(row)">人工确认覆盖</el-button></template></el-table-column></el-table></el-card>
    <el-button v-if="resource" class="mt-3" @click="metadataDialog = true">新增/覆盖字段</el-button><el-dialog v-model="metadataDialog" title="保存手动覆盖值" width="460px"><el-form label-position="top"><el-form-item label="字段"><el-input v-model="metadataForm.fieldKey" placeholder="例如 genre" /></el-form-item><el-form-item label="值"><el-input v-model="metadataForm.value" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="metadataDialog = false">取消</el-button><el-button type="primary" @click="saveMetadata">保存覆盖</el-button></template></el-dialog>
    <el-dialog v-model="relationDialog" title="建立资源关系" width="460px"><el-form label-position="top"><el-form-item label="目标 Resource ID" required><el-input v-model="relationForm.targetResourceId" /></el-form-item><el-form-item label="关系类型" required><el-select v-model="relationForm.type" class="w-full"><el-option v-for="type in ['CONTAINS','PART_OF','PREQUEL_TO','SEQUEL_TO','ADAPTATION_OF','VERSION_OF','DERIVED_FROM','RELATED_TO']" :key="type" :label="type" :value="type" /></el-select></el-form-item><el-form-item label="位置"><el-input-number v-model="relationForm.position" :min="0" /></el-form-item></el-form><template #footer><el-button @click="relationDialog = false">取消</el-button><el-button type="primary" @click="saveRelation">建立</el-button></template></el-dialog>
  </main>
</template>
