<template>
  <view class="wallet-page">
    <u-navbar
      title="店铺钱包"
      :border="false"
      :placeholder="true"
      :autoBack="true"
      bgColor="#ff943c"
      titleStyle="color:#fff;font-weight:bold;"
      leftIconColor="#fff"
    ></u-navbar>

    <!-- 固定：余额 + Tab，不参与滚动 -->
    <view class="wallet-fixed">
      <view class="balance-card">
        <view class="card-bg-decoration"></view>
        <view class="balance-content">
          <text class="label">店铺可入账余额 (元)</text>
          <view class="amount-row">
            <text class="symbol">￥</text>
            <text class="value">{{ balanceDisplay }}</text>
          </view>
          <text class="wallet-emoji">👛</text>
        </view>
        <view class="order-count-badge" v-if="paidOrderCount > 0">
          <text>已计入订单 {{ paidOrderCount }} 笔</text>
        </view>
      </view>

      <view class="tabs-box">
        <u-tabs
          :list="typelist"
          keyName="title"
          @change="onTabChange"
          lineColor="#ff943c"
          lineWidth="30"
          :activeStyle="{ color: '#ff943c', fontWeight: 'bold' }"
          :inactiveStyle="{ color: '#909399' }"
        ></u-tabs>
      </view>
    </view>

    <!-- 仅订单列表滚动（独立高度，避免整页 calc 与 vh/rpx 混算导致下半截空白） -->
    <scroll-view scroll-y class="list-scroll" :enable-flex="true" :show-scrollbar="true">
      <view class="list-wrapper">
        <view class="empty-state" v-if="fobjlist.length === 0">
          <text class="empty-icon">📂</text>
          <text class="empty-text">暂无相关订单记录</text>
        </view>

        <view
          class="order-card"
          v-for="(item, index) in fobjlist"
          :key="index"
          @click="openDetail(item.id)"
        >
          <view class="card-header">
            <view class="order-time">
              <text class="time-label">下单时间：</text>
              <text class="time-value">{{ item.ndate }}</text>
            </view>
            <view class="status-tag">
              {{ item.statecn || '处理中' }}
            </view>
          </view>

          <view class="card-body">
            <view class="goods-img-box">
              <image
                v-if="item.img"
                class="goods-img"
                :src="fileUrl + item.img"
                mode="aspectFill"
              ></image>
              <view v-else class="img-placeholder">🥡</view>
            </view>

            <view class="goods-info">
              <view class="goods-name">{{ item.gnames }}</view>
              <view class="goods-desc">共 {{ getCount(item.gnames) }} 件商品 · {{ item.way || '堂食' }}</view>
              <view class="goods-desc" v-if="item.ydate">预约：{{ item.ydate }}</view>
              <view class="goods-desc" v-if="item.user">用户：{{ item.user }}</view>
            </view>

            <view class="arrow-right">
              <u-icon name="arrow-right" color="#DCDFE6" size="16"></u-icon>
            </view>
          </view>

          <view class="card-footer">
            <view class="price-info">
              <text class="total-label">实付</text>
              <text class="currency">¥</text>
              <text class="price-value">{{ item.total || '0.00' }}</text>
            </view>
            <view class="action-btns">
              <view class="btn-outline">查看详情</view>
            </view>
          </view>
        </view>
      </view>

      <view class="safe-bottom"></view>
    </scroll-view>
  </view>
</template>

<script>
import { listj, savej, fileUrl } from '@/common/config/api.js'
import { mapState, mapActions } from 'vuex'

const PAID_STATES = ['已付款', '已完成', '已评价']

