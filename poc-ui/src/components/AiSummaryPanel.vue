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
        <option value="CUSTOM">Custom prompt (chat)</option>
      </select>
    </label>
    <label v-if="summaryType === 'CUSTOM'">
      Custom query / instruction
      <textarea
        :value="customPrompt"
        @input="$emit('update:customPrompt', $event.target.value)"
        placeholder="e.g., list all subsystems having stability below 0.8..."
        style="width: 100%; height: 80px; border: 1px solid #aeb6c2; border-radius: 5px; padding: 7px 9px; color: #0f172a; background: #ffffff; font-family: inherit; resize: none; overflow-y: auto; box-sizing: border-box;"
      ></textarea>
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
  customPrompt: { type: String, default: '' },
  llmModels: { type: Array, required: true },
  loading: { type: Boolean, default: false },
  hasDiscovery: { type: Boolean, default: false }
})

defineEmits(['update:llmModel', 'update:summaryType', 'update:customPrompt', 'summary'])
</script>
