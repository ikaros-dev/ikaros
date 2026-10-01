<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import PageCard from "@/views/console/PageCard.vue";
import {
  approveImportPlan,
  createIngestionSource,
  generateImportPlan,
  getIngestionScan,
  listImportPlanItems,
  listIngestionCandidates,
  listIngestionSources,
  startImportRun,
  startIngestionScan,
  updateImportPlanItem,
  type ImportPlan,
  type ImportPlanItem,
  type IngestionCandidate,
  type IngestionScan,
  type IngestionSource,
  type IngestionSourceType
} from "@/api/resource";
import { getHttpErrorMessage } from "@/utils/http";

const { t } = useI18n();
const router = useRouter();
const activeStep = ref(0);
const loading = ref(false);
const savingItem = ref("");
const sources = ref<IngestionSource[]>([]);
const selectedSourceId = ref("");
const scan = ref<IngestionScan | null>(null);
const candidates = ref<IngestionCandidate[]>([]);
const plan = ref<ImportPlan | null>(null);
const planItems = ref<ImportPlanItem[]>([]);
const runId = ref("");
const sourceFormVisible = ref(false);
const sourceForm = reactive({
  type: "LOCAL_FILESYSTEM" as IngestionSourceType,
  displayName: "",
  rootReference: ""
});

const selectedSource = computed(() =>
  sources.value.find(source => source.id === selectedSourceId.value)
);
const candidateById = computed(
  () => new Map(candidates.value.map(candidate => [candidate.id, candidate]))
);
const reviewReady = computed(
  () => planItems.value.length > 0 && planItems.value.every(item => item.action !== "REQUIRE_REVIEW" && item.action !== "CONFLICT")
);

const showError = (error: unknown) =>
  ElMessage.error(getHttpErrorMessage(error, t("addResource.actionFailed")));

const loadSources = async () => {
  loading.value = true;
  try {
    sources.value = await listIngestionSources();
    if (!selectedSourceId.value) {
      selectedSourceId.value = sources.value.find(source => source.status === "ENABLED")?.id ?? "";
    }
  } catch (error) {
    showError(error);
  } finally {
    loading.value = false;
  }
};

const saveSource = async () => {
  if (!sourceForm.displayName.trim() || !sourceForm.rootReference.trim()) return;
  loading.value = true;
  try {
    const source = await createIngestionSource({
      type: sourceForm.type,
      displayName: sourceForm.displayName.trim(),
      rootReference: sourceForm.rootReference.trim()
    });
    sources.value = [source, ...sources.value];
    selectedSourceId.value = source.id;
    sourceFormVisible.value = false;
    Object.assign(sourceForm, { displayName: "", rootReference: "" });
  } catch (error) {
    showError(error);
  } finally {
    loading.value = false;
  }
};

const beginScan = async () => {
  if (!selectedSourceId.value) return;
  loading.value = true;
  try {
    scan.value = await startIngestionScan(selectedSourceId.value);
    candidates.value = [];
    plan.value = null;
    planItems.value = [];
    activeStep.value = 1;
  } catch (error) {
    showError(error);
  } finally {
    loading.value = false;
  }
};

const refreshScan = async () => {
  if (!scan.value) return;
  loading.value = true;
  try {
    scan.value = await getIngestionScan(scan.value.id);
    if (scan.value.status === "SUCCEEDED") {
      candidates.value = await listIngestionCandidates(scan.value.id);
    }
  } catch (error) {
    showError(error);
  } finally {
    loading.value = false;
  }
};

const createPlan = async () => {
  if (!scan.value) return;
  loading.value = true;
  try {
    plan.value = await generateImportPlan(scan.value.id);
    planItems.value = await listImportPlanItems(plan.value.id);
    activeStep.value = 2;
  } catch (error) {
    showError(error);
  } finally {
    loading.value = false;
  }
};

const chooseAction = async (item: ImportPlanItem, action: "CREATE_RESOURCE" | "SKIP") => {
  if (!plan.value || item.version == null) return;
  savingItem.value = item.id;
  try {
    const updated = await updateImportPlanItem(plan.value.id, item.id, {
      expectedVersion: item.version,
      action,
      reason: action === "SKIP" ? "Skipped by user" : "Confirmed by user"
    });
    planItems.value = planItems.value.map(current => current.id === updated.id ? updated : current);
  } catch (error) {
    showError(error);
  } finally {
    savingItem.value = "";
  }
};

