<script setup lang="ts">
import { ElMessage, FormRules } from 'element-plus';
import { FolderOpened } from '@element-plus/icons-vue';
import { apiClient } from '@/utils/api-client';

const setting = ref({
	SITE_TITLE: '',
	SITE_SUBHEAD: '',
	LOGO: '',
	FAVICON: '',
	ALLOW_REGISTER: 'false',
	DEFAULT_ROLE: 'anonymous',
	GLOBAL_HEADER: '',
	GLOBAL_FOOTER: '',
});

const settingFormRules = reactive<FormRules>({
	SITE_TITLE: [
		{ required: true, message: '请输入有效的站点标题', trigger: 'blur' },
		{ min: 3, max: 15, message: '长度应该在3到15个字符内', trigger: 'blur' },
	],
});

const settingConfigMapName = 'setting.server.ikaros.run';

const getSettingFromServer = async () => {
	const { data } = await apiClient.configmap.getConfigmapMeta({
		name: settingConfigMapName,
		metaName: 'data',
	});
	// @ts-ignore
	setting.value = data;
};

// eslint-disable-next-line no-unused-vars
const updateSetting = async () => {
	await apiClient.configmap
		.updateConfigmapMeta({
			name: settingConfigMapName,
			metaName: 'data',
			body: JSON.stringify(setting.value),
		})
		.then(() => {
			ElMessage.success('更新成功');
		});
};

onMounted(getSettingFromServer);
</script>

<template>
	<el-form
		:model="setting"
		label-width="90"
		label-position="top"
		size="default"
		:rules="settingFormRules"
	>
		<el-tabs>
			<el-tab-pane label="基础设置">
				<el-form-item label="站点标题" prop="SITE_TITLE">
					<el-input
						v-model="setting.SITE_TITLE"
						placeholder="请输入站点标题"
						style="max-width: 600px"
						clearable
					/>
				</el-form-item>

				<el-form-item label="站点副标题">
					<el-input
						v-model="setting.SITE_SUBHEAD"
						placeholder="请输入站点副标题"
						style="max-width: 600px"
						clearable
					/>
				</el-form-item>

				<el-form-item label="LOGO">
					<el-input v-model="setting.LOGO" style="max-width: 600px" clearable>
						<template #prepend>
							<el-button>
								<el-icon><FolderOpened /></el-icon>
							</el-button>
						</template>
					</el-input>
				</el-form-item>

				<el-form-item label="FAVICON">
					<el-input
						v-model="setting.FAVICON"
						style="max-width: 600px"
						clearable
					>
						<template #prepend>
							<el-button>
								<el-icon><FolderOpened /></el-icon>
							</el-button>
						</template>
					</el-input>
				</el-form-item>

				<el-form-item>
					<el-button type="primary" @click="updateSetting">保存</el-button>
				</el-form-item>
			</el-tab-pane>
			<el-tab-pane label="用户设置">
				<el-form-item label="开放注册">
					<el-switch
						v-model="setting.ALLOW_REGISTER"
						inline-prompt
						size="large"
						active-text="开启"
						inactive-text="关闭"
					/>
				</el-form-item>
				<el-form-item>
					<el-button type="primary" @click="updateSetting">保存</el-button>
				</el-form-item>
			</el-tab-pane>
			<el-tab-pane label="代码注入">
				<el-form-item label="全局Header">
					<el-input
						v-model="setting.GLOBAL_HEADER"
						style="max-width: 600px"
						:autosize="{ minRows: 10 }"
						maxlength="2000"
						rows="10"
						show-word-limit
						type="textarea"
					/>
				</el-form-item>
				<el-form-item label="全局Footer">
					<el-input
						v-model="setting.GLOBAL_FOOTER"
						style="max-width: 600px"
						:autosize="{ minRows: 10 }"
						maxlength="2000"
						rows="10"
						show-word-limit
						type="textarea"
					/>
				</el-form-item>

				<el-form-item>
					<el-button type="primary" @click="updateSetting">保存</el-button>
				</el-form-item>
			</el-tab-pane>
		</el-tabs>
	</el-form>
</template>

<style lang="scss" scoped></style>
