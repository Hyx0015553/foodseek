<template>
  <view class="page-container">
    <!-- 顶部搜索栏 -->
    <view class="search-header" :style="{ paddingTop: (statusBarHeight + 12) + 'px' }">
      <view class="search-bar">
        <view class="back-btn" @click="goBack">
          <u-icon name="arrow-left" size="20" color="#333"></u-icon>
        </view>
        <view class="search-input-wrap">
          <input
            class="search-input"
            type="text"
            placeholder="搜索店铺、动态、美食"
            :value="searchKeyword"
            @input="onSearchInput"
            @confirm="handleSearch"
            confirm-type="search"
          />
          <view v-if="searchKeyword" class="search-clear" @click="clearSearchInput">×</view>
        </view>
      </view>
    </view>

    <!-- 搜索历史/热门搜索（未输入时显示） -->
    <view v-if="!searchKeyword && !hasSearched" class="suggest-section">
      <!-- 搜索历史 -->
      <view class="section-title" v-if="searchHistory.length">
        <text class="title-text">搜索历史</text>
        <text class="clear-btn" @click="clearHistory">清除</text>
      </view>
      <view class="tags-box" v-if="searchHistory.length">
        <u-tag
          v-for="(item, index) in searchHistory"
          :key="index"
          :text="item"
          plain
          @click="clickHistory(item)"
          class="history-tag"
        ></u-tag>
      </view>

      <!-- 热门搜索 -->
      <view class="section-title" v-if="hotKeywords.length" :class="{ 'mt-20': !searchHistory.length }">
        <text class="title-text">热门搜索</text>
      </view>
      <view class="tags-box">
        <u-tag
          v-for="(item, index) in hotKeywords"
          :key="index"
          :text="item"
          plain
          @click="doSearch(item)"
          class="hot-tag"
        ></u-tag>
      </view>
    </view>

    <!-- 搜索类型切换 -->
    <view v-if="searchKeyword || hasSearched" class="tab-bar">
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

    <!-- 搜索结果列表 -->
    <scroll-view
      v-if="searchKeyword || hasSearched"
      scroll-y
      :enable-flex="true"
      class="result-container"
    >
      <!-- 店铺搜索结果 -->
      <view v-show="currentTab === 0" class="result-list">
        <view
          v-for="(item, index) in shopList"
          :key="index"
          class="result-item shop-item"
          @click="toShopDetail(item.id)"
        >
          <image class="item-img" :src="fileUrl + item.img" mode="aspectFill"></image>
          <view class="item-info">
            <text class="item-title">{{ item.sname }}</text>
            <text class="item-desc">{{ item.note || '暂无简介' }}</text>
            <view class="item-meta">
              <text class="item-score">评分: {{ item.pf || '暂无' }}</text>
              <text class="item-address">{{ item.dz || '暂无地址' }}</text>
            </view>
          </view>
          <u-icon name="arrow-right" size="16" color="#ccc"></u-icon>
        </view>
        <u-empty v-if="currentTab === 0 && shopList.length === 0 && hasSearched" mode="search" text="暂无相关店铺"></u-empty>
      </view>

      <!-- 文章搜索结果 -->
      <view v-show="currentTab === 1" class="result-list">
        <imglist
          :showSearch="false"
          imgName="img"
          sName="note"
          imgSize="2"
          tName="username"
          tLabel="作者:"
          tColor="#ff943c"
          titleName="title"
          :dataList="articleList"
          @clickItem="toArticleDetail"
        ></imglist>
        <u-empty v-if="currentTab === 1 && articleList.length === 0 && hasSearched" mode="search" text="暂无相关动态"></u-empty>
      </view>

      <!-- 美食搜索结果 -->
      <view v-show="currentTab === 2" class="result-list">
        <imglist
          :showSearch="false"
          imgName="img"
          sName="note"
          imgSize="2"
          tName="pf"
          tLabel="评分:"
          tColor="#ff9900"
          titleName="gname"
          :dataList="foodList"
          @clickItem="toFoodDetail"
        ></imglist>
        <u-empty v-if="currentTab === 2 && foodList.length === 0 && hasSearched" mode="search" text="暂无相关美食"></u-empty>
      </view>

      <view class="safe-area-bottom"></view>
    </scroll-view>
  </view>
