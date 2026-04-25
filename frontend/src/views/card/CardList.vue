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
          <h1 class="page-title">银行卡管理</h1>
          <span class="page-subtitle">
            持卡人 <strong>{{ baseStats.userCount }}</strong>
            <i class="sub-divider"></i>
            银行卡 <strong>{{ baseStats.cardCount }}</strong>
            <i class="sub-divider"></i>
            当前卡片 <strong>{{ activeCardLabel }}</strong>
          </span>
        </div>
      </div>

      <div class="header-actions">
        <div class="search-box" :class="{ focused: searchFocused }">
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
            placeholder="搜索银行 / 卡号后四位…"
            @focus="searchFocused = true"
            @blur="searchFocused = false"
          />
          <button v-if="keyword" class="search-clear" @click="clearKeyword">&times;</button>
        </div>

        <div class="type-switch">
          <button :class="['chip', { active: query.cardType === undefined }]" @click="setCardType(undefined)">全部</button>
          <button :class="['chip', { active: query.cardType === CARD_TYPE_VALUE.CREDIT }]" @click="setCardType(CARD_TYPE_VALUE.CREDIT)">信用卡</button>
          <button :class="['chip', { active: query.cardType === CARD_TYPE_VALUE.DEBIT }]" @click="setCardType(CARD_TYPE_VALUE.DEBIT)">借记卡</button>
        </div>

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
        <section class="panel is-users" v-loading="loadingGroups">
          <div class="panel-head">
            <div class="panel-title"><span class="panel-dot"></span>持卡人信息</div>
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
                  placeholder="搜索持卡人 / 手机号…"
                  @focus="userSearchFocused = true"
                  @blur="userSearchFocused = false"
                />
                <button v-if="userKeyword" class="search-clear" @click.stop="clearUserKeyword">&times;</button>
              </div>

              <button class="icon-btn" @click="goUsers" title="管理持卡人">
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
                <span class="ms-label">持卡人</span>
                <span class="ms-value">{{ baseStats.userCount }}</span>
              </div>
              <div class="mini-stat">
                <span class="ms-label">银行卡</span>
                <span class="ms-value">{{ baseStats.cardCount }}</span>
              </div>
              <div class="mini-stat">
                <span class="ms-label">当前费率</span>
                <span class="ms-value font-mono">{{ activeUserFeeRate }}</span>
              </div>
            </div>

            <div class="user-split">
              <div class="user-col">
                <div class="child-head">
                  <div class="child-title">
                    <span>主用户</span>
                    <span v-if="baseStats.userCount" class="child-count">{{ baseStats.userCount }}</span>
                  </div>

                  <div class="panel-actions">
                    <div v-if="userPageCount > 1" class="pager">
                      <button class="icon-btn" @click.stop="prevUserPage" title="上一页">
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
                      <button class="icon-btn" @click.stop="nextUserPage" title="下一页">
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

                <div class="user-list">
                  <template v-for="(u, idx) in pagedUsersFilled" :key="u?.userId ?? `u-ph-${idx}`">
                    <button
                      v-if="u"
                      :class="['list-item', 'user-item', { active: u.userId === activeUserId }]"
                      @click="setActiveUser(u.userId)"
                    >
                      <div class="li-left">
                        <div class="li-avatar" :class="{ ghost: u.userId === 0 }">
                          <span>{{ userInitial(u.userName) }}</span>
                        </div>
                        <div class="li-main">
                          <div class="li-title">{{ u.userName || '未命名' }}</div>
                          <div class="li-sub">{{ u.phone ? maskPhone(u.phone) : '未填写' }}</div>
                        </div>
                      </div>
                      <div class="li-right">
                        <span class="pill">{{ Number(u.cardCount || 0) }}张</span>
                        <span class="pill pill-fee">{{ formatRate(u.feeRate) }}%</span>
                      </div>
                    </button>

                    <div v-else class="list-item placeholder" aria-hidden="true"></div>
                  </template>

                  <div v-if="!loadingGroups && filteredGroupList.length === 0" class="empty-hint">暂无主用户</div>
                </div>
              </div>

              <div class="child-col">
                <div class="child-head">
                  <div class="child-title">
                    <span>子用户</span>
                    <span v-if="childTotal" class="child-count">{{ childTotal }}</span>
                  </div>

                  <div class="panel-actions">
                    <div v-if="childPageCount > 1" class="pager">
                      <button class="icon-btn" @click.stop="prevChildPage" title="上一页">
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
                      <span class="panel-meta">{{ childPageIndex + 1 }}/{{ childPageCount }}</span>
                      <button class="icon-btn" @click.stop="nextChildPage" title="下一页">
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

                <div class="child-list" v-loading="userTreeLoading">
                  <template v-if="activeUserId && activeUserId > 0">
                    <template v-for="(cu, idx) in pagedChildUsersFilled" :key="cu?.id ?? `c-ph-${idx}`">
                      <button
                        v-if="cu"
                        :class="['list-item', 'child-item', { active: Number(cu.id) === Number(activeOwnerId), disabled: Number(cu.status) !== 0 }]"
                        :disabled="Number(cu.status) !== 0"
                        @click="setActiveChildUser(cu)"
                      >
                        <div class="li-left">
                          <div class="li-avatar sm" :class="{ ghost: Number(cu.status) !== 0 }">
                            <span>{{ userInitial(cu.name) }}</span>
                          </div>
                          <div class="li-main">
                            <div class="li-title">{{ cu.name || '未命名' }}</div>
                            <div class="li-sub">{{ cu.phone ? maskPhone(cu.phone) : '未填写' }}</div>
                          </div>
                        </div>
                        <div class="li-right">
                          <span class="pill">{{ childCardCount(cu.id) }}张</span>
                          <span class="pill pill-fee">{{ formatRate(childFeeRate(cu)) }}%</span>
                        </div>
                      </button>

                      <div v-else class="list-item child-item placeholder" aria-hidden="true"></div>
                    </template>

                    <div v-if="!userTreeLoading && activeChildUsers.length === 0" class="empty-hint">暂无子用户</div>
                  </template>
                  <div v-else class="empty-hint">选择持卡人查看子用户</div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 银行卡管理 -->
        <section class="panel">
          <div class="panel-head">
            <div class="panel-title"><span class="panel-dot is-primary"></span>银行卡管理</div>
            <div class="panel-actions">
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
              <button class="icon-btn" @click="openBillsPage" :disabled="!activeCardId" title="查看账单">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                  <polyline points="14 2 14 8 20 8" />
                  <line x1="16" y1="13" x2="8" y2="13" />
                  <line x1="16" y1="17" x2="8" y2="17" />
                </svg>
              </button>
            </div>
          </div>

          <div class="panel-body">
            <div class="active-meta" v-if="activeUser">
              <div class="am-left">
                <div class="am-name">{{ activeUser.userName }}</div>
                <div class="am-sub">卡片 {{ Number(activeUser.cardCount || 0) }} · 费率 {{ formatRate(activeUser.feeRate) }}%</div>
              </div>
              <div class="am-right" v-if="activeCard">
                <StatusTag
                  :value="activeCard.status"
                  :label-map="CARD_STATUS_MAP"
                  :type-map="CARD_STATUS_TAG_TYPE"
                  size="small"
                  effect="light"
                />
              </div>
            </div>

            <div class="card-list">
              <template v-if="pagedCards.length">
                <button
                  v-for="c in pagedCards"
                  :key="c.id"
                  :class="['list-item', 'card-item', { active: c.id === activeCardId }]"
                  @click="setActiveCard(c.id)"
                >
                  <div class="li-left">
                    <div class="li-icon" :class="{ credit: c.cardType === CARD_TYPE_VALUE.CREDIT }">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="3" y="5" width="18" height="14" rx="2" />
                        <line x1="3" y1="10" x2="21" y2="10" />
                      </svg>
                    </div>
                    <div class="li-main">
                      <div class="li-title">
                        <span class="bank-name">{{ c.bankName }}</span>
                        <span class="muted"> {{ maskCardNo(c.cardNoLast4) }}</span>
                      </div>
                      <div class="li-sub">
                        <span class="muted">{{ CARD_TYPE_MAP[c.cardType] || '-' }}</span>
                        <i class="sub-divider"></i>
                        <template v-if="c.cardType === CARD_TYPE_VALUE.CREDIT">
                          <span v-if="c.billDay">账单日 {{ c.billDay }}日</span>
                          <span v-else class="muted">账单日 —</span>
                          <i class="sub-divider"></i>
                          <span v-if="c.repayDay">还款日 {{ c.repayDay }}日</span>
                          <span v-else class="muted">还款日 —</span>
                        </template>
                        <template v-else>
                          <span class="muted">—</span>
                        </template>
                      </div>
                    </div>
                  </div>

                  <div class="li-right">
                    <div class="amt">
                      <span class="amt-label">{{ c.cardType === CARD_TYPE_VALUE.CREDIT ? '额度' : '余额' }}</span>
                      <span class="amt-value font-mono">{{ formatMoneySafe(c.cardType === CARD_TYPE_VALUE.CREDIT ? c.creditLimit : c.balance) }}</span>
                    </div>
                    <div class="li-actions">
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
                </button>
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
              <span class="panel-meta">{{ currentYear }}年</span>
              <button class="icon-btn" @click="openBillsPage" :disabled="!activeCardId" title="全部账单">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="9 18 15 12 9 6" />
                </svg>
              </button>
            </div>
          </div>

          <div class="panel-body">
            <div class="mini-stats mini-stats-4" v-loading="billOverviewLoading">
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
                <span class="ms-label">代还总额</span>
                <span class="ms-value font-mono">¥{{ formatMoneySafe(billOverview.totalBillAmount) }}</span>
              </div>
            </div>

            <div class="bill-list" v-loading="recentBillsLoading">
              <template v-if="recentBills.length">
                <div v-for="b in recentBills" :key="b.id" class="bill-item">
                  <div class="bill-left">
                    <div class="bill-month">{{ b.billMonth }}</div>
                    <div class="bill-sub">
                      <span v-if="b.repayDate" class="muted">还款日 {{ fmtRepayDate(b.repayDate) }}</span>
                      <span v-else class="muted">—</span>
                    </div>
                  </div>
                  <div class="bill-mid">
                    <StatusTag :value="b.status" :label-map="BILL_STATUS_MAP" :type-map="BILL_STATUS_TAG_TYPE" size="small" effect="light" />
                  </div>
                  <div class="bill-right">
                    <span class="bill-amt font-mono">¥{{ formatMoneySafe(b.billAmount) }}</span>
                  </div>
                </div>
              </template>
              <div v-else class="empty-hint">
                <div v-if="!activeCardId">请选择一张银行卡查看账单</div>
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
              <div class="scope-switch">
                <button :class="['chip', 'sm', { active: profitScope === 'month' }]" @click="profitScope = 'month'">本月</button>
                <button :class="['chip', 'sm', { active: profitScope === 'year' }]" @click="profitScope = 'year'">本年</button>
              </div>
              <button class="icon-btn" @click="goProfits" title="收益详情">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="3 17 9 11 13 15 21 7" />
                  <polyline points="14 7 21 7 21 14" />
                </svg>
              </button>
            </div>
          </div>

          <div class="panel-body" v-loading="profitLoading">
            <div class="profit-kpi">
              <div class="pk-label">{{ profitScopeLabel }}</div>
              <div class="pk-value" :class="Number(profitOverview.totalNetProfit || 0) >= 0 ? 'pos' : 'neg'">
                ¥{{ formatMoneySafe(profitOverview.totalNetProfit) }}
              </div>
              <div class="pk-sub">手续费收入 - POS成本</div>
            </div>

            <div class="profit-grid">
              <div class="profit-metric">
                <span class="pm-label">手续费收入</span>
                <span class="pm-value pos font-mono">¥{{ formatMoneySafe(profitOverview.totalFeeAmount) }}</span>
              </div>
              <div class="profit-metric">
                <span class="pm-label">POS成本</span>
                <span class="pm-value neg font-mono">¥{{ formatMoneySafe(profitOverview.totalPosCostAmount) }}</span>
              </div>
              <div class="profit-metric">
                <span class="pm-label">代还金额</span>
                <span class="pm-value font-mono">¥{{ formatMoneySafe(profitOverview.totalBillAmount) }}</span>
              </div>
            </div>

            <div class="panel-hint" v-if="activeCard">当前卡片：{{ activeCard.bankName }} {{ maskCardNo(activeCard.cardNoLast4) }}</div>
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
            <el-option v-for="u in userOptions" :key="u.id" :label="u.name" :value="u.id">
              <div class="user-opt">
                <span>{{ u.name }}</span>
                <el-tag v-if="!u.parentId" size="small" type="" effect="light" round style="margin-left:6px">主账户</el-tag>
                <el-tag v-else size="small" type="info" effect="plain" round style="margin-left:6px">子用户</el-tag>
              </div>
            </el-option>
          </el-select>
          <el-button type="primary" link size="small" style="margin-left:8px" @click="goUsers">+ 管理持卡人</el-button>
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
        <template v-if="form.cardType === CARD_TYPE_VALUE.CREDIT">
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
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Cards' })
import { computed, onActivated, onMounted, reactive, ref, watch } from 'vue'
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
import { getBillOverviewApi, getBillPageApi } from '@/api/bill'
import { getProfitOverviewApi } from '@/api/profit'
import { formatMoney, formatRate, maskCardNo, maskPhone } from '@/utils/formatters'
import {
  BILL_STATUS_MAP,
  BILL_STATUS_TAG_TYPE,
  CARD_STATUS_MAP,
  CARD_STATUS_TAG_TYPE,
  CARD_TYPE_MAP,
  CARD_TYPE_OPTIONS,
  CARD_TYPE_VALUE,
  CARD_STATUS_OPTIONS
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
  billMonth: string
  repayDate: string | null
  billAmount: number | null
  status: number
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
  totalNetProfit: number
}

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

