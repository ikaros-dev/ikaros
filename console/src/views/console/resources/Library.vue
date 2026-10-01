<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useI18n } from "vue-i18n";
import PageCard from "@/views/console/PageCard.vue";
import {
  listCollections,
  listResources,
  listTagCatalog,
  resourceLifecycles,
  resourceTypes,
  type Collection,
  type Resource,
  type ResourceTag
} from "@/api/resource";
import { getHttpErrorMessage } from "@/utils/http";

const { t } = useI18n();
const router = useRouter();
const loading = ref(false);
const resources = ref<Resource[]>([]);
const total = ref(0);
const collections = ref<Collection[]>([]);
const tags = ref<ResourceTag[]>([]);

const filters = reactive({
  query: "",
  type: "",
  lifecycle_status: "ACTIVE",
  collection_id: "",
  tag: "",
  source_provider: ""
});
const page = ref(0);
const size = ref(20);

const loadResources = async () => {
  loading.value = true;
  try {
    const result = await listResources({
      query: filters.query.trim() || undefined,
      type: filters.type || undefined,
      lifecycle_status: filters.lifecycle_status || undefined,
      collection_id: filters.collection_id || undefined,
      tag: filters.tag || undefined,
      source_provider: filters.source_provider.trim() || undefined,
      page: page.value,
      size: size.value
    });
    resources.value = result.items;
    total.value = result.total;
  } catch (error) {
    ElMessage.error(getHttpErrorMessage(error, t("resourceLibrary.loadFailed")));
  } finally {
    loading.value = false;
  }
};

const loadOptions = async () => {
  try {
    collections.value = await listCollections();
  } catch {
    collections.value = [];
  }
  try {
    tags.value = (await listTagCatalog()).items;
  } catch {
    tags.value = [];
  }
};

const search = () => {
  page.value = 0;
  void loadResources();
};

const reset = () => {
  Object.assign(filters, {
    query: "",
    type: "",
    lifecycle_status: "ACTIVE",
    collection_id: "",
    tag: "",
    source_provider: ""
  });
  search();
};

const openDetail = (resource: Resource) => {
  void router.push(`/resources/library/${resource.id}`);
};

onMounted(async () => {
  await Promise.all([loadOptions(), loadResources()]);
});
</script>

<template>
  <PageCard>
    <el-form :inline="true" :model="filters" class="mt-6" @submit.prevent="search">
      <el-form-item :label="t('resourceLibrary.query')">
        <el-input v-model="filters.query" clearable style="width: 200px" @keyup.enter="search" />
      </el-form-item>
      <el-form-item :label="t('resourceLibrary.type')">
        <el-select v-model="filters.type" clearable style="width: 160px">
          <el-option v-for="type in resourceTypes" :key="type" :label="type" :value="type" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('resourceLibrary.lifecycle')">
        <el-select v-model="filters.lifecycle_status" style="width: 160px">
          <el-option v-for="item in resourceLifecycles" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('resourceLibrary.collection')">
        <el-select v-model="filters.collection_id" clearable filterable style="width: 200px">
          <el-option v-for="item in collections" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('resourceLibrary.tag')">
        <el-select v-model="filters.tag" clearable filterable style="width: 160px">
          <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.name" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('resourceLibrary.source')">
        <el-input v-model="filters.source_provider" clearable style="width: 160px" @keyup.enter="search" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">{{ t("resourceLibrary.search") }}</el-button>
        <el-button @click="reset">{{ t("resourceLibrary.reset") }}</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4 flex justify-end">
      <el-button :loading="loading" @click="loadResources">{{ t("resourceLibrary.refresh") }}</el-button>
    </div>

    <el-table v-loading="loading" :data="resources" row-key="id" border>
      <el-table-column :label="t('resourceLibrary.title')" min-width="220">
        <template #default="scope">{{ scope.row.primaryTitle ?? "-" }}</template>
      </el-table-column>
      <el-table-column prop="type" :label="t('resourceLibrary.type')" width="120" />
      <el-table-column prop="dataClassification" :label="t('resourceLibrary.classification')" width="140" />
      <el-table-column :label="t('resourceLibrary.lifecycle')" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.lifecycle === 'ACTIVE' ? 'success' : 'info'">
            {{ scope.row.lifecycle }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('resourceLibrary.updatedAt')" width="200">
        <template #default="scope">{{ new Date(scope.row.updatedAt).toLocaleString() }}</template>
      </el-table-column>
      <el-table-column :label="t('resourceLibrary.actions')" width="120" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row)">{{ t("resourceLibrary.details") }}</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty :description="t('resourceLibrary.empty')" /></template>
    </el-table>

    <div class="mt-4 flex justify-end">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadResources"
        @size-change="search"
      />
    </div>
  </PageCard>
</template>
