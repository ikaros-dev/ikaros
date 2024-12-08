<script setup lang="ts">
import { apiClient } from '@/utils/api-client';
import { onMounted, reactive, ref, watch } from 'vue';
import {
	ElButton,
	ElForm,
	ElFormItem,
	ElInput,
	ElMessage,
	ElSwitch,
	ElTabPane,
	ElTabs,
	FormRules,
} from 'element-plus';
import { useSettingStore } from '@/stores/setting';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

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
		{
			required: true,
			message: t('module.setting.message.form-rule.site-title.required'),
			trigger: 'blur',
		},
		{
			min: 3,
			max: 15,
			message: t('module.setting.message.form-rule.site-title.length'),
			trigger: 'blur',
		},
	],
	MAIL_SMTP_HOST: [
		{
			required: true,
			message: t('module.setting.message.form-rule.mail-smtp-host.required'),
			trigger: 'blur',
		},
	],
	MAIL_SMTP_PORT: [
		{
			required: true,
			message: t('module.setting.message.form-rule.mail-smtp-port.required'),
			trigger: 'blur',
		},
	],
	MAIL_SMTP_ACCOUNT: [
		{
			required: true,
			message: t('module.setting.message.form-rule.mail-smtp-account.required'),
			trigger: 'blur',
		},
	],
	MAIL_SMTP_PASSWORD: [
		{
			required: true,
			message: t(
				'module.setting.message.form-rule.mail-smtp-password.required'
			),
			trigger: 'blur',
		},
	],
	MAIL_RECEIVE_ADDRESS: [
		{
			required: true,
			message: t(
				'module.setting.message.form-rule.mail-receive-address.required'
			),
			trigger: 'blur',
		},
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

const settingSaveBtnLoading = ref(false);
const settingStore = useSettingStore();
// eslint-disable-next-line no-unused-vars
const updateSetting = async () => {
	settingSaveBtnLoading.value = true;
	await apiClient.configmap
		.updateConfigmapMeta({
			name: settingConfigMapName,
			metaName: 'data',
			body: JSON.stringify(setting.value),
		})
		.then(async () => {
			ElMessage.success(t('module.setting.message.operate.update'));
			await settingStore.fetchSystemSetting();
		})
		.finally(() => {
			settingSaveBtnLoading.value = false;
		});
};

const mailEnable = ref(false);

watch(setting, () => {
	mailEnable.value = setting.value.MAIL_ENABLE === 'true';
});
watch(mailEnable, () => {
	setting.value.MAIL_ENABLE = mailEnable.value ? 'true' : 'false';
});

const testMailBtnLoading = ref(false);
const testMailConfig = async () => {
	if (
		!mailEnable.value ||
		setting.value.MAIL_RECEIVE_ADDRESS === '' ||
		setting.value.MAIL_SMTP_PASSWORD === ''
	)
		return;
	testMailBtnLoading.value = true;
	await apiClient.notify.testMailSend();
	testMailBtnLoading.value = false;
	ElMessage.success('Email has been sent.');
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
			<el-tab-pane :label="t('module.setting.label.tab.pane.mail-config')">
				<el-form-item :label="t('module.setting.label.tab.item.enable-mail')">
					<el-switch
						v-model="mailEnable"
						inline-prompt
						size="large"
						:active-text="t('module.setting.label.tab.item.switch.active')"
						:inactive-text="t('module.setting.label.tab.item.switch.inactive')"
					/>
				</el-form-item>
				<span v-if="mailEnable">
					<el-form-item
						:label="t('module.setting.label.tab.item.mail.protocol')"
						prop="MAIL_PROTOCOL"
					>
						<el-input
							v-model="setting.MAIL_PROTOCOL"
							style="max-width: 600px"
							clearable
							disabled
						/>
					</el-form-item>
					<el-form-item
						:label="t('module.setting.label.tab.item.mail.smtp.host')"
						prop="MAIL_SMTP_HOST"
					>
						<el-input
							v-model="setting.MAIL_SMTP_HOST"
							:placeholder="t('module.setting.placeholder.smtp.host')"
							style="max-width: 600px"
							clearable
						/>
					</el-form-item>
					<el-form-item
						:label="t('module.setting.label.tab.item.mail.smtp.port')"
						prop="MAIL_SMTP_PORT"
					>
						<el-input
							v-model="setting.MAIL_SMTP_PORT"
							:placeholder="t('module.setting.placeholder.smtp.port')"
							style="max-width: 600px"
							clearable
						/>
					</el-form-item>
					<el-form-item
						:label="t('module.setting.label.tab.item.mail.smtp.account')"
						prop="MAIL_SMTP_ACCOUNT"
					>
						<el-input
							v-model="setting.MAIL_SMTP_ACCOUNT"
							:placeholder="t('module.setting.placeholder.smtp.account')"
							style="max-width: 600px"
							clearable
						/>
					</el-form-item>
					<el-form-item
						:label="t('module.setting.label.tab.item.mail.smtp.password')"
						prop="MAIL_SMTP_PASSWORD"
					>
						<el-input
							v-model="setting.MAIL_SMTP_PASSWORD"
							:placeholder="t('module.setting.placeholder.smtp.password')"
							style="max-width: 600px"
							clearable
						/>
					</el-form-item>
					<el-form-item
						:label="t('module.setting.label.tab.item.mail.smtp.alias')"
						prop="MAIL_SMTP_ACCOUNT_ALIAS"
					>
						<el-input
							v-model="setting.MAIL_SMTP_ACCOUNT_ALIAS"
							:placeholder="t('module.setting.placeholder.smtp.alias')"
							style="max-width: 600px"
							clearable
						/>
					</el-form-item>
					<el-form-item
						:label="
							t('module.setting.label.tab.item.mail.smtp.receive-address')
						"
						prop="MAIL_RECEIVE_ADDRESS"
					>
						<el-input
							v-model="setting.MAIL_RECEIVE_ADDRESS"
							:placeholder="
								t('module.setting.placeholder.smtp.receive-address')
							"
							style="max-width: 600px"
							clearable
						/>
					</el-form-item>
				</span>
				<el-form-item>
					<el-button
						type="primary"
						:loading="settingSaveBtnLoading"
						@click="updateSetting"
					>
						{{ t('module.setting.button.save') }}
					</el-button>

					<el-button
						v-if="mailEnable"
						:loading="testMailBtnLoading"
						type="primary"
						@click="testMailConfig"
					>
						Test
					</el-button>
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
			<!-- 
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
			 -->
		</el-tabs>
	</el-form>
</template>

<style lang="scss" scoped></style>
