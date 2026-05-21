<template>
	<view>
		<u-navbar :title="title" :border="true" :placeholder="true"   :autoBack="true"></u-navbar>
		<scroll-view :enable-flex="true" class="svcontainer">
			<imglist :imgSize="2" imgName="img" titleName="sname" @clickItem='toDetail' :dataList='fobjList' sName='address'></imglist>
		</scroll-view>
	</view>
</template>

<script>
	import { recommendShopsj } from '@/common/config/api.js';
	import {mapState, mapActions} from 'vuex';
	import { ideautil, yewuutil } from '@/common/commontools.js';
	export default {
		data() {
			return {
				fobjList:[],
				title:"为我推荐"
			};
		},
		onLoad(params) {
			const uid = this.userInfo && this.userInfo.id != null ? String(this.userInfo.id) : ''
			const p = { limit: 30, useAi: 0 }
			if (uid) p.uid = uid
			recommendShopsj({ params: p }).then((res) => {
				const body = res && res.list != null ? res : { list: res || [] }
				this.fobjList = body.list || []
			})
		},
		methods: {
      toDetail(id){
          uni.itool.nto({
              url:'/pages/shops/shopdetail?pid='+id
          })
      },
			goBack(){
				uni.navigateBack({
					delta:1
				})
			}
		},
		computed: {
			...mapState(['userInfo'])
		},
	}
</script>

<style lang="scss">

</style>
