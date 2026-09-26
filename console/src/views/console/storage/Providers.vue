<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import PageCard from "@/views/console/PageCard.vue";
import {
  getStorageProvider,
  getStorageProviderStatus,
  listStorageProviders,
  probeStorageProvider,
  type StorageProvider,
  type StorageProviderStatus
} from "@/api/storageProvider";
import { getHttpErrorMessage } from "@/utils/http";

const { t } = useI18n();
const loading = ref(false);
const providers = ref<StorageProvider[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const probingId = ref("");
const detailVisible = ref(false);
const detailLoading = ref(false);
const selectedProvider = ref<StorageProvider | null>(null);
const providerStatus = ref<StorageProviderStatus | null>(null);
const form = reactive({ query: "", tier: "", enabled: "" });

const filteredProviders = computed(() => {
  const query = form.query.trim().toLowerCase();
  return providers.value.filter(provider =>
    (!query || provider.provider_key.toLowerCase().includes(query)
      || provider.display_name.toLowerCase().includes(query)
      || provider.provider_type.toLowerCase().includes(query))
    && (!form.tier || provider.tier === form.tier)
    && (form.enabled === "" || provider.enabled === (form.enabled === "true"))
  );
});
const pageProviders = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredProviders.value.slice(start, start + pageSize.value);
});

const loadProviders = async () => {
  loading.value = true;
  try {
    providers.value = await listStorageProviders();
    const pageCount = Math.max(1, Math.ceil(filteredProviders.value.length / pageSize.value));
    if (currentPage.value > pageCount) currentPage.value = pageCount;
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("storageProviderManagement.loadFailed")));
  } finally {
    loading.value = false;
  }
};

const search = () => {
  currentPage.value = 1;
};
const reset = () => {
  form.query = "";
  form.tier = "";
  form.enabled = "";
  search();
};

const probe = async (provider: StorageProvider) => {
  probingId.value = provider.id;
  try {
    const result = await probeStorageProvider(provider.id);
    ElMessage({
      type: result.status === "HEALTHY" ? "success" : "warning",
      message: t(`storageProviderManagement.probeResult.${result.status}`)
    });
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("storageProviderManagement.probeFailed")));
  } finally {
    probingId.value = "";
  }
};

const openDetail = async (provider: StorageProvider) => {
  selectedProvider.value = provider;
  providerStatus.value = null;
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    const [detail, status] = await Promise.all([
      getStorageProvider(provider.id),
      getStorageProviderStatus(provider.id)
    ]);
    selectedProvider.value = detail;
    providerStatus.value = status;
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("storageProviderManagement.detailFailed")));
    detailVisible.value = false;
  } finally {
    detailLoading.value = false;
  }
};

onMounted(() => void loadProviders());
</script>

<template>
  <PageCard>
    <el-form :inline="true" :model="form" class="mt-6" @submit.prevent="search">
      <el-form-item :label="t('storageProviderManagement.query')">
        <el-input v-model="form.query" clearable @keyup.enter="search" />
      </el-form-item>
      <el-form-item :label="t('storageProviderManagement.tier')">
        <el-select v-model="form.tier" clearable class="w-36">
          <el-option v-for="tier in ['HOT', 'WARM', 'COLD', 'ARCHIVE']" :key="tier" :label="tier" :value="tier" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('storageProviderManagement.status')">
        <el-select v-model="form.enabled" clearable class="w-36">
          <el-option :label="t('storageProviderManagement.enabled')" value="true" />
          <el-option :label="t('storageProviderManagement.disabled')" value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">{{ t("storageProviderManagement.search") }}</el-button>
        <el-button @click="reset">{{ t("storageProviderManagement.reset") }}</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4 flex justify-end">
      <el-button :loading="loading" @click="loadProviders">{{ t("storageProviderManagement.refresh") }}</el-button>
    </div>

    <el-table v-loading="loading" :data="pageProviders" row-key="id" border>
      <el-table-column prop="display_name" :label="t('storageProviderManagement.name')" min-width="150" />
      <el-table-column prop="provider_key" :label="t('storageProviderManagement.key')" min-width="150" />
      <el-table-column prop="provider_type" :label="t('storageProviderManagement.type')" min-width="150" />
      <el-table-column prop="tier" :label="t('storageProviderManagement.tier')" width="110" />
      <el-table-column :label="t('storageProviderManagement.status')" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.enabled ? 'success' : 'info'">
            {{ scope.row.enabled ? t("storageProviderManagement.enabled") : t("storageProviderManagement.disabled") }}
          </el-tag>
          <el-tag v-if="scope.row.drain_status !== 'NORMAL'" class="ml-2" type="warning">
            {{ scope.row.drain_status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('storageProviderManagement.actions')" width="170" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row)">{{ t("storageProviderManagement.details") }}</el-button>
          <el-button link type="primary" :loading="probingId === scope.row.id" @click="probe(scope.row)">
            {{ t("storageProviderManagement.probe") }}
          </el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty :description="t('storageProviderManagement.empty')" /></template>
    </el-table>

    <div class="mt-4 flex justify-end">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="filteredProviders.length"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>

    <el-drawer v-model="detailVisible" :title="t('storageProviderManagement.details')" size="520px">
      <el-skeleton v-if="detailLoading" :rows="6" animated />
      <template v-else-if="selectedProvider">
        <el-descriptions :column="1" border>
          <el-descriptions-item :label="t('storageProviderManagement.name')">{{ selectedProvider.display_name }}</el-descriptions-item>
          <el-descriptions-item :label="t('storageProviderManagement.key')">{{ selectedProvider.provider_key }}</el-descriptions-item>
          <el-descriptions-item :label="t('storageProviderManagement.type')">{{ selectedProvider.provider_type }}</el-descriptions-item>
          <el-descriptions-item :label="t('storageProviderManagement.tier')">{{ selectedProvider.tier }}</el-descriptions-item>
          <el-descriptions-item :label="t('storageProviderManagement.status')">
            {{ selectedProvider.enabled ? t("storageProviderManagement.enabled") : t("storageProviderManagement.disabled") }}
            / {{ selectedProvider.drain_status }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('storageProviderManagement.version')">{{ selectedProvider.version }}</el-descriptions-item>
          <el-descriptions-item :label="t('storageProviderManagement.capabilities')">
            <pre class="whitespace-pre-wrap">{{ JSON.stringify(selectedProvider.capabilities, null, 2) }}</pre>
          </el-descriptions-item>
          <el-descriptions-item :label="t('storageProviderManagement.configuration')">
            <pre class="whitespace-pre-wrap">{{ JSON.stringify(selectedProvider.configuration, null, 2) }}</pre>
          </el-descriptions-item>
        </el-descriptions>
        <el-divider />
        <template v-if="providerStatus">
          <div class="mb-2 font-medium">{{ t("storageProviderManagement.health") }}: {{ providerStatus.health.status }}</div>
          <div class="mb-2">{{ t("storageProviderManagement.connection") }}: {{ providerStatus.health.connection }}</div>
          <div class="mb-2">{{ t("storageProviderManagement.read") }}: {{ providerStatus.health.read }}</div>
          <div class="mb-2">{{ t("storageProviderManagement.write") }}: {{ providerStatus.health.write }}</div>
          <div v-if="providerStatus.capacity_bytes != null" class="mb-2">
            {{ t("storageProviderManagement.capacity") }}: {{ providerStatus.capacity_bytes }}
          </div>
        </template>
      </template>
    </el-drawer>
  </PageCard>
</template>
