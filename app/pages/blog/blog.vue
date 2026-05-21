<template>
	<view>
		<u-navbar title="查询" :border="true" :placeholder="true" @leftClick="goBack"></u-navbar>
		<scroll-view :enable-flex="true" class="svcontainer">
      <view class="hpaddingctn">
        <u--input v-model="keystr" prefixIcon="search" prefixIconStyle="font-size: 29px !important;color: #909399" />
      </view>
      <view style="display: flex;flex-wrap: wrap">
        <view  class="biaoqian" v-for="item in histlist">
          <u-tag @click="search(item)" :text="item" plain > </u-tag>
        </view>

      </view>
      <view class="formitem">
        <u-button @click="search" type="primary" text="查询"></u-button>
      </view>
      <imglist :showSearch="false" imgName="img" imgSize="2" titleName="title" sName="note" tName="username"  @clickItem="toBlogDetail" :dataList="fobjlist"></imglist>
		</scroll-view>
	</view>
</template>

<script>
	import { listSqlj, listj, findj, savej, fileUrl } from '@/common/config/api.js';
	import {mapState, mapActions} from 'vuex';

	import { ideautil, yewuutil } from '@/common/commontools.js';
	export default {
		data() {
			return {
				fobjList: [],
				typelist: [],
				noticelist: [],
        fileUrl: fileUrl,
        keystr:null,
        fobjlist: [],
        histlist:[]
			};
		},
		components:{},
		onLoad(params) {
      let hgids = uni.getStorageSync("his_keys")
      if (hgids) {
        let harray = hgids.split(",")
        this.histlist = harray
      }
			let uid = this.userInfo.id;
			let sql = `select b.*,(select count(pid) from fs_replay r where r.pid=b.id) pinglun,u.img uimg from fs_blog b left join fs_user u on u.id=b.uid where 1=1`
			listSqlj({params: {sql: sql}}).then(res => {
				this.fobjList = res
			}).catch(err => {
			 
			})
			
			listj({params: {table: 'btype'}}).then(res => {
				this.typelist = res
				this.typelist.unshift({id:0,title:"全部"})
			}).catch(err => {
				
			})
		},
		onShow() {
			
		},
		methods: {
			...mapActions(['updateUserInfo']),
			toBlogDetail:yewuutil.toBlogDetail,
			
			fobjMg(id){
				console.log("------------------>:aid:"+id)
				uni.itool.nto({
					url:'./blogmg?tid='+id
				})
			},

      search(item){
        let title = item || this.keystr
        listj({params: {table: 'blog', title: title }}).then(res => {
          this.fobjlist = res
        })
        if (this.keystr) {
          this.putHistory(this.keystr)
        }

      },
      putHistory(id){
        let hgids = uni.getStorageSync("his_keys")
        if(hgids){
          let flag = uni.itool.checkStrInStr(id,hgids)
          if (!flag) {
            hgids += ","+id
          }
        }else{
          hgids = id
        }
        uni.setStorageSync("his_keys",hgids)
      },
			
			goBack(){
				uni.itool.nto({
				    url:'/pages/index/index'
				})
			},
			toNotice(){
				uni.itool.nto({
					url:"/pages/notice/notice"
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