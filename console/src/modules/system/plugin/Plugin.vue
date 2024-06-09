<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { ArrowDown, More, Search } from '@element-plus/icons-vue';
import {
	Plugin,
	V1alpha1PluginApiOperatePluginStateByIdRequest,
} from '@runikaros/api-client';
// eslint-disable-next-line no-unused-vars
import PluginUploadDrawer from './PluginUploadDrawer.vue';
import type { AxiosResponse } from 'axios';
import { onMounted, ref } from 'vue';
import {
	ElAvatar,
	ElButton,
	ElCol,
	ElDropdown,
	ElDropdownItem,
	ElDropdownMenu,
	ElIcon,
	ElInput,
	ElMessage,
	ElMessageBox,
	ElPagination,
	ElRow,
	ElTable,
	ElTableColumn,
} from 'element-plus';
import router from '@/router';
import { useI18n } from 'vue-i18n';

const {t} = useI18n();

interface PluginSearch {
	page: number;
	size: number;
	total: number;
	lastPage?: boolean;
	firstPage?: boolean;
	hasPrevious?: boolean;
	hasNext?: boolean;
	name: string;
	state: string;
}

const defaultPluginLogoUrl = 'https://ikaros.run/logo.png';

const pluginSearch = ref<PluginSearch>({
	page: 1,
	size: 10,
	total: 1,
	name: '',
	state: 'ALL',
});

const onCurrentPageChange = (val: number) => {
	pluginSearch.value.page = val;
	getPluginsFromServer();
};

// eslint-disable-next-line no-unused-vars
const stateStrMap: Map<string, string> = new Map([
	['ALL', t('module.plugin.label.state.all')],
	['STARTED', t('module.plugin.label.state.started')],
	['STOPPED', t('module.plugin.label.state.stopped')],
	['CREATED', t('module.plugin.label.state.created')],
	['DISABLED', t('module.plugin.label.state.disabled')],
	['RESOLVED', t('module.plugin.label.state.resolved')],
	['FAILED', t('module.plugin.label.state.failed')],
]);

const onPluginSearchStateChange = (command: string | number | object) => {
	pluginSearch.value.state = command as string;
};

const filterPluginSearchState = (): string | undefined => {
	const state: string = pluginSearch.value.state as string;
	return stateStrMap.get(state);
};

const plugins = ref<Plugin[]>();
const getPluginsFromServer = async () => {
	const { data } = await apiClient.plugin.getPluginsByPaging({
		page: pluginSearch.value.page + '',
		size: pluginSearch.value.size + '',
	});
	plugins.value = data.items as Plugin[];
	pluginSearch.value.page = data.page;
	pluginSearch.value.size = data.size;
	pluginSearch.value.total = data.total;
	pluginSearch.value.hasNext = data.hasNext;
	pluginSearch.value.hasPrevious = data.hasPrevious;
};

const delegationPluginStateOperator = async (
	requestParameters: V1alpha1PluginApiOperatePluginStateByIdRequest
): Promise<AxiosResponse<string, any>> => {
	return await apiClient.corePlugin.operatePluginStateById(requestParameters);
};

