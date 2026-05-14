import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import naive, { createDiscreteApi } from 'naive-ui'
import App from './App.vue'
import router from './router'
import './styles/index.scss'

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)
app.use(pinia)

app.use(router)
app.use(naive)

const { message, dialog, loadingBar } = createDiscreteApi(['message', 'dialog', 'loadingBar'])
window.$message = message
window.$dialog = dialog
window.$loadingBar = loadingBar

app.mount('#app')
