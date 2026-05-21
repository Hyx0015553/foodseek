<template>
	<view class="imglist-container">
		<!-- 搜索栏 -->
		<view v-show="showSearch" class="hpaddingctn search-wrapper">
      <view class="searchctn" :style="{'background-color':bgcolor}">
        <u--input v-model="searchstr" @input="toggleSearch"
                  :placeholder="searchPlace" prefixIcon="search"
                  :prefixIconStyle="{fontSize: '22px', color: '#909399'}" 
                  border="none"
                  customStyle="background-color: transparent;"
        />
      </view>
		</view>

		<!-- 模式3: 卡片大图模式 -->
		<block v-if="imgSize==3">
			<block v-for="(item,index) in localList" :key="item[idName]">
        <view v-show="toggleShow(item.isShow)" class="card-item animate-fade-in">
				<view v-if="htitleName" class="htitlestatus header-row">
					<view @tap="fatherFun(item[idName])" class="htitle text-ellipsis">
						{{htitleLabel}}{{item[htitleName]}}
					</view>
					<view class="hstatus" :style="{'color': hsColor}">
						{{hsLabel}}{{getSimpleText(item[hsName])}}
					</view>
				</view>
				<view class="content-body" :class="'itemStyle'+sType">
					<view @tap="fatherFun(item[idName])" class="main-title text-ellipsis" :style="{'color': titleColor}">
						{{titleLabel}}{{item[titleName]}}
					</view>
					<view @tap="fatherFun(item[idName])" v-if="item[imgName]" class="image-wrapper-lg">
						<image class="blog_cover3" mode="aspectFill" :src="fileUrl+item[imgName]"></image>
					</view>
					<view v-if="sName" class="sub-text" :style="{'color': sColor}">
						{{sLabel}}{{getSimpleText(item[sName])}}
					</view>
					<view v-if="tName" class="tag-text" :style="{'color': tColor}">
						{{tLabel}}{{item[tName]}}
					</view>
					<view class="opctn2" v-if="oLabel">
						<view class="opitem">
							<u-button size="mini" shape="circle" :plain="true" type="primary" @tap="opFunction(item[idName])"
								:text="oLabel"></u-button>
						</view>
					</view>
				</view>
			</view>	
			</block>
		</block>

		<!-- 模式2: 左图右文 (大图) -->
		<block v-if="imgSize==2">
			<block v-for="(item,index) in localList" :key="item[idName]">
        <view v-show="toggleShow(item.isShow)" class="card-item animate-fade-in">
				<view v-if="htitleName" class="htitlestatus header-row">
					<view @tap="fatherFun(item[idName])" class="htitle text-ellipsis">
						{{htitleLabel}}{{item[htitleName]}}
					</view>
					<view class="hstatus" :style="{'color': hsColor}">
						{{hsLabel}}{{getSimpleText(item[hsName])}}
					</view>
				</view>
				<view class="flex-row content-body"
					:style="{'flex-direction': (fx?'row-reverse':'row')}"
					:class="(sshadow ? 'fshadow':'')">
					<view @tap="fatherFun(item[idName])" class="item_img2" v-if="item[imgName]">
						<image class="blog_cover2" mode="aspectFill" :src="fileUrl+item[imgName]"></image>
					</view>
					<view class="item_txt2">
						<view @tap="fatherFun(item[idName])" class="titlectn2 text-ellipsis-2" :style="{'color': titleColor}">
							{{titleLabel}}{{item[titleName]}}
						</view>
						<view class="sctn2 text-ellipsis-2" :style="{'color': sColor}">
							{{sLabel}}{{getSimpleText(item[sName])}}
						</view>
						<view :style="{'color': tColor}" v-if="tName" class="tctn2">
							{{tLabel}}{{item[tName]}}
						</view>
						<view class="opctn2" v-if="oLabel">
							<view class="opitem">
								<u-button size="mini" shape="circle" :plain="true" type="primary" @tap="opFunction(item[idName])"
									:text="oLabel"></u-button>
							</view>
						</view>
						<view class="opctn2" v-if="cbox">
							<view class="opitem">
								<u-checkbox :name="item[idName]+''" class="imglistcheck"></u-checkbox>
							</view>
						</view>
					</view>

				</view>
			    </view>
			</block>
		</block>

		<!-- 模式: 题目结果 (timuresult) -->
		<block v-if="imgSize=='timuresult'">
			<block v-for="(item,index) in localList" :key="item[idName]">
				<view v-show="toggleShow(item.isShow)" class="card-item animate-fade-in"
					:style="{'flex-direction': (fx?'row-reverse':'row')}">
					<view class="flex-row content-body">
            <view @tap="fatherFun(item[idName])" class="item_img2" v-if="item[imgName]">
              <image class="blog_cover2" mode="aspectFill" :src="fileUrl+item[imgName]"></image>
            </view>
            <view class="item_txt2">
              <view @tap="fatherFun(item[idName])" class="titlectn2 text-ellipsis-2" :style="{'color': titleColor}">
                {{titleLabel}}{{item[titleName]}}
              </view>
              <view class="sctn2 text-ellipsis" :style="{'color': sColor}">
                {{sLabel}}{{getSimpleText(item[sName])}}
              </view>
              <view :style="{'color': item[tName]=='答对'?'#19BE6B':'#FA3534'}" v-if="tName" class="tctn2 font-bold">
                {{tLabel}}{{item[tName]}}
              </view>
              <view class="opctn2" v-if="oLabel">
                <view class="opitem">
                  <u-button size="mini" shape="circle" :plain="true" type="primary" @tap="opFunction(item[idName])"
                    :text="oLabel"></u-button>
                </view>
              </view>
            </view>
          </view>
				</view>
			</block>
		</block>

		<!-- 模式1: 左图右文 (小图/列表模式) -->
		<block v-if="imgSize==1">
			<block v-for="(item,index) in localList" :key="item[idName]">
        <view v-show="toggleShow(item.isShow)" class="card-item animate-fade-in">
				<view v-if="htitleName" class="htitlestatus header-row">
					<view @tap="fatherFun(item[idName])" class="htitle text-ellipsis">
						{{htitleLabel}}{{item[htitleName]}}
					</view>
					<view class="hstatus" :style="{'color': hsColor}">
						{{hsLabel}}{{getSimpleText(item[hsName])}}
					</view>
				</view>
				<view class="flex-row content-body list-mode"
					:style="{'flex-direction': (fx?'row-reverse':'row')}">
					<view @tap="fatherFun(item[idName])" class="item_img" v-if="item[imgName]">
						<image class="blog_cover" mode="aspectFill" :style="{'border-radius': round?'50%':'8rpx'}"
							:src="fileUrl+item[imgName]"></image>
					</view>
					<view class="item_txt">
						<view @tap="fatherFun(item[idName])" class="titlectn text-ellipsis" :style="{'color': titleColor}">
							{{titleLabel}}{{item[titleName]}}
						</view>

						<view v-if="tName" class="tctn text-ellipsis" :style="{'color': tColor}">
							{{tLabel}}{{getSimpleText(item[tName])}}
						</view>
						<view v-if="oLabel" class="opctn2">
							<view class="opitem">
								<u-button size="mini" shape="circle" :plain="true" type="primary" @tap="opFunction(item[idName])"
									:text="oLabel"></u-button>
							</view>
						</view>
						<view class="opctn2" v-if="cbox">
							<view class="opitem">
								<u-checkbox :name="item[idName]+''" :checked="item.checked"
									class="imglistcheck"></u-checkbox>
							</view>
						</view>
					</view>
          </view>
				</view>
			</block>
		</block>

    <!-- 模式: 绘画 (huihua) -->
		<block v-if="imgSize=='huihua'">
			<block v-for="(item,index) in localList" :key="item[idName]">
				<view v-show="toggleShow(item.isShow)" class="card-item animate-fade-in"
					:style="{'flex-direction': (fx?'row-reverse':'row')}">
          <view class="flex-row content-body list-mode">
            <view @tap="fatherFun(item[idName])" class="item_img" v-if="item[imgName]">
              <image class="blog_cover" mode="aspectFill" :style="{'border-radius': round?'50%':'8rpx'}"
                :src="fileUrl+item[imgName]"></image>
            </view>
            <view class="item_txt">
              <view @tap="fatherFun(item[idName])" class="titlectn text-ellipsis" :style="{'color': titleColor}">
                {{titleLabel}}{{item[titleName]}}
              </view>

              <view v-if="tName" class="tctn" :style="{'color': tColor}">
                {{tLabel}}{{getSimpleText(item[tName])}}
              </view>
              <view v-if="oLabel" class="opctn2">
                <view class="opitem">
                  <u-button size="mini" shape="circle" :plain="true" type="primary" @tap="opFunction(item[idName])"
                    :text="oLabel"></u-button>
                </view>
              </view>
              <view class="opctn2" v-if="cbox">
                <view class="opitem">
                  <u-checkbox :name="item[idName]+''" :checked="item.checked"
                    class="imglistcheck"></u-checkbox>
                </view>
              </view>
              <view v-if="item.type == 1" class="notification-dot"></view>
            </view>
          </view>
				</view>
			</block>
		</block>

		<!-- 模式10: 瀑布流 -->
		<block v-if="imgSize==10">
			<view :class="'waterfall'+cCount+''" class="waterfall-container">
				<view v-show="toggleShow(item.isShow)" class="waterfallitem0 card-item animate-fade-in"
					v-for="item in localList" :key="item[idName]">
					<image @tap="fatherFun(item[idName])" class="wimg0" v-if="item[imgName]" mode="widthFix"
						:src="fileUrl+item[imgName]"></image>
					<view @tap="fatherFun(item[idName])" class="wnote0 text-ellipsis-2" :style="{'color': titleColor}">
						{{titleLabel}}{{item[titleName]}}</view>
					<view class="wnote0 sub-text" :style="{'color': sColor}" v-if="sName">
						{{sLabel}}{{getSimpleText(item[sName])}}</view>
					<view class="wprice0 price-text" :style="{'color': tColor}" v-if="tName">{{tLabel}}{{item[tName]}}</view>
					<view class="opctn2" v-if="oLabel">
						<view class="opitem">
							<u-button size="mini" shape="circle" :plain="true" type="primary" @tap="opFunction(item[idName])"
								:text="oLabel"></u-button>
						</view>
					</view>
				</view>
			</view>
		</block>

		<!-- 模式20: u-cell 列表 -->
		<block v-if="imgSize==20">
			<view class="u-cell-group card-item animate-fade-in" style="padding: 0;">
				<u-cell v-if="item[imgName] && toggleShow(item.isShow)" :icon="fileUrl+item[imgName]"
					@tap="fatherFun(item[idName])" v-for="(item,index) in localList" :key="item[idName]"
					:title="item[titleName]" :value="item[tName]" :label="getSimpleText(item[sName])"
					:isLink="showArrow"></u-cell>
				<u-cell v-if="imgName==='' && toggleShow(item.isShow)" @tap="fatherFun(item[idName])"
					v-for="(item,index) in localList" :key="item[idName]" :title="item[titleName]" :value="item[tName]"
					:label="getSimpleText(item[sName])" :isLink="showArrow"></u-cell>
			</view>
		</block>

    <!-- 模式: good (商品) -->
		<block v-if="imgSize=='good'">
			<block v-for="(item,index) in dataList" :key="item[idName]">
        <view class="card-item animate-fade-in">
				<view v-if="htitleName" class="htitlestatus header-row">
					<view @tap="fatherFun(item[idName])" class="htitle text-ellipsis">
						{{htitleLabel}}{{item[htitleName]}}
					</view>
					<view class="hstatus" :style="{'color': hsColor}">
						{{hsLabel}}{{getSimpleText(item[hsName])}}
					</view>
				</view>
				<view v-show="toggleShow(item.isShow)"
					:style="{'flex-direction': (fx?'row-reverse':'row')}"
					class="flex-row content-body">
					<view @tap="fatherFun(item[idName])" class="item_img2" v-if="item[imgName]">
						<image class="blog_cover2" mode="aspectFill" :src="fileUrl+item[imgName]"></image>
					</view>
					<view class="item_txt2">
						<view @tap="fatherFun(item[idName])" class="titlectn2 text-ellipsis-2" :style="{'color': titleColor}">
							{{titleLabel}}{{item[titleName]}}
						</view>
						<view class="sctn2 text-ellipsis" :style="{'color': sColor}">
							{{sLabel}}{{getSimpleText(item[sName])}}
						</view>
						<view :style="{'color': tColor}" v-if="tName" class="tctn2 price-text">
							{{tLabel}}{{item[tName]}}
						</view>
						<view class="opctn2" v-if="oLabel">
							<view class="opitem">
								<u-button size="mini" shape="circle" :plain="true" type="primary" @tap="opFunction(item[idName])"
									:text="oLabel"></u-button>
							</view>
						</view>

					</view>
				</view>
        </view>
			</block>
		</block>

    <!-- 模式: pubu (简单瀑布流) -->
    <block v-if="imgSize=='pubu'">
      <!-- 这里的 class "waterfall-pubu-container" 是关键 -->
      <view class="waterfall-pubu-container">
        <view v-show="toggleShow(item.isShow)" @tap="fatherFun(item[idName])"
              class="pubu-card animate-fade-in"
              v-for="item in dataList" :key="item[idName]">
          <image v-if="item[imgName]" class="pubu-img" mode="widthFix" :src="fileUrl+item[imgName]"></image>
          <view class="pubu-content">
            <text class="pubu-title text-ellipsis-2">{{titleLabel}}{{item[titleName]}}</text>
            <view class="pubu-footer">
              <text v-if="sName" class="pubu-tag text-ellipsis">{{sLabel}}{{getSimpleText(item[sName])}}</text>
              <text v-if="tName" class="pubu-price">{{tLabel}}{{item[tName]}}</text>
            </view>
          </view>
        </view>
      </view>
    </block>

    <!-- 模式: bloglist (博客列表) -->
		<block v-if="imgSize=='bloglist'">
			<view v-show="toggleShow(item.isShow)" @tap="fatherFun(item.id)" v-for="(item,index) in localList"
				:key="item.id" class="blog_item card-item animate-fade-in">
				<view class="blog_top header-row">
					<view class="blog_avatar">
						<image class="blogavatar" mode="aspectFill" :src="fileUrl+item.uimg"></image>
					</view>
					<view class="blog_user">
						<text class="busername">{{item.username}}</text>
						<text class="busernote">{{item.ndate}}</text>
					</view>
				</view>
				<view class="blog_note text-content">
					{{item.title}}
				</view>
				<view v-if="item.img" class="image-wrapper-lg">
					<image class="blog_cover" mode="widthFix" :src="fileUrl+item.img"></image>
				</view>
				<view class="blog_op">
					<text> 👍{{item.zan}}</text>
				</view>
			</view>
		</block>
	</view>
