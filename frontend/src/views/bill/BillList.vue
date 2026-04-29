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
          class="app-search-item app-search-item-md"
          placeholder="请输入持卡人姓名模糊查询"
          clearable
          maxlength="20"
        />
        <el-input
          v-model="query.cardName"
          class="app-search-item app-search-item-lg"
          placeholder="请输入名称或银行卡尾号查询"
          clearable
          maxlength="30"
        />
        <el-date-picker
          v-model="billMonthRange"
          class="app-search-item app-search-item-range"
          type="monthrange"
          value-format="YYYY-MM"
          range-separator="至"
          start-placeholder="请选择账单开始月份"
          end-placeholder="请选择账单结束月份"
          :editable="false"
          clearable
        />
        <el-select v-model="query.status" class="app-search-item app-search-item-sm" placeholder="请选择账单状态" clearable>
          <el-option v-for="item in BILL_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.feePaid" class="app-search-item app-search-item-sm" placeholder="请选择手续费支付状态" clearable>
          <el-option label="已支付" :value="true" />
          <el-option label="未支付" :value="false" />
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
            <div class="panel-title">账单数据区</div>
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
      <el-table-column v-if="isAdmin" type="selection" width="42" align="center" reserve-selection />
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
                        <el-dropdown-item :command="DETAIL_TYPE_VALUE.EXPENSE">转账</el-dropdown-item>
                        <el-dropdown-item :command="DETAIL_TYPE_VALUE.INCOME">支出</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>

                <div class="detail-split-grid">
                  <div class="detail-pane">
                    <div class="detail-pane-head">
                      <div class="detail-pane-head-main">
                        <div class="detail-pane-title">
                          <span>支出</span>
                          <el-tag type="success" size="small" effect="light">{{ detailTypeTotalCount(row.id, DETAIL_TYPE_VALUE.INCOME) }}</el-tag>
                        </div>
                        <span class="detail-pane-sub">左侧展示支出</span>
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
                          <el-popconfirm title="确认删除？" @confirm="handleDeleteDetail(detail.id)">
                            <template #reference><el-button type="danger" link size="small">删</el-button></template>
                          </el-popconfirm>
                        </div>
                      </div>
                    </div>
                    <div v-else class="detail-empty">暂无支出明细</div>
                  </div>

                  <div class="detail-pane">
                    <div class="detail-pane-head">
                      <div class="detail-pane-head-main">
                        <div class="detail-pane-title">
                          <span>转账</span>
                          <el-tag type="danger" size="small" effect="light">{{ detailTypeTotalCount(row.id, DETAIL_TYPE_VALUE.EXPENSE) }}</el-tag>
                          <div class="detail-verify-switch" @click.stop>
                            <span>明细核实</span>
                            <el-switch
                              :model-value="Boolean(row.expenseVerified)"
                              :loading="savingId === row.id"
                              active-text="已核实"
                              inactive-text="未核实"
                              @change="(val: any) => handleExpenseVerifiedChange(row, Boolean(val))"
                            />
                          </div>
                        </div>
                        <span class="detail-pane-sub">右侧展示转账</span>
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
                          <el-popconfirm title="确认删除？" @confirm="handleDeleteDetail(detail.id)">
                            <template #reference><el-button type="danger" link size="small">删</el-button></template>
                          </el-popconfirm>
                        </div>
                      </div>
                    </div>
                    <div v-else class="detail-empty">暂无转账明细</div>
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
      <el-table-column label="费率" width="54" align="center">
        <template #default="{ row }">
          <span class="fee-rate-badge">{{ formatRate(row.feeRate) }}%</span>
        </template>
      </el-table-column>
      <el-table-column label="手续费" width="88" align="center" header-align="center">
        <template #default="{ row }">
          <span class="amount-value amt-pos">{{ formatMoney(row.feeAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="手续费支付" width="86" align="center" header-align="center">
        <template #default="{ row }">
          <el-tag v-if="row.feePaid" class="fee-paid-tag" type="success" size="small" effect="light">已支付</el-tag>
          <el-tag v-else class="fee-paid-tag" type="warning" size="small" effect="plain">未支付</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否核实" width="78" align="center" header-align="center">
        <template #default="{ row }">
          <el-tag v-if="row.verified" class="verify-tag" type="success" size="small" effect="light">已核实</el-tag>
          <el-tag v-else class="verify-tag" type="warning" size="small" effect="plain">未核实</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="78" align="center" header-align="center">
        <template #default="{ row }">
          <StatusTag :value="row.status" :label-map="BILL_LIST_STATUS_MAP" :type-map="BILL_STATUS_TAG_TYPE" size="small" />
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

        <div v-if="showBillPagination && total > 0" class="bill-pagination">
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
        <el-form-item label="手续费率" prop="feeRate">
          <el-input-number v-model="createBillForm.feeRate" :min="0" :max="100" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <template v-if="createBillForm.mode === 'month'">
          <el-form-item label="账单金额" prop="billAmount">
            <el-input-number v-model="createBillForm.billAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="POS成本">
            <el-input-number v-model="createBillForm.posCostAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
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
          <el-form-item label="POS成本">
            <el-input-number
              :model-value="editFormMap[billEditRowId]?.posCostAmount"
              :min="0"
              :precision="2"
              controls-position="right"
              style="width: 100%"
              @update:model-value="(val:any) => updateEditField(billEditRowId, 'posCostAmount', val)"
            />
          </el-form-item>
          <el-form-item label="手续费支付">
            <el-switch
              :model-value="editFormMap[billEditRowId]?.feePaid"
              active-text="已付"
              inactive-text="未付"
              @update:model-value="(val:any) => updateEditField(billEditRowId, 'feePaid', val)"
            />
          </el-form-item>
          <el-form-item label="是否核实">
            <el-switch
              :model-value="editFormMap[billEditRowId]?.verified"
              active-text="已核实"
              inactive-text="未核实"
              @update:model-value="(val:any) => updateEditField(billEditRowId, 'verified', val)"
            />
          </el-form-item>
          <el-form-item label="手续费率">
            <span class="bill-edit-static">{{ formatRate(billEditRow.feeRate) }}%</span>
          </el-form-item>
          <el-form-item label="手续费收入">
            <span class="bill-edit-static amt-pos">¥{{ formatMoney(editFormMap[billEditRowId]?.feeAmount) }}</span>
          </el-form-item>
          <el-form-item label="净利润">
            <span class="bill-edit-static" :class="Number(editFormMap[billEditRowId]?.netProfit ?? 0) >= 0 ? 'amt-pos' : 'amt-neg'">
              ¥{{ formatMoney(editFormMap[billEditRowId]?.netProfit) }}
            </span>
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
          <el-input v-model="detailForm.description" placeholder="如：代还服务收入" />
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
import { ArrowDown, ArrowRight, UserFilled, CreditCard, Delete, RefreshRight, Edit, Plus } from '@element-plus/icons-vue'
import PageTable from '@/components/PageTable/index.vue'
import StatusTag from '@/components/StatusTag/index.vue'
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
  deleteBillApi,
  batchDeleteBillsApi,
  exportBillApi,
  generateAnnualBillsApi,
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
  cardId: number
  ownerName: string
  bankName: string
  cardNoLast4: string
  billMonth: string
  billDay: number | null
  repayDate: string | null
  billAmount: number | null
  feeRate: number | null
  feeAmount: number | null
  feePaid: boolean
  posCostAmount: number | null
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
  feePaid: boolean
  verified: boolean
  feeAmount: string
  netProfit: string
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

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const now = new Date()
const currentYear = now.getFullYear()
const currentMonthNumber = now.getMonth() + 1
const currentMonth = `${currentYear}-${String(currentMonthNumber).padStart(2, '0')}`
const SINGLE_CARD_ANNUAL_PAGE_SIZE = 12
const DEFAULT_BILL_PAGE_SIZE = 18
const DETAIL_PREFETCH_DELAY = 80
const DETAIL_PREFETCH_CONCURRENCY = 2
const DETAIL_PREFETCH_ANNUAL_LIMIT = 20
const BILL_LIST_STATUS_MAP: Record<number, string> = {
  0: '未还',
  1: '已还款',
  2: '部分',
  3: '逾期'
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

const billMonthRange = ref<string[]>([])

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
    ownerId: undefined as any,
    ownerName: '',
    cardName: '',
    startBillMonth: '',
    endBillMonth: '',
    status: undefined as any,
    feePaid: undefined as any
  },
  autoSearch: false,
  beforeFetch: (params) => {
    params.pageSize = resolveBillPageSize(params)
    ;(params as any).current = params.pageNum
    ;(params as any).size = params.pageSize
    delete (params as any).pageNum
    delete (params as any).pageSize
  },
  afterFetch: () => {
    fetchBillOverview()
    scheduleDetailPrefetch()
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
  feeRate: 0 as number | null,
  billAmount: 0,
  posCostAmount: 0,
  remark: ''
})

const billScopeLabel = computed(() => formatBillRangeLabel(query.startBillMonth, query.endBillMonth))
const singleCardAnnualMode = computed(() => isSingleCardAnnualScope(query))
const safeBillPageSize = computed(() => Number(query.pageSize || 0) || DEFAULT_BILL_PAGE_SIZE)
const isBillTableScrollable = computed(() => safeBillPageSize.value > defaultPageSizeForScope(query))
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
  feeRate: [{ required: true, message: '请输入手续费率', trigger: 'change' }],
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
let syncingBillFilters = false
let skipRouteDrivenSearch = false

const detailModeMessage = computed(() => {
  const scopeText = billScopeLabel.value
  if (route.query.cardId) {
    return scopeText ? `当前为单张银行卡账单模式，已定位到 ${scopeText}。` : '当前为单张银行卡账单模式。'
  }
  if (route.query.ownerId) {
    return scopeText ? `当前为持卡人/子用户账单模式，已定位到 ${scopeText}。` : '当前为持卡人/子用户账单模式。'
  }
  return ''
})

const quickMenus = computed(() => [
  { key: 'all', label: '全部', value: undefined, count: billOverview.value.billCount, color: '#0958d9' },
  { key: 'pending', label: '待还', value: 0, count: billOverview.value.pendingCount, color: '#d97706' },
  { key: 'partial', label: '部分', value: 2, count: billOverview.value.partialCount, color: '#7c8799' },
  { key: 'paid', label: '已还', value: 1, count: billOverview.value.repaidCount, color: '#2f9e44' },
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

function isWholeYearRange(startBillMonth?: string | null, endBillMonth?: string | null) {
  const start = parseBillMonthParts(startBillMonth)
  const end = parseBillMonthParts(endBillMonth)
  return !!start && !!end && start.year === end.year && start.month === 1 && end.month === 12
}

function isAnnualBillScope(params: any) {
  return isWholeYearRange(params?.startBillMonth, params?.endBillMonth)
}

function isSingleCardAnnualScope(params: any) {
  return isAnnualBillScope(params) && Number(params?.cardId || 0) > 0 && !Number(params?.ownerId || 0)
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
  const [startBillMonth = '', endBillMonth = ''] = billMonthRange.value || []
  query.startBillMonth = startBillMonth
  query.endBillMonth = endBillMonth
}

function applyBillMonthRange(startBillMonth?: string | null, endBillMonth?: string | null) {
  const start = String(startBillMonth || '').trim()
  const end = String(endBillMonth || '').trim()
  billMonthRange.value = start && end ? [start, end] : []
  query.startBillMonth = start
  query.endBillMonth = end
}

function submitBillSearch() {
  triggerBillSearch.cancel()
  syncBillMonthQuery()
  currentExpandedRow.value = null
  handleSearch()
}

function resolveMonthOrder(month: number) {
  const hasCardId = Number(query.cardId || 0) > 0
  const hasOwnerId = Number(query.ownerId || 0) > 0
  if (hasCardId && !hasOwnerId) {
    return month
  }
  return (month - currentMonthNumber + 12) % 12
}

const sortedList = computed<BillRow[]>(() => {
  return [...(list.value as BillRow[])].sort((a, b) => {
    const aParts = parseBillMonthParts(a.billMonth)
    const bParts = parseBillMonthParts(b.billMonth)

    if (aParts?.year !== bParts?.year) {
      return Number(bParts?.year || 0) - Number(aParts?.year || 0)
    }

    if (aParts?.month !== bParts?.month) {
      return resolveMonthOrder(Number(aParts?.month || 0)) - resolveMonthOrder(Number(bParts?.month || 0))
    }

    return Number(b.id || 0) - Number(a.id || 0)
  })
})

const tableDisplayList = computed<BillRow[]>(() => loading.value ? [] : sortedList.value)

function billRowClassName({ row }: { row: BillRow }) {
  return row?.billMonth === currentMonth ? 'current-month-row' : ''
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
  query.status = status as any
}

const { exporting, handleExport: doExport } = useExport({
  exportApi: exportBillApi,
  fileName: '账单记录'
})

function handleExport() {
  return doExport(buildExportParams())
}

function buildExportParams() {
  return {
    cardId: query.cardId,
    ownerId: query.ownerId,
    ownerName: query.ownerName,
    cardName: query.cardName,
    startBillMonth: query.startBillMonth,
    endBillMonth: query.endBillMonth,
    status: query.status,
    feePaid: query.feePaid
  }
}

function buildOverviewParams() {
  const params = buildExportParams()
  delete (params as any).status
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
  const num = Number(target)
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
  delete nextQuery.ownerId
  delete nextQuery.status
  delete nextQuery.year
  delete nextQuery.billMonth
  delete nextQuery.startBillMonth
  delete nextQuery.endBillMonth
  router.replace({ path: route.path, query: nextQuery })
}

async function handleResetAll() {
  triggerBillSearch.cancel()
  syncingBillFilters = true
  const hasRouteFilters = !!(route.query.cardId || route.query.ownerId || route.query.status || route.query.year || route.query.billMonth || route.query.startBillMonth || route.query.endBillMonth)
  if (hasRouteFilters) {
    skipRouteDrivenSearch = true
    clearRouteFilters()
  }
  billMonthRange.value = []
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
    feeRate: 0,
    billAmount: 0,
    posCostAmount: 0,
    remark: ''
  })
  nextTick(() => createBillFormRef.value?.clearValidate?.())
}

function applyCreateCardDefaults(card: BankCardOption | null) {
  createBillForm.billDay = card?.billDay ?? null
  createBillForm.repayDay = card?.repayDay ?? null
  createBillForm.feeRate = toNumber(card?.effectiveFeeRate)
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
    repayDay: createBillForm.repayDay,
    feeRate: createBillForm.feeRate == null ? undefined : toNumber(createBillForm.feeRate)
  }
}

function focusCreatedBills(cardId: number, startBillMonth: string, endBillMonth: string) {
  triggerBillSearch.cancel()
  const nextRouteQuery: Record<string, any> = {
    ...route.query,
    cardId: String(cardId),
    startBillMonth,
    endBillMonth
  }
  delete nextRouteQuery.ownerId
  delete nextRouteQuery.status
  delete nextRouteQuery.year
  delete nextRouteQuery.billMonth
  void router.replace({ path: route.path, query: nextRouteQuery }).catch(() => {})
  syncingBillFilters = true
  query.cardId = cardId as any
  query.ownerId = undefined as any
  query.ownerName = ''
  query.cardName = ''
  query.status = undefined as any
  query.feePaid = undefined as any
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
      posCostAmount: toNumber(createBillForm.posCostAmount),
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
  selectedBillRows.value = selection || []
}

function clearBillSelection() {
  selectedBillRows.value = []
  billTableRef.value?.clearSelection?.()
}

function ensureEditForm(row: BillRow) {
  if (!editFormMap.value[row.id]) {
    editFormMap.value[row.id] = {
      id: row.id,
      billAmount: toNumber(row.billAmount),
      billDay: row.billDay ?? null,
      posCostAmount: toNumber(row.posCostAmount),
      repayDay: parseRepayDay(row.repayDate),
      feePaid: row.feePaid ?? false,
      verified: Boolean(row.verified),
      feeAmount: '0.00',
      netProfit: '0.00'
    }
    syncInlineAmounts(row.id)
  }
  return editFormMap.value[row.id]
}

function syncInlineAmounts(billId: number) {
  const form = editFormMap.value[billId]
  const row = (list.value as BillRow[]).find(item => item.id === billId)
  if (!form || !row) return
  const feeAmount = buildFeeAmount(form.billAmount, row.feeRate)
  form.feeAmount = feeAmount.toFixed(2)
  form.netProfit = buildNetProfit(form.billAmount, row.feeRate, form.posCostAmount).toFixed(2)
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

function setBillDetails(billId: number, details: BillDetailRow[]) {
  detailListMap.value[billId] = details
  detailBucketMap.value[billId] = buildDetailBucket(details)
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

async function loadDetails(billId: number, options: { force?: boolean; silent?: boolean } = {}) {
  if (!options.force && detailLoadedMap.value[billId]) {
    resetDetailSelection(billId)
    return
  }
  if (detailLoadingMap.value[billId]) {
    await (detailRequestMap.get(billId) || Promise.resolve())
    return
  }

  detailLoadingMap.value[billId] = true
  const request = (async () => {
    try {
      const res: any = await fetchDetailListApi(billId)
      setBillDetails(billId, (res.data || []) as BillDetailRow[])
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
      detailLoadingMap.value[billId] = false
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
  detailType: DETAIL_TYPE_VALUE.EXPENSE,
  remark: ''
})
const detailRules = {
  detailDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }],
  detailType: [{ required: true, message: '请选择交易类型', trigger: 'change' }]
}

function openAddDetail(row: BillRow) {
  currentBillRow.value = row
  Object.assign(detailForm, { id: undefined, billId: row.id, detailDate: currentDateString(), description: '', amount: 0, detailType: DETAIL_TYPE_VALUE.EXPENSE, remark: '' })
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
      await loadDetails(currentBillRow.value.id, { force: true })
    }
    await refreshBillDataAfterDetailChange()
  } catch (error) {
    handleError(error, '保存明细')
  } finally {
    detailSaving.value = false
  }
}

async function handleDeleteDetail(id: number) {
  try {
    await deleteDetailApi(id)
    ElMessage.success('删除成功')
    if (currentBillRow.value) {
      await loadDetails(currentBillRow.value.id, { force: true })
    }
    await refreshBillDataAfterDetailChange()
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
    await loadDetails(billId, { force: true })
    await refreshBillDataAfterDetailChange()
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
    await loadDetails(billId, { force: true })
    await refreshBillDataAfterDetailChange()
  } catch (error) {
    handleError(error, '批量修改交易类型')
  }
}

async function refreshBillDataAfterDetailChange() {
  await loadData()
  clearBillSelection()
  if (currentExpandedRow.value?.id) {
    currentExpandedRow.value = sortedList.value.find(item => Number(item.id) === Number(currentExpandedRow.value?.id)) || currentExpandedRow.value
  }
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
    feePaid: form.feePaid,
    verified: form.verified,
    expenseVerified: Boolean(row.expenseVerified),
    posCostAmount: toNumber(form.posCostAmount),
    status: row.status,
    remark: row.remark || '',
    ...overrides
  }
}

