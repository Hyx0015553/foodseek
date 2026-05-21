<template>
  <view class="page-container">
    <u-navbar title="消息通知" :border="true" :placeholder="true" :autoBack="true"></u-navbar>

    <view class="tabs-box">
      <view class="tabs-row">
        <view class="tabs-inner">
          <u-tabs
            :list="tabList"
            keyName="title"
            lineColor="#ff943c"
            lineWidth="30"
            :activeStyle="{ color: '#ff943c', fontWeight: 'bold' }"
            :inactiveStyle="{ color: '#909399' }"
            @change="onTabChange"
          ></u-tabs>
        </view>
        <view class="manage-entry" @click="openManageMenu">
          <text>管理</text>
        </view>
      </view>
    </view>

    <!-- 订单：本店待处理订单提醒（数据来自 bill） -->
    <scroll-view
      v-show="currentTab === 0"
      scroll-y
      :class="['main-scroll', manageMode ? 'main-scroll--manage' : '']"
      :enable-flex="true"
    >
      <view v-if="orderBillRows.length === 0 && orderSysmsgRows.length === 0" class="empty-box">
        <text class="empty-icon">📦</text>
        <text class="empty-text">暂无待处理订单提醒</text>
        <text class="empty-sub">有新订单待处理时将在此提示「您有新的订单」</text>
      </view>
      <view v-else class="msg-list">
        <view
          class="msg-item order-card"
          v-for="row in orderBillRows"
          :key="row.id"
          @tap="onOrderBillRowTap(row)"
        >
          <view v-if="manageMode" class="chk-wrap">
            <view class="chk" :class="{ 'chk--on': isMsgKeySelected(msgKey('bill', row.id)) }"></view>
          </view>
          <view class="msg-item-main">
          <view class="msg-header">
            <view class="left-title">
              <text class="msg-title">订单提醒</text>
              <text class="msg-tag torder">订单</text>
            </view>
            <text class="msg-time">{{ row.ndate }}</text>
          </view>
          <view class="msg-plain">您有新的订单</view>
          <view class="msg-shop" v-if="row.gnames">
            {{ row.gnames }}
          </view>
          <view class="msg-footer-row">
            <text class="state-pill">{{ row.statecn || '处理中' }}</text>
            <text class="go-text">去处理</text>
            <u-icon name="arrow-right" size="14" color="#ff943c"></u-icon>
          </view>
          </view>
        </view>

        <view
          class="msg-item"
          v-for="item in orderSysmsgRows"
          :key="item.id"
          @tap="onSysmsgRowTap(item)"
        >
          <view v-if="manageMode" class="chk-wrap">
            <view class="chk" :class="{ 'chk--on': isMsgKeySelected(msgKey('sysmsg', item.id)) }"></view>
          </view>
          <view class="msg-item-main">
          <view class="msg-header">
            <view class="left-title">
              <text class="msg-title">{{ item.title }}</text>
              <text class="msg-tag torder">{{ tagText(item) }}</text>
              <view class="dot" v-if="isUnread(item)"></view>
            </view>
            <text class="msg-time">{{ item.ndate }}</text>
          </view>
          <view class="msg-shop" v-if="item.shop">所属店铺：{{ item.shop }}</view>
          <view class="msg-content">
            <u-parse :content="formatNote(item.note)" />
          </view>
          <view class="msg-action" v-if="canOpenQa(item)">
            <u-button
              type="primary"
              size="mini"
              shape="circle"
              text="去问答区回复"
              @click.stop="onSysmsgButtonOpen(item)"
            ></u-button>
          </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 互动：问答类（原页面中的问答类消息） -->
    <scroll-view
      v-show="currentTab === 1"
      scroll-y
      :class="['main-scroll', manageMode ? 'main-scroll--manage' : '']"
      :enable-flex="true"
    >
      <view v-if="qaList.length === 0" class="empty-box">
        <text class="empty-icon">💬</text>
        <text class="empty-text">暂无互动消息</text>
      </view>
      <view v-else class="msg-list">
        <view class="msg-item" v-for="item in qaList" :key="item.id" @tap="onSysmsgRowTap(item)">
          <view v-if="manageMode" class="chk-wrap">
            <view class="chk" :class="{ 'chk--on': isMsgKeySelected(msgKey('sysmsg', item.id)) }"></view>
          </view>
          <view class="msg-item-main">
          <view class="msg-header">
            <view class="left-title">
              <text class="msg-title">{{ item.title }}</text>
              <text class="msg-tag tqa">{{ tagText(item) }}</text>
              <view class="dot" v-if="isUnread(item)"></view>
            </view>
            <text class="msg-time">{{ item.ndate }}</text>
          </view>
          <view class="msg-shop" v-if="item.shop">所属店铺：{{ item.shop }}</view>
          <view class="msg-content">
            <u-parse :content="formatNote(item.note)" />
          </view>
          <view class="msg-action" v-if="canOpenQa(item)">
            <u-button
              type="primary"
              size="mini"
              shape="circle"
              text="去问答区回复"
              @click.stop="onSysmsgButtonOpen(item)"
            ></u-button>
          </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 系统：系统类消息 -->
    <scroll-view
      v-show="currentTab === 2"
      scroll-y
      :class="['main-scroll', manageMode ? 'main-scroll--manage' : '']"
      :enable-flex="true"
    >
      <view v-if="systemList.length === 0" class="empty-box">
        <text class="empty-icon">📭</text>
        <text class="empty-text">暂无系统消息</text>
      </view>
      <view v-else class="msg-list">
        <view class="msg-item" v-for="item in systemList" :key="item.id" @tap="onSysmsgRowTap(item)">
          <view v-if="manageMode" class="chk-wrap">
            <view class="chk" :class="{ 'chk--on': isMsgKeySelected(msgKey('sysmsg', item.id)) }"></view>
          </view>
          <view class="msg-item-main">
          <view class="msg-header">
            <view class="left-title">
              <text class="msg-title">{{ item.title }}</text>
              <text class="msg-tag tsystem">{{ tagText(item) }}</text>
              <view class="dot" v-if="isUnread(item)"></view>
            </view>
            <text class="msg-time">{{ item.ndate }}</text>
          </view>
          <view class="msg-shop" v-if="item.shop">所属店铺：{{ item.shop }}</view>
          <view class="msg-content">
            <u-parse :content="formatNote(item.note)" />
          </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <view v-if="manageMode" class="manage-bar safe-area-inset-bottom">
      <text class="manage-bar-done" @tap="exitManage">完成</text>
      <view class="manage-bar-del" :class="{ disabled: selectedKeys.length === 0 }" @tap="confirmDeleteSelected">
        <text>删除</text>
        <text v-if="selectedKeys.length">({{ selectedKeys.length }})</text>
      </view>
    </view>
    <ai-assistant-float />
  </view>
