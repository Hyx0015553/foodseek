<template>
	<view class="template-about tn-safe-area-inset-bottom">
		<!-- 顶部自定义导航 -->
		<!-- <tn-nav-bar fixed alpha customBack>
      <view slot="back" class='tn-custom-nav-bar__back'
        @click="goBack">
        <text class='icon tn-icon-left'></text>
        <text class='icon tn-icon-home-capsule-fill'></text>
      </view>
    </tn-nav-bar> -->




		<view class="top-backgroup">
			<image src='http://ideapic-1255600738.cos.ap-guangzhou.myqcloud.com/image/login/1/login_top2.jpg' mode='widthFix' class='backgroud-image'></image>
		</view>

		<view class="tnwave waveAnimation">
			<view class="waveWrapperInner bgTop">
				<view class="wave waveTop" style="background-image: url('../../static/image/wave/wave-2.png')"></view>
			</view>
			<view class="waveWrapperInner bgMiddle">
				<view class="wave waveMiddle" style="background-image: url('../../static/image/wave/wave-2.png')">
				</view>
			</view>
			<view class="waveWrapperInner bgBottom">
				<view class="wave waveBottom" style="background-image: url('../../static/image/wave/wave-1.png')">
				</view>
			</view>
		</view>

		<view class="about__wrap">
			<!-- 头像用户信息 -->
			<view @click="toLogin" class="user-info__container tn-flex tn-flex-direction-column tn-flex-col-center tn-flex-row-center">
				<view class="user-info__avatar tn-bg-grey--light tn-flex tn-flex-col-center tn-flex-row-center">
					<view class="tn-shadow-blur myphoto" :style="{background:'url('+fileUrl+userInfo.img+')'}">
					</view>
					<!-- <view class="tn-icon-logo-tuniao" style="font-size: 140rpx;color: #01BEFF;"></view> -->
				</view>
				<view class="user-info__nick-name">{{userInfo.username}}</view>
				<view  v-if="!userInfo.id" url="/pages/login/login">
				    点击登录
				</view>
			</view>



			<!-- 消息&数据 -->
			<view class="tn-shadow-warp" style="margin-top: 120rpx;background-color: rgba(255,255,255,1);">
				<view class="tn-flex">
					<view @click="toQianbao()" class="tn-flex-1 tn-padding-sm tn-margin-xs">
						<view class="tn-flex tn-flex-direction-column tn-flex-row-center tn-flex-col-center">
							<view class="">
								<view class="tn-text-xxl tn-color-orange">¥:{{userInfo.money||0}}</view>
							</view>
							<view class="tn-margin-top-xs tn-color-gray tn-text-df tn-text-center">
								<text class="tn-icon-fire"></text>
								<text class="tn-padding-left-xs"></text>
							</view>
						</view>
					</view>
					<view @click="toBillqs" v-if="userInfo.roletype==4" class="tn-flex-1 tn-padding-sm tn-margin-xs">
						<view class="tn-flex tn-flex-direction-column tn-flex-row-center tn-flex-col-center">
							<view class="">
								<view class="tn-text-xxl tn-color-blue">{{jiedan}}</view>
							</view>
							<view class="tn-margin-top-xs tn-color-gray tn-text-df tn-text-center">
								<text class="tn-icon-share-circle"></text>
								<text class="tn-padding-left-xs"></text>
							</view>
						</view>
					</view>
					<view @click="toMyFavs" class="tn-flex-1 tn-padding-sm tn-margin-xs">
						<view class="tn-flex tn-flex-direction-column tn-flex-row-center tn-flex-col-center">
							<view class="">
								<view class="tn-text-xxl tn-color-red">{{favcount}}</view>
							</view>
							<view class="tn-margin-top-xs tn-color-gray tn-text-df tn-text-center">
								<text class="tn-icon-like"></text>
								<text class="tn-padding-left-xs">我的收藏</text>
							</view>
						</view>
					</view>
				</view>
			</view>

			<view style="margin-top: 50rpx;" v-if="userInfo.roletype=='3'" class="about-shadow tn-margin-top-xl tn-padding-top-sm tn-padding-bottom-sm tn-margin-left tn-margin-right">
				<!-- 方式12 start-->
				<view class="tn-flex tn-flex-row-center tn-radius">
					<view @click="toMgGood()" class="tn-padding-sm tn-margin-xs tn-radius">
						<view class="tn-flex tn-flex-direction-column tn-flex-row-center tn-flex-col-center">
							<view class="icon12__item--icon tn-flex tn-flex-row-center tn-flex-col-center">
								<view class="tn-icon-vip"></view>
							</view>
							<view class="tn-text-center">
								<text class="tn-text-ellipsis"></text>
							</view>
						</view>
					</view>
					<view class="tn-padding-sm tn-margin-xs tn-radius">
						<navigator url="/pages/bill/billshop">
							<view class="tn-flex tn-flex-direction-column tn-flex-row-center tn-flex-col-center">
								<view class="icon12__item--icon tn-flex tn-flex-row-center tn-flex-col-center">
									<view class="tn-icon-airplane"></view>
								</view>
								<view class="tn-text-center">
									<text class="tn-text-ellipsis"></text>
								</view>
							</view>
						</navigator>
					</view>
					<!--<view @click="toJianView()" class="tn-padding-sm tn-margin-xs tn-radius">
						<view class="tn-flex tn-flex-direction-column tn-flex-row-center tn-flex-col-center">
							<view class="icon12__item&#45;&#45;icon tn-flex tn-flex-row-center tn-flex-col-center">
								<view class="tn-icon-ticket"></view>
							</view>
							<view class="tn-text-center">
								<text class="tn-text-ellipsis"></text>
							</view>
						</view>
					</view>-->
					<view class="tn-padding-sm tn-margin-xs tn-radius">
						<navigator url="/pages/shops/shopmg">
							<view class="tn-flex tn-flex-direction-column tn-flex-row-center tn-flex-col-center">
								<view class="icon12__item--icon tn-flex tn-flex-row-center tn-flex-col-center">
									<view class="tn-icon-assort"></view>
								</view>
								<view class="tn-text-center">
									<text class="tn-text-ellipsis"></text>
								</view>
							</view>
						</navigator>
					</view>
				</view>

				<!-- 方式12 end-->
			</view>

			<!-- 更多信息-->
			<view style="margin-top: 50upx;"
				class="about-shadow tn-margin-top-xl tn-padding-top-sm tn-padding-bottom-sm tn-margin-left tn-margin-right">
				<tn-list-cell @click="toUserDetail" :hover="true" :unlined="true" :radius="true" :fontSize="30">
					<view class="tn-flex tn-flex-col-center">
						<view
							class="icon1__item--icon tn-flex tn-flex-row-center tn-flex-col-center tn-cool-bg-color-5 tn-color-white">
							<view class="tn-icon-moon"></view>
						</view>
						<view class="tn-margin-left-sm tn-flex-1">个人信息</view>
						<view class="tn-margin-left-sm tn-color-cyan tn-icon-link"></view>
					</view>
				</tn-list-cell>
				<tn-list-cell @click="toYouhuiquan" :hover="true" :unlined="true" :radius="true" :fontSize="30">
					<view class="tn-flex tn-flex-col-center">
						<view
							class="icon1__item--icon tn-flex tn-flex-row-center tn-flex-col-center tn-cool-bg-color-8 tn-color-white">
							<view class="tn-icon-message-fill"></view>
						</view>
						<view class="tn-margin-left-sm tn-flex-1">优惠券</view>
						<view class="tn-margin-left-sm tn-color-blue tn-icon-copy-fill"></view>
					</view>
				</tn-list-cell>
				<tn-list-cell @click="toAddress" :hover="true" :unlined="true" :radius="true" :fontSize="30">
					<view class="tn-flex tn-flex-col-center">
						<view
							class="icon1__item--icon tn-flex tn-flex-row-center tn-flex-col-center tn-cool-bg-color-1 tn-color-white">
							<view class="tn-icon-like-fill"></view>
						</view>
						<view class="tn-margin-left-sm tn-flex-1"></view>
						<!-- <view class="tn-margin-left-sm tn-color-blue tn-icon-copy-fill"></view> -->
					</view>
				</tn-list-cell>
				<!-- <tn-list-cell @click="toMgGood" :hover="true" :unlined="true" :radius="true" :fontSize="30">
					<view class="tn-flex tn-flex-col-center">
						<view
							class="icon1__item--icon tn-flex tn-flex-row-center tn-flex-col-center tn-cool-bg-color-2 tn-color-white">
							<view class="tn-icon-set-fill"></view>
						</view>
						<view class="tn-margin-left-sm tn-flex-1"></view>
					</view>
				</tn-list-cell> -->
			</view>

			<!-- 更多信息-->
			<view
				class="about-shadow tn-margin-top-xl tn-padding-top-sm tn-padding-bottom-sm tn-margin-left tn-margin-right">

				<tn-list-cell @click="toPostsList" :hover="true" :unlined="true" :radius="true" :fontSize="30">
					<view class="tn-flex tn-flex-col-center">
						<view
							class="icon1__item--icon tn-flex tn-flex-row-center tn-flex-col-center tn-cool-bg-color-16 tn-color-white">
							<view class="tn-icon-wechat"></view>
						</view>
						<view class="tn-margin-left-sm tn-flex-1">我的发帖</view>
						<view class="tn-margin-left-sm tn-color-cyan tn-icon-link"></view>
					</view>
				</tn-list-cell>
				<tn-list-cell :hover="true" :unlined="true" :radius="true" :fontSize="30">
          <navigator url="/pages/chat/huihua">
            <view class="tn-flex tn-flex-col-center">
              <view
                  class="icon1__item--icon tn-flex tn-flex-row-center tn-flex-col-center tn-cool-bg-color-16 tn-color-white">
                <view class="tn-icon-like-fill"></view>
              </view>
              <view class="tn-margin-left-sm tn-flex-1">我的消息</view>
              <view class="tn-margin-left-sm tn-color-cyan tn-icon-link"></view>
            </view>
          </navigator>

				</tn-list-cell>

				
				<tn-list-cell @click="toHistory" :hover="true" :unlined="true" :radius="true" :fontSize="30">
					<view class="tn-flex tn-flex-col-center">
						<view
							class="icon1__item--icon tn-flex tn-flex-row-center tn-flex-col-center tn-cool-bg-color-6 tn-color-white">
							<view class="tn-icon-phone-fill"></view>
						</view>
						<view class="tn-margin-left-sm tn-flex-1">历史浏览</view>
						<view
							class="tn-margin-left-sm tn-color-orangered tn-text-sm tn-padding-left-xs tn-padding-right-xs tn-bg-orange--disabled tn-round">
						</view>
					</view>
				</tn-list-cell>
				<tn-list-cell @click="logOut" :hover="true" :unlined="true" :radius="true" :fontSize="30">
					<view class="tn-flex tn-flex-col-center">
						<view
							class="icon1__item--icon tn-flex tn-flex-row-center tn-flex-col-center tn-cool-bg-color-12 tn-color-white">
							<view class="tn-icon-set-fill"></view>
						</view>
						<view class="tn-margin-left-sm tn-flex-1">退出登录</view>
						<view class="tn-margin-left-sm tn-color-red tn-icon-fire-fill"></view>
					</view>
				</tn-list-cell>
			</view>

		</view>
		<view class="tn-padding-xl"></view>

	</view>
