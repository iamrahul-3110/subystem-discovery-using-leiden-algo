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
      <div class="header-actions" style="display: flex; align-items: center; gap: 12px;">
        <div class="snapshot-chip">
          <span>Analysis Time</span>
          <strong>{{ analysisTime || 'Not generated' }}</strong>
        </div>
        <button
          type="button"
          class="theme-toggle-btn"
          @click="toggleTheme"
          :title="isDark ? 'Switch to Light Theme' : 'Switch to Dark Theme'"
        >
          <svg v-if="isDark" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="5" />
            <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
          </svg>
          <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
          </svg>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted } from 'vue'

defineProps({
  analysisTime: { type: String, default: null },
  isCollapsed:  { type: Boolean, default: false }
})
defineEmits(['toggle-sidebar'])

const isDark = ref(false)

function toggleTheme() {
  isDark.value = !isDark.value
  if (isDark.value) {
    document.body.classList.add('dark')
    localStorage.setItem('theme', 'dark')
  } else {
    document.body.classList.remove('dark')
    localStorage.setItem('theme', 'light')
  }
}

onMounted(() => {
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'dark') {
    isDark.value = true
    document.body.classList.add('dark')
  } else {
    isDark.value = false
    document.body.classList.remove('dark')
  }
})
</script>
