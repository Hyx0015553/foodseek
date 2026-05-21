<template>
  <view class="page-container">
    <!-- 导航栏：使用背景色与整体融合 -->
    <u-navbar
        :title="fobj.sname"
        :border="false"
        :placeholder="true"
        :autoBack="true"
        bgColor="#ffffff"
        titleStyle="font-weight: bold; color: #1a1a1a;"
    ></u-navbar>

    <!-- 审核状态提示条：仅商家端可见 -->
    <view
      v-if="(fobj.state != null ? Number(fobj.state) : null) === SHOP_AUDIT_STATE.PENDING || fobj.statecn === '待审核'"
      class="audit-banner"
    >
      <text class="audit-text">当前店铺信息已提交，正在等待管理员审核，审核通过后才会对用户生效</text>
    </view>

    <scroll-view scroll-y class="main-scroll" :enable-flex="true">
      <!-- 头部大图：增加渐变投影增强文字辨识度 -->
      <view class="hero-section">
        <image class="hero-image" :src="fileUrl+fobj.img" mode="aspectFill"></image>
        <view class="hero-overlay"></view>
      </view>

      <!-- 档口信息卡片：悬浮效果 -->
      <view class="info-card">
        <view class="shop-header">
          <text class="shop-name">{{ fobj.sname }}</text>
          <view class="shop-tag">营业中</view>
        </view>

        <view class="info-item" @click="openMap">
          <view class="info-icon">📍</view>
          <text class="info-text">{{ fobj.address || '暂无地址信息' }}</text>
          <view class="info-action">导航</view>
        </view>

        <view class="info-item" @click="callPhone">
          <view class="info-icon">📞</view>
          <text class="info-text">{{ fobj.tel || '暂无联系电话' }}</text>
          <view class="info-action">呼叫</view>
        </view>
	<view class="formitem">
        <navigator url="/pages/unimap/unimap">
          <u-button type="primary" text="周边店铺"></u-button>
        </navigator>
      
	      </view>
      </view>

      <!-- 详情介绍区 -->
      <view class="content-section">
        <view class="section-title">
          <view class="title-line"></view>
          <text>店铺介绍</text>
        </view>
        <view class="rich-text-wrapper">
          <u-parse :content="fobj.note" />
        </view>
      </view>

      <!-- 菜品列表入口 -->
      <view class="food-list-entry" @click="toGoodView">
        <view class="entry-left">
          <text class="entry-icon">🍜</text>
          <text class="entry-title">店铺菜品</text>
        </view>
        <view class="entry-right">
          <text class="entry-desc">查看全部菜品</text>
          <u-icon name="arrow-right" size="14" color="#999"></u-icon>
        </view>
      </view>

      <!-- 问答区（预览 + 进入全部） -->
      <view class="comment-section">
        <view class="section-title">
          <view class="title-line"></view>
          <text>大家都在问</text>
          <text class="comment-count">({{ questionList.length }})</text>
          <text class="qa-more" @click="toQaPage">查看更多 ></text>
        </view>

        <!-- 预览列表：最多2条，点击进入“问大家”页 -->
        <view class="comment-list" v-if="questionList.length > 0" @click="toQaPage">
          <view class="qa-preview" v-for="(q, index) in questionList.slice(0,2)" :key="index">
            <view class="qa-q">
              <text class="qa-badge">问</text>
              <text class="qa-text">{{ q.note }}</text>
              <text class="qa-stat">{{ (q._answers && q._answers.length) ? (q._answers.length + '个回答') : '暂无回答' }}</text>
            </view>
            <view class="qa-a" v-if="q._answers && q._answers.length">
              <text class="qa-badge a">答</text>
              <text class="qa-atext">{{ q._answers[0].note }}</text>
            </view>
          </view>
        </view>
        <view class="comment-empty" v-else>
          <text>💬 还没有提问，快来问问大家吧</text>
        </view>

        <view class="qa-entry">
          <u-button type="primary" size="small" shape="circle" text="去提问 / 去回答" @click="toQaPage"></u-button>
        </view>
      </view>

      <!-- 本店全部菜品评价：fs_replay.type=1，经菜品 sid 汇总 -->
      <view class="content-section review-section" v-if="fobj.id">
        <view class="review-section-head">
          <view class="section-title review-section-title">
            <view class="title-line"></view>
            <text>本店菜品评价</text>
            <text class="review-count-paren">({{ replaylist.length }})</text>
          </view>
          <text v-if="hasReviewFilters" class="filter-reset" @tap="clearReviewFilters">重置筛选</text>
        </view>
        <text class="review-dish-hint">以下汇总本店各菜品用户评价；撰写新评价请进入对应菜品详情页。</text>

        <view v-if="replaylist.length" class="review-score-strip">
          <view class="rss-main">
            <text class="rss-num">{{ displayPf }}</text>
            <view class="rss-stars">
              <text v-for="n in 5" :key="'rs'+n" class="rss-star" :class="{ on: n <= Math.round(Number(fobj.pf) || 0) }">★</text>
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
          <view v-for="(line, idx) in reviewAiLines" :key="'ai'+idx" class="rai-line">{{ line }}</view>
        </view>

        <view class="review-wrapper">
          <imglist
            imgName="img"
            :showArrow="false"
            :showSearch="false"
            :dataList="displayedReplayList"
            :imgSize="2"
            titleName="ndate"
            htitleName="gname"
            htitleLabel="菜品："
            sName="note"
            tName="username"
            @clickItem="onShopReviewRowClick"
          />
          <view
            v-if="reviewShowExpandToggle"
            class="review-expand-bar"
            @tap.stop="toggleReviewExpand"
          >
            <text class="review-expand-text">{{ reviewExpandAll ? '收起' : ('展开其余 ' + reviewExpandRemainCount + ' 条评价') }}</text>
          </view>
          <view v-if="replaylist.length === 0" class="empty-reviews">
            <text>暂无本店菜品评价，欢迎引导顾客在菜品页评价~</text>
          </view>
          <view v-else-if="filteredReplayList.length === 0" class="empty-reviews filter-empty">
            <text>暂无符合当前筛选的评价</text>
            <view class="empty-clear" @tap="clearReviewFilters">清除筛选</view>
          </view>
        </view>
      </view>

      <!-- 底部占位 -->
      <view class="safe-area-bottom"></view>
    </scroll-view>

    <!-- 底部固定操作栏 -->
    <view class="fixed-footer">
      <view class="footer-btn-group">
        <view class="main-action" @click="toShopMg">
          编辑
        </view>
      </view>
    </view>

    <tn-tips ref="tips"></tn-tips>
  </view>
