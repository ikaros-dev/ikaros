<script setup lang="ts">
const setting = ref({
	SITE_TITLE: '',
	SITE_SUBHEAD: '',
	LOGO: '',
	FAVICON: '',
	ALLOW_REGISTER: 'false',
	DEFAULT_ROLE: 'false',
	GLOBAL_HEADER: '',
	GLOBAL_FOOTER: '',
});
</script>

<template>
	<div>
		<el-tabs type="border-card">
			<el-tab-pane label="基础设置">
				<el-input
					v-model="setting.SITE_TITLE"
					placeholder="请输入站点标题"
					style="width: 512"
					clearable
				/>
				<el-input
					v-model="setting.SITE_SUBHEAD"
					placeholder="请输入站点副标题"
					style="width: 512"
					clearable
				/>
			</el-tab-pane>
			<el-tab-pane label="用户设置">
				<h1>用户设置</h1>
			</el-tab-pane>
			<el-tab-pane label="代码注入">Config</el-tab-pane>
		</el-tabs>
	</div>
</template>

<style lang="scss" scoped></style>