</template>

<script>
	import {
		listj,
		findj,
		fileUrl
	} from '@/common/config/api.js'
	import {
		mapState,
		mapActions
	} from 'vuex'
	import {
		ideautil,
		yewuutil
	} from '@/common/commontools.js'
	export default {
		data() {
			return {
				fileUrl: fileUrl,
				favcount:0,
				jiedan:0
			};
		},
		onLoad(params) {
			
			
		},
		onShow(){
			if (this.userInfo.roletype == "4") {
				listj({params: {table: 'bill', qid: this.userInfo.id }}).then(res => {
					let list = res || []
					this.jiedan = list.length
				})
			}
			let favs = this.userInfo.favs||""
			if (favs && favs!="0") {
				let favids = favs.split(",")
				console.log(favids)
				this.favcount = favids.length || 0
			}else{
				this.favcount = 0
			}
			
		},
		methods: {
			...mapActions(['updateUserInfo', 'logout']),
			toBlog: yewuutil.toBlog,
			toPosts: yewuutil.toPosts,
			toBlogList: yewuutil.toBlogList,
			toHistory: yewuutil.toHistory,
			toMyFriend: yewuutil.toMyFriend,
			toYzMessage:yewuutil.toYzMessage,
			toHuihua: yewuutil.toHuihua,
			toMyFavs: yewuutil.toMyFavs,
			toBillqs: yewuutil.toBillqs,
			toMyBill: yewuutil.toMyBill,
			toLogin(){
				if (!this.userInfo || !this.userInfo.id) {
					uni.ytool.toLogin()
				}

			},
			toQianbao(){
				uni.navigateTo({
					url:'/pages/qianbao/qianbao'
				})
			},
			toPostsList() {
				uni.navigateTo({
					url: '/pages/posts/postslist'
				})
			},
			goBack() {
				uni.navigateBack({
					delta: 1
				})
			},
			toUserDetail() {
				uni.navigateTo({
					url: '/pages/me/medetail'
				})
			},
			toBill() {
				uni.switchTab({
					url: '/pages/bill/bill'
				})
			},
			toAddress() {
				uni.navigateTo({
					url: '/pages/address/address'
				})
			},
			toMgGood() {
				uni.navigateTo({
					url: '/pages/good/good'
				})
			},
			logOut() {
				this.updateUserInfo({})
				yewuutil.toLogin()
			},
			toYouhuiquan() {
				uni.navigateTo({
					url: '/pages/youhuiquan/youhuiquan'
				})
			},
			toQunzu(){
				uni.navigateTo({
					url: '/pages/qunzu/qunzu'
				})
			},
			toQunzuList(){
				uni.navigateTo({
					url:'/pages/qunzu/qunzulist'
				})
			}
		},
		computed: {
			...mapState(['userInfo'])
		}
	}
