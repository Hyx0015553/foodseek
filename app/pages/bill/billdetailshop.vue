<template>
  <view class="page-container">
    <u-navbar
      title="订单详情"
      :border="false"
      :placeholder="true"
      @leftClick="goBack"
      :autoBack="false"
      bgColor="#ffffff"
      titleStyle="font-weight: bold;"
    ></u-navbar>

    <scroll-view scroll-y class="sv-container">
      <!-- 取餐号与状态进度（与用户端一致） -->
      <view class="pickup-card">
        <view class="pickup-head">
          <text class="small">取餐号</text>
          <text class="tag">{{ statusText }}</text>
        </view>
        <view class="pickup-no">{{ pickupNo }}</view>
        <view class="steps">
          <view class="step" :class="{ active: stepIndex >= 0 }">
            <text class="dot">●</text>
            <text class="label">已下单</text>
          </view>
          <view class="line"></view>
          <view class="step" :class="{ active: stepIndex >= 1 }">
            <text class="dot">●</text>
            <text class="label">待取餐</text>
          </view>
          <view class="line"></view>
          <view class="step" :class="{ active: stepIndex >= 2 }">
            <text class="dot">●</text>
            <text class="label">已完成</text>
          </view>
        </view>
        <view class="tip">商家端：请核对取餐号与用户订单信息</view>
      </view>

      <view class="card shop-card">
        <view class="shop-row">
          <view class="shop-name">📍 {{ fobj.shop || '店铺' }}</view>
          <view class="shop-time">{{ fobj.ndate }}</view>
        </view>
      </view>

      <view class="card goods-card">
        <view class="section-title">菜品信息</view>
        <view
          v-for="(item, index) in fobjgoodlist"
          :key="index"
          class="good-item"
          @click="toGoodDetail(item.id, 1)"
        >
          <image class="good-img" :src="item.img ? fileUrl + item.img : ''" mode="aspectFill"></image>
          <view class="good-info">
            <view class="good-name">{{ item.gname }}</view>
            <view class="good-desc">x{{ item._count || 1 }}</view>
          </view>
          <view class="good-price">¥{{ item.price }}</view>
        </view>
        <view v-if="!fobjgoodlist.length" class="empty-goods">暂无菜品明细（请检查订单 gids）</view>
        <view class="summary-line">
          <text>合计</text>
          <text class="sum-price">¥{{ fobj.total || 0 }}</text>
        </view>
      </view>

      <view class="card info-card">
        <view class="section-title">订单信息</view>
        <view class="info-line"><text>订单号</text><text>{{ fobj.id || '—' }}</text></view>
        <view class="info-line"><text>取餐号</text><text>{{ pickupNo }}</text></view>
        <view class="info-line"><text>订单状态</text><text>{{ statusText }}</text></view>
        <view class="info-line"><text>下单用户</text><text>{{ fobj.user || '—' }}</text></view>
        <view class="info-line" v-if="fobj.uid"><text>用户ID</text><text>{{ fobj.uid }}</text></view>
        <view class="info-line"><text>用餐方式</text><text>{{ wayDisplay }}</text></view>
        <view class="info-line" v-if="fobj.ydate"><text>预约时间</text><text>{{ fobj.ydate }}</text></view>
        <view class="info-line" v-if="fobj.note"><text>订单备注</text><text>{{ fobj.note }}</text></view>
        <view class="info-line" v-if="couponDiscountDisplay">
          <text>优惠金额</text>
          <text class="discount-val">-¥{{ couponDiscountDisplay }}</text>
        </view>
        <view class="info-line" v-if="fobj.yhqid && fobj._yhq">
          <text>优惠券</text>
          <text>{{ fobj._yhq.fulluse ? '满减券 满' + fobj._yhq.fulluse + '元减' + fobj._yhq.total + '元' : '优惠卷 直减' + fobj._yhq.total + '元' }}</text>
        </view>
      </view>

      <view class="safe-bottom"></view>
    </scroll-view>

    <view class="footer-actions">
      <u-button
        v-if="fobj.statecn === '已付款'"
        type="error"
        plain
        size="small"
        text="取消订单"
        @click="changeBillStatecn('已取消')"
        customStyle="width: 180rpx; margin-right: 16rpx;"
      ></u-button>
      <u-button
        v-if="fobj.statecn === '已付款'"
        type="primary"
        size="small"
        text="标记已完成"
        @click="changeBillStatecn('已完成')"
        customStyle="width: 200rpx; margin-right: 16rpx;"
      ></u-button>
      <u-button
        v-if="fobj.statecn === '已完成' || fobj.statecn === '已评价' || fobj.statecn === '已取消'"
        type="primary"
        plain
        size="small"
        text="返回本店订单"
        @click="goBack"
        customStyle="width: 220rpx;"
      ></u-button>
    </view>
  </view>
