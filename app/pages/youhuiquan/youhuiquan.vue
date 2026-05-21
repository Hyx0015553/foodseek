<template>
	<view>
		<u-navbar title="优惠券" :border="true" :placeholder="true" @leftClick="goBack" :autoBack="true"></u-navbar>

		<scroll-view :enable-flex="true" class="svcontainer">
			<view class="header-bar">
				<u-tabs
					:list="typelist"
					keyName="title"
					:current="tabCurrent"
					lineColor="#ff943c"
					lineWidth="30"
					:activeStyle="{ color: '#ff943c', fontWeight: 'bold' }"
					:inactiveStyle="{ color: '#909399' }"
					@change="onTabChange"
				></u-tabs>
				<!-- 管理按钮：仅已领取tab且有过期券时显示 -->
				<view class="manage-btn" v-if="tabCurrent === 0 && expiredCount > 0" @tap="openManage">
					管理
				</view>
			</view>

			<!-- 已领取 -->
			<view v-if="tabCurrent === 0 && showlist.length > 0" class="coupon-list">
				<!-- 管理模式顶部提示条 -->
				<view class="manage-tip" v-if="manageMode">
					<text>已选 {{ selectedIds.length }} 张失效券</text>
					<view class="manage-actions">
						<text class="cancel-btn" @tap="manageMode = false">取消</text>
						<text class="clear-btn" @tap="clearExpired">清除</text>
					</view>
				</view>
				<view
					class="coupon-card"
					v-for="(item, index) in showlist"
					:key="index"
					:class="{ expired: isExpired(item), 'manage-checked': manageMode && selectedIds.includes(item.id) }"
					@click="manageMode ? toggleSelect(item.id) : ''"
				>
					<view class="manage-check" v-if="manageMode">
						<u-icon :name="selectedIds.includes(item.id) ? 'checkbox-mark' : 'checkbox-blank'" :color="selectedIds.includes(item.id) ? '#ff943c' : '#ccc'" size="22"></u-icon>
					</view>
					<view class="coupon-left">
						<view class="expired-tag" v-if="isExpired(item)">已失效</view>
						<view class="coupon-amount">
							<text class="amount-symbol">¥</text>
							<text class="amount-value">{{ item.total }}</text>
						</view>
						<view class="coupon-condition" v-if="item.fulluse">满{{ item.fulluse }}元可用</view>
						<view class="coupon-condition" v-else>无门槛</view>
					</view>
					<view class="coupon-right">
						<view class="coupon-exptime" v-if="item.extimestr">
							<text class="exptime-label">有效期至</text>
							<text class="exptime-value" :class="{ 'expired-text': isExpired(item) }">{{ item.extimestr }}</text>
						</view>
						<view class="coupon-note" v-if="item.note">{{ item.note }}</view>
						<view class="use-btn" v-if="!isExpired(item)" @tap.stop="toShopDetail(item)">去使用</view>
						<view class="del-btn" v-else @tap.stop="removeExpired(item, index)">删除</view>
					</view>
				</view>
			</view>

			<!-- 已使用 -->
			<view v-if="tabCurrent === 1 && showlist.length > 0" class="coupon-list">
				<view class="coupon-card used" v-for="(item, index) in showlist" :key="index">
					<view class="coupon-left">
						<view class="coupon-amount">
							<text class="amount-symbol">¥</text>
							<text class="amount-value">{{ item.total }}</text>
						</view>
						<view class="coupon-condition" v-if="item.fulluse">满{{ item.fulluse }}元可用</view>
						<view class="coupon-condition" v-else>无门槛</view>
					</view>
					<view class="coupon-right">
						<view class="coupon-exptime" v-if="item.extimestr">
							<text class="exptime-label">有效期至</text>
							<text class="exptime-value">{{ item.extimestr }}</text>
						</view>
						<view class="coupon-note" v-if="item.note">{{ item.note }}</view>
						<view class="del-btn" @tap="removeUsed(item, index)">删除</view>
					</view>
				</view>
			</view>

			<!-- 空状态 -->
			<view v-if="showlist.length === 0" class="empty-state">
				<text class="empty-icon">🎫</text>
				<text class="empty-text">{{ tabCurrent === 0 ? '暂无已领取的优惠券' : '暂无已使用的优惠券' }}</text>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import { listj, deletej } from '@/common/config/api.js'
	import { mapState } from 'vuex'
	import { yewuutil } from '@/common/commontools.js'

	export default {
		data() {
			return {
				showlist: [],
				tabCurrent: 0,
				typelist: [{ title: '已领取' }, { title: '已使用' }],
				_deletedIds: [], // 已删除的已使用券 ID，本地持久化
				_expiredIds: [], // 已删除的已失效券 ID，本地持久化
				manageMode: false,
				selectedIds: []
			};
		},
		onLoad(params) {
			const t = params && (params.tab || '')
			if (t === 'yilingqu' || t === '1' || t === '已领取') {
				this.tabCurrent = 0
			}
			try {
				this._deletedIds = uni.getStorageSync('_deletedYhqIds') || []
				this._expiredIds = uni.getStorageSync('_expiredYhqIds') || []
			} catch (e) {
				this._deletedIds = []
				this._expiredIds = []
			}
			this.uploadData()
		},
		methods: {
			onTabChange(item) {
				this.tabCurrent = item.index != null ? item.index : 0
				this.refreshList()
			},
			uploadData() {
				this.refreshList()
			},
			refreshList() {
				if (this.tabCurrent === 0) {
					// 已领取：typeid=2，状态=正常，当前用户的券
					listj({
						params: {
							table: 'youhuiquan',
							typeid: 2,
							uid: this.userInfo.id,
							statecn: '正常'
						}
				}).then(res => {
					const expired = this._expiredIds || []
					this.showlist = (res || []).filter(item => !expired.includes(Number(item.id)))
				}).catch(() => {
					this.showlist = []
				})
				} else {
					// 已使用：typeid=2，状态=已使用，当前用户的券
					listj({
						params: {
							table: 'youhuiquan',
							typeid: 2,
							uid: this.userInfo.id,
							statecn: '已使用'
						}
					}).then(res => {
						// 过滤掉本地已删除的记录
						const deleted = this._deletedIds || []
						this.showlist = (res || []).filter(item => !deleted.includes(Number(item.id)))
					}).catch(() => {
						this.showlist = []
					})
				}
			},
			goBack() {
				uni.switchTab({ url: '/pages/me/me' })
			},
			// 去使用：跳转店铺详情页，shopdetail.vue 的 onLoad 取 params.pid
			toShopDetail(item) {
				const sid = item && item.sid
				if (!sid) {
					uni.showToast({ title: '店铺信息缺失', icon: 'none' })
					return
				}
				uni.navigateTo({
					url: `/pages/shops/shopdetail?pid=${sid}`
				})
			},
			// 删除已使用券：仅从列表移除，不删数据库，ID 持久化到本地存储
			removeUsed(item, index) {
				const id = Number(item && item.id)
				if (!id) return
				uni.showModal({
					title: '提示',
					content: '确定删除这条已使用记录？',
					success: res => {
						if (res.confirm) {
							// 写入本地已删除列表并持久化
							const deleted = this._deletedIds || []
							if (!deleted.includes(id)) {
								deleted.push(id)
								this._deletedIds = deleted
								try {
									uni.setStorageSync('_deletedYhqIds', deleted)
								} catch (e) {}
							}
							// 从列表移除
							const list = [...this.showlist]
							list.splice(index, 1)
							this.showlist = list
							uni.showToast({ title: '已删除', icon: 'success' })
						}
					}
				})
			},
			isExpired(item) {
				const now = Math.floor(Date.now() / 1000)
				return item.extime && item.extime > 0 && item.extime < now
			},
			get expiredCount() {
				return (this.showlist || []).filter(item => this.isExpired(item)).length
			},
			openManage() {
				this.manageMode = true
				this.selectedIds = (this.showlist || [])
					.filter(item => this.isExpired(item))
					.map(item => item.id)
			},
			toggleSelect(id) {
				const idx = this.selectedIds.indexOf(id)
				if (idx > -1) {
					this.selectedIds.splice(idx, 1)
				} else {
					this.selectedIds.push(id)
				}
			},
			clearExpired() {
				if (!this.selectedIds.length) return
				uni.showModal({
					title: '提示',
					content: `确定清除选中的 ${this.selectedIds.length} 张失效券？`,
					success: res => {
						if (res.confirm) {
							const ids = this.selectedIds
							const expired = this._expiredIds || []
							ids.forEach(id => {
								if (!expired.includes(id)) expired.push(id)
							})
							this._expiredIds = expired
							try {
								uni.setStorageSync('_expiredYhqIds', expired)
							} catch (e) {}
							this.showlist = (this.showlist || []).filter(item => !ids.includes(item.id))
							this.manageMode = false
							this.selectedIds = []
							uni.showToast({ title: '已清除', icon: 'success' })
						}
					}
				})
			},
			removeExpired(item, index) {
				const id = Number(item && item.id)
				if (!id) return
				const expired = this._expiredIds || []
				if (!expired.includes(id)) {
					expired.push(id)
					this._expiredIds = expired
					try {
						uni.setStorageSync('_expiredYhqIds', expired)
					} catch (e) {}
				}
				const list = [...this.showlist]
				list.splice(index, 1)
				this.showlist = list
				uni.showToast({ title: '已删除', icon: 'success' })
			}
		},
		computed: {
			...mapState(['userInfo'])
		}
	}
