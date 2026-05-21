<template>
  <view class="page-container">
    <u-navbar
      title="我的订单"
      :border="false"
      :autoBack="false"
      @leftClick="goBack"
      :placeholder="true"
      bgColor="#ffffff"
      titleStyle="font-weight: bold; color: #1a1a1a;"
      :rightText="navRightText"
      @rightClick="toggleManageMode"
    ></u-navbar>

    <view class="tabs-box">
      <u-tabs
        :list="typelist"
        keyName="title"
        :current="currentTab"
        @change="onTabChange"
        lineColor="#ff943c"
        lineWidth="30"
        :activeStyle="{ color: '#ff943c', fontWeight: 'bold' }"
        :inactiveStyle="{ color: '#909399' }"
      ></u-tabs>
    </view>

    <view class="bill-tan-tip">
      订单状态为「已完成」后，可在对应店铺发布探店动态；探店计划状态需在计划页自行维护，将计划设为「已完成」时需在本店已有「已完成」订单。
    </view>

    <scroll-view scroll-y :class="['sv-container', managing && fobjlist.length ? 'sv-container--bar' : '']">
      <view class="list-wrapper">
        <view class="empty-state" v-if="fobjlist.length === 0">
          <text class="empty-icon">📂</text>
          <text class="empty-text">暂无相关订单记录</text>
        </view>

        <view
          class="order-card"
          :class="{ 'order-card--managing': managing }"
          v-for="item in fobjlist"
          :key="item.id"
          @tap="onCardTap(item)"
        >
          <view v-if="managing" class="chk-wrap" @tap.stop="toggleRowSelect(item)">
            <view class="chk" :class="{ 'chk--on': isRowSelected(item.id) }"></view>
          </view>
          <view class="order-card-body">
            <view class="card-header">
              <view class="order-time">
                <text class="time-label">下单时间：</text>
                <text class="time-value">{{ item.ndate }}</text>
              </view>
              <view class="status-tag">
                {{ item.statecn }}
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
              </view>

              <view v-if="!managing" class="arrow-right">
                <u-icon name="arrow-right" color="#DCDFE6" size="16"></u-icon>
              </view>
            </view>

            <view class="card-footer">
              <view class="price-info">
                <text class="total-label">实付</text>
                <text class="currency">¥</text>
                <text class="price-value">{{ item.total || '0.00' }}</text>
              </view>
              <view v-if="!managing" class="action-btns">
                <view class="btn-outline" @tap.stop="fobjDetail(item.id)">查看详情</view>
                <view
                  v-if="String(item.statecn || '') === '已完成'"
                  class="btn-review"
                  @tap.stop="toReviewOrder(item)"
                >去评价</view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="safe-area-bottom"></view>
    </scroll-view>

    <view v-if="managing && fobjlist.length" class="manage-bar safe-area-inset-bottom">
      <text class="manage-bar-all" @tap="toggleSelectAll">{{ selectAllLabel }}</text>
      <view class="manage-bar-del" :class="{ disabled: selectedIds.length === 0 }" @tap="confirmRemoveFromList">
        <text>从列表移除</text>
        <text v-if="selectedIds.length">({{ selectedIds.length }})</text>
      </view>
    </view>
  </view>
</template>

<script>
import { listSqlj, savej, fileUrl } from '@/common/config/api.js'
import { billStateCode, billStateLabel } from '@/common/billState.js'
import { mapState, mapActions } from 'vuex'

