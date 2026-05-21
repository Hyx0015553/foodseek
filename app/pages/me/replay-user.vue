<template>
	<view>
		<u-navbar
			title="我的评价"
			:border="true"
			:placeholder="true"
			@leftClick="goBack"
			:rightText="navRightText"
			@rightClick="toggleManageMode"
		></u-navbar>
		<scroll-view :enable-flex="true" :class="['svcontainer', managing && fobjList.length ? 'sv-with-bar' : '']">
			<view v-if="fobjList.length === 0" class="empty-tip">
				<text>暂无评价记录</text>
			</view>
			<view v-else class="review-list">
				<view
					v-for="(item, index) in fobjList"
					:key="item.id"
					class="review-item"
					:class="{ 'review-item--managing': managing }"
					:data-id="item.id"
					:data-pid="item.pid"
					@tap="onReviewItemTap"
				>
					<view v-if="managing" class="chk-wrap">
						<view class="chk" :class="{ 'chk--on': isRowSelected(item.id) }"></view>
					</view>
					<view class="review-body">
						<view class="review-header">
							<image v-if="item.gimg" class="good-img" :src="fileUrl+item.gimg" mode="aspectFill"></image>
							<view v-else class="good-img-placeholder">🍲</view>
							<view class="review-info">
								<view class="good-name">{{ item.gname || '未知菜品' }}</view>
								<view class="review-meta">
									<text class="score">{{ item.pf }}分</text>
									<text class="date">{{ item.ndate }}</text>
								</view>
							</view>
						</view>
						<view class="review-content">{{ item.note }}</view>
						<view v-if="item.img" class="review-imgs">
							<image v-for="(img, idx) in item.img.split(',')" :key="idx" :src="fileUrl+img" mode="aspectFill" class="review-img"></image>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>
		<view v-if="managing && fobjList.length" class="manage-bar safe-area-inset-bottom">
			<text class="manage-bar-all" @tap="toggleSelectAll">{{ selectAllLabel }}</text>
			<view class="manage-bar-del" :class="{ disabled: selectedIds.length === 0 }" @tap="deleteSelected">
				<text>删除</text>
				<text v-if="selectedIds.length">({{ selectedIds.length }})</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { listSqlj, listj, findj, savej, fileUrl, deletej } from '@/common/config/api.js';
	import {mapState, mapActions} from 'vuex';
	import { ideautil, yewuutil } from '@/common/commontools.js';
	export default {
		data() {
			return {
				fobjList: [],
				fileUrl: fileUrl,
				managing: false,
				selectedIds: []
			};
		},
		onLoad(params) {
			this.loadMyReviews()
		},
		onShow() {
			this.loadMyReviews()
		},
		computed: {
			...mapState(['userInfo']),
			navRightText() {
				if (!this.fobjList.length) return ''
				return this.managing ? '完成' : '管理'
			},
			selectAllLabel() {
				if (!this.fobjList.length) return '全选'
				return this.selectedIds.length === this.fobjList.length ? '取消全选' : '全选'
			}
		},
		methods: {
			...mapActions(['updateUserInfo']),
			normalizeId(id) {
				return id === undefined || id === null ? '' : String(id)
			},
			isRowSelected(id) {
				const k = this.normalizeId(id)
				return this.selectedIds.indexOf(k) !== -1
			},
			toggleManageMode() {
				if (!this.fobjList.length) return
				this.managing = !this.managing
				if (!this.managing) this.selectedIds = []
			},
			toggleSelectAll() {
				if (!this.fobjList.length) return
				if (this.selectedIds.length === this.fobjList.length) {
					this.selectedIds = []
				} else {
					this.selectedIds = this.fobjList.map((x) => this.normalizeId(x.id))
				}
			},
			onReviewItemTap(e) {
				const ds = e.currentTarget.dataset || {}
				const id = this.normalizeId(ds.id)
				const pid = ds.pid
				if (this.managing) {
					const i = this.selectedIds.indexOf(id)
					if (i === -1) this.selectedIds.push(id)
					else this.selectedIds.splice(i, 1)
					return
				}
				this.toGoodDetail(pid)
			},
			deleteSelected() {
				if (!this.selectedIds.length) {
					uni.showToast({ title: '请选择要删除的评价', icon: 'none' })
					return
				}
				const n = this.selectedIds.length
				uni.showModal({
					title: '确认删除',
					content: `将删除已选的 ${n} 条评价，不可恢复`,
					success: (res) => {
						if (!res.confirm) return
						uni.showLoading({ title: '删除中' })
						const chain = this.selectedIds.reduce(
							(p, rid) =>
								p.then(() => deletej({ params: { table: 'replay', id: rid } })),
							Promise.resolve()
						)
						chain
							.then(() => {
								uni.hideLoading()
								uni.showToast({ title: '已删除', icon: 'success' })
								this.selectedIds = []
								this.managing = false
								this.loadMyReviews()
							})
							.catch(() => {
								uni.hideLoading()
								uni.showToast({ title: '删除失败', icon: 'none' })
							})
					}
				})
			},
			loadMyReviews() {
				if (!this.userInfo || !this.userInfo.id) {
					uni.showToast({ title: '请先登录', icon: 'none' })
					return
				}
				// 查询当前用户的所有评价，关联菜品表获取菜品信息
				let sql = `SELECT r.*, g.gname, g.img as gimg
					FROM fs_replay r 
					LEFT JOIN fs_good g ON r.pid = g.id 
					WHERE r.uid = ${this.userInfo.id} AND r.type = 1
					ORDER BY r.ndate DESC`
				listSqlj({params: { sql: sql }}).then(res => {
					this.fobjList = res || []
				}).catch(err => {
					this.fobjList = []
				})
			},
			toGoodDetail(pid) {
				if (pid) {
					uni.navigateTo({
						url: '/pages/good/gooddetail?gid=' + pid
					})
				}
			},
			goBack(){
				if (this.managing) {
					this.managing = false
					this.selectedIds = []
					return
				}
				uni.switchTab({
					url:'/pages/me/me'
				})
			}
		}
	}
