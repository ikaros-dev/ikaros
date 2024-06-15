<script setup lang="ts">
import { ElMessage, ElInput, ElForm, ElFormItem, ElButton } from 'element-plus';
import axios from 'axios';
import { AxiosError } from 'axios';
import { useI18n } from 'vue-i18n';
import { useUserStore } from '@/stores/user';
import { randomUUID } from '@/utils/id';
import qs from 'qs';
import { onMounted, ref } from 'vue';
import LanguageSelect from '@/layouts/components/LanguageSelect.vue';
// import Dashboard from '@/modules/dashboard/Dashboard.vue';

const { t } = useI18n();
// const route = useRoute();
// const router = useRouter();

const handleGenerateToken = async () => {
	const token = randomUUID();
	form.value._csrf = token;
	document.cookie = `XSRF-TOKEN=${token}; Path=/;`;
};

const form = ref({
	username: '',
	password: '',
	_csrf: '',
});

const rules = ref({
	username: [
		{
			required: true,
			message: t('module.user.login.field.username.rule'),
			trigger: 'blur',
		},
	],
	password: [
		{
			required: true,
			message: t('module.user.login.field.password.rule'),
			trigger: 'blur',
		},
	],
});

const formRef = ref(null);
const userStore = useUserStore();

const handleLogin = async () => {
	try {
		const url = `${import.meta.env.VITE_API_URL}/login`;
		await axios.post(url, qs.stringify(form.value), {
			withCredentials: true,
			headers: {
				'Content-Type': 'application/x-www-form-urlencoded',
			},
		});

		await userStore.fetchCurrentUser();

		

		// Reload page
		// if (route.query.redirect_uri) {
		// 	window.location.reload();
		// } else {
		// 	router.push(Dashboard);
		// }
		window.location.reload();
	} catch (e: unknown) {
		console.error('Failed to login', e);

		if (e instanceof AxiosError) {
			if (/Network Error/.test(e.message)) {
				ElMessage.error(t('common.exception.network_error'));
				return;
			}

			if (e.response?.status === 403) {
				ElMessage.warning(t('module.user.login.operation.submit.toast_csrf'));
				await handleGenerateToken();
				return;
			}

			ElMessage.error(t('module.user.login.operation.submit.toast_failed'));
		} else {
			ElMessage.error(t('common.exception.unknown_error_with_title'));
		}

		form.value.password = '';
	}
};

const usernameRef = ref();

onMounted(() => {
	handleGenerateToken();
	usernameRef.value.focus();
});
</script>

<template>
	<div class="login-container">
		<el-form ref="formRef" :model="form" class="login-form" :rules="rules">
			<div class="title-container">
				<h3 class="title">{{ t('module.user.login.title') }}</h3>
			</div>
			<el-form-item prop="username">
				<el-input
					ref="usernameRef"
					v-model="form.username"
					:placeholder="t('module.user.login.field.username.placeholder')"
				>
				</el-input>
			</el-form-item>
			<el-form-item prop="password">
				<el-input
					v-model="form.password"
					type="password"
					:placeholder="t('module.user.login.field.password.placeholder')"
					@keyup.enter="handleLogin"
				></el-input>
			</el-form-item>
			<LanguageSelect />
			<el-button type="primary" class="login-button" @click="handleLogin">{{
				t('module.user.login.button')
			}}</el-button>
		</el-form>
	</div>
</template>

<style lang="scss" scoped>
$bg: #2d3a4b;
$dark_gray: #889aa4;
$light_gray: #eee;
$cursor: #fff;

.login-container {
	min-height: 100%;
	width: 100%;
	background-color: $bg;
	overflow: hidden;

	.login-form {
		position: relative;
		width: 520px;
		max-width: 100%;
		padding: 160px 35px 0;
		margin: 0 auto;
		overflow: hidden;

		::deep .el-form-item {
			border: 1px solid rgba(255, 255, 255, 0.1);
			background: rgba(0, 0, 0, 0.1);
			border-radius: 5px;
			color: #454545;
		}

		::deep .el-input {
			display: inline-block;
			height: 47px;
			width: 85%;

			input {
				background: transparent;
				border: 0px;
				-webkit-appearance: none;
				border-radius: 0px;
				padding: 12px 5px 12px 15px;
				color: $light_gray;
				height: 47px;
				caret-color: $cursor;
			}
		}
		.login-button {
			width: 80%;
			box-sizing: border-box;
		}
	}

	.tips {
		font-size: 16px;
		line-height: 28px;
		color: #fff;
		margin-bottom: 10px;

		span {
			&:first-of-type {
				margin-right: 16px;
			}
		}
	}

	.svg-container {
		padding: 6px 5px 6px 15px;
		color: $dark_gray;
		vertical-align: middle;
		display: inline-block;
	}

	.title-container {
		position: relative;

		.title {
			font-size: 26px;
			color: $light_gray;
			margin: 0px auto 40px auto;
			text-align: center;
			font-weight: bold;
		}

		::deep .lang-select {
			position: absolute;
			top: 4px;
			right: 0;
			background-color: white;
			font-size: 22px;
			padding: 4px;
			border-radius: 4px;
			cursor: pointer;
		}
	}

	.show-pwd {
		// position: absolute;
		// right: 10px;
		// top: 7px;
		font-size: 16px;
		color: $dark_gray;
		cursor: pointer;
		user-select: none;
	}
}
</style>
