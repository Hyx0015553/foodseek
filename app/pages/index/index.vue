<template>
  <view class="page-container">
    <!-- 顶部：菜单 + 搜索栏 + 更多（预留状态栏高度，搜索栏在黑色区域下） -->
    <view class="header-bar" :style="{ paddingTop: (statusBarHeight + 12) + 'px' }">
      <view class="header-left" @click="toNotice">
        <u-icon name="list" size="22" color="#333"></u-icon>
      </view>
      <view class="header-search" @click="toSearch">
        <view class="header-search-inner">
          <text class="header-search-placeholder">请搜索你想要的内容</text>
        </view>
      </view>
      <!--<view class="header-right">
        <u-icon name="more-dot-fill" size="22" color="#333"></u-icon>
      </view>-->
    </view>

    <!-- 城市选择 -->
    <!--<view class="city-row">
      <text class="city-text">成都市</text>
      <u-icon name="arrow-down" size="12" color="#666"></u-icon>
    </view>-->

        <!-- 顶部导航栏：个性化推荐、美食榜单、动态推荐 -->
    <view class="top-tabs">
      <u-tabs
          :list="tablist"
          keyName="title"
          :scrollable="false"
          @change="onTabChange"
          lineColor="transparent"
          :lineWidth="0"
          :lineHeight="0"
          :activeStyle="{ color: '#ff6037', fontWeight: 'bold' }"
          :inactiveStyle="{ color: '#606266' }"
          itemStyle="justify-content: center; text-align: center;"
      ></u-tabs>
      <view class="top-tabs-line"></view>
    </view>

    <scroll-view scroll-y class="main-content" :enable-flex="true">
      <!-- AI推荐：为你推荐·今日小众精选 -->
      <view v-show="currentTab === 0" class="content-section">
        <view class="section-header">
          <view class="section-header-row">
            <text class="section-title">为你推荐·小众精选</text>
            <view class="section-filter-entry" @click="recFilterShow = true">
              <u-icon name="hourglass" size="20" color="#ff6037"></u-icon>
              <text class="section-filter-text">推荐偏好</text>
            </view>
          </view>
          <!-- 以下区块仅 recDebugUi=true 时展示，面向开发/验收；正式用户不展示 -->
          <view v-if="recDebugUi && recPrefs.ai && recAiBanner" class="rec-ai-status-bar">
            <text class="rec-ai-status-k">千帆大模型</text>
            <text
              class="rec-ai-status-v"
              :class="recAiBanner.ok ? 'rec-ai-status-v--ok' : 'rec-ai-status-v--warn'"
            >{{ recAiBanner.text }}</text>
          </view>
          <text v-if="recAiHint" class="section-ai-hint">{{ recAiHint }}</text>
          <text
            v-if="recDebugUi"
            class="section-plain-summary"
            :class="{ 'section-plain-summary--sub': recAiHint }"
          >{{ recPlainSummary }}</text>
          <view v-if="recPrefs.tag && recTagLabels.length" class="rec-user-tags">
            <text class="rec-user-tags-label">参考你的口味偏好</text>
            <view class="rec-tag-chip-row">
              <view v-for="(t, ti) in recTagLabels" :key="ti" class="rec-tag-chip">
                <text>{{ t }}</text>
              </view>
            </view>
          </view>
          <view v-if="recDebugUi && recMetricDisplay.length" class="rec-metric-panel">
            <text class="rec-metric-panel-title">首条推荐 · 各指标得分（仅显示本次已开启的因子）</text>
            <view v-for="(m, mi) in recMetricDisplay" :key="mi" class="rec-metric-line">
              <view class="rec-metric-head">
                <text class="rec-metric-name">{{ m.label }}</text>
                <text class="rec-metric-val">{{ m.val }}</text>
              </view>
              <view class="rec-metric-bar-bg">
                <view class="rec-metric-bar-fg" :style="{ width: m.pct + '%' }"></view>
              </view>
            </view>
          </view>
          <text v-if="recDebugUi && recTechDetail && !showRecTech" class="section-tech-toggle" @click="showRecTech = true">查看原始算分串</text>
          <text v-if="recDebugUi && recTechDetail && showRecTech" class="section-score-note">{{ recTechDetail }}</text>
        </view>
        <view class="list-wrapper">
          <imglist
              :showSearch="false"
              imgName="img"
              sName="note"
              imgSize="2"
              tName="pf"
              tLabel="评分:"
              tColor="#ff9900"
              titleName="sname"
              :dataList="fobjList"
              @clickItem="toShopDetail"
          ></imglist>
          <view class="load-more" v-if="fobjList.length > 0"><text>已经到底啦 ~</text></view>
          <view class="empty-box" v-if="fobjList.length === 0">
            <text class="empty-icon">🍱</text>
            <text class="empty-text">暂无相关店铺</text>
          </view>
        </view>
      </view>

      <!-- 美食榜 -->
      <view v-show="currentTab === 1" class="content-section">
        <view class="section-header">
          <text class="section-title">美食榜</text>
        </view>
        <view class="list-wrapper">
          <imglist
              :showSearch="false"
              imgName="img"
              sName="note"
              imgSize="2"
              tName="pf"
              tLabel="评分:"
              tColor="#ff9900"
              titleName="gname"
              :dataList="goodList"
              @clickItem="toGoodDetail"
          ></imglist>
          <view class="load-more" v-if="goodList.length > 0"><text>已经到底啦 ~</text></view>
          <view class="empty-box" v-if="goodList.length === 0">
            <text class="empty-icon">🍜</text>
            <text class="empty-text">暂无美食</text>
          </view>
        </view>
      </view>

      <!-- 文章推荐 -->
      <view v-show="currentTab === 2" class="content-section">
        <view class="section-header">
          <text class="section-title">探店动态推荐</text>
        </view>
        <view class="list-wrapper">
          <imglist
              :showSearch="false"
              imgName="img"
              sName="note"
              imgSize="2"
              tName="username"
              tLabel="作者:"
              tColor="#ff943c"
              titleName="title"
              :dataList="blogList"
              @clickItem="toBlogDetail"
          ></imglist>
          <view class="load-more" v-if="blogList.length > 0"><text>已经到底啦 ~</text></view>
          <view class="empty-box" v-if="blogList.length === 0">
            <text class="empty-icon">📝</text>
            <text class="empty-text">暂无文章</text>
          </view>
        </view>
      </view>

      <view class="safe-area-bottom"></view>
    </scroll-view>

    <u-popup :show="recFilterShow" mode="bottom" round="16" @close="recFilterShow = false">
      <view class="rec-filter-panel">
        <view class="rec-filter-title">推荐算法 · 自定义指标</view>
        <text class="rec-filter-tip">关掉的项不参与打分。点「恢复默认」一键全开。开「附近」后请点「获取当前位置」。下方列表可上下滑动。</text>

        <scroll-view scroll-y class="rec-filter-scroll" :show-scrollbar="true">
          <view v-for="row in recFactorRows" :key="row._id" class="rec-filter-line">
            <text v-if="row.kind === 'head'" class="rec-filter-group-title">{{ row.title }}</text>
            <view v-else class="rec-filter-row">
              <view class="rec-filter-label">
                <text class="rec-filter-name">{{ row.name }}</text>
                <text class="rec-filter-desc">{{ row.desc }}</text>
              </view>
              <u-switch
                :value="getRecPref(row.key)"
                size="20"
                activeColor="#ff6037"
                @input="onRecPrefInput(row.key, $event)"
              ></u-switch>
            </view>
          </view>

          <view class="rec-near-block" v-if="recPrefs.nearby">
            <view class="rec-near-row">
              <text class="rec-near-label">距离衰减尺度 nearKm（km）</text>
              <u-number-box v-model="recNearKm" :min="3" :max="80" :step="3" integer></u-number-box>
            </view>
            <u-button size="small" type="primary" text="获取当前位置" customStyle="margin-top:16rpx" @click="getRecLocation"></u-button>
            <text class="rec-loc-status">{{ recLocStatus }}</text>
          </view>
        </scroll-view>

        <view class="rec-filter-actions">
          <u-button text="恢复默认" plain color="#909399" @click="resetRecPrefs"></u-button>
          <u-button text="应用并刷新" type="primary" color="#ff6037" @click="applyRecPrefs"></u-button>
        </view>
      </view>
    </u-popup>
    <ai-assistant-float />
  </view>
