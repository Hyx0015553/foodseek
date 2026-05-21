<template>
	<view class="index-page">
    <!-- 自定义导航栏 -->
		<u-navbar title="首页" leftIconSize="0" :border="false" :placeholder="true" :autoBack="false" bgColor="transparent" titleStyle="font-weight: 800; font-size: 34rpx; color: #333;"></u-navbar>
		
    <scroll-view scroll-y :enable-flex="true" class="svcontainer">
      
      <!-- 顶部背景装饰 -->
      <view class="top-decoration"></view>

      <!-- 顶部入口（原 fs_notice 轮播已移除） -->
      <view class="swiper-section">
        <view class="hero-banner" @click="navTo('/pages/blog/bloglist')">
          <view class="hero-banner-inner">
            <text class="hero-banner-title">探店精选</text>
            <text class="hero-banner-sub">查看全部动态</text>
          </view>
        </view>
      </view>

      <!-- 功能菜单区域 -->
<!--			<view class="gridmenu-card">
				<view class="gridmenuitem" @click="navTo('/pages/blog/blogtuijian')">
					<view class="icon-box icon-bg-1">
            <text class="emoji-icon">🔥</text>
          </view>
					<text class="gridmenutitle">智能推荐</text>
				</view>

				<view class="gridmenuitem" @click="navTo('/pages/favs/favs')">
					<view class="icon-box icon-bg-2">
            <text class="emoji-icon">⭐</text>
          </view>
					<text class="gridmenutitle">我的收藏</text>
				</view>
        
        <view class="gridmenuitem" @click="navTo('/pages/posts/posts')">
          <view class="icon-box icon-bg-3">
            <text class="emoji-icon">💬</text>
          </view>
          <text class="gridmenutitle">行程规划</text>
        </view>
        reque
				<view class="gridmenuitem" @click="navTo('/pages/blog/blogmg?from=index')">
					<view class="icon-box icon-bg-4">
            <text class="emoji-icon">✨</text>
          </view>
					<text class="gridmenutitle">发布动态</text>
				</view>
			</view>-->

      <!-- 推荐内容区域 -->
      <view class="content-section">
        <view class="section-header">
          <view class="title-left">
            <text class="section-title">精选推荐</text>
            <text class="section-subtitle">Discover</text>
          </view>
          <view class="tabs-wrapper">
            <u-tabs :list="typelist2" keyName="title" @change="refreshpx" lineColor="#ff5e62" :activeStyle="{color: '#ff5e62', fontWeight: 'bold', transform: 'scale(1.05)'}" :inactiveStyle="{color: '#999', fontSize: '26rpx'}" itemStyle="padding-left: 15px; padding-right: 15px; height: 34px;"></u-tabs>
          </view>

        </view>
        <view class="tabs-wrapper" style="margin-bottom: 20rpx;">
          <u-tabs :list="typelist" keyName="title" @change="refreshShow" lineColor="#ff5e62" :activeStyle="{color: '#ff5e62', fontWeight: 'bold', transform: 'scale(1.05)'}" :inactiveStyle="{color: '#999', fontSize: '26rpx'}" itemStyle="padding-left: 15px; padding-right: 15px; height: 34px;"></u-tabs>
        </view>
        <view class="blog-list">
          <view class="blog-item" v-for="(item, index) in fobjList" :key="index" @click="toBlogDetail(item.id)">
            <!-- 头部：作者信息 -->
            <view class="item-header">
              <view class="author-info">
                <u-avatar :src="fileUrl+item.uimg" size="24" :text="item.username ? item.username.substring(0,1) : 'U'" fontSize="14" randomBgColor></u-avatar>
                <text class="author-name">{{item.username}}</text>
              </view>
              <text class="publish-time">{{item.ndate}}</text>
            </view>
            
            <!-- 内容：标题与封面 -->
            <view class="item-body">
              <view class="text-content" :class="{'has-image': item.img}">
                <text class="item-title u-line-2">{{item.title}}</text>
                <text class="item-desc u-line-2" v-if="!item.img">{{item.note | filterHtml}}</text>
              </view>
              <view class="image-content" v-if="item.img">
                <image :src="fileUrl+item.img" mode="aspectFill" class="cover-img"></image>
              </view>
            </view>
            
            <!-- 底部：数据统计 -->
            <view class="item-footer">
              <view class="stat-group">
                <view class="stat-item">
                  <u-icon name="eye" size="16" color="#999"></u-icon>
                  <text class="stat-num">{{item.vcount || 0}}</text>
                </view>
                <view class="stat-item">
                  <u-icon name="thumb-up" size="16" color="#999"></u-icon>
                  <text class="stat-num">{{item.zan || 0}}</text>
                </view>
                <view class="stat-item">
                  <u-icon name="chat" size="16" color="#999"></u-icon>
                  <text class="stat-num">{{item.pinglun || 0}}</text>
                </view>
              </view>
              <view class="read-more">
                <text>阅读全文</text>
                <u-icon name="arrow-right" size="12" color="#ff9966"></u-icon>
              </view>
            </view>
          </view>
          
          <!-- 空状态 -->
          <view v-if="fobjList.length === 0" class="empty-box">
             <u-empty mode="data" icon="http://cdn.uviewui.com/uview/empty/data.png" text="暂无相关内容"></u-empty>
          </view>
        </view>
      </view>

		</scroll-view>
	</view>
