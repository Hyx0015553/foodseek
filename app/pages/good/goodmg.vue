<template>
  <view class="page-container">
    <u-navbar
      title="菜品管理"
      :border="false"
      :placeholder="true"
      :autoBack="true"
      bgColor="#ffffff"
      titleStyle="font-weight: bold; color: #1a1a1a;"
    ></u-navbar>

    <scroll-view scroll-y :enable-flex="true" class="main-scroll">
      <!-- 顶部菜品预览卡片 -->
      <view class="info-card">
        <view class="info-header">
          <view class="cover-wrapper">
            <image
              v-if="filelist.length"
              :src="filelist[0].url"
              class="cover-image"
              mode="aspectFill"
            ></image>
            <view v-else class="cover-placeholder">图</view>
          </view>
          <view class="info-texts">
            <text class="good-name">{{ fobj.gname || '请填写菜品名称' }}</text>
            <text class="good-sub">
              {{ fobj.type || '请选择分类' }} ·
              {{ fobj.price ? (fobj.price + ' 元') : '未设置价格' }}
            </text>
          </view>
        </view>
      </view>

      <!-- 基础信息表单卡片 -->
      <view class="form-card">
        <view class="section-title">
          <view class="title-line"></view>
          <text>基础信息</text>
        </view>

        <view class="form-item">
          <text class="form-label">菜品名称</text>
          <input
            type="text"
            v-model="fobj.gname"
            class="form-input"
            placeholder="请输入菜品名称"
          />
        </view>

        <view class="form-item" @click="sshow1 = true; hideKeyboard();">
          <text class="form-label">分类</text>
          <view class="select-input">
            <text class="select-text">
              {{ fobj.type || '请选择分类' }}
            </text>
            <u-icon name="arrow-right" size="16" color="#c0c4cc"></u-icon>
          </view>
          <u-action-sheet
            :show="sshow1"
            :actions="typelist"
            title="请选择分类"
            description="请选择"
            @close="sshow1 = false"
            @select="tSelected1"
          />
        </view>

        <view class="form-item">
          <text class="form-label">状态</text>
          <ideaslc
            label=""
            showName="title"
            :datalist="[['上架中','已下架']]"
            v-model="fobj.statecn"
          ></ideaslc>
        </view>

        <view class="form-item">
          <text class="form-label">价格（元）</text>
          <input
            type="number"
            v-model="fobj.price"
            class="form-input"
            placeholder="请输入价格"
          />
        </view>

        <view class="form-item">
          <text class="form-label">库存</text>
          <input
            type="number"
            v-model="fobj.mcount"
            class="form-input"
            placeholder="请输入库存数量"
          />
        </view>

        <view class="form-item form-item-logo">
          <text class="form-label">封面图</text>
          <ideaupload v-model="filelist"></ideaupload>
          <text class="logo-tip">建议上传清晰的菜品图片，展现菜品卖相</text>
        </view>
      </view>

      <!-- 详细信息编辑 -->
      <view class="form-card">
        <view class="section-title">
          <view class="title-line"></view>
          <text>菜品介绍</text>
        </view>
        <view class="editor-wrapper">
          <jinEdit
            :html="editorHtml"
            @getEditorContxt="getEditorContxt"
            v-model="fobj.note"
            height="300px"
            :uploadFileUrl="uploadUrl"
            placeholder="请输入菜品详情，如口味特点、推荐搭配等"
            @editOk="editOk"
          ></jinEdit>
        </view>
      </view>

      <view class="safe-area-bottom"></view>
    </scroll-view>

    <!-- 底部操作按钮 -->
    <view class="fixed-footer">
      <view class="footer-btn-group">
        <view class="sub-action" v-if="tid" @click="delGood">
          删除菜品
        </view>
        <view class="main-action" @click="saveGood">
          保存菜品信息
        </view>
      </view>
    </view>
  </view>
</template>

