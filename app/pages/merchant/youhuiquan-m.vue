<template>
	<view class="page-container">
		<view class="nav-section">
			<u-navbar
				title="福利活动管理"
				:border="false"
				:placeholder="true"
				:autoBack="true"
				bgColor="#ff943c"
				titleStyle="color:#fff;font-weight:bold;"
				leftIconColor="#fff"
			></u-navbar>
		</view>

		<!-- 顶部新增按钮 -->
		<view class="toolbar">
			<view class="toolbar-btn" @tap="openAddModal">
				<text>🎁 新增优惠券</text>
			</view>
		</view>

		<!-- 使用页面原生滚动：小程序里 scroll-view + flex/百分比高度极易算错，导致下半屏空白 -->
		<view class="list-body">
				<view class="content-wrapper">
				<!-- 优惠券列表 -->
				<view class="coupon-list" v-if="couponList.length > 0">
					<view
						class="coupon-card"
						v-for="(item, index) in couponList"
						:key="index"
					>
						<!-- 左侧：金额信息 -->
						<view class="coupon-left">
							<view class="coupon-amount">
								<text class="amount-symbol">¥</text>
								<text class="amount-value">{{ item.total }}</text>
							</view>
							<view class="coupon-condition" v-if="item.fulluse">
								满{{ item.fulluse }}元可用
							</view>
							<view class="coupon-condition" v-else>无门槛</view>
							<view class="coupon-type-tag" :class="'tag-' + item.coupontype">
								{{ getTypeName(item.coupontype) }}
							</view>
						</view>

						<!-- 右侧：统计 + 操作 -->
						<view class="coupon-right">
							<view class="coupon-exptime" v-if="item.extimestr">
								<text class="exptime-label">有效期至</text>
								<text class="exptime-value">{{ item.extimestr }}</text>
							</view>
							<view class="coupon-stats">
								<view class="stat-item">
									<text class="stat-num">{{ item.lingquCount || 0 }}</text>
									<text class="stat-label">已领取</text>
								</view>
								<view class="stat-item">
									<text class="stat-num">{{ item.usedCount || 0 }}</text>
									<text class="stat-label">已使用</text>
								</view>
								<view class="stat-item">
									<text class="stat-num">{{ item.kucun != null && item.kucun !== '' ? item.kucun : '∞' }}</text>
									<text class="stat-label">库存</text>
								</view>
							</view>
							<view class="coupon-actions">
								<view class="action-btn small" @tap="openEditModal(item)">编辑</view>
								<view class="action-btn small danger" @tap="deleteCoupon(item)">删除</view>
							</view>
						</view>
					</view>
				</view>

				<!-- 空状态 -->
				<view class="empty-state" v-else>
					<text class="empty-icon">🎫</text>
					<text class="empty-text">暂无优惠券，请点击顶部「新增优惠券」</text>
				</view>
			</view>
		</view>

		<!-- 新增/编辑弹窗 -->
		<view class="modal-mask" v-if="showModal" @tap="closeModal">
			<view class="modal-content" @tap.stop>
				<view class="modal-header">
					<text class="modal-title">{{ editingCoupon.id ? '编辑优惠券' : '新增优惠券' }}</text>
					<text class="modal-close" @tap="closeModal">×</text>
				</view>

				<view class="modal-body">
					<!-- 优惠金额 -->
					<view class="form-item">
						<text class="form-label">优惠金额 (元) <text class="required">*</text></text>
						<input
							class="form-input"
							type="digit"
							v-model="formData.total"
							placeholder="请输入优惠金额，如：10"
						/>
					</view>

					<!-- 满减条件 -->
					<view class="form-item">
						<text class="form-label">满减条件 (元)</text>
						<input
							class="form-input"
							type="digit"
							v-model="formData.fulluse"
							placeholder="不填则无门槛，如：50"
						/>
					</view>

					<!-- 优惠券类型 -->
					<view class="form-item">
						<text class="form-label">优惠券类型</text>
						<view class="type-selector">
							<view
								class="type-option"
								:class="{ active: formData.coupontype === 1 }"
								@tap="formData.coupontype = 1"
							>
								优惠卷
							</view>
							<view
								class="type-option"
								:class="{ active: formData.coupontype === 2 }"
								@tap="formData.coupontype = 2"
								style="display: none;"
							>
								优惠卷
							</view>
						</view>
					</view>

					<!-- 过期时间 -->
					<view class="form-item">
						<text class="form-label">过期时间</text>
						<view class="form-input datetime-input" @tap="showDatePicker">
							<text :class="formData.extimestr ? 'datetime-text' : 'datetime-placeholder'">
								{{ formData.extimestr || '请选择过期时间' }}
							</text>
							<text class="datetime-arrow">></text>
						</view>
					</view>

					<!-- 库存 -->
					<view class="form-item">
						<text class="form-label">库存数量</text>
						<input
							class="form-input"
							type="number"
							v-model="formData.kucun"
							placeholder="不填表示不限制库存"
						/>
					</view>

					<!-- 备注说明 -->
					<view class="form-item">
						<text class="form-label">备注说明</text>
						<input
							class="form-input"
							v-model="formData.note"
							placeholder="如：仅限周一至周五使用"
						/>
					</view>
				</view>

				<view class="modal-footer">
					<view class="modal-btn cancel" @tap="closeModal">取消</view>
					<view class="modal-btn confirm" @tap="saveCoupon">保存</view>
				</view>
			</view>
		</view>

		<!-- 日期时间选择器 -->
		<u-datetime-picker
			:show="showPicker"
			:value="pickerValue"
			mode="datetime"
			closeOnClickOverlay
			@confirm="onPickerConfirm"
			@cancel="showPicker = false"
			@close="showPicker = false"
		></u-datetime-picker>
		<ai-assistant-float />
	</view>
