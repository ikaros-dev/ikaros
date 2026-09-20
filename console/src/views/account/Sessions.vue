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
const query = ref("");
const currentPage = ref(1);
const pageSize = ref(20);
const token = getToken();

const formatDate = (value: number | null) => {
  if (!value) return t("sessionManagement.unknown");
  return new Intl.DateTimeFormat(locale.value === "zh" ? "zh-CN" : "en-US", {
    dateStyle: "medium",
    timeStyle: "medium"
  }).format(new Date(value));
};

const readIssuedAt = (): number | null => {
  const accessToken = token?.accessToken;
  if (!accessToken) return null;
  try {
    const payload = accessToken.split(".")[1];
    if (!payload) return null;
    const decoded = JSON.parse(
      atob(payload.replace(/-/g, "+").replace(/_/g, "/"))
    );
    return typeof decoded.iat === "number" ? decoded.iat * 1000 : null;
  } catch {
    return null;
  }
};

const record = computed(() => ({
  subject: token?.actorId || t("sessionManagement.unknown"),
  issuedAt: readIssuedAt(),
  expiresAt: token?.expires ? new Date(token.expires).getTime() : null,
  device: t("sessionManagement.currentBrowserDescription"),
  ip: t("sessionManagement.unavailable")
}));

const filteredRecords = computed(() => {
  if (!query.value.trim()) return [record.value];
  const normalized = query.value.trim().toLowerCase();
  return [record.value].filter(item =>
    [item.subject, item.device, item.ip].some(value =>
      value.toLowerCase().includes(normalized)
    )
  );
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

const search = () => {
  currentPage.value = 1;
};

const reset = () => {
  query.value = "";
  search();
};
</script>

<template>
  <PageCard>
    <div class="mt-6">
      <el-alert
        :title="t('sessionManagement.statelessTitle')"
        :description="t('sessionManagement.statelessDescription')"
        type="info"
        :closable="false"
        show-icon
      />

      <el-form :inline="true" class="mt-5" @submit.prevent="search">
        <el-form-item :label="t('sessionManagement.search')">
          <el-input
            v-model="query"
            clearable
            :placeholder="t('sessionManagement.searchPlaceholder')"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">{{
            t("sessionManagement.searchButton")
          }}</el-button>
          <el-button @click="reset">{{
            t("sessionManagement.reset")
          }}</el-button>
        </el-form-item>
      </el-form>

      <div class="mb-4 flex items-center justify-between">
        <span class="text-sm text-[var(--el-text-color-secondary)]">{{
          t("sessionManagement.resultHint")
        }}</span>
        <el-button :loading="loading" @click="reset">{{
          t("sessionManagement.refresh")
        }}</el-button>
      </div>

      <el-table v-loading="loading" :data="filteredRecords" border>
        <el-table-column
          prop="subject"
          :label="t('sessionManagement.subject')"
          min-width="190"
        />
        <el-table-column
          :label="t('sessionManagement.issuedAt')"
          min-width="190"
        >
          <template #default="{ row }">{{ formatDate(row.issuedAt) }}</template>
        </el-table-column>
        <el-table-column
          :label="t('sessionManagement.expiresAt')"
          min-width="190"
        >
          <template #default="{ row }">{{
            formatDate(row.expiresAt)
          }}</template>
        </el-table-column>
        <el-table-column
          prop="ip"
          :label="t('sessionManagement.ip')"
          min-width="150"
        />
        <el-table-column
          prop="device"
          :label="t('sessionManagement.device')"
          min-width="220"
        />
        <el-table-column
          fixed="right"
          :label="t('sessionManagement.actions')"
          width="180"
        >
          <template #default>
            <el-button
              type="warning"
              link
              :loading="loading"
              @click="invalidateTokens"
            >
              {{ t("sessionManagement.invalidateConfirmButton") }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-4 flex justify-end">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="filteredRecords.length"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :disabled="filteredRecords.length === 0"
        />
      </div>

      <p class="mt-4 text-sm text-[var(--el-text-color-secondary)]">
        {{ t("sessionManagement.invalidateDescription") }}
      </p>
    </div>
  </PageCard>
</template>
