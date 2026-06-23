<template>
  <div class="workbench-shell">
    <!-- Animated Pop-up Toasts -->
    <Transition name="toast">
      <div v-if="successMessage" class="toast success-toast">
        <span class="toast-icon">✓</span>
        <span class="toast-message">{{ successMessage }}</span>
        <button type="button" class="toast-close" @click="successMessage = ''">×</button>
      </div>
    </Transition>
    <Transition name="toast">
      <div v-if="errorMessage" class="toast error-toast">
        <span class="toast-icon">⚠</span>
        <span class="toast-message">{{ errorMessage }}</span>
        <button type="button" class="toast-close" @click="errorMessage = ''">×</button>
      </div>
    </Transition>

    <AppHeader
      :analysisTime="dataset?.analysisTime"
      :isCollapsed="isLnbCollapsed"
      @toggle-sidebar="isLnbCollapsed = !isLnbCollapsed"
    />

    <div class="layout-grid">
      <AppSidebar
        :isCollapsed="isLnbCollapsed"
        :loading="loading"
        :applications="applications"
        :nodeCounts="nodeCounts"
        :selectedTemplate="selectedTemplate"
        :selectedNodeCount="selectedNodeCount"
        :runs="leiden.runs"
        :consensusThreshold="leiden.consensusThreshold"
        :resolution="leiden.resolution"
        :llmModel="llm.model"
        :summaryType="llm.summaryType"
        :llmModels="llmModels"
        :hasDataset="!!dataset"
        :hasDiscovery="!!discovery"
        @update:selectedTemplate="selectedTemplate = $event"
        @update:selectedNodeCount="selectedNodeCount = $event"
        @update:runs="leiden.runs = $event"
        @update:consensusThreshold="leiden.consensusThreshold = $event"
        @update:resolution="leiden.resolution = $event"
        @update:llmModel="llm.model = $event"
        @update:summaryType="llm.summaryType = $event"
        @generate="generateDataset"
        @discover="runDiscovery"
        @summary="generateSummary"
      />

      <main class="main-panel">
        <StatusBar
          :currentApplicationLabel="currentApplicationLabel"
          :nodeCount="dataset ? dataset.nodeCount : selectedNodeCount"
          :relationCount="dataset ? dataset.relationCount : '-'"
          :effectiveRuns="discovery?.algorithm?.runs || '-'"
          :isBusy="isBusy"
          :statusText="statusText"
        />

        <section v-if="!discovery" class="empty-panel">
          <div class="empty-card">
            <p class="eyebrow">Ready for POC</p>
            <h2>Generate a graph, then discover subsystem boundaries.</h2>
            <p>
              The demo creates application-specific dummy relations, runs Leiden discovery,
              and turns the result into a Graph Intelligence view.
            </p>
          </div>
        </section>

        <template v-else>
          <MetricsGrid
            :totalNodes="discovery.summary.totalNodes"
            :totalEdges="discovery.summary.totalEdges"
            :subsystemCount="discovery.summary.subsystemCount"
            :averageStability="discovery.summary.averageStability"
          />

          <section class="discovery-grid" ref="resizerContainerRef">
            <ClusterTree
              :sortedSubsystems="sortedSubsystems"
              :selectedClusterId="selectedClusterId"
              :expandedClusters="expandedClusters"
              :style="{ width: treeWidth + 'px', flex: '0 0 auto' }"
              @toggle-cluster="toggleCluster"
            />

            <div class="panel-resizer" :class="{ resizing: isResizing }" @mousedown="onResizerMouseDown">
              <div class="resizer-line"></div>
            </div>

            <DiagramStage
              ref="diagramStageRef"
              :mermaidSvg="mermaidSvg"
              :loadingDiscovery="loading.discovery"
              :sortedSubsystems="sortedSubsystems"
              :selectedCluster="selectedCluster"
              @toggle-cluster="toggleCluster"
            />
          </section>

          <ArchitectureSummary
            :summaryText="summaryText"
            :formattedSummaryHtml="formattedSummaryHtml"
            :actualModelDisplay="actualModelDisplay"
          />
        </template>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import axios from 'axios'
import mermaid from 'mermaid'

// Import Split Vue Components
import AppHeader from './components/AppHeader.vue'
import AppSidebar from './components/AppSidebar.vue'
import StatusBar from './components/StatusBar.vue'
import MetricsGrid from './components/MetricsGrid.vue'
import ClusterTree from './components/ClusterTree.vue'
import DiagramStage from './components/DiagramStage.vue'
import ArchitectureSummary from './components/ArchitectureSummary.vue'

