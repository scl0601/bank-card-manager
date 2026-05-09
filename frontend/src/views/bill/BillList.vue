<template>
  <div class="bill-page" :class="{ 'single-card-annual-page': singleCardAnnualMode }">
    <div class="page-header">
      <div class="header-copy">
        <div class="header-title-row">
          <div class="header-title">账单信息</div>
          <span v-if="detailModeMessage" class="detail-mode-chip">明细模式</span>
        </div>
      </div>

      <div class="header-actions">
        <el-button type="primary" class="action-btn" :icon="Plus" @click="openCreateBillDialog">新增账单</el-button>
        <el-button class="action-btn" :icon="RefreshRight" @click="refresh">刷新</el-button>
        <ExportButton class="export-btn" :loading="exporting" @click="handleExport" />
      </div>
    </div>

    <div class="app-search-panel card-shell bill-search-panel">
      <div class="app-search-main">
        <div class="app-search-title">筛选</div>
        <el-input
          v-model="query.ownerName"
          class="app-search-item app-search-item-sm"
          placeholder="持卡人姓名"
          clearable
          maxlength="20"
        />
        <el-input
          v-model="query.cardName"
          class="app-search-item app-search-item-sm"
          placeholder="银行名称或尾号"
          clearable
          maxlength="30"
        />
        <el-date-picker
          v-model="billMonthFilter"
          class="app-search-item app-search-item-sm"
          type="month"
          value-format="YYYY-MM"
          placeholder="筛选还款月份"
          :editable="false"
          clearable
        />
        <el-select
          v-model="query.status"
          class="app-search-item app-search-item-sm"
          placeholder="请选择账单状态"
          clearable
          @change="handleStatusFilterChange"
        >
          <el-option v-for="item in BILL_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <div class="app-search-actions">
          <el-button v-if="detailModeMessage" link type="primary" @click="clearRouteFilters">查看全部</el-button>
          <el-button class="app-search-btn" @click="handleResetAll">重置</el-button>
        </div>
      </div>

      <div class="app-search-extra quick-menu-bar">
        <div class="menu-list menu-list-inline">
          <button
            v-for="item in quickMenus"
            :key="item.key"
            type="button"
            class="menu-item"
            :class="{ active: activeQuickMenu === item.key }"
            @click="applyQuickMenu(item.value)"
          >
            <span class="menu-dot" :style="{ background: item.color }" />
            <span class="menu-label">{{ item.label }}</span>
            <span class="menu-count">{{ item.count }}</span>
          </button>
        </div>
      </div>
    </div>

    <div class="workspace-grid">
      <section class="data-panel card-shell">
        <div class="panel-head data-head">
          <div>
            <div class="panel-title">
              账单数据区
              <el-button class="back-btn-inline" :icon="Back" @click="router.push('/cards')">返回</el-button>
            </div>
          </div>
          <div v-if="isAdmin && selectedBillRows.length > 0" class="table-batch-actions">
            <el-button type="danger" size="small" @click="handleBatchDeleteBills">
              批量删除账单 ({{ selectedBillRows.length }})
            </el-button>
          </div>
        </div>

        <PageTable
          ref="billTableRef"
          class="bill-page-table"
          :class="{
            'single-card-annual-table': singleCardAnnualMode,
            'bill-page-table-scrollable': isBillTableScrollable
          }"
          :data="tableDisplayList"
          :loading="loading"
          :total="total"
          :page-num="query.pageNum"
          :page-size="safeBillPageSize"
          :height="billTableHeight"
          :empty-text="loading ? ' ' : '暂无数据'"
          :show-pagination="false"
          border
          row-key="id"
          :expand-row-keys="expandedRowKeys"
          table-layout="fixed"
          :row-class-name="billRowClassName"
          @update:page-num="val => query.pageNum = val"
          @update:page-size="val => query.pageSize = val"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
          @selection-change="handleBillSelectionChange"
        >
      <el-table-column v-if="isAdmin" type="selection" width="42" align="center" reserve-selection :selectable="isSelectableBillRow" />
      <el-table-column type="expand" width="1" class-name="expand-toggle-col" label-class-name="expand-toggle-col">
        <template #default="{ row }">
          <div class="expand-bill-content">
            <div class="detail-section" v-loading="detailLoadingMap[row.id] && detailLoadedMap[row.id]">
                <div class="detail-header">
                  <div class="detail-header-main">
                    <span class="detail-title">本月明细流水</span>
                    <el-button type="success" size="small" @click="openAddDetail(row)">+ 新增</el-button>
                  </div>
                </div>

              <BillDetailSkeleton v-if="detailLoadingMap[row.id] && !detailLoadedMap[row.id]" />

              <template v-else-if="detailListMap[row.id]?.length">
                <div v-if="selectedDetailsMap[row.id]?.length > 0" class="batch-toolbar">
                  <el-button type="danger" size="small" @click="handleBatchDelete(row.id)">
                    批量删除 ({{ selectedDetailsMap[row.id].length }})
                  </el-button>
                  <el-dropdown @command="(type: number) => handleBatchUpdateType(row.id, type)">
                    <el-button size="small">
                      批量修改交易类型 <el-icon><arrow-down /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item :command="DETAIL_TYPE_VALUE.INCOME">还款</el-dropdown-item>
                        <el-dropdown-item :command="DETAIL_TYPE_VALUE.EXPENSE">消费</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>

                <div class="detail-split-grid">
                  <div class="detail-pane">
                    <div class="detail-pane-head">
                      <div class="detail-pane-head-main">
                        <div class="detail-pane-title">
                          <span>还款</span>
                          <el-tag type="success" size="small" effect="light">{{ detailTypeTotalCount(row.id, DETAIL_TYPE_VALUE.INCOME) }}</el-tag>
                          <div class="detail-verify-switch" @click.stop>
                            <span>还款核实</span>
                            <el-switch
                              :model-value="Boolean(row.verified)"
                              active-text="已核实"
                              inactive-text="未核实"
                              @change="(val: any) => handleRepayVerifiedChange(row, Boolean(val))"
                            />
                          </div>
                        </div>
                        <span class="detail-pane-sub">左侧展示还款</span>
                      </div>
                      <div class="detail-pane-total">
                        <span class="detail-pane-total-label">总额</span>
                        <span class="detail-pane-total-value amt-pos">¥{{ formatMoney(detailTypeTotalAmount(row.id, DETAIL_TYPE_VALUE.INCOME)) }}</span>
                      </div>
                    </div>
                    <div v-if="detailTypeDisplayList(row.id, DETAIL_TYPE_VALUE.INCOME).length" class="detail-lite-list">
                      <div class="detail-lite-head">
                        <div class="detail-check-col">
                          <el-checkbox
                            :model-value="isPaneAllSelected(row.id, 'income', DETAIL_TYPE_VALUE.INCOME)"
                            :indeterminate="isPaneSelectionIndeterminate(row.id, 'income', DETAIL_TYPE_VALUE.INCOME)"
                            @change="(checked: any) => togglePaneSelectAll(row.id, 'income', DETAIL_TYPE_VALUE.INCOME, Boolean(checked))"
                          />
                        </div>
                        <div class="detail-index-col">序号</div>
                        <div class="detail-date-col">日期</div>
                        <div class="detail-amount-col">金额</div>
                        <div class="detail-note-col">描述/备注</div>
                        <div class="detail-action-col">操作</div>
                      </div>
                      <div
                        v-for="(detail, index) in detailTypeDisplayList(row.id, DETAIL_TYPE_VALUE.INCOME)"
                        :key="detail.id"
                        class="detail-lite-row"
                      >
                        <div class="detail-check-col">
                          <el-checkbox
                            :model-value="isDetailSelected(row.id, 'income', detail.id)"
                            @change="(checked: any) => toggleDetailChecked(row.id, 'income', detail, Boolean(checked))"
                          />
                        </div>
                        <div class="detail-index-col">{{ index + 1 }}</div>
                        <div class="detail-date-col">{{ detail.detailDate }}</div>
                        <div class="detail-amount-col">
                          <span :class="detail.detailType === DETAIL_TYPE_VALUE.INCOME ? 'amt-pos' : 'amt-neg'" class="font-mono">
                            {{ detail.detailType === DETAIL_TYPE_VALUE.INCOME ? '+' : '-' }}{{ formatMoney(detail.amount) }}
                          </span>
                        </div>
                        <div class="detail-note-col">
                          <div class="detail-note-cell">
                            <span class="detail-note-main">{{ detail.description || '-' }}</span>
                            <span v-if="detail.remark" class="detail-note-sub">{{ detail.remark }}</span>
                          </div>
                        </div>
                        <div class="detail-action-col">
                          <el-button type="primary" link size="small" @click="openEditDetail(row, detail)">编辑</el-button>
                          <el-popconfirm title="确认删除？" @confirm="handleDeleteDetail(row.id, detail.id)">
                            <template #reference><el-button type="danger" link size="small">删</el-button></template>
                          </el-popconfirm>
                        </div>
                      </div>
                    </div>
                    <div v-else class="detail-empty">暂无还款明细</div>
                  </div>

                  <div class="detail-pane">
                    <div class="detail-pane-head">
                      <div class="detail-pane-head-main">
                        <div class="detail-pane-title">
                          <span>消费</span>
                          <el-tag type="danger" size="small" effect="light">{{ detailTypeTotalCount(row.id, DETAIL_TYPE_VALUE.EXPENSE) }}</el-tag>
                          <div class="detail-verify-switch" @click.stop>
                            <span>消费核实</span>
                            <el-switch
                              :model-value="Boolean(row.expenseVerified)"
                              active-text="已核实"
                              inactive-text="未核实"
                              @change="(val: any) => handleExpenseVerifiedChange(row, Boolean(val))"
                            />
                          </div>
                        </div>
                        <span class="detail-pane-sub">右侧展示消费</span>
                      </div>
                      <div class="detail-pane-total">
                        <span class="detail-pane-total-label">总额</span>
                        <span class="detail-pane-total-value amt-neg">¥{{ formatMoney(detailTypeTotalAmount(row.id, DETAIL_TYPE_VALUE.EXPENSE)) }}</span>
                      </div>
                    </div>
                    <div v-if="detailTypeDisplayList(row.id, DETAIL_TYPE_VALUE.EXPENSE).length" class="detail-lite-list">
                      <div class="detail-lite-head">
                        <div class="detail-check-col">
                          <el-checkbox
                            :model-value="isPaneAllSelected(row.id, 'expense', DETAIL_TYPE_VALUE.EXPENSE)"
                            :indeterminate="isPaneSelectionIndeterminate(row.id, 'expense', DETAIL_TYPE_VALUE.EXPENSE)"
                            @change="(checked: any) => togglePaneSelectAll(row.id, 'expense', DETAIL_TYPE_VALUE.EXPENSE, Boolean(checked))"
                          />
                        </div>
                        <div class="detail-index-col">序号</div>
                        <div class="detail-date-col">日期</div>
                        <div class="detail-amount-col">金额</div>
                        <div class="detail-note-col">描述/备注</div>
                        <div class="detail-action-col">操作</div>
                      </div>
                      <div
                        v-for="(detail, index) in detailTypeDisplayList(row.id, DETAIL_TYPE_VALUE.EXPENSE)"
                        :key="detail.id"
                        class="detail-lite-row"
                      >
                        <div class="detail-check-col">
                          <el-checkbox
                            :model-value="isDetailSelected(row.id, 'expense', detail.id)"
                            @change="(checked: any) => toggleDetailChecked(row.id, 'expense', detail, Boolean(checked))"
                          />
                        </div>
                        <div class="detail-index-col">{{ index + 1 }}</div>
                        <div class="detail-date-col">{{ detail.detailDate }}</div>
                        <div class="detail-amount-col">
                          <span :class="detail.detailType === DETAIL_TYPE_VALUE.INCOME ? 'amt-pos' : 'amt-neg'" class="font-mono">
                            {{ detail.detailType === DETAIL_TYPE_VALUE.INCOME ? '+' : '-' }}{{ formatMoney(detail.amount) }}
                          </span>
                        </div>
                        <div class="detail-note-col">
                          <div class="detail-note-cell">
                            <span class="detail-note-main">{{ detail.description || '-' }}</span>
                            <span v-if="detail.remark" class="detail-note-sub">{{ detail.remark }}</span>
                          </div>
                        </div>
                        <div class="detail-action-col">
                          <el-button type="primary" link size="small" @click="openEditDetail(row, detail)">编辑</el-button>
                          <el-popconfirm title="确认删除？" @confirm="handleDeleteDetail(row.id, detail.id)">
                            <template #reference><el-button type="danger" link size="small">删</el-button></template>
                          </el-popconfirm>
                        </div>
                      </div>
                    </div>
                    <div v-else class="detail-empty">暂无消费明细</div>
                  </div>
                </div>
              </template>

              <el-empty v-else description="暂无明细" :image-size="50" />
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="持卡人" width="96" align="center" header-align="center">
        <template #default="{ row }">
          <div class="owner-cell">
            <el-button
              type="primary"
              link
              size="small"
              class="row-action-btn detail-toggle-btn owner-expand-btn"
              :loading="pendingExpandBillId === row.id"
              :class="{ expanded: currentExpandedRow?.id === row.id }"
              :title="currentExpandedRow?.id === row.id ? '收起明细' : '展开明细'"
              @click.stop="toggleBillDetail(row)"
            >
              <el-icon :size="14"><ArrowRight /></el-icon>
            </el-button>
            <span class="owner-avatar">
              <el-icon :size="12"><UserFilled /></el-icon>
            </span>
            <span class="owner-name">{{ row.ownerName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="bankName" label="银行/尾号" width="150" align="center" header-align="center">
        <template #default="{ row }">
          <div class="bank-inline-cell" :title="bankCardText(row)">
            <el-icon :size="13" color="#67c23a"><CreditCard /></el-icon>
            <span class="bank-inline-name">{{ displayBankName(row.bankName) }}</span>
            <span class="bank-inline-last4">尾号{{ row.cardNoLast4 || '-' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="billMonth" label="年份" width="64" align="center">
        <template #default="{ row }">
          <span class="year-cell">{{ billYearLabel(row.billMonth) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="账单日期" width="88" align="center" header-align="center">
        <template #default="{ row }">
          <span class="date-cell">{{ billDateLabel(row.billMonth, row.billDay) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="还款日期" width="88" align="center" header-align="center">
        <template #default="{ row }">
          <span class="date-cell">{{ repayDateLabel(row.repayDate) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="账单金额" width="88" align="center" header-align="center">
        <template #default="{ row }">
          <div class="amount-cell">
            <span class="amount-value">{{ formatMoney(row.billAmount) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="实际还款" width="88" align="center" header-align="center">
        <template #default="{ row }">
          <span class="amount-value amt-pos">{{ formatMoney(row.actualPayAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="消费总额" width="88" align="center" header-align="center">
        <template #default="{ row }">
          <span class="amount-value amt-neg">{{ formatMoney(row.consumeAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="差额" width="86" align="center" header-align="center">
        <template #default="{ row }">
          <span class="amount-value" :class="billBalanceDiff(row) >= 0 ? 'amt-pos' : 'amt-neg'">
            {{ billBalanceDiff(row) < 0 ? '-' : '' }}{{ formatMoney(Math.abs(billBalanceDiff(row))) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="还款核实" width="78" align="center" header-align="center">
        <template #default="{ row }">
          <span class="bill-verify-cell">
            <span class="bill-verify-badge" :class="billVerifyClass(row.verified)">
              {{ billVerifyText(row.verified) }}
            </span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="消费核实" width="78" align="center" header-align="center">
        <template #default="{ row }">
          <span class="bill-verify-cell">
            <span class="bill-verify-badge" :class="billVerifyClass(row.expenseVerified)">
              {{ billVerifyText(row.expenseVerified) }}
            </span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="78" align="center" header-align="center">
        <template #default="{ row }">
          <span class="bill-status-cell">
            <span class="bill-status-badge" :class="billStatusClass(row.status)">
              {{ billStatusText(row.status) }}
            </span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="备注" width="82" align="center" header-align="center" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="bill-remark-cell">{{ row.remark || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="92" align="center">
        <template #default="{ row }">
          <div class="row-action-buttons">
            <el-button
              type="primary"
              link
              size="small"
              class="row-action-btn detail-toggle-btn"
              :loading="pendingExpandBillId === row.id"
              :class="{ expanded: currentExpandedRow?.id === row.id }"
              :title="currentExpandedRow?.id === row.id ? '收起明细' : '展开明细'"
              @click.stop="toggleBillDetail(row)"
            >
              <el-icon :size="14"><ArrowRight /></el-icon>
            </el-button>
            <el-button type="primary" link size="small" class="row-action-btn" title="编辑账单" @click="openBillEdit(row)">
              <el-icon :size="14"><Edit /></el-icon>
            </el-button>
            <el-popconfirm title="确认删除该月账单？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small" class="row-action-btn" title="删除">
                  <el-icon :size="14"><Delete /></el-icon>
                </el-button>
              </template>
            </el-popconfirm>
          </div>
        </template>
      </el-table-column>
        </PageTable>

        <div v-if="showBillPagination" class="bill-pagination">
          <div class="pagination-meta">
            <span>一共 {{ total }} 条</span>
            <span>第 {{ query.pageNum }} / {{ totalPages }} 页</span>
            <span>一页 {{ safeBillPageSize }} 条</span>
          </div>
          <el-pagination
            background
            small
            :current-page="query.pageNum"
            :page-size="safeBillPageSize"
            :page-sizes="billPageSizeOptions"
            :total="total"
            :disabled="total <= 0"
            layout="sizes, prev, pager, next, jumper"
            @current-change="handleCurrentChange"
            @size-change="handleSizeChange"
          />
        </div>
      </section>
    </div>

    <el-dialog v-model="createBillDialogVisible" title="新增账单" width="520px" destroy-on-close>
      <el-form ref="createBillFormRef" :model="createBillForm" :rules="createBillRules" label-width="96px">
        <el-form-item label="新增方式" prop="mode">
          <el-radio-group v-model="createBillForm.mode" @change="handleCreateModeChange">
            <el-radio-button value="month">新增单月</el-radio-button>
            <el-radio-button value="year">生成整年</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="银行卡" prop="cardId">
          <el-select
            v-model="createBillForm.cardId"
            placeholder="请选择信用卡"
            filterable
            clearable
            :loading="cardOptionsLoading"
            style="width: 100%"
            @change="handleCreateCardChange"
          >
            <el-option
              v-for="card in creditCardOptions"
              :key="card.id"
              :label="createBillCardLabel(card)"
              :value="card.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="createBillForm.mode === 'month'" label="账单月份" prop="billMonth">
          <el-date-picker
            v-model="createBillForm.billMonth"
            type="month"
            value-format="YYYY-MM"
            placeholder="请选择账单月份"
            style="width: 100%"
            :editable="false"
          />
        </el-form-item>
        <el-form-item v-else label="账单年份" prop="year">
          <el-date-picker
            v-model="createBillForm.year"
            type="year"
            value-format="YYYY"
            placeholder="请选择账单年份"
            style="width: 100%"
            :editable="false"
          />
        </el-form-item>
        <el-form-item label="账单日" prop="billDay">
          <el-input-number v-model="createBillForm.billDay" :min="1" :max="31" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="还款日" prop="repayDay">
          <el-input-number v-model="createBillForm.repayDay" :min="1" :max="31" controls-position="right" style="width: 100%" />
        </el-form-item>
        <template v-if="createBillForm.mode === 'month'">
          <el-form-item label="账单金额" prop="billAmount">
            <el-input-number v-model="createBillForm.billAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="createBillForm.remark" type="textarea" rows="2" maxlength="500" show-word-limit />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="createBillDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createBillSaving" @click="handleCreateBill">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="billEditDialogVisible" title="编辑账单" width="480px" destroy-on-close>
      <template v-if="billEditRow">
        <el-form label-width="92px">
          <el-form-item label="本月代还金额">
            <el-input-number
              :model-value="editFormMap[billEditRowId]?.billAmount"
              :min="0"
              :precision="2"
              controls-position="right"
              style="width: 100%"
              @update:model-value="(val:any) => updateEditField(billEditRowId, 'billAmount', val)"
            />
          </el-form-item>
          <el-form-item label="账单日">
            <el-input-number
              :model-value="editFormMap[billEditRowId]?.billDay"
              :min="1"
              :max="31"
              controls-position="right"
              style="width: 100%"
              @update:model-value="(val:any) => updateEditField(billEditRowId, 'billDay', val)"
            />
          </el-form-item>
          <el-form-item label="还款日">
            <el-input-number
              :model-value="editFormMap[billEditRowId]?.repayDay"
              :min="1"
              :max="31"
              controls-position="right"
              style="width: 100%"
              @update:model-value="(val:any) => updateEditField(billEditRowId, 'repayDay', val)"
            />
          </el-form-item>
          <el-form-item label="账单状态">
            <el-select
              :model-value="editFormMap[billEditRowId]?.status"
              style="width: 100%"
              @update:model-value="(val:any) => updateEditField(billEditRowId, 'status', val)"
            >
              <el-option v-for="item in BILL_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              :model-value="editFormMap[billEditRowId]?.remark"
              type="textarea"
              rows="2"
              maxlength="500"
              show-word-limit
              @update:model-value="(val:any) => updateEditField(billEditRowId, 'remark', val)"
            />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="billEditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingId === billEditRow?.id" @click="handleSaveBillEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" :title="detailDialogTitle" width="500px" destroy-on-close>
      <el-form :model="detailForm" label-width="90px" :rules="detailRules" ref="detailFormRef">
        <el-form-item label="日期" prop="detailDate">
          <el-date-picker v-model="detailForm.detailDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="detailForm.description" placeholder="如：还款入账" />
        </el-form-item>
        <el-form-item label="交易类型" prop="detailType">
          <el-radio-group v-model="detailForm.detailType">
            <el-radio v-for="t in DETAIL_TYPE_OPTIONS" :key="t.value" :value="t.value">{{ t.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="detailForm.amount" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="detailForm.remark" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="detailDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="detailSaving" @click="handleSaveDetail">保存</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Bills' })
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, ArrowRight, UserFilled, CreditCard, Delete, RefreshRight, Edit, Plus, Back } from '@element-plus/icons-vue'
import PageTable from '@/components/PageTable/index.vue'
import ExportButton from '@/components/ExportButton/index.vue'
import BillDetailSkeleton from '@/components/BillDetailSkeleton.vue'
import { usePageTable } from '@/composables/usePageTable'
import { useExport } from '@/composables/useExport'
import { handleError } from '@/utils/errorHandler'
import {
  getBillPageApi,
  getBillOverviewApi,
  saveBillApi,
  updateBillApi,
  updateBillVerificationApi,
  deleteBillApi,
  batchDeleteBillsApi,
  exportBillApi,
  generateAnnualBillsApi,
  syncBillScheduleApi,
  getDetailListApi as fetchDetailListApi,
  saveDetailApi,
  updateDetailApi,
  deleteDetailApi,
  batchDeleteDetailsApi,
  batchUpdateTypeApi
} from '@/api/bill'
import { getCardListApi } from '@/api/card'
import { useAuthStore } from '@/store/modules/auth'
import { formatMoney, formatRate, toNumber } from '@/utils/formatters'
import {
  BILL_STATUS_OPTIONS,
  BILL_STATUS_TAG_TYPE,
  DETAIL_TYPE_VALUE,
  DETAIL_TYPE_OPTIONS
} from '@/constants/dict'

interface BillRow {
  id: number
  __placeholder?: boolean
  cardId: number
  ownerId?: number
  ownerName: string
  bankName: string
  cardNoLast4: string
  billMonth: string
  billDay: number | null
  repayDate: string | null
  repayDay?: number | null
  billAmount: number | null
  actualPayAmount?: number | null
  consumeAmount?: number | null
  actualPayDate?: string | null
  feeRate: number | null
  feeAmount: number | null
  feePaid: boolean
  posCostAmount: number | null
  otherFeeAmount: number | null
  netProfit: number | null
  status: number
  verified?: boolean | null
  expenseVerified?: boolean | null
  remark?: string
}

interface EditFormItem {
  id: number
  billAmount: number
  billDay: number | null
  posCostAmount: number
  repayDay: number | null
  verified: boolean
  status: number
  feeAmount: string
  netProfit: string
  remark: string
}

interface BillOverview {
  year?: number
  billCount: number
  pendingCount: number
  repaidCount: number
  partialCount: number
  overdueCount: number
  totalBillAmount: number
  totalFeeAmount: number
  totalPosCostAmount: number
  totalOtherFeeAmount: number
  totalNetProfit: number
}

interface BillDetailRow {
  id: number
  billId: number
  detailDate: string
  description: string
  amount: number | string
  detailType: number
  remark?: string
}

interface BillDetailBucket {
  incomeRows: BillDetailRow[]
  expenseRows: BillDetailRow[]
}

interface BankCardOption {
  id: number
  userId: number
  userName?: string
  bankName?: string
  cardNoLast4?: string
  cardType?: number
  status?: number
  billDay?: number | null
  repayDay?: number | null
  effectiveFeeRate?: number | string | null
}

type CreateBillMode = 'month' | 'year'
type BillSortMode = 'currentFirst' | 'monthAsc'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const now = new Date()
const currentYear = now.getFullYear()
const currentMonthNumber = now.getMonth() + 1
const currentMonth = `${currentYear}-${String(currentMonthNumber).padStart(2, '0')}`
const BILL_SORT_CURRENT_FIRST: BillSortMode = 'currentFirst'
const BILL_SORT_MONTH_ASC: BillSortMode = 'monthAsc'
const SINGLE_CARD_ANNUAL_PAGE_SIZE = 12
const DEFAULT_BILL_PAGE_SIZE = 18
const DETAIL_PREFETCH_DELAY = 80
const DETAIL_PREFETCH_CONCURRENCY = 2
const DETAIL_PREFETCH_ANNUAL_LIMIT = 20
const BILL_LIST_STATUS_MAP: Record<number, string> = {
  0: '待还款',
  1: '已还清',
  2: '部分还款',
  3: '逾期'
}
const VERIFY_STATUS_MAP: Record<number, string> = {
  0: '未核实',
  1: '已核实'
}
const VERIFY_STATUS_TAG_TYPE: Record<number, string> = {
  0: 'warning',
  1: 'success'
}

function createDebouncedTask(fn: () => void, delay = 300) {
  let timer = 0
  const run = () => {
    if (timer) window.clearTimeout(timer)
    timer = window.setTimeout(() => {
      timer = 0
      fn()
    }, delay)
  }
  run.cancel = () => {
    if (timer) {
      window.clearTimeout(timer)
      timer = 0
    }
  }
  return run
}

const billMonthFilter = ref<string | null>('')

const {
  loading,
  list,
  total,
  query,
  loadData,
  handleSearch,
  resetQuery,
  handleCurrentChange: handleTableCurrentChange,
  handleSizeChange: handleTableSizeChange,
  refresh,
  refreshFirstPage
} = usePageTable({
  fetchApi: getBillPageApi,
  defaultQuery: {
    pageSize: DEFAULT_BILL_PAGE_SIZE,
    cardId: undefined as any,
    cardIds: '',
    ownerId: undefined as any,
    ownerName: '',
    cardName: '',
    startBillMonth: '',
    endBillMonth: '',
    repayMonth: '',
    repayYear: undefined as any,
    sortMode: BILL_SORT_CURRENT_FIRST as BillSortMode,
    status: undefined as any
  },
  autoSearch: false,
  beforeFetch: (params) => {
    params.pageSize = resolveBillPageSize(params)
    params.sortMode = resolveBillSortMode(params)
    const status = normalizeBillStatusFilter(params.status)
    if (status === undefined) {
      delete params.status
    } else {
      params.status = status
    }
    ;(params as any).current = params.pageNum
    ;(params as any).size = params.pageSize
    delete (params as any).pageNum
    delete (params as any).pageSize
  },
  afterFetch: () => {
    appliedStatusFilter.value = normalizeBillStatusFilter(query.status)
    fetchBillOverview()
  }
})

const billTableRef = ref<any>(null)
const billOverview = ref<BillOverview>({
  year: undefined,
  billCount: 0,
  pendingCount: 0,
  repaidCount: 0,
  partialCount: 0,
  overdueCount: 0,
  totalBillAmount: 0,
  totalFeeAmount: 0,
  totalPosCostAmount: 0,
  totalOtherFeeAmount: 0,
  totalNetProfit: 0
})

const createBillDialogVisible = ref(false)
const createBillSaving = ref(false)
const createBillFormRef = ref<any>()
const cardOptions = ref<BankCardOption[]>([])
const cardOptionsLoading = ref(false)
let cardOptionsLoaded = false
const createBillForm = reactive({
  mode: 'month' as CreateBillMode,
  cardId: undefined as number | undefined,
  billMonth: currentMonth,
  year: String(currentYear + 1),
  billDay: null as number | null,
  repayDay: null as number | null,
  billAmount: 0,
  remark: ''
})

const billScopeLabel = computed(() => formatRepayScopeLabel(query.repayMonth, query.repayYear) || formatBillRangeLabel(query.startBillMonth, query.endBillMonth))
const singleCardAnnualMode = computed(() => isSingleCardAnnualScope(query))
const safeBillPageSize = computed(() => Number(query.pageSize || 0) || DEFAULT_BILL_PAGE_SIZE)
const isBillTableScrollable = computed(() => true)
const billTableHeight = computed(() => '100%')
const showBillPagination = computed(() => true)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / safeBillPageSize.value)))
const isAdmin = computed(() => authStore.role === 'ADMIN')
const billPageSizeOptions = computed(() => {
  return Array.from(new Set([SINGLE_CARD_ANNUAL_PAGE_SIZE, 15, DEFAULT_BILL_PAGE_SIZE, 20, 30, safeBillPageSize.value].filter((size) => Number(size) > 0)))
    .sort((a, b) => Number(a) - Number(b))
})
const creditCardOptions = computed(() => {
  return cardOptions.value.filter(card => Number(card.cardType) === 2 && Number(card.status ?? 0) === 0)
})
const createBillRules = computed(() => ({
  mode: [{ required: true, message: '请选择新增方式', trigger: 'change' }],
  cardId: [{ required: true, message: '请选择信用卡', trigger: 'change' }],
  billDay: [{ required: true, message: '请输入账单日', trigger: 'change' }],
  repayDay: [{ required: true, message: '请输入还款日', trigger: 'change' }],
  ...(createBillForm.mode === 'month'
    ? {
        billMonth: [{ required: true, message: '请选择账单月份', trigger: 'change' }],
        billAmount: [{ required: true, message: '请输入账单金额', trigger: 'change' }]
      }
    : {
        year: [{ required: true, message: '请选择账单年份', trigger: 'change' }]
      })
}))
const triggerBillSearch = createDebouncedTask(() => {
  submitBillSearch()
}, 300)
const appliedStatusFilter = ref<number | undefined>(undefined)
let syncingBillFilters = false
let skipRouteDrivenSearch = false

const detailModeMessage = computed(() => {
  const scopeText = billScopeLabel.value
  if (route.query.cardId) {
    return scopeText ? `当前为单张银行卡账单模式，已定位到 ${scopeText}。` : '当前为单张银行卡账单模式。'
  }
  if (route.query.cardIds) {
    return scopeText ? `当前为当前持卡人全部银行卡账单模式，已定位到 ${scopeText}。` : '当前为当前持卡人全部银行卡账单模式。'
  }
  if (route.query.ownerId) {
    return scopeText ? `当前为持卡人/子用户账单模式，已定位到 ${scopeText}。` : '当前为持卡人/子用户账单模式。'
  }
  return ''
})

const quickMenus = computed(() => [
  { key: 'all', label: '全部', value: undefined, count: billOverview.value.billCount, color: '#0958d9' },
  { key: 'pending', label: '待还款', value: 0, count: billOverview.value.pendingCount, color: '#d97706' },
  { key: 'partial', label: '部分还款', value: 2, count: billOverview.value.partialCount, color: '#7c8799' },
  { key: 'paid', label: '已还清', value: 1, count: billOverview.value.repaidCount, color: '#2f9e44' },
  { key: 'overdue', label: '逾期', value: 3, count: billOverview.value.overdueCount, color: '#cf1322' }
])

const activeQuickMenu = computed(() => {
  const matched = quickMenus.value.find(item => item.value === query.status)
  return matched?.key || 'all'
})

function parseBillMonthParts(billMonth: string | null | undefined) {
  const match = String(billMonth || '').trim().match(/^(\d{4})-(\d{1,2})$/)
  if (!match) return null
  const year = Number(match[1])
  const month = Number(match[2])
  if (!Number.isFinite(year) || !Number.isFinite(month) || month < 1 || month > 12) {
    return null
  }
  return { year, month }
}

function formatBillRangeLabel(startBillMonth?: string | null, endBillMonth?: string | null) {
  const start = String(startBillMonth || '').trim()
  const end = String(endBillMonth || '').trim()
  if (!start && !end) return ''
  if (isWholeYearRange(start, end)) {
    const startYear = start.slice(0, 4)
    const endYear = end.slice(0, 4)
    return startYear === endYear ? `${startYear}年` : `${startYear}年 至 ${endYear}年`
  }
  if (start && end) {
    return start === end ? start : `${start} 至 ${end}`
  }
  return start || end
}

function formatRepayScopeLabel(repayMonth?: string | null, repayYear?: number | string | null) {
  const month = String(repayMonth || '').trim()
  if (/^\d{4}-\d{2}$/.test(month)) {
    const [year, monthPart] = month.split('-')
    return `${year}年${monthPart}月还款`
  }
  const year = Number(repayYear)
  if (Number.isFinite(year) && year >= 2000) {
    return `${year}年还款`
  }
  return ''
}

function isWholeYearRange(startBillMonth?: string | null, endBillMonth?: string | null) {
  const start = parseBillMonthParts(startBillMonth)
  const end = parseBillMonthParts(endBillMonth)
  return !!start && !!end && start.year === end.year && start.month === 1 && end.month === 12
}

function isAnnualBillScope(params: any) {
  return isWholeYearRange(params?.startBillMonth, params?.endBillMonth) || (!!params?.repayYear && !params?.repayMonth)
}

function hasScopedCardIds(params: any) {
  return String(params?.cardIds || '').trim().length > 0
}

function isSingleCardAnnualScope(params: any) {
  return isAnnualBillScope(params) && Number(params?.cardId || 0) > 0 && !Number(params?.ownerId || 0) && !hasScopedCardIds(params)
}

function resolveBillSortMode(params: any): BillSortMode {
  const rawMode = String(params?.sortMode || '').trim()
  if (rawMode === BILL_SORT_MONTH_ASC || rawMode === BILL_SORT_CURRENT_FIRST) {
    return rawMode as BillSortMode
  }
  return isSingleCardAnnualScope(params) ? BILL_SORT_MONTH_ASC : BILL_SORT_CURRENT_FIRST
}

function isMonthAscBillSort(params: any) {
  return resolveBillSortMode(params) === BILL_SORT_MONTH_ASC
}

function defaultPageSizeForScope(params: any) {
  return isSingleCardAnnualScope(params) ? SINGLE_CARD_ANNUAL_PAGE_SIZE : DEFAULT_BILL_PAGE_SIZE
}

function resolveBillPageSize(params: any) {
  if (isSingleCardAnnualScope(params)) {
    return SINGLE_CARD_ANNUAL_PAGE_SIZE
  }
  return Number(params?.pageSize || 0) || defaultPageSizeForScope(params)
}

function syncBillMonthQuery() {
  const billMonth = String(billMonthFilter.value || '').trim()
  query.repayMonth = billMonth
  query.startBillMonth = ''
  query.endBillMonth = ''
  query.repayYear = undefined as any
}

function applyBillMonthRange(startBillMonth?: string | null, endBillMonth?: string | null) {
  const start = String(startBillMonth || '').trim()
  const end = String(endBillMonth || '').trim()
  billMonthFilter.value = start && end && start === end ? start : ''
  query.startBillMonth = start
  query.endBillMonth = end
}

function submitBillSearch() {
  triggerBillSearch.cancel()
  syncBillMonthQuery()
  handleSearch()
}

function handleStatusFilterChange() {
  appliedStatusFilter.value = normalizeBillStatusFilter(query.status)
  submitBillSearch()
}

function resolveMonthOrder(month: number) {
  if (isMonthAscBillSort(query)) {
    return month
  }
  return (month - currentMonthNumber + 12) % 12
}

function resolveRepayMonthParts(row: BillRow) {
  const repayMonth = repayMonthOf(row)
  return parseBillMonthParts(repayMonth) || parseBillMonthParts(row.billMonth)
}

function resolveSortMonthParts(row: BillRow) {
  if (isMonthAscBillSort(query) && !query.repayMonth && !query.repayYear) {
    return parseBillMonthParts(row.billMonth) || resolveRepayMonthParts(row)
  }
  return resolveRepayMonthParts(row)
}

const sortedList = computed<BillRow[]>(() => {
  return [...(list.value as BillRow[])].sort((a, b) => {
    const aParts = resolveSortMonthParts(a)
    const bParts = resolveSortMonthParts(b)
    const monthOrderDelta = resolveMonthOrder(Number(aParts?.month || 0)) - resolveMonthOrder(Number(bParts?.month || 0))

    if (!isMonthAscBillSort(query) && monthOrderDelta !== 0) {
      return monthOrderDelta
    }

    if (aParts?.year !== bParts?.year) {
      return Number(bParts?.year || 0) - Number(aParts?.year || 0)
    }

    if (monthOrderDelta !== 0) {
      return monthOrderDelta
    }

    return Number(b.id || 0) - Number(a.id || 0)
  })
})

const tableDisplayList = computed<BillRow[]>(() => {
  return padBillRows(filteredBillRows.value)
})

const filteredBillRows = computed<BillRow[]>(() => {
  const status = appliedStatusFilter.value
  if (status === undefined) {
    return sortedList.value
  }
  return sortedList.value.filter(row => Number(row.status) === status)
})

function padBillRows(rows: BillRow[]) {
  const minRows = Math.max(1, Number(safeBillPageSize.value || DEFAULT_BILL_PAGE_SIZE))
  if (rows.length >= minRows) {
    return rows
  }
  return [
    ...rows,
    ...Array.from({ length: minRows - rows.length }, (_, index) => createBillPlaceholderRow(index))
  ]
}

function createBillPlaceholderRow(index: number): BillRow {
  return {
    id: -100000 - index,
    __placeholder: true,
    cardId: 0,
    ownerName: '',
    bankName: '',
    cardNoLast4: '',
    billMonth: '',
    billDay: null,
    repayDate: null,
    billAmount: null,
    actualPayAmount: null,
    consumeAmount: null,
    feeRate: null,
    feeAmount: null,
    feePaid: false,
    posCostAmount: null,
    otherFeeAmount: null,
    netProfit: null,
    status: 0,
    verified: false,
    expenseVerified: false,
    remark: ''
  }
}

function isBillPlaceholderRow(row: BillRow | null | undefined) {
  return Boolean(row?.__placeholder)
}

function isSelectableBillRow(row: BillRow) {
  return !isBillPlaceholderRow(row)
}

function billRowClassName({ row }: { row: BillRow }) {
  if (isBillPlaceholderRow(row)) {
    return 'bill-placeholder-row'
  }
  return repayMonthOf(row) === currentMonth ? 'current-month-row' : ''
}

function billStatusText(status: unknown) {
  return BILL_LIST_STATUS_MAP[Number(status)] || '-'
}

function billStatusClass(status: unknown) {
  const value = Number(status)
  return [0, 1, 2, 3].includes(value) ? `is-status-${value}` : 'is-status-unknown'
}

function billVerifyText(value: unknown) {
  return Boolean(value) ? '已核实' : '未核实'
}

function billVerifyClass(value: unknown) {
  return Boolean(value) ? 'is-verified' : 'is-unverified'
}

function repayMonthOf(row: BillRow | null | undefined) {
  const match = String(row?.repayDate || '').match(/^(\d{4})-(\d{2})-/)
  return match ? `${match[1]}-${match[2]}` : ''
}

function resetBillTableTransientState() {
  cancelDetailPrefetch()
  expandRequestToken += 1
  pendingExpandBillId.value = null
  currentExpandedRow.value = null
  clearBillSelection()
  nextTick(scheduleBillTableLayout)
}

function handleCurrentChange(pageNum: number) {
  resetBillTableTransientState()
  handleTableCurrentChange(pageNum)
}

function handleSizeChange(pageSize: number) {
  resetBillTableTransientState()
  handleTableSizeChange(pageSize)
}

function applyQuickMenu(status?: number) {
  const normalizedStatus = normalizeBillStatusFilter(status)
  query.status = normalizedStatus === undefined ? undefined as any : normalizedStatus as any
  appliedStatusFilter.value = normalizedStatus
  submitBillSearch()
}

const { exporting, handleExport: doExport } = useExport({
  exportApi: exportBillApi,
  fileName: '账单记录'
})

function handleExport() {
  return doExport(buildExportParams())
}

function buildExportParams() {
  const status = normalizeBillStatusFilter(query.status)
  return {
    cardId: query.cardId,
    cardIds: query.cardIds,
    ownerId: query.ownerId,
    ownerName: query.ownerName,
    cardName: query.cardName,
    startBillMonth: query.startBillMonth,
    endBillMonth: query.endBillMonth,
    repayMonth: query.repayMonth,
    repayYear: query.repayYear,
    sortMode: resolveBillSortMode(query),
    status
  }
}

function buildOverviewParams() {
  const params = buildExportParams()
  delete (params as any).status
  delete (params as any).sortMode
  return params
}

async function fetchBillOverview() {
  try {
    const res: any = await getBillOverviewApi(buildOverviewParams())
    billOverview.value = {
      year: res.data?.year != null ? Number(res.data.year) : undefined,
      billCount: Number(res.data?.billCount ?? 0),
      pendingCount: Number(res.data?.pendingCount ?? 0),
      repaidCount: Number(res.data?.repaidCount ?? 0),
      partialCount: Number(res.data?.partialCount ?? 0),
      overdueCount: Number(res.data?.overdueCount ?? 0),
      totalBillAmount: Number(res.data?.totalBillAmount ?? 0),
      totalFeeAmount: Number(res.data?.totalFeeAmount ?? 0),
      totalPosCostAmount: Number(res.data?.totalPosCostAmount ?? 0),
      totalOtherFeeAmount: Number(res.data?.totalOtherFeeAmount ?? 0),
      totalNetProfit: Number(res.data?.totalNetProfit ?? 0)
    }
  } catch (error) {
    console.error('加载账单概览失败:', error)
  }
}

function toRouteNumber(value: unknown) {
  const target = Array.isArray(value) ? value[0] : value
  const num = Number(target)
  return Number.isFinite(num) && num > 0 ? num : undefined
}

function toRouteBillStatus(value: unknown) {
  const target = Array.isArray(value) ? value[0] : value
  return normalizeBillStatusFilter(target)
}

function normalizeBillStatusFilter(value: unknown) {
  if (value === '' || value === null || value === undefined) {
    return undefined
  }
  const num = Number(value)
  return [0, 1, 2, 3].includes(num) ? num : undefined
}

function toRouteYearValue(value: unknown) {
  const target = Array.isArray(value) ? value[0] : value
  const num = Number(target)
  return Number.isFinite(num) && num >= 2000 ? num : undefined
}

function toRouteBillMonthValue(value: unknown) {
  const target = Array.isArray(value) ? value[0] : value
  return typeof target === 'string' && /^\d{4}-\d{2}$/.test(target) ? target : ''
}

function toRouteCardIdsValue(value: unknown) {
  const target = Array.isArray(value) ? value[0] : value
  if (typeof target !== 'string') return ''
  return Array.from(new Set(
    target
      .split(',')
      .map(item => Number(item.trim()))
      .filter(id => Number.isFinite(id) && id > 0)
      .map(id => String(Math.trunc(id)))
  )).join(',')
}

function toRouteBillSortMode(value: unknown) {
  const target = Array.isArray(value) ? value[0] : value
  return target === BILL_SORT_MONTH_ASC || target === BILL_SORT_CURRENT_FIRST ? target as BillSortMode : undefined
}

function normalizeRouteBillRange(
  startValue: unknown,
  endValue: unknown,
  yearValue: unknown,
  billMonthValue: unknown
) {
  const exactMonth = toRouteBillMonthValue(billMonthValue)
  if (exactMonth) {
    return [exactMonth, exactMonth] as [string, string]
  }

  const startBillMonth = toRouteBillMonthValue(startValue)
  const endBillMonth = toRouteBillMonthValue(endValue)
  if (startBillMonth && endBillMonth) {
    return [startBillMonth, endBillMonth] as [string, string]
  }

  const year = toRouteYearValue(yearValue)
  if (year) {
    return [`${year}-01`, `${year}-12`] as [string, string]
  }

  return ['', ''] as [string, string]
}

function clearRouteFilters() {
  const nextQuery = { ...route.query }
  delete nextQuery.cardId
  delete nextQuery.cardIds
  delete nextQuery.ownerId
  delete nextQuery.status
  delete nextQuery.year
  delete nextQuery.billMonth
  delete nextQuery.startBillMonth
  delete nextQuery.endBillMonth
  delete nextQuery.repayMonth
  delete nextQuery.repayYear
  delete nextQuery.sortMode
  router.replace({ path: route.path, query: nextQuery })
}

async function handleResetAll() {
  triggerBillSearch.cancel()
  syncingBillFilters = true
  const hasRouteFilters = !!(route.query.cardId || route.query.cardIds || route.query.ownerId || route.query.status || route.query.year || route.query.billMonth || route.query.startBillMonth || route.query.endBillMonth || route.query.repayMonth || route.query.repayYear || route.query.sortMode)
  if (hasRouteFilters) {
    skipRouteDrivenSearch = true
    clearRouteFilters()
  }
  billMonthFilter.value = ''
  currentExpandedRow.value = null
  await resetQuery()
  syncingBillFilters = false
}

async function ensureCardOptions(force = false) {
  if (!force && cardOptionsLoaded) return
  cardOptionsLoading.value = true
  try {
    const res: any = await getCardListApi()
    cardOptions.value = (res.data || []).map((card: any) => ({
      id: Number(card.id),
      userId: Number(card.userId),
      userName: card.userName,
      bankName: card.bankName,
      cardNoLast4: card.cardNoLast4,
      cardType: Number(card.cardType),
      status: Number(card.status ?? 0),
      billDay: card.billDay == null ? null : Number(card.billDay),
      repayDay: card.repayDay == null ? null : Number(card.repayDay),
      effectiveFeeRate: card.effectiveFeeRate
    }))
    cardOptionsLoaded = true
  } catch (error) {
    handleError(error, '加载信用卡列表')
  } finally {
    cardOptionsLoading.value = false
  }
}

function createBillCardLabel(card: BankCardOption) {
  const owner = card.userName || '未分配'
  return `${owner} / ${displayBankName(card.bankName)} 尾号${card.cardNoLast4 || '-'}`
}

function findCreateBillCard(cardId: number | string | null | undefined) {
  const targetId = Number(cardId || 0)
  return creditCardOptions.value.find(card => Number(card.id) === targetId) || null
}

function resetCreateBillForm() {
  Object.assign(createBillForm, {
    mode: 'month' as CreateBillMode,
    cardId: undefined,
    billMonth: currentMonth,
    year: String(currentYear + 1),
    billDay: null,
    repayDay: null,
    billAmount: 0,
    remark: ''
  })
  nextTick(() => createBillFormRef.value?.clearValidate?.())
}

function applyCreateCardDefaults(card: BankCardOption | null) {
  createBillForm.billDay = card?.billDay ?? null
  createBillForm.repayDay = card?.repayDay ?? null
}

async function openCreateBillDialog() {
  resetCreateBillForm()
  createBillDialogVisible.value = true
  await ensureCardOptions()

  const routeCard = findCreateBillCard(query.cardId || toRouteNumber(route.query.cardId))
  const defaultCard = routeCard || (creditCardOptions.value.length === 1 ? creditCardOptions.value[0] : null)
  if (defaultCard) {
    createBillForm.cardId = defaultCard.id
    applyCreateCardDefaults(defaultCard)
  }
}

function handleCreateModeChange() {
  nextTick(() => createBillFormRef.value?.clearValidate?.())
}

function handleCreateCardChange(cardId: number) {
  applyCreateCardDefaults(findCreateBillCard(cardId))
}

function normalizeCreateYear() {
  const year = Number(createBillForm.year)
  return Number.isFinite(year) && year >= 2000 && year <= 2100 ? year : 0
}

function buildCreateBillBasePayload(card: BankCardOption) {
  return {
    cardId: card.id,
    billDay: createBillForm.billDay,
    repayDay: createBillForm.repayDay
  }
}

function focusCreatedBills(cardId: number, startBillMonth: string, endBillMonth: string) {
  triggerBillSearch.cancel()
  const nextRouteQuery: Record<string, any> = {
    ...route.query,
    cardId: String(cardId),
    startBillMonth,
    endBillMonth,
    sortMode: BILL_SORT_MONTH_ASC
  }
  delete nextRouteQuery.ownerId
  delete nextRouteQuery.cardIds
  delete nextRouteQuery.status
  delete nextRouteQuery.year
  delete nextRouteQuery.billMonth
  delete nextRouteQuery.repayMonth
  delete nextRouteQuery.repayYear
  void router.replace({ path: route.path, query: nextRouteQuery }).catch(() => {})
  syncingBillFilters = true
  query.cardId = cardId as any
  query.cardIds = ''
  query.ownerId = undefined as any
  query.sortMode = BILL_SORT_MONTH_ASC
  query.ownerName = ''
  query.cardName = ''
  query.status = undefined as any
  applyBillMonthRange(startBillMonth, endBillMonth)
  query.pageSize = defaultPageSizeForScope(query)
  currentExpandedRow.value = null
  syncingBillFilters = false
  refreshFirstPage()
}

async function handleCreateBill() {
  try {
    await createBillFormRef.value?.validate()
  } catch {
    return
  }

  const card = findCreateBillCard(createBillForm.cardId)
  if (!card) {
    ElMessage.warning('请选择可用信用卡')
    return
  }

  if (createBillForm.mode === 'year' && !normalizeCreateYear()) {
    ElMessage.warning('账单年份必须在2000-2100之间')
    return
  }

  createBillSaving.value = true
  try {
    if (createBillForm.mode === 'year') {
      const year = normalizeCreateYear()
      await generateAnnualBillsApi(card.id, {
        ...buildCreateBillBasePayload(card),
        ownerId: card.userId,
        year
      })
      ElMessage.success(`${year}年账单生成成功`)
      createBillDialogVisible.value = false
      focusCreatedBills(card.id, `${year}-01`, `${year}-12`)
      return
    }

    await saveBillApi({
      ...buildCreateBillBasePayload(card),
      billMonth: createBillForm.billMonth,
      billAmount: toNumber(createBillForm.billAmount),
      minPayAmount: 0,
      feePaid: false,
      verified: false,
      expenseVerified: false,
      posCostAmount: 0,
      remark: createBillForm.remark || ''
    })
    ElMessage.success('账单新增成功')
    createBillDialogVisible.value = false
    focusCreatedBills(card.id, createBillForm.billMonth, createBillForm.billMonth)
  } catch (error) {
    handleError(error, '新增账单')
  } finally {
    createBillSaving.value = false
  }
}

const editFormMap = ref<Record<number, EditFormItem>>({})
const savingId = ref<number | null>(null)
const selectedBillRows = ref<BillRow[]>([])

function handleBillSelectionChange(selection: BillRow[]) {
  selectedBillRows.value = (selection || []).filter(row => !isBillPlaceholderRow(row))
}

function clearBillSelection() {
  selectedBillRows.value = []
  billTableRef.value?.clearSelection?.()
}

function ensureEditForm(row: BillRow) {
  if (!editFormMap.value[row.id]) {
    editFormMap.value[row.id] = buildEditForm(row)
  }
  return editFormMap.value[row.id]
}

function buildEditForm(row: BillRow): EditFormItem {
  const billAmount = toNumber(row.billAmount)
  const posCostAmount = toNumber(row.posCostAmount)
  const feeAmount = buildFeeAmount(billAmount, row.feeRate)
  const netProfit = buildNetProfit(billAmount, row.feeRate, posCostAmount, toNumber(row.otherFeeAmount))
  return {
    id: row.id,
    billAmount,
    billDay: row.billDay ?? null,
    posCostAmount,
    repayDay: parseRepayDay(row.repayDate),
    verified: Boolean(row.verified),
    status: row.status,
    feeAmount: feeAmount.toFixed(2),
    netProfit: netProfit.toFixed(2),
    remark: row.remark || ''
  }
}

function syncInlineAmounts(billId: number) {
  const form = editFormMap.value[billId]
  const row = (list.value as BillRow[]).find(item => item.id === billId)
  if (!form || !row) return
  const feeAmount = buildFeeAmount(form.billAmount, row.feeRate)
  form.feeAmount = feeAmount.toFixed(2)
  form.netProfit = buildNetProfit(form.billAmount, row.feeRate, form.posCostAmount, toNumber(row.otherFeeAmount)).toFixed(2)
}

const detailListMap = ref<Record<number, BillDetailRow[]>>({})
const detailBucketMap = ref<Record<number, BillDetailBucket>>({})
const detailLoadingMap = ref<Record<number, boolean>>({})
const detailLoadedMap = ref<Record<number, boolean>>({})
const selectedDetailsMap = ref<Record<number, any[]>>({})
const selectedIncomeDetailsMap = ref<Record<number, BillDetailRow[]>>({})
const selectedExpenseDetailsMap = ref<Record<number, BillDetailRow[]>>({})
const detailRequestMap = new Map<number, Promise<void>>()
let billTableLayoutFrame = 0
let detailPrefetchTimer = 0
let detailPrefetchToken = 0
const emptyDetailBucket: BillDetailBucket = {
  incomeRows: [],
  expenseRows: []
}

function scheduleBillTableLayout() {
  if (billTableLayoutFrame) return
  billTableLayoutFrame = window.requestAnimationFrame(() => {
    billTableLayoutFrame = 0
    billTableRef.value?.doLayout?.()
  })
}

function scrollExpandedContentIntoView() {
  window.requestAnimationFrame(() => {
    const wrap = document.querySelector('.bill-page-table .el-scrollbar__wrap') as HTMLElement | null
    const expandedRow = document.querySelector('.bill-page-table .el-table__expanded-row') as HTMLElement | null
    if (!wrap || !expandedRow) return

    const wrapRect = wrap.getBoundingClientRect()
    const expandedRect = expandedRow.getBoundingClientRect()
    const padding = 12

    if (expandedRect.bottom > wrapRect.bottom) {
      wrap.scrollTop += expandedRect.bottom - wrapRect.bottom + padding
    } else if (expandedRect.top < wrapRect.top) {
      wrap.scrollTop -= wrapRect.top - expandedRect.top + padding
    }
  })
}

function resetDetailSelection(billId: number) {
  selectedIncomeDetailsMap.value[billId] = []
  selectedExpenseDetailsMap.value[billId] = []
  selectedDetailsMap.value[billId] = []
}

function buildDetailBucket(details: BillDetailRow[]): BillDetailBucket {
  const incomeRows: BillDetailRow[] = []
  const expenseRows: BillDetailRow[] = []

  details.forEach(detail => {
    if (Number(detail.detailType) === Number(DETAIL_TYPE_VALUE.INCOME)) {
      incomeRows.push(detail)
      return
    }
    expenseRows.push(detail)
  })

  return {
    incomeRows,
    expenseRows
  }
}

function setBillDetails(billId: number, details: BillDetailRow[], options: { syncBillRow?: boolean } = {}) {
  detailListMap.value[billId] = details
  detailBucketMap.value[billId] = buildDetailBucket(details)
  if (options.syncBillRow) {
    syncBillRowFromDetails(billId, details)
  }
}

function calculateDetailTypeTotal(details: BillDetailRow[], detailType: number) {
  return details
    .filter(item => Number(item.detailType) === Number(detailType))
    .reduce((sum, item) => sum + toNumber(item.amount), 0)
}

function resolveBillStatusFromDetails(row: BillRow, actualPayAmount: number) {
  if (Boolean(row.verified) && Boolean(row.expenseVerified)) {
    return 1
  }

  const billAmount = toNumber(row.billAmount)
  if (actualPayAmount > 0) {
    if (billAmount <= 0 || actualPayAmount >= billAmount) {
      return 1
    }
    if (isBillRepayOverdue(row)) {
      return 3
    }
    return 2
  }

  if (isBillRepayOverdue(row) && billAmount > 0) {
    return 3
  }
  return 0
}

function isBillRepayOverdue(row: BillRow) {
  const repayDate = String(row.repayDate || '').slice(0, 10)
  return /^\d{4}-\d{2}-\d{2}$/.test(repayDate) && repayDate < currentDateString()
}

function syncBillRowFromDetails(billId: number, details: BillDetailRow[]) {
  const row = (list.value as BillRow[]).find(item => Number(item.id) === Number(billId))
  if (!row) return

  const actualPayAmount = Number(calculateDetailTypeTotal(details, DETAIL_TYPE_VALUE.INCOME).toFixed(2))
  const consumeAmount = Number(calculateDetailTypeTotal(details, DETAIL_TYPE_VALUE.EXPENSE).toFixed(2))
  row.actualPayAmount = actualPayAmount
  row.consumeAmount = consumeAmount
  // 只同步金额，不改变账单状态（状态由用户手动管理）
}

function getDetailPrefetchIds() {
  const rows = sortedList.value as BillRow[]
  if (!rows.length) return []

  const maxCount = isAnnualBillScope(query) ? DETAIL_PREFETCH_ANNUAL_LIMIT : rows.length
  return rows
    .map(item => Number(item.id))
    .filter(id => id > 0 && !detailLoadedMap.value[id])
    .slice(0, maxCount)
}

async function runDetailPrefetch(token: number, billIds: number[]) {
  const queue = [...billIds]
  const workerCount = Math.min(DETAIL_PREFETCH_CONCURRENCY, queue.length)
  const workers = Array.from({ length: workerCount }, async () => {
    while (queue.length) {
      if (token !== detailPrefetchToken) return
      const billId = queue.shift()
      if (!billId || detailLoadedMap.value[billId]) continue
      await loadDetails(billId, { silent: true })
    }
  })
  await Promise.all(workers)
}

function cancelDetailPrefetch() {
  detailPrefetchToken += 1
  if (detailPrefetchTimer) {
    window.clearTimeout(detailPrefetchTimer)
    detailPrefetchTimer = 0
  }
}

function scheduleDetailPrefetch() {
  cancelDetailPrefetch()

  const billIds = getDetailPrefetchIds()
  if (!billIds.length) return

  const currentToken = detailPrefetchToken
  detailPrefetchTimer = window.setTimeout(() => {
    detailPrefetchTimer = 0
    void runDetailPrefetch(currentToken, billIds)
  }, DETAIL_PREFETCH_DELAY)
}

async function loadDetails(billId: number, options: { force?: boolean; silent?: boolean; quiet?: boolean; syncBillRow?: boolean } = {}) {
  if (!options.force && detailLoadedMap.value[billId]) {
    resetDetailSelection(billId)
    return
  }
  if (detailLoadingMap.value[billId]) {
    await (detailRequestMap.get(billId) || Promise.resolve())
    return
  }

  if (!options.quiet) {
    detailLoadingMap.value[billId] = true
  }
  const request = (async () => {
    try {
      const res: any = await fetchDetailListApi(billId)
      setBillDetails(billId, (res.data || []) as BillDetailRow[], { syncBillRow: options.syncBillRow })
      detailLoadedMap.value[billId] = true
      resetDetailSelection(billId)
    } catch (error) {
      setBillDetails(billId, [])
      detailLoadedMap.value[billId] = false
      resetDetailSelection(billId)
      if (!options.silent) {
        handleError(error, '加载明细列表')
      }
    } finally {
      if (!options.quiet) {
        detailLoadingMap.value[billId] = false
      }
      detailRequestMap.delete(billId)
      if (currentExpandedRow.value?.id === billId) {
        nextTick(scheduleBillTableLayout)
      }
    }
  })()

  detailRequestMap.set(billId, request)
  await request
}

const currentExpandedRow = ref<BillRow | null>(null)
const expandedRowKeys = computed(() => currentExpandedRow.value?.id ? [currentExpandedRow.value.id] : [])
const pendingExpandBillId = ref<number | null>(null)
let expandRequestToken = 0

function detailTypeRows(billId: number, detailType: number) {
  const bucket = detailBucketMap.value[billId] || emptyDetailBucket
  return Number(detailType) === Number(DETAIL_TYPE_VALUE.INCOME) ? bucket.incomeRows : bucket.expenseRows
}

function detailTypeDisplayList(billId: number, detailType: number) {
  const bucket = detailBucketMap.value[billId] || emptyDetailBucket
  return Number(detailType) === Number(DETAIL_TYPE_VALUE.INCOME) ? bucket.incomeRows : bucket.expenseRows
}

function detailTypeTotalCount(billId: number, detailType: number) {
  return detailTypeRows(billId, detailType).length
}

function detailTypeTotalAmount(billId: number, detailType: number) {
  return detailTypeRows(billId, detailType).reduce((sum, item) => sum + toNumber(item.amount), 0)
}

function billBalanceDiff(row: BillRow) {
  return Number((toNumber(row.actualPayAmount) - toNumber(row.consumeAmount)).toFixed(2))
}

function syncSelectedDetails(billId: number) {
  const incomeList = selectedIncomeDetailsMap.value[billId] || []
  const expenseList = selectedExpenseDetailsMap.value[billId] || []
  const mergedMap = new Map<number, BillDetailRow>()
  ;[...incomeList, ...expenseList].forEach(item => {
    if (item?.id != null) mergedMap.set(Number(item.id), item)
  })
  selectedDetailsMap.value[billId] = [...mergedMap.values()]
}

function getPaneSelection(billId: number, pane: 'income' | 'expense') {
  return pane === 'income'
    ? (selectedIncomeDetailsMap.value[billId] || [])
    : (selectedExpenseDetailsMap.value[billId] || [])
}

function setPaneSelection(billId: number, pane: 'income' | 'expense', selection: BillDetailRow[]) {
  if (pane === 'income') {
    selectedIncomeDetailsMap.value[billId] = selection
  } else {
    selectedExpenseDetailsMap.value[billId] = selection
  }
}

function isDetailSelected(billId: number, pane: 'income' | 'expense', detailId: number) {
  return getPaneSelection(billId, pane).some(item => Number(item.id) === Number(detailId))
}

function isPaneAllSelected(billId: number, pane: 'income' | 'expense', detailType: number) {
  const rows = detailTypeDisplayList(billId, detailType)
  return rows.length > 0 && rows.every(item => isDetailSelected(billId, pane, item.id))
}

function isPaneSelectionIndeterminate(billId: number, pane: 'income' | 'expense', detailType: number) {
  const rows = detailTypeDisplayList(billId, detailType)
  if (!rows.length) return false
  const selectedCount = rows.filter(item => isDetailSelected(billId, pane, item.id)).length
  return selectedCount > 0 && selectedCount < rows.length
}

function toggleDetailChecked(billId: number, pane: 'income' | 'expense', detail: BillDetailRow, checked: boolean) {
  const current = getPaneSelection(billId, pane)
  const next = checked
    ? [...current.filter(item => Number(item.id) !== Number(detail.id)), detail]
    : current.filter(item => Number(item.id) !== Number(detail.id))
  setPaneSelection(billId, pane, next)
  syncSelectedDetails(billId)
}

function togglePaneSelectAll(billId: number, pane: 'income' | 'expense', detailType: number, checked: boolean) {
  const rows = detailTypeDisplayList(billId, detailType)
  const rowIds = new Set(rows.map(item => Number(item.id)))
  const retained = getPaneSelection(billId, pane).filter(item => !rowIds.has(Number(item.id)))
  setPaneSelection(billId, pane, checked ? [...retained, ...rows] : retained)
  syncSelectedDetails(billId)
}

const detailDialogVisible = ref(false)
const detailDialogTitle = ref('新增明细')
const detailSaving = ref(false)
const detailFormRef = ref<any>()
const currentBillRow = ref<BillRow | null>(null)
const billEditDialogVisible = ref(false)
const billEditRow = ref<BillRow | null>(null)
const billEditRowId = computed(() => Number(billEditRow.value?.id || 0))
const detailForm = reactive({
  id: undefined as number | undefined,
  billId: 0,
  detailDate: currentDateString(),
  description: '',
  amount: 0,
  detailType: DETAIL_TYPE_VALUE.INCOME,
  remark: ''
})
const detailRules = {
  detailDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }],
  detailType: [{ required: true, message: '请选择交易类型', trigger: 'change' }]
}

function openAddDetail(row: BillRow) {
  currentBillRow.value = row
  Object.assign(detailForm, { id: undefined, billId: row.id, detailDate: currentDateString(), description: '', amount: 0, detailType: DETAIL_TYPE_VALUE.INCOME, remark: '' })
  detailDialogTitle.value = '新增明细'
  detailDialogVisible.value = true
}

function openEditDetail(billRow: BillRow, detail: any) {
  currentBillRow.value = billRow
  Object.assign(detailForm, detail)
  detailDialogTitle.value = '编辑明细'
  detailDialogVisible.value = true
}

async function handleSaveDetail() {
  try {
    await detailFormRef.value?.validate()
  } catch {
    return
  }
  detailSaving.value = true
  try {
    const payload = { ...detailForm }
    if (payload.id) {
      await updateDetailApi(payload)
    } else {
      await saveDetailApi(payload)
    }
    ElMessage.success('操作成功')
    detailDialogVisible.value = false
    if (currentBillRow.value) {
      await loadDetails(currentBillRow.value.id, { force: true, silent: true, quiet: true, syncBillRow: true })
    }
    refreshBillDataAfterDetailChange()
  } catch (error) {
    handleError(error, '保存明细')
  } finally {
    detailSaving.value = false
  }
}

async function handleDeleteDetail(billId: number, id: number) {
  try {
    await deleteDetailApi(id)
    ElMessage.success('删除成功')
    await loadDetails(billId, { force: true, silent: true, quiet: true, syncBillRow: true })
    refreshBillDataAfterDetailChange()
  } catch (error) {
    handleError(error, '删除明细')
  }
}

async function handleBatchDelete(billId: number) {
  const ids = selectedDetailsMap.value[billId]?.map(d => d.id) || []
  if (ids.length === 0) return

  try {
    await ElMessageBox.confirm(`确认删除选中的 ${ids.length} 条明细？`, '批量删除', {
      type: 'warning'
    })
    await batchDeleteDetailsApi(ids)
    ElMessage.success('批量删除成功')
    selectedDetailsMap.value[billId] = []
    selectedIncomeDetailsMap.value[billId] = []
    selectedExpenseDetailsMap.value[billId] = []
    await loadDetails(billId, { force: true, silent: true, quiet: true, syncBillRow: true })
    refreshBillDataAfterDetailChange()
  } catch (error: any) {
    if (error !== 'cancel') {
      handleError(error, '批量删除明细')
    }
  }
}

async function handleBatchUpdateType(billId: number, detailType: number) {
  const ids = selectedDetailsMap.value[billId]?.map(d => d.id) || []
  if (ids.length === 0) return

  try {
    await batchUpdateTypeApi({ ids, detailType })
    ElMessage.success('批量修改成功')
    selectedDetailsMap.value[billId] = []
    selectedIncomeDetailsMap.value[billId] = []
    selectedExpenseDetailsMap.value[billId] = []
    await loadDetails(billId, { force: true, silent: true, quiet: true, syncBillRow: true })
    refreshBillDataAfterDetailChange()
  } catch (error) {
    handleError(error, '批量修改交易类型')
  }
}

function refreshBillDataAfterDetailChange() {
  void fetchBillOverview()
  nextTick(scheduleBillTableLayout)
}

async function handleDelete(id: number) {
  try {
    await deleteBillApi(id)
    ElMessage.success('删除成功')
    clearBillSelection()
    handleSearch()
  } catch (error) {
    handleError(error, '删除账单')
  }
}

async function handleBatchDeleteBills() {
  const ids = selectedBillRows.value.map(row => Number(row.id)).filter(id => id > 0)
  if (!ids.length) return

  try {
    await ElMessageBox.confirm(`确认删除选中的 ${ids.length} 条账单？`, '批量删除账单', {
      type: 'warning'
    })
    await batchDeleteBillsApi(ids)
    ElMessage.success('批量删除成功')
    clearBillSelection()
    handleSearch()
  } catch (error: any) {
    if (error !== 'cancel') {
      handleError(error, '批量删除账单')
    }
  }
}

function updateEditField(billId: number, field: string, value: any) {
  if (!editFormMap.value[billId]) return
  const form = editFormMap.value[billId] as any
  form[field] = value

  if (field === 'billAmount' || field === 'posCostAmount') {
    syncInlineAmounts(billId)
  }
}

function buildBillUpdatePayload(row: BillRow, form: EditFormItem, overrides: Record<string, any> = {}) {
  const rowData = row as any
  return {
    id: row.id,
    cardId: row.cardId,
    ownerId: rowData.ownerId,
    billMonth: row.billMonth,
    billDay: form.billDay,
    repayDay: form.repayDay,
    billAmount: toNumber(form.billAmount),
    minPayAmount: toNumber(rowData.minPayAmount),
    actualPayAmount: toNumber(rowData.actualPayAmount),
    actualPayDate: rowData.actualPayDate,
    feeRate: row.feeRate,
    feePaid: Boolean(row.feePaid),
    verified: form.verified,
    expenseVerified: Boolean(row.expenseVerified),
    posCostAmount: toNumber(form.posCostAmount),
    otherFeeAmount: toNumber(row.otherFeeAmount),
    status: form.status,
    remark: form.remark || '',
    ...overrides
  }
}

function applyLocalBillEdit(row: BillRow, form: EditFormItem) {
  const billAmount = toNumber(form.billAmount)
  const posCostAmount = toNumber(form.posCostAmount)
  const feeAmount = buildFeeAmount(billAmount, row.feeRate)
  const netProfit = buildNetProfit(billAmount, row.feeRate, posCostAmount, toNumber(row.otherFeeAmount))

  row.billAmount = billAmount
  row.billDay = form.billDay
  row.repayDate = buildRepayDate(row.billMonth, form.repayDay) || row.repayDate
  row.status = Number(form.status ?? row.status)
  row.posCostAmount = posCostAmount
  row.feeAmount = feeAmount
  row.netProfit = netProfit
  row.remark = form.remark || ''
  editFormMap.value[row.id] = buildEditForm(row)
}

async function handleRepayVerifiedChange(row: BillRow, verified: boolean) {
  const form = ensureEditForm(row)
  const previousVerified = Boolean(row.verified)
  const previousFormVerified = Boolean(form.verified)
  row.verified = verified
  form.verified = verified
  // 核实操作不再自动改变账单状态，状态由用户手动管理
  try {
    await updateBillVerificationApi(row.id, {
      verified,
      expenseVerified: Boolean(row.expenseVerified)
    })
  } catch (error) {
    row.verified = previousVerified
    form.verified = previousFormVerified
    handleError(error, '更新还款明细核实状态')
  }
}

async function handleExpenseVerifiedChange(row: BillRow, expenseVerified: boolean) {
  const form = ensureEditForm(row)
  const previousExpenseVerified = Boolean(row.expenseVerified)
  row.expenseVerified = expenseVerified
  // 核实操作不再自动改变账单状态，状态由用户手动管理
  try {
    await updateBillVerificationApi(row.id, {
      verified: Boolean(row.verified),
      expenseVerified
    })
  } catch (error) {
    row.expenseVerified = previousExpenseVerified
    handleError(error, '更新消费明细核实状态')
  }
}

async function handleInlineSave(row: BillRow) {
  const form = editFormMap.value[row.id]
  if (!form) return
  savingId.value = row.id
  let saved = false
  try {
    await updateBillApi(buildBillUpdatePayload(row, form))

    const originalRepayDay = parseRepayDay(row.repayDate)
    const billDayChanged = form.billDay !== row.billDay
    const repayDayChanged = form.repayDay !== originalRepayDay
    if (billDayChanged || repayDayChanged) {
      const syncData: { newBillDay?: number; newRepayDay?: number } = {}
      if (billDayChanged) syncData.newBillDay = form.billDay ?? undefined
      if (repayDayChanged) syncData.newRepayDay = form.repayDay ?? undefined
      syncBillScheduleApi(row.cardId, row.billMonth, syncData).catch(() => {})
    }

    applyLocalBillEdit(row, form)
    ElMessage.success('保存成功')
    void fetchBillOverview()
    nextTick(scheduleBillTableLayout)
    saved = true
  } catch (error) {
    handleError(error, '保存账单')
  } finally {
    savingId.value = null
  }
  return saved
}

function openBillEdit(row: BillRow) {
  editFormMap.value[row.id] = buildEditForm(row)
  billEditRow.value = row
  billEditDialogVisible.value = true
}

async function handleSaveBillEdit() {
  if (!billEditRow.value) return
  const saved = await handleInlineSave(billEditRow.value)
  if (saved) {
    billEditDialogVisible.value = false
  }
}

async function toggleBillDetail(row: BillRow) {
  if (currentExpandedRow.value?.id === row.id) {
    currentExpandedRow.value = null
    if (pendingExpandBillId.value === row.id) {
      pendingExpandBillId.value = null
    }
    nextTick(scheduleBillTableLayout)
    return
  }

  const billId = Number(row.id)

  if (detailLoadedMap.value[billId]) {
    pendingExpandBillId.value = null
    currentExpandedRow.value = row
    nextTick(() => {
      scheduleBillTableLayout()
      scrollExpandedContentIntoView()
    })
    return
  }

  pendingExpandBillId.value = billId
  const requestToken = ++expandRequestToken

  try {
    await loadDetails(billId)
    if (requestToken !== expandRequestToken || pendingExpandBillId.value !== billId) return
    currentExpandedRow.value = sortedList.value.find(item => Number(item.id) === billId) || row
  } finally {
    if (pendingExpandBillId.value === billId) {
      pendingExpandBillId.value = null
    }
    nextTick(() => {
      scheduleBillTableLayout()
      scrollExpandedContentIntoView()
    })
  }
}

function currentDateString() {
  const date = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 快捷键支持
function handleKeydown(e: KeyboardEvent) {
  if (e.ctrlKey && e.key === 's') {
    e.preventDefault()
    if (billEditDialogVisible.value && billEditRow.value) {
      handleSaveBillEdit()
    }
  }

  if (e.key === 'Escape') {
    createBillDialogVisible.value = false
    billEditDialogVisible.value = false
    detailDialogVisible.value = false
  }

  if (e.ctrlKey && e.key === 'n') {
    e.preventDefault()
    if (currentExpandedRow.value) {
      openAddDetail(currentExpandedRow.value)
    }
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  triggerBillSearch.cancel()
  document.removeEventListener('keydown', handleKeydown)
  if (billTableLayoutFrame) {
    window.cancelAnimationFrame(billTableLayoutFrame)
    billTableLayoutFrame = 0
  }
  cancelDetailPrefetch()
})

function buildFeeAmount(amount: number, feeRate: number | null | undefined) {
  return Number(((amount * toNumber(feeRate)) / 100).toFixed(2))
}

function buildNetProfit(amount: number, feeRate: number | null | undefined, posCostAmount: number, otherFeeAmount = 0) {
  return Number((buildFeeAmount(amount, feeRate) - posCostAmount - otherFeeAmount).toFixed(2))
}

function buildRepayDate(billMonth: string | null | undefined, repayDay: number | null | undefined) {
  const parts = parseBillMonthParts(billMonth)
  const day = Number(repayDay || 0)
  if (!parts || day < 1) return ''
  const repayMonthDate = new Date(parts.year, parts.month, 1)
  const repayYear = repayMonthDate.getFullYear()
  const repayMonth = repayMonthDate.getMonth() + 1
  const lastDay = new Date(repayYear, repayMonth, 0).getDate()
  return `${repayYear}-${String(repayMonth).padStart(2, '0')}-${String(Math.min(day, lastDay)).padStart(2, '0')}`
}

function parseRepayDay(repayDate: string | null | undefined) {
  if (!repayDate) return null
  const parts = String(repayDate).split('-')
  return parts[2] ? Number(parts[2]) : null
}

function padMonthDay(value: number | string | null | undefined) {
  const num = Number(value)
  return Number.isFinite(num) && num > 0 ? String(num).padStart(2, '0') : ''
}

function billYearLabel(billMonth: string | null | undefined) {
  return parseBillMonthParts(billMonth)?.year || '-'
}

function billDateLabel(billMonth: string | null | undefined, billDay: number | null | undefined) {
  const parts = parseBillMonthParts(billMonth)
  const day = padMonthDay(billDay)
  if (!parts || !day) return '-'
  return `${padMonthDay(parts.month)}月${day}日`
}

function repayDateLabel(date: string | null | undefined) {
  const match = String(date || '').match(/(\d{4})-(\d{2})-(\d{2})/)
  return match ? `${match[2]}月${match[3]}日` : '-'
}

function bankCardText(row: BillRow) {
  return `${displayBankName(row.bankName)} 尾号${row.cardNoLast4 || '-'}`
}

function displayBankName(name: string | null | undefined) {
  const value = String(name || '').trim()
  if (!value) return '-'
  return value.includes('银行') ? value : `${value}银行`
}

watch(
  () => loading.value,
  (isLoading) => {
    if (isLoading) {
      cancelDetailPrefetch()
      clearBillSelection()
    }
  }
)

watch(
  () => sortedList.value.map(item => item.id),
  (ids) => {
    if (!currentExpandedRow.value?.id) return

    if (!ids.includes(currentExpandedRow.value.id)) {
      currentExpandedRow.value = null
      nextTick(scheduleBillTableLayout)
      return
    }

    const latestRow = sortedList.value.find(item => item.id === currentExpandedRow.value?.id)
    if (latestRow && latestRow !== currentExpandedRow.value) {
      currentExpandedRow.value = latestRow
    }
  }
)

watch(
  billMonthFilter,
  () => {
    if (syncingBillFilters) return
    syncBillMonthQuery()
  },
  { flush: 'sync' }
)

watch(
  () => [query.ownerName, query.cardName, query.status, query.startBillMonth, query.endBillMonth, query.repayMonth, query.repayYear, query.sortMode],
  () => {
    if (syncingBillFilters) return
    triggerBillSearch()
  },
  { flush: 'sync' }
)

watch(
  () => [route.query.cardId, route.query.cardIds, route.query.ownerId, route.query.status, route.query.year, route.query.billMonth, route.query.startBillMonth, route.query.endBillMonth, route.query.repayMonth, route.query.repayYear, route.query.sortMode],
  ([cardId, cardIds, ownerId, status, year, billMonth, startBillMonth, endBillMonth, repayMonth, repayYear, sortMode]) => {
    triggerBillSearch.cancel()
    const previousSyncState = syncingBillFilters
    syncingBillFilters = true
    const routeCardId = toRouteNumber(cardId)
    const routeCardIds = toRouteCardIdsValue(cardIds)
    const routeOwnerId = toRouteNumber(ownerId)
    const routeRepayMonth = toRouteBillMonthValue(repayMonth)
    const routeRepayYear = toRouteYearValue(repayYear)
    const routeSortMode = toRouteBillSortMode(sortMode)
    let [routeStartBillMonth, routeEndBillMonth] = normalizeRouteBillRange(startBillMonth, endBillMonth, year, billMonth)
    if (routeRepayMonth || routeRepayYear) {
      routeStartBillMonth = ''
      routeEndBillMonth = ''
    } else if (routeCardId && !routeOwnerId && !routeCardIds && !routeStartBillMonth && !routeEndBillMonth) {
      routeStartBillMonth = `${currentYear}-01`
      routeEndBillMonth = `${currentYear}-12`
    }
    query.cardId = routeCardId as any
    query.cardIds = routeCardIds
    query.ownerId = routeOwnerId as any
    query.status = toRouteBillStatus(status) as any
    query.repayMonth = routeRepayMonth
    query.repayYear = routeRepayMonth ? undefined as any : routeRepayYear as any
    applyBillMonthRange(routeStartBillMonth, routeEndBillMonth)
    query.sortMode = routeSortMode || resolveBillSortMode({ ...query, sortMode: undefined })
    query.pageSize = defaultPageSizeForScope(query)
    syncingBillFilters = previousSyncState
    currentExpandedRow.value = null
    if (skipRouteDrivenSearch) {
      skipRouteDrivenSearch = false
      return
    }
    refreshFirstPage()
  },
  { immediate: true }
)
</script>

<style scoped>
.bill-page {
  --bill-gap: 6px;
  --bill-font-size: 12px;
  --bill-small-font-size: 11px;
  --bill-title-size: 15px;
  --bill-main-row-height: 34px;
  display: flex;
  flex-direction: column;
  gap: var(--bill-gap);
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 0;
  background: #f5f7fb;
  overflow: hidden;
  box-sizing: border-box;
}

.single-card-annual-page {
  --bill-gap: 4px;
}

.single-card-annual-page .page-header {
  padding: 4px 8px;
}

.single-card-annual-page .bill-search-panel {
  gap: 6px 10px;
  padding: 6px 8px;
}

.single-card-annual-page .app-search-main {
  gap: 6px;
}

.single-card-annual-page .app-search-title,
.single-card-annual-page .app-search-meta {
  min-height: 28px;
}

.single-card-annual-page .app-search-btn {
  height: 28px;
  padding: 0 10px;
}

.single-card-annual-page .bill-search-panel :deep(.el-input__wrapper),
.single-card-annual-page .bill-search-panel :deep(.el-select__wrapper),
.single-card-annual-page .bill-search-panel :deep(.el-date-editor.el-input__wrapper),
.single-card-annual-page .bill-search-panel :deep(.el-date-editor .el-input__wrapper) {
  min-height: 28px;
  border-radius: 8px;
}

.card-shell {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid #dbe2ea;
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.04);
}

.page-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--bill-gap);
  padding: 6px 8px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 250, 253, 0.98) 100%);
  border: 1px solid #dbe2ea;
  border-radius: 12px;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.03);
  flex-shrink: 0;
}

.header-copy {
  min-width: 0;
}

.header-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.header-title {
  font-size: var(--bill-title-size);
  line-height: 1.1;
  font-weight: 700;
  color: #1f2a37;
}

.detail-mode-chip {
  min-height: 18px;
  padding: 0 7px;
  border-radius: 999px;
  background: #eaf2ff;
  color: #0958d9;
  font-size: var(--bill-small-font-size);
  line-height: 18px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 5px;
}

.action-btn {
  height: 26px;
  padding: 0 9px;
  border-radius: 8px;
}

/*noinspection CssUnusedSymbol*/
.header-actions :deep(.el-button) {
  height: 26px;
  padding: 0 9px;
  border-radius: 8px;
}

.bill-search-panel {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  flex-shrink: 0;
  gap: 6px 10px;
  padding: 4px;
}

.bill-search-panel .app-search-main {
  display: grid;
  grid-template-columns: auto repeat(4, minmax(0, 1fr)) auto;
  align-items: center;
  gap: 6px;
}

.bill-search-panel .app-search-extra,
.bill-search-panel .app-search-actions {
  gap: 6px;
}

.bill-search-panel .app-search-item,
.bill-search-panel .app-search-item-sm {
  width: 100%;
  min-width: 0;
}

/*noinspection CssUnusedSymbol*/
.bill-search-panel :deep(.app-search-item),
.bill-search-panel :deep(.app-search-item-sm),
.bill-search-panel :deep(.el-date-editor.app-search-item-sm) {
  width: 100% !important;
  min-width: 0;
}

.bill-search-panel .app-search-title {
  min-height: 30px;
  font-size: 12px;
  color: #667085;
}

.bill-search-panel .app-search-actions {
  margin-left: 0;
  justify-content: flex-end;
}

.quick-menu-bar {
  align-items: stretch;
  justify-content: flex-start;
  padding-top: 4px;
  border-top: 1px dashed #e5eaf1;
}

.bill-search-panel .app-search-btn {
  height: 34px;
  padding: 0 16px;
  border-radius: 8px;
  font-size: 13px;
}

/*noinspection CssUnusedSymbol*/
.bill-search-panel :deep(.el-input__wrapper),
.bill-search-panel :deep(.el-select__wrapper),
.bill-search-panel :deep(.el-date-editor.el-input__wrapper),
.bill-search-panel :deep(.el-date-editor .el-input__wrapper) {
  min-height: 28px;
  border-radius: 8px;
}

/*noinspection CssUnusedSymbol*/
.bill-search-panel :deep(.el-input__inner),
.bill-search-panel :deep(.el-select__selected-item),
.bill-search-panel :deep(.el-range-input),
.bill-search-panel :deep(.el-input-number__input),
.bill-search-panel :deep(.el-date-editor .el-range-separator) {
  font-size: 12px;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--bill-gap);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.data-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 4px;
  overflow: hidden;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 5px;
  margin-bottom: 6px;
  flex-shrink: 0;
}

.table-batch-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.panel-title {
  font-size: 13px;
  font-weight: 700;
  color: #1f2a37;
  line-height: 1.15;
  display: flex;
  align-items: center;
  gap: 8px;
}

.back-btn-inline {
  font-size: 12px;
  padding: 0 8px;
  height: 24px;
  border-radius: 6px;
}

.panel-desc {
  margin-top: 2px;
  font-size: var(--bill-small-font-size);
  color: #8a94a6;
  line-height: 1.25;
  white-space: normal;
  word-break: break-word;
}

.menu-list {
  display: grid;
  gap: 3px;
  overflow: hidden;
}

.menu-list-inline {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
  min-width: 0;
  width: 100%;
}

.menu-item {
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr) auto;
  align-items: center;
  gap: 6px;
  width: 100%;
  height: 36px;
  padding: 0 12px;
  border: 1px solid rgba(218, 226, 236, 0.92);
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(255,255,255,0.99) 0%, rgba(248,250,253,0.97) 100%);
  color: #55657b;
  cursor: pointer;
  transition: background-color 0.12s ease, border-color 0.12s ease, color 0.12s ease, box-shadow 0.12s ease;
  box-shadow: 0 6px 16px rgba(15,23,42,0.05);
}

.menu-item:hover {
  transform: none;
  border-color: rgba(180, 206, 255, 0.92);
  box-shadow: 0 7px 14px rgba(15,23,42,0.06);
}

.menu-item.active {
  transform: none;
  border-color: transparent;
  background: linear-gradient(180deg, rgba(255,255,255,0.99) 0%, rgba(234,242,255,0.95) 160%);
  color: #0958d9;
  box-shadow: inset 0 0 0 1.5px rgba(180, 206, 255, 0.85), 0 7px 14px rgba(15,23,42,0.06);
}

.menu-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.menu-label {
  text-align: center;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-count {
  min-width: 20px;
  height: 20px;
  padding: 0 4px;
  border-radius: 999px;
  background: rgba(9, 88, 217, 0.1);
  color: inherit;
  font-size: 11px;
  line-height: 20px;
  text-align: center;
}

.data-head {
  align-items: flex-start;
}

.bill-page-table {
  flex: 1 1 0;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 4px 6px !important;
  background: #fff;
  border: 1px solid #e5eaf1;
  border-radius: 10px;
  box-shadow: none !important;
  overflow: hidden;
  box-sizing: border-box;
}

.bill-page-table.single-card-annual-table {
  flex: 1 1 0;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table) {
  flex: 1 1 0;
  min-height: 0;
  height: 100% !important;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__inner-wrapper) {
  height: 100% !important;
  min-height: 0;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__body-wrapper),
.bill-page-table :deep(.el-scrollbar) {
  flex: 1 1 auto;
  min-height: 0;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-scrollbar),
.bill-page-table :deep(.el-scrollbar__wrap) {
  height: 100%;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-scrollbar__view) {
  min-height: 100%;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table),
.bill-page-table :deep(.el-table__inner-wrapper),
.detail-section :deep(.el-table),
.detail-section :deep(.el-table__inner-wrapper) {
  --el-table-border-color: #e5eaf1;
  font-size: var(--bill-font-size);
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__inner-wrapper::before),
.detail-section :deep(.el-table__inner-wrapper::before) {
  display: none;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__body),
.bill-page-table :deep(.el-table__row),
.bill-page-table :deep(.el-table__expanded-cell) {
  animation: none !important;
  transition: none !important;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__header),
.bill-page-table :deep(.el-table__body),
.detail-section :deep(.el-table__header),
.detail-section :deep(.el-table__body) {
  width: 100% !important;
  table-layout: fixed;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table .cell),
.detail-section :deep(.el-table .cell) {
  padding: 1px 3px;
  line-height: 1.2;
  white-space: normal;
  overflow: visible;
  text-overflow: clip;
  word-break: break-word;
  font-size: var(--bill-font-size);
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table th.el-table__cell) {
  height: auto;
  padding: 5px 2px;
  background: #f7f9fc;
  font-size: var(--bill-small-font-size);
  color: #5b6472;
  font-weight: 600;
  vertical-align: middle;
}

/*noinspection CssUnusedSymbol*/
.detail-section :deep(.el-table th.el-table__cell) {
  height: auto;
  padding: 5px 2px;
  background: #f7f9fc;
  font-size: var(--bill-small-font-size);
  color: #5b6472;
  font-weight: 600;
  vertical-align: top;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table td.el-table__cell) {
  height: auto;
  padding: 4px 2px;
  vertical-align: middle;
}

/*noinspection CssUnusedSymbol*/
.detail-section :deep(.el-table td.el-table__cell) {
  height: auto;
  padding: 3px 2px;
  vertical-align: top;
}

.table-stack {
  display: flex;
  flex-direction: column;
  gap: 3px;
  width: 100%;
  min-width: 0;
  line-height: 1.2;
}

.table-stack-center {
  align-items: center;
  text-align: center;
}

.table-stack-line {
  display: inline-flex;
  align-items: center;
  justify-content: inherit;
  gap: 4px;
  min-width: 0;
  color: #1f2a37;
  line-height: 1.15;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__body tr:not(.el-table__expanded-row) > td.el-table__cell) {
  height: var(--bill-main-row-height) !important;
  padding-top: 2px !important;
  padding-bottom: 2px !important;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__body tr.el-table__row) {
  height: var(--bill-main-row-height);
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__body tr.el-table__row > td.el-table__cell .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  height: calc(var(--bill-main-row-height) - 4px);
  min-height: calc(var(--bill-main-row-height) - 4px);
  overflow: hidden;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__body tr.bill-placeholder-row > td.el-table__cell) {
  background: #fff !important;
  pointer-events: none;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__body tr.bill-placeholder-row > td.el-table__cell .cell) {
  visibility: hidden;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__body tr.current-month-row > td.el-table__cell) {
  background: #fff7e6 !important;
  box-shadow: inset 0 1px 0 #ffd591, inset 0 -1px 0 #ffd591;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__body tr.current-month-row > td.el-table__cell:first-child) {
  box-shadow: inset 3px 0 0 #fa8c16, inset 0 1px 0 #ffd591, inset 0 -1px 0 #ffd591;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__body tr.current-month-row:hover > td.el-table__cell) {
  background: #fff1cc !important;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__header-wrapper),
.detail-section :deep(.el-table__header-wrapper) {
  overflow: hidden !important;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__body-wrapper),
.bill-page-table :deep(.el-scrollbar__wrap) {
  overflow-x: hidden !important;
  overflow-y: auto !important;
  overscroll-behavior: contain;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table.bill-page-table-scrollable :deep(.el-table__body-wrapper),
.bill-page-table.bill-page-table-scrollable :deep(.el-scrollbar__wrap) {
  overflow-x: hidden !important;
  overflow-y: auto !important;
  overscroll-behavior: contain;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-loading-mask) {
  background: rgba(255, 255, 255, 0.32);
  transition: none !important;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-loading-spinner) {
  display: none;
}

/*noinspection CssUnusedSymbol*/
.detail-section :deep(.el-table__body-wrapper),
.detail-section :deep(.el-scrollbar__wrap) {
  overflow: hidden !important;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-scrollbar__bar),
.detail-section :deep(.el-scrollbar__bar) {
  display: none !important;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__expanded-cell) {
  height: auto !important;
  padding: 8px !important;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-table__expanded-cell .cell) {
  padding: 0 !important;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.pagination-wrapper) {
  margin-top: 4px;
  display: flex;
  justify-content: flex-end;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-pagination) {
  --el-pagination-button-height: 22px;
  --el-pagination-button-width: 22px;
  font-size: var(--bill-small-font-size);
  transform: scale(0.92);
  transform-origin: right center;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-pagination .btn-prev),
.bill-page-table :deep(.el-pagination .btn-next),
.bill-page-table :deep(.el-pagination .el-pager li) {
  min-width: 22px;
  height: 22px;
}

.bill-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  min-height: 28px;
  padding-top: 3px;
  flex-shrink: 0;
}

.pagination-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 10px;
  color: #667085;
  white-space: nowrap;
}

/*noinspection CssUnusedSymbol*/
.bill-pagination :deep(.el-pagination) {
  --el-pagination-button-height: 20px;
  --el-pagination-button-width: 20px;
  font-size: 10px;
}

.owner-cell {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 4px;
  width: 100%;
  min-width: 0;
  overflow: hidden;
}

.owner-expand-btn {
  flex-shrink: 0;
}

.owner-avatar {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  background: linear-gradient(135deg, #e6f0ff 0%, #cfe0ff 100%);
  color: #0958d9;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.el-tag),
.detail-section :deep(.el-tag) {
  width: fit-content;
  min-height: 16px;
  padding: 0 5px;
  font-size: var(--bill-small-font-size);
  line-height: 16px;
}

.bill-status-cell,
.bill-verify-cell,
.fee-paid-tag,
.verify-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  min-width: 52px;
}

.bill-status-badge,
.bill-verify-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  min-width: 52px;
  min-height: 16px;
  padding: 0 5px;
  border: 1px solid transparent;
  border-radius: 4px;
  font-size: var(--bill-small-font-size);
  line-height: 16px;
  font-weight: 500;
  white-space: nowrap;
  box-sizing: border-box;
}

.bill-verify-badge.is-unverified {
  color: #b26a00;
  background: #fdf6ec;
  border-color: #faecd8;
}

.bill-verify-badge.is-verified {
  color: #529b2e;
  background: #f0f9eb;
  border-color: #e1f3d8;
}

.bill-status-badge.is-status-0 {
  color: #b26a00;
  background: #fdf6ec;
  border-color: #faecd8;
}

.bill-status-badge.is-status-1 {
  color: #529b2e;
  background: #f0f9eb;
  border-color: #e1f3d8;
}

.bill-status-badge.is-status-2 {
  color: #73767a;
  background: #f4f4f5;
  border-color: #e9e9eb;
}

.bill-status-badge.is-status-3 {
  color: #c45656;
  background: #fef0f0;
  border-color: #fde2e2;
}

.bill-status-badge.is-status-unknown {
  color: #909399;
  background: #f4f4f5;
  border-color: #e9e9eb;
}

.bill-status-cell :deep(.el-tag),
.bill-verify-cell,
.verify-tag,
.fee-paid-tag {
  justify-content: center;
  width: 52px;
}

.bill-remark-cell {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #526074;
}

.owner-name,
.bank-cell,
.bank-inline-cell,
.bank-inline-name,
.bank-inline-last4,
.year-cell,
.date-cell,
.table-stack,
.table-stack-line,
.amount-value,
.fee-rate-badge,
.fee-paid-tag,
.verify-tag,
.no-data {
  font-size: var(--bill-font-size);
}

.owner-name {
  flex: 1;
  min-width: 0;
  font-weight: 600;
  color: #1f2a37;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: normal;
}

.bank-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-weight: 600;
  text-align: center;
  white-space: normal;
  overflow: visible;
  word-break: break-word;
}

.bank-inline-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  min-width: 0;
  font-weight: 600;
  color: #1f2a37;
  white-space: nowrap;
  overflow: hidden;
}

.bank-inline-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bank-inline-last4 {
  flex: 0 0 auto;
  font-family: var(--font-mono), monospace;
  font-weight: 700;
  color: #475467;
}

.year-cell,
.date-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  width: 100%;
  font-weight: 600;
  color: #1f2a37;
  white-space: nowrap;
}

.bank-card-cell {
  flex-direction: column;
  align-items: center;
  gap: 3px;
}

.bank-line {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  min-width: 0;
}

.bank-line span {
  min-width: 0;
  word-break: break-word;
}

.card-no-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  min-height: 20px;
  padding: 0 7px;
  font-weight: 700;
  font-size: var(--bill-small-font-size);
  color: #1f2a37;
  font-family: var(--font-mono), monospace;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 100%);
  border-radius: 999px;
  border: 1px solid #bae0ff;
}

.amount-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.amount-value,
.fee-rate-badge {
  font-weight: 600;
}

.amount-value {
  font-family: var(--font-mono), monospace;
  letter-spacing: 0.01em;
}

.fee-rate-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 20px;
  padding: 0 7px;
  color: #fa8c16;
  background: linear-gradient(135deg, #fff7e6 0%, #fffbf0 100%);
  border-radius: 999px;
  border: 1px solid #ffe7ba;
  font-family: var(--font-mono), monospace;
}

.no-data {
  color: #c0c7d6;
}

.row-action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  width: 100%;
}

.row-action-btn {
  width: 24px;
  height: 24px;
  padding: 0;
}

.row-action-buttons > .detail-toggle-btn {
  display: none !important;
}

/*noinspection CssUnusedSymbol*/
.detail-toggle-btn :deep(.el-icon) {
  transition: transform 0.18s ease;
}

/*noinspection CssUnusedSymbol*/
.detail-toggle-btn.expanded :deep(.el-icon) {
  transform: rotate(90deg);
}

.expand-bill-content {
  padding: 6px 8px;
  background: #f8fafc;
  border-radius: 8px;
  max-height: min(58vh, 560px);
  overflow: auto;
  overscroll-behavior: contain;
}

.detail-section {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5eaf1;
  overflow: hidden;
  box-shadow: none;
  min-height: 0;
}

.detail-header {
  display: flex;
  align-items: center;
  padding: 5px 8px;
  background: #fafbfc;
  border-bottom: 1px solid #e5eaf1;
}

.detail-header-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-verify-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #526074;
  font-size: var(--bill-small-font-size);
  white-space: nowrap;
}

.detail-verify-switch :deep(.el-switch) {
  flex: 0 0 auto;
  min-width: 74px;
}

/*noinspection CssUnusedSymbol*/
.detail-header :deep(.el-button) {
  flex-shrink: 0;
  height: 24px;
  padding: 0 8px;
}

.batch-toolbar {
  display: flex;
  gap: 6px;
  padding: 5px 8px;
  background: #f5f7fa;
  border-bottom: 1px solid #e5eaf1;
  flex-wrap: wrap;
}

.detail-split-grid {
  --detail-grid-columns: 34px 46px 92px 110px minmax(0, 1fr) 74px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 8px;
  min-height: 0;
}

.detail-pane {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #e5eaf1;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.detail-lite-list {
  display: flex;
  flex-direction: column;
  min-height: 0;
  max-height: min(34vh, 320px);
  overflow: auto;
  overscroll-behavior: contain;
}

.detail-lite-head,
.detail-lite-row {
  display: grid;
  grid-template-columns: var(--detail-grid-columns);
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
}

.detail-lite-head {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #fcfcfd;
  border-bottom: 1px solid #eef2f6;
  color: #7c8799;
  font-size: var(--bill-small-font-size);
}

.detail-lite-row + .detail-lite-row {
  border-top: 1px solid #f0f3f7;
}

.detail-check-col {
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-index-col {
  color: #526074;
  text-align: center;
  white-space: nowrap;
  font-family: var(--font-mono), monospace;
}

.detail-date-col {
  color: #526074;
  white-space: nowrap;
}

.detail-amount-col {
  display: flex;
  justify-content: flex-end;
  white-space: nowrap;
}

.detail-note-col {
  min-width: 0;
}

.detail-action-col {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  white-space: nowrap;
}

.detail-pane-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 8px;
  background: #fafbfc;
  border-bottom: 1px solid #e5eaf1;
}

.detail-pane-head-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.detail-pane-title {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  font-size: var(--bill-font-size);
  font-weight: 700;
  color: #1f2a37;
}

.detail-pane-sub {
  font-size: var(--bill-small-font-size);
  color: #98a2b3;
}

.detail-pane-total {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  min-width: 72px;
  text-align: right;
}

.detail-pane-total-label {
  font-size: var(--bill-small-font-size);
  color: #98a2b3;
  line-height: 1.2;
}

.detail-pane-total-value {
  font-family: var(--font-mono), monospace;
  font-size: var(--bill-font-size);
  font-weight: 700;
  line-height: 1.2;
}

.detail-empty {
  padding: 22px 12px;
  text-align: center;
  font-size: var(--bill-small-font-size);
  color: #98a2b3;
}

.detail-note-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.detail-note-main {
  color: #1f2a37;
}

.detail-note-sub {
  font-size: var(--bill-small-font-size);
  color: #98a2b3;
  line-height: 1.3;
  word-break: break-word;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.expand-toggle-col) {
  width: 1px !important;
  min-width: 1px !important;
  padding: 0 !important;
  border-right: none !important;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table :deep(.expand-toggle-col .cell) {
  display: none !important;
  padding: 0 !important;
}

.bill-edit-static {
  font-size: var(--bill-font-size);
  font-weight: 700;
}

.side-card {
  padding: 6px;
  background: #f8fafc;
  border: 1px solid #e7edf4;
  border-radius: 10px;
}

.side-card + .side-card {
  margin-top: 6px;
}

.side-card-title {
  margin-bottom: 6px;
  font-size: var(--bill-font-size);
  font-weight: 700;
  color: #1f2a37;
}

.detail-mode-text {
  margin-bottom: 3px;
  font-size: var(--bill-small-font-size);
  line-height: 1.4;
  color: #667085;
}

.action-stack {
  display: grid;
  gap: 5px;
}

/*noinspection CssUnusedSymbol*/
.action-stack :deep(.el-button) {
  width: 100%;
  height: 28px;
  margin-left: 0;
  border-radius: 8px;
}

.summary-list,
.shortcut-list {
  display: grid;
  gap: 5px;
}

.summary-item,
.shortcut-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 5px;
  font-size: var(--bill-small-font-size);
  color: #667085;
}

.summary-item strong {
  color: #1f2a37;
  font-size: var(--bill-font-size);
  word-break: break-all;
}

.shortcut-item kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  min-height: 18px;
  padding: 0 5px;
  font-family: monospace;
  font-size: var(--bill-small-font-size);
  background: #fff;
  border: 1px solid #dbe2ea;
  border-radius: 6px;
}

@media (max-width: 1520px) {
  .page-header {
    grid-template-columns: minmax(0, 1fr) auto;
  }
}

@media (max-width: 1320px) {
  .panel-desc {
    display: none;
  }
}

@media (max-width: 1380px) {
  .detail-split-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 1180px) {
  .page-header {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .menu-list-inline {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .bill-pagination {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
