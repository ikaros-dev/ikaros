import { $t } from "@/plugins/i18n";
const Layout = () => import("@/layout/index.vue");

export default [
  {
    path: "/setup",
    name: "Setup",
    component: () => import("@/views/setup/index.vue"),
    meta: { title: "初始化 Ikaros", showLink: false }
  },
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/login/index.vue"),
    meta: {
      title: $t("menus.pureLogin"),
      showLink: false
    }
  },
  {
    path: "/register",
    name: "Register",
    component: () => import("@/views/login/register.vue"),
    meta: { title: "注册", showLink: false }
  },
  { path: "/login/verify", name: "LoginVerify", component: () => import("@/views/login/Recovery.vue"), meta: { title: "登录验证", showLink: false } },
  { path: "/login/recovery", name: "LoginRecovery", component: () => import("@/views/login/Recovery.vue"), meta: { title: "账号恢复", showLink: false } },
  { path: "/login/recovery/verify", name: "LoginRecoveryVerify", component: () => import("@/views/login/Recovery.vue"), meta: { title: "恢复验证", showLink: false } },
  { path: "/login/recovery/reset", name: "LoginRecoveryReset", component: () => import("@/views/login/Recovery.vue"), meta: { title: "重设密码", showLink: false } },
  // 全屏403（无权访问）页面
  {
    path: "/access-denied",
    name: "AccessDenied",
    component: () => import("@/views/error/403.vue"),
    meta: {
      title: $t("menus.pureAccessDenied"),
      showLink: false
    }
  },
  // 全屏500（服务器出错）页面
  {
    path: "/server-error",
    name: "ServerError",
    component: () => import("@/views/error/500.vue"),
    meta: {
      title: $t("menus.pureServerError"),
      showLink: false
    }
  },
  {
    path: "/redirect",
    component: Layout,
    meta: {
      title: $t("status.pureLoad"),
      showLink: false
    },
    children: [
      {
        path: "/redirect/:path(.*)",
        name: "Redirect",
        component: () => import("@/layout/redirect.vue")
      }
    ]
  }
] satisfies Array<RouteConfigsTable>;
