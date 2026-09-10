<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { http } from "@/utils/http";

type Role = { id: string; code: string; name: string; description?: string; builtIn?: boolean; permissions?: string[] };
const roles = ref<Role[]>([]);
const permissions = ref<string[]>([]);
const selectedRoleId = ref("");
const selected = ref<string[]>([]);
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const dialog = ref(false);
const form = ref({ code: "", name: "", description: "" });
const query = ref("");
const groups = computed(() => {
  const result: Record<string, string[]> = {};
  permissions.value.filter(key => !query.value || key.toLowerCase().includes(query.value.toLowerCase())).forEach(key => {
    const group = key.split(".")[0] || "platform";
    (result[group] ||= []).push(key);
  });
  return result;
});
const currentRole = computed(() => roles.value.find(role => role.id === selectedRoleId.value));

async function load() {
  loading.value = true; error.value = "";
  try {
    const [roleResult, permissionResult] = await Promise.all([http.get<unknown, unknown>("/admin/roles"), http.get<unknown, unknown>("/admin/permissions")]);
    roles.value = Array.isArray(roleResult) ? roleResult as Role[] : [];
    permissions.value = Array.isArray(permissionResult) ? permissionResult as string[] : [];
    if (!selectedRoleId.value && roles.value.length) selectRole(roles.value[0]);
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "角色和权限加载失败"; }
  finally { loading.value = false; }
}
function selectRole(role: Role) { selectedRoleId.value = role.id; selected.value = [...(role.permissions || [])]; }
async function createRole() {
  if (!form.value.code || !form.value.name) { error.value = "角色编码和名称不能为空"; return; }
  try {
    const role = await http.post<Role, typeof form.value>("/admin/roles", { data: form.value });
    roles.value = [...roles.value, role]; form.value = { code: "", name: "", description: "" }; dialog.value = false; selectRole(role);
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "角色创建失败"; }
}
function toggle(key: string, value: boolean) { selected.value = value ? [...new Set([...selected.value, key])] : selected.value.filter(item => item !== key); }
async function save() {
  if (!currentRole.value) return;
  saving.value = true; error.value = "";
  try {
    const role = await http.request<Role>("put", `/admin/roles/${currentRole.value.id}/permissions`, { data: { permissions: selected.value } });
    roles.value = roles.value.map(item => item.id === role.id ? role : item); selectRole(role);
  } catch (e: any) { error.value = e?.response?.data?.detail || e?.message || "权限保存失败"; }
  finally { saving.value = false; }
}
onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex justify-between items-start gap-4 mb-6"><div><h1 class="text-2xl font-semibold">角色与权限</h1><p class="mt-1 text-[var(--el-text-color-secondary)]">选择角色配置已注册的平台权限。</p></div><div class="flex gap-2"><el-button type="primary" @click="dialog = true">创建角色</el-button><el-button :loading="loading" @click="load">刷新</el-button></div></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-4" />
    <el-skeleton v-if="loading" :rows="10" animated />
    <div v-else class="grid grid-cols-1 lg:grid-cols-4 gap-4">
      <el-card shadow="never"><template #header><span>角色</span></template><el-empty v-if="!roles.length" description="暂无角色" /><div v-for="role in roles" :key="role.id" class="p-3 rounded cursor-pointer" :class="selectedRoleId === role.id ? 'bg-[var(--el-fill-color-light)]' : ''" @click="selectRole(role)"><div class="font-medium">{{ role.name }}</div><div class="text-xs text-[var(--el-text-color-secondary)]">{{ role.code }} · {{ role.permissions?.length || 0 }} 项权限</div></div></el-card>
      <el-card shadow="never" class="lg:col-span-3"><template #header><div class="flex justify-between items-center"><span>{{ currentRole ? `${currentRole.name}（${currentRole.code}）` : "请选择角色" }}</span><el-button v-if="currentRole" type="primary" :loading="saving" @click="save">保存权限</el-button></div></template><template v-if="currentRole"><el-input v-model="query" clearable placeholder="搜索 Capability / Resource" class="mb-4 max-w-md" /><div v-for="(keys, group) in groups" :key="group" class="mb-5"><div class="font-medium mb-2 capitalize">{{ group }}</div><el-checkbox-group v-model="selected"><div class="grid grid-cols-1 md:grid-cols-2 gap-2"><el-checkbox v-for="key in keys" :key="key" :label="key" @change="value => toggle(key, !!value)">{{ key }}</el-checkbox></div></el-checkbox-group></div><el-empty v-if="!Object.keys(groups).length" description="暂无匹配权限" /></template><el-empty v-else description="请选择角色" /></el-card>
    </div>
    <el-dialog v-model="dialog" title="创建角色" width="480px"><el-form label-position="top"><el-form-item label="角色编码" required><el-input v-model="form.code" /></el-form-item><el-form-item label="角色名称" required><el-input v-model="form.name" /></el-form-item><el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item></el-form><template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="createRole">创建</el-button></template></el-dialog>
  </main>
</template>
