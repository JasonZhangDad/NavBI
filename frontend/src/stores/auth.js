import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('navbi_token') || '')

  function setToken(value) {
    token.value = value
    localStorage.setItem('navbi_token', value)
  }

  function logout() {
    token.value = ''
    localStorage.removeItem('navbi_token')
  }

  return { token, setToken, logout }
})