</template>

<script>
import { listj, savej, deletej, listSqlj } from '@/common/config/api.js'
import { mapState } from 'vuex'

export default {
	data() {
		return {
			couponList: [],
			showModal: false,
			showPicker: false,
			pickerValue: Number(new Date()),
			editingCoupon: {},
			typeNameMap: {
				1: '满减券',
				2: '优惠卷'
			},
			formData: {
				total: '',
				fulluse: '',
				coupontype: 1,
				extimestr: '',
				extime: 0,
				kucun: '',
				note: ''
			}
		}
	},
	computed: {
		...mapState(['userInfo'])
	},
	onShow() {
		if (!this.userInfo || !this.userInfo.sid) {
			uni.showToast({ title: '请先登录', icon: 'none' })
			return
		}
		this.loadCoupons()
	},
	methods: {
		getTypeName(type) {
			return this.typeNameMap[type] || '优惠卷'
		},
		loadCoupons() {
			listj({
				params: {
					table: 'youhuiquan',
					typeid: 1,
					sid: this.userInfo.sid
				}
			})
				.then(res => {
					this.couponList = (res || []).filter(item => yewuutil.isYouhuiquanTemplateOnShelf(item))
					this.loadCouponStats()
				})
				.catch(() => {
					this.couponList = []
				})
		},
		loadCouponStats() {
			const rawIds = (this.couponList || [])
				.map(c => c.id)
				.filter(id => id != null && id !== '')
			const ids = rawIds
				.map(id => parseInt(String(id), 10))
				.filter(n => Number.isInteger(n) && n > 0)
			if (!ids.length) return
			// 后端 list 接口只认实体字段，pid_in 无效；用 SQL 按模板 id 统计用户券（typeid=2）
			const sql = `select * from fs_youhuiquan where typeid=2 and pid in (${ids.join(',')})`
			listSqlj({ params: { sql } })
				.then(res => {
					const stats = {}
					;(res || []).forEach(item => {
						const pid = parseInt(String(item.pid != null ? item.pid : ''), 10)
						if (!Number.isFinite(pid)) return
						if (!stats[pid]) stats[pid] = { lingqu: 0, used: 0 }
						stats[pid].lingqu++
						const st = item.state != null ? Number(item.state) : null
						if (st === 2 || String(item.statecn || '').trim() === '已使用') stats[pid].used++
					})
					// 整表替换，保证 Vue2 对新增字段的视图更新
					this.couponList = (this.couponList || []).map(c => {
						const tid = parseInt(String(c.id), 10)
						const s = Number.isFinite(tid) ? stats[tid] : null
						return {
							...c,
							lingquCount: s ? s.lingqu : 0,
							usedCount: s ? s.used : 0
						}
					})
				})
				.catch(() => {})
		},
		openAddModal() {
			this.editingCoupon = {}
			this.formData = {
				total: '',
				fulluse: '',
				coupontype: 1,
				extimestr: '',
				extime: 0,
				kucun: '',
				note: ''
			}
			this.pickerValue = Number(new Date())
			this.showModal = true
		},
		openEditModal(item) {
			this.editingCoupon = item
			this.formData = {
				total: item.total != null ? String(item.total) : '',
				fulluse: item.fulluse != null ? String(item.fulluse) : '',
				coupontype: item.coupontype || 1,
				extimestr: item.extimestr || '',
				extime: item.extime || 0,
				kucun: item.kucun != null ? String(item.kucun) : '',
				note: item.note || ''
			}
			this.pickerValue = item.extime ? Number(item.extime) * 1000 : Number(new Date())
			this.showModal = true
		},
		closeModal() {
			this.showModal = false
		},
		showDatePicker() {
			this.showPicker = true
		},
		onPickerConfirm(e) {
			const timestamp = Math.floor(e.value / 1000)
			this.formData.extime = timestamp
			this.formData.extimestr = uni.$u.timeFormat(e.value, 'yyyy-mm-dd hh:MM')
			this.showPicker = false
		},
		saveCoupon() {
			if (!this.formData.total) {
				uni.showToast({ title: '请输入优惠金额', icon: 'none' })
				return
			}
			if (this.formData.coupontype === 1 && (!this.formData.fulluse || String(this.formData.fulluse).trim() === '')) {
				uni.showToast({ title: '满减券请填写满减条件', icon: 'none' })
				return
			}
			const data = {
				table: 'youhuiquan',
				total: Number(this.formData.total),
				fulluse: this.formData.fulluse ? Number(this.formData.fulluse) : null,
				coupontype: this.formData.coupontype,
				extime: this.formData.extime || null,
				extimestr: this.formData.extimestr || null,
				kucun: this.formData.kucun ? Number(this.formData.kucun) : null,
				note: this.formData.note || null,
				typeid: 1,
				sid: this.userInfo.sid,
				ndate: uni.$u.timeFormat(Date.now(), 'yyyy-mm-dd hh:MM:ss')
			}
			if (this.editingCoupon.id) {
				data.id = this.editingCoupon.id
			}
			savej({ params: data })
				.then(() => {
					uni.showToast({ title: '保存成功', icon: 'success' })
					this.closeModal()
					this.loadCoupons()
				})
				.catch(() => {
					uni.showToast({ title: '保存失败', icon: 'none' })
				})
		},
		deleteCoupon(item) {
			uni.showModal({
				title: '确认删除',
				content: '删除后用户将无法再领取该优惠券，确定删除吗？',
				success: res => {
					if (res.confirm) {
						// 软删除：更新 deletestate 字段为"已删除"，不实际删除数据
						savej({
							params: {
								table: 'youhuiquan',
								id: item.id,
								deletestate: '已删除'
							}
						})
							.then(() => {
								uni.showToast({ title: '删除成功', icon: 'success' })
								this.loadCoupons()
							})
							.catch(() => {
								uni.showToast({ title: '删除失败', icon: 'none' })
							})
					}
				}
			})
		}
	}
}
</script>

