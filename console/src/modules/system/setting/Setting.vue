<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { onMounted, reactive, ref } from 'vue';
import {
	ElMessage,
	FormRules,
	ElForm,
	ElTabs,
	ElTabPane,
	ElFormItem,
	ElInput,
	ElButton,
	ElSwitch,
} from 'element-plus';
import { useSettingStore } from '@/stores/setting';
import { watch } from 'vue';

const setting = ref({
	SITE_TITLE: '',
	SITE_SUBHEAD: '',
	LOGO: '',
	FAVICON: '',
	ALLOW_REGISTER: 'false',
	DEFAULT_ROLE: 'anonymous',
	GLOBAL_HEADER: '',
	GLOBAL_FOOTER: '',
	REMOTE_ENABLE: 'false',
	MAIL_ENABLE: 'false',
	MAIL_PROTOCOL: 'smtp',
	MAIL_SMTP_HOST: '',
	MAIL_SMTP_PORT: '465',
	MAIL_SMTP_ACCOUNT: '',
	MAIL_SMTP_PASSWORD: '',
	MAIL_SMTP_ACCOUNT_ALIAS: '',
	MAIL_RECEIVE_ADDRESS: '',
});

const settingFormRules = reactive<FormRules>({
	SITE_TITLE: [
		{ required: true, message: '请输入有效的站点标题', trigger: 'blur' },
		{ min: 3, max: 15, message: '长度应该在3到15个字符内', trigger: 'blur' },
	],
	MAIL_SMTP_HOST: [
		{ required: true, message: '请输入服务器地址', trigger: 'blur' },
	],
	MAIL_SMTP_PORT: [
		{ required: true, message: '请输入邮件服务器端口', trigger: 'blur' },
	],
	MAIL_SMTP_ACCOUNT: [
		{ required: true, message: '请输入邮件服务用户名', trigger: 'blur' },
	],
	MAIL_SMTP_PASSWORD: [
		{ required: true, message: '请输入邮件服务密码', trigger: 'blur' },
	],
	MAIL_RECEIVE_ADDRESS: [
		{ required: true, message: '请输入收件方邮件地址', trigger: 'blur' },
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

const settingStore = useSettingStore();
// eslint-disable-next-line no-unused-vars
const updateSetting = async () => {
	await apiClient.configmap
		.updateConfigmapMeta({
			name: settingConfigMapName,
			metaName: 'data',
			body: JSON.stringify(setting.value),
		})
		.then(async () => {
			ElMessage.success('更新成功');
			await settingStore.fetchSystemSetting();
		});
};

// eslint-disable-next-line no-unused-vars
const onDisableClick = () => {
	ElMessage({
		showClose: true,
		message: '此功能尚未实现',
		type: 'warning',
	});
};

const mailEnable = ref(false);

watch(setting, () => {
	mailEnable.value = setting.value.MAIL_ENABLE === 'true';
});
watch(mailEnable, () => {
	setting.value.MAIL_ENABLE = mailEnable.value ? 'true' : 'false';
});

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
			<!-- <el-tab-pane label="基础设置">
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
					<el-input
						v-model="setting.LOGO"
						disabled
						style="max-width: 600px"
						clearable
					>
						<template #prepend>
							<el-button disabled @click="onDisableClick">
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
						disabled
					>
						<template #prepend>
							<el-button disabled @click="onDisableClick">
								<el-icon><FolderOpened /></el-icon>
							</el-button>
						</template>
					</el-input>
				</el-form-item>

				<el-form-item>
					<el-button type="primary" @click="updateSetting">保存</el-button>
				</el-form-item>
			</el-tab-pane> -->
			<el-tab-pane label="邮件配置">
				<el-form-item label="启用邮件">
					<el-switch
						v-model="mailEnable"
						inline-prompt
						size="large"
						active-text="启用"
						inactive-text="禁用"
					/>
				</el-form-item>
				<span v-if="mailEnable">
					<el-form-item label="协议" prop="MAIL_PROTOCOL">
						<el-input
							v-model="setting.MAIL_PROTOCOL"
							style="max-width: 600px"
							clearable
							disabled
						/>
					</el-form-item>
					<el-form-item label="服务器地址" prop="MAIL_SMTP_HOST">
						<el-input
							v-model="setting.MAIL_SMTP_HOST"
							placeholder="请输入邮件服务器地址"
							style="max-width: 600px"
							clearable
						/>
					</el-form-item>
					<el-form-item label="服务器端口" prop="MAIL_SMTP_PORT">
						<el-input
							v-model="setting.MAIL_SMTP_PORT"
							placeholder="请输入邮件服务器端口"
							style="max-width: 600px"
							clearable
						/>
					</el-form-item>
					<el-form-item label="邮件服务用户名" prop="MAIL_SMTP_ACCOUNT">
						<el-input
							v-model="setting.MAIL_SMTP_ACCOUNT"
							placeholder="请输入邮件服务用户名"
							style="max-width: 600px"
							clearable
						/>
					</el-form-item>
					<el-form-item label="邮件服务密码" prop="MAIL_SMTP_PASSWORD">
						<el-input
							v-model="setting.MAIL_SMTP_PASSWORD"
							placeholder="请输入邮件服务密码"
							style="max-width: 600px"
							clearable
						/>
					</el-form-item>
					<el-form-item
						label="邮件服务用户名别名"
						prop="MAIL_SMTP_ACCOUNT_ALIAS"
					>
						<el-input
							v-model="setting.MAIL_SMTP_ACCOUNT_ALIAS"
							placeholder="请输入邮件服务用户名别名"
							style="max-width: 600px"
							clearable
						/>
					</el-form-item>
					<el-form-item label="收件方邮件地址" prop="MAIL_RECEIVE_ADDRESS">
						<el-input
							v-model="setting.MAIL_RECEIVE_ADDRESS"
							placeholder="请输入收件方邮件地址"
							style="max-width: 600px"
							clearable
						/>
					</el-form-item>
				</span>
				<el-form-item>
					<el-button type="primary" @click="updateSetting">保存</el-button>
				</el-form-item>
			</el-tab-pane>
			<!-- <el-tab-pane label="远端配置">
				<el-alert
					title="此功能尚不稳定，不建议开启。"
					type="warning"
					show-icon
				/>
				<el-form-item label="启用远端">
					<el-switch
						v-model="setting.REMOTE_ENABLE"
						inline-prompt
						size="large"
						active-text="启用"
						inactive-text="禁用"
					/>
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
						disabled
						@click="onDisableClick"
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
						disabled
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
						disabled
					/>
				</el-form-item>

				<el-form-item>
					<el-button type="primary" @click="updateSetting">保存</el-button>
				</el-form-item>
			</el-tab-pane> -->
		</el-tabs>
	</el-form>
</template>

<style lang="scss" scoped></style>