</template>

<script>
import { listj, findj, fileUrl, recommendShopsj } from '@/common/config/api.js'
import { ideautil, yewuutil } from '@/common/commontools.js'
import { mapState, mapActions } from 'vuex'

export default {
  data() {
    return {
      statusBarHeight: 0,
      searchstr: '',
      fileUrl: fileUrl,
      REC_PREFS_KEY: 'shop_rec_prefs_v1',
      currentTab: 0,
      tablist: [
        { id: 0, title: '个性化推荐' },
        { id: 1, title: '美食榜单' },
        { id: 2, title: '动态推荐' }
      ],
      fobjList: [],
      goodList: [],
      blogList: [],
      recAiHint: '',
      recPlainSummary: '',
      recTechDetail: '',
      recTagLabels: [],
      recMetricPreview: null,
      recAiMeta: { aiKeyConfigured: false, aiRerankApplied: false, aiInvoked: false },
      /** 为 true 时展示：千帆状态条、算法说明长文、分项得分、原始算分串（给用户请保持 false） */
      recDebugUi: false,
      showRecTech: false,
      recFilterShow: false,
      recNearKm: 12,
      recLat: null,
      recLng: null,
      recLocStatus: '',
      recPrefs: {
        collab: true,
        order: true,
        tag: true,
        pf: true,
        blog: true,
        plan: true,
        sales: true,
        global: true,
        fav: true,
        nearby: true,
        div: true,
        ai: true
      },
      recFactorGroups: [
        {
          title: '位置',
          items: [{ key: 'nearby', name: '附近优先', desc: '越近分越高，需定位授权' }]
        },
        {
          title: '兴趣与口碑',
          items: [
            { key: 'tag', name: '口味标签', desc: '与「我的标签」品类一致优先' },
            { key: 'pf', name: '店铺评分', desc: '高分店铺加权' },
            { key: 'fav', name: '我的收藏', desc: '已收藏的店略加分' }
          ]
        },
        {
          title: '你的行为',
          items: [
            { key: 'collab', name: '相似用户·收藏', desc: '和口味相近的人收藏共振' },
            { key: 'order', name: '我的订单', desc: '常点/复购店优先' },
            { key: 'plan', name: '探店计划', desc: '完成过计划的店' }
          ]
        },
        {
          title: '热度与销量',
          items: [
            { key: 'blog', name: '探店动态', desc: '笔记赞/浏览/收藏热度' },
            { key: 'sales', name: '门店销量', desc: '店内商品累计销量' },
            { key: 'global', name: '全站热度', desc: '全站订单量（冷启动）' }
          ]
        },
        {
          title: '列表与智能',
          items: [
            { key: 'div', name: '品类打散', desc: '减少连续同类店' },
            { key: 'ai', name: 'AI 重排与导语', desc: '千帆微调顺序 + 顶部一句话' }
          ]
        }
      ],
      factorLabelMap: {
        nearby: '距离',
        tag: '口味标签',
        pf: '评分',
        fav: '收藏',
        collab: '相似用户',
        order: '订单',
        blog: '探店动态',
        plan: '探店计划',
        sales: '销量',
        global: '全站热度'
      }
    }
  },
  created() {
    const sysInfo = uni.getSystemInfoSync();
    this.statusBarHeight = sysInfo.statusBarHeight || 0;
    this.loadRecPrefs();
  },
  onLoad() {
    this.initData();
  },
  onPullDownRefresh() {
    Promise.all([
      this.refreshFobj(),
      new Promise((resolve) => {
        this.loadGoodList()
        this.loadBlogList()
        resolve()
      })
    ]).finally(() => uni.stopPullDownRefresh())
  },
  methods: {
    ...mapActions(['updateUserInfo', 'logout']),
    initData() {
      // 首页个性化推荐：复用 shoptuijian.vue 的推荐逻辑
      this.refreshFobj();
      this.loadGoodList();
      this.loadBlogList();
    },
    onTabChange(e) {
      this.currentTab = e.index;
    },
    loadRecPrefs() {
      try {
        const raw = uni.getStorageSync(this.REC_PREFS_KEY)
        if (raw) {
          const o = typeof raw === 'string' ? JSON.parse(raw) : raw
          if (o && o.prefs) Object.assign(this.recPrefs, o.prefs)
          if (o && o.recNearKm != null) this.recNearKm = Math.min(80, Math.max(3, parseInt(o.recNearKm, 10) || 12))
          if (o && o.recLat != null && o.recLng != null) {
            this.recLat = o.recLat
            this.recLng = o.recLng
            this.recLocStatus = '已保存上次定位'
          }
        }
      } catch (e) {}
    },
    saveRecPrefs() {
      try {
        uni.setStorageSync(this.REC_PREFS_KEY, JSON.stringify({
          prefs: { ...this.recPrefs },
          recNearKm: this.recNearKm,
          recLat: this.recLat,
          recLng: this.recLng
        }))
      } catch (e) {}
    },
    resetRecPrefs() {
      Object.keys(this.recPrefs).forEach((k) => { this.recPrefs[k] = true })
      this.recNearKm = 12
      this.recLat = null
      this.recLng = null
      this.recLocStatus = '已恢复默认'
    },
    buildRecommendParams() {
      const uid = this.userInfo && this.userInfo.id != null ? String(this.userInfo.id) : ''
      const p = {
        limit: 12,
        useAi: this.recPrefs.ai ? 1 : 0,
        collab: this.recPrefs.collab ? 1 : 0,
        order: this.recPrefs.order ? 1 : 0,
        tag: this.recPrefs.tag ? 1 : 0,
        pf: this.recPrefs.pf ? 1 : 0,
        blog: this.recPrefs.blog ? 1 : 0,
        plan: this.recPrefs.plan ? 1 : 0,
        sales: this.recPrefs.sales ? 1 : 0,
        global: this.recPrefs.global ? 1 : 0,
        fav: this.recPrefs.fav ? 1 : 0,
        nearby: this.recPrefs.nearby ? 1 : 0,
        div: this.recPrefs.div ? 1 : 0,
        ai: this.recPrefs.ai ? 1 : 0
      }
      if (uid) p.uid = uid
      if (this.recPrefs.nearby && this.recLat != null && this.recLng != null) {
        p.lat = this.recLat
        p.lng = this.recLng
        p.nearKm = this.recNearKm
      }
      return p
    },
    getRecLocation() {
      this.recLocStatus = '定位中…'
      uni.getLocation({
        type: 'gcj02',
        success: (res) => {
          this.recLat = res.latitude
          this.recLng = res.longitude
          this.recLocStatus = '已获取当前位置'
        },
        fail: () => {
          this.recLocStatus = '定位失败，请在系统设置中开启位置权限'
        }
      })
    },
    applyRecPrefs() {
      this.saveRecPrefs()
      this.recFilterShow = false
      this.refreshFobj()
    },
    /** 顶部人话摘要：对应本次开关，避免直接展示技术分项 */
    buildRecPlainSummary() {
      const keys = ['nearby', 'tag', 'pf', 'fav', 'collab', 'order', 'blog', 'plan', 'sales', 'global']
      const active = keys.filter((k) => this.recPrefs[k])
      const labels = active.map((k) => this.factorLabelMap[k] || k)
      let s = ''
      if (labels.length === 0) {
        s = '当前未开启任何打分因子，排序主要按后台默认顺序。'
      } else {
        s = '本次排序参考：' + labels.join('、') + '。'
      }
      if (this.recPrefs.div) {
        s += ' 已开启品类穿插。'
      }
      if (this.recPrefs.nearby && (this.recLat == null || this.recLng == null)) {
        s += ' 「附近」已开但未定位，距离暂未计入。'
      }
      if (this.recPrefs.ai) {
        s += ' 已开启千帆大模型重排与导语（服务端需配置密钥）。'
      }
      if (this.recPrefs.tag && this.recTagLabels && this.recTagLabels.length) {
        s += ' 当前档案口味：' + this.recTagLabels.join('、') + '。'
      }
      return s
    },
    // 每次请求均带 buildRecommendParams()：与本地已保存的开关、定位一致
    refreshFobj() {
      this.recAiHint = ''
      this.recPlainSummary = this.buildRecPlainSummary()
      this.recTechDetail = ''
      this.recTagLabels = []
      this.recMetricPreview = null
      this.recAiMeta = { aiKeyConfigured: false, aiRerankApplied: false, aiInvoked: false }
      this.showRecTech = false
      const params = this.buildRecommendParams()
      return recommendShopsj({ params })
        .then((res) => {
          const body = res && res.list != null ? res : { list: res || [], aiHint: '' }
          const list = body.list || []
          this.fobjList = list
          this.recAiHint = (body.aiHint || '').trim()
          this.recTagLabels = Array.isArray(body.tagLabels) ? body.tagLabels : []
          this.recMetricPreview = body.metricPreview != null ? body.metricPreview : (list[0] && list[0].recMetrics) || null
          this.recAiMeta = {
            aiKeyConfigured: !!body.aiKeyConfigured,
            aiRerankApplied: !!body.aiRerankApplied,
            aiInvoked: !!body.aiInvoked
          }
          if (list.length && list[0]._rec_parts != null) {
            const sc = list[0]._rec_score != null ? list[0]._rec_score : ''
            this.recTechDetail = '首条综合分 ' + sc + ' · ' + list[0]._rec_parts
          }
          this.recPlainSummary = this.buildRecPlainSummary()
        })
        .catch(() => {
          this.fobjList = []
          this.recTagLabels = []
          this.recMetricPreview = null
          this.recAiMeta = { aiKeyConfigured: false, aiRerankApplied: false, aiInvoked: false }
        })
    },
    /** 与商家端美食排行一致：「饭类主食」不出现在首页美食榜单 */
    isStapleRiceCategoryGood(g) {
      const c = String((g && g.ctype) || '').trim()
      const t = String((g && g.type) || '').trim()
      return c === '饭类主食' || t === '饭类主食'
    },
    loadGoodList() {
      listj({ params: { table: 'good' } }).then((res) => {
        const rows = (res || []).filter((g) => g && !this.isStapleRiceCategoryGood(g))
        this.goodList = rows.sort((o1, o2) => (o2.pf * 1) - (o1.pf * 1))
      })
    },
    loadBlogList() {
      listj({ params: { table: 'blog' } }).then(res => {
        const rows = res || []
        this.blogList = rows.slice().sort((a, b) => {
          const sa = (parseInt(a.zan, 10) || 0) + (parseInt(a.vcount, 10) || 0) + (parseInt(a.favcount, 10) || 0)
          const sb = (parseInt(b.zan, 10) || 0) + (parseInt(b.vcount, 10) || 0) + (parseInt(b.favcount, 10) || 0)
          return sb - sa
        })
      });
    },
    toShopDetail(id) {
      uni.navigateTo({ url: '/pages/shops/shopdetail?pid=' + id });
    },
    toGoodDetail(id) {
      uni.navigateTo({ 
              url: `/pages/good/gooddetail?gid=${id}` 
            });
    },
    toBlogDetail(id) {
      uni.navigateTo({ url: '/pages/blog/blogdetail?id=' + id });
    },
    toNotice() {
      uni.navigateTo({ url: "/pages/good/goodfenlei" });
    },
    toSearch() {
      uni.navigateTo({ url: '/pages/index/search' });
    },
    /** 小程序端不支持 recPrefs[item.key] 的 v-model，改为 value + input + $set */
    getRecPref(key) {
      return !!this.recPrefs[key]
    },
    onRecPrefInput(key, val) {
      this.$set(this.recPrefs, key, val === true || val === 1)
    }
  },
  watch: {
    searchstr(v) {
      uni.$u.debounce(() => this.refreshFobj({ id: 0 }), 500);
    }
  },
  computed: {
    ...mapState(['userInfo']),
    /** 扁平化分组，避免嵌套 v-for + 动态 v-model 在微信端不渲染 */
    recFactorRows() {
      const rows = []
      let n = 0
      for (const g of this.recFactorGroups) {
        rows.push({ _id: 'h' + n++, kind: 'head', title: g.title })
        for (const it of g.items) {
          rows.push({
            _id: 'r' + it.key,
            kind: 'row',
            key: it.key,
            name: it.name,
            desc: it.desc
          })
        }
      }
      return rows
    },
    recAiBanner() {
      if (!this.recPrefs.ai) return null
      if (!this.recAiMeta.aiKeyConfigured) {
        return { ok: false, text: '未配置密钥 · 仅算法排序' }
      }
      const bits = []
      if (this.recAiMeta.aiRerankApplied) bits.push('已重排候选')
      if (this.recAiHint && String(this.recAiHint).trim()) bits.push('已生成导语')
      if (bits.length) return { ok: true, text: bits.join(' · ') }
      if (this.recAiMeta.aiInvoked) {
        return { ok: false, text: '模型未返回有效结果 · 已用算法排序' }
      }
      return { ok: true, text: '就绪' }
    },
    /** 与后端 recMetrics 键一致；max 用于进度条视觉尺度（非理论满分） */
    recMetricDisplay() {
      const mp = this.recMetricPreview
      if (!mp || typeof mp !== 'object') return []
      const order = ['综合', '协同', '订单', '标签', '评分', '动态', '计划', '销量', '热度', '收藏', '附近']
      const keyMap = {
        综合: { pref: null, max: 120 },
        协同: { pref: 'collab', max: 50 },
        订单: { pref: 'order', max: 45 },
        标签: { pref: 'tag', max: 32 },
        评分: { pref: 'pf', max: 10 },
        动态: { pref: 'blog', max: 8 },
        计划: { pref: 'plan', max: 15 },
        销量: { pref: 'sales', max: 25 },
        热度: { pref: 'global', max: 12 },
        收藏: { pref: 'fav', max: 7 },
        附近: { pref: 'nearby', max: 27 }
      }
      const arr = []
      for (const label of order) {
        if (mp[label] === undefined || mp[label] === null) continue
        const meta = keyMap[label]
        if (!meta) continue
        if (meta.pref && !this.recPrefs[meta.pref]) continue
        const val = Number(mp[label])
        if (Number.isNaN(val)) continue
        const pct = Math.min(100, Math.round((Math.max(0, val) / meta.max) * 100))
        arr.push({ label, val, pct })
      }
      return arr
    }
  }
}
</script>

