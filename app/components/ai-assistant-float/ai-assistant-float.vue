<template>
  <view v-if="fabVisible" class="ai-float-root">
    <view v-if="!open" class="fab" @tap="open = true">
      <text class="fab-ico">🤖</text>
    </view>
    <view v-else class="panel-mask" @tap="open = false"></view>
    <view v-if="open" class="panel" @tap.stop>
      <view class="panel-drag-bar"></view>
      <view class="panel-hd">
        <view class="panel-hd-left">
          <text class="panel-emoji">🤖</text>
          <text class="panel-title">{{ panelTitle }}</text>
        </view>
        <text class="panel-close" @tap="open = false">收起</text>
      </view>
      <text class="panel-tip">{{ panelTip }}</text>
      <scroll-view scroll-y class="panel-scroll" :scroll-top="scrollTop" :show-scrollbar="false">
        <scroll-view v-if="!messages.length" scroll-x class="chip-scroll" :show-scrollbar="false">
          <view class="chip-row">
            <text class="chip" v-for="(t, j) in chips" :key="j" @tap="setDraft(t)">{{ t }}</text>
          </view>
        </scroll-view>
        <view v-if="!messages.length" class="panel-empty">
          <text>{{ emptyHint }}</text>
        </view>
        <view v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role === 'user' ? 'msg-row-user' : ''">
          <view :class="['bubble', m.role === 'user' ? 'bubble-user' : 'bubble-ai']">
            <text class="bubble-t">{{ m.text }}</text>
          </view>
        </view>
      </scroll-view>
      <view class="panel-ft safe-ft">
        <view class="inp-wrap">
          <textarea
            v-model="draft"
            class="inp-ta"
            :placeholder="inputPlaceholder"
            placeholder-class="inp-ph"
            :maxlength="800"
            :adjust-position="true"
            :cursor-spacing="24"
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
  name: 'AiAssistantFloat',
  data() {
    return {
      open: false,
      messages: [],
      draft: '',
      loading: false,
      scrollTop: 0,
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
      },
      chipsMerchant: ['最近生意大概怎么样？', '低分预警要怎么处理？', '优惠券设置要注意什么？'],
      chipsUser: ['附近有什么好吃的推荐？', '怎么查看我的订单和评价？', '下单后多久能到店取餐？']
    }
  },
  computed: {
    ...mapState(['userInfo']),
    fabVisible() {
      if (!this.userInfo || !this.userInfo.id) return false
      try {
        const pages = getCurrentPages()
        const route = pages && pages.length ? pages[pages.length - 1].route || '' : ''
        if (
          route === 'pages/login/login' ||
          route === 'pages/login/register' ||
          route === 'pages/login/rootip' ||
          route === 'pages/assistant/assistant'
        ) {
          return false
        }
      } catch (e) {}
      return true
    },
    isMerchant() {
      return this.userInfo && String(this.userInfo.roletype) === '3'
    },
    panelTitle() {
      return this.isMerchant ? 'AI小助手（本店）' : 'AI小助手'
    },
    panelTip() {
      return this.isMerchant
        ? '本店经营问答；已登录时将附带您店铺的经营与评分等数据摘要，仅供参考。'
        : '美食与订单问答；已登录时将附带订单与评价概况。问附近店铺时请允许定位，以便按距离列出平台店铺。'
    },
    emptyHint() {
      return this.isMerchant
        ? '问问本店评分、优惠、客流～'
        : '问问推荐、订单、评价～'
    },
    inputPlaceholder() {
      return this.isMerchant ? '输入本店相关问题…' : '输入您想了解的问题…'
    },
    chips() {
      return this.isMerchant ? this.chipsMerchant : this.chipsUser
    }
  },
  watch: {
    open(v) {
      if (!v) return
      this.$nextTick(() => {
        this.scrollTop = 999999
      })
    }
  },
  methods: {
    push(role, text) {
      this.messages.push({ role, text })
      this.$nextTick(() => {
        this.scrollTop = 999999
      })
    },
    setDraft(t) {
      this.draft = t
    },
    send() {
      const q = (this.draft || '').trim()
      if (!q || this.loading) return
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
.ai-float-root {
  position: relative;
  z-index: 99990;
  pointer-events: none;
}
.fab {
  pointer-events: auto;
  position: fixed;
  right: 28rpx;
  bottom: calc(140rpx + env(safe-area-inset-bottom));
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff943c, #ff6b35);
  box-shadow: 0 12rpx 32rpx rgba(255, 107, 53, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99991;
}
.fab-ico {
  font-size: 52rpx;
  line-height: 1;
}
.panel-mask {
  pointer-events: auto;
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.42);
  z-index: 99992;
}
.panel {
  pointer-events: auto;
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 74vh;
  max-height: 920rpx;
  background: linear-gradient(180deg, #fffdfb 0%, #ffffff 18%);
  border-radius: 28rpx 28rpx 0 0;
  box-shadow: 0 -12rpx 48rpx rgba(15, 23, 42, 0.12);
  z-index: 99993;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.panel-drag-bar {
  width: 72rpx;
  height: 8rpx;
  border-radius: 999rpx;
  background: #e5e7eb;
  margin: 12rpx auto 0;
}
.panel-hd {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 24rpx 8rpx;
}
.panel-hd-left {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.panel-emoji {
  font-size: 36rpx;
}
.panel-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #111827;
}
.panel-close {
  font-size: 28rpx;
  color: #2563eb;
  font-weight: 600;
}
.panel-tip {
  flex-shrink: 0;
  font-size: 22rpx;
  color: #9ca3af;
  padding: 0 24rpx 12rpx;
  line-height: 1.45;
}
.panel-scroll {
  flex: 1;
  height: 0;
  padding: 8rpx 20rpx 12rpx;
  box-sizing: border-box;
}
.chip-scroll {
  width: 100%;
  white-space: nowrap;
  flex-shrink: 0;
  padding: 0 8rpx 8rpx;
  box-sizing: border-box;
}
.chip-row {
  display: inline-flex;
  gap: 12rpx;
  padding: 4rpx 12rpx;
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
.panel-empty {
  text-align: center;
  padding: 32rpx 24rpx 48rpx;
  font-size: 26rpx;
  color: #d1d5db;
}
.msg-row {
  display: flex;
  margin-bottom: 16rpx;
}
.msg-row-user {
  justify-content: flex-end;
}
.bubble {
  max-width: 88%;
  padding: 16rpx 20rpx;
  border-radius: 18rpx;
}
.bubble-user {
  background: linear-gradient(135deg, #fff7ed, #ffedd5);
  border: 1rpx solid #fdba74;
}
.bubble-ai {
  background: #fff;
  border: 1rpx solid #e5e7eb;
  box-shadow: 0 4rpx 14rpx rgba(15, 23, 42, 0.05);
}
.bubble-t {
  font-size: 26rpx;
  color: #374151;
  line-height: 1.5;
  word-break: break-word;
}
.panel-ft {
  flex-shrink: 0;
  display: flex;
  flex-direction: row;
  align-items: flex-end;
  gap: 14rpx;
  padding: 14rpx 20rpx 16rpx;
  border-top: 1rpx solid #f0f0f0;
  background: #fafafa;
}
.safe-ft {
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
}
.inp-wrap {
  flex: 1;
  width: 0;
  min-width: 0;
  overflow: hidden;
  min-height: 76rpx;
  max-height: 200rpx;
  background: #f3f4f6;
  border-radius: 18rpx;
  padding: 12rpx 16rpx;
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
  max-height: 200rpx;
  font-size: 28rpx;
  line-height: 1.45;
  color: #111827;
  box-sizing: border-box;
}
.inp-ph {
  color: #9ca3af;
}
</style>
