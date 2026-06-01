import { computed, ref } from "vue";
import { defineStore } from "pinia";

export const useAuthStore = defineStore("auth", () => {
  const token = ref<string>("");
  const username = ref<string>("");

  const isAuthenticated = computed(() => token.value.length > 0);

  function setAuth(nextToken: string, nextUsername = ""): void {
    token.value = nextToken;
    username.value = nextUsername;
  }

  function clearAuth(): void {
    token.value = "";
    username.value = "";
  }

  return {
    token,
    username,
    isAuthenticated,
    setAuth,
    clearAuth
  };
});
