<template>
  <view class="page">
    <u-navbar
      :title="shopName ? shopName + ' · 问大家' : '问大家'"
      :border="false"
      :placeholder="true"
      :autoBack="true"
      bgColor="#ffffff"
      titleStyle="font-weight: 800; color: #1a1a1a;"
    ></u-navbar>

    <view class="header">
      <view class="searchbar">
        <u-search
          v-model="keyword"
          placeholder="有问题直接搜索"
          :showAction="false"
          bgColor="#f5f6f8"
          shape="round"
          @change="onSearchChange"
        ></u-search>
      </view>

      <view class="tabs">
        <view class="tab" :class="{active: tab==='all'}" @click="tab='all'">全部 {{ questionList.length }}</view>
        <view class="tab" :class="{active: tab==='answered'}" @click="tab='answered'">已回答</view>
        <view class="tab" :class="{active: tab==='unanswered'}" @click="tab='unanswered'">待回答</view>
      </view>
    </view>

    <scroll-view
      scroll-y
      class="list"
      :scroll-into-view="scrollToView"
      :scroll-with-animation="true"
    >
      <view v-if="loading" class="empty">加载中…</view>

      <view v-else-if="filteredQuestions.length===0" class="empty">
        <text>暂无相关问题，快来提问吧</text>
      </view>

      <view
        v-else
        class="qcard"
        :class="{ highlight: highlightQid && String(q.id)===String(highlightQid) }"
        v-for="q in filteredQuestions"
        :key="q.id"
        :id="'q' + q.id"
      >
        <view class="qrow">
          <view class="qtag">问</view>
          <view class="qcontent">
            <text class="qtext">{{ q.note }}</text>
            <view class="qmeta">
              <text class="quser">{{ q.username || '匿名用户' }}</text>
              <text class="qdate">{{ q.ndate }}</text>
            </view>
          </view>
          <view class="qstat">{{ (q._answers && q._answers.length) ? (q._answers.length + '个回答') : '暂无回答' }}</view>
        </view>

        <view class="answers" v-if="q._answers && q._answers.length">
          <view class="arow" v-for="(a, idx) in q._answers.slice(0, q._showAll ? 999 : 2)" :key="idx">
            <view class="atag">答</view>
            <view class="acontent">
              <text class="atext">{{ a.note }}</text>
              <text class="auser">- {{ a.username || '匿名' }}<text v-if="a.roletype==='3'" class="role">（商家）</text></text>
            </view>
          </view>
          <view v-if="q._answers.length>2" class="more" @click="toggleAnswers(q)">
            {{ q._showAll ? '收起' : ('展开更多回答(' + q._answers.length + ')') }}
          </view>
        </view>

        <view class="answerBox">
          <u--textarea
            v-model="q._anote"
            placeholder="我来回答…（商家或其他用户都可以）"
            border="none"
            height="70"
            bgColor="#f7f8fa"
            shape="circle"
          ></u--textarea>
          <view class="abtn">
            <u-button type="primary" size="small" shape="circle" text="提交回答" @click="submitAnswer(q)"></u-button>
          </view>
        </view>
      </view>

      <view class="safe"></view>
    </scroll-view>

    <view class="askbar">
      <u-button type="primary" shape="circle" text="去提问" @click="openAsk"></u-button>
    </view>

    <u-popup :show="showAsk" mode="bottom" @close="showAsk=false">
      <view class="askpop">
        <view class="asktitle">
          <text>发布提问</text>
          <text class="close" @click="showAsk=false">×</text>
        </view>
        <u--textarea
          v-model="qnote"
          placeholder="我想问：比如营业时间？招牌菜？是否辣？是否可预约？"
          border="none"
          height="120"
          bgColor="#f7f8fa"
          shape="circle"
        ></u--textarea>
        <view class="askbtn">
          <u-button type="primary" shape="circle" text="发布" @click="submitQuestion"></u-button>
        </view>
      </view>
    </u-popup>

    <tn-tips ref="tips"></tn-tips>
  </view>
</template>

<script>
import { savej, listj, findj } from '@/common/config/api.js'
import { mapState } from 'vuex'

