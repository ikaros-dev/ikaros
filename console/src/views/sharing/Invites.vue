<script setup lang="ts">
import { onMounted, ref } from "vue";
import { http } from "@/utils/http";

type Invite = { id: string; roomId: string; issuerId: string; role: string; status: string; expiresAt?: string; createdAt?: string };
const invites = ref<Invite[]>([]); const loading = ref(false); const busy = ref(""); const error = ref(""); const message = ref("");
function formatDate(value?: string) { return value ? new Date(value).toLocaleString() : "未设置"; }
function statusType(status: string) { return status === "ACCEPTED" ? "success" : status === "PENDING" ? "warning" : "info"; }
async function load() { loading.value = true; error.value = ""; try { const result = await http.get<unknown, unknown>("/invites"); invites.value = Array.isArray(result) ? result as Invite[] : []; } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "邀请加载失败"; } finally { loading.value = false; } }
async function action(invite: Invite, name: "accept" | "decline") { busy.value = invite.id; error.value = ""; try { await http.post(`/invites/${invite.id}/actions/${name}`); message.value = name === "accept" ? "邀请已接受，已加入 Room" : "邀请已拒绝"; await load(); } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "邀请操作失败"; } finally { busy.value = ""; } }
onMounted(load);
</script>
<template>
  <main class="p-4 md:p-6"><div class="flex justify-between items-start mb-6"><div><h1 class="text-2xl font-semibold">我的邀请</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">查看、接受或拒绝协作 Room 邀请。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4"/><el-alert v-if="message" :title="message" type="success" show-icon :closable="false" class="mb-4"/>
    <el-card shadow="never"><el-skeleton v-if="loading" :rows="5" animated/><el-empty v-else-if="!invites.length" description="暂无邀请"/><el-table v-else :data="invites" stripe><el-table-column prop="roomId" label="Room" min-width="280"/><el-table-column prop="issuerId" label="邀请人" min-width="280"/><el-table-column prop="role" label="角色" width="120"/><el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ row.status }}</el-tag></template></el-table-column><el-table-column label="过期时间" min-width="180"><template #default="{ row }">{{ formatDate(row.expiresAt) }}</template></el-table-column><el-table-column label="操作" width="180"><template #default="{ row }"><el-button v-if="row.status === 'PENDING'" type="primary" link :loading="busy === row.id" @click="action(row, 'accept')">接受</el-button><el-button v-if="row.status === 'PENDING'" type="danger" link :loading="busy === row.id" @click="action(row, 'decline')">拒绝</el-button></template></el-table-column></el-table></el-card>
  </main>
</template>
