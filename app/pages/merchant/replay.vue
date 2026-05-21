<template>
  <view class="page-container">
    <u-navbar
      title="评价管理"
      :border="true"
      :placeholder="true"
      :autoBack="false"
      :rightText="navRightText"
      @rightClick="toggleManageMode"
      @leftClick="onNavLeftClick"
    ></u-navbar>
    <view class="tabs-box">
      <view class="tabs-row">
        <view class="tabs-inner">
          <u-tabs
            :list="tabList"
            keyName="title"
            :current="currentTab"
            lineColor="#ff943c"
            lineWidth="30"
            :activeStyle="{ color: '#ff943c', fontWeight: 'bold' }"
            :inactiveStyle="{ color: '#909399' }"
            @change="onTabChange"
          ></u-tabs>
        </view>
      </view>
    </view>
    <scroll-view scroll-y :class="['main-scroll', managing && list.length ? 'main-scroll--bar' : '']">
      <view v-if="list.length === 0" class="empty-box">
        <text class="empty-icon">💬</text>
        <text class="empty-text">{{ emptyText }}</text>
      </view>
      <view v-else class="comment-list">
        <view
          class="comment-item"
          :class="{ 'comment-item--managing': managing }"
          v-for="item in list"
          :key="item.id"
          @tap="onCommentTap(item)"
        >
          <view v-if="managing" class="chk-wrap" @tap.stop="toggleRowSelect(item)">
            <view class="chk" :class="{ 'chk--on': isRowSelected(item.id) }"></view>
          </view>
          <view class="comment-item-body">
            <view class="comment-user">
              <view class="user-avatar">{{ item.username ? item.username.charAt(0) : 'U' }}</view>
              <view class="user-meta">
                <text class="user-name">{{ item.username || '匿名用户' }}</text>
                <text class="comment-date">{{ item.ndate }}</text>
              </view>
              <view v-if="!managing" class="comment-right">
                <u-button
                  :type="item.appealStatecn === '待处理' ? 'success' : 'warning'"
                  size="mini"
                  :text="item.appealStatecn === '待处理' ? '申诉中' : '申诉'"
                  :disabled="item.appealStatecn === '待处理'"
                  @click.stop="openAppeal(item)"
                ></u-button>
              </view>
            </view>
            <view v-if="item.gname" class="comment-good">菜品：{{ item.gname }}</view>
            <view class="comment-content">{{ item.note }}</view>
            <view v-if="item.appealStatecn" class="appeal-state">
              申诉状态：{{ item.appealStatecn }}
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <view v-if="managing && list.length" class="manage-bar safe-area-inset-bottom">
      <text class="manage-bar-all" @tap="toggleSelectAll">{{ selectAllLabel }}</text>
      <view class="manage-bar-del" :class="{ disabled: selectedIds.length === 0 }" @tap="confirmHideSelected">
        <text>从列表移除</text>
        <text v-if="selectedIds.length">({{ selectedIds.length }})</text>
      </view>
    </view>

    <u-popup :show="appealShow" mode="center" round="20" @close="appealShow = false">
      <view class="appeal-dialog">
        <view class="appeal-title">评价申诉</view>
        <view class="appeal-origin-box" v-if="currentReplay">
          <text class="appeal-label">原评价：</text>
          <text class="appeal-origin-text">{{ currentReplay.note }}</text>
        </view>
        <u--textarea
          v-model="appealReason"
          height="140"
          placeholder="请填写申诉理由，例如评价内容与实际情况不符等"
          maxlength="300"
          count
        ></u--textarea>
        <view class="appeal-actions">
          <u-button type="info" text="取消" size="small" @click="appealShow = false"></u-button>
          <u-button type="primary" text="提交申诉" size="small" @click="submitAppeal"></u-button>
        </view>
      </view>
    </u-popup>
    <ai-assistant-float />
  </view>
</template>

<script>
import { listSqlj, savej } from '@/common/config/api.js'
import { APPEAL_STATE, appealStateLabel } from '@/common/appealState.js'
import { mapState } from 'vuex'

