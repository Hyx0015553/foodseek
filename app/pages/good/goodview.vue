<template>
	<view class="page-root">
		<u-navbar :title="title" :border="true" :placeholder="true" :autoBack="true"></u-navbar>

		<view class="main-body">
			<!-- 左侧：大类（与 goodfenlei 一致） -->
			<scroll-view scroll-y class="side-bar" :show-scrollbar="false">
				<view
					v-for="item in sidebarCategories"
					:key="item.id"
					class="side-item"
					:class="{ 'side-item-active': activeMainId === item.id }"
					@click="handleMainCategory(item.id)"
				>
					<text class="side-text">{{ item.title }}</text>
					<view class="active-line" v-if="activeMainId === item.id"></view>
				</view>
			</scroll-view>

			<!-- 右侧：价格排序 + 单列列表 -->
			<view class="content-area">
				<view class="sort-bar">
					<view
						class="sort-chip"
						:class="{ 'sort-chip-active': priceSort === 'default' }"
						@click="priceSort = 'default'"
					>综合</view>
					<view
						class="sort-chip"
						:class="{ 'sort-chip-active': priceSort === 'price_desc' }"
						@click="priceSort = 'price_desc'"
					>价格从高到低</view>
					<view
						class="sort-chip"
						:class="{ 'sort-chip-active': priceSort === 'price_asc' }"
						@click="priceSort = 'price_asc'"
					>价格从低到高</view>
				</view>

				<scroll-view scroll-y :enable-flex="true" class="goods-scroll">
					<view class="scroll-inner">
						<view class="goods-list">
							<view
								class="goods-item"
								v-for="item in displayGoods"
								:key="item.id"
								@click="toGoodDetail(item.id)"
							>
								<view class="goods-img-box">
									<image v-if="item.img" class="goods-img" :src="fileUrl+item.img" mode="aspectFill"></image>
									<view v-else class="goods-img-placeholder">🍲</view>
								</view>
								<view class="goods-info">
									<text class="goods-name">{{ item.gname }}</text>
									<view class="goods-bottom">
										<text class="goods-price">¥{{ item.price }}</text>
										<view class="stepper-wrap">
											<view
												v-if="cartCount(item.id) === 0"
												class="add-btn-only"
												@click.stop="addToCart(item)"
											>
												<text class="add-icon">+</text>
											</view>
											<view v-else class="stepper" @tap.stop="noop">
												<view class="stepper-minus" @click.stop="changeQty(item, -1)">
													<text class="minus-icon">−</text>
												</view>
												<text class="stepper-num">{{ cartCount(item.id) }}</text>
												<view class="stepper-plus-box" @click.stop="changeQty(item, 1)">
													<text class="stepper-plus">+</text>
												</view>
											</view>
										</view>
									</view>
								</view>
							</view>
						</view>
						<view class="empty-tip" v-if="fobjList.length === 0">
							<text>暂无菜品</text>
						</view>
						<view class="empty-tip" v-else-if="displayGoods.length === 0">
							<text>当前分类下暂无菜品</text>
						</view>
						<view class="scroll-bottom-spacer"></view>
					</view>
				</scroll-view>
			</view>
		</view>

		<!-- 底部购物车栏：始终显示 -->
		<view class="cart-bar">
			<view class="cart-info" @click="showCartDetail">
				<view class="cart-icon-box">
					<text class="cart-icon">🛒</text>
					<view class="cart-badge" v-if="cartTotalQty > 0">{{ cartTotalQty }}</view>
				</view>
				<view class="cart-text">
					<text class="cart-label">购物车</text>
					<text class="cart-count">{{ cartTotalQty > 0 ? cartTotalQty + '件商品' : '暂无商品' }}</text>
				</view>
			</view>
			<view
				class="checkout-btn"
				:class="{ 'checkout-btn-disabled': cartTotalQty === 0 }"
				@click="toCheckout"
			>
				<text>去结账</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { listj, fileUrl } from '@/common/config/api.js';
	import {mapState, mapActions} from 'vuex';
	import { yewuutil } from '@/common/commontools.js';
	export default {
		data() {
			return {
				fobjList:[],
				title:"菜品列表",
				fileUrl: fileUrl,
				activeMainId: 0,
				priceSort: 'default'
			};
		},
		onLoad(params) {
			let sid = params.sid || null
			let stype = params.stype || null
			let ppid = params.ppid || null
			if (stype) {
				this.title = stype+this.title
			}
			listj({params: {table: 'good', sid: sid, stype: stype, ppid: ppid }}).then(res => {
				this.fobjList = res || []
			}).catch(err => {

			})
		},
		methods: {
			...mapActions(['setCar']),
			noop() {},
			toGoodDetail:yewuutil.toGoodDetail2,
			goBack(){
				uni.navigateBack({
					delta:1
				})
			},
			handleMainCategory(id) {
				this.activeMainId = id
			},
			cartCount(gid) {
				const row = (this.carlist || []).find(i => String(i.id) === String(gid))
				return row ? Math.max(0, Number(row.count) || 0) : 0
			},
			addToCart(item) {
				if (!this.userInfo || !this.userInfo.id) {
					uni.ytool.toLogin();
					return;
				}
				const clist = [...this.carlist];
				const index = clist.findIndex(i => String(i.id) === String(item.id));
				if (index > -1) {
					clist[index].count = (Number(clist[index].count) || 0) + 1;
				} else {
					clist.push({ ...item, count: 1, checked: true });
				}
				this.setCar(clist);
			},
			changeQty(item, delta) {
				if (!this.userInfo || !this.userInfo.id) {
					uni.ytool.toLogin();
					return;
				}
				const clist = [...this.carlist];
				const index = clist.findIndex(i => String(i.id) === String(item.id));
				if (index < 0) return;
				const next = (Number(clist[index].count) || 0) + delta;
				if (next <= 0) {
					clist.splice(index, 1);
				} else {
					clist[index] = { ...clist[index], count: next };
				}
				this.setCar(clist);
			},
			showCartDetail() {
				uni.navigateTo({
					url: '/pages/shopcar/shopcar'
				});
			},
			toCheckout() {
				if (this.cartTotalQty === 0) {
					uni.showToast({ title: '请先添加商品', icon: 'none' });
					return;
				}
				uni.navigateTo({
					url: '/pages/bill/surebill'
				});

			}
		},
		computed: {
			cartTotalQty() {
				return (this.carlist || []).reduce((s, i) => s + (Number(i.count) || 0), 0);
			},
			/** 左侧分类：全部 + 根据当前列表去重的大类 */
			sidebarCategories() {
				const list = this.fobjList || []
				const map = new Map()
				for (const g of list) {
					const has = g.typeid != null && g.typeid !== ''
					const key = has ? String(g.typeid) : '_uncat'
					if (map.has(key)) continue
					const title = has ? (g.type || '分类') : '未分类'
					map.set(key, { id: has ? g.typeid : -1, title })
				}
				const rest = Array.from(map.values()).sort((a, b) => {
					if (a.id === -1) return 1
					if (b.id === -1) return -1
					return String(a.title).localeCompare(String(b.title), 'zh-CN')
				})
				return [{ id: 0, title: '全部' }, ...rest]
			},
			/** 当前大类下的菜品（未筛子类） */
			mainFilteredGoods() {
				const list = this.fobjList || []
				if (this.activeMainId === 0) return list
				if (this.activeMainId === -1) {
					return list.filter(g => g.typeid == null || g.typeid === '')
				}
				return list.filter(g => String(g.typeid) === String(this.activeMainId))
			},
			/** 当前分类下菜品，按价格排序 */
			displayGoods() {
				const list = (this.mainFilteredGoods || []).slice()
				const pa = (a, b) => (Number(a.price) || 0) - (Number(b.price) || 0)
				const pd = (a, b) => (Number(b.price) || 0) - (Number(a.price) || 0)
				if (this.priceSort === 'price_asc') list.sort(pa)
				else if (this.priceSort === 'price_desc') list.sort(pd)
				return list
			},
			...mapState(['userInfo', 'carlist'])
		}
	}
