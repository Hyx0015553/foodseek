<template>
  <view class="page-container">
    <!-- 1. 自定义导航栏 -->
    <view class="custom-navbar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">商家首页</text>
      </view>
    </view>

    <scroll-view scroll-y class="main-scroll">
      <!-- 2. 顶部背景与店铺信息区 -->
      <view class="user-header">
        <view class="header-bg header-bg-orange"></view>

        <view class="user-info-card">
          <view class="avatar-wrapper" @tap="toShopDetail">
            <image :src="fileUrl + (userInfo.img || '')" mode="aspectFill" class="avatar-img"></image>
            <view class="edit-badge">✏️</view>
          </view>

          <view class="user-detail">
            <view class="username">{{ userInfo.shopname || userInfo.username || '店铺' }}</view>
          </view>
        </view>
      </view>

      <view class="content-wrapper">
        <!-- 3. 商家功能金刚区 -->
        <view class="action-grid-card">
          <view class="grid-item" @tap="toGood">
            <view class="grid-icon bg-gold">💥</view>
            <text class="grid-text">商品管理</text>
          </view>
          <view class="grid-item" @tap="toBlogShop">
            <view class="grid-icon bg-red">🍎</view>
            <text class="grid-text">店铺文章</text>
          </view>
          <view class="grid-item" @tap="toShopmg">
            <view class="grid-icon bg-blue">📊</view>
            <text class="grid-text">店铺维护</text>
          </view>

          <view class="grid-item" @tap="toReplay">
            <view class="grid-icon bg-blue">💬</view>
            <text class="grid-text">评价管理</text>
          </view>
        </view>

        <view class="action-grid-card">
          <view class="grid-item" @tap="toMessage">
            <view class="grid-icon bg-gold msg-icon-wrap">
              <text>🔔</text>
              <view class="msg-dot" v-if="hasUnreadMsg"></view>
            </view>
            <text class="grid-text">消息通知</text>
          </view>
          <view class="grid-item" @tap="toShopOperation">
            <view class="grid-icon bg-red">📈</view>
            <text class="grid-text">运营数据</text>
          </view>

          <view class="grid-item" @tap="toMerchantWallet">
            <view class="grid-icon bg-blue">💰</view>
            <text class="grid-text">钱包订单</text>
          </view>

          <view class="grid-item" @tap="toYouhuiquan">
            <view class="grid-icon bg-gold">🧾</view>
            <text class="grid-text">福利活动</text>
          </view>
          <view class="grid-item" @tap="toAiAssistant">
            <view class="grid-icon bg-purple">🤖</view>
            <text class="grid-text">AI小助手</text>
          </view>
        </view>

        <!-- 本店美食排行：排除「饭类主食」，综合评分 + 销量 + 用户收藏(favs3) -->
        <view class="rank-card" v-if="userInfo && userInfo.sid">
          <view class="rank-card-hd">
            <text class="rank-card-title">本店美食排行</text>
            <text class="rank-card-sub">不含「饭类主食」· 综合评分、销量、收藏加权</text>
          </view>
          <view v-if="goodRankLoading" class="rank-loading">计算中…</view>
          <view v-else-if="!goodRankList.length" class="rank-empty">暂无可参与排行的商品</view>
          <view v-else class="rank-list">
            <view
              v-for="(row, ri) in goodRankList"
              :key="row.id"
              class="rank-row"
              @tap="toGoodDetailRank(row.id)"
            >
              <text class="rank-no" :class="{ top3: ri < 3 }">{{ ri + 1 }}</text>
              <view class="rank-mid">
                <text class="rank-name">{{ row.gname || '未命名' }}</text>
                <text class="rank-meta">评分 {{ row._pfDisp }} · 销量 {{ row._xlDisp }} · 收藏 {{ row._favDisp }}</text>
              </view>
              <text class="rank-score">{{ row._rankScore }}</text>
            </view>
          </view>
        </view>

        <!-- 退出登录 -->
        <view class="cell-group">
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
import { fileUrl, listj, listSqlj } from '@/common/config/api.js';
import { mapState, mapActions } from 'vuex'

