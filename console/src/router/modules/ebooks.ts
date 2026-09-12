import { type RouteRecordRaw } from "vue-router";

/** 兼容旧地址；电子书导入菜单已归入内容与媒体子系统。 */
export default {
  path: "/reading/ebooks",
  name: "EbookImportLegacy",
  redirect: "/content-center/ebooks",
  meta: { title: "电子书导入", showLink: false }
} satisfies RouteRecordRaw;
