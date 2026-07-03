import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('navbi_token') || '')
  const role = ref(localStorage.getItem('navbi_role') || '')
  const username = ref(localStorage.getItem('navbi_username') || '')

  function setAuth(newToken, newRole, newUsername) {
    token.value = newToken
    role.value = newRole
    username.value = newUsername
    localStorage.setItem('navbi_token', newToken)
    localStorage.setItem('navbi_role', newRole)
    localStorage.setItem('navbi_username', newUsername)
  }

  function logout() {
    token.value = ''
    role.value = ''
    username.value = ''
    localStorage.removeItem('navbi_token')
    localStorage.removeItem('navbi_role')
    localStorage.removeItem('navbi_username')
  }

  return { token, role, username, setAuth, logout }
})
