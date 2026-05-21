<template>
  <view class="u-search-stub">
    <input
      class="u-search-stub-input"
      type="text"
      :placeholder="placeholder"
      :value="value"
      @input="$emit('input', $event.detail.value)"
      @confirm="$emit('change', $event)"
    />
  </view>
</template>

<script>
export default {
  name: 'u-search',
  props: {
    value: { type: String, default: '' },
    placeholder: { type: String, default: '搜索' },
    showAction: { type: Boolean, default: false },
    bgColor: { type: String, default: '#f5f5f5' },
    shape: { type: String, default: 'round' }
  }
}
</script>

<style lang="scss" scoped>
.u-search-stub {
  padding: 16rpx 24rpx;
  background-color: #f5f5f5;
  border-radius: 32rpx;
}
.u-search-stub-input {
  width: 100%;
  height: 64rpx;
  font-size: 28rpx;
  color: #333;
}
</style>
