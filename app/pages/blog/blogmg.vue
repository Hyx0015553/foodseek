<template>
	<view class="page-container">
		<u-navbar title="发布动态" :border="false" :placeholder="true" @leftClick="goBack" :autoBack="false" bgColor="transparent" titleStyle="font-weight: bold;"></u-navbar>
		
    <scroll-view scroll-y :enable-flex="true" class="svcontainer">
      <view class="content-wrapper">
        <view class="form-card">
          <ideaselect label="分类" showName="title" :datalist="[typelist]" v-model="sobj"></ideaselect>
          <view class="form-item">
            <view class="label-row">
              <text class="emoji-icon">📝</text>
              <text class="label-text">标题</text>
            </view>
            <u--input border="none" v-model="fobj.title" placeholder="请输入标题..." class="custom-input"></u--input>
          </view>
          <view class="form-item">
            <view class="label-row">
              <text class="emoji-icon">✨</text>
              <text class="label-text">文案风格</text>
            </view>
            <u-radio-group v-model="styleType" placement="row">
              <u-radio
                v-for="item in styleList"
                :key="item.value"
                :name="item.value"
                :label="item.label"
              ></u-radio>
            </u-radio-group>
          </view>
          <ideaslc label="私密级别" :datalist="[['公开','好友可见','阅后即焚']]" v-model="fobj.vtype"></ideaslc>

          <ideaselect label="关联店铺" showName="title" :datalist="[shoplist]" v-model="shopobj"></ideaselect>
          <choosemapaddress v-model="addressObj"></choosemapaddress>
          <view class="form-item">
            <view class="label-row">
              <text class="emoji-icon">🖼️</text>
              <text class="label-text">配图</text>
            </view>
            <view class="upload-box">
              <fileupload v-model="fobj.img"></fileupload>
            </view>
          </view>

          <view class="form-item">
            <view class="label-row">
              <text class="emoji-icon">📄</text>
              <text class="label-text">详细内容</text>
            </view>
            <view class="editor-box">
              <jinEdit :html="editorHtml" @getEditorContxt="getEditorContxt" v-model="fobj.note" height="300px" :uploadFileUrl="uploadUrl" placeholder="请输入详细内容..." @editOk="editOk"></jinEdit>
            </view>
          </view>

          <view class="btn-group">
            <view class="ai-btn" hover-class="ai-btn--active" @tap.stop="genAiBlog">AI生成文案</view>
            <button class="submit-btn" hover-class="submit-btn--active" @tap.stop="saveBlog()">发布动态</button>
            <button class="delete-btn" hover-class="none" @tap.stop="delBlog()" v-if="tid">删除动态</button>
          </view>
          
        </view>
      </view>
		</scroll-view>

		<u-popup :show="entryNoOrderPopup" mode="bottom" round="20" @close="entryNoOrderPopup = false">
			<view class="no-order-popup">
				<view class="no-order-title">提示</view>
				<view class="no-order-msg">还没有已完成订单</view>
				<view class="no-order-opt" @click="goBlogplanFromPopup">创建探店计划</view>
				<view class="no-order-opt" @click="goBrowseFromPopup">再逛逛</view>
				<view class="no-order-cancel" @click="entryNoOrderPopup = false">关闭</view>
			</view>
		</u-popup>
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
    aiGenBlog
	} from '@/common/config/api.js'
	import {
		mapState,
		mapActions
	} from 'vuex';
	import jinEdit from '@/components/jin-edit/jin-edit.vue'

	import { ideautil, yewuutil } from '@/common/commontools.js'
	export default {
		components: {
			jinEdit
		},
		data() {
			return {
        addressObj:{
          address:'',
          latitude:'',
          longitude:''
        },

				tel: '',
				typelist: [],
        shoplist: [],
				filelist: [],
				fobj: {
					video:'',
          address:''
				},
        // AI 文案风格
        styleType: '简短生活化种草',
        styleList: [
          { value: '简短生活化种草', label: '简短种草' },
          { value: '专业点评', label: '专业点评' },
          { value: '幽默搞怪', label: '幽默搞怪' }
        ],
				tid: null,
				fileUrl: fileUrl,
				uploadUrl: uploadUrl,
				editorCtx:null,
				editorHtml:'',
				sshow1:false,
				sobj:{},
        shopobj:{},
				uid:null,
				mgclist:[],
        from:'',
        entryPid: null,
        entryNoOrderPopup: false
			};
		},
    onShow(){
      let addressobj = getApp().globalData.targetAddress
      if (addressobj) {
        this.fobj.address = addressobj.address
        this.fobj.latitude = addressobj.latitude
        this.fobj.longitude = addressobj.longitude
        getApp().globalData.targetAddress = null
      }
      if (!this.tid && this.entryPid && this.userInfo && this.userInfo.id) {
        yewuutil.hasCompletedOrderForShop(this.userInfo.id, this.entryPid).then(ok => {
          this.entryNoOrderPopup = !ok
        })
      } else {
        this.entryNoOrderPopup = false
      }
    },
		onLoad(params) {
			this.uid = params.uid
      this.from = params.from
      this.entryPid = params.pid && params.pid !== 'undefined' ? params.pid : null
			if (params.pid && params.pid !== 'undefined') {
				findj({
					params: { table: 'shop', id: params.pid }
				}).then(res => {
					if (res && res.id) {
						this.shopobj = { id: res.id, title: res.sname, sname: res.sname }
					}
				}).catch(() => {})
			}
			if (params.tid && params.tid !== 'undefined') {
				this.tid = params.tid

			}
			console.log('tttttttid:' + params.tid + " showdel:" + this.showdel)
			if (this.tid) {
				findj({
					params: {
						table: 'blog',
						id: this.tid
					}
				}).then(res => {
					this.fobj = res
          this.fobj.note = uni.itool.getHtmlNote(this.fobj.note)
          this.sobj.id = this.fobj.typeid
          this.sobj.title = this.fobj.typecn
					this.editorHtml = this.fobj.note
					this.editorCtx.setContents({
						html: this.fobj.note
					})
					
				}).catch(err => {

				})
			}
			
			listj({
				params: {
					table: 'btype'
				}
			}).then(res => {
				for (let s of res) {
					s.name = s.title
				}
				this.typelist = res || []
			}).catch(err => {
			
			})

			listj({
				params: {
					table: 'mgc'
				}
			}).then(res => {
				this.mgclist = res
			})
      listj({
        params: {
          table: 'shop'
        }
      }).then(res2 => {
        for (let s of res2) {
            s.title = s.sname
        }
        this.shoplist = res2
      })
		},
		methods: {
			//...mapActions(['']),
			hideKeyboard: ideautil.hideKeyboard,
      chooseLocation(){
        uni.itool.toTxmapLocation()
      },
			getUploadImg(imgurl){
				if(imgurl){
					this.fobj.img = imgurl
				}
			},
			saveBlog() {
				this.editorCtx.getContents({
					success: async (res) => {
						for (let s of this.mgclist) {
							if (res.html.indexOf(s.title) != -1) {
								uni.showToast({
									icon:'none',
									title: '包含敏感词!'
								})
								return
							}
						}
						if (!this.tid) {
							const sid = this.shopobj && this.shopobj.id
							if (sid && this.userInfo && this.userInfo.id) {
								const ok = await yewuutil.hasCompletedOrderForShop(this.userInfo.id, sid)
								if (!ok) {
									uni.showToast({
										icon: 'none',
										title: '还没有已完成订单'
									})
									this.entryNoOrderPopup = true
									return
								}
							}
						}
						this.fobj.note = res.html
						this.fobj.uid = this.userInfo.id
						this.fobj.username = this.userInfo.username
						this.fobj.typeid = this.sobj.id
						this.fobj.typecn = this.sobj.title
            this.fobj.address = this.addressObj.address
            this.fobj.latitude = this.addressObj.latitude
            this.fobj.longitude = this.addressObj.longitude
            this.fobj.sid = this.shopobj.id
            this.fobj.stitle = this.shopobj.sname
						let fdata = this.fobj
						fdata.table = "blog"
						fdata.zan = 1
						savej({
							params: fdata
						}).then(res => {
							uni.itool.nto({
								url: './bloglist'
							})
						}).catch(err => {
						
						})
					} 
				})
				
			},
      // 调用后端 AI 接口生成探店文案
      genAiBlog() {
        try {
          if (!this.userInfo || !this.userInfo.id) {
            uni.showToast({ icon: 'none', title: '请先登录后再使用 AI 生成' })
            return
          }
          const imgRaw = this.fobj.img
          if (!imgRaw || String(imgRaw).trim() === '') {
            uni.showToast({ icon: 'none', title: '请先上传配图，再生成文案' })
            return
          }
          const imgUrl = this.fileUrl + String(imgRaw).trim()
          const shopName = (this.shopobj && (this.shopobj.sname || this.shopobj.title))
            ? String(this.shopobj.sname || this.shopobj.title).trim()
            : ''
          const uid = this.userInfo.id
          const imgDesc = ''
          uni.showLoading({ title: 'AI 正在生成...', mask: true })
          let req
          try {
            req = aiGenBlog({
              params: {
                imgUrl,
                shopName,
                uid,
                imgDesc,
                style: this.styleType
              }
            })
          } catch (initErr) {
            uni.hideLoading()
            uni.showToast({
              icon: 'none',
              title: (initErr && initErr.message) ? String(initErr.message) : '网络模块未就绪，请重启小程序'
            })
            return
          }
          req.then((res) => {
            uni.hideLoading()
            if (!res) {
              uni.showToast({ icon: 'none', title: '未收到生成结果' })
              return
            }
            const content = res.content != null ? String(res.content) : ''
            if (content.indexOf('AI 文案生成失败') === 0 || content.indexOf('错误信息：') !== -1) {
              const tip = content.length > 120 ? content.slice(0, 120) + '…' : content
              uni.showToast({ icon: 'none', title: tip, duration: 3500 })
              return
            }
            if (res.title) {
              this.fobj.title = res.title
            }
            if (content) {
              this.fobj.note = content
              this.editorHtml = content
              if (this.editorCtx && typeof this.editorCtx.setContents === 'function') {
                this.editorCtx.setContents({ html: content })
              }
            }
            uni.showToast({ icon: 'success', title: 'AI 文案已生成' })
          }).catch((err) => {
            uni.hideLoading()
            console.error('[genAiBlog]', err)
            let msg = 'AI 生成失败，请检查网络与服务地址'
            try {
              const em = err && (err.errMsg || err.message)
              if (em && String(em).indexOf('timeout') !== -1) {
                msg = '请求超时，请稍后重试'
              }
            } catch (e) {}
            uni.showToast({ icon: 'none', title: msg, duration: 3000 })
          })
        } catch (e) {
          uni.hideLoading()
          console.error('[genAiBlog] sync', e)
          uni.showToast({
            icon: 'none',
            title: (e && e.message) ? String(e.message) : '操作异常，请重试'
          })
        }
      },
			comboboxselect(e){
				this.fobj.typeid = e.id
				this.fobj.type = e.title
			},
			delBlog() {
				deletej({
					params: {
						table: 'blog',
						id: this.tid
					}
				}).then(res => {
					uni.itool.nto({
						url: './blog'
					})
				}).catch(err => {

				})
			},
			goBlogplanFromPopup() {
				this.entryNoOrderPopup = false
				const pid = (this.shopobj && this.shopobj.id) || this.entryPid
				if (!pid) {
					uni.showToast({ title: '请先关联店铺', icon: 'none' })
					return
				}
				uni.navigateTo({ url: '/pages/blog/blogplan?pid=' + pid })
			},
			goBrowseFromPopup() {
				this.entryNoOrderPopup = false
				const sid = (this.shopobj && this.shopobj.id) || this.entryPid
				if (sid) {
					uni.navigateTo({ url: '/pages/good/goodview?sid=' + sid })
				} else {
					uni.switchTab({ url: '/pages/index/index' })
				}
			},
			goBack() {
        uni.navigateBack()
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
				this.fobj.typecn = e.name
				this.fobj.typeid = e.id
			},
		},
    watch:{
    },
		computed: {
			...mapState(['userInfo'])
		}
	}
