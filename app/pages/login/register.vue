<template>
  <view class="page-container">
    <!-- 沉浸式导航栏 -->
    <u-navbar
        title="创建新账号"
        @leftClick="goBack"
        :border="false"
        :placeholder="true"
        :autoBack="false"
        bgColor="#ffffff"
        titleStyle="font-weight: bold; color: #1a1a1a;"
    ></u-navbar>

    <scroll-view scroll-y class="sv-container">
      <view class="form-card">
        <view class="welcome-section">
          <text class="main-title">欢迎加入 👋</text>
          <text class="sub-title">请填写以下信息完成注册</text>
        </view>

        <u--form labelPosition="top" :model="fobj" :rules="rules" ref="regform" labelWidth="100%">
          <!-- 头像上传区 -->
          <view class="avatar-upload-box">
            <view class="avatar-label">设置头像</view>
            <view class="upload-wrapper">
              <ideaupload v-model="filelist"></ideaupload>
              <view v-if="filelist.length === 0" class="upload-placeholder">📸</view>
            </view>
          </view>

          <!-- 表单输入组 -->
          <view class="input-group">
            <u-form-item label="用户名" prop="username" borderBottom>
              <u--input
                  v-model="fobj.username"
                  placeholder="设置您的唯一昵称"
                  border="none"
                  prefixIcon="account"
                  prefixIconStyle="color: #909399; font-size: 36rpx;"
              />
            </u-form-item>

            <u-form-item label="电话号码" prop="tel" borderBottom>
              <u--input
                  v-model="fobj.tel"
                  type="number"
                  placeholder="请输入常用手机号"
                  border="none"
                  prefixIcon="phone"
                  prefixIconStyle="color: #909399; font-size: 36rpx;"
              />
            </u-form-item>

            <u-form-item label="登录密码" prop="passwd" borderBottom>
              <u--input
                  v-model="fobj.passwd"
                  type="password"
                  placeholder="请输入登录密码"
                  border="none"
                  prefixIcon="lock"
                  prefixIconStyle="color: #909399; font-size: 36rpx;"
              />
            </u-form-item>

            <u-form-item label="确认密码" prop="passwd2" borderBottom>
              <u--input
                  v-model="fobj.passwd2"
                  type="password"
                  placeholder="请再次输入密码"
                  border="none"
                  prefixIcon="lock-fill"
                  prefixIconStyle="color: #909399; font-size: 36rpx;"
              />
            </u-form-item>

            <u-form-item label="性别" prop="sex" @click="sexshow = true; hideKeyboard();" borderBottom>
              <u--input
                  v-model="fobj.sex"
                  disabled
                  disabledColor="transparent"
                  placeholder="请选择性别"
                  border="none"
                  prefixIcon="man"
                  prefixIconStyle="color: #909399; font-size: 36rpx;"
              />
              <u-icon slot="right" name="arrow-right" color="#c0c4cc"></u-icon>
            </u-form-item>
          </view>
        </u--form>

        <!-- 提交操作 -->
        <view class="submit-section">
          <view class="btn-wrapper">
            <u-button
                type="primary"
                text="立即注册"
                @click="saveUser"
                customStyle="height: 100rpx; border-radius: 50rpx; font-weight: bold; font-size: 32rpx; color: #ffffff; background: linear-gradient(to right, #ff943c, #ff7a1a); border: none; box-shadow: 0 10rpx 20rpx rgba(255, 148, 60, 0.3);"
            />
          </view>
          <view class="login-tip" @click="goBack">
            已有账号？<text class="link">去登录</text>
          </view>
        </view>
      </view>

      <!-- 性别选择器 -->
      <u-action-sheet
          :show="sexshow"
          :actions="[{name:'男', id: 1}, {name:'女', id:2}]"
          title="选择您的性别"
          @close="sexshow = false"
          @select="sexSelect"
          round="20"
      />

      <view class="safe-area-inset-bottom"></view>
    </scroll-view>
  </view>
</template>

<script>
import { listj, savej, findj, fileUrl, uploadUrl } from '@/common/config/api.js';
import { ideautil, yewuutil } from '@/common/commontools.js'
import { mapState, mapActions } from 'vuex'

