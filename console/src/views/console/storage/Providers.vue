<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import PageCard from "@/views/console/PageCard.vue";
import {
  getStorageProvider,
  getStorageProviderStatus,
  listStorageProviders,
  probeStorageProvider,
  createStorageProvider,
  updateStorageProvider,
  replaceStorageProviderCredentials,
  enableStorageProvider,
  disableStorageProvider,
  deleteStorageProvider,
  type CreateStorageProviderRequest,
  type UpdateStorageProviderRequest,
  type StorageProvider,
  type StorageProviderStatus
} from "@/api/storageProvider";
import {
  createDeliveryBinding,
  createDeliveryProvider,
  deleteDeliveryBinding,
  deleteDeliveryProvider,
  disableDeliveryProvider,
  enableDeliveryProvider,
  listDeliveryBindings,
  listDeliveryProviders,
  updateDeliveryBinding,
  updateDeliveryProvider,
  type DeliveryBinding,
  type DeliveryBindingCacheKeyPolicy,
  type DeliveryBindingRangePolicy,
  type DeliveryBindingRequest,
  type DeliveryProvider,
  type DeliveryProviderType,
  type DeliveryProviderWriteRequest
} from "@/api/deliveryProvider";
import { getHttpErrorMessage } from "@/utils/http";
import { useStepUpVerification } from "@/composables/useStepUpVerification";

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
const createVisible = ref(false);
const createLoading = ref(false);
const mutatingId = ref("");
const createForm = reactive({
  provider_key: "",
  provider_type: "S3",
  display_name: "",
  tier: "HOT" as StorageProvider["tier"],
  access_key_id: "",
  secret_access_key: "",
  session_token: "",
  endpoint: "",
  bucket: "",
  region: ""
});
const editVisible = ref(false);
const editLoading = ref(false);
const editTarget = ref<StorageProvider | null>(null);
const editForm = reactive({
  provider_type: "S3",
  display_name: "",
  tier: "HOT" as StorageProvider["tier"],
  endpoint: "",
  bucket: "",
  region: "",
  access_key_id: "",
  secret_access_key: "",
  session_token: ""
});
const stepUp = useStepUpVerification();
const pendingMutation = ref<(() => Promise<void>) | null>(null);
const verificationVisible = stepUp.visible;
const verificationLoading = stepUp.loading;
const verificationCode = stepUp.code;
const form = reactive({ query: "", tier: "", enabled: "" });
const providerTypes = ["S3", "AWS_S3", "S3_COMPATIBLE", "ALIYUN_OSS_S3", "TENCENT_COS_S3", "LOCAL_FILESYSTEM"];

const deliveryVisible = ref(false);
const deliveryTarget = ref<StorageProvider | null>(null);
const deliveryLoading = ref(false);
const deliveryBindings = ref<DeliveryBinding[]>([]);
const deliveryProviders = ref<DeliveryProvider[]>([]);
const bindingVisible = ref(false);
const bindingLoading = ref(false);
const bindingMutatingId = ref("");
const bindingTarget = ref<DeliveryBinding | null>(null);
const providerFormVisible = ref(false);
const providerFormLoading = ref(false);
const providerListLoading = ref(false);
const providerMutatingId = ref("");
const providerFormTarget = ref<DeliveryProvider | null>(null);
const deliveryProviderTypes: DeliveryProviderType[] = ["DIRECT", "CDN", "SERVER_PROXY"];
const providerForm = reactive({
  providerKey: "",
  providerType: "CDN" as DeliveryProviderType,
  displayName: "",
  endpoint: "",
  credentialRef: "",
  enabled: true
});
const cacheKeyPolicies: DeliveryBindingCacheKeyPolicy[] = ["CONTENT_IDENTITY", "FULL_REQUEST", "NO_CACHE"];
const rangePolicies: DeliveryBindingRangePolicy[] = ["PASSTHROUGH", "FIXED_CHUNK", "UNSUPPORTED"];
const bindingForm = reactive({
  deliveryProviderKey: "",
  priority: 10,
  enabled: true,
  cacheKeyPolicy: "CONTENT_IDENTITY" as DeliveryBindingCacheKeyPolicy,
  rangePolicy: "PASSTHROUGH" as DeliveryBindingRangePolicy,
  fallbackParticipation: false
});

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

