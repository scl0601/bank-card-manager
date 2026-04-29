    <template>
  <div class="cards-page" tabindex="0">
    <div class="page-header">
      <div class="header-left">
        <span class="page-title-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <rect x="3" y="5" width="18" height="14" rx="2" />
            <line x1="3" y1="10" x2="21" y2="10" />
            <line x1="7" y1="15" x2="9.5" y2="15" />
          </svg>
        </span>

        <div class="header-title-group">
          <h1 class="page-title">{{ uiText.cardManagement }}</h1>
        </div>
      </div>

      <div class="header-actions">
        <button class="icon-btn" @click="refreshAll" title="刷新">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12a9 9 0 1 1-2.64-6.36" />
            <polyline points="21 3 21 9 15 9" />
          </svg>
        </button>

        <el-button type="primary" @click="openAddCard()">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="margin-right:5px">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          新增银行卡
        </el-button>
      </div>
    </div>

    <div class="main-body">
      <div class="cards-grid">
        <!-- 持卡人信息 -->
        <section class="panel is-users" v-loading="groupsVisibleLoading">
          <div class="panel-head">
            <div class="panel-title"><span class="panel-dot"></span>{{ uiText.talkerInfo }}</div>
            <div class="panel-actions">
              <div class="user-search" :class="{ focused: userSearchFocused }">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  class="search-icon"
                >
                  <circle cx="11" cy="11" r="8" />
                  <line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
                <input
                  v-model="userKeyword"
                  :placeholder="uiText.searchTalkerPlaceholder"
                  @focus="userSearchFocused = true"
                  @blur="userSearchFocused = false"
                />
                <button v-if="userKeyword" class="search-clear" @click.stop="clearUserKeyword">&times;</button>
              </div>

              <button class="icon-btn" @click="goUsers" :title="uiText.manageUserInfo">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
              </button>
            </div>
          </div>

          <div class="panel-body">
            <div class="mini-stats">
              <div class="mini-stat">
                <span class="ms-label">{{ uiText.talker }}</span>
                <span class="ms-value">{{ baseStats.userCount }}</span>
              </div>
              <div class="mini-stat">
                <span class="ms-label">{{ uiText.cards }}</span>
                <span class="ms-value">{{ baseStats.cardCount }}</span>
              </div>
              <div class="mini-stat">
                <span class="ms-label">{{ cardQuotaSummaryLabel }}</span>
                <span class="ms-value font-mono">{{ formatMoneySafe(baseStats.totalQuota) }}</span>
              </div>
            </div>

            <div class="talker-board">
              <div class="child-head talker-board-head">
                <div class="child-title">
                  <span>{{ uiText.talker }}</span>
                  <span v-if="baseStats.userCount" class="child-count">{{ baseStats.userCount }}</span>
                </div>

                <div class="panel-actions">
                  <div v-if="userPageCount > 1" class="pager">
                    <button class="icon-btn" @click.stop="prevUserPage" :title="uiText.prevPage">
                      <svg
                        width="14"
                        height="14"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2.2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <polyline points="15 18 9 12 15 6" />
                      </svg>
                    </button>
                    <span class="panel-meta">{{ userPageIndex + 1 }}/{{ userPageCount }}</span>
                    <button class="icon-btn" @click.stop="nextUserPage" :title="uiText.nextPage">
                      <svg
                        width="14"
                        height="14"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2.2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      >
                        <polyline points="9 18 15 12 9 6" />
                      </svg>
                    </button>
                  </div>
                </div>
              </div>

              <div class="user-list talker-list">
                <template v-for="(u, idx) in pagedUsersFilled" :key="u?.userId ?? `u-ph-${idx}`">
                  <div
                    v-if="u"
                    :class="['list-item', 'user-item', 'talker-card', { active: u.userId === activeUserId }]"
                    role="button"
                    tabindex="0"
                    @click="setActiveUser(u.userId)"
                    @keydown.enter.prevent="setActiveUser(u.userId)"
                    @keydown.space.prevent="setActiveUser(u.userId)"
                  >
                    <div class="talker-rate-tag" :class="{ 'has-rate': (u.feeRate ?? 0) > 0 }">
                      <span class="rate-prefix">费率</span>{{ formatRate(u.feeRate) }}%
                    </div>
                    <div class="talker-main">
                      <div class="li-avatar" :class="{ ghost: u.userId === 0 }">
                        <span>{{ userInitial(u.userName) }}</span>
                      </div>
                      <div class="li-main">
                        <div class="li-title">{{ u.userName || uiText.unnamed }}</div>
                        <div class="li-sub">
                          <span>{{ u.phone ? maskPhone(u.phone) : uiText.unfilled }}</span>
                        </div>
                      </div>
                    </div>
                    <div class="talker-footer">
                      <div class="talker-stat-item">
                        <span class="ts-label">{{ uiText.cards }}</span>
                        <span class="ts-value">{{ formatCountLabel(u.cardCount) }}</span>
                      </div>
                      <div class="talker-stat-divider"></div>
                      <div class="talker-stat-item talker-stat-amount">
                        <span class="ts-label">{{ uiText.totalAmount }}</span>
                        <span class="ts-value font-mono">{{ formatMoneySafe(userTotalQuota(u)) }}</span>
                      </div>
                    </div>
                  </div>

                  <div v-else class="list-item talker-card placeholder" aria-hidden="true"></div>
                </template>

                <div v-if="groupsReady && !loadingGroups && filteredGroupList.length === 0" class="empty-hint">{{ uiText.emptyTalkers }}</div>
              </div>
            </div>
          </div>
        </section>

        <!-- 银行卡信息 -->
        <section class="panel is-card-info">
          <div class="panel-head">
            <div class="card-title-with-stats">
              <div class="panel-title"><span class="panel-dot is-primary"></span>{{ uiText.bankCardInfo }}</div>
              <div class="card-type-stats" v-if="activeUser && activeCards.length">
                <span class="cts-item cts-credit">
                  <i class="cts-dot credit"></i>信用卡 <b>{{ activeCardTypeStats.creditCount }}</b>
                </span>
                <span class="cts-divider"></span>
                <span class="cts-item cts-debit">
                  <i class="cts-dot debit"></i>借记卡 <b>{{ activeCardTypeStats.debitCount }}</b>
                </span>
              </div>
            </div>
            <div class="panel-actions">
              <div class="card-filters">
                <div class="panel-search" :class="{ focused: searchFocused }">
                  <svg
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2.5"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    class="search-icon"
                  >
                    <circle cx="11" cy="11" r="8" />
                    <line x1="21" y1="21" x2="16.65" y2="16.65" />
                  </svg>
                  <input
                    v-model="keyword"
                    :placeholder="uiText.searchBankCardPlaceholder"
                    @focus="searchFocused = true"
                    @blur="searchFocused = false"
                  />
                  <button v-if="keyword" class="search-clear" @click="clearKeyword">&times;</button>
                </div>
                <el-select v-model="query.cardType" class="mini-filter card-filter" :placeholder="uiText.cardType" clearable>
                  <el-option v-for="item in CARD_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
                <el-select v-model="query.status" class="mini-filter card-filter" :placeholder="uiText.status" clearable>
                  <el-option v-for="item in CARD_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </div>
              <div v-if="cardPageCount > 1" class="pager">
                <button class="icon-btn" @click.stop="prevCardPage" title="上一页">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="15 18 9 12 15 6" />
                  </svg>
                </button>
                <span class="panel-meta">{{ cardPageIndex + 1 }}/{{ cardPageCount }}</span>
                <button class="icon-btn" @click.stop="nextCardPage" title="下一页">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="9 18 15 12 9 6" />
                  </svg>
                </button>
              </div>
              <button class="icon-btn" @click="openAddCardWithActiveUser" :disabled="!canAddCardForUser" title="添加卡片">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="12" y1="5" x2="12" y2="19" />
                  <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
              </button>
              <button class="icon-btn" @click="openUserBillsPage" :disabled="!canOpenUserBills" title="当前用户全部账单">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                  <polyline points="14 2 14 8 20 8" />
                  <line x1="16" y1="13" x2="8" y2="13" />
                  <line x1="16" y1="17" x2="8" y2="17" />
                </svg>
              </button>
            </div>
          </div>

          <div class="panel-body card-panel-body">
            <div class="card-list">
              <template v-if="pagedCardsFilled.length">
                <template v-for="(c, idx) in pagedCardsFilled" :key="c?.id ?? `card-ph-${idx}`">
                <div
                  v-if="c"
                  :class="['list-item', 'card-item', { active: c.id === activeCardId }]"
                  role="button"
                  tabindex="0"
                  @click="setActiveCard(c.id)"
                  @keydown.enter.prevent="setActiveCard(c.id)"
                  @keydown.space.prevent="setActiveCard(c.id)"
                >
                  <div class="li-left card-info-left">
                    <div class="li-icon" :class="{ credit: c.cardType === CARD_TYPE_VALUE.CREDIT }">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="3" y="5" width="18" height="14" rx="2" />
                        <line x1="3" y1="10" x2="21" y2="10" />
                      </svg>
                    </div>
                    <div class="li-main">
                      <div class="card-row-top">
                        <span class="cig-name">{{ cardUserLabel(c) }}</span>
                        <i class="cig-sep"></i>
                        <span class="cig-bank">{{ c.bankName || '—' }}</span>
                        <i class="cig-sep"></i>
                        <span class="cig-last4 font-mono">{{ cardLast4Label(c) }}</span>
                      </div>
                      <div class="card-row-sub">
                        <span class="cig-type">{{ CARD_TYPE_MAP[c.cardType] || '—' }}</span>
                        <span class="cig-sep-dot"></span>
                        <span class="cig-label">账单日</span>
                        <span :class="['cig-date', { 'cig-empty': !c.billDay }]">{{ c.billDay ? `${c.billDay}日` : '—' }}</span>
                        <span class="cig-sep-dot"></span>
                        <span class="cig-label">还款日</span>
                        <span :class="['cig-date', { 'cig-empty': !c.repayDay }]">{{ c.repayDay ? `${c.repayDay}日` : '—' }}</span>
                        <span class="cig-sep-dot"></span>
                        <span class="cig-label">有效期</span>
                        <span :class="['cig-date', { 'cig-empty': !c.expireDate }]">{{ c.expireDate || '—' }}</span>
                      </div>
                    </div>
                  </div>

                  <div class="li-right card-info-right">
                    <div class="amt">
                      <span class="amt-label">卡片额度</span>
                      <span class="amt-value font-mono">{{ formatMoneySafe(cardDisplayAmount(c)) }}</span>
                    </div>
                    <div class="li-actions">
                      <button class="mini-icon" @click.stop="openCardBillsPage(c)" title="详情">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                          <polyline points="14 2 14 8 20 8" />
                          <line x1="16" y1="13" x2="8" y2="13" />
                          <line x1="16" y1="17" x2="8" y2="17" />
                        </svg>
                      </button>
                      <button class="mini-icon" @click.stop="openEditCard(c)" title="编辑">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M12 20h9" />
                          <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" />
                        </svg>
                      </button>
                      <button class="mini-icon danger" @click.stop="confirmDeleteCard(c)" title="删除">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                          <polyline points="3 6 5 6 21 6" />
                          <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
                          <path d="M10 11v6" />
                          <path d="M14 11v6" />
                          <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
                        </svg>
                      </button>
                    </div>
                  </div>
                </div>
                <div v-else class="list-item card-item placeholder" aria-hidden="true"></div>
                </template>
              </template>

              <div v-else class="empty-hint">
                <div v-if="!activeUser">暂无持卡人可选</div>
                <div v-else>当前持卡人暂无银行卡</div>
                <button class="link-btn" @click="openAddCardWithActiveUser" :disabled="!canAddCardForUser">+ 添加一张银行卡</button>
              </div>
            </div>

          </div>
        </section>

        <!-- 账单明细 -->
        <section class="panel">
          <div class="panel-head">
            <div class="panel-title"><span class="panel-dot is-warning"></span>账单明细</div>
            <div class="panel-actions">
              <el-select v-model="billFilter.year" class="mini-filter year-filter" placeholder="年份">
                <el-option v-for="year in yearOptions" :key="year" :label="`${year}年`" :value="year" />
              </el-select>
              <el-select v-model="billFilter.month" class="mini-filter month-filter" placeholder="全年" clearable>
                <el-option v-for="month in monthOptions" :key="month" :label="`${month}月`" :value="month" />
              </el-select>
              <button class="icon-btn" @click="openFilteredBillsPage" :disabled="!canOpenScopedBills" title="账单详情">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="9 18 15 12 9 6" />
                </svg>
              </button>
            </div>
          </div>
          <div class="panel-body">
            <div class="mini-stats mini-stats-4" v-loading="billOverviewVisibleLoading">
              <div class="mini-stat">
                <span class="ms-label">账单数</span>
                <span class="ms-value">{{ billOverview.billCount }}</span>
              </div>
              <div class="mini-stat">
                <span class="ms-label">待还</span>
                <span class="ms-value warn">{{ billOverview.pendingCount }}</span>
              </div>
              <div class="mini-stat">
                <span class="ms-label">逾期</span>
                <span class="ms-value danger">{{ billOverview.overdueCount }}</span>
              </div>
              <div class="mini-stat">
                <span class="ms-label">账单总金额</span>
                <span class="ms-value font-mono">¥{{ formatMoneySafe(billOverview.totalBillAmount) }}</span>
              </div>
            </div>
            <div class="bill-list" v-loading="recentBillsVisibleLoading">
              <template v-if="recentBills.length">
                <div v-for="b in recentBills" :key="b.id" class="bill-item" :class="{ current: b.billMonth === currentBillMonth }">
                  <div class="bill-owner" :title="b.ownerName || '未命名'">
                    {{ b.ownerName || '未命名' }}
                  </div>
                  <div class="bill-bank" :title="displayBankName(b.bankName)">
                    {{ displayBankName(b.bankName) }}
                  </div>
                  <div class="bill-last4 font-mono">尾号{{ b.cardNoLast4 || '-' }}</div>
                  <div class="bill-repay">还款日 {{ fmtRepayDay(b.repayDate) }}</div>
                  <div class="bill-amount-edit">
                    <span class="bill-amount-symbol">¥</span>
                    <el-input-number
                      :model-value="billAmountDraftValue(b)"
                      :min="0"
                      :precision="2"
                      :controls="false"
                      size="small"
                      class="bill-amount-input"
                      placeholder="账单金额"
                      :disabled="savingBillId === b.id"
                      @update:model-value="(val: any) => updateBillAmountDraft(b.id, val)"
                      @keyup.enter="saveBillAmount(b)"
                    />
                    <el-button
                      type="primary"
                      link
                      size="small"
                      class="bill-save-btn"
                      :loading="savingBillId === b.id"
                      :disabled="!isBillAmountChanged(b)"
                      @click="saveBillAmount(b)"
                    >
                      保存
                    </el-button>
                  </div>
                  <div class="bill-status">
                    <StatusTag :value="b.status" :label-map="BILL_STATUS_MAP" :type-map="BILL_STATUS_TAG_TYPE" size="small" effect="light" />
                  </div>
                </div>
              </template>
              <div v-else-if="recentBillsReady" class="empty-hint">
                <div v-if="!scopedCardIds.length">当前持卡人暂无银行卡数据</div>
                <div v-else>暂无账单记录</div>
              </div>
            </div>
          </div>
        </section>
        <!-- 收益统计 -->
        <section class="panel">
          <div class="panel-head">
            <div class="panel-title"><span class="panel-dot is-success"></span>收益统计</div>
            <div class="panel-actions">
              <div class="scope-toggle profit-scope-toggle">
                <button type="button" :class="{ active: profitScope === 'month' }" @click="setProfitScope('month')">本月</button>
                <button type="button" :class="{ active: profitScope === 'year' }" @click="setProfitScope('year')">本年</button>
              </div>
              <button class="icon-btn" @click="goProfits" :disabled="!canOpenScopedBills" title="收益详情">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="3 17 9 11 13 15 21 7" />
                  <polyline points="14 7 21 7 21 14" />
                </svg>
              </button>
            </div>
          </div>
          <div class="panel-body profit-summary-block" v-loading="profitVisibleLoading">
            <div class="profit-net-grid">
              <div class="profit-kpi">
                <div class="pk-label">预计{{ profitScopeLabel }}净利润</div>
                <div class="pk-value" :class="Number(profitOverview.expectedNetProfit || 0) >= 0 ? 'pos' : 'neg'">
                  ¥{{ formatMoneySafe(profitOverview.expectedNetProfit) }}
                </div>
                <div class="pk-sub">应收手续费 - POS成本</div>
              </div>
              <div class="profit-kpi">
                <div class="pk-label">实际{{ profitScopeLabel }}净利润</div>
                <div class="pk-value" :class="Number(profitOverview.totalNetProfit || 0) >= 0 ? 'pos' : 'neg'">
                  ¥{{ formatMoneySafe(profitOverview.totalNetProfit) }}
                </div>
                <div class="pk-sub">已收手续费 - POS成本</div>
              </div>
            </div>
            <div class="profit-grid">
              <div class="profit-metric">
                <span class="pm-label">已收手续费</span>
                <span class="pm-value pos font-mono">¥{{ formatMoneySafe(profitOverview.paidFeeAmount) }}</span>
              </div>
              <div class="profit-metric">
                <span class="pm-label">POS成本</span>
                <span class="pm-value neg font-mono">¥{{ formatMoneySafe(profitOverview.totalPosCostAmount) }}</span>
              </div>
            </div>
            <div class="fee-pay-card">
              <div class="fee-pay-head">
                <span>手续费收款明细</span>
                <span class="fee-pay-summary">
                  已收 {{ profitOverview.paidFeeCount }} 笔 ¥{{ formatMoneySafe(profitOverview.paidFeeAmount) }}
                  / 待收 {{ profitOverview.unpaidFeeCount }} 笔 ¥{{ formatMoneySafe(profitOverview.unpaidFeeAmount) }}
                </span>
              </div>
              <div v-if="feePaymentItems.length" class="fee-pay-list">
                <div v-for="item in feePaymentItems" :key="item.key" class="fee-pay-row">
                  <div class="fee-pay-main">
                    <span class="fee-pay-title">{{ item.label }}</span>
                    <span class="fee-pay-sub">{{ item.repayText }}</span>
                  </div>
                  <div class="fee-pay-amount" :class="item.paid ? 'pos' : 'warn'">
                    {{ item.paid ? '用户已给' : '用户未给' }} ¥{{ formatMoneySafe(item.amount) }}
                  </div>
                </div>
              </div>
              <div v-else class="fee-pay-empty">暂无手续费记录</div>
            </div>
          </div>
        </section>
      </div>
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
            <el-option v-for="u in userOptions" :key="u.id" :label="u.displayName" :value="u.id">
              <div class="user-opt">
                <span>{{ u.displayName }}</span>
                <el-tag v-if="!u.parentId" size="small" effect="light" round style="margin-left:6px">主账户</el-tag>
                <el-tag v-else size="small" type="info" effect="plain" round style="margin-left:6px">子用户</el-tag>
              </div>
            </el-option>
          </el-select>
          <el-button type="primary" link size="small" style="margin-left:8px" @click="goUsers">+ 管理持卡人</el-button>
        </el-form-item>
        <el-form-item label="银行名称" prop="bankName">
          <el-input v-model="form.bankName" placeholder="如：招商银行" />
        </el-form-item>
        <el-form-item label="卡号后四位" prop="cardNoLast4">
          <el-input v-model="form.cardNoLast4" placeholder="请输入卡号后四位（4位数字）" maxlength="4" />
        </el-form-item>
        <el-form-item label="卡片类型" prop="cardType">
          <el-radio-group v-model="form.cardType">
            <el-radio v-for="item in CARD_TYPE_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="信用额度">
          <el-input v-model="form.creditLimit" placeholder="请输入信用额度" />
        </el-form-item>
        <el-form-item label="账单日">
          <el-input-number v-model="form.billDay" :min="1" :max="31" controls-position="right" style="width: 100%" placeholder="请输入每月账单日" />
        </el-form-item>
        <el-form-item label="还款日">
          <el-input-number v-model="form.repayDay" :min="1" :max="31" controls-position="right" style="width: 100%" placeholder="请输入每月还款日" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-input v-model="form.expireDate" placeholder="月/年，如：06/28、06-28、06月28年" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option v-for="item in CARD_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="APP" prop="repayMethod">
          <el-radio-group v-model="form.repayMethod">
            <el-radio v-for="item in APP_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否已核实">
          <el-switch v-model="form.verified" active-text="已核实" inactive-text="未核实" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" rows="2" maxlength="500" show-word-limit placeholder="最多输入500字" />
        </el-form-item>
      </template>
    </CrudDialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Cards' })
