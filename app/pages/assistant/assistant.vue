<template>
  <view class="page">
    <view class="hero">
      <view class="hero-avatar">🤖</view>
      <view class="hero-info">
        <text class="hero-title">{{ heroTitle }}</text>
        <text class="hero-sub">{{ heroSub }}</text>
      </view>
    </view>

    <scroll-view scroll-y class="msg-scroll" :scroll-top="scrollTop" scroll-with-animation :show-scrollbar="false">
      <view v-if="!messages.length" class="empty-hint">
        <text class="empty-title">{{ emptyTitle }}</text>
        <text class="empty-desc">{{ emptyDesc }}</text>
      </view>
      <view v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role === 'user' ? 'msg-row-user' : 'msg-row-ai'">
        <view class="avatar" v-if="m.role === 'ai'">🤖</view>
        <view :class="['bubble', m.role === 'user' ? 'bubble-user' : 'bubble-ai']">
          <text class="bubble-text">{{ m.text }}</text>
        </view>
        <view class="avatar user-av" v-if="m.role === 'user'">我</view>
      </view>
    </scroll-view>

    <view class="foot safe-bottom">
      <scroll-view v-if="!messages.length" scroll-x class="chip-scroll" :show-scrollbar="false">
        <view class="chip-row">
          <text class="chip" v-for="(t, j) in quickChips" :key="j" @tap="setQ(t)">{{ t }}</text>
        </view>
      </scroll-view>
      <view class="input-bar">
        <view class="inp-wrap">
          <textarea
            v-model="draft"
            class="inp-ta"
            :placeholder="inputPlaceholder"
            placeholder-class="inp-ph"
            :maxlength="800"
            :adjust-position="true"
            :cursor-spacing="20"
            :show-confirm-bar="true"
            :auto-height="false"
            :disabled="loading"
            confirm-type="send"
            @confirm="send"
          />
        </view>
        <view class="send-col">
          <u-button
            type="primary"
            shape="circle"
            size="small"
            :customStyle="sendBtnStyle"
            :loading="loading"
            :disabled="loading"
            :text="loading ? '发送中' : '发送'"
            @click="send"
          />
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { aiAssistantChatReq } from '@/common/config/api.js'
import { withAssistantLocation } from '@/common/aiAssistantSend.js'
import { mapState } from 'vuex'

