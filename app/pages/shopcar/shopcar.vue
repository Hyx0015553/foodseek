<template>
  <view class="page-container">
    <!-- 导航栏：透明背景增强高级感 -->
    <u-navbar
        title="购物车"
        :border="false"
        :placeholder="true"
        :autoBack="true"
        bgColor="#f8f9fa"
        titleStyle="font-weight: bold; color: #1a1a1a;"
    ></u-navbar>

    <scroll-view scroll-y class="sv-container">
      <!-- 顶部状态提示：模拟丝滑体验 -->
      <view class="cart-header" v-if="carlist.length > 0">
        <text class="count-tip">共 {{ carlist.length }} 件商品</text>
        <text class="manage-btn" @click="bjstatus = !bjstatus">{{ bjstatus ? '完成' : '编辑' }}</text>
      </view>

      <!-- 空状态展示 -->
      <view class="empty-state" v-if="carlist.length === 0">
        <text class="empty-icon">🛒</text>
        <text class="empty-text">购物车空空如也</text>
        <view class="go-shopping" @click="goBack">去逛逛</view>
      </view>

      <!-- 购物车列表 -->
      <view class="shop-list">
        <checkbox-group @change="toggleSelectList">
          <view class="car-item" v-for="(item, index) in carlist" :key="index">
            <!-- 选择框 -->
            <view class="checkbox-box" v-if="bjstatus">
              <checkbox
                  :checked="item.checked"
                  :value="item.id+''"
                  color="#ff943c"
                  style="transform: scale(0.85);"
              />
            </view>

            <!-- 商品图片 -->
            <view class="img-wrapper" @click="toGoodDetail(item.id)">
              <image
                  class="car-img"
                  :src="item.img ? fileUrl + item.img : ''"
                  mode="aspectFill"
              ></image>
              <!-- 如果图片加载失败的占位符 -->
              <view v-if="!item.img" class="img-placeholder">🛍️</view>
            </view>

            <!-- 商品信息 -->
            <view class="car-info">
              <view class="info-top">
                <text class="goods-name" @click="toGoodDetail(item.id)">{{ item.gname }}</text>
                <view class="del-icon" @click="delCar(item.id)">
                  <u-icon name="trash" color="#ff4d4f" size="20"></u-icon>
                </view>
              </view>

              <view class="info-bottom">
                <view class="price-box">
                  <text class="currency">¥</text>
                  <text class="price-integer">{{ item.price }}</text>
                </view>
                <view class="stepper-wrapper">
                  <u-number-box
                      v-model="item.count"
                      @change="jisuantotal"
                      button-size="28"
                      color="#333"
                      bgColor="#f2f3f5"
                      iconStyle="color: #ffffff; font-weight: bold"
                  ></u-number-box>
                </view>
              </view>
            </view>
          </view>
        </checkbox-group>
      </view>

      <!-- 底部占位，防止被遮挡 -->
      <view class="safe-area-inset-bottom"></view>
    </scroll-view>

    <!-- 底部结算栏：区分平台 -->
    <!-- 使用内联样式动态控制 H5 和小程序/App 的底部距离 -->
    <view :class="['footer-bar', isH5 ? 'footer-h5' : '']">
      <view class="footer-left">
        <text class="total-label">合计:</text>
        <view class="total-price-box">
          <text class="symbol">¥</text>
          <text class="total-value">{{ totalprice }}</text>
        </view>
      </view>
      <view
          class="submit-btn"
          :class="totalprice > 0 ? 'submit-btn-active' : 'submit-btn-disabled'"
          @click="tijiaoBtn"
      >
        结算
      </view>
    </view>
  </view>
</template>

<script>
import { mapState, mapActions } from 'vuex'
import { listj, findj, fileUrl } from '@/common/config/api.js';
import { ideautil, yewuutil } from '@/common/commontools.js';

export default {
  data() {
    return {
      fileUrl: fileUrl,
      bjstatus: true,
      totalprice: 0,
      // #ifdef H5
      isH5: true,
      // #endif
      // #ifndef H5
      isH5: false,
      // #endif
    };
  },
  onShow() {
    this.jisuantotal()
  },
  onLoad() {
    if (!this.userInfo || !this.userInfo.id) {
      uni.ytool.toLogin()
    }
    this.jisuantotal()
  },
  methods: {
    toGoodDetail: yewuutil.toGoodDetail,
    ...mapActions(['setCar']),
    delCar(id) {
      uni.showModal({
        title: '提示',
        content: '确定要从购物车移除吗？',
        success: (res) => {
          if (res.confirm) {
            let clist = this.carlist.filter(obj => obj.id != id);
            this.setCar(clist);
            this.jisuantotal();
          }
        }
      });
    },
    tijiaoBtn() {
      if (this.totalprice > 0) {
        uni.itool.nto({
          url: '../bill/surebill'
        })
      } else {
        uni.showToast({ title: '请先选择商品', icon: 'none' });
      }
    },
    goBack() {
      uni.switchTab({
        url: '/pages/index/index' // 假设首页是逛逛的目标
      })
    },
    toggleSelectList(v) {
      let checkedlist = v.detail.value;
      this.carlist.forEach(tobj => {
        tobj.checked = checkedlist.includes(tobj.id + '');
      });
      this.jisuantotal();
    },
    jisuantotal() {
      // 延迟计算确保 v-model 更新完成
      this.$nextTick(() => {
        let total = 0;
        this.carlist.forEach(obj => {
          if (obj.checked) {
            total += (Number(obj.price) * Number(obj.count));
          }
        });
        this.totalprice = total.toFixed(2);
      });
    }
  },
  computed: {
    ...mapState(['carlist', 'userInfo'])
  }
}
</script>