</script>

<style lang="scss" scoped>
$price-color: #ff4d4f;
$brand-orange: #ff943c;
$side-bg: #f3f4f6;
$text-dark: #1a1a1a;

.page-root {
	display: flex;
	flex-direction: column;
	height: 100vh;
	min-height: 100vh;
	background: #fff;
	box-sizing: border-box;
}

.main-body {
	flex: 1;
	display: flex;
	overflow: hidden;
	min-height: 0;
	height: 0;
}

.side-bar {
	width: 180rpx;
	background-color: $side-bg;
	height: 100%;
	flex-shrink: 0;

	.side-item {
		min-height: 100rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		position: relative;
		transition: all 0.2s;
		padding: 16rpx 8rpx;

		.side-text {
			font-size: 26rpx;
			color: #555;
			text-align: center;
			padding: 0 10rpx;
		}
	}

	.side-item-active {
		background-color: #ffffff;

		.side-text {
			color: $text-dark;
			font-weight: bold;
		}

		.active-line {
			position: absolute;
			left: 0;
			top: 30rpx;
			bottom: 30rpx;
			width: 6rpx;
			background-color: $brand-orange;
			border-radius: 0 4rpx 4rpx 0;
		}
	}
}

.content-area {
	flex: 1;
	display: flex;
	flex-direction: column;
	min-width: 0;
	background: #f5f5f5;
}