mermaid.initialize({
  startOnLoad: false,
  theme: 'base',
  securityLevel: 'loose',
  flowchart: {
    htmlLabels: true,
    curve: 'basis'
  },
  themeVariables: {
    primaryColor: '#eff6ff',
    primaryTextColor: '#0f172a',
    primaryBorderColor: '#2563eb',
    lineColor: '#64748b',
    fontFamily: 'Inter, Segoe UI, Arial, sans-serif'
  }
})

const SERVER_CONTEXT = `${['code', 'analy', 'zer'].join('')}/server`
const API_BASE = `http://localhost:8081/${SERVER_CONTEXT}/api/poc`

const applications = [
  { label: 'Amazon', value: 'AMAZON' },
  { label: 'Swiggy', value: 'SWIGGY' },
  { label: 'Blinkit', value: 'BLINKIT' },
  { label: 'Zepto', value: 'ZEPTO' },
  { label: 'Myntra', value: 'MYNTRA' },
  { label: 'MakeMyTrip', value: 'MAKEMYTRIP' }
]

const llmModels = [
  { label: 'google/gemma-3-27b-it', value: 'google/gemma-3-27b-it' },
  { label: 'meta-llama/llama-3.3-70b-instruct', value: 'meta-llama/llama-3.3-70b-instruct' },
  { label: 'deepseek/deepseek-chat', value: 'deepseek/deepseek-chat' },
  { label: 'qwen/qwen3-32b', value: 'qwen/qwen3-32b' },
  { label: 'mistralai/mistral-small', value: 'mistralai/mistral-small' },
  { label: 'gemini-2.5-flash', value: 'gemini-2.5-flash' }
]

const nodeCounts = [100, 1000, 10000, 50000]

const selectedTemplate = ref('AMAZON')
const selectedNodeCount = ref(1000)
const dataset = ref(null)
const discovery = ref(null)
const mermaidSvg = ref('')
const summaryText = ref('')
const formattedSummaryHtml = ref('')
const selectedClusterId = ref(null)
const errorMessage = ref('')
const successMessage = ref('')
const expandedClusters = reactive(new Set())
const summaryMeta = reactive({ provider: '', fallback: false, llmModel: '' })

const diagramStageRef = ref(null)

const actualModelDisplay = computed(() => {
  if (!summaryMeta.provider) return ''
  if (summaryMeta.fallback) {
    return `${summaryMeta.llmModel} (Mock fallback)`
  }
  if (summaryMeta.provider === 'MOCK') {
    return 'Mock fallback'
  }
  return summaryMeta.llmModel || summaryMeta.provider
})

const isLnbCollapsed = ref(false)
const treeWidth = ref(380)
const isResizing = ref(false)
const resizerContainerRef = ref(null)

watch(successMessage, (newVal) => {
  if (newVal) {
    setTimeout(() => {
      if (successMessage.value === newVal) {
        successMessage.value = ''
      }
    }, 4000)
  }
})

watch(errorMessage, (newVal) => {
  if (newVal) {
    setTimeout(() => {
      if (errorMessage.value === newVal) {
        errorMessage.value = ''
      }
    }, 5000)
  }
})

const leiden = reactive({
  runs: 10,
  consensusThreshold: 0.7,
  resolution: 1.0
})

const llm = reactive({
  model: 'deepseek/deepseek-chat',
  summaryType: 'MEDIUM_DETAILED'
})

const loading = reactive({
  dataset: false,
  discovery: false,
  summary: false
})

const isBusy = computed(() => loading.dataset || loading.discovery || loading.summary)

const currentApplicationLabel = computed(() => {
  return applications.find((app) => app.value === selectedTemplate.value)?.label || selectedTemplate.value
})

const sortedSubsystems = computed(() => {
  return [...(discovery.value?.subsystems || [])].sort((a, b) => b.nodeCount - a.nodeCount)
})

const selectedCluster = computed(() => {
  return sortedSubsystems.value.find((cluster) => cluster.id === selectedClusterId.value) || sortedSubsystems.value[0]
})

const statusText = computed(() => {
  if (loading.dataset) return 'Generating graph'
  if (loading.discovery) return 'Running Leiden'
  if (loading.summary) return 'Generating summary'
  if (discovery.value) return 'Discovery ready'
  if (dataset.value) return 'Dataset ready'
  return 'Ready'
})

watch(
  () => [discovery.value, Array.from(expandedClusters).join('|')],
  async () => {
    if (discovery.value) {
      await renderMermaid()
    }
  },
  { deep: true }
)

