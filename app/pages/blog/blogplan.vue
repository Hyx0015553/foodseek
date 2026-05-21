<template>
  <view class="page-container">
    <u-navbar
      title="探店计划"
      :border="false"
      :placeholder="true"
      @leftClick="goBack"
      :autoBack="false"
      bgColor="transparent"
      titleStyle="font-weight: bold;"
    ></u-navbar>

    <scroll-view scroll-y :enable-flex="true" class="svcontainer">
      <view class="content-wrapper">
        <view v-if="shop.id" class="shop-card">
          <image v-if="shop.img" class="shop-cover" :src="fileUrl + shop.img" mode="aspectFill"></image>
          <view class="shop-body">
            <text class="shop-name">{{ shop.sname }}</text>
            <text class="shop-addr">{{ shop.address || '暂无地址' }}</text>
          </view>
        </view>

        <view class="intro-card">
          <view class="card-title">什么是探店计划</view>
          <text class="card-text">将本店加入你的探店清单，填写计划时间与备注，到店体验后发布动态，与更多食客分享真实感受。系统在计划到店日的前第 3 个自然日（每日北京时间约 22:35）会向您的「消息-系统-系统通知」推送一条提醒。</text>
        </view>

        <view class="steps-card">
          <view class="card-title">建议流程</view>
          <view class="step-row" v-for="(t, i) in steps" :key="i">
            <text class="step-num">{{ i + 1 }}</text>
            <text class="step-text">{{ t }}</text>
          </view>
        </view>

        <!-- 计划信息（可编辑，提交到 blogplan 表） -->
        <view class="form-card" v-if="shop.id">
          <view class="card-title">计划信息</view>
          <view class="form-row" @click="datepickshow = true">
            <text class="form-label">计划时间</text>
            <view class="form-field">
              <text :class="fobj.plantime ? 'form-val' : 'form-placeholder'">{{ fobj.plantime || '选择计划到店时间' }}</text>
              <u-icon name="arrow-right" color="#c0c4cc" size="14"></u-icon>
            </view>
          </view>
          <view class="form-row column">
            <text class="form-label">状态</text>
            <u-radio-group v-model="fobj.state" placement="row" class="state-group">
              <u-radio
                v-for="opt in stateOptions"
                :key="opt"
                :label="opt"
                :name="opt"
              ></u-radio>
            </u-radio-group>
            <text class="state-hint">状态由您自行选择；若选「已完成」，保存时将校验您在本店是否已有「已完成」订单</text>
          </view>
          <view class="form-row column">
            <text class="form-label">备注</text>
            <u--textarea
              v-model="fobj.note"
              placeholder="可填写同行人数、想点的菜、停车等备忘"
              border="surround"
              height="120"
              count
              maxlength="500"
            ></u--textarea>
          </view>
          <!-- 新增提示：已有计划时显示 -->
          <view class="exist-plan-tip" v-if="existingPlanList && existingPlanList.length > 0">
            <text class="tip-title">您已创建以下探店计划：</text>
            <view class="plan-list">
              <view class="plan-item" v-for="(plan, idx) in existingPlanList" :key="plan.id" @click="editPlan(plan)">
                <text class="plan-text">{{ idx + 1 }}. {{ plan.plantime || '未设置时间' }} - {{ plan.state }}</text>
                <text class="plan-edit">编辑</text>
              </view>
            </view>
          </view>
        </view>

        <u-datetime-picker
          :show="datepickshow"
          :value="datePickerValue"
          mode="datetime"
          closeOnClickOverlay
          @confirm="onPickTime"
          @cancel="datepickshow = false"
          @close="datepickshow = false"
        ></u-datetime-picker>

        <view class="join-wrap" v-if="shop.id">
          <button class="join-btn" type="default" @click="savePlan">{{ fobj.id ? '保存修改' : '保存探店计划' }}</button>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { findj, savej, listj, fileUrl } from '@/common/config/api.js'
import { mapState } from 'vuex'
import { yewuutil } from '@/common/commontools.js'