</script>

<style lang="scss">
.svcontainer {
	height: 100vh;
	background-color: #f5f5f5;
}
.empty-tip {
	text-align: center;
	padding: 100rpx 0;
	color: #999;
	font-size: 28rpx;
}
.review-list {
	padding: 20rpx;
}
.review-item {
	background: #fff;
	border-radius: 16rpx;
	padding: 24rpx;
	margin-bottom: 20rpx;
}
.review-item--managing {
	display: flex;
	align-items: flex-start;
}
.chk-wrap {
	flex-shrink: 0;
	padding: 8rpx 16rpx 0 0;
}
.chk {
	width: 40rpx;
	height: 40rpx;
	border-radius: 50%;
	border: 2rpx solid #ccc;
	box-sizing: border-box;
}
.chk--on {
	border-color: #ff9500;
	background-color: #ff9500;
	box-shadow: inset 0 0 0 6rpx #fff;
}
.review-body {
	flex: 1;
	min-width: 0;
}
.sv-with-bar {
	padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
}
.manage-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 20;
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 20rpx 32rpx;
	padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
	background: #fff;
	border-top: 1rpx solid #eee;
	box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
}
.manage-bar-all {
	font-size: 28rpx;
	color: #333;
}
.manage-bar-del {
	font-size: 28rpx;
	color: #fff;
	background: #ff3b30;
	padding: 16rpx 40rpx;
	border-radius: 40rpx;
}
.manage-bar-del.disabled {
	opacity: 0.45;
}
.review-header {
	display: flex;
	align-items: center;
	margin-bottom: 16rpx;
}
.good-img {
	width: 100rpx;
	height: 100rpx;
	border-radius: 12rpx;
	margin-right: 20rpx;
}
.good-img-placeholder {
	width: 100rpx;
	height: 100rpx;
	border-radius: 12rpx;
	background: #f0f0f0;
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 40rpx;
	margin-right: 20rpx;
}
.review-info {
	flex: 1;
}
.good-name {
	font-size: 30rpx;
	font-weight: bold;
	color: #333;
	margin-bottom: 8rpx;
}
.review-meta {
	display: flex;
	align-items: center;
	gap: 20rpx;
}
.score {
	color: #ff9500;
	font-size: 26rpx;
	font-weight: bold;
}
.date {
	color: #999;
	font-size: 24rpx;
}
.review-content {
	font-size: 28rpx;
	color: #666;
	line-height: 1.6;
	margin-bottom: 16rpx;
}
.review-imgs {
	display: flex;
	flex-wrap: wrap;
	gap: 12rpx;
}
.review-img {
	width: 160rpx;
	height: 160rpx;
	border-radius: 12rpx;
}
</style>

