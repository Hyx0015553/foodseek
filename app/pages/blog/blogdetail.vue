<template>
	<view class="detail-page">
		<u-navbar :title="fobj.title || '详情'" :border="false" :placeholder="true" @leftClick="goBack" bgColor="#fff" titleStyle="font-weight: bold; font-size: 32rpx;"></u-navbar>
		
    <scroll-view scroll-y :enable-flex="true" class="svcontainer">
      <view class="content-wrapper">
        <!-- 博客内容卡片 -->
        <view class="blog-card">
          <!-- 媒体展示区 -->
          <view class="media-section" v-if="fobj.img || fobj.video">
            <image v-if="fobj.img" class="blockimg" :src="fileUrl+fobj.img" mode="widthFix" @click="previewImg(fileUrl+fobj.img)"></image>
            <showfile v-if="fobj.video" :fileName="fobj.video"></showfile>
          </view>
          
          <!-- 标题区 -->
          <view class="header-section">
            <text class="main-title">{{fobj.title}}</text>
            <view class="meta-row">
              <view class="meta-left">
                <u-avatar :text="fobj.username ? fobj.username.substring(0,1) : 'U'" size="24" fontSize="14" randomBgColor></u-avatar>
                <text class="author-name">{{fobj.username}}</text>
              </view>
              <view class="meta-right">
                <text class="date-text">{{fobj.ndate}}</text>
              </view>
            </view>
          </view>
          
          <!-- 统计数据 -->
          <view class="stats-bar">
            <view class="stat-item">
              <u-icon name="eye" size="16" color="#999"></u-icon>
              <text class="stat-num">{{fobj.vcount || 0}} 阅读</text>
            </view>
          </view>
          
          <!-- 正文内容 -->
          <view class="article-content">
            <u-parse :content="fobj.note" :tagStyle="{img: 'border-radius: 12rpx; margin: 20rpx 0; max-width: 100%;'}" />
          </view>

          <!-- 关联店铺 -->
          <view class="related-shop" @click="viewShop" v-if="fobj.sid">
            <view class="shop-entry">
              <view class="shop-icon">🏪</view>
              <view class="shop-info">
                <view class="shop-label">关联店铺</view>
                <view class="shop-name">{{ shopName || '点击查看店铺' }}</view>
              </view>
              <u-icon name="arrow-right" size="16" color="#999"></u-icon>
            </view>
          </view>

          <view class="admin-toolbar" v-if="userInfo && fobj.uid == userInfo.id">
            <view class="admin-btn edit" @click="editBlog">
              <u-icon name="edit-pen" color="#2979ff" size="20"></u-icon>
              <text>编辑</text>
            </view>
            <view class="admin-btn delete" @click="deleteBlog">
              <u-icon name="trash" color="#fa3534" size="20"></u-icon>
              <text>删除</text>
            </view>
          </view>
          <!-- 交互按钮区 -->
          <view class="interaction-area">
            <view class="action-btn" :class="{active: faviconurl == favfocus}" @click="toggleFav">
              <view class="icon-wrapper">
                <u-icon :name="faviconurl == favfocus ? 'star-fill' : 'star'" :color="faviconurl == favfocus ? '#ff9900' : '#666'" size="24"></u-icon>
              </view>
              <text>{{fobj.favcount || 0}}</text>
            </view>
            
            <view class="action-btn" :class="{active: !zanflag}" @click="zan">
               <view class="icon-wrapper">
                 <u-icon :name="!zanflag ? 'thumb-up-fill' : 'thumb-up'" :color="!zanflag ? '#ff5e62' : '#666'" size="24"></u-icon>
               </view>
               <text>{{fobj.zan || 0}}</text>
            </view>
            
            <view class="action-btn" @click="userDetail">
              <view class="icon-wrapper">
                <u-icon name="chat" size="24" color="#666"></u-icon>
              </view>
              <text>私信</text>
            </view>
          </view>
        </view>

        <!-- 评论区 -->
        <view class="comments-card">
          <view class="section-header">
            <view class="section-indicator"></view>
            <text class="section-title">全部评论 ({{replaylist.length}})</text>
          </view>
          
          <view class="comments-list" v-if="replaylist && replaylist.length > 0">
            <view class="comment-item" v-for="(item, index) in replaylist" :key="index">
              <view class="comment-avatar">
                 <u-avatar :text="item.username ? item.username.substring(0,1) : 'U'" size="32" fontSize="16" randomBgColor></u-avatar>
              </view>
              <view class="comment-content-box">
                <view class="comment-header">
                  <text class="comment-user">{{item.username}}</text>
                  <text class="comment-time">{{item.ndate}}</text>
                </view>
                <view class="comment-text">{{item.note}}</view>
              </view>
            </view>
          </view>
          
          <!-- 空状态 -->
          <view class="empty-state" v-else>
            <u-empty mode="message" icon="http://cdn.uviewui.com/uview/empty/message.png" text="暂无评论, 快来抢沙发吧~"></u-empty>
          </view>
        </view>
      </view>
      
      <!-- 底部占位，防止内容被遮挡 -->
      <view class="safe-area-bottom"></view>
		</scroll-view>
    
    <!-- 底部固定回复栏 -->
    <view class="bottom-reply-bar">
      <view class="reply-container">
        <view class="input-box">
          <u-icon name="edit-pen" color="#999" size="20"></u-icon>
          <input class="reply-input" v-model="rnote" placeholder="写下你的想法..." confirm-type="send" @confirm="replay" />
        </view>
        <view class="send-btn" @click="replay">
          <text>发送</text>
        </view>
      </view>
    </view>
	</view>
