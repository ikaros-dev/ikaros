<script setup lang="ts">
import { onMounted, ref } from 'vue';
import {
	ElButton,
	ElDialog,
	ElInput,
	ElMessage,
	ElMessageBox,
	ElPagination,
	ElSwitch,
	ElTable,
	ElTableColumn,
	ElTag,
} from 'element-plus';
import { Plus, Edit, Delete, View, Search } from '@element-plus/icons-vue';
import { apiClient } from '@/utils/api-client';
import { useI18n } from 'vue-i18n';
import { episodeGroupLabelMap } from '@/modules/common/constants';
import RegularRuleEditDialog from './RegularRuleEditDialog.vue';
import RegularRuleDetailDrawer from './RegularRuleDetailDrawer.vue';

const { t } = useI18n();

const rules = ref<any[]>([]);
const loading = ref(false);
const page = ref(1);
const size = ref(10);
const total = ref(0);

const dialogVisible = ref(false);
const editingRule = ref<any>(null);

const detailDrawerVisible = ref(false);
const viewingRule = ref<any>(null);

const matchDialogVisible = ref(false);
const matchFileName = ref('');
const matchResult = ref<any>(null);
const matchLoading = ref(false);

const handleMatchTest = () => {
	matchFileName.value = '';
	matchResult.value = null;
	matchDialogVisible.value = true;
};

const handleMatchSubmit = async () => {
	if (!matchFileName.value.trim()) return;
	matchLoading.value = true;
	matchResult.value = null;
	try {
		const { data } = await apiClient.episode.matchEpisodeSequenceRegular({
			attachmentName: matchFileName.value.trim(),
		});
		matchResult.value = data;
	} catch (e) {
		console.error('Match failed', e);
		ElMessage.error(t('module.regular.match.failed'));
	} finally {
		matchLoading.value = false;
	}
};

const fetchRules = async () => {
	loading.value = true;
	try {
		const { data } = await apiClient.episode.listEpisodeSequenceRegulars({
			page: page.value,
			size: size.value,
		});
		rules.value = data.items || [];
		total.value = data.total || 0;
	} catch (e) {
		console.error('Failed to fetch rules', e);
	} finally {
		loading.value = false;
	}
};

const handleAdd = () => {
	editingRule.value = null;
	dialogVisible.value = true;
};

const handleEdit = (rule: any) => {
	editingRule.value = { ...rule };
	dialogVisible.value = true;
};

const handleDelete = async (rule: any) => {
	try {
		await ElMessageBox.confirm(
			t('module.regular.delete.confirm.message', { name: rule.name }),
			t('module.regular.delete.confirm.title'),
			{
				confirmButtonText: t('common.button.confirm'),
				cancelButtonText: t('common.button.cancel'),
				type: 'warning',
			}
		);
		await apiClient.episode.deleteEpisodeSequenceRegular({ id: rule.id });
		ElMessage.success(
			t('module.regular.success.delete', { name: rule.name })
		);
		await fetchRules();
	} catch (e) {
		// cancelled or error
	}
};

const handleEnableChange = async (rule: any) => {
	try {
		await apiClient.episode.updateEpisodeSequenceRegular({
			episodeSequenceRegular: {
				id: rule.id,
				name: rule.name,
				regex: rule.regex,
				epGroup: rule.epGroup,
				sequence: rule.sequence,
				priority: rule.priority,
				description: rule.description,
				enabled: rule.enabled,
			},
		});
		ElMessage.success(
			rule.enabled
				? t('module.regular.enable.success', { name: rule.name })
				: t('module.regular.disable.success', { name: rule.name })
		);
	} catch (e) {
		// revert on failure
		rule.enabled = !rule.enabled;
	}
};

const handleDialogClose = () => {
	dialogVisible.value = false;
	editingRule.value = null;
};

const handleShowDetail = (rule: any) => {
	viewingRule.value = rule;
	detailDrawerVisible.value = true;
};

const handleDetailDrawerClose = () => {
	detailDrawerVisible.value = false;
	viewingRule.value = null;
};

const handleDialogSuccess = () => {
	dialogVisible.value = false;
	editingRule.value = null;
	fetchRules();
};

const handlePageChange = (val: number) => {
	page.value = val;
	fetchRules();
};

const handleSizeChange = (val: number) => {
	size.value = val;
	page.value = 1;
	fetchRules();
};

const getEpGroupLabel = (group: string) => {
	return episodeGroupLabelMap.get(group) || group || '-';
};

const truncateDesc = (desc: string, maxLen = 30) => {
	if (!desc) return '-';
	return desc.length > maxLen ? desc.slice(0, maxLen) + '…' : desc;
};

onMounted(() => {
	fetchRules();
});
</script>

