<template>

	<view class="blog-list-root">

		<view v-if="shopId != null && shopId !== ''" class="blog-publish-bar">

			<!-- 小程序里 u-button 的 @click 常不触发或先于校验就跳转，改用原生 @tap 做订单校验 -->

			<view class="publish-btn" hover-class="publish-btn-hover" @tap.stop="onPublishDynamic">

				<text class="publish-btn-text">发布动态</text>

			</view>

		</view>



		<block v-for="(item,index) in dataList" :key="item.id">

			<view class="blog_item" @tap="fatherFun(item.id)">

				<view class="blog_top">

					<view class="blog_avatar">

						<image class="blogavatar" :src="fileUrl+item.uimg"></image>

					</view>

					<view class="blog_user">

						<text class="busername">{{item.username}}</text>

						<text class="busernote">{{item.ndate}}</text>

					</view>

				</view>

				<view class="blog_note">

					{{item.title}}

				</view>

				<view v-if="item.img">

					<image class="blog_cover" mode="widthFix" :src="fileUrl+item.img"></image>

				</view>

				<view class="blog_op">

					<text>{{item.zan}}赞</text>

				</view>

			</view>

		</block>



		<u-popup :show="noOrderPopup" mode="bottom" round="20" @close="noOrderPopup = false">

			<view class="no-order-box">

				<view class="no-order-title">提示</view>

				<view class="no-order-msg">还没有已完成订单</view>

				<view class="no-order-opt" @tap="goBlogplan">创建探店计划</view>

				<view class="no-order-opt" @tap="goBrowse">再逛逛</view>

				<view class="no-order-cancel" @tap="noOrderPopup = false">关闭</view>

			</view>

		</u-popup>

	</view>

</template>



<script>

	import {

		fileUrl

	} from '@/common/config/api.js';

	import { yewuutil } from '@/common/commontools.js';

	import { mapState } from 'vuex';



	export default {

		name:"blog-list",

		props: {

			dataList: {

				type: Array,

				require: false,

				default: []

			},

			clickItem: {

				type: Function,

				default: null

			},

			/** 传入店铺 id 时显示「发布动态」，并校验该店是否有「已完成」订单 */

			shopId: {

				type: [Number, String],

				default: null

			}

		},

		methods: {

			fatherFun(tid){

				this.$emit('clickItem',tid)

			},

			onPublishDynamic() {

				const user = this.userInfo || (this.$store && this.$store.state && this.$store.state.userInfo)

				if (!user || !user.id) {

					uni.showToast({ title: '请先登录', icon: 'none' })

					return

				}

				const sid = this.shopId

				if (sid == null || sid === '') return

				uni.showLoading({ title: '校验中...', mask: true })

				yewuutil.hasCompletedOrderForShop(user.id, sid).then(ok => {

					uni.hideLoading()

					if (!ok) {

						this.noOrderPopup = true

						return

					}

					uni.navigateTo({

						url: '/pages/blog/blogmg?pid=' + sid,

						fail: () => {

							uni.showToast({ title: '打开失败', icon: 'none' })

						}

					})

				}).catch(() => {

					uni.hideLoading()

					uni.showToast({ title: '校验失败，请重试', icon: 'none' })

				})

			},

			goBlogplan() {

				this.noOrderPopup = false

				const sid = this.shopId

				if (sid == null || sid === '') return

				uni.navigateTo({ url: '/pages/blog/blogplan?pid=' + sid })

			},

			goBrowse() {

				this.noOrderPopup = false

				const sid = this.shopId

				if (sid) {

					uni.navigateTo({ url: '/pages/good/goodview?sid=' + sid })

				} else {

					uni.switchTab({ url: '/pages/index/index' })

				}

			}

		},

		data() {

			return {

				fileUrl: fileUrl,

				noOrderPopup: false

			};

		},

		computed: {

			...mapState(['userInfo'])

		}

	}

</script>



<style>



.blog-publish-bar {

	display: flex;

	justify-content: flex-end;

	padding: 0 10upx 20upx 0;

}

.publish-btn {

	padding: 16rpx 36rpx;

	border-radius: 999rpx;

	background: linear-gradient(135deg, #ff943c, #ffb366);

	box-shadow: 0 6rpx 16rpx rgba(255, 148, 60, 0.35);

}

.publish-btn-hover {

	opacity: 0.88;

}

.publish-btn-text {

	font-size: 28rpx;

	color: #fff;

	font-weight: bold;

}



.blog_top{

	display: flex;

	margin-bottom: 20upx;

}

.blog_avatar{

	width: 100upx;

}

.blog_user{

	flex: 1;

	display: flex;

	flex-direction: column;

	justify-content: center;

}

.blogavatar{

	width: 80upx;

	height: 80upx;

}

.busername{

	font-size: 28upx !important;

	color: #222;

}

.busernote{

	font-size: 26upx;

	color: #666;

}

.blog_note{

	padding: 0 0 20upx 10upx;

}

.blog_cover{

	width: 100%;

}

.blog_op{

	padding: 20upx 0 20upx 0;

	

}

.blog_op text{

	color: red;

	font-size: 30upx;

}



.blog_item{

	border-bottom: 1px #eee solid;

	margin-bottom: 20upx;

}



.no-order-box {

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

