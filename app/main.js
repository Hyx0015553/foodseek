import App from './App'

// #ifndef VUE3
import AiAssistantFloat from '@/components/ai-assistant-float/ai-assistant-float.vue'
import Vue from 'vue'
Vue.config.productionTip = false

Vue.component('AiAssistantFloat', AiAssistantFloat)

import store from '@/store'

// 引入TuniaoUI提供的vuex简写方法
let vuexStore = require('@/store/$tn.mixin.js')
Vue.mixin(vuexStore)

import uView from 'uview-ui'
Vue.use(uView)
uni.$u.setConfig({
    // 修改$u.props对象的属性
    props: {
        // 修改radio组件的size参数的默认值，相当于执行 uni.$u.props.radio.size = 30
        navbar: {
            bgColor: '#fff',
            titleStyle() {
                return {
                    'color':'#333',
                    'font-weight': 'bolder'
                }
            },
            leftIconColor: '#333'
        },
        tabs:{
            lineColor: '#161E35',
            activeStyle() {
                return {
                    'color': '#303133',
                    'fontWeight': 'bold',
                    'transform': 'scale(1.05)'
                }
            },
            inactiveStyle() {
                return {
                    'color': '#606266',
                    'transform': 'scale(1)'
                }
            }
        }
        // 其他组件属性配置
        // ......
    }
})
// 引入全局TuniaoUI
import TuniaoUI from 'tuniao-ui'
Vue.use(TuniaoUI)

App.mpType = 'app'
const app = new Vue({
    ...App,
	store
})


import { ideautil, yewuutil } from '@/common/commontools.js'
uni.itool = ideautil
uni.ytool = yewuutil
// 引入请求封装，将app参数传递到配置中
require('./common/config/request.js')(app)

app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp } from 'vue'
export function createApp() {
  const app = createSSRApp(App)
  return {
    app
  }
}
// #endif