<template>
  <view class="page-container">
    <!-- 1. 自定义导航栏 (原生实现) -->
    <view class="custom-navbar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">个人中心</text>
      </view>
    </view>

    <scroll-view scroll-y class="main-scroll">
      <!-- 2. 顶部背景与用户信息区 -->
      <view class="user-header">
        <view class="header-bg header-bg-orange"></view>

        <view class="user-info-card">
          <view class="avatar-wrapper" @tap="toUserDetail">
            <image :src="fileUrl + userInfo.img" mode="aspectFill" class="avatar-img"></image>
            <view class="edit-badge">✏️</view>
          </view>

          <view class="user-detail">
            <view class="username">{{ userInfo.username || '未登录' }}</view>
          </view>
        </view>
      </view>

      <view class="content-wrapper">
        <!-- 3. 核心功能金刚区 (原生 Grid) -->
        <view class="action-grid-card">
          <view class="grid-item" @tap="toTags">
            <view class="grid-icon bg-red">❤️</view>
            <text class="grid-text">爱好标签</text>
          </view>
          <view class="grid-item" @tap="toMyFavs">
            <view class="grid-icon bg-gold">⭐</view>
            <text class="grid-text">我的收藏</text>
          </view>
          <view class="grid-item" @tap="toBill">
            <view class="grid-icon bg-orange">📋</view>
            <text class="grid-text">订单</text>
          </view>
          <view class="grid-item" @tap="toShopcar">
            <view class="grid-icon bg-blue">🛒</view>
            <text class="grid-text">购物车</text>
          </view>

          <view class="grid-item" v-if="userInfo.roletype=='3'" @tap="toMerchant">
            <view class="grid-icon bg-purple">🏪</view>
            <text class="grid-text">商家首页</text>
          </view>
        </view>

        <!-- 4. 常用功能列表 (原生实现 Cell) -->
        <view class="section-title">交流与反馈</view>
        <view class="cell-group">
	  <view class="cell-item" @tap="toBlogMg">
            <view class="cell-left">
              <text class="cell-icon">📝</text>
              <text class="cell-text">探店发帖</text>
            </view>
            <view class="cell-right">
              <text class="cell-arrow">></text>
            </view>
          </view>

          <view class="cell-item" @tap="toYzmessage">
            <view class="cell-left">
              <text class="cell-icon">💬️</text>
              <text class="cell-text">我的评价</text>
            </view>
            <view class="cell-right">
              <view class="dot-badge" v-if="false"></view>
              <text class="cell-arrow">></text>
            </view>
          </view>
          <view class="cell-item" @tap="toAiAssistant">
            <view class="cell-left">
              <text class="cell-icon">🤖</text>
              <text class="cell-text">AI 小助手</text>
            </view>
            <view class="cell-right">
              <text class="cell-arrow">></text>
            </view>
          </view>
          <view class="cell-item" @tap="toHistory">
            <view class="cell-left">
              <text class="cell-icon">🕒</text>
              <text class="cell-text">历史浏览</text>
            </view>
            <view class="cell-right">
              <text class="cell-arrow">></text>
            </view>
          </view>
          <view class="cell-item" @tap="toYouhuiquan">
            <view class="cell-left">
              <text class="cell-icon">🎫</text>
              <text class="cell-text">优惠券</text>
            </view>
            <view class="cell-right">
              <text class="cell-arrow">></text>
            </view>
          </view>
          <view class="cell-item" @tap="toQianbao">
            <view class="cell-left">
              <text class="cell-icon">💰</text>
              <text class="cell-text">我的钱包</text>
            </view>
            <view class="cell-right">
              <text class="cell-arrow">></text>
            </view>
          </view>
          <view class="cell-item" @tap="toBilldetailqs">
            <view class="cell-left">
              <text class="cell-icon">📋</text>
              <text class="cell-text">我的订单</text>
            </view>
            <view class="cell-right">
              <text class="cell-arrow">></text>
            </view>
          </view>
        </view>

        <!-- 5. 更多服务 -->
        <view class="section-title">系统服务</view>
        <view class="cell-group">
          <view class="cell-item" @tap="toUserDetail">
            <view class="cell-left">
              <text class="cell-icon">👤</text>
              <text class="cell-text">个人信息</text>
            </view>
            <view class="cell-right"><text class="cell-arrow">></text></view>
          </view>
          <view class="cell-item" @tap="logOut">
            <view class="cell-left">
              <text class="cell-icon">🚪</text>
              <text class="cell-text text-danger">退出登录</text>
            </view>
            <view class="cell-right"><text class="cell-arrow">></text></view>
          </view>
        </view>

        <!-- 底部占位 -->
        <view class="bottom-padding"></view>
      </view>
    </scroll-view>
    <ai-assistant-float />
  </view>
