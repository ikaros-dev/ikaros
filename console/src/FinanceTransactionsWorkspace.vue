<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElAlert, ElEmpty, ElTable, ElTableColumn, ElTag } from 'element-plus'
import { api, type FinanceLedgerRecord, type FinanceTransactionRecord } from './services/api'
const actorId = String(import.meta.env.VITE_ACTOR_ID || '')
const ledgers = ref<FinanceLedgerRecord[]>([]); const transactions = ref<FinanceTransactionRecord[]>([]); const error = ref(''); const loading = ref(false)
function date(value?: string) { return value ? new Date(value).toLocaleString('zh-CN') : '暂无时间' }
async function load() { if (!actorId) { error.value = '未配置当前用户身份，无法加载交易'; return }; loading.value = true; error.value = ''; try { ledgers.value = await api.listFinanceLedgers(actorId); const ledger = ledgers.value[0]; transactions.value = ledger ? await api.listFinanceTransactions(ledger.id, actorId) : [] } catch (e) { transactions.value = []; error.value = e instanceof Error ? e.message : '交易 API 加载失败' } finally { loading.value = false } }
onMounted(load)
</script>
<template><main class="catalog-main finance-main"><header class="dashboard-header"><div><p class="eyebrow">个人记账</p><h1>交易</h1><p>交易记录来自 Finance 服务端接口，不在浏览器本地生成或缓存。</p></div><el-button :loading="loading" @click="load">刷新</el-button></header><el-alert v-if="error" :title="error" type="warning" show-icon :closable="false" /><el-table v-loading="loading" :data="transactions" stripe empty-text="当前账本暂无交易"><el-table-column prop="payee" label="收付款方" min-width="180" /><el-table-column prop="note" label="备注" min-width="220" /><el-table-column prop="type" label="类型" width="130"><template #default="{ row }"><el-tag type="info">{{ row.type || '未标记' }}</el-tag></template></el-table-column><el-table-column label="金额" width="150"><template #default="{ row }">{{ row.currency || '' }} {{ row.amount ?? '暂无' }}</template></el-table-column><el-table-column label="发生时间" width="190"><template #default="{ row }">{{ date(row.occurredAt || row.occurred_at) }}</template></el-table-column></el-table><el-empty v-if="!loading && !transactions.length && !error" description="暂无交易数据" /></main></template>