<style lang="scss" scoped>
$page-bg: #f8f9fa;

.page-container {
  background-color: $page-bg;
  min-height: 100vh;
  box-sizing: border-box;
}

/* 顶部栏：菜单 + 搜索 + 更多（自适应状态栏，搜索栏在黑色区域下） */
.header-bar {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  padding-top: 12px; /* 默认，会被 :style 覆盖为 statusBarHeight + 12 */
  background-color: #fff;
  box-sizing: border-box;

  .header-left, .header-right {
    width: 72rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .header-search {
    flex: 1;
    padding: 0 16rpx;

    .header-search-inner {
      height: 64rpx;
      background-color: #f2f3f5;
      border-radius: 32rpx;
      display: flex;
      align-items: center;
      padding: 0 24rpx;
    }

    .header-search-placeholder {
      font-size: 28rpx;
      color: #999;
    }
  }
}

/* 城市选择 */
.city-row {
  display: flex;
  align-items: center;
  padding: 12rpx 32rpx 16rpx;
  background-color: #fff;

  .city-text {
    font-size: 28rpx;
    color: #333;
    margin-right: 8rpx;
  }
}

/* 顶部导航栏：居中且均匀分布 */
.top-tabs {
  background-color: #fff;
  padding: 0 20rpx 10rpx;
  position: relative;

  ::v-deep .u-tabs__wrapper__scroll-view {
    width: 100%;
  }

  ::v-deep .u-tabs__wrapper__nav {
    display: flex;
    justify-content: space-around;
    width: 100%;
  }

  ::v-deep .u-tabs__wrapper__nav__item {
    flex: 1;
    text-align: center;
    justify-content: center;
    display: flex;
  }
}

/* 顶部导航栏底部分隔线 */
.top-tabs-line {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 1rpx;
  background-color: #e5e5e5;
}

.main-content {
  height: calc(100vh - 260rpx);
  box-sizing: border-box;
}

/* 底部安全区（适配刘海屏等） */
.safe-area-bottom {
  height: calc(20rpx + env(safe-area-inset-bottom));
  min-height: env(safe-area-inset-bottom);
}

.content-section {
  background-color: #fff;
  min-height: 100%;
}

.section-header {
  padding: 24rpx 32rpx 20rpx;

  .section-header-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16rpx;
  }

  .section-filter-entry {
    display: flex;
    align-items: center;
    flex-shrink: 0;
    padding: 8rpx 16rpx;
    background: #fff5f0;
    border-radius: 999rpx;
  }

  .section-filter-text {
    margin-left: 8rpx;
    font-size: 24rpx;
    color: #ff6037;
  }

  .section-title {
    font-size: 34rpx;
    font-weight: bold;
    color: #1a1a1a;
  }

  .section-ai-hint {
    display: block;
    margin-top: 12rpx;
    font-size: 26rpx;
    color: #666;
    line-height: 1.5;
  }

  .section-plain-summary {
    display: block;
    margin-top: 12rpx;
    font-size: 26rpx;
    color: #555;
    line-height: 1.55;
  }

  .section-plain-summary--sub {
    margin-top: 8rpx;
    font-size: 22rpx;
    color: #909399;
  }

  .section-tech-toggle {
    display: inline-block;
    margin-top: 10rpx;
    font-size: 22rpx;
    color: #ff6037;
    text-decoration: underline;
  }

  .section-score-note {
    display: block;
    margin-top: 10rpx;
    font-size: 22rpx;
    color: #909399;
    line-height: 1.45;
  }
}

