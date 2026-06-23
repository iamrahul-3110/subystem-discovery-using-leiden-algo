<template>
  <header class="app-header">
    <div class="app-bar">
      <div class="header-title-container">
        <button
          class="lnb-toggle"
          type="button"
          @click="$emit('toggle-sidebar')"
          :title="isCollapsed ? 'Show Sidebar' : 'Hide Sidebar'"
        >
          <svg v-if="isCollapsed" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M4 6h16M4 12h16M4 18h16"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M15 19l-7-7 7-7"/>
          </svg>
        </button>
        <div>
          <p class="eyebrow">Graph Intelligence</p>
          <h1>Subsystem Discovery</h1>
        </div>
      </div>
      <div class="snapshot-chip">
        <span>Analysis Time</span>
        <strong>{{ analysisTime || 'Not generated' }}</strong>
      </div>
    </div>
  </header>
</template>

<script setup>
defineProps({
  analysisTime: { type: String, default: null },
  isCollapsed:  { type: Boolean, default: false }
})
defineEmits(['toggle-sidebar'])
</script>
