import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: () => import('../views/Home.vue') },
    { path: '/login', component: () => import('../views/Login.vue') },
    { path: '/register', component: () => import('../views/Register.vue') },
    { path: '/reset-password', component: () => import('../views/ResetPassword.vue') },
    { path: '/me', component: () => import('../views/Me.vue'), meta: { requiresAuth: true } },
    {
      path: '/admin',
      component: () => import('../views/admin/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        { path: 'dashboard', component: () => import('../views/admin/Dashboard.vue') },
        { path: 'nav', component: () => import('../views/admin/NavManage.vue') },
        { path: 'categories', component: () => import('../views/admin/CategoryManage.vue') },
        { path: 'users', component: () => import('../views/admin/UserManage.vue') },
        { path: 'api-logs', component: () => import('../views/admin/ApiLogs.vue') }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.token) {
    return '/login'
  }
  if (to.meta.requiresAdmin && auth.role !== 'ADMIN') {
    return '/me'
  }
})

export default router