.rec-ai-status-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12rpx;
  margin-top: 16rpx;
  padding: 14rpx 18rpx;
  background: linear-gradient(90deg, #fff8f5 0%, #f5f9ff 100%);
  border-radius: 12rpx;
  border: 1rpx solid #ffe4d6;
}

.rec-ai-status-k {
  font-size: 24rpx;
  font-weight: 600;
  color: #ff6037;
}

.rec-ai-status-v {
  font-size: 22rpx;
  color: #606266;
  flex: 1;
  min-width: 200rpx;
}

.rec-ai-status-v--ok {
  color: #2d8f4e;
}

.rec-ai-status-v--warn {
  color: #e6a23c;
}

.rec-user-tags {
  margin-top: 20rpx;
}

.rec-user-tags-label {
  display: block;
  font-size: 24rpx;
  color: #909399;
  margin-bottom: 12rpx;
}

.rec-tag-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.rec-tag-chip {
  padding: 8rpx 20rpx;
  background: #fff5f0;
  border-radius: 999rpx;
  border: 1rpx solid #ffd4c2;
}

.rec-tag-chip text {
  font-size: 24rpx;
  color: #ff6037;
  font-weight: 500;
}

.rec-metric-panel {
  margin-top: 24rpx;
  padding: 20rpx;
  background: #fafbfc;
  border-radius: 16rpx;
  border: 1rpx solid #ebeef5;
}

