import { createRouter, createWebHistory } from "vue-router";
import { h } from "vue";
import AppLayout from "../layouts/AppLayout.vue";
import LoginView from "../views/LoginView.vue";
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
          name: "home",
          component: {
            render() {
              return h("section", { class: "page-placeholder" }, [
                h("h1", "EasyArchive Console"),
                h("p", "UI module initialized.")
              ]);
            }
          }
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
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: "login", query: { redirect: to.fullPath } };
  }

  if (to.name === "login" && authStore.isAuthenticated) {
    return { name: "home" };
  }

  return true;
});

window.addEventListener(AUTH_EXPIRED_EVENT, () => {
  const current = router.currentRoute.value;
  if (current.name !== "login") {
    router.push({ name: "login", query: { redirect: current.fullPath } });
  }
});

export default router;
