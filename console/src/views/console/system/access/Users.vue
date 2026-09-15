<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useI18n } from "vue-i18n";
import {
  deleteManagedUser,
  issueStepUpChallenge,
  listManagedUsers,
  verifyStepUpChallenge,
  type ManagedUser,
  type ManagedUserStatus
} from "@/api/user";
import { clearVerificationGrant, setVerificationGrant } from "@/utils/verificationGrant";
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
const verificationVisible = ref(false);
const verificationLoading = ref(false);
const verificationCode = ref("");
const verificationChallengeId = ref("");
const pendingDeleteUser = ref<ManagedUser | null>(null);

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

const removeUser = async (user: ManagedUser) => {
  try {
    await ElMessageBox.confirm(
      t("userManagement.deleteConfirm", { username: user.username }),
      t("userManagement.deleteTitle"),
      { type: "warning", confirmButtonText: t("buttons.pureConfirm"), cancelButtonText: t("buttons.pureClose") }
    );
    pendingDeleteUser.value = user;
    verificationLoading.value = true;
    try {
      const challenge = await issueStepUpChallenge();
      verificationChallengeId.value = challenge.id;
      verificationCode.value = "";
      verificationVisible.value = true;
    } finally {
      verificationLoading.value = false;
    }
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(t("userManagement.deleteFailed"));
    }
  }
};

const verifyAndDelete = async () => {
  if (!verificationChallengeId.value || !/^\d{6}$/.test(verificationCode.value)) return;
  const user = pendingDeleteUser.value;
  if (!user) return;
  verificationLoading.value = true;
  try {
    const result = await verifyStepUpChallenge(verificationChallengeId.value, verificationCode.value);
    setVerificationGrant(result.verificationGrant);
    await deleteManagedUser(user.id);
    clearVerificationGrant();
    verificationVisible.value = false;
    pendingDeleteUser.value = null;
    ElMessage.success(t("userManagement.deleteSuccess"));
    await loadUsers();
  } catch {
    clearVerificationGrant();
    ElMessage.error(t("userManagement.deleteFailed"));
  } finally {
    verificationLoading.value = false;
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
          class="w-48"
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
        <el-button type="primary" @click="search">{{ t("userManagement.search") }}</el-button>
        <el-button @click="reset">{{ t("userManagement.reset") }}</el-button>
      </el-form-item>
    </el-form>

    <el-table class="mt-2" v-loading="loading" :data="users" row-key="id" border>
      <el-table-column prop="username" :label="t('userManagement.username')" min-width="150" />
      <el-table-column prop="displayName" :label="t('userManagement.displayName')" min-width="150" />
      <el-table-column prop="email" :label="t('userManagement.email')" min-width="210">
        <template #default="scope">{{ scope.row.email || t("userManagement.empty") }}</template>
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
          {{ scope.row.roleCodes.length ? scope.row.roleCodes.join(", ") : t("userManagement.empty") }}
        </template>
      </el-table-column>
      <el-table-column :label="t('userManagement.createdAt')" min-width="180">
        <template #default="scope">{{ formatDate(scope.row.createdAt) }}</template>
      </el-table-column>
      <el-table-column :label="t('userManagement.lastLoginAt')" min-width="180">
        <template #default="scope">{{ formatDate(scope.row.lastLoginAt) }}</template>
      </el-table-column>
      <el-table-column fixed="right" :label="t('userManagement.actions')" width="100">
        <template #default="scope">
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
        @keyup.enter="verifyAndDelete"
      />
      <template #footer>
        <el-button @click="verificationVisible = false">{{ t("buttons.pureClose") }}</el-button>
        <el-button
          type="primary"
          :loading="verificationLoading"
          :disabled="!/^\d{6}$/.test(verificationCode)"
          @click="verifyAndDelete"
        >
          {{ t("userManagement.verificationConfirm") }}
        </el-button>
      </template>
    </el-dialog>
  </PageCard>
</template>
