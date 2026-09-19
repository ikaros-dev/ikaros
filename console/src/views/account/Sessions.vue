<script setup lang="ts">
import { computed, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import { invalidateCurrentUserTokens } from "@/api/session";
import { getToken, removeToken } from "@/utils/auth";
import { getHttpErrorMessage } from "@/utils/http";
import PageCard from "@/views/console/PageCard.vue";

const { t, locale } = useI18n();
const loading = ref(false);
const token = getToken();
const expiresAt = computed(() => {
  if (!token?.expires) return t("sessionManagement.unknown");
  return new Intl.DateTimeFormat(locale.value === "zh" ? "zh-CN" : "en-US", {
    dateStyle: "medium",
    timeStyle: "medium"
  }).format(new Date(token.expires));
});

const invalidateTokens = async () => {
  try {
    await ElMessageBox.confirm(
      t("sessionManagement.invalidateConfirm"),
      t("sessionManagement.invalidateTitle"),
      {
        type: "warning",
        confirmButtonText: t("sessionManagement.invalidateConfirmButton"),
        cancelButtonText: t("buttons.pureClose")
      }
    );
  } catch (error) {
    if (error === "cancel" || error === "close") return;
    return;
  }

  loading.value = true;
  try {
    await invalidateCurrentUserTokens();
    ElMessage.success(t("sessionManagement.invalidateSuccess"));
    removeToken();
    window.location.replace("/login");
  } catch (error) {
    ElMessage.error(
      getHttpErrorMessage(error, t("sessionManagement.invalidateFailed"))
    );
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <PageCard>
    <div class="mt-6 space-y-5">
      <el-alert
        :title="t('sessionManagement.statelessTitle')"
        :description="t('sessionManagement.statelessDescription')"
        type="info"
        :closable="false"
        show-icon
      />

      <el-descriptions :column="1" border>
        <el-descriptions-item :label="t('sessionManagement.currentBrowser')">
          {{ t("sessionManagement.currentBrowserDescription") }}
        </el-descriptions-item>
        <el-descriptions-item
          :label="t('sessionManagement.accessTokenExpiresAt')"
        >
          {{ expiresAt }}
        </el-descriptions-item>
      </el-descriptions>

      <div
        class="flex items-center justify-between gap-4 rounded border border-[var(--el-color-warning-light-5)] bg-[var(--el-color-warning-light-9)] p-4"
      >
        <div>
          <div class="font-medium">
            {{ t("sessionManagement.invalidateTitle") }}
          </div>
          <p class="mt-1 text-sm text-[var(--el-text-color-secondary)]">
            {{ t("sessionManagement.invalidateDescription") }}
          </p>
        </div>
        <el-button type="warning" :loading="loading" @click="invalidateTokens">
          {{ t("sessionManagement.invalidateConfirmButton") }}
        </el-button>
      </div>
    </div>
  </PageCard>
</template>
