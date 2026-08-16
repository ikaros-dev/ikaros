<script setup lang="ts">
import {
	ElButton,
	ElForm,
	ElMessage,
	ElRadio,
	ElRadioGroup,
} from 'element-plus';
import { AxiosError } from 'axios';
import { useI18n } from 'vue-i18n';
import { useUserStore } from '@/stores/user';
import { useLayoutStore } from '@/stores/layout';
import { changeI18nLocal, locales } from '@/locales';
import { randomUUID } from '@/utils/id';
import FilledTextField from '@/components/common/FilledTextField.vue';
import { onMounted, ref, computed } from 'vue';

const { t, locale } = useI18n();
const layoutStore = useLayoutStore();
const languages = locales.filter((language) => language.name);
const selectedLanguage = ref(layoutStore.i18nCode || locale.value);
changeI18nLocal(selectedLanguage.value);

const changeLanguage = (
	languageCode: string | number | boolean | undefined
) => {
	const i18nCode = String(languageCode);
	changeI18nLocal(i18nCode);
	layoutStore.setI18nCode(i18nCode);
};

const step = ref<'credentials' | 'totp'>('credentials');

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

const totpCode = ref(['', '', '', '', '', '']);
const totpCodeRefs = ref<(HTMLInputElement | null)[]>([]);

const userStore = useUserStore();

const isTotpCodeComplete = computed(() => {
	return totpCode.value.every((d) => d !== '');
});

