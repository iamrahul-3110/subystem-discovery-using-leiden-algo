<template>
  <aside class="tree-panel" style="flex: 0 0 auto">
    <div class="cluster-list">
      <article
        v-for="cluster in sortedSubsystems"
        :key="cluster.id"
        class="cluster-card"
        :class="{ selected: selectedClusterId === cluster.id }"
      >
        <button type="button" class="cluster-main" @click="$emit('toggle-cluster', cluster.id)">
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
</template>

<script setup>
defineProps({
  sortedSubsystems: { type: Array, required: true },
  selectedClusterId: { type: [String, Number], default: null },
  expandedClusters: { type: Object, required: true } // Set
})

defineEmits(['toggle-cluster'])

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

function formatNumber(value) {
  if (value === null || value === undefined || value === '') return '-'
  return new Intl.NumberFormat('en-US').format(value)
}
</script>
