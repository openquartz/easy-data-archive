import { computed, readonly, ref } from "vue";
import { messages, type Locale } from "./messages";

const LOCALE_STORAGE_KEY = "easyarchive.locale";
const fallbackLocale: Locale = "zh-CN";
const availableLocales: Locale[] = ["zh-CN", "en-US"];

function resolveInitialLocale(): Locale {
  const stored = localStorage.getItem(LOCALE_STORAGE_KEY);
  if (stored === "zh-CN" || stored === "en-US") {
    return stored;
  }

  const browserLocale = navigator.language.toLowerCase();
  return browserLocale.startsWith("zh") ? "zh-CN" : "en-US";
}

const locale = ref<Locale>(resolveInitialLocale());

function resolveMessage(targetLocale: Locale, key: string): string | undefined {
  const path = key.split(".");
  let current: unknown = messages[targetLocale];
  for (const part of path) {
    if (!current || typeof current !== "object" || !(part in current)) {
      return undefined;
    }
    current = (current as Record<string, unknown>)[part];
  }
  return typeof current === "string" ? current : undefined;
}

function formatMessage(template: string, params?: Record<string, string | number>): string {
  if (!params) {
    return template;
  }
  return template.replace(/\{(\w+)\}/g, (_, token: string) => String(params[token] ?? `{${token}}`));
}

export function translate(key: string, params?: Record<string, string | number>): string {
  const currentMessage =
    resolveMessage(locale.value, key) ?? resolveMessage(fallbackLocale, key) ?? key;
  return formatMessage(currentMessage, params);
}

export function setLocale(nextLocale: Locale): void {
  locale.value = nextLocale;
  localStorage.setItem(LOCALE_STORAGE_KEY, nextLocale);
}

export function useI18n() {
  return {
    locale: readonly(locale),
    locales: availableLocales,
    setLocale,
    t: (key: string, params?: Record<string, string | number>) => translate(key, params),
    isZhCN: computed(() => locale.value === "zh-CN")
  };
}

export type { Locale };