</template>

<script>
	import { savej, listj, findj, fileUrl, deletej } from '@/common/config/api.js';
	import {mapState, mapActions} from 'vuex';
	import { ideautil, yewuutil } from '@/common/commontools.js';

	export default {
		components:{},
		data() {
			return {
				id: null,
				fileUrl: fileUrl,
				favtext: "加入收藏",
				fobj: {
          zan:0
        },
				replaylist: [],
				rnote: '',
				pingfen:0,
				favblur:'http://ideapic-1255600738.cos.ap-guangzhou.myqcloud.com/images/icons/favblur.png',
				favfocus:'http://ideapic-1255600738.cos.ap-guangzhou.myqcloud.com/images/icons/favfocus.png',
        faviconurl:'',
				zanurl:'http://ideapic-1255600738.cos.ap-guangzhou.myqcloud.com/images/icons2/zan.png',
				zanfocus:'http://ideapic-1255600738.cos.ap-guangzhou.myqcloud.com/images/icons2/zan2.png',
        zaniconurl:'',
				zanflag:false,
				shopName: ''
			};
		},
		onLoad(params) {
			this.id = params.id
			this.fobjDetail()
		},
		onShow(){
			this.checkZan()
		},
		methods: {
			...mapActions(['setCar']),
			toBlog: yewuutil.toBlog,
      // 编辑功能
      editBlog() {
        uni.itool.nto({
          // 假设你的发布/编辑页面是这个路径，并传入id
          url: '/pages/blog/blogmg?tid='+this.fobj.id
        })
      },


      viewShop() {
        let url = '/pages/shops/shopdetail?pid=' + this.fobj.sid
        if (this.userInfo && this.userInfo.sid) {
          url = '/pages/shops/shopdetail-m?pid=' + this.fobj.sid
        }
        uni.itool.nto({
          url: url
        })
      },

      // 删除功能
      deleteBlog() {
        uni.showModal({
          title: '提示',
          content: '确定要删除这篇内容吗？',
          success: (res) => {
            if (res.confirm) {
              deletej({
                params: {
                  table: 'blog',
                  id: this.fobj.id
                }
              }).then(res => {
                uni.showToast({
                  title: '删除成功',
                  icon: 'success'
                })
                setTimeout(() => {
                  uni.navigateBack()
                }, 1000)
              })
            }
          }
        })
      },
			toPlay(){
				uni.itool.nto({
					url: '/pages/blog/play?pid='+this.fobj.id
				})
			},
			toCall(){
				uni.itool.call(this.fobj.tel)
			},
      previewImg(url){
        uni.previewImage({
          urls: [url]
        })
      },
      checkZan(){
        if (!this.userInfo || !this.userInfo.id || !this.fobj.id) return
        const liked = yewuutil.isBlogZanMarked(this.userInfo.id, this.fobj.id)
        this.zanflag = !liked
        this.zaniconurl = liked ? this.zanfocus : this.zanurl
      },
			fobjDetail() {
				findj({params: {table: 'blog', id: this.id}}).then(res => {
          res.zan = res.zan || 0
					this.fobj = res
					this.fobj.note = ideautil.getHtmlNote(res.note)
					this.checkFavs()
					this.listReplay()
					this.putHistory()
					this.checkZan()
          this.addVcount()
          if (res.sid) {
            findj({ params: { table: 'shop', id: res.sid } }).then(shop => {
              if (shop) this.shopName = shop.sname || ''
            }).catch(() => {})
          }
          if(this.fobj.vtype=='阅后即焚'){
            savej({params: {table:'looked',uid:this.userInfo.id,bid:this.fobj.id}}).then(res => {

            })
          }
				}).catch(err => {
					
				})
			},
			toggleFav() {
				let myfavs = this.userInfo.favs2
				let favcount = this.fobj.favcount || 0
				console.log("favs2:"+myfavs)
				if (this.faviconurl == this.favblur) {
					if (myfavs && myfavs != '0') {
						myfavs += ","+this.fobj.id
					}else{
						myfavs = this.fobj.id+""
					}
					favcount += 1
				}else {
					myfavs = ideautil.removeStrInStr(this.fobj.id, myfavs) || "0"
					favcount -= 1
				}
				this.fobj.favcount = favcount
				savej({ params: { table: "user", favs2: myfavs, id: this.userInfo.id } }).then(res => {
					this.userInfo.favs2 = myfavs
					this.checkFavs()
					savej({params: {table: "blog",id: this.fobj.id, favcount: this.fobj.favcount }}).then(res => {
						
					}).catch(err => {
					 
					})
				}).catch(err => {
					
				})
			},

      zan() {
        if (!this.userInfo || !this.userInfo.id) {
          uni.showToast({ title: '请先登录', icon: 'none' })
          return
        }
        const uid = this.userInfo.id
        const bid = this.fobj.id
        if (this.zanflag) {
          this.fobj.zan = this.fobj.zan || 0
          this.fobj.zan = this.fobj.zan * 1 + 1
          savej({ params: { table: 'blog', id: bid, zan: this.fobj.zan } }).then(() => {
            yewuutil.setBlogZanMarked(uid, bid, true)
            yewuutil.addZanBlogId(uid, bid)
            this.zanflag = false
            this.zaniconurl = this.zanfocus
          })
        } else {
          this.fobj.zan = this.fobj.zan || 0
          this.fobj.zan = Math.max(0, this.fobj.zan * 1 - 1)
          savej({ params: { table: 'blog', id: bid, zan: this.fobj.zan } }).then(() => {
            yewuutil.setBlogZanMarked(uid, bid, false)
            yewuutil.removeZanBlogId(uid, bid)
            this.zanflag = true
            this.zaniconurl = this.zanurl
          })
        }
      },
      addVcount() {
        this.fobj.vcount = this.fobj.vcount || 0
        this.fobj.vcount = this.fobj.vcount*1+1
        savej({params: {table: "blog",id: this.fobj.id, vcount: this.fobj.vcount }}).then(res => {

        })
      },
			checkFavs() {
				this.faviconurl = this.favblur
				let myfavs = this.userInfo.favs2
				let isfav = ideautil.checkStrInStr(this.fobj.id+"",myfavs)
				if (isfav) {
					this.faviconurl = this.favfocus
				}
			},
      userDetail() {
        uni.itool.nto({
          url: '/pages/qunzu/userdetail?id=' + this.fobj.uid
        })
      },
			goBack(){
				/*uni.itool.nto({
				    url:'/pages/index/index'
				})*/
        uni.navigateBack()
			},
			toChat(){
				ideautil.toChatUni(this.fobj.uid)
			},
			listReplay(){
				listj({params: {table: 'replay', pid: this.fobj.id, type: 2}}).then(res => {
					this.replaylist = res
				}).catch(err => {
				 
				})
			},
			replay(){
				if (!this.userInfo || !this.userInfo.id) {
					uni.ytool.toLogin()
					return
				}
				if(this.rnote){
					let fdata = {
						table:"replay",
						pid: this.fobj.id,
						note: this.rnote,
						uid: this.userInfo.id,
						username: this.userInfo.username,
						type: 2
					}
					savej({params: fdata}).then(res => {
						this.listReplay()
						this.rnote = ""
					}).catch(err => {
					 
					})
				}else{
          uni.showToast({
            icon:'none',
            title:'请输入评论内容'
          })
        }
			},
			putHistory(){
				let id = this.fobj.id
				let hgids = uni.getStorageSync("his_ids")
				if(hgids){
					let flag = ideautil.checkStrInStr(id,hgids)
					if (!flag) {
						hgids += ","+id
					}
				}else{
					hgids = id
				}
				uni.setStorageSync("his_ids",hgids)
			},
		},
		computed: {
			...mapState(['carlist', 'userInfo'])
		}
	}
