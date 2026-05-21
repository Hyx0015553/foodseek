<template>
  <view class="page-container">
    <!-- 顶部导航 -->
    <u-navbar
        :title="focusshop.sname || '点餐'"
        :border="false"
        :placeholder="true"
        :autoBack="true"
        bgColor="#ffffff"
        titleStyle="font-weight: bold; color: #1a1a1a;"
    ></u-navbar>

    <!-- 主体双栏区域 -->
    <view class="main-body">
      <!-- 左侧分类侧边栏 -->
      <scroll-view scroll-y class="side-bar">
        <view
            v-for="(item, index) in typelist"
            :key="item.id"
            class="side-item"
            :class="[activeMainId === item.id ? 'side-item-active' : '']"
            @click="handleMainCategory(item.id)"
        >
          <text class="side-text">{{ item.title }}</text>
          <view class="active-line" v-if="activeMainId === item.id"></view>
        </view>
      </scroll-view>

      <!-- 右侧商品区域 -->
      <view class="content-area">
        <!-- 子分类 Tabs -->
        <view class="tabs-wrapper" v-if="ctypelist.length > 0">
          <u-tabs
              :list="ctypelist"
              keyName="title"
              @change="refreshGood2"
              lineColor="#3c9cff"
              :activeStyle="{ color: '#3c9cff', fontWeight: 'bold' }"
              :inactiveStyle="{ color: '#909399' }"
          ></u-tabs>
        </view>

        <!-- 排序筛选器 -->
        <view class="sort-bar">
          <view
              v-for="(item, index) in sortlist"
              :key="index"
              class="sort-item"
              :class="[ptype === item.id ? 'sort-item-active' : '']"
              @click="handleSort(item)"
          >
            <text>{{ item.title }}</text>
            <text class="sort-icon" v-if="ptype === item.id">▽</text>
          </view>
        </view>

        <!-- 商品列表 -->
        <scroll-view scroll-y class="goods-scroll">
          <view class="goods-list">
            <!-- 空状态 -->
            <view class="empty-box" v-if="fobjList.length === 0">
              <text class="empty-icon">🍽️</text>
              <text class="empty-text">该分类下暂无菜品</text>
            </view>

            <!-- 商品卡片 -->
            <view
                class="good-card"
                v-for="(item, index) in fobjList"
                :key="index"
                @click="toGoodDetail(item.id)"
            >
              <image class="good-img" :src="fileUrl + item.img" mode="aspectFill"></image>
              <view class="good-info">
                <view class="good-name">{{ item.gname }}</view>
                <view class="good-tags">
                  <text class="tag-item">⭐ {{ item.pf || '5.0' }}</text>
                  <text class="tag-item">月售 99+</text>
                </view>
                <view class="good-bottom">
                  <view class="price-box">
                    <text class="currency">¥</text>
                    <text class="price-val">{{ item.price }}</text>
                  </view>
<!--                  <view class="add-btn" @click.stop="addCarReal(item)">
                    <text class="add-icon">＋</text>
                  </view>-->
                </view>
              </view>
            </view>
          </view>
          <!-- 底部安全距离 -->
          <view class="safe-area-bottom"></view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import { listj, findj, fileUrl } from '@/common/config/api.js'
import { ideautil, yewuutil } from '@/common/commontools.js'
import { mapState, mapActions } from 'vuex'

export default {
  data() {
    return {
      fileUrl: fileUrl,
      fobjList: [],
      typelist: [],
      ctypelist: [],
      sortlist: [
        { id: 0, title: '价格降序' },
        { id: 1, title: '价格升序' },
        { id: 2, title: '评分最高' }
      ],
      focusshop: getApp().globalData.focusshop || {},
      ptype: 2,
      activeMainId: 0
    }
  },
  onLoad() {
    this.initData()
  },
  methods: {
    ...mapActions(['setCar']),
    initData() {
      // 加载商品
      listj({
        params: {
          table: 'good',
          statecn: '上架中',
          sid: this.focusshop.id
        }
      }).then(res => {
        this.fobjList = res || []
        this.sortChange({ id: this.ptype }) // 初始排序
      })

      // 加载分类
      listj({
        params: {
          table: 'type',
          ownid: this.focusshop.id
        }
      }).then(res => {
        this.typelist = [{ id: 0, title: "全部" }, ...(res || [])]
      })
    },
    handleMainCategory(id) {
      this.activeMainId = id
      this.refreshGood(id)
    },
    handleSort(item) {
      this.ptype = item.id
      this.sortChange(item)
    },
    sortChange(e) {
      this.ptype = e.id
      if (this.ptype == 0) {
        this.fobjList.sort((a, b) => b.price - a.price)
      } else if (this.ptype == 1) {
        this.fobjList.sort((a, b) => a.price - b.price)
      } else if (this.ptype == 2) {
        this.fobjList.sort((a, b) => (Number(b.pf) || 0) - (Number(a.pf) || 0))
      }
    },
    refreshGood(id) {
      const typeid = id === 0 ? null : id
      listj({
        params: {
          table: 'good',
          typeid: typeid,
          statecn: '上架中',
          sid: this.focusshop.id
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
      listj({ params: { table: 'type2', pid: typeid } }).then(res => {
        this.ctypelist = res || []
      })
    },
    refreshGood2(index) {
      const ctypeid = index.id === 0 ? null : index.id
      listj({
        params: {
          table: 'good',
          ctypeid: ctypeid,
          sid: this.focusshop.id,
          statecn: '上架中'
        }
      }).then(res => {
        this.fobjList = res || []
        this.sortChange({ id: this.ptype })
      })
    },
    toGoodDetail: yewuutil.toGoodDetail,
    addCarReal(item) {
      // 原生加入购物车逻辑
      let clist = [...this.carlist]
      let index = clist.findIndex(v => v.id === item.id)
      if (index > -1) {
        clist[index].count += 1
      } else {
        clist.push({ ...item, count: 1, checked: true })
      }
      this.setCar(clist)
      uni.showToast({ title: '已加入购物车', icon: 'none' })
    }
  },
  computed: {
    ...mapState(['userInfo', 'carlist'])
  }
}
</script>

<style lang="scss" scoped>
$primary-color: #3c9cff;
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

/* 左侧分类栏 */
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

/* 右侧内容区 */
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
    display: flex;
    align-items: center;

    .sort-icon {
      margin-left: 6rpx;
      font-size: 18rpx;
    }
  }

  .sort-item-active {
    background-color: rgba($primary-color, 0.1);
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

/* 商品卡片 */
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

      .add-btn {
        width: 48rpx;
        height: 48rpx;
        background-color: $primary-color;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 4rpx 10rpx rgba($primary-color, 0.3);

        .add-icon {
          color: #fff;
          font-size: 32rpx;
          font-weight: bold;
        }

        &:active {
          transform: scale(0.9);
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