const startPlugin = async (pluginName: string | undefined) => {
	ElMessageBox.confirm(t('module.plugin.operate.plugin.start.confirm.title'), t('module.plugin.operate.plugin.start.confirm.warning'), {
		confirmButtonText: t('module.plugin.operate.plugin.start.confirm.confirm'),
		cancelButtonText: t('module.plugin.operate.plugin.start.confirm.cancel'),
		type: 'warning',
	})
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'START',
			})
				.then(() => {
					ElMessage.success(t('module.plugin.operate.plugin.start.success', {name: pluginName}));
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
	ElMessageBox.confirm(t('module.plugin.operate.plugin.stop.confirm.title'), t('module.plugin.operate.plugin.stop.confirm.warning'), {
		confirmButtonText: t('module.plugin.operate.plugin.stop.confirm.confirm'),
		cancelButtonText: t('module.plugin.operate.plugin.stop.confirm.cancel'),
		type: 'warning',
	})
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'STOP',
			})
				.then(() => {
					ElMessage.success(t('module.plugin.operate.plugin.stop.success', {name: pluginName}));
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
	ElMessageBox.confirm(t('module.plugin.operate.plugin.enable.confirm.title'), t('module.plugin.operate.plugin.enable.confirm.warning'), {
		confirmButtonText: t('module.plugin.operate.plugin.enable.confirm.confirm'),
		cancelButtonText: t('module.plugin.operate.plugin.enable.confirm.cancel'),
		type: 'warning',
	})
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'ENABLE',
			})
				.then(() => {
					ElMessage.success(t('module.plugin.operate.plugin.enable.success', {name: pluginName}));
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
	ElMessageBox.confirm(t('module.plugin.operate.plugin.disable.confirm.title'), t('module.plugin.operate.plugin.disable.confirm.warning'), {
		confirmButtonText: t('module.plugin.operate.plugin.disable.confirm.confirm'),
		cancelButtonText: t('module.plugin.operate.plugin.disable.confirm.cancel'),
		type: 'warning',
	})
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'DISABLE',
			})
				.then(() => {
					ElMessage.success(t('module.plugin.operate.plugin.disable.success', {name: pluginName}));
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
	ElMessageBox.confirm(t('module.plugin.operate.plugin.reload.confirm.title'), t('module.plugin.operate.plugin.reload.confirm.warning'), {
		confirmButtonText: t('module.plugin.operate.plugin.reload.confirm.confirm'),
		cancelButtonText: t('module.plugin.operate.plugin.reload.confirm.cancel'),
		type: 'warning',
	})
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'RELOAD',
			})
				.then(() => {
					ElMessage.success(t('module.plugin.operate.plugin.reload.success', {name: pluginName}));
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
	ElMessageBox.confirm(t('module.plugin.operate.plugin.reload-all.confirm.title'), t('module.plugin.operate.plugin.reload-all.confirm.warning'), {
		confirmButtonText: t('module.plugin.operate.plugin.reload-all.confirm.confirm'),
		cancelButtonText: t('module.plugin.operate.plugin.reload-all.confirm.cancel'),
		type: 'warning',
	})
		.then(() => {
			delegationPluginStateOperator({
				name: 'ALL',
				operate: 'RELOAD_ALL',
			})
				.then(() => {
					ElMessage.success(t('module.plugin.operate.plugin.reload-all.success'));
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
	ElMessageBox.confirm(t('module.plugin.operate.plugin.delete.confirm.title'), t('module.plugin.operate.plugin.delete.confirm.warning'), {
		confirmButtonText: t('module.plugin.operate.plugin.delete.confirm.confirm'),
		cancelButtonText: t('module.plugin.operate.plugin.delete.confirm.cancel'),
		type: 'warning',
	})
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'DELETE',
			})
				.then(() => {
					ElMessage.success(t('module.plugin.operate.plugin.delete.success', {name: pluginName}));
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

// eslint-disable-next-line no-unused-vars
const upgradePlugin = (plugin: Plugin) => {
	ElMessageBox.confirm(t('module.plugin.operate.plugin.upgrade.confirm.title'), t('module.plugin.operate.plugin.upgrade.confirm.warning'), {
		confirmButtonText: t('module.plugin.operate.plugin.upgrade.confirm.confirm'),
		cancelButtonText: t('module.plugin.operate.plugin.upgrade.confirm.cancel'),
		type: 'warning',
	})
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

			<el-button plain @click="pluginUploadDrawerVisible = true"
				>
				{{ t('module.plugin.search.button.install-plugin') }}
			</el-button>

			&nbsp;&nbsp;
			<el-dropdown
				disabled
				size="large"
				trigger="click"
				style="
					cursor: not-allowed;
					vertical-align: middle;
					line-height: 40px;
					display: inline-block;
				"
				@command="onPluginSearchStateChange"
			>
				<span>
					{{ filterPluginSearchState() }}
					<el-icon>
						<arrow-down />
					</el-icon>
				</span>
				<template #dropdown>
					<el-dropdown-menu>
						<el-dropdown-item command="ALL" disabled> 
							{{ t('module.plugin.search.dropdown.all') }}
						</el-dropdown-item>
						<el-dropdown-item command="STARTED" disabled>
							{{ t('module.plugin.search.dropdown.started') }}
						</el-dropdown-item>
						<el-dropdown-item command="STOPPED" disabled>
							{{ t('module.plugin.search.dropdown.stoppedd') }}
						</el-dropdown-item>
						<el-dropdown-item command="RESOLVED" disabled>
							{{ t('module.plugin.search.dropdown.resolved') }}
						</el-dropdown-item>
						<el-dropdown-item command="DISABLED" disabled>
							{{ t('module.plugin.search.dropdown.disabled') }}
						</el-dropdown-item>
						<el-dropdown-item command="CREATED" disabled>
							{{ t('module.plugin.search.dropdown.created') }}
						</el-dropdown-item>
						<el-dropdown-item command="FAILED" disabled>
							{{ t('module.plugin.search.dropdown.failed') }}
						</el-dropdown-item>
					</el-dropdown-menu>
				</template>
			</el-dropdown>
		</el-col>
	</el-row>

	<el-table :data="plugins" style="width: 100%">
		<el-table-column prop="logo" :label="t('module.plugin.table.label.icon')" width="80">
			<template #default="scope">
				<el-avatar
					shape="square"
					:size="40"
					:src="scope.row.logo ? scope.row.logo : defaultPluginLogoUrl"
				/>
			</template>
		</el-table-column>

		<el-table-column prop="name" label="ID" width="150" />
		<el-table-column prop="displayName" :label="t('module.plugin.table.label.name')" width="150" />
		<el-table-column prop="author.name" :label="t('module.plugin.table.label.author')" width="200">
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
		<el-table-column prop="description" :label="t('module.plugin.table.label.description')" />
		<el-table-column prop="state" :label="t('module.plugin.table.label.state')" align="right" width="100">
			<template #default="scope">
				{{ stateStrMap.get(scope.row.state) }}
			</template>
		</el-table-column>
		<el-table-column align="right" :label="t('module.plugin.table.label.operate')" width="80">
			<template #default="scope">
				<el-dropdown style="cursor: pointer" trigger="click">
					<el-icon size="30">
						<More />
					</el-icon>
					<template #dropdown>
						<el-dropdown-menu>
							<el-dropdown-item @click="toPluginDetails(scope.row.name)">
								{{ t('module.plugin.table.operate.details') }}
							</el-dropdown-item>
							<el-dropdown-item
								divided
								:disabled="scope.row.state === 'STARTED'"
								@click="startPlugin(scope.row.name)"
							>
							{{ t('module.plugin.table.operate.start') }}
							</el-dropdown-item>

							<el-dropdown-item
								:disabled="scope.row.state === 'STOPPED'"
								@click="stopPlugin(scope.row.name)"
							>
							{{ t('module.plugin.table.operate.stop') }}
							</el-dropdown-item>

							<el-dropdown-item
								divided
								:disabled="scope.row.state !== 'DISABLED'"
								@click="enablePlugin(scope.row.name)"
							>
							{{ t('module.plugin.table.operate.enable') }}
							</el-dropdown-item>

							<el-dropdown-item
								:disabled="scope.row.state === 'DISABLED'"
								@click="disablePlugin(scope.row.name)"
							>
							{{ t('module.plugin.table.operate.disable') }}
							</el-dropdown-item>

							<el-dropdown-item divided @click="reloadPlugin(scope.row.name)">
								{{ t('module.plugin.table.operate.reload') }}
							</el-dropdown-item>
							<el-dropdown-item @click="upgradePlugin(scope.row)">
								{{ t('module.plugin.table.operate.upgrade') }}
							</el-dropdown-item>
							<el-dropdown-item
								style="width: 170; color: red"
								divided
								@click="deletePlugin(scope.row.name)"
							>
							{{ t('module.plugin.table.operate.delete') }}
							</el-dropdown-item>
							<el-dropdown-item style="width: 170; color: red" disabled>
								{{ t('module.plugin.table.operate.reset') }}
							</el-dropdown-item>
						</el-dropdown-menu>
					</template>
				</el-dropdown>
			</template>
		</el-table-column>
	</el-table>
</template>

<style lang="scss" scoped></style>
