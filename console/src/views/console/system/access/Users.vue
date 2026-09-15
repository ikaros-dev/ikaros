<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import {
  createManagedUser,
  deleteManagedUser,
  getManagedUser,
  listManagedUsers,
  type ManagedUser,
  type ManagedUserStatus,
  type CreateManagedUserRequest,
  type UpdateManagedUserRequest,
  updateManagedUser
} from "@/api/user";
import { useStepUpVerification } from "@/composables/useStepUpVerification";
import {
  clearVerificationGrant,
  setVerificationGrant
} from "@/utils/verificationGrant";
import { getHttpErrorMessage } from "@/utils/http";
import PageCard from "@/views/console/PageCard.vue";

const { t, locale } = useI18n();
const loading = ref(false);
const users = ref<ManagedUser[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(20);
const form = reactive<{ query: string; status: "" | ManagedUserStatus }>({
  query: "",
  status: ""
});
const stepUp = useStepUpVerification();
const verificationVisible = stepUp.visible;
const verificationLoading = stepUp.loading;
const verificationCode = stepUp.code;
const pendingDeleteUser = ref<ManagedUser | null>(null);
const createVisible = ref(false);
const createForm = reactive({ username: "", displayName: "", email: "" });
const pendingCreateRequest = ref<CreateManagedUserRequest | null>(null);
const pendingUpdateRequest = ref<{ userId: string; data: UpdateManagedUserRequest } | null>(null);
const verificationAction = ref<"create" | "delete" | "update" | null>(null);
const editVisible = ref(false);
const editForm = reactive({ userId: "", username: "", displayName: "", email: "", status: "PENDING" as ManagedUserStatus });
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailUser = ref<ManagedUser | null>(null);

const statusOptions: ManagedUserStatus[] = [
  "PENDING",
  "ACTIVE",
  "DISABLED",
  "LOCKED"
];

const formatDate = (value: string | null) => {
  if (!value) return t("userManagement.never");
  return new Intl.DateTimeFormat(locale.value === "zh" ? "zh-CN" : "en-US", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
};

const loadUsers = async () => {
  loading.value = true;
  try {
    const result = await listManagedUsers({
      query: form.query.trim() || undefined,
      status: form.status || undefined,
      page: currentPage.value - 1,
      size: pageSize.value
    });
    users.value = result.items;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
};

const search = () => {
  currentPage.value = 1;
  void loadUsers();
};

const reset = () => {
  form.query = "";
  form.status = "";
  search();
};

const openCreate = () => {
  createForm.username = "";
  createForm.displayName = "";
  createForm.email = "";
  createVisible.value = true;
};

const submitCreate = async () => {
  if (!createForm.username.trim() || !createForm.displayName.trim()) return;
  pendingCreateRequest.value = {
    username: createForm.username.trim(),
    displayName: createForm.displayName.trim(),
    email: createForm.email.trim() || undefined
  };
  verificationAction.value = "create";
  createVisible.value = false;
  try {
    await requestVerification();
  } catch (error) {
    pendingCreateRequest.value = null;
    verificationAction.value = null;
    ElMessage.error(getHttpErrorMessage(error, t("userManagement.createFailed")));
  }
};

const requestVerification = async () => {
  await stepUp.request("EMAIL_OTP", verifyAndExecute);
};

const openDetail = async (user: ManagedUser) => {
  detailUser.value = user;
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    detailUser.value = await getManagedUser(user.id);
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("userManagement.detailFailed")));
    detailVisible.value = false;
  } finally {
    detailLoading.value = false;
  }
};

const openEdit = (user: ManagedUser) => {
  editForm.userId = user.id;
  editForm.username = user.username;
  editForm.displayName = user.displayName;
  editForm.email = user.email || "";
  editForm.status = user.status;
  editVisible.value = true;
};

const submitUpdate = async () => {
  if (!editForm.userId || !editForm.username.trim() || !editForm.displayName.trim()) return;
  pendingUpdateRequest.value = {
    userId: editForm.userId,
    data: {
      username: editForm.username.trim(),
      displayName: editForm.displayName.trim(),
      email: editForm.email.trim() || undefined,
      status: editForm.status
    }
  };
  verificationAction.value = "update";
  editVisible.value = false;
  try {
    await requestVerification();
  } catch (error) {
    pendingUpdateRequest.value = null;
    verificationAction.value = null;
    ElMessage.error(getHttpErrorMessage(error, t("userManagement.updateFailed")));
  }
};