async function generateDataset() {
  loading.dataset = true
  errorMessage.value = ''
  successMessage.value = ''
  discovery.value = null
  dataset.value = null
  mermaidSvg.value = ''
  summaryText.value = ''
  formattedSummaryHtml.value = ''
  summaryMeta.provider = ''
  summaryMeta.fallback = false
  expandedClusters.clear()
  diagramStageRef.value?.resetZoom()

  try {
    const response = await axios.post(`${API_BASE}/dataset/generate`, null, {
      params: {
        template: selectedTemplate.value,
        nodeCount: selectedNodeCount.value
      }
    })
    dataset.value = response.data
    successMessage.value = 'Graph generated successfully. Ready for Leiden discovery.'
  } catch (error) {
    handleError(error, 'Failed to generate graph data')
  } finally {
    loading.dataset = false
  }
}

async function runDiscovery() {
  if (!dataset.value) return
  loading.discovery = true
  errorMessage.value = ''
  successMessage.value = ''
  summaryText.value = ''
  formattedSummaryHtml.value = ''
  summaryMeta.provider = ''
  summaryMeta.fallback = false
  expandedClusters.clear()
  diagramStageRef.value?.resetZoom()

  try {
    const response = await axios.post(`${API_BASE}/discover`, null, {
      params: discoveryParams()
    })
    discovery.value = response.data
    successMessage.value = `Leiden discovery completed successfully with ${discovery.value.summary.subsystemCount} subsystems.`
    selectedClusterId.value = sortedSubsystems.value[0]?.id || null
    if (selectedClusterId.value) {
      expandedClusters.add(selectedClusterId.value)
    }
    await nextTick()
    await renderMermaid()
  } catch (error) {
    handleError(error, 'Subsystem discovery failed')
  } finally {
    loading.discovery = false
  }
}

async function generateSummary() {
  if (!dataset.value || !discovery.value) return
  loading.summary = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const response = await axios.post(`${API_BASE}/summary`, null, {
      params: {
        ...discoveryParams(),
        llmModel: llm.model,
        summaryType: llm.summaryType
      }
    })
    summaryText.value = response.data.summary
    formattedSummaryHtml.value = response.data.formattedSummary
    summaryMeta.provider = response.data.provider
    summaryMeta.fallback = response.data.fallback
    summaryMeta.llmModel = response.data.llmModel
    successMessage.value = `Summary successfully generated for ${currentApplicationLabel.value}.`
  } catch (error) {
    const reason = error.response?.data?.error || error.message || 'unknown error'
    errorMessage.value = `Summary generation failed due to: ${reason}`
  } finally {
    loading.summary = false
  }
}

function discoveryParams() {
  return {
    applicationId: dataset.value.applicationId,
    applicationKey: dataset.value.applicationKey,
    analysisTime: dataset.value.analysisTime,
    runs: leiden.runs,
    consensusThreshold: leiden.consensusThreshold,
    resolution: leiden.resolution
  }
}

function toggleCluster(clusterId) {
  selectedClusterId.value = clusterId
  if (expandedClusters.has(clusterId)) {
    expandedClusters.delete(clusterId)
  } else {
    expandedClusters.add(clusterId)
  }
}

async function renderMermaid() {
  try {
    const graph = buildMermaidGraph()
    const renderId = `gi-mermaid-${Date.now()}-${Math.round(Math.random() * 10000)}`
    const { svg } = await mermaid.render(renderId, graph)
    mermaidSvg.value = svg
  } catch (error) {
    mermaidSvg.value = ''
    errorMessage.value = `Mermaid rendering failed: ${error.message}`
  }
}

function onResizerMouseDown(e) {
  e.preventDefault()
  isResizing.value = true
  document.addEventListener('mousemove', onResizerMouseMove)
  document.addEventListener('mouseup', onResizerMouseUp)
  document.body.style.cursor = 'col-resize'
  document.body.classList.add('resizing-active')
}

function onResizerMouseMove(e) {
  if (!isResizing.value || !resizerContainerRef.value) return
  const containerRect = resizerContainerRef.value.getBoundingClientRect()
  const relativeX = e.clientX - containerRect.left
  treeWidth.value = Math.max(240, Math.min(600, relativeX))
}

function onResizerMouseUp() {
  isResizing.value = false
  document.removeEventListener('mousemove', onResizerMouseMove)
  document.removeEventListener('mouseup', onResizerMouseUp)
  document.body.style.cursor = ''
  document.body.classList.remove('resizing-active')
}

