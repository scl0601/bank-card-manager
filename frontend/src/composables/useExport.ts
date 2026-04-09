/**
 * 导出功能组合式函数
 * 封装 Excel 导出逻辑
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

export interface ExportOptions {
  /** 导出接口函数 */
  exportApi: (params: any) => Promise<any>
  /** 默认文件名 */
  fileName?: string
  /** 导出前回调 */
  beforeExport?: (params: any) => any | false
  /** 导出成功回调 */
  onSuccess?: () => void
}

export function useExport(options: ExportOptions) {
  const { exportApi, fileName = '导出数据', beforeExport, onSuccess } = options

  // 导出中状态
  const exporting = ref(false)

  // 执行导出
  const handleExport = async (params: any = {}) => {
    // 导出前处理
    let exportParams = { ...params }
    if (beforeExport) {
      const result = beforeExport(exportParams)
      if (result === false) return
      exportParams = result
    }

    exporting.value = true
    try {
      const blob = await exportApi(exportParams)

      // 创建下载链接
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `${fileName}_${formatDate(new Date())}.xlsx`

      // 触发下载
      document.body.appendChild(link)
      link.click()

      // 清理
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)

      ElMessage.success('导出成功')
      onSuccess?.()
    } catch (error: any) {
      console.error('导出失败:', error)
      ElMessage.error(error.message || '导出失败')
    } finally {
      exporting.value = false
    }
  }

  return {
    exporting,
    handleExport
  }
}

// 格式化日期
function formatDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}${month}${day}_${hours}${minutes}${seconds}`
}
