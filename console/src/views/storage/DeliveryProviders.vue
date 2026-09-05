<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { http } from "@/utils/http";

type Provider = Record<string, any>;
type ProviderType = "DIRECT" | "CDN" | "SERVER_PROXY";

const providerTypes: Array<{
  value: ProviderType;
  label: string;
  description: string;
}> = [
  {
    value: "CDN",
    label: "CDN",
    description: "登记已在 ESA / EdgeOne 控制台配置好的加速域名和回源规则。"
  },
  {
    value: "DIRECT",
    label: "直连",
    description: "直接向源站签发访问地址，配置最少，适合内网或低流量场景。"
  },
  {
    value: "SERVER_PROXY",
    label: "服务端代理",
    description: "由 Ikaros 代理响应，可隐藏源站并统一处理访问策略。"
  }
];

const providers = ref<Provider[]>([]);
const storageProviders = ref<Provider[]>([]);
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const drawer = ref(false);
const step = ref(0);
const initialFormSnapshot = ref("");
const probingIds = ref<Set<string>>(new Set());
const form = ref({
  providerKey: "",
  displayName: "",
  providerType: "CDN" as ProviderType,
  credentialRef: "",
  endpoint: "",
  region: "",
  zoneId: "",
  enabled: true
});
const bindingDialog = ref(false);
const bindingLoading = ref(false);
const bindingSaving = ref(false);
const bindingError = ref("");
const selectedDeliveryProvider = ref<Provider | null>(null);
const selectedStorageProvider = ref<Provider | null>(null);
const editingBinding = ref<Provider | null>(null);
const bindings = ref<Provider[]>([]);
const bindingForm = ref({
  deliveryProviderKey: "",
  originType: "STORAGE_PROVIDER",
  authMode: "DELIVERY_GRANT",
  priority: 100,
  enabled: true,
  cacheKeyPolicy: "CONTENT_IDENTITY",
  rangePolicy: "PASSTHROUGH",
  fallbackParticipation: true
});

const selectedType = computed(
  () => providerTypes.find(item => item.value === form.value.providerType)!
);
const isCdn = computed(() => form.value.providerType === "CDN");
const endpointLabel = computed(() => (isCdn.value ? "CDN 加速域名" : "交付 Endpoint"));
const isValidKey = computed(() =>
  /^[a-z][a-z0-9-]{1,63}$/.test(form.value.providerKey)
);
const isDuplicateKey = computed(() =>
  providers.value.some(
    item =>
      String(item.providerKey).toLowerCase() ===
      form.value.providerKey.trim().toLowerCase()
  )
);
const isDirty = computed(
  () => JSON.stringify(form.value) !== initialFormSnapshot.value
);
const configPreview = computed(() => ({
  endpoint: form.value.endpoint || undefined,
  region: form.value.region || undefined,
  zoneId: form.value.zoneId || undefined
}));

