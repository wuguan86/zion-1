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

  if (to.meta.requiresAuth === false) {
    next()
    return
  }

  const userStore = useUserStore()

  if (!userStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (!userStore.user) {
    userStore.getInfo()
      .then(() => next())
      .catch(() => {
        userStore.logout()
        next('/login')
      })
    return
  }

  next()
})

export default router
