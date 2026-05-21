<template>
  <view class="page-container">
    <!-- 导航栏 -->
    <u-navbar title="确认订单" :border="false" :placeholder="true" :autoBack="true" bgColor="#ffffff" titleStyle="font-weight: bold;"></u-navbar>

    <scroll-view scroll-y class="sv-container">
      <view class="content-wrapper">
        <!-- 1. 商品信息卡片 -->
        <view class="order-card goods-card">
          <view class="card-title">
            <text class="title-text">订单内容</text>
          </view>
          <view class="goods-info-box">
            <view class="goods-names">{{ gnames }}</view>
            <view class="price-detail">
              <view class="price-line">
                <text>商品总额</text>
                <text>¥{{ total }}</text>
              </view>
              <view class="price-line" v-if="yhq && yhq.total">
                <text>优惠金额</text>
                <text style="color: #ff4d4f;">-¥{{ yhq.total }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 用餐方式：堂食 / 外带，写入订单 way -->
        <view class="order-card way-card">
          <view class="card-title">
            <text class="title-text">用餐方式</text>
            <text class="title-sub">请选择堂食或外带</text>
          </view>
          <view class="way-segment">
            <view
              class="way-item"
              :class="{ active: way === '堂食' }"
              @click="way = '堂食'"
            >
              <text class="way-icon">🍽</text>
              <text class="way-label">堂食</text>
            </view>
            <view
              class="way-item"
              :class="{ active: way === '外带' }"
              @click="way = '外带'"
            >
              <text class="way-icon">🥡</text>
              <text class="way-label">外带</text>
            </view>
          </view>
        </view>

        <!-- 2. 预约与备注 -->
        <view class="order-card option-card">
          <view class="option-item">
            <text class="label">预约时间</text>
            <view class="picker-wrap">
              <ideadatetime v-model="ydate" label="请选择"></ideadatetime>
            </view>
          </view>
          <view class="note-section">
            <text class="label">订单备注</text>
            <u--textarea
                confirmType="done"
                v-model="bnote"
                placeholder="如有口味偏好请备注..."
                border="none"
                height="60"
                customStyle="background-color: #f7f7f7; padding: 20rpx; border-radius: 12rpx; margin-top: 16rpx;"
            ></u--textarea>
          </view>
        </view>
      </view>

      <!-- 安全区底部占位 -->
      <view class="safe-area-inset-bottom"></view>
    </scroll-view>

    <!-- 底部固定结算栏 -->
    <view class="bottom-bar">
      <view class="bar-left">
        <text class="total-label">合计:</text>
        <text class="total-symbol">¥</text>
        <text class="total-value">{{ paytotal }}</text>
      </view>
      <view class="submit-btn" @click="popshow=true">立即支付</view>
    </view>

    <!-- 支付弹窗美化 -->
    <u-popup :show="popshow" mode="bottom" round="24" @close="popshow=false" :closeable="true">
      <view class="pay-popup-content">
        <view class="popup-title">确认支付</view>

        <!-- 优惠券选择 -->
        <view class="coupon-box">
          <view class="sub-title">可用优惠券</view>
          <scroll-view scroll-y class="coupon-scroll">
            <u-radio-group placement="column" @change="toggleTotal" iconPlacement="right">
              <u-radio
                  :customStyle="{padding: '24rpx 0', borderBottom: '1rpx solid #f2f2f2'}"
                  v-for="(item, index) in yhqlist"
                  :key="index"
                  :label="item.id == 0 ? '不使用优惠券' : (item.fulluse ? '满减券 满' + item.fulluse + '减' + item.total : '优惠卷 直减' + item.total)"
                  :name="item.id"
              ></u-radio>
            </u-radio-group>
          </scroll-view>
        </view>

        <!-- 最终金额 -->
        <view class="final-price-box">
          <text>支付金额</text>
          <view class="price-num">
            <text class="symbol">¥</text>
            <text>{{ paytotal }}</text>
          </view>
        </view>

        <!-- 密码输入区 -->
        <view class="pwd-section">
          <u-form-item borderBottom label="支付密码" labelWidth="80">
            <u--input border="none" type="password" v-model="passwd" :placeholder="hasPayPwd ? '请输入6位支付密码' : '请先在钱包设置支付密码'" :disabled="!hasPayPwd" />
          </u-form-item>
        </view>

        <button class="confirm-pay-btn" @click="payBill()">确认支付</button>
      </view>
    </u-popup>
  </view>
</template>

<script>
import {mapState, mapActions} from 'vuex'
import { listj, findj, savej } from '@/common/config/api.js';
import { ideautil } from '@/common/commontools.js';
import { BILL_STATE, billStateCode } from '@/common/billState.js';
export default {
  data() {
    return {
      bills: [],
      bnote:'',
      gnames:'',
      gids: '',
      paytotal:0,
      total:0,
      state: BILL_STATE.PENDING_PAY,
      statecn: '待付款',
      yhq:null,
      yhqlist:[],
      popshow:false,
      passwd:null,
      hasPayPwd: false,
      paygoodslist:[],
      way: '堂食',
      ydate:''
    };
  },
  methods: {
    ...mapActions(['updateUserInfo','setCar']),
    getNowStr() {
      const d = new Date()
      const pad = n => (n < 10 ? '0' + n : '' + n)
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    },
    /** 后端 save 成功时返回新 id 的纯文本或数字，不是 { id } */
    parseSaveId(res) {
      if (res == null || res === '') return null
      if (typeof res === 'object' && res.id != null) return Number(res.id)
      const s = String(res).trim()
      if (/^\d+$/.test(s)) return Number(s)
      return null
    },
    goBack(){
      uni.navigateBack({ delta:1 })
    },
    async billCommit(){
      const list = this.bills || []
      if (!list.length) {
        uni.showToast({ title: '购物车暂无可下单商品', icon: 'none' })
        return false
      }
      try {
        const createTime = this.getNowStr()
        let firstBillId = null
        for(let i=0; i < list.length; i++) {
          // 只提交数据库已存在字段，避免写单失败
          // 如果使用了优惠券，total 保存优惠后的金额（paytotal）
          const finalTotal = this.yhq && this.yhq.id > 0 ? String(this.paytotal) : String(list[i].total)
          const obj = {
            table: 'bill',
            total: finalTotal,
            gnames: list[i].gnames,
            gids: list[i].gids,
            shop: list[i].shop,
            sid: list[i].sid,
            note: this.bnote,
            user: this.userInfo.username,
            uid: this.userInfo.id,
            state: this.state,
            statecn: this.statecn,
            way: this.way,
            ndate: createTime,
            yhqid: this.yhq && this.yhq.id > 0 ? this.yhq.id : null
          }
          const saved = await savej({params: obj})
          const rid = this.parseSaveId(saved)
          if (!firstBillId && rid) firstBillId = rid
        }
        if (this.state === BILL_STATE.PAID && this.yhq) {
          await savej({params: {table: 'youhuiquan', id:this.yhq.id, statecn: "已使用" }})
        }

        const carlist = this.carlist
        const goodlist = this.paygoodslist
        const newcarlist = []
        for (let i = 0; i < carlist.length; i++) {
          const cobj = carlist[i]
          let flag = true
          for (let j = 0; j < goodlist.length; j++) {
            const gobj = goodlist[j]
            if (gobj.id == cobj.id) {
              flag = false
              break
            }
          }
          if (flag) newcarlist.push(cobj)
        }
        this.setCar(newcarlist)
        // 兜底：若接口未返回 id，按用户最新订单反查
        if (!firstBillId) {
          const latest = await listj({ params: { table: 'bill', uid: this.userInfo.id, sort: 'id', order: 'desc' } }).catch(() => [])
          if (latest && latest.length) firstBillId = latest[0].id
        }

        uni.showToast({ title: '下单成功!' })
        setTimeout(() => {
          if (firstBillId) {
            uni.navigateTo({ url: '/pages/bill/billdetail?bid=' + firstBillId })
          } else {
            uni.switchTab({ url: '/pages/bill/bill' })
          }
        }, 600)
        return true
      } catch (e) {
        uni.showToast({ title: '下单失败，请重试', icon: 'none' })
        return false
      }
    },
    payBill(){
      if(!this.passwd) return uni.showToast({ title: '请输入支付密码', icon: 'none' });
      let userpasswd = this.userInfo.paypwd
      if(!userpasswd) return uni.showToast({ title: '请先在钱包中设置支付密码', icon: 'none' });
      if(String(this.passwd) !== String(userpasswd)){
        uni.showToast({ title: '密码错误', icon : 'error' });
        return;
      }
      let mymoney = (this.userInfo.money||0)*1
      if (mymoney < this.paytotal) {
        uni.showToast({ icon:'none', title: '余额不足!' })
      }else{
        mymoney = mymoney - this.paytotal
        savej({params: {table: 'user', id: this.userInfo.id, money: mymoney }}).then(res => {
          this.userInfo.money = mymoney
          this.updateUserInfo(this.userInfo)
          this.state = BILL_STATE.PAID
          this.statecn = '已付款'
          this.billCommit()
        })
      }
    },
    getYouhuiquan(){
      listj({params: {table: 'youhuiquan',uid:this.userInfo.id, statecn:"正常" }}).then(yhl => {
        const now = Math.floor(Date.now() / 1000)
        const valid = (yhl || []).filter(item => !item.extime || item.extime > now)
        this.yhqlist = [{id:0, total:0, fulluse:0}, ...valid]
      })
    },
    toggleTotal(n){
      if (n > 0) {
        findj({params: {table: 'youhuiquan', id: n}}).then(yhq => {
          if (!yhq) return
          // 验证满减条件
          if (yhq.fulluse && this.total < yhq.fulluse) {
            uni.showToast({ title: `订单金额需满${yhq.fulluse}元才可使用此优惠券`, icon: 'none' })
            this.yhq = null
            this.paytotal = this.total
            return
          }
          this.yhq = yhq
          this.paytotal = this.total - yhq.total
        })
      } else {
        this.yhq = null
        this.paytotal = this.total
      }
    }
  },
  onLoad(){
    if (!this.userInfo || !this.userInfo.id) {
      uni.showToast({ title: '请先登录', icon: 'none' })
      setTimeout(() => uni.ytool.toLogin(), 500)
      return
    }
    let list = this.carlist.filter(item => item.checked)
    if (!list.length) {
      uni.showToast({ title: '请先勾选商品', icon: 'none' })
      setTimeout(() => uni.navigateBack({ delta: 1 }), 600)
      return
    }
    let shopslist = []
    list.forEach(item => {
      if (!ideautil.checkStrInList(item.sid, shopslist)) shopslist.push(item.sid)
    })

    shopslist.forEach(sid => {
      let btotal = 0, bgnames = '', bgids = '', sname = '';
      list.filter(obj => obj.sid === sid).forEach(obj => {
        let count = obj.count*1
        btotal += (obj.price*1 * count)
        bgnames += (bgnames === '' ? '' : ',') + obj.gname + "*" + count
        bgids += (bgids === '' ? '' : ',') + obj.id
        sname = obj.shop

        let mcount = Math.max((obj.mcount||0)*1 - count, 0)
        this.paygoodslist.push({table: 'good', mcount: mcount, id: obj.id})
      })

      this.bills.push({ total: btotal, gnames: bgnames, gids: bgids, shop: sname, sid: sid })
      this.gnames += (this.gnames === '' ? '' : ',') + bgnames
      this.total += btotal
    })
    this.paytotal = this.total
    this.getYouhuiquan()
    this.hasPayPwd = !!(this.userInfo && this.userInfo.paypwd)
    if (!this.hasPayPwd) {
      uni.showToast({ title: '请先在钱包中设置支付密码', icon: 'none', duration: 2000 })
    }
  },
  computed: {
    ...mapState(['carlist', 'userInfo'])
  }
}
</script>

<style lang="scss" scoped>
/* 变量 */
$bg-color: #f8f9fa;
$card-bg: #ffffff;
$primary-color: #ff943c;
$price-color: #ff4d4f;
$text-main: #333333;
$text-sub: #999999;

.page-container {
  background-color: $bg-color;
  min-height: 100vh;
}

.content-wrapper {
  padding: 24rpx;
}

/* 卡片通用 */
.order-card {
  background-color: $card-bg;
  border-radius: 20rpx;
  margin-bottom: 24rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.02);
}

