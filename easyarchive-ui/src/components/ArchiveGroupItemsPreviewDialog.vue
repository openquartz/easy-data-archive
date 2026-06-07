<script setup lang="ts">
import type { ArchiveGroupItemSummary } from "../api/archiveGroupItem";
import { computed } from "vue";
import { useI18n } from "../i18n";
import { archiveEnableStatusDictionary, getStatusLabel } from "../utils/dictionaries";
import { formatArchiveGroupItemRange } from "../utils/archiveGroupItemRange";
import { splitArchiveGroupItemsByType } from "../utils/archiveGroupItemPreview";

const props = defineProps<{
  visible: boolean;
  loading?: boolean;
  groupName?: string;
  items: ArchiveGroupItemSummary[];
}>();

const emit = defineEmits<{
  (event: "close"): void;
}>();

const { t } = useI18n();
const groupedItems = computed(() => splitArchiveGroupItemsByType(props.items));
const title = computed(() => t("archiveGroup.preview.title", { name: props.groupName || "-" }));

function formatSwitchFlag(value?: number): string {
  if (value === 0) {
    return t("common.yes");
  }
  if (value === 1) {
    return t("common.no");
  }
  return "-";
}
</script>

<template>
  <div v-if="visible" class="modal-backdrop" @click.self="emit('close')">
    <section class="modal-card modal-card--preview">
      <header class="modal-card__header">
        <h3>{{ title }}</h3>
      </header>

      <div class="preview-content">
        <div v-if="loading" class="empty">{{ t("archiveGroup.item.emptyLoading") }}</div>
        <template v-else>
          <section class="preview-section">
            <header class="preview-section__header">
              <h4>{{ t("archiveGroup.preview.timeSection") }}</h4>
            </header>
            <div v-if="!groupedItems.timeItems.length" class="empty">{{ t("archiveGroup.preview.emptyTime") }}</div>
            <div v-else class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th>{{ t("archiveGroup.item.sourceTable") }}</th>
                    <th>{{ t("archiveGroup.item.targetTable") }}</th>
                    <th>{{ t("archiveGroup.item.range") }}</th>
                    <th>{{ t("archiveGroup.item.priority") }}</th>
                    <th>{{ t("archiveGroup.item.stepCount") }}</th>
                    <th>{{ t("archiveGroup.item.enableWrite") }}</th>
                    <th>{{ t("archiveGroup.item.enableClean") }}</th>
                    <th>{{ t("archiveGroup.item.status") }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in groupedItems.timeItems" :key="item.id">
                    <td>{{ item.sourceTable }}</td>
                    <td>{{ item.targetTable }}</td>
                    <td>{{ formatArchiveGroupItemRange(item) }}</td>
                    <td>{{ item.priority }}</td>
                    <td>{{ item.stepCount ?? "-" }}</td>
                    <td>{{ formatSwitchFlag(item.enableWrite) }}</td>
                    <td>{{ formatSwitchFlag(item.enableClean) }}</td>
                    <td>{{ getStatusLabel(archiveEnableStatusDictionary, item.enableStatus) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section class="preview-section">
            <header class="preview-section__header">
              <h4>{{ t("archiveGroup.preview.idSection") }}</h4>
            </header>
            <div v-if="!groupedItems.idItems.length" class="empty">{{ t("archiveGroup.preview.emptyId") }}</div>
            <div v-else class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th>{{ t("archiveGroup.item.sourceTable") }}</th>
                    <th>{{ t("archiveGroup.item.targetTable") }}</th>
                    <th>{{ t("archiveGroup.item.range") }}</th>
                    <th>{{ t("archiveGroup.item.priority") }}</th>
                    <th>{{ t("archiveGroup.item.stepCount") }}</th>
                    <th>{{ t("archiveGroup.item.enableWrite") }}</th>
                    <th>{{ t("archiveGroup.item.enableClean") }}</th>
                    <th>{{ t("archiveGroup.item.status") }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in groupedItems.idItems" :key="item.id">
                    <td>{{ item.sourceTable }}</td>
                    <td>{{ item.targetTable }}</td>
                    <td>{{ formatArchiveGroupItemRange(item) }}</td>
                    <td>{{ item.priority }}</td>
                    <td>{{ item.stepCount ?? "-" }}</td>
                    <td>{{ formatSwitchFlag(item.enableWrite) }}</td>
                    <td>{{ formatSwitchFlag(item.enableClean) }}</td>
                    <td>{{ getStatusLabel(archiveEnableStatusDictionary, item.enableStatus) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </template>
      </div>

      <footer class="modal-card__footer">
        <button type="button" class="btn btn--subtle" @click="emit('close')">{{ t("common.cancel") }}</button>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.modal-card--preview {
  width: min(1120px, calc(100vw - 3rem));
  max-width: 96vw;
  max-height: 90vh;
  overflow: auto;
}

.preview-content {
  display: grid;
  gap: 1rem;
}

.preview-section {
  display: grid;
  gap: 0.75rem;
}

.preview-section__header h4 {
  margin: 0;
}

@media (max-width: 640px) {
  .modal-card--preview {
    width: calc(100vw - 1.5rem);
    max-height: 92vh;
  }
}
</style>
