import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { loginApi, logoutApi, meApi, type LoginPayload, type UserProfile } from "../api/auth";
import { AUTH_TOKEN_KEY } from "../utils/http";

export const useAuthStore = defineStore("auth", () => {
  const token = ref<string>(localStorage.getItem(AUTH_TOKEN_KEY) || "");
  const username = ref<string>("");
  const profile = ref<UserProfile | null>(null);

  const isAuthenticated = computed(() => token.value.length > 0);

  function setAuth(nextToken: string, nextUsername = ""): void {
    token.value = nextToken;
    username.value = nextUsername;
    localStorage.setItem(AUTH_TOKEN_KEY, nextToken);
  }

  function clearAuth(): void {
    token.value = "";
    username.value = "";
    profile.value = null;
    localStorage.removeItem(AUTH_TOKEN_KEY);
  }

  async function login(payload: LoginPayload): Promise<void> {
    const result = await loginApi(payload);
    setAuth(result.token, result.username || payload.username);
    await fetchMe();
  }

  async function logout(): Promise<void> {
    try {
      if (token.value) {
        await logoutApi();
      }
    } finally {
      clearAuth();
    }
  }

  async function fetchMe(): Promise<UserProfile | null> {
    if (!token.value) {
      return null;
    }
    const me = await meApi();
    profile.value = me;
    username.value = me.username;
    return me;
  }

  return {
    token,
    username,
    profile,
    isAuthenticated,
    setAuth,
    clearAuth,
    login,
    logout,
    fetchMe
  };
});