const runWithVerification = async (action: () => Promise<void>) => {
  pendingMutation.value = action;
  try {
    await stepUp.request("EMAIL_OTP", action);
    if (!stepUp.visible.value) pendingMutation.value = null;
  } catch (error) {
    pendingMutation.value = null;
    ElMessage.error(getHttpErrorMessage(error, t("storageProviderManagement.actionFailed")));
  }
};

const verifyAndExecute = async () => {
  try {
    const grant = await stepUp.verify();
    if (!grant || !pendingMutation.value) return;
    const action = pendingMutation.value;
    pendingMutation.value = null;
    await action();
    stepUp.close();
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("storageProviderManagement.actionFailed")));
    stepUp.close();
  }
};

const closeVerification = () => {
  stepUp.close();
  pendingMutation.value = null;
  probingId.value = "";
};

const resetCreateForm = () => {
  Object.assign(createForm, {
    provider_key: "",
    provider_type: "S3",
    display_name: "",
    tier: "HOT",
    access_key_id: "",
    secret_access_key: "",
    session_token: "",
    endpoint: "",
    bucket: "",
    region: ""
  });
};

// 只在新增成功后清空表单；关闭弹窗、二次验证取消或创建失败都保留已填内容。
const openCreate = () => {
  createVisible.value = true;
};

const submitCreate = async () => {
  if (!createForm.provider_key.trim() || !createForm.provider_type.trim()
    || !createForm.display_name.trim()) return;
  const accessKeyId = createForm.access_key_id.trim();
  const secretAccessKey = createForm.secret_access_key.trim();
  if (Boolean(accessKeyId) !== Boolean(secretAccessKey)) {
    ElMessage.warning(t("storageProviderManagement.credentialsPaired"));
    return;
  }
  const configuration = Object.fromEntries(
    Object.entries({ endpoint: createForm.endpoint, bucket: createForm.bucket, region: createForm.region })
      .filter(([, value]) => value.trim())
  );
  const request: CreateStorageProviderRequest = {
    provider_key: createForm.provider_key.trim(),
    provider_type: createForm.provider_type.trim(),
    display_name: createForm.display_name.trim(),
    tier: createForm.tier,
    capabilities: {},
    configuration
  };
  if (accessKeyId && secretAccessKey) {
    request.access_key_id = accessKeyId;
    request.secret_access_key = secretAccessKey;
    if (createForm.session_token.trim()) request.session_token = createForm.session_token.trim();
  }
  createVisible.value = false;
  try {
    await runWithVerification(async () => {
      createLoading.value = true;
      try {
        await createStorageProvider(request, crypto.randomUUID());
        ElMessage.success(t("storageProviderManagement.createSuccess"));
        resetCreateForm();
        await loadProviders();
      } finally {
        createLoading.value = false;
      }
    });
  } finally {
    createLoading.value = false;
  }
};

const openEdit = (provider: StorageProvider) => {
  editTarget.value = provider;
  const configuration = provider.configuration ?? {};
  Object.assign(editForm, {
    provider_type: providerTypes.includes(provider.provider_type) ? provider.provider_type : providerTypes[0],
    display_name: provider.display_name,
    tier: provider.tier,
    endpoint: String(configuration.endpoint ?? ""),
    bucket: String(configuration.bucket ?? ""),
    region: String(configuration.region ?? ""),
    access_key_id: "",
    secret_access_key: "",
    session_token: ""
  });
  editVisible.value = true;
};

