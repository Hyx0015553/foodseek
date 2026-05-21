import { listSqlj } from '@/common/config/api.js'

/**
 * 店铺详情：汇总本店所有菜品评价（fs_replay.type=1，pid=菜品 id，经 fs_good.sid 关联）
 * 与 gooddetail 评价区筛选/展示逻辑一致；不写评价表单（在菜品详情页撰写）
 */
/** 店铺详情评价列表默认展示条数，超出部分折叠 */
export const SHOP_REVIEW_PREVIEW_LIMIT = 5

export default {
  data() {
    return {
      replaylist: [],
      reviewTab: 'all',
      reviewSort: 'default',
      filterMedia: false,
      activeKeyword: '',
      /** false：列表最多展示 SHOP_REVIEW_PREVIEW_LIMIT 条 */
      reviewExpandAll: false
    }
  },
  watch: {
    reviewTab() {
      this.reviewExpandAll = false
    },
    reviewSort() {
      this.reviewExpandAll = false
    },
    filterMedia() {
      this.reviewExpandAll = false
    },
    activeKeyword() {
      this.reviewExpandAll = false
    }
  },
  methods: {
    loadShopDishReplays() {
      const sid = this.fobj && this.fobj.id
      if (!sid) {
        this.replaylist = []
        return Promise.resolve()
      }
      const sidEsc = String(sid).replace(/'/g, "''")
      const sql =
        `SELECT r.*, g.gname AS gname
        FROM fs_replay r
        INNER JOIN fs_good g ON r.type = 1 AND r.pid = g.id
        WHERE g.sid = '${sidEsc}'
        ORDER BY r.ndate DESC`
      return listSqlj({ params: { sql } })
        .then((res) => {
          const rows = res || []
          this.replaylist = rows.map((s) => {
            const pf = Number(s.pf) || 0
            const prefix = pf < 2 ? '差评:' : pf < 4 ? '中评:' : '好评:'
            return { ...s, noteRaw: s.note, note: prefix + s.note }
          })
        })
        .catch(() => {
          this.replaylist = []
        })
    },
    openSortSheet() {
      const labels = ['综合排序', '最新优先', '最早优先', '评分从高到低', '评分从低到高']
      uni.showActionSheet({
        itemList: labels,
        success: (e) => {
          const map = ['default', 'time_desc', 'time_asc', 'pf_desc', 'pf_asc']
          this.reviewSort = map[e.tapIndex] || 'default'
        }
      })
    },
    toggleFilterMedia() {
      this.filterMedia = !this.filterMedia
    },
    toggleKeyword(text) {
      this.activeKeyword = this.activeKeyword === text ? '' : text
    },
    clearReviewFilters() {
      this.reviewTab = 'all'
      this.reviewSort = 'default'
      this.filterMedia = false
      this.activeKeyword = ''
      this.reviewExpandAll = false
    },
    toggleReviewExpand() {
      this.reviewExpandAll = !this.reviewExpandAll
    },
    pfNum(i) {
      return Number(i.pf) || 0
    },
    parseReviewTime(item) {
      const d = item.ndate
      if (!d) return 0
      const t = new Date(d).getTime()
      return isNaN(t) ? 0 : t
    },
    subScore(seed) {
      const base = Number(this.fobj && this.fobj.pf)
      if (isNaN(base)) return '—'
      const jitter = [0, 0.1, -0.05, 0.05][seed % 4]
      const v = Math.min(5, Math.max(1, base + jitter))
      return v.toFixed(1)
    },
    onShopReviewRowClick(rid) {
      const row = (this.replaylist || []).find((x) => String(x.id) === String(rid))
      if (row && row.pid) {
        uni.navigateTo({ url: '/pages/good/gooddetail?gid=' + row.pid })
      }
    }
  },
  computed: {
    displayPf() {
      const p = Number(this.fobj && this.fobj.pf)
      return isNaN(p) ? '—' : p.toFixed(1)
    },
    reviewCountGood() {
      return this.replaylist.filter((i) => this.pfNum(i) >= 4).length
    },
    reviewCountNeutral() {
      return this.replaylist.filter((i) => {
        const p = this.pfNum(i)
        return p >= 2 && p < 4
      }).length
    },
    reviewCountBad() {
      return this.replaylist.filter((i) => this.pfNum(i) < 2).length
    },
    reviewCountWithImg() {
      return this.replaylist.filter((i) => i.img && String(i.img).trim()).length
    },
    sortLabel() {
      const m = {
        default: '综合',
        time_desc: '最新',
        time_asc: '最早',
        pf_desc: '分高',
        pf_asc: '分低'
      }
      return m[this.reviewSort] || '综合'
    },
    keywordTagList() {
      const presets = ['好吃', '推荐', '口味', '份量', '新鲜', '满意', '环境', '服务', '性价比', '下次']
      return presets
        .map((text) => ({
          text,
          count: this.replaylist.filter((i) => (i.noteRaw || '').indexOf(text) !== -1).length
        }))
        .filter((x) => x.count > 0)
        .sort((a, b) => b.count - a.count)
        .slice(0, 8)
    },
    filteredReplayList() {
      let list = this.replaylist.slice()
      if (this.reviewTab === 'good') list = list.filter((i) => this.pfNum(i) >= 4)
      else if (this.reviewTab === 'neutral') {
        list = list.filter((i) => {
          const p = this.pfNum(i)
          return p >= 2 && p < 4
        })
      } else if (this.reviewTab === 'bad') list = list.filter((i) => this.pfNum(i) < 2)
      if (this.filterMedia) list = list.filter((i) => i.img && String(i.img).trim())
      if (this.activeKeyword) {
        const k = this.activeKeyword
        list = list.filter((i) => (i.noteRaw || '').indexOf(k) !== -1)
      }
      const pn = (i) => this.pfNum(i)
      const pt = (i) => this.parseReviewTime(i)
      if (this.reviewSort === 'time_desc') list.sort((a, b) => pt(b) - pt(a))
      else if (this.reviewSort === 'time_asc') list.sort((a, b) => pt(a) - pt(b))
      else if (this.reviewSort === 'pf_desc') list.sort((a, b) => pn(b) - pn(a))
      else if (this.reviewSort === 'pf_asc') list.sort((a, b) => pn(a) - pn(b))
      return list
    },
    /** 评价列表默认最多展示 SHOP_REVIEW_PREVIEW_LIMIT 条，其余折叠 */
    displayedReplayList() {
      const list = this.filteredReplayList
      const lim = SHOP_REVIEW_PREVIEW_LIMIT
      if (this.reviewExpandAll || list.length <= lim) return list
      return list.slice(0, lim)
    },
    reviewShowExpandToggle() {
      return this.filteredReplayList.length > SHOP_REVIEW_PREVIEW_LIMIT
    },
    reviewExpandRemainCount() {
      return Math.max(0, this.filteredReplayList.length - SHOP_REVIEW_PREVIEW_LIMIT)
    },
    hasReviewFilters() {
      return this.reviewTab !== 'all' || this.reviewSort !== 'default' || this.filterMedia || !!this.activeKeyword
    },
    reviewAiLines() {
      const total = this.replaylist.length
      if (!total) return []
      const good = this.reviewCountGood
      const bad = this.reviewCountBad
      const rate = Math.round((good / total) * 100)
      const lines = []
      if (good >= bad * 2 && good >= total * 0.5) {
        lines.push(`约 ${rate}% 用户评价为好评，整体反馈偏正面。`)
      } else if (bad > good) {
        lines.push('近期中差评占比相对较高，建议结合具体评价与图片参考。')
      } else {
        lines.push('好评与建议并存，可用关键词标签快速定位关心的内容。')
      }
      const imgN = this.reviewCountWithImg
      if (imgN > 0) lines.push(`共 ${imgN} 条带图/视频评价，可点「图/视频」筛选查看。`)
      return lines
    }
  }
}
