import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('navbi_token') || '')
  const role = ref(localStorage.getItem('navbi_role') || '')

  function setAuth(newToken, newRole) {
    token.value = newToken
    role.value = newRole
    localStorage.setItem('navbi_token', newToken)
    localStorage.setItem('navbi_role', newRole)
  }

  function logout() {
    token.value = ''
    role.value = ''
    localStorage.removeItem('navbi_token')
    localStorage.removeItem('navbi_role')
  }

  return { token, role, setAuth, logout }
})
