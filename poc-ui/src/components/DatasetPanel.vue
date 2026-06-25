<template>
  <section class="config-block">
    <h2>Dataset</h2>
    <label>
      Application
      <select :value="selectedTemplate" @change="$emit('update:selectedTemplate', $event.target.value)">
        <option v-for="app in applications" :key="app.value" :value="app.value">
          {{ app.label }}
        </option>
      </select>
    </label>
    <label>
      Node count
      <select :value="selectedNodeCount" @change="$emit('update:selectedNodeCount', Number($event.target.value))">
        <option v-for="size in nodeCounts" :key="size" :value="size">
          {{ formatNumber(size) }} nodes
        </option>
      </select>
    </label>
    <button class="primary-action" type="button" :disabled="loading" @click="$emit('generate')">
      <svg v-if="loading" class="btn-spinner" viewBox="0 0 50 50">
        <circle class="path" cx="25" cy="25" r="20" fill="none" stroke-width="5"></circle>
      </svg>
      {{ loading ? 'Generating graph...' : 'Generate graph' }}
    </button>
  </section>
</template>

<script setup>
defineProps({
  loading: { type: Boolean, default: false },
  applications: { type: Array, required: true },
  nodeCounts: { type: Array, required: true },
  selectedTemplate: { type: String, required: true },
  selectedNodeCount: { type: Number, required: true }
})

defineEmits(['generate', 'update:selectedTemplate', 'update:selectedNodeCount'])

function formatNumber(value) {
  if (value === null || value === undefined || value === '') return '-'
  return new Intl.NumberFormat('en-US').format(value)
}
</script>
