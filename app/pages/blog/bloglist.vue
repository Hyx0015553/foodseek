<template>
	<view class="page-container">
		<u-navbar
			title="我的探店"
			:border="false"
			:placeholder="true"
			@leftClick="goBack"
			bgColor="transparent"
			titleStyle="font-weight: bold;"
		>
			<view slot="right" class="navbar-right" @tap="toggleManageMode">
				<text class="manage-btn">{{ isManageMode ? '完成' : '管理' }}</text>
			</view>
		</u-navbar>

		<view class="tabs-box">
			<u-tabs
				:list="tabList"
				keyName="title"
				:scrollable="false"
				@change="onTabChange"
				lineColor="#ff943c"
				lineWidth="30"
				:activeStyle="{ color: '#ff943c', fontWeight: 'bold' }"
				:inactiveStyle="{ color: '#909399' }"
			></u-tabs>
		</view>

		<!-- 探店计划 -->
		<scroll-view v-show="currentTab === 0" scroll-y :enable-flex="true" class="svcontainer tab-scroll">
			<view class="content-wrapper">
				<view class="plan-list">
					<view
						class="plan-item"
						:class="{ 'plan-item-checked': selectedPlans.includes(item.id) }"
						v-for="(item, index) in planList"
						:key="index"
					>
						<view class="plan-item-row">
							<u-checkbox-group v-if="isManageMode" @change="onPlanCheckboxChange">
								<u-checkbox
									:name="item.id"
									:checked="selectedPlans.includes(item.id)"
									shape="square"
									activeColor="#ff943c"
								></u-checkbox>
							</u-checkbox-group>
							<view class="plan-content">
								<view class="plan-head">
									<text class="plan-shop u-line-1">{{ item.stitle || '未命名店铺' }}</text>
									<text class="plan-state">{{ item.state || '待探店' }}</text>
								</view>
								<view class="plan-row" v-if="item.plantime">
									<text class="plan-label">计划时间</text>
									<text class="plan-val">{{ item.plantime }}</text>
								</view>
								<view class="plan-row" v-if="item.note">
									<text class="plan-note u-line-2">{{ item.note }}</text>
								</view>
								<view class="plan-foot">
									<text class="plan-date">{{ item.ndate || '' }}</text>
									<!-- 小程序 scroll-view 内 @click 常失效，用 navigator 更稳 -->
									<navigator
										v-if="planNavigateUrl(item) && !isManageMode"
										class="plan-action"
										:url="planNavigateUrl(item)"
										hover-class="plan-action-hover"
									>
										<text class="plan-tip">查看 / 编辑</text>
									</navigator>
									<view
										v-else-if="!isManageMode"
										class="plan-action"
										:data-idx="index"
										@tap.stop="openPlanByIndex"
									>
										<text class="plan-tip">查看 / 编辑</text>
									</view>
								</view>
							</view>
						</view>
					</view>
					<u-empty
						v-if="!planList.length"
						mode="data"
						icon="http://cdn.uviewui.com/uview/empty/data.png"
						text="暂无探店计划"
					></u-empty>
				</view>
			</view>
		</scroll-view>

		<!-- 探店动态 -->
		<scroll-view v-show="currentTab === 1" scroll-y :enable-flex="true" class="svcontainer tab-scroll">
			<view class="content-wrapper">
				<view class="blog-list">
					<view
						class="blog-item"
						:class="{ 'blog-item-checked': selectedBlogs.includes(item.id) }"
						v-for="(item, index) in fobjList"
						:key="index"
					>
						<view class="blog-item-row">
							<u-checkbox-group v-if="isManageMode" @change="onBlogCheckboxChange">
								<u-checkbox
									:name="item.id"
									:checked="selectedBlogs.includes(item.id)"
									shape="square"
									activeColor="#ff943c"
								></u-checkbox>
							</u-checkbox-group>
							<view class="blog-content" :class="{ 'no-pointer': isManageMode }" @click="isManageMode ? '' : fobjDetail(item.id)">
								<view class="item-header">
									<view class="user-info">
										<u-avatar :src="fileUrl+item.uimg" size="24" :text="item.username ? item.username.substring(0,1) : 'U'" fontSize="14" randomBgColor></u-avatar>
										<text class="user-name">{{item.username}}</text>
									</view>
									<text class="item-time">{{item.ndate}}</text>
								</view>

								<view class="item-content">
									<view class="text-box">
										<text class="item-title u-line-1">{{item.title}}</text>
										<text class="item-desc u-line-1" v-if="!item.img">{{item.note | filterHtml}}</text>
									</view>
									<image v-if="item.img" :src="fileUrl+item.img" mode="aspectFill" class="item-img"></image>
								</view>

								<view class="item-footer">
									<text class="edit-btn" v-if="!isManageMode">点击编辑</text>
								</view>
							</view>
						</view>
					</view>

					<u-empty v-if="!fobjList.length" mode="data" icon="http://cdn.uviewui.com/uview/empty/data.png" text="暂无动态"></u-empty>
				</view>
			</view>
		</scroll-view>

		<!-- 两个 Tab 共用底部操作栏 -->
		<view class="tab-add-footer">
			<view v-if="isManageMode" class="manage-footer">
				<text class="select-tip">已选择 {{ currentTab === 0 ? selectedPlans.length : selectedBlogs.length }} 项</text>
				<view class="delete-btn" @tap="deleteSelectedItems">
					<text class="delete-btn-text">删除</text>
				</view>
			</view>
			<view v-else class="tab-add-btn" hover-class="tab-add-btn-hover" @tap="openPlanMenu">
				<text class="tab-add-text">新增</text>
			</view>
		</view>

		<u-popup :show="planPopupShow" mode="bottom" round="20" @close="planPopupShow = false">
			<view class="plan-popup">
				<view class="plan-option" @tap="goJoinPlanFromList">探店计划</view>
				<view class="plan-option" @tap="goPublishBlogFromList">探店动态</view>
				<view class="plan-cancel" @tap="planPopupShow = false">取消</view>
			</view>
		</u-popup>

		<u-popup :show="publishShopPickerShow" mode="bottom" round="20" @close="publishShopPickerShow = false">
			<view class="plan-popup">
				<view class="plan-popup-title">选择店铺</view>
				<view
					class="plan-option"
					v-for="(s, shopIdx) in publishShopOptions"
					:key="s.sid"
					:data-i="shopIdx"
					@tap="onPickPublishShopIdx"
				>{{ s.title }}</view>
				<view class="plan-cancel" @tap="publishShopPickerShow = false">取消</view>
			</view>
		</u-popup>

		<u-popup :show="noOrderPublishPopup" mode="bottom" round="20" @close="noOrderPublishPopup = false">
			<view class="plan-popup">
				<view class="plan-popup-title">提示</view>
				<view class="plan-popup-msg">还没有已完成订单</view>
				<view class="plan-option" @tap="goPlanFromNoOrderList">创建探店计划</view>
				<view class="plan-option" @tap="goBrowseFromNoOrderList">再逛逛</view>
				<view class="plan-cancel" @tap="noOrderPublishPopup = false">关闭</view>
			</view>
		</u-popup>
	</view>
