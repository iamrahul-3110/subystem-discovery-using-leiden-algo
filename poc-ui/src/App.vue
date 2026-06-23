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

    <header class="app-header">
      <div class="app-bar">
        <div class="header-title-container">
          <button
            class="lnb-toggle"
            type="button"
            @click="isLnbCollapsed = !isLnbCollapsed"
            :title="isLnbCollapsed ? 'Show Sidebar' : 'Hide Sidebar'"
          >
            <svg v-if="isLnbCollapsed" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5">
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
          <strong>{{ dataset?.analysisTime || 'Not generated' }}</strong>
        </div>
      </div>
    </header>

    <div class="layout-grid">
      <aside class="side-rail" :class="{ 'side-rail-collapsed': isLnbCollapsed }">
        <div class="brand-mark">GI</div>
        <nav>
          <button class="nav-item active" type="button">Subsystem Discovery</button>
        </nav>

        <section class="config-block">
          <h2>Dataset</h2>
          <label>
            Application
            <select v-model="selectedTemplate">
              <option v-for="app in applications" :key="app.value" :value="app.value">
                {{ app.label }}
              </option>
            </select>
          </label>
          <label>
            Node count
            <select v-model.number="selectedNodeCount">
              <option v-for="size in nodeCounts" :key="size" :value="size">
                {{ formatNumber(size) }} nodes
              </option>
            </select>
          </label>
          <button class="primary-action" type="button" :disabled="loading.dataset" @click="generateDataset">
            <svg v-if="loading.dataset" class="btn-spinner" viewBox="0 0 50 50">
              <circle class="path" cx="25" cy="25" r="20" fill="none" stroke-width="5"></circle>
            </svg>
            {{ loading.dataset ? 'Generating graph...' : 'Generate graph' }}
          </button>
        </section>

        <section class="config-block">
          <h2>Leiden Config</h2>
          <label>
            <div class="label-header">
              Runs
              <span class="tooltip-container ">
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
            <input v-model.number="leiden.runs" type="number" min="1" max="100" />
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
            <input v-model.number="leiden.consensusThreshold" type="number" min="0.1" max="0.95" step="0.05" />
          </label>
          <label>
            <div class="label-header">
              Resolution
              <span class="tooltip-container ">
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
            <input v-model.number="leiden.resolution" type="number" min="0.1" max="5" step="0.1" />
          </label>
          <button class="primary-action blue" type="button" :disabled="!dataset || loading.discovery" @click="runDiscovery">
            <svg v-if="loading.discovery" class="btn-spinner" viewBox="0 0 50 50">
              <circle class="path" cx="25" cy="25" r="20" fill="none" stroke-width="5"></circle>
            </svg>
            {{ loading.discovery ? 'Running Leiden...' : 'Discover subsystems' }}
          </button>
        </section>

        <section class="config-block">
          <h2>AI Summary</h2>
          <label>
            LLM model
            <select v-model="llm.model">
              <option v-for="option in llmModels" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
          <label>
            Summary type
            <select v-model="llm.summaryType">
              <option value="LESS_DETAILED">Executive overview</option>
              <option value="MEDIUM_DETAILED">Architecture summary</option>
              <option value="COMPLETE_DETAILED">Detailed analysis</option>
            </select>
          </label>
          <button class="primary-action dark" type="button" :disabled="!discovery || loading.summary" @click="generateSummary">
            <svg v-if="loading.summary" class="btn-spinner" viewBox="0 0 50 50">
              <circle class="path" cx="25" cy="25" r="20" fill="none" stroke-width="5"></circle>
            </svg>
            {{ loading.summary ? 'Summarizing...' : 'Generate summary' }}
          </button>
        </section>
      </aside>

      <main class="main-panel">
        <section class="command-row">
          <div class="command-group">
            <span>Application</span>
            <strong>{{ currentApplicationLabel }}</strong>
          </div>
          <div class="command-group">
            <span>Dataset size</span>
            <strong>{{ dataset ? formatNumber(dataset.nodeCount) : formatNumber(selectedNodeCount) }}</strong>
          </div>
          <div class="command-group">
            <span>Relations</span>
            <strong>{{ dataset ? formatNumber(dataset.relationCount) : '-' }}</strong>
          </div>
          <div class="command-group">
            <span>Effective runs</span>
            <strong>{{ discovery?.algorithm?.runs || '-' }}</strong>
          </div>
          <div class="command-status" :class="{ busy: isBusy }">
            {{ statusText }}
          </div>
        </section>



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
          <section class="metrics-grid">
            <article>
              <span>Total nodes</span>
              <strong>{{ formatNumber(discovery.summary.totalNodes) }}</strong>
            </article>
            <article>
              <span>Total edges</span>
              <strong>{{ formatNumber(discovery.summary.totalEdges) }}</strong>
            </article>
            <article>
              <span>Subsystems</span>
              <strong>{{ discovery.summary.subsystemCount }}</strong>
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
              <strong>{{ discovery.summary.averageStability }}</strong>
            </article>
          </section>

          <section class="discovery-grid" ref="resizerContainerRef">
            <aside class="tree-panel" :style="{ width: treeWidth + 'px', flex: '0 0 auto' }">
              <div class="panel-heading">
                <div>
                  <p class="eyebrow">Cluster Tree</p>
                  <h2>Subsystems and associated nodes</h2>
                </div>
              </div>

              <div class="cluster-list">
                <article
                  v-for="cluster in sortedSubsystems"
                  :key="cluster.id"
                  class="cluster-card"
                  :class="{ selected: selectedClusterId === cluster.id }"
                >
                  <button type="button" class="cluster-main" @click="toggleCluster(cluster.id)">
                    <span class="toggle-mark">{{ expandedClusters.has(cluster.id) ? '-' : '+' }}</span>
                    <span>
                      <strong>{{ cluster.name }}</strong>
                      <small>{{ cluster.id }} / {{ formatNumber(cluster.nodeCount) }} nodes</small>
                    </span>
                  </button>

                  <div v-if="expandedClusters.has(cluster.id)" class="cluster-detail">
                    <div class="cluster-meta">
                      <span>Stability {{ cluster.stabilityScore }}
                        <span class="tooltip-container tooltip-align-left-down">
                          <i class="tooltip-icon">ⓘ</i>
                          <span class="tooltip-box">
                            <strong>Stability (Cluster Level)</strong><br/><br/>
                            Measures how consistently this subsystem was discovered across multiple Leiden executions.<br/><br/>
                            Range: 0.00 - 1.00<br/><br/>
                            Higher values indicate a stable business domain with strong internal cohesion.<br/><br/>
                            Interpretation:<br/>
                            • &gt; 0.90 = Very stable subsystem<br/>
                            • 0.75 - 0.90 = Stable subsystem<br/>
                            • 0.50 - 0.75 = Moderate confidence<br/>
                            • &lt; 0.50 = Weak or overlapping subsystem
                          </span>
                        </span>
                      </span>
                      <span>Connectivity {{ cluster.internalConnectivity }}
                        <span class="tooltip-container tooltip-align-right-down">
                          <i class="tooltip-icon">ⓘ</i>
                          <span class="tooltip-box">
                            <strong>Connectivity (Cluster Level)</strong><br/><br/>
                            Measures the density of internal dependencies within this subsystem.<br/><br/>
                            Range: 0.00 - 1.00<br/><br/>
                            Higher values indicate stronger relationships between classes, methods, and components inside the subsystem.<br/><br/>
                            Interpretation:<br/>
                            • &gt; 0.70 = Highly cohesive subsystem<br/>
                            • 0.40 - 0.70 = Moderately connected<br/>
                            • &lt; 0.40 = Loosely connected<br/><br/>
                            A high connectivity score typically indicates a well-defined business capability.
                          </span>
                        </span>
                      </span>
                    </div>
                    <p v-if="cluster.topPackages?.length">
                      Top packages: 
                      <span 
                        v-for="(pkg, idx) in cluster.topPackages.slice(0, 3)" 
                        :key="idx" 
                        :title="pkg"
                      >
                        {{ truncate(pkg, 18) }}{{ idx < Math.min(2, cluster.topPackages.length - 1) ? ', ' : '' }}
                      </span>
                    </p>
                    <ul>
                      <li v-for="item in clusterItems(cluster)" :key="item.id">
                        <span :title="item.name">{{ truncate(item.name) }}</span>
                        <small>{{ item.type }}</small>
                      </li>
                    </ul>
                  </div>
                </article>
              </div>

            </aside>

            <div class="panel-resizer" :class="{ resizing: isResizing }" @mousedown="onResizerMouseDown">
              <div class="resizer-line"></div>
            </div>

            <section class="diagram-panel" :style="{ flex: '1 1 0%', minWidth: '0' }">
              <div
                class="mermaid-stage"
                ref="stageRef"
                @mousedown="onMouseDown"
                @mousemove="onMouseMove"
                @mouseup="onMouseUp"
                @mouseleave="onMouseLeave"
                @wheel="onWheel"
              >
                <div v-if="mermaidSvg && !loading.discovery" class="mermaid-controls" @mousedown.stop>
                  <button type="button" @click="zoomIn" title="Zoom In">＋</button>
                  <button type="button" @click="zoomOut" title="Zoom Out">－</button>
                  <button type="button" @click="resetZoom" title="Reset view">Reset</button>
                  <span class="zoom-level">{{ Math.round(zoom * 100) }}%</span>
                </div>

                <div v-if="loading.discovery" class="diagram-skeleton">
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
          </section>

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
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'
import axios from 'axios'
import mermaid from 'mermaid'

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
// File: `poc-ui/src/App.vue`
// Replace the existing llmModels definition with this:
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