</script>

<style lang="scss" scoped>
/* 发布者管理工具栏样式 */
.admin-toolbar {
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 20rpx 0;
  margin-bottom: 20rpx;
  background-color: #f9fbfd;
  border-radius: 12rpx;
  border: 1rpx solid #eef2f7;

  .admin-btn {
    display: flex;
    align-items: center;
    padding: 10rpx 20rpx;

    text {
      font-size: 26rpx;
      margin-left: 8rpx;
      font-weight: 500;
    }

    &.edit text { color: #2979ff; }
    &.ledger text { color: #19be6b; }
    &.delete text { color: #fa3534; }

    &:active {
      opacity: 0.7;
      background-color: #f0f0f0;
      border-radius: 8rpx;
    }
  }
}
.detail-page {
  background-color: #f7f8fa;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.svcontainer {
  flex: 1;
  height: 0; // Important for scroll-view in flex layout
}

.content-wrapper {
  padding: 20rpx;
}

/* 博客内容卡片 */
.blog-card {
  background-color: #fff;
  border-radius: 24rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.03);
  
  .media-section {
    width: 100%;
    margin-bottom: 30rpx;
    border-radius: 16rpx;
    overflow: hidden;
    
    .blockimg {
      width: 100%;
      display: block;
    }
  }
  
  .header-section {
    margin-bottom: 24rpx;
    
    .main-title {
      font-size: 38rpx;
      font-weight: 700;
      color: #333;
      line-height: 1.4;
      margin-bottom: 20rpx;
      display: block;
    }
    
    .meta-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .meta-left {
        display: flex;
        align-items: center;
        
        .author-name {
          font-size: 28rpx;
          color: #333;
          margin-left: 12rpx;
          font-weight: 500;
        }
      }
      
      .meta-right {
        .date-text {
          font-size: 24rpx;
          color: #999;
        }
      }
    }
  }
  
  .stats-bar {
    margin-bottom: 30rpx;
    
    .stat-item {
      display: inline-flex;
      align-items: center;
      background-color: #f5f7fa;
      padding: 6rpx 16rpx;
      border-radius: 8rpx;
      
      .stat-num {
        font-size: 24rpx;
        color: #999;
        margin-left: 8rpx;
      }
    }
  }
  
  .article-content {
    font-size: 32rpx;
    line-height: 1.8;
    color: #333;
    margin-bottom: 40rpx;
  }
  
  .interaction-area {
    display: flex;
    justify-content: space-around;
    padding-top: 30rpx;
    border-top: 1rpx solid #f0f0f0;
    
    .action-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      transition: all 0.3s;
      
      .icon-wrapper {
        width: 80rpx;
        height: 80rpx;
        border-radius: 50%;
        background-color: #f5f7fa;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 10rpx;
        transition: all 0.3s;
      }
      
      text {
        font-size: 24rpx;
        color: #666;
      }
      
      &.active {
        .icon-wrapper {
          background-color: #fff0f0;
          transform: scale(1.05);
        }
        text {
          color: #ff5e62;
        }
      }
    }
  }
}