</template>

<script>
	import {
		fileUrl
	} from '@/common/config/api.js';
	import {
		ideautil
	} from '@/common/commontools.js';
	export default {
		name: "imglist",
		options: {
			styleIsolation: 'shared'
		},
		props: {
			dataList: {
				type: Array,
				require: false,
				default: () => []
			},
			clickItem: {
				type: Function,
				default: null
			},
			clickOp: {
				type: Function,
				default: null
			},
			//显示checkbox
			cbox: {
				type: String,
				require: false,
				default: null
			},
			idName: {
				type: String,
				require: false,
				default: 'id'
			},
			titleName: {
				type: String,
				require: false,
				default: 'title'
			},
			titleLabel: {
				type: String,
				require: false,
				default: ''
			},
			titleColor: {
				type: String,
				require: false,
				default: ''
			},
			htitleName: {
				type: String,
				require: false,
				default: null
			},
			htitleLabel: {
				type: String,
				require: false,
				default: ''
			},
			htitleColor: {
				type: String,
				require: false,
				default: '#333'
			},
			searchPlace: {
				type: String,
				require: false,
				default: '请输入关键字'
			},
			sName: {
				type: String,
				require: false,
				default: ''
			},
			sLabel: {
				type: String,
				require: false,
				default: ''
			},
			hsName: {
				type: String,
				require: false,
				default: ''
			},
			hsLabel: {
				type: String,
				require: false,
				default: ''
			},
			sColor: {
				type: String,
				require: false,
				default: ''
			},
			hsColor: {
				type: String,
				require: false,
				default: 'orangered'
			},
			tName: {
				type: String,
				require: false,
				default: ''
			},
			tLabel: {
				type: String,
				require: false,
				default: ''
			},
			tColor: {
				type: String,
				require: false,
				default: ''
			},
			imgName: {
				type: String,
				require: false,
				default: ''
			},
			//图片的位置true在右边
			fx: {
				type: Boolean,
				require: false,
				default: false
			},
			imgSize: {
				type: [Number, String],
				require: false,
				default: 1
			},
			//列表条目显示类型,带阴影样式啥的
			sType: {
				type: [Number, String],
				require: false,
				default: 3
			},
			//瀑布流每行的个数
			cCount: {
				type: [Number, String],
				require: false,
				default: 2
			},
			//缩略图已废弃
			thumb: {
				type: String,
				require: false,
				default: null
			},
			//缩略图已废弃
			thumbSize: {
				type: String,
				require: false,
				default: 'medium'
			},
			showArrow: {
				type: Boolean,
				require: false,
				default: true
			},
			sshadow: {
				type: Boolean,
				require: false,
				default: true
			},
			showSearch: {
				type: Boolean,
				require: false,
				default: true
			},
			oLabel: {
				type: String,
				require: false,
				default: ''
			},
			oColor: {
				type: String,
				require: false,
				default: ''
			},
			round: {
				type: Boolean,
				require: false,
				default: false
			},
			htitlebar: {
				type: Boolean,
				require: false,
				default: false
			},
			bgcolor: {
				type: String,
				require: false,
				default: '#fff !important'
			}
		},
		methods: {
			fatherFun(tid) {
				this.$emit('clickItem', tid)
			},
			opFunction(tid) {
				this.$emit('clickOp', tid)
			},
			getSimpleText: ideautil.getSimpleText,
			toggleShow(ne) {
				if (ne === undefined) {
					return true
				} else {
					return ne
				}

			},
			toggleSearch(e) {
				let fd = uni.itool.FangDou()
				// Use localList instead of dataList to avoid prop mutation
				let lis = this.localList
				if (lis && lis.length) {
					fd(() => {
						for (let i = 0; i < lis.length; i++) {
							lis[i].isShow = true
						}
						let searchstr = this.searchstr
						if (searchstr) {
							for (let i = 0; i < lis.length; i++) {
								let title = lis[i][this.titleName] || '';
								let note = lis[i][this.sName] || '';
								// Safe check for string conversion
								title = title + '';
								note = note + '';
								
								if (title.indexOf(searchstr) != -1 || note.indexOf(searchstr) != -1) {
									lis[i].isShow = true
								} else {
									lis[i].isShow = false
								}
							}
						}
						// Trigger reactivity if needed, though localList mutation should work
						this.localList = [...lis]
					})
				}

			}
		},
		data() {
			return {
				fileUrl: fileUrl,
				searchstr: '',
				localList: []
			};
		},
		watch: {
			dataList: {
				handler(newVal) {
					if (newVal && newVal.length) {
						// Deep copy to avoid reference issues
						try {
							this.localList = JSON.parse(JSON.stringify(newVal)).map(item => {
								if (item.isShow === undefined) item.isShow = true;
								return item;
							});
						} catch (e) {
							console.error('Data cloning error:', e);
							this.localList = [];
						}
						
						if (this.searchstr) {
							this.toggleSearch();
						}
					} else {
						this.localList = [];
					}
				},
				immediate: true,
				deep: true
			}
		}
	}