const handleLogin = async () => {
	try {
		await userStore.applyJwtToken(form.value.username, form.value.password);

		if (userStore.totpRequired) {
			step.value = 'totp';
			setTimeout(() => totpCodeRefs.value[0]?.focus(), 100);
			return;
		}

		if (!userStore.isAnonymous) {
			window.location.reload();
		}
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

const handleTotpInput = (index: number) => {
	if (totpCode.value[index] && index < 5) {
		totpCodeRefs.value[index + 1]?.focus();
	}
};

const handleTotpKeydown = (index: number, e: KeyboardEvent) => {
	if (e.key === 'Backspace' && !totpCode.value[index] && index > 0) {
		totpCodeRefs.value[index - 1]?.focus();
	}
	if (e.key === 'Enter' && isTotpCodeComplete.value) {
		handleTotpSubmit();
	}
};

const handleTotpSubmit = async () => {
	const code = totpCode.value.join('');
	if (code.length !== 6) return;

	const success = await userStore.validateTotp(code);
	if (success) {
		window.location.reload();
	} else {
		ElMessage.error(t('common.exception.unknown_error_with_title'));
		totpCode.value = ['', '', '', '', '', ''];
		setTimeout(() => totpCodeRefs.value[0]?.focus(), 100);
	}
};

const handleBackToLogin = () => {
	step.value = 'credentials';
	totpCode.value = ['', '', '', '', '', ''];
};

const usernameRef = ref<InstanceType<typeof FilledTextField> | null>(null);
const totpContainerRef = ref<HTMLDivElement | null>(null);

onMounted(() => {
	handleGenerateToken();
	if (userStore.consumeConsoleAccessDenied()) {
		ElMessage.error(t('common.exception.console_access_denied'));
	}
	usernameRef.value?.focus();
});
</script>

<template>
	<div class="m3-login">
		<div class="m3-login__card">
			<!-- Step 1: 用户名密码 -->
			<template v-if="step === 'credentials'">
				<div class="m3-login__header">
					<h1 class="m3-login__title">{{ t('module.user.login.title') }}</h1>
				</div>

				<el-form
					:model="form"
					class="m3-login__form"
					@submit.prevent="handleLogin"
				>
					<filled-text-field
						id="username"
						ref="usernameRef"
						v-model="form.username"
						:label="t('module.user.login.field.username.placeholder')"
						autocomplete="username"
						required
					/>

					<filled-text-field
						id="password"
						v-model="form.password"
						type="password"
						:label="t('module.user.login.field.password.placeholder')"
						autocomplete="current-password"
						required
					/>

					<div class="m3-login__actions">
						<el-radio-group
							v-model="selectedLanguage"
							class="m3-language-select"
							@change="changeLanguage"
						>
							<el-radio
								v-for="language in languages"
								:key="language.code"
								:value="language.code"
							>
								{{ language.name }}
							</el-radio>
						</el-radio-group>
						<el-button type="primary" native-type="submit" size="large">
							{{ t('module.user.login.button') }}
						</el-button>
					</div>
				</el-form>
			</template>

			<!-- Step 2: TOTP 验证码 -->
			<template v-else>
				<div class="m3-login__header">
					<h1 class="m3-login__title">二步验证</h1>
					<p class="m3-login__subtitle">
						请在 Authenticator 应用中输入当前显示的验证码
					</p>
				</div>

				<div ref="totpContainerRef" class="m3-totp">
					<div class="m3-totp__inputs">
						<!-- eslint-disable-next-line vue/no-restricted-html-elements -- 原生输入框用于逐位验证码焦点控制 -->
						<input
							v-for="(digit, index) in totpCode"
							:key="index"
							:ref="
								(el) => {
									totpCodeRefs[index] = el as HTMLInputElement;
								}
							"
							v-model="totpCode[index]"
							type="text"
							maxlength="1"
							class="m3-totp__digit"
							inputmode="numeric"
							pattern="[0-9]"
							autocomplete="off"
							@input="handleTotpInput(index)"
							@keydown="handleTotpKeydown(index, $event)"
						/>
					</div>

					<p v-if="userStore.totpTempToken" class="m3-totp__hint">
						已验证身份，请输入验证码
					</p>

					<div class="m3-totp__actions">
						<el-button size="large" @click="handleBackToLogin">
							返回登录
						</el-button>
						<el-button
							type="primary"
							size="large"
							:disabled="!isTotpCodeComplete"
							@click="handleTotpSubmit"
						>
							验证
						</el-button>
					</div>
				</div>
			</template>
		</div>
	</div>
</template>

<style scoped>
/* ========== M3 Color System & Layout ========== */
.m3-login {
	--m3-primary: #409eff;
	--m3-on-primary: #ffffff;
	--m3-primary-container: #d6e4ff;
	--m3-on-primary-container: #001b3e;
	--m3-secondary: #565f71;
	--m3-on-secondary: #ffffff;
	--m3-surface: #ffffff;
	--m3-surface-dim: #f0f5fa;
	--m3-surface-container: #ffffff;
	--m3-surface-container-high: #f8faff;
	--m3-surface-container-highest: #eef3f8;
	--m3-on-surface: #1f1f1f;
	--m3-on-surface-variant: #44474f;
	--m3-outline: #c4c6d0;
	--m3-error: #ba1a1a;
	--m3-elevation-1: 0 1px 2px 0 rgba(0, 0, 0, 0.04);
	--m3-elevation-3:
		0 4px 8px 0 rgba(0, 0, 0, 0.04), 0 1px 3px 0 rgba(0, 0, 0, 0.04);
	--m3-elevation-5:
		0 8px 16px 0 rgba(0, 0, 0, 0.04), 0 2px 6px 0 rgba(0, 0, 0, 0.04);

	min-height: 100%;
	width: 100%;
	display: flex;
	align-items: center;
	justify-content: center;
	background: var(--m3-surface-dim);
	padding: 24px;
	box-sizing: border-box;
}

/* ========== Card ========== */
.m3-login__card {
	width: 100%;
	max-width: 400px;
	background: var(--m3-surface-container);
	border-radius: 28px;
	box-shadow: var(--m3-elevation-3);
	padding: 48px 32px 32px;
	box-sizing: border-box;
	transition: box-shadow 0.2s ease;
}

/* ========== Header ========== */
.m3-login__header {
	margin-bottom: 32px;
	text-align: center;
}

.m3-login__title {
	margin: 0 0 8px;
	font-family: 'Roboto', system-ui, sans-serif;
	font-size: 28px;
	font-weight: 400;
	line-height: 36px;
	letter-spacing: 0;
	color: var(--m3-on-surface);
}

.m3-login__subtitle {
	margin: 0;
	font-family: 'Roboto', system-ui, sans-serif;
	font-size: 14px;
	font-weight: 400;
	line-height: 20px;
	letter-spacing: 0.25px;
	color: var(--m3-on-surface-variant);
}

/* ========== Form ========== */
.m3-login__form {
	display: flex;
	flex-direction: column;
	gap: 16px;
}

/* ========== Actions (step 1) ========== */
.m3-login__actions {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-top: 8px;
	gap: 16px;
}

.m3-language-select {
	display: flex;
	align-items: center;
	gap: 16px;
	flex-shrink: 0;
}

:deep(.m3-language-select .el-radio) {
	margin-right: 0;
}

/* ========== TOTP Input ========== */
.m3-totp {
	display: flex;
	flex-direction: column;
	gap: 24px;
}

.m3-totp__inputs {
	display: flex;
	gap: 12px;
	justify-content: center;
}

.m3-totp__digit {
	width: 48px;
	height: 56px;
	text-align: center;
	font-family: 'Roboto', system-ui, sans-serif;
	font-size: 28px;
	font-weight: 500;
	letter-spacing: 4px;
	color: var(--m3-on-surface);
	background: var(--m3-surface-container-highest);
	border: none;
	border-radius: 8px;
	outline: none;
	caret-color: var(--m3-primary);
	transition:
		background 0.15s ease,
		box-shadow 0.15s ease;
}

.m3-totp__digit:focus {
	background: var(--m3-surface-container-high);
	box-shadow: inset 0 0 0 2px var(--m3-primary);
}

.m3-totp__digit::selection {
	background: var(--m3-primary-container);
}

.m3-totp__hint {
	margin: 0;
	text-align: center;
	font-family: 'Roboto', system-ui, sans-serif;
	font-size: 13px;
	font-weight: 400;
	line-height: 18px;
	letter-spacing: 0.4px;
	color: var(--m3-on-surface-variant);
}

.m3-totp__actions {
	display: flex;
	gap: 12px;
	justify-content: center;
}

/* ========== Responsive ========== */
@media (max-width: 480px) {
	.m3-login__card {
		padding: 32px 24px 24px;
		border-radius: 24px;
	}

	.m3-login__actions {
		flex-direction: column;
		gap: 12px;
	}

	.m3-login__actions > .el-button {
		width: 100%;
	}

	.m3-totp__digit {
		width: 40px;
		height: 48px;
		font-size: 22px;
	}

	.m3-totp__inputs {
		gap: 8px;
	}
}
</style>