</template>

<script>
	import { listj, savej, deletej, fileUrl } from '@/common/config/api.js';
	import {mapState, mapActions} from 'vuex';
	import { yewuutil } from '@/common/commontools.js';

	export default {
		data() {
			return {
				tabList: [{ title: '探店计划' }, { title: '探店动态' }],
				currentTab: 0,
				planList: [],
				fobjList: [],
        fileUrl: fileUrl,
				planPopupShow: false,
				publishShopPickerShow: false,
				publishShopOptions: [],
				noOrderPublishPopup: false,
				publishContextSid: null,
				/** join | publish，多店选择后执行 */
				pendingMenuAction: null,
				isManageMode: false,
				selectedPlans: [],
				selectedBlogs: []
			};
		},
    filters: {
      filterHtml(val) {
        if (!val) return '';
        return val.replace(/<[^>]+>/g, "").substring(0, 30);
      }
    },
		components:{},
		onLoad() {
			if (!this.userInfo || !this.userInfo.id) {
				uni.ytool.toLogin()
			}
		},
		onShow() {
			if (this.userInfo && this.userInfo.id) {
				this.refreshAll()
			}
		},
		methods: {
			...mapActions(['updateUserInfo']),
			onTabChange(e) {
				this.currentTab = e.index
				this.exitManageMode()
			},
			toggleManageMode() {
				if (this.isManageMode) {
					this.exitManageMode()
				} else {
					this.isManageMode = true
				}
			},
			exitManageMode() {
				this.isManageMode = false
				this.selectedPlans = []
				this.selectedBlogs = []
			},
			onPlanCheckboxChange(e) {
				this.selectedPlans = e
			},
			onBlogCheckboxChange(e) {
				this.selectedBlogs = e
			},
			deleteSelectedItems() {
				const currentTab = this.currentTab
				const selectedIds = currentTab === 0 ? this.selectedPlans : this.selectedBlogs
				const tableName = currentTab === 0 ? 'blogplan' : 'blog'
				if (!selectedIds.length) {
					uni.showToast({ title: '请先选择要删除的项', icon: 'none' })
					return
				}
				uni.showModal({
					title: '确认删除',
					content: `确定要删除选中的 ${selectedIds.length} 项吗？`,
					success: async (res) => {
						if (!res.confirm) return
						uni.showLoading({ title: '删除中...' })
						try {
							for (const id of selectedIds) {
								await deletej({ params: { table: tableName, id } })
							}
							uni.hideLoading()
							uni.showToast({ title: '删除成功', icon: 'success' })
							this.exitManageMode()
							this.refreshAll()
						} catch (e) {
							uni.hideLoading()
							uni.showToast({ title: '删除失败', icon: 'none' })
						}
					}
				})
			},
			openPlanMenu() {
				if (!this.userInfo || !this.userInfo.id) {
					uni.showToast({ title: '请先登录', icon: 'none' })
					return
				}
				this.planPopupShow = true
			},
			goJoinPlanFromList() {
				this.planPopupShow = false
				this.startShopFlow('join')
			},
			goPublishBlogFromList() {
				this.planPopupShow = false
				if (!this.userInfo || !this.userInfo.id) {
					uni.showToast({ title: '请先登录', icon: 'none' })
					return
				}
				this.startShopFlow('publish')
			},
			async startShopFlow(kind) {
				if (!this.userInfo || !this.userInfo.id) return
				uni.showLoading({ title: '加载中...', mask: true })
				let shops = []
				try {
					shops = await this.buildPublishShopOptions()
				} finally {
					uni.hideLoading()
				}
				if (!shops.length) {
					uni.showToast({
						title: '暂无关联店铺，请先在店铺下单或添加探店计划',
						icon: 'none'
					})
					return
				}
				if (shops.length === 1) {
					const sid = shops[0].sid
					this.publishContextSid = sid
					await this.execJoinOrPublish(kind, sid)
					return
				}
				this.pendingMenuAction = kind
				this.publishShopOptions = shops
				this.publishShopPickerShow = true
			},
			/** 与店铺详情一致：探店计划中的店 + 有「已完成」订单的店 */
			async buildPublishShopOptions() {
				const uid = this.userInfo.id
				const map = new Map()
				for (const p of this.planList || []) {
					const sid = this.planSid(p)
					if (sid == null || sid === '') continue
					const key = String(sid)
					if (!map.has(key)) {
						map.set(key, { sid, title: p.stitle || '店铺' })
					}
				}
				const bills = await listj({ params: { table: 'bill', uid } }).catch(() => [])
				for (const b of bills || []) {
					if (String(b.statecn || '') !== '已完成') continue
					const sid = b.sid
					if (sid == null || sid === '') continue
					const key = String(sid)
					if (!map.has(key)) {
						map.set(key, { sid, title: b.shop || '店铺' })
					}
				}
				return Array.from(map.values())
			},
			/** 小程序勿用 @tap="fn(arg)"，会生成错误的 data-event-opts */
			onPickPublishShopIdx(e) {
				const shopIdx = parseInt(e.currentTarget.dataset.i, 10)
				if (Number.isNaN(shopIdx)) return
				const s = this.publishShopOptions[shopIdx]
				if (!s || s.sid == null) return
				this.publishShopPickerShow = false
				this.publishContextSid = s.sid
				this.execJoinOrPublish(this.pendingMenuAction, s.sid)
			},
			openPlanByIndex(e) {
				const idx = parseInt(e.currentTarget.dataset.idx, 10)
				if (Number.isNaN(idx)) return
				const item = this.planList[idx]
				if (item) this.openPlan(item)
			},
			/** 对齐 shopdetail：加入仅跳转计划页；发布需 hasCompletedOrderForShop */
			async execJoinOrPublish(kind, sid) {
				if (kind === 'join') {
					uni.navigateTo({
						url: '/pages/blog/blogplan?pid=' + sid,
						fail: () => {
							uni.showToast({ title: '打开失败', icon: 'none' })
						}
					})
					return
				}
				if (kind !== 'publish') return
				uni.showLoading({ title: '校验中...', mask: true })
				let ok = false
				try {
					ok = await yewuutil.hasCompletedOrderForShop(this.userInfo.id, sid)
				} finally {
					uni.hideLoading()
				}
				if (!ok) {
					this.noOrderPublishPopup = true
					return
				}
				uni.navigateTo({
					url: '/pages/blog/blogmg?pid=' + sid,
					fail: () => {
						uni.showToast({ title: '打开失败', icon: 'none' })
					}
				})
			},
			goPlanFromNoOrderList() {
				this.noOrderPublishPopup = false
				const sid = this.publishContextSid
				if (!sid) return
				uni.navigateTo({ url: '/pages/blog/blogplan?pid=' + sid })
			},
			goBrowseFromNoOrderList() {
				this.noOrderPublishPopup = false
				const sid = this.publishContextSid
				if (sid) {
					uni.navigateTo({ url: '/pages/good/goodview?sid=' + sid })
				}
			},
			refreshAll() {
				this.loadPlans()
				this.loadBlogs()
			},
			loadPlans() {
				if (!this.userInfo || !this.userInfo.id) return
				const uid = this.userInfo.id
				listj({ params: { table: 'bill', uid } })
					.then(bills => {
						const sidDone = new Set()
						for (const b of bills || []) {
							if (String(b.statecn || '') === '已完成' && b.sid != null && b.sid !== '') {
								sidDone.add(String(b.sid))
							}
						}
						return listj({
							params: {
								table: 'blogplan',
								uid,
								sort: 'id',
								order: 'desc'
							}
						}).then(plans => ({ plans: plans || [], sidDone }))
					})
					.then(({ plans, sidDone }) => {
						const out = plans.map(p => {
							if (sidDone.has(String(p.sid)) && String(p.state || '') !== '已完成') {
								savej({
									params: {
										table: 'blogplan',
										id: p.id,
										state: '已完成'
									}
								}).catch(() => {})
								return { ...p, state: '已完成' }
							}
							return p
						})
						this.planList = out
					})
					.catch(() => {
						listj({
							params: {
								table: 'blogplan',
								uid,
								sort: 'id',
								order: 'desc'
							}
						}).then(plans => {
							this.planList = plans || []
						}).catch(() => {
							this.planList = []
						})
					})
			},
			loadBlogs() {
				if (!this.userInfo || !this.userInfo.id) return
				listj({params: {table: 'blog', uid: this.userInfo.id, sort: 'id', order: 'desc'}}).then(res => {
					this.fobjList = res || []
				}).catch(() => {
					this.fobjList = []
				})
			},
			/** 兼容 JDBC Map 大小写、字符串数字 */
			planSid(item) {
				if (!item) return null
				const v = item.sid != null && item.sid !== '' ? item.sid : item.SID
				if (v == null || v === '') return null
				const n = parseInt(v, 10)
				return Number.isNaN(n) ? v : n
			},
			planNavigateUrl(item) {
				if (!item) return ''
				const sid = this.planSid(item)
				const id = item.id != null && item.id !== '' ? item.id : item.ID
				if (sid != null && sid !== '') {
					let u = '/pages/blog/blogplan?pid=' + sid
					if (id != null && id !== '') u += '&id=' + id
					return u
				}
				if (id != null && id !== '') {
					return '/pages/blog/blogplan?id=' + id
				}
				return ''
			},
			openPlan(item) {
				const url = this.planNavigateUrl(item)
				if (!url) {
					uni.showToast({ title: '缺少店铺信息，请重新保存计划', icon: 'none' })
					return
				}
				uni.navigateTo({
					url,
					fail: (err) => {
						console.error('navigateTo blogplan', err)
						uni.showToast({ title: '无法打开页面', icon: 'none' })
					}
				})
			},
      fobjDetail(id){
				uni.itool.nto({
					url:'./blogdetail?id='+id
				})
			},
			goBack(){
				const pages = getCurrentPages()
				if (pages && pages.length > 1) {
					uni.navigateBack({ delta: 1 })
					return
				}
				uni.itool.nto({
					url: '/pages/me/me'
				})
			}
		},
		computed: {
			...mapState(['userInfo'])
		}
	}