const submitEdit = async () => {
  const provider = editTarget.value;
  if (!provider || !editForm.display_name.trim() || !editForm.provider_type.trim()) return;
  const accessKeyId = editForm.access_key_id.trim();
  const secretAccessKey = editForm.secret_access_key.trim();
  if (Boolean(accessKeyId) !== Boolean(secretAccessKey)) {
    ElMessage.warning(t("storageProviderManagement.credentialsPaired"));
    return;
  }
  const configuration = Object.fromEntries(
    Object.entries({ endpoint: editForm.endpoint, bucket: editForm.bucket, region: editForm.region })
      .filter(([, value]) => value.trim())
  );
  const request: UpdateStorageProviderRequest = {
    provider_type: editForm.provider_type,
    display_name: editForm.display_name.trim(),
    tier: editForm.tier,
    configuration
  };
  editVisible.value = false;
  await runWithVerification(async () => {
    editLoading.value = true;
    try {
      await updateStorageProvider(provider.id, request, provider.version);
      if (accessKeyId && secretAccessKey) {
        await replaceStorageProviderCredentials(provider.id, {
          access_key_id: accessKeyId,
          secret_access_key: secretAccessKey,
          ...(editForm.session_token.trim() ? { session_token: editForm.session_token.trim() } : {})
        });
      }
      ElMessage.success(t("storageProviderManagement.updateSuccess"));
      await loadProviders();
    } finally {
      editLoading.value = false;
    }
  });
};

const setEnabled = async (provider: StorageProvider, enabled: boolean) => {
  await runWithVerification(async () => {
    mutatingId.value = provider.id;
    try {
      if (enabled) await enableStorageProvider(provider.id);
      else await disableStorageProvider(provider.id);
      ElMessage.success(t(enabled ? "storageProviderManagement.enableSuccess" : "storageProviderManagement.disableSuccess"));
      await loadProviders();
    } finally {
      mutatingId.value = "";
    }
  });
};

const removeProvider = async (provider: StorageProvider) => {
  try {
    await ElMessageBox.confirm(
      t("storageProviderManagement.deleteConfirm", { name: provider.display_name }),
      t("storageProviderManagement.deleteTitle"),
      { type: "warning", confirmButtonText: t("buttons.pureConfirm"), cancelButtonText: t("buttons.pureClose") }
    );
  } catch {
    return;
  }
  await runWithVerification(async () => {
    mutatingId.value = provider.id;
    try {
      await deleteStorageProvider(provider.id, provider.version);
      ElMessage.success(t("storageProviderManagement.deleteSuccess"));
      await loadProviders();
    } finally {
      mutatingId.value = "";
    }
  });
};

const deliveryProviderLabel = (providerKey: string) => {
  const provider = deliveryProviders.value.find(item => item.providerKey === providerKey);
  if (!provider) return providerKey;
  return `${provider.displayName || provider.providerKey} (${provider.providerKey})`;
};

const loadDeliveryProviders = async () => {
  providerListLoading.value = true;
  try {
    deliveryProviders.value = await listDeliveryProviders();
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("storageProviderManagement.deliveryProviderLoadFailed")));
    deliveryProviders.value = [];
  } finally {
    providerListLoading.value = false;
  }
};

const loadDeliveryBindings = async () => {
  const provider = deliveryTarget.value;
  if (!provider) return;
  deliveryLoading.value = true;
  try {
    deliveryBindings.value = await listDeliveryBindings(provider.id);
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("storageProviderManagement.deliveryLoadFailed")));
  } finally {
    deliveryLoading.value = false;
  }
};

const refreshDelivery = async () => {
  await Promise.all([loadDeliveryProviders(), loadDeliveryBindings()]);
};

const openDelivery = async (provider: StorageProvider) => {
  deliveryTarget.value = provider;
  deliveryVisible.value = true;
  await refreshDelivery();
};

const openProviderCreate = () => {
  providerFormTarget.value = null;
  Object.assign(providerForm, {
    providerKey: "",
    providerType: "CDN",
    displayName: "",
    endpoint: "",
    credentialRef: "",
    enabled: true
  });
  providerFormVisible.value = true;
};

const openProviderEdit = (provider: DeliveryProvider) => {
  providerFormTarget.value = provider;
  Object.assign(providerForm, {
    providerKey: provider.providerKey,
    providerType: provider.providerType,
    displayName: provider.displayName,
    endpoint: String(provider.config?.endpoint ?? ""),
    credentialRef: provider.credentialRef ?? "",
    enabled: provider.enabled
  });
  providerFormVisible.value = true;
};

