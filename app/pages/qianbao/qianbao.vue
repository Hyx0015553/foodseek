<template>
  <view class="wallet-page">
    <!-- 导航栏 -->
    <u-navbar title="我的钱包" :border="false" :placeholder="true" :autoBack="true" bgColor="#ffffff" titleStyle="color:#333;font-weight:bold;" leftIconColor="#333"></u-navbar>

    <scroll-view scroll-y class="content-scroll">
      <!-- 1. 余额展示卡片 -->
      <view class="balance-card">
        <view class="card-bg-decoration"></view>
        <view class="balance-content">
          <text class="label">当前可用余额 (元)</text>
          <view class="amount-row">
            <text class="symbol">￥</text>
            <text class="value">{{ userInfo.money || 0 }}</text>
          </view>
          <text class="wallet-emoji">👛</text>
        </view>
      </view>

      <!-- 支付密码设置入口 -->
      <view class="pwd-manage-card" @click="openPwdManage">
        <view class="pwd-left">
          <text class="pwd-icon">🔐</text>
          <view class="pwd-info">
            <text class="pwd-title">支付密码</text>
            <text class="pwd-desc">{{ hasPayPwd ? '已设置，可修改' : '未设置，请先设置' }}</text>
          </view>
        </view>
        <view class="pwd-arrow">›</view>
      </view>

      <!-- 2. 充值功能区 -->
      <view class="recharge-section">
        <view class="section-title">
          <text class="emoji">💹</text>
          <text class="text">账户充值</text>
        </view>

        <!-- 快捷选择金额 -->
        <view class="quick-pick">
          <view
              class="pick-item"
              v-for="amt in [10, 50, 100, 200, 500]"
              :key="amt"
              :class="{'active': money == amt}"
              @tap="money = amt"
          >
            ￥{{ amt }}
          </view>
        </view>

        <!-- 自定义输入 -->
        <view class="input-group">
          <text class="input-label">自定义金额</text>
          <view class="input-box">
            <text class="unit">￥</text>
            <input type="number" v-model="money" placeholder="请输入充值金额" placeholder-class="ph-style" />
          </view>
        </view>

        <!-- 支付方式选择 -->
        <view class="pay-type-selector" @tap="asshow = true">
          <view class="selector-left">
            <text class="selector-emoji">💳</text>
            <text class="selector-label">支付方式</text>
          </view>
          <view class="selector-right">
            <text class="selected-val">{{ paytype }}</text>
            <text class="arrow">❯</text>
          </view>
        </view>

        <!-- 提交按钮 -->
        <button class="submit-btn" @tap="charge">立即充值 🚀</button>

        <view class="tips-box">
          <text class="tips-text">温馨提示：充值实时到账，如遇延迟请刷新页面或联系客服。</text>
        </view>
      </view>

      <view class="safe-bottom"></view>
    </scroll-view>

    <!-- 支付方式动作面板 -->
    <u-action-sheet
        :show="asshow"
        :actions="[{name:'微信支付'}, {name:'支付宝'}, {name:'银行卡'}]"
        title="选择充值方式"
        @close="asshow = false"
        @select="asSelect"
        round="20"
    />

    <!-- 支付密码设置弹窗 -->
    <u-popup :show="pwdShow" mode="bottom" round="24" @close="pwdShow = false" :closeable="true">
      <view class="pwd-popup-content">
        <view class="popup-title">{{ pwdStep === 1 ? (hasPayPwd ? '修改支付密码' : '设置支付密码') : '确认支付密码' }}</view>

        <!-- 第一步：输入密码 -->
        <view class="pwd-step" v-if="pwdStep === 1">
          <view class="pwd-tips">请输入6位数字支付密码</view>
          <view class="pwd-input-row">
            <input
              class="pwd-input"
              type="number"
              :maxlength="6"
              v-model="pwdFirst"
              password
              placeholder="请输入6位数字"
              placeholder-class="ph-style"
            />
          </view>
          <button class="pwd-next-btn" @click="checkPwd1">下一步</button>
        </view>

        <!-- 第二步：确认密码 -->
        <view class="pwd-step" v-if="pwdStep === 2">
          <view class="pwd-tips">请再次输入支付密码以确认</view>
          <view class="pwd-input-row">
            <input
              class="pwd-input"
              type="number"
              :maxlength="6"
              v-model="pwdSecond"
              password
              placeholder="请再次输入6位数字"
              placeholder-class="ph-style"
            />
          </view>
          <view class="pwd-btn-row">
            <button class="pwd-cancel-btn" @click="pwdStep = 1">上一步</button>
            <button class="pwd-confirm-btn" @click="savePwd">确认设置</button>
          </view>
        </view>
      </view>
    </u-popup>

    <!-- 验证原密码弹窗（修改时） -->
    <u-popup :show="verifyOldShow" mode="bottom" round="24" @close="verifyOldShow = false" :closeable="true">
      <view class="pwd-popup-content">
        <view class="popup-title">验证原密码</view>
        <view class="pwd-tips">请输入原支付密码</view>
        <view class="pwd-input-row">
          <input
            class="pwd-input"
            type="number"
            :maxlength="6"
            v-model="verifyOldPwd"
            password
            placeholder="请输入原支付密码"
            placeholder-class="ph-style"
          />
        </view>
        <button class="pwd-next-btn" @click="verifyOldPwdFn">验证并修改</button>
      </view>
    </u-popup>
  </view>
