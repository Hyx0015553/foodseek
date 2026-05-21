
<template>
  <view class="page-container">
    <u-navbar title="消息" :border="true" :placeholder="true" :autoBack="true"></u-navbar>

    <view class="tabs-box">
      <view class="tabs-row">
        <view class="tabs-inner">
          <view class="custom-tabs">
            <view
              class="tab-item"
              :class="{ active: currentTab === 0 }"
              @click="switchTab(0)"
            >
              <text>互动</text>
              <view class="tab-dot" v-if="hasListUnread"></view>
            </view>
            <view
              class="tab-item"
              :class="{ active: currentTab === 1 }"
              @click="switchTab(1)"
            >
              <text>系统</text>
              <view class="tab-dot" v-if="hasSystemUnread"></view>
            </view>
            <view class="tab-line" :style="{ transform: 'translateX(' + (currentTab * 100) + '%)' }"></view>
          </view>
        </view>
        <view class="manage-entry" @click="openManageMenu">
          <text>管理</text>
        </view>
      </view>
    </view>

    <!-- 互动：原 huihua 系统通知（问答回复等） -->
    <scroll-view
      v-show="currentTab === 0"
      scroll-y
      :class="['main-scroll', manageMode ? 'main-scroll--manage' : '']"
      :enable-flex="true"
    >
      <view v-if="list.length === 0" class="empty-box">
        <text class="empty-icon">📭</text>
        <text class="empty-text">暂无互动消息</text>
      </view>
      <view v-else class="msg-list">
        <view class="group" v-for="g in groupedList" :key="g.key">
          <view class="group-title">{{ g.title }}</view>
          <view class="msg-item" v-for="item in g.items" :key="item.id" @tap="onInteractItemTap(item)">
            <view class="row-top">
              <view v-if="manageMode" class="chk-wrap">
                <view class="chk" :class="{ 'chk--on': isMsgKeySelected(msgKey('huihua', item.id)) }"></view>
              </view>
              <view class="left">
                <view class="icon" :class="'t' + getType(item)">
                  <text class="icon-text">{{ getIcon(item) }}</text>
                  <view class="dot" v-if="isUnread(item)"></view>
                </view>
              </view>
              <view class="mid">
                <view class="msg-header">
                  <view class="title-left">
                    <text class="msg-title">{{ getTitle(item) }}</text>
                    <text class="msg-tag" v-if="getTag(item)">{{ getTag(item) }}</text>
                  </view>
                  <text class="msg-time">{{ item.ndate }}</text>
                </view>
                <view class="msg-sub" v-if="getSub(item)">
                  <text>{{ getSub(item) }}</text>
                </view>
                <view class="msg-content">
                  <u-parse :content="getContent(item)" />
                </view>
                <view class="msg-action" v-if="getActionText(item)">
                  <text class="action-text">{{ getActionText(item) }}</text>
                  <u-icon name="arrow-right" size="14" color="#999"></u-icon>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 系统：根据「已完成」订单展示评价提醒（数据来自 bill，无需新表） -->
    <scroll-view
      v-show="currentTab === 1"
      scroll-y
      :class="['main-scroll', manageMode ? 'main-scroll--manage' : '']"
      :enable-flex="true"
    >
      <view v-if="systemList.length === 0 && systemNotifyList.length === 0" class="empty-box">
        <text class="empty-icon">🔔</text>
        <text class="empty-text">暂无系统提醒</text>
        <text class="empty-sub">当有「已完成」且待评价的订单时，将在此提示您去评价</text>
      </view>
      <view v-else class="msg-list">
        <!-- 管理端删除评价提醒 -->
        <view class="group" v-if="groupedSystemNotify && groupedSystemNotify.length > 0">
          <view class="group-title sys-hint">系统通知</view>
          <view
            class="msg-item"
            v-for="item in groupedSystemNotify"
            :key="item._rowKey"
            @tap="onSystemNotifyRowTap(item)"
          >
            <view class="row-top">
              <view v-if="manageMode" class="chk-wrap">
                <view
                  class="chk"
                  :class="{ 'chk--on': isMsgKeySelected(msgKey(item._table || 'system_notify', item.id)) }"
                ></view>
              </view>
              <view class="left">
                <view class="icon tnotify">
                  <text class="icon-text">⚙</text>
                  <view class="dot" v-if="item.type == 1 || item.type === '1'"></view>
                </view>
              </view>
              <view class="mid">
                <view class="msg-header">
                  <view class="title-left">
                    <text class="msg-title">{{ systemNotifyTitle(item) }}</text>
                    <text class="msg-tag sys-tag">系统</text>
                  </view>
                  <text class="msg-time">{{ item.ndate }}</text>
                </view>
                <view class="msg-content sys-plain">
                  <text>{{ systemNotifyBody(item) }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>
        <!-- 订单评价提醒 -->
        <view class="group-title sys-hint" v-if="systemList.length > 0">订单评价提醒</view>
        <view
          class="msg-item sys-card"
          v-for="row in systemList"
          :key="row.id"
          @tap="onSystemBillRowTap(row)"
        >
          <view class="row-top">
            <view v-if="manageMode" class="chk-wrap">
              <view class="chk" :class="{ 'chk--on': isMsgKeySelected(msgKey('bill', row.id)) }"></view>
            </view>
            <view class="left">
              <view class="icon torder">
                <text class="icon-text">评</text>
              </view>
            </view>
            <view class="mid">
              <view class="msg-header">
                <view class="title-left">
                  <text class="msg-title">订单评价提醒</text>
                  <text class="msg-tag sys-tag">系统</text>
                </view>
                <text class="msg-time">{{ row.ndate }}</text>
              </view>
              <view class="msg-sub" v-if="row.shop">
                <text>店铺：{{ row.shop }}</text>
              </view>
              <view class="msg-content sys-plain">
                <text>有已完成订单，快去评价吧</text>
              </view>
              <view class="msg-action">
                <text class="action-text">去评价</text>
                <u-icon name="arrow-right" size="14" color="#ff943c"></u-icon>
              </view>
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
import { listj, savej, findj, deletej } from '@/common/config/api.js'
import { mapState } from 'vuex'

export default {
  data() {
    return {
      currentTab: 0,
      tabList: [
        { title: '互动', dot: false },
        { title: '系统', dot: false }
      ],
      list: [],
      systemList: [],
      /** 系统消息红点：管理端删除评价提醒的未读列表（可扩展） */
      systemNotifyList: [],
      manageMode: false,
      /** 选中项：table|id，如 huihua|12、system_notify|3、bill|127 */
      selectedKeys: []
    }
  },
  onShow() {
    this.loadList()
    this.loadSystemList()
    this.loadSystemNotifyList()
  },
  methods: {
    switchTab(index) {
      if (this.manageMode) {
        this.exitManage()
      }
      this.currentTab = index
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
    onInteractItemTap(item) {
      if (this.manageMode) {
        this.toggleMsgKey(this.msgKey('huihua', item.id))
        return
      }
      this.onOpen(item)
    },
    /** 探店计划提醒正文末尾机器可读段，展示时去掉 */
    stripBlogplanRefSuffix(note) {
      const s = (note || '').toString()
      return s.replace(/\n?__REF:id=\d+(?:,sid=[^_]*)?__\s*$/, '').trim() || '您有一条探店计划即将到店，请提前安排。'
    },
    /** 解析 {@link BlogplanVisitReminderScheduler} 写入的 __REF:id=,sid=__ */
    parseBlogplanNotifyRef(note) {
      const m = String(note || '').match(/__REF:id=(\d+)(?:,sid=([^_]*))?__/)
      if (!m) return null
      return { planId: m[1], sid: (m[2] || '').trim() }
    },
    onSystemNotifyRowTap(item) {
      if (!item || item.id == null) return
      if (this.manageMode) {
        this.toggleMsgKey(this.msgKey(item._table || 'system_notify', item.id))
        return
      }
      item.type = 0
      this.systemNotifyList = [...this.systemNotifyList]
      const tbl = item._table || 'system_notify'
      savej({ params: { table: tbl, type: 0, id: item.id } }).catch(() => {})
      if (tbl === 'system_notify' && String(item.msgtype) === '20') {
        const ref = this.parseBlogplanNotifyRef(item.note)
        if (ref && ref.planId) {
          const sidQ = ref.sid ? `pid=${encodeURIComponent(ref.sid)}&` : ''
          uni.navigateTo({
            url: `/pages/blog/blogplan?${sidQ}id=${encodeURIComponent(ref.planId)}`
          })
        }
      }
    },
    onSystemBillRowTap(row) {
      if (!row || row.id == null) return
      if (this.manageMode) {
        this.toggleMsgKey(this.msgKey('bill', row.id))
        return
      }
      this.onOpenSystem(row)
    },
    billEvalHidden(b) {
      if (!b || b.id == null) return false
      try {
        return uni.getStorageSync('bill_eval_hide_' + b.id) === '1'
      } catch (e) {
        return false
      }
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
              this.loadSystemList()
              this.loadSystemNotifyList()
            })
            .catch(() => {
              uni.hideLoading()
              uni.showToast({ title: '部分删除失败', icon: 'none' })
              this.exitManage()
              this.loadList()
              this.loadSystemList()
              this.loadSystemNotifyList()
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
        try {
          uni.setStorageSync('bill_eval_hide_' + id, '1')
        } catch (e) {}
        return Promise.resolve()
      }
      return deletej({ params: { table: tbl, id } })
    },
    onTabChange(e) {
      this.currentTab = typeof e.index === 'number' ? e.index : 0
    },
    /** 一键已读：所有未读消息标记为已读（互动+系统） */
    onMarkAllRead() {
      let hasUnread = false
      // 互动消息
      if (this.list && this.list.length) {
        const unreadItems = this.list.filter(item => String(item.type) === '1')
        if (unreadItems.length) {
          hasUnread = true
          unreadItems.forEach(item => {
            item.type = 0
            savej({ params: { table: 'huihua', type: 0, id: item.id } }).catch(() => {})
          })
        }
      }
      // 系统通知（system_notify + 评价申诉结果 huihua msgtype=11）
      if (this.systemNotifyList && this.systemNotifyList.length) {
        const unreadNotifies = this.systemNotifyList.filter(item => String(item.type) === '1')
        if (unreadNotifies.length) {
          hasUnread = true
          unreadNotifies.forEach(item => {
            item.type = 0
            const tbl = item._table || 'system_notify'
            if (tbl === 'huihua') {
              savej({ params: { table: 'huihua', type: 0, id: item.id } }).catch(() => {})
            } else {
              savej({ params: { table: 'system_notify', type: 0, id: item.id } }).catch(() => {})
            }
          })
        }
      }
      if (!hasUnread) {
        uni.showToast({ title: '暂无未读消息', icon: 'none' })
        return
      }
      this.list = [...this.list]
      this.systemNotifyList = [...this.systemNotifyList]
      uni.showToast({ title: '已全部标记为已读', icon: 'success' })
    },
    loadList() {
      if (!this.userInfo || !this.userInfo.id) {
        this.list = []
        return
      }
      listj({ params: { table: 'huihua', uid: this.userInfo.id, msgtype: 9 } }).then(res => {
        const arr = (res || []).filter(it => {
          const n = (it.note || '').toString()
          const fu = (it.fusername || '').toString()
          if (fu === '系统通知' && /申诉|恶意差评已下架|评价被商家|恶意评价|删除.*评价/.test(n)) return false
          return true
        })
        this.list = arr.sort((a, b) => (b.ndate || '').localeCompare(a.ndate || ''))
      }).catch(err => {
        this.list = []
      })
    },
    /** 系统 Tab：从订单表筛选「已完成」待评价提醒（与 bill 页「去评价」一致） */
    loadSystemList() {
      if (!this.userInfo || !this.userInfo.id) {
        this.systemList = []
        return
      }
      listj({
        params: { table: 'bill', uid: this.userInfo.id, sort: 'id', order: 'desc' }
      }).then(res => {
        const arr = res || []
        this.systemList = arr.filter(
          b => String(b.statecn || '') === '已完成' && !this.billEvalHidden(b)
        )
      }).catch(() => {
        this.systemList = []
      })
    },
    /**
     * 申诉结果在用户端的统一展示文案（不展示库里管理员备注，避免「删除恶意评价」等内部用语）
     * @param {boolean} hasMeta 已单独展示店铺/评价摘要时，正文避免重复说「该店铺」
     */
    appealUserDisplayNote(msgtype, rawNote, hasMeta) {
      const meta = !!hasMeta
      const ok = meta
        ? '经平台审核，该条评价不符合平台规范，已作删除处理。'
        : '经平台审核，您在该店铺发表的评价不符合平台规范，已作删除处理。'
      const fail = meta
        ? '商家曾就该条评价发起申诉，经平台复核，未发现违规情形，评价将继续展示。感谢您的理解。'
        : '商家曾就您的评价发起申诉，经平台复核，该评价未发现违规情形，将继续展示。感谢您的理解。'
      const mt = String(msgtype)
      if (mt === '11') return ok
      if (mt === '12') return fail
      const n = (rawNote || '').toString()
      if (/未违规|因此保留|申诉未通过|未违反|未发现违规|将继续展示|评价内容未违规/.test(n)) return fail
      return ok
    },
    /** 加载系统通知：system_notify + 评价申诉（huihua msgtype 11/12）+ 历史 msgtype9 */
    loadSystemNotifyList() {
      if (!this.userInfo || !this.userInfo.id) {
        this.systemNotifyList = []
        return
      }
      const uid = this.userInfo.id
      const pSys = listj({ params: { table: 'system_notify', uid } }).catch(() => [])
      const pAppealOk = listj({ params: { table: 'huihua', uid, msgtype: 11 } }).catch(() => [])
      const pAppealFail = listj({ params: { table: 'huihua', uid, msgtype: 12 } }).catch(() => [])
      const pLegacyQa = listj({ params: { table: 'huihua', uid, msgtype: 9 } }).catch(() => [])
      Promise.all([pSys, pAppealOk, pAppealFail, pLegacyQa]).then(([sysRes, okRes, failRes, qaRes]) => {
        const sysRows = (sysRes || []).map(it => ({
          ...it,
          _table: 'system_notify',
          _rowKey: 'system_notify-' + it.id
        }))
        const toAppealRow = it => {
          const mt = String(it.msgtype || '')
          const isAppealResult = mt === '11' || mt === '12'
          return {
            id: it.id,
            title: '',
            note: isAppealResult
              ? (it.note || '').toString()
              : this.appealUserDisplayNote(it.msgtype, it.note),
            ndate: it.ndate,
            type: it.type,
            msgtype: it.msgtype,
            _table: 'huihua',
            shop: it.shop,
            qtitle: it.qtitle,
            _rowKey: 'huihua-' + it.id
          }
        }
        const appealRows = [...(okRes || []), ...(failRes || [])].map(toAppealRow)
        const legacyAppealRows = (qaRes || [])
          .filter(it => {
            const n = (it.note || '').toString()
            const fu = (it.fusername || '').toString()
            if (fu !== '系统通知') return false
            return /申诉|恶意差评已下架|评价被商家|恶意评价|删除.*评价/.test(n)
          })
          .map(toAppealRow)
        const seen = new Set(appealRows.map(it => String(it.id)))
        const legacyOnly = legacyAppealRows.filter(it => !seen.has(String(it.id)))
        this.systemNotifyList = [...appealRows, ...legacyOnly, ...sysRows].sort((a, b) =>
          String(b.ndate || '').localeCompare(String(a.ndate || ''))
        )
      })
    },
    /**
     * 评价申诉结果（msgtype 11/12）：正文以库里 note 为准（管理端已写入店铺+评价+说明）；
     * 旧数据仅有「在该店铺」模板时，用 shop/qtitle 字段补全；仍无则退回统一话术。
     */
    buildAppealNotifyBody(item) {
      let body = (item.note || '').toString().trim()
      const shop = (item.shop || '').toString().trim()
      let snip = (item.qtitle || '').toString().replace(/\s+/g, ' ').trim()
      if (snip.length > 72) snip = snip.slice(0, 72) + '…'
      const hasShopInNote = /店铺\s*[:：]/.test(body)
      const hasSnipInNote = /评价内容\s*[:：]/.test(body)
      const head = []
      if (shop && !hasShopInNote) head.push('店铺：' + shop)
      if (snip && !hasSnipInNote) head.push('评价内容：' + snip)
      if (head.length) {
        const prefix = head.join('\n') + '\n\n'
        const isOldGeneric =
          /经平台审核，您在该店铺发表的评价/.test(body) ||
          /商家曾就您的评价发起申诉/.test(body)
        if (body && isOldGeneric) {
          body = prefix + this.appealUserDisplayNote(item.msgtype, body, true)
        } else if (body) {
          body = prefix + body
        } else {
          body = prefix + this.appealUserDisplayNote(item.msgtype, '', true)
        }
      }
      if (!body) {
        const hasMeta = !!(shop || snip)
        body = this.appealUserDisplayNote(item.msgtype, '', hasMeta)
      }
      return body
    },
    /** 系统 Tab 正文：渲染时再算一遍，避免小程序运行时仍用库里的管理员备注（如「删除恶意评价」） */
    systemNotifyBody(item) {
      if (!item) return '您有新的系统通知'
      const raw = (item.note || '').toString()
      const tit = (item.title || '').toString()
      if (item._table === 'huihua') {
        const mt = String(item.msgtype || '')
        if (mt === '11' || mt === '12') {
          return this.buildAppealNotifyBody(item)
        }
        return this.appealUserDisplayNote(item.msgtype, raw)
      }
      if (item._table === 'system_notify') {
        if (String(item.msgtype) === '20') {
          return this.stripBlogplanRefSuffix(raw)
        }
        if (/评价申诉/.test(tit) || /删除恶意|恶意差评|已核实|申诉成立|申诉未通过|下架/.test(raw)) {
          return this.appealUserDisplayNote(item.msgtype != null && item.msgtype !== '' ? item.msgtype : '11', raw)
        }
      }
      return raw || '您有新的系统通知'
    },
    systemNotifyTitle(item) {
      if (!item) return '系统通知'
      if (item._table === 'huihua') return '评价申诉处理结果'
      if (/评价申诉/.test((item.title || '').toString())) return '评价申诉处理结果'
      return item.title || '系统通知'
    },
    isUnread(item) {
      return item && String(item.type) === '1'
    },
    getType(item) {
      if (!item) return 'system'
      const mt = String(item.msgtype)
      if (mt === '9') return 'qa'
      const t = `${item.title || ''} ${item.note || ''}`
      if (t.indexOf('订单') !== -1) return 'order'
      return 'system'
    },
    getIcon(item) {
      const t = this.getType(item)
      if (t === 'qa') return '问'
      if (t === 'order') return '单'
      return '系'
    },
    getTitle(item) {
      const mt = String(item && item.msgtype)
      if (mt === '9') return '问答回复提醒'
      return item.title || '系统通知'
    },
    getTag(item) {
      const mt = String(item && item.msgtype)
      if (mt === '9') return '问答'
      if (this.getType(item) === 'order') return '订单'
      return ''
    },
    getSub(item) {
      if (!item) return ''
      if (String(item.msgtype) === '9') {
        const shop = item.shop || ''
        return shop ? `店铺：${shop}` : ''
      }
      return ''
    },
    getContent(item) {
      if (!item) return ''
      const raw = (item.note || '').toString()
      return raw.replace(/\n/g, '<br/>')
    },
    getActionText(item) {
      if (!item) return ''
      if (String(item.msgtype) === '9') return '去店铺查看'
      return ''
    },
    markRead(item) {
      if (!item || !item.id) return
      item.type = 0
      this.list = [...this.list]
      savej({ params: { table: 'huihua', type: 0, id: item.id } }).catch(() => {})
    },
    toPage(url, onSuccess) {
      if (!url) return
      uni.navigateTo({
        url,
        success: () => {
          onSuccess && onSuccess()
        },
        fail: () => {
          if (uni && uni.itool && typeof uni.itool.nto === 'function') {
            uni.itool.nto({ url })
            onSuccess && onSuccess()
          } else {
            uni.showToast({ title: '跳转失败，请稍后重试', icon: 'none' })
          }
        }
      })
    },
    isQaNotify(item) {
      return item && String(item.msgtype) === '9'
    },
    resolveSid(item) {
      if (item && item.sid) return Promise.resolve(item.sid)
      const qid = item && item.qid
      if (qid) {
        return findj({ params: { table: 'shop_qa', id: qid } })
          .then(res => {
            if (res && res.sid) return res.sid
            const sname = (item && item.shop) ? String(item.shop).trim() : ''
            if (!sname) return ''
            return listj({ params: { table: 'shop' } }).then(rows => {
              const arr = rows || []
              const hit = arr.find(s => String(s.sname || '').trim() === sname) ||
                arr.find(s => String(s.sname || '').indexOf(sname) !== -1 || sname.indexOf(String(s.sname || '')) !== -1)
              return hit ? hit.id : ''
            }).catch(() => '')
          })
          .catch(() => {
            const sname = (item && item.shop) ? String(item.shop).trim() : ''
            if (!sname) return ''
            return listj({ params: { table: 'shop' } }).then(rows => {
              const arr = rows || []
              const hit = arr.find(s => String(s.sname || '').trim() === sname) ||
                arr.find(s => String(s.sname || '').indexOf(sname) !== -1 || sname.indexOf(String(s.sname || '')) !== -1)
              return hit ? hit.id : ''
            }).catch(() => '')
          })
      }
      const sname = (item && item.shop) ? String(item.shop).trim() : ''
      if (!sname) return Promise.resolve('')
      return listj({ params: { table: 'shop' } }).then(rows => {
        const arr = rows || []
        const hit = arr.find(s => String(s.sname || '').trim() === sname) ||
          arr.find(s => String(s.sname || '').indexOf(sname) !== -1 || sname.indexOf(String(s.sname || '')) !== -1)
        return hit ? hit.id : ''
      }).catch(() => '')
    },
    onOpen(item) {
      if (!item || !item.id) return
      if (this.isQaNotify(item)) {
        this.resolveSid(item).then(sid => {
          if (!sid) {
            uni.showToast({ title: '店铺信息缺失，暂无法跳转', icon: 'none' })
            return
          }
          const url = `/pages/shops/shopdetail?pid=${sid}`
          this.toPage(url, () => this.markRead(item))
        })
        return
      }
      this.markRead(item)
    },
    onOpenSystem(row) {
      if (!row || !row.id) return
      this.toReviewOrder(row)
    },
    /** 与 bill.vue 一致：跳转首个商品详情并打开点评 */
    toReviewOrder(item) {
      const gids = String(item.gids || '')
        .split(',')
        .map(s => s.trim())
        .filter(Boolean)
      if (!gids.length) {
        uni.navigateTo({ url: '/pages/bill/billdetail?bid=' + item.id })
        return
      }
      const gid = gids[0]
      uni.navigateTo({
        url: '/pages/good/gooddetail-2?gid=' + gid + '&showpl=1'
      })
    }
  },
  computed: {
    ...mapState(['userInfo']),
    /** 互动列表是否有未读：只统计问答互动消息（msgtype === '9'） */
    hasListUnread() {
      return this.list && this.list.some(item => String(item.msgtype) === '9' && String(item.type) === '1')
    },
    /** 系统是否有未读：只统计系统通知消息 */
    hasSystemUnread() {
      return this.systemNotifyList && this.systemNotifyList.some(item => String(item.type) === '1')
    },
    /** 系统通知分组（用于渲染） */
    groupedSystemNotify() {
      return this.systemNotifyList || []
    },
    groupedList() {
      const buckets = {
        qa: { key: 'qa', title: '问答', items: [] },
        system: { key: 'system', title: '系统', items: [] },
        order: { key: 'order', title: '订单', items: [] }
      }
      ;(this.list || []).forEach(it => {
        const t = this.getType(it)
        if (buckets[t]) buckets[t].items.push(it)
        else buckets.system.items.push(it)
      })
      return Object.values(buckets).filter(g => g.items.length > 0)
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
  display: flex;
  justify-content: flex-start;
  align-items: center;
}

/* 缩小「互动 / 系统」整体占位，右侧留出「管理」 */
.custom-tabs {
  display: flex;
  align-items: center;
  position: relative;
  width: 280rpx;
  flex-shrink: 0;
  padding: 0 0 0 24rpx;
  box-sizing: border-box;
}

.tab-item {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16rpx 8rpx;
  font-size: 28rpx;
  color: #909399;
  position: relative;
  transition: color 0.3s;

  &.active {
    color: #ff943c;
    font-weight: bold;
  }

  text {
    position: relative;
  }
}

.tab-dot {
  position: absolute;
  top: 8rpx;
  right: -16rpx;
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background-color: #ef4444;
}

.tab-line {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 50%;
  height: 4rpx;
  background-color: #ff943c;
  transition: transform 0.3s ease;
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

.chk-wrap {
  flex-shrink: 0;
  padding-right: 12rpx;
  display: flex;
  align-items: center;
  align-self: center;
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

.group-title {
  font-size: 24rpx;
  color: #8a8f98;
  padding: 10rpx 6rpx 6rpx;
}

.sys-hint {
  padding-bottom: 12rpx;
}

.msg-item {
  background-color: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx 24rpx 20rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.03);
}

.sys-card {
  border: 1rpx solid rgba(255, 148, 60, 0.2);
}

.sys-tag {
  background: #fff7ed !important;
  color: $primary !important;
}

.sys-plain {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
  white-space: pre-line;
}

.row-top {
  display: flex;
  gap: 18rpx;
}
.left {
  padding-top: 2rpx;
}
.mid {
  flex: 1;
  min-width: 0;
}
.icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  flex-shrink: 0;
}
.icon-text {
  color: #fff;
  font-weight: 900;
  font-size: 26rpx;
}
.icon.tqa {
  background: linear-gradient(135deg, #ffb347, #ff6a00);
}
.icon.torder {
  background: linear-gradient(135deg, #ffb347, $primary);
}
.icon.tsystem {
  background: linear-gradient(135deg, #9ca3af, #6b7280);
}
.icon.tnotify {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
}
.dot {
  position: absolute;
  top: -6rpx;
  right: -6rpx;
  width: 18rpx;
  height: 18rpx;
  border-radius: 999rpx;
  background: #ef4444;
  border: 4rpx solid #fff;
}

.msg-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;

  .title-left {
    display: flex;
    align-items: center;
    gap: 12rpx;
    min-width: 0;
  }

  .msg-title {
    font-size: 30rpx;
    font-weight: bold;
    color: #1a1a1a;
    max-width: 360rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .msg-time {
    font-size: 22rpx;
    color: #999999;
  }
}

.msg-tag {
  font-size: 20rpx;
  padding: 4rpx 10rpx;
  border-radius: 999rpx;
  background: #fff0e6;
  color: #ff6a00;
  flex-shrink: 0;
}

.msg-sub {
  font-size: 24rpx;
  color: #666666;
  margin-bottom: 8rpx;
}

.msg-content {
  font-size: 26rpx;
  color: #444444;
  line-height: 1.6;
}

.msg-action {
  margin-top: 10rpx;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6rpx;

  .action-text {
    font-size: 24rpx;
    color: #2563eb;
    font-weight: 600;
  }
}

.sys-card .msg-action .action-text {
  color: $primary;
}
</style>