const removeUser = async (user: ManagedUser) => {
  try {
    await ElMessageBox.confirm(
      t("userManagement.deleteConfirm", { username: user.username }),
      t("userManagement.deleteTitle"),
      {
        type: "warning",
        confirmButtonText: t("buttons.pureConfirm"),
        cancelButtonText: t("buttons.pureClose")
      }
    );
    pendingDeleteUser.value = user;
    verificationAction.value = "delete";
    await requestVerification();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(getHttpErrorMessage(error, t("userManagement.deleteFailed")));
    }
  }
};

const verifyAndExecute = async (reusedGrant?: string) => {
  if (!reusedGrant && (!stepUp.challengeId.value || !/^\d{6}$/.test(stepUp.code.value))) return;
  const action = verificationAction.value;
  if (!action) return;
  try {
    const result = reusedGrant
      ? { verificationGrant: reusedGrant }
      : { verificationGrant: await stepUp.verify() };
    if (!result.verificationGrant) return;
    setVerificationGrant(result.verificationGrant);
    if (action === "delete" && pendingDeleteUser.value) {
      await deleteManagedUser(pendingDeleteUser.value.id);
    } else if (action === "create" && pendingCreateRequest.value) {
      await createManagedUser(pendingCreateRequest.value);
    } else if (action === "update" && pendingUpdateRequest.value) {
      await updateManagedUser(pendingUpdateRequest.value.userId, pendingUpdateRequest.value.data);
    }
    clearVerificationGrant();
    stepUp.close();
    pendingDeleteUser.value = null;
    pendingCreateRequest.value = null;
    pendingUpdateRequest.value = null;
    verificationAction.value = null;
    ElMessage.success(t(
      action === "delete"
        ? "userManagement.deleteSuccess"
        : action === "create"
          ? "userManagement.createSuccess"
          : "userManagement.updateSuccess"
    ));
    await loadUsers();
  } catch (error) {
    clearVerificationGrant();
    ElMessage.error(
      getHttpErrorMessage(
        error,
        t(
          action === "delete"
            ? "userManagement.deleteFailed"
            : action === "create"
              ? "userManagement.createFailed"
              : "userManagement.updateFailed"
        )
      )
    );
  }
};

onMounted(loadUsers);
</script>