</script>

<style scoped>
/* Common Styles */
.imglist-container {
  padding-bottom: 20upx;
}

.card-item {
  background-color: var(--bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  margin-bottom: var(--space-md);
  overflow: hidden;
  transition: all 0.3s ease;
}

.card-item:active {
  background-color: var(--bg-hover);
  transform: scale(0.99);
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-sm) var(--space-md);
  border-bottom: 1px solid var(--border-light);
  background-color: #FAFAFA;
}

.content-body {
  padding: var(--space-md);
}

.flex-row {
  display: flex;
  flex-direction: row;
}

.text-ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.text-ellipsis-2 {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* Animations */
.animate-fade-in {
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10upx); }
  to { opacity: 1; transform: translateY(0); }
}

/* Specific Mode Styles */
/* Mode 3 & General */
.htitle {
  font-size: var(--font-size-md);
  font-weight: bold;
  color: var(--text-main);
}

.hstatus {
  font-size: var(--font-size-sm);
}

.main-title {
  font-size: var(--font-size-lg);
  font-weight: bold;
  color: var(--text-main);
  margin-bottom: var(--space-sm);
}

.sub-text {
  font-size: var(--font-size-sm);
  color: var(--text-sub);
  margin-bottom: var(--space-xs);
}

.tag-text {
  font-size: var(--font-size-sm);
  color: var(--primary-color);
}