// ====== 顶部筛选 ======
const keyword = ref('')
const searchFocused = ref(false)
const query = reactive({
  bankName: '',
  cardNoLast4: '',
  cardType: undefined as number | undefined
})

// ====== 列表数据 ======
const loadingGroups = ref(false)
const groupList = ref<UserGroup[]>([])
const activeUserId = ref<number | null>(null)
const activeCardId = ref<number | null>(null)
const activeOwnerId = ref<number | null>(null)

const userPageIndex = ref(0)
const cardPageIndex = ref(0)
const childPageIndex = ref(0)

const baseStats = computed(() => {
  const users = filteredGroupList.value.filter(item => item.userId !== 0)
  const cardCount = groupList.value.reduce((sum, item) => sum + Number(item.cardCount || 0), 0)
  return {
    userCount: users.length,
    cardCount
  }
})

const activeUser = computed<UserGroup | undefined>(() => {
  if (!groupList.value.length) return undefined
  const found = groupList.value.find(u => u.userId === activeUserId.value)
  return found || groupList.value[0]
})

const activeCards = computed<any[]>(() => {
  const cards = (activeUser.value?.cards || []) as any[]
  if (!activeOwnerId.value) return cards
  return cards.filter((c: any) => Number(c.userId) === Number(activeOwnerId.value))
})

