<template>
	<view class="page-container">
		<u-navbar title="店铺文章" @leftClick="goBack" :border="false" :placeholder="true" :autoBack="true" bgColor="transparent" titleStyle="font-weight: bold;"></u-navbar>
		
    <scroll-view scroll-y :enable-flex="true" class="svcontainer">
      <view class="content-wrapper">
        <view class="like-list">
          <imglist
            :showSearch="false"
            imgName="img"
            @clickItem="toBlogDetail"
            :dataList="fobjList"
            :imgSize="3"
            titleName="title"
            sName="note"
            tName="typecn"
          ></imglist>
          
          <u-empty v-if="!fobjList.length" mode="favor" icon="http://cdn.uviewui.com/uview/empty/favor.png" text="暂无店铺动态"></u-empty>
        </view>
      </view>
		</scroll-view>
	</view>
</template>

<script>
	import { listSqlj, listj, findj, savej, fileUrl } from '@/common/config/api.js';
	import {mapState, mapActions} from 'vuex';
	import { ideautil, yewuutil } from '@/common/commontools.js';
  
	export default {
		data() {
			return {
				fobjList: [],
				typelist: [],
				noticelist: []
			};
		},
		components:{},
		onLoad() {
			this.refreshBlogList()
		},
		onShow() {
			this.refreshBlogList()
		},
		methods: {
			...mapActions(['updateUserInfo']),
			toBlogDetail:yewuutil.toBlogDetail,
			/**
			 * 商家端：仅使用当前登录用户的店铺 id（userInfo.sid），与 fs_blog.sid 对应。
			 * 不再读取路由 ?sid=，避免篡改链接查看其它店铺文章。
			 */
			resolveMerchantBlogSid() {
				const u = this.userInfo
				if (!u || u.sid == null || String(u.sid).trim() === '') {
					return ''
				}
				return String(u.sid).trim()
			},
			/** 仅保留 blog.sid 与当前店铺一致的记录（双保险，与后端 list 条件一致） */
			filterBlogsByShop(rows, shopSid) {
				const want = String(shopSid).trim()
				if (!want) return []
				return (rows || []).filter((row) => {
					if (!row) return false
					const rsid = row.sid != null && row.sid !== '' ? String(row.sid).trim() : ''
					return rsid === want
				})
			},
			refreshBlogList() {
				const sid = this.resolveMerchantBlogSid()
				if (!sid) {
					this.fobjList = []
					return
				}
				listj({ params: { table: 'blog', sid } })
					.then((res) => {
						this.fobjList = this.filterBlogsByShop(res, sid)
					})
					.catch(() => {
						this.fobjList = []
					})
			},
			goBack(){
				uni.navigateBack()
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