.rec-metric-panel-title {
  display: block;
  font-size: 24rpx;
  color: #606266;
  font-weight: 600;
  margin-bottom: 16rpx;
}

.rec-metric-line {
  margin-bottom: 18rpx;
}

.rec-metric-line:last-child {
  margin-bottom: 0;
}

.rec-metric-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}

.rec-metric-name {
  font-size: 26rpx;
  color: #303133;
}

.rec-metric-val {
  font-size: 26rpx;
  font-weight: 600;
  color: #ff6037;
}

.rec-metric-bar-bg {
  height: 12rpx;
  background: #e4e7ed;
  border-radius: 999rpx;
  overflow: hidden;
}

.rec-metric-bar-fg {
  height: 100%;
  background: linear-gradient(90deg, #ff9f7a, #ff6037);
  border-radius: 999rpx;
  max-width: 100%;
}

.list-wrapper {
  padding: 0 20rpx 40rpx;

  ::v-deep .imglist-item {
    background: #fff;
    border-radius: 20rpx;
    overflow: hidden;
    margin-bottom: 24rpx;
    box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.03);
  }
}

.load-more {
  text-align: center;
  padding: 40rpx 0;
  color: #c0c4cc;
  font-size: 24rpx;
}

.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 100rpx;

  .empty-icon {
    font-size: 100rpx;
    margin-bottom: 20rpx;
    opacity: 0.6;
  }

  .empty-text {
    color: #909399;
    font-size: 28rpx;
  }
}

