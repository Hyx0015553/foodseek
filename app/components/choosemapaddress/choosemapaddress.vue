<template>
  <view class="choose-map-address" @tap="handleOpenMap">
    <!-- 支持自定义插槽 -->
    <slot>
      <view class="input-container" :class="{ 'has-value': addressName }">
        <text class="location-icon">📍</text>
        <text class="address-text">{{ addressName || placeholder }}</text>
        <text class="arrow">></text>
      </view>
    </slot>
  </view>
</template>

<script>
/**
 * choose-map-address 地图选点组件
 * @description 适配微信小程序与H5，移除外部图标依赖
 */
export default {
  name: "choose-map-address",
  props: {
    // 接收 v-model 绑定的对象 {latitude, longitude, name, address}
    value: {
      type: Object,
      default: () => ({})
    },
    placeholder: {
      type: String,
      default: "点击选择位置"
    }
  },
  computed: {
    addressName() {
      // 优先显示地点名称，没有则显示详细地址
      return this.value.name || this.value.address || "";
    }
  },
  methods: {
    // 打开地图主逻辑
    async handleOpenMap() {
      // #ifdef MP-WEIXIN
      const hasAuth = await this.checkWechatPermission();
      if (!hasAuth) return;
      // #endif

      this.doChooseLocation();
    },

    // 执行选点
    doChooseLocation() {
      uni.chooseLocation({
        success: (res) => {
          const result = {
            latitude: res.latitude,
            longitude: res.longitude,
            name: res.name,
            address: res.address
          };
          // 兼容 Vue2 v-model
          this.$emit('input', result);
          // 兼容 Vue3 v-model (如果你是Vue3环境)
          this.$emit('update:modelValue', result);
          // 触发确认事件
          this.$emit('confirm', result);
        },
        fail: (err) => {
          console.log('选点失败或取消：', err);
        }
      });
    },

    // 微信小程序特有的权限检查
    checkWechatPermission() {
      return new Promise((resolve) => {
        uni.getSetting({
          success: (res) => {
            if (res.authSetting['scope.userLocation'] === false) {
              // 用户之前拒绝过，需引导去设置页
              uni.showModal({
                title: '提示',
                content: '需要位置权限才能选择地址，请在设置中开启',
                confirmText: '去设置',
                success: (modalRes) => {
                  if (modalRes.confirm) {
                    uni.openSetting({
                      success: (setRes) => {
                        resolve(setRes.authSetting['scope.userLocation']);
                      }
                    });
                  } else {
                    resolve(false);
                  }
                }
              });
            } else {
              resolve(true);
            }
          },
          fail: () => resolve(false)
        });
      });
    }
  }
};
</script>

<style scoped>
.choose-map-address {
  width: 100%;
  display: block;
}

.input-container {
  display: flex;
  align-items: center;
  padding: 24rpx;
  background-color: #f8f8f8;
  border-radius: 12rpx;
  color: #999;
  font-size: 28rpx;
  min-height: 40rpx;
  border: 1px solid #eee;
}

.has-value {
  color: #333;
  background-color: #fff;
}

.location-icon {
  margin-right: 12rpx;
  font-size: 32rpx;
}

.address-text {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  margin-right: 10rpx;
}

.arrow {
  color: #ccc;
  font-family: simsun;
  font-size: 24rpx;
}
</style>
