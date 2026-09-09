<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { http } from "@/utils/http";

const route = useRoute();
const page = computed(() => String(route.path.split("/").pop()));
const titles: Record<string, string> = { automation: "自动化规则", executions: "自动化执行", events: "集成事件", sync: "导入与同步", plugins: "插件与连接器" };
const query = ref("");
const enabled = ref(false);
const plugins = ref<Record<string, any>[]>([]);
const loading = ref(false);
const error = ref("");
const installDialog = ref(false);
const installing = ref(false);
const form = ref({ pluginId: "", name: "", version: "", publisher: "", pluginApiVersion: "1", minimumServerVersion: "2.0.0", maximumServerVersion: "", entrypoint: "", permissions: "", extensionPoints: "", capabilities: "", grantedPermissions: "" });
const filteredPlugins = computed(() => plugins.value.filter(row => (!query.value || JSON.stringify(row).toLowerCase().includes(query.value.toLowerCase())) && (!enabled.value || row.lifecycle === "ENABLED")));

function split(value: string) { return value.split(",").map(item => item.trim()).filter(Boolean); }
async function loadPlugins() {
  if (page.value !== "plugins") return;
  loading.value = true; error.value = "";
  try { const result = await http.get<unknown, unknown>("/plugins"); plugins.value = Array.isArray(result) ? result as Record<string, any>[] : []; }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "插件列表加载失败"; }
  finally { loading.value = false; }
}
async function installPlugin() {
  if (!form.value.pluginId.trim() || !form.value.name.trim() || !form.value.version.trim() || !form.value.publisher.trim() || !form.value.entrypoint.trim()) { error.value = "请填写 Plugin ID、名称、版本、发布者和入口点"; return; }
  installing.value = true; error.value = "";
  try {
    await http.post("/plugins", { data: { manifest: { pluginId: form.value.pluginId.trim(), name: form.value.name.trim(), version: form.value.version.trim(), publisher: form.value.publisher.trim(), pluginApiVersion: form.value.pluginApiVersion.trim(), minimumServerVersion: form.value.minimumServerVersion.trim(), maximumServerVersion: form.value.maximumServerVersion.trim() || null, entrypoint: form.value.entrypoint.trim(), permissions: split(form.value.permissions), extensionPoints: split(form.value.extensionPoints), capabilities: split(form.value.capabilities) }, grantedPermissions: split(form.value.grantedPermissions) } });
    installDialog.value = false; await loadPlugins();
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "插件安装校验失败"; }
  finally { installing.value = false; }
}
watch(page, loadPlugins); onMounted(loadPlugins);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">{{ titles[page] || "集成与自动化" }}</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">外部连接、规则和任务均受权限与审计策略约束。</p></div><div class="flex gap-2"><el-button v-if="page === 'plugins'" type="primary" @click="installDialog = true">安装插件</el-button><el-button :loading="loading" @click="loadPlugins">刷新</el-button></div></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <template v-if="page === 'plugins'">
      <el-card shadow="never" class="mb-5"><div class="grid grid-cols-1 md:grid-cols-3 gap-4"><div><div class="text-sm text-[var(--el-text-color-secondary)]">已安装插件</div><div class="text-2xl font-semibold mt-2">{{ plugins.length }}</div></div><div><div class="text-sm text-[var(--el-text-color-secondary)]">已启用</div><div class="text-2xl font-semibold mt-2">{{ plugins.filter(item => item.lifecycle === 'ENABLED').length }}</div></div><div><div class="text-sm text-[var(--el-text-color-secondary)]">待启用</div><div class="text-2xl font-semibold mt-2">{{ plugins.filter(item => item.lifecycle === 'INSTALLED').length }}</div></div></div></el-card>
      <el-card shadow="never"><template #header><div class="flex justify-between items-center"><span>插件清单</span><div class="flex gap-2"><el-input v-model="query" placeholder="搜索 Plugin ID 或名称" clearable class="w-64" /><el-switch v-model="enabled" active-text="仅启用" /></div></div></template><el-skeleton v-if="loading" :rows="6" animated /><el-empty v-else-if="!filteredPlugins.length" description="暂无已安装插件" /><el-table v-else :data="filteredPlugins" stripe><el-table-column label="插件" min-width="220"><template #default="{ row }"><div class="font-medium">{{ row.manifest?.name || row.pluginId }}</div><div class="text-xs text-[var(--el-text-color-secondary)]">{{ row.manifest?.pluginId }}</div></template></el-table-column><el-table-column label="版本" width="130"><template #default="{ row }">{{ row.manifest?.version || "-" }}</template></el-table-column><el-table-column label="发布者" width="160"><template #default="{ row }">{{ row.manifest?.publisher || "-" }}</template></el-table-column><el-table-column prop="lifecycle" label="生命周期" width="130" /><el-table-column label="已授予权限" min-width="260"><template #default="{ row }">{{ (row.grantedPermissions || []).join(', ') || '无' }}</template></el-table-column></el-table></el-card>
    </template>
    <el-card v-else shadow="never"><el-empty description="该集成页面尚未接入对应后端目录接口" /><el-alert title="不会在前端伪造外部连接器状态或执行结果。" type="info" show-icon :closable="false" /></el-card>
    <el-dialog v-model="installDialog" title="安装并校验插件 Manifest" width="640px"><el-alert title="安装完成后保持 INSTALLED，不会自动启用；权限只可从 Manifest 声明项中选择。版本范围不兼容或权限未声明时，安装会被拒绝。" type="info" show-icon :closable="false" class="mb-4" /><el-form label-position="top"><div class="grid grid-cols-2 gap-3"><el-form-item label="Plugin ID" required><el-input v-model="form.pluginId" placeholder="run.example.plugin" /></el-form-item><el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item><el-form-item label="版本" required><el-input v-model="form.version" placeholder="1.0.0" /></el-form-item><el-form-item label="发布者" required><el-input v-model="form.publisher" /></el-form-item><el-form-item label="Plugin API 版本" required><el-input v-model="form.pluginApiVersion" /></el-form-item><el-form-item label="最低 Server 版本" required><el-input v-model="form.minimumServerVersion" /></el-form-item><el-form-item label="最高 Server 版本"><el-input v-model="form.maximumServerVersion" placeholder="可选，例如 3.0.0" /></el-form-item></div><el-form-item label="入口点" required><el-input v-model="form.entrypoint" placeholder="run.example.Plugin" /></el-form-item><el-form-item label="声明权限"><el-input v-model="form.permissions" placeholder="逗号分隔，例如 resource.read,resource.metadata.suggest" /></el-form-item><el-form-item label="授予权限"><el-input v-model="form.grantedPermissions" placeholder="只能填写上面声明的权限" /></el-form-item><el-form-item label="扩展点"><el-input v-model="form.extensionPoints" placeholder="逗号分隔" /></el-form-item><el-form-item label="能力"><el-input v-model="form.capabilities" placeholder="逗号分隔" /></el-form-item></el-form><template #footer><el-button @click="installDialog = false">取消</el-button><el-button type="primary" :loading="installing" @click="installPlugin">安装并校验</el-button></template></el-dialog>
  </main>
</template>