async function handleExpenseVerifiedChange(row: BillRow, expenseVerified: boolean) {
  const form = ensureEditForm(row)
  const previous = Boolean(row.expenseVerified)
  row.expenseVerified = expenseVerified
  savingId.value = row.id
  try {
    await updateBillApi(buildBillUpdatePayload(row, form, { expenseVerified }))
    row.expenseVerified = expenseVerified
    ElMessage.success(expenseVerified ? '支出明细已标记为已核实' : '支出明细已标记为未核实')
  } catch (error) {
    row.expenseVerified = previous
    handleError(error, '更新支出明细核实状态')
  } finally {
    savingId.value = null
  }
}

async function handleInlineSave(row: BillRow) {
  const form = editFormMap.value[row.id]
  if (!form) return
  savingId.value = row.id
  let saved = false
  try {
    await updateBillApi(buildBillUpdatePayload(row, form))

    ElMessage.success(`保存成功，净利润 ¥${form.netProfit}`)
    handleSearch()
    saved = true
  } catch (error) {
    handleError(error, '保存账单')
  } finally {
    savingId.value = null
  }
  return saved
}

function openBillEdit(row: BillRow) {
  billEditRow.value = row
  ensureEditForm(row)
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

function buildNetProfit(amount: number, feeRate: number | null | undefined, posCostAmount: number) {
  return Number((buildFeeAmount(amount, feeRate) - posCostAmount).toFixed(2))
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
  billMonthRange,
  () => {
    if (syncingBillFilters) return
    syncBillMonthQuery()
  },
  { flush: 'sync' }
)

watch(
  () => [query.ownerName, query.cardName, query.status, query.feePaid, query.startBillMonth, query.endBillMonth],
  () => {
    if (syncingBillFilters) return
    triggerBillSearch()
  },
  { flush: 'sync' }
)

watch(
  () => [route.query.cardId, route.query.ownerId, route.query.status, route.query.year, route.query.billMonth, route.query.startBillMonth, route.query.endBillMonth],
  ([cardId, ownerId, status, year, billMonth, startBillMonth, endBillMonth]) => {
    triggerBillSearch.cancel()
    const previousSyncState = syncingBillFilters
    syncingBillFilters = true
    const routeCardId = toRouteNumber(cardId)
    const routeOwnerId = toRouteNumber(ownerId)
    let [routeStartBillMonth, routeEndBillMonth] = normalizeRouteBillRange(startBillMonth, endBillMonth, year, billMonth)
    if (routeCardId && !routeOwnerId && !routeStartBillMonth && !routeEndBillMonth) {
      routeStartBillMonth = `${currentYear}-01`
      routeEndBillMonth = `${currentYear}-12`
    }
    query.cardId = routeCardId as any
    query.ownerId = routeOwnerId as any
    query.status = toRouteBillStatus(status) as any
    applyBillMonthRange(routeStartBillMonth, routeEndBillMonth)
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
  padding: 6px 8px;
}

.bill-search-panel .app-search-main {
  gap: 6px;
}

.bill-search-panel .app-search-extra,
.bill-search-panel .app-search-actions {
  gap: 6px;
}

.bill-search-panel .app-search-title {
  min-height: 28px;
  font-size: 11px;
}

.bill-search-panel .app-search-actions {
  margin-left: auto;
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
  transition: all 0.2s ease;
  box-shadow: 0 6px 16px rgba(15,23,42,0.05);
}

.menu-item:hover {
  transform: translateY(-1px);
  border-color: rgba(180, 206, 255, 0.92);
  box-shadow: 0 10px 18px rgba(15,23,42,0.07);
}

.menu-item.active {
  transform: translateY(-1px);
  border-color: transparent;
  background: linear-gradient(180deg, rgba(255,255,255,0.99) 0%, rgba(234,242,255,0.95) 160%);
  color: #0958d9;
  box-shadow: inset 0 0 0 1.5px rgba(180, 206, 255, 0.85), 0 12px 22px rgba(15,23,42,0.08);
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
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 4px 6px !important;
  background: #fff;
  border: 1px solid #e5eaf1;
  border-radius: 10px;
  box-shadow: none !important;
  overflow: hidden;
}

.bill-page-table.single-card-annual-table {
  flex: 1;
  min-height: 0;
  overflow: hidden;
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
  height: auto;
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
  overflow-y: hidden !important;
}

/*noinspection CssUnusedSymbol*/
.bill-page-table.bill-page-table-scrollable :deep(.el-table__body-wrapper),
.bill-page-table.bill-page-table-scrollable :deep(.el-scrollbar__wrap) {
  overflow-x: hidden !important;
  overflow-y: auto !important;
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
  white-space: normal;
  overflow: visible;
  text-overflow: clip;
  word-break: break-word;
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
}

.detail-section {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5eaf1;
  overflow: hidden;
  box-shadow: none;
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
}

.detail-pane {
  min-width: 0;
  border: 1px solid #e5eaf1;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.detail-lite-list {
  display: flex;
  flex-direction: column;
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
