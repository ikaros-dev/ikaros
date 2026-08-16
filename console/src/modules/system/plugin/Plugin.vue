<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import {
	Delete,
	Document,
	Refresh,
	RefreshLeft,
	Top,
	Search,
} from '@element-plus/icons-vue';
import {
	Plugin,
	V1PluginApiOperatePluginStateByIdRequest,
} from '@runikaros/api-client';

import PluginUploadDrawer from './PluginUploadDrawer.vue';
import type { AxiosResponse } from 'axios';
import { computed, nextTick, onMounted, ref } from 'vue';
import {
	ElAvatar,
	ElButton,
	ElButtonGroup,
	ElCol,
	ElInput,
	ElMessage,
	ElMessageBox,
	ElPagination,
	ElRow,
	ElSegmented,
	ElTable,
	ElTableColumn,
	ElTag,
} from 'element-plus';
import router from '@/router';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

interface PluginSearch {
	page: number;
	size: number;
	total: number;
	lastPage?: boolean;
	firstPage?: boolean;
	hasPrevious?: boolean;
	hasNext?: boolean;
	name: string;
}

const defaultPluginLogoUrl = 'https://ikaros.run/logo.png';

const pluginSearch = ref<PluginSearch>({
	page: 1,
	size: 10,
	total: 1,
	name: '',
});

const onCurrentPageChange = (val: number) => {
	pluginSearch.value.page = val;
	getPluginsFromServer();
};

const plugins = ref<Plugin[]>();
const pluginTableRef = ref<InstanceType<typeof ElTable>>();
const selectedPluginName = ref<string>();
const expandedPluginNames = computed(() =>
	selectedPluginName.value ? [selectedPluginName.value] : []
);

const runtimeOptions = [
	{ label: t('module.plugin.table.operate.start'), value: 'START' },
	{ label: t('module.plugin.table.operate.stop'), value: 'STOP' },
];
const availabilityOptions = [
	{ label: t('module.plugin.table.operate.enable'), value: 'ENABLE' },
	{ label: t('module.plugin.table.operate.disable'), value: 'DISABLE' },
];

const onCurrentPluginChange = (plugin?: Plugin) => {
	selectedPluginName.value = plugin?.name as string | undefined;
};

const runtimeValue = (plugin: Plugin) =>
	plugin.state === 'STARTED' ? 'START' : 'STOP';

const availabilityValue = (plugin: Plugin) =>
	plugin.state === 'DISABLED' ? 'DISABLE' : 'ENABLE';

const runtimeLabel = (plugin: Plugin) => {
	if (plugin.state === 'DISABLED') {
		return '';
	}
	if (plugin.state === 'STARTED') {
		return t('module.plugin.table.state.started');
	}
	if (plugin.state === 'FAILED') {
		return t('module.plugin.table.state.failed');
	}
	return t('module.plugin.table.state.stopped');
};

const runtimeTagType = (plugin: Plugin) => {
	if (plugin.state === 'STARTED') {
		return 'success';
	}
	if (plugin.state === 'FAILED') {
		return 'danger';
	}
	return 'info';
};

const getPluginsFromServer = async () => {
	const { data } = await apiClient.plugin.getPluginsByPaging({
		page: pluginSearch.value.page + '',
		size: pluginSearch.value.size + '',
	});
	plugins.value = data.items as Plugin[];
	const firstPlugin = plugins.value[0];
	selectedPluginName.value = firstPlugin?.name as string | undefined;
	await nextTick();
	pluginTableRef.value?.setCurrentRow(firstPlugin);
	pluginSearch.value.page = data.page;
	pluginSearch.value.size = data.size;
	pluginSearch.value.total = data.total;
	pluginSearch.value.hasNext = data.hasNext;
	pluginSearch.value.hasPrevious = data.hasPrevious;
};

const delegationPluginStateOperator = async (
	requestParameters: V1PluginApiOperatePluginStateByIdRequest
): Promise<AxiosResponse<string, any>> => {
	return await apiClient.corePlugin.operatePluginStateById(requestParameters);
};