.image-wrapper-lg {
  width: 100%;
  margin: var(--space-sm) 0;
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.blog_cover3 {
  width: 100%;
  height: 350upx; /* Fixed height for consistency */
}

/* Mode 2 */
.list_item2 {
  display: flex;
}

.item_img2 {
  width: 220upx;
  height: 160upx;
  flex-shrink: 0;
  margin-right: var(--space-md);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.blog_cover2 {
  width: 100%;
  height: 100%;
}

.item_txt2 {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.titlectn2 {
  font-size: var(--font-size-md);
  font-weight: 500;
  color: var(--text-main);
  line-height: 1.4;
  margin-bottom: var(--space-xs);
}

.sctn2 {
  font-size: var(--font-size-sm);
  color: var(--text-content);
  margin-bottom: var(--space-xs);
}

.tctn2 {
  font-size: var(--font-size-xs);
  color: var(--text-sub);
}

/* Mode 1 & List Mode */
.list-mode {
  align-items: flex-start;
}

.item_img {
  width: 120upx;
  height: 120upx;
  flex-shrink: 0;
  margin-right: var(--space-md);
}

.blog_cover {
  width: 100%;
  height: 100%;
}

.item_txt {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 120upx;
}

.titlectn {
  font-size: var(--font-size-md);
  font-weight: 500;
  color: var(--text-main);
  margin-bottom: var(--space-xs);
}

.tctn {
  font-size: var(--font-size-sm);
  color: var(--text-sub);
}

/* Waterfall */
.waterfall-container {
  width: 100%;
  margin: 0 auto;
}

.waterfallitem0, .waterfallitem {
  break-inside: avoid;
  background-color: var(--bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  margin-bottom: var(--space-sm);
  padding-bottom: var(--space-sm);
}

.wimg0, .wimg {
  width: 100%;
  border-radius: var(--radius-md) var(--radius-md) 0 0;
}

.wnote0, .wnote {
  font-size: var(--font-size-sm);
  color: var(--text-main);
  padding: var(--space-xs) var(--space-sm);
  line-height: 1.4;
}

.wuserctn, .wprice0 {
  padding: 0 var(--space-sm) var(--space-xs);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.wname {
  font-size: var(--font-size-xs);
  color: var(--text-sub);
}

.wprice, .price-text {
  font-size: var(--font-size-sm);
  color: var(--error-color) !important;
  font-weight: bold;
}

/* Blog List */
.blog_top {
  display: flex;
  align-items: center;
  margin-bottom: var(--space-sm);
}

.blog_avatar {
  width: 80upx;
  height: 80upx;
  margin-right: var(--space-sm);
}

.blogavatar {
  width: 100%;
  height: 100%;
  border-radius: var(--radius-circle);
}

.blog_user {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.busername {
  font-size: var(--font-size-md);
  font-weight: bold;
  color: var(--text-main);
}

.busernote {
  font-size: var(--font-size-xs);
  color: var(--text-sub);
}

.blog_note {
  font-size: var(--font-size-md);
  color: var(--text-content);
  margin-bottom: var(--space-sm);
  padding: 0 var(--space-sm);
  line-height: 1.6;
}

.blog_op {
  padding: var(--space-sm);
  border-top: 1px solid var(--border-light);
  display: flex;
  justify-content: flex-end;
}

/* Operation Buttons */
.opctn2 {
  display: flex;
  flex-direction: row-reverse;
  margin-top: var(--space-xs);
}

.opitem {
  margin-left: var(--space-sm);
}

.search-wrapper {
  margin-bottom: var(--space-sm);
}

.notification-dot {
  position: absolute;
  top: var(--space-sm);
  right: var(--space-sm);
  width: 16upx;
  height: 16upx;
  background-color: var(--error-color);
  border-radius: var(--radius-circle);
}

.font-bold {
  font-weight: bold;
}

/* 瀑布流外层容器 */
.waterfall-pubu-container {
  padding: 20rpx;
  /* 核心属性：分为2列 */
  column-count: 2;
  /* 列与列之间的间距 */
  column-gap: 20rpx;
  width: 100%;
  box-sizing: border-box;
}

/* 瀑布流卡片 */
.pubu-card {
  /* 核心属性：防止卡片在分页处断开（小程序端尤为重要） */
  break-inside: avoid;
  /* 兼容性写法 */
  -webkit-column-break-inside: avoid;

  background-color: #ffffff;
  border-radius: 16rpx;
  margin-bottom: 20rpx; /* 卡片上下间距 */
  overflow: hidden;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
}

/* 图片样式 */
.pubu-img {
  width: 100%;
  /* 瀑布流必须用 widthFix 保证高度自适应 */
  height: auto;
  display: block;
}

/* 文字内容区域 */
.pubu-content {
  padding: 16rpx;
}

.pubu-title {
  font-size: 28rpx;
  color: #333;
  line-height: 1.4;
  font-weight: 500;
  margin-bottom: 12rpx;
  /* 限制两行省略，你在 CSS 中已有 text-ellipsis-2 样式 */
}

/* 底部价格和分类区域 */
.pubu-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pubu-tag {
  font-size: 22rpx;
  color: #999;
  background-color: #f5f5f5;
  padding: 2rpx 10rpx;
  border-radius: 4rpx;
  max-width: 60%;
}

.pubu-price {
  font-size: 30rpx;
  color: #ff4d4f;
  font-weight: bold;
}

/* 动画效果（可选） */
.animate-fade-in {
  animation: fadeIn 0.4s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10rpx); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
