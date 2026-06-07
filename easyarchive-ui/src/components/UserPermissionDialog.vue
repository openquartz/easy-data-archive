<script setup lang="ts">
import type { Datasource } from "../api/datasource";
import { computed, ref, watch } from "vue";
import { useI18n } from "../i18n";

const props = defineProps<{
  visible: boolean;
  loading?: boolean;
  submitting?: boolean;
  username?: string;
  datasources: Datasource[];
  selectedIds: number[];
  isAdminUser?: boolean;
}>();

const emit = defineEmits<{
  (event: "close"): void;
  (event: "submit", datasourceIds: number[]): void;
}>();

const { t } = useI18n();
const localSelectedIds = ref<number[]>([]);
const keyword = ref("");

watch(
  () => [props.visible, props.selectedIds],
  () => {
    localSelectedIds.value = [...props.selectedIds];
    keyword.value = "";
  },
  { immediate: true }
);

const filteredDatasources = computed(() => {
  const term = keyword.value.trim().toLowerCase();
  if (!term) {
    return props.datasources;
  }
  return props.datasources.filter((item) => {
    return [item.datasourceCode, item.datasourceName, item.datasourceType]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(term));
  });
});

function toggleDatasource(id: number): void {
  const next = new Set(localSelectedIds.value);
  if (next.has(id)) {
    next.delete(id);
  } else {
    next.add(id);
  }
  localSelectedIds.value = Array.from(next).sort((a, b) => a - b);
}

function handleSubmit(): void {
  if (props.submitting || props.isAdminUser) {
    return;
  }
  emit("submit", [...localSelectedIds.value]);
}
</script>

<template>
  <div v-if="visible" class="modal-backdrop" @click.self="emit('close')">
    <section class="modal-card modal-card--wide modal-card--resizable">
      <header class="modal-card__header">
        <h3>{{ t("user.permissions.title", { username: username || "-" }) }}</h3>
      </header>
      <div class="form-grid">
        <p v-if="isAdminUser" class="feedback">
          {{ t("user.permissions.adminReadonly") }}
        </p>
        <label class="full-width">
          {{ t("user.permissions.search") }}
          <input v-model="keyword" :disabled="loading || submitting || isAdminUser" />
        </label>
        <div v-if="loading" class="empty full-width">{{ t("common.loading") }}</div>
        <div v-else-if="!filteredDatasources.length" class="empty full-width">{{ t("user.permissions.empty") }}</div>
        <div v-else class="permission-grid full-width">
          <label
            v-for="item in filteredDatasources"
            :key="item.id"
            class="permission-tile"
            :class="{ 'permission-tile--checked': localSelectedIds.includes(item.id) }"
          >
            <input
              type="checkbox"
              :checked="localSelectedIds.includes(item.id)"
              :disabled="submitting || isAdminUser"
              @change="toggleDatasource(item.id)"
            />
            <div>
              <strong>{{ item.datasourceName }}</strong>
              <p>{{ item.datasourceCode }}</p>
              <small>{{ item.datasourceType }}</small>
            </div>
          </label>
        </div>
        <footer class="modal-card__footer full-width">
          <button type="button" class="btn btn--subtle" :disabled="submitting" @click="emit('close')">
            {{ t("common.cancel") }}
          </button>
          <button type="button" class="btn btn--primary" :disabled="submitting || isAdminUser" @click="handleSubmit">
            {{ submitting ? t("common.saving") : t("common.save") }}
          </button>
        </footer>
      </div>
    </section>
  </div>
</template>

<style scoped>
.modal-card--wide {
  width: min(860px, calc(100vw - 3rem));
}

.modal-card--resizable {
  min-width: 560px;
  min-height: 360px;
  max-width: 96vw;
  max-height: 90vh;
  overflow: auto;
  resize: both;
}

.permission-grid {
  display: grid;
  gap: 0.85rem;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
}

.permission-tile {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  padding: 0.9rem 1rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.75);
  cursor: pointer;
}

.permission-tile--checked {
  border-color: rgba(13, 148, 136, 0.8);
  background: rgba(204, 251, 241, 0.6);
}

.permission-tile p,
.permission-tile small {
  margin: 0.15rem 0 0;
}

@media (max-width: 640px) {
  .modal-card--resizable {
    min-width: 0;
    width: calc(100vw - 1.5rem);
    max-height: 92vh;
    resize: none;
  }
}
</style>