</template>

<script>
	import { listSqlj, fileUrl } from '@/common/config/api.js';
	import {mapState, mapActions} from 'vuex';
	import { ideautil, yewuutil } from '@/common/commontools.js';
  
	export default {
		data() {
			return {
				fobjList: [],
				typelist: [{id:0,title:'公开'},{id:1,title:'好友'},{id:2,title:'阅后即焚'}],
				fileUrl:fileUrl,
        typelist2:[{id:0,title:'热门'},{id:2,title:'最新'},{id:3,title:'点击量'}],
        pxtype:0,
        stype:0
			};
		},
    filters: {
      filterHtml(val) {
        if (!val) return '';
        return val.replace(/<[^>]+>/g, "").substring(0, 50);
      }
    },
		components:{},
		onLoad(params) {
			/*listj({params: {table: 'type'}}).then(res => {
				this.typelist = res
				this.typelist.unshift({id:0,title:"全部"})
			})*/
		},
		onShow() {
			this.refreshObj()
		},
		methods: {
			...mapActions(['updateUserInfo']),
			toBlogDetail: yewuutil.toBlogDetail,
      
      navTo(url) {
        uni.navigateTo({
          url: url
        })
      },
			
			fobjMg(id){
				uni.itool.nto({
					url:'/pages/blog/blogmg?tid='+id
				})
			},
      refreshObj1(index){
        this.typeid = null
        if(index){
          this.typeid = index.id==0?null:index.id
        }
        this.refreshObj()
      },

      refreshShow(index){
        this.stype = null
        if(index){
          this.stype = index.id
        }
        this.refreshObj()
      },

      refreshpx(index){
        this.pxtype = index.id
        let res = this.fobjList
        if(this.pxtype == 0){
          res = res.sort((o1,o2)=>{
            return o2.zan-o1.zan
          })
        }else if(this.pxtype == 1){
          res = res.sort((o1,o2)=>{
            return o2.zishu-o1.zishu
          })
        }else if(this.pxtype == 2){
          res = res.sort((o1,o2)=>{
            return o2.gxtime-o1.gxtime
          })
        }else if(this.pxtype == 3){
          res = res.sort((o1,o2)=>{
            return o2.vcount-o1.vcount
          })
        }
        this.fobjList = res
      },
			refreshObj(){
				let sql = `select b.*,(select count(pid) from fs_replay r where r.pid=b.id) pinglun,u.img uimg from fs_blog b left join fs_user u on u.id=b.uid`
        if(this.stype=='1'){
          sql = `SELECT b.*, u.id as user_id, u.fids FROM fs_blog b INNER JOIN fs_user u ON FIND_IN_SET(b.uid, u.fids) > 0 WHERE b.vtype='好友可见' and u.id = ${this.userInfo.id} ORDER BY b.zan DESC`
        }else if(this.stype=='0'){
          sql = `select b.*,(select count(pid) from fs_replay r where r.pid=b.id) pinglun,u.img uimg from fs_blog b left join fs_user u on u.id=b.uid where b.vtype='公开'`
        }else if(this.stype=='2'){
          sql = `SELECT b.*,(SELECT COUNT(pid) FROM fs_replay r WHERE r.pid = b.id) pinglun,u.img uimg
                 FROM fs_blog b
                        LEFT JOIN fs_user u ON u.id = b.uid
                 WHERE b.vtype='阅后即焚' AND NOT EXISTS (SELECT 1
                                   FROM fs_looked l
                                   WHERE l.bid = b.id AND l.uid = ${this.userInfo.id})`
        }
				/*if (this.typeid) {
					sql = `select b.*,(select count(pid) from fs_replay r where r.pid=b.id) pinglun,u.img uimg from fs_blog b left join fs_user u on u.id=b.uid where typeid=${this.typeid}`
				}*/
				listSqlj({params: {sql: sql}}).then(res => {
          for (let s of res) {
              s.zan = s.zan*1 || 0
              s.gxtime = uni.itool.str2Date(s.ndate).getTime()
          }
					this.fobjList = res
				})
			},
			
			goBack(){
				uni.navigateBack({
					delta:1
				})
			},
		},
		computed: {
			...mapState(['userInfo'])
		}
	}