.sort-bar {
	flex-shrink: 0;
	display: flex;
	flex-wrap: wrap;
	align-items: center;
	gap: 16rpx;
	padding: 20rpx 24rpx;
	background: #fff;
	border-bottom: 1rpx solid #f2f3f5;
}
.sort-chip {
	padding: 12rpx 24rpx;
	background: #f2f3f5;
	border-radius: 32rpx;
	font-size: 24rpx;
	color: #909399;
	border: 2rpx solid transparent;
}
.sort-chip-active {
	background: rgba(255, 148, 60, 0.12);
	color: $brand-orange;
	font-weight: 600;
	border-color: rgba(255, 148, 60, 0.35);
}

.goods-scroll {
	flex: 1;
	height: 100%;
	min-height: 0;
	width: 100%;
}

.scroll-inner {
	min-height: 100%;
	padding: 20rpx;
	padding-bottom: 0;
	box-sizing: border-box;
}

/* 与底部 cart-bar 总高度一致 */
.scroll-bottom-spacer {
	height: calc(160rpx + constant(safe-area-inset-bottom));
	height: calc(160rpx + env(safe-area-inset-bottom));
	flex-shrink: 0;
}

.goods-list {
	padding: 0 8rpx 20rpx;
}

.goods-item {
	display: flex;
	flex-direction: row;
	align-items: stretch;
	width: 100%;
	background: #fff;
	border-radius: 16rpx;
	margin-bottom: 20rpx;
	padding: 24rpx 20rpx;
	box-sizing: border-box;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);

	.goods-img-box {
		width: 180rpx;
		height: 180rpx;
		flex-shrink: 0;
		border-radius: 12rpx;
		overflow: hidden;
		background: #f5f5f5;

		.goods-img {
			width: 100%;
			height: 100%;
		}

		.goods-img-placeholder {
			width: 100%;
			height: 100%;
			display: flex;
			align-items: center;
			justify-content: center;
			font-size: 64rpx;
		}
	}

	.goods-info {
		flex: 1;
		min-width: 0;
		margin-left: 24rpx;
		display: flex;
		flex-direction: column;
		justify-content: space-between;
		padding: 0;

		.goods-name {
			font-size: 30rpx;
			color: #333;
			font-weight: 600;
			display: -webkit-box;
			-webkit-box-orient: vertical;
			-webkit-line-clamp: 2;
			line-clamp: 2;
			overflow: hidden;
			line-height: 1.4;
			margin-bottom: 16rpx;
		}

		.goods-bottom {
			display: flex;
			justify-content: space-between;
			align-items: center;

			.goods-price {
				color: $price-color;
				font-size: 32rpx;
				font-weight: bold;
			}

			.stepper-wrap {
				flex-shrink: 0;
			}

			.add-btn-only {
				width: 56rpx;
				height: 56rpx;
				background: $brand-orange;
				border-radius: 50%;
				display: flex;
				align-items: center;
				justify-content: center;
				box-shadow: 0 4rpx 12rpx rgba(255, 148, 60, 0.45);

				.add-icon {
					color: #fff;
					font-size: 36rpx;
					font-weight: bold;
					line-height: 1;
				}
			}

			.stepper {
				display: flex;
				align-items: center;
				height: 56rpx;
				padding: 0 8rpx 0 16rpx;
				background: $brand-orange;
				border-radius: 28rpx;
				box-shadow: 0 4rpx 12rpx rgba(255, 148, 60, 0.35);
			}

			.stepper-minus {
				width: 44rpx;
				height: 44rpx;
				display: flex;
				align-items: center;
				justify-content: center;
				.minus-icon {
					color: #fff;
					font-size: 36rpx;
					font-weight: 300;
					line-height: 1;
				}
			}

			.stepper-num {
				min-width: 40rpx;
				padding: 0 12rpx;
				text-align: center;
				font-size: 28rpx;
				font-weight: 600;
				color: #fff;
			}

			.stepper-plus-box {
				width: 44rpx;
				height: 44rpx;
				background: #fff;
				border-radius: 12rpx;
				display: flex;
				align-items: center;
				justify-content: center;
				.stepper-plus {
					color: $brand-orange;
					font-size: 34rpx;
					font-weight: bold;
					line-height: 1;
				}
			}
		}
	}
}