const stageRef = ref(null)
const zoom = ref(1)
const pan = reactive({ x: 0, y: 0 })
const isDragging = ref(false)
const dragStart = reactive({ x: 0, y: 0 })

const isLnbCollapsed = ref(false)
const treeWidth = ref(380)
const isResizing = ref(false)
const resizerContainerRef = ref(null)

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

const formattedSummary = computed(() => {
  return summaryText.value
    .split(/\n{2,}/)
    .map((value) => value.replace(/\s+/g, ' ').trim())
    .filter(Boolean)
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
  resetZoom()

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
  resetZoom()

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

function representativeNodes(cluster) {
  const nodes = cluster.centralNodes || []
  return nodes.slice(0, 12)
}

function clusterItems(cluster) {
  const items = []
  
  if (cluster.apiEndpoints) {
    cluster.apiEndpoints.forEach(api => {
      items.push({
        id: `api-${api.id}`,
        name: `${api.method} ${api.path}`,
        type: 'API'
      })
    })
  }
  
  if (cluster.centralNodes) {
    cluster.centralNodes.forEach(node => {
      items.push({
        id: `node-${node.id}`,
        name: node.name,
        type: node.type
      })
    })
  }
  
  return items.slice(0, 15)
}

function truncate(str, len = 24) {
  if (!str) return ''
  if (str.length <= len) return str
  return str.substring(0, len - 3) + '...'
}

async function renderMermaid() {
  try {
    const graph = buildMermaidGraph()
    const renderId = `gi-mermaid-${Date.now()}-${Math.round(Math.random() * 10000)}`
    const { svg } = await mermaid.render(renderId, graph)
    mermaidSvg.value = svg
    await nextTick()
    attachNodeClickListeners()
  } catch (error) {
    mermaidSvg.value = ''
    errorMessage.value = `Mermaid rendering failed: ${error.message}`
  }
}

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
        const cluster = sortedSubsystems.value.find(c => mermaidId(c.id) === clusterMermaidId)
        if (cluster) {
          toggleCluster(cluster.id)
        }
      })
    }
  })
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
      const otherClasses = classes.filter(c => !controllers.includes(c) && !services.includes(c) && !repos.includes(c))

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
