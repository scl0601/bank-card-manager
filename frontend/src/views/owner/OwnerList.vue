<template>
  <div>
    <div class="page-title">持卡人管理</div>

    <!-- 筛选区 -->
    <SearchBar :model-value="query" :hide-search-button="true" @search="handleSearch" @reset="resetQuery">
      <el-form-item label="姓名">
        <el-input v-model="query.name" placeholder="请输入姓名" clearable style="width:160px" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="query.phone" placeholder="请输入手机号" clearable style="width:160px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
          <el-option v-for="item in OWNER_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <template #extra-buttons>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增持卡人</el-button>
      </template>
    </SearchBar>

    <!-- 表格区 -->
    <PageTable
      :data="list"
      :loading="loading"
      :total="total"
      :page-num="query.pageNum"
      :page-size="query.pageSize"
      border
      @update:page-num="val => query.pageNum = val"
      @update:page-size="val => query.pageSize = val"
      @current-change="handleCurrentChange"
      @size-change="handleSizeChange"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="idCard" label="身份证" width="200" />
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <StatusTag :value="row.status" :label-map="OWNER_STATUS_MAP" :type-map="OWNER_STATUS_TAG_TYPE" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
            <template #reference><el-button type="danger" link>删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </PageTable>

    <!-- 新增/编辑弹窗 -->
    <CrudDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :form-data="formData"
      :rules="rules"
      :loading="submitting"
      :is-edit="isEdit"
      width="500px"
      @confirm="handleSubmit"
    >
      <template #default="{ form }">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" :placeholder="isEdit ? '不修改请留空' : '请输入手机号'" />
        </el-form-item>
        <el-form-item label="身份证">
          <el-input v-model="form.idCard" :placeholder="isEdit ? '不修改请留空' : '请输入身份证号'" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio v-for="item in OWNER_STATUS_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" rows="3" />
        </el-form-item>
      </template>
    </CrudDialog>
  </div>
</template>

<script setup lang="ts">
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { formatTime } from '@/utils/formatters'
import SearchBar from '@/components/SearchBar/index.vue'
import PageTable from '@/components/PageTable/index.vue'
import CrudDialog from '@/components/CrudDialog/index.vue'
import StatusTag from '@/components/StatusTag/index.vue'
import { getOwnerPageApi, getOwnerDetailApi, saveOwnerApi, updateOwnerApi, deleteOwnerApi } from '@/api/owner'
import { usePageTable } from '@/composables/usePageTable'
import { useCrudDialog } from '@/composables/useCrudDialog'
import { OWNER_STATUS_OPTIONS, OWNER_STATUS_MAP, OWNER_STATUS_TAG_TYPE } from '@/constants/dict'

const defaultForm = { id: undefined as number | undefined, name: '', phone: '', idCard: '', remark: '', status: 0 }

const {
  loading, list, total, query,
  handleSearch, resetQuery, handleCurrentChange, handleSizeChange
} = usePageTable({
  fetchApi: getOwnerPageApi,
  defaultQuery: { name: '', phone: '', status: undefined as number | undefined },
  autoSearch: true,
  beforeFetch: (params) => {
    // 后端使用 current/size 分页参数，做适配
    ;(params as any).current = params.pageNum
    ;(params as any).size = params.pageSize
    delete (params as any).pageNum
    delete (params as any).pageSize
  }
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{
    validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
      if (!isEdit.value && !value) { callback(new Error('请输入手机号')); return }
      if (!value || /^1[3-9]\d{9}$/.test(value)) { callback() }
      else { callback(new Error('手机号格式不正确')) }
    },
    trigger: 'blur'
  }]
}

const {
  dialogVisible, isEdit, submitting, formData, dialogTitle,
  openAdd, openEdit, handleSubmit: submitForm
} = useCrudDialog({
  defaultForm,
  addApi: saveOwnerApi,
  updateApi: updateOwnerApi,
  getByIdApi: getOwnerDetailApi as any,
  rules,
  fetchDetailOnEdit: true,
  onAddSuccess: () => handleSearch(),
  onUpdateSuccess: () => handleSearch(),
  beforeSubmit: (data) => {
    // 编辑模式：清空敏感字段让用户重新填写
    if (isEdit.value) return { ...data, phone: data.phone || '', idCard: data.idCard || '' }
    return data
  }
})

async function handleSubmit() {
  await submitForm()
}

async function handleDelete(id: number) {
  await deleteOwnerApi(id)
  ElMessage.success('删除成功')
  handleSearch()
}
</script>
