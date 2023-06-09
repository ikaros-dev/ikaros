<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { Search, ArrowDown } from '@element-plus/icons-vue';
import { Plugin } from '@runikaros/api-client';
import { More } from '@element-plus/icons-vue';
// eslint-disable-next-line no-unused-vars
import { ElMessage, ElMessageBox } from 'element-plus';

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
	['STARTED', '启用'],
	['STOPPED', '停用'],
	['CREATED', '创建'],
	['DISABLED', '卸载'],
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

// eslint-disable-next-line no-unused-vars
const updatePluginState = async (plugin: Plugin) => {
	console.log('等后端实现，插件是', plugin.name, plugin.state);
	if (plugin.state === 'STARTED') {
		startPlugin(plugin.name);
	} else if (plugin.state === 'STOPPED') {
		stopPlugin(plugin.name);
	}
};

const startPlugin = (pluginName: string | undefined) => {
	console.log('启动插件：', pluginName);
	ElMessageBox.confirm('您确定要启动插件吗?', '警告', {
		confirmButtonText: '确定',
		cancelButtonText: '取消',
		type: 'warning',
	})
		.then(() => {
			ElMessage({
				type: 'success',
				message: '已请求启动插件，请等待插件就绪。',
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
	console.log('停止插件：', pluginName);
};

// eslint-disable-next-line no-unused-vars
const disablePlugin = (pluginName: string | undefined) => {
	console.log('禁用插件：', pluginName);
};

// eslint-disable-next-line no-unused-vars
const enablePlugin = (pluginName: string | undefined) => {
	console.log('启用插件：', pluginName);
};

onMounted(getPluginsFromServer);
</script>

<template>
	<el-card>
		<template #header>
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
					<el-dropdown
						size="large"
						trigger="click"
						style="
							cursor: pointer;
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
								<el-dropdown-item command="ALL" style="width: 200px" disabled>
									全部
								</el-dropdown-item>
								<el-dropdown-item
									command="STARTED"
									style="width: 200px"
									disabled
								>
									启用
								</el-dropdown-item>
								<el-dropdown-item
									command="STOPPED"
									style="width: 200px"
									disabled
								>
									停用
								</el-dropdown-item>
								<el-dropdown-item
									command="RESOLVED"
									style="width: 200px"
									disabled
								>
									就绪
								</el-dropdown-item>
								<el-dropdown-item
									command="DISABLED"
									style="width: 200px"
									disabled
								>
									卸载
								</el-dropdown-item>
								<el-dropdown-item
									command="CREATED"
									style="width: 200px"
									disabled
								>
									创建
								</el-dropdown-item>
								<el-dropdown-item
									command="FAILED"
									style="width: 200px"
									disabled
								>
									失败
								</el-dropdown-item>
							</el-dropdown-menu>
						</template>
					</el-dropdown>
				</el-col>
			</el-row>
		</template>

		<div>
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
				<el-table-column prop="name" label="名称" width="150" />
				<el-table-column prop="author.name" label="作者" width="200" />
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
									<el-dropdown-item style="width: 200px" disabled>
										详情
									</el-dropdown-item>
									<el-dropdown-item
										style="width: 200px"
										@click="startPlugin(scope.row.name)"
									>
										启动
									</el-dropdown-item>

									<el-dropdown-item style="width: 200px" disabled>
										停止
									</el-dropdown-item>
									<el-dropdown-item style="width: 200px" disabled>
										升级
									</el-dropdown-item>
									<el-dropdown-item
										style="width: 200px; color: red"
										divided
										disabled
									>
										卸载
									</el-dropdown-item>
									<el-dropdown-item style="width: 200px; color: red" disabled>
										重置
									</el-dropdown-item>
								</el-dropdown-menu>
							</template>
						</el-dropdown>
					</template>
				</el-table-column>
			</el-table>
		</div>
	</el-card>
</template>

<style lang="scss" scoped></style>
