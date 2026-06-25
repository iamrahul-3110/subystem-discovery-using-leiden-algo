<template>
  <section class="config-block">
    <h2>Leiden Config</h2>
    <label>
      <div class="label-header">
        Runs
        <span class="tooltip-container">
          <i class="tooltip-icon">ⓘ</i>
          <span class="tooltip-box">
            <strong>Runs</strong><br/><br/>
            Number of independent Leiden algorithm executions used to build the final consensus clustering.<br/><br/>
            Range: 1 - 100<br/>
            Recommended: 5 - 20<br/>
            Default: 10<br/><br/>
            Higher values improve subsystem stability and reduce random variations but increase execution time.<br/><br/>
            Use:<br/>
            • 5-10 for quick exploration<br/>
            • 10-20 for production-quality subsystem discovery<br/>
            • 20+ for very large codebases where maximum stability is required
          </span>
        </span>
      </div>
      <input
        :value="runs"
        @input="$emit('update:runs', Number($event.target.value))"
        type="number"
        min="1"
        max="100"
      />
    </label>
    <label>
      <div class="label-header">
        Consensus threshold
        <span class="tooltip-container">
          <i class="tooltip-icon">ⓘ</i>
          <span class="tooltip-box">
            <strong>Consensus Threshold</strong><br/><br/>
            Determines how consistently nodes must appear together across multiple runs before being assigned to the same subsystem.<br/><br/>
            Range: 0.10 - 0.95<br/>
            Recommended: 0.60 - 0.80<br/>
            Default: 0.70<br/><br/>
            Lower values create larger subsystems by grouping nodes more aggressively.<br/><br/>
            Higher values create stricter subsystem boundaries and may produce more subsystems.<br/><br/>
            Examples:<br/>
            • 0.50 = More permissive grouping<br/>
            • 0.70 = Balanced discovery<br/>
            • 0.90 = Very strict clustering
          </span>
        </span>
      </div>
      <input
        :value="consensusThreshold"
        @input="$emit('update:consensusThreshold', Number($event.target.value))"
        type="number"
        min="0.1"
        max="0.95"
        step="0.05"
      />
    </label>
    <label>
      <div class="label-header">
        Resolution
        <span class="tooltip-container">
          <i class="tooltip-icon">ⓘ</i>
          <span class="tooltip-box">
            <strong>Resolution</strong><br/><br/>
            Controls the granularity of subsystem discovery.<br/><br/>
            Range: 0.10 - 5.00<br/>
            Recommended: 0.50 - 2.00<br/>
            Default: 1.00<br/><br/>
            Lower values produce fewer, larger subsystems.<br/><br/>
            Higher values produce more, smaller subsystems.<br/><br/>
            Examples:<br/>
            • 0.50 = Broad business domains<br/>
            • 1.00 = Balanced subsystem discovery<br/>
            • 2.00+ = Fine-grained technical modules
          </span>
        </span>
      </div>
      <input
        :value="resolution"
        @input="$emit('update:resolution', Number($event.target.value))"
        type="number"
        min="0.1"
        max="5"
        step="0.1"
      />
    </label>
    <button
      class="primary-action blue"
      type="button"
      :disabled="!hasDataset || loading"
      @click="$emit('discover')"
    >
      <svg v-if="loading" class="btn-spinner" viewBox="0 0 50 50">
        <circle class="path" cx="25" cy="25" r="20" fill="none" stroke-width="5"></circle>
      </svg>
      {{ loading ? 'Running Leiden...' : 'Discover subsystems' }}
    </button>
  </section>
</template>

<script setup>
defineProps({
  runs: { type: Number, required: true },
  consensusThreshold: { type: Number, required: true },
  resolution: { type: Number, required: true },
  loading: { type: Boolean, default: false },
  hasDataset: { type: Boolean, default: false }
})

defineEmits([
  'update:runs',
  'update:consensusThreshold',
  'update:resolution',
  'discover'
])
</script>