</template>

<script>
import { savej } from '@/common/config/api.js'
import { mapState, mapActions } from 'vuex'
import { ideautil } from '@/common/commontools.js'

export default {
  data() {
    return {
      asshow: false,
      money: 100,
      paytype: "微信支付",
      // 支付密码相关
      pwdShow: false,
      verifyOldShow: false,
      pwdStep: 1,
      pwdFirst: '',
      pwdSecond: '',
      verifyOldPwd: ''
    }
  },
  computed: {
    hasPayPwd() {
      return !!(this.userInfo && this.userInfo.paypwd)
    }
  },
  computed: {
    ...mapState(['userInfo'])
  },
  methods: {
    ...mapActions(['updateUserInfo']),

    asSelect(v) {
      this.paytype = v.name;
      this.asshow = false;
    },

    charge() {
      if (this.money <= 0) {
        uni.showToast({ title: '请输入有效的充值金额', icon: 'none' });
        return;
      }

      uni.showLoading({ title: '处理中...' });

      // 逻辑计算
      let currentMoney = Number(this.userInfo.money || 0);
      let newMoney = currentMoney + Number(this.money);

      // 模拟支付并更新数据库
      savej({
        params: {
          table: 'user',
          id: this.userInfo.id,
          money: newMoney
        }
      }).then(res => {
        // 更新本地Vuex状态
        let newUser = { ...this.userInfo, money: newMoney };
        this.updateUserInfo(newUser);

        uni.hideLoading();
        uni.showToast({
          title: '充值成功！',
          icon: 'success'
        });

        // 充值后可重置金额，或者延迟返回
        setTimeout(() => {
          this.money = 0;
        }, 1500);
      }).catch(() => {
        uni.hideLoading();
      });
    },

    openPwdManage() {
      this.pwdStep = 1
      this.pwdFirst = ''
      this.pwdSecond = ''
      this.verifyOldPwd = ''
      if (this.hasPayPwd) {
        // 已设置过，先验证原密码
        this.verifyOldShow = true
      } else {
        // 未设置，直接打开设置弹窗
        this.pwdShow = true
      }
    },

    checkPwd1() {
      if (!/^\d{6}$/.test(this.pwdFirst)) {
        uni.showToast({ title: '请输入6位数字密码', icon: 'none' })
        return
      }
      this.pwdStep = 2
    },

    verifyOldPwdFn() {
      if (!this.verifyOldPwd) {
        uni.showToast({ title: '请输入原支付密码', icon: 'none' })
        return
      }
      if (String(this.verifyOldPwd) !== String(this.userInfo.paypwd)) {
        uni.showToast({ title: '原密码错误', icon: 'error' })
        return
      }
      this.verifyOldShow = false
      this.pwdShow = true
      this.pwdStep = 1
      this.pwdFirst = ''
      this.pwdSecond = ''
    },

    savePwd() {
      if (!/^\d{6}$/.test(this.pwdSecond)) {
        uni.showToast({ title: '请输入6位数字密码', icon: 'none' })
        return
      }
      if (this.pwdSecond !== this.pwdFirst) {
        uni.showToast({ title: '两次密码不一致', icon: 'none' })
        this.pwdSecond = ''
        return
      }
      uni.showLoading({ title: '保存中...' })
      savej({
        params: {
          table: 'user',
          id: this.userInfo.id,
          paypwd: this.pwdSecond
        }
      }).then(() => {
        let newUser = { ...this.userInfo, paypwd: this.pwdSecond }
        this.updateUserInfo(newUser)
        uni.hideLoading()
        this.pwdShow = false
        this.verifyOldShow = false
        uni.showToast({ title: '支付密码设置成功', icon: 'success' })
      }).catch(() => {
        uni.hideLoading()
        uni.showToast({ title: '保存失败，请重试', icon: 'none' })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.wallet-page {
  background-color: #f5f5f5;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 余额卡片样式 */
.balance-card {
  margin: 30rpx;
  height: 320rpx;
  background: linear-gradient(135deg, #ff7a1a 0%, #ff943c 50%, #ffab5c 100%);
  border-radius: 40rpx;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8rpx 24rpx rgba(255, 122, 26, 0.3);
  border: none;

  .card-bg-decoration {
    position: absolute;
    width: 300rpx;
    height: 300rpx;
    background: rgba(255, 255, 255, 0.15);
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
      color: rgba(255, 255, 255, 0.85);
      font-size: 26rpx;
      margin-bottom: 20rpx;
    }

    .amount-row {
      display: flex;
      align-items: baseline;
      color: #fff;
      .symbol { font-size: 40rpx; font-weight: bold; margin-right: 10rpx; }
      .value { font-size: 80rpx; font-weight: bold; letter-spacing: 2rpx; }
    }

    .wallet-emoji {
      position: absolute;
      bottom: 40rpx;
      right: 40rpx;
      font-size: 80rpx;
      opacity: 0.3;
    }
  }
}

/* 充值区域样式 */
.recharge-section {
  background-color: #ffffff;
  margin: 0 30rpx;
  border-radius: 40rpx;
  padding: 40rpx;
  box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.03);

  .section-title {
    display: flex;
    align-items: center;
    margin-bottom: 40rpx;
    .emoji { font-size: 36rpx; margin-right: 12rpx; }
    .text { font-size: 32rpx; font-weight: bold; color: #333; }
  }
}

/* 快捷选额 */
.quick-pick {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
  margin-bottom: 40rpx;

  .pick-item {
    flex: 1;
    min-width: 140rpx;
    height: 80rpx;
    line-height: 80rpx;
    text-align: center;
    background-color: #ffffff;
    border: 2rpx solid #eee;
    border-radius: 20rpx;
    font-size: 28rpx;
    color: #666;
    transition: all 0.2s;

    &.active {
      background-color: #fff5eb;
      border-color: #ff7a1a;
      color: #ff7a1a;
      font-weight: bold;
    }
  }
}

/* 输入框样式 */
.input-group {
  margin-bottom: 40rpx;
  .input-label { font-size: 26rpx; color: #999; margin-bottom: 20rpx; display: block; }
  .input-box {
    display: flex;
    align-items: center;
    background-color: #ffffff;
    height: 100rpx;
    border-radius: 20rpx;
    padding: 0 30rpx;
    border: 2rpx solid #eee;
    .unit { font-size: 32rpx; color: #333; font-weight: bold; margin-right: 10rpx; }
    input { flex: 1; font-size: 32rpx; color: #333; font-weight: bold; }
  }
}

/* 选择器样式 */
.pay-type-selector {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 30rpx 0;
  border-top: 1rpx solid #f2f5f8;
  border-bottom: 1rpx solid #f2f5f8;
  margin-bottom: 60rpx;

  .selector-left {
    display: flex;
    align-items: center;
    .selector-emoji { font-size: 32rpx; margin-right: 12rpx; }
    .selector-label { font-size: 28rpx; color: #666; }
  }

  .selector-right {
    display: flex;
    align-items: center;
    .selected-val { font-size: 28rpx; color: #333; font-weight: 500; }
    .arrow { font-size: 24rpx; color: #ccc; margin-left: 10rpx; }
  }
}

.submit-btn {
  width: 100%;
  height: 100rpx;
  line-height: 100rpx;
  background: linear-gradient(to right, #ff7a1a, #ff943c);
  color: #ffffff;
  border-radius: 50rpx;
  font-size: 32rpx;
  font-weight: bold;
  border: none;
  box-shadow: 0 10rpx 20rpx rgba(0, 122, 255, 0.2);

  &:active { transform: scale(0.98); opacity: 0.9; }
}

.tips-box {
  margin-top: 40rpx;
  .tips-text { font-size: 22rpx; color: #bbb; text-align: center; display: block; line-height: 1.4; }
}

.safe-bottom { height: 60rpx; }
.ph-style { color: #ccc; font-weight: normal; }

/* 支付密码入口卡片 */
.pwd-manage-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 30rpx 24rpx;
  padding: 30rpx;
  background: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);

  .pwd-left {
    display: flex;
    align-items: center;
    .pwd-icon { font-size: 44rpx; margin-right: 20rpx; }
    .pwd-info {
      display: flex;
      flex-direction: column;
      .pwd-title { font-size: 28rpx; color: #333; font-weight: bold; }
      .pwd-desc { font-size: 22rpx; color: #999; margin-top: 6rpx; }
    }
  }
  .pwd-arrow { font-size: 36rpx; color: #ccc; }
}

/* 支付密码弹窗 */
.pwd-popup-content {
  padding: 40rpx;
  .popup-title {
    font-size: 34rpx;
    font-weight: bold;
    text-align: center;
    margin-bottom: 40rpx;
  }
  .pwd-tips {
    font-size: 26rpx;
    color: #999;
    text-align: center;
    margin-bottom: 30rpx;
  }
  .pwd-input-row {
    margin-bottom: 40rpx;
    .pwd-input {
      width: 100%;
      height: 100rpx;
      background: #ffffff;
      border-radius: 16rpx;
      padding: 0 30rpx;
      font-size: 36rpx;
      letter-spacing: 8rpx;
      text-align: center;
      box-sizing: border-box;
      border: 2rpx solid #eee;
    }
  }
  .pwd-next-btn {
    width: 100%;
    height: 90rpx;
    line-height: 90rpx;
    background: linear-gradient(135deg, #ff7a1a, #ff943c);
    color: #fff;
    border-radius: 50rpx;
    font-size: 30rpx;
    font-weight: bold;
    border: none;
  }
  .pwd-btn-row {
    display: flex;
    gap: 20rpx;
    .pwd-cancel-btn {
      flex: 1;
      height: 90rpx;
      line-height: 90rpx;
      background: #f5f5f5;
      color: #666;
      border-radius: 50rpx;
      font-size: 30rpx;
      border: none;
    }
    .pwd-confirm-btn {
      flex: 1;
      height: 90rpx;
      line-height: 90rpx;
      background: linear-gradient(135deg, #ff7a1a, #ff943c);
      color: #fff;
      border-radius: 50rpx;
      font-size: 30rpx;
      font-weight: bold;
      border: none;
    }
  }
}
</style>