export default {
  data() {
    return {
      messages: [],
      draft: '',
      loading: false,
      scrollTop: 0,
      chipsMerchant: ['最近生意大概怎么样？', '低分预警要怎么处理？', '优惠券设置要注意什么？'],
      chipsUser: ['附近有什么好吃的推荐？', '怎么查看我的订单和评价？', '下单后多久能到店取餐？'],
      sendBtnStyle: {
        width: '148rpx',
        minWidth: '148rpx',
        maxWidth: '148rpx',
        height: '76rpx',
        padding: '0',
        margin: '0',
        background: 'linear-gradient(90deg, #ff943c, #ff6b35)',
        border: 'none',
        boxShadow: '0 8rpx 24rpx rgba(255,107,53,0.35)'
      }
    }
  },
  onLoad(options) {
    if (options && options.from === 'merchant') {
      uni.setNavigationBarTitle({ title: 'AI小助手（本店）' })
    }
  },
  computed: {
    ...mapState(['userInfo']),
    isMerchant() {
      return this.userInfo && String(this.userInfo.roletype) === '3'
    },
    heroTitle() {
      return this.isMerchant ? 'AI 小助手 · 本店' : 'AI 小助手'
    },
    heroSub() {
      return this.isMerchant
        ? '已登录时将附带本店经营、评分与订单等数据摘要，仅供参考。'
        : '已登录时将附带您近期的订单与评价概况；询问附近店铺时若允许定位，将按距离列出平台店铺供参考。'
    },
    emptyTitle() {
      return this.isMerchant ? '问问本店经营' : '随便问问'
    },
    emptyDesc() {
      return this.isMerchant
        ? '试试下方店铺快捷句，或在输入框描述您的问题'
        : '试试下方常用问题，或在输入框输入想了解的内容'
    },
    inputPlaceholder() {
      return this.isMerchant ? '输入本店相关问题…' : '输入您想了解的问题…'
    },
    quickChips() {
      return this.isMerchant ? this.chipsMerchant : this.chipsUser
    }
  },
  methods: {
    setQ(t) {
      this.draft = t
    },
    push(role, text) {
      this.messages.push({ role, text })
      this.$nextTick(() => {
        this.scrollTop = 999999
      })
    },
    send() {
      const q = (this.draft || '').trim()
      if (!q || this.loading) return
      if (!this.userInfo || !this.userInfo.id) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }
      this.push('user', q)
      this.draft = ''
      this.loading = true
      const baseParams = {
        q,
        roletype: String(this.userInfo.roletype != null ? this.userInfo.roletype : ''),
        uid: this.userInfo.id
      }
      if (String(this.userInfo.roletype) === '3' && this.userInfo.sid) {
        baseParams.sid = this.userInfo.sid
      }
      withAssistantLocation(this.userInfo, baseParams)
        .then((params) => aiAssistantChatReq({ params }))
        .then((res) => {
          if (!res || !res.ok) {
            this.push('ai', '抱歉：' + ((res && res.error) ? res.error : '服务异常'))
            return
          }
          let head = res.hasContext ? '【已结合系统数据】\n' : ''
          this.push('ai', head + (res.reply || '（无回复）'))
        })
        .catch(() => {
          this.push('ai', '网络异常，请稍后再试')
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fff7ed 0%, #f6f7f9 22%);
  display: flex;
  flex-direction: column;
}
.hero {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  padding: 24rpx 28rpx 16rpx;
}
.hero-avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #ff943c, #ff6b35);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44rpx;
  box-shadow: 0 12rpx 28rpx rgba(255, 107, 53, 0.3);
}
.hero-info {
  flex: 1;
  min-width: 0;
}
.hero-title {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #111827;
  margin-bottom: 8rpx;
}
.hero-sub {
  font-size: 24rpx;
  color: #6b7280;
  line-height: 1.45;
}
.msg-scroll {
  flex: 1;
  height: 0;
  padding: 8rpx 24rpx 16rpx;
  box-sizing: border-box;
}
.empty-hint {
  padding: 48rpx 16rpx;
  text-align: center;
}
.empty-title {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #9ca3af;
  margin-bottom: 12rpx;
}
.empty-desc {
  font-size: 24rpx;
  color: #d1d5db;
}
.msg-row {
  display: flex;
  align-items: flex-end;
  gap: 12rpx;
  margin-bottom: 20rpx;
}
.msg-row-user {
  flex-direction: row-reverse;
}
.avatar {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #fef3c7;
  font-size: 22rpx;
  color: #b45309;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.user-av {
  background: #e0f2fe;
  color: #0369a1;
  font-weight: 700;
  font-size: 24rpx;
}
.bubble {
  max-width: calc(100% - 80rpx);
  padding: 18rpx 22rpx;
  border-radius: 20rpx;
  font-size: 28rpx;
  line-height: 1.55;
  word-break: break-word;
}
.bubble-user {
  background: linear-gradient(135deg, #fff7ed, #ffedd5);
  border: 1rpx solid #fdba74;
}
.bubble-ai {
  background: #ffffff;
  border: 1rpx solid #e5e7eb;
  box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.04);
}
.bubble-text {
  color: #374151;
}
.foot {
  flex-shrink: 0;
  background: #fff;
  border-top: 1rpx solid #f0f0f0;
  padding: 16rpx 20rpx 20rpx;
  box-shadow: 0 -8rpx 30rpx rgba(15, 23, 42, 0.06);
}
.safe-bottom {
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
}
.chip-scroll {
  width: 100%;
  white-space: nowrap;
  margin-bottom: 16rpx;
}
.chip-row {
  display: inline-flex;
  gap: 12rpx;
  padding: 4rpx 0;
}
.chip {
  display: inline-block;
  font-size: 24rpx;
  color: #c2410c;
  background: #fff7ed;
  padding: 12rpx 22rpx;
  border-radius: 999rpx;
  border: 1rpx solid #fdba74;
}
.input-bar {
  display: flex;
  align-items: flex-end;
  gap: 16rpx;
}
.inp-wrap {
  flex: 1;
  width: 0;
  min-width: 0;
  overflow: hidden;
  min-height: 76rpx;
  max-height: 220rpx;
  background: #f3f4f6;
  border-radius: 20rpx;
  padding: 14rpx 18rpx;
  border: 2rpx solid #d1d5db;
  box-shadow: inset 0 1rpx 2rpx rgba(15, 23, 42, 0.04);
}
.send-col {
  flex-shrink: 0;
  width: 148rpx;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 2rpx;
}
.inp-ta {
  display: block;
  width: 100%;
  height: 120rpx;
  min-height: 120rpx;
  max-height: 180rpx;
  font-size: 28rpx;
  line-height: 1.45;
  color: #111827;
  box-sizing: border-box;
}
.inp-ph {
  color: #9ca3af;
}
</style>
