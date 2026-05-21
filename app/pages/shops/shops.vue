<template>
	<view>
		<u-navbar title="店铺推荐" :border="true" :placeholder="true" @leftClick="goBack"  :autoBack="false"></u-navbar>
		<scroll-view :enable-flex="true" class="svcontainer">
      <view class="formitem">
        <navigator url="/pages/unimap/unimap">
          <u-button type="primary" text="地图"></u-button>
        </navigator>

      </view>
			<imglist :imgSize="2" imgName="img" titleName="sname" @clickItem='shopDetail' tName="pf" tLabel="评分:" tColor="red" :dataList='fobjList' sName='address' ></imglist>
		</scroll-view>
	</view>
</template>

<script>
	import { listj, findj, savej, deletej, fileUrl } from '@/common/config/api.js';
	import { SHOP_AUDIT_STATE } from '@/common/shopState.js';
	import {mapState, mapActions} from 'vuex';
	
	export default {
		data() {
			return {
				fobjList:[]
			};
		},
		onLoad(params) {
			// 只展示“审核通过”的店铺给普通用户
			listj({params: {table: 'shop', state: SHOP_AUDIT_STATE.APPROVED}}).then(res => {
				res = res.sort((o1,o2)=>{
					return o2.pf*1 - o1.pf*1
				})
				this.fobjList = res
			}).catch(err => {
			 
			})
		},
		methods: {
			
			shopGoods(sid){
				uni.navigateTo({
					url: '/pages/good/goodview?sid='+sid
				})
			},
			shopDetail(pid){
				uni.navigateTo({
					url: '/pages/shops/shopdetail?pid='+pid
				})
			},
			goBack(){
				uni.switchTab({
					url:'/pages/index/index'
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
