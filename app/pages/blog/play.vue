<template>
  <view>
    <u-navbar title="文章阅读" :border="true" :placeholder="true" @leftClick="goBack()" :autoBack="false"></u-navbar>
    <!--<view class="bkimg" :style="{ 'background-image': 'url(' + fileUrl+pobj.img + ')' }">
    </view>-->
    <view style="background-color: #fff;height: 60rpx;margin-bottom: 30rpx;">
      <u-tabs :list="[{id:2,title:'正常字体'},{id:1,title:'小字体'},{id:3,title:'大字体'}]" keyName="title" @change="onTabChange"></u-tabs>
    </view>
    <scroll-view :enable-flex="true" class="svcontainer">
      <view style="z-index: 999;">
        <!--<view style="z-index: 999;text-align: center;">{{playlist[pindex].title}}</view>-->
        <!--<view style="text-align: center;margin-top: 50rpx;">
          <image @click="togglePlay"  :class="xuanzhuan" style="z-index: 999;border-radius: 50%;width: 80%" :src="fileUrl+playlist[pindex].img" mode="widthFix"></image>
        </view>-->
        <!--<view style="text-align: center;">
          <audioQuickPlay :url="fileUrl+playlist[pindex].music"></audioQuickPlay>
          <audio id="myVideo" @play="xuanzhuan" @ :poster="fileUrl+playlist[pindex].img" :author="playlist[pindex].zuozhe" :name="playlist[pindex].gname" :controls="true" :src="fileUrl+playlist[pindex].music"></audio>
        </view>
        <view class="audo-video">
          <video id="myVideo" :src="fileUrl+playlist[pindex].music" class="hidden" @timeupdate="timeupdate" ref="video" @loadedmetadata="loadedmetadata" ></video>

          <view class="slider-box">
            <text class="mm">{{timer}}</text>
            <slider
                    style="width: 500upx;"
                    @change="sliderChange"
                    @changing="sliderChanging"
                    class="audio-slider"
                    block-size="16"
                    :min="0"
                    :max="duration"
                    :value="currentTime"
                    activeColor="#ff7400"
                    @touchstart="lock= true"
                    @touchend="lock = false"
            />
            <text class="ss">{{overTimer}}</text>
          </view>
          -->
          <!--<button @tap="play">播放</button>
          <button @tap="stop">暫停</button>
          <button @tap="setRate(0.5)">0.5倍</button>
          <button @tap="setRate(0.75)">0.75倍</button>
          <button @tap="setRate(1)">1倍</button>
          <button @tap="setRate(1.5)">1.5倍</button>
          <button @tap="setRate(2)">2倍</button>


        </view>-->
        <view :class="fclass">
          <u-parse style="z-index: 999;" :content="playlist[pindex].note"></u-parse>
        </view>

      </view>
      <view class="footerctn" style="background-color: rgba(0,0,0,0.1)">
        <view style="display: flex;width: 100%;justify-content: space-around;">
          <view style="width: 300rpx">
              <u-button @click="pre" type="warning" text="上一篇"></u-button>
          </view>
          <view style="width: 300rpx">
            <u-button @click="next" type="success" text="下一篇"></u-button>
          </view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
  import {
    listj,
    findj,
    savej,
    deletej,
    fileUrl
  } from '@/common/config/api.js';
  import {
    mapState,
    mapActions
  } from 'vuex';

  export default {
    components:{},
    data() {
      return {
        fileUrl: fileUrl,
        playlist: [{}],
        pindex:0,
        audioCtx:null,


        lock: false, // 锁
        status: 1, // 1暂停 2播放
        currentTime: 0,  //当前进度
        duration: 100, // 总进度
        videoContext: '',
        xuanzhuan:'',
        pobj:{},
        fclass:'normalfont'
      };
    },
    onLoad(params) {
      //this.videoContext = uni.createVideoContext('myVideo')
      let pid = params.pid
      findj({params: {table: 'blog', id: pid}}).then(pobj => {
        this.pobj = pobj
        listj({params: {table: 'zhangjie',pid:this.pobj.id}}).then(res => {
          for (let s of res) {
              s.note = uni.itool.getHtmlNote(s.note)
          }
          this.playlist = res
        })
      })

    },
    methods: {
      onTabChange (item){
          if(item.id==1){
            this.fclass = 'smallfont'
          }else if(item.id == 2){
            this.fclass = 'normalfont'
          }else if(item.id == 3){
            this.fclass = 'bigfont'
          }
      },
      togglePlay(){
        if (this.status==1) {
          this.play()
        }else{
          this.stop()
        }
      },
      next(){
        //this.stop()
        if (this.pindex < this.playlist.length-1) {
          this.pindex++
        } else {
          uni.showToast({
            icon:'none',
            title: '没有下一页了'
          })
        }
      },
      pre(){
        //this.stop()
        if (this.pindex > 0) {
          this.pindex--; //上一页
        } else {
          uni.showToast({
            icon:'none',
            title: '没有上一页了'
          })
        }
      },
      goBack() {
        uni.itool.nto({
          url:'/pages/index/index'
        })
      },
      calcTimer(timer) {
        if(timer === 0 || typeof timer !== 'number') {
          return '00:00'
        }
        let mm = Math.floor(timer / 60)
        let ss = Math.floor(timer % 60)
        if(mm < 10) {
          mm = '0' + mm
        }
        if(ss < 10) {
          ss = '0' + ss
        }
        return mm + ':' + ss
      },
      // 倍速
      setRate(num) {
        this.videoContext.playbackRate(num)
      },

      // 播放
      play() {
        this.status = 2
        this.videoContext.play()
      },

      // 暂停
      stop() {
        this.videoContext.pause()
        this.status = 1
      },

      // 更新进度条
      timeupdate(event) {
        if(this.lock) return; // 锁
        var currentTime,duration;
        if(event.detail.detail) {
          currentTime = event.detail.detail.currentTime
          duration = event.detail.detail.duration
        }else {
          currentTime = event.detail.currentTime
          duration = event.detail.duration
        }
        this.currentTime = currentTime
        this.duration = duration
      },

      // 拖动进度条
      sliderChange(data) {
        this.videoContext.seek(data.detail.value)
      },

      //拖动中
      sliderChanging(data) {
        this.currentTime = data.detail.value
      },

      // 视频加载完成
      loadedmetadata(data) {
        this.duration = data.detail.duration
      }
    },
    computed:{
      timer() {
        return this.calcTimer(this.currentTime)
      },
      overTimer() {
        return this.calcTimer(this.duration)
      }
    },
    watch:{
        status:{
            immediate:true,
            deep:true,
            handler(news){
                if (news==1) {
                  this.xuanzhuan = ''
                }else{
                  this.xuanzhuan = 'xuanzhuan'
                }
            }
        }
    }
  }
</script>

<style lang="scss">
  .bigfont{
    font-size: 60rpx;
  }
  .normalfont{
    font-size: 30rpx;
  }
  .smallfont{
    font-size: 25rpx;
  }
  .bkimg{
    -webkit-filter: blur(10rpx); /* Chrome, Safari, Opera */
    filter: blur(10rpx);
    width: 100vw;
    height: 100vh;
    position: absolute;
    z-index: 0;
    opacity: 0.8;
  }


  .xuanzhuan{
    animation: spin 5s linear infinite;
  }
  .xuanzhuanstop{
    animation: spin 0s linear infinite;
  }
  @keyframes spin {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
      transition: all 5s;
    }

  }




  .audo-video {
    padding-bottom: 20upx;
  }
  .slider-box {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 26upx;
    color: #999;
  }

  button {
    display: inline-block;
    width: 100upx;
    background-color: #fff;
    font-size: 24upx;
    color: #000;
    padding: 0;
  }
  .hidden {
    position: fixed;
    z-index: -1;
    width: 1upx;height: 1upx;
  }
</style>
