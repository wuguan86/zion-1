import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import naive from 'naive-ui'
import App from './App.vue'
import router from './router'
import './styles/index.scss'
import { fetchCryptoConfig } from './utils/request'
import { useSiteStore } from './stores/site'

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)
app.use(naive)

// 预加载加密配置
fetchCryptoConfig()

// 预加载站点配置
const siteStore = useSiteStore()
siteStore.loadConfig().then(() => {
  // 根据配置动态启用前端禁止调试
  if (siteStore.disableDevtool) {
    import('disable-devtool').then((DisableDevtool) => {
      DisableDevtool.default()
    })
  }
})

app.mount('#app')
