<template>
  <view class="page-container">
    <u-navbar title="我的收藏" :border="true" :placeholder="true" :autoBack="true"></u-navbar>

    <view class="tab-bar">
      <view
        v-for="(tab, index) in tabs"
        :key="index"
        class="tab-item"
        :class="{ active: currentTab === index }"
        @click="switchTab(index)"
      >
        <text class="tab-text">{{ tab.name }}</text>
        <view class="tab-line" v-if="currentTab === index"></view>
      </view>
    </view>

    <!-- 列表内容 -->
    <scroll-view scroll-y :enable-flex="true" class="result-container">
      <!-- 美食收藏 -->
      <view v-show="currentTab === 0" class="result-list">
        <view v-if="goodList.length === 0" class="empty-box">
          <text class="empty-icon">🍜</text>
          <text class="empty-text">暂无收藏美食</text>
        </view>
        <imglist
          v-else
          :showSearch="false"
          imgName="img"
          sName="note"
          imgSize="2"
          tName="pf"
          tLabel="评分:"
          tColor="#ff9900"
          titleName="gname"
          :dataList="goodList"
          @clickItem="toGoodDetail"
        ></imglist>
      </view>

      <!-- 店铺收藏 -->
      <view v-show="currentTab === 1" class="result-list">
        <view v-if="shopList.length === 0" class="empty-box">
          <text class="empty-icon">🏪</text>
          <text class="empty-text">暂无收藏店铺</text>
        </view>
        <imglist
          v-else
          :showSearch="false"
          imgName="img"
          sName="note"
          imgSize="2"
          tName="pf"
          tLabel="评分:"
          tColor="#ff9900"
          titleName="sname"
          :dataList="shopList"
          @clickItem="toShopDetail"
        ></imglist>
      </view>

      <!-- 动态收藏 -->
      <view v-show="currentTab === 2" class="result-list">
        <view v-if="blogList.length === 0" class="empty-box">
          <text class="empty-icon">📄</text>
          <text class="empty-text">暂无收藏动态</text>
        </view>
        <imglist
          v-else
          :showSearch="false"
          imgName="img"
          sName="note"
          imgSize="2"
          tName="username"
          tLabel="作者:"
          tColor="#ff943c"
          titleName="title"
          :dataList="blogList"
          @clickItem="toBlogDetail"
        ></imglist>
      </view>

      <view class="safe-area-bottom"></view>
    </scroll-view>
  </view>
</template>

<script>
import { listSqlj, fileUrl } from '@/common/config/api.js'
import { mapState, mapActions } from 'vuex'
import { yewuutil } from '@/common/commontools.js'

export default {
  data() {
    return {
      currentTab: 0,
      tabs: [
        { name: '美食', type: 'good' },
        { name: '店铺', type: 'shop' },
        { name: '动态', type: 'blog' }
      ],
      goodList: [],
      shopList: [],
      blogList: [],
      fileUrl: fileUrl
    }
  },
  onShow() {
    this.loadGoodFavs()
    this.loadShopFavs()
    this.loadBlogFavs()
  },
  methods: {
    ...mapActions(['updateUserInfo']),
    switchTab(index) {
      this.currentTab = index
    },
    loadGoodFavs() {
      const favs3 = this.userInfo && this.userInfo.favs3
      if (!favs3 || favs3 === '0') {
        this.goodList = []
        return
      }
      const sql = `select * from fs_good where id in ( ${favs3} )`
      listSqlj({ params: { sql: sql } })
        .then(res => {
          this.goodList = res || []
        })
        .catch(() => {
          this.goodList = []
        })
    },
    loadShopFavs() {
      const favs = this.userInfo && this.userInfo.favs
      if (!favs || favs === '0') {
        this.shopList = []
        return
      }
      const sql = `select * from fs_shop where id in ( ${favs} )`
      listSqlj({ params: { sql: sql } })
        .then(res => {
          this.shopList = res || []
        })
        .catch(() => {
          this.shopList = []
        })
    },
    loadBlogFavs() {
      const favs2 = this.userInfo && this.userInfo.favs2
      if (!favs2 || favs2 === '0') {
        this.blogList = []
        return
      }
      const sql = `select * from fs_blog where id in ( ${favs2} )`
      listSqlj({ params: { sql: sql } })
        .then(res => {
          this.blogList = res || []
        })
        .catch(() => {
          this.blogList = []
        })
    },
    toGoodDetail(id) {
      uni.navigateTo({ url: '/pages/good/gooddetail?gid=' + id })
    },
    toShopDetail(id) {
      uni.navigateTo({ url: '/pages/shops/shopdetail?pid=' + id })
    },
    toBlogDetail(id) {
      uni.navigateTo({ url: '/pages/blog/blogdetail?id=' + id })
    }
  },
  computed: {
    ...mapState(['userInfo'])
  }
}
</script>