</script>

<style lang="scss" scoped>
.page-container {
  background-color: #f7f8fa;
  min-height: 100vh;
}

.tabs-box {
  background-color: #ffffff;
  padding: 10rpx 0;
}

.tab-scroll {
  height: calc(100vh - 88rpx - 88rpx - 120rpx - env(safe-area-inset-bottom));
}

.content-wrapper {
  padding: 20rpx 30rpx;
}

.plan-list {
  .plan-item {
    background-color: #fff;
    border-radius: 20rpx;
    padding: 24rpx 28rpx;
    margin-bottom: 24rpx;
    box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
  }
  .plan-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16rpx;
  }
  .plan-shop {
    flex: 1;
    font-size: 30rpx;
    font-weight: bold;
    color: #333;
    margin-right: 20rpx;
  }
  .plan-state {
    font-size: 24rpx;
    color: #ff943c;
    flex-shrink: 0;
  }
  .plan-row {
    margin-bottom: 12rpx;
  }
  .plan-label {
    font-size: 24rpx;
    color: #909399;
    margin-right: 12rpx;
  }
  .plan-val {
    font-size: 26rpx;
    color: #606266;
  }
  .plan-note {
    font-size: 26rpx;
    color: #909399;
    line-height: 1.4;
  }
  .plan-foot {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 16rpx;
    padding-top: 16rpx;
    border-top: 1rpx solid #f5f5f5;
  }
  .plan-date {
    font-size: 22rpx;
    color: #ccc;
  }
  .plan-action {
    padding: 8rpx 16rpx;
    margin: -8rpx -16rpx -8rpx 0;
  }
  .plan-action-hover {
    opacity: 0.75;
  }
  .plan-tip {
    font-size: 24rpx;
    color: #ff943c;
  }
}

