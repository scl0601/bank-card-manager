<template>
  <div>
    <div class="page-title">银行卡管理</div>

    <!-- 筛选区 -->
    <SearchBar :model-value="query" :hide-search-button="true" @search="fetchData" @reset="resetQuery">
      <el-form-item label="银行"><el-input v-model="query.bankName" placeholder="银行名称" clearable style="width:140px" /></el-form-item>
      <el-form-item label="卡号后四位"><el-input v-model="query.cardNoLast4" placeholder="后四位" clearable style="width:120px" /></el-form-item>
      <el-form-item label="卡片类型">
        <el-select v-model="query.cardType" placeholder="全部" clearable style="width:120px">
          <el-option v-for="item in CARD_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <template #extra-buttons>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增银行卡</el-button>
        <el-button :icon="UserFilled" @click="openUserDialog()">管理用户</el-button>
      </template>
    </SearchBar>

    <!-- 用户分组表格 -->
    <div v-loading="loading" class="user-table-wrap">
      <el-table ref="groupTableRef" :data="groupList" border stripe row-key="userId" :expand-row-keys="expandedKeys"
        @expand-change="onExpandChange" :cell-style="{ padding: '6px 0' }">
        <!-- 展开行：该用户下的卡片列表 -->
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-card-list" v-if="row.cards && row.cards.length">
              <el-table :data="row.cards" size="small" :border="false" class="inner-table">
                <el-table-column prop="userName" label="持卡人" width="120">
                  <template #default="{ row: card }">
                    <div class="card-user-cell">
                      <el-icon :size="14" color="#409eff"><User /></el-icon>
                      <span>{{ card.userName }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="ownerRelation" label="归属关系" width="100">
                  <template #default="{ row: card }">
                    <el-tag size="small" effect="plain" type="info">{{ card.ownerRelation || '本人' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="bankName" label="银行" width="140">
                  <template #default="{ row: card }">
                    <div class="bank-cell">
                      <el-icon :size="14" color="#67c23a"><CreditCard /></el-icon>
                      <span>{{ card.bankName }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="卡号后四位" width="120">
                  <template #default="{ row: card }">
                    <div class="card-no-cell">
                      <span class="card-no-badge">{{ card.cardNoLast4 }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="cardTypeDesc" label="类型" width="90" align="center">
                  <template #default="{ row: card }">
                    <el-tag :type="card.cardType === 2 ? 'warning' : 'success'" size="small" effect="light">
                      {{ card.cardTypeDesc }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="余额/额度" width="160" align="right">
                  <template #default="{ row: card }">
                    <div class="amount-cell">
                      <template v-if="card.cardType === 2">
                        <span class="amount-label">额度</span>
                        <AmountDisplay :value="card.creditLimit" class="amount-value" />
                      </template>
                      <template v-else>
                        <span class="amount-label">余额</span>
                        <AmountDisplay :value="card.balance" class="amount-value" />
                      </template>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="repayMethod" label="还款方式" width="110" align="center">
                  <template #default="{ row: card }">
                    <el-tag v-if="card.repayMethod === 'cloudpay'" type="primary" size="small" effect="light">云闪付</el-tag>
                    <el-tag v-else-if="card.repayMethod === 'invoice'" type="warning" size="small" effect="light">开票</el-tag>
                    <span v-else class="no-data">-</span>
                  </template>
                </el-table-column>
                <el-table-column label="核实" width="80" align="center">
                  <template #default="{ row: card }">
                    <el-tag v-if="card.repayMethod === 'cloudpay'" :type="card.verified ? 'success' : 'info'" size="small" effect="light">
                      {{ card.verified ? '已核' : '未核' }}
                    </el-tag>
                    <span v-else class="no-data">-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="expireDate" label="有效期" width="100" align="center" />
                <el-table-column label="状态" width="80" align="center">
                  <template #default="{ row: card }">
                    <StatusTag :value="card.status" :label-map="CARD_STATUS_MAP" :type-map="CARD_STATUS_TAG_TYPE" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="240" fixed="right">
                  <template #default="{ row: card }">
                    <div class="action-cell">
                      <el-button type="primary" link size="small" @click="openBillYearView(card)">
                        <el-icon :size="13"><Calendar /></el-icon>全年账单
                      </el-button>
                      <el-button type="primary" link size="small" @click="openEdit(card)">
                        <el-icon :size="13"><EditPen /></el-icon>编辑
                      </el-button>
                      <el-popconfirm title="确认删除？若该卡无历史账单/流水，将同步清理提醒和自动生成的空账单。" @confirm="handleDelete(card.id)">
                        <template #reference>
                          <el-button type="danger" link size="small">
                            <el-icon :size="13"><Delete /></el-icon>删除
                          </el-button>
                        </template>
                      </el-popconfirm>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div v-else class="empty-cards">暂无卡片</div>
          </template>
        </el-table-column>

        <!-- 用户信息行 -->
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }">
            <div class="user-info-cell">
              <div class="user-avatar" :class="{ 'unassigned': row.userId === 0 }">
                <el-icon :size="18"><UserFilled /></el-icon>
              </div>
              <div class="user-details">
                <div class="user-name-row">
                  <span class="user-name">{{ row.userName }}</span>
                  <el-tag v-if="row.userId === 0" type="info" size="small" effect="light">未分配</el-tag>
                </div>
                <div class="user-phone" v-if="row.phone">
                  <el-icon :size="12"><Phone /></el-icon>{{ row.phone }}
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="手续费率" width="180" align="center">
          <template #default="{ row }">
            <div class="fee-rate-cell">
              <template v-if="editingFeeUserId === row.userId && row.userId !== 0">
                <div class="fee-edit-wrap">
                  <el-input-number v-model="editingFeeRate" :min="0" :max="100" :precision="2" :controls="false"
                    size="small" style="width:90px" />
                  <span class="fee-unit">%</span>
                </div>
                <div class="fee-actions">
                  <el-button type="primary" size="small" @click="handleSaveFeeRate(row)" :loading="savingFee">保存</el-button>
                  <el-button size="small" @click="cancelEditFee">取消</el-button>
                </div>
              </template>
              <template v-else>
                <div class="fee-display">
                  <span class="fee-badge" :class="{ 'has-rate': row.feeRate > 0 }">{{ formatFeeRate(row.feeRate) }}%</span>
                </div>
                <el-button v-if="row.userId !== 0" type="primary" link size="small" @click="startEditFeeRate(row)">
                  <el-icon :size="13"><EditPen /></el-icon>修改
                </el-button>
              </template>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="卡片数" width="100" align="center">
          <template #default="{ row }">
            <div class="card-count-cell">
              <el-badge v-if="row.cardCount > 0" :value="row.cardCount" :max="99" type="primary" class="count-badge" />
              <span v-else class="no-data">0</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button type="primary" link size="small" @click="toggleExpand(row)">
                <el-icon :size="13"><View /></el-icon>查看卡片
              </el-button>
              <el-button v-if="row.userId !== 0" type="success" link size="small" @click="openAddWithUser(row)">
                <el-icon :size="13"><Plus /></el-icon>添加卡片
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <el-empty v-if="!loading && groupList.length === 0" description="暂无数据" />
    </div>

    <!-- 新增/编辑弹窗 -->
    <CrudDialog
      v-model="dialogVisible"
      :title="dialogTitle + '银行卡'"
      :form-data="formData"
      :rules="computedRules"
      :loading="submitting"
      :is-edit="isEdit"
      width="560px"
      @confirm="handleSubmit"
    >
      <template #default="{ form }">
        <el-form-item label="用户" prop="userId">
          <el-select v-model="form.userId" placeholder="选择用户" clearable filterable style="width:100%">
            <el-option v-for="u in userOptions" :key="u.id" :label="u.name" :value="u.id">
              <div class="user-opt">
                <span>{{ u.name }}</span>
                <el-tag v-if="!u.parentId" size="small" type="" effect="light" round style="margin-left:6px">主账户</el-tag>
                <el-tag v-else size="small" type="info" effect="plain" round style="margin-left:6px">子用户</el-tag>
              </div>
            </el-option>
          </el-select>
          <el-button type="primary" link size="small" style="margin-left:8px" @click="openUserDialog()">+ 管理用户</el-button>
        </el-form-item>
        <el-form-item label="银行名称" prop="bankName">
          <el-input v-model="form.bankName" placeholder="如：招商银行" />
        </el-form-item>
        <el-form-item label="归属关系">
          <el-input v-model="form.ownerRelation" placeholder="如：本人 / 配偶 / 子女" maxlength="20" />
        </el-form-item>
        <el-form-item label="归属人姓名">
          <el-input v-model="form.ownerName" placeholder="可选，如：张三" maxlength="20" />
        </el-form-item>
        <el-form-item label="卡号后四位" prop="cardNoLast4">
          <el-input v-model="form.cardNoLast4" placeholder="请输入卡号后四位（4位数字）" maxlength="4" />
        </el-form-item>
        <el-form-item label="卡片类型" prop="cardType">
          <el-radio-group :model-value="form.cardType" @change="onCardTypeChange">
            <el-radio v-for="item in CARD_TYPE_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.cardType === 2">
          <el-form-item label="信用额度">
            <el-input v-model="form.creditLimit" placeholder="请输入信用额度" />
          </el-form-item>
          <el-form-item label="账单日">
            <el-input-number v-model="form.billDay" :min="1" :max="31" controls-position="right" style="width: 100%" placeholder="请输入每月账单日" />
          </el-form-item>
          <el-form-item label="还款日">
            <el-input-number v-model="form.repayDay" :min="1" :max="31" controls-position="right" style="width: 100%" placeholder="请输入每月还款日" />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="余额">
            <el-input v-model="form.balance" placeholder="请输入余额" />
          </el-form-item>
          <el-form-item label="总额度">
            <el-input v-model="form.totalLimit" placeholder="请输入这张卡的总额度" />
          </el-form-item>
        </template>
        <el-form-item label="有效期">
          <el-input v-model="form.expireDate" placeholder="格式：2028-06" />
        </el-form-item>
        <el-form-item label="还款方式" prop="repayMethod">
          <el-radio-group v-model="form.repayMethod">
            <el-radio value="cloudpay">云闪付</el-radio>
            <el-radio value="invoice">开票</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.repayMethod === 'cloudpay'" label="是否核实">
          <el-switch v-model="form.verified" active-text="已核实" inactive-text="未核实" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option v-for="item in CARD_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" rows="2" maxlength="500" show-word-limit placeholder="最多输入500字" />
        </el-form-item>
      </template>
    </CrudDialog>

    <!-- 用户管理弹窗（两级层级） -->
    <el-dialog v-model="userDialogVisible" title="管理用户" width="850px" destroy-on-close>
      <!-- 快速添加一级用户 -->
      <div style="margin-bottom:12px;display:flex;gap:8px;align-items:center">
        <span style="font-weight:600;font-size:14px;color:#606266">新增一级用户：</span>
        <el-input v-model="newUser.name" placeholder="名称" style="width:140px" />
        <el-input-number v-model="newUser.feeRate" :min="0" :max="100" :precision="2" :controls="false" placeholder="费率%" style="width:90px" />
        <el-input v-model="newUser.phone" placeholder="电话" style="width:130px" />
        <el-button type="primary" @click="handleQuickAddTopUser">添加</el-button>
      </div>

      <!-- 树形用户列表：一级 + 子用户缩进展示 -->
      <el-table :data="userTreeList" border stripe size="small" v-loading="userLoading" row-key="id"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }" default-expand-all>
        <el-table-column prop="name" label="姓名" min-width="150">
          <template #default="{ row }">
            <div style="display:flex;align-items:center">
              <span>{{ row.name }}</span>
              <el-tag v-if="row.parentId === null || !row.parentId" type="" size="small" style="margin-left:8px;background:#ecf5ff;color:#409eff;border-color:#d9ecff">主账户</el-tag>
              <el-tag v-else type="info" size="small" style="margin-left:8px">子用户</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="手续费率" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.parentId === null || !row.parentId" class="fee-value" :class="{ 'fee-highlight': row.effectiveFeeRate > 0 }">{{ formatFeeRate(row.effectiveFeeRate) }}%</span>
            <span v-else style="color:#909399;font-size:12px">继承 {{ formatFeeRate(row.effectiveFeeRate) }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="120" />
        <el-table-column prop="cardCount" label="卡片数" width="70" align="center">
          <template #default="{ row }">
            <el-badge v-if="row.cardCount > 0" :value="row.cardCount" :max="99" type="primary">
              <span>&nbsp;</span>
            </el-badge>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">{{ row.status === 0 ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.parentId" type="success" link size="small" @click="openAddChild(row)">+ 添加子用户</el-button>
            <el-button type="primary" link size="small" @click="openEditUser(row)">编辑</el-button>
            <el-popconfirm title="确认删除该用户？关联的子用户、银行卡及历史账单/流水需先处理。" @confirm="handleDeleteUser(row.id)">
              <template #reference><el-button type="danger" link size="small">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="userDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑用户弹窗 -->
    <el-dialog v-model="editUserVisible" :title="editUserTitle" width="480px" destroy-on-close>
      <el-form :model="editUserForm" label-width="80px" ref="editUserFormRef" :rules="editUserRules">
        <el-form-item label="姓名" prop="name"><el-input v-model="editUserForm.name" placeholder="请输入姓名" /></el-form-item>
        <el-form-item label="父用户" v-if="editUserForm.parentId">
          <el-input :model-value="editUserParentName" disabled />
        </el-form-item>
        <el-form-item label="手续费率" prop="feeRate" v-if="!editUserForm.parentId">
          <el-input-number v-model="editUserForm.feeRate" :min="0" :max="100" :precision="2" style="width:100%" />
          <div style="font-size:12px;color:#909399;margin-top:4px">修改此值将同步更新所有关联的子用户</div>
        </el-form-item>
        <el-form-item label="手续费率" v-else>
          <span style="color:#909399">继承自父级（{{ formatFeeRate(editUserInheritedRate) }}%）</span>
        </el-form-item>
        <el-form-item label="电话"><el-input v-model="editUserForm.phone" placeholder="选填" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="editUserForm.remark" type="textarea" rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editUserVisible = false">取消</el-button>
        <el-button type="primary" :loading="editUserSaving" @click="handleSaveEditUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Cards' })
import { ref, reactive, computed, watch, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, UserFilled, User, Phone, EditPen, Delete, CreditCard, Calendar, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchBar from '@/components/SearchBar/index.vue'
import CrudDialog from '@/components/CrudDialog/index.vue'
import StatusTag from '@/components/StatusTag/index.vue'
import AmountDisplay from '@/components/AmountDisplay/index.vue'
import {
  saveCardApi, updateCardApi, deleteCardApi,
  getCardsGroupedByUserApi,
  getUserTreeApi, saveUserApi, updateUserApi, deleteUserApi, updateUserFeeRateApi
} from '@/api/card'
import { CARD_TYPE_OPTIONS, CARD_STATUS_OPTIONS, CARD_STATUS_MAP, CARD_STATUS_TAG_TYPE } from '@/constants/dict'

/** 用户分组数据 */
interface UserGroup {
  userId: number
  userName: string
  feeRate: number
  phone?: string
  status: number
  cardCount: number
  cards?: any[]
}

const router = useRouter()

const defaultForm = {
  id: undefined as number | undefined,
  userId: null as any,
  bankName: '',
  cardNoLast4: '',
  ownerRelation: '本人',
  ownerName: '',
  cardType: 2,
  creditLimit: '',
  balance: '',
  totalLimit: '',
  billDay: '',
  repayDay: '',
  expireDate: '',
  status: 0,
  remark: '',
  repayMethod: 'cloudpay',
  verified: false
}

// ====== 数据状态 ======
const loading = ref(false)
const groupList = ref<UserGroup[]>([])
const expandedKeys = ref<number[]>([])
const query = reactive({
  bankName: '',
  cardNoLast4: '',
  cardType: undefined as any
})

// 费率编辑状态
const editingFeeUserId = ref<number | null>(null)
const editingFeeRate = ref(0)
const savingFee = ref(false)

// 弹窗状态
const formData = reactive({ ...defaultForm })
const isEdit = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)

// 表格 ref
const groupTableRef = ref<any>(null)

// 用户下拉选项（用于新增/编辑卡片）
const userOptions = ref<any[]>([])

// ====== 方法 ======

async function fetchData() {
  loading.value = true
  try {
    const res: any = await getCardsGroupedByUserApi(query)
    groupList.value = res.data || []
    syncUserOptions()
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.bankName = ''
  query.cardNoLast4 = ''
  query.cardType = undefined
  fetchData()
}

function toggleExpand(row: UserGroup) {
  if (groupTableRef.value) {
    groupTableRef.value.toggleRowExpansion(row)
  }
}

function onExpandChange(row: UserGroup, expanded: boolean) {
  const uid = row.userId
  if (expanded && !expandedKeys.value.includes(uid)) expandedKeys.value.push(uid)
  else if (!expanded) expandedKeys.value = expandedKeys.value.filter(k => k !== uid)
}

// ====== 费率编辑 ======
function startEditFeeRate(row: UserGroup) {
  editingFeeUserId.value = row.userId
  editingFeeRate.value = Number(formatFeeRate(row.feeRate))
}

async function handleSaveFeeRate(row: UserGroup) {
  savingFee.value = true
  try {
    await updateUserFeeRateApi({
      userId: row.userId,
      feeRate: editingFeeRate.value
    })
    ElMessage.success('费率修改成功，已同步到所有子用户')
    cancelEditFee()
    fetchData()
  } finally {
    savingFee.value = false
  }
}

function cancelEditFee() {
  editingFeeUserId.value = null
}

function formatFeeRate(rate: number | string | null | undefined) {
  return Number(rate ?? 0).toFixed(2)
}

// ====== 表单操作 ======
const dialogTitle = computed(() => isEdit.value ? '编辑' : '新增')
const computedRules = computed(() => ({
  userId: [{ required: true, message: '请选择用户', trigger: 'change' }],
  bankName: [{ required: true, message: '请输入银行名称', trigger: 'blur' }],
  cardNoLast4: [{ required: true, message: '请输入卡号后四位', trigger: 'blur' }, { pattern: /^\d{4}$/, message: '必须为4位数字', trigger: 'blur' }],
  cardType: [{ required: true, message: '请选择卡片类型', trigger: 'change' }]
}))

async function openAdd() {
  isEdit.value = false
  Object.assign(formData, defaultForm)
  await syncUserOptions()
  dialogVisible.value = true
}

async function openAddWithUser(row: UserGroup) {
  isEdit.value = false
  Object.assign(formData, defaultForm, { userId: row.userId })
  await syncUserOptions()
  dialogVisible.value = true
}

function openEdit(row: any) {
  isEdit.value = true
  Object.assign(formData, row)
  dialogVisible.value = true
}

function openBillYearView(card: any) {
  router.push({
    path: '/bills',
    query: { cardId: String(card.id) }
  })
}

async function onCardTypeChange(newType: number) {
  const oldType = formData.cardType
  if (newType === oldType) return
  if (!isEdit.value) {
    formData.cardType = newType
    clearCardTypeFields(newType)
    return
  }
  const clearFields = oldType === 2 ? '信用额度、账单日、还款日' : '余额、总额度'
  try {
    await ElMessageBox.confirm(`切换卡片类型将清空${clearFields}，是否继续？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    formData.cardType = newType
    clearCardTypeFields(oldType)
  } catch { /* 取消 */ }
}

function clearCardTypeFields(cardType: number) {
  if (cardType === 2) Object.assign(formData, { creditLimit: '', billDay: '', repayDay: '' })
  else if (cardType === 1) Object.assign(formData, { balance: '', totalLimit: '' })
}

async function handleSubmit() {
  const data: any = { ...formData }
  let createdCardId: number | undefined
  // 空字符串转null
  ;['creditLimit', 'balance', 'totalLimit', 'billDay', 'repayDay'].forEach(key => {
    if (data[key] === '') data[key] = null
  })

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCardApi(data)
    } else {
      const res: any = await saveCardApi(data)
      const rawId = Number(res?.data)
      createdCardId = Number.isFinite(rawId) && rawId > 0 ? rawId : undefined
    }
    ElMessage.success(isEdit.value ? '操作成功' : (data.cardType === 2 ? '新增成功，已生成本年账单模板' : '新增成功'))
    dialogVisible.value = false
    await fetchData()
    if (!isEdit.value && data.cardType === 2 && createdCardId) {
      openBillYearView({ id: createdCardId })
    }
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  try {
    await deleteCardApi(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error: any) {
    ElMessage.error(error?.message || error?.response?.data?.message || '删除失败，该卡可能存在关联历史数据')
  }
}

// ====== 用户管理（两级层级） ======
const userDialogVisible = ref(false)
const userTreeList = ref<any[]>([])
const userLoading = ref(false)
const newUser = reactive({ name: '', feeRate: 0, phone: '' })

async function openUserDialog() {
  userDialogVisible.value = true
  await fetchUserTree()
}

async function fetchUserTree() {
  userLoading.value = true
  try {
    const res: any = await getUserTreeApi()
    userTreeList.value = res.data || []
    syncUserOptions()
  } finally { userLoading.value = false }
}

async function syncUserOptions() {
  if (!userTreeList.value.length) {
    try {
      const res: any = await getUserTreeApi()
      userTreeList.value = res.data || []
    } catch { /* ignore */ }
  }

  const flatUsers: any[] = []
  function flatten(list: any[]) {
    for (const u of list) {
      flatUsers.push({
        id: u.id,
        name: u.name,
        parentId: u.parentId,
        status: u.status,
        effectiveFeeRate: u.effectiveFeeRate ?? u.feeRate ?? 0
      })
      if (u.children) flatten(u.children)
    }
  }
  flatten(userTreeList.value)

  const currentUserId = Number(formData.userId || 0)
  userOptions.value = flatUsers.filter((user: any) => user.status === 0 || user.id === currentUserId)
}

/** 快速添加一级用户 */
async function handleQuickAddTopUser() {
  if (!newUser.name) { ElMessage.warning('请输入名称'); return }
  await saveUserApi({ ...newUser, status: 0 })
  ElMessage.success('添加成功')
  newUser.name = ''; newUser.phone = ''; newUser.feeRate = 0
  fetchUserTree()
  fetchData()
}

// ====== 子用户添加 ======
function openAddChild(parentRow: any) {
  editUserParentName.value = parentRow.name
  editUserInheritedRate.value = Number(parentRow.effectiveFeeRate ?? parentRow.feeRate ?? 0)
  Object.assign(editUserForm, { id: undefined, parentId: parentRow.id, name: '', phone: '', feeRate: 0, remark: '', status: 0 })
  editUserTitle.value = `添加子用户 - ${parentRow.name}`
  editUserVisible.value = true
}

// ====== 用户编辑 ======
const editUserVisible = ref(false)
const editUserSaving = ref(false)
const editUserFormRef = ref<any>()
const editUserForm = reactive({ id: undefined as number | undefined, parentId: null as any, name: '', phone: '', feeRate: 0, remark: '', status: 0 })
const editUserTitle = ref('编辑用户')
const editUserParentName = ref('')
const editUserInheritedRate = ref(0)
const editUserRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  feeRate: [{ required: true, message: '请输入费率', trigger: 'blur' }]
}

function openEditUser(row: any) {
  editUserParentName.value = row.parentName || ''
  editUserInheritedRate.value = Number(row.effectiveFeeRate ?? row.feeRate ?? 0)
  Object.assign(editUserForm, {
    id: row.id,
    parentId: row.parentId,
    name: row.name,
    phone: row.phone || '',
    feeRate: Number(formatFeeRate(row.feeRate || 0)),
    remark: row.remark || '',
    status: row.status
  })
  editUserTitle.value = '编辑用户'
  editUserVisible.value = true
}

async function handleSaveEditUser() {
  try {
    const formEl = editUserFormRef.value
    if (!formEl) return
    await formEl.validate()
  } catch { return }

  editUserSaving.value = true
  try {
    const payload: any = {
      id: editUserForm.id,
      parentId: editUserForm.parentId,
      name: editUserForm.name,
      phone: editUserForm.phone,
      remark: editUserForm.remark,
      status: editUserForm.status
    }
    // 一级用户才传费率
    if (!editUserForm.parentId) {
      payload.feeRate = editUserForm.feeRate
    }

    if (editUserForm.id) {
      await updateUserApi(payload)
      ElMessage.success('修改成功')
    } else {
      await saveUserApi(payload)
      ElMessage.success('创建成功')
    }
    editUserVisible.value = false
    fetchUserTree()
    fetchData()
  } finally { editUserSaving.value = false }
}

async function handleDeleteUser(id: number) {
  try {
    await deleteUserApi(id)
    ElMessage.success('删除成功')
    fetchUserTree()
    fetchData()
  } catch (error: any) {
    ElMessage.error(error?.message || error?.response?.data?.message || '删除失败，请先处理关联数据')
  }
}

// 初始化加载
onMounted(() => {
  fetchData()
})
// keep-alive 缓存恢复时自动刷新
onActivated(() => { fetchData() })

// 监听查询条件变化，自动搜索
watch(query, () => {
  fetchData()
}, { deep: true })
</script>

<style scoped>
.user-table-wrap {
  background: var(--color-card);
  border-radius: var(--border-radius-lg);
  padding: var(--spacing-2xl);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border-light);
}

:deep(.el-table) {
  --el-table-border-color: var(--color-border-light);
  font-size: var(--font-size-base);
}
:deep(.el-table__row) {
  transition: background-color 0.2s ease;
}
:deep(.el-table .cell) {
  padding-top: 10px;
  padding-bottom: 10px;
}
:deep(.el-table th.el-table__cell) {
  background-color: #fafafa;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-regular);
  font-size: var(--font-size-sm);
}

/* 用户信息单元格 */
.user-info-cell {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-avatar {
  width: 44px;
  height: 44px;
  border-radius: var(--border-radius-lg);
  background: linear-gradient(135deg, #e6f7ff 0%, #bae0ff 100%);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.2);
  transition: all 0.3s;
}

.user-avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.3);
}

.user-avatar.unassigned {
  background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%);
  color: #8c8c8c;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.user-name {
  font-weight: var(--font-weight-semibold);
  font-size: 15px;
  color: var(--color-text-primary);
}

.user-phone {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

/* 费率单元格 */
.fee-rate-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.fee-edit-wrap {
  display: flex;
  align-items: center;
  gap: 4px;
}

.fee-actions {
  display: flex;
  gap: 6px;
}

.fee-display {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}

.fee-badge {
  font-size: 16px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text-secondary);
  padding: 6px 14px;
  background: #f5f5f5;
  border-radius: var(--border-radius);
  font-family: var(--font-mono);
  border: 1px solid var(--color-border-light);
}

.fee-badge.has-rate {
  color: #fff;
  background: linear-gradient(135deg, #faad14 0%, #ffc53d 100%);
  box-shadow: 0 2px 8px rgba(250, 173, 20, 0.3);
  border: none;
}

.fee-unit {
  font-size: var(--font-size-sm);
  color: var(--color-text-regular);
  margin-left: 2px;
}

/* 卡片数单元格 */
.card-count-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.count-badge :deep(.el-badge__content) {
  font-weight: var(--font-weight-semibold);
}

.no-data {
  color: #d9d9d9;
  font-size: 14px;
}

/* 操作单元格 */
.action-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

/* 展开的卡片列表 */
.expand-card-list {
  padding: var(--spacing-lg) var(--spacing-2xl);
  background: #fafafa;
  border-radius: var(--border-radius);
}

.empty-cards {
  padding: 16px 20px;
  color: #999;
  text-align: center;
  background: #fafafa;
}

.inner-table {
  --el-table-border-color: transparent;
}

.inner-table :deep(th) {
  background: #fafafa !important;
  font-size: 13px;
}

.inner-table :deep(.el-table__row) {
  background: #fff;
}

.inner-table :deep(.el-table__row:hover > td) {
  background: #f5f7fa !important;
}

/* 内部表格单元格样式 */
.card-user-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.bank-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
}

.card-no-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-no-badge {
  font-weight: 700;
  font-size: 15px;
  color: #303133;
  font-family: 'Courier New', monospace;
  background: linear-gradient(135deg, #e6f4ff 0%, #f0f9ff 100%);
  padding: 4px 12px;
  border-radius: var(--border-radius);
  border: 1px solid #d9ecff;
}

.amount-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  justify-content: flex-end;
}

.amount-label {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.amount-value {
  font-weight: 600;
  font-size: 14px;
}

/* 用户选项 */
.user-opt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
</style>