import { computed, onActivated, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import CrudDialog from '@/components/CrudDialog/index.vue'
import StatusTag from '@/components/StatusTag/index.vue'
import {
  getCardsGroupedByUserApi,
  saveCardApi,
  updateCardApi,
  deleteCardApi,
  getUserTreeApi
} from '@/api/card'
import { getBillPageApi, updateBillApi } from '@/api/bill'
import { formatMoney, formatRate, maskPhone } from '@/utils/formatters'
import {
  BILL_STATUS_MAP,
  BILL_STATUS_TAG_TYPE,
  CARD_TYPE_MAP,
  CARD_TYPE_OPTIONS,
  CARD_TYPE_VALUE,
  CARD_STATUS_OPTIONS,
  APP_OPTIONS
} from '@/constants/dict'

interface UserGroup {
  userId: number
  userName: string
  feeRate: number
  phone?: string
  status: number
  cardCount: number
  cards?: any[]
}

interface BillRow {
  id: number
  cardId: number
  ownerId?: number
  bankName?: string
  cardNoLast4?: string
  ownerName?: string
  billMonth: string
  billDay?: number | null
  repayDate: string | null
  repayDay?: number | null
  billAmount: number | null
  minPayAmount?: number | null
  actualPayAmount?: number | null
  actualPayDate?: string | null
  feeRate?: number | null
  feeAmount?: number | null
  feePaid?: boolean | null
  posCostAmount?: number | null
  netProfit?: number | null
  verified?: boolean | null
  expenseVerified?: boolean | null
  status: number
  remark?: string | null
}

interface BillOverview {
  billCount: number
  pendingCount: number
  overdueCount: number
  totalBillAmount: number
}

interface ProfitOverview {
  year?: number
  month?: number
  userCount: number
  cardCount: number
  totalBillAmount: number
  totalFeeAmount: number
  totalPosCostAmount: number
  expectedNetProfit: number
  totalNetProfit: number
  paidFeeAmount: number
  unpaidFeeAmount: number
  paidFeeCount: number
  unpaidFeeCount: number
}

type ProfitScope = 'month' | 'year'

interface UserTreeNode {
  id: number
  parentId: number | null
  name: string
  phone?: string
  status: number
  feeRate?: number
  effectiveFeeRate?: number
  children?: UserTreeNode[]
}

const router = useRouter()
const currentYear = new Date().getFullYear()
const currentMonth = new Date().getMonth() + 1
const currentBillMonth = `${currentYear}-${String(currentMonth).padStart(2, '0')}`
const yearOptions = Array.from({ length: 6 }, (_, index) => currentYear - 2 + index)
const monthOptions = Array.from({ length: 12 }, (_, index) => index + 1)
const uiText = {
  cardManagement: '\u5361\u52a1\u7ba1\u7406',
  bankCardInfo: '\u94f6\u884c\u5361\u4fe1\u606f',
  talkerInfo: '\u6d3d\u8c08\u4eba\u4fe1\u606f',
  talker: '\u6d3d\u8c08\u4eba',
  ownedCardholders: '\u540d\u4e0b\u6301\u5361\u4eba',
  searchTalkerPlaceholder: '\u641c\u7d22\u6d3d\u8c08\u4eba / \u624b\u673a\u53f7',
  manageUserInfo: '\u7ba1\u7406\u7528\u6237\u4fe1\u606f',
  searchBankCardPlaceholder: '\u641c\u7d22\u94f6\u884c / \u5361\u53f7\u540e\u56db\u4f4d\u2026',
  cards: '\u94f6\u884c\u5361',
  cardType: '\u5361\u7c7b\u578b',
  status: '\u72b6\u6001',
  totalQuota: '\u603b\u989d\u5ea6',
  totalAmount: '\u603b\u91d1\u989d',
  feeRate: '\u8d39\u7387',
  prevPage: '\u4e0a\u4e00\u9875',
  nextPage: '\u4e0b\u4e00\u9875',
  cardCountSuffix: '\u5f20',
  cardQuotaSummarySuffix: '\u5f20\u94f6\u884c\u5361\u603b\u91d1\u989d',
  emptyTalkers: '\u6682\u65e0\u6d3d\u8c08\u4eba',
  emptyOwnedCardholders: '\u6682\u65e0\u540d\u4e0b\u6301\u5361\u4eba',
  selectTalkerHint: '\u9009\u62e9\u6d3d\u8c08\u4eba\u67e5\u770b\u540d\u4e0b\u6301\u5361\u4eba',
  unnamed: '\u672a\u547d\u540d',
  unfilled: '\u672a\u586b\u5199'
} as const

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

// ====== 顶部筛选 ======
const keyword = ref('')
const searchFocused = ref(false)
const query = reactive({
  bankName: '',
  cardNoLast4: '',
  cardType: undefined as number | undefined,
  status: undefined as number | undefined
})

// ====== 列表数据 ======
const loadingGroups = ref(false)
const groupList = ref<UserGroup[]>([])
const groupsReady = ref(false)
const activeUserId = ref<number | null>(null)
const activeCardId = ref<number | null>(null)
const activeOwnerId = ref<number | null>(null)
const hasActivatedOnce = ref(false)
const groupsFetchedAt = ref(0)
const userTreeFetchedAt = ref(0)
const VIEW_CACHE_TTL = 5 * 1000

let groupsLoadingPromise: Promise<void> | null = null
let groupsLoadingKey = ''
let groupsRequestSeq = 0
const groupsVisibleLoading = computed(() => loadingGroups.value && !groupsReady.value)
const triggerGroupSearch = createDebouncedTask(() => {
  fetchGroups({ silent: true })
}, 300)
let syncingKeywordQuery = false

const userPageIndex = ref(0)
const cardPageIndex = ref(0)
const childPageIndex = ref(0)

function toAmount(value: any) {
  const num = Number(value ?? 0)
  return Number.isFinite(num) ? num : 0
}

function cardQuotaValue(card: any) {
  if (!card) return 0
  return toAmount(card.creditLimit)
}

function sumCardQuota(cards: any[]) {
  return (cards || []).reduce((sum, card) => sum + cardQuotaValue(card), 0)
}

function cardDisplayAmount(card: any) {
  return cardQuotaValue(card)
}

const baseStats = computed(() => {
  const users = filteredGroupList.value.filter(item => Number(item?.userId || 0) > 0)
  const visibleCards = users.flatMap(item => Array.isArray(item?.cards) ? item.cards : [])
  return {
    userCount: users.length,
    cardCount: visibleCards.length,
    totalQuota: sumCardQuota(visibleCards)
  }
})

const cardQuotaSummaryLabel = computed(() => uiText.totalAmount)

const activeUser = computed<UserGroup | undefined>(() => {
  if (!groupList.value.length) return undefined
  const found = groupList.value.find(u => u.userId === activeUserId.value)
  return found || groupList.value[0]
})

const activeUserCards = computed<any[]>(() => ((activeUser.value?.cards || []) as any[]))

const activeCards = computed<any[]>(() => {
  return activeUserCards.value
})

const activeCard = computed<any | undefined>(() => {
  if (!activeCards.value.length) return undefined
  const found = activeCards.value.find(c => Number(c.id) === Number(activeCardId.value))
  return found || activeCards.value[0]
})


const activeCardsTotalQuota = computed(() => sumCardQuota(activeCards.value))

const activeCardTypeStats = computed(() => {
  const cards = activeCards.value || []
  const creditCount = cards.filter((c: any) => Number(c?.cardType) === CARD_TYPE_VALUE.CREDIT).length
  const debitCount = cards.filter((c: any) => Number(c?.cardType) === CARD_TYPE_VALUE.DEBIT).length
  return { creditCount, debitCount }
})

// ====== 持卡人搜索（仅影响左侧列表展示，不请求后端） ======
const userKeyword = ref('')
const userSearchFocused = ref(false)

const filteredGroupList = computed(() => {
  const kw = userKeyword.value.trim()
  const list = groupList.value || []
  if (!kw) return list

  const k = kw.toLowerCase()
  const digits = kw.replace(/\D/g, '')

  function isMatchNamePhone(u: any) {
    const name = String(u?.userName || '').toLowerCase()
    const phone = String(u?.phone || '')
    const phoneDigits = phone.replace(/\D/g, '')
    return name.includes(k)
      || phone.toLowerCase().includes(k)
      || Boolean(digits && phoneDigits.includes(digits))
  }

  function isMatchChildNamePhone(c: any) {
    const name = String(c?.name || '').toLowerCase()
    const phone = String(c?.phone || '')
    const phoneDigits = phone.replace(/\D/g, '')
    return name.includes(k)
      || phone.toLowerCase().includes(k)
      || Boolean(digits && phoneDigits.includes(digits))
  }

  const matchedMainIds = new Set<number>()

  // 1) 命中的主用户（来自卡分组列表）
  for (const u of list) {
    if (!u) continue
    if (isMatchNamePhone(u)) matchedMainIds.add(Number(u.userId))
  }

  // 2) 命中的子用户（来自用户树 children）
  const matchedChildIds = new Set<number>()
  for (const node of userTreeList.value || []) {
    const children = (node?.children || []) as any[]
    for (const c of children) {
      if (!c) continue
      if (isMatchChildNamePhone(c)) {
        matchedChildIds.add(Number(c.id))
      }
    }
  }

  // 3) 命中子用户时，加入其主用户 id
  const mainTopIds = new Set<number>()
  for (const node of userTreeList.value || []) {
    const topId = Number(node?.id)
    const children = (node?.children || []) as any[]
    for (const c of children) {
      if (!c) continue
      const childId = Number(c.id)
      if (matchedChildIds.has(childId)) mainTopIds.add(topId)
    }
  }

  // 将 userTree 的主用户映射为 groupList 的 userId（两边 id 可能不同）
  const mainGroupIds = new Set<number>()
  for (const node of userTreeList.value || []) {
    const topId = Number(node?.id)
    if (!mainTopIds.has(topId)) continue

    const group = list.find(x => String(x?.userName || '') === String(node?.name || ''))
    if (group) mainGroupIds.add(Number(group.userId))
  }

  // 4) 最终：命中主用户 or 子用户命中的主用户
  return list.filter(u => {
    const id = Number(u?.userId)
    if (!Number.isFinite(id)) return false
    return matchedMainIds.has(id) || mainGroupIds.has(id)
  })
})

function clearUserKeyword() {
  userKeyword.value = ''
}

// ====== 子用户信息（来自用户树） ======
const userTreeLoading = ref(false)
const userTreeLoaded = ref(false)
const topToChildrenMap = ref<Map<number, UserTreeNode[]>>(new Map())
let userTreeLoadingPromise: Promise<void> | null = null
const userTreeVisibleLoading = computed(() => userTreeLoading.value && !userTreeLoaded.value && activeChildUsers.value.length === 0)

async function ensureUserTree(force = false, silent = false) {
  if (userTreeLoadingPromise) return userTreeLoadingPromise
  if (!force && userTreeLoaded.value) return

  const showLoading = !silent
  if (showLoading) userTreeLoading.value = true

  userTreeLoadingPromise = (async () => {
    try {
      const res: any = await getUserTreeApi()
      const list = (res.data || []) as UserTreeNode[]
      const childrenMap = new Map<number, UserTreeNode[]>()

      for (const top of list) {
        if (!top) continue
        const children = (top.children || []).map(c => ({ ...c, parentId: Number(top.id) }))
        childrenMap.set(Number(top.id), children)
      }

      topToChildrenMap.value = childrenMap
      userTreeLoaded.value = true
      userTreeFetchedAt.value = Date.now()
      userTreeList.value = list
    } catch {
      topToChildrenMap.value = new Map()
      userTreeLoaded.value = false
      userTreeList.value = []
    } finally {
      if (showLoading) userTreeLoading.value = false
      userTreeLoadingPromise = null
    }
  })()

  return userTreeLoadingPromise
}

const activeChildUsers = computed<UserTreeNode[]>(() => {
  const id = Number(activeUserId.value || 0)
  if (!id) return []
  const list = (topToChildrenMap.value.get(id) || []) as UserTreeNode[]
  const kw = userKeyword.value.trim()
  if (!kw) return list

  const k = kw.toLowerCase()
  const digits = kw.replace(/\D/g, '')
  return list.filter(cu => {
    const name = String(cu?.name || '').toLowerCase()
    const phone = String(cu?.phone || '')
    const phoneDigits = phone.replace(/\D/g, '')
    return name.includes(k)
      || phone.toLowerCase().includes(k)
      || Boolean(digits && phoneDigits.includes(digits))
  })
})

const childTotal = computed(() => activeChildUsers.value.length)

// 子用户区域在单屏内更容易触顶裁切，这里默认 3 条/页更稳妥
const CHILD_PAGE_SIZE = 3
const childPageCount = computed(() => {
  const total = activeChildUsers.value.length
  return Math.max(1, Math.ceil(total / CHILD_PAGE_SIZE))
})

const pagedChildUsers = computed(() => {
  const list = activeChildUsers.value || []
  const start = childPageIndex.value * CHILD_PAGE_SIZE
  return list.slice(start, start + CHILD_PAGE_SIZE)
})

const pagedChildUsersFilled = computed<(UserTreeNode | null)[]>(() => {
  if (!activeUserId.value || Number(activeUserId.value) <= 0) return []
  if (!activeChildUsers.value.length) return []
  const list = [...pagedChildUsers.value] as (UserTreeNode | null)[]
  while (list.length < CHILD_PAGE_SIZE) list.push(null)
  return list
})

function prevChildPage() {
  childPageIndex.value = (childPageIndex.value - 1 + childPageCount.value) % childPageCount.value
}

function nextChildPage() {
  childPageIndex.value = (childPageIndex.value + 1) % childPageCount.value
}

function childFeeRate(child: any) {
  const v = child?.effectiveFeeRate ?? child?.feeRate
  if (v === null || v === undefined) return Number(activeUser.value?.feeRate ?? 0)
  return Number(v)
}

function userTotalQuota(user: UserGroup | null | undefined) {
  return sumCardQuota(Array.isArray(user?.cards) ? user.cards : [])
}

function childCards(childId: number) {
  return activeUserCards.value.filter((c: any) => Number(c.userId) === Number(childId))
}

function childCardCount(childId: number) {
  return childCards(childId).length
}

function childTotalQuota(childId: number) {
  return sumCardQuota(childCards(childId))
}

const canAddCardForUser = computed(() => {
  return !!activeUser.value && Number(activeUser.value.userId) > 0 && Number(activeUser.value.status) === 0
})

const billScopeOwnerId = computed(() => {
  const ownerId = activeUser.value?.userId
  const normalized = Number(ownerId || 0)
  return Number.isFinite(normalized) && normalized > 0 ? normalized : undefined
})

const canOpenUserBills = computed(() => !!billScopeOwnerId.value)

// 洽谈人区域按三列三行展示，与顶部三格统计保持一致
const USER_PAGE_SIZE = 9
const CARD_PAGE_SIZE = 5

const userPageCount = computed(() => {
  const total = filteredGroupList.value.length
  return Math.max(1, Math.ceil(total / USER_PAGE_SIZE))
})

const cardPageCount = computed(() => {
  const total = activeCards.value.length
  return Math.max(1, Math.ceil(total / CARD_PAGE_SIZE))
})

const pagedUsers = computed(() => {
  const list = filteredGroupList.value || []
  const start = userPageIndex.value * USER_PAGE_SIZE
  return list.slice(start, start + USER_PAGE_SIZE)
})

const pagedUsersFilled = computed<(UserGroup | null)[]>(() => {
  if (!filteredGroupList.value.length) return []
  const list = [...pagedUsers.value] as (UserGroup | null)[]
  while (list.length < USER_PAGE_SIZE) list.push(null)
  return list
})

const pagedCards = computed(() => {
  const list = activeCards.value || []
  const start = cardPageIndex.value * CARD_PAGE_SIZE
  return list.slice(start, start + CARD_PAGE_SIZE)
})

const pagedCardsFilled = computed<(any | null)[]>(() => {
  if (!activeCards.value.length) return []
  const list = [...pagedCards.value] as (any | null)[]
  while (list.length < CARD_PAGE_SIZE) list.push(null)
  return list
})

function prevUserPage() {
  userPageIndex.value = (userPageIndex.value - 1 + userPageCount.value) % userPageCount.value
}

function nextUserPage() {
  userPageIndex.value = (userPageIndex.value + 1) % userPageCount.value
}

function prevCardPage() {
  cardPageIndex.value = (cardPageIndex.value - 1 + cardPageCount.value) % cardPageCount.value
}

function nextCardPage() {
  cardPageIndex.value = (cardPageIndex.value + 1) % cardPageCount.value
}

function userInitial(name: string | undefined) {
  const v = String(name || '').trim()
  return v ? v.slice(0, 1) : 'U'
}

function formatMoneySafe(val: any) {
  return formatMoney(val)
}

function formatCountLabel(count: any) {
  return `${Number(count || 0)}${uiText.cardCountSuffix}`
}

function cardUserLabel(card: any) {
  const userName = String(card?.userName || '').trim()
  if (userName) return userName
  return uiText.unfilled
}

function cardLast4Label(card: any) {
  const last4 = String(card?.cardNoLast4 || '').trim()
  return last4 || '-'
}

function applyKeyword() {
  const v = keyword.value.trim()
  triggerGroupSearch.cancel()
  syncingKeywordQuery = true
  if (!v) {
    query.bankName = ''
    query.cardNoLast4 = ''
    syncingKeywordQuery = false
    fetchGroups()
    return
  }
  if (/^\d{1,4}$/.test(v)) {
    query.cardNoLast4 = v
    query.bankName = ''
  } else {
    query.bankName = v
    query.cardNoLast4 = ''
  }
  syncingKeywordQuery = false
  triggerGroupSearch()
}

function clearKeyword() {
  triggerGroupSearch.cancel()
  syncingKeywordQuery = true
  keyword.value = ''
  query.bankName = ''
  query.cardNoLast4 = ''
  syncingKeywordQuery = false
  fetchGroups()
}

function setActiveUser(userId: number) {
  activeUserId.value = userId
  activeOwnerId.value = null
  cardPageIndex.value = 0
  childPageIndex.value = 0
  ensureUserTree(false, true)
}

function setActiveChildUser(cu: any) {
  if (!cu) return
  if (Number(cu.status) !== 0) return
  activeOwnerId.value = Number(cu.id)
  cardPageIndex.value = 0
}

function setActiveCard(cardId: number) {
  activeCardId.value = cardId
}

function syncActiveSelection() {
  if (!groupList.value.length) {
    activeUserId.value = null
    activeCardId.value = null
    return
  }

  if (!activeUserId.value || !groupList.value.some(u => u.userId === activeUserId.value)) {
    const first = groupList.value.find(u => u.userId !== 0) || groupList.value[0]
    activeUserId.value = first?.userId ?? null
  }

  // 如果选中的持卡人不在当前页，则将分页定位到该项所在页
  const userIdx = filteredGroupList.value.findIndex(u => u.userId === activeUserId.value)
  if (userIdx >= 0) {
    userPageIndex.value = Math.floor(userIdx / USER_PAGE_SIZE)
  }

  const cards = (groupList.value.find(u => u.userId === activeUserId.value)?.cards || []) as any[]
  if (!cards.length) {
    activeCardId.value = null
    return
  }
  if (!activeCardId.value || !cards.some(c => Number(c.id) === Number(activeCardId.value))) {
    activeCardId.value = Number(cards[0].id)
  }

  // 同步卡片分页
  const cardIdx = cards.findIndex(c => Number(c.id) === Number(activeCardId.value))
  if (cardIdx >= 0) {
    cardPageIndex.value = Math.floor(cardIdx / CARD_PAGE_SIZE)
  }
}

async function fetchGroups(options: { silent?: boolean } = {}) {
  const params = { ...query }
  const requestSeq = ++groupsRequestSeq
  const showLoading = !options.silent && !groupsReady.value
  groupsLoadingKey = JSON.stringify(params)
  if (showLoading) loadingGroups.value = true

  try {
    const res: any = await getCardsGroupedByUserApi(params)
    if (requestSeq !== groupsRequestSeq) return
    groupList.value = res.data || []
    groupsReady.value = true
    groupsFetchedAt.value = Date.now()
    syncActiveSelection()
  } finally {
    if (requestSeq === groupsRequestSeq) {
      loadingGroups.value = false
      groupsLoadingPromise = null
      groupsLoadingKey = ''
    }
  }
}

function refreshAll() {
  fetchGroups()
  ensureUserTree(true)
}

// ====== 账单模块 ======
const billFilter = reactive({
  year: currentYear,
  month: currentMonth as number | undefined
})
const billOverviewLoading = ref(false)
const billOverviewReady = ref(false)
const billOverview = ref<BillOverview>({
  billCount: 0,
  pendingCount: 0,
  overdueCount: 0,
  totalBillAmount: 0
})

const recentBillsLoading = ref(false)
const recentBillsReady = ref(false)
const recentBills = ref<BillRow[]>([])
const billAmountDraftMap = ref<Record<number, number>>({})
const savingBillId = ref<number | null>(null)
let billScopeRequestSeq = 0
const billOverviewVisibleLoading = computed(() => billOverviewLoading.value && !billOverviewReady.value)
const recentBillsVisibleLoading = computed(() => recentBillsLoading.value && !recentBillsReady.value)

const scopedCardIds = computed(() => {
  return activeCards.value
    .map((card: any) => Number(card?.id || 0))
    .filter(id => Number.isFinite(id) && id > 0)
})

const selectedBillMonth = computed(() => {
  return billFilter.month ? `${billFilter.year}-${String(billFilter.month).padStart(2, '0')}` : ''
})

const canOpenScopedBills = computed(() => scopedCardIds.value.length > 0)

function emptyBillOverview(): BillOverview {
  return { billCount: 0, pendingCount: 0, overdueCount: 0, totalBillAmount: 0 }
}

function buildBillQueryParams() {
  return {
    cardIds: scopedCardIds.value.join(','),
    ownerId: undefined,
    cardName: '',
    year: billFilter.year,
    billMonth: selectedBillMonth.value || undefined,
    status: undefined
  }
}

function buildBillOverviewFromRows(list: BillRow[]): BillOverview {
  return list.reduce((acc, item) => {
    acc.billCount += 1
    if (Number(item?.status) === 0) acc.pendingCount += 1
    if (Number(item?.status) === 3) acc.overdueCount += 1
    acc.totalBillAmount += Number(item?.billAmount ?? 0)
    return acc
  }, emptyBillOverview())
}

function billMonthOrder(row: BillRow) {
  const match = String(row?.billMonth || '').match(/^(\d{4})-(\d{2})$/)
  if (!match) return 999999
  return Number(match[1]) * 100 + Number(match[2])
}

async function fetchBillScopeData(options: { silent?: boolean } = {}) {
  const cardIds = scopedCardIds.value
  const requestSeq = ++billScopeRequestSeq
  if (!cardIds.length) {
    billOverviewLoading.value = false
    billOverview.value = emptyBillOverview()
    recentBills.value = []
    recentBillsLoading.value = false
    billOverviewReady.value = groupsReady.value
    recentBillsReady.value = groupsReady.value
    return
  }

  const showLoading = !options.silent && (!billOverviewReady.value || !recentBillsReady.value)
  if (showLoading) {
    billOverviewLoading.value = true
    recentBillsLoading.value = true
  }
  try {
    const res: any = await getBillPageApi({
      current: 1,
      size: 100,
      ...buildBillQueryParams()
    })
    if (requestSeq !== billScopeRequestSeq) return
    const records = ((res.data?.records || []) as BillRow[]).sort((a, b) => {
      const monthDelta = billMonthOrder(a) - billMonthOrder(b)
      if (monthDelta !== 0) return monthDelta
      const aCard = `${a.bankName || ''}${a.cardNoLast4 || ''}`
      const bCard = `${b.bankName || ''}${b.cardNoLast4 || ''}`
      return aCard.localeCompare(bCard)
    })
    recentBills.value = records
    syncBillAmountDrafts(records)
    billOverview.value = buildBillOverviewFromRows(records)
    billOverviewReady.value = true
    recentBillsReady.value = true
  } finally {
    if (requestSeq === billScopeRequestSeq) {
      billOverviewLoading.value = false
      recentBillsLoading.value = false
    }
  }
}

function fmtRepayDay(date: string | null | undefined) {
  const match = String(date || '').match(/(\d{4})-(\d{2})-(\d{2})/)
  return match ? `${Number(match[3])}日` : '—'
}

function displayBankName(name: string | null | undefined) {
  const value = String(name || '').trim()
  if (!value) return '—'
  return value.includes('银行') ? value : `${value}银行`
}

function syncBillAmountDrafts(rows: BillRow[]) {
  const next: Record<number, number> = {}
  rows.forEach(row => {
    next[row.id] = billAmountDraftMap.value[row.id] ?? toAmount(row.billAmount)
  })
  billAmountDraftMap.value = next
}

function billAmountDraftValue(row: BillRow) {
  return billAmountDraftMap.value[row.id] ?? toAmount(row.billAmount)
}

function updateBillAmountDraft(billId: number, value: any) {
  billAmountDraftMap.value[billId] = toAmount(value)
}

function isBillAmountChanged(row: BillRow) {
  return Math.abs(billAmountDraftValue(row) - toAmount(row.billAmount)) >= 0.005
}

function buildBillAmountUpdatePayload(row: BillRow, billAmount: number) {
  return {
    id: row.id,
    cardId: row.cardId,
    billMonth: row.billMonth,
    billDay: row.billDay,
    billAmount,
    minPayAmount: row.minPayAmount,
    repayDate: row.repayDate,
    repayDay: row.repayDay,
    actualPayAmount: row.actualPayAmount,
    actualPayDate: row.actualPayDate,
    feeRate: row.feeRate,
    feePaid: row.feePaid,
    verified: row.verified,
    expenseVerified: row.expenseVerified,
    posCostAmount: row.posCostAmount,
    status: row.status,
    remark: row.remark || ''
  }
}

async function saveBillAmount(row: BillRow) {
  if (!isBillAmountChanged(row) || savingBillId.value === row.id) return
  savingBillId.value = row.id
  try {
    const billAmount = billAmountDraftValue(row)
    await updateBillApi(buildBillAmountUpdatePayload(row, billAmount))
    ElMessage.success('账单金额已保存')
    await fetchBillScopeData({ silent: true })
    await fetchProfitScopeData({ silent: true })
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || '保存账单金额失败')
  } finally {
    savingBillId.value = null
  }
}