export default {
  data() {
    return {
      fileUrl: fileUrl,
      statusBarHeight: 0,
      hasUnreadMsg: false,
      goodRankLoading: false,
      goodRankList: []
    }
  },
  created() {
    const sysInfo = uni.getSystemInfoSync();
    this.statusBarHeight = sysInfo.statusBarHeight || 0;
  },
  onShow() {
    if (!this.userInfo || !this.userInfo.id) {
      uni.itool.nto({ url: '/pages/login/login' });
      return
    }
    this.loadUnreadMsgState()
    this.loadMerchantGoodRank()
  },
  methods: {
    ...mapActions(['updateUserInfo']),
    /** 「饭类主食」不参与排行；ctype / type 任一匹配即排除 */
    isStapleRiceCategory(g) {
      const c = String((g && g.ctype) || '').trim()
      const t = String((g && g.type) || '').trim()
      return c === '饭类主食' || t === '饭类主食'
    },
    parseGoodSales(g) {
      const x = parseInt(g && g.xl, 10)
      if (!isNaN(x) && x >= 0) return x
      const m = parseInt(String((g && g.mcount) || '').replace(/\D/g, ''), 10)
      return isNaN(m) ? 0 : Math.max(0, m)
    },
    parseGoodPf(g) {
      const p = parseFloat(g && g.pf)
      if (isNaN(p) || p < 0) return 0
      return Math.min(10, p)
    },
    /** 从 fs_user_favorite（fav_type=3）统计本店菜品收藏次数 */
    buildGoodFavMap(rows, shopGoodIds) {
      const favMap = new Map()
      shopGoodIds.forEach((id) => favMap.set(String(id), 0))
      ;(rows || []).forEach((row) => {
        const id = String((row && (row.target_id != null ? row.target_id : row.favs3)) || '').trim()
        if (!id || id === '0') return
        if (favMap.has(id)) favMap.set(id, (favMap.get(id) || 0) + 1)
      })
      return favMap
    },
    /**
     * 综合分（0～100）：评分 50% + 销量 35% + 收藏 15%，组内按 max 归一化，避免单项碾压。
     */
    computeRankScore(goods, favMap) {
      if (!goods.length) return []
      let maxXl = 0
      let maxFav = 0
      const W_PF = 0.5
      const W_XL = 0.35
      const W_FAV = 0.15
      const tmp = goods.map((g) => {
        const xl = this.parseGoodSales(g)
        const fav = favMap.get(String(g.id)) || 0
        maxXl = Math.max(maxXl, xl)
        maxFav = Math.max(maxFav, fav)
        return { g, xl, fav }
      })
      const denomXl = maxXl > 0 ? maxXl : 1
      const denomFav = maxFav > 0 ? maxFav : 1
      return tmp
        .map(({ g, xl, fav }) => {
          const pf = this.parseGoodPf(g)
          const pfNorm = Math.min(1, pf / 5)
          const xlNorm = xl / denomXl
          const favNorm = fav / denomFav
          const raw = 100 * (W_PF * pfNorm + W_XL * xlNorm + W_FAV * favNorm)
          const rankScore = Math.round(raw * 10) / 10
          return {
            ...g,
            _rankScore: rankScore,
            _pfDisp: pf.toFixed(1),
            _xlDisp: xl,
            _favDisp: fav
          }
        })
        .sort((a, b) => b._rankScore - a._rankScore)
    },
    loadMerchantGoodRank() {
      if (!this.userInfo || !this.userInfo.sid) {
        this.goodRankList = []
        return
      }
      this.goodRankLoading = true
      const sid = String(this.userInfo.sid).replace(/'/g, "''")
      Promise.all([
        listj({ params: { table: 'good', sid: this.userInfo.sid } }),
        listSqlj({
          params: {
            sql: `SELECT uf.target_id FROM fs_user_favorite uf INNER JOIN fs_good g ON g.id = uf.target_id WHERE uf.fav_type=3 AND g.sid=${sid}`
          }
        })
      ])
        .then(([goods, favRows]) => {
          const all = goods || []
          const eligible = all.filter((g) => g && !this.isStapleRiceCategory(g))
          const idSet = new Set(eligible.map((g) => String(g.id)))
          const favMap = this.buildGoodFavMap(favRows || [], idSet)
          this.goodRankList = this.computeRankScore(eligible, favMap).slice(0, 15)
        })
        .catch(() => {
          this.goodRankList = []
        })
        .finally(() => {
          this.goodRankLoading = false
        })
    },
    toGoodDetailRank(gid) {
      if (!gid) return
      uni.navigateTo({ url: '/pages/good/gooddetail?gid=' + gid })
    },
    loadUnreadMsgState() {
      if (!this.userInfo || !this.userInfo.sid) {
        this.hasUnreadMsg = false
        return
      }
      let readMap = {}
      try {
        readMap = uni.getStorageSync('merchant_sysmsg_read_map') || {}
      } catch (e) {
        readMap = {}
      }
      listj({ params: { table: 'sysmsg', sid: this.userInfo.sid } }).then(res => {
        const arr = res || []
        this.hasUnreadMsg = arr.some(item => {
          if (!item) return false
          if (item.id && readMap[String(item.id)]) return false
          if (item.state !== undefined && item.state !== null) return String(item.state) === '1'
          return String(item.type) === '1'
        })
      }).catch(() => {
        this.hasUnreadMsg = false
      })
    },
    toGood() {
      uni.itool.nto({ url: '/pages/good/good' });
    },
    toBlogShop() {
      uni.itool.nto({ url: '/pages/blog/blogshop' });
    },
    toShopDetail() {
      if (this.userInfo && this.userInfo.sid) {
        uni.itool.nto({ url: '/pages/shops/shopdetail-m?pid=' + this.userInfo.sid });
      }
    },
    toShopMg() {
      uni.itool.nto({ url: '/pages/shops/shopmg' });
    },
    toMessage() {
      uni.itool.nto({ url: '/pages/merchant/sysmsg' });
    },
    toReplay() {
      // 从商家首页进入：默认展示全部评价
      uni.itool.nto({ url: '/pages/merchant/replay?tab=all' });
    },
    toShopOperation() {
      uni.itool.nto({ url: '/pages/merchant/shopoperation' });
    },
    // 兼容小程序端可能将方法名中 Mg 转为 mg 调用
    toShopmg() {
      this.toShopMg();
    },
    toPosts() {
      uni.itool.nto({ url: '/pages/blog/bloglist' });
    },
    toMerchantWallet() {
      uni.itool.nto({ url: '/pages/qianbao/qianbao-m' });
    },
    toYouhuiquan() {
      uni.itool.nto({ url: '/pages/merchant/youhuiquan-m' })
    },
    toAiAssistant() {
      uni.navigateTo({ url: '/pages/assistant/assistant?from=merchant' })
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
    },
    // 兼容小程序端可能将方法名转为小写调用
    logout() {
      this.logOut()
    }
  },
  computed: {
    ...mapState(['userInfo'])
  }
}
</script>

<style lang="scss" scoped>
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

.custom-navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background-color: rgba(255, 255, 255, 0);

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
    bottom: 40rpx; /* 与 user-info-card 底部对齐 */
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
    }
  }
}

