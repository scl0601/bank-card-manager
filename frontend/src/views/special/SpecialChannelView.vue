<template>
  <div class="special-page">
    <section class="special-header">
      <div class="header-main">
        <div>
          <div class="page-title">特殊通道</div>
          <div class="page-meta">
            <span v-if="config.userId">{{ config.userName || '-' }}</span>
            <span v-if="config.userId">费率 {{ formatRate(config.feeRate) }}%</span>
            <span v-if="config.userId">{{ cards.length }} 张卡</span>
            <span v-else>请先绑定用户信息里的特殊用户</span>
          </div>
        </div>
      </div>

      <div class="config-bar">
        <el-select-v2
          v-model="selectedUserId"
          class="user-select"
          :options="userOptions"
          placeholder="选择特殊用户"
          filterable
          :height="300"
          :item-height="32"
          :disabled="!canEdit"
        />
        <el-button type="primary" :disabled="!canEdit || !selectedUserId" :loading="savingConfig" @click="saveConfig">
          保存配置
        </el-button>
        <el-button :icon="RefreshRight" @click="refreshAll">刷新</el-button>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="special-tabs">
      <el-tab-pane label="特殊卡务" name="cards">
        <section class="card-toolbar">
          <div class="stat-strip">
            <div class="stat-item">
              <span>银行卡</span>
              <strong>{{ cards.length }}</strong>
            </div>
            <div class="stat-item">
              <span>卡片额度</span>
              <strong>{{ formatMoney(totalCardAmount) }}</strong>
            </div>
            <div class="stat-item">
              <span>账单数量</span>
              <strong>{{ totalBillCount }}</strong>
            </div>
          </div>
          <el-button type="primary" :icon="Plus" :disabled="!canEdit || !config.userId" @click="openCreateCard">
            新增银行卡
          </el-button>
        </section>

        <section v-loading="cardLoading" class="special-card-grid">
          <article
            v-for="card in cards"
            :key="card.id"
            class="bank-card-tile"
            :class="[specialCardExpireClass(card.expireDate), { disabled: card.status === 1 }]"
            :title="specialCardTitle(card)"
          >
            <div class="special-card-left">
              <div class="bank-mark">
                <el-icon><CreditCard /></el-icon>
              </div>
              <div class="special-card-main">
                <div class="special-card-row-top">
                  <span class="special-card-bank">{{ card.bankName || '-' }}</span>
                  <i class="special-card-sep"></i>
                  <span class="special-card-last4">尾号 {{ card.cardNoLast4 || '-' }}</span>
                  <span v-if="card.status === 1" class="special-card-status-disabled">停用</span>
                  <el-tag v-else size="small" type="success" effect="light">正常</el-tag>
                </div>
                <div class="special-card-row-sub">
                  <span class="special-card-type">特殊卡</span>
                  <span class="special-card-dot"></span>
                  <span class="special-card-label">持卡人</span>
                  <span class="special-card-date">{{ card.userName || config.userName || '-' }}</span>
                  <span class="special-card-dot"></span>
                  <span class="special-card-label">账单日</span>
                  <span :class="['special-card-date', { empty: !card.billDay }]">{{ formatDayOfMonth(card.billDay) }}</span>
                  <span class="special-card-dot"></span>
                  <span class="special-card-label">还款日</span>
                  <span :class="['special-card-date', { empty: !card.repaymentDay }]">{{ formatDayOfMonth(card.repaymentDay) }}</span>
                  <span class="special-card-dot"></span>
                  <span class="special-card-label">有效期</span>
                  <span :class="['special-card-date', { 'special-card-expire-date': card.expireDate, empty: !card.expireDate, 'expire-warning': isSpecialCardExpiringSoon(card.expireDate), 'expire-expired': isSpecialCardExpired(card.expireDate) }]">{{ card.expireDate || '-' }}</span>
                  <span class="special-card-dot"></span>
                  <span class="special-card-label">费率</span>
                  <span class="special-card-date">{{ formatRate(card.feeRate) }}%</span>
                  <span class="special-card-dot"></span>
                  <span class="special-card-label">账单</span>
                  <span class="special-card-date">{{ card.billCount || 0 }}条</span>
                </div>
                <div v-if="card.remark" class="special-card-remark" :title="card.remark">{{ card.remark }}</div>
              </div>
            </div>
            <div class="special-card-right">
              <div class="tile-amount">
                <span>卡片额度</span>
                <strong>{{ formatMoney(card.totalAmount) }}</strong>
              </div>
              <div class="tile-actions">
                <el-button size="small" :disabled="!canEdit" @click="openEditCard(card)">编辑</el-button>
                <el-button size="small" type="danger" plain :disabled="!isAdmin" @click="deleteCard(card)">删除</el-button>
              </div>
            </div>
          </article>
          <el-empty v-if="!cardLoading && cards.length === 0" description="暂无特殊银行卡" />
        </section>
      </el-tab-pane>

      <el-tab-pane label="特殊账单" name="bills">
        <section class="filter-line">
          <el-select v-model="billQuery.year" class="filter-item" placeholder="年份" clearable>
            <el-option v-for="year in yearOptions" :key="year" :label="`${year}年`" :value="year" />
          </el-select>
          <el-select v-model="billQuery.month" class="filter-item" placeholder="月份" clearable>
            <el-option v-for="month in monthOptions" :key="month" :label="`${month}月`" :value="month" />
          </el-select>
          <el-select v-model="billQuery.cardId" class="filter-card" placeholder="银行卡" clearable filterable>
            <el-option v-for="card in cards" :key="card.id" :label="cardLabel(card)" :value="card.id" />
          </el-select>
          <el-select v-model="deleteBoundaryYear" class="filter-delete-year" placeholder="删除账单年份">
            <el-option v-for="year in deleteBoundaryYearOptions" :key="year" :label="`删除账单用：${year}年`" :value="year" />
          </el-select>
          <el-button
            type="danger"
            plain
            :disabled="!isAdmin || !billQuery.cardId || deletingHistoryBills"
            :loading="deletingHistoryBills"
            @click="deleteHistoryBills"
          >
            删除之前账单
          </el-button>
          <el-button
            type="danger"
            plain
            :disabled="!isAdmin || !billQuery.cardId || deletingFutureBills"
            :loading="deletingFutureBills"
            @click="deleteFutureBills"
          >
            删除之后账单
          </el-button>
          <el-button
            v-if="isAdmin && selectedBillRows.length > 0"
            type="danger"
            :loading="batchDeletingBills"
            @click="batchDeleteBills"
          >
            批量删除 ({{ selectedBillRows.length }})
          </el-button>
          <el-button @click="resetBillQuery">重置</el-button>
          <span class="filter-hint">{{ annualBillHint }}</span>
        </section>

        <section class="bill-table-shell">
          <el-table
            ref="billTableRef"
            v-loading="billLoading"
            :data="billTableRows"
            border
            stripe
            size="small"
            :row-key="billRowKey"
            :row-class-name="billRowClassName"
            table-layout="fixed"
            height="100%"
            @selection-change="handleBillSelectionChange"
          >
            <el-table-column v-if="isAdmin" type="selection" width="38" align="center" reserve-selection :selectable="isSelectableBillRow" />
            <el-table-column prop="billYear" label="年" width="44" align="center">
              <template #default="{ row }">
                <span v-if="row.__summary" class="summary-label">合计</span>
                <span v-else>{{ row.billYear }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="billMonthNo" label="月" width="38" align="center">
              <template #default="{ row }">
                <span v-if="!row.__summary">{{ row.billMonthNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="银行" width="90" show-overflow-tooltip>
              <template #default="{ row }">
                <span v-if="!row.__summary" class="strong-cell">
                  {{ row.bankName }} / {{ row.cardNoLast4 }}
                  <span v-if="isSpecialBillCardDisabled(row)" class="card-disabled-pill">停用</span>
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="totalAmount" label="卡片额度" align="right">
              <template #default="{ row }">
                <span v-if="!row.__summary" class="money-text">{{ formatMoney(row.totalAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="账单日" width="48" align="center">
              <template #default="{ row }">
                <el-input v-if="!row.__summary" :model-value="formatDayInput(row.billDay)" class="day-input" size="small" maxlength="3" placeholder="-" :disabled="!canEditSpecialBillRow(row)" @focus="selectInputText" @update:model-value="updateSpecialBillDay(row, 'billDay', $event)" />
              </template>
            </el-table-column>
            <el-table-column label="还款日" width="48" align="center">
              <template #default="{ row }">
                <el-input v-if="!row.__summary" :model-value="formatDayInput(row.repaymentDay)" class="day-input" size="small" maxlength="3" placeholder="-" :disabled="!canEditSpecialBillRow(row)" @focus="selectInputText" @update:model-value="updateSpecialBillDay(row, 'repaymentDay', $event)" />
              </template>
            </el-table-column>
            <el-table-column prop="billAmount" label="账单金额" align="right">
              <template #default="{ row }">
                <span v-if="row.__summary" class="money-text summary-number">{{ formatMoney(row.billAmount) }}</span>
                <div v-else class="amount-verify-cell">
                  <el-input-number v-model="row.billAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEditSpecialBillRow(row)" @update:model-value="refreshBillSummary" />
                  <el-switch v-model="row.billAmountVerified" size="small" :disabled="!canEditSpecialBillRow(row)" />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="xiaohuanRepayAmount" label="小焕还款" align="right">
              <template #default="{ row }">
                <span v-if="row.__summary" class="money-text summary-number">{{ formatMoney(row.xiaohuanRepayAmount) }}</span>
                <div v-else class="amount-verify-cell">
                  <el-input-number v-model="row.xiaohuanRepayAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEditSpecialBillRow(row)" @update:model-value="refreshBillSummary" />
                  <el-switch v-model="row.xiaohuanRepayVerified" size="small" :disabled="!canEditSpecialBillRow(row)" />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="xiaohuanConsumeAmount" label="小焕消费" align="right">
              <template #default="{ row }">
                <span v-if="row.__summary" class="money-text summary-number">{{ formatMoney(row.xiaohuanConsumeAmount) }}</span>
                <div v-else class="amount-verify-cell">
                  <el-input-number v-model="row.xiaohuanConsumeAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEditSpecialBillRow(row)" @update:model-value="refreshBillSummary" />
                  <el-switch v-model="row.xiaohuanConsumeVerified" size="small" :disabled="!canEditSpecialBillRow(row)" />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="diffAmount" label="差额" width="74" align="right">
              <template #default="{ row }">
                <span class="money-text" :class="billDiffValue(row) >= 0 ? 'amount-income' : 'amount-cost'">
                  {{ formatMoney(billDiffValue(row)) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="customerNeedAmount" label="客户需要" align="right">
              <template #default="{ row }">
                <span v-if="row.__summary" class="money-text summary-number">{{ formatMoney(row.customerNeedAmount) }}</span>
                <div v-else class="amount-verify-cell">
                  <el-input-number v-model="row.customerNeedAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEditSpecialBillRow(row)" @update:model-value="refreshBillSummary" />
                  <el-switch v-model="row.customerNeedVerified" size="small" :disabled="!canEditSpecialBillRow(row)" />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="customerRepayAmount" label="客户还款" align="right">
              <template #default="{ row }">
                <span v-if="row.__summary" class="money-text summary-number">{{ formatMoney(row.customerRepayAmount) }}</span>
                <div v-else class="amount-verify-cell">
                  <el-input-number v-model="row.customerRepayAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEditSpecialBillRow(row)" @update:model-value="refreshBillSummary" />
                  <el-switch v-model="row.customerRepayVerified" size="small" :disabled="!canEditSpecialBillRow(row)" />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="customerConsumeAmount" label="客户消费" align="right">
              <template #default="{ row }">
                <span v-if="row.__summary" class="money-text summary-number">{{ formatMoney(row.customerConsumeAmount) }}</span>
                <div v-else class="amount-verify-cell">
                  <el-input-number v-model="row.customerConsumeAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEditSpecialBillRow(row)" @update:model-value="refreshBillSummary" />
                  <el-switch v-model="row.customerConsumeVerified" size="small" :disabled="!canEditSpecialBillRow(row)" />
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="balance" label="余额" width="78" align="right">
              <template #default="{ row }">
                <span v-if="row.__summary" class="money-text summary-number">{{ formatMoney(row.balance) }}</span>
                <el-input-number v-else v-model="row.balance" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEditSpecialBillRow(row)" @update:model-value="refreshBillSummary" />
              </template>
            </el-table-column>
            <el-table-column prop="interestAmount" label="利息" width="70" align="right">
              <template #default="{ row }">
                <span v-if="row.__summary" class="money-text summary-number">{{ formatMoney(row.interestAmount) }}</span>
                <el-input-number v-else v-model="row.interestAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEditSpecialBillRow(row)" @update:model-value="refreshBillSummary" />
              </template>
            </el-table-column>
            <el-table-column prop="lateFeeAmount" label="滞纳金" width="70" align="right">
              <template #default="{ row }">
                <span v-if="row.__summary" class="money-text summary-number">{{ formatMoney(row.lateFeeAmount) }}</span>
                <el-input-number v-else v-model="row.lateFeeAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEditSpecialBillRow(row)" @update:model-value="refreshBillSummary" />
              </template>
            </el-table-column>
            <el-table-column prop="installmentFeeAmount" label="分期费" width="70" align="right">
              <template #default="{ row }">
                <span v-if="row.__summary" class="money-text summary-number">{{ formatMoney(row.installmentFeeAmount) }}</span>
                <el-input-number v-else v-model="row.installmentFeeAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEditSpecialBillRow(row)" @update:model-value="refreshBillSummary" />
              </template>
            </el-table-column>
            <el-table-column label="备注" width="84">
              <template #default="{ row }">
                <el-input v-if="!row.__summary" v-model="row.remark" size="small" maxlength="500" clearable :disabled="!canEditSpecialBillRow(row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="54" align="center">
              <template #default="{ row }">
                <el-button v-if="!row.__summary" type="primary" link size="small" :disabled="!canEditSpecialBillRow(row)" :loading="savingBillId === row.id" @click="saveBill(row)">
                  保存
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="bill-pagination">
            <span class="pagination-meta">共 {{ billTotal }} 条</span>
            <el-pagination
              v-model:current-page="billQuery.current"
              v-model:page-size="billQuery.size"
              :total="billTotal"
              :page-sizes="billPageSizeOptions"
              small
              background
              layout="sizes, prev, pager, next"
            />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="特殊收益" name="profit">
        <section class="filter-line">
          <el-select v-model="profitQuery.year" class="filter-item" placeholder="全部年份" clearable>
            <el-option v-for="year in yearOptions" :key="year" :label="`${year}年`" :value="year" />
          </el-select>
          <el-select v-model="profitQuery.month" class="filter-item" placeholder="月份" clearable>
            <el-option v-for="month in monthOptions" :key="month" :label="`${month}月`" :value="month" />
          </el-select>
          <el-select v-model="profitQuery.cardId" class="filter-card" placeholder="银行卡" clearable filterable>
            <el-option v-for="card in cards" :key="card.id" :label="cardLabel(card)" :value="card.id" />
          </el-select>
          <el-button @click="resetProfitQuery">重置</el-button>
          <span class="filter-hint">筛选变化后自动刷新</span>
        </section>

        <section class="profit-summary-grid" v-loading="profitLoading">
          <div v-for="item in profitSummaryCards" :key="item.label" class="profit-summary-item">
              <span>{{ item.label === '每月账单金额' ? '账单总金额' : item.label }}</span>
            <strong :class="item.className">{{ item.value }}</strong>
          </div>
        </section>

        <section class="profit-tables">
          <div class="profit-table-block">
            <div class="block-title">
              <span>收益统计</span>
              <span>{{ profitPaginationText }}</span>
            </div>
            <el-table :data="pagedProfitRows" border stripe size="small" height="100%" table-layout="fixed" :row-class-name="profitRowClassName">
              <el-table-column prop="billYear" label="年" width="48" align="center" />
              <el-table-column prop="billMonthNo" label="月" width="40" align="center" />
              <el-table-column label="银行" min-width="96" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ row.bankName }} / {{ row.cardNoLast4 }}
                  <span v-if="isSpecialBillCardDisabled(row)" class="card-disabled-pill">停用</span>
                </template>
              </el-table-column>
              <el-table-column label="账单日" width="56" align="center">
                <template #default="{ row }">{{ formatDayOfMonth(row.billDay) }}</template>
              </el-table-column>
              <el-table-column label="还款日" width="56" align="center">
                <template #default="{ row }">{{ formatDayOfMonth(row.repaymentDay) }}</template>
              </el-table-column>
              <el-table-column label="账单金额" min-width="90" align="right">
                <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
              </el-table-column>
              <el-table-column label="小焕还款" min-width="90" align="right">
                <template #default="{ row }">{{ formatMoney(row.xiaohuanRepayAmount) }}</template>
              </el-table-column>
              <el-table-column label="还款手续费" min-width="94" align="right">
                <template #default="{ row }">{{ formatMoney(row.repaymentFee) }}</template>
              </el-table-column>
              <el-table-column label="小焕消费" min-width="90" align="right">
                <template #default="{ row }">{{ formatMoney(row.xiaohuanConsumeAmount) }}</template>
              </el-table-column>
              <el-table-column label="消费手续费" min-width="94" align="right">
                <template #default="{ row }">{{ formatMoney(row.consumeFee) }}</template>
              </el-table-column>
              <el-table-column label="利息" min-width="82" align="right">
                <template #default="{ row }">
                  <el-input-number v-model="row.interestAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEditSpecialProfitRow(row)" />
                </template>
              </el-table-column>
              <el-table-column label="滞纳金" min-width="82" align="right">
                <template #default="{ row }">
                  <el-input-number v-model="row.lateFeeAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEditSpecialProfitRow(row)" />
                </template>
              </el-table-column>
              <el-table-column label="分期费" min-width="82" align="right">
                <template #default="{ row }">
                  <el-input-number v-model="row.installmentFeeAmount" class="money-input" size="small" :precision="2" :controls="false" :disabled="!canEditSpecialProfitRow(row)" />
                </template>
              </el-table-column>
              <el-table-column label="总计" min-width="90" align="right">
                <template #default="{ row }">
                  <span class="amount-income">{{ formatMoney(calcProfitTotal(row)) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="54" align="center">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" :disabled="!canEditSpecialProfitRow(row)" :loading="savingProfitBillId === row.billId" @click="saveProfitExtras(row)">
                    保存
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="profit-pagination">
              <span class="pagination-meta">共 {{ profitTotal }} 条</span>
              <el-pagination
                v-model:current-page="profitPage.current"
                v-model:page-size="profitPage.size"
                :total="profitTotal"
                :page-sizes="profitPageSizeOptions"
                small
                background
                layout="sizes, prev, pager, next"
              />
            </div>
          </div>
        </section>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="cardDialogVisible" :title="cardDialogTitle" width="520px" destroy-on-close>
      <el-form ref="cardFormRef" :model="cardForm" :rules="cardRules" label-width="96px">
        <el-form-item label="银行名称" prop="bankName">
          <el-input v-model="cardForm.bankName" maxlength="64" placeholder="请输入银行名称" />
        </el-form-item>
        <el-form-item label="卡号后四位" prop="cardNoLast4">
          <el-input v-model="cardForm.cardNoLast4" maxlength="4" placeholder="请输入4位数字" />
        </el-form-item>
        <el-form-item label="卡片额度" prop="totalAmount">
          <el-input-number v-model="cardForm.totalAmount" class="full-input" :min="0" :precision="2" :controls="false" placeholder="请输入卡片额度" />
        </el-form-item>
        <el-form-item label="账单日">
          <el-input-number v-model="cardForm.billDay" class="full-input" :min="1" :max="31" :precision="0" controls-position="right" placeholder="每月账单日" />
        </el-form-item>
        <el-form-item label="还款日">
          <el-input-number v-model="cardForm.repaymentDay" class="full-input" :min="1" :max="31" :precision="0" controls-position="right" placeholder="每月还款日" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-input v-model="cardForm.expireDate" maxlength="32" placeholder="如：06/28" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="cardForm.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="cardForm.remark" type="textarea" maxlength="500" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cardDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingCard" @click="submitCard">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'SpecialChannel' })

import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { CreditCard, Plus, RefreshRight } from '@element-plus/icons-vue'
import { getUserTreeApi } from '@/api/card'
import {
  batchDeleteSpecialBillsApi,
  deleteSpecialBillsAfterYearApi,
  deleteSpecialBillsBeforeYearApi,
  deleteSpecialCardApi,
  getSpecialBillPageApi,
  getSpecialCardsApi,
  getSpecialConfigApi,
  getSpecialProfitStatsApi,
  saveSpecialCardApi,
  saveSpecialConfigApi,
  updateSpecialBillApi,
  updateSpecialCardApi,
  updateSpecialProfitExtraFeesApi
} from '@/api/special'
import { useAuthStore } from '@/store/modules/auth'
import { getCardExpireStatus } from '@/utils/cardExpiry'

type TabName = 'cards' | 'bills' | 'profit'

interface UserNode {
  id: number
  name: string
  parentId?: number | null
  parentName?: string
  phone?: string
  feeRate?: number | null
  effectiveFeeRate?: number | null
  status?: number
  children?: UserNode[]
}

interface SelectOption {
  label: string
  value: number
}

interface SpecialConfig {
  id?: number
  userId?: number
  userName?: string
  feeRate?: number | null
  phone?: string
  remark?: string
}

interface SpecialCard {
  id: number
  userId?: number
  userName?: string
  feeRate?: number | null
  bankName: string
  cardNoLast4: string
  totalAmount?: number | string | null
  expireDate?: string | null
  billDay?: number | null
  repaymentDay?: number | null
  status?: number
  statusDesc?: string
  remark?: string
  billCount?: number
}

interface SpecialBill {
  id: number
  cardId: number
  bankName?: string
  cardNoLast4?: string
  cardStatus?: number | null
  totalAmount?: number | string | null
  billMonth: string
  billYear?: number
  billMonthNo?: number
  monthlyTotalBillAmount?: number | null
  billDay?: number | null
  repaymentDay?: number | null
  billAmount?: number | null
  billAmountVerified?: boolean
  xiaohuanRepayAmount?: number | null
  xiaohuanRepayVerified?: boolean
  customerRepayAmount?: number | null
  customerRepayVerified?: boolean
  xiaohuanConsumeAmount?: number | null
  xiaohuanConsumeVerified?: boolean
  customerNeedAmount?: number | null
  customerNeedVerified?: boolean
  customerConsumeAmount?: number | null
  customerConsumeVerified?: boolean
  diffAmount?: number | null
  balance?: number | null
  feeRate?: number | null
  repaymentFee?: number | null
  consumeFee?: number | null
  interestAmount?: number | null
  lateFeeAmount?: number | null
  installmentFeeAmount?: number | null
  profitTotalAmount?: number | null
  remark?: string | null
  __summary?: boolean
}

interface ProfitOverview {
  cardCount?: number
  billCount?: number
  totalMonthlyTotalBillAmount?: number | string | null
  totalBillAmount?: number | string | null
  totalXiaohuanRepayAmount?: number | string | null
  totalCustomerRepayAmount?: number | string | null
  totalXiaohuanConsumeAmount?: number | string | null
  totalCustomerNeedAmount?: number | string | null
  totalCustomerConsumeAmount?: number | string | null
  totalDiffAmount?: number | string | null
  totalRepaymentFee?: number | string | null
  totalConsumeFee?: number | string | null
  totalInterestAmount?: number | string | null
  totalLateFeeAmount?: number | string | null
  totalInstallmentFeeAmount?: number | string | null
  totalProfitAmount?: number | string | null
}

interface ProfitStats {
  overview: ProfitOverview
  rows: any[]
  cardStats: any[]
  monthStats: any[]
}

const authStore = useAuthStore()
const activeTab = ref<TabName>('cards')
const selectedUserId = ref<number>()
const users = ref<UserNode[]>([])
const config = reactive<SpecialConfig>({})
const cards = ref<SpecialCard[]>([])
const billRows = ref<SpecialBill[]>([])
const profitStats = reactive<ProfitStats>({
  overview: {},
  rows: [],
  cardStats: [],
  monthStats: []
})

const savingConfig = ref(false)
const cardLoading = ref(false)
const savingCard = ref(false)
const billLoading = ref(false)
const profitLoading = ref(false)
const savingBillId = ref<number>()
const savingProfitBillId = ref<number>()
const deletingHistoryBills = ref(false)
const deletingFutureBills = ref(false)
const batchDeletingBills = ref(false)
const cardDialogVisible = ref(false)
const cardFormRef = ref<FormInstance>()
const billTableRef = ref<any>()

const yearOptions = [2020, 2021, 2022, 2023, 2024, 2025, 2026]
const deleteBoundaryYearOptions = [...yearOptions]
const monthOptions = Array.from({ length: 12 }, (_, index) => index + 1)
const billListSize = 10
const billPageSizeOptions = [10, 20, 50, 100]

const billQuery = reactive({
  current: 1,
  size: billListSize,
  year: new Date().getFullYear() as number | undefined,
  month: new Date().getMonth() + 1 as number | undefined,
  cardId: undefined as number | undefined
})
const billTotal = ref(0)
const deleteBoundaryYear = ref(2023)
const selectedBillRows = ref<SpecialBill[]>([])

const profitQuery = reactive({
  year: undefined as number | undefined,
  month: undefined as number | undefined,
  cardId: undefined as number | undefined
})

const profitPage = reactive({
  current: 1,
  size: 10
})
const profitPageSizeOptions = [10, 20, 50, 100]

const cardForm = reactive({
  id: undefined as number | undefined,
  bankName: '',
  cardNoLast4: '',
  totalAmount: 0,
  expireDate: '',
  billDay: undefined as number | undefined,
  repaymentDay: undefined as number | undefined,
  status: 0,
  remark: ''
})

const cardRules: FormRules = {
  bankName: [{ required: true, message: '请输入银行名称', trigger: 'blur' }],
  cardNoLast4: [
    { required: true, message: '请输入卡号后四位', trigger: 'blur' },
    { pattern: /^\d{4}$/, message: '卡号后四位必须是4位数字', trigger: 'blur' }
  ]
}

const canEdit = computed(() => ['ADMIN', 'OPERATOR'].includes(authStore.role))
const isAdmin = computed(() => authStore.role === 'ADMIN')
const userOptions = computed<SelectOption[]>(() => flattenUsers(users.value))
const cardDialogTitle = computed(() => (cardForm.id ? '编辑特殊银行卡' : '新增特殊银行卡'))
const totalCardAmount = computed(() => cards.value.reduce((sum, card) => sum + toNumber(card.totalAmount), 0))
const totalBillCount = computed(() => cards.value.reduce((sum, card) => sum + Number(card.billCount || 0), 0))
const selectedBillCard = computed(() => cards.value.find(card => Number(card.id) === Number(billQuery.cardId)))
const annualBillHint = computed(() => {
  if (!cards.value.length) return '新增银行卡后自动生成2020-2026年度账单'
  const card = selectedBillCard.value
  const year = billQuery.year ? `${billQuery.year}年` : '全部年份'
  const month = billQuery.month ? `${billQuery.month}月` : '全部月份'
  const scope = card ? cardLabel(card) : '全部银行卡'
  return `${scope} · ${year} · ${month}`
})

const profitSummaryCards = computed(() => {
  const overview = profitStats.overview || {}
  return [
    { label: '银行卡', value: String(overview.cardCount || 0) },
    { label: '账单数', value: String(overview.billCount || 0) },
    { label: '每月账单金额', value: formatMoney(overview.totalBillAmount) },
    { label: '还款手续费', value: formatMoney(overview.totalRepaymentFee), className: 'amount-income' },
    { label: '消费手续费', value: formatMoney(overview.totalConsumeFee), className: 'amount-income' },
    { label: '利息', value: formatMoney(overview.totalInterestAmount), className: 'amount-income' },
    { label: '滞纳金', value: formatMoney(overview.totalLateFeeAmount), className: 'amount-income' },
    { label: '分期费', value: formatMoney(overview.totalInstallmentFeeAmount), className: 'amount-income' },
    { label: '总计', value: formatMoney(overview.totalProfitAmount), className: 'amount-income' }
  ]
})

const profitTotal = computed(() => profitStats.rows.length)
const pagedProfitRows = computed(() => {
  const start = (profitPage.current - 1) * profitPage.size
  return profitStats.rows.slice(start, start + profitPage.size)
})
const profitPaginationText = computed(() => {
  if (!profitTotal.value) return '暂无数据'
  return `每页 ${profitPage.size} 条，共 ${profitTotal.value} 条`
})

const billSummaryTotals = computed<Record<string, number>>(() => {
  return billRows.value.reduce((totals, row) => {
    totals.billAmount += toNumber(row.billAmount)
    totals.xiaohuanRepayAmount += toNumber(row.xiaohuanRepayAmount)
    totals.customerRepayAmount += toNumber(row.customerRepayAmount)
    totals.xiaohuanConsumeAmount += toNumber(row.xiaohuanConsumeAmount)
    totals.customerNeedAmount += toNumber(row.customerNeedAmount)
    totals.customerConsumeAmount += toNumber(row.customerConsumeAmount)
    totals.diffAmount += calcDiff(row)
    totals.balance += toNumber(row.balance)
    totals.interestAmount += toNumber(row.interestAmount)
    totals.lateFeeAmount += toNumber(row.lateFeeAmount)
    totals.installmentFeeAmount += toNumber(row.installmentFeeAmount)
    return totals
  }, {
    billAmount: 0,
    xiaohuanRepayAmount: 0,
    customerRepayAmount: 0,
    xiaohuanConsumeAmount: 0,
    customerNeedAmount: 0,
    customerConsumeAmount: 0,
    diffAmount: 0,
    balance: 0,
    interestAmount: 0,
    lateFeeAmount: 0,
    installmentFeeAmount: 0
  })
})

const billSummaryRow = computed<SpecialBill>(() => ({
  id: -1,
  cardId: -1,
  billMonth: '',
  __summary: true,
  billAmount: billSummaryTotals.value.billAmount,
  xiaohuanRepayAmount: billSummaryTotals.value.xiaohuanRepayAmount,
  customerRepayAmount: billSummaryTotals.value.customerRepayAmount,
  xiaohuanConsumeAmount: billSummaryTotals.value.xiaohuanConsumeAmount,
  customerNeedAmount: billSummaryTotals.value.customerNeedAmount,
  customerConsumeAmount: billSummaryTotals.value.customerConsumeAmount,
  diffAmount: billSummaryTotals.value.diffAmount,
  balance: billSummaryTotals.value.balance,
  interestAmount: billSummaryTotals.value.interestAmount,
  lateFeeAmount: billSummaryTotals.value.lateFeeAmount,
  installmentFeeAmount: billSummaryTotals.value.installmentFeeAmount
}))

const billTableRows = computed<SpecialBill[]>(() => {
  if (!billRows.value.length) return []
  return [...billRows.value, billSummaryRow.value]
})

onMounted(async () => {
  await refreshAll()
})

watch(activeTab, async (tab) => {
  if (tab === 'bills') {
    await fetchBills()
  }
  if (tab === 'profit') {
    await fetchProfitStats()
  }
})

watch(
  () => [billQuery.year, billQuery.month, billQuery.cardId],
  () => {
    billQuery.current = 1
    queueBillFetch()
  }
)

watch(
  () => [billQuery.current, billQuery.size],
  () => {
    queueBillFetch()
  }
)

watch(
  () => [profitQuery.year, profitQuery.month, profitQuery.cardId],
  () => {
    profitPage.current = 1
    queueProfitFetch()
  }
)

watch(
  () => [billTotal.value, billQuery.size],
  () => {
    const maxPage = Math.max(1, Math.ceil(billTotal.value / billQuery.size))
    if (billQuery.current > maxPage) {
      billQuery.current = maxPage
    }
  }
)

watch(
  billSummaryTotals,
  () => {
    nextTick(() => billTableRef.value?.doLayout?.())
  },
  { deep: true }
)

watch(
  () => [profitTotal.value, profitPage.size],
  () => {
    const maxPage = Math.max(1, Math.ceil(profitTotal.value / profitPage.size))
    if (profitPage.current > maxPage) {
      profitPage.current = maxPage
    }
  }
)

let billFetchTimer = 0
let profitFetchTimer = 0

function queueBillFetch() {
  if (activeTab.value !== 'bills') return
  window.clearTimeout(billFetchTimer)
  billFetchTimer = window.setTimeout(() => {
    fetchBills()
  }, 250)
}

function queueProfitFetch() {
  if (activeTab.value !== 'profit') return
  window.clearTimeout(profitFetchTimer)
  profitFetchTimer = window.setTimeout(() => {
    fetchProfitStats()
  }, 120)
}

function refreshBillSummary() {
  billRows.value = [...billRows.value]
  nextTick(() => billTableRef.value?.doLayout?.())
}

function clearBillSelection() {
  selectedBillRows.value = []
  billTableRef.value?.clearSelection?.()
}

async function refreshAll() {
  await fetchUsers()
  await fetchConfig()
  await fetchCards()
  await refreshActiveTabData()
}

async function fetchUsers() {
  const res = await getUserTreeApi()
  users.value = res.data || []
}

async function fetchConfig() {
  const res = await getSpecialConfigApi()
  Object.assign(config, res.data || {})
  selectedUserId.value = config.userId
}

async function saveConfig() {
  if (!selectedUserId.value) return
  savingConfig.value = true
  try {
    const res = await saveSpecialConfigApi({ userId: selectedUserId.value })
    Object.assign(config, res.data || {})
    ElMessage.success('特殊用户配置已保存')
    await fetchCards()
    await refreshActiveTabData()
  } finally {
    savingConfig.value = false
  }
}

async function fetchCards() {
  if (!config.userId) {
    cards.value = []
    billQuery.cardId = undefined
    profitQuery.cardId = undefined
    return
  }
  cardLoading.value = true
  try {
    const res = await getSpecialCardsApi()
    cards.value = sortCardsByRepaymentDay(res.data || [])
    normalizeSelectedCardFilters()
  } finally {
    cardLoading.value = false
  }
}

function normalizeSelectedCardFilters() {
  const cardIds = new Set(cards.value.map(card => Number(card.id)))
  if (!cards.value.length) {
    billQuery.cardId = undefined
    profitQuery.cardId = undefined
    return
  }
  if (billQuery.cardId && !cardIds.has(Number(billQuery.cardId))) {
    billQuery.cardId = undefined
  }
  if (profitQuery.cardId && !cardIds.has(Number(profitQuery.cardId))) {
    profitQuery.cardId = undefined
  }
}

function openCreateCard() {
  resetCardForm()
  cardDialogVisible.value = true
}

function openEditCard(card: SpecialCard) {
  cardForm.id = card.id
  cardForm.bankName = card.bankName || ''
  cardForm.cardNoLast4 = card.cardNoLast4 || ''
  cardForm.totalAmount = toNumber(card.totalAmount)
  cardForm.expireDate = card.expireDate || ''
  cardForm.billDay = card.billDay || undefined
  cardForm.repaymentDay = card.repaymentDay || undefined
  cardForm.status = card.status ?? 0
  cardForm.remark = card.remark || ''
  cardDialogVisible.value = true
}

async function submitCard() {
  await cardFormRef.value?.validate()
  savingCard.value = true
  try {
    const payload = {
      id: cardForm.id,
      userId: config.userId,
      bankName: cardForm.bankName,
      cardNoLast4: cardForm.cardNoLast4,
      totalAmount: cardForm.totalAmount,
      expireDate: cardForm.expireDate,
      billDay: cardForm.billDay,
      repaymentDay: cardForm.repaymentDay,
      status: cardForm.status,
      remark: cardForm.remark
    }
    if (cardForm.id) {
      await updateSpecialCardApi(payload)
      ElMessage.success('银行卡已更新')
    } else {
      await saveSpecialCardApi(payload)
      ElMessage.success('银行卡已新增，2020-2026账单已生成')
    }
    cardDialogVisible.value = false
    await fetchCards()
    await refreshActiveTabData()
  } finally {
    savingCard.value = false
  }
}

async function deleteCard(card: SpecialCard) {
  await ElMessageBox.confirm(
    `确认删除 ${card.bankName} 尾号 ${card.cardNoLast4}？确认后这张卡关联的所有账单信息也会一起删除。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  await deleteSpecialCardApi(card.id)
  ElMessage.success('银行卡已删除')
  await fetchCards()
  await refreshActiveTabData()
}

function resetCardForm() {
  cardForm.id = undefined
  cardForm.bankName = ''
  cardForm.cardNoLast4 = ''
  cardForm.totalAmount = 0
  cardForm.expireDate = ''
  cardForm.billDay = undefined
  cardForm.repaymentDay = undefined
  cardForm.status = 0
  cardForm.remark = ''
}

async function fetchBills() {
  if (!config.userId) {
    billRows.value = []
    billTotal.value = 0
    return
  }
  billLoading.value = true
  try {
    const res = await getSpecialBillPageApi({
      current: billQuery.current,
      size: billQuery.size,
      year: billQuery.year,
      month: billQuery.month,
      cardId: billQuery.cardId
    })
    billRows.value = sortRowsByRepaymentDay(res.data?.records || [])
    billTotal.value = res.data?.total || 0
    clearBillSelection()
  } finally {
    billLoading.value = false
  }
}

async function resetBillQuery() {
  billQuery.current = 1
  billQuery.size = billListSize
  billQuery.year = new Date().getFullYear()
  billQuery.month = new Date().getMonth() + 1
  billQuery.cardId = undefined
  await fetchBills()
}

async function deleteHistoryBills() {
  if (!billQuery.cardId) {
    ElMessage.warning('请先选择银行卡')
    return
  }
  const card = selectedBillCard.value
  const cardName = card ? cardLabel(card) : '当前银行卡'
  await ElMessageBox.confirm(
    `确认删除 ${cardName} ${deleteBoundaryYear.value} 年之前的所有账单？删除后不会影响 ${deleteBoundaryYear.value} 年及之后账单。`,
    '批量删除确认',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  deletingHistoryBills.value = true
  try {
    const res = await deleteSpecialBillsBeforeYearApi({
      cardId: billQuery.cardId,
      beforeYear: deleteBoundaryYear.value
    })
    ElMessage.success(`已删除 ${res.data || 0} 条账单`)
    clearBillSelection()
    await fetchCards()
    await fetchBills()
  } finally {
    deletingHistoryBills.value = false
  }
}

async function deleteFutureBills() {
  if (!billQuery.cardId) {
    ElMessage.warning('请先选择银行卡')
    return
  }
  const card = selectedBillCard.value
  const cardName = card ? cardLabel(card) : '当前银行卡'
  await ElMessageBox.confirm(
    `确认删除 ${cardName} ${deleteBoundaryYear.value} 年之后的所有账单？删除后不会影响 ${deleteBoundaryYear.value} 年及之前账单。`,
    '批量删除确认',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  deletingFutureBills.value = true
  try {
    const res = await deleteSpecialBillsAfterYearApi({
      cardId: billQuery.cardId,
      afterYear: deleteBoundaryYear.value
    })
    ElMessage.success(`已删除 ${res.data || 0} 条账单`)
    clearBillSelection()
    await fetchCards()
    await fetchBills()
  } finally {
    deletingFutureBills.value = false
  }
}

async function batchDeleteBills() {
  const ids = selectedBillRows.value.map(row => Number(row.id)).filter(id => id > 0)
  if (!ids.length) return
  await ElMessageBox.confirm(
    `确认删除选中的 ${ids.length} 条特殊账单？`,
    '批量删除账单',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  batchDeletingBills.value = true
  try {
    await batchDeleteSpecialBillsApi(ids)
    ElMessage.success('批量删除成功')
    clearBillSelection()
    await fetchCards()
    await fetchBills()
  } finally {
    batchDeletingBills.value = false
  }
}

async function saveBill(row: SpecialBill) {
  if (!assertSpecialBillEditable(row)) return
  savingBillId.value = row.id
  try {
    await updateSpecialBillApi({
      id: row.id,
      billDay: row.billDay || undefined,
      repaymentDay: row.repaymentDay || undefined,
      billAmount: toNumber(row.billAmount),
      billAmountVerified: Boolean(row.billAmountVerified),
      xiaohuanRepayAmount: toNumber(row.xiaohuanRepayAmount),
      xiaohuanRepayVerified: Boolean(row.xiaohuanRepayVerified),
      customerRepayAmount: toNumber(row.customerRepayAmount),
      customerRepayVerified: Boolean(row.customerRepayVerified),
      xiaohuanConsumeAmount: toNumber(row.xiaohuanConsumeAmount),
      xiaohuanConsumeVerified: Boolean(row.xiaohuanConsumeVerified),
      customerNeedAmount: toNumber(row.customerNeedAmount),
      customerNeedVerified: Boolean(row.customerNeedVerified),
      customerConsumeAmount: toNumber(row.customerConsumeAmount),
      customerConsumeVerified: Boolean(row.customerConsumeVerified),
      balance: toNumber(row.balance),
      interestAmount: toNumber(row.interestAmount),
      lateFeeAmount: toNumber(row.lateFeeAmount),
      installmentFeeAmount: toNumber(row.installmentFeeAmount),
      remark: row.remark
    })
    ElMessage.success('账单已保存')
    refreshBillSummary()
  } finally {
    savingBillId.value = undefined
  }
}

async function fetchProfitStats() {
  if (!config.userId) {
    Object.assign(profitStats, { overview: {}, rows: [], cardStats: [], monthStats: [] })
    return
  }
  profitLoading.value = true
  try {
    const res = await getSpecialProfitStatsApi({
      year: profitQuery.year,
      month: profitQuery.month,
      cardId: profitQuery.cardId
    })
    const data = res.data || { overview: {}, rows: [], cardStats: [], monthStats: [] }
    Object.assign(profitStats, {
      ...data,
      rows: sortRowsByRepaymentDay(data.rows || [])
    })
  } finally {
    profitLoading.value = false
  }
}

async function saveProfitExtras(row: any) {
  if (!assertSpecialBillEditable(row)) return
  savingProfitBillId.value = row.billId
  try {
    await updateSpecialProfitExtraFeesApi({
      billId: row.billId,
      interestAmount: toNumber(row.interestAmount),
      lateFeeAmount: toNumber(row.lateFeeAmount),
      installmentFeeAmount: toNumber(row.installmentFeeAmount)
    })
    ElMessage.success('收益费用已保存')
    await fetchProfitStats()
  } finally {
    savingProfitBillId.value = undefined
  }
}

async function refreshActiveTabData() {
  if (activeTab.value === 'bills') {
    await fetchBills()
  }
  if (activeTab.value === 'profit') {
    await fetchProfitStats()
  }
}

async function resetProfitQuery() {
  profitQuery.year = undefined
  profitQuery.month = undefined
  profitQuery.cardId = undefined
  profitPage.current = 1
  await fetchProfitStats()
}

function flattenUsers(tree: UserNode[], parentName = ''): SelectOption[] {
  const result: SelectOption[] = []
  for (const user of tree || []) {
    if (user.status === 1) continue
    const label = parentName ? `${parentName} / ${user.name}` : user.name
    result.push({ label, value: Number(user.id) })
    if (user.children?.length) {
      result.push(...flattenUsers(user.children, user.name))
    }
  }
  return result
}

function cardLabel(card: SpecialCard) {
  return `${card.bankName || '-'} / 尾号 ${card.cardNoLast4 || '-'}`
}

function statusText(status?: number) {
  return status === 1 ? '停用' : '正常'
}

function sortCardsByRepaymentDay(list: SpecialCard[]) {
  return sortRowsByRepaymentDay(list)
}

function sortRowsByRepaymentDay<T extends Record<string, any>>(list: T[]) {
  return [...list].sort(compareRepaymentDay)
}

function compareRepaymentDay(a: Record<string, any>, b: Record<string, any>) {
  const dayDiff = repaymentDaySortValue(a) - repaymentDaySortValue(b)
  if (dayDiff !== 0) return dayDiff
  const yearDiff = toNumber(a.billYear) - toNumber(b.billYear)
  if (yearDiff !== 0) return yearDiff
  const monthDiff = toNumber(a.billMonthNo) - toNumber(b.billMonthNo)
  if (monthDiff !== 0) return monthDiff
  const cardDiff = toNumber(a.cardId ?? a.id) - toNumber(b.cardId ?? b.id)
  if (cardDiff !== 0) return cardDiff
  return String(a.bankName || '').localeCompare(String(b.bankName || ''), 'zh-CN')
}

function repaymentDaySortValue(row: Record<string, any>) {
  const day = Number(row.repaymentDay)
  return Number.isFinite(day) && day > 0 ? Math.trunc(day) : 999
}

function formatDayOfMonth(day: number | string | null | undefined) {
  const value = Number(day)
  return Number.isFinite(value) && value > 0 ? `${String(Math.trunc(value)).padStart(2, '0')}日` : '-'
}

function formatDayInput(value: number | string | null | undefined) {
  const text = formatDayOfMonth(value)
  return text === '-' ? '' : text
}

function parseDayInput(value: string) {
  return value.replace(/[^\d]/g, '')
}

function updateSpecialBillDay(row: SpecialBill, field: 'billDay' | 'repaymentDay', value: string) {
  const digits = parseDayInput(value)
  if (!digits) {
    row[field] = null
    return
  }
  const day = Number(digits)
  if (!Number.isFinite(day)) return
  row[field] = Math.min(31, Math.max(1, Math.trunc(day)))
}

function selectInputText(event: FocusEvent) {
  if (event.target instanceof HTMLInputElement) {
    event.target.select()
  }
}

function specialCardExpireStatus(expireDate: string | null | undefined) {
  return getCardExpireStatus(expireDate)
}

function isSpecialCardExpiringSoon(expireDate: string | null | undefined) {
  return specialCardExpireStatus(expireDate) === 'soon'
}

function isSpecialCardExpired(expireDate: string | null | undefined) {
  return specialCardExpireStatus(expireDate) === 'expired'
}

function specialCardExpireClass(expireDate: string | null | undefined) {
  const status = specialCardExpireStatus(expireDate)
  return {
    'is-expire-warning': status === 'soon',
    'is-expire-expired': status === 'expired'
  }
}

function specialCardExpireTitle(expireDate: string | null | undefined) {
  const status = specialCardExpireStatus(expireDate)
  if (status === 'soon') return '银行卡有效期将在一个月内到期'
  if (status === 'expired') return '银行卡有效期已过期'
  return undefined
}

function isSpecialCardDisabled(card: SpecialCard | null | undefined) {
  return Number(card?.status ?? 0) === 1
}

function specialCardTitle(card: SpecialCard) {
  const parts: string[] = []
  if (isSpecialCardDisabled(card)) {
    parts.push('银行卡已停用，关联账单不可编辑')
  }
  const expireTitle = specialCardExpireTitle(card.expireDate)
  if (expireTitle) {
    parts.push(expireTitle)
  }
  return parts.join('；') || undefined
}

function findSpecialCard(cardId: number | string | null | undefined) {
  const targetId = Number(cardId || 0)
  return cards.value.find(card => Number(card.id) === targetId) || null
}

function isSpecialBillCardDisabled(row: { cardStatus?: number | null; cardId?: number | null } | null | undefined) {
  if (!row) return false
  if (row.cardStatus !== null && row.cardStatus !== undefined) {
    return Number(row.cardStatus) === 1
  }
  return isSpecialCardDisabled(findSpecialCard(row.cardId))
}

function canEditSpecialBillRow(row: SpecialBill | null | undefined) {
  return canEdit.value && !row?.__summary && !isSpecialBillCardDisabled(row)
}

function canEditSpecialProfitRow(row: any) {
  return canEdit.value && !isSpecialBillCardDisabled(row)
}

function assertSpecialBillEditable(row: { cardStatus?: number | null; cardId?: number | null } | null | undefined) {
  if (!isSpecialBillCardDisabled(row)) return true
  ElMessage.warning('银行卡已停用，账单不能编辑')
  return false
}

function calcDiff(row: SpecialBill) {
  return roundMoney(toNumber(row.xiaohuanRepayAmount) - toNumber(row.xiaohuanConsumeAmount))
}

function billDiffValue(row: SpecialBill) {
  return row.__summary ? toNumber(row.diffAmount) : calcDiff(row)
}

function billRowKey(row: SpecialBill) {
  return row.__summary ? 'summary' : row.id
}

function isSelectableBillRow(row: SpecialBill) {
  return !row.__summary
}

function handleBillSelectionChange(selection: SpecialBill[]) {
  selectedBillRows.value = (selection || []).filter(row => isSelectableBillRow(row))
}

function billRowClassName({ row }: { row: SpecialBill }) {
  if (row.__summary) return 'bill-summary-row'
  return isSpecialBillCardDisabled(row) ? 'card-disabled-row' : ''
}

function profitRowClassName({ row }: { row: any }) {
  return isSpecialBillCardDisabled(row) ? 'card-disabled-row' : ''
}

function calcRepaymentFee(row: SpecialBill) {
  return roundMoney(toNumber(row.billAmount) * toNumber(row.feeRate) / 100)
}

function calcConsumeFee(row: SpecialBill) {
  return roundMoney((toNumber(row.xiaohuanConsumeAmount) + toNumber(row.customerConsumeAmount)) * toNumber(row.feeRate) / 100)
}

function calcProfitTotal(row: any) {
  return roundMoney(
    toNumber(row.repaymentFee) +
    toNumber(row.consumeFee) +
    toNumber(row.interestAmount) +
    toNumber(row.lateFeeAmount) +
    toNumber(row.installmentFeeAmount)
  )
}

function toNumber(value: number | string | null | undefined) {
  const num = Number(value ?? 0)
  return Number.isFinite(num) ? num : 0
}

function roundMoney(value: number | string | null | undefined) {
  return Number(toNumber(value).toFixed(2))
}

function formatMoney(value: number | string | null | undefined) {
  return toNumber(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

function formatRate(value: number | string | null | undefined) {
  return toNumber(value).toFixed(2)
}

</script>

<style scoped>
.special-page {
  --special-list-text-color: #1f2a37;
  --special-list-muted-color: #667085;
  --special-list-empty-color: #98a2b3;
  --special-list-text-weight: 750;
  --special-list-strong-weight: 800;
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
  background: #f5f7fb;
  box-sizing: border-box;
}

.special-header,
.card-toolbar,
.filter-line,
.bill-table-shell,
.profit-summary-grid,
.profit-table-block {
  border: 1px solid #dbe2ea;
  border-radius: 8px;
  background: #fff;
}

.special-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 58px;
  padding: 8px 10px;
  flex-shrink: 0;
}

.header-main {
  min-width: 0;
}

.page-title {
  color: #1f2a37;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.2;
}

.page-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
}

.config-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.user-select {
  width: 260px;
}

.special-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.special-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 8px;
  border: 1px solid #dbe2ea;
  border-radius: 8px;
  background: #fff;
  flex-shrink: 0;
}

.special-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  padding-top: 6px;
  overflow: hidden;
}

.special-tabs :deep(.el-tab-pane) {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  overflow: hidden;
}

.card-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 44px;
  padding: 6px 8px;
  flex-shrink: 0;
}

.stat-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(120px, 1fr));
  gap: 8px;
  min-width: 0;
}

.stat-item,
.profit-summary-item {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  min-height: 34px;
  padding: 4px 6px;
  border: 1px solid #e5eaf1;
  border-radius: 8px;
  background: #f8fafc;
}

.stat-item span,
.profit-summary-item span {
  color: #7c8799;
  font-size: 10px;
  font-weight: 700;
}

.stat-item strong,
.profit-summary-item strong {
  margin-top: 3px;
  overflow: hidden;
  color: #1f2a37;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.15;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.special-card-grid {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 2px 4px 2px 2px;
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, .55) transparent;
}

.special-card-grid::-webkit-scrollbar {
  width: 6px;
}

.special-card-grid::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, .55);
  border-radius: 999px;
}

.bank-card-tile {
  position: relative;
  min-width: 0;
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid #dbe2ea;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
  overflow: hidden;
}

.bank-card-tile.disabled {
  border-color: rgba(207, 19, 34, .48);
  background: #fff1f0;
  box-shadow: inset 0 0 0 1px rgba(207, 19, 34, .16);
}

.bank-card-tile.disabled .bank-mark {
  border-color: #ffa39e;
  color: #cf1322;
  background: #fff1f0;
}

.bank-card-tile.is-expire-warning {
  border-color: rgba(217, 119, 6, .48);
  background: #fff8eb;
  box-shadow: inset 0 0 0 1px rgba(217, 119, 6, .16), 0 4px 12px rgba(217, 119, 6, .08);
}

.bank-card-tile.is-expire-expired {
  border-color: rgba(207, 19, 34, .48);
  background: #fff1f0;
  box-shadow: inset 0 0 0 1px rgba(207, 19, 34, .14), 0 4px 12px rgba(207, 19, 34, .08);
}

.bank-card-tile.disabled.is-expire-warning,
.bank-card-tile.disabled.is-expire-expired {
  border-color: rgba(207, 19, 34, .55);
  background: #fff1f0;
  box-shadow: inset 0 0 0 1px rgba(207, 19, 34, .2), 0 4px 12px rgba(207, 19, 34, .08);
}

.special-card-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.bank-mark {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  border: 1px solid rgba(9, 88, 217, .16);
  color: #0958d9;
  background: #eaf2ff;
  flex-shrink: 0;
}

.special-card-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.special-card-row-top,
.special-card-row-sub {
  display: flex;
  align-items: center;
  gap: 0;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  line-height: 1.5;
}

.special-card-row-top :deep(.el-tag) {
  margin-left: 7px;
  flex-shrink: 0;
}

.special-card-bank {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--special-list-text-color);
  font-size: 13px;
  font-weight: var(--special-list-strong-weight);
  flex-shrink: 1;
}

.special-card-last4 {
  color: var(--special-list-text-color);
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: var(--special-list-strong-weight);
  flex-shrink: 0;
}

.special-card-sep {
  display: inline-block;
  width: 1px;
  height: 10px;
  margin: 0 7px;
  background: rgba(148, 163, 184, .4);
  flex-shrink: 0;
}

.special-card-dot {
  display: inline-block;
  width: 3px;
  height: 3px;
  margin: 0 5px;
  border-radius: 50%;
  background: rgba(148, 163, 184, .5);
  flex-shrink: 0;
}

.special-card-type,
.special-card-date {
  color: var(--special-list-text-color);
  font-size: 11.5px;
  font-weight: var(--special-list-text-weight);
  flex-shrink: 0;
}

.special-card-expire-date {
  width: 48px;
  min-width: 48px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  padding: 0 5px;
  border: 1px solid transparent;
  border-radius: 999px;
  line-height: 1;
}

.special-card-label {
  margin-right: 2px;
  color: var(--special-list-muted-color);
  font-size: 11px;
  font-weight: var(--special-list-text-weight);
  flex-shrink: 0;
}

.special-card-date.empty {
  color: var(--special-list-empty-color);
  font-weight: var(--special-list-text-weight);
}

.special-card-expire-date.expire-warning,
.special-card-expire-date.expire-expired,
.special-card-status-disabled {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  padding: 1px 5px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
  line-height: 1.2;
}

.special-card-expire-date.expire-warning {
  color: #ad6800;
  background: #fff1b8;
  border: 1px solid #ffd666;
}

.special-card-expire-date.expire-expired,
.special-card-status-disabled {
  color: #a8071a;
  background: #ffd8d6;
  border: 1px solid #ffa39e;
}

.special-card-status-disabled {
  margin-left: 7px;
}

.special-card-remark {
  min-width: 0;
  overflow: hidden;
  color: var(--special-list-muted-color);
  font-size: 11px;
  font-weight: var(--special-list-text-weight);
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.special-card-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  min-width: 260px;
  flex-shrink: 0;
}

.tile-amount {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 2px;
  min-width: 118px;
  min-height: 38px;
  padding: 0;
  background: transparent;
}

.tile-amount span {
  color: var(--special-list-muted-color);
  font-size: 10.5px;
  font-weight: var(--special-list-text-weight);
}

.tile-amount strong {
  min-width: 0;
  overflow: hidden;
  color: var(--special-list-text-color);
  font-family: var(--font-mono);
  font-size: 17px;
  font-weight: var(--special-list-strong-weight);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tile-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  flex-shrink: 0;
}

.filter-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 36px;
  padding: 4px 8px;
  flex-shrink: 0;
}

.filter-line :deep(.el-button) {
  margin-left: 0;
}

.filter-item {
  width: 108px;
}

.filter-delete-year {
  width: 154px;
}

.filter-card {
  width: 176px;
}

.filter-hint {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  color: #667085;
  font-size: 12.5px;
  font-weight: 700;
  white-space: nowrap;
}

.bill-table-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 4px;
}

.bill-table-shell :deep(.el-table) {
  flex: 1;
  width: 100% !important;
  font-size: 13px;
}

.bill-table-shell :deep(.el-table__body-wrapper),
.profit-table-block :deep(.el-table__body-wrapper) {
  overflow-x: hidden;
}

.bill-table-shell :deep(.el-table th.el-table__cell) {
  height: 30px;
  padding: 2px 0;
  color: #3f4a5f;
  font-size: 12.5px;
  font-weight: 800;
}

.bill-table-shell :deep(.el-table td.el-table__cell) {
  height: 36px;
  padding: 2px 0;
}

.bill-table-shell :deep(.el-table__row) {
  height: 36px;
}

.bill-table-shell :deep(.bill-summary-row) {
  background: #f8fafc;
  font-weight: 800;
}

.bill-table-shell :deep(.bill-summary-row td.el-table__cell) {
  border-top: 1px solid #cbd5e1;
}

.bill-table-shell :deep(.card-disabled-row td.el-table__cell),
.profit-table-block :deep(.card-disabled-row td.el-table__cell) {
  background: #fff1f0 !important;
  box-shadow: inset 0 1px 0 #ffa39e, inset 0 -1px 0 #ffa39e;
}

.bill-table-shell :deep(.card-disabled-row td.el-table__cell:first-child),
.profit-table-block :deep(.card-disabled-row td.el-table__cell:first-child) {
  box-shadow: inset 3px 0 0 #cf1322, inset 0 1px 0 #ffa39e, inset 0 -1px 0 #ffa39e;
}

.bill-table-shell :deep(.el-table .cell),
.profit-table-block :deep(.el-table .cell) {
  min-width: 0;
  padding: 0 4px;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  word-break: normal;
}

.money-input {
  width: 100%;
  min-width: 0;
}

.day-input {
  width: 100%;
  min-width: 0;
}

.amount-verify-cell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 34px;
  align-items: center;
  gap: 3px;
}

.bill-table-shell .money-input :deep(.el-input__wrapper),
.bill-table-shell .day-input :deep(.el-input__wrapper),
.bill-table-shell :deep(.el-input__wrapper) {
  min-height: 28px;
  padding: 0 5px;
  border-radius: 6px;
}

.bill-table-shell :deep(.el-switch) {
  --el-switch-on-color: #2f9e44;
  height: 20px;
  min-width: 32px;
  width: 32px;
}

.bill-table-shell :deep(.el-switch__core) {
  min-width: 32px;
  width: 32px;
  height: 18px;
}

.bill-table-shell :deep(.el-switch__core .el-switch__action) {
  width: 14px;
  height: 14px;
}

.money-input :deep(.el-input__inner) {
  color: var(--special-list-text-color);
  text-align: right;
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: var(--special-list-strong-weight);
}

.bill-table-shell .day-input :deep(.el-input__inner),
.bill-table-shell :deep(.el-input__inner),
.profit-table-block :deep(.el-input__inner) {
  color: var(--special-list-text-color);
  font-size: 13px;
  font-weight: var(--special-list-strong-weight);
}

.money-text {
  color: var(--special-list-text-color);
  font-family: var(--font-mono);
  font-weight: var(--special-list-strong-weight);
}

.strong-cell {
  color: var(--special-list-text-color);
  font-family: inherit;
  font-weight: var(--special-list-strong-weight);
}

.bill-table-shell :deep(.el-table td.el-table__cell),
.profit-table-block :deep(.el-table td.el-table__cell) {
  color: var(--special-list-text-color);
  font-weight: var(--special-list-text-weight);
}

.profit-table-block :deep(.el-table td.el-table__cell .cell) {
  color: var(--special-list-text-color);
  font-weight: var(--special-list-text-weight);
}

.profit-table-block :deep(.el-table td.el-table__cell[align="right"] .cell),
.profit-table-block :deep(.el-table td.el-table__cell.is-right .cell),
.bill-table-shell :deep(.el-table td.el-table__cell[align="right"] .cell),
.bill-table-shell :deep(.el-table td.el-table__cell.is-right .cell) {
  font-family: var(--font-mono);
  font-weight: var(--special-list-strong-weight);
}

.card-disabled-pill {
  display: inline-flex;
  align-items: center;
  height: 16px;
  margin-left: 4px;
  padding: 0 5px;
  border: 1px solid #ffa39e;
  border-radius: 999px;
  color: #a8071a;
  background: #ffd8d6;
  font-size: 10px;
  font-weight: 800;
  line-height: 1;
  vertical-align: middle;
}

.summary-label,
.summary-number {
  font-weight: 800;
}

.profit-summary-grid {
  display: grid;
  grid-template-columns: repeat(9, minmax(0, 1fr));
  gap: 4px;
  padding: 3px 4px;
  flex-shrink: 0;
}

.profit-tables {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  overflow: hidden;
}

.profit-table-block {
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 4px;
}

.block-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 3px;
  color: #1f2a37;
  font-size: 13px;
  font-weight: 800;
  flex-shrink: 0;
}

.profit-table-block :deep(.el-table) {
  flex: 1;
  width: 100% !important;
  font-size: 13px;
}

.profit-table-block :deep(.el-table th.el-table__cell) {
  height: 30px;
  padding: 2px 0;
  color: #3f4a5f;
  font-size: 12.5px;
  font-weight: 800;
}

.profit-table-block :deep(.el-table td.el-table__cell) {
  height: 36px;
  padding: 2px 0;
}

.profit-table-block .money-input :deep(.el-input__wrapper) {
  min-height: 28px;
  padding: 0 5px;
  border-radius: 6px;
}

.profit-pagination {
  min-height: 26px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 3px;
  flex-shrink: 0;
}

.bill-pagination {
  min-height: 26px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 3px;
  flex-shrink: 0;
}

.bill-pagination :deep(.el-pagination),
.profit-pagination :deep(.el-pagination) {
  --el-pagination-button-height: 22px;
  --el-pagination-button-width: 22px;
  --el-pagination-font-size: 12px;
}

.amount-income {
  color: #2f9e44 !important;
}

.amount-cost {
  color: #cf1322 !important;
}

.full-input {
  width: 100%;
}

@media (max-width: 1360px) {
  .profit-tables {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 980px) {
  .special-header,
  .config-bar,
  .card-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .user-select,
  .filter-card {
    width: 100%;
  }

  .stat-strip,
  .profit-summary-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .bank-card-tile {
    align-items: stretch;
    flex-direction: column;
    gap: 8px;
  }

  .special-card-row-top,
  .special-card-row-sub {
    flex-wrap: wrap;
    row-gap: 2px;
    overflow: visible;
    white-space: normal;
  }

  .special-card-bank {
    flex-basis: auto;
  }

  .special-card-right {
    min-width: 0;
    width: 100%;
    justify-content: space-between;
    gap: 8px;
  }

  .tile-amount {
    align-items: flex-start;
    min-width: 0;
  }

  .tile-amount strong {
    max-width: 100%;
  }

  .tile-actions {
    flex-wrap: nowrap;
  }

  .tile-actions :deep(.el-button) {
    margin-left: 0;
  }
}
</style>
