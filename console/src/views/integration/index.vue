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
const eventDelivery = ref<Record<string, any> | null>(null);
const pendingEvents = ref<Record<string, any>[]>([]);
const retryingEventId = ref("");
const loading = ref(false);
const error = ref("");
const installDialog = ref(false);
const installing = ref(false);
const upgradingPluginId = ref("");
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
async function loadEvents() {
  if (page.value !== "events") return;
  loading.value = true; error.value = "";
  try {
    const [diagnostics, events] = await Promise.all([
      http.get<any, any>("/health/operations"),
      http.get<any, any>("/admin/integration/events")
    ]);
    eventDelivery.value = diagnostics?.eventDelivery || null;
    pendingEvents.value = Array.isArray(events) ? events : [];
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "事件投递状态加载失败"; }
  finally { loading.value = false; }
}
async function retryEvent(row: Record<string, any>) {
  if (!row.id || !window.confirm(`确认重试事件 ${row.id} 吗？`)) return;
  retryingEventId.value = row.id; error.value = "";
  try { await http.post(`/admin/integration/events/${encodeURIComponent(row.id)}/actions/retry`); await loadEvents(); }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "事件重试失败"; }
  finally { retryingEventId.value = ""; }
}
async function installPlugin() {
  if (!form.value.pluginId.trim() || !form.value.name.trim() || !form.value.version.trim() || !form.value.publisher.trim() || !form.value.entrypoint.trim()) { error.value = "请填写 Plugin ID、名称、版本、发布者和入口点"; return; }
  installing.value = true; error.value = "";
  try {
    const payload = { manifest: { pluginId: form.value.pluginId.trim(), name: form.value.name.trim(), version: form.value.version.trim(), publisher: form.value.publisher.trim(), pluginApiVersion: form.value.pluginApiVersion.trim(), minimumServerVersion: form.value.minimumServerVersion.trim(), maximumServerVersion: form.value.maximumServerVersion.trim() || null, entrypoint: form.value.entrypoint.trim(), permissions: split(form.value.permissions), extensionPoints: split(form.value.extensionPoints), capabilities: split(form.value.capabilities) }, grantedPermissions: split(form.value.grantedPermissions) };
    if (upgradingPluginId.value) await http.post(`/plugins/${encodeURIComponent(upgradingPluginId.value)}/upgrade`, { data: payload }); else await http.post("/plugins", { data: payload });
    installDialog.value = false; upgradingPluginId.value = ""; await loadPlugins();
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "插件安装校验失败"; }
  finally { installing.value = false; }
}
function openInstall() { upgradingPluginId.value = ""; installDialog.value = true; }
function openUpgrade(row: Record<string, any>) {
  const manifest = row.manifest || {};
  upgradingPluginId.value = manifest.pluginId || row.pluginId || "";
  form.value = { pluginId: manifest.pluginId || "", name: manifest.name || "", version: manifest.version || "", publisher: manifest.publisher || "", pluginApiVersion: manifest.pluginApiVersion || "1", minimumServerVersion: manifest.minimumServerVersion || "2.0.0", maximumServerVersion: manifest.maximumServerVersion || "", entrypoint: manifest.entrypoint || "", permissions: (manifest.permissions || []).join(", "), extensionPoints: (manifest.extensionPoints || []).join(", "), capabilities: (manifest.capabilities || []).join(", "), grantedPermissions: (row.grantedPermissions || []).join(", ") };
  installDialog.value = true;
}
async function togglePlugin(row: Record<string, any>) {
  const pluginId = row.manifest?.pluginId || row.pluginId;
  if (!pluginId) return;
  if (row.lifecycle === "ENABLED" && !window.confirm("确认禁用此插件并撤销其扩展注册吗？")) return;
  error.value = "";
  try { await http.post(`/plugins/${encodeURIComponent(pluginId)}/${row.lifecycle === "ENABLED" ? "disable" : "enable"}`); await loadPlugins(); }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "更新插件生命周期失败"; }
}
async function uninstallPlugin(row: Record<string, any>, retention: "KEEP_DATA" | "DELETE_DATA") {
  const pluginId = row.manifest?.pluginId || row.pluginId;
  if (!pluginId || row.lifecycle === "ENABLED") return;
  const message = retention === "KEEP_DATA" ? "确认卸载插件并保留插件数据/配置吗？" : "确认永久删除插件记录及插件数据吗？此操作不可恢复。";
  if (!window.confirm(message)) return;
  error.value = "";
  try { await http.post(`/plugins/${encodeURIComponent(pluginId)}/uninstall`, { data: { retention } }); await loadPlugins(); }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "插件卸载失败"; }
}
function loadPage() { return page.value === "plugins" ? loadPlugins() : page.value === "events" ? loadEvents() : Promise.resolve(); }
watch(page, loadPage); onMounted(loadPage);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">{{ titles[page] || "集成与自动化" }}</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">外部连接、规则和任务均受权限与审计策略约束。</p></div><div class="flex gap-2"><el-button v-if="page === 'plugins'" type="primary" @click="openInstall">安装插件</el-button><el-button :loading="loading" @click="loadPage">刷新</el-button></div></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <template v-if="page === 'plugins'">
      <el-card shadow="never" class="mb-5"><div class="grid grid-cols-1 md:grid-cols-3 gap-4"><div><div class="text-sm text-[var(--el-text-color-secondary)]">已安装插件</div><div class="text-2xl font-semibold mt-2">{{ plugins.length }}</div></div><div><div class="text-sm text-[var(--el-text-color-secondary)]">已启用</div><div class="text-2xl font-semibold mt-2">{{ plugins.filter(item => item.lifecycle === 'ENABLED').length }}</div></div><div><div class="text-sm text-[var(--el-text-color-secondary)]">待启用</div><div class="text-2xl font-semibold mt-2">{{ plugins.filter(item => item.lifecycle === 'INSTALLED').length }}</div></div></div></el-card>
      <el-card shadow="never"><template #header><div class="flex justify-between items-center"><span>插件清单</span><div class="flex gap-2"><el-input v-model="query" placeholder="搜索 Plugin ID 或名称" clearable class="w-64" /><el-switch v-model="enabled" active-text="仅启用" /></div></div></template><el-skeleton v-if="loading" :rows="6" animated /><el-empty v-else-if="!filteredPlugins.length" description="暂无已安装插件" /><el-table v-else :data="filteredPlugins" stripe><el-table-column label="插件" min-width="220"><template #default="{ row }"><div class="font-medium">{{ row.manifest?.name || row.pluginId }}</div><div class="text-xs text-[var(--el-text-color-secondary)]">{{ row.manifest?.pluginId }}</div></template></el-table-column><el-table-column label="版本" width="130"><template #default="{ row }">{{ row.manifest?.version || "-" }}</template></el-table-column><el-table-column label="发布者" width="160"><template #default="{ row }">{{ row.manifest?.publisher || "-" }}</template></el-table-column><el-table-column prop="lifecycle" label="生命周期" width="130" /><el-table-column label="已授予权限" min-width="260"><template #default="{ row }">{{ (row.grantedPermissions || []).join(', ') || '无' }}</template></el-table-column><el-table-column label="操作" width="250"><template #default="{ row }"><el-button v-if="row.lifecycle !== 'UNINSTALLED'" link type="primary" @click="togglePlugin(row)">{{ row.lifecycle === 'ENABLED' ? '禁用' : '启用' }}</el-button><el-button v-if="row.lifecycle !== 'UNINSTALLED'" link @click="openUpgrade(row)">升级</el-button><el-button v-if="row.lifecycle !== 'ENABLED' && row.lifecycle !== 'UNINSTALLED'" link @click="uninstallPlugin(row, 'KEEP_DATA')">卸载并保留</el-button><el-button v-if="row.lifecycle !== 'ENABLED' && row.lifecycle !== 'UNINSTALLED'" link type="danger" @click="uninstallPlugin(row, 'DELETE_DATA')">卸载并删除</el-button></template></el-table-column></el-table></el-card>
    </template>
    <template v-else-if="page === 'events'">
      <el-skeleton v-if="loading" :rows="5" animated />
      <template v-else-if="eventDelivery"><el-card shadow="never" class="mb-5"><template #header><div class="flex justify-between items-center"><span>Durable Event 投递</span><el-tag :type="eventDelivery.pendingCount ? 'warning' : 'success'">{{ eventDelivery.pendingCount ? '有待处理事件' : '已清空' }}</el-tag></div></template><div class="grid grid-cols-1 md:grid-cols-3 gap-4"><div><div class="text-sm text-[var(--el-text-color-secondary)]">待投递</div><div class="text-2xl font-semibold mt-2">{{ eventDelivery.pendingCount ?? 0 }}</div></div><div><div class="text-sm text-[var(--el-text-color-secondary)]">已尝试未完成</div><div class="text-2xl font-semibold mt-2">{{ eventDelivery.attemptedPendingCount ?? 0 }}</div></div><div><div class="text-sm text-[var(--el-text-color-secondary)]">最近尝试</div><div class="text-lg font-semibold mt-3">{{ eventDelivery.lastAttemptAt || '—' }}</div></div></div><el-alert title="状态来自后端 Outbox 投递诊断；重复投递由 Inbox 去重，不在前端推断或伪造事件结果。" type="info" show-icon :closable="false" class="mt-5" /></el-card><el-card shadow="never"><template #header>待投递事件</template><el-empty v-if="!pendingEvents.length" description="暂无待投递事件" /><el-table v-else :data="pendingEvents" stripe><el-table-column prop="id" label="Event ID" min-width="250" /><el-table-column prop="eventType" label="事件类型" min-width="220" /><el-table-column prop="producerSubsystem" label="生产者" width="150" /><el-table-column prop="occurredAt" label="发生时间" min-width="190" /><el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" :loading="retryingEventId === row.id" @click="retryEvent(row)">重试</el-button></template></el-table-column></el-table></el-card></template>
      <el-empty v-else description="后端未返回事件投递诊断" />
    </template>
    <el-card v-else shadow="never"><el-empty description="该集成页面尚未接入对应后端目录接口" /><el-alert title="不会在前端伪造外部连接器状态或执行结果。" type="info" show-icon :closable="false" /></el-card>
    <el-dialog v-model="installDialog" :title="upgradingPluginId ? '升级并校验插件 Manifest' : '安装并校验插件 Manifest'" width="640px"><el-alert :title="upgradingPluginId ? '升级失败会保留当前版本和生命周期。' : '安装完成后保持 INSTALLED，不会自动启用。'" type="info" show-icon :closable="false" class="mb-4" /><el-form label-position="top"><div class="grid grid-cols-2 gap-3"><el-form-item label="Plugin ID" required><el-input v-model="form.pluginId" :disabled="Boolean(upgradingPluginId)" placeholder="run.example.plugin" /></el-form-item><el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item><el-form-item label="版本" required><el-input v-model="form.version" placeholder="1.0.0" /></el-form-item><el-form-item label="发布者" required><el-input v-model="form.publisher" /></el-form-item><el-form-item label="Plugin API 版本" required><el-input v-model="form.pluginApiVersion" /></el-form-item><el-form-item label="最低 Server 版本" required><el-input v-model="form.minimumServerVersion" /></el-form-item><el-form-item label="最高 Server 版本"><el-input v-model="form.maximumServerVersion" placeholder="可选，例如 3.0.0" /></el-form-item></div><el-form-item label="入口点" required><el-input v-model="form.entrypoint" placeholder="run.example.Plugin" /></el-form-item><el-form-item label="声明权限"><el-input v-model="form.permissions" placeholder="逗号分隔，例如 resource.read,resource.metadata.suggest" /></el-form-item><el-form-item label="授予权限"><el-input v-model="form.grantedPermissions" placeholder="只能填写上面声明的权限" /></el-form-item><el-form-item label="扩展点"><el-input v-model="form.extensionPoints" placeholder="逗号分隔" /></el-form-item><el-form-item label="能力"><el-input v-model="form.capabilities" placeholder="逗号分隔" /></el-form-item></el-form><template #footer><el-button @click="installDialog = false">取消</el-button><el-button type="primary" :loading="installing" @click="installPlugin">{{ upgradingPluginId ? '升级并校验' : '安装并校验' }}</el-button></template></el-dialog>
  </main>
</template>
