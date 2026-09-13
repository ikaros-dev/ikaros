import { type RouteRecordRaw } from "vue-router";

/** 兼容旧地址；电子书导入统一进入 Add Content。 */
export default {
  path: "/reading/ebooks",
  name: "EbookImportLegacy",
  redirect: "/add",
  meta: { title: "电子书导入", showLink: false }
} satisfies RouteRecordRaw;
