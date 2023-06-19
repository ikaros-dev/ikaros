<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { Search, ArrowDown } from '@element-plus/icons-vue';
import {
	Plugin,
	V1alpha1PluginApiOperatePluginStateByIdRequest,
} from '@runikaros/api-client';
import { More } from '@element-plus/icons-vue';
// eslint-disable-next-line no-unused-vars
import { ElMessage, ElMessageBox } from 'element-plus';
import PluginUploadDrawer from './PluginUploadDrawer.vue';
import type { AxiosResponse } from 'axios';

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

const defaultPluginLogoUrl =
	'https://gss0.baidu.com/7Po3dSag_xI4khGko9WTAnF6hhy/zhidao/pic/item/e7cd7b899e510fb3f001a7d2db33c895d1430c5f.jpg';

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
	['ALL', '状态'],
	['STARTED', '启动'],
	['STOPPED', '停用'],
	['CREATED', '启用'],
	['DISABLED', '已禁用'],
	['RESOLVED', '就绪'],
	['FAILED', '失败'],
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
	ElMessageBox.confirm('您确定要启动插件吗?', '警告', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning',
	})
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'START',
			})
				.then(() => {
					ElMessage.success('启动插件[' + pluginName + ']成功');
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
				message: '已取消',
			});
		});
};

const stopPlugin = (pluginName: string | undefined) => {
	ElMessageBox.confirm('您确定要停止插件吗?', '警告', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning',
	})
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'STOP',
			})
				.then(() => {
					ElMessage.success('停止插件[' + pluginName + ']成功');
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
				message: '已取消',
			});
		});
};

const enablePlugin = (pluginName: string | undefined) => {
	ElMessageBox.confirm('您确定要启用插件吗?', '警告', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning',
	})
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'ENABLE',
			})
				.then(() => {
					ElMessage.success('启用插件[' + pluginName + ']成功');
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
				message: '已取消',
			});
		});
};

const disablePlugin = (pluginName: string | undefined) => {
	ElMessageBox.confirm(
		'您确定要禁用插件吗? 禁用后也可直接启动，会进行启用并启动。',
		'警告',
		{
			confirmButtonText: '确定',
			cancelButtonText: '取消',
			type: 'warning',
		}
	)
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'DISABLE',
			})
				.then(() => {
					ElMessage.success('禁用插件[' + pluginName + ']成功');
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
				message: '已取消',
			});
		});
};

const reloadPlugin = (pluginName: string | undefined) => {
	ElMessageBox.confirm('您确定要重载插件吗? ', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning',
	})
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'RELOAD',
			})
				.then(() => {
					ElMessage.success('重载插件[' + pluginName + ']成功');
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
				message: '已取消',
			});
		});
};

const reloadAllPlugin = () => {
	ElMessageBox.confirm('您确定要重载所有插件吗? 重载后会自动启动。', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning',
	})
		.then(() => {
			delegationPluginStateOperator({
				name: 'ALL',
				operate: 'RELOAD_ALL',
			})
				.then(() => {
					ElMessage.success('重载所有插件成功');
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
				message: '已取消',
			});
		});
};

const deletePlugin = (pluginName: string | undefined) => {
	ElMessageBox.confirm('您确定要删除插件吗? ', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning',
	})
		.then(() => {
			delegationPluginStateOperator({
				name: pluginName as string,
				operate: 'DELETE',
			})
				.then(() => {
					ElMessage.success('删除插件[' + pluginName + ']成功');
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
				message: '已取消',
			});
		});
};

const pluginUploadDrawerVisible = ref(false);

const onPluginUploadDrawerClose = () => {
	pluginUploadDrawerVisible.value = false;
	window.location.reload();
};

onMounted(getPluginsFromServer);
</script>

<template>
	<PluginUploadDrawer
		v-model:visible="pluginUploadDrawerVisible"
		@close="onPluginUploadDrawerClose"
	/>

	<el-row :gutter="10">
		<el-col :xs="24" :sm="24" :md="24" :lg="8" :xl="8">
			<el-input
				v-model="pluginSearch.name"
				style="width: 100%"
				size="large"
				placeholder="请输入插件名称"
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
			<el-button plain @click="reloadAllPlugin">重载所有</el-button>

			<el-button plain @click="pluginUploadDrawerVisible = true"
				>安装插件</el-button
			>

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
						<el-dropdown-item command="ALL" disabled> 全部 </el-dropdown-item>
						<el-dropdown-item command="STARTED" disabled>
							启用
						</el-dropdown-item>
						<el-dropdown-item command="STOPPED" disabled>
							停用
						</el-dropdown-item>
						<el-dropdown-item command="RESOLVED" disabled>
							就绪
						</el-dropdown-item>
						<el-dropdown-item command="DISABLED" disabled>
							禁用
						</el-dropdown-item>
						<el-dropdown-item command="CREATED" disabled>
							创建
						</el-dropdown-item>
						<el-dropdown-item command="FAILED" disabled>
							失败
						</el-dropdown-item>
					</el-dropdown-menu>
				</template>
			</el-dropdown>
		</el-col>
	</el-row>

	<el-table :data="plugins" style="width: 100%">
		<el-table-column prop="logo" label="图标" width="80">
			<template #default="scope">
				<el-avatar
					shape="square"
					:size="40"
					:src="scope.row.logo ? scope.row.logo : defaultPluginLogoUrl"
				/>
			</template>
		</el-table-column>

		<el-table-column prop="name" label="ID" width="150" />
		<el-table-column prop="displayName" label="名称" width="150" />
		<el-table-column prop="author.name" label="作者" width="200">
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
		<el-table-column prop="description" label="描述" />
		<el-table-column prop="state" label="状态" align="right" width="100">
			<template #default="scope">
				{{ stateStrMap.get(scope.row.state) }}
			</template>
		</el-table-column>
		<el-table-column align="right" label="操作" width="55">
			<template #default="scope">
				<el-dropdown style="cursor: pointer" trigger="click">
					<el-icon size="30"><More /></el-icon>
					<template #dropdown>
						<el-dropdown-menu>
							<el-dropdown-item disabled> 详情 </el-dropdown-item>
							<el-dropdown-item
								divided
								:disabled="scope.row.state === 'STARTED'"
								@click="startPlugin(scope.row.name)"
							>
								启动
							</el-dropdown-item>

							<el-dropdown-item
								:disabled="scope.row.state === 'STOPPED'"
								@click="stopPlugin(scope.row.name)"
							>
								停止
							</el-dropdown-item>

							<el-dropdown-item
								divided
								:disabled="scope.row.state !== 'DISABLED'"
								@click="enablePlugin(scope.row.name)"
							>
								启用
							</el-dropdown-item>

							<el-dropdown-item
								:disabled="scope.row.state === 'DISABLED'"
								@click="disablePlugin(scope.row.name)"
							>
								禁用
							</el-dropdown-item>

							<el-dropdown-item divided @click="reloadPlugin(scope.row.name)">
								重载
							</el-dropdown-item>
							<el-dropdown-item disabled> 升级 </el-dropdown-item>
							<el-dropdown-item
								style="width: 170; color: red"
								divided
								@click="deletePlugin(scope.row.name)"
							>
								卸载
							</el-dropdown-item>
							<el-dropdown-item style="width: 170; color: red" disabled>
								重置
							</el-dropdown-item>
						</el-dropdown-menu>
					</template>
				</el-dropdown>
			</template>
		</el-table-column>
	</el-table>
</template>

<style lang="scss" scoped></style>