<script>
	import {
		listj,
		findj,
		savej,
		deletej,
		fileUrl,
		uploadUrl
	} from '@/common/config/api.js'
	import {
		mapState,
		mapActions
	} from 'vuex';
	import jinEdit from '@/components/jin-edit/jin-edit.vue'
	import {
		ideautil,
		yewuutil
	} from '@/common/commontools.js'
	import { GOOD_STATE, goodStateCode } from '@/common/goodState.js'
	export default {
		components: {
			jinEdit
		},
		data() {
			return {
				tel: '',
				typelist: [],
				filelist: [],
				fobj: {
					btype:1,
					gname: '',
					price: 0,
					note: '',
					img: '',
					sid: '',
					shop: '',
					typeid: '',
					type: '',
					state: GOOD_STATE.ON_SHELF,
					statecn:'上架中',
					table: 'good'
				},
				tid: null,
				fileUrl: fileUrl,
				uploadUrl: uploadUrl,
				editorCtx:null,
				editorHtml:'',
				sshow1:false
			};
		},
		onLoad(params) {
			if (params.tid && params.tid !== 'undefined') {
				this.tid = params.tid

			}
			console.log('tttttttid:' + params.tid + " showdel:" + this.showdel)
			if (this.tid) {
				findj({
					params: {
						table: 'good',
						id: this.tid
					}
				}).then(res => {
					this.fobj = res
					this.filelist.push({url: this.fileUrl + res.img})
					console.log(this.imgfile)
					this.editorHtml = this.fobj.note
					this.editorCtx.setContents({
						html: uni.itool.getHtmlNote(this.fobj.note)
					})
					
				}).catch(err => {

				})
			}
			
			listj({
				params: {
					table: 'type'
				}
			}).then(res => {
				for (let s of res) {
					s.name = s.title
				}
				this.typelist = res
			}).catch(err => {
			
			})
		},
		methods: {
			//...mapActions(['']),
			hideKeyboard: ideautil.hideKeyboard,
			saveGood() {
				this.editorCtx.getContents({
					success: res => {
						this.fobj.note = res.html
						this.fobj.sid = this.userInfo.sid
						this.fobj.shop = this.userInfo.username
						console.log(this.filelist)
						//this.fobj.img = this.filelist[0].url.data
						//this.filelist.length?this.fobj.img = this.filelist[0].url.data:this.fobj.img=""
						if (this.filelist.length && this.filelist[0].url.data) {
							this.fobj.img = this.filelist[0].url.data
						}
						let fdata = this.fobj
						fdata.table = "good"
						fdata.state = goodStateCode(fdata.statecn) != null ? goodStateCode(fdata.statecn) : (fdata.state || GOOD_STATE.ON_SHELF)
						savej({
							params: fdata
						}).then(res => {
							uni.redirectTo({
								url: './good'
							})
						}).catch(err => {
						
						})
					} 
				})
				
			},
			comboboxselect(e){
				this.fobj.typeid = e.id
				this.fobj.type = e.title
			},
			delGood() {
				deletej({
					params: {
						table: 'good',
						id: this.tid
					}
				}).then(res => {
					uni.redirectTo({
						url: './good'
					})
				}).catch(err => {

				})
			},
			goBack() {
				uni.navigateBack({
					delta: 1
				})
			},
			
			editOk(res) {
				this.fobj.note = res.html
			},
			getEditorContxt(ctx){
				this.editorCtx = ctx
				this.editorCtx.setContents({
					html: this.fobj.note
				})
			},
			tSelected1(e){
				this.fobj.type = e.name
				this.fobj.typeid = e.id
			}
		},
		computed: {
			...mapState(['userInfo'])
		}
	}
</script>

<style lang="scss" scoped>
$primary-color: #ff943c;
$bg-color: #f8f9fb;
$card-shadow: 0 8rpx 30rpx rgba(0, 0, 0, 0.05);

.page-container {
  background-color: $bg-color;
  min-height: 100vh;
}

.main-scroll {
  height: calc(100vh - 140rpx);
}

.info-card {
  margin: 20rpx 30rpx 10rpx;
  background-color: #ffffff;
  border-radius: 32rpx;
  padding: 32rpx;
  box-shadow: $card-shadow;

  .info-header {
    display: flex;
    align-items: center;
  }

  .cover-wrapper {
    width: 120rpx;
    height: 120rpx;
    border-radius: 24rpx;
    background: #fff7e6;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 24rpx;

    .cover-image {
      width: 100%;
      height: 100%;
      border-radius: 24rpx;
    }

    .cover-placeholder {
      font-size: 40rpx;
      font-weight: bold;
      color: $primary-color;
    }
  }

  .info-texts {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;

    .good-name {
      font-size: 34rpx;
      font-weight: 700;
      color: #1a1a1a;
      margin-bottom: 8rpx;
    }

    .good-sub {
      font-size: 24rpx;
      color: #909399;
    }
  }
}

.form-card {
  margin: 10rpx 30rpx 20rpx;
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 28rpx 30rpx 10rpx;
  box-shadow: $card-shadow;
}

.section-title {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;

  .title-line {
    width: 8rpx;
    height: 32rpx;
    background-color: $primary-color;
    border-radius: 4rpx;
    margin-right: 16rpx;
  }

.section-title text {
    font-size: 30rpx;
    font-weight: bold;
    color: #1a1a1a;
  }
}

.form-item {
  margin-bottom: 26rpx;

  .form-label {
    display: block;
    font-size: 26rpx;
    color: #606266;
    margin-bottom: 10rpx;
  }

  .form-input {
    width: 100%;
    height: 80rpx;
    padding: 0 24rpx;
    border-radius: 999rpx;
    background-color: #f7f8fa;
    font-size: 26rpx;
    color: #303133;
    box-sizing: border-box;
  }
}

.select-input {
  width: 100%;
  height: 80rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background-color: #f7f8fa;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;

  .select-text {
    font-size: 26rpx;
    color: #303133;
  }
}

.form-item-logo {
  .logo-tip {
    margin-top: 8rpx;
    font-size: 22rpx;
    color: #c0c4cc;
  }
}

.editor-wrapper {
  border-radius: 20rpx;
  overflow: hidden;
  border: 1rpx solid #ebeef5;
}

.fixed-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 120rpx;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  padding: 0 30rpx;
  padding-bottom: env(safe-area-inset-bottom);
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);
  z-index: 100;

  .footer-btn-group {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20rpx;
  }

  .sub-action {
    width: 220rpx;
    height: 84rpx;
    background-color: #ff4d4f;
    color: #fff;
    border-radius: 42rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 26rpx;
    font-weight: bold;
  }

  .main-action {
    flex: 1;
    height: 84rpx;
    background: linear-gradient(135deg, $primary-color, #ffb366);
    color: #fff;
    border-radius: 42rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 30rpx;
    font-weight: bold;
    box-shadow: 0 6rpx 16rpx rgba(255, 148, 60, 0.3);
  }
}

.safe-area-bottom {
  height: 140rpx;
}
</style>