function openUserBillsPage() {
  if (!billScopeOwnerId.value) return
  router.push({
    path: '/bills',
    query: { ownerId: String(billScopeOwnerId.value), year: String(currentYear) }
  })
}

function openFilteredBillsPage() {
  if (!scopedCardIds.value.length) return
  const routeQuery: Record<string, string> = {
    year: String(billFilter.year)
  }
  if (selectedBillMonth.value) {
    routeQuery.billMonth = selectedBillMonth.value
  }
  if (scopedCardIds.value.length === 1) {
    routeQuery.cardId = String(scopedCardIds.value[0])
  } else if (billScopeOwnerId.value) {
    routeQuery.ownerId = String(billScopeOwnerId.value)
  }
  router.push({ path: '/bills', query: routeQuery })
}

function openCardBillsPage(card: any) {
  const cardId = Number(card?.id || 0)
  if (!cardId) return
  router.push({
    path: '/bills',
    query: { cardId: String(cardId), year: String(currentYear) }
  })
}

// ====== 收益模块 ======
const profitLoading = ref(false)
const profitReady = ref(false)
const profitScope = ref<ProfitScope>('month')
const profitRows = ref<BillRow[]>([])
const profitOverview = ref<ProfitOverview>({
  year: currentYear,
  month: currentMonth,
  userCount: 0,
  cardCount: 0,
  totalBillAmount: 0,
  totalFeeAmount: 0,
  totalPosCostAmount: 0,
  expectedNetProfit: 0,
  totalNetProfit: 0,
  paidFeeAmount: 0,
  unpaidFeeAmount: 0,
  paidFeeCount: 0,
  unpaidFeeCount: 0
})
const profitVisibleLoading = computed(() => profitLoading.value && !profitReady.value)
const profitScopeLabel = computed(() => profitScope.value === 'month' ? '本月' : '本年')
let profitScopeRequestSeq = 0