</template>

<script>
import { listj, savej, deletej } from '@/common/config/api.js'
import { mapState } from 'vuex'

import { BILL_STATE, isPaidBillState } from '@/common/billState.js'

const ORDER_END_STATES = ['已完成', '已评价', '已取消']

export default {
  data() {
    return {
      currentTab: 0,
      tabList: [{ title: '订单' }, { title: '互动' }, { title: '系统' }],
      list: [],
      orderBillRows: [],
      localReadMap: {},
      manageMode: false,
      /** bill|id 或 sysmsg|id */
      selectedKeys: []
    }
  },
  onShow() {
    this.loadLocalReadMap()
    this.loadList()
    this.loadOrderBills()
  },
  methods: {
    onTabChange(e) {
      if (this.manageMode) {
        this.exitManage()
      }
      this.currentTab = typeof e.index === 'number' ? e.index : 0
    },
    openManageMenu() {
      uni.showActionSheet({
        itemList: ['一键已读', '选择消息删除'],
        success: (res) => {
          if (res.tapIndex === 0) {
            this.onMarkAllRead()
          } else if (res.tapIndex === 1) {
            this.manageMode = true
            this.selectedKeys = []
          }
        }
      })
    },
    exitManage() {
      this.manageMode = false
      this.selectedKeys = []
    },
    msgKey(table, id) {
      return `${table}|${id == null ? '' : String(id)}`
    },
    isMsgKeySelected(key) {
      return this.selectedKeys.indexOf(key) !== -1
    },
    toggleMsgKey(key) {
      const ix = this.selectedKeys.indexOf(key)
      if (ix === -1) {
        this.selectedKeys = [...this.selectedKeys, key]
      } else {
        this.selectedKeys = this.selectedKeys.filter((_, j) => j !== ix)
      }
    },
    onOrderBillRowTap(row) {
      if (!row || row.id == null) return
      if (this.manageMode) {
        this.toggleMsgKey(this.msgKey('bill', row.id))
        return
      }
      this.openBillDetail(row)
    },
    onSysmsgRowTap(item) {
      if (!item || item.id == null) return
      if (this.manageMode) {
        this.toggleMsgKey(this.msgKey('sysmsg', item.id))
        return
      }
      this.onOpen(item)
    },
    onSysmsgButtonOpen(item) {
      if (this.manageMode) return
      this.onOpen(item)
    },
    merchantBillHidden(b) {
      if (!b) return false
      const v = b.merchantmsghide != null ? b.merchantmsghide : b.merchantMsgHide
      return String(v || '') === '1'
    },
    confirmDeleteSelected() {
      if (!this.selectedKeys.length) {
        uni.showToast({ title: '请选择要删除的消息', icon: 'none' })
        return
      }
      uni.showModal({
        title: '确认删除',
        content: '删除后再次登录也不会显示所选消息，确定删除？',
        success: (res) => {
          if (!res.confirm) return
          uni.showLoading({ title: '处理中' })
          const chain = this.selectedKeys.reduce(
            (p, key) => p.then(() => this.deleteOneMessageKey(key)),
            Promise.resolve()
          )
          chain
            .then(() => {
              uni.hideLoading()
              uni.showToast({ title: '已删除', icon: 'success' })
              this.exitManage()
              this.loadList()
              this.loadOrderBills()
            })
            .catch(() => {
              uni.hideLoading()
              uni.showToast({ title: '部分删除失败', icon: 'none' })
              this.exitManage()
              this.loadList()
              this.loadOrderBills()
            })
        }
      })
    },
    deleteOneMessageKey(key) {
      const bar = String(key).indexOf('|')
      if (bar < 0) return Promise.resolve()
      const tbl = key.slice(0, bar)
      const id = key.slice(bar + 1)
      if (!id) return Promise.resolve()
      if (tbl === 'bill') {
        return savej({ params: { table: 'bill', id, merchantMsgHide: '1' } })
      }
      if (tbl === 'sysmsg') {
        return deletej({ params: { table: 'sysmsg', id } })
      }
      return Promise.resolve()
    },
    /** 未读 sysmsg 一键已读（订单 Tab 的 bill 列表无未读态） */
    onMarkAllRead() {
      let n = 0
      ;(this.list || []).forEach((item) => {
        if (!item || !this.isUnread(item)) return
        n++
        this.markRead(item)
      })
      if (!n) {
        uni.showToast({ title: '暂无未读消息', icon: 'none' })
        return
      }
      uni.showToast({ title: '已全部标记为已读', icon: 'success' })
    },
    formatNote(note) {
      const raw = (note || '').toString()
      return raw.replace(/\n/g, '<br/>')
    },
    loadLocalReadMap() {
      try {
        this.localReadMap = uni.getStorageSync('merchant_sysmsg_read_map') || {}
      } catch (e) {
        this.localReadMap = {}
      }
    },
    saveLocalRead(id) {
      if (!id) return
      this.localReadMap = {
        ...this.localReadMap,
        [String(id)]: 1
      }
      try {
        uni.setStorageSync('merchant_sysmsg_read_map', this.localReadMap)
      } catch (e) {}
    },
    markRead(item) {
      if (!item) return
      item.state = 0
      if (item.id) this.saveLocalRead(item.id)
      this.list = [...this.list]
      if (item.id) {
        savej({ params: { table: 'sysmsg', id: item.id, state: 0 } }).catch(() => {})
      }
    },
    loadList() {
      if (!this.userInfo || !this.userInfo.sid) {
        this.list = []
        return
      }
      listj({ params: { table: 'sysmsg', sid: this.userInfo.sid } }).then(res => {
        const arr = (res || []).map(item => {
          if (item && item.id && this.localReadMap[String(item.id)]) {
            item.state = 0
          }
          return item
        })
        this.list = arr.sort((a, b) => (b.ndate || '').localeCompare(a.ndate || ''))
      }).catch(err => {
        this.list = []
      })
    },
    /** 本店待处理订单，用于「订单」Tab 提醒（无需新表） */
    loadOrderBills() {
      if (!this.userInfo || !this.userInfo.sid) {
        this.orderBillRows = []
        return
      }
      listj({
        params: { table: 'bill', sid: this.userInfo.sid, sort: 'id', order: 'desc' }
      }).then(res => {
        const arr = res || []
        this.orderBillRows = arr.filter(
          (b) => {
            const st = b.state != null ? Number(b.state) : null
            const ended = st != null
              ? (st === BILL_STATE.COMPLETED || st === BILL_STATE.REVIEWED || st === BILL_STATE.CANCELLED)
              : ORDER_END_STATES.includes(String(b.statecn || '').trim())
            return !ended && !this.merchantBillHidden(b)
          }
        )
      }).catch(() => {
        this.orderBillRows = []
      })
    },
    openBillDetail(row) {
      if (!row || !row.id) return
      uni.navigateTo({
        url: '/pages/bill/billdetailshop?bid=' + row.id,
        fail: () => {
          if (uni && uni.itool && typeof uni.itool.nto === 'function') {
            uni.itool.nto({ url: '/pages/bill/billdetailshop?bid=' + row.id })
          }
        }
      })
    },
    inferType(item) {
      if (!item) return 'system'
      const bt = String(item.biztype || '')
      const ty = String(item.type || '').toLowerCase()
      // 后台写入的 biztype：平台 AI/预警类必须进「系统」Tab，勿用正文关键词判断（正文中常出现「问答数量」「订单」等）
      if (
        bt === 'admin_ai_ops_decision' ||
        bt === 'admin_ai_ops_advice' ||
        bt === 'admin_low_pf_warn'
      ) {
        return 'system'
      }
      if (ty === 'system') return 'system'
      const title = String(item.title || '')
      // 互动：仅标题或明确 biztype，避免全文扫描把 AI 长文里的「问答」误判为互动
      if (bt === 'shop_qa_question' || title.indexOf('提问') !== -1 || title.indexOf('问答') !== -1) {
        return 'qa'
      }
      const full = `${title}${item.note || ''}`
      if (title.indexOf('订单') !== -1 || full.indexOf('您有新的订单') !== -1) return 'order'
      return 'system'
    },
    tagText(item) {
      const t = this.inferType(item)
      if (t === 'qa') return '问答'
      if (t === 'order') return '订单'
      return '系统'
    },
    isUnread(item) {
      if (item && item.id && this.localReadMap[String(item.id)]) return false
      if (item && item.state !== undefined && item.state !== null) return String(item.state) === '1'
      return String(item && item.type) === '1'
    },
    canOpenQa(item) {
      return this.inferType(item) === 'qa' && (item.sid || (this.userInfo && this.userInfo.sid))
    },
    onOpen(item) {
      if (!item) return
      this.markRead(item)
      if (this.canOpenQa(item)) {
        const sid = item.sid || this.userInfo.sid
        const url = `/pages/shops/shopqa?sid=${sid}&shop=${encodeURIComponent(item.shop || '')}&qid=${item.qid || ''}`
        uni.navigateTo({
          url,
          fail: () => {
            if (uni && uni.itool && typeof uni.itool.nto === 'function') {
              uni.itool.nto({ url })
            } else {
              uni.showToast({ title: '跳转失败，请稍后重试', icon: 'none' })
            }
          }
        })
      }
    }
  },
  computed: {
    ...mapState(['userInfo']),
    qaList() {
      return (this.list || []).filter(it => this.inferType(it) === 'qa')
    },
    systemList() {
      return (this.list || []).filter(it => this.inferType(it) === 'system')
    },
    orderSysmsgRows() {
      return (this.list || []).filter(it => this.inferType(it) === 'order')
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #ff943c;

.page-container {
  background-color: #f6f7f9;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  height: 100vh;
  box-sizing: border-box;
}

.tabs-box {
  flex-shrink: 0;
  background-color: #ffffff;
  padding: 10rpx 0;
}

.tabs-row {
  display: flex;
  align-items: center;
}

.tabs-inner {
  flex: 1;
  min-width: 0;
}

.manage-entry {
  flex-shrink: 0;
  padding: 0 24rpx 0 8rpx;
  align-self: stretch;
  display: flex;
  align-items: center;
  justify-content: center;

  text {
    font-size: 26rpx;
    color: $primary;
    font-weight: 500;
  }
}

.main-scroll {
  flex: 1;
  height: 0;
  min-height: 0;
}

.main-scroll--manage {
  padding-bottom: 140rpx;
  box-sizing: border-box;
}

.msg-item-main {
  flex: 1;
  min-width: 0;
}

.chk-wrap {
  flex-shrink: 0;
  padding-top: 6rpx;
  display: flex;
  align-items: center;
}

.chk {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  border: 2rpx solid #c0c4cc;
  box-sizing: border-box;
}

.chk--on {
  border-color: $primary;
  background: $primary;
  box-shadow: inset 0 0 0 6rpx #fff;
}

.manage-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 28rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #ebeef5;
  box-shadow: 0 -8rpx 24rpx rgba(0, 0, 0, 0.06);
}

.manage-bar-done {
  font-size: 28rpx;
  color: #606266;
  padding: 12rpx 20rpx;
}

.manage-bar-del {
  font-size: 28rpx;
  color: #fff;
  background: #ef4444;
  padding: 16rpx 36rpx;
  border-radius: 999rpx;
  font-weight: 600;
}

.manage-bar-del.disabled {
  opacity: 0.45;
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

  .empty-sub {
    margin-top: 16rpx;
    font-size: 24rpx;
    color: #c0c4cc;
    padding: 0 48rpx;
    text-align: center;
    line-height: 1.5;
  }
}

.msg-list {
  padding: 20rpx 24rpx 40rpx;
}

.msg-item {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx 24rpx 20rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.03);
}

.order-card {
  border: 1rpx solid rgba(255, 148, 60, 0.25);
}

.msg-plain {
  font-size: 28rpx;
  color: #333;
  font-weight: 600;
  margin-bottom: 12rpx;
}

.msg-footer-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8rpx;
  margin-top: 16rpx;
}