/* 评论区卡片 */
.comments-card {
  background-color: #fff;
  border-radius: 24rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.03);
  min-height: 300rpx;
  
  .section-header {
    display: flex;
    align-items: center;
    margin-bottom: 30rpx;
    
    .section-indicator {
      width: 8rpx;
      height: 32rpx;
      background: linear-gradient(to bottom, #ff9966, #ff5e62);
      border-radius: 4rpx;
      margin-right: 16rpx;
    }
    
    .section-title {
      font-size: 32rpx;
      font-weight: bold;
      color: #333;
    }
  }
  
  .comments-list {
    .comment-item {
      display: flex;
      margin-bottom: 30rpx;
      
      .comment-avatar {
        margin-right: 20rpx;
        flex-shrink: 0;
      }
      
      .comment-content-box {
        flex: 1;
        
        .comment-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8rpx;
          
          .comment-user {
            font-size: 28rpx;
            color: #666;
            font-weight: 500;
          }
          
          .comment-time {
            font-size: 24rpx;
            color: #ccc;
          }
        }
        
        .comment-text {
          font-size: 28rpx;
          color: #333;
          line-height: 1.5;
        }
      }
    }
  }
  
  .empty-state {
    padding: 60rpx 0;
    display: flex;
    justify-content: center;
  }
}

