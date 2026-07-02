import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const root = dirname(fileURLToPath(import.meta.url))
const dashboard = readFileSync(resolve(root, 'src/views/admin/Dashboard.vue'), 'utf8')
const home = readFileSync(resolve(root, 'src/views/Home.vue'), 'utf8')

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

assert(!home.includes('to="/admin"'), 'Home page must not expose an /admin link')
assert(!home.includes('管理后台'), 'Home page must not show the admin entry label')

assert(dashboard.includes('<el-pagination'), 'Dashboard logs need a pagination control')
assert(dashboard.includes('v-model:current-page="logPage"'), 'Dashboard pagination must bind current page')
assert(dashboard.includes('v-model:page-size="logPageSize"'), 'Dashboard pagination must bind page size')
assert(dashboard.includes(':total="logTotal"'), 'Dashboard pagination must use backend total')
assert(dashboard.includes('page: logPage.value'), 'Dashboard logs request must send the selected page')
assert(dashboard.includes('size: logPageSize.value'), 'Dashboard logs request must send the selected page size')

console.log('dashboard_pagination_ok')