</template>

<script>
import { listj, findj, fileUrl } from '@/common/config/api.js';
import { mapState, mapActions } from 'vuex'

export default {
  data() {
    return {
      fileUrl: fileUrl,
      statusBarHeight: 0
    }
  },
  created() {
    // 获取系统状态栏高度用于导航栏适配
    const sysInfo = uni.getSystemInfoSync();
    this.statusBarHeight = sysInfo.statusBarHeight || 0;
  },
  onShow() {
    if (!this.userInfo || !this.userInfo.id) {
      // 可以在此处处理未登录跳转
    }
  },
  methods: {
    ...mapActions(['updateUserInfo', 'logout']),
    toBillqs: uni.ytool.toBillqs,
    toHistory: uni.ytool.toHistory,
    toBlogShop(){
        uni.itool.nto({
            url:'/pages/blog/blogshop'
        })
    },
    toGood(){
        uni.itool.nto({
            url:'/pages/good/good'
        })
    },
    toTags(){
        uni.itool.nto({
            url:'/pages/me/tags'
        })
    },
    toHuihua() { uni.itool.nto({ url: '/pages/chat/huihua' }) },
    toYzmessage(){
        uni.itool.nto({
            url:'/pages/me/replay-user'
        })
    },
    toMyFavs: uni.ytool.toMyFavs,
    toQianbao() { uni.itool.nto({ url: '/pages/qianbao/qianbao' }) },
    toBilldetailqs() { uni.navigateTo({ url: '/pages/bill/billdetail' }) },
    toYouhuiquan() { uni.itool.nto({ url: '/pages/youhuiquan/youhuiquan' }) },
    toBill() { uni.navigateTo({ url: '/pages/bill/bill' }) },
    toShopcar() { uni.navigateTo({ url: '/pages/shopcar/shopcar' }) },
    toBlogMg() { uni.itool.nto({ url: '/pages/blog/bloglist' }) },
    toMerchant() { uni.itool.nto({ url: '/pages/merchant/index' }) },
    toAiAssistant() {
      if (!this.userInfo || !this.userInfo.id) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }
      uni.navigateTo({ url: '/pages/assistant/assistant' })
    },
    toUserDetail() { uni.itool.nto({ url: '/pages/me/medetail' }) },
    toShopDetail(){
        uni.itool.nto({
            url:'/pages/shops/shopdetail?pid='+this.userInfo.sid
        })
    },
    logOut() {
      uni.showModal({
        title: '提示',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            this.updateUserInfo({})
            uni.ytool.toLogin()
          }
        }
      })
    }
  },
  computed: {
    ...mapState(['userInfo'])
  }
}
</script>

<style lang="scss" scoped>
/* 颜色变量 */
$primary-color: #3c9cff;
$bg-color: #f6f7f9;
$card-bg: #ffffff;
$text-main: #333333;
$text-sub: #999999;
$danger: #ff4d4f;

.page-container {
  background-color: $bg-color;
  min-height: 100vh;
}

/* 自定义导航栏 */
.custom-navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background-color: rgba(255, 255, 255, 0); /* 初始透明 */
  transition: background-color 0.3s;

  .nav-content {
    height: 44px;
    display: flex;
    align-items: center;
    padding: 0 30rpx;

    .nav-title {
      font-size: 32rpx;
      font-weight: bold;
      color: #333333;
      text-shadow: 0 1rpx 2rpx rgba(255,255,255,0.5);
    }
  }
}