.state-pill {
  margin-right: auto;
  font-size: 22rpx;
  color: $primary;
  background: rgba(255, 148, 60, 0.12);
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
}

.go-text {
  font-size: 24rpx;
  color: $primary;
  font-weight: 600;
}

.msg-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;

  .left-title {
    display: flex;
    align-items: center;
    gap: 10rpx;
    position: relative;
  }

  .msg-title {
    font-size: 30rpx;
    font-weight: bold;
    color: #1a1a1a;
  }

  .msg-time {
    font-size: 22rpx;
    color: #999999;
  }
}

.msg-tag {
  font-size: 20rpx;
  line-height: 1;
  padding: 6rpx 10rpx;
  border-radius: 999rpx;
}
.msg-tag.tqa {
  background: #fff0e6;
  color: #ff6a00;
}
.msg-tag.torder {
  background: #e8f0ff;
  color: #2563eb;
}
.msg-tag.tsystem {
  background: #eef0f3;
  color: #6b7280;
}
.dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 999rpx;
  background: #ef4444;
}

.msg-shop {
  font-size: 24rpx;
  color: #666666;
  margin-bottom: 8rpx;
}

.msg-content {
  font-size: 26rpx;
  color: #444444;
}

.msg-action {
  margin-top: 10rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6rpx;
}
</style>