</template>

<script>
import { findj, savej, listSqlj, listj, fileUrl } from '@/common/config/api.js'
import { yewuutil } from '@/common/commontools.js'
import { mapState } from 'vuex'

export default {
  data() {
    return {
      fobj: {},
      fobjgoodlist: [],
      fileUrl
    }
  },
  onLoad(params) {
    const bid = params && (params.bid != null && params.bid !== '' ? params.bid : params.id)
    if (bid != null && bid !== '') {
      this.loadDetail(String(bid))
      return
    }
    if (!this.userInfo || !this.userInfo.sid) {
      uni.showToast({ title: '请先登录商家账号', icon: 'none' })
      setTimeout(() => uni.itool.nto({ url: '/pages/login/login' }), 600)
      return
    }
    listj({
      params: {
        table: 'bill',
        sid: String(this.userInfo.sid),
        sort: 'id',
        order: 'desc'
      }
    })
      .then(res => {
        const arr = res || []
        if (!arr.length) {
          uni.showToast({ title: '暂无本店订单', icon: 'none' })
          setTimeout(() => this.goBack(), 700)
          return
        }
        this.loadDetail(String(arr[0].id))
      })
      .catch(() => {
        uni.showToast({ title: '订单加载失败', icon: 'none' })
      })
  },
  methods: {
    loadDetail(bid) {
      if (!bid) {
        uni.showToast({ title: '订单参数无效', icon: 'none' })
        return
      }
      findj({ params: { table: 'bill', id: bid } })
        .then(res => {
          const row = res || {}
          if (!row.id) {
            uni.showToast({ title: '未找到订单', icon: 'none' })
            return
          }
          if (this.userInfo && this.userInfo.sid && String(row.sid || '') !== String(this.userInfo.sid)) {
            uni.showToast({ title: '非本店订单', icon: 'none' })
            setTimeout(() => this.goBack(), 800)
            return
          }
          this.fobj = row
          // 如果订单使用了优惠券，加载优惠券信息
          if (this.fobj.yhqid) {
            findj({ params: { table: 'youhuiquan', id: this.fobj.yhqid } }).then(yhq => {
              this.fobj._yhq = yhq
            }).catch(() => {})
          }
          const gids = (this.fobj.gids || '').toString().trim()
          if (!gids) {
            this.fobjgoodlist = []
            return
          }
          const sql = `select * from fs_good where id in (${gids})`
          listSqlj({ params: { sql } })
            .then(glist => {
              const counts = this.buildCountMap(this.fobj.gids, this.fobj.gnames)
              this.fobjgoodlist = (glist || []).map(g => ({
                ...g,
                _count: counts[String(g.id)] || 1
              }))
            })
            .catch(() => {
              this.fobjgoodlist = []
            })
        })
        .catch(() => {
          uni.showToast({ title: '订单详情加载失败', icon: 'none' })
        })
    },
    buildCountMap(gids, gnames) {
      const gidArr = (gids || '').toString().split(',').map(s => s.trim()).filter(Boolean)
      const nameArr = (gnames || '').toString().split(',').map(s => s.trim())
      const re = /\*(\d+)\s*$/
      const map = {}
      gidArr.forEach((gid, i) => {
        const line = nameArr[i] || ''
        const m = line.match(re)
        let c = m ? parseInt(m[1], 10) : 1
        if (isNaN(c) || c < 1) c = 1
        map[gid] = c
      })
      return map
    },
    changeBillStatecn(statecn) {
      if (!this.fobj || !this.fobj.id) return
      uni.showModal({
        title: '提示',
        content: `确认将订单状态改为「${statecn}」？`,
        success: r => {
          if (!r.confirm) return
          savej({ params: { table: 'bill', id: this.fobj.id, statecn } }).then(() => {
            this.fobj.statecn = statecn
            uni.showToast({ title: '操作成功', icon: 'none' })
          })
        }
      })
    },
    goBack() {
      const pages = getCurrentPages()
      if (pages && pages.length > 1) {
        uni.navigateBack({ delta: 1 })
        return
      }
      uni.itool.nto({ url: '/pages/qianbao/qianbao-m' })
    },
    toGoodDetail: yewuutil.toGoodDetail
  },
  computed: {
    pickupNo() {
      const id = Number(this.fobj.id || 0)
      if (!id) return 'A0000'
      return 'A' + String(2000 + id).slice(-4)
    },
    statusText() {
      return this.fobj.statecn || '处理中'
    },
    stepIndex() {
      if (this.fobj.statecn === '已完成' || this.fobj.statecn === '已评价') return 2
      if (this.fobj.statecn === '已付款') return 1
      return 0
    },
    wayDisplay() {
      const w = String(this.fobj.way || '').trim()
      if (w === '堂食' || w === '外带') return w
      if (w === '到店') return '堂食'
      return w || '堂食'
    },
    couponDiscountDisplay() {
      const y = this.fobj && this.fobj._yhq
      if (!y || y.total == null || y.total === '') return ''
      const n = Number(y.total)
      if (!Number.isFinite(n) || n <= 0) return ''
      return Number.isInteger(n) ? String(n) : String(Number(n.toFixed(2)))
    },
    ...mapState(['userInfo'])
  }
}
</script>