const feePaymentItems = computed(() => {
  const rows = [...profitRows.value]
    .filter(row => billFeeAmount(row) > 0)
    .sort((a, b) => {
      if (Boolean(a.feePaid) !== Boolean(b.feePaid)) {
        return Boolean(a.feePaid) ? 1 : -1
      }
      const monthDelta = billMonthOrder(a) - billMonthOrder(b)
      if (monthDelta !== 0) return monthDelta
      return profitBillLabel(a).localeCompare(profitBillLabel(b))
    })

  return rows.map(row => ({
    key: row.id,
    label: profitBillLabel(row),
    repayText: row.repayDate ? `还款日 ${fmtRepayDay(row.repayDate)}` : '还款日 —',
    amount: billFeeAmount(row),
    paid: row.feePaid === true
  }))
})

function emptyProfitOverview(): ProfitOverview {
  return {
    year: currentYear,
    month: profitScope.value === 'month' ? currentMonth : undefined,
    userCount: 0,
    cardCount: 0,
    totalBillAmount: 0,
    totalFeeAmount: 0,
    totalPosCostAmount: 0,
    expectedNetProfit: 0,
    totalNetProfit: 0,
    paidFeeAmount: 0,
    unpaidFeeAmount: 0,
    paidFeeCount: 0,
    unpaidFeeCount: 0
  }
}

