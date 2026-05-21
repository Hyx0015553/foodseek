<template>
  <view class="page-container">
    <u-navbar
      title="店铺信息维护"
      :border="false"
      :placeholder="true"
      :autoBack="true"
      bgColor="#ffffff"
      titleStyle="font-weight: bold; color: #1a1a1a;"
    ></u-navbar>

    <scroll-view scroll-y :enable-flex="true" class="main-scroll">
      <!-- 头部卡片：店铺基础信息预览 -->
      <view class="info-card">
        <view class="info-header">
          <view class="logo-wrapper">
            <image
              v-if="fobj.img"
              :src="fileUrl + fobj.img"
              class="logo-image"
              mode="aspectFill"
            ></image>
            <view v-else class="logo-placeholder">店</view>
          </view>
          <view class="info-texts">
            <text class="shop-name">{{ fobj.sname || '请填写店铺名称' }}</text>
            <text class="shop-sub">
              {{ fobj.address || '请完善店铺地址信息' }}
            </text>
          </view>
        </view>
      </view>

      <!-- 表单卡片：基础信息 -->
      <view class="form-card">
        <view class="section-title">
          <view class="title-line"></view>
          <text>基础信息</text>
        </view>

        <view class="form-item">
          <text class="form-label">店铺名称</text>
          <input
            type="text"
            v-model="fobj.sname"
            class="form-input"
            placeholder="请输入店铺名称"
          />
        </view>

        <view class="form-item">
          <text class="form-label">联系电话</text>
          <input
            type="text"
            v-model="fobj.tel"
            class="form-input"
            placeholder="请输入联系电话"
          />
        </view>

        <view class="form-item">
          <text class="form-label">详细地址</text>
          <input
            type="text"
            v-model="fobj.address"
            class="form-input"
            placeholder="请输入详细地址"
          />
        </view>

        <view class="form-item form-item-logo">
          <text class="form-label">店铺 LOGO</text>
          <view class="logo-upload">
            <fileupload v-model="fobj.img"></fileupload>
            <text class="logo-tip">建议使用方形清晰图片，展示店铺品牌形象</text>
          </view>
        </view>
      </view>

      <!-- 表单卡片：图文介绍 -->
      <view class="form-card">
        <view class="section-title">
          <view class="title-line"></view>
          <text>店铺介绍</text>
        </view>
        <view class="editor-wrapper">
          <jinEdit
            :html="editorHtml"
            @getEditorContxt="getEditorContxt"
            v-model="fobj.note"
            height="300px"
            :uploadFileUrl="uploadUrl"
            placeholder="请输入店铺介绍，如主营菜品、环境特色等"
            @editOk="editOk"
          ></jinEdit>
        </view>
      </view>

      <view class="safe-area-bottom"></view>
    </scroll-view>

    <!-- 底部提交按钮 -->
    <view class="fixed-footer">
      <view class="footer-btn-group">
        <view class="main-action" @click="saveObj">
          保存店铺信息
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
		uploadUrl,
		gongshangxhaxun
	} from '@/common/config/api.js';
	import {
		mapState,
		mapActions
	} from 'vuex';
	import jinEdit from '@/components/jin-edit/jin-edit.vue';

	import { ideautil, yewuutil } from '@/common/commontools.js'
	import { SHOP_AUDIT_STATE, shopAuditCode } from '@/common/shopState.js'
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
					title: '',
					type: 1,
					note: '',
					img: '',
					table: 'shop'
				},
				tid: null,
				fileUrl: fileUrl,
				uploadUrl: uploadUrl,
				editorCtx:null,
				editorHtml:'',
				totype:1,
				sfileName:''
			};
		},
		onLoad(params) {
			findj({params: {table: 'shop', id: this.userInfo.sid}}).then(res => {
				let id = null
				if(res && res.id){
					id = res.id
				}

				this.totype = params.totype || 1
				if (id) {
					findj({
						params: {
							table: 'shop',
							id: id
						}
					}).then(res => {
						this.fobj = res
						this.filelist.push({url: this.fileUrl + res.img})
						console.log(this.imgfile)
						this.editorHtml = this.fobj.note
						this.editorCtx.setContents({
                            html: this.fobj.note
                        })

					}).catch(err => {

					})
				}
			})


		},
		methods: {
		
			...mapActions(['updateUserInfo']),
			async gscx(){
				return new Promise((resove)=>{
					gongshangxhaxun({params: {companyName: this.fobj.sname }}).then(res => {
						resove(res)
					})
				})
			},
			async saveObj() {
				/*let r = await this.gscx()
				let errorCode = r.error_code
				if (errorCode!="0") {
					uni.showToast({
						icon:'none',
						title: "检查公司名称:"+r.reason
					})
					return
				}*/
				this.editorCtx.getContents({
					success: res => {
						this.fobj.note = res.html
						this.fobj.uid = this.userInfo.id
						this.fobj.username = this.userInfo.username
						console.log(this.filelist)
						 
						//this.filelist.length?this.fobj.img = this.filelist[0].url.data:this.fobj.img=""
						let fdata = this.fobj
						fdata.table = "shop"
						fdata.ownid = this.userInfo.id
						// 商家端提交或修改店铺信息后，统一进入待审核状态
						const auditSt = this.fobj.state != null ? Number(this.fobj.state) : shopAuditCode(this.fobj.statecn)
						if (auditSt == null || auditSt === SHOP_AUDIT_STATE.APPROVED) {
							fdata.state = SHOP_AUDIT_STATE.PENDING
						}
						savej({
							params: fdata
						}).then(res => {
							if (this.fobj.id) {
								uni.navigateBack()
							}else{
								this.fobj.id = res
								savej({params: {table:"user", sid:this.fobj.id, shop:this.fobj.sname, id:this.userInfo.id}}).then(res => {
									this.userInfo.sid = this.fobj.id
									this.userInfo.shop = this.fobj.sname
									this.updateUserInfo(this.userInfo)
									uni.navigateBack()
								})
							}

							
						}).catch(err => {
						
						})
					} 
				})
				
			},
			comboboxselect(e){
				this.fobj.typeid = e.id
				this.fobj.type = e.title
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
  height: calc(100vh - 120rpx);
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

  .logo-wrapper {
    width: 120rpx;
    height: 120rpx;
    border-radius: 24rpx;
    background: #fff7e6;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 24rpx;

    .logo-image {
      width: 100%;
      height: 100%;
      border-radius: 24rpx;
    }

    .logo-placeholder {
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

    .shop-name {
      font-size: 34rpx;
      font-weight: 700;
      color: #1a1a1a;
      margin-bottom: 8rpx;
    }

    .shop-sub {
      font-size: 24rpx;
      color: #909399;
      line-height: 1.5;
      white-space: normal;
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

.form-item-logo {
  .logo-upload {
    display: flex;
    flex-direction: column;
    gap: 8rpx;
  }

  .logo-tip {
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

    &:active {
      opacity: 0.9;
      transform: scale(0.98);
    }
  }
}

.safe-area-bottom {
  height: 140rpx;
}
</style>
