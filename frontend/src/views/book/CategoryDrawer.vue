<template>
  <div class="category-drawer">
    <!-- 工具栏 -->
    <div class="drawer-toolbar">
      <el-button type="primary" :icon="Plus" size="small" @click="openAdd">新增分类</el-button>
    </div>

    <!-- 分类树形列表 -->
    <el-table :data="flatCategoryList" border size="small" row-key="id" v-loading="loading"
      default-expand-all :tree-props="{ children: 'children' }" max-height="calc(100vh - 180px)">
      <el-table-column prop="name" label="分类名称" min-width="120" />
      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }">
          <StatusTag :value="row.type" :label-map="{ 1: '收入', 2: '支出' }"
            :type-map="{ 1: 'success', 2: 'danger' }" size="small" />
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="60" align="center" />
      <el-table-column label="状态" width="70" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'info'" size="small">{{ row.status === 0 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确认删除该分类？" @confirm="handleDelete(row.id)">
            <template #reference><el-button type="danger" link size="small">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <CrudDialog
      v-model="dialogVisible"
      :title="dialogTitle + '分类'"
      :form-data="formData"
      :rules="rules"
      :loading="submitting"
      :is-edit="isEdit"
      width="420px"
      @confirm="handleSubmit"
    >
      <template #default="{ form }">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">收入</el-radio>
            <el-radio :value="2">支出</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="父级分类">
          <el-select v-model="form.parentId" placeholder="无（一级分类）" clearable style="width:100%">
            <el-option v-for="item in parentOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="0" :inactive-value="1"
            active-text="启用" inactive-text="停用" inline-prompt />
        </el-form-item>
      </template>
    </CrudDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import CrudDialog from '@/components/CrudDialog/index.vue'
import StatusTag from '@/components/StatusTag/index.vue'
import {
  getCategoryListApi, saveCategoryApi, updateCategoryApi, deleteCategoryApi
} from '@/api/book'

const emit = defineEmits(['refresh'])

const loading = ref(false)
const treeList = ref<any[]>([])

/** 展平为表格数据（保留树形结构） */
const flatCategoryList = computed(() => {
  return [...treeList.value].sort((a, b) => a.sortOrder - b.sortOrder)
})

// 父级选项：根据当前表单的 type 过滤一级分类
const parentOptions = computed(() => {
  if (!formData.type) return []
  return treeList.value
    .filter((c: any) => c.parentId === 0 && c.type === formData.type && c.id !== formData.id)
})

// ========== 加载 ==========
async function loadData() {
  loading.value = true
  try {
    const res: any = await getCategoryListApi(undefined, false)
    // 扁平化：将 children 提取到顶层，保持 parentId 关系用于树形展示
    const list = res.data || []
    const flat: any[] = []
    for (const item of list) {
      flat.push(item)
      if (item.children) {
        flat.push(...item.children)
      }
    }
    treeList.value = flat
  } catch { /* ignore */ }
  finally { loading.value = false }
}

// ========== 弹窗表单 ==========
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const dialogTitle = ref('')
const formData = reactive({
  id: undefined as number | undefined,
  name: '',
  type: 2,
  sortOrder: 0,
  parentId: undefined as number | undefined,
  status: 0
})
const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择分类类型', trigger: 'change' }]
}

function openAdd() {
  isEdit.value = false; dialogTitle.value = '新增'
  Object.assign(formData, { id: undefined, name: '', type: 2, sortOrder: 0, parentId: undefined, status: 0 })
  dialogVisible.value = true
}

function openEdit(row: any) {
  isEdit.value = true; dialogTitle.value = '编辑'
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    type: row.type,
    sortOrder: row.sortOrder || 0,
    parentId: row.parentId === 0 ? undefined : row.parentId,
    status: row.status ?? 0
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCategoryApi({ ...formData })
    } else {
      await saveCategoryApi({ ...formData })
    }
    ElMessage.success('操作成功')
    dialogVisible.value = false
    loadData()
    emit('refresh')
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  await deleteCategoryApi(id)
  ElMessage.success('删除成功')
  loadData()
  emit('refresh')
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.category-drawer {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.drawer-toolbar {
  margin-bottom: 12px;
}
</style>