function buildProfitQueryParams() {
  return {
    cardIds: scopedCardIds.value.join(','),
    ownerId: undefined,
    cardName: '',
    year: currentYear,
    billMonth: profitScope.value === 'month' ? currentBillMonth : undefined,
    status: undefined
  }
}

function setProfitScope(scope: ProfitScope) {
  if (profitScope.value === scope) return
  profitReady.value = false
  profitScope.value = scope
}

async function fetchProfitScopeData(options: { silent?: boolean } = {}) {
  const cardIds = scopedCardIds.value
  const requestSeq = ++profitScopeRequestSeq
  if (!cardIds.length) {
    profitLoading.value = false
    profitRows.value = []
    profitOverview.value = emptyProfitOverview()
    profitReady.value = groupsReady.value
    return
  }

  const showLoading = !options.silent && !profitReady.value
  if (showLoading) {
    profitLoading.value = true
  }

  try {
    const records = await fetchAllProfitBillRows()
    if (requestSeq !== profitScopeRequestSeq) return

    records.sort((a, b) => {
      const monthDelta = billMonthOrder(a) - billMonthOrder(b)
      if (monthDelta !== 0) return monthDelta
      const aCard = `${a.ownerName || ''}${a.bankName || ''}${a.cardNoLast4 || ''}`
      const bCard = `${b.ownerName || ''}${b.bankName || ''}${b.cardNoLast4 || ''}`
      return aCard.localeCompare(bCard)
    })
    if (requestSeq !== profitScopeRequestSeq) return

    profitRows.value = records
    profitOverview.value = buildProfitOverviewFromRows(records)
    profitReady.value = true
  } finally {
    if (requestSeq === profitScopeRequestSeq) {
      profitLoading.value = false
    }
  }
}

async function fetchAllProfitBillRows() {
  const pageSize = 100
  const params = buildProfitQueryParams()
  const records: BillRow[] = []
  let current = 1
  let total = 0

  while (true) {
    const res: any = await getBillPageApi({
      current,
      size: pageSize,
      ...params
    })
    const pageRecords = (res.data?.records || []) as BillRow[]
    total = Number(res.data?.total ?? total)
    records.push(...pageRecords)

    if (!pageRecords.length) break
    if (total > 0 && records.length >= total) break
    if (pageRecords.length < pageSize) break
    current += 1
  }

  return records
}

function buildProfitOverviewFromRows(list: BillRow[]): ProfitOverview {
  const ownerIds = new Set<number>()
  let paidFeeAmount = 0
  let unpaidFeeAmount = 0
  let paidFeeCount = 0
  let unpaidFeeCount = 0

  for (const row of list) {
    const ownerId = Number(row?.ownerId || 0)
    if (ownerId > 0) ownerIds.add(ownerId)
    const feeAmount = billFeeAmount(row)
    if (feeAmount > 0) {
      if (row.feePaid === true) {
        paidFeeAmount += feeAmount
        paidFeeCount += 1
      } else {
        unpaidFeeAmount += feeAmount
        unpaidFeeCount += 1
      }
    }
  }

  const overview = list.reduce((acc, item) => {
    acc.totalBillAmount += Number(item?.billAmount ?? 0)
    acc.totalFeeAmount += billFeeAmount(item)
    acc.totalPosCostAmount += Number(item?.posCostAmount ?? 0)
    return acc
  }, {
    ...emptyProfitOverview(),
    userCount: ownerIds.size,
    cardCount: scopedCardIds.value.length
  })

  overview.expectedNetProfit = overview.totalFeeAmount - overview.totalPosCostAmount
  overview.paidFeeAmount = paidFeeAmount
  overview.unpaidFeeAmount = unpaidFeeAmount
  overview.paidFeeCount = paidFeeCount
  overview.unpaidFeeCount = unpaidFeeCount
  overview.totalNetProfit = overview.paidFeeAmount - overview.totalPosCostAmount
  return overview
}

function billFeeAmount(row: BillRow) {
  const billAmount = toAmount(row.billAmount)
  const feeRate = Number(row.feeRate)
  if (Number.isFinite(feeRate)) {
    return Number(((billAmount * feeRate) / 100).toFixed(2))
  }
  return toAmount(row.feeAmount)
}

function profitBillLabel(row: BillRow) {
  const parts = [
    profitScope.value === 'year' ? billMonthShortLabel(row.billMonth) : '',
    row.ownerName || '未命名',
    displayBankName(row.bankName),
    `尾号${row.cardNoLast4 || '-'}`
  ].filter(Boolean)
  return parts.join(' · ')
}

function billMonthShortLabel(billMonth: string | null | undefined) {
  const match = String(billMonth || '').match(/^(\d{4})-(\d{2})$/)
  return match ? `${Number(match[2])}月` : ''
}

function goProfits() {
  if (!scopedCardIds.value.length) return
  const routeQuery: Record<string, string> = {
    year: String(currentYear)
  }
  if (profitScope.value === 'month') {
    routeQuery.month = String(currentMonth)
  }
  if (scopedCardIds.value.length === 1) {
    routeQuery.cardId = String(scopedCardIds.value[0])
  } else if (activeUser.value?.userId) {
    routeQuery.userId = String(activeUser.value.userId)
  }
  router.push({
    path: '/profits',
    query: routeQuery
  })
}

function goUsers() {
  router.push('/users')
}

// ====== 新增/编辑卡片 ======
const defaultForm = {
  id: undefined as number | undefined,
  userId: null as any,
  bankName: '',
  cardNoLast4: '',
  cardType: Number(CARD_TYPE_VALUE.CREDIT),
  creditLimit: '',
  billDay: '',
  repayDay: '',
  expireDate: '',
  status: 0,
  remark: '',
  repayMethod: 'cloudpay',
  verified: false
}

const formData = reactive({ ...defaultForm })
const isEdit = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const userTreeList = ref<any[]>([])
const userOptions = ref<any[]>([])

