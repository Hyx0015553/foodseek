<template>
  <view class="page-container">
    <u-navbar
      title="店铺优惠券"
      :border="false"
      :placeholder="true"
      :autoBack="true"
      bgColor="#ffffff"
      titleStyle="font-weight: bold; color: #1a1a1a;"
    ></u-navbar>

    <scroll-view scroll-y class="main-scroll" :enable-flex="true">
      <view class="list-wrap" v-if="couponList.length > 0">
        <view class="coupon-card" v-for="item in couponList" :key="item.id">
          <view class="card-left">
            <view class="amt">
              <text class="yen">¥</text>
              <text class="val">{{ item.total }}</text>
            </view>
            <view class="cond" v-if="item.fulluse">满{{ item.fulluse }}元可用</view>
            <view class="cond" v-else>无门槛</view>
            <view class="tag">{{ typeName(item.coupontype) }}</view>
          </view>
          <view class="card-right">
            <view class="note text-ellipsis" v-if="item.note">{{ item.note }}</view>
            <view class="exp" v-if="item.extimestr">有效期至 {{ item.extimestr }}</view>
            <view class="stock" v-if="item.kucun != null && item.kucun !== ''">剩余 {{ item.kucun }} 张</view>
            <view class="stock" v-else>库存充足</view>
            <view class="claim" @tap="claim(item)">领取</view>
          </view>
        </view>
      </view>
      <view class="empty" v-else>
        <text>暂无可领取的优惠券</text>
      </view>
      <view class="safe-pad"></view>
    </scroll-view>
  </view>
</template>

<script>
import { listj } from '@/common/config/api.js'
import { mapState } from 'vuex'
import { yewuutil } from '@/common/commontools.js'

export default {
  data() {
    return {
      sid: null,
      couponList: [],
		typeMap: { 1: '满减券', 2: '优惠卷' }
    }
  },
  computed: {
    ...mapState(['userInfo'])
  },
  onLoad(params) {
    this.sid = params.sid
    this.loadList()
  },
  onShow() {
    if (this.sid) this.loadList()
  },
  methods: {
		typeName(t) {
      return this.typeMap[t] || '优惠卷'
    },
    loadList() {
      if (!this.sid) return
      listj({
        params: { table: 'youhuiquan', typeid: 1, sid: this.sid }
      })
        .then(res => {
          this.couponList = yewuutil.filterClaimableYouhuiquanTemplates(res)
        })
        .catch(() => {
          this.couponList = []
        })
    },
    claim(item) {
      if (!item || !item.id) return
      if (!this.userInfo || !this.userInfo.id) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }
      yewuutil
        .claimYouhuiquanTemplate({
          userInfo: this.userInfo,
          templateId: item.id,
          shopSid: this.sid
        })
        .then(() => {
          uni.showToast({ title: '您已领取优惠券，可在个人中心查看', icon: 'success' })
          this.loadList()
        })
        .catch(err => {
          uni.showToast({ title: (err && err.message) || '领取失败', icon: 'none' })
        })
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #ff943c;
$card-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.05);

.page-container {
  min-height: 100vh;
  background: #f8f9fb;
}

.main-scroll {
  height: calc(100vh - 88rpx);
  padding: 24rpx 30rpx;
  box-sizing: border-box;
}

.list-wrap {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.coupon-card {
  display: flex;
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: $card-shadow;
}

.card-left {
  flex: 1;
  padding-right: 20rpx;
  border-right: 2rpx dashed #eee;
}

.amt {
  display: flex;
  align-items: baseline;
  margin-bottom: 8rpx;
}

.yen {
  font-size: 28rpx;
  color: #ff4d4f;
  font-weight: bold;
}

.val {
  font-size: 48rpx;
  color: #ff4d4f;
  font-weight: 800;
}

.cond {
  font-size: 22rpx;
  color: #909399;
  margin-bottom: 10rpx;
}

.tag {
  display: inline-flex;
  padding: 4rpx 12rpx;
  background: #fff3e0;
  color: #e65100;
  font-size: 20rpx;
  font-weight: bold;
  border-radius: 8rpx;
}

.card-right {
  width: 240rpx;
  padding-left: 24rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.note {
  font-size: 24rpx;
  color: #606266;
  margin-bottom: 6rpx;
}

.exp {
  font-size: 22rpx;
  color: #909399;
  margin-bottom: 6rpx;
}

.stock {
  font-size: 22rpx;
  color: #909399;
  margin-bottom: 12rpx;
}

.claim {
  text-align: center;
  padding: 16rpx 0;
  background: linear-gradient(135deg, $primary, #ff6b00);
  color: #fff;
  font-size: 26rpx;
  font-weight: bold;
  border-radius: 12rpx;
}

.empty {
  padding: 120rpx 40rpx;
  text-align: center;
  font-size: 28rpx;
  color: #909399;
}

.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.safe-pad {
  height: 40rpx;
}
</style>
