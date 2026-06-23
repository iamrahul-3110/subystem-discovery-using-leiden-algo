<template>
  <aside class="side-rail" :class="{ 'side-rail-collapsed': isCollapsed }">
    <div class="brand-mark">GI</div>
    <nav>
      <button class="nav-item active" type="button">Subsystem Discovery</button>
    </nav>

    <DatasetPanel
      :loading="loading.dataset"
      :applications="applications"
      :nodeCounts="nodeCounts"
      :selectedTemplate="selectedTemplate"
      :selectedNodeCount="selectedNodeCount"
      @update:selectedTemplate="$emit('update:selectedTemplate', $event)"
      @update:selectedNodeCount="$emit('update:selectedNodeCount', $event)"
      @generate="$emit('generate')"
    />

    <LeidenConfigPanel
      :runs="runs"
      :consensusThreshold="consensusThreshold"
      :resolution="resolution"
      :loading="loading.discovery"
      :hasDataset="hasDataset"
      @update:runs="$emit('update:runs', $event)"
      @update:consensusThreshold="$emit('update:consensusThreshold', $event)"
      @update:resolution="$emit('update:resolution', $event)"
      @discover="$emit('discover')"
    />

    <AiSummaryPanel
      :llmModel="llmModel"
      :summaryType="summaryType"
      :llmModels="llmModels"
      :loading="loading.summary"
      :hasDiscovery="hasDiscovery"
      @update:llmModel="$emit('update:llmModel', $event)"
      @update:summaryType="$emit('update:summaryType', $event)"
      @summary="$emit('summary')"
    />
  </aside>
</template>

<script setup>
import DatasetPanel from './DatasetPanel.vue'
import LeidenConfigPanel from './LeidenConfigPanel.vue'
import AiSummaryPanel from './AiSummaryPanel.vue'

defineProps({
  isCollapsed: { type: Boolean, default: false },
  loading: { type: Object, required: true },
  applications: { type: Array, required: true },
  nodeCounts: { type: Array, required: true },
  selectedTemplate: { type: String, required: true },
  selectedNodeCount: { type: Number, required: true },
  runs: { type: Number, required: true },
  consensusThreshold: { type: Number, required: true },
  resolution: { type: Number, required: true },
  llmModel: { type: String, required: true },
  summaryType: { type: String, required: true },
  llmModels: { type: Array, required: true },
  hasDataset: { type: Boolean, default: false },
  hasDiscovery: { type: Boolean, default: false }
})

defineEmits([
  'update:selectedTemplate',
  'update:selectedNodeCount',
  'update:runs',
  'update:consensusThreshold',
  'update:resolution',
  'update:llmModel',
  'update:summaryType',
  'generate',
  'discover',
  'summary'
])
</script>
