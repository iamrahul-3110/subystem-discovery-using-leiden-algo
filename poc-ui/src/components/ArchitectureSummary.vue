<template>
  <section class="summary-row-container">
    <article
      class="summary-panel full-width-summary"
      :class="{ 'collapsed': isCollapsed, 'discovery-collapsed': isDiscoveryCollapsed }"
    >
      <div class="panel-heading">
        <div>
          <p class="eyebrow">Graph Intelligence Explanation</p>
          <h2>Architecture summary</h2>
        </div>
        <div class="header-actions" style="display: flex; align-items: center; gap: 8px;">
          <span v-if="actualModelDisplay" class="diagram-badge">
            {{ actualModelDisplay }}
          </span>
          <button
            type="button"
            class="collapse-toggle-btn icon-only"
            @click="$emit('toggle-collapse')"
            :title="isCollapsed ? 'Expand Summary' : 'Collapse Summary'"
          >
            <svg v-if="isCollapsed" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M6 9l6 6 6-6" />
            </svg>
            <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M18 15l-6-6-6 6" />
            </svg>
          </button>
        </div>
      </div>
      <div class="summary-body">
        <p v-if="!summaryText">Generate a summary after discovery to explain the subsystem boundaries.</p>
        <div v-else class="summary-container" v-html="formattedSummaryHtml"></div>
      </div>
    </article>
  </section>
</template>

<script setup>
defineProps({
  summaryText: { type: String, default: '' },
  formattedSummaryHtml: { type: String, default: '' },
  actualModelDisplay: { type: String, default: '' },
  isCollapsed: { type: Boolean, default: false },
  isDiscoveryCollapsed: { type: Boolean, default: false }
})

defineEmits(['toggle-collapse'])
</script>