.empty-tip {
	text-align: center;
	padding: 100rpx 0;
	color: #999;
	font-size: 28rpx;
}

/* 底部购物车栏 */
.cart-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	height: 130rpx;
	background: rgba(255, 255, 255, 0.98);
	backdrop-filter: blur(10px);
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 0 32rpx;
	padding-bottom: constant(safe-area-inset-bottom);
	padding-bottom: env(safe-area-inset-bottom);
	box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.08);
	z-index: 100;

	.cart-info {
		display: flex;
		align-items: center;

		.cart-icon-box {
			position: relative;
			margin-right: 20rpx;

			.cart-icon {
				font-size: 48rpx;
			}

			.cart-badge {
				position: absolute;
				top: -8rpx;
				right: -16rpx;
				background: $brand-orange;
				color: #fff;
				font-size: 22rpx;
				min-width: 36rpx;
				height: 36rpx;
				border-radius: 18rpx;
				display: flex;
				align-items: center;
				justify-content: center;
				padding: 0 8rpx;
			}
		}

		.cart-text {
			display: flex;
			flex-direction: column;

			.cart-label {
				font-size: 28rpx;
				color: #333;
				font-weight: 500;
			}

			.cart-count {
				font-size: 22rpx;
				color: #999;
			}
		}
	}

	.checkout-btn {
		width: 200rpx;
		height: 76rpx;
		background: $brand-orange;
		border-radius: 38rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 6rpx 16rpx rgba(255, 148, 60, 0.35);

		text {
			color: #fff;
			font-size: 30rpx;
			font-weight: bold;
		}
	}

	.checkout-btn-disabled {
		background: #e4e7ed;
		box-shadow: none;

		text {
			color: #c0c4cc;
		}
	}
}
</style>