</script>

<style lang="scss" scoped>
.svcontainer {
	height: calc(100vh - 88rpx);
	padding: 20rpx 24rpx;
	background: #f6f7f9;
}

.header-bar {
	position: relative;
	.manage-btn {
		position: absolute;
		right: 0;
		top: 50%;
		transform: translateY(-50%);
		padding: 6rpx 24rpx;
		background: #fff3e0;
		color: #ff943c;
		font-size: 24rpx;
		border-radius: 30rpx;
		font-weight: bold;
		z-index: 1;
	}
}

.manage-tip {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 20rpx 24rpx;
	background: #fff3e0;
	border-radius: 12rpx;
	margin-bottom: 20rpx;
	font-size: 26rpx;
	color: #e65100;
	.manage-actions {
		display: flex;
		gap: 24rpx;
		.cancel-btn { color: #999; }
		.clear-btn { color: #ff4d4f; font-weight: bold; }
	}
}

.coupon-list {
	margin-top: 20rpx;
}

.coupon-card {
	display: flex;
	background: #fff;
	border-radius: 16rpx;
	overflow: hidden;
	margin-bottom: 20rpx;
	box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.06);
	position: relative;
	&.used {
		opacity: 0.65;
	}
	&.expired {
		opacity: 0.5;
	}
	&.manage-checked {
		background: #fff9f5;
		border: 2rpx solid #ff943c;
	}
	.manage-check {
		position: absolute;
		left: 16rpx;
		top: 50%;
		transform: translateY(-50%);
		z-index: 2;
	}
}

.coupon-left {
	width: 220rpx;
	background: linear-gradient(135deg, #ff943c, #ff6a00);
	padding: 30rpx 24rpx;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	position: relative;
	.expired-tag {
		position: absolute;
		top: 8rpx;
		right: -8rpx;
		background: #999;
		color: #fff;
		font-size: 18rpx;
		padding: 2rpx 10rpx;
		border-radius: 10rpx;
		transform: rotate(15deg);
	}
}

.coupon-amount {
	display: flex;
	align-items: baseline;
	color: #fff;
}

.amount-symbol {
	font-size: 28rpx;
	font-weight: 600;
}

.amount-value {
	font-size: 52rpx;
	font-weight: 900;
}

.coupon-condition {
	font-size: 22rpx;
	color: rgba(255, 255, 255, 0.85);
	margin-top: 6rpx;
}

.coupon-right {
	flex: 1;
	padding: 24rpx;
	display: flex;
	flex-direction: column;
	justify-content: space-between;
}

.coupon-exptime {
	font-size: 22rpx;
	color: #909399;
	.exptime-label { margin-right: 6rpx; }
	.exptime-value { color: #606266; }
	.expired-text { color: #bbb; text-decoration: line-through; }
}

.coupon-note {
	font-size: 22rpx;
	color: #c0c4cc;
	margin-top: 6rpx;
}

.use-btn {
	align-self: flex-end;
	margin-top: 12rpx;
	padding: 10rpx 28rpx;
	background: linear-gradient(135deg, #ff943c, #ff6a00);
	color: #fff;
	font-size: 24rpx;
	border-radius: 30rpx;
	font-weight: 600;
}

.del-btn {
	align-self: flex-end;
	margin-top: 12rpx;
	padding: 10rpx 28rpx;
	background: #f5f5f5;
	color: #909399;
	font-size: 24rpx;
	border-radius: 30rpx;
}

.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	margin-top: 120rpx;
	.empty-icon {
		font-size: 100rpx;
		opacity: 0.4;
	}
	.empty-text {
		margin-top: 24rpx;
		font-size: 26rpx;
		color: #909399;
	}
}
</style>