const startPlugin = async (pluginName: string | undefined) => {
	ElMessageBox.confirm(
		t('module.plugin.operate.plugin.start.confirm.title'),
		t('module.plugin.operate.plugin.start.confirm.warning'),
		{
			confirmButtonText: t(
				'module.plugin.operate.plugin.start.confirm.confirm'
			),
			cancelButtonText: t('module.plugin.operate.plugin.start.confirm.cancel'),
			type: 'warning',
		}
	)
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'START',
			})
				.then(() => {
					ElMessage.success(
						t('module.plugin.operate.plugin.start.success', {
							name: pluginName,
						})
					);
					window.location.reload();
				})
				.catch((err) => {
					ElMessage.error(err.message);
					console.log(err);
				});
		})
		.catch(() => {
			ElMessage({
				type: 'info',
				message: t('module.plugin.operate.plugin.start.cancel'),
			});
		});
};

const stopPlugin = (pluginName: string | undefined) => {
	ElMessageBox.confirm(
		t('module.plugin.operate.plugin.stop.confirm.title'),
		t('module.plugin.operate.plugin.stop.confirm.warning'),
		{
			confirmButtonText: t('module.plugin.operate.plugin.stop.confirm.confirm'),
			cancelButtonText: t('module.plugin.operate.plugin.stop.confirm.cancel'),
			type: 'warning',
		}
	)
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'STOP',
			})
				.then(() => {
					ElMessage.success(
						t('module.plugin.operate.plugin.stop.success', { name: pluginName })
					);
					window.location.reload();
				})
				.catch((err) => {
					ElMessage.error(err.message);
					console.log(err);
				});
		})
		.catch(() => {
			ElMessage({
				type: 'info',
				message: t('module.plugin.operate.plugin.stop.cancel'),
			});
		});
};

const enablePlugin = (pluginName: string | undefined) => {
	ElMessageBox.confirm(
		t('module.plugin.operate.plugin.enable.confirm.title'),
		t('module.plugin.operate.plugin.enable.confirm.warning'),
		{
			confirmButtonText: t(
				'module.plugin.operate.plugin.enable.confirm.confirm'
			),
			cancelButtonText: t('module.plugin.operate.plugin.enable.confirm.cancel'),
			type: 'warning',
		}
	)
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'ENABLE',
			})
				.then(() => {
					ElMessage.success(
						t('module.plugin.operate.plugin.enable.success', {
							name: pluginName,
						})
					);
					window.location.reload();
				})
				.catch((err) => {
					ElMessage.error(err.message);
					console.log(err);
				});
		})
		.catch(() => {
			ElMessage({
				type: 'info',
				message: t('module.plugin.operate.plugin.enable.cancel'),
			});
		});
};

const disablePlugin = (pluginName: string | undefined) => {
	ElMessageBox.confirm(
		t('module.plugin.operate.plugin.disable.confirm.title'),
		t('module.plugin.operate.plugin.disable.confirm.warning'),
		{
			confirmButtonText: t(
				'module.plugin.operate.plugin.disable.confirm.confirm'
			),
			cancelButtonText: t(
				'module.plugin.operate.plugin.disable.confirm.cancel'
			),
			type: 'warning',
		}
	)
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'DISABLE',
			})
				.then(() => {
					ElMessage.success(
						t('module.plugin.operate.plugin.disable.success', {
							name: pluginName,
						})
					);
					window.location.reload();
				})
				.catch((err) => {
					ElMessage.error(err.message);
					console.log(err);
				});
		})
		.catch(() => {
			ElMessage({
				type: 'info',
				message: t('module.plugin.operate.plugin.disable.cancel'),
			});
		});
};

const reloadPlugin = (pluginName: string | undefined) => {
	ElMessageBox.confirm(
		t('module.plugin.operate.plugin.reload.confirm.title'),
		t('module.plugin.operate.plugin.reload.confirm.warning'),
		{
			confirmButtonText: t(
				'module.plugin.operate.plugin.reload.confirm.confirm'
			),
			cancelButtonText: t('module.plugin.operate.plugin.reload.confirm.cancel'),
			type: 'warning',
		}
	)
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'RELOAD',
			})
				.then(() => {
					ElMessage.success(
						t('module.plugin.operate.plugin.reload.success', {
							name: pluginName,
						})
					);
					window.location.reload();
				})
				.catch((err) => {
					ElMessage.error(err.message);
					console.log(err);
				});
		})
		.catch(() => {
			ElMessage({
				type: 'info',
				message: t('module.plugin.operate.plugin.reload.cancel'),
			});
		});
};