</script>

<style lang="scss" scoped>
	@import '@/static/css/templatePage/custom_nav_bar.scss';

	.template-about {}

	/* 顶部背景图 start */
	.top-backgroup {
		height: 450rpx;
		z-index: -1;

		.backgroud-image {
			width: 100%;
			height: 450rpx;
			// z-index: -1;
		}
	}

	/* 顶部背景图 end */

	/* 波浪*/
	@keyframes move_wave {
		0% {
			transform: translateX(0) translateZ(0) scaleY(1)
		}

		50% {
			transform: translateX(-25%) translateZ(0) scaleY(1)
		}

		100% {
			transform: translateX(-50%) translateZ(0) scaleY(1)
		}
	}

	.tnwave {
		overflow: hidden;
		position: absolute;
		z-index: 9999;
		height: 200rpx;
		left: 0;
		right: 0;
		top: 290rpx;
		bottom: auto;
	}

	.waveWrapperInner {
		position: absolute;
		width: 100%;
		overflow: hidden;
		height: 100%;
	}

	.wave {
		position: absolute;
		left: 0;
		width: 200%;
		height: 100%;
		background-repeat: repeat no-repeat;
		background-position: 0 bottom;
		transform-origin: center bottom;
	}

	.myphoto {
		width: 170rpx;
		height: 170rpx;
		background-size: 100% !important;
		background-repeat: no-repeat !important;
	}

	.bgTop {
		opacity: 0.4;
	}

	.waveTop {
		background-size: 50% 45px;
	}

	.waveAnimation .waveTop {
		animation: move_wave 4s linear infinite;
	}

	.bgMiddle {
		opacity: 0.6;
	}

	.waveMiddle {
		background-size: 50% 40px;
	}

	.waveAnimation .waveMiddle {
		animation: move_wave 3.5s linear infinite;
	}

	.bgBottom {
		opacity: 0.95;
	}

	.waveBottom {
		background-size: 50% 35px;
	}

	.waveAnimation .waveBottom {
		animation: move_wave 2s linear infinite;
	}

	/* 波浪*/

	/* 页面 start*/
	.about-shadow {
		border-radius: 15rpx;
		box-shadow: 0rpx 0rpx 50rpx 0rpx rgba(0, 0, 0, 0.07);
	}

	.about {

		&__wrap {
			position: relative;
			z-index: 1;
			// margin: 20rpx 30rpx;
			margin-top: -330rpx;
		}
	}

	/* 页面 end*/

	/* 用户信息 start */
	.user-info {
		&__container {}

		&__avatar {
			width: 170rpx;
			height: 170rpx;
			border: 8rpx solid rgba(255, 255, 255, 0.05);
			border-radius: 50%;
			overflow: hidden;
			box-shadow: 0rpx 0rpx 80rpx 0rpx rgba(0, 0, 0, 0.15);
		}

		&__nick-name {
			margin-top: 26rpx;
			font-size: 36rpx;
			font-weight: 600;
			text-align: center;
		}
	}

	/* 用户信息 end */
	/* 图标容器1 start */
	.icon1 {
		&__item {
			// width: 30%;
			background-color: #FFFFFF;
			border-radius: 10rpx;
			padding: 30rpx;
			margin: 20rpx 10rpx;
			transform: scale(1);
			transition: transform 0.3s linear;
			transform-origin: center center;

			&--icon {
				width: 40rpx;
				height: 40rpx;
				font-size: 28rpx;
				border-radius: 50%;
				position: relative;
				z-index: 1;

				&::after {
					content: " ";
					position: absolute;
					z-index: -1;
					width: 100%;
					height: 100%;
					left: 0;
					bottom: 0;
					border-radius: inherit;
					opacity: 1;
					transform: scale(1, 1);
					background-size: 100% 100%;
					background-image: url(http://ideapic-1255600738.cos.ap-guangzhou.myqcloud.com/image/cool_bg_image/icon_bg.png);
				}
			}
		}
	}

	/* 图标容器1 end */

	/* 底部tabbar start*/
	.footerfixed {
		position: fixed;
		width: 100%;
		bottom: 0;
		z-index: 999;
		background-color: #FFFFFF;
		box-shadow: 0rpx 0rpx 30rpx 0rpx rgba(0, 0, 0, 0.07);
	}

	.tabbar {
		display: flex;
		align-items: center;
		min-height: 110rpx;
		justify-content: space-between;
		padding: 0;
		height: calc(110rpx + env(safe-area-inset-bottom) / 2);
		padding-bottom: calc(env(safe-area-inset-bottom) / 2);
	}

	.tabbar .action {
		font-size: 22rpx;
		position: relative;
		flex: 1;
		text-align: center;
		padding: 0;
		display: block;
		height: auto;
		line-height: 1;
		margin: 0;
		overflow: initial;
	}

	.tabbar .action .bar-icon {
		width: 100rpx;
		position: relative;
		display: block;
		height: auto;
		margin: 0 auto 10rpx;
		text-align: center;
		font-size: 42rpx;
	}

	.tabbar .action .bar-icon image {
		width: 50rpx;
		height: 50rpx;
		display: inline-block;
	}


	.icon12 {
		&__item {
			width: 30%;
			background-color: #FFFFFF;
			border-radius: 10rpx;
			padding: 30rpx;
			margin: 20rpx 10rpx;
			transform: scale(1);
			transition: transform 0.3s linear;
			transform-origin: center center;

			&--icon {
				width: 100rpx;
				height: 100rpx;
				font-size: 60rpx;
				// border-radius: 50%;
				margin-bottom: 0rpx;
				position: relative;
				z-index: 1;

				&::after {
					content: " ";
					position: absolute;
					z-index: -1;
					width: 100%;
					height: 100%;
					left: 0;
					bottom: 0;
					border-radius: inherit;
					opacity: 1;
					transform: scale(1, 1);
					background-size: 100% 100%;
					// background-image: url(https://tnuiimage.tnkjapp.com/cool_bg_image/icon_bg.png);


				}
			}
		}
	}
</style>