const submitProvider = async () => {
  if (!providerForm.providerKey.trim() || !providerForm.displayName.trim()) return;
  const request: DeliveryProviderWriteRequest = {
    providerKey: providerForm.providerKey.trim(),
    providerType: providerForm.providerType,
    displayName: providerForm.displayName.trim(),
    credentialRef: providerForm.credentialRef.trim() || null,
    config: providerForm.endpoint.trim() ? { endpoint: providerForm.endpoint.trim() } : {},
    enabled: providerForm.enabled
  };
  const target = providerFormTarget.value;
  providerFormVisible.value = false;
  await runWithVerification(async () => {
    providerFormLoading.value = true;
    try {
      if (target) {
        await updateDeliveryProvider(target.id, request, target.version);
      } else {
        await createDeliveryProvider(request, crypto.randomUUID());
      }
      ElMessage.success(t(target
        ? "storageProviderManagement.deliveryProviderUpdateSuccess"
        : "storageProviderManagement.deliveryProviderCreateSuccess"));
      await refreshDelivery();
    } finally {
      providerFormLoading.value = false;
    }
  });
};

const toggleDeliveryProvider = async (provider: DeliveryProvider) => {
  await runWithVerification(async () => {
    providerMutatingId.value = provider.id;
    try {
      if (provider.enabled) await disableDeliveryProvider(provider.id);
      else await enableDeliveryProvider(provider.id);
      ElMessage.success(t(provider.enabled
        ? "storageProviderManagement.deliveryProviderDisableSuccess"
        : "storageProviderManagement.deliveryProviderEnableSuccess"));
      await refreshDelivery();
    } finally {
      providerMutatingId.value = "";
    }
  });
};

const removeDeliveryProvider = async (provider: DeliveryProvider) => {
  try {
    await ElMessageBox.confirm(
      t("storageProviderManagement.deliveryProviderDeleteConfirm", {
        name: provider.displayName || provider.providerKey
      }),
      t("storageProviderManagement.deliveryProviderDeleteTitle"),
      { type: "warning", confirmButtonText: t("buttons.pureConfirm"), cancelButtonText: t("buttons.pureClose") }
    );
  } catch {
    return;
  }
  await runWithVerification(async () => {
    providerMutatingId.value = provider.id;
    try {
      await deleteDeliveryProvider(provider.id);
      ElMessage.success(t("storageProviderManagement.deliveryProviderDeleteSuccess"));
      await refreshDelivery();
    } finally {
      providerMutatingId.value = "";
    }
  });
};

const openBindingCreate = () => {
  bindingTarget.value = null;
  const maxPriority = deliveryBindings.value.reduce((max, item) => Math.max(max, item.priority), 0);
  Object.assign(bindingForm, {
    deliveryProviderKey: deliveryProviders.value[0]?.providerKey ?? "",
    priority: maxPriority + 10,
    enabled: true,
    cacheKeyPolicy: "CONTENT_IDENTITY",
    rangePolicy: "PASSTHROUGH",
    fallbackParticipation: false
  });
  bindingVisible.value = true;
};

const openBindingEdit = (binding: DeliveryBinding) => {
  bindingTarget.value = binding;
  Object.assign(bindingForm, {
    deliveryProviderKey: binding.deliveryProviderKey,
    priority: binding.priority,
    enabled: binding.enabled,
    cacheKeyPolicy: binding.cacheKeyPolicy,
    rangePolicy: binding.rangePolicy,
    fallbackParticipation: binding.fallbackParticipation
  });
  bindingVisible.value = true;
};

