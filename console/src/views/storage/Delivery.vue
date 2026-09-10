<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { http } from "@/utils/http";

type Provider = Record<string, any>;
const route = useRoute();
const section = computed(() => String(route.path.split("/").pop()));
const titles: Record<string, string> = { reliability: "Delivery Reliability", failover: "Failover 观测", restore: "Restore Reconciliation", budget: "Traffic Budget", purge: "CDN Purge" };
const providers = ref<Provider[]>([]);
const loading = ref(false);
const probing = ref<string | null>(null);
const error = ref("");

async function load() {
  loading.value = true; error.value = "";
  try {
    const result = await http.get<unknown, unknown>("/admin/delivery-providers");
    providers.value = Array.isArray(result) ? result as Provider[] : [];
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "Delivery Provider 状态加载失败"; }
  finally { loading.value = false; }
}
async function probe(row: Provider) {
  if (!row.id || probing.value) return;
  probing.value = row.id; error.value = "";
  try { await http.post(`/admin/delivery-providers/${row.id}/probe`, { headers: { "Idempotency-Key": crypto.randomUUID() } }); await load(); }
  catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "Delivery Provider 检测任务提交失败"; }
  finally { probing.value = null; }
}
onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">{{ titles[section] || "Delivery 运维" }}</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">展示真实 Provider 健康状态，不暴露 Signed URL、Raw Token 或 Origin Credential。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="warning" show-icon :closable="false" class="mb-4" />
    <el-skeleton v-if="loading" :rows="7" animated />
    <el-card v-else shadow="never"><el-empty v-if="!providers.length" description="暂无已配置 Delivery Provider" /><el-table v-else :data="providers" stripe><el-table-column prop="displayName" label="Provider" min-width="220"><template #default="{ row }">{{ row.displayName || row.providerKey || row.id }}</template></el-table-column><el-table-column prop="providerType" label="类型" width="150" /><el-table-column prop="healthStatus" label="健康状态" width="150"><template #default="{ row }"><el-tag :type="String(row.healthStatus).toUpperCase() === 'HEALTHY' ? 'success' : 'warning'">{{ row.healthStatus || "UNKNOWN" }}</el-tag></template></el-table-column><el-table-column prop="enabled" label="启用" width="100"><template #default="{ row }">{{ row.enabled ? "是" : "否" }}</template></el-table-column><el-table-column prop="updatedAt" label="最近更新" min-width="180" /><el-table-column label="操作" width="130"><template #default="{ row }"><el-button link type="primary" :loading="probing === row.id" @click="probe(row)">检测连接</el-button></template></el-table-column></el-table></el-card>
    <el-alert v-if="section === 'purge'" title="Purge 当前没有对应的后端操作契约，页面不提供伪造的执行按钮。" type="info" show-icon :closable="false" class="mt-4" />
  </main>
</template>