export default {
  data() {
    return {
      list: [],
      currentTab: 0,
      tabList: [
        { title: '所有评价', key: 'all' },
        { title: '申诉中', key: 'appeal_ing' },
        { title: '申诉结果', key: 'appeal_result' }
      ],
      appealShow: false,
      appealReason: '',
      currentReplay: null,
      managing: false,
      selectedIds: []
    }
  },
  onShow() {
    const t = (this.$mp && this.$mp.query && this.$mp.query.tab)
      ? String(this.$mp.query.tab)
      : ((this.$route && this.$route.query && this.$route.query.tab) ? String(this.$route.query.tab) : '')
    if (t) {
      const idx = this.tabList.findIndex((x) => x && x.key === t)
      this.currentTab = idx >= 0 ? idx : this.currentTab
    }
    this.loadList()
  },
  computed: {
    ...mapState(['userInfo']),
    navRightText() {
      if (!this.list.length) return ''
      return this.managing ? '完成' : '管理'
    },
    selectAllLabel() {
      if (!this.list.length) return '全选'
      return this.selectedIds.length === this.list.length ? '取消全选' : '全选'
    },
    emptyText() {
      const key = (this.tabList[this.currentTab] && this.tabList[this.currentTab].key)
        ? this.tabList[this.currentTab].key
        : 'all'
      if (key === 'appeal_ing') return '暂无申诉中的评价'
      if (key === 'appeal_result') return '暂无申诉结果记录'
      return '当前店铺还没有评价'
    }
  },
  methods: {
    onNavLeftClick() {
      if (this.managing) {
        this.managing = false
        this.selectedIds = []
        return
      }
      uni.navigateBack({ fail: () => {} })
    },
    normalizeId(id) {
      return id === undefined || id === null ? '' : String(id)
    },
    isRowSelected(id) {
      return this.selectedIds.indexOf(this.normalizeId(id)) !== -1
    },
    toggleManageMode() {
      if (!this.list.length) return
      this.managing = !this.managing
      if (!this.managing) this.selectedIds = []
    },
    toggleSelectAll() {
      if (!this.list.length) return
      if (this.selectedIds.length === this.list.length) {
        this.selectedIds = []
      } else {
        this.selectedIds = this.list.map((x) => this.normalizeId(x.id))
      }
    },
    toggleRowSelect(item) {
      const k = this.normalizeId(item && item.id)
      if (!k) return
      const i = this.selectedIds.indexOf(k)
      if (i === -1) this.selectedIds.push(k)
      else this.selectedIds.splice(i, 1)
    },
    onCommentTap(item) {
      if (!this.managing) return
      this.toggleRowSelect(item)
    },
    onTabChange(e) {
      this.managing = false
      this.selectedIds = []
      this.currentTab = typeof e.index === 'number' ? e.index : 0
      this.loadList()
    },
    loadList() {
      if (!this.userInfo || !this.userInfo.sid) {
        this.list = []
        return
      }
      const sidEsc = String(this.userInfo.sid).replace(/'/g, "''")
      let appealWhere = ''
      const key = (this.tabList[this.currentTab] && this.tabList[this.currentTab].key)
        ? this.tabList[this.currentTab].key
        : 'all'
      if (key === 'appeal_ing') appealWhere = ` AND a.state = ${APPEAL_STATE.PENDING} `
      else if (key === 'appeal_result') {
        appealWhere = ` AND (a.state = ${APPEAL_STATE.SUCCESS} OR a.state = ${APPEAL_STATE.REJECTED}) `
      }
      const sql = `SELECT r.*, a.state AS appealState, g.gname AS gname
        FROM fs_replay r
        LEFT JOIN fs_replay_appeal a ON a.rid = r.id
        LEFT JOIN fs_good g ON r.type = 1 AND r.pid = g.id
        WHERE (
          (r.type = 9 AND r.pid = '${sidEsc}')
          OR (r.type = 1 AND g.sid IS NOT NULL AND g.sid = '${sidEsc}')
        )
        AND IFNULL(r.merchant_delete, 0) = 0
        ${appealWhere}
        ORDER BY r.ndate DESC`
      listSqlj({ params: { sql } })
        .then((res) => {
          this.list = (res || []).map((row) => ({
            ...row,
            appealStatecn: appealStateLabel(row.appealState) || row.appealStatecn || ''
          }))
        })
        .catch(() => {
          this.list = []
        })
    },
    confirmHideSelected() {
      if (!this.selectedIds.length) {
        uni.showToast({ title: '请先选择评价', icon: 'none' })
        return
      }
      const n = this.selectedIds.length
      uni.showModal({
        title: '从列表移除',
        content: `已选 ${n} 条将不在本页显示；换设备登录后仍不显示。不会删除 fs_replay 记录。`,
        success: (res) => {
          if (!res.confirm) return
          this.persistMerchantDelete()
        }
      })
    },
    persistMerchantDelete() {
      if (!this.selectedIds.length) return
      const ids = this.selectedIds
        .map((id) => String(id).replace(/\D/g, ''))
        .filter(Boolean)
      if (!ids.length) return
      uni.showLoading({ title: '保存中' })
      const chain = ids.reduce(
        (p, rid) =>
          p.then(() => savej({ params: { table: 'replay', id: rid, merchant_delete: 1 } })),
        Promise.resolve()
      )
      chain
        .then(() => {
          uni.hideLoading()
          uni.showToast({ title: '已移除', icon: 'success' })
          this.selectedIds = []
          this.managing = false
          this.loadList()
        })
        .catch(() => {
          uni.hideLoading()
          uni.showToast({ title: '保存失败，请重试', icon: 'none' })
        })
    },
    openAppeal(item) {
      if (!this.userInfo || !this.userInfo.sid) {
        uni.showToast({ title: '请先登录商家账号', icon: 'none' })
        return
      }
      this.currentReplay = item
      this.appealReason = ''
      this.appealShow = true
    },
    submitAppeal() {
      if (!this.currentReplay) {
        return
      }
      const reason = (this.appealReason || '').trim()
      if (!reason) {
        uni.showToast({ title: '请先填写申诉理由', icon: 'none' })
        return
      }
      const params = {
        table: 'replay_appeal',
        rid: this.currentReplay.id,
        sid: this.userInfo.sid,
        shop: this.userInfo.shopname || this.userInfo.username || '',
        reason: reason,
        statecn: '待处理'
      }
      savej({ params })
        .then(() => {
          uni.showToast({ title: '申诉已提交', icon: 'none' })
          this.appealShow = false
          this.loadList()
        })
        .catch(() => {
          uni.showToast({ title: '申诉提交失败，请稍后重试', icon: 'none' })
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container {
  background-color: #f6f7f9;
  min-height: 100vh;
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

.main-scroll {
  height: calc(100vh - 88rpx);
}

.main-scroll--bar {
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.manage-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 20;
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

.comment-list {
  padding: 20rpx 24rpx 40rpx;
}

.comment-item {
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx 24rpx 20rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.03);
}

.comment-item--managing {
  display: flex;
  align-items: flex-start;
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

.comment-item-body {
  flex: 1;
  min-width: 0;
}

.comment-user {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}

.user-avatar {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  background-color: #ffefe0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  color: #ff7a45;
  margin-right: 16rpx;
}

.user-meta {
  display: flex;
  flex-direction: column;
}

.comment-right {
  margin-left: auto;
}

.user-name {
  font-size: 28rpx;
  color: #333333;
}

.comment-date {
  font-size: 22rpx;
  color: #999999;
}

.comment-good {
  font-size: 24rpx;
  color: #666;
  margin-bottom: 8rpx;
}

.comment-content {
  font-size: 26rpx;
  color: #444444;
  line-height: 1.6;
}

.appeal-state {
  margin-top: 10rpx;
  font-size: 22rpx;
  color: #fa8c16;
}

.appeal-dialog {
  width: 600rpx;
  padding: 30rpx;
}

.appeal-title {
  font-size: 32rpx;
  font-weight: bold;
  margin-bottom: 20rpx;
  text-align: center;
}

.appeal-origin-box {
  background-color: #f7f7f7;
  border-radius: 12rpx;
  padding: 12rpx 16rpx;
  margin-bottom: 16rpx;
}

.appeal-label {
  font-size: 24rpx;
  color: #999999;
}

.appeal-origin-text {
  font-size: 24rpx;
  color: #666666;
  margin-top: 4rpx;
  display: block;
}

.appeal-actions {
  margin-top: 20rpx;
  display: flex;
  justify-content: flex-end;
  gap: 20rpx;
}
</style>
