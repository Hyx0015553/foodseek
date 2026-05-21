<template>
  <view class="page-container">
    <!-- 导航栏 -->
    <u-navbar title="个人信息" :border="false" :placeholder="true" :autoBack="true" bgColor="#ffffff" titleStyle="font-weight: bold; color: #1a1a1a;"></u-navbar>

    <scroll-view scroll-y class="sv-container">
      <view class="form-wrapper">

        <!-- 1. 头像选择区 -->
        <view class="avatar-section card">
          <view class="avatar-title">更换头像</view>
          <view class="avatar-content">
            <view class="upload-box">
              <ideaupload v-model="filelist" :maxCount="1"></ideaupload>
              <view class="avatar-hint" v-if="filelist.length === 0">👤</view>
            </view>

            <!-- #ifdef MP-WEIXIN -->
            <button class="wx-avatar-btn" open-type="chooseAvatar" @chooseavatar="getWxAvatar">
              <text class="btn-icon">✨</text>使用微信头像
            </button>
            <!-- #endif -->
          </view>
        </view>

        <!-- 2. 基础资料区 -->
        <view class="info-section card">
          <view class="section-header">
            <text class="header-icon">📝</text>
            <text class="header-text">基础资料</text>
          </view>

          <view class="form-item">
            <text class="label">用户名</text>
            <input id="nickname-input" @blur="setNickName" type="nickname" v-model="fobj.username" class="custom-input" placeholder="请设置用户名" />
          </view>

          <view class="form-item">
            <text class="label">电话号码</text>
            <input type="tel" v-model="fobj.tel" class="custom-input" placeholder="请输入手机号" />
          </view>

          <view class="form-item">
            <text class="label">登录密码</text>
            <input type="password" v-model="fobj.passwd" class="custom-input" placeholder="请输入新密码" />
          </view>

          <view class="form-item no-border">
            <text class="label">性别</text>
            <radio-group class="gender-group" @change="showSex">
              <label class="gender-label">
                <radio value="男" :checked="fobj.sex=='男'" color="#ff943c" />
                <text>男</text>
              </label>
              <label class="gender-label">
                <radio value="女" :checked="fobj.sex=='女'" color="#ff4d4f" />
                <text>女</text>
              </label>
            </radio-group>
          </view>
        </view>

        <!-- 3. 健康信息区 (仅当 roletype == 4 显示) -->
        <view class="info-section card health-card" v-if="fobj.roletype=='4'">
          <view class="section-header">
            <text class="header-icon">🌡️</text>
            <text class="header-text">健康打卡</text>
          </view>

          <view class="form-item">
            <text class="label">当日体温</text>
            <input type="digit" v-model="fobj.tiwen" class="custom-input" placeholder="例如: 36.5" />
            <text class="unit">℃</text>
          </view>

          <view class="form-item no-border">
            <text class="label">自我感觉</text>
            <textarea v-model="fobj.jknote" class="custom-textarea" placeholder="请简述身体状况..." />
          </view>
        </view>

        <!-- 提交按钮 -->
        <view class="submit-box">
          <button class="primary-btn" @click="updateInfo()">
            <text class="btn-text">保存修改</text>
          </button>
          <text class="safe-hint">🔒 您的信息将被加密存储</text>
        </view>

      </view>
      <view class="safe-area-bottom"></view>
    </scroll-view>
  </view>
</template>

<script>
import { listj, findj, savej, fileUrl, saveWxUser, uploadUrl } from '@/common/config/api.js'
import { mapState, mapActions } from 'vuex'

export default {
  data() {
    return {
      fobj: {},
      filelist: [],
      fileUrl: fileUrl,
      uploadUrl: uploadUrl,
      nickname: ''
    }
  },
  onLoad(params) {
    // 深度克隆用户信息，避免直接修改状态
    this.fobj = JSON.parse(JSON.stringify(this.userInfo))
    if (this.fobj.img) {
      setTimeout(() => {
        this.filelist = [{ url: this.fileUrl + this.fobj.img }]
      }, 100)
    }
  },
  methods: {
    ...mapActions(['updateUserInfo']),
    getWxAvatar(e) {
      let imgurl = e.detail.avatarUrl
      uni.uploadFile({
        url: uploadUrl,
        filePath: imgurl,
        name: 'file',
        success: (uploadFileRes) => {
          let img = uploadFileRes.data
          this.fobj.img = img
          this.filelist = [{ url: this.fileUrl + img, fileName: img }]
        }
      });
    },
    showSex(v) {
      this.fobj.sex = v.detail.value
    },
    setNickName(event) {
      uni.createSelectorQuery().in(this)
          .select("#nickname-input")
          .fields({ properties: ["value"] })
          .exec((res) => {
            const nickName = res?.[0]?.value
            this.fobj.username = nickName
            if (nickName) {
              findj({ params: { table: 'user', username: nickName } }).then(res2 => {
                if (res2 && res2.id != this.userInfo.id) {
                  uni.showToast({ icon: 'error', title: '用户名已存在!' })
                  this.fobj.username = ""
                }
              })
            }
          })
    },
    updateInfo() {
      if (this.fobj.username && this.fobj.tel) {
        this.fobj.table = 'user'
        if (this.filelist.length > 0 && this.filelist[0].fileName) {
          this.fobj.img = this.filelist[0].fileName
        }
        savej({ params: this.fobj }).then(res => {
          this.updateUserInfo(this.fobj)
          uni.showToast({ icon: 'success', title: '修改成功' })
          setTimeout(() => {
            uni.navigateBack()
          }, 1500)
        })
      } else {
        uni.showToast({ icon: 'none', title: '请完善必要信息' })
      }
    }
  },
  computed: {
    ...mapState(['userInfo'])
  }
}
</script>