.main-scroll {
  height: 100vh;
}

/* 用户头部 */
.user-header {
  position: relative;
  height: 500rpx;
  display: flex;
  align-items: flex-end;
  padding: 0 30rpx 40rpx;

  .header-bg {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 40rpx; 
    z-index: 0;
  }

  .header-bg-orange {
    background: linear-gradient(180deg, #ffecd2 0%, #ff943c 100%);
  }

  .user-info-card {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    width: 100%;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(10px);
    padding: 40rpx 30rpx;
    border-radius: 30rpx;
    box-shadow: 0 10rpx 30rpx rgba(0,0,0,0.1);

    .avatar-wrapper {
      position: relative;
      margin-right: 30rpx;

      .avatar-img {
        width: 120rpx;
        height: 120rpx;
        border-radius: 50%;
        border: 4rpx solid #ffffff;
        background-color: #eee;
      }

      .edit-badge {
        position: absolute;
        right: 0;
        bottom: 0;
        background: $primary-color;
        width: 40rpx;
        height: 40rpx;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20rpx;
        border: 2rpx solid #ffffff;
      }
    }

    .user-detail {
      flex: 1;

      .username {
        font-size: 36rpx;
        font-weight: bold;
        color: $text-main;
        margin-bottom: 10rpx;
      }

      .balance-box {
        display: flex;
        align-items: center;
        font-size: 24rpx;

        .label { color: $text-sub; }
        .amount {
          color: #f39c12;
          font-weight: bold;
          margin-left: 10rpx;
          font-size: 30rpx;
        }
        .arrow {
          margin-left: auto;
          color: $primary-color;
          background: rgba(60, 156, 255, 0.1);
          padding: 4rpx 16rpx;
          border-radius: 20rpx;
        }
      }
    }
  }
}

.content-wrapper {
  padding: 0 30rpx;
  margin-top: -20rpx;
}

/* 金刚区 Grid */
.action-grid-card {
  display: flex;
  flex-wrap: wrap;
  background-color: $card-bg;
  border-radius: 24rpx;
  padding: 30rpx 0;
  margin-bottom: 40rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.03);

  .grid-item {
    width: 25%;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20rpx 0;

    .grid-icon {
      width: 90rpx;
      height: 90rpx;
      border-radius: 30rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 44rpx;
      margin-bottom: 16rpx;
      transition: transform 0.2s;

      &:active { transform: scale(0.9); }
    }

    .bg-gold { background: linear-gradient(135deg, #fff9e6, #ffecb3); }
    .bg-red { background: linear-gradient(135deg, #fff1f0, #ffccc7); }
    .bg-orange { background: linear-gradient(135deg, #fff7e6, #ffe7ba); }
    .bg-blue { background: linear-gradient(135deg, #e6f7ff, #bae7ff); }
    .bg-purple { background: linear-gradient(135deg, #f9f0ff, #efdbff); }

    .grid-text {
      font-size: 24rpx;
      color: #555;
    }
  }
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: $text-main;
  margin: 0 0 20rpx 10rpx;
}

/* 列表单元格 Cell */
.cell-group {
  background-color: $card-bg;
  border-radius: 24rpx;
  padding: 0 30rpx;
  margin-bottom: 40rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.03);

  .cell-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 110rpx;
    border-bottom: 1rpx solid #f5f5f5;

    &:last-child { border-bottom: none; }
    &:active { opacity: 0.7; }

    .cell-left {
      display: flex;
      align-items: center;

      .cell-icon {
        font-size: 36rpx;
        margin-right: 20rpx;
      }

      .cell-text {
        font-size: 28rpx;
        color: $text-main;
      }

      .text-danger { color: $danger; }
    }

    .cell-right {
      display: flex;
      align-items: center;

      .dot-badge {
        width: 12rpx;
        height: 12rpx;
        background-color: $danger;
        border-radius: 50%;
        margin-right: 15rpx;
      }

      .cell-arrow {
        font-size: 24rpx;
        color: #ccc;
      }
    }
  }
}

.bottom-padding {
  height: calc(50rpx + env(safe-area-inset-bottom));
}
</style>
