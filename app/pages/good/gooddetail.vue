<template>
  <view class="detail-page">
    <!-- 导航栏 -->
    <u-navbar title="菜品详情" :border="false" :placeholder="true" :autoBack="true" bgColor="#ffffff" titleStyle="font-weight: bold;"></u-navbar>

    <scroll-view scroll-y class="main-scroll" :enable-flex="true">
      <!-- 顶部大图区域 -->
      <view class="hero-section">
        <image v-if="fobj.img" class="main-img" :src="fileUrl+fobj.img" mode="aspectFill"></image>
        <view v-else class="img-placeholder">🍲</view>
        <view class="fav-badge" @tap="toggleFav">
          <u-icon :name="favtext === '移除收藏' ? 'star-fill' : 'star'" :color="favtext === '移除收藏' ? '#ff9900' : '#fff'" size="22"></u-icon>
        </view>
      </view>

      <!-- 核心信息卡片 -->
      <view class="info-card">
        <view class="price-row">
          <view class="price-box">
            <text class="symbol">¥</text>
            <text class="value">{{ fobj.price }}</text>
          </view>
          <view class="score-box">
            <text class="star">⭐</text>
            <text class="score-num">{{ fobj.pf || '5.0' }}</text>
            <text class="score-label">评分</text>
          </view>
        </view>
        <view class="goods-title">{{ fobj.gname }}</view>
        <view class="tags-row">
          <text class="tag">精心烹饪</text>
          <text class="tag">今日特供</text>
          <text class="tag">健康食材</text>
        </view>
      </view>

      <!-- 详情描述 -->
      <view class="section-container">
        <view class="section-title">菜品详情</view>
        <view class="rich-text-content">
          <u-parse :content="fobj.note" />
        </view>
      </view>

      <!-- 关联店铺 -->
      <view class="section-container related-shop" @click="viewShop" v-if="fobj.sid">
        <view class="shop-entry">
          <view class="shop-icon">🏪</view>
          <view class="shop-info">
            <view class="shop-label">关联店铺</view>
            <view class="shop-name">{{ shopName || '点击查看店铺' }}</view>
          </view>
          <u-icon name="arrow-right" size="16" color="#999"></u-icon>
        </view>
      </view>

      <!-- 评论列表区域（词云保留；筛选/点评表单与 gooddetail-2 一致） -->
      <view class="section-container review-section">
        <view class="section-header">
          <view class="section-title">评价 ({{ replaylist.length }})</view>
          <text v-if="hasReviewFilters" class="filter-reset" @tap="clearReviewFilters">重置筛选</text>
        </view>

        <!-- 评价词云（来源：菜品评价 type=1 pid=gid） -->
        <view class="wc-box">
          <view class="wc-hd">
            <text class="wc-title">评价词云</text>
            <text class="wc-more" @tap="loadGoodWordCloud(true)">{{ wcRefreshing ? '刷新中…' : '刷新' }}</text>
          </view>
          <view class="wc-tabs">
            <view class="wc-tab" :class="{ on: wcDays === 7 }" @tap="wcDays = 7">近7天</view>
            <view class="wc-tab" :class="{ on: wcDays === 30 }" @tap="wcDays = 30">近30天</view>
          </view>
          <view class="wc-wrap" v-if="currentWc.length">
            <replay-word-cloud :key="wcDays" :items="currentWc" />
          </view>
          <view class="empty-reviews wc-empty" v-else>
            <text>暂无有效评价词</text>
          </view>
        </view>

        <view v-if="replaylist.length" class="review-score-strip">
          <view class="rss-main">
            <text class="rss-num">{{ displayPf }}</text>
            <view class="rss-stars">
              <text v-for="n in 5" :key="n" class="rss-star" :class="{ on: n <= Math.round(Number(fobj.pf) || 0) }">★</text>
            </view>
          </view>
          <view class="rss-cols">
            <view class="rss-col"><text class="rss-mini">{{ subScore(0) }}</text><text class="rss-cap">口味</text></view>
            <view class="rss-col"><text class="rss-mini">{{ subScore(1) }}</text><text class="rss-cap">份量</text></view>
            <view class="rss-col"><text class="rss-mini">{{ subScore(2) }}</text><text class="rss-cap">新鲜</text></view>
            <view class="rss-col"><text class="rss-mini">{{ subScore(3) }}</text><text class="rss-cap">性价比</text></view>
          </view>
        </view>

        <view v-if="replaylist.length" class="review-tabs">
          <view class="rt-item" :class="{ active: reviewTab === 'all' }" @tap="reviewTab = 'all'">全部</view>
          <view class="rt-item" :class="{ active: reviewTab === 'good' }" @tap="reviewTab = 'good'">好评 <text class="rt-count">{{ reviewCountGood }}</text></view>
          <view class="rt-item" :class="{ active: reviewTab === 'neutral' }" @tap="reviewTab = 'neutral'">中评 <text class="rt-count">{{ reviewCountNeutral }}</text></view>
          <view class="rt-item" :class="{ active: reviewTab === 'bad' }" @tap="reviewTab = 'bad'">差评 <text class="rt-count">{{ reviewCountBad }}</text></view>
        </view>

        <scroll-view v-if="replaylist.length" scroll-x class="review-sub-scroll" :show-scrollbar="false" :enable-flex="true">
          <view class="review-sub-inner">
            <view class="rs-chip" @tap="openSortSheet">
              <text>{{ sortLabel }}</text>
              <text class="rs-arrow">▼</text>
            </view>
            <view class="rs-chip" :class="{ on: filterMedia }" @tap="toggleFilterMedia">图/视频 <text class="rs-count">{{ reviewCountWithImg }}</text></view>
          </view>
        </scroll-view>

        <view v-if="keywordTagList.length" class="review-keywords">
          <view
            v-for="tag in keywordTagList"
            :key="tag.text"
            class="kw-tag"
            :class="{ on: activeKeyword === tag.text }"
            @tap="toggleKeyword(tag.text)"
          >
            {{ tag.text }} <text class="kw-n">{{ tag.count }}</text>
          </view>
        </view>

        <view v-if="replaylist.length && reviewAiLines.length" class="review-ai-box">
          <view class="rai-head">
            <view class="rai-icon">智</view>
            <text class="rai-title">评价小结</text>
          </view>
          <view v-for="(line, idx) in reviewAiLines" :key="idx" class="rai-line">{{ line }}</view>
        </view>

        <view class="review-wrapper">
          <imglist
            imgName="img"
            :showArrow="false"
            :showSearch="false"
            :dataList="displayedReplayList"
            :imgSize="2"
            titleName="ndate"
            sName="note"
            tName="username"
          ></imglist>
          <view
            v-if="reviewShowExpandToggle"
            class="review-expand-bar"
            @tap.stop="toggleReviewExpand"
          >
            <text class="review-expand-text">{{ reviewExpandAll ? '收起' : ('展开其余 ' + reviewExpandRemainCount + ' 条评价') }}</text>
          </view>
          <view v-if="replaylist.length === 0" class="empty-reviews">
            <text>暂无评价，快来抢首评吧~</text>
          </view>
          <view v-else-if="filteredReplayList.length === 0" class="empty-reviews filter-empty">
            <text>暂无符合当前筛选的评价</text>
            <view class="empty-clear" @tap="clearReviewFilters">清除筛选</view>
          </view>
        </view>

        <view class="review-form" v-if="showpl">
          <view class="form-title">撰写点评</view>
          <view class="form-item">
            <text class="form-label">上传图片</text>
            <imgupload v-model="rimg"></imgupload>
          </view>
          <view class="form-item">
            <text class="form-label">评分</text>
            <tn-rate v-model="pingfen" :size="44" activeColor="#ffca28"></tn-rate>
          </view>
          <view class="form-item">
            <textarea
              v-model="rnote"
              class="custom-textarea"
              placeholder="谈谈口味与就餐体验吧..."
              maxlength="200"
            ></textarea>
          </view>
          <view class="form-btns">
            <view class="btn-cancel" @click="showpl = false">取消</view>
            <view class="btn-submit" @click="replay()">提交点评</view>
          </view>
        </view>
      </view>

      <!-- 底部占位 -->
      <view class="safe-area-bottom"></view>
    </scroll-view>

    <!-- 底部操作栏 - 毛玻璃质感 -->
