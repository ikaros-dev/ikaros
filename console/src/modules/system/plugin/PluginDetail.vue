<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { Plugin } from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';
import {
	ElDescriptions,
	ElDescriptionsItem,
	ElImage,
	ElTabPane,
	ElTabs,
} from 'element-plus';

const route = useRoute();

const updatePluginName = (pluginNewName: string) => {
	//@ts-ignore
	plugin.value.name = pluginNewName;
	fetchPlugin();
};

watch(route, () => {
	updatePluginName(route.params.name as string);
});

onMounted(() => {
	updatePluginName(route.params.name as string);
});

// eslint-disable-next-line no-unused-vars
const plugin = ref<Plugin>({
	version: '',
});

const fetchPlugin = async () => {
	const { data } = await apiClient.plugin.getPlugin({
		name: plugin.value.name as string,
	});
	plugin.value = data;
};
</script>

<template>
	<el-tabs>
		<el-tab-pane label="详情">
			<el-descriptions :column="2" size="large" border>
				<el-descriptions-item label="ID">
					{{ plugin.name }}
				</el-descriptions-item>
				<el-descriptions-item label="版本">
					{{ plugin.version }}
				</el-descriptions-item>
				<el-descriptions-item label="名称">
					{{ plugin.displayName }}
				</el-descriptions-item>
				<el-descriptions-item label="描述">
					{{ plugin.description }}
				</el-descriptions-item>
				<el-descriptions-item label="作者">
					<span v-if="plugin.author?.website">
						<a :href="plugin.author.website" target="_blank">
							<span>{{ plugin.author?.name }}</span>
						</a>
					</span>
					<span v-else>
						<span>{{ plugin.author?.name }}</span>
					</span>
				</el-descriptions-item>
				<el-descriptions-item label="系统版本前提">
					{{ plugin.requires }}
				</el-descriptions-item>
				<el-descriptions-item label="主页">
					<a :href="plugin.homepage" target="_blank">{{ plugin.homepage }}</a>
				</el-descriptions-item>
				<el-descriptions-item label="LICENSE">
					{{ plugin.license }}
				</el-descriptions-item>
				<el-descriptions-item v-if="plugin.entry" label="Entry" :span="2">
					{{ plugin.entry }}
				</el-descriptions-item>
				<el-descriptions-item
					v-if="plugin.stylesheet"
					label="Stylesheet"
					:span="2"
				>
					{{ plugin.stylesheet }}
				</el-descriptions-item>
				<el-descriptions-item label="LOGO" :span="2">
					<el-image
						style="width: 100px"
						:src="plugin.logo as string"
						:zoom-rate="1.2"
						:preview-src-list="new Array(plugin.logo)  as string[]"
						:initial-index="4"
						fit="cover"
					/>
				</el-descriptions-item>
			</el-descriptions>
		</el-tab-pane>
		<el-tab-pane label="基本设置">Config</el-tab-pane>
	</el-tabs>
</template>

<style scoped lang="scss"></style>
