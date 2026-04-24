import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { updateUserFeeRateApi } from '@/api/card'

/**
 * 费率编辑管理 composable
 * 用于处理费率的内联编辑功能
 */
export function useFeeRateEdit() {
  const editingId = ref<number | null>(null)
  const editingValue = ref(0)
  const saving = ref(false)

  /**
   * 开始编辑费率
   */
  function startEdit(id: number, currentRate: number) {
    editingId.value = id
    editingValue.value = currentRate
  }

  /**
   * 取消编辑
   */
  function cancelEdit() {
    editingId.value = null
    editingValue.value = 0
  }

  /**
   * 保存费率
   */
  async function saveFeeRate(onSuccess?: () => void) {
    if (editingId.value === null) return

    if (editingValue.value < 0 || editingValue.value > 100) {
      ElMessage.warning('费率必须在 0-100 之间')
      return
    }

    saving.value = true
    try {
      await updateUserFeeRateApi({ userId: editingId.value, feeRate: editingValue.value })
      ElMessage.success('费率修改成功')
      editingId.value = null
      onSuccess?.()
    } catch (error: any) {
      ElMessage.error(error.message || '费率修改失败')
    } finally {
      saving.value = false
    }
  }

  /**
   * 检查是否正在编辑指定ID
   */
  function isEditing(id: number) {
    return editingId.value === id
  }

  return {
    editingId,
    editingValue,
    saving,
    startEdit,
    cancelEdit,
    saveFeeRate,
    isEditing
  }
}
