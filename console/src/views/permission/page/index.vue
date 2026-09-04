<script setup lang="ts">
import { type CSSProperties, computed } from "vue";
import { useUserStoreHook } from "@/store/modules/user";

defineOptions({
  name: "PermissionPage"
});

const elStyle = computed((): CSSProperties => {
  return {
    width: "85vw",
    justifyContent: "start"
  };
});

const user = useUserStoreHook();
const username = computed(() => user.username || "未登录");
const roles = computed(() => user.roles || []);
const permissions = computed(() => user.permissions || []);
</script>

<template>
  <div>
    <p class="mb-2!">
      当前角色和按钮权限来自后端登录响应，不在前端伪造或切换权限。
    </p>
    <el-card shadow="never" :style="elStyle">
      <template #header>
        <div class="card-header">
          <span>当前用户：{{ username }}</span>
        </div>
      </template>
      <p>角色：{{ roles.length ? roles.join("、") : "暂无角色" }}</p>
      <p>权限：{{ permissions.length ? permissions.join("、") : "暂无权限" }}</p>
    </el-card>
  </div>
</template>
