<template>
	<view>
		<u-navbar title="购物车" :border="true" leftIconSize="0" :placeholder="true" :autoBack="true"></u-navbar>
		<scroll-view :enable-flex="true" class="svcontainer">
		
			<view class="shopcarlist">
				<checkbox-group  @change="toggleSelectList">
					<view class="caritem" v-for="(item,index) in carlist" :key="index">
						<checkbox v-if="bjstatus" :checked="item.checked" class="carcheckbox" :value="item.id+''" />
						<image @click="toGoodDetail(item.id)" class="carimg" :src="fileUrl+item.img"></image>
						<view  class="carinfo">
							<view class="cartitletop">
								<view class="cartitle" @click="toGoodDetail(item.id)">{{item.gname}}<text style="color:blue;margin-left: 10rpx">{{item.shop}}</text></view>
								<view class="caropctn">
									<u-button type="primary" size="mini" text="移除" @click="delCar(item.id)" />
								</view>
							</view>
							
							<view class="carpricectn">
								<view class="carprice">价格:{{item.price}}</view>
								<view class="carcount">							
									<u-number-box v-model="item.count" @change="jisuantotal"></u-number-box>
								</view>
							</view>
						</view>
					</view>
				</checkbox-group>
			</view>
		  <!-- #ifdef MP-WEIXIN  || APP-PLUS -->
				<view class="footerctn">
					<navigator url="#" class="gmbtn" @click="tijiaoBtn()">结算</navigator>
					<view class="hejictn"><text>合计:</text><text class="totalprice">{{totalprice}}</text></view>
				</view>
			<!-- #endif -->
			<!-- #ifdef H5 -->
				<view class="footerctnhasfooter">
					<navigator url="#" class="gmbtn" @click="tijiaoBtn()">结算</navigator>
					<view class="hejictn"><text>合计:</text><text class="totalprice">{{totalprice}}</text></view>
				</view>
			<!-- #endif -->
		</scroll-view>
	</view>


</template>

<script>
	import {
		mapState,
		mapActions
	} from 'vuex'
	import {
		listj,
		findj,
		fileUrl
	} from '@/common/config/api.js';
	import { ideautil, yewuutil } from '@/common/commontools.js';
	export default {
		data() {
			return {
				fileUrl: fileUrl,
				bjstatus: true,
				totalprice: 0
			};
		},
		onShow(){
			this.jisuantotal()
		},
		onLoad() {
			if (!this.userInfo || !this.userInfo.id) {
				uni.ytool.toLogin()
			}
			this.jisuantotal()
		},
		methods: {
			toGoodDetail: yewuutil.toGoodDetail,
			...mapActions(['setCar']),
			delCar(id){
				let clist = []
				for (let i = 0; i < this.carlist.length; i++) {
					let obj = this.carlist[i]
					if (obj.id == id) {
						continue
					}
					clist.push(obj)
				}
				this.setCar(clist)
				this.jisuantotal()
			},
			tijiaoBtn() {
				if (this.totalprice>0) {
					uni.navigateTo({
						url: '../bill/surebill'
					})
				}
				
			},
			goBack() {
				uni.redirectTo({
					url: '../good/gooddetail'
				})
			},
			toggleSelectList(v){
				let checkedlist = v.detail.value
				//if (checkedlist.length) {
					for (let i = 0; i < this.carlist.length; i++) {
						let tobj = this.carlist[i]
						tobj.checked = false
						for (let j = 0; j < checkedlist.length; j++) {
							let gid = checkedlist[j]
							if (tobj.id == gid) {
								tobj.checked = true
								break
							}
							
						}
					}
					this.jisuantotal()
				//}
			},
			jisuantotal() {
				setTimeout(()=>{
					let list = this.carlist
					let total = 0
					for (let i = 0; i < list.length; i++) {
						let obj = list[i]
						if (obj.checked) {
							let p = obj.price * 1 * obj.count
							total += p
						}
						
					}
					this.totalprice = total
				},100)
				
			}
		},
		computed: {
			...mapState(['carlist', 'userInfo'])
		},
		watch: {
			carlist: {
				deep: true, //深度监听
				handler(newValue, oldValue) {
					console.log(newValue)
				},
				immediate: true
			},
		}
	}
</script>

<style lang="scss">
	.gmbtn{
		width: 200rpx;
		text-align: center;
		background-color: #161E35;
		color: #fff;
		height: 100%;
		line-height: 100upx;
	}
	.caritem {
		display: flex;
		width: 100%;
		height: 200upx;
		background-color: #fafafa;
		align-items: center;
		padding: 10upx;
		margin-bottom: 10upx;
    box-shadow: 2px 2px #161E35;
	}

	.carcheckbox {
		width: 50upx;
	}

	.carimg {
		width: 200upx;
		height: 180upx;
		margin-right: 10upx;
	}

	.carinfo {
		height: 100%;
		display: flex;
		flex-direction: column;
		justify-content: space-around;
		flex: 1;
	}

	.cartitle {
		font-size: 25upx;
	}

	.carpricectn {
		display: flex;
		justify-content: space-between;
		width: 100%;
	}

	.carprice {
		color: red;
		font-size: 20upx;
	}

	.carcount {}

	.totalprice {
		color: red;
		padding: 20upx 0;
		font-size: 40rpx;
	}
	.hejictn{
		flex: 1;
		padding-left: 20rpx;
		color: #000;
	}
	.cartitletop{
		display: flex;
	}
	.cartitle{
		flex: 9;
		overflow: hidden;
	}
	.caropctn{
		flex: 1;
	}
	
</style>
