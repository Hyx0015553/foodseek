<template>
  <view class="login-container">
    <!-- 顶部背景装饰 -->
    <view class="header-decoration">
      <view class="circle-top"></view>
      <view class="title-section">
        <text class="main-title">欢迎回来</text>
        <text class="sub-title">请登录您的账号以继续</text>
      </view>
    </view>

    <view class="login-card">
      <!-- 隐藏的调试/返回入口，保持原逻辑 -->
      <navigator class="debug-portal" url="/pages/login/rootip" open-type="redirect"></navigator>

      <view class="form-wrapper">
        <!-- 用户名输入 -->
        <view class="input-group">
          <view class="icon-box">👤</view>
          <input
              type="text"
              class="input-field"
              placeholder="请输入用户名"
              v-model="username"
              placeholder-class="placeholder-style"
          />
        </view>

        <!-- 密码输入 -->
        <view class="input-group">
          <view class="icon-box">🔒</view>
          <input
              type="password"
              class="input-field"
              :password="true"
              placeholder="请输入密码"
              v-model="passwd"
              placeholder-class="placeholder-style"
          />
        </view>

        <!-- 图形验证码 -->
        <view class="captcha-row">
          <view class="input-group captcha-input-wrap">
            <view class="icon-box">🔤</view>
            <input
              type="text"
              class="input-field"
              placeholder="请输入验证码"
              v-model="captchaCode"
              maxlength="6"
              placeholder-class="placeholder-style"
            />
          </view>
          <view class="captcha-img-wrap" @tap="refreshCaptcha">
            <image v-if="captchaSrc" class="captcha-img" :src="captchaSrc" mode="aspectFit" />
            <text v-else class="captcha-placeholder">点击刷新</text>
          </view>
        </view>

        <!-- 登录按钮 -->
        <button class="login-btn" @tap="handLogin">登 录</button>

        <!-- 注册链接 -->
        <view class="reg-link-box">
          <text class="reg-text" @tap="toReg">还没有账号？<text class="highlight">立即注册</text></text>
        </view>
      </view>

      <!-- #ifdef MP-WEIXIN -->
      <view class="social-section">
        <view class="divider">
          <view class="line"></view>
          <text class="divider-text">其他登录方式</text>
          <view class="line"></view>
        </view>

        <view class="social-icons">
          <view class="icon-item" @tap="getUserInfos">
            <image src="https://ideapic-1255600738.cos.ap-guangzhou.myqcloud.com/images/wx2.png" mode="aspectFit"></image>
            <text>微信</text>
          </view>
        </view>
      </view>
      <!-- #endif -->
    </view>

    <!-- 底部备案/版权信息 -->
    <view class="footer-copyright">
      <text></text>
    </view>
  </view>
</template>

<script>
import { savej, findj, serverUrl, captchaj, userLoginj } from '@/common/config/api.js'
import { mapState, mapActions } from 'vuex'

