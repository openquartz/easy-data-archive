import { createRouter, createWebHistory } from "vue-router";
import { h } from "vue";
import AppLayout from "../layouts/AppLayout.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      component: AppLayout,
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
    }
  ]
});

export default router;
