<template>
	<view class="page-container">
		<u-navbar title="我的点赞" :border="false" :placeholder="true" :autoBack="true" bgColor="transparent" titleStyle="font-weight: bold;"></u-navbar>
		
    <scroll-view scroll-y :enable-flex="true" class="svcontainer">
      <view class="content-wrapper">
        <view class="like-list">
          <view class="like-item" v-for="(item, index) in fobjList" :key="index" @click="toBlogDetail(item.id)">
            <view class="item-left">
              <view class="avatar-box">
                <text class="emoji-icon">❤️</text>
              </view>
            </view>
            <view class="item-center">
              <text class="item-title u-line-1">{{item.title}}</text>
              <text class="item-author">@{{item.username}}</text>
            </view>
            <view class="item-right">
              <u-icon name="arrow-right" size="14" color="#ccc"></u-icon>
            </view>
          </view>
          
          <u-empty v-if="!fobjList.length" mode="favor" icon="http://cdn.uviewui.com/uview/empty/favor.png" text="暂无点赞内容"></u-empty>
        </view>
      </view>
		</scroll-view>
	</view>
</template>

<script>
	import { listSqlj } from '@/common/config/api.js';
	import {mapState, mapActions} from 'vuex';
	import { ideautil, yewuutil } from '@/common/commontools.js';
  
	export default {
		data() {
			return {
				fobjList: []
			};
		},
		components:{},
		onLoad(params) {
			const uid = this.userInfo && this.userInfo.id
			if (!uid) {
				this.fobjList = []
				return
			}
			const ids = yewuutil.getZanBlogIds(uid)
			if (!ids.length) {
				this.fobjList = []
				return
			}
			const zids = ids.map(id => String(id)).join(',')
			const sql = `select b.*,(select count(pid) from fs_replay r where r.pid=b.id) pinglun,u.img uimg from fs_blog b left join fs_user u on u.id=b.uid where b.id in (${zids})`
			listSqlj({ params: { sql } }).then(res => {
				this.fobjList = res || []
			}).catch(() => {
				this.fobjList = []
			})
		},
		methods: {
			...mapActions(['updateUserInfo']),
			toBlogDetail:yewuutil.toBlogDetail,
			
			goBack(){
				uni.navigateBack({
					delta:1
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
  padding: 20rpx 30rpx;
}

.like-list {
  .like-item {
    background-color: #fff;
    border-radius: 20rpx;
    padding: 24rpx 30rpx;
    margin-bottom: 20rpx;
    display: flex;
    align-items: center;
    box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.02);
    
    .item-left {
      margin-right: 24rpx;
      
      .avatar-box {
        width: 80rpx;
        height: 80rpx;
        border-radius: 50%;
        background-color: #fff0f0;
        display: flex;
        justify-content: center;
        align-items: center;
        
        .emoji-icon {
          font-size: 40rpx;
        }
      }
    }
    
    .item-center {
      flex: 1;
      display: flex;
      flex-direction: column;
      
      .item-title {
        font-size: 30rpx;
        font-weight: bold;
        color: #333;
        margin-bottom: 8rpx;
      }
      
      .item-author {
        font-size: 24rpx;
        color: #999;
      }
    }
    
    .item-right {
      margin-left: 20rpx;
    }
  }
}
</style>
