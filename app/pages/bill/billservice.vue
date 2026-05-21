<template>
	<view>
		<u-navbar title="申请售后" :border="true" :placeholder="true" :autoBack="true"></u-navbar>
		<scroll-view :enable-flex="true" class="svcontainer">
			<view class="tip">售后请直接联系商家沟通处理。</view>
			<view class="formitem">
				<u-button type="primary" @click="goOrder" text="返回订单详情"></u-button>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import {
		findj
	} from '@/common/config/api.js';
	export default {
		data() {
			return {
				focusobj: {},
				tid: null
			};
		},
		onLoad(params) {
			if (params.tid && params.tid !== 'undefined') {
				this.tid = params.tid
			}
			if (this.tid) {
				findj({
					params: {
						table: 'bill',
						id: this.tid
					}
				}).then(res => {
					this.focusobj = res || {}
				}).catch(() => {})
			}
		},
		methods: {
			goOrder() {
				if (this.focusobj && this.focusobj.id) {
					uni.redirectTo({ url: '/pages/bill/billdetail?bid=' + this.focusobj.id })
				} else {
					uni.navigateBack({ delta: 1 })
				}
			}
		}
	}
</script>

<style lang="scss">
.tip { padding: 32rpx; color: #666; font-size: 28rpx; line-height: 1.6; }
.formitem { padding: 24rpx 32rpx; }
</style>