export default {
  data() {
    return {
      sid: null,
      shopName: '',
      keyword: '',
      tab: 'all',
      loading: false,
      questionList: [],
      answerList: [],
      showAsk: false,
      qnote: '',
      _searchTimer: null,
      scrollToView: '',
      highlightQid: ''
    }
  },
  onLoad(params) {
    this.sid = params.sid
    // 解码店铺名，避免 URL 编码导致乱码（如 %E7%BE%8E%E7%BE%8E%E7%BE%8E%E）
    try {
      this.shopName = (params.shop && typeof params.shop === 'string') ? decodeURIComponent(params.shop) : ''
    } catch (e) {
      this.shopName = params.shop || ''
    }
    this.highlightQid = params.qid || ''
    this.loadQA()
  },
  computed: {
    ...mapState(['userInfo']),
    filteredQuestions() {
      const kw = (this.keyword || '').trim().toLowerCase()
      let list = this.questionList || []
      if (kw) {
        list = list.filter(q => ((q.note || '') + '').toLowerCase().indexOf(kw) !== -1)
      }
      if (this.tab === 'answered') {
        list = list.filter(q => q._answers && q._answers.length)
      } else if (this.tab === 'unanswered') {
        list = list.filter(q => !q._answers || !q._answers.length)
      }
      return list
    }
  },
  methods: {
    notifyMerchantNewQuestion({ sid, shopName, qid, qnote, fromUser }) {
      if (!sid || !fromUser || !fromUser.id) return Promise.resolve()
      const uname = fromUser.username || '用户'
      const title = '店铺收到新提问'
      const note = `${uname} 对您的店铺进行了提问：<br/>${(qnote || '').toString()}`
      const data = {
        table: 'sysmsg',
        sid,
        shop: shopName,
        title,
        note,
        biztype: 'shop_qa_question',
        qid,
        fromuid: fromUser.id,
        fromusername: fromUser.username
      }
      return savej({ params: data }).catch(() => {})
    },
    upsertQaReplyNotify({ toUid, fromUser, sid, shopName, qid, qnote, anote }) {
      if (!toUid || !fromUser || !fromUser.id) return Promise.resolve()
      // 问答回复提醒：写入 huihua 表，出现在“我的消息”
      const note = `回复了你的提问：${(qnote || '').slice(0, 24)}${(qnote || '').length > 24 ? '…' : ''}\n${(anote || '').slice(0, 30)}${(anote || '').length > 30 ? '…' : ''}`
      return findj({
        params: {
          table: 'huihua',
          uid: toUid,
          fid: fromUser.id,
          msgtype: 9,
          sid,
          qid
        }
      }).then(old => {
        const data = old ? { ...old } : {}
        data.table = 'huihua'
        data.type = 1
        data.uid = toUid
        data.fid = fromUser.id
        data.fusername = fromUser.username
        data.img = fromUser.img
        data.username = '' // 会话列表主要展示 fusername/note
        data.note = note
        data.msgtype = 9
        data.sid = sid
        data.shop = shopName
        data.qid = qid
        data.ndate = null
        return savej({ params: data })
      }).catch(() => {})
    },
    onSearchChange() {
      // 简单防抖，避免频繁触发重渲染
      if (this._searchTimer) clearTimeout(this._searchTimer)
      this._searchTimer = setTimeout(() => {}, 120)
    },
    openAsk() {
      if (!this.userInfo || !this.userInfo.id) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }
      this.qnote = ''
      this.showAsk = true
    },
    toggleAnswers(q) {
      q._showAll = !q._showAll
      this.questionList = [...this.questionList]
    },
    loadQA() {
      this.loading = true
      listj({ params: { table: 'shop_qa', sid: this.sid, qtype: 'Q', sort: 'id', order: 'desc' } })
        .then(qs => {
          this.questionList = (qs || []).map(q => ({
            ...q,
            _answers: [],
            _showAll: false,
            _anote: ''
          }))
          return listj({ params: { table: 'shop_qa', sid: this.sid, qtype: 'A', sort: 'id', order: 'desc' } })
        })
        .then(ans => {
          this.answerList = ans || []
          const qidSet = new Set((this.questionList || []).map(q => String(q.id)))
          const grouped = {}
          this.answerList.forEach(a => {
            const qid = String(a.qid || '')
            if (!qidSet.has(qid)) return
            if (!grouped[qid]) grouped[qid] = []
            grouped[qid].push(a)
          })
          Object.keys(grouped).forEach(k => {
            grouped[k].sort((a, b) => (b.id || 0) - (a.id || 0))
          })
          this.questionList = (this.questionList || []).map(q => ({
            ...q,
            _answers: grouped[String(q.id)] || []
          }))
        })
        .finally(() => {
          this.loading = false
          // 带 qid 打开时，自动定位并高亮该条提问
          if (this.highlightQid) {
            this.$nextTick(() => {
              this.scrollToView = 'q' + this.highlightQid
              // 轻微重置，避免下次进入不触发滚动
              setTimeout(() => {
                this.scrollToView = ''
              }, 800)
            })
          }
        })
    },
    submitQuestion() {
      if (!this.userInfo || !this.userInfo.id) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }
      const txt = (this.qnote || '').trim()
      if (!txt) {
        uni.showToast({ title: '请输入问题内容', icon: 'none' })
        return
      }
      const fdata = {
        table: 'shop_qa',
        sid: this.sid,
        shop: this.shopName,
        note: txt,
        uid: this.userInfo.id,
        username: this.userInfo.username,
        roletype: this.userInfo.roletype,
        qtype: 'Q'
      }
      savej({ params: fdata }).then(res => {
        this.showAsk = false
        this.qnote = ''
        this.loadQA()
        this.$refs.tips.show({ msg: '提问成功' })

        // 同步写入商家端“消息通知”（sysmsg）
        const qid = (res && res.id) ? res.id : undefined
        this.notifyMerchantNewQuestion({
          sid: this.sid,
          shopName: this.shopName,
          qid,
          qnote: txt,
          fromUser: this.userInfo
        })
      })
    },
    submitAnswer(q) {
      if (!this.userInfo || !this.userInfo.id) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }
      const txt = (q && q._anote) ? q._anote.trim() : ''
      if (!txt) {
        uni.showToast({ title: '请输入回答内容', icon: 'none' })
        return
      }
      const fdata = {
        table: 'shop_qa',
        sid: this.sid,
        shop: this.shopName,
        qid: q.id,
        note: txt,
        uid: this.userInfo.id,
        username: this.userInfo.username,
        roletype: this.userInfo.roletype,
        qtype: 'A'
      }
      savej({ params: fdata }).then(() => {
        // 给提问者推送一条“问答回复提醒”，显示在“我的消息”
        if (q && String(q.uid) && String(q.uid) !== String(this.userInfo.id)) {
          this.upsertQaReplyNotify({
            toUid: q.uid,
            fromUser: this.userInfo,
            sid: this.sid,
            shopName: this.shopName,
            qid: q.id,
            qnote: q.note,
            anote: txt
          })
        }
        q._anote = ''
        this.loadQA()
        this.$refs.tips.show({ msg: '回答成功' })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #ff943c;
$bg: #f8f9fb;

.page{ background: $bg; min-height: 100vh; }
.header{ padding: 20rpx 26rpx 16rpx; background:#fff; border-bottom: 1rpx solid #f2f3f5; }
.searchbar{ margin-bottom: 16rpx; }
.tabs{ display:flex; gap: 16rpx; }
.tab{ padding: 12rpx 22rpx; border-radius: 999rpx; background:#f5f6f8; color:#666; font-size: 26rpx; }
.tab.active{ background: linear-gradient(135deg, #ffb347, #ff6a00); color: #fff; font-weight: 700; }

.list{ height: calc(100vh - 320rpx); padding: 16rpx 20rpx; }
.empty{ padding: 40rpx 0; text-align:center; color:#9aa1a9; }
.qcard{ background:#fff; border-radius: 18rpx; padding: 20rpx; margin-bottom: 16rpx; box-shadow: 0 8rpx 30rpx rgba(0,0,0,0.04); border: 2rpx solid transparent; }
.qcard.highlight{
  border-color: rgba(255, 106, 0, 0.55);
  box-shadow: 0 10rpx 36rpx rgba(255, 106, 0, 0.12);
}
.qrow{ display:flex; gap: 14rpx; align-items:flex-start; }
.qtag,.atag{ width: 40rpx; height: 40rpx; border-radius: 10rpx; display:flex; align-items:center; justify-content:center; font-weight: 800; font-size: 24rpx; color:#fff; }
.qtag{ background: linear-gradient(135deg, #ffb347, #ff6a00); }
.atag{ background: #10b981; }
.qcontent{ flex:1; }
.qtext{ font-size: 28rpx; color:#1a1a1a; line-height: 1.5; }
.qmeta{ margin-top: 8rpx; font-size: 22rpx; color:#9aa1a9; display:flex; gap: 10rpx; }
.qstat{ font-size: 22rpx; color:#9aa1a9; white-space: nowrap; }
.answers{ margin-top: 16rpx; padding-top: 12rpx; border-top: 1rpx solid #f2f3f5; }
.arow{ display:flex; gap: 12rpx; align-items:flex-start; margin-top: 10rpx; }
.acontent{ flex:1; }
.atext{ font-size: 26rpx; color:#333; line-height: 1.5; }
.auser{ font-size: 22rpx; color:#9aa1a9; margin-left: 8rpx; }
.role{ color:#ff6a00; }
.more{ margin-top: 10rpx; color:#2563eb; font-size: 24rpx; }
.answerBox{ margin-top: 16rpx; }
.abtn{ margin-top: 10rpx; display:flex; justify-content:flex-end; }
.safe{ height: 160rpx; }

.askbar{ position: fixed; left: 20rpx; right: 20rpx; bottom: calc(24rpx + env(safe-area-inset-bottom)); z-index: 100; }
.askpop{ background:#fff; border-top-left-radius: 22rpx; border-top-right-radius: 22rpx; padding: 18rpx 20rpx 26rpx; }
.asktitle{ display:flex; align-items:center; justify-content:space-between; font-weight: 800; margin-bottom: 14rpx; }
.close{ font-size: 40rpx; color:#9aa1a9; padding: 0 8rpx; }
.askbtn{ margin-top: 14rpx; }
</style>