<style lang="scss" scoped>
/* 核心变量 */
$main-bg: #f8f9fa;
$card-bg: #ffffff;
$primary-color: #ff943c;
$accent-color: #ff4d4f;
$text-main: #1a1a1a;
$text-sub: #909399;
$border-radius: 20rpx;

.page-container {
  background-color: $main-bg;
  min-height: 100vh;
}

.sv-container {
  height: calc(100vh - 110px); /* 动态适配高度 */
}

/* 顶部状态 */
.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 32rpx;

  .count-tip {
    font-size: 26rpx;
    color: $text-sub;
  }

  .manage-btn {
    font-size: 26rpx;
    color: $primary-color;
    font-weight: 500;
  }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;

  .empty-icon {
    font-size: 120rpx;
    margin-bottom: 30rpx;
  }

  .empty-text {
    color: $text-sub;
    font-size: 28rpx;
    margin-bottom: 40rpx;
  }

  .go-shopping {
    padding: 16rpx 60rpx;
    border: 1px solid $primary-color;
    color: $primary-color;
    border-radius: 40rpx;
    font-size: 26rpx;
  }
}

/* 列表样式 */
.shop-list {
  padding: 0 24rpx 40rpx;

  .car-item {
    background-color: $card-bg;
    border-radius: $border-radius;
    padding: 24rpx;
    margin-bottom: 24rpx;
    display: flex;
    align-items: center;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.02);
    transition: transform 0.2s;

    &:active {
      transform: scale(0.99);
    }

    .checkbox-box {
      margin-right: 16rpx;
    }

    .img-wrapper {
      width: 180rpx;
      height: 180rpx;
      background-color: #f5f5f5;
      border-radius: 12rpx;
      overflow: hidden;
      position: relative;

      .car-img {
        width: 100%;
        height: 100%;
      }

      .img-placeholder {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        font-size: 60rpx;
      }
    }

    .car-info {
      flex: 1;
      margin-left: 24rpx;
      height: 180rpx;
      display: flex;
      flex-direction: column;
      justify-content: space-between;

      .info-top {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;

        .goods-name {
          font-size: 30rpx;
          color: $text-main;
          font-weight: 500;
          line-height: 1.4;
          display: -webkit-box;
          -webkit-box-orient: vertical;
          -webkit-line-clamp: 2;
          overflow: hidden;
          flex: 1;
          margin-right: 20rpx;
        }

        .del-icon {
          padding: 10rpx;
        }
      }

      .info-bottom {
        display: flex;
        justify-content: space-between;
        align-items: flex-end;

        .price-box {
          color: $accent-color;

          .currency {
            font-size: 24rpx;
            font-weight: bold;
          }

          .price-integer {
            font-size: 36rpx;
            font-weight: bold;
            margin-left: 4rpx;
          }
        }

        .stepper-wrapper {
          ::v-deep .u-number-box {
            background: #f2f3f5;
            border-radius: 8rpx;
            padding: 4rpx;
          }
          ::v-deep .u-number-box__minus:not(.u-number-box__minus--disabled),
          ::v-deep .u-number-box__plus:not(.u-number-box__minus--disabled) {
            background-color: #ff943c !important;
          }
          ::v-deep .u-number-box__minus:not(.u-number-box__minus--disabled) .u-icon,
          ::v-deep .u-number-box__plus:not(.u-number-box__minus--disabled) .u-icon {
            color: #ffffff !important;
          }
        }
      }
    }
  }
}

/* 底部结算栏 */
.footer-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 110rpx;
  background-color: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32rpx;
  padding-bottom: constant(safe-area-inset-bottom);
  padding-bottom: env(safe-area-inset-bottom);
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);
  z-index: 100;

  .footer-left {
    display: flex;
    align-items: baseline;

    .total-label {
      font-size: 28rpx;
      color: $text-main;
    }

    .total-price-box {
      margin-left: 12rpx;
      color: $accent-color;

      .symbol {
        font-size: 24rpx;
        font-weight: bold;
      }

      .total-value {
        font-size: 40rpx;
        font-weight: bold;
      }
    }
  }

  .submit-btn {
    width: 240rpx;
    height: 80rpx;
    border-radius: 40rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 30rpx;
    font-weight: bold;
    transition: all 0.3s;

    &:active {
      opacity: 0.8;
      transform: scale(0.95);
    }
  }

  .submit-btn-active {
    background: $primary-color;
    color: #ffffff;
    box-shadow: 0 6rpx 16rpx rgba(255, 148, 60, 0.35);
  }

  .submit-btn-disabled {
    background: #e4e7ed;
    color: #ffffff;
  }
}

/* H5端适配：H5 通常有底部 TabBar 遮挡，需要向上偏移 */
.footer-h5 {
  bottom: 50px; /* 根据实际 TabBar 高度调整 */
}

/* 安全区域占位 */
.safe-area-inset-bottom {
  height: calc(110rpx + constant(safe-area-inset-bottom));
  height: calc(110rpx + env(safe-area-inset-bottom));
}

</style>