const dialogTitle = computed(() => (isEdit.value ? '编辑' : '新增'))
const computedRules = computed(() => ({
  userId: [{ required: true, message: '请选择用户', trigger: 'change' }],
  bankName: [{ required: true, message: '请输入银行名称', trigger: 'blur' }],
  cardNoLast4: [
    { required: true, message: '请输入卡号后四位', trigger: 'blur' },
    { pattern: /^\d{4}$/, message: '必须为4位数字', trigger: 'blur' }
  ],
  cardType: [{ required: true, message: '请选择卡片类型', trigger: 'change' }],
  repayMethod: [{ required: true, message: '请选择APP', trigger: 'change' }]
}))

function flattenUserTree(list: any[], parentName = '') {
  const flatUsers: any[] = []
  const walk = (nodes: any[], currentParentName = '') => {
    for (const u of nodes || []) {
      const isChild = !!u.parentId
      const resolvedParentName = u.parentName || currentParentName
      const displayName = isChild && resolvedParentName
        ? `${u.name}（${resolvedParentName}的子用户）`
        : u.name
      flatUsers.push({
        id: u.id,
        name: u.name,
        displayName,
        parentId: u.parentId,
        parentName: resolvedParentName,
        status: Number(u.status ?? 0),
        effectiveFeeRate: u.effectiveFeeRate ?? u.feeRate ?? 0
      })
      if (u.children?.length) walk(u.children, u.name)
    }
  }
  walk(list, parentName)
  return flatUsers
}

function findUserById(userId: number | null | undefined) {
  const normalizedUserId = Number(userId || 0)
  if (!normalizedUserId) return null
  return flattenUserTree(userTreeList.value).find((user: any) => Number(user.id) === normalizedUserId) || null
}

function isUserDisabled(userId: number | null | undefined) {
  const user = findUserById(userId)
  return !!user && Number(user.status) !== 0
}

function notifyUserDisabled(userId: number | null | undefined) {
  const user = findUserById(userId)
  const userName = user?.name || '该用户'
  ElMessage.warning(`${userName}已停用，不能新增银行卡`)
}

async function openAddCard() {
  isEdit.value = false
  Object.assign(formData, defaultForm)
  if (activeUser.value?.userId) {
    formData.userId = activeUser.value.userId
  }
  await syncUserOptions()
  if (isUserDisabled(formData.userId)) {
    notifyUserDisabled(formData.userId)
    formData.userId = null
  }
  dialogVisible.value = true
}

async function openAddCardWithActiveUser() {
  if (!canAddCardForUser.value) return
  isEdit.value = false
  Object.assign(formData, defaultForm, { userId: activeUser.value?.userId })
  await syncUserOptions()
  if (isUserDisabled(formData.userId)) {
    notifyUserDisabled(formData.userId)
    formData.userId = null
  }
  dialogVisible.value = true
}

async function openEditCard(row: any) {
  isEdit.value = true
  Object.assign(formData, row, {
    userId: Number(row?.userId || 0) || null,
    repayMethod: row?.repayMethod === 'invoice' ? 'other' : (row?.repayMethod || 'cloudpay'),
    verified: Boolean(row?.verified)
  })
  await syncUserOptions()
  dialogVisible.value = true
}

function openBillYearView(card: any) {
  router.push({
    path: '/bills',
    query: { cardId: String(card.id), year: String(currentYear) }
  })
}

async function handleSubmit() {
  const data: any = { ...formData }
  let createdCardId: number | undefined

  ;['creditLimit', 'billDay', 'repayDay'].forEach(key => {
    if (data[key] === '') data[key] = null
  })

  if (isUserDisabled(data.userId)) {
    notifyUserDisabled(data.userId)
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCardApi(data)
    } else {
      const res: any = await saveCardApi(data)
      const rawId = Number(res?.data)
      createdCardId = Number.isFinite(rawId) && rawId > 0 ? rawId : undefined
    }
    ElMessage.success(isEdit.value ? '操作成功' : (data.cardType === CARD_TYPE_VALUE.CREDIT ? '新增成功，已生成本年账单模板' : '新增成功'))
    dialogVisible.value = false
    await fetchGroups()
    if (!isEdit.value && data.cardType === CARD_TYPE_VALUE.CREDIT && createdCardId) {
      openBillYearView({ id: createdCardId })
    }
  } finally {
    submitting.value = false
  }
}