export default {
  data() {
    return {
      username: '',
      passwd: '',
      captchaId: '',
      captchaCode: '',
      captchaSrc: '',
      serverUrl: serverUrl,
      open_id: '',
      session_key: ''
    };
  },
  onLoad() {
    const u = this.userInfo
    this.username = (u && u.username != null) ? String(u.username) : ''
    this.passwd = (u && u.passwd != null) ? String(u.passwd) : ''
    // 账号密码登录需填写图形验证码，不再自动提交；微信 open_id 流程仍可在授权后自动走 handLogin
  },
  onShow() {
    if (!this.open_id) {
      this.refreshCaptcha()
    }
  },
  methods: {
    ...mapActions(['updateUserInfo', 'logout']),
    toReg() {
      uni.redirectTo({ url: './register' })
    },
    getUserInfos() {
      uni.showLoading({ title: '微信登录中...' });
      uni.getUserProfile({
        provider: 'weixin',
        desc: '获取用户信息以完善资料',
        lang: 'zh_CN',
        success: (res) => {
          this.wechatLogin()
        },
        fail: () => {
          uni.hideLoading();
          uni.showToast({ title: '已取消授权', icon: 'none' });
        }
      })
    },
    wechatLogin() {
      uni.login({
        provider: 'weixin',
        success: (loginRes) => {
          uni.request({
            url: this.serverUrl + "wxlogin",
            data: { code: loginRes.code },
            success: (res) => {
              if (res.statusCode == 200) {
                this.open_id = res.data.openid;
                this.session_key = res.data.session_key;
                this.handLogin();
              }
            }
          })
        }
      });
    },
    refreshCaptcha() {
      captchaj().then((res) => {
        if (res && res.captchaId && res.imageBase64) {
          this.captchaId = res.captchaId
          this.captchaSrc = 'data:image/png;base64,' + res.imageBase64
          this.captchaCode = ''
        }
      }).catch(() => {
        this.captchaSrc = ''
        uni.showToast({ title: '验证码加载失败', icon: 'none' })
      })
    },
    handLogin() {
      if (this.open_id) {
        uni.showLoading({ title: '静默登录中...' });
        findj({ params: { table: 'user', openid: this.open_id } }).then(res => {
          if (res) {
            this.loginSuccess(res);
          } else {
            savej({ params: { table: "user", openid: this.open_id, roletype: "2" } }).then(newId => {
              if (newId) this.loginSuccess({ id: newId, openid: this.open_id });
            });
          }
        }).catch(() => uni.hideLoading());
      } else {
        if (!this.username || !this.passwd) {
          uni.showToast({ icon: 'none', title: '请输入完整信息' })
          return
        }
        if (!this.captchaId || !(this.captchaCode || '').trim()) {
          uni.showToast({ icon: 'none', title: '请输入验证码' })
          return
        }
        uni.showLoading({ title: '验证中...' });
        userLoginj({
          params: {
            table: 'user',
            username: this.username,
            passwd: this.passwd,
            captchaId: this.captchaId,
            captchaCode: (this.captchaCode || '').trim()
          }
        }).then(res => {
          uni.hideLoading();
          if (!res) {
            uni.showToast({ icon: 'none', title: '登录失败' })
            this.refreshCaptcha()
            return
          }
          if (res.errcode === 0 && res.user) {
            this.loginSuccess(res.user)
          } else {
            uni.showToast({ icon: 'none', title: res.errmsg || '登录失败' })
            this.refreshCaptcha()
          }
        }).catch(() => {
          uni.hideLoading();
          uni.showToast({ icon: 'none', title: '网络异常' })
          this.refreshCaptcha()
        });
      }
    },
    loginSuccess(userobj) {
      uni.hideLoading();
      this.updateUserInfo(userobj);
      uni.showToast({ title: '欢迎回来', icon: 'success' });
      // 商家用户跳转到商家首页，普通用户跳转到首页
      setTimeout(() => {
        if (userobj.roletype == '3') {
          uni.reLaunch({ url: '/pages/merchant/index' });
        } else {
          uni.ytool.toIndex();
        }
      }, 800);
    }
  },
  computed: {
    ...mapState(['userInfo'])
  }
};
</script>