export default {
  data() {
    return {
      merchantBalance: 0,
      paidOrderCount: 0,
      syncing: false,
      fileUrl,
      fobjlist: [],
      tabtitle: null,
      typelist: [
        { title: '全部' }, { title: '已付款' }, { title: '已完成' }
      ]
    }
  },
  computed: {
    ...mapState(['userInfo']),
    balanceDisplay() {
      const n = Number(this.merchantBalance) || 0
      return Number.isInteger(n) ? n : n.toFixed(2)
    }
  },
  onShow() {
    if (!this.userInfo || !this.userInfo.id) {
      uni.showToast({ title: '请先登录', icon: 'none' })
      return
    }
    if (!this.userInfo.sid) {
      uni.showToast({ title: '非商家账号', icon: 'none' })
      return
    }
    this.syncBalanceFromOrders()
  },
  methods: {
    ...mapActions(['updateUserInfo']),
    syncBalanceFromOrders() {
      if (this.syncing) return
      const sid = this.userInfo && this.userInfo.sid
      const uid = this.userInfo && this.userInfo.id
      if (!sid || !uid) return
      this.syncing = true
      listj({
        params: { table: 'bill', sid, sort: 'id', order: 'desc' }
      })
        .then(res => {
          const arr = res || []
          const paid = arr.filter(b =>
            PAID_STATES.includes(String(b.statecn || '').trim())
          )
          let sum = 0
          paid.forEach(b => {
            sum += Number(b.total) || 0
          })
          const balanceInt = Math.round(sum)
          this.merchantBalance = balanceInt
          this.paidOrderCount = paid.length
          this.refreshBill()
          return savej({
            params: {
              table: 'user',
              id: uid,
              money: balanceInt
            }
          }).then(() => {
            this.updateUserInfo({
              ...this.userInfo,
              money: balanceInt
            })
          })
        })
        .catch(() => {
          uni.showToast({ title: '同步失败', icon: 'none' })
        })
        .finally(() => {
          this.syncing = false
        })
    },
    onTabChange(item) {
      this.tabtitle = item.title === '全部' ? null : item.title
      this.refreshBill()
    },
    refreshBill() {
      if (!this.userInfo || !this.userInfo.sid) {
        this.fobjlist = []
        return
      }
      const params = {
        table: 'bill',
        sid: String(this.userInfo.sid),
        sort: 'id',
        order: 'desc'
      }
      listj({ params }).then(res => {
        let arr = res || []
        if (this.tabtitle) {
          arr = arr.filter(it => String(it.statecn || '') === String(this.tabtitle))
        }
        this.fobjlist = arr
      }).catch(() => {
        this.fobjlist = []
      })
    },
    openDetail(bid) {
      uni.navigateTo({ url: '/pages/bill/billdetailshop?bid=' + bid })
    },
    getCount(gnames) {
      if (!gnames) return 0
      return String(gnames).split(',').filter(Boolean).length
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #ff943c;
$bg-color: #f7f9fc;
$text-main: #2d3436;
$text-sub: #95a5a6;

.wallet-page {
  background-color: $bg-color;
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  overflow: hidden;
}

.wallet-fixed {
  flex-shrink: 0;
}

.list-scroll {
  flex: 1;
  min-height: 0;
  height: 0;
  width: 100%;
}

/* 余额卡片 */
.balance-card {
  margin: 30rpx;
  height: 320rpx;
  background: linear-gradient(135deg, #ff943c 0%, #ffb366 100%);
  border-radius: 40rpx;
  position: relative;
  overflow: hidden;
  box-shadow: 0 15rpx 30rpx rgba(255, 148, 60, 0.25);

  .card-bg-decoration {
    position: absolute;
    width: 300rpx;
    height: 300rpx;
    background: rgba(255, 255, 255, 0.12);
    border-radius: 50%;
    top: -100rpx;
    right: -100rpx;
  }

  .balance-content {
    position: relative;
    z-index: 2;
    padding: 60rpx;
    display: flex;
    flex-direction: column;

    .label {
      color: rgba(255, 255, 255, 0.9);
      font-size: 26rpx;
      margin-bottom: 20rpx;
    }

    .amount-row {
      display: flex;
      align-items: baseline;
      color: #ffffff;
      .symbol {
        font-size: 40rpx;
        font-weight: bold;
        margin-right: 10rpx;
      }
      .value {
        font-size: 80rpx;
        font-weight: bold;
        letter-spacing: 2rpx;
      }
    }

    .wallet-emoji {
      position: absolute;
      bottom: 40rpx;
      right: 40rpx;
      font-size: 80rpx;
      opacity: 0.35;
    }
  }

  .order-count-badge {
    position: absolute;
    bottom: 24rpx;
    left: 50%;
    transform: translateX(-50%);
    background: rgba(255, 255, 255, 0.22);
    color: #fff;
    font-size: 24rpx;
    font-weight: 600;
    padding: 8rpx 28rpx;
    border-radius: 30rpx;
    z-index: 3;
    white-space: nowrap;
  }
}

/* Tab 栏 */
.tabs-box {
  background-color: #ffffff;
  padding: 10rpx 0;
  margin: 0 30rpx;
  border-radius: 20rpx 20rpx 0 0;
}

/* 订单列表 */
.list-wrapper {
  background-color: $bg-color;
  padding: 20rpx 30rpx;
}

.order-card {
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.03);
  transition: all 0.2s ease;
  &:active {
    transform: scale(0.98);
    background-color: #fafafa;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 24rpx;
    border-bottom: 1rpx solid #f2f2f2;
    margin-bottom: 24rpx;
    .order-time {
      font-size: 24rpx;
      .time-label { color: $text-sub; }
      .time-value { color: $text-main; font-weight: 500; }
    }
    .status-tag {
      font-size: 24rpx;
      color: $primary;
      font-weight: bold;
      background: rgba($primary, 0.1);
      padding: 4rpx 16rpx;
      border-radius: 8rpx;
    }
  }

  .card-body {
    display: flex;
    align-items: center;
    .goods-img-box {
      width: 120rpx;
      height: 120rpx;
      border-radius: 16rpx;
      overflow: hidden;
      background-color: #f8f9fa;
      margin-right: 24rpx;
      flex-shrink: 0;
      .goods-img { width: 100%; height: 100%; }
      .img-placeholder {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 60rpx;
      }
    }

    .goods-info {
      flex: 1;
      margin-right: 20rpx;
      overflow: hidden;
      .goods-name {
        font-size: 30rpx;
        color: $text-main;
        font-weight: 600;
        margin-bottom: 12rpx;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .goods-desc { font-size: 24rpx; color: $text-sub; }
    }
  }

  .card-footer {
    margin-top: 30rpx;
    display: flex;
    justify-content: space-between;
    align-items: center;
    .price-info {
      display: flex;
      align-items: baseline;
      .total-label { font-size: 24rpx; color: $text-sub; margin-right: 8rpx; }
      .currency { font-size: 22rpx; color: $text-main; font-weight: bold; }
      .price-value { font-size: 36rpx; color: $text-main; font-weight: bold; margin-left: 4rpx; }
    }
    .btn-outline {
      font-size: 24rpx;
      color: $primary;
      border: 1rpx solid $primary;
      background: rgba($primary, 0.08);
      padding: 10rpx 24rpx;
      border-radius: 30rpx;
    }
  }
}

.empty-state {
  padding-top: 120rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  .empty-icon { font-size: 100rpx; margin-bottom: 20rpx; opacity: 0.5; }
  .empty-text { font-size: 28rpx; color: $text-sub; }
}

.safe-bottom {
  height: 60rpx;
}
</style>