</template>

<script>
import { listj, listSqlj, fileUrl } from '@/common/config/api.js'
import { mapState } from 'vuex'

export default {
  data() {
    return {
      statusBarHeight: 0,
      searchKeyword: '',
      hasSearched: false,
      currentTab: 0,
      tabs: [
        { name: '店铺', type: 'shop' },
        { name: '动态', type: 'article' },
        { name: '美食', type: 'food' }
      ],
      searchHistory: [],
      hotKeywords: [],
      // 全部数据（用于本地过滤）
      allShopList: [],
      allArticleList: [],
      allFoodList: [],
      // 搜索结果
      shopList: [],
      articleList: [],
      foodList: [],
      fileUrl: fileUrl
    }
  },
  created() {
    const sysInfo = uni.getSystemInfoSync()
    this.statusBarHeight = sysInfo.statusBarHeight || 0
  },
  onLoad() {
    this.loadAllData()
  },
  onShow() {
    this.loadSearchHistory()
  },
  methods: {
    // 加载全部数据
    loadAllData() {
      this.loadShops()
      this.loadArticles()
      this.loadFoods()
    },
    // 加载搜索历史
    loadSearchHistory() {
      this.searchHistory = uni.getStorageSync('search_history') || []
      this.calcHotKeywords()
    },
    // 加载店铺数据
    loadShops() {
      listSqlj({ params: { sql: 'select * from fs_shop order by id desc' } }).then(res => {
        this.allShopList = res || []
      })
    },
    // 加载文章数据
    loadArticles() {
      listj({ params: { table: 'blog' } }).then(res => {
        this.allArticleList = res || []
      })
    },
    // 加载美食数据
    loadFoods() {
      listj({ params: { table: 'good' } }).then(res => {
        this.allFoodList = res || []
      })
    },
    // 搜索处理
    handleSearch() {
      if (!this.searchKeyword) {
        this.hasSearched = false
        this.clearResults()
        return
      }
      this.hasSearched = true
      this.doSearch(this.searchKeyword)
    },
    // 输入框输入：只更新关键字，不立即搜索/存历史
    onSearchInput(e) {
      this.searchKeyword = (e.detail && e.detail.value) || ''
      this.hasSearched = false
      this.clearResults()
    },
    // 清空输入框
    clearSearchInput() {
      this.searchKeyword = ''
      this.hasSearched = false
      this.clearResults()
    },
    // 执行搜索
    doSearch(keyword) {
      if (keyword) {
        this.searchKeyword = keyword
        this.hasSearched = true
        // 保存搜索历史
        this.saveSearchHistory(keyword)
      }
      const kw = keyword || this.searchKeyword
      if (!kw) return

      const lowerKw = kw.toLowerCase()

      // 搜索店铺
      this.shopList = this.allShopList.filter(item => {
        const sname = (item.sname || '').toLowerCase()
        const note = (item.note || '').toLowerCase()
        const dz = (item.dz || '').toLowerCase()
        return sname.includes(lowerKw) || note.includes(lowerKw) || dz.includes(lowerKw)
      })

      // 搜索文章
      this.articleList = this.allArticleList.filter(item => {
        const title = (item.title || '').toLowerCase()
        const note = (item.note || '').toLowerCase()
        const username = (item.username || '').toLowerCase()
        return title.includes(lowerKw) || note.includes(lowerKw) || username.includes(lowerKw)
      })

      // 搜索美食
      this.foodList = this.allFoodList.filter(item => {
        const gname = (item.gname || '').toLowerCase()
        const note = (item.note || '').toLowerCase()
        return gname.includes(lowerKw) || note.includes(lowerKw)
      })

      // 切换到有结果的Tab
      if (this.shopList.length > 0) {
        this.currentTab = 0
      } else if (this.articleList.length > 0) {
        this.currentTab = 1
      } else if (this.foodList.length > 0) {
        this.currentTab = 2
      }
    },
    // 切换Tab
    switchTab(index) {
      this.currentTab = index
    },
    // 清空结果
    clearResults() {
      this.shopList = []
      this.articleList = []
      this.foodList = []
    },
    // 跳转店铺详情
    toShopDetail(id) {
      uni.navigateTo({ url: '/pages/shops/shopdetail?pid=' + id })
    },
    // 跳转文章详情
    toArticleDetail(id) {
      uni.navigateTo({ url: '/pages/blog/blogdetail?id=' + id })
    },
    // 跳转美食详情
    toFoodDetail(id) {
      uni.navigateTo({ url: '/pages/good/gooddetail?gid=' + id })
    },
    // 返回首页
    goBack() {
      uni.navigateBack()
    },
    // 保存搜索历史
    saveSearchHistory(keyword) {
      if (!keyword) return
      let history = uni.getStorageSync('search_history') || []
      // 如果已存在则移除（后面会加到最前面）
      const index = history.indexOf(keyword)
      if (index > -1) {
        history.splice(index, 1)
      }
      // 添加到最前面
      history.unshift(keyword)
      // 最多保留20条
      if (history.length > 20) {
        history = history.slice(0, 20)
      }
      uni.setStorageSync('search_history', history)
      this.searchHistory = history
      // 重新计算热门搜索
      this.calcHotKeywords()
    },
    // 从历史记录加载并计算热门搜索
    calcHotKeywords() {
      const history = uni.getStorageSync('search_history') || []
      if (history.length === 0) {
        this.hotKeywords = []
        return
      }
      // 统计词频
      const countMap = {}
      history.forEach(item => {
        countMap[item] = (countMap[item] || 0) + 1
      })
      // 转换为数组并按频率排序
      const sorted = Object.entries(countMap)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 10) // 取前10个
        .map(item => item[0])
      this.hotKeywords = sorted
    },
    // 清除搜索历史
    clearHistory() {
      uni.removeStorageSync('search_history')
      this.searchHistory = []
      this.hotKeywords = []
    },
    // 点击历史记录搜索
    clickHistory(keyword) {
      this.searchKeyword = keyword
      this.doSearch(keyword)
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

/* 顶部搜索栏 */
.search-header {
  background-color: #fff;
  padding: 24rpx;
  padding-bottom: 16rpx;
  box-sizing: border-box;
}

.search-bar {
  width: 100%;
  display: flex;
  align-items: center;

  .back-btn {
    width: 60rpx;
    height: 60rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 16rpx;
    flex-shrink: 0;
  }

  .search-input-wrap {
    flex: 1;
    height: 64rpx;
    background-color: #f5f5f5;
    border-radius: 32rpx;
    display: flex;
    align-items: center;
    padding: 0 24rpx;
    position: relative;
  }

  .search-input {
    flex: 1;
    height: 100%;
    font-size: 28rpx;
    color: #333;
  }

  .search-clear {
    width: 36rpx;
    height: 36rpx;
    line-height: 36rpx;
    text-align: center;
    font-size: 36rpx;
    color: #999;
    margin-left: 12rpx;
  }
}

/* 热门搜索 */
.suggest-section {
  background-color: #fff;
  padding: 24rpx;
  margin-top: 16rpx;

  .section-title {
    margin-bottom: 20rpx;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title-text {
      font-size: 28rpx;
      font-weight: bold;
      color: #333;
    }

    .clear-btn {
      font-size: 24rpx;
      color: #999;
    }
  }

  .mt-20 {
    margin-top: 20rpx;
  }

  .tags-box {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;

    .hot-tag,
    .history-tag {
      margin: 0;
    }
  }
}

/* Tab切换栏 */
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

/* 结果容器 */
.result-container {
  height: calc(100vh - 200rpx - env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.result-list {
  padding: 16rpx 24rpx;
}

/* 店铺结果项 */
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
    }
  }
}

/* 底部安全区 */
.safe-area-bottom {
  height: calc(20rpx + env(safe-area-inset-bottom));
  min-height: env(safe-area-inset-bottom);
}
</style>
