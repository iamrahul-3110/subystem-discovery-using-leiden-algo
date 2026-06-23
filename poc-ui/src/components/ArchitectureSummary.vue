<template>
  <section class="summary-row-container">
    <article class="summary-panel full-width-summary">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">Graph Intelligence Explanation</p>
          <h2>Architecture summary</h2>
        </div>
        <span v-if="actualModelDisplay" class="diagram-badge">
          {{ actualModelDisplay }}
        </span>
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
  actualModelDisplay: { type: String, default: '' }
})
</script>
