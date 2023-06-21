<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ConfigMap, Plugin } from '@runikaros/api-client';
import { apiClient } from '@/utils/api-client';
import {
	ElDescriptions,
	ElDescriptionsItem,
	ElImage,
	ElMessage,
	ElTabPane,
	ElTabs,
} from 'element-plus';

const route = useRoute();

const onPluginNameUpdate = (pluginNewName: string) => {
	//@ts-ignore
	plugin.value.name = pluginNewName;
	fetchPlugin();
	fetchConfigMap();
};

watch(route, () => {
	onPluginNameUpdate(route.params.name as string);
});

// eslint-disable-next-line no-unused-vars, no-undef
const configMapSchemas = computed(() => {
	let str = plugin.value?.configMapSchemas?.replace(/(\n|\r|\r\n|↵)/g, '');
	// str = str.replace(/\$/g, '\\$');
	// @ts-ignore
	str = str.replace(' ', '');
	// console.log('str', str);
	str = JSON.parse(str);
	// @ts-ignore
	str = Object.values(str);
	// console.log('str', str);
	// 变量Schemas，如果找到包含字段名为 name的对象，
	// 则从configMap中拿出对应的字段名的值，
	// 赋值给 schema的对应value上，目的是完成初始化。
	// @ts-ignore
	for (const obj of str) {
		// console.log(obj, typeof obj.name);
		// @ts-ignore
		if (obj && 'string' === typeof obj.name) {
			// @ts-ignore
			const field = obj.name;
			// console.log(field);
			console.log(configMap.value);
			if (
				configMap.value &&
				configMap.value.data &&
				'undefined' !== typeof configMap.value?.data[field]
			) {
				// @ts-ignore
				obj.value = configMap.value?.data[field];
			}
		}
	}
	// console.log(str);
	return str;
});

onMounted(() => {
	onPluginNameUpdate(route.params.name as string);
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

const configMap = ref<ConfigMap>({});
// eslint-disable-next-line no-unused-vars
const fetchConfigMap = async () => {
	if (!plugin.value.configMapSchemas) {
		return;
	}
	const { data } = await apiClient.configmap.getConfigmap({
		name: plugin.value.name as string,
	});
	configMap.value = data;
};

const onSubmit = (form) => {
	console.log('form', form);
	configMap.value.data = form;
	updateConfigMap();
};

const updateConfigMap = async () => {
	await apiClient.configmap.updateConfigmap({
		name: configMap.value.name as string,
		configMap: configMap.value,
	});
	ElMessage.success('更新配置成功');
	window.location.reload();
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
		<el-tab-pane v-if="plugin.configMapSchemas" label="基本设置">
			<!--			{{ plugin.configMapSchemas }}-->
			<!--			<hr />-->
			<!--			{{ configMap }}-->
			<!--			<hr />-->
			<div style="padding: 5px">
				<FormKit type="form" submit-label="保存" @submit="onSubmit">
					<FormKitSchema :schema="configMapSchemas as any" />
				</FormKit>
			</div>
		</el-tab-pane>
	</el-tabs>
</template>

<style scoped lang="scss"></style>