const submitBinding = async () => {
  const provider = deliveryTarget.value;
  if (!provider || !bindingForm.deliveryProviderKey.trim()) return;
  const request: DeliveryBindingRequest = {
    deliveryProviderKey: bindingForm.deliveryProviderKey.trim(),
    priority: Math.max(0, Number(bindingForm.priority) || 0),
    enabled: bindingForm.enabled,
    cacheKeyPolicy: bindingForm.cacheKeyPolicy,
    rangePolicy: bindingForm.rangePolicy,
    fallbackParticipation: bindingForm.fallbackParticipation
  };
  const target = bindingTarget.value;
  bindingVisible.value = false;
  await runWithVerification(async () => {
    bindingLoading.value = true;
    try {
      if (target && target.version != null) {
        await updateDeliveryBinding(provider.id, target.id, request, target.version);
      } else {
        await createDeliveryBinding(provider.id, request);
      }
      ElMessage.success(t("storageProviderManagement.bindingSaveSuccess"));
      await loadDeliveryBindings();
    } finally {
      bindingLoading.value = false;
    }
  });
};

const toggleBinding = async (binding: DeliveryBinding) => {
  const provider = deliveryTarget.value;
  if (!provider || binding.version == null) return;
  await runWithVerification(async () => {
    bindingMutatingId.value = binding.id;
    try {
      await updateDeliveryBinding(
        provider.id,
        binding.id,
        {
          deliveryProviderKey: binding.deliveryProviderKey,
          priority: binding.priority,
          enabled: !binding.enabled,
          cacheKeyPolicy: binding.cacheKeyPolicy,
          rangePolicy: binding.rangePolicy,
          fallbackParticipation: binding.fallbackParticipation
        },
        binding.version as number
      );
      ElMessage.success(t(
        binding.enabled
          ? "storageProviderManagement.bindingDisableSuccess"
          : "storageProviderManagement.bindingEnableSuccess"
      ));
      await loadDeliveryBindings();
    } finally {
      bindingMutatingId.value = "";
    }
  });
};

const removeBinding = async (binding: DeliveryBinding) => {
  const provider = deliveryTarget.value;
  if (!provider) return;
  try {
    await ElMessageBox.confirm(
      t("storageProviderManagement.bindingDeleteConfirm", {
        key: deliveryProviderLabel(binding.deliveryProviderKey)
      }),
      t("storageProviderManagement.bindingDeleteTitle"),
      { type: "warning", confirmButtonText: t("buttons.pureConfirm"), cancelButtonText: t("buttons.pureClose") }
    );
  } catch {
    return;
  }
  await runWithVerification(async () => {
    bindingMutatingId.value = binding.id;
    try {
      await deleteDeliveryBinding(provider.id, binding.id);
      ElMessage.success(t("storageProviderManagement.bindingDeleteSuccess"));
      await loadDeliveryBindings();
    } finally {
      bindingMutatingId.value = "";
    }
  });
};

