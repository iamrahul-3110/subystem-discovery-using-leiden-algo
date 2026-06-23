<template>
  <section class="command-row">
    <div class="command-group">
      <span>Application</span>
      <strong>{{ currentApplicationLabel }}</strong>
    </div>
    <div class="command-group">
      <span>Dataset size</span>
      <strong>{{ formatNumber(nodeCount) }}</strong>
    </div>
    <div class="command-group">
      <span>Relations</span>
      <strong>{{ formatNumber(relationCount) }}</strong>
    </div>
    <div class="command-group">
      <span>Effective runs</span>
      <strong>{{ formatNumber(effectiveRuns) }}</strong>
    </div>
    <div class="command-status" :class="{ busy: isBusy }">
      {{ statusText }}
    </div>
  </section>
</template>

<script setup>
defineProps({
  currentApplicationLabel: { type: String, required: true },
  nodeCount: { type: Number, required: true },
  relationCount: { type: [Number, String], default: '-' },
  effectiveRuns: { type: [Number, String], default: '-' },
  isBusy: { type: Boolean, default: false },
  statusText: { type: String, required: true }
})

function formatNumber(value) {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'string') return value
  return new Intl.NumberFormat('en-US').format(value)
}
</script>