const activeCard = computed<any | undefined>(() => {
  if (!activeCards.value.length) return undefined
  const found = activeCards.value.find(c => Number(c.id) === Number(activeCardId.value))
  return found || activeCards.value[0]
})

const activeCardLabel = computed(() => {
  if (!activeCard.value) return '—'
  return `${activeCard.value.bankName || ''}${activeCard.value.cardNoLast4 ? ` · ${activeCard.value.cardNoLast4}` : ''}`.trim() || '—'
})

const activeUserFeeRate = computed(() => {
  if (!activeUser.value) return '—'
  return `${formatRate(activeUser.value.feeRate)}%`
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
    if (name.includes(k)) return true
    if (phone.toLowerCase().includes(k)) return true
    if (digits && phoneDigits.includes(digits)) return true
    return false
  }

  function isMatchChildNamePhone(c: any) {
    const name = String(c?.name || '').toLowerCase()
    const phone = String(c?.phone || '')
    const phoneDigits = phone.replace(/\D/g, '')
    if (name.includes(k)) return true
    if (phone.toLowerCase().includes(k)) return true
    if (digits && phoneDigits.includes(digits)) return true
    return false
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

async function ensureUserTree(force = false) {
  if (userTreeLoading.value) return
  if (!force && userTreeLoaded.value) return
  userTreeLoading.value = true
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
    userTreeList.value = list
  } catch {
    topToChildrenMap.value = new Map()
    userTreeLoaded.value = false
    userTreeList.value = []
  } finally {
    userTreeLoading.value = false
  }
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
    if (name.includes(k)) return true
    if (phone.toLowerCase().includes(k)) return true
    if (digits && phoneDigits.includes(digits)) return true
    return false
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

function childCardCount(childId: number) {
  const cards = activeCards.value || []
  const count = cards.filter((c: any) => Number(c.userId) === Number(childId)).length
  return Number(count || 0)
}

const canAddCardForUser = computed(() => {
  return !!activeUser.value && Number(activeUser.value.userId) > 0
})

// 主用户区域：3条/页更稳，且与子用户对称
const USER_PAGE_SIZE = 3
const CARD_PAGE_SIZE = 4

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

function setCardType(type: number | undefined) {
  query.cardType = type
  fetchGroups()
}

function applyKeyword() {
  const v = keyword.value.trim()
  if (!v) {
    query.bankName = ''
    query.cardNoLast4 = ''
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
  fetchGroups()
}

function clearKeyword() {
  keyword.value = ''
  query.bankName = ''
  query.cardNoLast4 = ''
  fetchGroups()
}

function setActiveUser(userId: number) {
  activeUserId.value = userId
  activeOwnerId.value = null
  cardPageIndex.value = 0
  childPageIndex.value = 0
  ensureUserTree()
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

async function fetchGroups() {
  loadingGroups.value = true
  try {
    const res: any = await getCardsGroupedByUserApi(query)
    groupList.value = res.data || []
    syncActiveSelection()
  } finally {
    loadingGroups.value = false
  }
}

function refreshAll() {
  fetchGroups()
  ensureUserTree(true)
}

// ====== 账单模块 ======
const billOverviewLoading = ref(false)
const billOverview = ref<BillOverview>({
  billCount: 0,
  pendingCount: 0,
  overdueCount: 0,
  totalBillAmount: 0
})

const recentBillsLoading = ref(false)
const recentBills = ref<BillRow[]>([])

async function fetchBillOverview() {
  if (!activeCardId.value) {
    billOverview.value = { billCount: 0, pendingCount: 0, overdueCount: 0, totalBillAmount: 0 }
    return
  }

  billOverviewLoading.value = true
  try {
    const res: any = await getBillOverviewApi({
      cardId: activeCardId.value,
      ownerId: undefined,
      cardName: '',
      year: currentYear,
      billMonth: '',
      status: undefined
    })

    billOverview.value = {
      billCount: Number(res.data?.billCount ?? 0),
      pendingCount: Number(res.data?.pendingCount ?? 0),
      overdueCount: Number(res.data?.overdueCount ?? 0),
      totalBillAmount: Number(res.data?.totalBillAmount ?? 0)
    }
  } finally {
    billOverviewLoading.value = false
  }
}

async function fetchRecentBills() {
  if (!activeCardId.value) {
    recentBills.value = []
    return
  }

  recentBillsLoading.value = true
  try {
    const res: any = await getBillPageApi({
      current: 1,
      size: 12,
      cardId: activeCardId.value,
      ownerId: undefined,
      cardName: '',
      year: currentYear,
      billMonth: '',
      status: undefined
    })
    recentBills.value = (res.data?.records || []) as BillRow[]

    // 账单明细格子：仅展示本年；按“从本月开始依次往后”，不回绕到1月
    const cur = `${currentYear}-${String(currentMonth).padStart(2, '0')}`
    recentBills.value = recentBills.value
      .filter(r => String(r?.billMonth || '').startsWith(`${currentYear}-`))
      .filter(r => {
        const m = String(r?.billMonth || '').match(/^(\d{4})-(\d{2})$/)
        if (!m) return false
        const mo = Number(m[2])
        return Number.isFinite(mo) && mo >= currentMonth
      })

    recentBills.value.sort((a, b) => {
      const aMonth = String(a?.billMonth || '')
      const bMonth = String(b?.billMonth || '')

      const aIsCur = aMonth === cur
      const bIsCur = bMonth === cur
      if (aIsCur && !bIsCur) return -1
      if (!aIsCur && bIsCur) return 1

      return aMonth.localeCompare(bMonth)
    })
  } finally {
    recentBillsLoading.value = false
  }
}

function fmtRepayDate(date: string) {
  const match = String(date).match(/(\d{4})-(\d{2})-(\d{2})/)
  return match ? `${Number(match[2])}月${Number(match[3])}日` : date
}

function openBillsPage() {
  if (!activeCardId.value) return
  router.push({
    path: '/bills',
    query: { cardId: String(activeCardId.value), year: String(currentYear) }
  })
}

// ====== 收益模块 ======
const profitScope = ref<'month' | 'year'>('month')
const profitLoading = ref(false)
const profitOverview = ref<ProfitOverview>({
  year: currentYear,
  month: undefined,
  userCount: 0,
  cardCount: 0,
  totalBillAmount: 0,
  totalFeeAmount: 0,
  totalPosCostAmount: 0,
  totalNetProfit: 0
})

const profitScopeLabel = computed(() => {
  return profitScope.value === 'month' ? `${currentYear}年${currentMonth}月` : `${currentYear}年`
})

async function fetchProfitOverview() {
  if (!activeCardId.value) {
    profitOverview.value = {
      year: currentYear,
      month: undefined,
      userCount: 0,
      cardCount: 0,
      totalBillAmount: 0,
      totalFeeAmount: 0,
      totalPosCostAmount: 0,
      totalNetProfit: 0
    }
    return
  }

  profitLoading.value = true
  try {
    const res: any = await getProfitOverviewApi({
      year: currentYear,
      month: profitScope.value === 'month' ? currentMonth : undefined,
      cardId: activeCardId.value,
      userId: undefined
    })

    profitOverview.value = {
      year: res.data?.year ?? currentYear,
      month: res.data?.month,
      userCount: Number(res.data?.userCount ?? 0),
      cardCount: Number(res.data?.cardCount ?? 0),
      totalBillAmount: Number(res.data?.totalBillAmount ?? 0),
      totalFeeAmount: Number(res.data?.totalFeeAmount ?? 0),
      totalPosCostAmount: Number(res.data?.totalPosCostAmount ?? 0),
      totalNetProfit: Number(res.data?.totalNetProfit ?? 0)
    }
  } finally {
    profitLoading.value = false
  }
}

function goProfits() {
  router.push({
    path: '/profits',
    query: activeCardId.value ? { cardId: String(activeCardId.value), year: String(currentYear) } : {}
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
  ownerRelation: '本人',
  ownerName: '',
  cardType: Number(CARD_TYPE_VALUE.CREDIT),
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
  cardType: [{ required: true, message: '请选择卡片类型', trigger: 'change' }]
}))

async function openAddCard() {
  isEdit.value = false
  Object.assign(formData, defaultForm)
  if (activeOwnerId.value) {
    formData.userId = activeOwnerId.value
  } else if (activeUser.value?.userId) {
    formData.userId = activeUser.value.userId
  }
  await syncUserOptions()
  dialogVisible.value = true
}

async function openAddCardWithActiveUser() {
  if (!canAddCardForUser.value) return
  isEdit.value = false
  Object.assign(formData, defaultForm, { userId: activeOwnerId.value || activeUser.value?.userId })
  await syncUserOptions()
  dialogVisible.value = true
}

function openEditCard(row: any) {
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

  const clearFields = oldType === CARD_TYPE_VALUE.CREDIT ? '信用额度、账单日、还款日' : '余额、总额度'
  try {
    await ElMessageBox.confirm(`切换卡片类型将清空${clearFields}，是否继续？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    formData.cardType = newType
    clearCardTypeFields(oldType)
  } catch {
    /* cancel */
  }
}

function clearCardTypeFields(cardType: number) {
  if (cardType === CARD_TYPE_VALUE.CREDIT) Object.assign(formData, { creditLimit: '', billDay: '', repayDay: '' })
  else if (cardType === CARD_TYPE_VALUE.DEBIT) Object.assign(formData, { balance: '', totalLimit: '' })
}

async function handleSubmit() {
  const data: any = { ...formData }
  let createdCardId: number | undefined

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
  if (!userTreeList.value.length) {
    try {
      const res: any = await getUserTreeApi()
      userTreeList.value = res.data || []
    } catch {
      userTreeList.value = []
    }
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

watch(
  () => keyword.value,
  () => {
    applyKeyword()
  }
)

// ====== 联动刷新 ======
watch(
  () => activeUserId.value,
  () => {
    activeOwnerId.value = null
    if (!activeUser.value) return
    childPageIndex.value = 0
    if (activeUserId.value && activeUserId.value > 0) {
      ensureUserTree()
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
    await ensureUserTree()

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
  () => [activeCardId.value, profitScope.value],
  () => {
    fetchBillOverview()
    fetchRecentBills()
    fetchProfitOverview()
  },
  { immediate: true }
)

onMounted(() => {
  fetchGroups()
})

onActivated(() => {
  fetchGroups()
  ensureUserTree(true)
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
  margin: -20px;
  width: calc(100% + 40px);
  height: calc(100% + 40px);
  min-height: 0;
  background: $bg;
  overflow: hidden;
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
  padding: 16px 28px;
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
  width: 34px;
  height: 34px;
  border-radius: 12px;
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
  font-size: 18px;
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
  gap: 10px;
  flex-shrink: 0;
}

.search-box {
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

.type-switch {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px;
  background: rgba(148,163,184,.12);
  border-radius: 999px;
  border: 1px solid rgba(219,226,234,.7);
}

.chip {
  height: 26px;
  padding: 0 10px;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  background: transparent;
  color: $ink2;
  font-size: 12px;
  font-weight: 700;
  transition: all .15s;
  &.active {
    background: $surface;
    color: $primary;
    box-shadow: 0 6px 14px rgba(15,23,42,.06);
  }
  &:hover { color: $primary; }
  &.sm { height: 24px; padding: 0 9px; font-size: 11.5px; }
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
  gap: 12px;
  padding: 12px 16px 16px;
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
    padding: 8px 12px 10px;
    gap: 8px;
  }

  .mini-stat {
    padding: 9px 10px 8px;
    gap: 5px;
  }

  .list-item {
    padding: 9px 9px;
    min-height: 58px;
    height: 100%;
  }

}

.list-item.placeholder {
  cursor: default;
  background: rgba(148,163,184,.06);
  border-style: dashed;
  border-color: rgba(219,226,234,.7);
  box-shadow: none;
  min-height: 58px;
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

.panel-body {
  padding: 10px 12px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.panel-hint {
  font-size: 11.5px;
  color: $faint;
  padding-left: 2px;
  align-self: flex-end;
}

.mini-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.mini-stats-4 {
  grid-template-columns: repeat(4, minmax(0, 1fr));
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
}

.ms-label {
  font-size: 11.5px;
  color: $sub;
  font-weight: 700;
}

.ms-value {
  font-size: 16px;
  font-weight: 800;
  color: $ink;
  line-height: 1.1;
  &.warn { color: $warning; }
  &.danger { color: $danger; }
}

.user-split {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  overflow: hidden;
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
  --user-row-h: minmax(58px, 1fr);
}

.user-list,
.child-list {
  height: calc(100% - var(--user-head-h));
  display: grid;
  grid-template-rows: repeat(3, var(--user-row-h));
  gap: 8px;
  align-content: stretch;
  align-items: stretch;
  min-height: 0;
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
  gap: 8px;
  min-height: 0;
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
  min-height: 58px;
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
button.list-item {
  appearance: none;
  -webkit-appearance: none;
  text-align: left;
  width: 100%;
  border: 1px solid rgba(219,226,234,.85);
}

.panel.is-users .list-item,
.panel.is-users .list-item.placeholder {
  height: 100%;
  min-height: 58px;
}

.li-left { display: flex; align-items: center; gap: 10px; min-width: 0; }
.li-main { display: flex; flex-direction: column; gap: 3px; min-width: 0; }
.li-title { font-size: 13px; font-weight: 800; color: $ink; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.li-sub { font-size: 11.5px; color: $sub; display: flex; align-items: center; gap: 6px; min-width: 0; }
.li-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }

.li-avatar {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
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

.pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 800;
  color: $ink2;
  background: rgba(148,163,184,.12);
  border: 1px solid rgba(219,226,234,.75);
  &.pill-fee {
    color: $primary;
    background: $primary-soft;
    border-color: rgba($primary,.18);
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
  align-items: baseline;
  gap: 6px;
  text-align: right;
}

.amt-label { font-size: 11.5px; color: $sub; font-weight: 800; }
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
  gap: 8px;
  overflow: hidden;
}

.bill-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 94px 120px;
  gap: 10px;
  align-items: center;
  padding: 10px 10px;
  border-radius: 14px;
  background: $surface;
  border: 1px solid rgba(219,226,234,.85);
}

.bill-month { font-size: 13px; font-weight: 900; color: $ink; }
.bill-sub { margin-top: 3px; font-size: 11.5px; color: $sub; font-weight: 700; }
.bill-right { text-align: right; }
.bill-amt { font-size: 13px; font-weight: 900; color: $ink; }

.scope-switch { display: flex; align-items: center; gap: 6px; }

.profit-kpi {
  padding: 12px 12px;
  border-radius: 16px;
  border: 1px solid rgba(219,226,234,.85);
  background: linear-gradient(180deg, rgba(255,255,255,.99) 0%, rgba($primary,.03) 180%);
}

.pk-label { font-size: 11.5px; color: $sub; font-weight: 800; }
.pk-value { margin-top: 8px; font-size: 26px; font-weight: 900; letter-spacing: -0.5px; }
.pk-value.pos { color: $success; }
.pk-value.neg { color: $danger; }
.pk-sub { margin-top: 6px; font-size: 11.5px; color: $faint; font-weight: 700; }

.profit-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.profit-metric {
  border-radius: 14px;
  border: 1px solid rgba(219,226,234,.75);
  background: rgba(148,163,184,.10);
  padding: 10px 10px 9px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.pm-label { font-size: 11.5px; color: $sub; font-weight: 800; }
.pm-value { font-size: 13px; font-weight: 900; color: $ink; }
.pm-value.pos { color: $success; }
.pm-value.neg { color: $danger; }

.font-mono {
  font-family: var(--font-mono);
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
  .type-switch { display: none; }
  .bill-item { grid-template-columns: minmax(0, 1fr) 86px 110px; }
  .profit-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
