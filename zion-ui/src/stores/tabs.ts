import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'

export interface TabItem {
  /** 路由路径 */
  path: string
  /** 页签标题 */
  title: string
  /** 路由 name */
  name?: string
  /** 是否固定（不可关闭） */
  affix?: boolean
}

export const useTabsStore = defineStore('tabs', () => {
  // 首页固定页签
  const HOME_TAB: TabItem = { path: '/dashboard', title: '首页', name: 'Dashboard', affix: true }

  // 已打开的页签列表
  const tabs = ref<TabItem[]>([{ ...HOME_TAB }])

  // 当前激活的页签路径
  const activeTab = ref<string>(HOME_TAB.path)

  /**
   * 添加页签
   */
  function addTab(route: RouteLocationNormalized) {
    // 忽略登录、404 等非布局页面
    if (route.meta?.requiresAuth === false) return
    // 忽略根路径和重定向路径
    if (route.path === '/' || route.path.startsWith('/redirect')) return

    const title = (route.meta?.title as string) || '未命名'
    const exists = tabs.value.find(t => t.path === route.path)
    if (!exists) {
      tabs.value.push({
        path: route.path,
        title,
        name: route.name as string
      })
    }
    activeTab.value = route.path
  }

  /**
   * 关闭页签，返回下一个应该激活的路径
   */
  function closeTab(path: string): string | null {
    const tab = tabs.value.find(t => t.path === path)
    if (tab?.affix) return null // 固定页签不能关闭

    const index = tabs.value.findIndex(t => t.path === path)
    if (index === -1) return null

    tabs.value.splice(index, 1)

    // 如果关闭的是当前页签，激活相邻的
    if (activeTab.value === path) {
      const next = tabs.value[index] || tabs.value[index - 1]
      activeTab.value = next?.path || HOME_TAB.path
      return activeTab.value
    }
    return null
  }

  /**
   * 关闭其他页签
   */
  function closeOtherTabs(path: string) {
    tabs.value = tabs.value.filter(t => t.affix || t.path === path)
    activeTab.value = path
  }

  /**
   * 关闭左侧页签
   */
  function closeLeftTabs(path: string) {
    const index = tabs.value.findIndex(t => t.path === path)
    if (index <= 0) return
    tabs.value = tabs.value.filter((t, i) => t.affix || i >= index)
  }

  /**
   * 关闭右侧页签
   */
  function closeRightTabs(path: string) {
    const index = tabs.value.findIndex(t => t.path === path)
    if (index === -1) return
    tabs.value = tabs.value.filter((t, i) => t.affix || i <= index)
  }

  /**
   * 关闭所有页签（保留固定页签）
   */
  function closeAllTabs(): string {
    tabs.value = tabs.value.filter(t => t.affix)
    activeTab.value = HOME_TAB.path
    return HOME_TAB.path
  }

  return {
    tabs,
    activeTab,
    addTab,
    closeTab,
    closeOtherTabs,
    closeLeftTabs,
    closeRightTabs,
    closeAllTabs
  }
})