.blog-list {
  .blog-item {
    background-color: #fff;
    border-radius: 20rpx;
    padding: 24rpx 30rpx;
    margin-bottom: 24rpx;
    box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.02);

    .item-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20rpx;

      .user-info {
        display: flex;
        align-items: center;

        .user-name {
          font-size: 26rpx;
          color: #666;
          margin-left: 12rpx;
        }
      }

      .item-time {
        font-size: 22rpx;
        color: #ccc;
      }
    }

    .item-content {
      display: flex;
      justify-content: space-between;
      margin-bottom: 20rpx;

      .text-box {
        flex: 1;
        margin-right: 20rpx;

        .item-title {
          font-size: 30rpx;
          font-weight: bold;
          color: #333;
          margin-bottom: 8rpx;
        }

        .item-desc {
          font-size: 26rpx;
          color: #999;
        }
      }

      .item-img {
        width: 120rpx;
        height: 120rpx;
        border-radius: 12rpx;
        background-color: #f5f5f5;
      }
    }

    .item-footer {
      border-top: 1rpx solid #f9f9f9;
      padding-top: 16rpx;
      text-align: right;

      .edit-btn {
        font-size: 24rpx;
        color: #ff9966;
      }
    }
  }
}

.navbar-right {
  padding-right: 30rpx;
}