async function confirmDeleteCard(card: any) {
  try {
    await ElMessageBox.confirm(
      `确认删除 ${card.bankName || ''}${card.cardNoLast4 ? ` · ${card.cardNoLast4}` : ''} 吗？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await deleteCardApi(Number(card.id))
    ElMessage.success('删除成功')
    await fetchGroups()
  } catch (error: any) {
    if (String(error?.message || '').includes('cancel')) return
    ElMessage.error(error?.message || error?.response?.data?.message || '删除失败，该卡可能存在关联历史数据')
  }
}

async function syncUserOptions() {
  // 每次打开弹窗都强制重新拉取，防止删除用户后下拉显示旧数据
  try {
    const res: any = await getUserTreeApi()
    userTreeList.value = res.data || []
  } catch {
    userTreeList.value = []
  }
  const flatUsers = flattenUserTree(userTreeList.value)

  const currentUserId = Number(formData.userId || 0)
  userOptions.value = flatUsers.filter((user: any) => user.status === 0 || user.id === currentUserId)
}

watch(
  () => keyword.value,
  () => {
    if (syncingKeywordQuery) return
    applyKeyword()
  },
  { flush: 'sync' }
)

watch(
  () => [query.bankName, query.cardNoLast4, query.cardType, query.status],
  () => {
    if (syncingKeywordQuery) return
    triggerGroupSearch()
  },
  { flush: 'sync' }
)

// ====== 联动刷新 ======
watch(
  () => activeUserId.value,
  () => {
    activeOwnerId.value = null
    if (!activeUser.value) return
    childPageIndex.value = 0
    if (activeUserId.value && activeUserId.value > 0) {
      ensureUserTree(false, true)
    }
    const cards = (activeUser.value.cards || []) as any[]
    if (!cards.length) {
      activeCardId.value = null
      return
    }
    if (!activeCardId.value || !cards.some(c => Number(c.id) === Number(activeCardId.value))) {
      activeCardId.value = Number(cards[0].id)
    }

    const cardIdx = cards.findIndex(c => Number(c.id) === Number(activeCardId.value))
    cardPageIndex.value = cardIdx >= 0 ? Math.floor(cardIdx / CARD_PAGE_SIZE) : 0
  }
)

watch(
  () => userKeyword.value,
  async () => {
    userPageIndex.value = 0
    await ensureUserTree(false, true)

    const kw = userKeyword.value.trim()
    if (!kw) return
    if (activeUserId.value && filteredGroupList.value.some(u => u.userId === activeUserId.value)) return

    const first = filteredGroupList.value.find(u => Number(u.userId) > 0)
    if (first) setActiveUser(first.userId)
  }
)

watch(
  () => filteredGroupList.value.length,
  () => {
    userPageIndex.value = Math.min(userPageIndex.value, Math.max(0, userPageCount.value - 1))
  }
)

watch(
  () => activeCards.value.length,
  () => {
    cardPageIndex.value = Math.min(cardPageIndex.value, Math.max(0, cardPageCount.value - 1))
  }
)

watch(
  () => [scopedCardIds.value.join(','), billFilter.year, billFilter.month],
  () => {
    fetchBillScopeData({ silent: true })
  },
  { immediate: true }
)

watch(
  () => [scopedCardIds.value.join(','), profitScope.value],
  () => {
    fetchProfitScopeData()
  },
  { immediate: true }
)

onMounted(() => {
  fetchGroups({ silent: true })
  ensureUserTree(false, true)
})

onUnmounted(() => {
  triggerGroupSearch.cancel()
})

onActivated(() => {
  if (!hasActivatedOnce.value) {
    hasActivatedOnce.value = true
    return
  }
  // 从其他页面（如持卡人管理）返回时始终刷新，确保数据最新
  fetchGroups({ silent: true })
  ensureUserTree(false, true)
})
</script>

<style scoped lang="scss">
$primary:       #0958d9;
$primary-soft:  #eaf2ff;
$ink:           #1f2937;
$ink2:          #5b6475;
$sub:           #6b7280;
$faint:         #94a3b8;
$border:        #dbe2ea;
$surface:       #ffffff;
$bg:            #f5f7fb;
$danger:        #cf1322;
$warning:       #d97706;
$success:       #2f9e44;
$rs:            8px;
$shadow-sm:     0 8px 20px rgba(15,23,42,.045);

.cards-page {
  display: flex;
  flex-direction: column;
  margin: 0;
  width: 100%;
  height: 100%;
  min-height: 0;
  background: $bg;
  overflow: hidden;
  box-sizing: border-box;
  outline: none;

  /* 局部覆盖主题色，保持与日历模块一致 */
  --color-primary: #0958d9;
  --color-primary-light: #1677ff;
  --color-primary-dark: #0540b0;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: linear-gradient(180deg, rgba(255,255,255,.98) 0%, rgba(248,250,253,.98) 100%);
  border-bottom: 1px solid rgba(211,223,238,.92);
  flex-shrink: 0;
  box-shadow: 0 10px 24px rgba(15,23,42,.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.page-title-icon {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  background: linear-gradient(180deg, rgba($primary,.12) 0%, rgba($primary,.06) 100%);
  color: $primary;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 8px 18px rgba(9,88,217,.08);
}

.header-title-group {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.page-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: $ink;
  letter-spacing: -0.3px;
}

.page-subtitle {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
  font-size: 13px;
  color: $sub;
  line-height: 1.4;
  min-width: 0;
  strong { font-weight: 600; color: $ink2; }
}

.sub-divider {
  display: inline-block;
  width: 1px;
  height: 12px;
  background: $border;
  flex-shrink: 0;
  opacity: 0.6;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.panel-search {
  display: flex;
  align-items: center;
  gap: 7px;
  background: $bg;
  border: 1.5px solid transparent;
  border-radius: $rs;
  padding: 7px 12px;
  width: clamp(170px, 18vw, 280px);
  transition: all .2s;
  .search-icon { color: $sub; flex-shrink: 0; }
  input {
    border: none;
    outline: none;
    font-size: 13px;
    color: $ink;
    background: transparent;
    width: 100%;
    min-width: 0;
    &::placeholder { color: $faint; }
  }
  &.focused {
    background: $surface;
    border-color: $primary;
    box-shadow: 0 0 0 3px rgba($primary,.1);
  }
}

.search-clear {
  border: none;
  background: none;
  cursor: pointer;
  font-size: 16px;
  color: $sub;
  padding: 0;
  line-height: 1;
  &:hover { color: $danger; }
}

.card-filters {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
}

.icon-btn {
  width: 28px;
  height: 28px;
  border: 1px solid $border;
  background: $surface;
  border-radius: $rs;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $sub;
  transition: all .18s;
  flex-shrink: 0;
  &:hover:not(:disabled) { border-color: $primary; color: $primary; background: $primary-soft; }
  &:disabled { cursor: not-allowed; opacity: .55; }
}

.main-body {
  flex: 1;
  overflow: hidden;
  min-height: 0;
}

.cards-grid {
  height: 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 10px 12px 12px;
  min-height: 0;
}

/* 持卡人信息面板：更紧凑，避免第4行裁切 */
.panel.is-users {
  .panel-head {
    .panel-actions {
      flex: 1;
      justify-content: flex-end;
    }
  }

  .user-search {
    display: flex;
    align-items: center;
    gap: 7px;
    background: $bg;
    border: 1.5px solid transparent;
    border-radius: $rs;
    padding: 6px 10px;
    width: clamp(180px, 22vw, 320px);
    height: 28px;
    transition: all .2s;

    .search-icon { color: $sub; flex-shrink: 0; }
    input {
      border: none;
      outline: none;
      font-size: 12.5px;
      color: $ink;
      background: transparent;
      width: 100%;
      min-width: 0;
      &::placeholder { color: $faint; }
    }

    &.focused {
      background: $surface;
      border-color: $primary;
      box-shadow: 0 0 0 3px rgba($primary,.1);
    }
  }

  .panel-body {
    padding: 7px 11px 10px;
    gap: 7px;
  }

  .mini-stats {
    gap: 6px;
  }

  .mini-stat {
    min-height: 30px;
    padding: 5px 10px;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 6px;
    border-radius: 10px;
    background: rgba(148,163,184,.07);
    border-color: rgba(219,226,234,.6);
  }

  .ms-label {
    font-size: 11px;
    font-weight: 600;
    color: $sub;
  }

  .ms-value {
    font-size: 13.5px;
    font-weight: 900;
    color: $ink;
  }

  .list-item {
    padding: 7px 9px;
    min-height: 0;
    height: 100%;
    border-radius: 12px;
  }

}

.list-item.placeholder {
  cursor: default;
  background: transparent;
  border-style: dashed;
  border-color: transparent;
  box-shadow: none;
  pointer-events: none;
  box-shadow: none;
  min-height: 54px;
  height: 100%;
  &:hover {
    transform: none;
    box-shadow: none;
    border-color: rgba(219,226,234,.7);
  }
}

.panel {
  background: linear-gradient(180deg, rgba(255,255,255,.99) 0%, rgba(248,250,253,.97) 100%);
  border-radius: 18px;
  box-shadow: $shadow-sm;
  border: 1px solid rgba(218,226,236,.9);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid rgba(219,226,234,.8);
  background: linear-gradient(180deg, rgba(255,255,255,.92) 0%, rgba(248,250,253,.92) 100%);
  flex-shrink: 0;
}

.panel-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 800;
  color: $ink;
  letter-spacing: .2px;
}

.panel-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: $ink2;
  &.is-primary { background: $primary; }
  &.is-warning { background: $warning; }
  &.is-success { background: $success; }
}

.panel-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.pager {
  display: flex;
  align-items: center;
  gap: 6px;
}

.panel-meta {
  font-size: 11.5px;
  color: $sub;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(148,163,184,.10);
  border: 1px solid rgba(219,226,234,.7);
}

.mini-filter {
  width: 88px;
}

.card-filter {
  width: 102px;
}

.panel.is-card-info {
  .card-title-with-stats {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
    min-width: 0;
  }

  .panel-title {
    flex-shrink: 0;
    white-space: nowrap;
  }

  .panel-actions {
    flex: 1;
    min-width: 0;
    flex-wrap: nowrap;
  }

  .card-filters {
    flex: 1;
    min-width: 0;
    justify-content: flex-end;
  }

  .panel-search {
    flex: 1 1 190px;
    width: clamp(190px, 22vw, 320px);
    max-width: 320px;
  }

  .card-filter {
    width: 92px;
    flex-shrink: 0;
  }
}

.card-type-stats {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 9px;
  background: rgba(148, 163, 184, 0.08);
  border: 1px solid rgba(219, 226, 234, 0.85);
  border-radius: 999px;
  flex-shrink: 0;
  white-space: nowrap;
}

.cts-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  font-weight: 600;
  color: $sub;
  white-space: nowrap;

  b {
    font-weight: 800;
    color: $ink;
  }

  &.cts-credit b {
    color: #3b82f6;
  }

  &.cts-debit b {
    color: #10b981;
  }
}

.cts-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;

  &.credit {
    background: #3b82f6;
  }

  &.debit {
    background: #10b981;
  }
}

.cts-divider {
  width: 1px;
  height: 12px;
  background: rgba(219, 226, 234, 1);
  flex-shrink: 0;
}

.year-filter {
  width: 94px;
}

.mini-filter :deep(.el-select__wrapper) {
  min-height: 28px;
  border-radius: $rs;
  box-shadow: 0 0 0 1px $border inset;
}

.mini-filter :deep(.el-select__placeholder),
.mini-filter :deep(.el-select__selected-item) {
  font-size: 11.5px;
  font-weight: 700;
}

.panel-body {
  padding: 10px 12px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  min-width: 0;
}

.mini-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  min-width: 0;
}

.mini-stats-4 {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  min-width: 0;
}

.mini-stat {
  background: rgba(148,163,184,.10);
  border: 1px solid rgba(219,226,234,.75);
  border-radius: 14px;
  padding: 10px 10px 9px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  overflow: hidden;
}

.ms-label {
  font-size: 11.5px;
  color: $sub;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ms-value {
  font-size: 16px;
  font-weight: 800;
  color: $ink;
  line-height: 1.1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  &.warn { color: $warning; }
  &.danger { color: $danger; }
}

.talker-board {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
}

.talker-list {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-auto-rows: auto;
  gap: 8px;
  min-height: 0;
  align-content: start;
  overflow-y: auto;
  overflow-x: hidden;
}

.talker-list .empty-hint {
  grid-column: 1 / -1;
  grid-row: 1 / -1;
}

.talker-card {
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
  gap: 5px;
  padding: 8px 8px 7px;
  position: relative;
  border-radius: 12px;
}

.talker-main {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.talker-card .li-avatar {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  font-size: 11.5px;
  flex-shrink: 0;
}

.talker-card .li-title {
  font-size: 12.5px;
  font-weight: 800;
  padding-right: 48px;
  line-height: 1.35;
  letter-spacing: -0.1px;
}

.talker-card .li-sub {
  font-size: 10.5px;
  color: $faint;
  line-height: 1.3;
  margin-top: 1px;
}

.talker-footer {
  display: flex;
  align-items: stretch;
  gap: 0;
  min-width: 0;
  background: rgba(148, 163, 184, 0.05);
  border: 1px solid rgba(219, 226, 234, 0.75);
  border-radius: 7px;
  overflow: hidden;
  flex-shrink: 0;
}

.talker-stats {
  display: flex;
  align-items: center;
  gap: 0;
  min-width: 0;
  flex: 1;
}

.talker-stat-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  padding: 4px 7px;
  min-width: 0;
  flex: 1;
}

.talker-stat-amount {
  flex: 1.8;
  min-width: 0;
}

.talker-stat-divider {
  width: 1px;
  align-self: stretch;
  background: rgba(219, 226, 234, 0.9);
  flex-shrink: 0;
}

.ts-label {
  font-size: 9.5px;
  color: $faint;
  font-weight: 600;
  white-space: nowrap;
  flex-shrink: 0;
  line-height: 1.4;
}

.ts-value {
  font-size: 11.5px;
  color: $ink;
  font-weight: 800;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
  text-align: right;
}

.talker-rate-tag {
  position: absolute;
  top: 7px;
  right: 7px;
  padding: 1px 5px;
  border-radius: 5px;
  background: rgba(148, 163, 184, 0.09);
  border: 1px solid rgba(219, 226, 234, 0.75);
  color: $faint;
  font-size: 9.5px;
  font-weight: 700;
  white-space: nowrap;
  line-height: 1.65;
  transition: all 0.2s;
  letter-spacing: 0.1px;
}

.talker-rate-tag.has-rate {
  background: rgba($primary, 0.07);
  border-color: rgba($primary, 0.15);
  color: $primary;
}

.rate-prefix {
  font-size: 8.5px;
  font-weight: 500;
  opacity: 0.65;
  margin-right: 1px;
  letter-spacing: 0;
}

.user-col,
.child-col {
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
}

/* 固定三行，左右严格对齐 */
/*
  主/子用户严格对齐：
  1) 头部区域固定高度，避免 badge/pager 有无导致列表高度不同
  2) 列表区域固定 3 行固定行高，左右完全一致
*/
.user-col,
.child-col {
  --user-head-h: 26px;
  --user-row-h: minmax(0, 1fr);
}

.user-list,
.child-list {
  flex: 1;
  display: grid;
  grid-template-rows: repeat(3, var(--user-row-h));
  gap: 8px;
  align-content: stretch;
  align-items: stretch;
  min-height: 0;
  overflow: hidden;
}

.user-list .empty-hint,
.child-list .empty-hint {
  grid-row: 1 / -1;
}

.child-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 0 2px;
  flex-shrink: 0;
  height: var(--user-head-h);
}

.child-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 800;
  color: $ink2;
}

.child-count {
  height: 20px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
  color: $primary;
  background: rgba($primary, .08);
  border: 1px solid rgba($primary, .18);
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.card-panel-body {
  padding-top: 8px;
  gap: 0;
}

.card-list .card-item,
.card-list .card-item.placeholder {
  height: auto;
  min-height: 56px;
}

.card-item {
  padding: 8px 10px;
}

.card-info-left {
  min-width: 0;
}

.card-info-left .li-main {
  gap: 2px;
}

.card-row-top {
  display: flex;
  align-items: center;
  gap: 0;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  line-height: 1.5;
}

.card-row-sub {
  display: flex;
  align-items: center;
  gap: 0;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  line-height: 1.5;
}

.cig-sep {
  display: inline-block;
  width: 1px;
  height: 10px;
  background: rgba(148,163,184,0.4);
  margin: 0 7px;
  flex-shrink: 0;
  vertical-align: middle;
}

.cig-sep-dot {
  display: inline-block;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(148,163,184,0.5);
  margin: 0 5px;
  flex-shrink: 0;
  vertical-align: middle;
}

.cig-label {
  color: $sub;
  font-weight: 600;
  font-size: 11px;
  white-space: nowrap;
  flex-shrink: 0;
  margin-right: 2px;
}

.cig-name {
  font-size: 13px;
  font-weight: 800;
  color: $ink;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 1;
}

.cig-bank {
  font-size: 13px;
  font-weight: 700;
  color: $ink;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 1;
}

.cig-last4 {
  font-size: 13px;
  font-weight: 800;
  color: $ink;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}

.cig-type {
  font-size: 11.5px;
  font-weight: 700;
  color: $ink2;
  flex-shrink: 0;
}

.cig-date {
  font-size: 11.5px;
  font-weight: 700;
  color: $ink2;
  flex-shrink: 0;
}

.cig-empty {
  color: $sub;
  font-weight: 600;
}

.list-item {
  box-sizing: border-box;
  border: 1px solid rgba(219,226,234,.85);
  background: $surface;
  border-radius: 14px;
  padding: 10px 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 54px;
  cursor: pointer;
  transition: all .16s;
  min-width: 0;
  position: relative;
  &:hover {
    border-color: rgba($primary,.22);
    box-shadow: 0 10px 18px rgba(15,23,42,.06);
    transform: translateY(-1px);
  }
  &.active {
    border-color: rgba($primary,.38);
    background: linear-gradient(180deg, rgba($primary,.10) 0%, rgba($primary,.04) 100%);
    box-shadow: inset 0 0 0 2px rgba($primary,.28), 0 14px 22px rgba(15,23,42,.07);
  }
  &.active::before {
    content: '';
    position: absolute;
    left: 8px;
    top: 10px;
    bottom: 10px;
    width: 4px;
    border-radius: 999px;
    background: $primary;
    box-shadow: 0 6px 14px rgba($primary,.25);
  }
}

/* 主用户行用 button，强制 reset 以消除默认样式导致的像素偏差 */
.list-item[role='button'] {
  text-align: left;
  width: 100%;
  user-select: none;
}

.list-item[role='button']:focus-visible {
  outline: 2px solid rgba($primary, .32);
  outline-offset: 2px;
}

.panel.is-users .list-item,
.panel.is-users .list-item.placeholder {
  height: 100%;
  min-height: 54px;
}

.talker-board {
  --user-head-h: 26px;
}

.talker-list {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  grid-auto-rows: auto;
  align-content: start;
}

.talker-card {
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
  padding: 8px 8px 7px;
  position: relative;
  border-radius: 12px;
}

.li-left { display: flex; align-items: center; gap: 10px; min-width: 0; flex: 1; }
.li-main { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.li-title { font-size: 13px; font-weight: 800; color: $ink; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.li-sub { font-size: 11.5px; color: $sub; display: flex; align-items: center; gap: 6px; min-width: 0; }
.li-right { display: grid; grid-template-columns: repeat(2, minmax(78px, 1fr)); gap: 8px; flex-shrink: 0; }

.card-info-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.metrics-grid {
  min-width: 176px;
}

.metric-box {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 7px 8px;
  min-width: 0;
  border-radius: 12px;
  background: rgba(148,163,184,.10);
  border: 1px solid rgba(219,226,234,.75);
}

.metric-label {
  font-size: 10.5px;
  color: $sub;
  font-weight: 700;
  line-height: 1;
}

.metric-value {
  font-size: 12px;
  color: $ink;
  font-weight: 800;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rate-inline {
  color: $primary;
  font-weight: 700;
}

.li-avatar {
  width: 32px;
  height: 32px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  color: $primary;
  background: linear-gradient(180deg, rgba($primary,.12) 0%, rgba($primary,.06) 100%);
  box-shadow: 0 8px 18px rgba(9,88,217,.08);
  flex-shrink: 0;
  &.ghost {
    color: $faint;
    background: rgba(148,163,184,.12);
    box-shadow: none;
  }
}

.li-avatar.sm {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  font-size: 13px;
  box-shadow: 0 8px 18px rgba(9,88,217,.06);
}

.child-item {
  padding: 9px 9px;
  cursor: pointer;
  position: relative;
  &.active {
    border-color: rgba($primary,.38);
    background: linear-gradient(180deg, rgba($primary,.10) 0%, rgba($primary,.04) 100%);
    box-shadow: inset 0 0 0 2px rgba($primary,.28), 0 14px 22px rgba(15,23,42,.07);
  }
  &.active::before {
    content: '';
    position: absolute;
    left: 8px;
    top: 10px;
    bottom: 10px;
    width: 4px;
    border-radius: 999px;
    background: $primary;
    box-shadow: 0 6px 14px rgba($primary,.25);
  }
  &:hover:not(:disabled) {
    border-color: rgba($primary,.28);
    box-shadow: 0 10px 18px rgba(15,23,42,.06);
    transform: translateY(-1px);
  }
  &:hover {
    border-color: rgba($primary,.22);
  }
  &.disabled {
    opacity: .78;
    cursor: not-allowed;
  }
}


.li-icon {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: $sub;
  background: rgba(148,163,184,.12);
  border: 1px solid rgba(219,226,234,.7);
  &.credit {
    color: $primary;
    background: linear-gradient(180deg, rgba($primary,.12) 0%, rgba($primary,.06) 100%);
    border-color: rgba($primary,.14);
  }
}

.muted { color: $sub; font-weight: 700; }
.bank-name { font-weight: 800; }

.amt {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
  text-align: right;
  min-width: 88px;
}

.amt-label { font-size: 11px; color: $sub; font-weight: 800; }
.amt-value { font-size: 13px; font-weight: 900; color: $ink; letter-spacing: .2px; }

.li-actions { display: flex; align-items: center; gap: 6px; }
.mini-icon {
  width: 28px;
  height: 28px;
  border-radius: 10px;
  border: 1px solid rgba(219,226,234,.75);
  background: rgba(148,163,184,.10);
  color: $sub;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .15s;
  &:hover {
    border-color: rgba($primary,.25);
    background: $primary-soft;
    color: $primary;
  }
  &.danger:hover {
    border-color: rgba($danger,.4);
    background: rgba($danger,.08);
    color: $danger;
  }
}

.active-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 10px;
  border: 1px solid rgba(219,226,234,.85);
  background: linear-gradient(180deg, rgba(255,255,255,.98) 0%, rgba(248,250,253,.98) 100%);
  border-radius: 14px;
}

.am-name { font-size: 13px; font-weight: 900; color: $ink; }
.am-sub { margin-top: 2px; font-size: 11.5px; color: $sub; font-weight: 700; }

.empty-hint {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: $faint;
  font-size: 12px;
  padding: 10px;
}

.link-btn {
  border: none;
  background: none;
  cursor: pointer;
  color: $primary;
  font-weight: 800;
  font-size: 12px;
  padding: 0;
  &:disabled { cursor: not-allowed; opacity: .55; }
  &:hover:not(:disabled) { text-decoration: underline; }
}

.bill-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 2px;
}

.profit-summary-block {
  overflow-y: auto;
  overflow-x: hidden;
  gap: 5px;
  padding-right: 2px;
}

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: $sub;
  font-size: 12px;
  font-weight: 800;
}

.block-head strong {
  color: $ink;
  font-size: 12px;
  font-weight: 900;
}

.bill-item {
  display: grid;
  grid-template-columns: minmax(86px, 1fr) minmax(86px, .9fr) 66px 82px minmax(152px, 1.2fr) 74px;
  gap: 8px;
  align-items: center;
  padding: 8px 10px;
  border-radius: 12px;
  background: $surface;
  border: 1px solid rgba(219,226,234,.85);
  min-width: 0;
}

.bill-item.current {
  border-color: rgba($warning,.55);
  background: linear-gradient(180deg, rgba(255,255,255,.99) 0%, rgba($warning,.08) 150%);
}

.bill-owner,
.bill-bank,
.bill-last4,
.bill-repay,
.bill-amount-edit,
.bill-status {
  min-width: 0;
}

.bill-owner,
.bill-bank {
  overflow: hidden;
  color: $ink;
  font-size: 12.5px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bill-bank {
  color: $ink2;
}

.bill-last4,
.bill-repay {
  overflow: hidden;
  color: $sub;
  font-size: 11.5px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bill-amount-edit {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}

.bill-amount-symbol {
  color: $sub;
  font-size: 11.5px;
  font-weight: 900;
}

.bill-amount-input {
  width: 96px;
}

.bill-amount-input :deep(.el-input__wrapper) {
  min-height: 26px;
  padding: 0 7px;
  border-radius: 8px;
}

.bill-amount-input :deep(.el-input__inner) {
  font-size: 12px;
  font-weight: 900;
  color: $ink;
  text-align: right;
}

.bill-save-btn {
  min-width: 34px;
  padding: 0;
  font-weight: 900;
}

.bill-status {
  display: flex;
  justify-content: flex-end;
}

.scope-switch { display: flex; align-items: center; gap: 6px; }

.scope-toggle {
  display: inline-flex;
  align-items: center;
  padding: 2px;
  border: 1px solid rgba(219,226,234,.9);
  border-radius: 999px;
  background: rgba(148,163,184,.10);
}

.scope-toggle button {
  height: 24px;
  min-width: 44px;
  padding: 0 10px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: $sub;
  cursor: pointer;
  font-size: 11.5px;
  font-weight: 900;
  line-height: 24px;
}

.scope-toggle button.active {
  background: $success;
  color: #fff;
  box-shadow: 0 4px 10px rgba($success,.18);
}

.profit-scope-toggle {
  flex-shrink: 0;
}

.profit-kpi {
  padding: 5px 8px;
  border-radius: 10px;
  border: 1px solid rgba(219,226,234,.85);
  background: linear-gradient(180deg, rgba(255,255,255,.99) 0%, rgba($primary,.03) 180%);
}

.pk-label { font-size: 10.5px; color: $sub; font-weight: 800; }
.pk-value { margin-top: 2px; font-size: 18px; font-weight: 900; letter-spacing: 0; line-height: 1.05; }
.pk-value.pos { color: $success; }
.pk-value.neg { color: $danger; }
.pk-sub { margin-top: 1px; font-size: 10px; color: $faint; font-weight: 700; }

.profit-net-grid,
.profit-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 5px;
}

.profit-metric {
  border-radius: 10px;
  border: 1px solid rgba(219,226,234,.75);
  background: rgba(148,163,184,.10);
  padding: 6px 8px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.pm-label { font-size: 10.5px; color: $sub; font-weight: 800; }
.pm-value { font-size: 12px; font-weight: 900; color: $ink; }
.pm-value.pos { color: $success; }
.pm-value.warn { color: $warning; }
.pm-value.neg { color: $danger; }

.fee-pay-card {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-height: 0;
  padding: 6px 8px;
  border: 1px solid rgba(219,226,234,.75);
  border-radius: 10px;
  background: rgba(255,255,255,.96);
}

.fee-pay-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  color: $ink;
  font-size: 11.5px;
  font-weight: 900;
}

.fee-pay-summary {
  color: $sub;
  font-size: 10.5px;
  font-weight: 800;
  text-align: right;
}

.fee-pay-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 238px;
  overflow-y: auto;
  padding-right: 2px;
}

.fee-pay-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 6px;
  min-width: 0;
  padding: 4px 6px;
  border-radius: 8px;
  background: rgba(148,163,184,.08);
}

.fee-pay-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.fee-pay-title,
.fee-pay-sub {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fee-pay-title {
  color: $ink;
  font-size: 11px;
  font-weight: 900;
}

.fee-pay-sub {
  color: $faint;
  font-size: 10px;
  font-weight: 800;
}

.fee-pay-amount {
  font-size: 11px;
  font-weight: 900;
  white-space: nowrap;
}

.fee-pay-amount.pos {
  color: $success;
}

.fee-pay-amount.warn {
  color: $warning;
}

.fee-pay-empty {
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $faint;
  font-size: 12px;
  font-weight: 800;
}

.font-mono {
  font-family: var(--font-mono), monospace;
  font-variant-numeric: tabular-nums;
}

.user-opt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

@media (max-width: 1440px) {
  .page-header { padding: 14px 20px; }
  .cards-grid { gap: 10px; padding: 10px 12px 12px; }
  .mini-stats { gap: 6px; }
  .mini-stat { padding: 9px 9px 8px; }
  .pk-value { font-size: 24px; }
}

@media (max-width: 1280px) {
  .metrics-grid { min-width: 160px; }
  .metric-box { padding: 6px 7px; }
  .bill-item {
    grid-template-columns: minmax(80px, 1fr) minmax(76px, .9fr) 60px 70px minmax(130px, 1fr) 64px;
    gap: 6px;
    padding: 8px;
  }
  .bill-amount-input { width: 82px; }
  .profit-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