<style lang="scss" scoped>
/* 全局背景设置 */
.login-container {
  min-height: 100vh;
  background-color: #fff5eb;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 顶部背景装饰 */
.header-decoration {
  width: 100%;
  height: 45vh;
  background: linear-gradient(135deg, #ff943c 0%, #ff7a1a 100%);
  border-bottom-left-radius: 80rpx;
  border-bottom-right-radius: 80rpx;
  position: relative;
  padding-top: 150rpx;
  box-sizing: border-box;

  .circle-top {
    position: absolute;
    top: -50rpx;
    right: -50rpx;
    width: 300rpx;
    height: 300rpx;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 50%;
  }

  .title-section {
    padding: 0 60rpx;
    color: #ffffff;

    .main-title {
      font-size: 64rpx;
      font-weight: bold;
      display: block;
      letter-spacing: 4rpx;
    }

    .sub-title {
      font-size: 28rpx;
      opacity: 0.8;
      margin-top: 20rpx;
      display: block;
    }
  }
}

/* 登录卡片 */
.login-card {
  width: 88%;
  background-color: #ffffff;
  border-radius: 40rpx;
  margin-top: -120rpx;
  padding: 60rpx 40rpx;
  box-shadow: 0 20rpx 40rpx rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 10;

  .debug-portal {
    position: absolute;
    top: 0;
    left: 0;
    width: 80rpx;
    height: 80rpx;
    z-index: 99;
  }
}

/* 输入表单 */
.form-wrapper {
  .input-group {
    display: flex;
    align-items: center;
    background-color: #f5f7fa;
    border-radius: 30rpx;
    margin-bottom: 30rpx;
    padding: 0 30rpx;
    height: 100rpx;
    transition: all 0.3s;
    border: 1px solid transparent;

    &:focus-within {
      border-color: #ff943c;
      background-color: #ffffff;
      box-shadow: 0 0 10rpx rgba(255, 148, 60, 0.2);
    }

    .icon-box {
      font-size: 36rpx;
      margin-right: 20rpx;
    }

    .input-field {
      flex: 1;
      font-size: 28rpx;
      color: #333;
    }

    .placeholder-style {
      color: #b0b8c1;
    }
  }

  .captcha-row {
    display: flex;
    align-items: stretch;
    gap: 20rpx;
    margin-bottom: 30rpx;
  }

  .captcha-input-wrap {
    flex: 1;
    margin-bottom: 0;
  }

  .captcha-img-wrap {
    width: 220rpx;
    height: 100rpx;
    border-radius: 24rpx;
    background: #eef1f6;
    overflow: hidden;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #e4e7ed;
  }

  .captcha-img {
    width: 100%;
    height: 100%;
  }

  .captcha-placeholder {
    font-size: 22rpx;
    color: #909399;
  }

  .login-btn {
    width: 100%;
    height: 100rpx;
    line-height: 100rpx;
    background: linear-gradient(to right, #ff943c, #ff7a1a);
    color: #ffffff;
    border-radius: 50rpx;
    font-size: 32rpx;
    font-weight: bold;
    margin-top: 50rpx;
    border: none;
    box-shadow: 0 10rpx 20rpx rgba(255, 148, 60, 0.3);

    &:active {
      transform: scale(0.98);
      opacity: 0.9;
    }
  }

  .reg-link-box {
    margin-top: 40rpx;
    text-align: center;

    .reg-text {
      font-size: 26rpx;
      color: #909399;

      .highlight {
        color: #ff943c;
        font-weight: bold;
        margin-left: 10rpx;
      }
    }
  }
}

/* 第三方登录区 */
.social-section {
  margin-top: 80rpx;

  .divider {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 40rpx;

    .line {
      flex: 1;
      height: 1rpx;
      background-color: #ebeef5;
    }

    .divider-text {
      font-size: 24rpx;
      color: #c0c4cc;
      padding: 0 30rpx;
    }
  }

  .social-icons {
    display: flex;
    justify-content: center;
    padding: 0 20rpx;

    .icon-item {
      display: flex;
      flex-direction: column;
      align-items: center;

      image {
        width: 72rpx;
        height: 72rpx;
        margin-bottom: 12rpx;
        transition: transform 0.2s;
      }

      text {
        font-size: 22rpx;
        color: #909399;
      }

      &:active image {
        transform: scale(1.1);
      }
    }
  }
}

.footer-copyright {
  margin-top: auto;
  padding: 40rpx 0;

  text {
    font-size: 22rpx;
    color: #b0b8c1;
  }
}
</style>