const probe = async (provider: StorageProvider) => {
  probingId.value = provider.id;
  await runWithVerification(async () => {
    try {
      const result = await probeStorageProvider(provider.id);
      ElMessage({
        type: result.status === "HEALTHY" ? "success" : "warning",
        message: t(`storageProviderManagement.probeResult.${result.status}`)
      });
    } finally {
      probingId.value = "";
    }
  });
  if (!stepUp.visible.value) probingId.value = "";
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
        <el-select v-model="form.tier" clearable class="provider-filter-select">
          <el-option v-for="tier in ['HOT', 'WARM', 'COLD', 'ARCHIVE', 'DEEP_ARCHIVE']" :key="tier" :label="tier" :value="tier" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('storageProviderManagement.status')">
        <el-select v-model="form.enabled" clearable class="provider-filter-select">
          <el-option :label="t('storageProviderManagement.enabled')" value="true" />
          <el-option :label="t('storageProviderManagement.disabled')" value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">{{ t("storageProviderManagement.search") }}</el-button>
        <el-button @click="reset">{{ t("storageProviderManagement.reset") }}</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4 flex justify-end gap-2">
      <el-button type="primary" :loading="createLoading" @click="openCreate">{{ t("storageProviderManagement.create") }}</el-button>
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
      <el-table-column :label="t('storageProviderManagement.actions')" width="420" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row)">{{ t("storageProviderManagement.details") }}</el-button>
          <el-button link type="primary" @click="openEdit(scope.row)">{{ t("storageProviderManagement.edit") }}</el-button>
          <el-button link type="primary" @click="openDelivery(scope.row)">{{ t("storageProviderManagement.delivery") }}</el-button>
          <el-button link type="primary" :disabled="!scope.row.enabled" :loading="probingId === scope.row.id" @click="probe(scope.row)">
            {{ t("storageProviderManagement.probe") }}
          </el-button>
          <el-button link type="primary" :loading="mutatingId === scope.row.id" @click="setEnabled(scope.row, !scope.row.enabled)">
            {{ scope.row.enabled ? t("storageProviderManagement.disable") : t("storageProviderManagement.enable") }}
          </el-button>
          <el-button link type="danger" :disabled="scope.row.enabled" :loading="mutatingId === scope.row.id" @click="removeProvider(scope.row)">
            {{ t("storageProviderManagement.delete") }}
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

    <el-drawer
      v-model="deliveryVisible"
      direction="rtl"
      size="900px"
      :title="t('storageProviderManagement.deliveryTitle', { name: deliveryTarget?.display_name ?? '' })"
    >
      <div class="mb-3 flex items-center justify-between">
        <span class="text-sm text-gray-500">
          {{ t("storageProviderManagement.deliveryHint") }}
        </span>
        <el-button :loading="providerListLoading || deliveryLoading" @click="refreshDelivery">
          {{ t("storageProviderManagement.refresh") }}
        </el-button>
      </div>

      <div class="mb-2 flex items-center justify-between">
        <span class="font-medium">{{ t("storageProviderManagement.deliveryProviderSection") }}</span>
        <el-button type="primary" plain @click="openProviderCreate">
          {{ t("storageProviderManagement.deliveryProviderCreate") }}
        </el-button>
      </div>
      <el-table v-loading="providerListLoading" :data="deliveryProviders" row-key="id" border>
        <el-table-column prop="providerKey" :label="t('storageProviderManagement.key')" min-width="150" />
        <el-table-column prop="providerType" :label="t('storageProviderManagement.type')" width="130" />
        <el-table-column prop="displayName" :label="t('storageProviderManagement.name')" min-width="150" />
        <el-table-column :label="t('storageProviderManagement.status')" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.enabled ? 'success' : 'info'">
              {{ scope.row.enabled ? t("storageProviderManagement.enabled") : t("storageProviderManagement.disabled") }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="healthStatus" :label="t('storageProviderManagement.health')" width="120" />
        <el-table-column :label="t('storageProviderManagement.actions')" width="230" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openProviderEdit(scope.row)">{{ t("storageProviderManagement.edit") }}</el-button>
            <el-button
              link
              type="primary"
              :loading="providerMutatingId === scope.row.id"
              @click="toggleDeliveryProvider(scope.row)"
            >
              {{ scope.row.enabled ? t("storageProviderManagement.disable") : t("storageProviderManagement.enable") }}
            </el-button>
            <el-button
              link
              type="danger"
              :loading="providerMutatingId === scope.row.id"
              @click="removeDeliveryProvider(scope.row)"
            >
              {{ t("storageProviderManagement.delete") }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('storageProviderManagement.deliveryProviderEmpty')" />
        </template>
      </el-table>

      <el-divider />

      <div class="mb-2 flex items-center justify-between">
        <span class="font-medium">{{ t("storageProviderManagement.bindingSection") }}</span>
        <div>
          <el-alert
            v-if="deliveryProviders.length === 0"
            class="mb-2"
            type="info"
            :closable="false"
            :title="t('storageProviderManagement.noDeliveryProviders')"
          />
          <el-button type="primary" :disabled="deliveryProviders.length === 0" @click="openBindingCreate">
            {{ t("storageProviderManagement.bindingCreate") }}
          </el-button>
        </div>
      </div>
      <el-table v-loading="deliveryLoading" :data="deliveryBindings" row-key="id" border>
        <el-table-column :label="t('storageProviderManagement.deliveryProvider')" min-width="200">
          <template #default="scope">{{ deliveryProviderLabel(scope.row.deliveryProviderKey) }}</template>
        </el-table-column>
        <el-table-column prop="priority" :label="t('storageProviderManagement.priority')" width="90" />
        <el-table-column :label="t('storageProviderManagement.status')" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.enabled ? 'success' : 'info'">
              {{ scope.row.enabled ? t("storageProviderManagement.enabled") : t("storageProviderManagement.disabled") }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cacheKeyPolicy" :label="t('storageProviderManagement.cacheKeyPolicy')" width="150" />
        <el-table-column prop="rangePolicy" :label="t('storageProviderManagement.rangePolicy')" width="140" />
        <el-table-column :label="t('storageProviderManagement.fallbackParticipation')" width="120">
          <template #default="scope">{{ scope.row.fallbackParticipation ? t("storageProviderManagement.yes") : t("storageProviderManagement.no") }}</template>
        </el-table-column>
        <el-table-column :label="t('storageProviderManagement.actions')" width="230" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openBindingEdit(scope.row)">{{ t("storageProviderManagement.edit") }}</el-button>
            <el-button
              link
              type="primary"
              :loading="bindingMutatingId === scope.row.id"
              @click="toggleBinding(scope.row)"
            >
              {{ scope.row.enabled ? t("storageProviderManagement.disable") : t("storageProviderManagement.enable") }}
            </el-button>
            <el-button
              link
              type="danger"
              :loading="bindingMutatingId === scope.row.id"
              @click="removeBinding(scope.row)"
            >
              {{ t("storageProviderManagement.bindingUnbind") }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('storageProviderManagement.bindingEmpty')" />
        </template>
      </el-table>
    </el-drawer>

    <el-dialog
      v-model="providerFormVisible"
      :title="providerFormTarget
        ? t('storageProviderManagement.deliveryProviderEditTitle')
        : t('storageProviderManagement.deliveryProviderCreateTitle')"
      width="560px"
    >
      <el-form :model="providerForm" label-width="170px">
        <el-form-item :label="t('storageProviderManagement.key')" required>
          <el-input v-model="providerForm.providerKey" maxlength="128" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.type')" required>
          <el-select v-model="providerForm.providerType" class="w-full">
            <el-option v-for="type in deliveryProviderTypes" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.name')" required>
          <el-input v-model="providerForm.displayName" maxlength="256" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.endpoint')">
          <el-input v-model="providerForm.endpoint" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.credentialRef')">
          <el-input v-model="providerForm.credentialRef" maxlength="512" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.enabled')">
          <el-switch v-model="providerForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="providerFormVisible = false">{{ t("buttons.pureClose") }}</el-button>
        <el-button type="primary" :loading="providerFormLoading" @click="submitProvider">
          {{ t("buttons.pureConfirm") }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="bindingVisible"
      :title="bindingTarget ? t('storageProviderManagement.bindingEditTitle') : t('storageProviderManagement.bindingCreateTitle')"
      width="560px"
    >
      <el-form :model="bindingForm" label-width="170px">
        <el-form-item :label="t('storageProviderManagement.deliveryProvider')" required>
          <el-select v-model="bindingForm.deliveryProviderKey" class="w-full">
            <el-option
              v-for="item in deliveryProviders"
              :key="item.id"
              :label="deliveryProviderLabel(item.providerKey)"
              :value="item.providerKey"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.priority')" required>
          <el-input-number v-model="bindingForm.priority" :min="0" class="w-full" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.enabled')">
          <el-switch v-model="bindingForm.enabled" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.cacheKeyPolicy')" required>
          <el-select v-model="bindingForm.cacheKeyPolicy" class="w-full">
            <el-option v-for="policy in cacheKeyPolicies" :key="policy" :label="policy" :value="policy" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.rangePolicy')" required>
          <el-select v-model="bindingForm.rangePolicy" class="w-full">
            <el-option v-for="policy in rangePolicies" :key="policy" :label="policy" :value="policy" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.fallbackParticipation')">
          <el-switch v-model="bindingForm.fallbackParticipation" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindingVisible = false">{{ t("buttons.pureClose") }}</el-button>
        <el-button type="primary" :loading="bindingLoading" @click="submitBinding">
          {{ t("buttons.pureConfirm") }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createVisible" :title="t('storageProviderManagement.create')" width="560px">
      <el-form :model="createForm" label-width="140px">
        <el-form-item :label="t('storageProviderManagement.key')" required>
          <el-input v-model="createForm.provider_key" maxlength="256" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.type')" required>
          <el-select v-model="createForm.provider_type" class="w-full">
            <el-option v-for="type in providerTypes" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.name')" required>
          <el-input v-model="createForm.display_name" maxlength="256" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.tier')" required>
          <el-select v-model="createForm.tier" class="w-full">
            <el-option v-for="tier in ['HOT', 'WARM', 'COLD', 'ARCHIVE', 'DEEP_ARCHIVE']" :key="tier" :label="tier" :value="tier" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.accessKeyId')">
          <el-input v-model="createForm.access_key_id" maxlength="256" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.secretAccessKey')">
          <el-input v-model="createForm.secret_access_key" type="password" show-password maxlength="512" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.sessionToken')">
          <el-input v-model="createForm.session_token" type="password" show-password maxlength="2048" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.endpoint')">
          <el-input v-model="createForm.endpoint" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.bucket')">
          <el-input v-model="createForm.bucket" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.region')">
          <el-input v-model="createForm.region" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ t("buttons.pureClose") }}</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreate">{{ t("storageProviderManagement.create") }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" :title="t('storageProviderManagement.editTitle')" width="560px">
      <el-form :model="editForm" label-width="140px">
        <el-form-item :label="t('storageProviderManagement.key')">
          <el-input :model-value="editTarget?.provider_key" disabled />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.type')" required>
          <el-select v-model="editForm.provider_type" class="w-full">
            <el-option v-for="type in providerTypes" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.name')" required>
          <el-input v-model="editForm.display_name" maxlength="256" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.tier')" required>
          <el-select v-model="editForm.tier" class="w-full">
            <el-option v-for="tier in ['HOT', 'WARM', 'COLD', 'ARCHIVE', 'DEEP_ARCHIVE']" :key="tier" :label="tier" :value="tier" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.endpoint')">
          <el-input v-model="editForm.endpoint" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.bucket')">
          <el-input v-model="editForm.bucket" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.region')">
          <el-input v-model="editForm.region" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.accessKeyId')">
          <el-input v-model="editForm.access_key_id" maxlength="256" :placeholder="t('storageProviderManagement.credentialsUnchanged')" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.secretAccessKey')">
          <el-input v-model="editForm.secret_access_key" type="password" show-password maxlength="512" :placeholder="t('storageProviderManagement.credentialsUnchanged')" />
        </el-form-item>
        <el-form-item :label="t('storageProviderManagement.sessionToken')">
          <el-input v-model="editForm.session_token" type="password" show-password maxlength="2048" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ t("buttons.pureClose") }}</el-button>
        <el-button type="primary" :loading="editLoading" @click="submitEdit">{{ t("buttons.pureConfirm") }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="verificationVisible" :title="t('storageProviderManagement.verificationTitle')" width="420px" :close-on-click-modal="false" @closed="closeVerification">
      <p class="mb-4 text-[var(--el-text-color-secondary)]">{{ t("storageProviderManagement.verificationDescription") }}</p>
      <el-input v-model="verificationCode" maxlength="6" inputmode="numeric" @keyup.enter="verifyAndExecute" />
      <template #footer>
        <el-button @click="closeVerification">{{ t("buttons.pureClose") }}</el-button>
        <el-button type="primary" :loading="verificationLoading" :disabled="!/^[0-9]{6}$/.test(verificationCode)" @click="verifyAndExecute">
          {{ t("buttons.pureConfirm") }}
        </el-button>
      </template>
    </el-dialog>
  </PageCard>
</template>

<style scoped>
.provider-filter-select {
  width: 200px;
}
</style>