export default {
  data() {
    return {
      fileUrl,
      pid: null,
      planId: null,
      shop: {},
      datepickshow: false,
      datePickerValue: Number(new Date()),
      fobj: {
        id: null,
        uid: null,
        username: '',
        sid: null,
        stitle: '',
        plantime: '',
        note: '',
        state: '待探店'
      },
      existingPlanList: [],
      stateOptions: ['待探店', '已取消', '已完成'],
      steps: ['到店前可电话或线上确认营业时间与排队情况', '到店后拍照、记录菜品与环境', '完成后在「发布探店动态」中关联本店发布'],
      _origPlantime: ''
    }
  },
  computed: {
    ...mapState(['userInfo'])
  },
  onLoad(params) {
    this.pid = params.pid ? parseInt(params.pid, 10) : null
    this.planId = params.id ? parseInt(params.id, 10) : null
    if (!this.pid && !this.planId) {
      uni.showToast({ title: '参数错误', icon: 'none' })
      return
    }
    this.initPage()
  },
  methods: {
    goBack() {
      uni.navigateBack()
    },
    async initPage() {
      if (this.planId) {
        await this.loadPlanById(this.planId)
        if (this.fobj.sid) {
          this.pid = this.fobj.sid
        }
      }
      if (!this.pid) {
        uni.showToast({ title: '缺少店铺信息', icon: 'none' })
        return
      }
      await this.loadShop(this.pid)
      if (!this.planId) {
        await this.tryLoadPlanByUserShop()
      }
      this.syncShopToForm()
    },
    loadShop(sid) {
      return findj({ params: { table: 'shop', id: sid } })
        .then(res => {
          if (res && res.id) this.shop = res
        })
        .catch(() => {})
    },
    loadPlanById(id) {
      return findj({ params: { table: 'blogplan', id } })
        .then(res => {
          if (res && res.id) {
            this.fobj = {
              id: res.id,
              uid: res.uid,
              username: res.username || '',
              sid: res.sid,
              stitle: res.stitle || '',
              plantime: res.plantime || '',
              note: res.note || '',
              state: res.state || '待探店'
            }
            this._origPlantime = String(res.plantime || '').trim()
          }
        })
        .catch(() => {})
    },
    tryLoadPlanByUserShop() {
      if (!this.userInfo || !this.userInfo.id || !this.pid) return Promise.resolve()
      return listj({
        params: { table: 'blogplan', uid: this.userInfo.id, sid: this.pid }
      })
        .then(rows => {
          const list = rows || []
          this.existingPlanList = list
        })
        .catch(() => {})
    },
    syncShopToForm() {
      if (this.shop && this.shop.id) {
        this.fobj.sid = this.shop.id
        if (!this.fobj.stitle) this.fobj.stitle = this.shop.sname || ''
      }
    },
    editPlan(plan) {
      this.fobj = {
        id: plan.id,
        uid: plan.uid,
        username: plan.username || '',
        sid: plan.sid,
        stitle: plan.stitle || '',
        plantime: plan.plantime || '',
        note: plan.note || '',
        state: plan.state || '待探店'
      }
    },
    addNewPlan() {
      this.fobj = {
        id: null,
        uid: null,
        username: '',
        sid: this.shop.id,
        stitle: this.shop.sname || '',
        plantime: '',
        note: '',
        state: '待探店'
      }
    },
    onPickTime(e) {
      this.datepickshow = false
      if (e && e.value != null) {
        this.fobj.plantime = uni.$u.timeFormat(e.value, 'yyyy-mm-dd hh:MM:ss')
        this.datePickerValue = e.value
      }
    },
    async savePlan() {
      if (!this.userInfo || !this.userInfo.id) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        return
      }
      if (!this.shop || !this.shop.id) {
        uni.showToast({ title: '店铺信息缺失', icon: 'none' })
        return
      }
      const state = this.fobj.state || '待探店'
      if (state === '已完成') {
        uni.showLoading({ title: '校验中' })
        let ok = false
        try {
          ok = !!(await yewuutil.hasCompletedOrderForShop(this.userInfo.id, this.shop.id))
        } catch (e) {
          ok = false
        }
        uni.hideLoading()
        if (!ok) {
          uni.showToast({
            title: '请在本店有「已完成」订单后再将计划设为已完成',
            icon: 'none',
            duration: 2800
          })
          return
        }
      }
      const payload = {
        table: 'blogplan',
        id: this.fobj.id,
        uid: this.userInfo.id,
        username: this.userInfo.username || '',
        sid: this.shop.id,
        stitle: this.shop.sname || '',
        plantime: this.fobj.plantime || '',
        note: this.fobj.note || '',
        state
      }
      const pt = String(this.fobj.plantime || '').trim()
      if (this.fobj.id && pt !== String(this._origPlantime || '').trim()) {
        payload.remind_3d_sent = 0
      }
      uni.showLoading({ title: '保存中' })
      savej({ params: payload })
        .then(res => {
          uni.hideLoading()
          if (res && !String(res).includes('失败')) {
            if (!this.fobj.id && res) {
              this.fobj.id = parseInt(res, 10) || this.fobj.id
            }
            this.fobj.state = state
            uni.showToast({ title: '已保存', icon: 'success' })
          } else {
            uni.showToast({ title: String(res || '保存失败'), icon: 'none' })
          }
        })
        .catch(() => {
          uni.hideLoading()
          uni.showToast({ title: '保存失败', icon: 'none' })
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.page-container {
  min-height: 100vh;
  background: #f7f8fa;
}

.svcontainer {
  height: calc(100vh - 88rpx);
}

.content-wrapper {
  padding: 24rpx 30rpx 48rpx;
}

.shop-card {
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.shop-cover {
  width: 100%;
  height: 280rpx;
}

.shop-body {
  padding: 24rpx 28rpx 28rpx;
}

.shop-name {
  font-size: 34rpx;
  font-weight: bold;
  color: #1a1a1a;
}

.shop-addr {
  display: block;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #909399;
}

.intro-card,
.steps-card,
.form-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.card-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
  margin-bottom: 16rpx;
}

.card-text {
  font-size: 28rpx;
  color: #606266;
  line-height: 1.6;
}

.step-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 20rpx;
}

.step-row:last-child {
  margin-bottom: 0;
}

.step-num {
  flex-shrink: 0;
  width: 40rpx;
  height: 40rpx;
  line-height: 40rpx;
  text-align: center;
  font-size: 24rpx;
  font-weight: bold;
  color: #fff;
  background: linear-gradient(135deg, #ff943c, #ffb366);
  border-radius: 50%;
  margin-right: 16rpx;
}

.step-text {
  flex: 1;
  font-size: 28rpx;
  color: #606266;
  line-height: 1.5;
  padding-top: 4rpx;
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f2f3f5;
}

.form-row.column {
  flex-direction: column;
  align-items: stretch;
}

.form-label {
  font-size: 28rpx;
  color: #606266;
  margin-bottom: 12rpx;
}

.form-row:not(.column) .form-label {
  margin-bottom: 0;
  flex-shrink: 0;
  margin-right: 24rpx;
}

.form-field {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8rpx;
}

.form-val {
  font-size: 28rpx;
  color: #303133;
}

.form-placeholder {
  font-size: 28rpx;
  color: #c0c4cc;
}

.state-group {
  margin-top: 8rpx;
}

.state-hint {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #909399;
  line-height: 1.4;
}

.join-wrap {
  margin-top: 16rpx;
}

.join-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: bold;
  color: #fff;
  background: linear-gradient(135deg, #ff943c, #ffb366);
  border: none;
}

.join-btn::after {
  border: none;
}

.add-new-btn {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
  color: #ff943c;
  background: #fff;
  border: 2rpx solid #ff943c;
  margin-top: 20rpx;
}

.add-new-btn::after {
  border: none;
}

.exist-plan-tip {
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx dashed #e8e8e8;

  .tip-title {
    display: block;
    font-size: 26rpx;
    color: #909399;
    margin-bottom: 16rpx;
  }

  .plan-list {
    .plan-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16rpx 0;
      border-bottom: 1rpx solid #f2f3f5;

      &:last-child {
        border-bottom: none;
      }

      .plan-text {
        font-size: 26rpx;
        color: #606266;
        flex: 1;
      }

      .plan-edit {
        font-size: 26rpx;
        color: #ff943c;
        padding: 8rpx 16rpx;
        background: #fff5eb;
        border-radius: 8rpx;
      }
    }
  }
}
</style>