/* 堂食 / 外带 */
.way-card {
  .way-segment {
    display: flex;
    gap: 20rpx;
    margin-top: 8rpx;
  }
  .way-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 28rpx 16rpx;
    border-radius: 16rpx;
    background: #f5f6f8;
    border: 2rpx solid transparent;
    transition: all 0.25s;
    .way-icon { font-size: 40rpx; margin-bottom: 10rpx; }
    .way-label { font-size: 28rpx; color: $text-sub; }
    &.active {
      background: rgba($primary-color, 0.12);
      border-color: $primary-color;
      .way-label { color: $primary-color; font-weight: bold; }
    }
  }
}

/* 标题样式 */
.card-title {
  margin-bottom: 20rpx;
  .title-text {
    font-size: 30rpx;
    font-weight: bold;
    color: $text-main;
    display: block;
  }
  .title-sub {
    font-size: 24rpx;
    color: $text-sub;
    margin-top: 4rpx;
  }
}

/* 商品卡片 */
.goods-card {
  .goods-names {
    font-size: 28rpx;
    color: #555;
    line-height: 1.6;
    padding: 20rpx;
    background-color: #f9f9f9;
    border-radius: 12rpx;
    margin-bottom: 24rpx;
  }
  .price-detail {
    .price-line {
      display: flex;
      justify-content: space-between;
      font-size: 26rpx;
      color: #666;
      margin-bottom: 12rpx;
      &:last-child { margin-bottom: 0; }
    }
  }
}

