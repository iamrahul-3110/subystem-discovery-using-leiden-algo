<template>
  <section class="diagram-panel" style="flex: 1 1 0%; min-width: 0">
    <div
      class="mermaid-stage"
      ref="stageRef"
      @mousedown="onMouseDown"
      @mousemove="onMouseMove"
      @mouseup="onMouseUp"
      @mouseleave="onMouseLeave"
      @wheel="onWheel"
    >
      <div v-if="mermaidSvg && !loadingDiscovery" class="mermaid-info-container tooltip-container" @mousedown.stop>
        <i
          class="tooltip-icon"
          @mouseenter="showInfoPopup"
          @mouseleave="hideInfoPopup"
          style="font-size: 16px; cursor: help; padding: 4px; display: inline-block;"
        >ⓘ</i>

        <Transition name="fade">
          <div
            v-if="showPopup"
            class="mermaid-info-popup"
            @mouseenter="showInfoPopup"
            @mouseleave="hideInfoPopup"
          >
            <div class="popup-arrow"></div>
            <h4>Graph Interpretation</h4>
            
            <div class="legend-section">
              <h5>Coupling Strength (Lines)</h5>
              <div class="legend-item">
                <span class="line-indicator high"></span>
                <span><strong>High Coupling (Red):</strong> Strong dependencies between subsystems.</span>
              </div>
              <div class="legend-item">
                <span class="line-indicator medium"></span>
                <span><strong>Medium Coupling (Blue):</strong> Moderate dependencies between subsystems.</span>
              </div>
              <div class="legend-item">
                <span class="line-indicator low"></span>
                <span><strong>Low Coupling (Grey):</strong> Weak dependencies / loose coupling.</span>
              </div>
            </div>

            <div class="legend-section">
              <h5>Components & Layers</h5>
              <div class="legend-item">
                <span class="node-indicator root-node"></span>
                <span><strong>Subsystem Root:</strong> Orchestrator node with stability scores.</span>
              </div>
              <div class="legend-item">
                <span class="node-indicator api-node"></span>
                <span><strong>API Controller:</strong> HTTP entry points / controller layers.</span>
              </div>
              <div class="legend-item">
                <span class="node-indicator service-node"></span>
                <span><strong>Service Layer:</strong> Core business logic rules.</span>
              </div>
              <div class="legend-item">
                <span class="node-indicator repo-node"></span>
                <span><strong>Repository Layer:</strong> Database tables and entities.</span>
              </div>
              <div class="legend-item">
                <span class="node-indicator class-node"></span>
                <span><strong>Internal Details:</strong> Supporting helper classes and methods.</span>
              </div>
            </div>
          </div>
        </Transition>
      </div>

      <div v-if="mermaidSvg && !loadingDiscovery" class="mermaid-controls" @mousedown.stop>
        <button type="button" @click="zoomIn" title="Zoom In">＋</button>
        <button type="button" @click="zoomOut" title="Zoom Out">－</button>
        <button type="button" @click="resetZoom" title="Reset view">Reset</button>
        <span class="zoom-level">{{ Math.round(zoom * 100) }}%</span>
      </div>

      <div v-if="loadingDiscovery" class="diagram-skeleton">
        <div class="skeleton-nodes-row">
          <div class="skeleton-node pulsing" style="width: 70px; height: 70px; border-radius: 50%;"></div>
          <div class="skeleton-edge pulsing" style="width: 100px; height: 4px;"></div>
          <div class="skeleton-node pulsing" style="width: 90px; height: 90px; border-radius: 8px;"></div>
          <div class="skeleton-edge pulsing" style="width: 80px; height: 4px;"></div>
          <div class="skeleton-node pulsing" style="width: 70px; height: 70px; border-radius: 50%;"></div>
        </div>
        <div class="skeleton-text pulsing">Discovering subsystem boundaries using Leiden consensus clustering...</div>
      </div>

      <div
        v-else-if="mermaidSvg"
        class="mermaid-output"
        :style="outputStyle"
        v-html="mermaidSvg"
      ></div>
      <div v-else class="diagram-placeholder">Diagram will render after discovery.</div>
    </div>

    <!-- Merged Selected Cluster Details -->
    <div class="selected-cluster-sidebar-card">
      <p class="eyebrow">Selected Cluster</p>
      <h3>{{ selectedCluster?.name || 'Select a subsystem' }}</h3>
      <p class="selected-cluster-desc">{{ selectedCluster?.description || 'Expand or select a cluster to inspect its role.' }}</p>
      <div v-if="selectedCluster" class="detail-stats">
        <span>{{ formatNumber(selectedCluster.nodeCount) }} nodes</span>
        <span>{{ formatNumber(selectedCluster.edgeCount) }} internal edges</span>
        <span>{{ selectedCluster.centralNodes?.length || 0 }} representative nodes</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick } from 'vue'