export default {
  data() {
    return {
      fileUrl: fileUrl,
      fobjlist: [],
      tabtitle: null,
      currentTab: 0,
      typelist: [{ title: '全部' }, { title: '已付款' }, { title: '已完成' }],
      managing: false,
      selectedIds: []
    }
  },
  onLoad() {
    if (!this.userInfo || !this.userInfo.id) {
      uni.ytool.toLogin()
    }
  },
  onShow() {
    this.refreshBill()
  },
  computed: {
    ...mapState(['userInfo']),
    navRightText() {
      if (!this.fobjlist.length) return ''
      return this.managing ? '完成' : '管理'
    },
    selectAllLabel() {
      if (!this.fobjlist.length) return '全选'
      return this.selectedIds.length === this.fobjlist.length ? '取消全选' : '全选'
    }
  },
  methods: {
    ...mapActions(['updateUserInfo']),
    normalizeId(id) {
      return id === undefined || id === null ? '' : String(id)
    },
    isRowSelected(id) {
      return this.selectedIds.indexOf(this.normalizeId(id)) !== -1
    },
    toggleManageMode() {
      if (!this.fobjlist.length) return
      this.managing = !this.managing
      if (!this.managing) this.selectedIds = []
    },
    toggleSelectAll() {
      if (!this.fobjlist.length) return
      if (this.selectedIds.length === this.fobjlist.length) {
        this.selectedIds = []
      } else {
        this.selectedIds = this.fobjlist.map((x) => this.normalizeId(x.id))
      }
    },
    toggleRowSelect(item) {
      const k = this.normalizeId(item && item.id)
      if (!k) return
      const i = this.selectedIds.indexOf(k)
      if (i === -1) this.selectedIds.push(k)
      else this.selectedIds.splice(i, 1)
    },
    onCardTap(item) {
      if (this.managing) {
        this.toggleRowSelect(item)
        return
      }
      this.fobjDetail(item.id)
    },
    onTabChange(e) {
      this.managing = false
      this.selectedIds = []
      const idx = typeof e.index === 'number' ? e.index : 0
      this.currentTab = idx
      const title = (this.typelist[idx] && this.typelist[idx].title) ? this.typelist[idx].title : '全部'
      this.tabtitle = title === '全部' ? null : title
      this.refreshBill()
    },
    refreshBill() {
      if (!this.userInfo || !this.userInfo.id) {
        this.fobjlist = []
        return
      }
      uni.showLoading({ title: '加载中' })
      const uidEsc = String(this.userInfo.id).replace(/'/g, "''")
      let stateWhere = ''
      if (this.tabtitle) {
        const code = billStateCode(this.tabtitle)
        if (code != null) {
          stateWhere = ` AND state = ${code} `
        }
      }
      const sql = `SELECT * FROM fs_bill WHERE uid = ${uidEsc} AND IFNULL(user_delete, 0) = 0 ${stateWhere} ORDER BY id DESC`
      listSqlj({ params: { sql } })
        .then((res) => {
          this.fobjlist = (res || []).map((row) => ({
            ...row,
            statecn: billStateLabel(row.state) || row.statecn || ''
          }))
          uni.hideLoading()
        })
        .catch(() => {
          this.fobjlist = []
          uni.hideLoading()
        })
    },
    confirmRemoveFromList() {
      if (!this.selectedIds.length) {
        uni.showToast({ title: '请先选择订单', icon: 'none' })
        return
      }
      const n = this.selectedIds.length
      uni.showModal({
        title: '从列表移除',
        content: `已选 ${n} 单将不在「我的订单」中显示；换设备登录后仍不显示。不会删除订单数据。`,
        success: (res) => {
          if (!res.confirm) return
          this.persistUserDelete()
        }
      })
    },
    persistUserDelete() {
      const ids = this.selectedIds
        .map((id) => String(id).replace(/\D/g, ''))
        .filter(Boolean)
      if (!ids.length) return
      uni.showLoading({ title: '保存中' })
      const chain = ids.reduce(
        (p, bid) =>
          p.then(() => savej({ params: { table: 'bill', id: bid, user_delete: 1 } })),
        Promise.resolve()
      )
      chain
        .then(() => {
          uni.hideLoading()
          uni.showToast({ title: '已移除', icon: 'success' })
          this.selectedIds = []
          this.managing = false
          this.refreshBill()
        })
        .catch(() => {
          uni.hideLoading()
          uni.showToast({ title: '保存失败，请重试', icon: 'none' })
        })
    },
    fobjDetail(bid) {
      uni.navigateTo({
        url: '/pages/bill/billdetail?bid=' + bid
      })
    },
    toReviewOrder(item) {
      const gids = String(item.gids || '')
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean)
      if (!gids.length) {
        uni.showToast({ title: '暂无商品信息', icon: 'none' })
        return
      }
      const gid = gids[0]
      uni.navigateTo({
        url: '/pages/good/gooddetail-2?gid=' + gid + '&showpl=1'
      })
    },
    goBack() {
      if (this.managing) {
        this.managing = false
        this.selectedIds = []
        return
      }
      const pages = getCurrentPages()
      if (pages && pages.length > 1) {
        uni.navigateBack({ delta: 1 })
        return
      }
      uni.switchTab({ url: '/pages/me/me' })
    },
    getCount(gnames) {
      if (!gnames) return 0
      return gnames.split(',').length
    }
  }
}
</script>