</template>

<script>
import { savej, listj, findj, fileUrl } from '@/common/config/api.js'
import { SHOP_AUDIT_STATE } from '@/common/shopState.js'
import { mapState, mapActions } from 'vuex'
import { ideautil, yewuutil } from '@/common/commontools.js'
import shopDishReviewsMixin from './mixins/shopDishReviewsMixin.js'

export default {
  mixins: [shopDishReviewsMixin],
  data() {
    return {
      SHOP_AUDIT_STATE,
      pid: null,
      fileUrl: fileUrl,
      fobj: {},
      questionList: [],
      answerList: [],
      qnote: ''
    };
  },
  onLoad(params) {
    this.pid = params.pid
    this.fobjDetail()
  },
  methods: {
    ...mapActions(['setCar']),
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
      const note = `回复了你的提问：${(qnote || '').slice(0, 24)}${(qnote || '').length > 24 ? '…' : ''}\n${(anote || '').slice(0, 30)}${(anote || '').length > 30 ? '…' : ''}`
      return findj({
        params: { table: 'huihua', uid: toUid, fid: fromUser.id, msgtype: 9, sid, qid }
      }).then(old => {
        const data = old ? { ...old } : {}
        data.table = 'huihua'
        data.type = 1
        data.uid = toUid
        data.fid = fromUser.id
        data.fusername = fromUser.username
        data.img = fromUser.img
        data.note = note
        data.msgtype = 9
        data.sid = sid
        data.shop = shopName
        data.qid = qid
        data.ndate = null
        return savej({ params: data })
      }).catch(() => {})
    },
    fobjDetail() {
      findj({params: {table: 'shop', id: this.pid}}).then(res => {
        this.fobj = res
        this.fobj.note = ideautil.getHtmlNote(this.fobj.note)
        getApp().globalData.focusshop = res
        this.loadQA()
        this.loadShopDishReplays()
      }).catch(err => {})
    },
    toShopMg() {
      uni.navigateTo({
        url: '/pages/shops/shopmg'
      })
    },
    toGoodView() {
      uni.navigateTo({
        url: `/pages/good/good?sid=${this.fobj.id}`
      })
    },
    callPhone() {
      if(this.fobj.tel) {
        uni.makePhoneCall({ phoneNumber: this.fobj.tel });
      }
    },
    openMap(){
      if(!this.fobj.latitude) {
        uni.showToast({ title: '暂无位置坐标', icon: 'none' });
        return;
      }
      uni.openLocation({
        latitude: Number(this.fobj.latitude),
        longitude: Number(this.fobj.longitude),
        address: this.fobj.address,
        name: this.fobj.sname
      })
    },
    loadQA(){
      listj({params: {table: 'shop_qa', sid: this.fobj.id, qtype: 'Q'}}).then(qs => {
        this.questionList = (qs || []).map(q => ({
          ...q,
          _answers: [],
          _showAll: false,
          _anote: ''
        }))
        return listj({params: {table: 'shop_qa', sid: this.fobj.id, qtype: 'A'}})
      }).then(ans => {
        this.answerList = ans || []
        const qidSet = new Set((this.questionList || []).map(q => String(q.id)))
        const grouped = {}
        this.answerList.forEach(a => {
          const qid = String(a.qid || '')
          if(!qidSet.has(qid)) return
          if(!grouped[qid]) grouped[qid] = []
          grouped[qid].push(a)
        })
        Object.keys(grouped).forEach(k => {
          grouped[k].sort((a,b) => (b.id||0) - (a.id||0))
        })
        this.questionList = (this.questionList || []).map(q => ({
          ...q,
          _answers: grouped[String(q.id)] || []
        }))
      }).catch(() => {})
    },
    toggleAnswers(q){
      q._showAll = !q._showAll
      this.questionList = [...this.questionList]
    },
    submitQuestion(){
      if(!this.userInfo || !this.userInfo.id) {
        uni.showToast({ title: '请先登录', icon: 'none' });
        return;
      }
      if(!this.qnote){
        uni.showToast({ title: '请输入问题内容', icon: 'none' });
        return;
      }
      const fdata = {
        table: "shop_qa",
        sid: this.fobj.id,
        shop: this.fobj.sname,
        note: this.qnote,
        uid: this.userInfo.id,
        username: this.userInfo.username,
        roletype: this.userInfo.roletype,
        qtype: 'Q'
      }
      const txt = this.qnote
      savej({params: fdata}).then(res => {
        this.qnote = ''
        this.loadQA()
        this.$refs.tips.show({ msg: '提问成功' })

        const qid = (res && res.id) ? res.id : undefined
        this.notifyMerchantNewQuestion({
          sid: this.fobj.id,
          shopName: this.fobj.sname,
          qid,
          qnote: txt,
          fromUser: this.userInfo
        })
      })
    },
    submitAnswer(q){
      if(!this.userInfo || !this.userInfo.id) {
        uni.showToast({ title: '请先登录', icon: 'none' });
        return;
      }
      const txt = (q && q._anote) ? q._anote.trim() : ''
      if(!txt){
        uni.showToast({ title: '请输入回答内容', icon: 'none' });
        return;
      }
      const fdata = {
        table: "shop_qa",
        sid: this.fobj.id,
        shop: this.fobj.sname,
        qid: q.id,
        note: txt,
        uid: this.userInfo.id,
        username: this.userInfo.username,
        roletype: this.userInfo.roletype,
        qtype: 'A'
      }
      savej({params: fdata}).then(() => {
        if (q && String(q.uid) && String(q.uid) !== String(this.userInfo.id)) {
          this.upsertQaReplyNotify({
            toUid: q.uid,
            fromUser: this.userInfo,
            sid: this.fobj.id,
            shopName: this.fobj.sname,
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
    ,
    toQaPage(){
      uni.navigateTo({
        url: `/pages/shops/shopqa?sid=${this.fobj.id}&shop=${encodeURIComponent(this.fobj.sname || '')}`
      })
    }
  },
  computed: {
    ...mapState(['carlist', 'userInfo'])
  }
}
</script>

<style lang="scss" scoped>
/* 颜色变量 */
$primary-color: #ff943c;
$bg-color: #f8f9fb;
$card-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.05);

.page-container {
  background-color: $bg-color;
  min-height: 100vh;
}

.main-scroll {
  height: calc(100vh - 120rpx);
}

/* 审核状态提示条 */
.audit-banner {
  padding: 16rpx 24rpx;
  background-color: #fff7e6;
  border-bottom: 1rpx solid #ffe7ba;
  display: flex;
  align-items: center;
}

.audit-text {
  font-size: 24rpx;
  color: #d46b08;
}

/* 头部大图 */
.hero-section {
  width: 100%;
  height: 480rpx;
  position: relative;

  .hero-image {
    width: 100%;
    height: 100%;
  }

  .hero-overlay {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 160rpx;
    background: linear-gradient(transparent, $bg-color);
  }
}

/* 档口信息卡片 */
.info-card {
  margin: -80rpx 30rpx 30rpx;
  background-color: #ffffff;
  border-radius: 32rpx;
  padding: 40rpx;
  box-shadow: $card-shadow;
  position: relative;
  z-index: 10;

  .shop-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30rpx;

    .shop-name {
      font-size: 44rpx;
      font-weight: 800;
      color: #1a1a1a;
    }

    .shop-tag {
      padding: 4rpx 16rpx;
      background-color: #fff0e6;
      color: $primary-color;
      font-size: 22rpx;
      border-radius: 8rpx;
      font-weight: bold;
    }
  }

  .info-item {
    display: flex;
    align-items: center;
    padding: 24rpx 0;
    border-top: 1rpx solid #f2f3f5;

    .info-icon {
      font-size: 32rpx;
      margin-right: 20rpx;
    }

    .info-text {
      flex: 1;
      font-size: 28rpx;
      color: #606266;
      line-height: 1.4;
    }

    .info-action {
      font-size: 24rpx;
      color: $primary-color;
      font-weight: bold;
      padding-left: 20rpx;
    }
  }
}

/* 菜品列表入口 */
.food-list-entry {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  margin: 0 30rpx 30rpx;
  padding: 28rpx 24rpx;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);

  .entry-left {
    display: flex;
    align-items: center;

    .entry-icon {
      font-size: 40rpx;
      margin-right: 16rpx;
    }

    .entry-title {
      font-size: 30rpx;
      font-weight: bold;
      color: #333;
    }
  }

  .entry-right {
    display: flex;
    align-items: center;

    .entry-desc {
      font-size: 26rpx;
      color: #999;
      margin-right: 8rpx;
    }
  }
}

/* 通用章节标题 */
.section-title {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;

  .title-line {
    width: 8rpx;
    height: 32rpx;
    background-color: $primary-color;
    border-radius: 4rpx;
    margin-right: 16rpx;
  }

  text {
    font-size: 32rpx;
    font-weight: bold;
    color: #1a1a1a;
  }

  .comment-count {
    margin-left: 10rpx;
    color: #909399;
    font-weight: normal;
    font-size: 24rpx;
  }

  .qa-more {
    margin-left: auto;
    font-size: 26rpx;
    color: $primary-color;
    font-weight: 600;
  }
}

/* 问答预览区（淘宝问大家风格） */
.qa-preview {
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f2f3f5;

  &:last-of-type { border-bottom: none; }
}

.qa-q {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  margin-bottom: 12rpx;
}

.qa-badge {
  flex-shrink: 0;
  width: 36rpx;
  height: 36rpx;
  line-height: 36rpx;
  text-align: center;
  font-size: 22rpx;
  font-weight: bold;
  background: linear-gradient(135deg, #ffb347, #ff6a00);
  color: #fff;
  border-radius: 8rpx;

  &.a {
    background: linear-gradient(135deg, #4facfe, #00f2fe);
  }
}

.qa-text {
  flex: 1;
  font-size: 28rpx;
  color: #303133;
  line-height: 1.5;
}

.qa-stat {
  flex-shrink: 0;
  font-size: 22rpx;
  color: #909399;
}

.qa-a {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  padding-left: 48rpx;
  margin-top: 8rpx;
}

.qa-atext {
  flex: 1;
  font-size: 26rpx;
  color: #606266;
  line-height: 1.5;
}

.qa-entry {
  margin-top: 24rpx;
  display: flex;
  justify-content: center;
}

/* 内容章节 */
.content-section, .comment-section {
  margin: 0 30rpx 30rpx;
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 30rpx;
  box-shadow: $card-shadow;
}

/* 本店菜品评价区（对齐 gooddetail 评价 UI） */
.review-section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
  flex-wrap: wrap;
  gap: 12rpx;
}
.review-section-title {
  margin-bottom: 0;
  flex: 1;
  min-width: 0;
}
.review-count-paren {
  margin-left: 6rpx;
  color: #909399;
  font-size: 24rpx;
  font-weight: normal;
}
.review-dish-hint {
  display: block;
  font-size: 24rpx;
  color: #909399;
  line-height: 1.5;
  margin-bottom: 20rpx;
}
.review-section {
  .filter-reset {
    font-size: 24rpx;
    color: $primary-color;
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
    &.on { color: #ff4d4f; }
  }
  .rss-cols { display: flex; justify-content: space-between; }
  .rss-col { flex: 1; text-align: center; font-size: 22rpx; }
  .rss-mini { display: block; color: #333; font-weight: 600; margin-bottom: 6rpx; }
  .rss-cap { color: #999; font-size: 20rpx; }
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
  .rt-count { font-size: 24rpx; color: #bbb; font-weight: 400; }
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
  .rs-arrow { font-size: 20rpx; color: #999; transform: scale(0.85); }
  .rs-count { font-size: 22rpx; color: #aaa; }
  &.on .rs-count { color: #ff8787; }
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
  .kw-n { font-size: 22rpx; color: #aaa; margin-left: 6rpx; }
  &.on .kw-n { color: #ff8787; }
}
.review-ai-box {
  margin: 16rpx 0 8rpx;
  padding: 20rpx 24rpx;
  background: linear-gradient(135deg, #f0f7ff 0%, #e8f4fc 100%);
  border-radius: 16rpx;
  border: 1rpx solid #d6e8f5;
  .rai-head { display: flex; align-items: center; gap: 12rpx; margin-bottom: 12rpx; }
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
  .rai-title { font-size: 26rpx; font-weight: 600; color: #2c6aa0; }
  .rai-line { font-size: 24rpx; color: #4a6d8c; line-height: 1.55; margin-top: 8rpx; }
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

.rich-text-wrapper {
  font-size: 28rpx;
  line-height: 1.6;
  color: #444;
}

/* 评论样式 */
.comment-list {
  .comment-item {
    padding: 24rpx 0;
    border-bottom: 1rpx solid #f2f3f5;

    &:last-child { border-bottom: none; }

    .comment-user {
      display: flex;
      align-items: center;
      margin-bottom: 12rpx;

      .user-avatar {
        width: 64rpx;
        height: 64rpx;
        background-color: $primary-color;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        font-weight: bold;
        margin-right: 16rpx;
      }

      .user-meta {
        display: flex;
        flex-direction: column;

        .user-name {
          font-size: 26rpx;
          font-weight: bold;
          color: #303133;
        }

        .comment-date {
          font-size: 22rpx;
          color: #909399;
        }
      }
    }

    .comment-content {
      font-size: 28rpx;
      color: #606266;
      line-height: 1.5;
      padding-left: 80rpx;
    }
  }
}

.comment-empty {
  text-align: center;
  padding: 40rpx 0;
  color: #909399;
  font-size: 26rpx;
}

.input-wrapper {
  margin-top: 30rpx;
  background-color: #f7f8fa;
  border-radius: 20rpx;
  padding: 20rpx;

  .submit-btn-box {
    display: flex;
    justify-content: flex-end;
    margin-top: 16rpx;
  }
}

/* 底部操作栏 */
.fixed-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 120rpx;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  padding: 0 30rpx;
  padding-bottom: env(safe-area-inset-bottom);
  box-shadow: 0 -4rpx 20rpx rgba(0,0,0,0.05);
  z-index: 100;

  .footer-btn-group {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .main-action {
    flex: 1;
    height: 84rpx;
    background: linear-gradient(135deg, $primary-color, #ffb366);
    color: #fff;
    border-radius: 42rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 30rpx;
    font-weight: bold;
    box-shadow: 0 6rpx 16rpx rgba(255, 148, 60, 0.3);

    &:active {
      opacity: 0.9;
      transform: scale(0.98);
    }
  }
}

.safe-area-bottom {
  height: 140rpx;
}
</style>