/* 选项卡片 */
.option-card {
  .option-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f2f2f2;
    .label { font-size: 28rpx; color: $text-main; }
    .picker-wrap { flex: 1; text-align: right; }
  }
  .note-section {
    padding-top: 20rpx;
    .label { font-size: 28rpx; color: $text-main; }
  }
}

/* 底部结算栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 110rpx;
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40rpx;
  padding-bottom: env(safe-area-inset-bottom);
  box-shadow: 0 -4rpx 20rpx rgba(0,0,0,0.05);
  z-index: 99;
  .bar-left {
    display: flex;
    align-items: baseline;
    .total-label { font-size: 26rpx; color: $text-sub; }
    .total-symbol { font-size: 26rpx; color: $price-color; margin-left: 12rpx; font-weight: bold; }
    .total-value { font-size: 40rpx; color: $price-color; font-weight: bold; }
  }
  .submit-btn {
    background: $primary-color;
    color: #fff;
    padding: 20rpx 60rpx;
    border-radius: 40rpx;
    font-size: 30rpx;
    font-weight: bold;
    box-shadow: 0 6rpx 16rpx rgba(255, 148, 60, 0.35);
    &:active { transform: scale(0.96); opacity: 0.92; }
  }
}

/* 支付弹窗 */
.pay-popup-content {
  padding: 40rpx;
  .popup-title {
    font-size: 34rpx;
    font-weight: bold;
    text-align: center;
    margin-bottom: 40rpx;
  }
  .coupon-box {
    .sub-title { font-size: 26rpx; color: $text-sub; margin-bottom: 20rpx; }
    .coupon-scroll { max-height: 300rpx; }
  }
  .final-price-box {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 40rpx 0;
    border-top: 1rpx solid #f2f2f2;
    font-size: 28rpx;
    .price-num {
      color: $price-color;
      font-weight: bold;
      font-size: 44rpx;
      .symbol { font-size: 28rpx; margin-right: 4rpx; }
    }
  }
  .pwd-section {
    margin-bottom: 40rpx;
  }
  .confirm-pay-btn {
    background-color: $primary-color;
    color: #fff;
    border-radius: 50rpx;
    height: 90rpx;
    line-height: 90rpx;
    font-size: 32rpx;
    font-weight: bold;
    border: none;
  }
}

.safe-area-inset-bottom {
  height: 140rpx;
}
</style>