.rec-filter-panel {
  padding: 28rpx 28rpx 0;
  padding-bottom: calc(12rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.rec-filter-scroll {
  max-height: 52vh;
  width: 100%;
  box-sizing: border-box;
}

.rec-filter-line {
  width: 100%;
}

.rec-filter-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
  margin-bottom: 12rpx;
}

.rec-filter-tip {
  font-size: 24rpx;
  color: #909399;
  line-height: 1.5;
  margin-bottom: 24rpx;
  display: block;
}

.rec-filter-group-title {
  display: block;
  font-size: 24rpx;
  font-weight: 600;
  color: #909399;
  padding: 16rpx 0 8rpx;
}

.rec-filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.rec-filter-label {
  flex: 1;
  padding-right: 20rpx;
}

.rec-filter-name {
  font-size: 28rpx;
  color: #303133;
  display: block;
}

.rec-filter-desc {
  font-size: 22rpx;
  color: #909399;
  margin-top: 6rpx;
  display: block;
  line-height: 1.4;
}

.rec-near-block {
  margin-top: 20rpx;
  padding: 20rpx;
  background: #f8f9fa;
  border-radius: 12rpx;
}

.rec-near-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12rpx;
}

.rec-near-label {
  font-size: 24rpx;
  color: #606266;
}

.rec-loc-status {
  display: block;
  margin-top: 12rpx;
  font-size: 22rpx;
  color: #909399;
}

.rec-filter-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 32rpx;
}

.rec-filter-actions .u-button {
  flex: 1;
}

</style>