<style lang="scss" scoped>
/* 核心颜色定义 */
$primary-color: #ff943c;
$bg-color: #f7f8fa;
$text-dark: #333333;
$text-light: #999999;
$card-bg: #ffffff;

.page-container {
  background-color: $bg-color;
  min-height: 100vh;
}

.sv-container {
  height: calc(100vh - 88rpx);
}

.form-wrapper {
  padding: 30rpx;
}

/* 通用卡片样式 */
.card {
  background-color: $card-bg;
  border-radius: 24rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.02);
}

/* 头像区域样式 */
.avatar-section {
  .avatar-title {
    font-size: 28rpx;
    color: $text-dark;
    font-weight: bold;
    margin-bottom: 24rpx;
  }

  .avatar-content {
    display: flex;
    flex-direction: column;
    align-items: center;

    .upload-box {
      position: relative;
      width: 160rpx;
      height: 160rpx;

      ::v-deep .u-upload {
        justify-content: center;
      }

      .avatar-hint {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: #f2f3f5;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 80rpx;
        pointer-events: none;
      }
    }

    .wx-avatar-btn {
      margin-top: 30rpx;
      font-size: 24rpx;
      color: $primary-color;
      background-color: rgba(255, 148, 60, 0.12);
      border: none;
      border-radius: 40rpx;
      padding: 0 40rpx;
      height: 60rpx;
      line-height: 60rpx;

      .btn-icon {
        margin-right: 8rpx;
      }

      &::after {
        border: none;
      }
    }
  }
}

/* 表单列表样式 */
.info-section {
  .section-header {
    display: flex;
    align-items: center;
    margin-bottom: 30rpx;

    .header-icon {
      font-size: 32rpx;
      margin-right: 12rpx;
    }

    .header-text {
      font-size: 30rpx;
      font-weight: bold;
      color: $text-dark;
    }
  }
}

.form-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f2f3f5;
  position: relative;

  &.no-border {
    border-bottom: none;
  }

  .label {
    width: 160rpx;
    font-size: 28rpx;
    color: #666;
  }

  .custom-input {
    flex: 1;
    font-size: 28rpx;
    color: $text-dark;
    height: 60rpx;
  }

  .unit {
    font-size: 26rpx;
    color: $text-light;
    margin-left: 10rpx;
  }

  .custom-textarea {
    flex: 1;
    background-color: #f8f9fa;
    border-radius: 12rpx;
    padding: 20rpx;
    height: 160rpx;
    font-size: 26rpx;
    color: $text-dark;
  }
}

/* 性别选择器定制 */
.gender-group {
  display: flex;
  gap: 40rpx;

  .gender-label {
    display: flex;
    align-items: center;
    font-size: 28rpx;
    color: $text-dark;

    radio {
      transform: scale(0.85);
      margin-right: 8rpx;
    }
  }
}

/* 提交按钮区域 */
.submit-box {
  margin-top: 60rpx;
  display: flex;
  flex-direction: column;
  align-items: center;

  .primary-btn {
    width: 100%;
    height: 90rpx;
    background: linear-gradient(135deg, #ff943c, #ff7a1a);
    border-radius: 45rpx;
    border: none;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8rpx 20rpx rgba(255, 148, 60, 0.3);

    .btn-text {
      color: #ffffff;
      font-size: 32rpx;
      font-weight: bold;
    }

    &:active {
      transform: scale(0.98);
      opacity: 0.9;
    }
  }

  .safe-hint {
    margin-top: 24rpx;
    font-size: 22rpx;
    color: $text-light;
  }
}

.safe-area-bottom {
  height: 60rpx;
}

</style>