</script>

<style lang="scss" scoped>
.index-page {
  background-color: #f7f8fa;
  min-height: 100vh;
  position: relative;
}

.svcontainer {
  height: calc(100vh - 88rpx);
}

.top-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 400rpx;
  background: linear-gradient(180deg, #fff0f0 0%, rgba(255,255,255,0) 100%);
  z-index: 0;
  pointer-events: none;
}

/* 顶部横幅（替代公告轮播） */
.swiper-section {
  padding: 20rpx 30rpx;
  position: relative;
  z-index: 1;

  .hero-banner {
    height: 340rpx;
    border-radius: 24rpx;
    overflow: hidden;
    box-shadow: 0 10rpx 30rpx rgba(255, 94, 98, 0.15);
    background: linear-gradient(135deg, #ff8f3d 0%, #ff5e62 100%);
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .hero-banner-inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12rpx;
  }
  .hero-banner-title {
    color: #fff;
    font-size: 36rpx;
    font-weight: 700;
  }
  .hero-banner-sub {
    color: rgba(255,255,255,0.9);
    font-size: 26rpx;
  }
  
  .main-swiper {
    height: 340rpx;
    border-radius: 24rpx;
    overflow: hidden;
    box-shadow: 0 10rpx 30rpx rgba(255, 94, 98, 0.15);
    transform: translateZ(0); // Fix IOS border-radius bug
  }
  
  .swiper-item-inner {
    width: 100%;
    height: 100%;
    position: relative;
    
    .swiperimg {
      width: 100%;
      height: 100%;
    }
    
    .swiper-mask {
      position: absolute;
      bottom: 0;
      left: 0;
      width: 100%;
      height: 120rpx;
      background: linear-gradient(to top, rgba(0,0,0,0.6), transparent);
    }
    
    .swiper-title {
      position: absolute;
      bottom: 30rpx;
      left: 30rpx;
      color: #fff;
      font-size: 32rpx;
      font-weight: 600;
      z-index: 2;
      text-shadow: 0 2rpx 4rpx rgba(0,0,0,0.3);
    }
  }
}

/* 宫格菜单 */
.gridmenu-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #ffffff;
  margin: 10rpx 30rpx 30rpx;
  padding: 30rpx 20rpx;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.03);
  position: relative;
  z-index: 1;
}

