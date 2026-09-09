import { type RouteRecordRaw } from "vue-router";

export default {
  path: "/reading/ebooks",
  name: "EbookImport",
  component: () => import("@/views/reading/EbookImport.vue"),
  meta: { title: "电子书导入", icon: "ep:document" }
} satisfies RouteRecordRaw;
