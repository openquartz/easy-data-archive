<script setup lang="ts">
import { computed } from "vue";
import { getStatusLabel, getStatusTagClass, taskStatusDictionary } from "../utils/dictionaries";

const props = defineProps<{
  status?: number;
}>();

const text = computed(() => getStatusLabel(taskStatusDictionary, props.status));
const cssClass = computed(() => getStatusTagClass(taskStatusDictionary, props.status));
</script>

<template>
  <span :class="cssClass">{{ text }}</span>
</template>
