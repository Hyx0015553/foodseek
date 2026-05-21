/**
 * 普通用户在问 AI 助手「附近店铺」类问题时，附带小程序定位（gcj02，与首页推荐一致），供后端拼装 nearbyShopsKm。
 * 商家端不请求定位。
 */
export function withAssistantLocation(userInfo, baseParams) {
  return new Promise((resolve) => {
    if (!userInfo || String(userInfo.roletype) === '3') {
      resolve(baseParams)
      return
    }
    uni.getLocation({
      type: 'gcj02',
      success: (res) => {
        resolve({
          ...baseParams,
          lat: String(res.latitude),
          lng: String(res.longitude)
        })
      },
      fail: () => resolve(baseParams)
    })
  })
}
