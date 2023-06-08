<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { Search, ArrowDown } from '@element-plus/icons-vue';
import { Plugin } from '@runikaros/api-client';
import { More } from '@element-plus/icons-vue';

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
	total: -1,
	name: '',
	state: 'ALL',
});
// eslint-disable-next-line no-unused-vars
const stateStrMap: Map<string, string> = new Map([
	['ALL', '状态'],
	['STARTED', '启用'],
	['STOPPED', '停用'],
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
	const { data } = await apiClient.plugin.getpluginsbyPaging({
		page: '1',
		size: '99999',
	});
	plugins.value = data.items as Plugin[];
	pluginSearch.value.page = data.page;
	pluginSearch.value.size = data.size;
	pluginSearch.value.total = data.total;
	pluginSearch.value.hasNext = data.hasNext;
	pluginSearch.value.hasPrevious = data.hasPrevious;
};

const updatePluginState = async (plugin: Plugin) => {
	console.log('等后端实现，插件是', plugin.name, plugin.state);
};

onMounted(getPluginsFromServer);
</script>

<template>
	<el-card>
		<template #header>
			<el-row :gutter="5">
				<el-col :xs="24" :sm="24" :md="24" :lg="16" :xl="16">
					<el-input
						v-model="pluginSearch.name"
						style="width: 500px"
						size="large"
						placeholder="请输入插件名称"
						:prefix-icon="Search"
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
								<el-dropdown-item command="ALL" style="width: 200px"
									>全部</el-dropdown-item
								>
								<el-dropdown-item command="STARTED" style="width: 200px"
									>启用</el-dropdown-item
								>
								<el-dropdown-item command="STOPPED" style="width: 200px"
									>停用</el-dropdown-item
								>
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
				<el-table-column prop="state" label="状态" align="right" width="65">
					<template #default="scope">
						<el-switch
							v-model="scope.row.state"
							active-value="STARTED"
							inactive-value="STOPPED"
							size="large"
							@change="updatePluginState(scope.row)"
						/>
					</template>
				</el-table-column>
				<el-table-column align="right" label="操作" width="65">
					<template #default>
						<el-dropdown style="cursor: pointer" trigger="click">
							<el-icon size="30"><More /></el-icon>
							<template #dropdown>
								<el-dropdown-menu>
									<el-dropdown-item style="width: 200px">详情</el-dropdown-item>
									<el-dropdown-item style="width: 200px">升级</el-dropdown-item>
									<el-dropdown-item style="width: 200px; color: red">
										卸载
									</el-dropdown-item>
									<el-dropdown-item style="width: 200px; color: red">
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
