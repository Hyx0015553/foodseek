<template>
	<view class="page-container">
		<u-navbar title="我的标签" :border="false" :placeholder="true" @leftClick="goBack" :autoBack="false" bgColor="transparent" titleStyle="font-weight: bold;"></u-navbar>
		
    <scroll-view scroll-y :enable-flex="true" class="svcontainer">
      <view class="content-wrapper">
        <view class="tags-card">
          <view class="card-header">
            <text class="title">选择你的兴趣标签</text>
            <text class="subtitle">选择标签让推荐更精准</text>
          </view>
          
          <view class="tags-container">
            <view class="tag-item" v-for="(item, index) in taglist" :key="index" @click="toggleTag(item)" :class="{active: item.checked}">
              <text class="tag-text">{{item.title}}</text>
              <text class="tag-icon" v-if="item.checked">✓</text>
            </view>
          </view>
          
          <view class="action-area">
            <button class="save-btn" @click="saveTags">保存设置</button>
          </view>
        </view>
      </view>
		</scroll-view>
	</view>
</template>

<script>
	import { listSqlj, listj, findj, savej, deletej, fileUrl } from 'common/config/api.js'
	import { ideautil } from '@/common/commontools.js';
	import {mapState, mapActions} from 'vuex'
	export default {
		data() {
			return {
				tid: null,
				fileUrl: fileUrl,
				fobj: {},
				taglist:[],
				tags:''
			};
		},
		onLoad(params) {

		},
		onShow(){
			let tags = this.userInfo.tags || ''
			tags = tags+""
			let taglist = tags.split(",")
			listj({params: {table: 'type' }}).then(res => {
				for (let s of res) {
					s.checked = false
				    for (let g of taglist) {
				        if (g == s.id) {
				        	s.checked = true
				        }
				    }
				}
				this.taglist = res
			})
		},
		methods: {
			...mapActions(['updateUserInfo']),
			toggleTag(item){
				if(item.checked){
					item.checked = false
				}else{
					item.checked = true
				}
			},
			saveTags(){
				let tags = ''
				for (let s of this.taglist) {
					if (s.checked) {
						if (tags) {
							tags+=","+s.id
						}else{
							tags = s.id
						}
					}

				}
				savej({params: {table:'user',id:this.userInfo.id,tags:tags}}).then(res => {
					this.userInfo.tags = tags
					this.updateUserInfo(this.userInfo)
					uni.showToast({
						icon:'none',
						title: '操作成功!'
					})
				})
			},
			goBack(){
				uni.itool.nto({
				    url:'/pages/me/me'
				})
			}
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

.tags-card {
  background-color: #fff;
  border-radius: 24rpx;
  padding: 40rpx 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.03);
  
  .card-header {
    margin-bottom: 40rpx;
    text-align: center;
    
    .title {
      font-size: 36rpx;
      font-weight: bold;
      color: #333;
      display: block;
      margin-bottom: 10rpx;
    }
    
    .subtitle {
      font-size: 24rpx;
      color: #999;
    }
  }
  
  .tags-container {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    margin-bottom: 60rpx;
    
    .tag-item {
      padding: 16rpx 32rpx;
      background-color: #f5f7fa;
      border-radius: 40rpx;
      margin: 12rpx;
      transition: all 0.3s;
      border: 2rpx solid transparent;
      
      .tag-text {
        font-size: 28rpx;
        color: #666;
      }
      
      .tag-icon {
        font-size: 24rpx;
        margin-left: 8rpx;
        color: #fff;
      }
      
      &.active {
        background-color: #fff0f0;
        border-color: #ff5e62;
        
        .tag-text {
          color: #ff5e62;
          font-weight: bold;
        }
        
        .tag-icon {
          color: #ff5e62;
        }
      }
    }
  }
  
  .action-area {
    .save-btn {
      background: linear-gradient(to right, #ff9966, #ff5e62);
      color: #fff;
      border-radius: 50rpx;
      font-size: 32rpx;
      font-weight: bold;
      height: 90rpx;
      line-height: 90rpx;
      box-shadow: 0 8rpx 20rpx rgba(255, 94, 98, 0.3);
      
      &:active {
        transform: scale(0.98);
      }
    }
  }
}
</style>
