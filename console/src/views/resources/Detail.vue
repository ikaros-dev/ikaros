<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { http } from "@/utils/http";

const route = useRoute();
const resource = ref<Record<string, any> | null>(null);
const attachments = ref<Record<string, any>[]>([]);
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const editDialog = ref(false);
const form = ref({ primaryTitle: "", summary: "" });

async function load() {
  loading.value = true; error.value = "";
  try {
    const [result, files] = await Promise.all([http.get(`/resources/${route.params.resourceId}`), http.get(`/resources/${route.params.resourceId}/attachments`)]);
    resource.value = result; attachments.value = Array.isArray(files) ? files : [];
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "资源详情加载失败"; }
  finally { loading.value = false; }
}
function beginEdit() { form.value = { primaryTitle: resource.value?.primaryTitle || resource.value?.title || "", summary: resource.value?.summary || "" }; editDialog.value = true; }
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
onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start mb-6"><div><p class="text-sm text-[var(--el-text-color-secondary)]">资源中心 / 资源详情</p><h1 class="mt-2 text-2xl font-semibold">{{ resource?.primaryTitle || resource?.title || route.params.resourceId }}</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">查看资源元数据、附件关系和生命周期状态。</p></div><div class="flex gap-2"><el-button @click="$router.back()">返回</el-button><el-button :loading="loading" @click="load">刷新</el-button><el-button v-if="resource" type="primary" @click="beginEdit">编辑元数据</el-button><el-button v-if="resource" @click="$router.push(`/storage-center/attachments?resourceId=${route.params.resourceId}`)">查看附件</el-button></div></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
    <el-skeleton v-if="loading" :rows="8" animated /><el-card v-else-if="resource" shadow="never" class="mt-4"><el-tabs><el-tab-pane label="概览"><el-descriptions :column="1" border><el-descriptions-item label="Resource ID">{{ resource.id }}</el-descriptions-item><el-descriptions-item label="标题">{{ resource.primaryTitle || resource.title || "-" }}</el-descriptions-item><el-descriptions-item label="摘要">{{ resource.summary || "-" }}</el-descriptions-item><el-descriptions-item label="类型">{{ resource.type || resource.resourceType || "-" }}</el-descriptions-item><el-descriptions-item label="生命周期"><el-tag>{{ resource.lifecycle || resource.status || "ACTIVE" }}</el-tag></el-descriptions-item><el-descriptions-item label="版本">{{ resource.version ?? 0 }}</el-descriptions-item><el-descriptions-item label="语言">{{ resource.locale || "-" }}</el-descriptions-item><el-descriptions-item label="更新时间">{{ resource.updatedAt || "-" }}</el-descriptions-item></el-descriptions></el-tab-pane><el-tab-pane label="Attachment 关联"><el-empty v-if="!attachments.length" description="暂无附件关联" /><el-table v-else :data="attachments" stripe><el-table-column prop="fileName" label="文件名" min-width="220" /><el-table-column prop="kind" label="角色" width="130" /><el-table-column prop="blobId" label="Blob" min-width="220" /><el-table-column prop="sizeBytes" label="大小（Bytes）" width="140" /><el-table-column prop="availability" label="可用性" width="130" /></el-table></el-tab-pane><el-tab-pane label="活动"><el-empty description="暂无资源活动" /></el-tab-pane></el-tabs></el-card><el-empty v-else description="暂无资源数据" />
    <el-dialog v-model="editDialog" title="编辑资源元数据" width="520px"><el-form label-position="top"><el-form-item label="主标题" required><el-input v-model="form.primaryTitle" maxlength="512" /></el-form-item><el-form-item label="摘要"><el-input v-model="form.summary" type="textarea" maxlength="4000" :rows="5" /></el-form-item></el-form><template #footer><el-button @click="editDialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template></el-dialog>
  </main>
</template>

