import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/home/index.vue'),
    meta: { requiresAuth: true, title: '主页' }
  },
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || 'Zion 用户端'
  const userStore = useUserStore()

  // Handle OAuth callback redirect: store token from URL params
  if (to.query.token) {
    const token = Array.isArray(to.query.token) ? to.query.token[0] : to.query.token
    userStore.token = token || ''
    userStore.user = {
      id: Number(to.query.userId) || 0,
      username: '',
      nickname: (to.query.nickname as string) || '',
      avatar: (to.query.avatar as string) || '',
      email: '',
      phone: '',
      gender: 0,
      status: 1
    }

    const cleanedQuery = { ...to.query }
    delete cleanedQuery.token
    delete cleanedQuery.userId
    delete cleanedQuery.nickname
    delete cleanedQuery.avatar
    delete cleanedQuery.error
    next({ ...to, query: cleanedQuery, replace: true })
    return
  }

  // Handle error from OAuth callback
  if (to.query.error) {
    const cleanedQuery = { ...to.query }
    delete cleanedQuery.error
    next({ path: '/login', query: { ...cleanedQuery, wechatError: to.query.error as string }, replace: true })
    return
  }

  if (to.meta.requiresAuth === false) {
    next()
    return
  }

  if (!userStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (!userStore.user) {
    userStore.getInfo()
      .then(() => {
        next()
      })
      .catch(() => {
        userStore.logout()
        next('/login')
      })
    return
  }
  next()
})

export default router
