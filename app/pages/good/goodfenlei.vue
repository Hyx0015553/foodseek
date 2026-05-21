<template>
  <view class="page-container">
    <u-navbar
      title="美食分类筛选"
      :border="false"
      :placeholder="true"
      :autoBack="true"
      bgColor="#ffffff"
      titleStyle="font-weight: bold; color: #1a1a1a;"
    ></u-navbar>

    <view class="main-body">
      <!-- 左侧大类 -->
      <scroll-view scroll-y class="side-bar">
        <view
          v-for="item in typelist"
          :key="item.id"
          class="side-item"
          :class="[activeMainId === item.id ? 'side-item-active' : '']"
          @click="handleMainCategory(item.id)"
        >
          <text class="side-text">{{ item.title }}</text>
          <view class="active-line" v-if="activeMainId === item.id"></view>
        </view>
      </scroll-view>

      <!-- 右侧内容 -->
      <view class="content-area">
        <!-- 子分类 tabs（可选） -->
        <view class="tabs-wrapper" v-if="ctypelist.length > 0">
          <u-tabs
            :list="ctypelist"
            keyName="title"
            @change="refreshGoodByCtype"
            lineColor="#ff943c"
            :activeStyle="{ color: '#ff943c', fontWeight: 'bold' }"
            :inactiveStyle="{ color: '#909399' }"
          ></u-tabs>
        </view>

        <!-- 排序条 -->
        <view class="sort-bar">
          <view
            v-for="item in sortlist"
            :key="item.id"
            class="sort-item"
            :class="[ptype === item.id ? 'sort-item-active' : '']"
            @click="handleSort(item)"
          >
            <text>{{ item.title }}</text>
          </view>
        </view>

        <!-- 商品列表 -->
        <scroll-view scroll-y class="goods-scroll">
          <view class="goods-list">
            <view class="empty-box" v-if="fobjList.length === 0">
              <text class="empty-icon">🍽️</text>
              <text class="empty-text">当前条件下暂无菜品</text>
            </view>

            <view
              v-for="item in fobjList"
              :key="item.id"
              class="good-card"
              @click="toGoodDetail(item.id)"
            >
              <image
                class="good-img"
                :src="fileUrl + item.img"
                mode="aspectFill"
              ></image>
              <view class="good-info">
                <view class="good-name">{{ item.gname }}</view>
                <view class="good-tags">
                  <text class="tag-item">⭐ {{ item.pf || '5.0' }}</text>
                  <text class="tag-item">{{ item.shop || '未知店铺' }}</text>
                </view>
                <view class="good-bottom">
                  <view class="price-box">
                    <text class="currency">¥</text>
                    <text class="price-val">{{ item.price }}</text>
                  </view>
                </view>
              </view>
            </view>
          </view>
          <view class="safe-area-bottom"></view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import { listj, fileUrl } from '@/common/config/api.js'
import { GOOD_STATE } from '@/common/goodState.js'
import { yewuutil } from '@/common/commontools.js'

export default {
  data() {
    return {
      fileUrl: fileUrl,
      typelist: [],
      ctypelist: [],
      fobjList: [],
      activeMainId: 0,
      ptype: 2,
      sortlist: [
        { id: 0, title: '价格从高到低' },
        { id: 1, title: '价格从低到高' },
        { id: 2, title: '评分从高到低' }
      ]
    }
  },
  onLoad() {
    this.initData()
  },
  methods: {
    initData() {
      // 加载所有上架菜品
      listj({
        params: {
          table: 'good',
          state: GOOD_STATE.ON_SHELF
        }
      }).then(res => {
        this.fobjList = res || []
        this.sortChange({ id: this.ptype })
      })

      // 加载所有商品大类
      listj({
        params: {
          table: 'type'
        }
      }).then(res => {
        const list = res || []
        this.typelist = [{ id: 0, title: '全部' }, ...list]
      })
    },
    handleMainCategory(id) {
      this.activeMainId = id
      const typeid = id === 0 ? null : id
      listj({
        params: {
          table: 'good',
          typeid: typeid,
          state: GOOD_STATE.ON_SHELF
        }
      }).then(res => {
        this.fobjList = res || []
        this.sortChange({ id: this.ptype })
        this.refreshCtype(typeid)
      })
    },
    refreshCtype(typeid) {
      if (!typeid) {
        this.ctypelist = []
        return
      }
      listj({
        params: {
          table: 'type2',
          pid: typeid
        }
      }).then(res => {
        this.ctypelist = res || []
      })
    },
    refreshGoodByCtype(tab) {
      const ctypeid = tab.id === 0 ? null : tab.id
      listj({
        params: {
          table: 'good',
          ctypeid: ctypeid,
          state: GOOD_STATE.ON_SHELF
        }
      }).then(res => {
        this.fobjList = res || []
        this.sortChange({ id: this.ptype })
      })
    },
    handleSort(item) {
      this.ptype = item.id
      this.sortChange(item)
    },
    sortChange(e) {
      this.ptype = e.id
      if (!this.fobjList || !this.fobjList.length) return
      if (this.ptype === 0) {
        this.fobjList.sort((a, b) => (Number(b.price) || 0) - (Number(a.price) || 0))
      } else if (this.ptype === 1) {
        this.fobjList.sort((a, b) => (Number(a.price) || 0) - (Number(b.price) || 0))
      } else if (this.ptype === 2) {
        this.fobjList.sort((a, b) => (Number(b.pf) || 0) - (Number(a.pf) || 0))
      }
    },
    toGoodDetail: yewuutil.toGoodDetail
  }
}
</script>

