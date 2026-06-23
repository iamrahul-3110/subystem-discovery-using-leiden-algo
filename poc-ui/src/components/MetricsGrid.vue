<template>
  <section class="metrics-grid">
    <article>
      <span>Total nodes</span>
      <strong>{{ formatNumber(totalNodes) }}</strong>
    </article>
    <article>
      <span>Total edges</span>
      <strong>{{ formatNumber(totalEdges) }}</strong>
    </article>
    <article>
      <span>Subsystems</span>
      <strong>{{ formatNumber(subsystemCount) }}</strong>
    </article>
    <article>
      <div class="metric-label">
        <span>Average stability</span>
        <span class="tooltip-container tooltip-align-right-down">
          <i class="tooltip-icon">ⓘ</i>
          <span class="tooltip-box">
            <strong>Average Stability</strong><br/><br/>
            Average confidence score across all discovered subsystems.<br/><br/>
            Range: 0.00 - 1.00<br/><br/>
            Interpretation:<br/>
            • 0.90 - 1.00 = Excellent subsystem separation<br/>
            • 0.75 - 0.90 = Good subsystem boundaries<br/>
            • 0.50 - 0.75 = Moderate overlap between domains<br/>
            • Below 0.50 = Weak subsystem structure<br/><br/>
            Higher scores indicate nodes consistently clustered together across multiple Leiden runs.
          </span>
        </span>
      </div>
      <strong>{{ averageStability }}</strong>
    </article>
  </section>
</template>

<script setup>
defineProps({
  totalNodes: { type: Number, required: true },
  totalEdges: { type: Number, required: true },
  subsystemCount: { type: Number, required: true },
  averageStability: { type: [Number, String], required: true }
})

function formatNumber(value) {
  if (value === null || value === undefined || value === '') return '-'
  return new Intl.NumberFormat('en-US').format(value)
}
</script>