.safe-area-bottom {
  height: 120rpx;
  width: 100%;
}

/* 底部回复栏 */
.bottom-reply-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  background-color: #fff;
  box-shadow: 0 -4rpx 16rpx rgba(0,0,0,0.05);
  z-index: 100;
  padding-bottom: env(safe-area-inset-bottom);
  
  .reply-container {
    display: flex;
    align-items: center;
    padding: 20rpx 30rpx;
    
    .input-box {
      flex: 1;
      background-color: #f5f7fa;
      border-radius: 40rpx;
      height: 72rpx;
      display: flex;
      align-items: center;
      padding: 0 30rpx;
      margin-right: 20rpx;
      
      .reply-input {
        flex: 1;
        height: 100%;
        font-size: 28rpx;
        margin-left: 10rpx;
      }
    }
    
    .send-btn {
      width: 120rpx;
      height: 72rpx;
      background: linear-gradient(to right, #ff9966, #ff5e62);
      border-radius: 40rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 28rpx;
      font-weight: 500;
      box-shadow: 0 4rpx 12rpx rgba(255, 94, 98, 0.3);
      
      &:active {
        transform: scale(0.96);
      }
    }
  }
}

/* 关联店铺 */
.related-shop {
  margin: 0 30rpx 24rpx;
  padding: 0;
  background: transparent;
  border-radius: 0;

  .shop-entry {
    display: flex;
    align-items: center;
    padding: 20rpx;
    background: linear-gradient(135deg, #fff7e6, #fff);
    border-radius: 16rpx;
    border: 1rpx solid #ffe4b3;

    .shop-icon {
      font-size: 48rpx;
      margin-right: 20rpx;
    }

    .shop-info {
      flex: 1;

      .shop-label {
        font-size: 24rpx;
        color: #ff9d00;
        margin-bottom: 6rpx;
      }

      .shop-name {
        font-size: 30rpx;
        color: #333;
        font-weight: 500;
      }
    }
  }
}
</style>