<template>
  <PageCard>
    <el-form class="mt-6" :inline="true" :model="form" @submit.prevent="search">
      <el-form-item :label="t('userManagement.username')">
        <el-input
          v-model="form.query"
          clearable
          :placeholder="t('userManagement.usernamePlaceholder')"
          @keyup.enter="search"
        />
      </el-form-item>
      <el-form-item :label="t('userManagement.status')">
        <el-select
          v-model="form.status"
          class="user-status-select"
          clearable
          :placeholder="t('userManagement.statusPlaceholder')"
        >
          <el-option
            v-for="status in statusOptions"
            :key="status"
            :label="t(`userManagement.statuses.${status}`)"
            :value="status"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">{{
          t("userManagement.search")
        }}</el-button>
        <el-button @click="reset">{{ t("userManagement.reset") }}</el-button>
      </el-form-item>
    </el-form>

    <div class="mt-2">
      <el-button type="primary" @click="openCreate">{{
        t("userManagement.create")
      }}</el-button>
    </div>

    <el-table
      v-loading="loading"
      class="mt-2"
      :data="users"
      row-key="id"
      border
    >
      <el-table-column
        prop="username"
        :label="t('userManagement.username')"
        min-width="150"
      />
      <el-table-column
        prop="displayName"
        :label="t('userManagement.displayName')"
        min-width="150"
      />
      <el-table-column
        prop="email"
        :label="t('userManagement.email')"
        min-width="210"
      >
        <template #default="scope">{{
          scope.row.email || t("userManagement.empty")
        }}</template>
      </el-table-column>
      <el-table-column :label="t('userManagement.status')" width="130">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ t(`userManagement.statuses.${scope.row.status}`) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('userManagement.roles')" min-width="180">
        <template #default="scope">
          {{
            scope.row.roleCodes.length
              ? scope.row.roleCodes.join(", ")
              : t("userManagement.empty")
          }}
        </template>
      </el-table-column>
      <el-table-column :label="t('userManagement.createdAt')" min-width="180">
        <template #default="scope">{{
          formatDate(scope.row.createdAt)
        }}</template>
      </el-table-column>
      <el-table-column :label="t('userManagement.lastLoginAt')" min-width="180">
        <template #default="scope">{{
          formatDate(scope.row.lastLoginAt)
        }}</template>
      </el-table-column>
      <el-table-column
        fixed="right"
        :label="t('userManagement.actions')"
        width="190"
      >
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">
            {{ t("userManagement.edit") }}
          </el-button>
          <el-button link type="primary" @click="openDetail(scope.row)">
            {{ t("userManagement.detail") }}
          </el-button>
          <el-button link type="danger" @click="removeUser(scope.row)">
            {{ t("userManagement.delete") }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="mt-4 flex justify-end">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="search"
        @current-change="loadUsers"
      />
    </div>
    <el-dialog
      v-model="createVisible"
      :title="t('userManagement.createTitle')"
      width="480px"
    >
      <el-form
        :model="createForm"
        label-width="80px"
        @submit.prevent="submitCreate"
      >
        <el-form-item :label="t('userManagement.username')" required>
          <el-input
            v-model="createForm.username"
            :placeholder="t('userManagement.usernamePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('userManagement.displayName')" required>
          <el-input
            v-model="createForm.displayName"
            :placeholder="t('userManagement.displayNamePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('userManagement.email')">
          <el-input
            v-model="createForm.email"
            :placeholder="t('userManagement.emailPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">{{
          t("buttons.pureClose")
        }}</el-button>
        <el-button
          type="primary"
          :disabled="
            !createForm.username.trim() || !createForm.displayName.trim()
          "
          @click="submitCreate"
        >
          {{ t("userManagement.createConfirm") }}
        </el-button>
      </template>
    </el-dialog>
    <el-dialog
      v-model="editVisible"
      :title="t('userManagement.editTitle')"
      width="480px"
    >
      <el-form :model="editForm" label-width="80px" @submit.prevent="submitUpdate">
        <el-form-item :label="t('userManagement.username')" required>
          <el-input v-model="editForm.username" :placeholder="t('userManagement.usernamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('userManagement.displayName')" required>
          <el-input v-model="editForm.displayName" :placeholder="t('userManagement.displayNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('userManagement.email')">
          <el-input v-model="editForm.email" :placeholder="t('userManagement.emailPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('userManagement.status')" required>
          <el-select v-model="editForm.status" class="user-status-select">
            <el-option
              v-for="status in statusOptions"
              :key="status"
              :label="t(`userManagement.statuses.${status}`)"
              :value="status"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">{{ t("buttons.pureClose") }}</el-button>
        <el-button
          type="primary"
          :disabled="!editForm.username.trim() || !editForm.displayName.trim()"
          @click="submitUpdate"
        >
          {{ t("userManagement.updateConfirm") }}
        </el-button>
      </template>
    </el-dialog>
    <el-dialog
      v-model="detailVisible"
      :title="t('userManagement.detailTitle')"
      width="520px"
    >
      <el-skeleton v-if="detailLoading" :rows="5" animated />
      <el-descriptions v-else-if="detailUser" :column="1" border>
        <el-descriptions-item :label="t('userManagement.username')">{{
          detailUser.username
        }}</el-descriptions-item>
        <el-descriptions-item :label="t('userManagement.displayName')">{{
          detailUser.displayName
        }}</el-descriptions-item>
        <el-descriptions-item :label="t('userManagement.email')">{{
          detailUser.email || t("userManagement.empty")
        }}</el-descriptions-item>
        <el-descriptions-item :label="t('userManagement.status')">
          {{ t(`userManagement.statuses.${detailUser.status}`) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('userManagement.roles')">
          {{
            detailUser.roleCodes.length
              ? detailUser.roleCodes.join(", ")
              : t("userManagement.empty")
          }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('userManagement.createdAt')">{{
          formatDate(detailUser.createdAt)
        }}</el-descriptions-item>
        <el-descriptions-item :label="t('userManagement.lastLoginAt')">{{
          formatDate(detailUser.lastLoginAt)
        }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">{{
          t("buttons.pureClose")
        }}</el-button>
      </template>
    </el-dialog>
    <el-dialog
      v-model="verificationVisible"
      :title="t('userManagement.verificationTitle')"
      width="420px"
      :close-on-click-modal="false"
    >
      <p class="mb-4 text-[var(--el-text-color-secondary)]">
        {{ t("userManagement.verificationDescription") }}
      </p>
      <el-input
        v-model="verificationCode"
        maxlength="6"
        inputmode="numeric"
        :placeholder="t('userManagement.verificationPlaceholder')"
        @keyup.enter="() => verifyAndExecute()"
      />
      <template #footer>
        <el-button @click="stepUp.close()">{{
          t("buttons.pureClose")
        }}</el-button>
        <el-button
          type="primary"
          :loading="verificationLoading"
          :disabled="!/^\d{6}$/.test(verificationCode)"
          @click="() => verifyAndExecute()"
        >
          {{ t("userManagement.verificationConfirm") }}
        </el-button>
      </template>
    </el-dialog>
  </PageCard>
</template>

<style scoped>
.user-status-select {
  width: 100px;
}
</style>
