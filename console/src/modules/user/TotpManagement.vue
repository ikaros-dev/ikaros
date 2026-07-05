<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { onMounted, ref, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useUserStore } from '@/stores/user';
import axios from 'axios';
import QRCode from 'qrcode';

const { t } = useI18n();
const userStore = useUserStore();

const baseURL = import.meta.env.VITE_API_URL || '/api';
const totpBase = `${baseURL}/v1/security/auth/totp`;

// 状态
const totpEnabled = ref(false);
const isLoading = ref(false);

// 设置流程
const step = ref<'idle' | 'setup' | 'verify'>('idle');
const secret = ref('');
const otpAuthUri = ref('');
const qrDataUrl = ref('');
const verifyCode = ref(['', '', '', '', '', '']);
const verifyCodeRefs = ref<(HTMLInputElement | null)[]>([]);

const isVerifyCodeComplete = computed(() => {
  return verifyCode.value.every((d) => d !== '');
});

// 禁用流程
const disablePassword = ref('');

// 获取状态
async function fetchStatus() {
  try {
    const token = userStore.jwtToken;
    const { data, status } = await axios.get(`${totpBase}/status`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (status === 200) {
      totpEnabled.value = data.enabled;
    }
  } catch (e) {
    console.error('Failed to fetch TOTP status', e);
  }
}

// 开始设置
async function handleSetup() {
  isLoading.value = true;
  try {
    const token = userStore.jwtToken;
    const { data, status } = await axios.post(
      `${totpBase}/setup`,
      {},
      { headers: { Authorization: `Bearer ${token}` } }
    );
    if (status === 200) {
      secret.value = data.secret;
      otpAuthUri.value = data.otpAuthUri;
      step.value = 'verify';
    }
  } catch (e) {
    console.error('Failed to setup TOTP', e);
    ElMessage.error('生成密钥失败');
  } finally {
    isLoading.value = false;
  }
}

// otpAuthUri变化时生成二维码
watch(otpAuthUri, async (uri) => {
  if (uri) {
    try {
      qrDataUrl.value = await QRCode.toDataURL(uri, {
        width: 200,
        margin: 2,
        color: { dark: '#1f1f1f', light: '#ffffff' },
      });
    } catch (e) {
      console.error('Failed to generate QR code', e);
    }
  }
});

// 验证并启用
async function handleEnable() {
  const code = verifyCode.value.join('');
  if (code.length !== 6) return;
  isLoading.value = true;
  try {
    const token = userStore.jwtToken;
    const { status } = await axios.post(
      `${totpBase}/enable?code=${code}`,
      {},
      { headers: { Authorization: `Bearer ${token}` } }
    );
    if (status === 200) {
      ElMessage.success('二步验证已启用');
      totpEnabled.value = true;
      step.value = 'idle';
    }
  } catch (e) {
    console.error('Failed to enable TOTP', e);
    ElMessage.error('验证码错误，请重试');
    verifyCode.value = ['', '', '', '', '', ''];
    setTimeout(() => verifyCodeRefs.value[0]?.focus(), 100);
  } finally {
    isLoading.value = false;
  }
}

// 禁用
async function handleDisable() {
  if (!disablePassword.value) return;
  isLoading.value = true;
  try {
    const token = userStore.jwtToken;
    const { status } = await axios.post(
      `${totpBase}/disable?password=${encodeURIComponent(disablePassword.value)}`,
      {},
      { headers: { Authorization: `Bearer ${token}` } }
    );
    if (status === 200) {
      ElMessage.success('二步验证已关闭');
      totpEnabled.value = false;
      disablePassword.value = '';
    }
  } catch (e) {
    console.error('Failed to disable TOTP', e);
    ElMessage.error('关闭失败，请检查密码');
  } finally {
    isLoading.value = false;
  }
}

// 验证码输入处理
function handleVerifyInput(index: number) {
  if (verifyCode.value[index] && index < 5) {
    verifyCodeRefs.value[index + 1]?.focus();
  }
}

function handleVerifyKeydown(index: number, e: KeyboardEvent) {
  if (e.key === 'Backspace' && !verifyCode.value[index] && index > 0) {
    verifyCodeRefs.value[index - 1]?.focus();
  }
  if (e.key === 'Enter' && isVerifyCodeComplete.value) {
    handleEnable();
  }
}

function copySecret() {
  navigator.clipboard.writeText(secret.value)
    .then(() => ElMessage.success('密钥已复制'))
    .catch(() => ElMessage.error('复制失败'));
}

onMounted(() => {
  fetchStatus();
});
</script>

<template>
  <div class="m3-totp-page">
    <div class="m3-totp-page__card">
      <div class="m3-totp-page__header">
        <h1 class="m3-totp-page__title">二步验证</h1>
        <p class="m3-totp-page__subtitle">
          {{ totpEnabled ? '当前已启用' : '当前未启用' }}
        </p>
      </div>

      <!-- 已启用：关闭界面 -->
      <template v-if="totpEnabled && step === 'idle'">
        <div class="m3-totp-page__status">
          <div class="m3-totp-page__status-icon m3-totp-page__status-icon--enabled">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
              <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" fill="currentColor"/>
            </svg>
          </div>
          <p class="m3-totp-page__status-text">二步验证已启用，每次登录需要输入验证码</p>
        </div>
        <div class="m3-totp-page__actions">
          <button
            class="m3-btn m3-btn--outlined"
            :disabled="isLoading"
            @click="handleSetup"
          >
            重新绑定
          </button>
          <div class="m3-totp-page__disable">
            <div class="m3-field">
              <div class="m3-field__container">
                <input
                  id="disable-password"
                  v-model="disablePassword"
                  type="password"
                  class="m3-field__input"
                  placeholder="请输入当前密码"
                />
                <label for="disable-password" class="m3-field__label">当前密码</label>
                <div class="m3-field__underline"></div>
              </div>
            </div>
            <button
              class="m3-btn m3-btn--error"
              :disabled="isLoading || !disablePassword"
              @click="handleDisable"
            >
              关闭二步验证
            </button>
          </div>
        </div>
      </template>

      <!-- 未启用：引导开启 -->
      <template v-else-if="!totpEnabled && step === 'idle'">
        <div class="m3-totp-page__status">
          <div class="m3-totp-page__status-icon m3-totp-page__status-icon--disabled">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
              <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z" fill="currentColor"/>
            </svg>
          </div>
          <p class="m3-totp-page__status-text">开启二步验证以增强账户安全性</p>
        </div>
        <div class="m3-totp-page__actions">
          <button
            class="m3-btn m3-btn--filled"
            :disabled="isLoading"
            @click="handleSetup"
          >
            {{ isLoading ? '生成中...' : '开启二步验证' }}
          </button>
        </div>
      </template>

      <!-- 设置界面：显示密钥和 QR 码 -->
      <template v-else-if="step === 'verify'">
        <div class="m3-totp-page__setup">
          <p class="m3-totp-page__setup-title">
            使用 Authenticator 应用扫描此二维码或手动输入密钥
          </p>

          <!-- QR 码 -->
          <div class="m3-totp-page__qr-container" v-if="qrDataUrl">
            <img :src="qrDataUrl" alt="TOTP QR Code" width="200" height="200" />
          </div>

          <div class="m3-totp-page__secret">
            <span class="m3-totp-page__secret-label">密钥:</span>
            <code class="m3-totp-page__secret-value">{{ secret }}</code>
            <button
              class="m3-totp-page__secret-copy"
              @click="copySecret"
            >
              复制
            </button>
          </div>

          <p class="m3-totp-page__setup-title" style="margin-top: 24px;">
            输入 Authenticator 应用中显示的 6 位验证码完成验证
          </p>

          <div class="m3-totp__inputs">
            <input
              v-for="(digit, index) in verifyCode"
              :key="index"
              :ref="(el) => { verifyCodeRefs[index] = el as HTMLInputElement; }"
              v-model="verifyCode[index]"
              type="text"
              maxlength="1"
              class="m3-totp__digit"
              inputmode="numeric"
              pattern="[0-9]"
              autocomplete="off"
              @input="handleVerifyInput(index)"
              @keydown="handleVerifyKeydown(index, $event)"
            />
          </div>

          <div class="m3-totp-page__actions" style="margin-top: 24px;">
            <button
              class="m3-btn m3-btn--outlined"
              :disabled="isLoading"
              @click="step = 'idle'"
            >
              取消
            </button>
            <button
              class="m3-btn m3-btn--filled"
              :disabled="!isVerifyCodeComplete || isLoading"
              @click="handleEnable"
            >
              {{ isLoading ? '验证中...' : '验证并启用' }}
            </button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.m3-totp-page {
  --m3-primary: #409eff;
  --m3-on-primary: #ffffff;
  --m3-primary-container: #d6e4ff;
  --m3-on-primary-container: #001b3e;
  --m3-surface: #ffffff;
  --m3-surface-dim: #f0f5fa;
  --m3-surface-container: #ffffff;
  --m3-surface-container-high: #f8faff;
  --m3-surface-container-highest: #eef3f8;
  --m3-on-surface: #1f1f1f;
  --m3-on-surface-variant: #44474f;
  --m3-outline: #c4c6d0;
  --m3-error: #ba1a1a;
  --m3-elevation-3: 0 4px 8px 0 rgba(0,0,0,0.04), 0 1px 3px 0 rgba(0,0,0,0.04);
  --m3-elevation-5: 0 8px 16px 0 rgba(0,0,0,0.04), 0 2px 6px 0 rgba(0,0,0,0.04);

  max-width: 480px;
  margin: 0 auto;
  padding: 24px;
  box-sizing: border-box;
}

.m3-totp-page__card {
  background: var(--m3-surface-container);
  border-radius: 28px;
  box-shadow: var(--m3-elevation-5);
  padding: 40px 32px 32px;
  box-sizing: border-box;
}

.m3-totp-page__header {
  margin-bottom: 32px;
  text-align: center;
}

.m3-totp-page__title {
  margin: 0 0 4px;
  font-family: 'Roboto', system-ui, sans-serif;
  font-size: 28px;
  font-weight: 400;
  line-height: 36px;
  color: var(--m3-on-surface);
}

.m3-totp-page__subtitle {
  margin: 0;
  font-family: 'Roboto', system-ui, sans-serif;
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
  letter-spacing: 0.25px;
  color: var(--m3-on-surface-variant);
}

.m3-totp-page__status {
  text-align: center;
  margin-bottom: 32px;
}

.m3-totp-page__status-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 36px;
  margin-bottom: 16px;
}

