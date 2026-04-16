/**
 * 设备检测工具函数
 */

/** 检测当前设备是否为移动端 */
export function isMobileDevice(): boolean {
  if (typeof navigator === 'undefined') return false
  const ua = navigator.userAgent.toLowerCase()
  return /android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini|mobile/i.test(ua)
}

// 全局类型声明
declare global {
  interface Window {
    __IS_MOBILE__: boolean | undefined
  }
}