</script>

<style lang="scss" scoped>
.page-container {
  background-color: #f7f8fa;
  min-height: 100vh;
}

.svcontainer {
  height: calc(100vh - 88rpx);
}

.content-wrapper {
  padding: 30rpx;
}

.form-card {
  background-color: #fff;
  border-radius: 24rpx;
  padding: 40rpx 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.03);
}

.form-item {
  margin-bottom: 40rpx;
  
  .label-row {
    display: flex;
    align-items: center;
    margin-bottom: 20rpx;
    
    .emoji-icon {
      font-size: 36rpx;
      margin-right: 12rpx;
    }
    
    .label-text {
      font-size: 30rpx;
      font-weight: bold;
      color: #333;
    }
  }
  
  .custom-input {
    background-color: #f5f7fa;
    border-radius: 16rpx;
    padding: 10rpx 20rpx;
  }
  
  .editor-box {
    border: 2rpx solid #f0f0f0;
    border-radius: 16rpx;
    overflow: hidden;
  }
  
  .upload-box {
    padding: 10rpx 0;
  }
}

.btn-group {
  margin-top: 60rpx;

  /* 使用 view 替代原生 button，避免小程序里 button 与 editor 叠层导致点击无响应 */
  .ai-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #fff;
    color: #333;
    border: 2rpx solid #e5e5e5;
    border-radius: 50rpx;
    font-size: 30rpx;
    font-weight: 600;
    height: 88rpx;
    line-height: 88rpx;
    margin-bottom: 24rpx;
    box-sizing: border-box;
  }
  .ai-btn--active {
    opacity: 0.85;
    background-color: #f7f8fa;
  }
  
  .submit-btn {
    background: linear-gradient(to right, #ff9966, #ff5e62);
    color: #fff;
    border-radius: 50rpx;
    font-size: 32rpx;
    font-weight: bold;
    height: 90rpx;
    line-height: 90rpx;
    margin-bottom: 30rpx;
    box-shadow: 0 8rpx 20rpx rgba(255, 94, 98, 0.3);
    border: none;

    &::after {
      border: none;
    }
    &.submit-btn--active {
      opacity: 0.92;
    }
  }
  
  .delete-btn {
    background-color: #fff0f0;
    color: #ff5e62;
    border-radius: 50rpx;
    font-size: 32rpx;
    font-weight: bold;
    height: 90rpx;
    line-height: 90rpx;
    border: none;

    &::after {
      border: none;
    }
    &:active {
      background-color: #ffe6e6;
    }
  }
}

.no-order-popup {
  padding: 32rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
}
.no-order-title {
  text-align: center;
  font-size: 32rpx;
  font-weight: bold;
  color: #1a1a1a;
  margin-bottom: 16rpx;
}
.no-order-msg {
  text-align: center;
  font-size: 28rpx;
  color: #606266;
  margin-bottom: 24rpx;
}
.no-order-opt {
  padding: 28rpx 24rpx;
  text-align: center;
  font-size: 30rpx;
  color: #303133;
  border-bottom: 1rpx solid #f0f0f0;
}
.no-order-cancel {
  margin-top: 16rpx;
  padding: 24rpx;
  text-align: center;
  font-size: 28rpx;
  color: #909399;
}
</style>