<!--    <view class="fixed-footer">
      <view class="footer-inner">
        <view class="icon-btns">
          <view class="icon-item" @click="showCar()">
            <view class="badge" v-if="carlist.length">{{ carlist.length }}</view>
            <text class="icon">🛒</text>
            <text class="txt">购物车</text>
          </view>
          <view class="icon-item" @click="toChat">
            <text class="icon">💬</text>
            <text class="txt">客服</text>
          </view>
        </view>
        <view class="main-btns">
          <view class="btn-cart" @click="addCar()">加入购物车</view>
          <view class="btn-buy" @click="goumai()">立即购买</view>
        </view>
      </view>
    </view>-->

    <tn-tips ref="tips"></tn-tips>
  </view>
</template>

<script>
import { savej, listj, findj, fileUrl, goodWordCloudj, rebuildGoodWordCloudj } from '@/common/config/api.js'
import { isShopApproved } from '@/common/shopState.js'
import { mapState, mapActions } from 'vuex'
import { ideautil } from '@/common/commontools.js'

/** 评价列表默认展示条数，超出折叠 */
const REVIEW_PREVIEW_LIMIT = 5

export default {
  data() {
    return {
      gid: null,
      fileUrl: fileUrl,
      favtext: "加入收藏",
      fobj: {},
      replaylist: [],
      rnote: '',
      rimg: '',
      pingfen: 5,
      showpl: false,
      reviewTab: 'all',
      reviewSort: 'default',
      filterMedia: false,
      activeKeyword: '',
      shopName: '',
      wcDays: 30,
      wc7: [],
      wc30: [],
      wcRefreshing: false,
      /** false：列表最多展示 REVIEW_PREVIEW_LIMIT 条 */
      reviewExpandAll: false
    };
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
  onLoad(params) {
    this.gid = params.gid
    if (params.showpl == 1) {
      this.showpl = true
    }
    this.fobjDetail()
  },
  methods: {
    ...mapActions(['setCar']),
    fobjDetail() {
      findj({ params: { table: 'good', id: this.gid } }).then(res => {
        this.fobj = res
        this.fobj.note = ideautil.getHtmlNote(this.fobj.note)
        this.checkFavs()
        this.listReplay()
        this.putHistory()
        if (this.fobj.sid) {
          this.loadShopName()
        }
      })
    },
    loadShopName() {
      findj({ params: { table: 'shop', id: this.fobj.sid } }).then(res => {
        // 商品详情里展示的店铺名称也仅在店铺审核通过时显示
        if (res && isShopApproved(res)) {
          this.shopName = res.sname
        } else {
          this.shopName = '店铺审核中'
        }
      }).catch(err => {})
    },
    viewShop() {
      if (!this.fobj.sid) return
      let url = '/pages/shops/shopdetail?pid=' + this.fobj.sid
      if (this.userInfo && this.userInfo.sid) {
        url = '/pages/shops/shopdetail-m?pid=' + this.fobj.sid
      }
      uni.itool.nto({
        url: url
      })
    },
    putHistory() {
      let id = this.gid
      let hgids = uni.getStorageSync("his_ids")
      if (hgids) {
        let flag = ideautil.checkStrInStr(id, hgids)
        if (!flag) hgids += "," + id
      } else {
        hgids = id
      }
      uni.setStorageSync("his_ids", hgids)
    },
    goumai() {
      if (!this.userInfo || !this.userInfo.id) {
        uni.ytool.toLogin(); return;
      }
      let buyItem = { ...this.fobj, count: 1, checked: true };
      this.carlist.push(buyItem);
      uni.navigateTo({ url: '/pages/bill/surebill' });
    },
    toggleFav() {
      if (!this.userInfo || !this.userInfo.id) {
        uni.ytool.toLogin(); return;
      }
      let myfavs3 = this.userInfo.favs3 || ''
      let isFav = this.favtext === '移除收藏'
      if (isFav) {
        myfavs3 = ideautil.removeStrInStr(this.fobj.id, myfavs3) || "0"
      } else {
        myfavs3 = (myfavs3 && myfavs3 !== '0') ? (myfavs3 + "," + this.fobj.id) : (this.fobj.id + "")
      }
      savej({ params: { table: "user", favs3: myfavs3, id: this.userInfo.id } }).then(res => {
        this.userInfo.favs3 = myfavs3
        this.checkFavs()
        uni.showToast({ title: isFav ? '已取消收藏' : '已收藏', icon: 'none' });
      })
    },
    checkFavs() {
      this.favtext = "加入收藏"
      let myfavs3 = this.userInfo.favs3 || ''
      if (ideautil.checkStrInStr(this.fobj.id + "", myfavs3)) {
        this.favtext = "移除收藏"
      }
    },
    addCar() {
      if (!this.userInfo || !this.userInfo.id) {
        uni.ytool.toLogin(); return;
      }
      let clist = [...this.carlist]
      let gid = this.fobj.id
      let index = clist.findIndex(item => item.id === gid)
      if (index > -1) {
        clist[index].count += 1
      } else {
        clist.push({ ...this.fobj, count: 1, checked: true })
      }
      this.setCar(clist)
      uni.showToast({ title: '已加入购物车', icon: 'success' })
    },
    showCar() {
      if (!this.userInfo || !this.userInfo.id) {
        uni.ytool.toLogin(); return;
      }
      uni.switchTab({ url: '/pages/shopcar/shopcar' })
    },
    goBack() { uni.navigateBack() },
    toChat() {
      findj({ params: { table: 'user', sid: this.fobj.sid } }).then(res => {
        if (res) uni.ytool.toChatUni(res.id)
        else uni.showToast({ title: '客服暂时不在线', icon: 'none' })
      })
    },
    listReplay() {
      listj({ params: { table: 'replay', pid: this.fobj.id, type: 1 } }).then(res => {
        this.replaylist = res.map(s => {
          const pf = Number(s.pf) || 0
          const prefix = pf < 2 ? '差评:' : (pf < 4 ? '中评:' : '好评:')
          return { ...s, noteRaw: s.note, note: prefix + s.note }
        })
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
      const base = Number(this.fobj.pf)
      if (isNaN(base)) return '—'
      const jitter = [0, 0.1, -0.05, 0.05][seed % 4]
      const v = Math.min(5, Math.max(1, base + jitter))
      return v.toFixed(1)
    },
    replay() {
      if (!this.userInfo || !this.userInfo.id) {
        uni.ytool.toLogin()
        return
      }
      if (!this.rnote) {
        uni.showToast({ title: '请输入评价内容', icon: 'none' })
        return
      }
      const fdata = {
        table: 'replay',
        pid: this.fobj.id,
        note: this.rnote,
        uid: this.userInfo.id,
        username: this.userInfo.username,
        type: 1,
        pf: this.pingfen,
        img: this.rimg
      }
      savej({ params: fdata })
        .then(() => {
          this.rnote = ''
          this.rimg = ''
          this.pingfen = 5
          this.listReplay()
          this.showpl = false
          this.$refs.tips.show({ msg: '点评成功!' })
          this.setPf()
        })
        .catch(() => {})
    },
    setPf() {
      listj({ params: { table: 'replay', type: 1, pid: this.fobj.id } }).then(res => {
        if (!res.length) return
        const avgpf = (res.reduce((a, b) => a + b.pf * 1, 0) / res.length).toFixed(1)
        this.fobj.pf = avgpf
        savej({ params: { table: 'good', id: this.fobj.id, pf: avgpf } })
      })
    },
    loadGoodWordCloud(force) {
      const gid = this.gid
      if (!gid) return
      const forceRebuild = !!force
      if (forceRebuild) this.wcRefreshing = true
      const callOne = (days) => {
        const fn = forceRebuild ? rebuildGoodWordCloudj : goodWordCloudj
        return fn({ params: { gid, days, topN: 36, minFreq: 2, force: forceRebuild ? 1 : 0 } })
          .then(res => {
            if (!res || !res.ok || !Array.isArray(res.items)) return []
            return res.items
              .filter(x => x && x.name)
              .slice(0, 36)
              .map((x) => ({ ...x }))
          })
          .catch(() => [])
      }
      Promise.all([callOne(7), callOne(30)]).then(([w7, w30]) => {
        this.wc7 = w7
        this.wc30 = w30
        if (forceRebuild) uni.showToast({ title: '词云已刷新', icon: 'none' })
      }).finally(() => {
        if (forceRebuild) this.wcRefreshing = false
      })
    }
  },
  computed: {
    ...mapState(['carlist', 'userInfo']),
    currentWc() {
      return this.wcDays === 7 ? (this.wc7 || []) : (this.wc30 || [])
    },
    displayPf() {
      const p = Number(this.fobj.pf)
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
    displayedReplayList() {
      const list = this.filteredReplayList
      if (this.reviewExpandAll || list.length <= REVIEW_PREVIEW_LIMIT) return list
      return list.slice(0, REVIEW_PREVIEW_LIMIT)
    },
    reviewShowExpandToggle() {
      return this.filteredReplayList.length > REVIEW_PREVIEW_LIMIT
    },
    reviewExpandRemainCount() {
      return Math.max(0, this.filteredReplayList.length - REVIEW_PREVIEW_LIMIT)
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
</script>

<style lang="scss" scoped>
/* 全局变量与容器 */
.detail-page {
  background-color: #f8f9fb;
  min-height: 100vh;
}

.main-scroll {
  height: calc(100vh - 180rpx); /* 留出底部操作栏位置 */
}

.wc-box{
  margin: 12rpx 0 10rpx;
  padding: 18rpx 18rpx 8rpx;
  background: #ffffff;
  border-radius: 18rpx;
  border: 1rpx solid #f1f5f9;
}
.wc-hd{
  display:flex;
  align-items:center;
  justify-content:space-between;
  margin-bottom: 12rpx;
}
.wc-title{
  font-size: 28rpx;
  font-weight: 800;
  color:#111827;
}
.wc-more{
  font-size: 24rpx;
  color:#ff7a45;
  font-weight: 700;
}
.wc-tabs{
  display:flex;
  gap: 14rpx;
  margin-bottom: 14rpx;
}
.wc-tab{
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  color:#6b7280;
  background:#f3f4f6;
}
.wc-tab.on{
  color:#ff7a45;
  background:#fff3e8;
  font-weight: 800;
}
.wc-wrap{
  display: block;
  width: 100%;
  min-height: 220rpx;
}
.wc-empty{
  padding: 10rpx 0 2rpx;
}

/* 顶部图 */
.hero-section {
  position: relative;
  width: 100%;
  height: 600rpx;
  background-color: #eee;

  .main-img {
    width: 100%;
    height: 100%;
  }

  .img-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 100rpx;
  }

  .fav-badge {
    position: absolute;
    right: 30rpx;
    top: 30rpx;
    width: 80rpx;
    height: 80rpx;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 40rpx;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.1);
  }
}

/* 信息卡片 */
.info-card {
  margin: -40rpx 30rpx 20rpx;
  padding: 40rpx;
  background: #ffffff;
  border-radius: 32rpx;
  box-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.05);
  position: relative;
  z-index: 2;

  .price-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24rpx;

    .price-box {
      color: #ff4d4f;
      .symbol { font-size: 32rpx; font-weight: bold; }
      .value { font-size: 56rpx; font-weight: 800; }
    }

    .score-box {
      display: flex;
      align-items: center;
      background: #fff8e6;
      padding: 8rpx 20rpx;
      border-radius: 40rpx;
      .star { font-size: 24rpx; margin-right: 6rpx; }
      .score-num { color: #ffa000; font-weight: bold; font-size: 32rpx; }
      .score-label { color: #ffa000; font-size: 20rpx; margin-left: 6rpx; }
    }
  }

  .goods-title {
    font-size: 40rpx;
    font-weight: bold;
    color: #1a1a1a;
    line-height: 1.4;
    margin-bottom: 20rpx;
  }

  .tags-row {
    display: flex;
    gap: 16rpx;
    .tag {
      font-size: 22rpx;
      color: #666;
      background: #f0f2f5;
      padding: 4rpx 16rpx;
      border-radius: 8rpx;
    }
  }
}

/* 关联店铺 */
.related-shop {
  .shop-entry {
    display: flex;
    align-items: center;
    padding: 20rpx;
    background: linear-gradient(135deg, #fff7e6, #fff);
    border-radius: 16rpx;
    border: 1rpx solid #ffe4b3;

    .shop-icon {
      font-size: 48rpx;
      margin-right: 20rpx;
    }

    .shop-info {
      flex: 1;

      .shop-label {
        font-size: 24rpx;
        color: #ff9d00;
        margin-bottom: 6rpx;
      }

      .shop-name {
        font-size: 30rpx;
        color: #333;
        font-weight: 500;
      }
    }
  }
}

/* 通用板块样式 */
.section-container {
  margin: 20rpx 30rpx;
  padding: 30rpx;
  background: #ffffff;
  border-radius: 24rpx;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20rpx;
  }

  .section-title {
    font-size: 32rpx;
    font-weight: bold;
    color: #333;
    position: relative;
    padding-left: 20rpx;
    &:before {
      content: '';
      position: absolute;
      left: 0;
      top: 15%;
      height: 70%;
      width: 6rpx;
      background: #ff4d4f;
      border-radius: 4rpx;
    }
  }

}

.rich-text-content {
  line-height: 1.6;
  color: #444;
}

/* 评论区域（与 gooddetail-2 对齐的筛选与表单样式） */
.review-section {
  .filter-reset {
    font-size: 24rpx;
    color: #3c9cff;
  }
}

.review-score-strip {
  padding: 24rpx 0 8rpx;
  margin-bottom: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;

  .rss-main {
    display: flex;
    align-items: center;
    gap: 16rpx;
    margin-bottom: 20rpx;
  }
  .rss-num {
    font-size: 56rpx;
    font-weight: 800;
    color: #ff4d4f;
    line-height: 1;
  }
  .rss-stars {
    display: flex;
    gap: 4rpx;
  }
  .rss-star {
    font-size: 28rpx;
    color: #e0e0e0;
    &.on {
      color: #ff4d4f;
    }
  }
  .rss-cols {
    display: flex;
    justify-content: space-between;
  }
  .rss-col {
    flex: 1;
    text-align: center;
    font-size: 22rpx;
  }
  .rss-mini {
    display: block;
    color: #333;
    font-weight: 600;
    margin-bottom: 6rpx;
  }
  .rss-cap {
    color: #999;
    font-size: 20rpx;
  }
}

.review-tabs {
  display: flex;
  align-items: flex-end;
  gap: 32rpx;
  padding: 8rpx 0 16rpx;
  border-bottom: 1rpx solid #f5f5f5;

  .rt-item {
    position: relative;
    font-size: 30rpx;
    color: #666;
    padding-bottom: 12rpx;
    &.active {
      color: #1a1a1a;
      font-weight: 700;
      &::after {
        content: '';
        position: absolute;
        left: 50%;
        bottom: 0;
        transform: translateX(-50%);
        width: 48rpx;
        height: 6rpx;
        background: #ff4d4f;
        border-radius: 4rpx;
      }
    }
  }
  .rt-count {
    font-size: 24rpx;
    color: #bbb;
    font-weight: 400;
  }
}

.review-sub-scroll {
  width: 100%;
  white-space: nowrap;
  margin-top: 16rpx;
  margin-bottom: 8rpx;
}
.review-sub-inner {
  display: inline-flex;
  align-items: center;
  gap: 16rpx;
  padding: 8rpx 0 12rpx;
}
.rs-chip {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 24rpx;
  background: #f5f6f8;
  border-radius: 40rpx;
  font-size: 24rpx;
  color: #555;
  border: 2rpx solid transparent;
  &.on {
    background: #fff0f0;
    color: #ff4d4f;
    border-color: #ffcdd2;
  }
  .rs-arrow {
    font-size: 20rpx;
    color: #999;
    transform: scale(0.85);
  }
  .rs-count {
    font-size: 22rpx;
    color: #aaa;
  }
  &.on .rs-count {
    color: #ff8787;
  }
}

.review-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  padding: 16rpx 0 8rpx;
}
.kw-tag {
  padding: 10rpx 20rpx;
  background: #f5f6f8;
  border-radius: 12rpx;
  font-size: 24rpx;
  color: #555;
  border: 2rpx solid transparent;
  &.on {
    background: #fff0f0;
    color: #ff4d4f;
    border-color: #ffcdd2;
  }
  .kw-n {
    font-size: 22rpx;
    color: #aaa;
    margin-left: 6rpx;
  }
  &.on .kw-n {
    color: #ff8787;
  }
}

.review-ai-box {
  margin: 16rpx 0 8rpx;
  padding: 20rpx 24rpx;
  background: linear-gradient(135deg, #f0f7ff 0%, #e8f4fc 100%);
  border-radius: 16rpx;
  border: 1rpx solid #d6e8f5;
  .rai-head {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-bottom: 12rpx;
  }
  .rai-icon {
    width: 36rpx;
    height: 36rpx;
    border-radius: 50%;
    background: #3c9cff;
    color: #fff;
    font-size: 20rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
  }
  .rai-title {
    font-size: 26rpx;
    font-weight: 600;
    color: #2c6aa0;
  }
  .rai-line {
    font-size: 24rpx;
    color: #4a6d8c;
    line-height: 1.55;
    margin-top: 8rpx;
  }
}

.review-wrapper {
  .review-expand-bar {
    margin-top: 8rpx;
    padding: 28rpx 0 12rpx;
    text-align: center;
    border-top: 1rpx solid #f0f0f0;
  }
  .review-expand-text {
    font-size: 26rpx;
    color: #1989fa;
  }
  .empty-reviews {
    text-align: center;
    padding: 40rpx 0;
    color: #999;
    font-size: 26rpx;
  }
  .filter-empty .empty-clear {
    margin-top: 20rpx;
    display: inline-block;
    padding: 12rpx 32rpx;
    font-size: 26rpx;
    color: #ff4d4f;
    border: 1rpx solid #ffccc7;
    border-radius: 40rpx;
  }
}

.review-form {
  margin-top: 24rpx;
  padding: 30rpx;
  background: #fff;
  border-radius: 24rpx;
  border: 2rpx solid #eef0f3;

  .form-title {
    font-weight: bold;
    margin-bottom: 30rpx;
    font-size: 30rpx;
  }
  .form-item {
    margin-bottom: 30rpx;
    .form-label {
      font-size: 26rpx;
      color: #666;
      display: block;
      margin-bottom: 12rpx;
    }
  }

  .custom-textarea {
    width: 100%;
    height: 160rpx;
    background: #f7f8fa;
    padding: 20rpx;
    border-radius: 12rpx;
    font-size: 28rpx;
    box-sizing: border-box;
  }

  .form-btns {
    display: flex;
    gap: 20rpx;
    .btn-cancel {
      flex: 1;
      text-align: center;
      padding: 20rpx;
      border-radius: 12rpx;
      background: #f0f2f5;
      color: #666;
      font-size: 28rpx;
    }
    .btn-submit {
      flex: 2;
      text-align: center;
      padding: 20rpx;
      border-radius: 12rpx;
      background: #ff943c;
      color: #fff;
      font-size: 28rpx;
    }
  }
}

/* 底部操作栏 */
.fixed-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);

  .footer-inner {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .icon-btns {
    display: flex;
    gap: 30rpx;
    .icon-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      position: relative;
      .icon { font-size: 40rpx; margin-bottom: 4rpx; }
      .txt { font-size: 20rpx; color: #666; }
      .badge {
        position: absolute;
        top: -6rpx;
        right: -6rpx;
        background: #ff4d4f;
        color: #fff;
        font-size: 20rpx;
        padding: 0 8rpx;
        border-radius: 20rpx;
        min-width: 24rpx;
        text-align: center;
      }
    }
  }

  .main-btns {
    display: flex;
    flex: 1;
    margin-left: 40rpx;
    gap: 0;
    border-radius: 50rpx;
    overflow: hidden;

    .btn-cart {
      flex: 1; background: #ffa000; color: #fff; padding: 24rpx 0;
      text-align: center; font-size: 28rpx; font-weight: bold;
    }
    .btn-buy {
      flex: 1; background: #ff4d4f; color: #fff; padding: 24rpx 0;
      text-align: center; font-size: 28rpx; font-weight: bold;
    }
  }
}

.safe-area-bottom {
  height: 120rpx;
}
</style>