<style lang="scss" scoped>
$primary-color: #ff943c;

.page-container {
  background-color: #f8f9fa;
  min-height: 100vh;
  box-sizing: border-box;
}

.tab-bar {
  display: flex;
  background-color: #fff;
  padding: 0 24rpx;
  border-bottom: 1rpx solid #f0f0f0;

  .tab-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 24rpx 0;
    position: relative;

    .tab-text {
      font-size: 28rpx;
      color: #666;
    }

    &.active .tab-text {
      color: $primary-color;
      font-weight: bold;
    }

    .tab-line {
      position: absolute;
      bottom: 0;
      width: 48rpx;
      height: 4rpx;
      background-color: $primary-color;
      border-radius: 2rpx;
    }
  }
}

.result-container {
  height: calc(100vh - 88rpx - 90rpx - env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.result-list {
  padding: 16rpx 24rpx;
}

.shop-item {
  display: flex;
  align-items: center;
  background-color: #fff;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

  .item-img {
    width: 140rpx;
    height: 140rpx;
    border-radius: 12rpx;
    flex-shrink: 0;
  }

  .item-info {
    flex: 1;
    margin: 0 16rpx;
    overflow: hidden;

    .item-title {
      font-size: 30rpx;
      font-weight: bold;
      color: #333;
      display: block;
      margin-bottom: 8rpx;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .item-desc {
      font-size: 24rpx;
      color: #999;
      display: block;
      margin-bottom: 8rpx;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .item-meta {
      display: flex;
      align-items: center;
      gap: 16rpx;

      .item-score {
        font-size: 22rpx;
        color: #ff9900;
      }

      .item-address {
        font-size: 22rpx;
        color: #999;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        max-width: 200rpx;
      }

      .item-author {
        font-size: 22rpx;
        color: #666;
      }
    }
  }
}

.blog-item {
  display: flex;
  align-items: center;
  background-color: #fff;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

  .item-img {
    width: 180rpx;
    height: 120rpx;
    border-radius: 12rpx;
    flex-shrink: 0;
  }

  .item-info {
    flex: 1;
    margin: 0 16rpx;
    overflow: hidden;

    .item-title {
      font-size: 30rpx;
      font-weight: bold;
      color: #333;
      display: block;
      margin-bottom: 8rpx;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .item-desc {
      font-size: 24rpx;
      color: #999;
      display: block;
      margin-bottom: 8rpx;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .item-meta {
      display: flex;
      align-items: center;
      gap: 16rpx;

      .item-author {
        font-size: 22rpx;
        color: #666;
      }
    }
  }
}

.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 200rpx;

  .empty-icon {
    font-size: 120rpx;
    margin-bottom: 30rpx;
  }

  .empty-text {
    font-size: 28rpx;
    color: #999;
  }
}

.safe-area-bottom {
  height: calc(20rpx + env(safe-area-inset-bottom));
  min-height: env(safe-area-inset-bottom);
}
</style>
