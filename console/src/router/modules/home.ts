import { $t } from "@/plugins/i18n";
const { VITE_HIDE_HOME } = import.meta.env;
const Layout = () => import("@/layout/index.vue");

export default {
  path: "/",
  name: "Home",
  component: Layout,
  redirect: "/welcome",
  meta: {
    icon: "ep/home-filled",
    title: $t("menus.pureHome"),
    rank: 0
  },
  children: [
    {
      path: "/welcome",
      name: "Welcome",
      component: () => import("@/views/welcome/index.vue"),
      meta: {
        title: $t("menus.pureHome"),
        showLink: VITE_HIDE_HOME === "true" ? false : true
      }
    },
    { path: "/resources", name: "Resources", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "资源管理", description: "管理资源、标题、标签和生命周期。", endpoint: "/resources", columns: ["id", "resourceType", "status", "createdAt"], icon: "ep:files" } },
    { path: "/documents", name: "Documents", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "文档管理", description: "管理个人文档和工作副本。", endpoint: "/documents", columns: ["id", "title", "status", "updatedAt"], icon: "ep:document" } },
    { path: "/drive", name: "Drive", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "个人云盘", description: "查看云盘空间和文件节点。", endpoint: "/drive/spaces", columns: ["id", "name", "status", "createdAt"], icon: "ep:folder-opened" } },
    { path: "/rooms", name: "Rooms", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "协作房间", description: "查看共享协作房间及其状态。", endpoint: "/rooms", columns: ["id", "name", "status", "createdAt"], icon: "ep:chat-line-round" } },
    { path: "/finance", name: "Finance", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "个人财务", description: "查看账本、账户和财务记录。", endpoint: "/finance/ledgers", columns: ["id", "name", "currency", "createdAt"], icon: "ep:money" } },
    { path: "/media", name: "Media", component: () => import("@/views/modules/ModulePage.vue"), meta: { title: "媒体库", description: "查看媒体资源和播放内容。", endpoint: "/media/catalog", columns: ["id", "title", "mediaType", "status"], icon: "ep:video-camera" } }
  ]
} satisfies RouteConfigsTable;
