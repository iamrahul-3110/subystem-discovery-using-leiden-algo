<template>
  <aside class="side-rail" :class="{ 'side-rail-collapsed': isCollapsed }">
    <div class="brand-mark">GI</div>
    <nav>
      <div class="sidebar-feature-heading">Subsystem Discovery</div>
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
      :customPrompt="customPrompt"
      :llmModels="llmModels"
      :loading="loading.summary"
      :hasDiscovery="hasDiscovery"
      @update:llmModel="$emit('update:llmModel', $event)"
      @update:summaryType="$emit('update:summaryType', $event)"
      @update:customPrompt="$emit('update:customPrompt', $event)"
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
  customPrompt: { type: String, default: '' },
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
  'update:customPrompt',
  'generate',
  'discover',
  'summary'
])
</script>