function resetForm() {
  form.value = {
    providerKey: "",
    displayName: "",
    providerType: "CDN",
    credentialRef: "",
    endpoint: "",
    region: "",
    zoneId: "",
    enabled: true
  };
  step.value = 0;
  initialFormSnapshot.value = JSON.stringify(form.value);
}
function openDrawer() {
  resetForm();
  error.value = "";
  drawer.value = true;
}
function requestClose() {
  if (saving.value) return;
  if (!isDirty.value) {
    drawer.value = false;
    return;
  }
  ElMessageBox.confirm("已填写的配置尚未保存，确定要放弃吗？", "放弃添加", {
    type: "warning",
    confirmButtonText: "放弃并关闭",
    cancelButtonText: "继续编辑"
  })
    .then(() => {
      drawer.value = false;
    })
    .catch(() => undefined);
}
function beforeClose(done: () => void) {
  if (saving.value) return;
  if (!isDirty.value) {
    done();
    return;
  }
  ElMessageBox.confirm("已填写的配置尚未保存，确定要放弃吗？", "放弃添加", {
    type: "warning",
    confirmButtonText: "放弃并关闭",
    cancelButtonText: "继续编辑"
  })
    .then(done)
    .catch(() => undefined);
}
function nextStep() {
  if (!form.value.displayName.trim()) {
    error.value = "请填写展示名称";
    return;
  }
  if (!isValidKey.value) {
    error.value = "标识需使用 2-64 位小写字母、数字或短横线，且以字母开头";
    return;
  }
  if (isDuplicateKey.value) {
    error.value = "该内部标识已存在，请换一个标识";
    return;
  }
  if (isCdn.value && !form.value.endpoint.trim()) {
    error.value = "CDN Endpoint 不能为空";
    return;
  }
  if (
    isCdn.value &&
    form.value.endpoint &&
    !/^https:\/\/[^\s]+$/i.test(form.value.endpoint.trim())
  ) {
    error.value = "CDN Endpoint 必须是 https:// 开头的地址";
    return;
  }
  if (
    form.value.credentialRef &&
    !form.value.credentialRef.trim().startsWith("secret://")
  ) {
    error.value = "凭据引用必须使用 secret:// URI";
    return;
  }
  error.value = "";
  step.value = 1;
}
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const [deliveryResult, storageResult] = await Promise.all([
      http.get<unknown, unknown>("/admin/delivery-providers"),
      http.get<unknown, unknown>("/storage/providers")
    ]);
    providers.value = Array.isArray(deliveryResult)
      ? (deliveryResult as Provider[])
      : [];
    storageProviders.value = Array.isArray(storageResult)
      ? (storageResult as Provider[])
      : [];
  } catch (e: any) {
    error.value =
      e?.response?.data?.detail || e?.message || "分发 Provider 加载失败";
  } finally {
    loading.value = false;
  }
}
function closeBindingDialog() {
  if (bindingSaving.value) return;
  bindingDialog.value = false;
  bindingError.value = "";
  bindings.value = [];
  selectedDeliveryProvider.value = null;
  selectedStorageProvider.value = null;
  editingBinding.value = null;
}
async function loadBindings(storageProviderId: string) {
  bindingLoading.value = true;
  bindingError.value = "";
  try {
    const result = await http.get<unknown, unknown>(
      `/storage/providers/${storageProviderId}/delivery-bindings`
    );
    bindings.value = Array.isArray(result) ? (result as Provider[]) : [];
  } catch (e: any) {
    bindingError.value =
      e?.response?.data?.detail || e?.message || "绑定列表加载失败";
  } finally {
    bindingLoading.value = false;
  }
}
async function openBindingDialog(row: Provider) {
  selectedDeliveryProvider.value = row;
  editingBinding.value = null;
  selectedStorageProvider.value = null;
  bindingForm.value = {
    deliveryProviderKey: String(row.providerKey || ""),
    originType: "STORAGE_PROVIDER",
    authMode: "DELIVERY_GRANT",
    priority: 100,
    enabled: true,
    cacheKeyPolicy: "CONTENT_IDENTITY",
    rangePolicy: "PASSTHROUGH",
    fallbackParticipation: true
  };
  bindingError.value = "";
  bindingDialog.value = true;
}
async function selectStorageProvider(row: Provider) {
  selectedStorageProvider.value = row;
  await loadBindings(String(row.id));
}
function editBinding(row: Provider) {
  editingBinding.value = row;
  bindingForm.value = {
    deliveryProviderKey: String(row.deliveryProviderKey || ""),
    originType: String(row.originType || "STORAGE_PROVIDER"),
    authMode: String(row.authMode || "DELIVERY_GRANT"),
    priority: Number(row.priority || 0),
    enabled: Boolean(row.enabled),
    cacheKeyPolicy: String(row.cacheKeyPolicy || "CONTENT_IDENTITY"),
    rangePolicy: String(row.rangePolicy || "PASSTHROUGH"),
    fallbackParticipation: Boolean(row.fallbackParticipation)
  };
}
async function saveBinding() {
  const providerId = String(selectedStorageProvider.value?.id || "");
  if (!providerId || !bindingForm.value.deliveryProviderKey) {
    bindingError.value = "请选择存储 Provider";
    return;
  }
  bindingSaving.value = true;
  bindingError.value = "";
  try {
    if (editingBinding.value?.id) {
      await http.request("put",
        `/storage/providers/${providerId}/delivery-bindings/${editingBinding.value.id}`,
        {
          headers: {
            "If-Match": `"${editingBinding.value.version ?? 0}"`
          },
          data: bindingForm.value
        }
      );
      ElMessage.success("Delivery Binding 已更新");
      editingBinding.value = null;
    } else {
      await http.post(`/storage/providers/${providerId}/delivery-bindings`, {
        data: bindingForm.value
      });
      ElMessage.success("Delivery Binding 已创建，附件现在可以使用该分发路径");
    }
    await loadBindings(providerId);
  } catch (e: any) {
    bindingError.value =
      e?.response?.data?.detail || e?.message || "Delivery Binding 创建失败";
  } finally {
    bindingSaving.value = false;
  }
}
async function removeBinding(row: Provider) {
  if (!row.id) return;
  try {
    await ElMessageBox.confirm(
      "解绑后，使用该存储 Provider 的附件将不再通过此分发 Provider 预览；如果存在历史租约，系统会自动停用而不是删除。",
      "确认解绑",
      { type: "warning", confirmButtonText: "确认解绑", cancelButtonText: "取消" }
    );
    await http.request("delete", `/storage/providers/${selectedStorageProvider.value?.id}/delivery-bindings/${row.id}`);
    await loadBindings(String(selectedStorageProvider.value?.id));
    ElMessage.success("Delivery Binding 已解绑");
  } catch (e: any) {
    if (e === "cancel" || e === "close") return;
    bindingError.value =
      e?.response?.data?.detail || e?.message || "Delivery Binding 解绑失败";
  }
}
async function save() {
  saving.value = true;
  error.value = "";
  try {
    await http.post("/admin/delivery-providers", {
      headers: { "Idempotency-Key": crypto.randomUUID() },
      data: {
        providerKey: form.value.providerKey.trim(),
        displayName: form.value.displayName.trim(),
        providerType: form.value.providerType,
        credentialRef: form.value.credentialRef.trim() || undefined,
        enabled: form.value.enabled,
        config: Object.fromEntries(
          Object.entries(configPreview.value).filter(([, value]) => value)
        )
      }
    });
    drawer.value = false;
    ElMessage.success("分发 Provider 已添加");
    await load();
  } catch (e: any) {
    error.value =
      e?.response?.data?.detail || e?.message || "分发 Provider 添加失败";
  } finally {
    saving.value = false;
  }
}
function isProbing(id: unknown) {
  return probingIds.value.has(String(id));
}
function setProbing(id: unknown, probing: boolean) {
  const next = new Set(probingIds.value);
  if (probing) next.add(String(id));
  else next.delete(String(id));
  probingIds.value = next;
}
async function waitForProbe(taskId: string, providerId: string) {
  if (!taskId) {
    setProbing(providerId, false);
    return;
  }
  for (let attempt = 0; attempt < 20; attempt += 1) {
    await new Promise(resolve => window.setTimeout(resolve, 1000));
    try {
      const task = await http.get<any, any>(`/background-tasks/${taskId}`);
      if (
        ["SUCCEEDED", "FAILED", "CANCELLED", "TIMED_OUT"].includes(
          String(task?.status)
        )
      ) {
        await load();
        setProbing(providerId, false);
        return;
      }
    } catch {
      setProbing(providerId, false);
      return;
    }
  }
  await load();
  setProbing(providerId, false);
}
async function probe(row: Provider) {
  const providerId = String(row.id || "");
  if (!providerId || isProbing(providerId)) return;
  setProbing(providerId, true);
  try {
    const task = await http.post<any, any>(
      `/admin/delivery-providers/${providerId}/probe`,
      { headers: { "Idempotency-Key": crypto.randomUUID() } }
    );
    ElMessage.info("已提交检测，正在等待结果");
    await waitForProbe(String(task?.id || ""), providerId);
  } catch (e: any) {
    ElMessage.error(
      e?.response?.data?.detail || e?.message || "Provider 检测提交失败"
    );
    setProbing(providerId, false);
  }
}
async function toggleProvider(row: Provider) {
  const providerId = String(row.id || "");
  if (!providerId) return;
  const enabled = Boolean(row.enabled);
  try {
    await ElMessageBox.confirm(
      enabled
        ? "停用后，使用该 Provider 的新交付请求将不会再选择它。确定停用吗？"
        : "启用后，该 Provider 可以重新参与交付。确定启用吗？",
      enabled ? "确认停用" : "确认启用",
      { type: enabled ? "warning" : "info", confirmButtonText: enabled ? "停用" : "启用", cancelButtonText: "取消" }
    );
    await http.post(`/admin/delivery-providers/${providerId}/${enabled ? "disable" : "enable"}`);
    await load();
    ElMessage.success(enabled ? "分发 Provider 已停用" : "分发 Provider 已启用");
  } catch (e: any) {
    if (e === "cancel" || e === "close") return;
    ElMessage.error(e?.response?.data?.detail || e?.message || "Provider 状态更新失败");
  }
}
async function deleteProvider(row: Provider) {
  const providerId = String(row.id || "");
  if (!providerId) return;
  try {
    await ElMessageBox.confirm(
      "删除后 Provider 配置不可恢复；如果仍被 Delivery Binding 引用，删除会被拒绝。确定删除吗？",
      "确认删除分发 Provider",
      { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" }
    );
    await http.request("delete", `/admin/delivery-providers/${providerId}`);
    await load();
    ElMessage.success("分发 Provider 已删除");
  } catch (e: any) {
    if (e === "cancel" || e === "close") return;
    ElMessage.error(e?.response?.data?.detail || e?.message || "Provider 删除失败");
  }
}
function healthType(status: string): "success" | "warning" | "danger" | "info" {
  return (
    (
      {
        HEALTHY: "success",
        DEGRADED: "warning",
        UNHEALTHY: "danger"
      } as Record<string, "success" | "warning" | "danger">
    )[status] || "info"
  );
}
onMounted(load);
</script>

<template>
  <main class="p-4 md:p-6">
    <div class="flex flex-wrap justify-between items-start gap-4 mb-6">
      <div>
        <h1 class="text-2xl font-semibold">分发 Provider</h1>
        <p class="mt-1 text-[var(--el-text-color-secondary)]">
          配置内容如何从源站交付到客户端，Credential 仅保存 Secret 引用。
        </p>
      </div>
      <div class="flex gap-2">
        <el-button :loading="loading" @click="load">刷新</el-button
        ><el-button type="primary" @click="openDrawer"
          >添加分发 Provider</el-button
        >
      </div>
    </div>
    <el-alert
      v-if="error && !drawer"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      class="mb-4"
    />
    <el-alert
      title="CDN Provider 只登记已有的加速域名和回源规则，不会自动调用 ESA / EdgeOne API 修改控制台配置。"
      type="info"
      show-icon
      :closable="false"
      class="mb-4"
    />
    <el-skeleton v-if="loading" :rows="7" animated />
    <el-card v-else shadow="never">
      <el-empty v-if="!providers.length" description="暂无分发 Provider" />
      <el-table v-else :data="providers" stripe>
        <el-table-column prop="displayName" label="名称" min-width="190" />
        <el-table-column prop="providerKey" label="内部标识" min-width="170" />
        <el-table-column prop="providerType" label="类型" width="150" />
        <el-table-column label="健康状态" width="140"
          ><template #default="{ row }"
            ><el-tag
              :type="
                isProbing(row.id) ? 'warning' : healthType(row.healthStatus)
              "
              >{{
                isProbing(row.id) ? "检测中" : row.healthStatus || "UNKNOWN"
              }}</el-tag
            ></template
          ></el-table-column
        >
        <el-table-column label="启用状态" width="120"
          ><template #default="{ row }"
            ><el-tag :type="row.enabled ? 'success' : 'info'">{{
              row.enabled ? "启用" : "停用"
            }}</el-tag></template
          ></el-table-column
        >
        <el-table-column
          prop="signingKeyVersion"
          label="签名版本"
          width="110"
        />
        <el-table-column prop="updatedAt" label="最近更新" min-width="180" />
        <el-table-column label="操作" width="330" fixed="right"
          ><template #default="{ row }"
            ><el-button
              link
              type="primary"
              :disabled="!row.id || !storageProviders.length"
              @click="openBindingDialog(row)"
              >绑定存储</el-button
            ><el-button
              link
              type="primary"
              :loading="isProbing(row.id)"
              :disabled="!row.id"
              @click="probe(row)"
              >{{ isProbing(row.id) ? "检测中" : "立即检测" }}</el-button
            ><el-button
              link
              :type="row.enabled ? 'warning' : 'success'"
              :disabled="!row.id"
              @click="toggleProvider(row)"
              >{{ row.enabled ? "停用" : "启用" }}</el-button
            ><el-button
              link
              type="danger"
              :disabled="!row.id"
              @click="deleteProvider(row)"
              >删除</el-button
            ></template
          ></el-table-column
        >
      </el-table>
    </el-card>

    <el-drawer
      v-model="drawer"
      title="添加分发 Provider"
      size="520px"
      :before-close="beforeClose"
    >
      <el-steps :active="step" simple class="mb-6"
        ><el-step title="基础信息" /><el-step title="确认配置"
      /></el-steps>
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        show-icon
        :closable="false"
        class="mb-4"
      />
      <template v-if="step === 0">
        <el-form label-position="top">
          <el-form-item label="分发类型" required>
            <div class="grid grid-cols-1 gap-2 w-full">
              <button
                v-for="item in providerTypes"
                :key="item.value"
                type="button"
                class="text-left rounded border p-3 transition-colors"
                :class="
                  form.providerType === item.value
                    ? 'border-[var(--el-color-primary)] bg-[var(--el-color-primary-light-9)] ring-1 ring-[var(--el-color-primary)]'
                    : 'border-[var(--el-border-color)]'
                "
                :aria-pressed="form.providerType === item.value"
                @click="
                  form.providerType = item.value;
                  error = '';
                "
              >
                <div class="flex justify-between">
                  <span class="font-medium">{{ item.label }}</span
                  ><el-tag v-if="item.value === 'CDN'" size="small"
                    >推荐</el-tag
                  >
                </div>
                <p class="text-xs text-[var(--el-text-color-secondary)] mt-1">
                  {{ item.description }}
                </p>
              </button>
            </div>
          </el-form-item>
          <el-form-item label="展示名称" required
            ><el-input
              v-model="form.displayName"
              placeholder="例如：生产环境 CDN"
              maxlength="80"
              show-word-limit
          /></el-form-item>
          <el-form-item label="内部标识" required
            ><el-input
              v-model="form.providerKey"
              placeholder="例如 production-cdn"
              maxlength="64"
              ><template #prefix>provider://</template></el-input
            >
            <div class="text-xs text-[var(--el-text-color-secondary)] mt-1">
              创建后用于 Binding 引用，只能使用小写字母、数字和短横线。
            </div>
            <div
              v-if="isDuplicateKey"
              class="text-xs text-[var(--el-color-danger)] mt-1"
            >
              该标识已存在，请换一个标识。
            </div></el-form-item
          >
          <el-form-item v-if="isCdn" :label="endpointLabel" required
            ><el-input
              v-model="form.endpoint"
              placeholder="https://cdn.example.com"
            />
            <div class="text-xs text-[var(--el-text-color-secondary)] mt-1">
              填写 ESA / EdgeOne 控制台中已经启用的对外加速域名，不要填写控制台地址；仅支持 HTTPS。
              系统会将附件的 object key 拼接到此域名后，由 CDN 现有回源规则读取存储。
            </div></el-form-item
          >
          <div v-if="!isCdn" class="grid grid-cols-2 gap-3">
            <el-form-item label="Region"
              ><el-input
                v-model="form.region"
                placeholder="例如 cn-east-1" /></el-form-item
            ><el-form-item label="Zone ID"
              ><el-input v-model="form.zoneId" placeholder="可选"
            /></el-form-item>
          </div>
          <el-form-item v-if="!isCdn" label="签名密钥引用"
            ><el-input
              v-model="form.credentialRef"
              placeholder="secret://delivery/cdn-prod"
            />
            <div class="text-xs text-[var(--el-text-color-secondary)] mt-1">
              只填写 Secret 引用，不要把 Token 或密钥明文粘贴到此处。
            </div></el-form-item
          >
          <el-checkbox v-model="form.enabled">创建后立即启用</el-checkbox>
        </el-form>
      </template>
      <template v-else>
        <el-alert
          title="请确认以下配置。保存后会写入审计事件，密钥内容不会进入 Provider 配置。"
          type="warning"
          show-icon
          :closable="false"
          class="mb-4"
        />
        <el-descriptions :column="1" border
          ><el-descriptions-item label="名称">{{
            form.displayName
          }}</el-descriptions-item
          ><el-descriptions-item label="内部标识"
            >provider://{{ form.providerKey }}</el-descriptions-item
          ><el-descriptions-item label="类型"
            >{{ selectedType.label }} ·
            {{ selectedType.description }}</el-descriptions-item
          ><el-descriptions-item v-if="isCdn" label="加速域名">{{
            form.endpoint
          }}</el-descriptions-item
          ><el-descriptions-item v-if="!isCdn" label="凭据">{{
            form.credentialRef
              ? "已填写 Secret 引用（不会保存密钥明文）"
              : "未配置"
          }}</el-descriptions-item
          ><el-descriptions-item label="状态">{{
            form.enabled ? "立即启用" : "先停用"
          }}</el-descriptions-item
          ><el-descriptions-item label="其他配置">
            <pre class="text-xs whitespace-pre-wrap">{{
              JSON.stringify(
                {
                  region: form.region || undefined,
                  zoneId: form.zoneId || undefined
                },
                null,
                2
              )
            }}</pre>
          </el-descriptions-item></el-descriptions
        >
      </template>
      <template #footer
        ><div class="flex justify-between">
          <el-button v-if="step === 1" @click="step = 0">返回修改</el-button
          ><span v-else />
          <div class="flex gap-2">
            <el-button @click="requestClose">取消</el-button
            ><el-button v-if="step === 0" type="primary" @click="nextStep"
              >下一步</el-button
            ><el-button v-else type="primary" :loading="saving" @click="save"
              >确认添加</el-button
            >
          </div>
        </div></template
      >
    </el-drawer>

    <el-dialog
      v-model="bindingDialog"
      :title="editingBinding ? '编辑 Delivery Binding' : '创建 Delivery Binding'"
      width="720px"
      @closed="closeBindingDialog"
    >
      <el-alert
        v-if="bindingError"
        :title="bindingError"
        type="error"
        show-icon
        :closable="false"
        class="mb-4"
      />
      <el-alert
        title="Binding 决定哪些存储 Provider 的附件可以使用当前分发 Provider。通常使用存储源直接交付和 Delivery Grant。"
        type="info"
        :closable="false"
        class="mb-4"
      />
      <el-form label-position="top">
        <el-form-item label="分发 Provider" required>
          <el-input :model-value="selectedDeliveryProvider?.displayName || selectedDeliveryProvider?.providerKey || ''" disabled />
        </el-form-item>
        <el-form-item label="存储 Provider" required>
          <el-select
            v-model="selectedStorageProvider"
            value-key="id"
            class="w-full"
            placeholder="选择附件实际存储所在的 Provider"
            @change="selectStorageProvider"
          >
            <el-option
              v-for="item in storageProviders"
              :key="item.id"
              :label="`${item.providerKey} · ${item.providerType || 'Storage'}`"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <div class="grid grid-cols-2 gap-3">
          <el-form-item label="源站类型">
            <el-select v-model="bindingForm.originType" class="w-full">
              <el-option label="存储 Provider 直出" value="STORAGE_PROVIDER" />
              <el-option label="服务端代理" value="SERVER_PROXY" />
            </el-select>
            <div class="text-xs text-[var(--el-text-color-secondary)] mt-1">
              决定附件内容从哪里读取；通常选择存储 Provider 直出，服务端代理适合需要由 Ikaros 统一转发的场景。
            </div>
          </el-form-item>
          <el-form-item label="鉴权模式">
            <el-select v-model="bindingForm.authMode" class="w-full">
              <el-option label="Delivery Grant" value="DELIVERY_GRANT" />
              <el-option label="Provider 签名" value="PROVIDER_SIGNED" />
              <el-option label="服务端鉴权" value="SERVER_AUTH" />
            </el-select>
            <div class="text-xs text-[var(--el-text-color-secondary)] mt-1">
              决定访问凭证如何生成；Delivery Grant 使用 Ikaros 的短时授权，安全性和通用性通常最好。
            </div>
          </el-form-item>
          <el-form-item label="优先级">
            <el-input-number v-model="bindingForm.priority" :min="0" :max="9999" class="w-full" />
            <div class="text-xs text-[var(--el-text-color-secondary)] mt-1">
              数值越小越优先；主分发路径可设为 0，备用路径设置更大的值。
            </div>
          </el-form-item>
          <el-form-item label="范围请求">
            <el-select v-model="bindingForm.rangePolicy" class="w-full">
              <el-option label="透传 Range" value="PASSTHROUGH" />
              <el-option label="固定分片" value="FIXED_CHUNK" />
              <el-option label="不支持 Range" value="UNSUPPORTED" />
            </el-select>
            <div class="text-xs text-[var(--el-text-color-secondary)] mt-1">
              决定是否支持视频拖动、断点续传等分段读取；对象存储和 CDN 通常选择透传 Range。
            </div>
          </el-form-item>
          <el-form-item label="缓存 Key">
            <el-select v-model="bindingForm.cacheKeyPolicy" class="w-full">
              <el-option label="内容身份" value="CONTENT_IDENTITY" />
              <el-option label="完整请求" value="FULL_REQUEST" />
              <el-option label="不缓存" value="NO_CACHE" />
            </el-select>
            <div class="text-xs text-[var(--el-text-color-secondary)] mt-1">
              决定 CDN 或缓存如何区分请求；内容身份适合同一附件复用缓存，完整请求适合需要区分查询参数的场景。
            </div>
          </el-form-item>
        </div>
        <div>
          <el-checkbox v-model="bindingForm.enabled">立即启用</el-checkbox>
          <div class="text-xs text-[var(--el-text-color-secondary)] ml-1 mt-1">
            只有启用的 Binding 才会参与附件预览地址选择。
          </div>
        </div>
        <div class="mt-3">
          <el-checkbox v-model="bindingForm.fallbackParticipation">参与故障回退</el-checkbox>
          <div class="text-xs text-[var(--el-text-color-secondary)] ml-1 mt-1">
            当前路径不可用时允许切换到其他可用 Binding；建议主、备分发路径都开启。
          </div>
        </div>
      </el-form>
      <el-divider />
      <div class="font-medium mb-2">当前存储 Provider 的绑定</div>
      <el-skeleton v-if="bindingLoading" :rows="2" animated />
      <el-empty v-else-if="!selectedStorageProvider" description="选择存储 Provider 后查看已有绑定" />
      <el-empty v-else-if="!bindings.length" description="暂无绑定" />
      <el-table v-else :data="bindings" size="small">
        <el-table-column prop="deliveryProviderKey" label="分发 Provider" />
        <el-table-column prop="priority" label="优先级" width="90" />
        <el-table-column prop="enabled" label="状态" width="80">
          <template #default="{ row }">{{ row.enabled ? "启用" : "停用" }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }"><el-button link type="primary" @click="editBinding(row)">编辑</el-button><el-button link type="danger" @click="removeBinding(row)">解绑</el-button></template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="closeBindingDialog">关闭</el-button>
        <el-button type="primary" :loading="bindingSaving" :disabled="!selectedStorageProvider" @click="saveBinding">{{ editingBinding ? "保存修改" : "创建绑定" }}</el-button>
      </template>
    </el-dialog>
  </main>
</template>
