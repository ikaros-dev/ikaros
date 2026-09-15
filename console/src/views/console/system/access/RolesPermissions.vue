<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import {
  createManagedRole,
  listManagedRoles,
  replaceManagedRolePermissions,
  type CreateRoleRequest,
  type ManagedRole
} from "@/api/role";
import { getHttpErrorMessage } from "@/utils/http";
import PageCard from "@/views/console/PageCard.vue";

const { t } = useI18n();
const loading = ref(false);
const roles = ref<ManagedRole[]>([]);
const query = reactive({ code: "", name: "" });
const currentPage = ref(1);
const pageSize = ref(20);
const createVisible = ref(false);
const detailVisible = ref(false);
const editVisible = ref(false);
const selectedRole = ref<ManagedRole | null>(null);
const createForm = reactive<CreateRoleRequest>({ code: "", name: "", description: "" });
const editPermissions = ref<string[]>([]);

const permissionKeys = [
  "system.user.read",
  "system.user.manage",
  "system.role.read",
  "system.role.manage",
  "system.session.read",
  "system.session.manage",
  "system.audit.read",
  "resource.read",
  "resource.write",
  "resource.delete",
  "resource.download",
  "resource.share",
  "storage.provider.read",
  "storage.provider.manage",
  "storage.delivery.read",
  "storage.delivery.manage",
  "storage.tiering.manage",
  "storage.restore.request",
  "storage.restore.read",
  "storage.restore.manage",
  "ingestion.source.manage",
  "account.self.read",
  "account.preference.read",
  "account.notification.read",
  "account.security.read",
  "user.read",
  "role.read",
  "security.authentication.read",
  "integration.read",
  "notification.read",
  "audit.read",
  "platform.read",
  "system.health.read",
  "system.diagnostics.read",
  "collection.read",
  "search.use",
  "storage.policy.read",
  "storage.archive.read",
  "storage.maintenance.read",
  "backup.read",
  "drive.space.read",
  "drive.file.read",
  "drive.transfer.read",
  "drive.sync.read",
  "drive.conflict.read",
  "drive.trash.read",
  "drive.settings.read",
  "document.read",
  "media.read",
  "planning.read",
  "finance.read",
  "private_note.read",
  "password.read",
  "ai.read",
  "share.read",
  "analytics.read",
  "automation.read",
  "dashboard.read",
  "ingestion.read",
  "activity.read",
  "storage.read",
  "app.read",
  "system.read"
];

const permissionLabel = (permission: string) =>
  t(`roleManagement.permissionNames.${permission.replaceAll(".", "_")}`);

const filteredRoles = computed(() => {
  const code = query.code.trim().toLowerCase();
  const name = query.name.trim().toLowerCase();
  return roles.value.filter(
    role =>
      (!code || role.code.toLowerCase().includes(code)) &&
      (!name || role.name.toLowerCase().includes(name))
  );
});

const pagedRoles = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRoles.value.slice(start, start + pageSize.value);
});

const loadRoles = async () => {
  loading.value = true;
  try {
    roles.value = await listManagedRoles();
    const maxPage = Math.max(1, Math.ceil(filteredRoles.value.length / pageSize.value));
    if (currentPage.value > maxPage) currentPage.value = maxPage;
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("roleManagement.loadFailed")));
  } finally {
    loading.value = false;
  }
};

const search = () => {
  currentPage.value = 1;
};

const reset = () => {
  query.code = "";
  query.name = "";
  search();
};

const openCreate = () => {
  createForm.code = "";
  createForm.name = "";
  createForm.description = "";
  createVisible.value = true;
};

const submitCreate = async () => {
  if (!createForm.code.trim() || !createForm.name.trim()) return;
  try {
    await createManagedRole({
      code: createForm.code.trim(),
      name: createForm.name.trim(),
      description: createForm.description?.trim() || undefined
    });
    createVisible.value = false;
    ElMessage.success(t("roleManagement.createSuccess"));
    await loadRoles();
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("roleManagement.createFailed")));
  }
};

const openDetail = (role: ManagedRole) => {
  selectedRole.value = role;
  detailVisible.value = true;
};

const openEdit = (role: ManagedRole) => {
  selectedRole.value = role;
  editPermissions.value = [...role.permissions];
  editVisible.value = true;
};

const submitPermissions = async () => {
  if (!selectedRole.value) return;
  try {
    await replaceManagedRolePermissions(selectedRole.value.id, {
      permissions: editPermissions.value
    });
    editVisible.value = false;
    ElMessage.success(t("roleManagement.updateSuccess"));
    await loadRoles();
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("roleManagement.updateFailed")));
  }
};

const confirmEdit = async () => {
  try {
    await ElMessageBox.confirm(
      t("roleManagement.updateConfirm"),
      t("roleManagement.updateTitle"),
      {
        type: "warning",
        confirmButtonText: t("buttons.pureConfirm"),
        cancelButtonText: t("buttons.pureClose")
      }
    );
    await submitPermissions();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(getHttpErrorMessage(error, t("roleManagement.updateFailed")));
    }
  }
};