<style lang="scss" scoped>
$primary-color: #ff943c;
$bg-color: #f6f7f9;
$card-bg: #ffffff;

.page-container {
	background-color: $bg-color;
	min-height: 100vh;
	display: flex;
	flex-direction: column;
	box-sizing: border-box;
}

.nav-section {
	flex-shrink: 0;
	width: 100%;
}

.toolbar {
	flex-shrink: 0;
	padding: 20rpx 30rpx 24rpx;
	background-color: #ffffff;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.04);
}

.toolbar-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	background: linear-gradient(135deg, #ff943c, #ff6b00);
	color: #fff;
	font-size: 28rpx;
	font-weight: bold;
	padding: 24rpx;
	border-radius: 16rpx;
}

.list-body {
	flex: 1;
	width: 100%;
	padding-bottom: calc(24rpx + constant(safe-area-inset-bottom));
	padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
}

.content-wrapper {
	padding: 24rpx 30rpx 40rpx;
	box-sizing: border-box;
}

.coupon-list {
	.coupon-card {
		display: flex;
		background: $card-bg;
		border-radius: 20rpx;
		padding: 30rpx;
		margin-bottom: 24rpx;
		box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
		position: relative;
		overflow: hidden;

		.coupon-left {
			flex: 1;
			padding-right: 20rpx;
			display: flex;
			flex-direction: column;
			justify-content: center;

			.coupon-amount {
				display: flex;
				align-items: baseline;
				margin-bottom: 8rpx;

				.amount-symbol {
					font-size: 32rpx;
					color: #ff4d4f;
					font-weight: bold;
				}

				.amount-value {
					font-size: 56rpx;
					color: #ff4d4f;
					font-weight: bold;
					line-height: 1;
				}
			}

			.coupon-condition {
				font-size: 22rpx;
				color: #888;
				margin-bottom: 10rpx;
			}

			.coupon-type-tag {
				display: inline-flex;
				align-items: center;
				padding: 4rpx 12rpx;
				border-radius: 6rpx;
				font-size: 20rpx;
				font-weight: bold;
				width: fit-content;

				&.tag-1 {
					background: #fff3e0;
					color: #e65100;
				}
				&.tag-2 {
					background: #e8f5e9;
					color: #2e7d32;
				}
			}
		}

		.coupon-right {
			width: 280rpx;
			display: flex;
			flex-direction: column;
			justify-content: space-between;

			.coupon-exptime {
				display: flex;
				flex-direction: column;
				margin-bottom: 8rpx;
				padding-bottom: 8rpx;
				border-bottom: 1rpx dashed #eee;

				.exptime-label {
					font-size: 20rpx;
					color: #bbb;
					margin-bottom: 2rpx;
				}

				.exptime-value {
					font-size: 22rpx;
					color: #666;
				}
			}

			.coupon-stats {
				display: flex;
				justify-content: space-around;
				margin-bottom: 12rpx;

				.stat-item {
					text-align: center;

					.stat-num {
						display: block;
						font-size: 32rpx;
						font-weight: bold;
						color: #333;
					}

					.stat-label {
						font-size: 20rpx;
						color: #999;
					}
				}
			}

			.coupon-actions {
				display: flex;
				gap: 12rpx;

				.action-btn {
					flex: 1;
					text-align: center;
					font-size: 24rpx;
					padding: 14rpx 0;
					border-radius: 8rpx;
					background: #f5f5f5;
					color: #666;

					&.danger {
						background: #fff1f0;
						color: #ff4d4f;
					}
				}
			}
		}
	}
}

