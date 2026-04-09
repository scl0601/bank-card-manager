import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const username = ref<string>(localStorage.getItem('username') || '')
  const nickname = ref<string>(localStorage.getItem('nickname') || '')
  const role = ref<string>(localStorage.getItem('role') || '')

  async function login(loginForm: { username: string; password: string }) {
    const res = await loginApi(loginForm)
    token.value = res.data.token
    username.value = res.data.username
    nickname.value = res.data.nickname
    role.value = res.data.role
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('username', res.data.username)
    localStorage.setItem('nickname', res.data.nickname)
    localStorage.setItem('role', res.data.role)
  }

  function logout() {
    token.value = ''
    username.value = ''
    nickname.value = ''
    role.value = ''
    localStorage.clear()
  }

  return { token, username, nickname, role, login, logout }
})
