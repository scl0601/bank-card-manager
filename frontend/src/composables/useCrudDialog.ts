/**
 * CRUD 对话框组合式函数
 * 封装新增/编辑对话框的通用逻辑
 */
import { ref, reactive, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'

export interface CrudDialogOptions<T extends Record<string, any>> {
  /** 表单初始数据 */
  defaultForm: T
  /** 新增接口 */
  addApi?: (data: T) => Promise<any>
  /** 编辑接口 */
  updateApi?: (data: T) => Promise<any>
  /** 详情接口（编辑时获取数据） */
  getByIdApi?: (id: number | string) => Promise<{ data: T }>
  /** 表单验证规则 */
  rules?: FormRules
  /** 新增成功回调 */
  onAddSuccess?: () => void
  /** 编辑成功回调 */
  onUpdateSuccess?: () => void
  /** 提交前处理 */
  beforeSubmit?: (data: T, isEdit: boolean) => T | false
  /** 是否在编辑时获取详情 */
  fetchDetailOnEdit?: boolean
}

export function useCrudDialog<T extends Record<string, any>>(options: CrudDialogOptions<T>) {
  const {
    defaultForm,
    addApi,
    updateApi,
    getByIdApi,
    rules,
    onAddSuccess,
    onUpdateSuccess,
    beforeSubmit,
    fetchDetailOnEdit = true
  } = options

  // 对话框可见性
  const dialogVisible = ref(false)

  // 是否编辑模式
  const isEdit = ref(false)

  // 提交中状态
  const submitting = ref(false)

  // 表单引用
  const formRef = ref<FormInstance>()

  // 表单数据
  const formData = reactive<T>({ ...defaultForm } as T)

  // 对话框标题
  const dialogTitle = computed(() => isEdit.value ? '编辑' : '新增')

  // 打开新增对话框
  const openAdd = () => {
    isEdit.value = false
    resetForm()
    dialogVisible.value = true
  }

  // 打开编辑对话框
  const openEdit = async (row: any) => {
    isEdit.value = true
    resetForm()

    // 如果提供了详情接口且需要获取详情
    if (fetchDetailOnEdit && getByIdApi && row.id) {
      try {
        const { data } = await getByIdApi(row.id)
        Object.assign(formData, data)
      } catch (error) {
        console.error('获取详情失败:', error)
        ElMessage.error('获取详情失败')
        return
      }
    } else {
      // 直接使用传入的数据
      Object.assign(formData, row)
    }

    dialogVisible.value = true
  }

  // 重置表单
  const resetForm = () => {
    // 清空表单数据
    Object.keys(formData).forEach(key => {
      delete formData[key]
    })
    Object.assign(formData, defaultForm)

    // 清空验证
    formRef.value?.resetFields()
  }

  // 提交表单
  const handleSubmit = async () => {
    // 如果有 formRef 且需要验证则验证；否则跳过（由 CrudDialog 组件已负责验证）
    if (formRef.value) {
      try {
        await formRef.value.validate()
      } catch {
        return
      }
    }

    // 提交前处理
    let submitData: any = { ...formData }
    if (beforeSubmit) {
      const result = beforeSubmit(submitData, isEdit.value)
      if (result === false) return
      submitData = result
    }

    submitting.value = true
    try {
      if (isEdit.value) {
        // 编辑
        if (!updateApi) {
          ElMessage.error('未配置编辑接口')
          return
        }
        await updateApi(submitData)
        ElMessage.success('编辑成功')
        onUpdateSuccess?.()
      } else {
        // 新增
        if (!addApi) {
          ElMessage.error('未配置新增接口')
          return
        }
        await addApi(submitData)
        ElMessage.success('新增成功')
        onAddSuccess?.()
      }

      closeDialog()
    } catch (error: any) {
      console.error('提交失败:', error)
      ElMessage.error(error.message || '操作失败')
    } finally {
      submitting.value = false
    }
  }

  // 关闭对话框
  const closeDialog = () => {
    dialogVisible.value = false
    resetForm()
  }

  // 对话框关闭前回调
  const handleDialogClose = (done: () => void) => {
    if (submitting.value) {
      return
    }
    done()
  }

  return {
    // 状态
    dialogVisible,
    isEdit,
    submitting,
    formRef,
    formData,
    dialogTitle,
    rules,

    // 方法
    openAdd,
    openEdit,
    handleSubmit,
    closeDialog,
    handleDialogClose,
    resetForm
  }
}