const props = defineProps({
  mermaidSvg: { type: String, default: '' },
  loadingDiscovery: { type: Boolean, default: false },
  sortedSubsystems: { type: Array, required: true },
  selectedCluster: { type: Object, default: null }
})

const showPopup = ref(false)
let hideTimeout = null

function showInfoPopup() {
  if (hideTimeout) clearTimeout(hideTimeout)
  showPopup.value = true
}

function hideInfoPopup() {
  if (hideTimeout) clearTimeout(hideTimeout)
  hideTimeout = setTimeout(() => {
    showPopup.value = false
  }, 200)
}

const emit = defineEmits(['toggle-cluster'])

const stageRef = ref(null)
const zoom = ref(1)
const pan = reactive({ x: 0, y: 0 })
const isDragging = ref(false)
const dragStart = reactive({ x: 0, y: 0 })

const outputStyle = computed(() => {
  return {
    transform: `translate(${pan.x}px, ${pan.y}px)`,
    transition: isDragging.value ? 'none' : 'transform 0.1s ease-out',
    display: 'inline-block',
    userSelect: 'none',
    width: `${Math.round(zoom.value * 100)}%`,
    height: 'auto'
  }
})

watch(() => props.mermaidSvg, async (newVal) => {
  resetZoom()
  if (newVal) {
    await nextTick()
    attachNodeClickListeners()
  }
})

function onMouseDown(e) {
  if (e.button !== 0) return
  if (e.target.closest('.mermaid-controls')) return
  isDragging.value = true
  dragStart.x = e.clientX - pan.x
  dragStart.y = e.clientY - pan.y
}

function onMouseMove(e) {
  if (!isDragging.value) return
  pan.x = e.clientX - dragStart.x
  pan.y = e.clientY - dragStart.y
}

function onMouseUp() {
  isDragging.value = false
}

function onMouseLeave() {
  isDragging.value = false
}

function onWheel(e) {
  e.preventDefault()
  const zoomFactor = 0.08
  const direction = e.deltaY < 0 ? 1 : -1
  const newZoom = zoom.value + direction * zoomFactor * zoom.value
  zoom.value = Math.max(0.1, Math.min(10, newZoom))
}

function zoomIn() {
  zoom.value = Math.min(10, zoom.value + 0.15 * zoom.value)
}

function zoomOut() {
  zoom.value = Math.max(0.1, zoom.value - 0.15 * zoom.value)
}

function resetZoom() {
  zoom.value = 1
  pan.x = 0
  pan.y = 0
}

defineExpose({
  resetZoom
})

function attachNodeClickListeners() {
  const stage = stageRef.value
  if (!stage) return
  const rootNodes = stage.querySelectorAll('[id*="_root"]')
  rootNodes.forEach((node) => {
    const fullId = node.getAttribute('id')
    const match = fullId.match(/(m_[a-z0-9_]+)_root/)
    if (match) {
      const clusterMermaidId = match[1]
      node.style.cursor = 'pointer'
      
      // Highlight on hover
      node.style.transition = 'opacity 0.15s ease'
      node.addEventListener('mouseenter', () => {
        node.style.opacity = '0.75'
      })
      node.addEventListener('mouseleave', () => {
        node.style.opacity = '1'
      })

      // Click event
      node.addEventListener('click', (e) => {
        e.stopPropagation()
        const cluster = props.sortedSubsystems.find(c => mermaidId(c.id) === clusterMermaidId)
        if (cluster) {
          emit('toggle-cluster', cluster.id)
        }
      })
    }
  })
}

function mermaidId(raw) {
  return `m_${String(raw || 'cluster').toLowerCase().replace(/[^a-z0-9_]/g, '_')}`
}

function formatNumber(value) {
  if (value === null || value === undefined || value === '') return '-'
  return new Intl.NumberFormat('en-US').format(value)
}
</script>