const reloadAllPlugin = () => {
	ElMessageBox.confirm(
		t('module.plugin.operate.plugin.reload-all.confirm.title'),
		t('module.plugin.operate.plugin.reload-all.confirm.warning'),
		{
			confirmButtonText: t(
				'module.plugin.operate.plugin.reload-all.confirm.confirm'
			),
			cancelButtonText: t(
				'module.plugin.operate.plugin.reload-all.confirm.cancel'
			),
			type: 'warning',
		}
	)
		.then(() => {
			delegationPluginStateOperator({
				name: 'ALL',
				operate: 'RELOAD_ALL',
			})
				.then(() => {
					ElMessage.success(
						t('module.plugin.operate.plugin.reload-all.success')
					);
					window.location.reload();
				})
				.catch((err) => {
					ElMessage.error(err.message);
					console.log(err);
				});
		})
		.catch(() => {
			ElMessage({
				type: 'info',
				message: t('module.plugin.operate.plugin.reload-all.cancel'),
			});
		});
};

const deletePlugin = (pluginName: string | undefined) => {
	ElMessageBox.confirm(
		t('module.plugin.operate.plugin.delete.confirm.title'),
		t('module.plugin.operate.plugin.delete.confirm.warning'),
		{
			confirmButtonText: t(
				'module.plugin.operate.plugin.delete.confirm.confirm'
			),
			cancelButtonText: t('module.plugin.operate.plugin.delete.confirm.cancel'),
			type: 'warning',
		}
	)
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'DELETE',
			})
				.then(() => {
					ElMessage.success(
						t('module.plugin.operate.plugin.delete.success', {
							name: pluginName,
						})
					);
					window.location.reload();
				})
				.catch((err) => {
					ElMessage.error(err.message);
					console.log(err);
				});
		})
		.catch(() => {
			ElMessage({
				type: 'info',
				message: t('module.plugin.operate.plugin.delete.cancel'),
			});
		});
};

const upgradePlugin = (plugin: Plugin) => {
	ElMessageBox.confirm(
		t('module.plugin.operate.plugin.upgrade.confirm.title'),
		t('module.plugin.operate.plugin.upgrade.confirm.warning'),
		{
			confirmButtonText: t(
				'module.plugin.operate.plugin.upgrade.confirm.confirm'
			),
			cancelButtonText: t(
				'module.plugin.operate.plugin.upgrade.confirm.cancel'
			),
			type: 'warning',
		}
	)
		.then(() => {
			// console.log('upgrade plugin.', plugin);
			pluginUploadDrawerVisible.value = true;
			pluginUploadDrawerUpgradePlugin.value = plugin;
		})
		.catch(() => {
			ElMessage({
				type: 'info',
				message: t('module.plugin.operate.plugin.upgrade.cancel'),
			});
		});
};

const pluginUploadDrawerVisible = ref(false);
const pluginUploadDrawerUpgradePlugin = ref<Plugin>();

const onPluginUploadDrawerClose = () => {
	pluginUploadDrawerVisible.value = false;
	window.location.reload();
};

const toPluginDetails = (pluginName: string) => {
	router.push('/plugin/' + pluginName + '/details');
};

const changeRuntimeState = (
	value: string | number | boolean,
	plugin: Plugin
) => {
	if (value === 'START') {
		startPlugin(plugin.name);
	} else {
		stopPlugin(plugin.name);
	}
};

const changeAvailabilityState = (
	value: string | number | boolean,
	plugin: Plugin
) => {
	if (value === 'ENABLE') {
		enablePlugin(plugin.name);
	} else {
		disablePlugin(plugin.name);
	}
};

onMounted(getPluginsFromServer);
</script>

