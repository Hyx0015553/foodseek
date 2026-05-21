<template>
  <view class="page-container">
    <u-navbar title="运营数据" :border="true" :placeholder="true" :autoBack="true"></u-navbar>

    <scroll-view scroll-y class="main-scroll">
      <view v-if="!userInfo || !userInfo.sid" class="empty-box">
        <text class="empty-icon">🔒</text>
        <text class="empty-text">请先登录商家账号</text>
      </view>

      <view v-else class="content-wrapper">
        <!-- 店铺名称 -->
        <view class="shop-header">
          <text class="shop-name">{{ userInfo.shopname || userInfo.username || '我的店铺' }}</text>
          <text class="shop-desc">本店运营数据概览</text>
        </view>

        <!-- AI 决策建议：与系统消息同源（admin_ai_ops_decision / admin_ai_ops_advice） -->
        <view class="ai-card" v-if="aiDecisionEnabled">
          <view class="ai-card-hd">
            <view class="ai-left">
              <text class="ai-title">AI 决策建议</text>
              <view class="ai-badge" v-if="aiHasNew">NEW</view>
            </view>
            <view class="ai-actions">
              <u-button
                type="primary"
                size="mini"
                shape="circle"
                :text="aiLoading ? '加载中…' : (aiLatest ? '查看' : '暂无')"
                :disabled="aiLoading || !aiLatest"
                @click="openAiDecision()"
              ></u-button>
            </view>
          </view>
          <view class="ai-card-bd">
            <text class="ai-sub" v-if="aiLatest">
              最新更新时间：{{ aiLatest.ndate || '-' }}
            </text>
            <text class="ai-sub" v-else>
              暂无平台推送的运营决策建议（管理员生成后会展示在此）
            </text>
            <view class="ai-quick-row" v-if="aiLatest">
              <text class="ai-quick-link" @tap="reloadAiDecision">刷新建议</text>
              <text class="ai-quick-link" @tap="copyAiDecision">复制全文</text>
            </view>
          </view>
        </view>

        <!-- 时间窗口切换 -->
        <view class="ops-tabs">
          <view class="ops-tab" :class="{ on: opsDays === 7 }" @tap="switchOpsDays(7)">近7天</view>
          <view class="ops-tab" :class="{ on: opsDays === 30 }" @tap="switchOpsDays(30)">近30天</view>
        </view>

        <!-- 核心概览 -->
        <view class="section-title">核心概览（近{{ opsDays }}天）</view>
        <view class="stats-grid">
          <view class="stat-card">
            <view class="stat-icon bg-orange">🍜</view>
            <text class="stat-value">{{ stats.goodCount }}</text>
            <text class="stat-label">商品数量</text>
          </view>
          <view class="stat-card">
            <view class="stat-icon bg-cyan">📦</view>
            <text class="stat-value">{{ curOpsStats.totalSales }}</text>
            <text class="stat-label">商品总销量</text>
          </view>
          <view class="stat-card">
            <view class="stat-icon bg-gold">💰</view>
            <text class="stat-value">{{ curOpsStats.paidGmvText }}</text>
            <text class="stat-label">营业额</text>
          </view>
          <view class="stat-card">
            <view class="stat-icon bg-purple">🧾</view>
            <text class="stat-value">{{ curOpsStats.paidOrders }}</text>
            <text class="stat-label">成交单</text>
          </view>
          <view class="stat-card">
            <view class="stat-icon bg-blue">💬</view>
            <text class="stat-value">{{ stats.replayCount }}</text>
            <text class="stat-label">用户评价</text>
          </view>
          <view class="stat-card">
            <view class="stat-icon bg-green">👍</view>
            <text class="stat-value">{{ stats.goodRateText }}</text>
            <text class="stat-label">好评率</text>
          </view>
          <view class="stat-card">
            <view class="stat-icon bg-purple">❓</view>
            <text class="stat-value">{{ stats.qaCount }}</text>
            <text class="stat-label">问答数量</text>
          </view>
          <view class="stat-card">
            <view class="stat-icon bg-gold">🎫</view>
            <text class="stat-value">{{ stats.couponCount }}</text>
            <text class="stat-label">优惠券发放</text>
          </view>
        </view>

        <!-- 风险与待办 -->
        <view class="section-title">风险与待办</view>
        <view class="alert-grid">
          <view class="alert-card" :class="{ warn: stats.appealCount > 0 }" @tap="toReplay">
            <text class="alert-value">{{ stats.appealCount }}</text>
            <text class="alert-label">待处理申诉</text>
          </view>
          <view class="alert-card" :class="{ warn: stats.lowScoreCount > 0 }" @tap="toReplay">
            <text class="alert-value">{{ stats.lowScoreCount }}</text>
            <text class="alert-label">低分评价</text>
          </view>
          <view class="alert-card" :class="{ warn: stats.unansweredQa > 0 }" @tap="toShopQa">
            <text class="alert-value">{{ stats.unansweredQa }}</text>
            <text class="alert-label">待回答</text>
          </view>
          <view class="alert-card" :class="{ warn: stats.unsoldCount > 0 }" @tap="toGood">
            <text class="alert-value">{{ curOpsStats.unsoldCount }}</text>
            <text class="alert-label">滞销商品</text>
          </view>
        </view>

        <!-- 口碑分析 -->
        <view class="section-title">口碑分析</view>
        <view class="info-card">
          <view class="info-row">
            <text class="info-label">评价总数</text>
            <text class="info-value">{{ stats.replayCount }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">好评率</text>
            <text class="info-value">{{ stats.goodRateText }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">低分评价(≤2星)</text>
            <text class="info-value" :class="{ danger: stats.lowScoreCount > 0 }">{{ stats.lowScoreCount }}</text>
          </view>
        </view>

        <!-- 评价词云 -->
        <view class="section-title">评价词云（近{{ opsDays }}天）</view>
        <view class="info-card">
          <view class="wordcloud-toolbar">
            <text class="wordcloud-tip">高频词越大，说明用户提及次数越多</text>
            <u-button
              size="mini"
              shape="circle"
              type="primary"
              plain
              :text="wordCloudRefreshing ? '重建中…' : '刷新词云'"
              :disabled="wordCloudRefreshing"
              @click="refreshWordCloud(true)"
            ></u-button>
          </view>
          <view class="wordcloud-wrap" v-if="curWordCloud.length">
            <replay-word-cloud :key="opsDays" :items="curWordCloud" />
          </view>
          <view class="empty-tip" v-else>暂无有效评价词，可先积累评价数据</view>
        </view>

        <!-- 内容与互动 -->
        <view class="section-title">内容与互动</view>
        <view class="info-card">
          <view class="info-row">
            <text class="info-label">店铺文章</text>
            <text class="info-value">{{ stats.blogCount }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">文章总点赞</text>
            <text class="info-value">{{ stats.blogZan }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">文章总浏览</text>
            <text class="info-value">{{ stats.blogView }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">待回答问答</text>
            <text class="info-value" :class="{ danger: stats.unansweredQa > 0 }">{{ stats.unansweredQa }}</text>
          </view>
        </view>

        <!-- 趋势（折线图） -->
        <view class="section-title">变化趋势（近{{ opsDays }}天）</view>
        <view class="info-card">
          <view class="trend-legend">
            <view class="lg-item"><text class="lg-dot dot-orange"></text><text>成交单</text></view>
            <view class="lg-item"><text class="lg-dot dot-blue"></text><text>营业额</text></view>
          </view>
          <canvas
            class="trend-canvas"
            canvas-id="trendCanvas"
            id="trendCanvas"
            :style="{ width: canvasW + 'px', height: canvasH + 'px' }"
          ></canvas>
          <view class="empty-tip" v-if="!curTrendFilled.length">暂无趋势数据</view>
        </view>

        <!-- 热销商品 Top5（按订单汇总销量口径） -->
        <view class="section-title">热销商品 Top5（近{{ opsDays }}天）</view>
        <view class="hot-list" v-if="curHotGoods.length > 0">
          <view class="hot-item" v-for="(g, i) in curHotGoods" :key="g.id" @tap="toGoodDetail(g.id)">
            <text class="hot-rank">{{ i + 1 }}</text>
            <text class="hot-name">{{ g.gname || '未命名' }}</text>
            <text class="hot-sales">销量 {{ g.qty || 0 }}</text>
          </view>
        </view>
        <view class="empty-tip" v-else>暂无销量数据</view>

        <!-- 快捷入口 -->
        <view class="section-title">快捷入口</view>
        <view class="entry-list">
          <view class="entry-item" v-if="aiLatest" @tap="openAiDecision()">
            <view class="entry-left">
              <text class="entry-icon">🧠</text>
              <text class="entry-text">AI 决策建议</text>
              <view class="entry-new" v-if="aiHasNew"><text>NEW</text></view>
            </view>
            <text class="entry-arrow">></text>
          </view>
          <view class="entry-item" @tap="toGood">
            <view class="entry-left">
              <text class="entry-icon">🍜</text>
              <text class="entry-text">商品管理</text>
            </view>
            <text class="entry-arrow">></text>
          </view>
          <view class="entry-item" @tap="toReplay">
            <view class="entry-left">
              <text class="entry-icon">💬</text>
              <text class="entry-text">评价管理</text>
            </view>
            <text class="entry-arrow">></text>
          </view>
          <view class="entry-item" @tap="toShopQa">
            <view class="entry-left">
              <text class="entry-icon">❓</text>
              <text class="entry-text">店铺问答</text>
            </view>
            <text class="entry-arrow">></text>
          </view>
          <view class="entry-item" @tap="toBlogShop">
            <view class="entry-left">
              <text class="entry-icon">📝</text>
              <text class="entry-text">店铺文章</text>
            </view>
            <text class="entry-arrow">></text>
          </view>
        </view>

        <view class="bottom-padding"></view>
      </view>
    </scroll-view>

    <!-- AI 决策建议详情弹窗 -->
    <u-popup :show="aiPopupShow" mode="bottom" round="16" @close="aiPopupShow = false">
      <view class="ai-popup">
        <view class="ai-popup-hd">
          <text class="ai-popup-title">AI 决策建议</text>
          <text class="ai-popup-time">{{ (aiLatest && aiLatest.ndate) ? aiLatest.ndate : '' }}</text>
        </view>
        <scroll-view scroll-y class="ai-popup-scroll">
          <view class="ai-popup-body">
            <u-parse :content="formatNote(aiLatest ? aiLatest.note : '')" />
          </view>
        </scroll-view>
        <view class="ai-popup-ft">
          <u-button type="default" size="mini" shape="circle" text="关闭" @click="aiPopupShow=false"></u-button>
          <u-button type="primary" size="mini" shape="circle" text="复制全文" @click="copyAiDecision"></u-button>
        </view>
      </view>
    </u-popup>
    <ai-assistant-float />
  </view>
</template>

<script>
import { listj, listSqlj, merchantOpsReportj, shopWordCloudj, rebuildShopWordCloudj } from '@/common/config/api.js'
import { mapState } from 'vuex'

export default {
  data() {
    return {
      opsDays: 7,
      canvasW: 320,
      canvasH: 180,
      stats: {
        goodCount: 0,
        replayCount: 0,
        goodRateText: '-',
        lowScoreCount: 0,
        qaCount: 0,
        unansweredQa: 0,
        blogCount: 0,
        blogZan: 0,
        blogView: 0,
        couponCount: 0,
        appealCount: 0,
        unsoldCount: 0
      },
      stats7: { windowDays: 7, totalSales: 0, paidOrders: 0, paidGmvText: '0', unsoldCount: 0, goodsSoldCount: 0, goodsCount: 0 },
      stats30: { windowDays: 30, totalSales: 0, paidOrders: 0, paidGmvText: '0', unsoldCount: 0, goodsSoldCount: 0, goodsCount: 0 },
      hotGoods7: [],
      hotGoods30: [],
      trendDaily7: [],
      trendDaily30: [],
      wordCloud7: [],
      wordCloud30: [],
      wordCloudRefreshing: false,

      // 平台 AI 建议（sysmsg：批量/单店「决策」= admin_ai_ops_decision；评价地图「运营建议」= admin_ai_ops_advice）
      aiDecisionEnabled: true,
      aiLoading: false,
      aiLatest: null,
      aiPopupShow: false,
      aiLastReadId: ''
    }
  },
  onShow() {
    this.loadStats()
    this.loadAiDecision()
  },
  methods: {
    switchOpsDays(d) {
      if (d !== 7 && d !== 30) return
      this.opsDays = d
      this.$nextTick(() => {
        this.drawTrendChart()
      })
    },
    initCanvasSize() {
      try {
        const sys = uni.getSystemInfoSync()
        const w = sys && sys.windowWidth ? sys.windowWidth : 375
        // card 左右 padding 大约 24*2 + 额外内边距，留一点余量
        this.canvasW = Math.max(280, Math.floor(w - 24 - 24 - 24))
      } catch (e) {
        this.canvasW = 320
      }
      this.canvasH = 180
    },
    pad2(n) { return n < 10 ? ('0' + n) : ('' + n) },
    fmtDay(d) {
      return d.getFullYear() + '-' + this.pad2(d.getMonth() + 1) + '-' + this.pad2(d.getDate())
    },
    buildFilledTrend(days, rawTrend) {
      const map = new Map()
      ;(rawTrend || []).forEach(x => {
        if (!x || !x.day) return
        map.set(String(x.day), {
          day: String(x.day),
          paidOrders: parseInt(x.paidOrders || 0, 10) || 0,
          items: parseInt(x.items || 0, 10) || 0,
          gmv: (x.gmv != null) ? Number(x.gmv) : 0
        })
      })
      const out = []
      const end = new Date()
      end.setHours(0, 0, 0, 0)
      for (let i = days - 1; i >= 0; i--) {
        const d = new Date(end.getTime() - i * 86400000)
        const key = this.fmtDay(d)
        const hit = map.get(key) || { day: key, paidOrders: 0, items: 0, gmv: 0 }
        out.push(hit)
      }
      return out
    },
    drawTrendChart() {
      const series = this.curTrendFilled || []
      const ctx = uni.createCanvasContext('trendCanvas', this)
      const w = this.canvasW
      const h = this.canvasH
      const padL = 34, padR = 12, padT = 10, padB = 22
      ctx.clearRect(0, 0, w, h)

      // 边框/背景
      ctx.setFillStyle('#ffffff')
      ctx.fillRect(0, 0, w, h)

      if (!series.length) {
        ctx.draw()
        return
      }

      const maxOrders = Math.max(...series.map(x => x.paidOrders || 0), 1)
      const maxGmv = Math.max(...series.map(x => (x.gmv != null ? Number(x.gmv) : 0)), 1)

      const innerW = w - padL - padR
      const innerH = h - padT - padB
      const n = series.length
      const stepX = n <= 1 ? innerW : (innerW / (n - 1))

      // 网格线
      ctx.setStrokeStyle('#f1f5f9')
      ctx.setLineWidth(1)
      for (let i = 0; i <= 4; i++) {
        const y = padT + (innerH * i / 4)
        ctx.beginPath()
        ctx.moveTo(padL, y)
        ctx.lineTo(w - padR, y)
        ctx.stroke()
      }

      // 轴
      ctx.setStrokeStyle('#e5e7eb')
      ctx.beginPath()
      ctx.moveTo(padL, padT)
      ctx.lineTo(padL, padT + innerH)
      ctx.lineTo(w - padR, padT + innerH)
      ctx.stroke()

      const toX = (i) => padL + stepX * i
      const toYOrders = (v) => padT + innerH - (v / maxOrders) * innerH
      const toYGmv = (v) => padT + innerH - (v / maxGmv) * innerH

      // 折线：订单（橙）
      ctx.setStrokeStyle('#ff7a45')
      ctx.setLineWidth(2)
      ctx.beginPath()
      series.forEach((p, i) => {
        const x = toX(i)
        const y = toYOrders(p.paidOrders || 0)
        if (i === 0) ctx.moveTo(x, y)
        else ctx.lineTo(x, y)
      })
      ctx.stroke()

      // 折线：GMV（蓝）
      ctx.setStrokeStyle('#3b82f6')
      ctx.setLineWidth(2)
      ctx.beginPath()
      series.forEach((p, i) => {
        const x = toX(i)
        const y = toYGmv((p.gmv != null ? Number(p.gmv) : 0))
        if (i === 0) ctx.moveTo(x, y)
        else ctx.lineTo(x, y)
      })
      ctx.stroke()

      // x 轴日期标注：只标 3 个点（左/中/右）
      ctx.setFillStyle('#9ca3af')
      ctx.setFontSize(10)
      const idxs = Array.from(new Set([0, Math.floor((n - 1) / 2), n - 1])).sort((a, b) => a - b)
      idxs.forEach((i) => {
        const day = (series[i].day || '').slice(5) // MM-DD
        const x = toX(i)
        const y = padT + innerH + 14
        ctx.fillText(day, Math.max(0, x - 14), y)
      })

      ctx.draw()
    },
    formatNote(note) {
      const raw = (note || '').toString()
      return raw.replace(/\n/g, '<br/>')
    },
    loadAiDecision() {
      if (!this.aiDecisionEnabled) return
      if (!this.userInfo || !this.userInfo.sid) {
        this.aiLatest = null
        return
      }
      try {
        this.aiLastReadId = uni.getStorageSync('merchant_ai_decision_last_read_id') || ''
      } catch (e) {
        this.aiLastReadId = ''
      }
      this.aiLoading = true
      const sid = this.userInfo.sid
      const pDecision = listj({ params: { table: 'sysmsg', sid, biztype: 'admin_ai_ops_decision' } }).catch(() => [])
      const pAdvice = listj({ params: { table: 'sysmsg', sid, biztype: 'admin_ai_ops_advice' } }).catch(() => [])
      Promise.all([pDecision, pAdvice])
        .then(([decisionRows, adviceRows]) => {
          const seen = new Set()
          const merged = []
          ;(decisionRows || []).concat(adviceRows || []).forEach((r) => {
            if (!r || !r.note) return
            const id = r.id != null ? String(r.id) : ''
            if (id && seen.has(id)) return
            if (id) seen.add(id)
            merged.push(r)
          })
          merged.sort((a, b) => {
            const ad = (a.ndate || '') + ''
            const bd = (b.ndate || '') + ''
            if (bd !== ad) return bd.localeCompare(ad)
            return (parseInt(b.id || 0, 10) - parseInt(a.id || 0, 10))
          })
          this.aiLatest = merged.length ? merged[0] : null
        })
        .catch(() => {
          this.aiLatest = null
        })
        .finally(() => {
          this.aiLoading = false
        })
    },
    reloadAiDecision() {
      this.loadAiDecision()
    },
    openAiDecision() {
      if (!this.aiLatest) return
      this.aiPopupShow = true
      if (this.aiLatest && this.aiLatest.id) {
        const id = String(this.aiLatest.id)
        this.aiLastReadId = id
        try {
          uni.setStorageSync('merchant_ai_decision_last_read_id', id)
        } catch (e) {}
      }
    },
    copyAiDecision() {
      if (!this.aiLatest || !this.aiLatest.note) {
        uni.showToast({ title: '暂无可复制内容', icon: 'none' })
        return
      }
      const txt = (this.aiLatest.note || '').toString()
      uni.setClipboardData({
        data: txt,
        success: () => {
          uni.showToast({ title: '已复制', icon: 'none' })
        }
      })
    },
    loadStats() {
      if (!this.userInfo || !this.userInfo.sid) return
      const sid = this.userInfo.sid
      const sidStr = String(sid)
      this.initCanvasSize()
      this.refreshWordCloud(false)

      // 商品：数量（仍以商品表为准）；销量/热销（改为按订单汇总，确保准确）
      listj({ params: { table: 'good', sid } }).then(res => {
        const list = res || []
        this.stats.goodCount = list.length
      }).catch(() => { this.stats.goodCount = 0 })

      const loadWindow = (days) => {
        return merchantOpsReportj({ params: { sid, days } }).then(res => {
          if (!res || !res.ok || !res.summary) {
            throw new Error((res && res.error) ? res.error : '运营数据接口异常')
          }
          const s = res.summary
          const gmv = (s.paidGmv != null) ? Number(s.paidGmv) : 0
          const goodSales = res.goodSales || []
          const soldCount = goodSales.filter(x => parseInt(x.qty || 0, 10) > 0).length
          const goodsCount = goodSales.length
          const pack = {
            windowDays: days,
            totalSales: parseInt(s.paidItemCount || 0, 10),
            paidOrders: parseInt(s.paidOrderCount || 0, 10),
            paidGmvText: isNaN(gmv) ? '0' : gmv.toFixed(2),
            goodsSoldCount: soldCount,
            goodsCount: goodsCount,
            unsoldCount: Math.max(0, goodsCount - soldCount),
            trendDaily: res.trendDaily || [],
            topGoods: (res.topGoods || []).slice(0, 5)
          }
          return pack
        })
      }

      Promise.all([loadWindow(7), loadWindow(30)]).then(([w7, w30]) => {
        this.stats7 = { ...this.stats7, ...w7 }
        this.stats30 = { ...this.stats30, ...w30 }
        this.trendDaily7 = w7.trendDaily
        this.trendDaily30 = w30.trendDaily
        this.hotGoods7 = w7.topGoods
        this.hotGoods30 = w30.topGoods
        this.$nextTick(() => {
          this.drawTrendChart()
        })
      }).catch(() => {
        // 回退到历史字段（可能不准，但页面不空）
        listj({ params: { table: 'good', sid } }).then(res => {
          const list = res || []
          const totalSales = list.reduce((s, g) => s + (parseInt(g.xl || 0, 10)), 0)
          const unsoldCount = list.filter(g => !parseInt(g.xl || 0, 10)).length
          const hot = [...list].sort((a, b) => (parseInt(b.xl || 0, 10) - parseInt(a.xl || 0, 10))).slice(0, 5)
          this.stats7.totalSales = totalSales
          this.stats30.totalSales = totalSales
          this.stats30.unsoldCount = unsoldCount
          this.hotGoods7 = hot.map(x => ({ ...x, qty: parseInt(x.xl || 0, 10) }))
          this.hotGoods30 = hot.map(x => ({ ...x, qty: parseInt(x.xl || 0, 10) }))
          this.trendDaily7 = []
          this.trendDaily30 = []
          this.$nextTick(() => {
            this.drawTrendChart()
          })
        }).catch(() => {
          this.stats7.totalSales = 0
          this.stats30.totalSales = 0
          this.stats30.unsoldCount = 0
          this.hotGoods7 = []
          this.hotGoods30 = []
          this.trendDaily7 = []
          this.trendDaily30 = []
          this.$nextTick(() => {
            this.drawTrendChart()
          })
        })
      })

      // 评价：店铺直评(type=9) + 本店菜品评价(type=1，pid 为商品 id)
      const sidSql = sidStr.replace(/'/g, "''")
      listSqlj({
        params: {
          sql: `SELECT r.pf FROM fs_replay r
            LEFT JOIN fs_good g ON r.type = 1 AND CAST(r.pid AS UNSIGNED) = g.id
            WHERE (
              (r.type = 9 AND r.pid = '${sidSql}')
              OR (r.type = 1 AND g.sid IS NOT NULL AND g.sid = '${sidSql}')
            )`
        }
      }).then(res => {
        const list = res || []
        this.stats.replayCount = list.length
        if (list.length === 0) {
          this.stats.goodRateText = '-'
          this.stats.lowScoreCount = 0
          return
        }
        const withScore = list.filter(r => r.pf != null && r.pf !== '')
        const goodCount = withScore.filter(r => parseFloat(r.pf) >= 4).length
        const lowCount = withScore.filter(r => parseFloat(r.pf) <= 2).length
        this.stats.goodRateText = withScore.length ? (Math.round(goodCount / withScore.length * 100) + '%') : '-'
        this.stats.lowScoreCount = lowCount
      }).catch(() => {
        this.stats.replayCount = 0
        this.stats.goodRateText = '-'
        this.stats.lowScoreCount = 0
      })

      // 待处理申诉
      listj({ params: { table: 'replay_appeal', sid, statecn: '待处理' } }).then(res => {
        this.stats.appealCount = (res || []).length
      }).catch(() => { this.stats.appealCount = 0 })

      // 店铺问答：总数、待回答
      listj({ params: { table: 'shop_qa', sid, qtype: 'Q' } }).then(qs => {
        const questions = qs || []
        this.stats.qaCount = questions.length
        if (questions.length === 0) {
          this.stats.unansweredQa = 0
          return
        }
        return listj({ params: { table: 'shop_qa', sid, qtype: 'A' } }).then(ans => {
          const answeredIds = new Set((ans || []).map(a => String(a.qid || '')).filter(Boolean))
          this.stats.unansweredQa = questions.filter(q => !answeredIds.has(String(q.id))).length
        })
      }).catch(() => {
        this.stats.qaCount = 0
        this.stats.unansweredQa = 0
      })

      // 店铺文章：数量、点赞、浏览
      listj({ params: { table: 'blog', sid } }).then(res => {
        const list = res || []
        this.stats.blogCount = list.length
        this.stats.blogZan = list.reduce((s, b) => s + (parseInt(b.zan || 0, 10)), 0)
        this.stats.blogView = list.reduce((s, b) => s + (parseInt(b.vcount || 0, 10)), 0)
      }).catch(() => {
        this.stats.blogCount = 0
        this.stats.blogZan = 0
        this.stats.blogView = 0
      })

      // 优惠券发放
      listj({ params: { table: 'youhuiquan', sid } }).then(res => {
        const list = res || []
        this.stats.couponCount = list.reduce((sum, item) => sum + (parseInt(item.total || 0, 10)), 0)
      }).catch(() => { this.stats.couponCount = 0 })
    },
    toGood() {
      uni.navigateTo({ url: '/pages/good/good' })
    },
    toReplay() {
      // 从运营数据页进入：默认只看“待处理申诉”
      uni.navigateTo({ url: '/pages/merchant/replay?tab=appeal_ing' })
    },
    toShopQa() {
      const sid = this.userInfo.sid
      const shop = encodeURIComponent(this.userInfo.shopname || this.userInfo.username || '')
      uni.navigateTo({ url: `/pages/shops/shopqa?sid=${sid}&shop=${shop}` })
    },
    toBlogShop() {
      uni.navigateTo({ url: '/pages/blog/blogshop?sid=' + this.userInfo.sid })
    },
    toGoodDetail(gid) {
      if (gid) uni.navigateTo({ url: '/pages/good/gooddetail?gid=' + gid })
    },
    refreshWordCloud(forceRebuild) {
      if (!this.userInfo || !this.userInfo.sid) return
      const sid = this.userInfo.sid
      const force = !!forceRebuild
      if (force) this.wordCloudRefreshing = true
      const loadOne = (days) => {
        const call = force ? rebuildShopWordCloudj : shopWordCloudj
        return call({
          params: {
            sid,
            days,
            topN: 48,
            minFreq: 2,
            force: force ? 1 : 0
          }
        }).then(res => {
          if (!res || !res.ok) return []
          const arr = Array.isArray(res.items) ? res.items : []
          return arr
            .filter(x => x && x.name)
            .slice(0, 48)
            .map((x) => ({ ...x }))
        }).catch(() => [])
      }
      Promise.all([loadOne(7), loadOne(30)]).then(([w7, w30]) => {
        this.wordCloud7 = w7 || []
        this.wordCloud30 = w30 || []
        if (force) {
          uni.showToast({ title: '词云已刷新', icon: 'none' })
        }
      }).finally(() => {
        if (force) this.wordCloudRefreshing = false
      })
    }
  },
  computed: {
    ...mapState(['userInfo']),
    curOpsStats() {
      return this.opsDays === 30 ? this.stats30 : this.stats7
    },
    curHotGoods() {
      return this.opsDays === 30 ? (this.hotGoods30 || []) : (this.hotGoods7 || [])
    },
    curTrendRaw() {
      return this.opsDays === 30 ? (this.trendDaily30 || []) : (this.trendDaily7 || [])
    },
    curTrendFilled() {
      return this.buildFilledTrend(this.opsDays, this.curTrendRaw || [])
    },
    curWordCloud() {
      return this.opsDays === 30 ? (this.wordCloud30 || []) : (this.wordCloud7 || [])
    },
    aiHasNew() {
      if (!this.aiLatest || !this.aiLatest.id) return false
      return String(this.aiLatest.id) !== String(this.aiLastReadId || '')
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #ff943c;
$bg: #f6f7f9;
$card: #ffffff;

.ops-tabs{
  display:flex;
  background:#fff;
  border: 1px solid #f0f2f5;
  border-radius: 999px;
  padding: 8rpx;
  margin: 16rpx 24rpx 8rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.03);
}
.ops-tab{
  flex:1;
  text-align:center;
  font-size: 26rpx;
  font-weight: 700;
  color:#6b7280;
  padding: 14rpx 0;
  border-radius: 999px;
}
.ops-tab.on{
  background: rgba(255, 148, 60, 0.18);
  color:#ff7a45;
}

.trend-legend{
  display:flex;
  gap: 18rpx;
  align-items:center;
  padding: 6rpx 8rpx 12rpx;
  color:#6b7280;
  font-size: 24rpx;
}
.lg-item{ display:flex; align-items:center; gap: 10rpx; }
.lg-dot{ width: 14rpx; height: 14rpx; border-radius: 999px; display:inline-block; }
.dot-orange{ background:#ff7a45; }
.dot-blue{ background:#3b82f6; }
.trend-canvas{
  width: 100%;
  height: 360rpx;
}

.page-container {
  background-color: $bg;
  min-height: 100vh;
}

.main-scroll {
  height: calc(100vh - 88rpx);
}

.empty-box {
  padding-top: 160rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #909399;

  .empty-icon {
    font-size: 96rpx;
    margin-bottom: 20rpx;
  }

  .empty-text {
    font-size: 28rpx;
  }
}

.content-wrapper {
  padding: 24rpx 30rpx;
}

.shop-header {
  background: linear-gradient(135deg, #ffecd2 0%, #ff943c 100%);
  border-radius: 24rpx;
  padding: 36rpx 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 8rpx 24rpx rgba(255, 148, 60, 0.2);

  .shop-name {
    display: block;
    font-size: 36rpx;
    font-weight: bold;
    color: #1a1a1a;
    margin-bottom: 8rpx;
  }

  .shop-desc {
    font-size: 24rpx;
    color: rgba(0, 0, 0, 0.5);
  }
}

.ai-card{
  background-color: $card;
  border-radius: 20rpx;
  padding: 22rpx 22rpx 18rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  border: 2rpx solid rgba(255, 148, 60, 0.18);
}
.ai-card-hd{
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap: 16rpx;
}
.ai-left{
  display:flex;
  align-items:center;
  gap: 12rpx;
  min-width: 0;
}
.ai-title{
  font-size: 30rpx;
  font-weight: 800;
  color: #1a1a1a;
}
.ai-badge{
  padding: 4rpx 10rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, #ff6a00, #ffb347);
}
.ai-card-bd{
  margin-top: 12rpx;
}
.ai-sub{
  display:block;
  font-size: 24rpx;
  color:#909399;
  line-height: 1.55;
}
.ai-quick-row{
  margin-top: 10rpx;
  display:flex;
  gap: 20rpx;
}
.ai-quick-link{
  font-size: 24rpx;
  color: $primary;
  font-weight: 600;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #1a1a1a;
  margin-bottom: 16rpx;
}

.stats-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
  margin-bottom: 30rpx;
}

.stat-card {
  width: calc(50% - 10rpx);
  background-color: $card;
  border-radius: 20rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  align-items: center;

  .stat-icon {
    width: 64rpx;
    height: 64rpx;
    border-radius: 16rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32rpx;
    margin-bottom: 12rpx;
  }

  .bg-orange { background: linear-gradient(135deg, #fff1e6, #ffd9c4); }
  .bg-cyan { background: linear-gradient(135deg, #e0f7fa, #b2ebf2); }
  .bg-blue { background: linear-gradient(135deg, #e6f4ff, #bae0ff); }
  .bg-green { background: linear-gradient(135deg, #e6f9f0, #b3f0d4); }
  .bg-purple { background: linear-gradient(135deg, #f3e8ff, #e9d5ff); }
  .bg-gold { background: linear-gradient(135deg, #fff9e6, #ffecb3); }

  .stat-value {
    font-size: 40rpx;
    font-weight: bold;
    color: #1a1a1a;
    margin-bottom: 4rpx;
  }

  .stat-label {
    font-size: 24rpx;
    color: #909399;
  }
}

.alert-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 30rpx;
}

.alert-card {
  width: calc(25% - 12rpx);
  background-color: $card;
  border-radius: 16rpx;
  padding: 20rpx;
  text-align: center;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.04);
  border: 2rpx solid transparent;

  &.warn {
    border-color: #ff9800;
    background-color: #fff8e1;
  }

  .alert-value {
    display: block;
    font-size: 32rpx;
    font-weight: bold;
    color: #1a1a1a;
  }

  .alert-label {
    font-size: 22rpx;
    color: #909399;
  }
}

.info-card {
  background-color: $card;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.wordcloud-toolbar{
  display:flex;
  justify-content:space-between;
  align-items:center;
  gap: 16rpx;
  margin-bottom: 14rpx;
}

.wordcloud-tip{
  font-size: 22rpx;
  color: #9ca3af;
}

.wordcloud-wrap{
  display: block;
  width: 100%;
  min-height: 220rpx;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f2f3f5;

  &:last-child { border-bottom: none; }
}

.info-label {
  font-size: 28rpx;
  color: #606266;
}

.info-value {
  font-size: 28rpx;
  font-weight: 600;
  color: #1a1a1a;

  &.danger {
    color: #ff4d4f;
  }
}

.hot-list {
  background-color: $card;
  border-radius: 20rpx;
  overflow: hidden;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.hot-item {
  display: flex;
  align-items: center;
  padding: 24rpx 28rpx;
  border-bottom: 1rpx solid #f2f3f5;

  &:last-child { border-bottom: none; }
  &:active { background-color: #f8f9fa; }
}

.hot-rank {
  width: 48rpx;
  height: 48rpx;
  line-height: 48rpx;
  text-align: center;
  font-size: 24rpx;
  font-weight: bold;
  background: linear-gradient(135deg, #ffb347, #ff6a00);
  color: #fff;
  border-radius: 12rpx;
  margin-right: 20rpx;
}

.hot-name {
  flex: 1;
  font-size: 28rpx;
  color: #333;
}

.hot-sales {
  font-size: 24rpx;
  color: #909399;
}

.empty-tip {
  padding: 24rpx;
  text-align: center;
  color: #909399;
  font-size: 26rpx;
  background-color: $card;
  border-radius: 20rpx;
  margin-bottom: 30rpx;
}

.entry-list {
  background-color: $card;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.entry-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 24rpx;
  border-bottom: 1rpx solid #f2f3f5;

  &:last-child { border-bottom: none; }
  &:active { background-color: #f8f9fa; }
}

.entry-left {
  display: flex;
  align-items: center;
}

.entry-new{
  margin-left: 14rpx;
  padding: 2rpx 10rpx;
  border-radius: 999rpx;
  background: rgba(255, 106, 0, 0.12);
  border: 1rpx solid rgba(255, 106, 0, 0.28);
  display:flex;
  align-items:center;
  justify-content:center;
  text{
    font-size: 20rpx;
    font-weight: 800;
    color: #ff6a00;
  }
}

.entry-icon {
  font-size: 36rpx;
  margin-right: 20rpx;
}

.entry-text {
  font-size: 28rpx;
  color: #333;
}

.entry-arrow {
  font-size: 24rpx;
  color: #ccc;
}

.bottom-padding {
  height: 40rpx;
}

.ai-popup{
  padding: 18rpx 22rpx 18rpx;
  background:#fff;
  max-height: 82vh;
  display:flex;
  flex-direction:column;
}
.ai-popup-hd{
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap: 16rpx;
  padding-bottom: 12rpx;
  border-bottom: 1rpx solid #f2f3f5;
}
.ai-popup-title{
  font-size: 30rpx;
  font-weight: 800;
  color:#1a1a1a;
}
.ai-popup-time{
  font-size: 22rpx;
  color:#909399;
}
.ai-popup-scroll{
  height: 55vh;
}
.ai-popup-body{
  padding: 14rpx 4rpx 4rpx;
  font-size: 26rpx;
  color:#303133;
  line-height: 1.7;
}
.ai-popup-ft{
  display:flex;
  justify-content:flex-end;
  gap: 16rpx;
  padding-top: 12rpx;
}
</style>