<template>
	<div class="regular-rules">
		<div class="page-header">
			<span class="page-title">{{ t('module.regular.page.title') }}</span>
			<div class="header-buttons">
				<el-button :icon="Search" @click="handleMatchTest">
					{{ t('module.regular.match.button') }}
				</el-button>
				<el-button type="primary" :icon="Plus" @click="handleAdd">
					{{ t('module.regular.add') }}
				</el-button>
			</div>
		</div>

		<el-table
			:data="rules"
			v-loading="loading"
			stripe
			border
			style="width: 100%"
		>
			<el-table-column
				:label="t('module.regular.table.column.name')"
				prop="name"
				min-width="160"
				show-overflow-tooltip
			/>
			<el-table-column
				:label="t('module.regular.table.column.regex')"
				prop="regex"
				min-width="200"
				show-overflow-tooltip
			>
				<template #default="{ row }">
					<el-tag type="info" effect="plain" style="font-family: monospace">
						{{ row.regex }}
					</el-tag>
				</template>
			</el-table-column>
			<el-table-column
				:label="t('module.regular.table.column.epGroup')"
				prop="epGroup"
				width="150"
			>
				<template #default="{ row }">
					{{ getEpGroupLabel(row.epGroup) }}
				</template>
			</el-table-column>
			<el-table-column
				:label="t('module.regular.table.column.sequence')"
				prop="sequence"
				width="80"
				align="center"
			>
				<template #default="{ row }">
					{{ row.sequence ?? '-' }}
				</template>
			</el-table-column>
			<el-table-column
				:label="t('module.regular.table.column.priority')"
				prop="priority"
				width="80"
				align="center"
			/>
			<el-table-column
				:label="t('module.regular.table.column.description')"
				min-width="180"
				show-overflow-tooltip
			>
				<template #default="{ row }">
					{{ row.description ? truncateDesc(row.description) : '-' }}
				</template>
			</el-table-column>
			<el-table-column
				:label="t('module.regular.table.column.enabled')"
				width="80"
				align="center"
			>
				<template #default="{ row }">
					<el-switch
						v-model="row.enabled"
						@change="handleEnableChange(row)"
					/>
				</template>
			</el-table-column>
			<el-table-column
				:label="t('module.regular.table.column.actions')"
				width="220"
				fixed="right"
				align="center"
			>
				<template #default="{ row }">
					<el-button
						:icon="View"
						size="small"
						type="info"
						link
						@click="handleShowDetail(row)"
					>
						{{ t('common.button.detail') }}
					</el-button>
					<el-button
						:icon="Edit"
						size="small"
						type="primary"
						link
						@click="handleEdit(row)"
					>
						{{ t('common.button.edit') }}
					</el-button>
					<el-button
						:icon="Delete"
						size="small"
						type="danger"
						link
						@click="handleDelete(row)"
					>
						{{ t('common.button.delete') }}
					</el-button>
				</template>
			</el-table-column>
		</el-table>

		<div class="pagination-wrapper" v-if="total > 0">
			<el-pagination
				v-model:current-page="page"
				v-model:page-size="size"
				:total="total"
				:page-sizes="[10, 20, 50, 100]"
				layout="total, sizes, prev, pager, next, jumper"
				@current-change="handlePageChange"
				@size-change="handleSizeChange"
			/>
		</div>

		<RegularRuleEditDialog
			v-model:visible="dialogVisible"
			:rule="editingRule"
			@close="handleDialogClose"
			@success="handleDialogSuccess"
		/>

		<RegularRuleDetailDrawer
			v-model:visible="detailDrawerVisible"
			:rule="viewingRule"
			@close="handleDetailDrawerClose"
		/>

		<el-dialog
			v-model="matchDialogVisible"
			:title="t('module.regular.match.title')"
			width="500px"
			:close-on-click-modal="false"
		>
			<el-input
				v-model="matchFileName"
				:placeholder="t('module.regular.match.placeholder')"
				clearable
				@keyup.enter="handleMatchSubmit"
			/>
			<div style="margin-top: 12px; text-align: right">
				<el-button type="primary" :loading="matchLoading" @click="handleMatchSubmit">
					{{ t('module.regular.match.submit') }}
				</el-button>
			</div>

			<template v-if="matchResult">
				<el-divider />
				<div class="match-result">
					<div class="match-result-item">
						<span class="match-result-label">{{ t('module.regular.match.result.matched') }}</span>
						<span>
							<el-tag :type="matchResult.matched ? 'success' : 'danger'">
								{{ matchResult.matched ? '✔' : '✘' }}
							</el-tag>
						</span>
					</div>
					<div class="match-result-item">
						<span class="match-result-label">{{ t('module.regular.match.result.ruleName') }}</span>
						<span>{{ matchResult.matchedRuleName || '-' }}</span>
					</div>
					<div class="match-result-item">
						<span class="match-result-label">{{ t('module.regular.match.result.regex') }}</span>
						<span>
							<el-tag v-if="matchResult.matchedRegex" type="info" effect="plain" style="font-family: monospace">
								{{ matchResult.matchedRegex }}
							</el-tag>
							<span v-else>-</span>
						</span>
					</div>
					<div class="match-result-item">
						<span class="match-result-label">{{ t('module.regular.match.result.epGroup') }}</span>
						<span>{{ matchResult.epGroup ? getEpGroupLabel(matchResult.epGroup) : '-' }}</span>
					</div>
					<div class="match-result-item">
						<span class="match-result-label">{{ t('module.regular.match.result.sequence') }}</span>
						<span>{{ matchResult.sequence ?? '-' }}</span>
					</div>
				</div>
			</template>
		</el-dialog>
	</div>
</template>

<style lang="scss" scoped>
.regular-rules {
	.page-header {
		margin-bottom: 16px;
		display: flex;
		justify-content: space-between;
		align-items: center;

		.page-title {
			font-size: 16px;
			font-weight: 600;
			color: var(--el-text-color-primary);
		}
	}

	.header-buttons {
		display: flex;
		gap: 8px;
	}

	.pagination-wrapper {
		margin-top: 16px;
		display: flex;
		justify-content: center;
	}

	.match-result {
		display: flex;
		flex-direction: column;
		gap: 10px;

		.match-result-item {
			display: flex;
			align-items: center;

			.match-result-label {
				width: 80px;
				flex-shrink: 0;
				color: var(--el-text-color-secondary);
				font-size: 14px;
			}
		}
	}
}
</style>
