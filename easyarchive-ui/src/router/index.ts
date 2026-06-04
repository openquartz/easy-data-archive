import { createRouter, createWebHistory } from "vue-router";
import AppLayout from "../layouts/AppLayout.vue";
import LoginView from "../views/LoginView.vue";
import DashboardView from "../views/DashboardView.vue";
import DatasourceView from "../views/DatasourceView.vue";
import ArchiveGroupView from "../views/ArchiveGroupView.vue";
import ArchiveGroupDetailView from "../views/ArchiveGroupDetailView.vue";
import UserView from "../views/UserView.vue";
import TaskListView from "../views/TaskListView.vue";
import TaskDetailView from "../views/TaskDetailView.vue";
import GuideView from "../views/GuideView.vue";
import OperationLogView from "../views/OperationLogView.vue";
import { useAuthStore } from "../stores/auth";
import { AUTH_EXPIRED_EVENT } from "../utils/http";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      component: AppLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: "",
          redirect: { name: "dashboard" }
        },
        {
          path: "dashboard",
          name: "dashboard",
          component: DashboardView
        },
        {
          path: "datasources",
          name: "datasources",
          component: DatasourceView
        },
        {
          path: "archive/groups",
          name: "archive-groups",
          component: ArchiveGroupView
        },
        {
          path: "archive/groups/:id/detail",
          name: "archive-group-detail",
          component: ArchiveGroupDetailView
        },
        {
          path: "users",
          name: "users",
          component: UserView,
          meta: { adminOnly: true }
        },
        {
          path: "system/operation-logs",
          name: "operation-logs",
          component: OperationLogView,
          meta: { adminOnly: true }
        },
        {
          path: "tasks",
          name: "tasks",
          component: TaskListView
        },
        {
          path: "guide",
          name: "guide",
          component: GuideView
        },
        {
          path: "tasks/:taskId",
          name: "task-detail",
          component: TaskDetailView
        }
      ]
    },
    {
      path: "/login",
      name: "login",
      component: LoginView,
      meta: { requiresAuth: false }
    }
  ]
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  if (to.meta.requiresAuth) {
    if (!authStore.isAuthenticated) {
      return { name: "login", query: { redirect: to.fullPath } };
    }
    const sessionOk = await authStore.ensureSession();
    if (!sessionOk) {
      return { name: "login", query: { redirect: to.fullPath } };
    }
    if (to.meta.adminOnly && !authStore.profile?.isAdmin) {
      return { name: "dashboard" };
    }
  }

  if (to.name === "login" && authStore.isAuthenticated) {
    const sessionOk = await authStore.ensureSession();
    if (!sessionOk) {
      return true;
    }
    return { name: "dashboard" };
  }

  return true;
});

window.addEventListener(AUTH_EXPIRED_EVENT, () => {
  const authStore = useAuthStore();
  authStore.clearAuth();
  const current = router.currentRoute.value;
  if (current.name !== "login") {
    router.push({ name: "login", query: { redirect: current.fullPath } });
  }
});

export default router;