<template>
	<PluginUploadDrawer
		v-model:visible="pluginUploadDrawerVisible"
		v-model:upgradePlugin="pluginUploadDrawerUpgradePlugin"
		@close="onPluginUploadDrawerClose"
	/>

	<el-row :gutter="10">
		<el-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
			<el-input
				v-model="pluginSearch.name"
				style="width: 100%"
				size="large"
				:placeholder="t('module.plugin.search.placeholder.name')"
				:prefix-icon="Search"
				disabled
			/>
		</el-col>

		<el-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
			<el-pagination
				v-model:page-size="pluginSearch.size"
				v-model:current-page="pluginSearch.page"
				background
				:total="pluginSearch.total"
				:disabled="pluginSearch.total < pluginSearch.size"
				layout="prev, pager, nex"
				style="vertical-align: middle; line-height: 40px; height: 40px"
				@current-change="onCurrentPageChange"
			/>
		</el-col>

		<el-col
			:xs="24"
			:sm="24"
			:md="24"
			:lg="8"
			:xl="8"
			style="text-align: right"
		>
			<el-button plain @click="reloadAllPlugin">
				{{ t('module.plugin.search.button.reload-all') }}
			</el-button>

			<el-button plain @click="pluginUploadDrawerVisible = true">
				{{ t('module.plugin.search.button.install-plugin') }}
			</el-button>
		</el-col>
	</el-row>

	<el-table
		ref="pluginTableRef"
		:data="plugins"
		row-key="name"
		highlight-current-row
		:expand-row-keys="expandedPluginNames"
		style="width: 100%"
		@current-change="onCurrentPluginChange"
	>
		<el-table-column
			type="expand"
			width="1"
			class-name="plugin-expand-column"
			label-class-name="plugin-expand-column"
		>
			<template #default="scope">
				<div class="plugin-actions">
					<el-button
						plain
						:icon="Document"
						@click="toPluginDetails(scope.row.name)"
					>
						{{ t('module.plugin.table.operate.details') }}
					</el-button>

					<el-segmented
						:model-value="runtimeValue(scope.row)"
						:options="runtimeOptions"
						:disabled="scope.row.state === 'DISABLED'"
						@change="changeRuntimeState($event, scope.row)"
					/>

					<el-segmented
						:model-value="availabilityValue(scope.row)"
						:options="availabilityOptions"
						@change="changeAvailabilityState($event, scope.row)"
					/>

					<el-button-group>
						<el-button :icon="Refresh" @click="reloadPlugin(scope.row.name)">
							{{ t('module.plugin.table.operate.reload') }}
						</el-button>
						<el-button :icon="Top" @click="upgradePlugin(scope.row)">
							{{ t('module.plugin.table.operate.upgrade') }}
						</el-button>
					</el-button-group>

					<el-button-group>
						<el-button
							plain
							type="danger"
							:icon="Delete"
							@click="deletePlugin(scope.row.name)"
						>
							{{ t('module.plugin.table.operate.delete') }}
						</el-button>
						<el-button disabled :icon="RefreshLeft">
							{{ t('module.plugin.table.operate.reset') }}
						</el-button>
					</el-button-group>
				</div>
			</template>
		</el-table-column>

		<el-table-column
			prop="logo"
			:label="t('module.plugin.table.label.icon')"
			width="80"
		>
			<template #default="scope">
				<el-avatar
					shape="square"
					:size="40"
					:src="scope.row.logo ? scope.row.logo : defaultPluginLogoUrl"
				/>
			</template>
		</el-table-column>

		<el-table-column prop="name" :label="t('common.label.id')" width="150" />
		<el-table-column
			prop="displayName"
			:label="t('module.plugin.table.label.name')"
			width="150"
		/>
		<el-table-column
			prop="author.name"
			:label="t('module.plugin.table.label.author')"
			width="200"
		>
			<template #default="scope">
				<a
					v-if="scope?.row?.author?.website"
					:href="scope.row.author.website"
					target="_blank"
				>
					{{ scope.row.author.name }}
				</a>
				<span v-else>{{ scope.row.author.name }}</span>
			</template>
		</el-table-column>
		<el-table-column
			prop="description"
			:label="t('module.plugin.table.label.description')"
		/>
		<el-table-column
			prop="state"
			:label="t('module.plugin.table.label.runtime')"
			align="center"
			width="110"
		>
			<template #default="scope">
				<el-tag
					v-if="scope.row.state !== 'DISABLED'"
					:type="runtimeTagType(scope.row)"
					effect="plain"
				>
					{{ runtimeLabel(scope.row) }}
				</el-tag>
			</template>
		</el-table-column>
		<el-table-column
			align="center"
			:label="t('module.plugin.table.label.availability')"
			width="110"
		>
			<template #default="scope">
				<el-tag
					:type="scope.row.state === 'DISABLED' ? 'info' : 'success'"
					effect="plain"
				>
					{{
						t(
							scope.row.state === 'DISABLED'
								? 'module.plugin.table.state.disabled'
								: 'module.plugin.table.state.enabled'
						)
					}}
				</el-tag>
			</template>
		</el-table-column>
	</el-table>
</template>

<style lang="scss" scoped>
.plugin-actions {
	display: flex;
	flex-wrap: wrap;
	align-items: center;
	gap: 16px;
}

:deep(.plugin-expand-column) {
	padding: 0 !important;
}

:deep(.plugin-expand-column .cell) {
	width: 0;
	padding: 0;
	overflow: hidden;
}

:deep(.el-table__expanded-cell) {
	padding: 12px;
}
</style>