function buildMermaidGraph() {
  const clusters = sortedSubsystems.value.slice(0, 18)
  const clusterIds = new Set(clusters.map((cluster) => cluster.id))
  const lines = ['flowchart LR']

  clusters.forEach((cluster) => {
    const clusterId = mermaidId(cluster.id)
    const rootId = `${clusterId}_root`
    lines.push(`  subgraph ${clusterId}["${escapeMermaid(cluster.name)}"]`)
    lines.push(`    ${rootId}["${escapeMermaid(cluster.name)}<br/>${formatNumber(cluster.nodeCount)} nodes<br/>stability ${cluster.stabilityScore}"]`)

    if (expandedClusters.has(cluster.id)) {
      const apis = cluster.apiEndpoints || []
      const central = (cluster.centralNodes || []).slice(0, 12)

      const packages = central.filter(n => n.type === 'PACKAGE')
      const classes = central.filter(n => n.type === 'CLASS')
      const methods = central.filter(n => n.type === 'METHOD')

      // Classify classes
      const controllers = classes.filter(c => c.name.endsWith('Controller') || c.name.endsWith('Client'))
      const services = classes.filter(c => c.name.endsWith('Service') || c.name.endsWith('Policy') || c.name.endsWith('Workflow'))
      const repos = classes.filter(c => c.name.endsWith('Repository') || c.name.endsWith('Mapper') || c.name.endsWith('Dao'))

      // Draw APIs
      apis.forEach((api, index) => {
        const apiId = `${clusterId}_api_${index}`
        lines.push(`    ${apiId}(["${api.method} ${api.path}"])`)
        lines.push(`    class ${apiId} apiNode`)
        lines.push(`    ${rootId} -.-> ${apiId}`)

        // Link to matching Controller or fallback
        let targetController = controllers.find(ctrl => {
          const apiPrefix = api.path.split('/')[2] || ''
          return ctrl.name.toLowerCase().startsWith(apiPrefix.toLowerCase())
        })
        if (!targetController && controllers.length > 0) {
          targetController = controllers[0]
        }

        if (targetController) {
          const targetId = `${clusterId}_class_${targetController.id}`
          lines.push(`    ${apiId} == Routing ==> ${targetId}`)
        } else if (classes.length > 0) {
          const targetId = `${clusterId}_class_${classes[0].id}`
          lines.push(`    ${apiId} == Routing ==> ${targetId}`)
        }
      })

      // Draw Packages
      packages.forEach(pkg => {
        const pkgId = `${clusterId}_pkg_${pkg.id}`
        lines.push(`    ${pkgId}{{"Package: ${escapeMermaid(pkg.name)}"}}`)
        lines.push(`    class ${pkgId} packageNode`)
        lines.push(`    ${rootId} -.- ${pkgId}`)
      })

      // Draw Classes
      classes.forEach(c => {
        const classId = `${clusterId}_class_${c.id}`
        let classLabel = c.name
        let nodeShape = `["${classLabel}"]`
        let nodeClass = 'serviceNode'

        if (controllers.includes(c)) {
          nodeShape = `["[Controller] ${classLabel}"]`
          nodeClass = 'controllerNode'
        } else if (services.includes(c)) {
          nodeShape = `["[Service] ${classLabel}"]`
          nodeClass = 'serviceNode'
        } else if (repos.includes(c)) {
          nodeShape = `[("${classLabel} Repository")]`
          nodeClass = 'repoNode'
        } else {
          nodeShape = `["[Class] ${classLabel}"]`
          nodeClass = 'classNode'
        }

        lines.push(`    ${classId}${nodeShape}`)
        lines.push(`    class ${classId} ${nodeClass}`)

        // Link package to class if matching qualifiedName
        const lastDot = c.qualifiedName.lastIndexOf('.')
        const classPkgPath = lastDot !== -1 ? c.qualifiedName.substring(0, lastDot) : ''
        const matchingPkg = packages.find(pkg => pkg.qualifiedName === classPkgPath)
        if (matchingPkg) {
          const pkgId = `${clusterId}_pkg_${matchingPkg.id}`
          lines.push(`    ${pkgId} -. Contains .-> ${classId}`)
        } else {
          lines.push(`    ${rootId} --- ${classId}`)
        }
      })

      // Draw Methods
      methods.forEach(m => {
        const methodId = `${clusterId}_method_${m.id}`
        lines.push(`    ${methodId}("${m.name}()")`)
        lines.push(`    class ${methodId} methodNode`)

        // Link class to method
        const lastDotM = m.qualifiedName.lastIndexOf('.')
        const methodClassPath = lastDotM !== -1 ? m.qualifiedName.substring(0, lastDotM) : ''
        const matchingClass = classes.find(c => c.qualifiedName === methodClassPath)
        if (matchingClass) {
          const classId = `${clusterId}_class_${matchingClass.id}`
          lines.push(`    ${classId} === ${methodId}`)
        } else {
          lines.push(`    ${rootId} --- ${methodId}`)
        }
      })

      // Draw Class Call Chains
      // Controller -> Service
      controllers.forEach(ctrl => {
        const ctrlId = `${clusterId}_class_${ctrl.id}`
        services.forEach(srv => {
          const srvId = `${clusterId}_class_${srv.id}`
          const isRelated = ctrl.name.replace('Controller', '').toLowerCase() === srv.name.replace('Service', '').toLowerCase()
          if (isRelated || controllers.length === 1 || services.length === 1) {
            lines.push(`    ${ctrlId} --> ${srvId}`)
          }
        })
      })

      // Service -> Repo
      services.forEach(srv => {
        const srvId = `${clusterId}_class_${srv.id}`
        repos.forEach(rp => {
          const rpId = `${clusterId}_class_${rp.id}`
          const isRelated = srv.name.replace('Service', '').toLowerCase() === rp.name.replace('Repository', '').toLowerCase()
          if (isRelated || services.length === 1 || repos.length === 1) {
            lines.push(`    ${srvId} --> ${rpId}`)
          }
        })
      })
    }
    lines.push('  end')
  })

  ;(discovery.value?.subsystemLinks || [])
    .filter((link) => clusterIds.has(link.source) && clusterIds.has(link.target))
    .slice(0, 24)
    .forEach((link, index) => {
      const source = `${mermaidId(link.source)}_root`
      const target = `${mermaidId(link.target)}_root`
      const arrow = link.couplingStrength === 'HIGH' ? '==>' : link.couplingStrength === 'LOW' ? '-.->' : '-->'
      lines.push(`  ${source} ${arrow}|${escapeMermaid(link.couplingStrength)} / ${link.edgeCount} edges| ${target}`)
      lines.push(`  linkStyle ${index} stroke:${linkColor(link.couplingStrength)},stroke-width:${link.couplingStrength === 'HIGH' ? '3px' : '1.8px'}`)
    })

  lines.push('  classDef clusterRoot fill:#eff6ff,stroke:#2563eb,stroke-width:1.5px,color:#0f172a')
  lines.push('  classDef expandedNode fill:#ffffff,stroke:#94a3b8,color:#0f172a')
  lines.push('  classDef apiNode fill:#e2fcf1,stroke:#0f9f58,stroke-width:1.5px,color:#0b7a43,font-weight:700')
  lines.push('  classDef controllerNode fill:#eff6ff,stroke:#2563eb,stroke-width:1.5px,color:#1e40af,font-weight:700')
  lines.push('  classDef serviceNode fill:#f3e8ff,stroke:#9333ea,stroke-width:1.5px,color:#581c87')
  lines.push('  classDef repoNode fill:#ffedd5,stroke:#ea580c,stroke-width:1.5px,color:#7c2d12')
  lines.push('  classDef methodNode fill:#f8fafc,stroke:#94a3b8,stroke-width:1px,color:#475569,font-style:italic')
  lines.push('  classDef packageNode fill:#fffbeb,stroke:#d97706,stroke-width:1.2px,color:#78350f')
  lines.push('  classDef classNode fill:#f1f5f9,stroke:#475569,stroke-width:1.2px,color:#1e293b')

  clusters.forEach((cluster) => {
    const clusterId = mermaidId(cluster.id)
    lines.push(`  class ${clusterId}_root clusterRoot`)
  })

  return lines.join('\n')
}

function linkColor(strength) {
  if (strength === 'HIGH') return '#dc2626'
  if (strength === 'MEDIUM') return '#2563eb'
  return '#94a3b8'
}

function mermaidId(raw) {
  return `m_${String(raw || 'cluster').toLowerCase().replace(/[^a-z0-9_]/g, '_')}`
}

function escapeMermaid(raw) {
  return String(raw || '').replace(/"/g, "'").replace(/\[/g, '(').replace(/\]/g, ')')
}

function formatNumber(value) {
  if (value === null || value === undefined || value === '') return '-'
  return new Intl.NumberFormat('en-US').format(value)
}

function handleError(error, fallback) {
  const serverError = error?.response?.data?.error
  errorMessage.value = serverError ? `${fallback}: ${serverError}` : `${fallback}: ${error.message}`
}
</script>
