<template>
  <view class="page">
    <u-navbar
      title="管理菜品"
      :border="false"
      :placeholder="true"
      :autoBack="true"
      bgColor="#ffffff"
      titleStyle="font-weight: bold; color: #1a1a1a;"
      @rightClick="toAddGood"
    >
      <!-- 微信小程序胶囊按钮占位，避免「新增」与系统按钮重叠 -->
      <template #right>
        <view class="nav-right-add" :style="navRightStyle">
          <text class="nav-right-add__text">新增</text>
        </view>
      </template>
    </u-navbar>

    <scroll-view scroll-y class="main-scroll" :enable-flex="true">
      <view v-if="!userInfo || !userInfo.sid" class="empty-box">
        <text class="empty-icon">🔒</text>
        <text class="empty-text">请先登录商家账号</text>
      </view>

      <view v-else class="content">
        <view v-if="loading" class="hint">加载中…</view>

        <view v-else-if="!fobjList.length" class="empty-box">
          <text class="empty-icon">🍜</text>
          <text class="empty-text">暂无菜品，点击右上角「新增」添加</text>
        </view>

        <view v-else class="good-list">
          <view v-for="item in fobjList" :key="item.id" class="good-card">
            <image
              v-if="item.img"
              class="good-cover"
              :src="fileUrl + item.img"
              mode="aspectFill"
              @tap="toEditGood(item.id)"
            />
            <view v-else class="good-cover good-cover--ph" @tap="toEditGood(item.id)">🍲</view>

            <view class="good-body" @tap="toEditGood(item.id)">
              <text class="good-name">{{ item.gname || '未命名' }}</text>
              <text class="good-meta">{{ item.type || '未分类' }} · ¥{{ item.price || '—' }}</text>
              <view class="good-tags">
                <text class="tag" :class="isGoodOnShelf(item) ? 'tag--on' : 'tag--off'">{{ goodStateLabel(item.state) || item.statecn || '—' }}</text>
                <text v-if="item.pf" class="tag tag--pf">评分 {{ item.pf }}</text>
              </view>
            </view>

            <view class="good-actions" @tap.stop>
              <view class="btn btn--edit" @tap="toEditGood(item.id)">编辑</view>
              <view class="btn btn--del" @tap="confirmDelete(item)">删除</view>
            </view>
          </view>
        </view>

        <view class="bottom-safe"></view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { listj, deletej, fileUrl } from '@/common/config/api.js'
import { goodStateLabel, isGoodOnShelf } from '@/common/goodState.js'
import { mapState, mapActions } from 'vuex'

export default {
  data() {
    return {
      fileUrl: fileUrl,
      fobjList: [],
      loading: false,
      /** 导航栏右侧额外间距（px），避开小程序胶囊 */
      navRightPadPx: 0
    }
  },
  computed: {
    navRightStyle() {
      if (!this.navRightPadPx) return {}
      return { paddingRight: this.navRightPadPx + 'px' }
    },
    ...mapState(['userInfo'])
  },
  onLoad() {
    this.initNavRightPad()
    this.loadList()
  },
  onShow() {
    this.loadList()
  },
  methods: {
    ...mapActions(['updateUserInfo']),
    initNavRightPad() {
      // #ifdef MP-WEIXIN
      try {
        const sys = uni.getSystemInfoSync()
        const mb = uni.getMenuButtonBoundingClientRect()
        if (mb && sys.windowWidth != null) {
          // 从屏幕右缘到胶囊左缘的宽度，用于把「新增」整体左移，避免与胶囊重叠
          const gap = Math.max(0, sys.windowWidth - mb.left)
          this.navRightPadPx = gap
        }
      } catch (e) {}
      // #endif
    },
    loadList() {
      if (!this.userInfo || !this.userInfo.sid) {
        this.fobjList = []
        return
      }
      this.loading = true
      listj({ params: { table: 'good', sid: this.userInfo.sid } })
        .then((res) => {
          this.fobjList = res || []
        })
        .catch(() => {
          this.fobjList = []
        })
        .finally(() => {
          this.loading = false
        })
    },
    toAddGood() {
      uni.navigateTo({ url: '/pages/good/goodmg' })
    },
    toEditGood(id) {
      if (id == null || id === '') return
      uni.navigateTo({ url: '/pages/good/goodmg?tid=' + id })
    },
    confirmDelete(item) {
      const name = (item && item.gname) ? item.gname : '该菜品'
      uni.showModal({
        title: '删除确认',
        content: `确定删除「${name}」吗？删除后不可恢复。`,
        confirmColor: '#ff4d4f',
        success: (res) => {
          if (!res.confirm) return
          uni.showLoading({ title: '删除中' })
          deletej({ params: { table: 'good', id: item.id } })
            .then(() => {
              uni.hideLoading()
              uni.showToast({ title: '已删除', icon: 'success' })
              this.loadList()
            })
            .catch(() => {
              uni.hideLoading()
              uni.showToast({ title: '删除失败', icon: 'none' })
            })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
$bg: #f5f7fa;
$card: #ffffff;
$primary: #ff943c;
$danger: #ff4d4f;
$text: #1a1a1a;
$sub: #909399;

.page {
  min-height: 100vh;
  background: $bg;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.main-scroll {
  flex: 1;
  min-height: 0;
}

.nav-right-add {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
}

.nav-right-add__text {
  font-size: 30rpx;
  color: #333333;
}

.content {
  padding: 24rpx 28rpx 0;
}

.hint {
  text-align: center;
  font-size: 28rpx;
  color: $sub;
  padding: 48rpx 0;
}

.empty-box {
  padding: 120rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-icon {
  font-size: 88rpx;
  margin-bottom: 24rpx;
  opacity: 0.85;
}

.empty-text {
  font-size: 28rpx;
  color: $sub;
  text-align: center;
  line-height: 1.6;
}

.good-list {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.good-card {
  display: flex;
  align-items: stretch;
  background: $card;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 8rpx 28rpx rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.good-cover {
  width: 160rpx;
  height: 160rpx;
  border-radius: 20rpx;
  flex-shrink: 0;
  background: #f0f2f5;
}

.good-cover--ph {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 64rpx;
}

.good-body {
  flex: 1;
  min-width: 0;
  margin-left: 24rpx;
  margin-right: 16rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.good-name {
  font-size: 32rpx;
  font-weight: 700;
  color: $text;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
}

.good-meta {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: $sub;
}

.good-tags {
  margin-top: 14rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}

.tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
  background: #f3f4f6;
  color: #666;
}

.tag--on {
  background: rgba($primary, 0.12);
  color: #d97706;
}

.tag--off {
  background: #fff1f0;
  color: #cf1322;
}

.tag--pf {
  background: #fff8e6;
  color: #d48806;
}

.good-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 16rpx;
}

.btn {
  min-width: 120rpx;
  text-align: center;
  font-size: 24rpx;
  padding: 14rpx 20rpx;
  border-radius: 999rpx;
  font-weight: 600;
}

.btn--edit {
  color: $primary;
  background: rgba($primary, 0.12);
  border: 1rpx solid rgba($primary, 0.35);
}

.btn--del {
  color: #fff;
  background: linear-gradient(135deg, #ff7875, $danger);
  box-shadow: 0 6rpx 16rpx rgba(255, 77, 79, 0.25);
}

.bottom-safe {
  height: calc(40rpx + env(safe-area-inset-bottom));
}
</style>
