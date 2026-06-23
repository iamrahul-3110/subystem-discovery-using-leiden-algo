<template>
  <section class="config-block">
    <h2>AI Summary</h2>
    <label>
      LLM model
      <select :value="llmModel" @change="$emit('update:llmModel', $event.target.value)">
        <option v-for="option in llmModels" :key="option.value" :value="option.value">
          {{ option.label }}
        </option>
      </select>
    </label>
    <label>
      Summary type
      <select :value="summaryType" @change="$emit('update:summaryType', $event.target.value)">
        <option value="LESS_DETAILED">Executive overview</option>
        <option value="MEDIUM_DETAILED">Architecture summary</option>
        <option value="COMPLETE_DETAILED">Detailed analysis</option>
      </select>
    </label>
    <button
      class="primary-action dark"
      type="button"
      :disabled="!hasDiscovery || loading"
      @click="$emit('summary')"
    >
      <svg v-if="loading" class="btn-spinner" viewBox="0 0 50 50">
        <circle class="path" cx="25" cy="25" r="20" fill="none" stroke-width="5"></circle>
      </svg>
      {{ loading ? 'Summarizing...' : 'Generate summary' }}
    </button>
  </section>
</template>

<script setup>
defineProps({
  llmModel: { type: String, required: true },
  summaryType: { type: String, required: true },
  llmModels: { type: Array, required: true },
  loading: { type: Boolean, default: false },
  hasDiscovery: { type: Boolean, default: false }
})

defineEmits(['update:llmModel', 'update:summaryType', 'summary'])
</script>
