<script setup lang="ts">
import { onMounted, ref } from "vue";
import { http } from "@/utils/http";

type EbookImport = Record<string, any>;
const imports = ref<EbookImport[]>([]); const loading = ref(false); const submitting = ref(false); const error = ref(""); const message = ref("");
const form = ref({ attachmentId: "", title: "", language: "zh-CN" });
async function load() { loading.value = true; error.value = ""; try { const result: any = await http.get("/reading/ebook-imports"); imports.value = Array.isArray(result) ? result : []; } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "电子书导入记录加载失败"; } finally { loading.value = false; } }
async function submit() { if (!form.value.attachmentId.trim()) { error.value = "请输入电子书 Attachment ID"; return; } submitting.value = true; error.value = ""; message.value = ""; try { const result: any = await http.post("/reading/ebook-imports", { headers: { "Idempotency-Key": crypto.randomUUID() }, data: { attachmentId: form.value.attachmentId.trim(), title: form.value.title.trim() || undefined, language: form.value.language || undefined } }); message.value = `电子书导入已受理：${result?.id || "已创建"}`; form.value = { attachmentId: "", title: "", language: "zh-CN" }; await load(); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "电子书导入失败"; } finally { submitting.value = false; } }
function statusType(status: string) { return status === "ACCEPTED" ? "warning" : status === "FAILED" ? "danger" : "success"; }
onMounted(load);
</script>
<template>
  <main class="p-4 md:p-6"><div class="flex justify-between items-start gap-4 mb-6"><div><h1 class="text-2xl font-semibold">电子书导入</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">导入 EPUB 电子书并创建可阅读的作品版本。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4"/><el-alert v-if="message" :title="message" type="success" show-icon :closable="false" class="mb-4"/>
    <el-card shadow="never" class="mb-4"><el-form label-position="top" class="max-w-2xl"><el-form-item label="EPUB Attachment ID" required><el-input v-model="form.attachmentId" placeholder="先在附件管理上传 .epub，再粘贴 Attachment ID" clearable/></el-form-item><div class="grid grid-cols-1 md:grid-cols-2 gap-4"><el-form-item label="书名"><el-input v-model="form.title" placeholder="留空使用文件名"/></el-form-item><el-form-item label="语言"><el-input v-model="form.language" placeholder="zh-CN"/></el-form-item></div><el-button type="primary" :loading="submitting" @click="submit">提交 EPUB 导入</el-button></el-form></el-card>
    <el-card shadow="never"><el-empty v-if="!imports.length" description="暂无电子书导入记录"/><el-table v-else :data="imports" stripe><el-table-column prop="sourceAttachmentId" label="源附件" min-width="280"/><el-table-column prop="workId" label="Work ID" min-width="280"/><el-table-column prop="editionId" label="Edition ID" min-width="280"/><el-table-column label="状态" width="130"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ row.status }}</el-tag></template></el-table-column><el-table-column prop="errorMessage" label="错误" min-width="240"/><el-table-column prop="createdAt" label="导入时间" min-width="180"/></el-table></el-card>
  </main>
</template>