.manage-btn {
  font-size: 28rpx;
  color: #ff943c;
  font-weight: bold;
}

.plan-item-row {
  display: flex;
  align-items: flex-start;
}

.plan-content {
  flex: 1;
}

.plan-item-checked {
  background-color: #fff5eb;
  border: 2rpx solid #ff943c;
}

.manage-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 88rpx;
}

.select-tip {
  font-size: 26rpx;
  color: #606266;
}

.delete-btn {
  padding: 16rpx 40rpx;
  background-color: #ff4d4f;
  border-radius: 40rpx;
}

.delete-btn-text {
  font-size: 28rpx;
  color: #fff;
  font-weight: bold;
}

.blog-content {
  flex: 1;
}

.no-pointer {
  pointer-events: none;
}

.blog-item-checked {
  background-color: #fff5eb;
  border: 2rpx solid #ff943c;
}

.blog-item-row {
  display: flex;
  align-items: flex-start;
}

.tab-add-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 200;
  padding: 16rpx 30rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #ffffff;
  box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.06);
}

.tab-add-btn {
  height: 88rpx;
  border-radius: 44rpx;
  background: linear-gradient(135deg, #ff943c, #ffb366);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6rpx 16rpx rgba(255, 148, 60, 0.3);
}

.tab-add-btn-hover {
  opacity: 0.92;
}

.tab-add-text {
  font-size: 30rpx;
  font-weight: bold;
  color: #fff;
}

.plan-popup {
  padding: 32rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
}

.plan-popup-title {
  text-align: center;
  font-size: 32rpx;
  font-weight: bold;
  color: #1a1a1a;
  margin-bottom: 24rpx;
}

.plan-popup-msg {
  text-align: center;
  font-size: 28rpx;
  color: #606266;
  margin-bottom: 24rpx;
  padding: 0 16rpx;
}

.plan-option {
  padding: 28rpx 24rpx;
  text-align: center;
  font-size: 30rpx;
  color: #303133;
  border-bottom: 1rpx solid #f0f0f0;
}

.plan-option:active {
  background: #f7f8fa;
}

.plan-cancel {
  margin-top: 16rpx;
  padding: 24rpx;
  text-align: center;
  font-size: 28rpx;
  color: #909399;
}
</style>