<style lang="scss" scoped>
$bg-color: #f7f8fa;
$text-main: #2d3436;
$text-sub: #95a5a6;
$primary: #ff943c;
$danger: #ff4757;

.page-container {
  background-color: $bg-color;
  min-height: 100vh;
}

.tabs-box {
  background-color: #ffffff;
  padding: 10rpx 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.bill-tan-tip {
  font-size: 24rpx;
  color: #909399;
  line-height: 1.5;
  padding: 16rpx 28rpx 8rpx;
  background: #f7f8fa;
}

.sv-container {
  height: calc(100vh - 180rpx);
}

.sv-container--bar {
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.manage-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #eee;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
}

.manage-bar-all {
  font-size: 28rpx;
  color: #333;
}

.manage-bar-del {
  font-size: 28rpx;
  color: #fff;
  background: #ff3b30;
  padding: 16rpx 40rpx;
  border-radius: 40rpx;
}

.manage-bar-del.disabled {
  opacity: 0.45;
}

.list-wrapper {
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

      .time-label {
        color: $text-sub;
      }
      .time-value {
        color: $text-main;
        font-weight: 500;
      }
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

      .goods-img {
        width: 100%;
        height: 100%;
      }

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

      .goods-desc {
        font-size: 24rpx;
        color: $text-sub;
      }
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

      .total-label {
        font-size: 24rpx;
        color: $text-sub;
        margin-right: 8rpx;
      }
      .currency {
        font-size: 22rpx;
        color: $text-main;
        font-weight: bold;
      }
      .price-value {
        font-size: 36rpx;
        color: $text-main;
        font-weight: bold;
        margin-left: 4rpx;
      }
    }

    .action-btns {
      display: flex;
      align-items: center;
      gap: 16rpx;
    }

    .btn-outline {
      font-size: 24rpx;
      color: $primary;
      border: 1rpx solid $primary;
      background: rgba($primary, 0.08);
      padding: 10rpx 24rpx;
      border-radius: 30rpx;
    }

    .btn-review {
      font-size: 24rpx;
      color: $primary;
      border: 1rpx solid $primary;
      padding: 10rpx 24rpx;
      border-radius: 30rpx;
      background: rgba($primary, 0.06);
    }
  }
}

.order-card--managing {
  display: flex;
  align-items: flex-start;

  &:active {
    transform: none;
  }
}

.chk-wrap {
  flex-shrink: 0;
  padding: 8rpx 16rpx 0 0;
}

.chk {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  border: 2rpx solid #ccc;
  box-sizing: border-box;
}

.chk--on {
  border-color: #ff943c;
  background-color: #ff943c;
  box-shadow: inset 0 0 0 6rpx #fff;
}

.order-card-body {
  flex: 1;
  min-width: 0;
}

.empty-state {
  padding-top: 200rpx;
  display: flex;
  flex-direction: column;
  align-items: center;

  .empty-icon {
    font-size: 100rpx;
    margin-bottom: 20rpx;
    opacity: 0.5;
  }

  .empty-text {
    font-size: 28rpx;
    color: $text-sub;
  }
}

.safe-area-bottom {
  height: calc(env(safe-area-inset-bottom) + 40rpx);
}
</style>