const confirmImport = async () => {
  if (!plan.value || plan.value.version == null || !reviewReady.value) return;
  loading.value = true;
  try {
    const approved = await approveImportPlan(plan.value.id, plan.value.version);
    const run = await startImportRun(approved.id, approved.version ?? plan.value.version);
    runId.value = run.id;
    activeStep.value = 3;
  } catch (error) {
    showError(error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => void loadSources());
</script>

<template>
  <PageCard>
    <el-steps :active="activeStep" finish-status="success" class="mt-8 mb-8">
      <el-step :title="t('addResource.steps.source')" />
      <el-step :title="t('addResource.steps.scan')" />
      <el-step :title="t('addResource.steps.review')" />
      <el-step :title="t('addResource.steps.import')" />
    </el-steps>

    <section v-if="activeStep === 0" class="add-resource-panel">
      <div class="mb-4 flex items-center justify-between gap-3">
        <div>
          <h2 class="text-lg font-semibold">{{ t("addResource.sourceTitle") }}</h2>
          <p class="mt-1 text-sm text-[var(--el-text-color-secondary)]">{{ t("addResource.sourceDescription") }}</p>
        </div>
        <el-button @click="sourceFormVisible = !sourceFormVisible">{{ t("addResource.addSource") }}</el-button>
      </div>

      <el-form v-if="sourceFormVisible" :model="sourceForm" label-position="top" class="source-form">
        <el-form-item :label="t('addResource.sourceType')" required>
          <el-select v-model="sourceForm.type" class="w-full">
            <el-option value="LOCAL_FILESYSTEM" :label="t('addResource.types.LOCAL_FILESYSTEM')" />
            <el-option value="NAS_MOUNT" :label="t('addResource.types.NAS_MOUNT')" />
            <el-option value="OBJECT_STORAGE" :label="t('addResource.types.OBJECT_STORAGE')" />
            <el-option value="REMOTE_URL" :label="t('addResource.types.REMOTE_URL')" />
            <el-option value="PROVIDER_COLLECTION" :label="t('addResource.types.PROVIDER_COLLECTION')" />
            <el-option value="PLUGIN_SOURCE" :label="t('addResource.types.PLUGIN_SOURCE')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('addResource.sourceName')" required>
          <el-input v-model="sourceForm.displayName" maxlength="256" />
        </el-form-item>
        <el-form-item :label="t('addResource.sourceLocation')" required>
          <el-input v-model="sourceForm.rootReference" maxlength="1024" :placeholder="t('addResource.sourceLocationHint')" />
        </el-form-item>
        <div class="flex justify-end gap-2">
          <el-button @click="sourceFormVisible = false">{{ t("buttons.pureClose") }}</el-button>
          <el-button type="primary" :loading="loading" :disabled="!sourceForm.displayName.trim() || !sourceForm.rootReference.trim()" @click="saveSource">
            {{ t("addResource.saveSource") }}
          </el-button>
        </div>
      </el-form>

      <el-empty v-if="!sources.length && !loading" :description="t('addResource.noSources')" />
      <el-radio-group v-else v-model="selectedSourceId" class="source-list">
        <el-radio v-for="source in sources" :key="source.id" :value="source.id" border :disabled="source.status !== 'ENABLED'">
          <span class="font-medium">{{ source.displayName }}</span>
          <span class="ml-2 text-sm text-[var(--el-text-color-secondary)]">{{ t(`addResource.types.${source.type}`) }} · {{ source.rootReference }}</span>
        </el-radio>
      </el-radio-group>
      <div class="mt-6 flex justify-end">
        <el-button type="primary" :loading="loading" :disabled="!selectedSource" @click="beginScan">{{ t("addResource.startScan") }}</el-button>
      </div>
    </section>

    <section v-else-if="activeStep === 1" class="add-resource-panel">
      <h2 class="text-lg font-semibold">{{ t("addResource.scanTitle") }}</h2>
      <p class="mt-2 text-[var(--el-text-color-secondary)]">{{ t("addResource.scanDescription", { source: selectedSource?.displayName ?? "" }) }}</p>
      <el-alert v-if="scan?.status === 'FAILED'" class="mt-4" type="error" :title="scan.errorSummary || t('addResource.scanFailed')" :closable="false" />
      <div class="mt-6 flex items-center gap-3">
        <el-tag :type="scan?.status === 'SUCCEEDED' ? 'success' : scan?.status === 'FAILED' ? 'danger' : 'info'">{{ scan?.status }}</el-tag>
        <span class="text-sm text-[var(--el-text-color-secondary)]">{{ t("addResource.discoveredCount", { count: scan?.discoveredCount ?? 0 }) }}</span>
        <el-button :loading="loading" @click="refreshScan">{{ t("addResource.refreshScan") }}</el-button>
      </div>
      <div class="mt-6 flex justify-between">
        <el-button @click="activeStep = 0">{{ t("addResource.back") }}</el-button>
        <el-button type="primary" :loading="loading" :disabled="scan?.status !== 'SUCCEEDED'" @click="createPlan">{{ t("addResource.previewResults") }}</el-button>
      </div>
    </section>

    <section v-else-if="activeStep === 2" class="add-resource-panel">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 class="text-lg font-semibold">{{ t("addResource.reviewTitle") }}</h2>
          <p class="mt-1 text-sm text-[var(--el-text-color-secondary)]">{{ t("addResource.reviewDescription") }}</p>
        </div>
        <el-tag type="info">{{ t("addResource.candidateCount", { count: candidates.length }) }}</el-tag>
      </div>
      <el-table :data="planItems" row-key="id" border>
        <el-table-column :label="t('addResource.candidateTitle')" min-width="220">
          <template #default="scope">{{ candidateById.get(scope.row.candidateId)?.titleHint || t("addResource.untitled") }}</template>
        </el-table-column>
        <el-table-column :label="t('addResource.candidateType')" width="180">
          <template #default="scope">{{ candidateById.get(scope.row.candidateId)?.suggestedResourceType || "-" }}</template>
        </el-table-column>
        <el-table-column :label="t('addResource.confidence')" width="120">
          <template #default="scope">{{ candidateById.get(scope.row.candidateId)?.confidence ?? scope.row.confidence }}%</template>
        </el-table-column>
        <el-table-column :label="t('addResource.decision')" width="230" fixed="right">
          <template #default="scope">
            <el-select :model-value="scope.row.action" :loading="savingItem === scope.row.id" @change="(action: 'CREATE_RESOURCE' | 'SKIP') => chooseAction(scope.row, action)">
              <el-option value="REQUIRE_REVIEW" :label="t('addResource.chooseDecision')" disabled />
              <el-option value="CREATE_RESOURCE" :label="t('addResource.createResource')" />
              <el-option value="SKIP" :label="t('addResource.skipItem')" />
            </el-select>
          </template>
        </el-table-column>
        <template #empty><el-empty :description="t('addResource.noCandidates')" /></template>
      </el-table>
      <div class="mt-6 flex justify-between">
        <el-button @click="activeStep = 1">{{ t("addResource.back") }}</el-button>
        <el-button type="primary" :loading="loading" :disabled="!reviewReady" @click="confirmImport">{{ t("addResource.confirmImport") }}</el-button>
      </div>
    </section>

    <section v-else class="add-resource-panel text-center">
      <el-result icon="success" :title="t('addResource.importStarted')" :sub-title="t('addResource.importStartedDescription')">
        <template #extra>
          <el-button type="primary" @click="router.push('/resources/activity')">{{ t("addResource.openActivity") }}</el-button>
          <el-button @click="router.push('/resources/library')">{{ t("addResource.openLibrary") }}</el-button>
        </template>
      </el-result>
      <p class="text-xs text-[var(--el-text-color-secondary)]">{{ t("addResource.runReference", { id: runId }) }}</p>
    </section>
  </PageCard>
</template>

<style scoped>
.add-resource-panel {
  max-width: 960px;
  margin: 0 auto 24px;
}

.source-form {
  margin: 20px 0;
  padding: 20px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.source-list {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  margin-top: 18px;
}

.source-list :deep(.el-radio) {
  height: auto;
  margin-right: 0;
  padding: 16px;
}
</style>
