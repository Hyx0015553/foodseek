<template>
	<view>
		<u-navbar :title="title" :border="true" :placeholder="true"   :autoBack="true"></u-navbar>
		<scroll-view :enable-flex="true" class="svcontainer">
			<imglist :imgSize="2" tColor="red" imgName="img" titleName="gname" @clickItem='toGoodDetail' tLabel="评分:" :dataList='fobjList' sName='note' tName="pf"></imglist>
		</scroll-view>
	</view>
</template>

<script>
	import { listj, findj, savej, deletej, fileUrl } from '@/common/config/api.js';
	import {mapState, mapActions} from 'vuex';
	import { ideautil, yewuutil } from '@/common/commontools.js';
	export default {
		data() {
			return {
				fobjList:[],
				title:"排行榜"
			};
		},
		onLoad(params) {
			let sid = params.sid || null
			let stype = params.stype || null
			let ppid = params.ppid || null
			if (stype) {
				this.title = stype+this.title
			}
			listj({params: {table: 'good'}}).then(res => {
        res = res.sort(function (o1,o2){
          o2.pf = o2.pf*1 || 0
          o1.pf = o1.pf*1 || 0
          return o2.pf-o1.pf
        })
				this.fobjList = res
			}).catch(err => {
			 
			})
		},
		methods: {
			toGoodDetail:yewuutil.toGoodDetail,
			goBack(){
				uni.navigateBack({
					delta:1
				})
			}
		},
		computed: {
			...mapState(['userInfo'])
		}
	}
</script>

<style lang="scss">

</style>