onMounted(loadRoles);
</script>

<template>
  <PageCard>
    <el-form :inline="true" class="mt-6" @submit.prevent="search">
      <el-form-item :label="t('roleManagement.code')">
        <el-input
          v-model="query.code"
          :placeholder="t('roleManagement.codePlaceholder')"
          clearable
          @keyup.enter="search"
        />
      </el-form-item>
      <el-form-item :label="t('roleManagement.name')">
        <el-input
          v-model="query.name"
          :placeholder="t('roleManagement.namePlaceholder')"
          clearable
          @keyup.enter="search"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">{{ t("roleManagement.search") }}</el-button>
        <el-button @click="reset">{{ t("roleManagement.reset") }}</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4">
      <el-button type="primary" @click="openCreate">{{ t("roleManagement.create") }}</el-button>
    </div>

    <el-table v-loading="loading" :data="pagedRoles" border>
      <el-table-column prop="code" :label="t('roleManagement.code')" min-width="180" />
      <el-table-column prop="name" :label="t('roleManagement.name')" min-width="160" />
      <el-table-column :label="t('roleManagement.description')" min-width="220">
        <template #default="{ row }">
          {{ row.description || t("roleManagement.empty") }}
        </template>
      </el-table-column>
      <el-table-column :label="t('roleManagement.type')" width="120">
        <template #default="{ row }">
          <el-tag :type="row.builtIn ? 'warning' : 'success'" effect="light">
            {{ row.builtIn ? t("roleManagement.builtIn") : t("roleManagement.custom") }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('roleManagement.permissions')" min-width="300">
        <template #default="{ row }">
          <el-space wrap>
            <el-tag v-for="permission in row.permissions.slice(0, 2)" :key="permission" size="small">
              {{ permissionLabel(permission) }}
            </el-tag>
            <el-tag v-if="row.permissions.length > 2" size="small" type="info">
              {{ t("roleManagement.morePermissions", { count: row.permissions.length - 2 }) }}
            </el-tag>
            <span v-if="!row.permissions.length">{{ t("roleManagement.empty") }}</span>
          </el-space>
        </template>
      </el-table-column>
      <el-table-column fixed="right" :label="t('roleManagement.actions')" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">{{ t("roleManagement.detail") }}</el-button>
          <el-button link type="primary" :disabled="row.builtIn" @click="openEdit(row)">
            {{ t("roleManagement.edit") }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="mt-4 flex justify-end">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="filteredRoles.length"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="currentPage = 1"
      />
    </div>

    <el-dialog v-model="createVisible" :title="t('roleManagement.createTitle')" width="520px">
      <el-form label-width="90px">
        <el-form-item :label="t('roleManagement.code')" required>
          <el-input v-model="createForm.code" :placeholder="t('roleManagement.codePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('roleManagement.name')" required>
          <el-input v-model="createForm.name" :placeholder="t('roleManagement.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('roleManagement.description')">
          <el-input v-model="createForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{ t("buttons.pureClose") }}</el-button>
        <el-button type="primary" @click="submitCreate">{{ t("roleManagement.createConfirm") }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="t('roleManagement.detailTitle')" width="620px">
      <el-descriptions v-if="selectedRole" :column="1" border>
        <el-descriptions-item :label="t('roleManagement.code')">{{ selectedRole.code }}</el-descriptions-item>
        <el-descriptions-item :label="t('roleManagement.name')">{{ selectedRole.name }}</el-descriptions-item>
        <el-descriptions-item :label="t('roleManagement.description')">
          {{ selectedRole.description || t("roleManagement.empty") }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('roleManagement.type')">
          {{ selectedRole.builtIn ? t("roleManagement.builtIn") : t("roleManagement.custom") }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('roleManagement.permissions')">
          <el-space wrap>
            <el-tag v-for="permission in selectedRole.permissions" :key="permission">
              {{ permissionLabel(permission) }}
            </el-tag>
            <span v-if="!selectedRole.permissions.length">{{ t("roleManagement.empty") }}</span>
          </el-space>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="editVisible" :title="t('roleManagement.editTitle')" width="620px">
      <el-form v-if="selectedRole" label-width="90px">
        <el-form-item :label="t('roleManagement.name')">{{ selectedRole.name }}</el-form-item>
        <el-form-item :label="t('roleManagement.permissions')">
          <el-checkbox-group v-model="editPermissions" class="grid grid-cols-1 gap-2 md:grid-cols-2">
            <el-checkbox v-for="permission in permissionKeys" :key="permission" :label="permission">
              {{ permissionLabel(permission) }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ t("buttons.pureClose") }}</el-button>
        <el-button type="primary" @click="confirmEdit">{{ t("roleManagement.save") }}</el-button>
      </template>
    </el-dialog>
  </PageCard>
</template>