.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 100rpx 0;

	.empty-icon {
		font-size: 100rpx;
		margin-bottom: 30rpx;
	}

	.empty-text {
		font-size: 28rpx;
		color: #999;
	}
}

/* 弹窗 */
.modal-mask {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(0, 0, 0, 0.5);
	z-index: 9999;
	display: flex;
	align-items: center;
	justify-content: center;
}

.modal-content {
	width: 650rpx;
	max-height: 85vh;
	background: #fff;
	border-radius: 24rpx;
	overflow: hidden;
	display: flex;
	flex-direction: column;
}

.modal-header {
	flex-shrink: 0;
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 30rpx;
	border-bottom: 1rpx solid #eee;

	.modal-title {
		font-size: 32rpx;
		font-weight: bold;
		color: #333;
	}

	.modal-close {
		font-size: 48rpx;
		color: #999;
		line-height: 1;
	}
}

.modal-body {
	flex: 1;
	overflow-y: auto;
	padding: 30rpx;

	.form-item {
		margin-bottom: 28rpx;

		&:last-child {
			margin-bottom: 0;
		}

		.form-label {
			display: block;
			font-size: 28rpx;
			color: #333;
			margin-bottom: 14rpx;
			font-weight: 500;

			.required {
				color: #ff4d4f;
				margin-left: 4rpx;
			}
		}

		.form-input {
			width: 100%;
			height: 80rpx;
			padding: 0 24rpx;
			background: #f5f5f5;
			border-radius: 12rpx;
			font-size: 28rpx;
			box-sizing: border-box;
			display: flex;
			align-items: center;
		}
	}
}

.type-selector {
	display: flex;
	gap: 16rpx;

	.type-option {
		flex: 1;
		height: 72rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		background: #f5f5f5;
		border-radius: 12rpx;
		font-size: 26rpx;
		color: #666;
		border: 2rpx solid transparent;
		transition: all 0.2s;

		&.active {
			background: #fff3e0;
			color: #e65100;
			border-color: #ff943c;
			font-weight: bold;
		}
	}
}

.datetime-input {
	display: flex;
	align-items: center;
	justify-content: space-between;

	.datetime-text {
		font-size: 28rpx;
		color: #333;
	}

	.datetime-placeholder {
		font-size: 28rpx;
		color: #bbb;
	}

	.datetime-arrow {
		font-size: 24rpx;
		color: #ccc;
	}
}

.modal-footer {
	flex-shrink: 0;
	display: flex;
	border-top: 1rpx solid #eee;

	.modal-btn {
		flex: 1;
		text-align: center;
		padding: 32rpx;
		font-size: 30rpx;

		&.cancel {
			color: #999;
			border-right: 1rpx solid #eee;
		}

		&.confirm {
			color: #ff943c;
			font-weight: bold;
		}
	}
}
</style>
