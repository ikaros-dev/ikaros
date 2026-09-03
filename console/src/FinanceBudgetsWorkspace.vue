<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElAlert, ElEmpty, ElTable, ElTableColumn } from 'element-plus'
import { api, type FinanceBudgetRecord, type FinanceLedgerRecord } from './services/api'
const actorId = String(import.meta.env.VITE_ACTOR_ID || ''); const month = new Date().toISOString().slice(0, 7); const ledgers = ref<FinanceLedgerRecord[]>([]); const budgets = ref<FinanceBudgetRecord[]>([]); const error = ref(''); const loading = ref(false)
async function load() { if (!actorId) { error.value = '未配置当前用户身份，无法加载预算'; return }; loading.value = true; error.value = ''; try { ledgers.value = await api.listFinanceLedgers(actorId); const ledger = ledgers.value[0]; budgets.value = ledger ? await api.listFinanceBudgets(ledger.id, month, actorId) : [] } catch (e) { budgets.value = []; error.value = e instanceof Error ? e.message : '预算 API 加载失败' } finally { loading.value = false } }
onMounted(load)
</script>
<template><main class="catalog-main finance-main"><header class="dashboard-header"><div><p class="eyebrow">个人记账</p><h1>预算与周期账</h1><p>显示当前账本 {{ month }} 月的服务端预算数据。</p></div><el-button :loading="loading" @click="load">刷新</el-button></header><el-alert v-if="error" :title="error" type="warning" show-icon :closable="false" /><el-table v-loading="loading" :data="budgets" stripe empty-text="当前月份暂无预算"><el-table-column prop="categoryId" label="分类" min-width="220" /><el-table-column prop="month" label="月份" width="140" /><el-table-column prop="budget" label="预算" width="150" /><el-table-column prop="actual" label="已使用" width="150" /><el-table-column prop="remaining" label="剩余" width="150" /></el-table><el-empty v-if="!loading && !budgets.length && !error" description="暂无预算数据" /></main></template>