<style lang="scss" scoped>
$primary-color: #ff943c;
$bg-main: #f8f9fa;
$side-bg: #f3f4f6;
$text-dark: #1a1a1a;
$text-grey: #909399;

.page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.main-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.side-bar {
  width: 180rpx;
  background-color: $side-bg;
  height: 100%;

  .side-item {
    height: 100rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    transition: all 0.2s;

    .side-text {
      font-size: 26rpx;
      color: #555;
      text-align: center;
      padding: 0 10rpx;
    }
  }

  .side-item-active {
    background-color: #ffffff;

    .side-text {
      color: $text-dark;
      font-weight: bold;
    }

    .active-line {
      position: absolute;
      left: 0;
      top: 30rpx;
      bottom: 30rpx;
      width: 6rpx;
      background-color: $primary-color;
      border-radius: 0 4rpx 4rpx 0;
    }
  }
}

.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
}

.tabs-wrapper {
  padding: 10rpx 0;
  border-bottom: 1rpx solid #f2f3f5;
}

.sort-bar {
  display: flex;
  padding: 20rpx;
  gap: 20rpx;

  .sort-item {
    padding: 10rpx 24rpx;
    background-color: #f2f3f5;
    border-radius: 30rpx;
    font-size: 22rpx;
    color: $text-grey;
  }

  .sort-item-active {
    background-color: rgba(255, 148, 60, 0.1);
    color: $primary-color;
    font-weight: bold;
  }
}

.goods-scroll {
  flex: 1;
  overflow: hidden;
}

.goods-list {
  padding: 0 20rpx 40rpx;
}

.good-card {
  display: flex;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f2f3f5;

  .good-img {
    width: 160rpx;
    height: 160rpx;
    border-radius: 12rpx;
    background-color: #f5f5f5;
    flex-shrink: 0;
  }

  .good-info {
    flex: 1;
    margin-left: 20rpx;
    display: flex;
    flex-direction: column;
    justify-content: space-between;

    .good-name {
      font-size: 30rpx;
      color: $text-dark;
      font-weight: bold;
      display: -webkit-box;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 1;
      overflow: hidden;
    }

    .good-tags {
      display: flex;
      gap: 12rpx;
      margin: 8rpx 0;

      .tag-item {
        font-size: 20rpx;
        color: $text-grey;
        background-color: #f8f9fa;
        padding: 2rpx 8rpx;
        border-radius: 4rpx;
      }
    }

    .good-bottom {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .price-box {
        .currency {
          font-size: 22rpx;
          color: #ff4d4f;
          font-weight: bold;
        }
        .price-val {
          font-size: 36rpx;
          color: #ff4d4f;
          font-weight: bold;
        }
      }
    }
  }
}

.empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 100rpx;

  .empty-icon {
    font-size: 80rpx;
    margin-bottom: 20rpx;
  }

  .empty-text {
    color: $text-grey;
    font-size: 26rpx;
  }
}

.safe-area-bottom {
  height: calc(40rpx + env(safe-area-inset-bottom));
}
</style>