<style lang="scss" scoped>
.page-container {
  background: #f6f7f9;
  min-height: 100vh;
}

.sv-container {
  height: calc(100vh - 180rpx);
  padding: 20rpx 24rpx;
}

.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.pickup-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  text-align: center;
}

.pickup-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  .small {
    color: #999;
    font-size: 24rpx;
  }
  .tag {
    color: #e11d48;
    font-size: 24rpx;
    font-weight: 700;
  }
}

.pickup-no {
  margin-top: 10rpx;
  font-size: 68rpx;
  font-weight: 900;
  color: #e11d48;
  letter-spacing: 4rpx;
}

.steps {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #c0c4cc;
  width: 150rpx;
  .dot {
    font-size: 20rpx;
    line-height: 1;
  }
  .label {
    margin-top: 8rpx;
    font-size: 22rpx;
  }
  &.active {
    color: #303133;
    font-weight: 700;
  }
}

.line {
  width: 50rpx;
  height: 2rpx;
  background: #e5e7eb;
}

.tip {
  margin-top: 14rpx;
  color: #909399;
  font-size: 22rpx;
}

.shop-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.shop-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
}
.shop-time {
  font-size: 22rpx;
  color: #9ca3af;
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 16rpx;
}

.good-item {
  display: flex;
  align-items: center;
  padding: 14rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
  &:last-of-type {
    border-bottom: none;
  }
}

.good-img {
  width: 92rpx;
  height: 92rpx;
  border-radius: 12rpx;
  background: #f3f4f6;
}

.good-info {
  flex: 1;
  margin-left: 16rpx;
  .good-name {
    font-size: 28rpx;
    color: #111827;
  }
  .good-desc {
    margin-top: 6rpx;
    font-size: 24rpx;
    color: #9ca3af;
  }
}

.good-price {
  font-size: 30rpx;
  font-weight: 700;
  color: #111827;
}

.empty-goods {
  font-size: 26rpx;
  color: #9ca3af;
  padding: 20rpx 0;
}

.summary-line {
  margin-top: 10rpx;
  display: flex;
  justify-content: space-between;
  font-size: 28rpx;
  color: #374151;
  .sum-price {
    font-size: 36rpx;
    font-weight: 800;
    color: #111827;
  }
}

.info-line {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12rpx;
  font-size: 26rpx;
  color: #6b7280;
  text:last-child {
    color: #111827;
    max-width: 420rpx;
    text-align: right;
  }
  .discount-val {
    color: #ff4d4f;
    font-weight: 600;
  }
  &.single {
    display: block;
    text {
      color: #111827;
      max-width: 100%;
    }
  }
}

.footer-row {
  display: flex;
  gap: 20rpx;
  margin-top: 16rpx;
  flex-wrap: wrap;
}

.footer-actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  box-shadow: 0 -2rpx 16rpx rgba(0, 0, 0, 0.05);
}

.safe-bottom {
  height: 140rpx;
}
</style>
