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
    description: "通过第三方边缘网络分发，适合公开或高频访问内容。"
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

const selectedType = computed(
  () => providerTypes.find(item => item.value === form.value.providerType)!
);
const isCdn = computed(() => form.value.providerType === "CDN");
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
    const result = await http.get<unknown, unknown>(
      "/admin/delivery-providers"
    );
    providers.value = Array.isArray(result) ? (result as Provider[]) : [];
  } catch (e: any) {
    error.value =
      e?.response?.data?.detail || e?.message || "分发 Provider 加载失败";
  } finally {
    loading.value = false;
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
      title="添加后 Provider 默认不会改变已有 Binding；请在绑定策略中选择并设置优先级。"
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
        <el-table-column label="操作" width="120" fixed="right"
          ><template #default="{ row }"
            ><el-button
              link
              type="primary"
              :loading="isProbing(row.id)"
              :disabled="!row.id"
              @click="probe(row)"
              >{{ isProbing(row.id) ? "检测中" : "立即检测" }}</el-button
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
          <el-form-item v-if="isCdn" label="CDN Endpoint" required
            ><el-input
              v-model="form.endpoint"
              placeholder="https://cdn.example.com"
            />
            <div class="text-xs text-[var(--el-text-color-secondary)] mt-1">
              填写对外分发域名或 CDN 接入地址，不要填写控制台地址；仅支持
              HTTPS。
            </div></el-form-item
          >
          <div class="grid grid-cols-2 gap-3">
            <el-form-item label="Region"
              ><el-input
                v-model="form.region"
                placeholder="例如 cn-east-1" /></el-form-item
            ><el-form-item label="Zone ID"
              ><el-input v-model="form.zoneId" placeholder="可选"
            /></el-form-item>
          </div>
          <el-form-item v-if="isCdn" label="签名密钥引用"
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
          ><el-descriptions-item v-if="isCdn" label="分发地址">{{
            form.endpoint
          }}</el-descriptions-item
          ><el-descriptions-item label="凭据">{{
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
  </main>
</template>