.gridmenuitem {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  
  .icon-box {
    width: 96rpx;
    height: 96rpx;
    border-radius: 30rpx;
    display: flex;
    justify-content: center;
    align-items: center;
    margin-bottom: 16rpx;
    transition: all 0.2s;
    
    .emoji-icon {
      font-size: 48rpx;
    }
    
    &:active {
      transform: scale(0.92);
    }
  }
  
  .gridmenutitle {
    font-size: 26rpx;
    color: #333;
    font-weight: 500;
  }
  
  .icon-bg-1 { background-color: #fff0f0; } // Warm Red
  .icon-bg-2 { background-color: #fff7e6; } // Warm Orange/Yellow
  .icon-bg-3 { background-color: #e6fffb; } // Teal
  .icon-bg-4 { background-color: #f0f5ff; } // Blue
}

/* 推荐内容 */
.content-section {
  background-color: #f7f8fa;
  padding: 0 30rpx 40rpx;
  position: relative;
  z-index: 1;
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-bottom: 24rpx;
    
    .title-left {
      display: flex;
      flex-direction: column;
      
      .section-title {
        font-size: 36rpx;
        font-weight: 800;
        color: #333;
        position: relative;
        z-index: 1;
        
        &::after {
          content: '';
          position: absolute;
          bottom: 4rpx;
          left: 0;
          width: 100%;
          height: 12rpx;
          background-color: rgba(255, 153, 102, 0.2);
          z-index: -1;
          border-radius: 6rpx;
        }
      }
      
      .section-subtitle {
        font-size: 20rpx;
        color: #ccc;
        letter-spacing: 2rpx;
        margin-top: 4rpx;
        text-transform: uppercase;
      }
    }
    
    .tabs-wrapper {
      width: 480rpx;
    }
  }
}

/* 博客列表项 */
.blog-list {
  .blog-item {
    background-color: #fff;
    border-radius: 24rpx;
    padding: 30rpx;
    margin-bottom: 24rpx;
    box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.02);
    transition: transform 0.2s;
    
    &:active {
      transform: scale(0.99);
    }
    
    .item-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20rpx;
      
      .author-info {
        display: flex;
        align-items: center;
        
        .author-name {
          font-size: 26rpx;
          color: #666;
          margin-left: 12rpx;
          font-weight: 500;
        }
      }
      
      .publish-time {
        font-size: 22rpx;
        color: #ccc;
      }
    }
    
    .item-body {
      display: flex;
      justify-content: space-between;
      margin-bottom: 24rpx;
      
      .text-content {
        flex: 1;
        
        &.has-image {
          margin-right: 24rpx;
        }
        
        .item-title {
          font-size: 32rpx;
          font-weight: bold;
          color: #333;
          line-height: 1.5;
          margin-bottom: 12rpx;
        }
        
        .item-desc {
          font-size: 26rpx;
          color: #999;
          line-height: 1.6;
        }
      }
      
      .image-content {
        .cover-img {
          width: 200rpx;
          height: 140rpx;
          border-radius: 16rpx;
          background-color: #f5f5f5;
        }
      }
    }
    
    .item-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-top: 1rpx solid #f9f9f9;
      padding-top: 20rpx;
      
      .stat-group {
        display: flex;
        
        .stat-item {
          display: flex;
          align-items: center;
          margin-right: 30rpx;
          
          .stat-num {
            font-size: 22rpx;
            color: #999;
            margin-left: 6rpx;
          }
        }
      }
      
      .read-more {
        display: flex;
        align-items: center;
        
        text {
          font-size: 22rpx;
          color: #ff9966;
          margin-right: 4rpx;
        }
      }
    }
  }
  
  .empty-box {
    padding: 60rpx 0;
  }
}
</style>