.content-wrapper {
  padding: 0 30rpx;
  margin-top: -20rpx;
}

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

    .msg-icon-wrap{
      position: relative;
    }

    .msg-dot{
      position: absolute;
      top: -4rpx;
      right: -4rpx;
      width: 18rpx;
      height: 18rpx;
      border-radius: 999rpx;
      background: #ef4444;
      border: 3rpx solid #fff;
    }

    .bg-gold { background: linear-gradient(135deg, #fff9e6, #ffecb3); }
    .bg-red { background: linear-gradient(135deg, #fff1f0, #ffccc7); }
    .bg-blue { background: linear-gradient(135deg, #e6f7ff, #bae7ff); }

    .grid-text {
      font-size: 24rpx;
      color: #555;
    }
  }
}

.bottom-padding {
  height: calc(50rpx + env(safe-area-inset-bottom));
}

.rank-card {
  background-color: $card-bg;
  border-radius: 24rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 40rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.03);
}

.rank-card-hd {
  margin-bottom: 20rpx;
}

.rank-card-title {
  display: block;
  font-size: 30rpx;
  font-weight: bold;
  color: $text-main;
  margin-bottom: 8rpx;
}

.rank-card-sub {
  font-size: 22rpx;
  color: $text-sub;
  line-height: 1.4;
}

.rank-loading,
.rank-empty {
  font-size: 26rpx;
  color: $text-sub;
  padding: 24rpx 0;
  text-align: center;
}

.rank-list {
  border-top: 1rpx solid #f0f0f0;
}

.rank-row {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
  &:last-child {
    border-bottom: none;
  }
  &:active {
    opacity: 0.75;
  }
}

.rank-no {
  width: 48rpx;
  font-size: 28rpx;
  font-weight: 800;
  color: #bbb;
  flex-shrink: 0;
  &.top3 {
    color: #ff943c;
  }
}

.rank-mid {
  flex: 1;
  min-width: 0;
  margin-right: 16rpx;
}

.rank-name {
  display: block;
  font-size: 28rpx;
  color: $text-main;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-meta {
  display: block;
  font-size: 22rpx;
  color: $text-sub;
  margin-top: 6rpx;
}

.rank-score {
  font-size: 30rpx;
  font-weight: 800;
  color: #ff6037;
  flex-shrink: 0;
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

      .cell-arrow {
        font-size: 24rpx;
        color: #ccc;
      }
    }
  }
}
</style>