.m3-totp-page__status-icon--enabled {
  background: var(--m3-primary-container);
  color: var(--m3-primary);
}

.m3-totp-page__status-icon--disabled {
  background: var(--m3-surface-container-highest);
  color: var(--m3-on-surface-variant);
}

.m3-totp-page__status-text {
  margin: 0;
  font-family: 'Roboto', system-ui, sans-serif;
  font-size: 15px;
  font-weight: 400;
  line-height: 22px;
  letter-spacing: 0.25px;
  color: var(--m3-on-surface-variant);
}

.m3-totp-page__actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: center;
}

.m3-totp-page__disable {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* ========== Setup ========== */
.m3-totp-page__setup {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.m3-totp-page__setup-title {
  margin: 0;
  font-family: 'Roboto', system-ui, sans-serif;
  font-size: 14px;
  font-weight: 400;
  line-height: 20px;
  letter-spacing: 0.25px;
  color: var(--m3-on-surface-variant);
  text-align: center;
}

.m3-totp-page__qr-container {
  text-align: center;
  margin: 16px 0;
}

.m3-totp-page__qr-container img {
  display: block;
  margin: 0 auto;
  border-radius: 12px;
  padding: 12px;
  background: #fff;
}

.m3-totp-page__secret {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: var(--m3-surface-container-highest);
  border-radius: 12px;
  padding: 12px 16px;
  margin: 8px 0;
}

.m3-totp-page__secret-label {
  font-family: 'Roboto', system-ui, sans-serif;
  font-size: 13px;
  font-weight: 500;
  color: var(--m3-on-surface-variant);
  white-space: nowrap;
}

.m3-totp-page__secret-value {
  font-family: 'Roboto Mono', 'Cascadia Code', monospace;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 2px;
  color: var(--m3-on-surface);
  word-break: break-all;
}

.m3-totp-page__secret-copy {
  background: transparent;
  border: none;
  color: var(--m3-primary);
  cursor: pointer;
  font-family: 'Roboto', system-ui, sans-serif;
  font-size: 13px;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 8px;
  white-space: nowrap;
  transition: background 0.15s ease;
}

.m3-totp-page__secret-copy:hover {
  background: rgba(64, 158, 255, 0.08);
}

/* ========== TOTP Input ========== */
.m3-totp__inputs {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 12px;
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
  transition: background 0.15s ease, box-shadow 0.15s ease;
}

.m3-totp__digit:focus {
  background: var(--m3-surface-container-high);
  box-shadow: inset 0 0 0 2px var(--m3-primary);
}

/* ========== M3 Filled Button ========== */
.m3-btn {
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
  overflow: hidden;
  transition: background 0.15s ease;
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

.m3-btn--outlined {
  background: transparent;
  color: var(--m3-primary);
  border: 1px solid var(--m3-outline);
}

.m3-btn--outlined:hover {
  background: rgba(64, 158, 255, 0.08);
}

.m3-btn--error {
  background: var(--m3-error);
  color: #ffffff;
}

.m3-btn--error:hover {
  background: #d32f2f;
}

/* ========== M3 Filled Text Field (for disable password) ========== */
.m3-field {
  width: 100%;
}

.m3-field__container {
  position: relative;
  display: flex;
  align-items: center;
  background: var(--m3-surface-container-highest);
  border-radius: 4px 4px 0 0;
  height: 48px;
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
  padding: 20px 16px 4px;
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
  top: 6px;
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

.m3-field__container:focus-within .m3-field__underline {
  background: var(--m3-primary);
  height: 2px;
}

@media (max-width: 480px) {
  .m3-totp-page__card {
    padding: 24px 20px;
    border-radius: 24px;
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
