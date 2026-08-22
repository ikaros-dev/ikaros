<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { AxiosError } from 'axios';
import { useI18n } from 'vue-i18n';
import { useUserStore } from '@/stores/user';
import { randomUUID } from '@/utils/id';
import { onMounted, ref, computed } from 'vue';
import LanguageSelect from '@/layouts/components/LanguageSelect.vue';

const { t } = useI18n();

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

const formRef = ref<HTMLFormElement | null>(null);
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

const usernameRef = ref<HTMLInputElement | null>(null);
const totpContainerRef = ref<HTMLDivElement | null>(null);

onMounted(() => {
	handleGenerateToken();
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

				<form
					ref="formRef"
					class="m3-login__form"
					@submit.prevent="handleLogin"
				>
					<div class="m3-field">
						<div class="m3-field__container">
							<input
								ref="usernameRef"
								id="username"
								v-model="form.username"
								type="text"
								class="m3-field__input"
								:placeholder="t('module.user.login.field.username.placeholder')"
								required
							/>
							<label for="username" class="m3-field__label">
								{{ t('module.user.login.field.username.placeholder') }}
							</label>
							<div class="m3-field__underline"></div>
						</div>
					</div>

					<div class="m3-field">
						<div class="m3-field__container">
							<input
								id="password"
								v-model="form.password"
								type="password"
								class="m3-field__input"
								:placeholder="t('module.user.login.field.password.placeholder')"
								required
								@keyup.enter="handleLogin"
							/>
							<label for="password" class="m3-field__label">
								{{ t('module.user.login.field.password.placeholder') }}
							</label>
							<div class="m3-field__underline"></div>
						</div>
					</div>

					<div class="m3-login__actions">
						<LanguageSelect />
						<button type="submit" class="m3-btn m3-btn--filled">
							<span class="m3-btn__content">{{
								t('module.user.login.button')
							}}</span>
						</button>
					</div>
				</form>
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
						<button
							type="button"
							class="m3-btn m3-btn--outlined"
							@click="handleBackToLogin"
						>
							<span class="m3-btn__content">返回登录</span>
						</button>
						<button
							type="button"
							class="m3-btn m3-btn--filled"
							:disabled="!isTotpCodeComplete"
							@click="handleTotpSubmit"
						>
							<span class="m3-btn__content">验证</span>
						</button>
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

/* ========== M3 Filled Text Field ========== */
.m3-field {
	width: 100%;
}

.m3-field__container {
	position: relative;
	display: flex;
	align-items: center;
	background: var(--m3-surface-container-highest);
	border-radius: 4px 4px 0 0;
	height: 56px;
	cursor: text;
	transition: background 0.15s ease;
}

.m3-field__container:hover {
	background: var(--m3-surface-container-high);
}

.m3-field__container:focus-within {
	background: var(--m3-surface-container-highest);
}

.m3-field__input {
	width: 100%;
	height: 100%;
	padding: 24px 16px 8px;
	border: none;
	outline: none;
	background: transparent;
	color: var(--m3-on-surface);
	font-family: 'Roboto', system-ui, sans-serif;
	font-size: 16px;
	font-weight: 400;
	line-height: 24px;
	letter-spacing: 0.5px;
	box-sizing: border-box;
}

.m3-field__input::placeholder {
	color: transparent;
}

.m3-field__input:-webkit-autofill,
.m3-field__input:-webkit-autofill:hover,
.m3-field__input:-webkit-autofill:focus {
	-webkit-box-shadow: 0 0 0 1000px var(--m3-surface-container-highest) inset;
	-webkit-text-fill-color: var(--m3-on-surface);
	caret-color: var(--m3-primary);
	border-radius: 4px 4px 0 0;
}

.m3-field__label {
	position: absolute;
	left: 16px;
	top: 50%;
	transform: translateY(-50%);
	font-family: 'Roboto', system-ui, sans-serif;
	font-size: 16px;
	font-weight: 400;
	line-height: 24px;
	letter-spacing: 0.5px;
	color: var(--m3-on-surface-variant);
	pointer-events: none;
	transition: all 0.15s ease;
	transform-origin: left top;
}

.m3-field__input:focus + .m3-field__label,
.m3-field__input:not(:placeholder-shown) + .m3-field__label {
	top: 8px;
	transform: translateY(0) scale(0.75);
	color: var(--m3-primary);
	font-weight: 500;
}

.m3-field__underline {
	position: absolute;
	bottom: 0;
	left: 0;
	right: 0;
	height: 1px;
	background: var(--m3-outline);
	transition: all 0.15s ease;
}

.m3-field__container:hover .m3-field__underline {
	background: var(--m3-on-surface);
	height: 1px;
}

.m3-field__container:focus-within .m3-field__underline {
	background: var(--m3-primary);
	height: 2px;
}

/* ========== Actions (step 1) ========== */
.m3-login__actions {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-top: 8px;
	gap: 16px;
}

.m3-login__actions :deep(.lang-select) {
	flex-shrink: 0;
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

/* ========== M3 Filled Button ========== */
.m3-btn {
	position: relative;
	display: inline-flex;
	align-items: center;
	justify-content: center;
	min-width: 64px;
	height: 40px;
	padding: 0 24px;
	border: none;
	border-radius: 20px;
	cursor: pointer;
	font-family: 'Roboto', system-ui, sans-serif;
	font-size: 14px;
	font-weight: 500;
	line-height: 20px;
	letter-spacing: 0.1px;
	text-transform: none;
	overflow: hidden;
	transition: background 0.15s ease;
	-webkit-tap-highlight-color: transparent;
}

.m3-btn--filled {
	background: var(--m3-primary);
	color: var(--m3-on-primary);
}

.m3-btn--filled:hover {
	background: #66b1ff;
}

.m3-btn--filled:active {
	background: #3a8ee6;
}

.m3-btn--filled:disabled {
	background: rgba(31, 31, 31, 0.12);
	color: rgba(31, 31, 31, 0.38);
	cursor: not-allowed;
}

.m3-btn--filled:focus-visible {
	outline: 2px solid var(--m3-on-surface);
	outline-offset: 2px;
}

/* ========== M3 Outlined Button ========== */
.m3-btn--outlined {
	background: transparent;
	color: var(--m3-primary);
	border: 1px solid var(--m3-outline);
}

.m3-btn--outlined:hover {
	background: rgba(64, 158, 255, 0.08);
}

.m3-btn__content {
	position: relative;
	z-index: 1;
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

	.m3-btn {
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
