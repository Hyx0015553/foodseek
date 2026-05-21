<template>
  <view class="wc-root">
    <view v-if="ovalItems.length" class="wc-oval-outer">
      <view class="wc-oval-inner">
        <view
          v-for="(w, idx) in ovalItems"
          :key="idx"
          class="wc-oval-word"
          :style="w.styleText"
        >{{ w.name }}</view>
      </view>
    </view>
  </view>
</template>

<script>
function patchWordCloudSeriesData(arr) {
  if (!arr || !arr.length) return []
  return arr
    .filter((x) => x && x.name)
    .map((x) => ({
      name: String(x.name).trim(),
      value: Math.max(1, parseInt(x.value, 10) || 1)
    }))
    .filter((x) => x.name)
}

/** 纯文字词云：高饱和色；频次用非线性字号 + 字重拉开差距 */
function buildWordItemStyle(item, idx) {
  const vmax = item._vmax || 1
  const vmin = item._vmin || 1
  const v = Math.max(1, parseInt(item.value, 10) || 1)
  const t = vmax > vmin ? (v - vmin) / (vmax - vmin) : 1
  const te = Math.pow(Math.max(0, Math.min(1, t)), 1.28)
  const size = Math.round(16 + te * 32)
  const colors = ['#e53935', '#ff6d00', '#ffd600', '#00c853', '#00b0ff', '#7c4dff', '#ff4081']
  const name = String(item.name || '')
  let h = 0
  for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0
  const color = colors[(h + idx) % colors.length]
  const weight = t > 0.72 ? 800 : t > 0.4 ? 700 : t > 0.15 ? 600 : 500
  return `font-size:${size}rpx;font-weight:${weight};color:${color};background:transparent;padding:2rpx 8rpx;line-height:1.35;white-space:nowrap;max-width:48%;overflow:hidden;text-overflow:ellipsis;text-shadow:0 0 2rpx rgba(255,255,255,0.98),0 1rpx 4rpx rgba(15,23,42,0.18);`
}

/** 椭圆内分布：高频略靠中心，黄金角螺旋减少重叠 */
function layoutOvalPlaced(rows) {
  if (!rows.length) return []
  const sorted = rows.slice().sort((a, b) => b.value - a.value)
  const vals = sorted.map((r) => r.value)
  const vmax = Math.max.apply(null, vals)
  const vmin = Math.min.apply(null, vals)
  const n = sorted.length
  const golden = 2.39996322972865332
  const cx = 50
  const cy = 50
  /* 略放大椭圆轴 + 半径增长略缓，减少词与词挤叠 */
  const a = 43
  const b = 31
  return sorted.map((r, idx) => {
    const ext = { ...r, _vmax: vmax, _vmin: vmin }
    let x
    let y
    let z = n - idx + 2
    if (n === 1) {
      x = cx
      y = cy
    } else {
      const ang = idx * golden
      const rr = idx === 0 ? 0.12 : 0.08 + 0.9 * Math.pow(idx / (n - 1), 0.48)
      x = cx + a * rr * Math.cos(ang)
      y = cy + b * rr * Math.sin(ang)
    }
    const place = `position:absolute;left:${x.toFixed(1)}%;top:${y.toFixed(1)}%;transform:translate(-50%,-50%);z-index:${z};`
    const base = buildWordItemStyle(ext, idx)
    return { name: r.name, styleText: place + base }
  })
}

export default {
  name: 'ReplayWordCloud',
  props: {
    items: {
      type: Array,
      default() {
        return []
      }
    }
  },
  computed: {
    ovalItems() {
      const rows = patchWordCloudSeriesData(this.items || [])
      return layoutOvalPlaced(rows)
    }
  }
}
</script>

<style scoped>
.wc-root {
  width: 100%;
  min-height: 120rpx;
}
.wc-oval-outer {
  width: 100%;
  padding: 8rpx 0 20rpx;
  box-sizing: border-box;
}
/* 横向椭圆容器：宽 > 高 + border-radius:50% */
.wc-oval-inner {
  position: relative;
  width: 100%;
  height: 288rpx;
  border-radius: 50%;
  background: linear-gradient(180deg, #fbfcfe 0%, #f4f6f9 100%);
  border: 1rpx solid #e8ecf1;
  overflow: hidden;
  box-sizing: border-box;
}
.wc-oval-word {
  box-sizing: border-box;
}
</style>