export default {
  data() {
    return {
      filelist: [],
      fobj: {
        username: '',
        tel: '',
        passwd: '',
        passwd2: '',
        sex: '女',
        roletype: 2
      },
      uploadUrl: uploadUrl,
      sexshow: false,
      duser: false,
      rules: {
        username: [{
          required: true,
          message: "请输入用户名",
          trigger: ['blur'],
        }, {
          asyncValidator: (rule, value, callback) => {
            if (value) {
              findj({ params: { table: 'user', username: value } }).then(res => {
                if (res) {
                  this.duser = true;
                  callback(new Error('该用户名已被占用'));
                } else {
                  this.duser = false;
                  callback();
                }
              }).catch(() => callback(new Error('系统校验失败')));
            } else {
              callback();
            }
          },
          trigger: ['blur'],
        }],
        passwd: [{
          required: true,
          message: "请输入密码",
          trigger: ['blur'],
        }],
        passwd2: [{
          validator: (rule, value, callback) => {
            return this.fobj.passwd === value;
          },
          message: '两次输入的密码不一致',
          trigger: ['change', 'blur'],
        }],
        tel: [{
          validator: (rule, value, callback) => {
            return uni.$u.test.mobile(value);
          },
          message: '手机号码格式不正确',
          trigger: ['change', 'blur'],
        }]
      }
    }
  },
  onReady() {
    this.$refs.regform.setRules(this.rules);
  },
  methods: {
    ...mapActions(['updateUserInfo']),
    hideKeyboard: ideautil.hideKeyboard,
    saveUser() {
      this.$refs.regform.validate().then(res => {
        if (this.duser) {
          uni.showToast({ icon: 'none', title: '用户名已存在!' });
          return;
        }
        // 提取上传成功的地址
        this.fobj.img = this.filelist.length > 0 ? (this.filelist[0].url.data || this.filelist[0].url) : "";

        let fdata = { ...this.fobj, table: "user" };
        uni.showLoading({ title: '注册中...' });
        savej({ params: fdata }).then(res => {
          uni.hideLoading();
          uni.showToast({ title: '注册成功' });
          setTimeout(() => { this.goBack(); }, 1000);
        }).catch(() => uni.hideLoading());
      }).catch(() => {
        uni.$u.toast('请完善注册信息');
      });
    },
    goBack() {
      yewuutil.toLogin();
    },
    sexSelect(e) {
      this.fobj.sex = e.name;
    }
  }
}
</script>

<style lang="scss" scoped>
/* 颜色与设计变量 */
$primary-color: #3c9cff;
$bg-color: #f8f9fa;
$text-dark: #1a1a1a;
$text-sub: #909399;

.page-container {
  background-color: $bg-color;
  min-height: 100vh;
}

.sv-container {
  height: calc(100vh - 44px);
}

.form-card {
  margin: 24rpx;
  padding: 40rpx;
  background-color: #ffffff;
  border-radius: 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.03);
}

.welcome-section {
  margin-bottom: 50rpx;
  .main-title {
    font-size: 48rpx;
    font-weight: 800;
    color: $text-dark;
    display: block;
  }
  .sub-title {
    font-size: 26rpx;
    color: $text-sub;
    margin-top: 12rpx;
    display: block;
  }
}

/* 头像上传美化 */
.avatar-upload-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 40rpx;

  .avatar-label {
    font-size: 28rpx;
    color: $text-dark;
    font-weight: bold;
    margin-bottom: 20rpx;
    width: 100%;
  }

  .upload-wrapper {
    position: relative;
    width: 160rpx;
    height: 160rpx;
    background-color: #f0f2f5;
    border-radius: 80rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    border: 4rpx dashed #dcdfe6;

    .upload-placeholder {
      font-size: 60rpx;
      pointer-events: none;
    }

    ::v-deep .u-upload {
      margin: 0;
      justify-content: center;
    }
  }
}

.input-group {
  ::v-deep .u-form-item__body__left__content__label {
    font-weight: bold;
    color: #333;
    margin-bottom: 10rpx;
  }
}

.submit-section {
  margin-top: 60rpx;

  .btn-wrapper {
    &:active {
      transform: scale(0.98);
      opacity: 0.9;
    }
    transition: all 0.2s;
  }

  .login-tip {
    text-align: center;
    margin-top: 30rpx;
    font-size: 26rpx;
    color: $text-sub;

    .link {
      color: $primary-color;
      font-weight: bold;
      margin-left: 8rpx;
    }
  }
}

.safe-area-inset-bottom {
  height: env(safe-area-inset-bottom);
  margin-top: 30rpx;
}
</style>