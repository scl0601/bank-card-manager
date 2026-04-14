<template>
  <div class="calendar-page" tabindex="0" @keydown="handleKeydown" ref="pageRef">
    <div class="page-header">
      <div class="header-left">
        <span class="page-title-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
        </span>
        <div class="header-title-group">
          <h1 class="page-title">日历计划</h1>
        </div>
      </div>
      <div class="header-actions">
        <div class="search-box" :class="{ focused: searchFocused }">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="search-icon"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-model="keyword" placeholder="搜索日程标题或备注..." @focus="searchFocused = true" @blur="searchFocused = false" />
          <button v-if="keyword" class="search-clear" @click="clearSearch">&times;</button>
        </div>
        <!-- [E] 导出/打印 -->
        <el-dropdown trigger="click" @command="handleExportCmd">
          <button class="icon-btn" :title="viewMode === 'month' ? '导出本月数据 / PDF' : '导出当天日程'">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <template v-if="viewMode === 'day'">
                <el-dropdown-item command="export-day">导出当天 CSV</el-dropdown-item>
                <el-dropdown-item command="print-day" divided>打印当天日程</el-dropdown-item>
              </template>
              <template v-else>
                <el-dropdown-item command="export-month">导出本月 CSV</el-dropdown-item>
                <el-dropdown-item command="export-month-filtered">导出当前筛选结果 CSV</el-dropdown-item>
<!--                <el-dropdown-item command="export-month-pdf" divided>导出本月 PDF</el-dropdown-item>-->
              </template>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <!-- [M] 视图模式切换 -->
        <div class="view-mode-switch">
          <button :class="['vm-btn',{active:viewMode==='day'}]" @click="switchToTodayDayView" title="日视图">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            日
          </button>
          <button :class="['vm-btn',{active:viewMode==='month'}]" @click="viewMode='month'" title="月视图">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
            月
          </button>
        </div>
        <el-button type="primary" @click="openDrawer(null)">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="margin-right:5px"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新建日程
        </el-button>
      </div>
    </div>

    <!-- [M] 月视图（全览模式） -->
    <div v-show="viewMode === 'month'" class="main-body main-month" @click="closeMoPopover">
      <div class="month-overview">
        <!-- 月份导航 + 筛选 -->
        <div class="mo-toolbar">
          <div class="mo-toolbar-actions">
            <div class="mo-nav">
              <button class="nav-btn" @click="prevMonth" :disabled="monthChanging">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
              </button>
              <!-- 可点击月份标题 + 快速选月弹窗 -->
              <div class="mo-nav-center" ref="moNavCenterRef">
                <transition name="mo-month-fade" mode="out-in">
                  <span
                    class="mo-title mo-month-btn"
                    :key="currentYear + '-' + currentMonth"
                    @click.stop="toggleMoMonthPicker"
                  >
                    {{ currentYear }}年{{ String(currentMonth).padStart(2,'0') }}月
                    <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="mo-mp-arrow" :class="{ open: moShowMonthPicker }"><polyline points="6 9 12 15 18 9"/></svg>
                  </span>
                </transition>
                <!-- 月份选择弹窗 -->
                <div v-if="moShowMonthPicker" class="mo-month-picker-popup" ref="moMonthPickerRef">
                  <div class="mp-head">
                    <button class="mp-yr-btn" @click.stop="moMpYear--"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg></button>
                    <span class="mp-yr-label">{{ moMpYear }} 年</span>
                    <button class="mp-yr-btn" @click.stop="moMpYear++"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg></button>
                  </div>
                  <div class="mp-grid">
                    <button
                      v-for="m in 12" :key="m"
                      :class="['mp-m-btn', { active: moMpYear === currentYear && m === currentMonth }]"
                      @click.stop="jumpMoToMonth(moMpYear, m)"
                    >{{ m }}月</button>
                  </div>
                </div>
              </div>
              <button class="nav-btn" @click="nextMonth" :disabled="monthChanging">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
              </button>
              <button class="today-btn" @click="goToday">今天</button>
            </div>
            <div class="mo-filters">
              <el-select v-model="moFilterCategory" placeholder="分类筛选" clearable size="small" style="width:100px">
                <el-option v-for="c in EVENT_CATEGORY_OPTIONS" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
              <!-- [M] 状态筛选按钮组（每按钮含颜色圆点+tooltip说明） -->
              <div class="mo-status-filters">
                <button :class="['msf-btn', { active: moFilterStatus === undefined }]" @click="moFilterStatus = undefined">全部</button>
                <el-tooltip v-for="item in statusFilterOptions" :key="item.key" :content="item.label" placement="bottom" :show-after="400">
                  <button :class="['msf-btn', `msf-${item.key}`, { active: moFilterStatus === item.value }]" @click="moFilterStatus = item.value"><i class="msf-dot" :style="{ background: item.color }"></i>{{ item.label }}</button>
                </el-tooltip>
              </div>
              <div class="legend mo-legend" v-if="EVENT_CATEGORY_OPTIONS.length > 0">
                <span v-for="(cat,ci) in EVENT_CATEGORY_OPTIONS" :key="'c'+ci" class="leg-item">
                  <i :style="{background:categoryColor[cat.value]}"></i>{{ cat.label }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="mo-calendar-stage">
          <div class="mo-calendar-panel">
            <!-- 星期标题行 -->
            <div class="mo-week-header">
              <span v-for="(w,wi) in weekDays" :key="w" :class="{'wk-end':wi>=5}">{{ w }}</span>
            </div>

            <!-- 月视图网格 -->
            <transition name="mo-grid-fade" mode="out-in">
            <div
              class="mo-grid"
              ref="moGridRef"
              :style="{ '--mo-row-count': String(moRowCount) }"
              :key="`${currentYear}-${currentMonth}`"
            >
              <div
                v-for="(cell,ci) in moAllCells"
                :key="cell.key||ci"
                :class="[
                  'mo-cell',
                  !cell.isOther ? getMoCellToneClass(cell.date) : '',
                  {
                    'other': cell.isOther,
                    'today': !cell.isOther && cell.isToday && shouldShowMoTodayState(cell.date),
                    'weekend': !cell.isOther && cell.isWeekend,
                    'holiday': !cell.isOther && !!cell.holidayName,
                    'mo-has-events': !cell.isOther && getMoDayEvents(cell.date).length > 0,
                    'mo-cell-selected': isMoCellSelected(cell.date),
                    'mo-cell-popover-active': isMoPopoverActiveCell(cell.date),
                    'mo-cell-filter-match': !cell.isOther && isMoCellFilteredMatch(cell.date),
                    'mo-search-match': !cell.isOther && getMoDayMatchCount(cell.date) > 0,
                    'mo-cell-dimmed': !cell.isOther && isMoCellFilteredOut(cell.date),
                    'mo-cell-flash': moCellFlash && !cell.isOther && cell.isToday && shouldShowMoTodayState(cell.date)
                  }
                ]"
                @click.stop="onMoCellClick(cell, $event)"
              >
                <div class="mo-cell-hd">
                  <b class="mo-day-num" @click.stop="jumpToDayByDate(cell.date)">{{ cell.day }}</b>
                  <small v-if="cell.holidayName && !cell.isOther" class="mo-holiday-tag" :title="cell.holidayName">{{ cell.holidayName }}</small>
                  <div class="mo-cell-hd-right" v-if="!cell.isOther">
                    <span v-if="getMoDayEvents(cell.date).length > 0" class="mo-day-badge">{{ getMoDayEvents(cell.date).length }}</span>
                    <button class="mo-cell-add-btn" @click.stop="openDrawer(null, cell.date)" title="快速新建日程">
                      <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                    </button>
                  </div>
                </div>

                <template v-if="!cell.isOther && getMoDayEvents(cell.date).length > 0">
                  <span v-if="getMoDayMatchCount(cell.date) > 0" class="mo-cell-summary-hit">命中 {{ getMoDayMatchCount(cell.date) }}</span>
                  <div class="mo-event-list">
                    <template v-for="row in getMoCellRows(cell.date)" :key="row.key">
                      <div
                        v-if="row.type === 'event'"
                        :class="['mo-event-item', {
                          'mo-todo': row.event.status === EVENT_STATUS_VALUE.TODO,
                          'mo-doing': row.event.status === EVENT_STATUS_VALUE.DOING,
                          'mo-done': row.event.status === EVENT_STATUS_VALUE.DONE,
                          'mo-cancelled': row.event.status === EVENT_STATUS_VALUE.CANCELLED,
                          'mo-ev-hit': !!keyword.trim()
                        }]"
                        @click.stop="openDrawer(row.event)"
                        :style="{ '--cat-color': categoryColor[row.event.category] }"
                      >
                        <span class="mo-ev-dot" :style="{ background: categoryColor[row.event.category] }"></span>
                        <span class="mo-ev-time">{{ row.event.startTime ? formatTime(row.event.startTime) : '全天' }}</span>
                        <span class="mo-ev-title" :title="row.event.title">{{ row.event.title }}</span>
                        <span v-if="row.event.status === EVENT_STATUS_VALUE.DOING" class="mo-ev-doing-dot"></span>
                        <span v-else-if="row.event.status === EVENT_STATUS_VALUE.CANCELLED" class="mo-ev-cancel-icon">&times;</span>
                      </div>
                      <button
                        v-else-if="row.type === 'more'"
                        type="button"
                        :class="['mo-more-tip', { 'mo-more-active': moPopoverDate === cell.date && moPopoverVisible }]"
                        @click.stop="toggleMoPopover(cell, $event)"
                      >
                        <svg width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline :points="moPopoverDate === cell.date && moPopoverVisible ? '18 9 12 15 6 9' : '6 9 12 15 18 9'"/></svg>
                        {{ moPopoverDate === cell.date && moPopoverVisible ? '收起' : '还有' }} {{ row.remaining }} 项
                      </button>
                      <div v-else class="mo-event-placeholder" aria-hidden="true"></div>
                    </template>
                  </div>
                  <div v-if="cell.isToday" class="mo-cell-progress">
                    <div class="mo-cell-progress-fill" :style="{ width: getMoDayDonePct(cell.date) + '%' }"></div>
                  </div>
                </template>

                <div
                  v-else-if="!cell.isOther && !isMoCellFilteredOut(cell.date) && (!cell.isToday || shouldShowMoTodayState(cell.date))"
                  class="mo-empty-cell"
                  @click.stop="onMoCellClick(cell, $event)"
                >
                  <button class="mo-empty-add-hint" @click.stop="openDrawer(null, cell.date)" title="快速新建日程">
                    <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                    新建
                  </button>
                </div>
              </div>
            </div>
            </transition>

            <!-- 底部统计栏 -->
            <div class="mo-footer">
              <div class="mo-footer-stats">
                <span class="mo-stat">共 <strong>{{ totalEvents }}</strong> 个日程</span>
                <span class="mo-stat mo-stat-todo">待办 <strong>{{ stats.todoCount }}</strong></span>
                <span class="mo-stat mo-stat-doing">进行中 <strong>{{ stats.doingCount }}</strong></span>
                <span class="mo-stat mo-stat-done">已完成 <strong>{{ stats.doneCount }}</strong></span>
                <span class="mo-stat mo-stat-cancelled">已取消 <strong>{{ stats.cancelledCount }}</strong></span>
              </div>
              <div v-if="monthData.length > 0" class="mo-footer-progress">
                <span class="mo-footer-pct-label">本月完成率</span>
                <div class="mo-footer-track">
                  <div class="mo-footer-fill" :style="{ width: statPercent('done') + '%' }"></div>
                </div>
                <span class="mo-footer-pct">{{ statPercent('done') }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Day Popover 浮层 -->
      <teleport to="body">
        <transition name="popover-fade">
          <div
            v-if="moPopoverVisible"
            :class="['mo-day-popover', `is-${moPopoverPlacement}`]"
            ref="moPopoverRef"
            :style="moPopoverStyle"
            @click.stop
          >
            <div class="mdp-header">
              <div class="mdp-title-block">
                <div class="mdp-title">
                  <span class="mdp-date-text">{{ moPopoverTitle }}</span>
                  <el-tag v-if="moPopoverIsToday" size="small" type="primary" effect="dark" round style="font-size:10px;line-height:18px;">今天</el-tag>
                </div>
                <div v-if="moPopoverEvents.length > 0" class="mdp-subtitle">
                  <span>{{ moPopoverStats.total }} 项日程</span>
                  <span>完成率 {{ moPopoverDonePercent }}%</span>
                  <span v-if="moPopoverKeywordHitCount > 0">命中 {{ moPopoverKeywordHitCount }}</span>
                </div>
              </div>
              <button class="mdp-close" type="button" title="收起浮窗" aria-label="收起浮窗" @click.stop="closeMoPopover">
                <span>收起</span>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <template v-if="moPopoverEvents.length === 0">
              <div class="mdp-body">
                <div class="mdp-empty">
                  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="color:#c9cdd4"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
                  <p>当天暂无日程</p>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="mdp-summary">
                <span class="mdp-summary-chip mdp-summary-total">共 {{ moPopoverStats.total }} 项</span>
                <span v-if="moPopoverStats.doingCount" class="mdp-summary-chip is-doing">进行中 {{ moPopoverStats.doingCount }}</span>
                <span v-if="moPopoverStats.todoCount" class="mdp-summary-chip is-todo">待办 {{ moPopoverStats.todoCount }}</span>
                <span v-if="moPopoverStats.doneCount" class="mdp-summary-chip is-done">已完成 {{ moPopoverStats.doneCount }}</span>
                <span v-if="moPopoverStats.cancelledCount" class="mdp-summary-chip is-cancelled">已取消 {{ moPopoverStats.cancelledCount }}</span>
                <span v-if="moPopoverKeywordHitCount > 0" class="mdp-summary-chip is-search">搜索命中 {{ moPopoverKeywordHitCount }}</span>
              </div>
              <div class="mdp-body">
                <section v-for="section in moPopoverSections" :key="section.key" class="mdp-section">
                  <div class="mdp-section-head">
                    <span class="mdp-section-title" :class="`is-${section.key}`">{{ section.label }}</span>
                    <span class="mdp-section-count">{{ section.events.length }}</span>
                  </div>
                  <div
                    v-for="ev in section.events"
                    :key="ev.id"
                    :class="['mdp-ev-item', {
                      'mdp-ev-done': ev.status === EVENT_STATUS_VALUE.DONE,
                      'mdp-ev-doing': ev.status === EVENT_STATUS_VALUE.DOING,
                      'mdp-ev-cancelled': ev.status === EVENT_STATUS_VALUE.CANCELLED
                    }]"
                    @click.stop="openDrawer(ev); closeMoPopover()"
                  >
                    <span class="mdp-ev-color" :style="{background:categoryColor[ev.category]}"></span>
                    <div class="mdp-ev-main">
                      <div class="mdp-ev-top">
                        <span class="mdp-ev-title">{{ ev.title }}</span>
                        <span class="mdp-ev-time-range">{{ formatTime(ev.startTime) || '全天' }}<template v-if="ev.endTime"> - {{ formatTime(ev.endTime) }}</template></span>
                      </div>
                      <div class="mdp-ev-meta">
                        <span class="mdp-ev-cat" :style="{ color: categoryColor[ev.category], background: categoryColorLight[ev.category] }">{{ EVENT_CATEGORY_MAP[ev.category] }}</span>
                        <span v-if="ev.priority===2" class="mdp-priority-chip is-urgent">紧急</span>
                        <span v-else-if="ev.priority===1" class="mdp-priority-chip is-important">重要</span>
                      </div>
                      <div class="mdp-ev-actions" @click.stop>
                        <button
                          v-for="action in moQuickStatusActions"
                          :key="action.key"
                          type="button"
                          :class="['mdp-ev-action-chip', `is-${action.key}`, {
                            'is-active': ev.status === action.value,
                            'is-pending': isEventStatusPending(ev.id)
                          }]"
                          :disabled="isEventStatusPending(ev.id)"
                          @click.stop="onMoQuickStatusChange(ev, action.value)"
                        >
                          {{ action.shortLabel }}
                        </button>
                        <button
                          v-if="ev.status === EVENT_STATUS_VALUE.CANCELLED"
                          type="button"
                          class="mdp-ev-action-chip is-delete"
                          :disabled="isEventStatusPending(ev.id)"
                          @click.stop="onDelete(ev.id)"
                        >删除</button>
                      </div>
                      <div v-if="getEventRemark(ev)" class="mdp-ev-remark">{{ getEventRemark(ev) }}</div>
                    </div>
                    <span class="mdp-ev-status-icon" :class="[`mdp-s-${ev.status}`, { 'is-loading': isEventStatusPending(ev.id) }]">
                      <svg v-if="ev.status===EVENT_STATUS_VALUE.DONE" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
                      <span v-else-if="ev.status===EVENT_STATUS_VALUE.DOING" class="doing-pulse-sm"></span>
                      <span v-else-if="ev.status===EVENT_STATUS_VALUE.CANCELLED" class="mdp-cancel-mark">×</span>
                      <svg v-else width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                    </span>
                  </div>
                </section>
              </div>
            </template>
            <div class="mdp-footer">
              <button class="mdp-btn-new" @click.stop="openDrawer(null, moPopoverDate || undefined); closeMoPopover()">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                新建日程
              </button>
              <button class="mdp-btn-detail" @click.stop="jumpToDay()">
                查看全天
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>
              </button>
            </div>
          </div>
        </transition>
      </teleport>
    </div>

    <!-- [M] 日视图（原有功能） -->
    <div v-show="viewMode === 'day'" class="main-body main-day">
      <div class="left-panel">
        <div class="stats-row">
          <div class="stat-card stat-all" @click="setDayCalendarQuickFilter('all')" :class="{ 'stat-active': dayCalendarQuickFilter === 'all' }">

            <div class="stat-card-top">
              <div class="stat-icon-wrap"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg></div>
            </div>
            <div class="stat-info"><em>{{ stats.todoCount + stats.doingCount + stats.doneCount }}</em><i>全部</i></div>
            <div class="stat-bar"><div class="stat-bar-inner" :style="{ width: '100%' }"></div></div>
          </div>
          <div class="stat-card stat-todo" @click="setDayCalendarQuickFilter('todo')" :class="{ 'stat-active': dayCalendarQuickFilter === 'todo' }">

            <div class="stat-card-top">
              <div class="stat-icon-wrap"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg></div>
            </div>
            <div class="stat-info"><em>{{ stats.todoCount }}</em><i>待办事项</i></div>
            <div class="stat-bar"><div class="stat-bar-inner" :style="{ width: statPercent('todo') + '%' }"></div></div>
          </div>
          <div class="stat-card stat-doing" @click="setDayCalendarQuickFilter('doing')" :class="{ 'stat-active': dayCalendarQuickFilter === 'doing' }">

            <div class="stat-card-top">
              <div class="stat-icon-wrap"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg></div>
            </div>
            <div class="stat-info"><em>{{ stats.doingCount }}</em><i>进行中</i></div>
            <div class="stat-bar"><div class="stat-bar-inner" :style="{ width: statPercent('doing') + '%' }"></div></div>
          </div>
          <div class="stat-card stat-done" @click="setDayCalendarQuickFilter('done')" :class="{ 'stat-active': dayCalendarQuickFilter === 'done' }">

            <div class="stat-card-top">
              <div class="stat-icon-wrap"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg></div>
            </div>
            <div class="stat-info"><em>{{ stats.doneCount }}</em><i>已完成</i></div>
            <div class="stat-bar"><div class="stat-bar-inner" :style="{ width: statPercent('done') + '%' }"></div></div>
          </div>
          <div class="stat-card stat-cancelled" @click="setDayCalendarQuickFilter('cancelled')" :class="{ 'stat-active': dayCalendarQuickFilter === 'cancelled' }">

            <div class="stat-card-top">
              <div class="stat-icon-wrap"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg></div>
            </div>
            <div class="stat-info"><em>{{ stats.cancelledCount }}</em><i>已取消</i></div>
            <div class="stat-bar"><div class="stat-bar-inner" :style="{ width: statPercent('cancelled') + '%' }"></div></div>
          </div>
        </div>

        <div class="cal-card">
          <div class="cal-nav">
            <button class="nav-btn" @click="prevMonth" :disabled="monthChanging"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg></button>
            <div class="cal-nav-center">
              <transition name="month-fade" mode="out-in">
                <span class="cal-month cal-month-btn" :key="currentYear+'-'+currentMonth" @click.stop="toggleMonthPicker">
                  {{ currentYear }}<em>年</em>{{ String(currentMonth).padStart(2,'0') }}<em>月</em>
                  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="mp-arrow" :class="{ open: showMonthPicker }"><polyline points="6 9 12 15 18 9"/></svg>
                </span>
              </transition>
              <div v-if="showMonthPicker" class="month-picker-popup" ref="monthPickerRef">
                <div class="mp-head">
                  <button class="mp-yr-btn" @click.stop="mpYear--"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg></button>
                  <span class="mp-yr-label">{{ mpYear }} 年</span>
                  <button class="mp-yr-btn" @click.stop="mpYear++"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg></button>
                </div>
                <div class="mp-grid">
                  <button v-for="m in 12" :key="m" :class="['mp-m-btn',{active:mpYear===currentYear&&m===currentMonth}]" @click.stop="jumpToMonth(mpYear,m)">{{ m }}月</button>
                </div>
              </div>
              <button class="today-btn" @click="goToday">今天</button>
            </div>
            <button class="nav-btn" @click="nextMonth" :disabled="monthChanging"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg></button>
          </div>
          <div v-if="monthLoading" class="cal-loading"><div class="loading-spinner"></div></div>
          <div class="cal-grid-wrap">
            <div class="cal-week-row">
              <span v-for="(w,wi) in weekDays" :key="w" :class="{'wk-end':wi>=5}">{{ w }}</span>
            </div>
            <div class="cal-day-grid" :style="{ '--day-row-count': String(dayCalendarRowCount) }">
              <div v-for="(cell,ci) in dayCalendarCells" :key="cell.key||ci"
                :class="['day-cell',{'other':cell.isOther,'today':cell.isToday,'active':isSelected(cell),'weekend':!cell.isOther&&cell.isWeekend,'holiday':!cell.isOther&&!!cell.holidayName,'has-events':!cell.isOther&&cell.dots.length>0}]"
                @click="selectDate(cell)" @dblclick="openDrawer(null,cell.date)">
                <div class="cell-inner">
                  <div class="cell-hd">
                    <b class="cell-num">{{ cell.day }}</b>
                    <small v-if="cell.holidayName&&!cell.isOther" :title="cell.holidayName">{{ cell.holidayName }}</small>
                  </div>
                  <div v-if="cell.dots.length&&!cell.isOther" class="cell-dots">
                    <i v-for="(dot,di) in cell.dots.slice(0,3)" :key="di" :style="{background:categoryColor[dot]}"></i>
                    <i v-if="cell.eventCount>3" class="dot-more">{{ cell.eventCount }}</i>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="toolbar">
          <div class="toolbar-right">
            <el-select v-model="dayCalendarFilterCategory" placeholder="月历筛选" clearable size="small" style="width:108px">
              <el-option v-for="c in EVENT_CATEGORY_OPTIONS" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
            <div class="legend">

              <span v-for="(cat,ci) in EVENT_CATEGORY_OPTIONS" :key="ci" class="leg-item">
                <i :style="{background:categoryColor[cat.value]}"></i>{{ cat.label }}
              </span>
            </div>
          </div>
        </div>

        <div class="kbd-hints-wrap">
          <div class="kbd-hints-toggle">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg>
            快捷键提示
          </div>
          <div class="kbd-hints-panel">
            <div class="kbd-hint-row"><kbd>←</kbd><kbd>→</kbd><span>切换日期</span></div>
            <div class="kbd-hint-row"><kbd>↑</kbd><kbd>↓</kbd><span>切换月份</span></div>
            <div class="kbd-hint-row"><kbd>N</kbd><span>新建日程</span></div>
            <div class="kbd-hint-row"><kbd>T</kbd><span>回到今天</span></div>
            <div class="kbd-hint-row"><kbd>B</kbd><span>批量选择</span></div>
            <div class="kbd-hint-row"><kbd>Ctrl</kbd><kbd>F</kbd><span>搜索</span></div>
          </div>
        </div>
      </div>


      <div class="right-panel">
        <div class="rp-head">
          <div class="rp-date-info">
            <div class="rp-date-main">
              <span class="rp-day-num">{{ selectedDayNum }}</span>
              <div class="rp-date-sub">
                <strong class="rp-date-text">{{ selectedMonthWeek }}</strong>
                <span class="rp-rel-date" :class="relDateClass">{{ relativeDateText }}</span>
              </div>
            </div>
            <el-tag v-if="filteredEvents.length>0" size="small" type="info" effect="plain" round>{{ filteredEvents.length }} 项</el-tag>
          </div>
          <div class="rp-head-right">
            <!-- [C] 批量模式按钮 -->
            <button :class="['icon-btn',{'icon-btn-active':batchMode}]" @click="toggleBatchMode" title="批量选择 (B)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="5" width="4" height="4" rx="1"/><rect x="3" y="11" width="4" height="4" rx="1"/><rect x="3" y="17" width="4" height="4" rx="1"/><line x1="10" y1="7" x2="21" y2="7"/><line x1="10" y1="13" x2="21" y2="13"/><line x1="10" y1="19" x2="21" y2="19"/></svg>
            </button>
            <div class="rp-nav-btns">
              <button class="rp-arrow" @click="moveDay(-1)"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg></button>
              <button class="rp-arrow" @click="moveDay(1)"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg></button>
            </div>
          </div>
        </div>
        <div class="rp-toolbar">
          <div class="rp-toolbar-left">
            <el-select v-model="filterCategory" placeholder="分类筛选" clearable size="small" style="width:124px">
              <el-option v-for="c in EVENT_CATEGORY_OPTIONS" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
          </div>
          <div class="quick-filters">
            <button :class="['qf-btn',{active:quickFilter==='all'}]" @click="setQuickFilter('all')">全部</button>
            <el-tooltip v-for="item in statusFilterOptions" :key="item.key" :content="item.label" placement="bottom" :show-after="300">
              <button :class="['qf-btn', `qf-${item.key}`, { active: quickFilter === item.key }]" @click="setQuickFilter(item.key)"><i class="qf-dot" :style="{ background: item.color }"></i>{{ item.label }}</button>
            </el-tooltip>
          </div>
        </div>


        <!-- [C] 批量操作栏 -->
        <transition name="batch-bar-slide">
          <div v-if="batchMode" class="batch-bar">
            <label class="batch-check-all" @click.stop>
              <input type="checkbox" :checked="isAllSelected" :indeterminate="isSomeSelected&&!isAllSelected" @change="toggleSelectAll" />
              <span class="check-box"></span>
              <span class="batch-count">{{ selectedIds.size>0 ? `已选 ${selectedIds.size} 项` : '全选' }}</span>
            </label>
            <div class="batch-actions">
              <button class="batch-action-btn batch-done-btn" :disabled="selectedIds.size===0" @click="batchMarkDone">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                标记完成
              </button>
              <button class="batch-action-btn batch-del-btn" :disabled="selectedIds.size===0" @click="batchDelete">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                删除所选
              </button>
              <button class="batch-action-btn batch-cancel-btn" @click="exitBatchMode">取消</button>
            </div>
          </div>
        </transition>



        <div class="rp-body">
          <div v-if="listLoading" class="list-loading">
            <div class="loading-skeleton" v-for="n in 3" :key="n">
              <div class="sk-left"><div class="sk-time"></div><div class="sk-check"></div></div>
              <div class="sk-content"><div class="sk-line sk-line-1"></div><div class="sk-line sk-line-2"></div></div>
            </div>
          </div>
          <div v-else-if="loadError" class="rp-error">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="color:#c9cdd4"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <p>加载失败，请重试</p>
            <el-button size="small" type="primary" plain @click="retryLoad">重新加载</el-button>
          </div>
          <template v-else>
            <!-- 按时段聚合，保留不同状态在原时间位置中的展示 -->
            <div v-if="dayAgendaSections.length" class="agenda-wrap">
              <section v-for="section in dayAgendaSections" :key="section.key" class="agenda-section">
                <div class="agenda-section-head">
                  <div>
                    <strong class="agenda-section-title">{{ section.label }}</strong>
                    <span class="agenda-section-hint">{{ section.hint }}</span>
                  </div>
                  <span class="agenda-section-count">{{ section.events.length }} 项</span>
                </div>
                <div class="ev-group">
                  <div v-for="ev in section.events" :key="ev.id"
                    :class="[
                      'ev-card',
                      `pri-${ev.priority}`,
                      {
                        'ev-card-todo': ev.status === EVENT_STATUS_VALUE.TODO,
                        'ev-card-doing': ev.status === EVENT_STATUS_VALUE.DOING,
                        'ev-card-done': ev.status === EVENT_STATUS_VALUE.DONE,
                        'ev-card-cancelled': ev.status === EVENT_STATUS_VALUE.CANCELLED,
                        'ev-selected': selectedIds.has(ev.id),
                        'batch-mode': batchMode,
                        'is-batch-disabled': batchMode && !canSelectEventInBatch(ev)
                      }
                    ]"
                    @click="handleEventCardClick(ev)">
                    <div class="ev-left">
                      <template v-if="batchMode && canSelectEventInBatch(ev)">
                        <span class="ev-batch-check">
                          <span :class="['batch-cb',{checked:selectedIds.has(ev.id)}]">
                            <svg v-if="selectedIds.has(ev.id)" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                          </span>
                        </span>
                      </template>
                      <span v-else-if="ev.status===EVENT_STATUS_VALUE.CANCELLED" class="ev-check ev-check-cancelled">&times;</span>
                      <label v-else-if="ev.status===EVENT_STATUS_VALUE.DONE" class="ev-check checked" @click.stop>
                        <input type="checkbox" checked @change="onUndo(ev)" />
                        <span class="check-box"></span>
                      </label>
                      <label v-else class="ev-check" @click.stop title="标记完成">
                        <input type="checkbox" :checked="false" @change="onToggleDone(ev)" />
                        <span class="check-box"></span>
                      </label>
                    </div>
                    <div class="ev-main">
                      <div class="ev-title-row">
                        <span class="ev-inline-time" :class="{ 'is-muted': ev.status===EVENT_STATUS_VALUE.DONE || ev.status===EVENT_STATUS_VALUE.CANCELLED }">{{ formatEventTimeRange(ev.startTime, ev.endTime) }}</span>
                        <span
                          v-if="inlineEditId!==ev.id || batchMode || !canInlineEditEvent(ev)"
                          class="ev-tit"
                          :class="{ strike: ev.status===EVENT_STATUS_VALUE.DONE || ev.status===EVENT_STATUS_VALUE.CANCELLED }"
                          @dblclick.stop="canInlineEditEvent(ev) && startInlineEdit(ev)"
                        >{{ ev.title }}</span>
                        <input v-else class="ev-tit-input" v-model="inlineEditTitle" @blur="submitInlineEdit(ev)" @keydown.enter.stop="submitInlineEdit(ev)" @keydown.esc.stop="cancelInlineEdit" @click.stop />
                      </div>
                      <div class="ev-detail-row">
                        <el-tooltip
                          v-if="getEventRemark(ev)"
                          placement="bottom-start"
                          effect="light"
                          :show-after="200"
                          :offset="6"
                          :show-arrow="false"
                          popper-class="calendar-remark-tooltip"
                        >
                          <template #content>
                            <div class="calendar-remark-tooltip-content">{{ getEventRemark(ev) }}</div>
                          </template>
                          <span class="ev-remark">{{ getEventRemark(ev) }}</span>
                        </el-tooltip>
                        <span v-else class="ev-remark ev-remark-empty">暂无步骤详情</span>
                      </div>
                    </div>
                    <div class="ev-side" @click.stop>
                      <div class="ev-side-top">
                        <span
                          class="ev-cat-tag"
                          :class="{ 'done-tag': ev.status===EVENT_STATUS_VALUE.DONE, 'cancelled-tag': ev.status===EVENT_STATUS_VALUE.CANCELLED }"
                          :style="ev.status===EVENT_STATUS_VALUE.TODO || ev.status===EVENT_STATUS_VALUE.DOING ? { background: categoryColorLight[ev.category], color: categoryColor[ev.category] } : undefined"
                        >{{ EVENT_CATEGORY_MAP[ev.category] }}</span>
                        <div class="ev-priority-slot">
                          <el-tag v-if="ev.priority===2" size="small" type="danger" effect="dark" round>紧急</el-tag>
                          <el-tag v-else-if="ev.priority===1" size="small" type="warning" effect="plain" round>重要</el-tag>
                        </div>
                        <div class="ev-status-line">
                          <span class="ev-status-indicator">
                            <span v-if="ev.status===EVENT_STATUS_VALUE.DOING" class="doing-pulse"></span>
                          </span>
                          <span :class="['ev-status-chip', `is-${ev.status}`, { 'is-loading': isEventStatusPending(ev.id) }]">{{ STATUS_LABEL[ev.status] }}</span>
                        </div>
                      </div>
                      <div v-if="!batchMode" class="ev-status-actions">
                        <button
                          v-for="action in getDayQuickStatusActions(ev)"
                          :key="`${ev.id}-${action.key}`"
                          type="button"
                          :class="['ev-status-btn', `is-${action.key}`, { 'is-pending': isEventStatusPending(ev.id) }]"
                          :disabled="isEventStatusPending(ev.id)"
                          @click.stop="onMoQuickStatusChange(ev, action.value)"
                          :title="action.label"
                        >
                          {{ action.shortLabel }}
                        </button>
                        <el-tooltip content="删除" placement="top" :show-after="300">
                          <button
                            type="button"
                            class="ev-status-btn is-delete"
                            :disabled="isEventStatusPending(ev.id)"
                            @click.stop="onDelete(ev.id)"
                          >
                            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                          </button>
                        </el-tooltip>
                      </div>
                    </div>

                  </div>
                </div>
              </section>
            </div>

            <div v-if="events.length>0&&!filteredEvents.length" class="rp-empty rp-filter-empty">
              <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="color:#c9cdd4"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
              <p>当前筛选条件下无结果</p>
              <el-button size="small" link type="primary" @click="resetFilters">清除筛选</el-button>
            </div>

            <div v-if="!events.length&&!listLoading" class="rp-empty">
              <div class="empty-illus">
                <svg width="90" height="90" viewBox="0 0 90 90" fill="none"><rect x="11" y="17" width="68" height="62" rx="8" fill="#f0f2f5" stroke="#e5e6eb" stroke-width="1.5"/><rect x="11" y="17" width="68" height="20" rx="8" fill="#dce9ff" stroke="#e5e6eb" stroke-width="1.5"/><rect x="11" y="29" width="68" height="8" fill="#dce9ff"/><line x1="25" y1="11" x2="25" y2="25" stroke="#1677ff" stroke-width="2.5" stroke-linecap="round"/><line x1="65" y1="11" x2="65" y2="25" stroke="#1677ff" stroke-width="2.5" stroke-linecap="round"/><rect x="23" y="48" width="20" height="6" rx="3" fill="#bcd5ff"/><rect x="23" y="59" width="34" height="6" rx="3" fill="#e5e6eb"/><rect x="23" y="70" width="26" height="6" rx="3" fill="#e5e6eb" opacity=".6"/><circle cx="68" cy="68" r="14" fill="#e6f4ff" stroke="#1677ff" stroke-width="1.5"/><line x1="68" y1="63" x2="68" y2="73" stroke="#1677ff" stroke-width="2.5" stroke-linecap="round"/><line x1="63" y1="68" x2="73" y2="68" stroke="#1677ff" stroke-width="2.5" stroke-linecap="round"/></svg>
              </div>
              <p class="empty-title">当天暂无日程安排</p>
              <p class="empty-hint">双击日历格子快速创建 &nbsp;·&nbsp; 按 <kbd>N</kbd> 快捷新建</p>
              <el-button type="primary" size="small" style="margin-top:14px;border-radius:8px" @click="openDrawer(null)">立即新建日程</el-button>
            </div>
          </template>
        </div>

        <div class="rp-foot">
          <span class="foot-hint">按 <kbd>N</kbd> 快捷新建 · 双击日期快速创建</span>
        </div>
      </div>
    </div>

    <EventDrawer v-model:visible="drawerVisible" :event-id="editingId" :default-date="defaultNewDate" @saved="onSaved" @deleted="onSaved" />

    <!-- [E] 打印预览弹窗 -->
    <el-dialog v-model="printPreviewVisible" :title="printMode === 'month' ? '月视图导出预览' : '打印预览'" :width="printMode === 'month' ? '1100px' : '600px'" class="print-dialog">
      <div class="print-preview" id="print-area">
        <template v-if="printMode === 'day'">
          <div class="pp-header">
            <div class="pp-title">日程计划</div>
            <div class="pp-date">{{ selectedDate }} &nbsp; {{ selectedMonthWeek }}</div>
          </div>
          <div class="pp-body">
            <div v-if="!events.length" class="pp-empty">当天暂无日程</div>
            <template v-else>
              <div v-for="(ev,i) in events" :key="ev.id" class="pp-row">
                <span class="pp-num">{{ i+1 }}</span>
                <span class="pp-cat-dot" :style="{background:categoryColor[ev.category]}"></span>
                <span class="pp-tit">{{ ev.title }}</span>
                <span class="pp-time">{{ formatTime(ev.startTime) }}{{ ev.endTime?'–'+formatTime(ev.endTime):'' }}</span>
                <span :class="['pp-status',`pp-s-${ev.status}`]">{{ STATUS_LABEL[ev.status] }}</span>
              </div>
            </template>
          </div>
          <div class="pp-footer">共 {{ events.length }} 项日程 · 生成时间：{{ nowStr }}</div>
        </template>

        <template v-else>
          <div class="pp-header pp-header-month">
            <div class="pp-title">日程计划月视图</div>
            <div class="pp-date">{{ currentYear }}年{{ String(currentMonth).padStart(2,'0') }}月</div>
          </div>
          <div class="pp-filter-summary">
            <span class="pp-filter-chip pp-filter-chip-primary">口径：当前筛选结果</span>
            <span v-for="item in monthPrintFilters" :key="item" class="pp-filter-chip">{{ item }}</span>
          </div>
          <div class="pp-month-wrap">
            <div class="pp-month-week">
              <span v-for="(w,wi) in weekDays" :key="w" :class="{'wk-end':wi>=5}">{{ w }}</span>
            </div>
            <div class="pp-month-grid" :style="{ '--pp-row-count': String(moRowCount) }">
              <div
                v-for="(cell,ci) in monthPrintCells"
                :key="cell.key || ci"
                :class="['pp-month-cell', {
                  other: cell.isOther,
                  today: cell.isToday,
                  weekend: !cell.isOther && cell.isWeekend,
                  empty: !cell.isOther && cell.eventCount === 0
                }]"
              >
                <div class="pp-month-cell-hd">
                  <span class="pp-month-day">{{ cell.day }}</span>
                  <span v-if="!cell.isOther && cell.eventCount > 0" class="pp-month-count">{{ cell.eventCount }}</span>
                </div>
                <div v-if="!cell.isOther" class="pp-month-cell-body">
                  <template v-if="cell.visibleEvents.length">
                    <div v-for="ev in cell.visibleEvents" :key="ev.id" :class="['pp-month-event', `is-s-${ev.status}`]">
                      <i class="pp-month-event-dot" :style="{ background: categoryColor[ev.category] }"></i>
                      <span class="pp-month-event-title">{{ ev.title }}</span>
                    </div>
                    <div v-if="cell.remainingCount > 0" class="pp-month-more">+{{ cell.remainingCount }} 项</div>
                  </template>
                  <div v-else class="pp-month-empty">暂无日程</div>
                </div>
              </div>
            </div>
          </div>
          <div class="pp-month-summary">
            <div class="pp-month-stats">
              <span class="pp-month-stat">共 <strong>{{ monthPrintStats.total }}</strong> 项</span>
              <span class="pp-month-stat todo">待办 <strong>{{ monthPrintStats.todoCount }}</strong></span>
              <span class="pp-month-stat doing">进行中 <strong>{{ monthPrintStats.doingCount }}</strong></span>
              <span class="pp-month-stat done">已完成 <strong>{{ monthPrintStats.doneCount }}</strong></span>
              <span class="pp-month-stat cancelled">已取消 <strong>{{ monthPrintStats.cancelledCount }}</strong></span>
            </div>
            <div class="pp-footer">生成时间：{{ nowStr }}</div>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="printPreviewVisible=false">关闭</el-button>
        <el-button type="primary" @click="doPrint">{{ printMode === 'month' ? '打印 / 保存为 PDF' : '打印' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMonthEventsApi, getDayEventsApi, deleteEventApi, updateEventStatusApi,
  getCalendarStatsApi, updateEventApi
} from '@/api/calendar'
import {
  EVENT_CATEGORY_OPTIONS, EVENT_STATUS_VALUE, EVENT_CATEGORY_MAP
} from '@/constants/dict'
import EventDrawer from './EventDrawer.vue'

const today = new Date()
const currentYear = ref(today.getFullYear())
const currentMonth = ref(today.getMonth() + 1)
const selectedDate = ref(formatDate(today))
const dayCalendarFilterCategory = ref<number | undefined>(undefined)
const filterCategory = ref<number | undefined>(undefined)
const keyword = ref('')


const searchFocused = ref(false)
const monthLoading = ref(false)
const listLoading = ref(false)
const monthChanging = ref(false)
const loadError = ref(false)
const dayCalendarQuickFilter = ref<'all'|'todo'|'doing'|'done'|'cancelled'>('all')
const quickFilter = ref<'all'|'todo'|'doing'|'done'|'cancelled'>('all')
const editingId = ref<number | null>(null)

const drawerVisible = ref(false)
const defaultNewDate = ref<string>('')
const pageRef = ref<HTMLElement | null>(null)
const weekDays = ['一','二','三','四','五','六','日']
const monthData = ref<any[]>([])
const events = ref<any[]>([])
const stats = ref({ todoCount: 0, doingCount: 0, doneCount: 0, cancelledCount: 0 })
let dayEventsRequestToken = 0

const showMonthPicker = ref(false)

const mpYear = ref(today.getFullYear())
const monthPickerRef = ref<HTMLElement | null>(null)
const inlineEditId = ref<number | null>(null)
const inlineEditTitle = ref('')

// [M] 月视图模式
const viewMode = ref<'day'|'month'>('day')
const moFilterCategory = ref<number | undefined>(undefined)
// [M] 状态筛选（月视图工具栏按钮）
const moFilterStatus = ref<number | undefined>(undefined)
const moMaxEventsPerCell = 5
const moGridRef = ref<HTMLElement | null>(null)
const moMonthChanging = ref(false)

// ---- [M] 月视图 - 月份选择器 ----
const moShowMonthPicker = ref(false)
const moMpYear = ref(today.getFullYear())
const moMonthPickerRef = ref<HTMLElement | null>(null)
const moNavCenterRef = ref<HTMLElement | null>(null)

// ---- [M] Day Popover ----
const moPopoverVisible = ref(false)
const moPopoverDate = ref<string | null>(null)
const moPopoverPlacement = ref<'top' | 'bottom'>('bottom')
const moPopoverStyle = ref<Record<string,string>>({})
const moPopoverRef = ref<HTMLElement | null>(null)
const moCellFlash = ref(false)
const eventStatusPendingIds = ref<Set<number>>(new Set())
const moQuickStatusActions = [
  { key: 'todo', value: EVENT_STATUS_VALUE.TODO, shortLabel: '待办', label: '设为待办' },
  { key: 'doing', value: EVENT_STATUS_VALUE.DOING, shortLabel: '进行', label: '设为进行中' },
  { key: 'done', value: EVENT_STATUS_VALUE.DONE, shortLabel: '完成', label: '标记完成' },
  { key: 'cancelled', value: EVENT_STATUS_VALUE.CANCELLED, shortLabel: '取消', label: '取消日程' },
] as const

function getQuickStatusAction(key: 'todo' | 'doing' | 'done' | 'cancelled') {
  return moQuickStatusActions.find(action => action.key === key)!
}
function getDayQuickStatusActions(ev: any) {
  const actionKeys: Array<'todo' | 'doing' | 'done' | 'cancelled'> =
    ev.status === EVENT_STATUS_VALUE.TODO
      ? ['doing', 'done', 'cancelled']
      : ev.status === EVENT_STATUS_VALUE.DOING
        ? ['todo', 'done', 'cancelled']
        : ['todo', 'doing']
  return actionKeys.map(getQuickStatusAction)
}

const moPopoverEvents = computed(() => {

  if (!moPopoverDate.value) return []
  return getMoDayEvents(moPopoverDate.value)
})
const moPopoverIsToday = computed(() => moPopoverDate.value === formatDate(new Date()))
const moPopoverTitle = computed(() => {
  if (!moPopoverDate.value) return ''
  const d = new Date(moPopoverDate.value + 'T00:00:00')
  const w = ['日','一','二','三','四','五','六']
  return `${d.getMonth()+1}月${d.getDate()}日（周${w[d.getDay()]}）`
})
const moPopoverStats = computed(() => {
  const data = moPopoverEvents.value
  return {
    total: data.length,
    todoCount: data.filter((e:any)=>e.status===EVENT_STATUS_VALUE.TODO).length,
    doingCount: data.filter((e:any)=>e.status===EVENT_STATUS_VALUE.DOING).length,
    doneCount: data.filter((e:any)=>e.status===EVENT_STATUS_VALUE.DONE).length,
    cancelledCount: data.filter((e:any)=>e.status===EVENT_STATUS_VALUE.CANCELLED).length,
  }
})
const moPopoverDonePercent = computed(() => {
  const total = moPopoverEvents.value.filter((e:any)=>e.status!==EVENT_STATUS_VALUE.CANCELLED).length
  if (!total) return 0
  return Math.round((moPopoverStats.value.doneCount / total) * 100)
})
const moPopoverKeywordHitCount = computed(() => moPopoverDate.value ? getMoDayMatchCount(moPopoverDate.value) : 0)
const moPopoverSections = computed(() => {
  const sectionKeys = ['doing', 'todo', 'done', 'cancelled'] as const
  return sectionKeys
    .map((key) => {
      const option = statusFilterOptions.find(item => item.key === key)
      if (!option) return null
      const events = sortMonthEvents(moPopoverEvents.value.filter((event:any) => event.status === option.value))
      return { ...option, events }
    })
    .filter((section): section is NonNullable<typeof section> => !!section && section.events.length > 0)
})

function getMoCellTarget(e: MouseEvent): HTMLElement | null {
  const current = e.currentTarget as HTMLElement | null
  return current?.closest('.mo-cell') as HTMLElement | null
}
function isMoCellSelected(dateKey?: string | null): boolean {
  return !!dateKey && selectedDate.value === dateKey
}
function isMoPopoverActiveCell(dateKey?: string | null): boolean {
  return !!dateKey && moPopoverVisible.value && moPopoverDate.value === dateKey
}
function isEventStatusPending(id: number): boolean {
  return eventStatusPendingIds.value.has(id)
}
function positionMoPopover(target: HTMLElement) {
  const rect = target.getBoundingClientRect()
  const popoverW = 356
  const popoverH = 476
  const gap = 16
  const vw = window.innerWidth
  const vh = window.innerHeight
  let left = rect.left + rect.width / 2 - popoverW / 2
  if (left + popoverW > vw - 12) left = vw - popoverW - 12
  if (left < 12) left = 12

  // 优先判断下方是否有足够空间（不遮挡当前格子）
  const spaceBelow = vh - rect.bottom - gap
  const spaceAbove = rect.top - gap

  let placement: 'top' | 'bottom' | 'right' | 'left' = 'bottom'

  if (spaceBelow >= popoverH) {
    // 下方空间充足 → 放下方，完全不遮挡
    placement = 'bottom'
  } else if (spaceAbove >= popoverH) {
    // 上方空间充足 → 放上方，完全不遮挡
    placement = 'top'
  } else if (vw - rect.right >= popoverW + gap) {
    // 右侧有空间 → 放右侧
    placement = 'right'
  } else if (rect.left >= popoverW + gap) {
    // 左侧有空间 → 放左侧
    placement = 'left'
  } else {
    // 空间都不够时，优先选遮挡更少的一侧
    placement = spaceBelow > spaceAbove ? 'bottom' : 'top'
  }

  let top: number
  if (placement === 'bottom') {
    top = rect.bottom + gap
  } else if (placement === 'top') {
    top = rect.top - popoverH - gap
  } else if (placement === 'right') {
    top = Math.max(12, rect.top)
  } else { // left
    top = Math.max(12, rect.top)
  }
  if (placement !== 'right' && placement !== 'left') {
    top = Math.max(12, Math.min(top, vh - popoverH - 12))
  }
  if (top < 12) top = 12

  let finalLeft = left
  if (placement === 'right') {
    finalLeft = rect.right + gap
  } else if (placement === 'left') {
    finalLeft = rect.left - popoverW - gap
  }

  const anchorLeft = Math.min(popoverW - 30, Math.max(30, rect.left + rect.width / 2 - finalLeft))
  moPopoverPlacement.value = placement as 'top' | 'bottom'
  moPopoverStyle.value = {
    left: finalLeft + 'px',
    top: top + 'px',
    width: popoverW + 'px',
    '--mdp-anchor-left': anchorLeft + 'px',
    '--mdp-origin-x': anchorLeft + 'px',
    '--mdp-enter-offset': (placement === 'bottom' || placement === 'right') ? '-10px' : '10px',
  }
}
function onMoCellClick(cell: any, e: MouseEvent) {
  if (!cell.date) return
  selectedDate.value = cell.date
  if (moPopoverDate.value === cell.date && moPopoverVisible.value) {
    moPopoverVisible.value = false
    return
  }
  moPopoverDate.value = cell.date
  void loadDayEvents()
  nextTick(() => {
    const target = getMoCellTarget(e)
    if (!target) return
    positionMoPopover(target)
    moPopoverVisible.value = true
  })
}

/** 点击"还有 N 项"：同一格则展开/收起，不同格则切换 */
function toggleMoPopover(cell: any, e: MouseEvent) {
  // 同一格：切换展开/收起
  if (moPopoverDate.value === cell.date && moPopoverVisible.value) {
    moPopoverVisible.value = false
    return
  }
  onMoCellClick(cell, e)
}

function closeMoPopover() {
  moPopoverVisible.value = false
}

function jumpToDay() {
  if (moPopoverDate.value) {
    const cell = allCells.value.find(c => c.date === moPopoverDate.value)
    if (cell) selectDate(cell)
  }
  closeMoPopover()
  viewMode.value = 'day'
}

function jumpToDayByDate(date: string) {
  if (!date) return
  const cell = allCells.value.find(c => c.date === date)
  if (cell) selectDate(cell)
  closeMoPopover()
  viewMode.value = 'day'
}

async function switchToTodayDayView() {
  if (viewMode.value === 'day') return
  moShowMonthPicker.value = false
  closeMoPopover()
  viewMode.value = 'day'
}

function toggleMoMonthPicker() {
  if (!moShowMonthPicker.value) moMpYear.value = currentYear.value
  moShowMonthPicker.value = !moShowMonthPicker.value
}

async function jumpMoToMonth(y: number, m: number) {
  moShowMonthPicker.value = false
  moMonthChanging.value = true
  // 先预加载数据，再更新年月，避免过渡时显示空格子
  const targetStr=`${y}-${String(m).padStart(2,'0')}`
  try{
    const preRes:any=await getMonthEventsApi(targetStr)
    currentYear.value=y; currentMonth.value=m
    monthData.value=preRes.data||[]; syncStatsFromMonthData()
  }catch{
    currentYear.value=y; currentMonth.value=m
  }
  if(viewMode.value === 'month'){ await Promise.all([loadDayEvents(),loadStats()]) }
  else { await loadStats() }
  setTimeout(() => { moMonthChanging.value = false }, 280)
}
function getMoDayDonePct(dateKey: string): number {
  const dayEvs = getMoDayEvents(dateKey)
  if (!dayEvs.length) return 0
  const doneCount = dayEvs.filter((e: any) => e.status === EVENT_STATUS_VALUE.DONE).length
  return Math.round((doneCount / dayEvs.length) * 100)
}

// [C] 批量模式
const batchMode = ref(false)
const selectedIds = ref<Set<number>>(new Set())

// [E] 打印
const printPreviewVisible = ref(false)
const printMode = ref<'day'|'month'>('day')
const nowStr = ref('')
const STATUS_LABEL: Record<number,string> = { 0:'待办', 1:'进行中', 2:'已完成', 3:'已取消' }
const statusFilterOptions = [
  { key: 'todo', value: EVENT_STATUS_VALUE.TODO, label: '待办', color: '#d97706' },
  { key: 'doing', value: EVENT_STATUS_VALUE.DOING, label: '进行中', color: '#2563eb' },
  { key: 'done', value: EVENT_STATUS_VALUE.DONE, label: '已完成', color: '#2f9e44' },
  { key: 'cancelled', value: EVENT_STATUS_VALUE.CANCELLED, label: '已取消', color: '#64748b' },
] as const

const categoryColor: Record<string,string> = {
  0:'#0958d9',
  1:'#2f9e44',
  2:'#d97706',
  3:'#cf1322',
  4:'#64748b',
  5:'#6d28d9',
}
const categoryColorLight: Record<string,string> = {
  0:'#eaf2ff',1:'#e8f7ed',2:'#fff4db',3:'#fff1f0',4:'#eef2f6',5:'#f1eafe',
}
const holidayMap: Record<string,string> = {
  '01-01':'元旦','01-28':'除夕','01-29':'春节',
  '04-05':'清明','05-01':'劳动','06-19':'端午',
  '09-25':'中秋','10-01':'国庆',
}
function getHolidayName(m:number,d:number):string {
  return holidayMap[`${String(m).padStart(2,'0')}-${String(d).padStart(2,'0')}`]||''
}
function isWeekend(y:number,m:number,d:number):boolean {
  const dow=new Date(y,m-1,d).getDay(); return dow===0||dow===6
}

const selectedDayNum = computed(()=>new Date(selectedDate.value+'T00:00:00').getDate())
const selectedMonthWeek = computed(()=>{
  const d=new Date(selectedDate.value+'T00:00:00')
  const w=['日','一','二','三','四','五','六']
  return `${d.getFullYear()}年${d.getMonth()+1}月 · 周${w[d.getDay()]}`
})
const relativeDateText = computed(()=>{
  const now=new Date(); now.setHours(0,0,0,0)
  const sel=new Date(selectedDate.value+'T00:00:00')
  const diff=Math.round((sel.getTime()-now.getTime())/86400000)
  if(diff===0)return'今天'; if(diff===-1)return'昨天'; if(diff===1)return'明天'
  if(diff===-2)return'前天'; if(diff===2)return'后天'
  return diff>0?`${diff}天后`:`${Math.abs(diff)}天前`
})
const relDateClass = computed(()=>{
  const now=new Date(); now.setHours(0,0,0,0)
  const sel=new Date(selectedDate.value+'T00:00:00')
  const diff=Math.round((sel.getTime()-now.getTime())/86400000)
  if(diff===0)return'rel-today'; return diff>0?'rel-future':'rel-past'
})
const totalEvents = computed(()=>stats.value.todoCount+stats.value.doingCount+stats.value.doneCount+stats.value.cancelledCount)
const activeMonthFilterLabel = computed(() => statusFilterOptions.find(item => item.value === moFilterStatus.value)?.label || '')
const monthDonePercent = computed(() => monthPrintStats.value.total ? Math.round((monthPrintStats.value.doneCount / monthPrintStats.value.total) * 100) : 0)
function statPercent(type:'todo'|'doing'|'done'|'cancelled'):number {

  const total=totalEvents.value; if(!total)return 0
  const val=type==='todo'?stats.value.todoCount:type==='doing'?stats.value.doingCount:type==='done'?stats.value.doneCount:stats.value.cancelledCount
  return Math.round((val/total)*100)
}
const filteredEvents = computed(()=>{
  let list=events.value
  if(quickFilter.value==='todo') list=list.filter(e=>e.status===EVENT_STATUS_VALUE.TODO)
  else if(quickFilter.value==='doing') list=list.filter(e=>e.status===EVENT_STATUS_VALUE.DOING)
  else if(quickFilter.value==='done') list=list.filter(e=>e.status===EVENT_STATUS_VALUE.DONE)
  else if(quickFilter.value==='cancelled') list=list.filter(e=>e.status===EVENT_STATUS_VALUE.CANCELLED)
  if(keyword.value.trim()){
    const kw=keyword.value.trim().toLowerCase()
    list=list.filter((e:any)=>(e.title||'').toLowerCase().includes(kw)||(e.remark||'').toLowerCase().includes(kw))
  }
  if(filterCategory.value!==undefined&&filterCategory.value!==null)
    list=list.filter((e:any)=>e.category===filterCategory.value)
  return list
})
const filteredSelectableEvents = computed(() =>

  filteredEvents.value.filter((e:any)=>e.status!==EVENT_STATUS_VALUE.CANCELLED)
)
const dayAgendaSections = computed(() => {
  const sections = [
    { key: 'all-day', label: '全天事项', hint: '未设置时间', events: [] as any[] },
    { key: 'morning', label: '上午', hint: '06:00 - 11:59', events: [] as any[] },
    { key: 'afternoon', label: '下午', hint: '12:00 - 17:59', events: [] as any[] },
    { key: 'evening', label: '晚间', hint: '18:00 - 23:59', events: [] as any[] },
  ]

  const orderMap = new Map(events.value.map((event:any, index:number) => [event.id, index]))
  const sortedEvents = [...filteredEvents.value].sort((a: any, b: any) => {
    const aMinutes = parseTimeMinutes(a.startTime)
    const bMinutes = parseTimeMinutes(b.startTime)
    if (aMinutes !== bMinutes) return aMinutes - bMinutes
    return (orderMap.get(a.id) ?? 0) - (orderMap.get(b.id) ?? 0)
  })

  sortedEvents.forEach((event: any) => {
    sections[getAgendaSectionIndex(event.startTime)].events.push(event)
  })

  return sections.filter(section => section.events.length > 0)
})
const isAllSelected = computed(()=>{
  const all=filteredSelectableEvents.value
  return all.length>0&&all.every(e=>selectedIds.value.has(e.id))
})
const isSomeSelected = computed(()=>selectedIds.value.size>0)

const dayViewMonthData = computed(()=>{
  let list=monthData.value
  if(dayCalendarQuickFilter.value==='todo') list=list.filter((e:any)=>e.status===EVENT_STATUS_VALUE.TODO)
  else if(dayCalendarQuickFilter.value==='doing') list=list.filter((e:any)=>e.status===EVENT_STATUS_VALUE.DOING)
  else if(dayCalendarQuickFilter.value==='done') list=list.filter((e:any)=>e.status===EVENT_STATUS_VALUE.DONE)
  else if(dayCalendarQuickFilter.value==='cancelled') list=list.filter((e:any)=>e.status===EVENT_STATUS_VALUE.CANCELLED)
  if(keyword.value.trim()){
    const kw=keyword.value.trim().toLowerCase()
    list=list.filter((e:any)=>(e.title||'').toLowerCase().includes(kw)||(e.remark||'').toLowerCase().includes(kw))
  }
  if(dayCalendarFilterCategory.value!==undefined&&dayCalendarFilterCategory.value!==null)
    list=list.filter((e:any)=>e.category===dayCalendarFilterCategory.value)
  return list
})


const dayViewEventMap = computed<Record<string, any[]>>(()=>{
  const map:Record<string, any[]> = {}
  for(const event of dayViewMonthData.value){
    const key=event.eventDate
    if(!key) continue
    if(!map[key]) map[key]=[]
    map[key].push(event)
  }
  return map
})
const calendarRows = computed(()=>{
  const y=currentYear.value; const m=currentMonth.value
  const firstDay=new Date(y,m-1,1).getDay()
  const offset=firstDay===0?6:firstDay-1
  const daysInMonth=new Date(y,m,0).getDate()
  const daysInPrev=new Date(y,m-1,0).getDate()
  const todayStr=formatDate(new Date())
  let cells:any[]=[]
  for(let i=offset-1;i>=0;i--) cells.push({day:daysInPrev-i,isOther:true,dots:[],key:`prev-${i}`})
  for(let d=1;d<=daysInMonth;d++){
    const dateKey=`${y}-${String(m).padStart(2,'0')}-${String(d).padStart(2,'0')}`
    const dayEvents=monthData.value.filter((e:any)=>e.eventDate===dateKey)
    const dots=dayEvents.map((e:any)=>e.category)
    cells.push({day:d,date:dateKey,isToday:dateKey===todayStr,dots,visibleDots:dots.slice(0,10),eventCount:dayEvents.length,isWeekend:isWeekend(y,m,d),holidayName:getHolidayName(m,d),key:dateKey})
  }
  const remaining=42-cells.length
  for(let d=1;d<=remaining;d++) cells.push({day:d,isOther:true,dots:[],key:`next-${d}`})
  const rows:any[][]=[]
  for(let i=0;i<6;i++) rows.push(cells.slice(i*7,(i+1)*7))
  return rows
})
const dayCalendarRows = computed(() => {
  return calendarRows.value
    .map((row:any[]) => row.map((cell:any) => {
      if(cell.isOther || !cell.date) return cell
      const dayEvents = dayViewEventMap.value[cell.date] || []
      return {
        ...cell,
        dots: dayEvents.map((event:any) => event.category),
        visibleDots: dayEvents.map((event:any) => event.category).slice(0, 10),
        eventCount: dayEvents.length,
      }
    }))
    .filter((row:any[]) => row.some((cell:any) => !cell.isOther))
})
const dayCalendarCells = computed(()=>dayCalendarRows.value.flat())
const dayCalendarRowCount = computed(() => Math.max(dayCalendarRows.value.length, 1))
const allCells = computed(()=>calendarRows.value.flat())
const moCalendarRows = computed(() => {
  return calendarRows.value.filter((row: any[]) => row.some((cell: any) => !cell.isOther))
})
const moAllCells = computed(() => moCalendarRows.value.flat())
const moRowCount = computed(() => Math.max(moCalendarRows.value.length, 1))

// ---- [M] 月视图计算与方法 ----
const moFilteredMonthData = computed(()=>{
  let list = monthData.value
  if(moFilterCategory.value!==undefined&&moFilterCategory.value!==null)
    list = list.filter((e:any)=>e.category===moFilterCategory.value)
  if(moFilterStatus.value!==undefined&&moFilterStatus.value!==null)
    list = list.filter((e:any)=>e.status===moFilterStatus.value)
  if(keyword.value.trim()){
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter((e:any)=>
      (e.title||'').toLowerCase().includes(kw) || (e.remark||'').toLowerCase().includes(kw)
    )
  }
  return list
})
function getMonthStatusRank(status:number){
  return status===EVENT_STATUS_VALUE.DOING ? 0 :
    status===EVENT_STATUS_VALUE.TODO ? 1 :
    status===EVENT_STATUS_VALUE.DONE ? 2 : 3
}
function sortMonthEvents(list:any[]){
  return [...list].sort((a:any,b:any)=>{
    const priorityDiff=(b.priority ?? 0)-(a.priority ?? 0)
    if(priorityDiff!==0)return priorityDiff
    const statusDiff=getMonthStatusRank(a.status)-getMonthStatusRank(b.status)
    if(statusDiff!==0)return statusDiff
    return String(a.startTime||'99:99').localeCompare(String(b.startTime||'99:99'))
  })
}
const hasMoMonthFilter = computed(() => (
  (moFilterCategory.value !== undefined && moFilterCategory.value !== null) ||
  (moFilterStatus.value !== undefined && moFilterStatus.value !== null) ||
  !!keyword.value.trim()
))
const moSourceDayEventsMap = computed<Record<string, any[]>>(()=>{
  const map:Record<string, any[]> = {}
  for(const event of monthData.value){
    const key=event.eventDate
    if(!key)continue
    if(!map[key]) map[key]=[]
    map[key].push(event)
  }
  Object.keys(map).forEach((key)=>{ map[key]=sortMonthEvents(map[key]) })
  return map
})
const moDayEventsMap = computed<Record<string, any[]>>(()=>{
  const map:Record<string, any[]> = {}
  for(const event of moFilteredMonthData.value){
    const key=event.eventDate
    if(!key)continue
    if(!map[key]) map[key]=[]
    map[key].push(event)
  }
  Object.keys(map).forEach((key)=>{ map[key]=sortMonthEvents(map[key]) })
  return map
})
function getMoDaySourceEvents(dateKey:string):any[] {
  return moSourceDayEventsMap.value[dateKey] || []
}
function getMoDayEvents(dateKey:string):any[] {
  return moDayEventsMap.value[dateKey] || []
}
const monthPrintStats = computed(()=>{
  const data=moFilteredMonthData.value
  return {
    total:data.length,
    todoCount:data.filter((e:any)=>e.status===EVENT_STATUS_VALUE.TODO).length,
    doingCount:data.filter((e:any)=>e.status===EVENT_STATUS_VALUE.DOING).length,
    doneCount:data.filter((e:any)=>e.status===EVENT_STATUS_VALUE.DONE).length,
    cancelledCount:data.filter((e:any)=>e.status===EVENT_STATUS_VALUE.CANCELLED).length,
  }
})
const monthPrintFilters = computed(()=>{
  const items:string[]=[]
  const categoryLabel=EVENT_CATEGORY_OPTIONS.find(item=>item.value===moFilterCategory.value)?.label
  if(categoryLabel) items.push(`分类：${categoryLabel}`)
  if(moFilterStatus.value!==undefined&&moFilterStatus.value!==null)
    items.push(`状态：${STATUS_LABEL[moFilterStatus.value]}`)
  if(keyword.value.trim()) items.push(`关键词：${keyword.value.trim()}`)
  if(items.length===0) items.push('筛选：全部')
  return items
})
const moDayMetaMap = computed<Record<string, any>>(() => {
  const map: Record<string, any> = {}

  Object.entries(moDayEventsMap.value).forEach(([dateKey, dayEvents]) => {
    const counts: Record<string, number> = { todo: 0, doing: 0, done: 0, cancelled: 0 }

    dayEvents.forEach((event: any) => {
      if (event.status === EVENT_STATUS_VALUE.TODO) counts.todo += 1
      else if (event.status === EVENT_STATUS_VALUE.DOING) counts.doing += 1
      else if (event.status === EVENT_STATUS_VALUE.DONE) counts.done += 1
      else if (event.status === EVENT_STATUS_VALUE.CANCELLED) counts.cancelled += 1
    })

    const activeStatuses = statusFilterOptions
      .map(item => ({ ...item, count: counts[item.key] || 0 }))
      .filter(item => item.count > 0)
      .sort((a, b) => b.count - a.count || getMonthStatusRank(a.value) - getMonthStatusRank(b.value))

    if (!activeStatuses.length) return

    const mixed = activeStatuses.length > 1
    map[dateKey] = {
      toneKey: mixed ? 'mixed' : activeStatuses[0].key,
      pillLabel: mixed ? '混合' : activeStatuses[0].label,
      summaryLabel: mixed ? `${activeStatuses.length}种状态` : activeStatuses[0].label,
      matchCount: keyword.value.trim() ? dayEvents.length : 0,
    }
  })

  return map
})
const monthPrintCells = computed(()=>moAllCells.value.map((cell:any)=>{
  if(cell.isOther || !cell.date){
    return { ...cell, eventCount: 0, visibleEvents: [], remainingCount: 0 }
  }
  const dayEvents=getMoDayEvents(cell.date)
  return {
    ...cell,
    eventCount: dayEvents.length,
    visibleEvents: dayEvents.slice(0, 3),
    remainingCount: Math.max(dayEvents.length - 3, 0),
  }
}))
function getMoDayMeta(dateKey:string){
  return moDayMetaMap.value[dateKey]
}
function getMoCellToneClass(dateKey:string):string {
  const toneKey = getMoDayMeta(dateKey)?.toneKey
  return toneKey ? `mo-tone-${toneKey}` : ''
}
function getMoDayMatchCount(dateKey:string):number {
  return getMoDayMeta(dateKey)?.matchCount ?? 0
}
function getMoDayHasVisibleData(dateKey:string):boolean {
  return getMoDayEvents(dateKey).length > 0
}
function shouldShowMoTodayState(dateKey?: string | null):boolean {
  return !!dateKey && getMoDayHasVisibleData(dateKey)
}
function isMoCellFilteredMatch(dateKey:string):boolean {
  return hasMoMonthFilter.value && getMoDayHasVisibleData(dateKey)
}
function isMoCellFilteredOut(dateKey:string):boolean {
  return hasMoMonthFilter.value && getMoDaySourceEvents(dateKey).length > 0 && !getMoDayHasVisibleData(dateKey)
}
function getMoCellRows(dateKey:string):Array<{ key:string; type:'event'|'more'|'empty'; event?:any; remaining?:number }> {
  const dayEvents = getMoDayEvents(dateKey)
  if (!dayEvents.length) return []

  const rows:Array<{ key:string; type:'event'|'more'|'empty'; event?:any; remaining?:number }> = []
  const eventLimit = dayEvents.length > moMaxEventsPerCell ? Math.max(moMaxEventsPerCell - 1, 1) : Math.min(dayEvents.length, moMaxEventsPerCell)

  dayEvents.slice(0, eventLimit).forEach((event:any) => {
    rows.push({ key: `event-${event.id}`, type: 'event', event })
  })

  const remaining = dayEvents.length - eventLimit
  if (remaining > 0) {
    rows.push({ key: `more-${dateKey}`, type: 'more', remaining })
  }

  while (rows.length < moMaxEventsPerCell) {
    rows.push({ key: `empty-${dateKey}-${rows.length}`, type: 'empty' })
  }

  return rows
}
// [M] end

function formatDate(d:Date):string {
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}
function formatMonth():string{ return `${currentYear.value}-${String(currentMonth.value).padStart(2,'0')}` }
function formatTime(t:string):string{ if(!t)return''; return t.length>=5?t.slice(0,5):t }
function getEventRemark(event:any):string {
  return String(event?.remark || event?.description || '').trim()
}
function formatEventTimeRange(startTime?: string, endTime?: string): string {

  const start = formatTime(startTime || '')
  const end = formatTime(endTime || '')
  if (start && end) return `${start}–${end}`
  return start || end || '全天'
}
function parseTimeMinutes(t?: string): number { 
  if (!t) return -1
  const [hourText, minuteText = '0'] = String(t).split(':')
  const hour = Number(hourText)
  const minute = Number(minuteText)
  if (Number.isNaN(hour) || Number.isNaN(minute)) return -1
  return hour * 60 + minute
}
function getAgendaSectionIndex(t?: string): number {
  const minutes = parseTimeMinutes(t)
  if (minutes < 0) return 0
  if (minutes < 12 * 60) return 1
  if (minutes < 18 * 60) return 2
  return 3
}
function canSelectEventInBatch(ev:any){
  return ev?.status !== EVENT_STATUS_VALUE.CANCELLED
}
function canInlineEditEvent(ev:any){
  return ev?.status === EVENT_STATUS_VALUE.TODO || ev?.status === EVENT_STATUS_VALUE.DOING
}
function handleEventCardClick(ev:any){
  if(batchMode.value){
    if(canSelectEventInBatch(ev)) toggleSelect(ev.id)
    return
  }
  openDrawer(ev)
}
function syncStatsFromMonthData(){
  stats.value = {
    todoCount: monthData.value.filter((event:any)=>event.status===EVENT_STATUS_VALUE.TODO).length,
    doingCount: monthData.value.filter((event:any)=>event.status===EVENT_STATUS_VALUE.DOING).length,
    doneCount: monthData.value.filter((event:any)=>event.status===EVENT_STATUS_VALUE.DONE).length,
    cancelledCount: monthData.value.filter((event:any)=>event.status===EVENT_STATUS_VALUE.CANCELLED).length,
  }
}
function setEventStatusLocally(id:number,status:number){
  events.value = events.value.map((event:any)=>event.id===id ? { ...event, status } : event)
  monthData.value = monthData.value.map((event:any)=>event.id===id ? { ...event, status } : event)
  syncStatsFromMonthData()
}
async function changeEventStatus(ev:any, status:number, successMessage:string){
  if(!ev?.id || ev.status===status || isEventStatusPending(ev.id)) return
  const nextPending = new Set(eventStatusPendingIds.value)
  nextPending.add(ev.id)
  eventStatusPendingIds.value = nextPending
  try{
    await updateEventStatusApi(ev.id,status)
    setEventStatusLocally(ev.id, status)
    ElMessage.success(successMessage)
  }catch{
    ElMessage.error('操作失败，请稍后重试')
  }finally{
    const restPending = new Set(eventStatusPendingIds.value)
    restPending.delete(ev.id)
    eventStatusPendingIds.value = restPending
  }
}
async function onMoQuickStatusChange(ev:any, status:number){
  const successMessage = status===EVENT_STATUS_VALUE.DONE ? '已标记完成' : status===EVENT_STATUS_VALUE.DOING ? '已切换为进行中' : status===EVENT_STATUS_VALUE.CANCELLED ? '已标记取消' : '已切换为待办'
  await changeEventStatus(ev, status, successMessage)
}
function removeEventLocally(ids:number[]){
  const removedIds = new Set(ids)
  events.value = events.value.filter((event:any)=>!removedIds.has(event.id))
  monthData.value = monthData.value.filter((event:any)=>!removedIds.has(event.id))
  selectedIds.value = new Set([...selectedIds.value].filter(id => !removedIds.has(id)))
  syncStatsFromMonthData()
}
function isSelected(cell:any):boolean{ return !cell.isOther&&cell.date===selectedDate.value }
function syncDayListFiltersFromCalendar(){
  quickFilter.value=dayCalendarQuickFilter.value
  filterCategory.value=dayCalendarFilterCategory.value
}

async function selectDate(cell:any){

  if(!cell.date)return
  selectedDate.value=cell.date
  syncDayListFiltersFromCalendar()
  const d=new Date(cell.date+'T00:00:00')

  if(d.getFullYear()!==currentYear.value||d.getMonth()+1!==currentMonth.value){
    currentYear.value=d.getFullYear(); currentMonth.value=d.getMonth()+1
    await loadMonthData()
  }
  await loadDayEvents()
}
async function loadMonthData(){
  monthLoading.value=true
  try{
    monthData.value=(await getMonthEventsApi(formatMonth()) as any).data||[]
    syncStatsFromMonthData()
  }catch{}finally{ monthLoading.value=false }
}
async function loadDayEvents(targetDate = selectedDate.value){
  const requestToken = ++dayEventsRequestToken
  listLoading.value = true
  loadError.value = false
  try{
    const res:any = await getDayEventsApi({ eventDate: targetDate })
    if(requestToken!==dayEventsRequestToken || targetDate!==selectedDate.value) return
    events.value = res.data || []
  }catch{
    if(requestToken===dayEventsRequestToken && targetDate===selectedDate.value) loadError.value = true
  }finally{
    if(requestToken===dayEventsRequestToken && targetDate===selectedDate.value) listLoading.value = false
  }
}

async function loadStats(){
  try{ const res:any=await getCalendarStatsApi(formatMonth()); if(res.data)stats.value=res.data }catch{}
}
function retryLoad(){ refreshAll() }
async function prevMonth(){
  if(monthChanging.value||moMonthChanging.value)return
  monthChanging.value=true; moMonthChanging.value = true
  let nextYear=currentYear.value
  let nextMonth=currentMonth.value-1
  if(nextMonth===0){nextMonth=12;nextYear--}
  // 先预加载数据，再更新年月，避免过渡时显示空格子
  const targetStr=`${nextYear}-${String(nextMonth).padStart(2,'0')}`
  try{
    const preRes:any=await getMonthEventsApi(targetStr)
    currentYear.value=nextYear; currentMonth.value=nextMonth
    monthData.value=preRes.data||[]; syncStatsFromMonthData()
  }catch{
    currentYear.value=nextYear; currentMonth.value=nextMonth
  }
  if(viewMode.value === 'month') await loadDayEvents()
  await loadStats()
  setTimeout(()=>{monthChanging.value=false; moMonthChanging.value=false},280)
}
async function nextMonth(){
  if(monthChanging.value||moMonthChanging.value)return
  monthChanging.value=true; moMonthChanging.value = true
  let nextYear=currentYear.value
  let nextMonth=currentMonth.value+1
  if(nextMonth===13){nextMonth=1;nextYear++}
  // 先预加载数据，再更新年月，避免过渡时显示空格子
  const targetStr=`${nextYear}-${String(nextMonth).padStart(2,'0')}`
  try{
    const preRes:any=await getMonthEventsApi(targetStr)
    currentYear.value=nextYear; currentMonth.value=nextMonth
    monthData.value=preRes.data||[]; syncStatsFromMonthData()
  }catch{
    currentYear.value=nextYear; currentMonth.value=nextMonth
  }
  if(viewMode.value === 'month') await loadDayEvents()
  await loadStats()
  setTimeout(()=>{monthChanging.value=false; moMonthChanging.value=false},280)
}
async function goToday(){
  const t=new Date()
  const todayStr=formatDate(t)
  const targetYear=t.getFullYear()
  const targetMonth=t.getMonth()+1
  selectedDate.value=todayStr
  syncDayListFiltersFromCalendar()
  moCellFlash.value = true

  setTimeout(() => { moCellFlash.value = false }, 3000)
  // 如果已在当前月份，仍需刷新当天右侧列表
  if(currentYear.value===targetYear&&currentMonth.value===targetMonth){
    await loadDayEvents(todayStr)
    return
  }
  moMonthChanging.value = true

  // 先预加载数据，再更新年月，避免过渡时显示空格子
  const targetMonthStr=`${targetYear}-${String(targetMonth).padStart(2,'0')}`
  try{
    const preRes:any=await getMonthEventsApi(targetMonthStr)
    currentYear.value=targetYear; currentMonth.value=targetMonth
    monthData.value=preRes.data||[]; syncStatsFromMonthData()
  }catch{
    currentYear.value=targetYear; currentMonth.value=targetMonth
  }
  await Promise.all([loadDayEvents(),loadStats()])
  setTimeout(()=>{ moMonthChanging.value=false }, 280)
}
function moveDay(dir:number){
  const d=new Date(selectedDate.value+'T00:00:00'); d.setDate(d.getDate()+dir)
  selectedDate.value=formatDate(d)
  syncDayListFiltersFromCalendar()
  if(d.getFullYear()!==currentYear.value||d.getMonth()+1!==currentMonth.value){

    currentYear.value=d.getFullYear(); currentMonth.value=d.getMonth()+1; refreshAll()
  }else{ loadDayEvents() }
}
function setDayCalendarQuickFilter(val:'all'|'todo'|'doing'|'done'|'cancelled'){
  dayCalendarQuickFilter.value=val
}
function setQuickFilter(val:'all'|'todo'|'doing'|'done'|'cancelled'){
  quickFilter.value=val
}
function resetFilters(){ quickFilter.value='all'; filterCategory.value=undefined; keyword.value='' }


function clearSearch(){ keyword.value='' }
function openDrawer(event:any,date?:string){
  if(event){editingId.value=event.id;defaultNewDate.value=''}
  else{editingId.value=null;defaultNewDate.value=date||selectedDate.value}
  drawerVisible.value=true
  moPopoverVisible.value=false   // 打开抽屉时隐藏 Popover
}
async function onToggleDone(ev:any){
  await changeEventStatus(ev, EVENT_STATUS_VALUE.DONE, '已标记完成')
}
async function onUndo(ev:any){
  await changeEventStatus(ev, EVENT_STATUS_VALUE.TODO, '已还原为待办')
}
async function onDelete(id:number){
  try{
    await ElMessageBox.confirm(
      '确定要删除此日程吗？删除后不可恢复。',
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
      }
    )
    await deleteEventApi(id)
    removeEventLocally([id])
    ElMessage.success('已删除')
  }catch(e: any){
    if (e !== 'cancel') {
      ElMessage.error('删除失败，请稍后重试')
    }
  }
}
function onSaved(){ refreshAll() }

async function refreshAll(){ await Promise.all([loadMonthData(),loadDayEvents(),loadStats()]) }
/** 仅刷新月视图相关数据（日历格子、统计），不刷新右侧日程列表 */
async function refreshMonthOnly(){ await Promise.all([loadMonthData(),loadStats()]) }

function toggleMonthPicker(){
  if(!showMonthPicker.value)mpYear.value=currentYear.value
  showMonthPicker.value=!showMonthPicker.value
}
async function jumpToMonth(y:number,m:number){
  showMonthPicker.value=false; currentYear.value=y; currentMonth.value=m
  if(viewMode.value === 'month') await refreshAll()
  else await refreshMonthOnly()
}
function handleDocClick(e:MouseEvent){
  // 日视图选月器
  if(showMonthPicker.value){
    const popup=monthPickerRef.value
    const trigger=pageRef.value?.querySelector('.cal-month-btn')
    if(popup&&!popup.contains(e.target as Node)&&trigger&&!trigger.contains(e.target as Node))
      showMonthPicker.value=false
  }
  // 月视图选月器
  if(moShowMonthPicker.value){
    const moPopup=moMonthPickerRef.value
    const moTrigger=moNavCenterRef.value?.querySelector('.mo-month-btn')
    if(moPopup&&!moPopup.contains(e.target as Node)&&moTrigger&&!moTrigger.contains(e.target as Node))
      moShowMonthPicker.value=false
  }
  // Day Popover
  if(moPopoverVisible.value){
    const pop=moPopoverRef.value
    if(pop&&!pop.contains(e.target as Node)) moPopoverVisible.value=false
  }
}

function startInlineEdit(ev:any){
  inlineEditId.value=ev.id; inlineEditTitle.value=ev.title
  nextTick(()=>{ const el=document.querySelector('.ev-tit-input') as HTMLInputElement; el?.focus(); el?.select() })
}
async function submitInlineEdit(ev:any){
  if(inlineEditId.value===null)return
  const newTitle=inlineEditTitle.value.trim()
  if(!newTitle||newTitle===ev.title){cancelInlineEdit();return}
  try{
    await updateEventApi(ev.id,{...ev,title:newTitle})
    ElMessage.success('标题已更新'); refreshAll()
  }catch{ElMessage.error('更新失败')}finally{cancelInlineEdit()}
}
function cancelInlineEdit(){ inlineEditId.value=null; inlineEditTitle.value='' }

// ---- [C] 批量操作 ----
function toggleBatchMode(){ batchMode.value=!batchMode.value; if(!batchMode.value)selectedIds.value=new Set() }
function exitBatchMode(){ batchMode.value=false; selectedIds.value=new Set() }
function toggleSelect(id:number){
  const s=new Set(selectedIds.value)
  if(s.has(id))s.delete(id); else s.add(id)
  selectedIds.value=s
}
function toggleSelectAll(){
  const all=filteredSelectableEvents.value
  if(isAllSelected.value) selectedIds.value=new Set()
  else selectedIds.value=new Set(all.map(e=>e.id))
}
async function batchMarkDone(){
  if(selectedIds.value.size===0)return
  try{
    await ElMessageBox.confirm(`确定将 ${selectedIds.value.size} 项日程标记为完成？`,'批量操作',{confirmButtonText:'确定',cancelButtonText:'取消',type:'info'})
    const ids=[...selectedIds.value]
    await Promise.all(ids.map(id=>updateEventStatusApi(id,EVENT_STATUS_VALUE.DONE)))
    ids.forEach(id => setEventStatusLocally(id, EVENT_STATUS_VALUE.DONE))
    ElMessage.success(`已将 ${ids.length} 项标记为完成`)
    exitBatchMode()
  }catch{}
}
async function batchDelete(){
  if(selectedIds.value.size===0)return
  try{
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.size} 项日程？此操作不可撤销。`,'批量删除',{confirmButtonText:'确定删除',cancelButtonText:'取消',type:'warning'})
    const ids=[...selectedIds.value]
    await Promise.all(ids.map(id=>deleteEventApi(id)))
    removeEventLocally(ids)
    ElMessage.success(`已删除 ${ids.length} 项日程`)
    exitBatchMode()
  }catch{}
}

// ---- [E] 导出 & 打印 ----
function handleExportCmd(cmd:string){
  if(cmd==='export-day') exportCSV(events.value,`日程_${selectedDate.value}`)
  else if(cmd==='export-month') exportCSV(monthData.value,`日程_${formatMonth()}`)
  else if(cmd==='export-month-filtered') exportCSV(moFilteredMonthData.value,`日程_${formatMonth()}_筛选结果`)
  else if(cmd==='export-month-pdf') openPrintPreview('month')
  else if(cmd==='print-day') openPrintPreview('day')
}
function exportCSV(data:any[],filename:string){
  if(!data.length){ElMessage.warning('暂无数据可导出');return}
  const SM:Record<number,string>={0:'待办',1:'进行中',2:'已完成',3:'已取消'}
  const PM:Record<number,string>={0:'普通',1:'重要',2:'紧急'}
  const CM:Record<number,string>={0:'工作',1:'生活',2:'财务',3:'健康',4:'其他',5:'学习'}
  const header=['标题','日期','开始时间','结束时间','分类','优先级','状态','备注']
  const rows=data.map(e=>[
    `"${(e.title||'').replace(/"/g,'""')}"`,
    e.eventDate||'',
    formatTime(e.startTime||''),
    formatTime(e.endTime||''),
    CM[e.category]||'',
    PM[e.priority]||'',
    SM[e.status]||'',
    `"${(e.remark||'').replace(/"/g,'""')}"`
  ])
  const csv='\uFEFF'+[header,...rows].map(r=>r.join(',')).join('\n')
  const blob=new Blob([csv],{type:'text/csv;charset=utf-8;'})
  const url=URL.createObjectURL(blob)
  const a=document.createElement('a'); a.href=url; a.download=`${filename}.csv`
  document.body.appendChild(a); a.click(); document.body.removeChild(a); URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}
function openPrintPreview(mode:'day'|'month'='day'){
  printMode.value=mode
  nowStr.value=new Date().toLocaleString('zh-CN')
  printPreviewVisible.value=true
}
function getPrintStyleText(mode:'day'|'month'){
  const pageSize=mode==='month' ? 'A4 landscape' : 'A4 portrait'
  return `
    @page{size:${pageSize};margin:10mm;}
    *{box-sizing:border-box;}
    body{font-family:'PingFang SC','Microsoft YaHei',sans-serif;padding:${mode==='month'?'18px 20px':'32px 40px'};color:#1d2129;font-size:13px;background:#fff;}
    .print-preview{width:100%;}
    .pp-header{border-bottom:2px solid #1677ff;padding-bottom:12px;margin-bottom:16px;}
    .pp-title{font-size:20px;font-weight:700;color:#1677ff;margin-bottom:4px;}
    .pp-date{font-size:12px;color:#86909c;}
    .pp-body{width:100%;}
    .pp-empty{text-align:center;color:#c9cdd4;padding:40px 0;}
    .pp-row{display:flex;align-items:center;gap:10px;padding:9px 0;border-bottom:1px solid #f0f2f5;}
    .pp-num{width:20px;font-size:11px;color:#c9cdd4;flex-shrink:0;text-align:right;}
    .pp-cat-dot{width:8px;height:8px;border-radius:50%;flex-shrink:0;}
    .pp-tit{flex:1;font-size:13px;font-weight:600;}
    .pp-time{font-size:11px;color:#86909c;white-space:nowrap;}
    .pp-status{font-size:11px;padding:1px 8px;border-radius:10px;white-space:nowrap;}
    .pp-s-0{background:#fff7e6;color:#fa8c16;}.pp-s-1{background:#e6f4ff;color:#1677ff;}.pp-s-2{background:#f6ffed;color:#52c41a;}.pp-s-3{background:#f2f3f5;color:#86909c;}
    .pp-footer{margin-top:14px;font-size:11px;color:#c9cdd4;text-align:right;}
    .pp-filter-summary{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:14px;}
    .pp-filter-chip{display:inline-flex;align-items:center;padding:4px 10px;border-radius:999px;background:#f2f3f5;color:#4e5969;font-size:11px;font-weight:600;}
    .pp-filter-chip-primary{background:#e6f4ff;color:#1677ff;}
    .pp-month-wrap{border:1px solid #d4d8e2;border-radius:12px;overflow:hidden;}
    .pp-month-week{display:grid;grid-template-columns:repeat(7,minmax(0,1fr));background:#f7f8fa;border-bottom:1px solid #d4d8e2;}
    .pp-month-week span{text-align:center;padding:8px 0;font-size:11px;font-weight:700;color:#4e5969;letter-spacing:1px;}
    .pp-month-week .wk-end{color:#cf1322;}
    .pp-month-grid{display:grid;grid-template-columns:repeat(7,minmax(0,1fr));grid-template-rows:repeat(var(--pp-row-count,6),minmax(96px,1fr));}
    .pp-month-cell{min-height:96px;border-right:1px solid #d4d8e2;border-bottom:1px solid #d4d8e2;padding:8px 8px 10px;background:#fff;display:flex;flex-direction:column;gap:6px;}
    .pp-month-cell:nth-child(7n){border-right:none;}
    .pp-month-cell:nth-last-child(-n+7){border-bottom:none;}
    .pp-month-cell.other{background:#f7f8fa;color:#c9cdd4;}
    .pp-month-cell.weekend:not(.other){background:#fff8f8;}
    .pp-month-cell.today{background:#f0f7ff;box-shadow:inset 3px 0 0 #1677ff;}
    .pp-month-cell.empty:not(.other){background:#fcfcfd;}
    .pp-month-cell-hd{display:flex;align-items:center;justify-content:space-between;gap:6px;}
    .pp-month-day{font-size:13px;font-weight:800;color:#1d2129;}
    .pp-month-cell.today .pp-month-day{color:#1677ff;}
    .pp-month-count{min-width:18px;height:18px;padding:0 5px;border-radius:10px;background:#1677ff;color:#fff;font-size:10px;font-weight:800;display:inline-flex;align-items:center;justify-content:center;}
    .pp-month-cell-body{display:flex;flex-direction:column;gap:4px;flex:1;min-height:0;}
    .pp-month-event{display:flex;align-items:center;gap:5px;padding:3px 5px;border-radius:6px;background:#f7f8fa;font-size:11px;line-height:1.35;}
    .pp-month-event.is-s-1{background:#eef6ff;}
    .pp-month-event.is-s-2{opacity:.6;}
    .pp-month-event.is-s-3{opacity:.45;}
    .pp-month-event-dot{width:6px;height:6px;border-radius:50%;flex-shrink:0;}
    .pp-month-event-title{flex:1;min-width:0;font-size:9px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
    .pp-month-more{margin-top:auto;font-size:11px;font-weight:700;color:#1677ff;}
    .pp-month-empty{margin-top:auto;font-size:11px;color:#c9cdd4;}
    .pp-month-summary{display:flex;align-items:flex-end;justify-content:space-between;gap:12px;margin-top:14px;}
    .pp-month-stats{display:flex;flex-wrap:wrap;gap:14px;}
    .pp-month-stat{font-size:12px;color:#4e5969;font-weight:600;}
    .pp-month-stat strong{font-weight:800;}
    .pp-month-stat.todo strong{color:#fa8c16;}
    .pp-month-stat.doing strong{color:#1677ff;}
    .pp-month-stat.done strong{color:#52c41a;}
    .pp-month-stat.cancelled strong{color:#86909c;}
    @media print{body{padding:${mode==='month'?'8px 10px':'16px 20px'};}}
  `
}
function doPrint(){
  const area=document.getElementById('print-area')
  if(!area)return
  const title=printMode.value==='month' ? `日程月视图_${formatMonth()}` : `日程_${selectedDate.value}`
  const win=window.open('', '_blank', printMode.value==='month' ? 'width=1400,height=960' : 'width=700,height=900')
  if(!win){ElMessage.warning('请允许浏览器弹出窗口');return}
  win.document.write(`<!DOCTYPE html><html><head><meta charset="utf-8"><title>${title}</title><style>${getPrintStyleText(printMode.value)}</style></head><body>${area.innerHTML}</body></html>`)
  win.document.close(); win.focus()
  setTimeout(()=>{win.print();win.close()},400)
}

function handleKeydown(e:KeyboardEvent){
  if(e.target instanceof HTMLInputElement||e.target instanceof HTMLTextAreaElement){
    if(e.key==='Escape')(e.target as HTMLElement).blur()
    return
  }
  const k=e.key.toLowerCase()
  if(k==='arrowleft'){e.preventDefault();moveDay(-1)}
  else if(k==='arrowright'){e.preventDefault();moveDay(1)}
  else if(k==='arrowup'){e.preventDefault();prevMonth()}
  else if(k==='arrowdown'){e.preventDefault();nextMonth()}
  else if(k==='n'){e.preventDefault();drawerVisible.value?(drawerVisible.value=false):openDrawer(null)}
  else if(k==='enter'){e.preventDefault();openDrawer(null)}
  else if(k==='t'){e.preventDefault();goToday()}
  else if(k==='b'){e.preventDefault();toggleBatchMode()}
  else if(k==='v'){e.preventDefault();viewMode.value=viewMode.value==='day'?'month':'day'}
  else if(k==='escape'){e.preventDefault();drawerVisible.value=false;showMonthPicker.value=false;moShowMonthPicker.value=false;moPopoverVisible.value=false;exitBatchMode()}
  else if((e.ctrlKey||e.metaKey)&&k==='f'){
    e.preventDefault()
    ;(pageRef.value?.querySelector('.search-box input') as HTMLInputElement)?.focus()
  }
}

onMounted(async()=>{
  pageRef.value?.focus()
  mpYear.value=currentYear.value
  document.addEventListener('click',handleDocClick)
  await refreshAll()
})
onBeforeUnmount(()=>{ document.removeEventListener('click',handleDocClick) })
</script>

<style scoped lang="scss">
$primary:       #0958d9;
$primary-light: #eaf2ff;
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
$rs-lg:         12px;
$shadow-sm:     0 4px 18px rgba(15,23,42,.06);
$shadow-md:     0 10px 26px rgba(15,23,42,.10);
$shadow-lg:     0 18px 40px rgba(15,23,42,.14);

.calendar-page {
  display:flex; flex-direction:column;
  margin:-20px; width:calc(100% + 40px); height:calc(100% + 40px);
  min-height:0; background:$bg; overflow:hidden; outline:none; box-sizing:border-box;
}
.page-header {
  display:flex; align-items:center; justify-content:space-between;
  padding:16px 28px;
  background:linear-gradient(180deg, rgba(255,255,255,.98) 0%, rgba(248,250,253,.98) 100%);
  border-bottom:1px solid rgba(211,223,238,.92);
  flex-shrink:0;
  box-shadow:0 10px 24px rgba(15,23,42,.04);
}
.header-left { display:flex; align-items:center; gap:12px; }
.header-title-group { display:flex; flex-direction:column; gap:0; min-width:0; }
.page-title-icon {
  width:34px; height:34px; border-radius:12px;
  background:linear-gradient(180deg, rgba($primary,.12) 0%, rgba($primary,.06) 100%);
  color:$primary;
  display:flex; align-items:center; justify-content:center; flex-shrink:0;
  box-shadow:0 8px 18px rgba(9,88,217,.08);
}
.page-title { margin:0; font-size:18px; font-weight:700; color:$ink; letter-spacing:-.3px; }
.header-meta { display:flex; align-items:center; gap:6px; flex-wrap:wrap; }
.meta-year { font-size:12px; color:$sub; font-weight:500; }
.meta-divider { font-size:11px; color:$faint; }
.header-badge {
  display:flex; align-items:center; gap:4px;
  font-size:12px; color:$sub; background:$bg;
  padding:2px 9px 2px 7px; border-radius:20px; font-weight:500;
  .badge-dot { width:6px; height:6px; border-radius:50%; background:$primary; }
}

.header-actions { display:flex; align-items:center; gap:10px; }
.search-box {
  display:flex; align-items:center; gap:7px;
  background:$bg; border:1.5px solid transparent;
  border-radius:$rs; padding:7px 12px; width:220px; transition:all .2s;
  .search-icon { color:$sub; flex-shrink:0; }
  input { border:none; outline:none; font-size:13px; color:$ink; background:transparent; width:100%; min-width:0;
    &::placeholder { color:$faint; } }
  &.focused { background:$surface; border-color:$primary; box-shadow:0 0 0 3px rgba($primary,.1); }
  .search-clear { border:none; background:none; cursor:pointer; font-size:16px; color:$sub; padding:0; line-height:1;
    &:hover { color:$danger; } }
}
/* 通用图标按钮 */
.icon-btn {
  width:28px; height:28px; border:1px solid $border; background:$surface;
  border-radius:$rs; cursor:pointer; display:flex; align-items:center; justify-content:center;
  color:$sub; transition:all .18s; flex-shrink:0;
  &:hover { border-color:$primary; color:$primary; background:$primary-light; }
  &.icon-btn-active { border-color:$primary; color:$primary; background:$primary-light; }
}
.main-body { display:flex; flex:1; overflow:hidden; min-height:0; }
.main-day {
  display:grid;
  grid-template-columns:minmax(360px, 42%) minmax(0, 58%);
  gap:12px;
  padding:12px 16px 16px;
  min-height:0;
  align-items:stretch;
}

.left-panel {
  flex:none; min-width:0; max-width:none;
  display:flex; flex-direction:column; gap:12px;
  padding:0; overflow-y:auto; overflow-x:hidden;
  &::-webkit-scrollbar { width:4px; }
  &::-webkit-scrollbar-thumb { background:#d0d3da; border-radius:4px; }
  &::-webkit-scrollbar-track { background:transparent; }
}



.stats-row { display:grid; grid-template-columns:repeat(5,1fr); gap:12px; }
.stat-card {
  --stat-accent: #7b8ba2;
  --stat-number: #5f6f86;
  --stat-label: #7d8ca3;
  --stat-soft: rgba(123, 139, 162, .08);
  --stat-soft-strong: rgba(123, 139, 162, .16);
  position: relative;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255,255,255,.99) 0%, rgba(248,250,253,.97) 100%);
  border-radius: 18px;
  padding: 13px 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  box-shadow: 0 8px 20px rgba(15,23,42,.045);
  border: 1px solid rgba(218,226,236,.9);
  cursor: pointer;
  transition: all .2s ease;

  &::before {
    content: '';
    position: absolute;
    inset: 0 0 auto 0;
    height: 50px;
    background: linear-gradient(180deg, var(--stat-soft-strong) 0%, transparent 100%);
    opacity: .75;
    pointer-events: none;
  }

  &:hover {
    transform: translateY(-2px);
    border-color: var(--stat-soft-strong);
    box-shadow: 0 14px 26px rgba(15,23,42,.07);
  }

  &.stat-active {
    transform: translateY(-2px);
    border-color: transparent;
    background: linear-gradient(180deg, rgba(255,255,255,.99) 0%, var(--stat-soft) 180%);
    box-shadow: inset 0 0 0 1.5px var(--stat-soft-strong), 0 14px 28px rgba(15,23,42,.08);
  }
}
.stat-card-top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: flex-start;
}
.stat-icon-wrap {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--stat-accent);
  background: linear-gradient(180deg, var(--stat-soft) 0%, rgba(255,255,255,.96) 100%);
  box-shadow: inset 0 0 0 1px rgba(255,255,255,.62), 0 6px 14px rgba(15,23,42,.04);

  svg { width: 15px; height: 15px; }
}
.stat-all {
  --stat-accent: #5f86c9;
  --stat-number: #4169ae;
  --stat-label: #6f8fbe;
  --stat-soft: rgba(95, 134, 201, .10);
  --stat-soft-strong: rgba(95, 134, 201, .18);
}

.stat-todo {
  --stat-accent: #e39a32;
  --stat-number: #c98217;
  --stat-label: #c9974f;
  --stat-soft: rgba(227, 154, 50, .10);
  --stat-soft-strong: rgba(227, 154, 50, .18);
}
.stat-doing {
  --stat-accent: #4b84f2;
  --stat-number: #2d6fe8;
  --stat-label: #5c8ff0;
  --stat-soft: rgba(75, 132, 242, .10);
  --stat-soft-strong: rgba(75, 132, 242, .18);
}
.stat-done {
  --stat-accent: #4ab56a;
  --stat-number: #2d9a4f;
  --stat-label: #5ea976;
  --stat-soft: rgba(74, 181, 106, .10);
  --stat-soft-strong: rgba(74, 181, 106, .18);
}
.stat-cancelled {
  --stat-accent: #96a0af;
  --stat-number: #748092;
  --stat-label: #96a0af;
  --stat-soft: rgba(150, 160, 175, .09);
  --stat-soft-strong: rgba(150, 160, 175, .17);
}
.stat-info {
  position: relative;
  z-index: 1;

  em {
    display: block;
    font-style: normal;
    font-size: 22px;
    font-weight: 800;
    line-height: 1;
    color: var(--stat-number);
    letter-spacing: -.4px;
  }

  i {
    display: block;
    font-style: normal;
    font-size: 11px;
    color: var(--stat-label);
    margin-top: 4px;
  }
}
.stat-bar {
  position: relative;
  z-index: 1;
  height: 4px;
  background: rgba(148, 163, 184, .14);
  border-radius: 999px;
  overflow: hidden;

  .stat-bar-inner {
    height: 100%;
    border-radius: 999px;
    transition: width .6s cubic-bezier(.4,0,.2,1);
    min-width: 4px;
    background: linear-gradient(90deg, var(--stat-accent) 0%, var(--stat-accent) 100%);
  }
}


.legend { display:flex; align-items:center; gap:8px; flex-wrap:wrap; }
.calendar-legend { margin-left:auto; }
.leg-item {
  display:flex; align-items:center; gap:4px; font-size:11px; color:$sub; white-space:nowrap;
  padding:2px 0;
  i { width:8px; height:8px; border-radius:50%; display:inline-block; flex-shrink:0; }
}
/* 图例分隔符 */
.mo-legend .leg-divider {
  display:inline-block; width:1px; height:12px; background:$border; margin:0 6px; vertical-align:middle;
}

/* 日历卡片 */
/* 日历卡片 */
.cal-card {
  background:linear-gradient(180deg, rgba(255,255,255,.98) 0%, rgba(247,250,254,.98) 100%);
  border-radius:18px; padding:14px 12px 12px;
  box-shadow:0 12px 28px rgba(15,23,42,.06); border:1px solid rgba(211,223,238,.94); flex:1;
  display:flex; flex-direction:column; min-height:0; position:relative;
}
.cal-nav { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px; padding:0 2px; flex-shrink:0; }
.cal-nav-center { display:flex; align-items:center; gap:10px; flex:1; justify-content:center; position:relative; }
.nav-btn {
  width:32px; height:32px; border:1px solid rgba(211,223,238,.96); background:rgba(255,255,255,.95);
  border-radius:10px; cursor:pointer; display:flex; align-items:center; justify-content:center;
  color:$sub; transition:all .18s; flex-shrink:0; box-shadow:0 4px 12px rgba(15,23,42,.04);
  &:hover:not(:disabled) { border-color:$primary; color:$primary; background:$primary-light; box-shadow:0 8px 16px rgba(9,88,217,.08); }
  &:disabled { opacity:.3; cursor:not-allowed; }
}
.cal-month {
  font-size:16px; font-weight:700; color:$ink; letter-spacing:.2px;
  em { font-style:normal; font-size:13px; color:$sub; font-weight:500; }
}
.cal-month-btn {
  display:inline-flex; align-items:center; gap:4px;
  cursor:pointer; padding:6px 12px; border-radius:999px; transition:all .15s; user-select:none;
  background:rgba(255,255,255,.84); border:1px solid rgba(211,223,238,.9);
  &:hover { background:$primary-light; color:$primary; border-color:rgba($primary,.18); em { color:rgba($primary,.7); } .mp-arrow { color:$primary; } }
  .mp-arrow { color:$faint; transition:all .2s; &.open { transform:rotate(180deg); } }
}
.month-picker-popup {
  position:absolute; top:calc(100% + 10px); left:50%; transform:translateX(-50%);
  background:rgba(255,255,255,.98); border:1px solid rgba(211,223,238,.96); border-radius:16px;
  box-shadow:0 20px 40px rgba(15,23,42,.12); padding:14px; z-index:200; width:216px;
  backdrop-filter:blur(10px); animation:popup-in .15s ease;
}
@keyframes popup-in {
  from { opacity:0; transform:translateX(-50%) translateY(-6px); }
  to   { opacity:1; transform:translateX(-50%) translateY(0); }
}
.mp-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:10px; }
.mp-yr-label { font-size:14px; font-weight:700; color:$ink; }
.mp-yr-btn {
  width:28px; height:28px; border:1px solid rgba(211,223,238,.96); background:$surface;
  border-radius:8px; cursor:pointer; display:flex; align-items:center; justify-content:center;
  color:$sub; transition:all .15s;
  &:hover { border-color:$primary; color:$primary; background:$primary-light; }
}
.mp-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:6px; }
.mp-m-btn {
  font-size:12.5px; font-weight:500; padding:7px 0; border-radius:8px;
  border:1px solid transparent; background:transparent; cursor:pointer;
  color:$ink2; text-align:center; transition:all .15s;
  &:hover { background:$primary-light; color:$primary; }
  &.active { background:$primary; color:#fff; border-color:$primary; font-weight:700; box-shadow:0 8px 16px rgba(9,88,217,.2); }
}
.today-btn {
  font-size:12px; font-weight:700; color:$primary;
  background:$primary-light; border:none; border-radius:999px;
  padding:6px 14px; cursor:pointer; transition:all .18s; box-shadow:inset 0 0 0 1px rgba($primary,.08);
  &:hover { background:$primary; color:#fff; box-shadow:0 8px 16px rgba(9,88,217,.22); }
}
.month-fade-enter-active,.month-fade-leave-active { transition:opacity .18s,transform .18s; }
.month-fade-enter-from { opacity:0; transform:translateX(8px); }
.month-fade-leave-to { opacity:0; transform:translateX(-8px); }

.cal-grid-wrap { flex:1; display:flex; flex-direction:column; min-height:0; }
.cal-week-row {
  display:grid; grid-template-columns:repeat(7,1fr); margin-bottom:6px;
  span { text-align:center; font-size:11px; font-weight:700; color:#7b8798; padding:4px 0 8px; letter-spacing:1.5px;
    &.wk-end { color:#cf5b2d; } }
}
.cal-day-grid {
  display:grid; grid-template-columns:repeat(7,1fr); grid-auto-rows:1fr; flex:1; gap:6px;
  padding:8px; border-radius:16px; background:linear-gradient(180deg, rgba(244,247,252,.82) 0%, rgba(255,255,255,.82) 100%);
  box-shadow:inset 0 0 0 1px rgba(220,228,239,.72);
}
.day-cell {
  border-radius:12px; cursor:pointer; transition:all .18s ease;
  position:relative; border:1px solid transparent; min-height:54px;
  background:linear-gradient(180deg, rgba(255,255,255,.94) 0%, rgba(250,252,255,.96) 100%);
  box-shadow:inset 0 0 0 1px rgba(229,236,244,.72);
  .cell-inner { width:100%; height:100%; display:flex; flex-direction:column; align-items:center; justify-content:flex-start; padding:7px 3px 5px; }
  &:hover { background:linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%); border-color:rgba($primary,.16); box-shadow:inset 0 0 0 1px rgba($primary,.16), 0 8px 16px rgba(9,88,217,.05); }
  &.active { background:linear-gradient(180deg, #eef5ff 0%, #e8f0ff 100%); border-color:rgba($primary,.28); box-shadow:inset 0 0 0 1px rgba($primary,.22), 0 10px 18px rgba(9,88,217,.08); .cell-num { color:$primary; font-weight:800; } }
  &.today {
    background:linear-gradient(180deg, rgba(#ff7875,.14) 0%, rgba(#fff1f0,.86) 100%);
    box-shadow:inset 4px 0 0 #ff4d4f, inset 0 0 0 1px rgba(#ff4d4f,.16), 0 10px 18px rgba(255,77,79,.08);
    .cell-num { display:inline-flex; align-items:center; justify-content:center; width:28px; height:28px; border-radius:50%; background:#ff4d4f; color:#fff !important; font-weight:800; font-size:13px; box-shadow:0 2px 10px rgba(255,77,79,.38), 0 0 0 3px rgba(255,77,79,.12); }
  }
  &.other { background:#f7f8fa; opacity:.52; pointer-events:none; filter:saturate(.8); box-shadow:none; }
  &.weekend:not(.other) { background:linear-gradient(180deg, rgba(255,247,237,.55) 0%, rgba(255,255,255,.96) 100%); }
  &.weekend .cell-num { color:#cf5b2d; font-weight:700; }
  &.holiday .cell-num { color:#b8290e; font-weight:800; }
  &.has-events { background:linear-gradient(180deg, rgba(238,245,255,.78) 0%, rgba(255,255,255,.98) 100%); }
}
.cell-hd { display:flex; flex-direction:column; align-items:center; gap:3px; width:100%; justify-content:center; min-height:34px; flex-shrink:0; }
.cell-num { font-size:14px; font-weight:700; color:$ink; line-height:1.2; }
.cell-hd small {
  font-size:9px; color:#c4380d; background:#fff0e6; border-radius:4px; padding:0 4px; line-height:15px; font-weight:800;
  white-space:nowrap; max-width:32px; overflow:hidden; text-overflow:ellipsis; display:block; border:1px solid rgba(196,56,13,.18);
}
.cell-dots {
  display:flex; justify-content:center; gap:3px; padding-top:4px;
  i { width:6px; height:6px; border-radius:50%; display:inline-block; flex-shrink:0; box-shadow:0 0 0 2px rgba(255,255,255,.8);
    &.dot-more { width:auto; height:auto; font-size:8px; color:$sub; background:none !important; font-style:normal; line-height:1; font-weight:800; box-shadow:none; } }
}
.dot-more {
  display:inline-flex; align-items:center; justify-content:center;
}
.cal-loading { position:absolute; inset:0; background:rgba(255,255,255,.8); display:flex; align-items:center; justify-content:center; z-index:10; border-radius:$rs; }
.loading-spinner { width:28px; height:28px; border:3px solid $border; border-top-color:$primary; border-radius:50%; animation:spin .6s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }

.toolbar {
  display:flex; align-items:center; gap:8px; flex-shrink:0; flex-wrap:wrap;
  margin-top:2px; padding:4px 2px 0;
}
.toolbar-right {
  display:flex; align-items:center; gap:8px; margin-left:auto;
  padding:8px 10px; border-radius:14px; background:rgba(255,255,255,.76); border:1px solid rgba(219,226,234,.88);
}

.kbd-hints-wrap {
  position:relative; flex-shrink:0; align-self:flex-start;
  &:hover .kbd-hints-panel { opacity:1; pointer-events:auto; transform:translateY(0); }
}
.kbd-hints-toggle {
  display:inline-flex; align-items:center; gap:5px;
  font-size:11px; color:$faint; cursor:default; padding:3px 0; transition:color .15s;
  svg { flex-shrink:0; } &:hover { color:$sub; }
}
.kbd-hints-panel {
  position:absolute; bottom:calc(100% + 6px); left:0;
  background:$surface; border:1px solid $border; border-radius:$rs;
  box-shadow:$shadow-md; padding:10px 14px;
  display:flex; flex-direction:column; gap:7px; min-width:190px;
  opacity:0; pointer-events:none; transform:translateY(4px); transition:all .2s; z-index:50;
}
.kbd-hint-row {
  display:flex; align-items:center; gap:5px;
  span { font-size:11.5px; color:$ink2; margin-left:auto; }
  kbd { display:inline-block; font-family:inherit; font-size:10px; font-weight:600; background:$bg; border:1px solid $border; border-radius:3px; padding:1px 5px; line-height:1.5; color:$ink2; box-shadow:0 1px 2px rgba(0,0,0,.06); }
}


.right-panel {
  flex:none; display:flex; flex-direction:column; min-width:0;
  background:linear-gradient(180deg, rgba(255,255,255,.98) 0%, rgba(247,250,254,.98) 100%);
  border:1px solid rgba(211,223,238,.94); border-radius:18px; overflow:hidden; box-shadow:0 12px 28px rgba(15,23,42,.06);
}
.rp-head {
  display:flex; align-items:center; justify-content:space-between;
  padding:12px 16px 8px; flex-shrink:0;
  background:linear-gradient(180deg, rgba(255,255,255,.98) 0%, rgba(244,248,253,.95) 100%);
  gap:8px; flex-wrap:nowrap; border-bottom:1px solid rgba(223,231,241,.84);
}
.rp-date-info { display:flex; align-items:center; gap:8px; min-width:0; }
.rp-date-main { display:flex; align-items:center; gap:8px; min-width:0; }
.rp-day-num { font-size:30px; font-weight:900; color:$ink; line-height:1; font-variant-numeric:tabular-nums; letter-spacing:-1px; }
.rp-date-sub { display:flex; align-items:center; gap:6px; flex-wrap:wrap; min-width:0; }
.rp-date-text { font-size:12px; font-weight:700; color:$ink2; }
.rp-rel-date {
  font-size:10px; font-weight:700; padding:0 8px; border-radius:999px; display:inline-flex; align-items:center; height:21px; width:fit-content;
  &.rel-today { background:#e6f4ff; color:$primary; }
  &.rel-future { background:#edf8ef; color:$success; }
  &.rel-past { background:#f3f5f8; color:$sub; }
}
.rp-date-info :deep(.el-tag) {
  height:21px; padding:0 8px; font-size:10px; line-height:19px;
}
.rp-head-right { display:flex; align-items:center; gap:10px; margin-left:auto; }
.rp-toolbar {
  display:flex; align-items:center; justify-content:space-between; gap:12px; flex-wrap:nowrap;
  padding:10px 16px; border-bottom:1px solid rgba(223,231,241,.84);
  background:linear-gradient(180deg, rgba(250,252,255,.92) 0%, rgba(255,255,255,.96) 100%);
}
.rp-toolbar-left { display:flex; align-items:center; gap:8px; min-width:0; }
.quick-filters { display:flex; gap:3px; background:#f5f7fb; padding:4px; border-radius:12px; border:1px solid rgba(219,226,234,.96); flex-wrap:nowrap; box-shadow:inset 0 1px 0 rgba(255,255,255,.8); }
.qf-btn {
  font-size:10.5px; font-weight:500; padding:4px 8px;
  border:1px solid transparent; border-radius:5px; background:transparent; cursor:pointer; color:$sub; transition:all .15s; white-space:nowrap;
  display:flex; align-items:center; gap:4px;
  &:hover:not(.active) { color:$ink; background:$surface; border-color:rgba($primary,.08); }
  &.active { background:$primary; color:#fff; font-weight:600; box-shadow:0 1px 4px rgba($primary,.2); }
  &.qf-todo.active     { background:$warning; }
  &.qf-doing.active    { background:#2563eb; }
  &.qf-done.active     { background:$success; }
  &.qf-cancelled.active{ background:#64748b; }
}
.qf-dot {
  display:inline-block; width:7px; height:7px; border-radius:50%; flex-shrink:0;
}
.rp-nav-btns { display:flex; gap:5px; }
.rp-arrow {
  width:26px; height:26px; border:1px solid $border; background:$surface;
  border-radius:$rs; cursor:pointer; display:flex; align-items:center; justify-content:center;
  color:$sub; transition:all .18s;
  &:hover { border-color:$primary; color:$primary; background:$primary-light; }
}



/* [C] 批量操作栏 */
.batch-bar {
  display:flex; align-items:center; justify-content:space-between;
  padding:7px 15px; background:#fffbe6; border-bottom:1px solid #ffe58f;
  flex-shrink:0; gap:9px;
}


.batch-check-all {
  display:flex; align-items:center; gap:8px; cursor:pointer;
  input[type="checkbox"] { position:absolute; opacity:0; width:0; height:0; }
  .check-box {
    display:block; width:16px; height:16px; border:1.8px solid #c0c4cc; border-radius:4px;
    transition:all .18s; position:relative; background:$surface; flex-shrink:0;
  }
  input:checked ~ .check-box { background:$primary; border-color:$primary;
    &::after { content:''; position:absolute; left:4px; top:1.5px; width:4px; height:8px; border:solid #fff; border-width:0 2px 2px 0; transform:rotate(45deg); }
  }
  input:indeterminate ~ .check-box { background:$primary; border-color:$primary;
    &::after { content:''; position:absolute; left:3px; top:6px; width:8px; height:2px; background:#fff; border-radius:1px; }
  }
}
.batch-count { font-size:12px; color:$ink2; font-weight:600; }
.batch-actions { display:flex; align-items:center; gap:6px; }
.batch-action-btn {
  display:inline-flex; align-items:center; gap:4px;
  font-size:11.5px; font-weight:600; padding:5px 10px; border-radius:6px;
  border:1px solid transparent; cursor:pointer; transition:all .15s;
  &:disabled { opacity:.4; cursor:not-allowed; }
}


.batch-done-btn { background:#f6ffed; color:$success; border-color:#b7eb8f; &:hover:not(:disabled) { background:$success; color:#fff; } }
.batch-del-btn { background:#fff1f0; color:$danger; border-color:#ffa39e; &:hover:not(:disabled) { background:$danger; color:#fff; } }
.batch-cancel-btn { background:$bg; color:$sub; border-color:$border; &:hover { color:$ink; background:$surface; } }
.batch-bar-slide-enter-active,.batch-bar-slide-leave-active { transition:all .22s ease; overflow:hidden; }
.batch-bar-slide-enter-from,.batch-bar-slide-leave-to { opacity:0; max-height:0; padding-top:0; padding-bottom:0; }
.batch-bar-slide-enter-to,.batch-bar-slide-leave-from { max-height:60px; }

.day-focus-strip {
  display:flex; align-items:center; justify-content:space-between;
  gap:10px; padding:8px 16px; border-bottom:1px solid rgba($primary,.08);
  background:linear-gradient(135deg, rgba($primary,.05) 0%, rgba($primary,.015) 100%);
  flex-shrink:0;
}
.dfs-main { flex:1; min-width:0; }
.dfs-sub {
  display:flex; align-items:center; gap:6px; flex-wrap:wrap;
}
.dfs-chip {
  display:inline-flex; align-items:center; gap:4px; padding:3px 9px;
  border-radius:999px; border:1px solid rgba($border,.95); background:#fff; color:$ink2;
  font-size:11px; font-weight:600;
  &.dfs-chip-primary { color:$primary; background:$primary-light; border-color:rgba($primary,.14); }
  &.dfs-chip-success { color:$success; background:#e8f7ed; border-color:rgba($success,.14); }
  &.dfs-chip-status { color:$warning; background:#fff4db; border-color:rgba($warning,.18); }
  &.dfs-chip-search { color:#9a6700; background:#fff8df; border-color:rgba(250,173,20,.2); }
}
.dfs-progress {
  width:120px; display:flex; flex-direction:column; gap:4px; flex-shrink:0;
}
.dfs-progress-text {
  font-size:14px; font-weight:800; color:$primary; text-align:right;
}
.dfs-progress-track {
  height:6px; border-radius:999px; overflow:hidden; background:rgba($primary,.1);
}
.dfs-progress-fill {
  height:100%; border-radius:999px; background:linear-gradient(90deg, $primary 0%, $success 100%);
  transition:width .35s ease;
}

.rp-body {
  flex:1; overflow-y:auto; padding:10px 0 12px;
  scrollbar-width:none; -ms-overflow-style:none;
  &::-webkit-scrollbar { width:0; height:0; display:none; }
}


.list-loading { padding:0 14px; }
.loading-skeleton {
  display:flex; align-items:center; gap:10px;
  padding:13px 14px; border-radius:$rs; border:1px solid $border; margin-bottom:6px;
  background-image:linear-gradient(90deg,#fafafa 25%,#f0f0f0 50%,#fafafa 75%);
  background-size:200% 100%; animation:shimmer 1.4s ease infinite;
  .sk-left { display:flex; align-items:center; gap:8px; flex-shrink:0; }
  .sk-time { width:40px; height:13px; background:#eee; border-radius:4px; }
  .sk-check { width:16px; height:16px; background:#eee; border-radius:4px; }
  .sk-content { flex:1; display:flex; flex-direction:column; gap:6px;
    .sk-line { height:11px; background:#eee; border-radius:4px; }
    .sk-line-1 { width:65%; } .sk-line-2 { width:38%; } }
}
@keyframes shimmer { 0% { background-position:-200% 0; } 100% { background-position:200% 0; } }
.rp-error {
  display:flex; flex-direction:column; align-items:center; justify-content:center; padding:60px 20px; gap:10px;
  p { font-size:13.5px; color:$sub; margin:0; }
}
.agenda-wrap {
  display:flex; flex-direction:column; gap:10px; padding:2px 0 0;
}
.agenda-section {
  display:flex; flex-direction:column; gap:6px;
}
.agenda-section-head {
  display:flex; align-items:center; justify-content:space-between;
  gap:8px; padding:0 16px; min-height:24px;
}
.agenda-section-title {
  font-size:12px; font-weight:800; color:$ink;
}
.agenda-section-hint {
  margin-left:5px; font-size:10.5px; color:$faint;
}
.agenda-section-count {
  font-size:10.5px; font-weight:800; color:$primary; background:rgba($primary,.08);
  border-radius:999px; padding:3px 8px; border:1px solid rgba($primary,.12);
}
.ev-group { padding:0 16px; display:flex; flex-direction:column; gap:6px; }
.ev-card {
  width:100%; box-sizing:border-box;
  display:grid; grid-template-columns:26px minmax(0,1fr) 200px; align-items:center; column-gap:10px;
  padding:10px 12px; min-height:60px; border-radius:14px; cursor:pointer;
  transition:all .18s ease; border:1px solid rgba(219,226,234,.72); background:rgba(255,255,255,.92); position:relative;
  box-shadow:0 6px 16px rgba(15,23,42,.04);

  &:hover { transform:translateY(-1px); box-shadow:0 12px 24px rgba(15,23,42,.08); }
  &.pri-0 { border-left:3px solid $faint; padding-left:9px; }
  &.pri-1 { border-left:3px solid $warning; padding-left:9px; }
  &.pri-2 { border-left:3px solid $danger; padding-left:9px; }

  &.ev-card-todo {
    background:linear-gradient(180deg, #fffaf1 0%, #fff6eb 100%); border-color:rgba(217,119,6,.18);
    &:hover { background:linear-gradient(180deg, #fff7e8 0%, #fff1df 100%); border-color:rgba(217,119,6,.26); }
  }
  &.ev-card-doing {
    background:linear-gradient(180deg, #f4f8ff 0%, #eef4ff 100%); border-color:rgba(9,88,217,.18);
    &:hover { background:linear-gradient(180deg, #edf4ff 0%, #e5efff 100%); border-color:rgba(9,88,217,.26); }
  }
  &.ev-card-done {
    background:linear-gradient(180deg, #f5fcf6 0%, #eef8f0 100%); border-color:rgba(47,158,68,.18);
    &:hover { background:linear-gradient(180deg, #eef9ef 0%, #e7f5e9 100%); border-color:rgba(47,158,68,.26); }
  }
  &.ev-card-cancelled {
    background:linear-gradient(180deg, #f7f8fa 0%, #f1f3f6 100%); border-color:rgba(134,144,156,.22);
    &:hover { background:linear-gradient(180deg, #f1f3f6 0%, #eceff3 100%); border-color:rgba(134,144,156,.32); }
    .ev-title-row .strike { text-decoration:line-through; color:#86909c; }
  }

  &.ev-selected { background:#eaf2ff !important; border-color:rgba($primary,.32) !important; box-shadow:0 12px 24px rgba(9,88,217,.1); }
  &.batch-mode { cursor:pointer; }
  &.is-batch-disabled { cursor:not-allowed; }
}
.ev-check-cancelled {
  width:16px; height:16px; border-radius:50%; border:1.5px solid #c7ced8;
  display:flex; align-items:center; justify-content:center;
  color:#86909c; font-size:11px; line-height:1; flex-shrink:0;
  cursor:default;
}

.cancelled-tag {
  background:#f2f3f5 !important; color:#86909c !important;
}

/* [C] 批量复选框 */
.ev-batch-check { display:flex; align-items:center; justify-content:center; width:26px; flex-shrink:0; }
.batch-cb {
  width:16px; height:16px; border-radius:5px; border:1.6px solid #c0c4cc;
  display:flex; align-items:center; justify-content:center; background:$surface;
  transition:all .15s; flex-shrink:0;
  &.checked { background:$primary; border-color:$primary; color:#fff; }
}

.ev-left { display:flex; align-items:center; justify-content:center; gap:4px; width:26px; min-width:26px; }
.ev-check {
  flex-shrink:0; position:relative; cursor:pointer;
  input[type="checkbox"] { position:absolute; opacity:0; width:0; height:0; }
  .check-box { display:block; width:16px; height:16px; border:1.6px solid #c0c4cc; border-radius:5px; transition:all .18s; position:relative; background:$surface; }
  &:hover .check-box { border-color:$primary; }
  &.checked .check-box { background:$success; border-color:$success;
    &::after { content:''; position:absolute; left:4px; top:1.4px; width:4px; height:7.5px; border:solid #fff; border-width:0 2px 2px 0; transform:rotate(45deg); } }
}
.ev-main { min-width:0; display:flex; flex-direction:column; justify-content:center; gap:3px; min-height:38px; }
.ev-title-row { display:flex; align-items:center; gap:5px; flex-wrap:nowrap; min-width:0; min-height:19px; }
.ev-inline-time {
  width:72px; min-width:72px; flex-shrink:0;
  display:inline-flex; align-items:center; justify-content:center;
  font-size:10.5px; font-weight:800; color:$ink2; font-variant-numeric:tabular-nums;
  padding:2px 7px; border-radius:999px; background:#f3f6fa; white-space:nowrap; text-align:center;
  box-shadow:inset 0 0 0 1px rgba(219,226,234,.8);
  &.is-muted { color:$faint; background:#f5f7fa; }
}

.ev-tit {
  font-size:13px; font-weight:700; color:$ink;
  white-space:nowrap; overflow:hidden; text-overflow:ellipsis; min-width:0; flex:1; cursor:text;
  &.strike { text-decoration:line-through; color:$faint; font-weight:500; }
}
.ev-tit-input {
  flex:1; min-width:0; font-size:13px; font-weight:700; color:$ink;
  border:1.5px solid $primary; border-radius:8px; outline:none;
  padding:3px 8px; background:$primary-light; box-shadow:0 0 0 3px rgba($primary,.1);
}
.ev-side {
  width:200px; min-width:200px; display:flex; flex-direction:column; align-items:flex-end; justify-content:center; gap:6px;
}
.ev-side-top {
  width:100%; display:grid; grid-template-columns:60px 56px 72px; align-items:center; justify-content:end; column-gap:6px;
}
.ev-cat-tag {
  display:inline-flex; align-items:center; justify-content:center; width:60px; min-width:60px;
  font-size:10px; font-weight:700; padding:2px 4px; border-radius:999px; white-space:nowrap; box-sizing:border-box;
  box-shadow:inset 0 0 0 1px rgba(255,255,255,.45);
  &.done-tag { background:#edf7eb !important; color:$success !important; }
}
.ev-priority-slot {
  width:56px; min-width:56px; min-height:20px; display:flex; align-items:center; justify-content:center;
}
.ev-priority-slot :deep(.el-tag) {
  width:100%; margin:0; font-size:10px; justify-content:center; padding:0 4px;
}

.ev-detail-row { display:flex; align-items:center; min-height:16px; }


.ev-remark {
  flex:1; min-width:0; font-size:11px; color:#66758a; line-height:1.4;
  overflow:hidden; white-space:nowrap; text-overflow:ellipsis; cursor:help; display:block;
}

.ev-remark-empty {
  visibility:hidden; pointer-events:none;
}
:deep(.calendar-remark-tooltip) {
  max-width:300px; padding:0 !important; border-radius:8px !important; box-shadow:0 10px 28px rgba(15,23,42,.12);
}
.calendar-remark-tooltip-content {
  max-width:300px; padding:10px 12px; white-space:pre-wrap; word-break:break-word; line-height:1.6; color:$ink2;
}

.doing-pulse {
  width:6px; height:6px; border-radius:50%; background:$primary;
  flex-shrink:0; display:inline-block; animation:pulse-dot 1.6s ease-out infinite;
}


@keyframes pulse-dot {
  0%   { box-shadow:0 0 0 0 rgba(22,119,255,.55); }
  70%  { box-shadow:0 0 0 7px rgba(22,119,255,0); }
  100% { box-shadow:0 0 0 0 rgba(22,119,255,0); }
}
@keyframes mdp-status-pulse {
  0%, 100% { transform:scale(1); }
  50% { transform:scale(.92); }
}
.ev-status-line {
  width:72px; min-width:72px; display:grid; grid-template-columns:8px minmax(0, 1fr); align-items:center; justify-content:end; column-gap:4px; min-height:19px;
}
.ev-status-indicator {
  width:8px; min-width:8px; display:flex; align-items:center; justify-content:center;
}
.ev-status-chip {
  display:inline-flex; align-items:center; justify-content:center; width:100%; min-width:0; height:19px; padding:0 6px;
  border-radius:999px; font-size:10.5px; font-weight:700; white-space:nowrap; border:1px solid transparent;

  &.is-0 { background:#ffe7ba; border-color:#ffd591; color:#ad6800; }
  &.is-1 { background:#dbeafe; border-color:#bfdbfe; color:#1d4ed8; }
  &.is-2 { background:#dcfce7; border-color:#bbf7d0; color:#15803d; }
  &.is-3 { background:#e5e7eb; border-color:#d1d5db; color:#475569; }
  &.is-loading { opacity:.68; }
}

.ev-status-actions {
  width:100%; display:flex; align-items:center; justify-content:flex-end; gap:6px;
}
.ev-card.batch-mode .ev-side {
  gap:0;
}

.ev-status-btn {
  width:48px; height:24px; padding:0; border-radius:999px; border:1px solid #dbe3ef;
  background:#fff; color:$ink2; cursor:pointer; display:inline-flex; align-items:center; justify-content:center;
  font-size:10.5px; font-weight:500; line-height:1; transition:all .15s; box-shadow:0 1px 2px rgba(15,23,42,.04);
  &:hover:not(:disabled) { transform:none; box-shadow:0 1px 3px rgba(15,23,42,.08); }
  &:disabled { cursor:wait; opacity:.64; }
  &.is-pending { background:#f5f7fa; }

  &.is-todo {
    border-color:#f5c78a; color:#ad6800; background:#fffdf8;
    &:hover:not(:disabled) { background:$warning; border-color:$warning; color:#fff; }
  }
  &.is-doing {
    border-color:#93c5fd; color:#1d4ed8; background:#f8fbff;
    &:hover:not(:disabled) { background:$primary; border-color:$primary; color:#fff; }
  }
  &.is-done {
    border-color:#86efac; color:#15803d; background:#f8fffa;
    &:hover:not(:disabled) { background:$success; border-color:$success; color:#fff; }
  }
  &.is-cancelled {
    border-color:#cbd5e1; color:#475569; background:#fcfcfd;
    &:hover:not(:disabled) { background:#64748b; border-color:#64748b; color:#fff; }
  }
  &.is-delete {
    width:48px; padding:0;
    border-color:#fecaca; color:#dc2626; background:#fef2f2;
    &:hover:not(:disabled) { background:#ef4444; border-color:#ef4444; color:#fff; }
  }
}



.rp-empty { display:flex; flex-direction:column; align-items:center; justify-content:center; padding:40px 20px; }
.rp-filter-empty { gap:10px; p { font-size:13px; } }
.empty-illus { margin-bottom:16px; line-height:0; }
.empty-title { font-size:14.5px; font-weight:600; color:$ink2; margin:0 0 6px; }
.empty-hint {
  font-size:12px; color:$faint; text-align:center; line-height:1.7; margin:0;
  kbd { display:inline-block; font-family:inherit; font-size:10px; font-weight:600; background:$surface; border:1px solid $border; border-radius:3px; padding:1px 5px; color:$ink2; }
}

.rp-foot { padding:9px 15px; border-top:1px solid $border; flex-shrink:0; background:#fafbfc; }
.foot-hint {
  font-size:11.5px; color:$faint;

  kbd { display:inline-block; font-family:inherit; font-size:10px; font-weight:600; background:$surface; border:1px solid $border; border-radius:3px; padding:1px 4px; color:$ink2; box-shadow:0 1px 2px rgba(0,0,0,.06); }
}

.foot-progress-wrap { display:flex; flex-direction:column; gap:5px; }

.foot-progress-labels { display:flex; align-items:center; justify-content:space-between; }
.foot-progress-title { font-size:11.5px; font-weight:600; color:$ink2; }
.foot-progress-pct { font-size:12px; font-weight:700; color:$primary; }
.foot-progress-track { height:5px; background:$border; border-radius:4px; overflow:hidden; }
.foot-progress-fill {
  height:100%; border-radius:4px;
  background:linear-gradient(to right,$primary,$success);
  transition:width .6s cubic-bezier(.4,0,.2,1); min-width:4px;
}
.foot-progress-counts {
  display:flex; align-items:center; gap:12px; font-size:11px; color:$sub;
  em { font-style:normal; font-weight:700; margin-right:2px;
    &.clr-warning { color:$warning; } &.clr-primary { color:$primary; } &.clr-success { color:$success; } &.clr-info { color: $sub; } }
}

/* [E] 打印预览 */
.print-preview { padding:20px 24px; }
.pp-header { border-bottom:2px solid $primary; padding-bottom:12px; margin-bottom:16px; }
.pp-header-month { margin-bottom:12px; }
.pp-title { font-size:18px; font-weight:700; color:$primary; margin-bottom:3px; }
.pp-date { font-size:12px; color:$sub; }
.pp-body {}
.pp-empty { text-align:center; color:$faint; padding:30px 0; font-size:14px; }
.pp-row {
  display:flex; align-items:center; gap:10px; padding:8px 0; border-bottom:1px solid #f0f2f5;
  .pp-num { width:18px; font-size:11px; color:$faint; flex-shrink:0; text-align:right; }
  .pp-cat-dot { width:8px; height:8px; border-radius:50%; flex-shrink:0; }
  .pp-tit { flex:1; font-size:13px; font-weight:600; color:$ink; }
  .pp-time { font-size:11px; color:$sub; white-space:nowrap; }
  .pp-status { font-size:11px; padding:1px 8px; border-radius:10px; white-space:nowrap;
    &.pp-s-0 { background:#fff7e6; color:$warning; }
    &.pp-s-1 { background:$primary-light; color:$primary; }
    &.pp-s-2 { background:#f6ffed; color:$success; }
    &.pp-s-3 { background:#f2f3f5; color:#86909c; } }
}
.pp-filter-summary { display:flex; flex-wrap:wrap; gap:8px; margin-bottom:14px; }
.pp-filter-chip {
  display:inline-flex; align-items:center; padding:4px 10px; border-radius:999px;
  background:#f2f3f5; color:#4e5969; font-size:11px; font-weight:600;
}
.pp-filter-chip-primary { background:$primary-light; color:$primary; }
.pp-month-wrap {
  border:1px solid $border; border-radius:$rs-lg; overflow:hidden; background:$surface;
}
.pp-month-week {
  display:grid; grid-template-columns:repeat(7,minmax(0,1fr));
  background:linear-gradient(to bottom, #f6f8fc, #f2f4f8);
  border-bottom:1px solid $border;
  span {
    text-align:center; padding:8px 0; font-size:11px; font-weight:700; color:$sub; letter-spacing:1px;
    &.wk-end { color:$danger; }
  }
}
.pp-month-grid {
  display:grid;
  grid-template-columns:repeat(7,minmax(0,1fr));
  grid-template-rows:repeat(var(--pp-row-count, 6), minmax(96px, 1fr));
}
.pp-month-cell {
  min-height:96px; background:$surface; padding:8px 8px 10px;
  border-right:1px solid $border; border-bottom:1px solid $border;
  display:flex; flex-direction:column; gap:6px;
  &:nth-child(7n) { border-right:none; }
  &:nth-last-child(-n+7) { border-bottom:none; }
  &.other { background:#f7f8fa; opacity:.55; }
  &.weekend:not(.other) { background:#fff8f8; }
  &.today { background:#f0f7ff; box-shadow:inset 3px 0 0 $primary; }
  &.empty:not(.other) { background:#fcfcfd; }
}
.pp-month-cell-hd { display:flex; align-items:center; justify-content:space-between; gap:6px; }
.pp-month-day { font-size:13px; font-weight:800; color:$ink; }
.pp-month-cell.today .pp-month-day { color:$primary; }
.pp-month-count {
  min-width:18px; height:18px; padding:0 5px; border-radius:10px;
  background:$primary; color:#fff; font-size:10px; font-weight:800;
  display:inline-flex; align-items:center; justify-content:center;
}
.pp-month-cell-body { display:flex; flex-direction:column; gap:4px; flex:1; min-height:0; }
.pp-month-event {
  display:flex; align-items:center; gap:5px; padding:3px 5px; border-radius:6px;
  background:#f7f8fa; font-size:11px; line-height:1.35;
  &.is-s-1 { background:#eef6ff; }
  &.is-s-2 { opacity:.62; }
  &.is-s-3 { opacity:.45; }
}
.pp-month-event-dot { width:6px; height:6px; border-radius:50%; flex-shrink:0; }
.pp-month-event-title {
  flex:1; min-width:0; font-size:9px; font-weight:600; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;
}
.pp-month-more { margin-top:auto; font-size:11px; font-weight:700; color:$primary; }
.pp-month-empty { margin-top:auto; font-size:11px; color:$faint; }
.pp-month-summary {
  display:flex; align-items:flex-end; justify-content:space-between; gap:12px; margin-top:14px;
}
.pp-month-stats { display:flex; flex-wrap:wrap; gap:14px; }
.pp-month-stat {
  font-size:12px; color:$ink2; font-weight:600;
  strong { font-weight:800; }
  &.todo strong { color:$warning; }
  &.doing strong { color:$primary; }
  &.done strong { color:$success; }
  &.cancelled strong { color:#86909c; }
}
.pp-footer { margin-top:14px; font-size:11px; color:$faint; text-align:right; }

/* 列表动画 */
.ev-list-enter-active { transition:all .22s ease; }
.ev-list-leave-active { transition:all .18s ease; position:absolute; width:100%; }
.ev-list-enter-from { opacity:0; transform:translateY(-6px); }
.ev-list-leave-to { opacity:0; transform:translateX(10px); }
.ev-list-move { transition:transform .22s ease; }

/* 响应式 */
@media (max-width:1150px) {
  .left-panel { padding:14px 16px; }
  .toolbar .legend { display:none; }
  .search-box { width:170px; }
  .kbd-hints-wrap { display:none; }
}
@media (max-width:900px) {
  .main-day { grid-template-columns:1fr; }
  .left-panel { max-height:52vh; border-bottom:1px solid $border; padding:12px 14px; }
  .right-panel { border-left:none; }
  .header-actions .search-box { display:none; }
  .rp-day-num { font-size:28px; }
  .rp-head { flex-wrap:wrap; gap:8px; }
  .quick-filters { order:3; }
  .day-focus-strip { flex-direction:column; align-items:flex-start; }
  .dfs-progress { width:100%; }
  .dfs-progress-text { text-align:left; }
  .mo-toolbar-actions { justify-content:space-between; }
}

/* ============================================================
   [M] 月视图模式 - 纯增量样式，不影响日视图
   ============================================================ */

/* 视图切换按钮组 */
.view-mode-switch {
  display:flex; background:$bg; border-radius:$rs; padding:1px; gap:1px; border:1px solid $border;
}
.vm-btn {
  display:inline-flex; align-items:center; gap:3px;
  font-size:11px; font-weight:500; padding:3px 8px;
  border:none; border-radius:6px; background:transparent; cursor:pointer;
  color:$sub; transition:all .15s; white-space:nowrap;
  svg{flex-shrink:0;}
  &:hover:not(.active) { color:$ink; background:$surface; }
  &.active { background:$primary; color:#fff; box-shadow:0 2px 6px rgba($primary,.35); }
}

/* 月视图容器：铺满，不滚动 */
.main-month {
  flex:1;
  overflow:hidden !important;  /* 一屏无滚动 */
  display:flex;
  flex-direction:column;
}

.month-overview {
  width:100%; height:100%;
  display:flex; flex-direction:column;
  padding:6px 16px 10px;
  gap:6px;
  overflow:hidden;
}

/* 月视图工具栏 */
.mo-toolbar {
  display:flex;
  align-items:center;
  justify-content:flex-start;
  padding:1px 0 2px;
  flex-shrink:0;
}
.mo-calendar-stage {
  flex:1;
  min-height:0;
  display:flex;
  align-items:center;
  justify-content:center;
  padding:0 6px 36px;
}
.mo-calendar-panel {
  width:min(100%, 1680px);
  min-height:0;
  height:min(100%, clamp(580px, calc(100vh - 238px), 780px));
  display:flex;
  flex-direction:column;
  margin:0 auto;
  padding:10px 10px 8px;
  border:1px solid rgba(219,226,234,.88);
  border-radius:18px;
  background:linear-gradient(180deg, rgba(255,255,255,.98) 0%, rgba(247,250,254,.98) 100%);
  box-shadow:0 18px 42px rgba(15,23,42,.08), inset 0 1px 0 rgba(255,255,255,.85);
}
.mo-toolbar-actions {
  display:flex;
  align-items:center;
  justify-content:flex-start;
  gap:6px 10px;
  flex-wrap:wrap;
  width:100%;
}
.mo-nav {
  display:flex; align-items:center; gap:6px;
}

/* 月份标题可点击 */
.mo-nav-center { position:relative; display:flex; align-items:center; }
.mo-title {
  font-size:14.5px; font-weight:700; color:$ink; letter-spacing:.5px;
  min-width:100px; text-align:center;
}
.mo-month-btn {
  display:inline-flex; align-items:center; gap:5px;
  cursor:pointer; padding:4px 10px; border-radius:$rs; transition:all .15s; user-select:none;
  &:hover { background:$primary-light; color:$primary; }
  .mo-mp-arrow { color:$faint; transition:transform .2s; flex-shrink:0; &.open { transform:rotate(180deg); } }
}

/* 月份选择弹窗 */
.mo-month-picker-popup {
  position:absolute; top:calc(100% + 8px); left:50%; transform:translateX(-50%);
  background:$surface; border:1px solid $border; border-radius:$rs-lg;
  box-shadow:$shadow-lg; padding:14px; z-index:200; width:216px;
  animation:popup-in .15s ease;
}

/* 月份切换动画 */
.mo-month-fade-enter-active, .mo-month-fade-leave-active { transition:opacity .14s, transform .14s; }
.mo-month-fade-enter-from { opacity:0; transform:translateX(2px); }
.mo-month-fade-leave-to { opacity:0; transform:translateX(-2px); }

/* 月网格切换淡入淡出：避免先出格子骨架再填数据的闪烁 */
.mo-grid-fade-enter-active {
  transition: opacity .18s ease, transform .16s ease;
}
.mo-grid-fade-leave-active {
  /* leave 时用更快的动画，减少等待感 */
  transition: opacity .08s ease, transform .06s ease;
  position: absolute;
  width: 100%;
  pointer-events: none;
}
.mo-grid-fade-enter-from {
  opacity: 0;
  transform: translateY(6px) scale(0.995);
}
.mo-grid-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.998);
}

.mo-filters { display:flex; align-items:center; gap:8px; margin-left:auto; }

/* [M] 状态筛选按钮组 */
.mo-status-filters {
  display:flex; gap:3px; background:rgba(255,255,255,.88); padding:2px; border-radius:8px; border:1px solid rgba(219,226,234,.92);
  box-shadow:0 4px 14px rgba(15,23,42,.04);
}
.msf-btn {
  font-size:10.5px; font-weight:600; padding:3px 8px;
  border:1px solid transparent; border-radius:6px; background:transparent; cursor:pointer;
  color:#64748b; transition:all .15s ease; white-space:nowrap;
  &:hover:not(.active) { color:$ink; background:#f8fbff; border-color:rgba($primary,.10); }
  &.active {
    background:linear-gradient(180deg, #2b6ef2 0%, #0958d9 100%); color:#fff; font-weight:700; box-shadow:0 4px 10px rgba($primary,.24);
  }
  &.msf-todo.active { background:linear-gradient(180deg, #f0a53b 0%, #d97706 100%); }
  &.msf-doing.active { background:linear-gradient(180deg, #4d8eff 0%, #2563eb 100%); }
  &.msf-done.active { background:linear-gradient(180deg, #46b96f 0%, #2f9e44 100%); }
  &.msf-cancelled.active { background:linear-gradient(180deg, #7a8798 0%, #64748b 100%); }
}
/* 月视图状态按钮内的小色点 */
.msf-btn { display:inline-flex; align-items:center; gap:4px; }
.msf-dot {
  display:inline-block; width:7px; height:7px; border-radius:50%; flex-shrink:0;
}

.mo-legend { display:flex; align-items:center; gap:6px; flex-wrap:wrap; color:#64748b; }

/* 星期标题行 */
.mo-week-header {
  display:grid; grid-template-columns:repeat(7,1fr);
  background:linear-gradient(180deg, #f8fbff 0%, #edf4ff 100%);
  border-radius:12px 12px 0 0;
  border:1px solid rgba(211,223,238,.95); border-bottom:none; margin-bottom:0;
  flex-shrink:0;
  box-shadow:none;
  span {
    text-align:center; font-size:10px; font-weight:700; color:#607089;
    padding:6px 0 7px; letter-spacing:1.5px; text-transform:uppercase;
    &.wk-end { color:#c05a31; }
  }
}

/* 月视图网格：按实际行数等分撑满剩余空间 */
.mo-grid {
  display:grid;
  grid-template-columns:repeat(7, minmax(0, 1fr));
  grid-template-rows:repeat(var(--mo-row-count, 6), minmax(56px, 1fr));
  flex:1;
  min-height:0;
  border:1px solid rgba(211,223,238,.95);
  border-radius:0 0 12px 12px;
  overflow:hidden;
  position:relative;
  background:linear-gradient(180deg, rgba(255,255,255,.99) 0%, rgba(248,250,253,.99) 100%);
  box-shadow:inset 0 1px 0 rgba(255,255,255,.8);
}

/* 月视图单元格 */
.mo-cell {
  background:linear-gradient(180deg, rgba(255,255,255,.98) 0%, rgba(249,251,254,.98) 100%);
  border-right:1px solid rgba(219,226,234,.82);
  border-bottom:1px solid rgba(219,226,234,.82);
  display:flex; flex-direction:column;
  min-height:0;
  cursor:pointer;
  transition:background .16s ease, box-shadow .16s ease, transform .16s ease;
  position:relative;
  overflow:hidden;
  transform:translateZ(0);
  isolation:isolate;

  &::after {
    content:'';
    position:absolute;
    left:0;
    top:0;
    bottom:0;
    width:3px;
    opacity:0;
    background:transparent;
    transition:opacity .15s ease, background .15s ease;
  }

  &:nth-child(7n) { border-right:none; }
  &:nth-last-child(-n+7) { border-bottom:none; }

  &:hover {
    background:linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
    z-index:1;
    box-shadow:inset 0 0 0 1px rgba($primary,.16), 0 10px 18px rgba(9,88,217,.04);
  }
  &:hover .mo-cell-add-btn { opacity:1; }
  &:hover .mo-empty-add-hint { opacity:1 !important; color:$primary; }

  &.today {
    background:linear-gradient(180deg, rgba(#ff7875,.14) 0%, rgba(#fff1f0,.86) 100%);
    box-shadow:inset 4px 0 0 #ff4d4f, 0 0 0 1px rgba(#ff4d4f,.5), 0 10px 18px rgba(255,77,79,.08);
    z-index:2;
    position:relative;
    .mo-day-num {
      display:inline-flex; align-items:center; justify-content:center;
      width:26px; height:26px; border-radius:50%;
      background:#ff4d4f; color:#fff !important;
      font-weight:900; font-size:12.5px;
      box-shadow:0 2px 10px rgba(255,77,79,.42), 0 0 0 3px rgba(255,77,79,.12);
      cursor:pointer;
      transition:transform .15s ease, box-shadow .15s ease;
      &:hover {
        transform:scale(1.12);
        box-shadow:0 3px 14px rgba(255,77,79,.55), 0 0 0 4px rgba(255,77,79,.16);
      }
    }
  }

  &.mo-cell-flash {
    box-shadow:inset 4px 0 0 #ff4d4f, 0 0 0 1.5px rgba(#ff4d4f,.8), 0 0 16px rgba(255,77,79,.4);
    animation:mo-today-flash .8s ease-in-out infinite;
  }

  &.mo-cell-selected {
    background:linear-gradient(180deg, rgba($primary,.08) 0%, rgba($primary,.03) 100%);
    box-shadow:inset 0 0 0 1px rgba($primary,.24), 0 8px 18px rgba(9,88,217,.05);
  }

  &.mo-cell-popover-active {
    background:linear-gradient(180deg, #eef5ff 0%, #e8f0ff 100%);
    transform:translateY(-1px);
    box-shadow:inset 0 0 0 2px rgba($primary,.30), 0 12px 22px rgba($primary,.10);
    z-index:10;
    isolation:isolate;
  }

  &.other {
    background:#f7f8fa; opacity:.55; filter:saturate(.82); pointer-events:none;
  }

  &.weekend {
    background:linear-gradient(180deg, rgba(255,247,237,.55) 0%, rgba(255,255,255,.96) 100%);
  }
  &.weekend .mo-day-num { color:#cf5b2d; font-weight:800; }
  &.holiday .mo-day-num { color:#b8290e; font-weight:900; }

  &.mo-has-events {
    background:linear-gradient(180deg, rgba(238,245,255,.74) 0%, rgba(255,255,255,.98) 100%);
    &:hover { background:linear-gradient(180deg, #f2f7ff 0%, #e8f1ff 100%); }
  }

  &.mo-cell-filter-match {
    background:linear-gradient(180deg, rgba(230,244,255,.96) 0%, rgba(255,255,255,.99) 100%);
    box-shadow:inset 0 0 0 1px rgba($primary,.26), 0 10px 20px rgba(9,88,217,.08);
    z-index:1;
    .mo-day-badge {
      background:linear-gradient(180deg, rgba($primary,.22) 0%, rgba($primary,.10) 100%);
      box-shadow:inset 0 0 0 1px rgba($primary,.10), 0 4px 10px rgba(9,88,217,.08);
    }
  }

  &.mo-search-match {
    box-shadow:inset 0 0 0 2px rgba(250,173,14,.46), 0 8px 18px rgba(250,173,14,.08);
    background:linear-gradient(180deg, rgba(255,249,230,.96) 0%, rgba(255,255,255,.98) 100%);
    &:hover {
      background:#fff5db;
      box-shadow:inset 0 0 0 2px rgba(250,173,14,.72), 0 10px 20px rgba(250,173,14,.12);
    }
  }

  &.mo-tone-todo::after { background:$warning; opacity:1; }
  &.mo-tone-doing::after { background:#2563eb; opacity:1; }
  &.mo-tone-done::after { background:$success; opacity:1; }
  &.mo-tone-cancelled::after { background:#64748b; opacity:1; }
  &.mo-tone-mixed::after {
    opacity:1;
    background:linear-gradient(180deg, #2563eb 0%, #d97706 50%, #2f9e44 100%);
  }

  &.mo-cell-dimmed {
    opacity:.52;
    filter:saturate(.72);
    background:linear-gradient(180deg, rgba(245,247,250,.96) 0%, rgba(255,255,255,.94) 100%);
    box-shadow:inset 0 0 0 1px rgba(148,163,184,.16);
    .mo-cell-summary,
    .mo-event-list,
    .mo-cell-progress { display:none; }
    .mo-day-badge,
    .mo-day-status-pill,
    .mo-cell-add-btn,
    .mo-empty-add-hint { opacity:0 !important; pointer-events:none; }
    .mo-day-num,
    .mo-holiday-tag { opacity:.72; }
    &:hover {
      opacity:.7;
      box-shadow:inset 0 0 0 1px rgba(148,163,184,.24), 0 6px 12px rgba(15,23,42,.04);
    }
  }
}

.mo-cell-hd {
  display:flex; align-items:center; padding:2px 5px 0; flex-shrink:0; gap:4px; min-height:18px;
}
.mo-day-num {
  font-size:11.5px; font-weight:800; color:$ink; line-height:1;
  cursor:pointer; border-radius:50%; padding:1px 2px;
  transition:color .15s ease, background .15s ease;
  &:hover { color:$primary; background:rgba($primary,.08); }
}
.mo-holiday-tag {
  font-size:9.5px; color:#c4380d; background:#fff0e6; border-radius:4px;
  padding:1px 4px; line-height:14px; font-weight:800;
  white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:32px;
  border:1px solid rgba(196,56,13,.25);
}
.mo-cell-hd-right {
  margin-left:auto; display:flex; align-items:center; gap:5px;
}
.mo-day-status-pill,
.mo-cell-summary-main,
.mo-cell-summary-hit {
  display:inline-flex; align-items:center; justify-content:center;
  border-radius:999px; font-size:9px; font-weight:800; line-height:1; min-height:16px;
}
.mo-day-status-pill {
  padding:4px 7px; border:1px solid transparent; white-space:nowrap;
}
.mo-cell-summary {
  display:flex; align-items:center; justify-content:space-between;
  gap:4px; padding:0 6px 1px; min-height:14px;
}
.mo-cell-summary-main {
  padding:4px 8px; border:1px solid transparent; min-width:0;
}
.mo-cell-summary-hit {
  display:inline-flex; align-items:center; font-size:8.5px; font-weight:700; line-height:1;
  padding:1px 5px; color:#9a6700; background:#fff8df; border-radius:3px; border:1px solid rgba(250,173,20,.24); flex-shrink:0;
}
.mo-day-status-pill.is-todo,
.mo-cell-summary-main.is-todo { color:$warning; background:#fff4db; border-color:rgba($warning,.18); }
.mo-day-status-pill.is-doing,
.mo-cell-summary-main.is-doing { color:#2563eb; background:#eaf2ff; border-color:rgba(37,99,235,.16); }
.mo-day-status-pill.is-done,
.mo-cell-summary-main.is-done { color:$success; background:#e8f7ed; border-color:rgba($success,.18); }
.mo-day-status-pill.is-cancelled,
.mo-cell-summary-main.is-cancelled { color:#64748b; background:#eef2f6; border-color:rgba(100,116,139,.18); }
.mo-day-status-pill.is-mixed,
.mo-cell-summary-main.is-mixed { color:#6d28d9; background:#f1eafe; border-color:rgba(109,40,217,.18); }

.mo-day-badge {
  font-size:9px; font-weight:700;
  background:linear-gradient(180deg, rgba($primary,.16) 0%, rgba($primary,.08) 100%); color:$primary;
  min-width:16px; height:16px; border-radius:999px; padding:0 5px;
  display:inline-flex; align-items:center; justify-content:center;
  flex-shrink:0; line-height:1;
  box-shadow:inset 0 0 0 1px rgba($primary,.08);
}

/* 快速新建按钮（悬停显示） */
.mo-cell-add-btn {
  width:20px; height:20px;
  border:1px solid rgba($primary,.24); background:linear-gradient(180deg, rgba($primary,.08) 0%, rgba($primary,.04) 100%);
  border-radius:6px; cursor:pointer; color:$primary;
  display:inline-flex; align-items:center; justify-content:center;
  opacity:0; transition:all .15s; flex-shrink:0;
  &:hover { background:$primary; border-color:$primary; color:#fff; box-shadow:0 4px 10px rgba($primary,.28); }
}

/* 日程列表：每格展示 5 条 */
.mo-event-list {
  display:grid;
  grid-template-rows:repeat(5, minmax(0, 1fr));
  gap:2px;
  padding:0 5px 2px;
  flex:1;
  min-height:0;
}
.mo-event-item,
.mo-more-tip,
.mo-event-placeholder {
  min-height:0;
  box-sizing:border-box;
}
.mo-event-item {
  display:flex; align-items:center; gap:4px;
  padding:0 4px 0 7px; border-radius:6px;
  border:1px solid transparent; transition:background .15s ease, border-color .15s ease, transform .15s ease, box-shadow .15s ease;
  cursor:pointer; position:relative; background:rgba(255,255,255,.58); min-width:0;
  min-height:19px;
  backdrop-filter:blur(2px);

  &::before {
    content:''; position:absolute; left:0; top:50%; transform:translateY(-50%);
    width:3px; height:70%; border-radius:999px; background:var(--cat-color, #0958d9); opacity:1;
    box-shadow:0 0 0 1px rgba(255,255,255,.35);
  }

  &:hover {
    background:#f3f8ff;
    border-color:rgba($primary,.14);
    transform:translateX(1px);
    box-shadow:0 6px 12px rgba(9,88,217,.06);
  }

  &.mo-ev-hit {
    border-color:rgba(250,173,20,.32);
    box-shadow:0 0 0 1px rgba(250,173,20,.14) inset;
  }

  &.mo-todo {
    &::before { background:$warning; }
  }

  &.mo-done {
    opacity:.64;
    background:rgba(47,158,68,.08);
    .mo-ev-title { text-decoration:line-through; color:$faint; }
    &::before { background:$success; }
  }

  &.mo-doing {
    background:linear-gradient(180deg, rgba($primary,.10) 0%, rgba($primary,.06) 100%);
    border-color:rgba($primary,.16);
    &::before {
      background:$primary;
      box-shadow:0 0 6px rgba($primary,.32);
    }
  }

  &.mo-cancelled {
    opacity:.54;
    background:rgba(148,163,184,.08);
    .mo-ev-title { text-decoration:line-through; color:#8c8c8c; font-weight:400; }
    &::before { background:#c9cdd4; }
  }
}
.mo-event-placeholder {
  border-radius:5px;
  background:transparent;
  border:1px solid transparent;
}
.mo-ev-dot {
  width:5px; height:5px; border-radius:50%; flex-shrink:0;
}
.mo-ev-time {
  width:32px; text-align:left;
  font-size:9.5px; font-weight:700; color:#748095; letter-spacing:-.1px; flex-shrink:0;
  font-variant-numeric:tabular-nums;
}
.mo-ev-title {
  font-size:12px; font-weight:700; color:$ink; line-height:1.2;
  white-space:nowrap; overflow:hidden; text-overflow:ellipsis; min-width:0; flex:1;
}

/* 进行中脉冲点 */
.mo-ev-doing-dot {
  width:5px; height:5px; border-radius:50%; background:$primary; flex-shrink:0;
  animation:pulse-dot 1.6s ease-out infinite;
}

/* 已取消图标 */
.mo-ev-cancel-icon {
  font-size:13px; font-weight:700; color:#8c8c8c;
  flex-shrink:0; line-height:1; margin-left:2px;
}

/* 超出数量提示 */
.mo-more-tip {
  width:100%; border:1px dashed rgba($primary,.16); outline:none;
  display:inline-flex; align-items:center; justify-content:flex-start; gap:3px;
  font-size:10px; font-weight:700; color:$primary;
  padding:0 4px 0 6px; border-radius:6px; cursor:pointer;
  background:rgba($primary,.05);
  transition:all .15s ease; margin:0; line-height:1;
  white-space:nowrap; flex-shrink:0;
  min-height:19px;
  &:hover { background:rgba($primary,.10); color:$primary; border-color:rgba($primary,.24); }
  svg { flex-shrink:0; transition:transform .2s ease; }
  &.mo-more-active {
    background:rgba($primary,.12); color:$primary; border-color:rgba($primary,.28);
    svg { transform:rotate(180deg); }
  }
}

/* 今日完成率进度条：固定在单元格底部，不占用列表行高 */
.mo-cell-progress {
  position:absolute;
  left:6px;
  right:6px;
  bottom:3px;
  height:2px; background:rgba($primary,.12); overflow:hidden;
  border-radius:999px; pointer-events:none;
}
.mo-cell-progress-fill {
  height:100%; background:linear-gradient(to right, $primary 0%, $success 100%);
  transition:width .5s cubic-bezier(.4,0,.2,1); min-width:5px;
  box-shadow:0 0 4px rgba($primary,.4);
}

/* 空白格提示：常态显示浅灰提示，悬停变蓝 */
.mo-empty-cell {
  flex:1; display:flex; align-items:center; justify-content:center;
  padding-bottom:4px;
}
.mo-empty-add-hint {
  display:inline-flex; align-items:center; gap:3px;
  font-size:10.5px; color:#c2c8d2; opacity:0.6; transition:all .18s;
  background:none; border:1px dashed transparent; cursor:pointer;
  padding:3px 8px; border-radius:5px;
  &:hover {
    opacity:1 !important; background:rgba($primary,.07);
    color:$primary; border-color:rgba($primary,.3);
  }
}

/* 底部统计栏：更紧凑，让月网格居中 */
.mo-footer {
  display:flex; align-items:center; justify-content:space-between;
  padding:6px 6px 0; margin-top:4px;
  border-top:1px dashed rgba(211,223,238,.92); flex-shrink:0; flex-wrap:wrap; gap:6px 12px;
}
.mo-footer-stats { display:flex; align-items:center; gap:10px 12px; flex-wrap:wrap; }
.mo-stat {
  font-size:10.5px; color:#5d687b; font-weight:600;
  display:inline-flex; align-items:center; gap:4px;
  strong { font-weight:800; }
}
.mo-stat::before {
  content:''; display:inline-block; width:8px; height:8px; border-radius:50%; flex-shrink:0;
}
.mo-stat-todo::before { background:$warning; }
.mo-stat-todo strong { color:$warning; }
.mo-stat-doing::before { background:$primary; }
.mo-stat-doing strong { color:$primary; }
.mo-stat-done::before { background:$success; }
.mo-stat-done strong { color:$success; }
.mo-stat-cancelled::before { background:#86909c; }
.mo-stat-cancelled strong { color:#86909c; }

/* 共计数不加圆点 */
.mo-footer-stats > .mo-stat:first-child::before { display:none; }
.mo-footer-progress {
  display:flex; align-items:center; gap:8px; flex-shrink:0;
  padding:4px 8px; border-radius:999px; background:rgba($primary,.045); border:1px solid rgba($primary,.10);
}
.mo-footer-pct-label { font-size:10.5px; color:#607089; font-weight:700; white-space:nowrap; }
.mo-footer-track {
  width:118px; height:6px;
  background:rgba($primary,.10); border-radius:999px; overflow:hidden;
}
.mo-footer-fill {
  height:100%; background:linear-gradient(90deg, #2b6ef2 0%, #2f9e44 100%);
  border-radius:999px; transition:width .6s cubic-bezier(.4,0,.2,1); min-width:4px;
  box-shadow:0 0 4px rgba($primary,.24);
}
.mo-footer-pct { font-size:12px; font-weight:800; color:$primary; white-space:nowrap; }

/* ============================================================
  Day Popover
  ============================================================ */
.mo-day-popover {
  --mdp-anchor-left: 50%;
  --mdp-origin-x: 178px;
  --mdp-enter-offset: -10px;
  position:fixed; z-index:9999;
  background:$surface; border:1px solid $border; border-radius:$rs-lg;
  box-shadow:$shadow-lg; overflow:visible;
  display:flex; flex-direction:column;
  backdrop-filter:blur(10px);
  transform-origin:var(--mdp-origin-x) top;

  &::before,
  &::after {
    content:'';
    position:absolute;
    left:var(--mdp-anchor-left);
    transform:translateX(-50%);
    pointer-events:none;
  }

  &::before {
    width:12px;
    height:12px;
    background:$surface;
    border:1px solid $border;
    rotate:45deg;
    box-shadow:-2px -2px 10px rgba(15, 23, 42, .05);
  }

  &::after {
    width:2px;
    height:12px;
    border-radius:999px;
    background:linear-gradient(180deg, rgba($primary,.24) 0%, rgba($primary,0) 100%);
    opacity:.9;
  }

  &.is-bottom {
    transform-origin:var(--mdp-origin-x) top;
    &::before { top:-7px; }
    &::after { top:-18px; }
  }

  &.is-top {
    transform-origin:var(--mdp-origin-x) bottom;
    &::before { bottom:-7px; }
    &::after {
      bottom:-18px;
      background:linear-gradient(180deg, rgba($primary,0) 0%, rgba($primary,.24) 100%);
    }
  }
}
.popover-fade-enter-active, .popover-fade-leave-active { transition:opacity .18s, transform .18s; }
.popover-fade-enter-from, .popover-fade-leave-to { opacity:0; transform:translate3d(0, var(--mdp-enter-offset), 0) scale(.97); }


.mdp-header {
  display:flex; align-items:center; justify-content:space-between;
  gap:12px; padding:14px 14px 12px 16px;
  border-bottom:1px solid $border; flex-shrink:0;
  background:linear-gradient(180deg, #fbfcff 0%, #f6f9ff 100%);
}
.mdp-title-block {
  display:flex; flex-direction:column; gap:6px; min-width:0;
}
.mdp-title {
  display:flex; align-items:center; gap:8px;
}
.mdp-subtitle {
  display:flex; align-items:center; gap:10px; flex-wrap:wrap;
  font-size:11px; color:$sub; font-weight:600;
  span { position:relative; }
  span + span::before {
    content:''; display:inline-block; width:4px; height:4px; border-radius:50%;
    background:#cbd5e1; margin-right:10px; vertical-align:middle;
  }
}
.mdp-date-text {
  font-size:13.5px; font-weight:700; color:$ink;
}
.mdp-close {
  min-width:68px; height:34px; padding:0 12px;
  border:1px solid rgba($primary,.10); background:rgba($primary,.04);
  border-radius:999px; cursor:pointer; display:inline-flex; align-items:center; justify-content:center;
  gap:6px; color:$ink2; transition:all .15s; flex-shrink:0; font-size:11px; font-weight:700;
  &:hover {
    background:rgba($primary,.10);
    border-color:rgba($primary,.22);
    color:$primary;
    box-shadow:0 4px 12px rgba($primary,.10);
  }
  &:active {
    transform:scale(.98);
  }
  svg { flex-shrink:0; }
}

.mdp-summary {
  display:flex; align-items:center; gap:8px; flex-wrap:wrap;
  padding:10px 16px; border-bottom:1px solid rgba($border,.9);
  background:#fbfcfe;
}
.mdp-summary-chip {
  display:inline-flex; align-items:center; justify-content:center;
  padding:4px 9px; border-radius:999px; font-size:11px; font-weight:700;
  border:1px solid transparent; line-height:1;
}
.mdp-summary-total { color:$primary; background:$primary-light; border-color:rgba($primary,.14); }
.mdp-summary-chip.is-todo { color:$warning; background:#fff4db; border-color:rgba($warning,.18); }
.mdp-summary-chip.is-doing { color:#2563eb; background:#eaf2ff; border-color:rgba(37,99,235,.16); }
.mdp-summary-chip.is-done { color:$success; background:#e8f7ed; border-color:rgba($success,.16); }
.mdp-summary-chip.is-cancelled { color:#64748b; background:#eef2f6; border-color:rgba(100,116,139,.16); }
.mdp-summary-chip.is-search { color:#9a6700; background:#fff8df; border-color:rgba(250,173,20,.24); }

.mdp-body {
  flex:1;
  overflow-y:auto;
  overflow-x:hidden;
  max-height:276px;
  padding:10px 0;
  scrollbar-gutter:stable both-edges;
  scrollbar-width:thin;
  scrollbar-color:#c4ccda transparent;
  overscroll-behavior:contain;
  &::-webkit-scrollbar { width:8px; height:8px; }
  &::-webkit-scrollbar-track { background:transparent; }
  &::-webkit-scrollbar-thumb {
    background:rgba(148, 163, 184, .58);
    border-radius:999px;
    border:2px solid transparent;
    background-clip:padding-box;
  }
  &:hover::-webkit-scrollbar-thumb {
    background:rgba(100, 116, 139, .62);
    border:2px solid transparent;
    background-clip:padding-box;
  }
}
.mdp-empty {
  display:flex; flex-direction:column; align-items:center; gap:6px;
  padding:26px 20px;
  p { font-size:12.5px; color:$faint; margin:0; }
}
.mdp-section {
  display:flex; flex-direction:column; gap:6px;
  & + .mdp-section { margin-top:10px; }
}
.mdp-section-head {
  display:flex; align-items:center; justify-content:space-between;
  gap:8px; padding:0 16px;
}
.mdp-section-title {
  display:inline-flex; align-items:center; gap:6px;
  font-size:11.5px; font-weight:800; color:$ink2;
  &::before { content:''; width:7px; height:7px; border-radius:50%; background:currentColor; }
  &.is-todo { color:$warning; }
  &.is-doing { color:#2563eb; }
  &.is-done { color:$success; }
  &.is-cancelled { color:#64748b; }
}
.mdp-section-count {
  font-size:10.5px; font-weight:800; color:$sub;
  background:#f3f6fa; border-radius:999px; padding:3px 7px;
}

.mdp-ev-item {
  display:flex; align-items:flex-start; gap:10px;
  padding:10px 16px; cursor:pointer; transition:background .12s, box-shadow .12s, border-color .12s;
  border-left:2px solid transparent;
  &:hover {
    background:#f5f8fc;
    box-shadow:inset 0 0 0 1px rgba($primary,.08);
  }
  &.mdp-ev-done {
    opacity:.62;
    .mdp-ev-title { text-decoration:line-through; color:$faint; }
  }
  &.mdp-ev-doing {
    background:rgba($primary,.035);
    border-left-color:$primary;
  }
  &.mdp-ev-cancelled {
    opacity:.5;
    .mdp-ev-remark { color:$faint; }
  }
}
.mdp-ev-color {
  width:4px; height:38px; border-radius:3px; flex-shrink:0; margin-top:2px;
}
.mdp-ev-main {
  flex:1; min-width:0; display:flex; flex-direction:column; gap:5px;
}
.mdp-ev-top {
  display:flex; align-items:flex-start; justify-content:space-between; gap:8px;
}
.mdp-ev-title {
  font-size:13px; font-weight:700; color:$ink;
  overflow:hidden; text-overflow:ellipsis; display:-webkit-box; -webkit-line-clamp:1; -webkit-box-orient:vertical;
}
.mdp-ev-time-range {
  flex-shrink:0; font-size:10.5px; color:$ink2; font-weight:700; font-variant-numeric:tabular-nums;
  background:#f3f6fa; border-radius:999px; padding:3px 7px;
}
.mdp-ev-meta {
  display:flex; align-items:center; gap:6px; flex-wrap:wrap;
}
.mdp-ev-actions {
  display:flex; align-items:center; gap:6px; flex-wrap:wrap;
}
.mdp-ev-action-chip {
  min-width:44px; height:24px; padding:0 8px;
  border-radius:999px; border:1px solid #dbe3ef;
  background:#fff; color:$ink2; cursor:pointer;
  display:inline-flex; align-items:center; justify-content:center;
  font-size:10.5px; font-weight:700; line-height:1;
  transition:all .15s;

  &:hover:not(:disabled) {
    border-color:rgba($primary,.3);
    color:$primary;
    background:$primary-light;
  }

  &:disabled {
    cursor:wait;
    opacity:.64;
  }

  &.is-active {
    color:#fff;
    box-shadow:0 4px 10px rgba(15, 23, 42, .08);
  }
  &.is-todo.is-active { background:$warning; border-color:$warning; }
  &.is-doing.is-active { background:$primary; border-color:$primary; }
  &.is-done.is-active { background:$success; border-color:$success; }
  &.is-cancelled.is-active { background:#64748b; border-color:#64748b; }

  &.is-delete {
    border-color:#f5c2c7; color:$danger;
    &:hover:not(:disabled) {
      border-color:$danger; color:$danger; background:#fff1f0;
    }
  }

  &.is-pending:not(.is-active) {
    background:#f5f7fa;
  }
}
.mdp-ev-cat,
.mdp-priority-chip {
  display:inline-flex; align-items:center; justify-content:center;
  min-height:20px; padding:0 8px; border-radius:999px; font-size:10.5px; font-weight:700;
}
.mdp-priority-chip.is-urgent { color:$danger; background:#fff1f0; }
.mdp-priority-chip.is-important { color:$warning; background:#fff4db; }
.mdp-ev-remark {
  font-size:11px; color:$sub; line-height:1.5;
  display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden;
}
.mdp-ev-status-icon {
  width:22px; height:22px; border-radius:50%; display:flex; align-items:center; justify-content:center; flex-shrink:0;
  margin-top:2px; transition:transform .15s ease, opacity .15s ease;
  &.mdp-s-0 { color:$faint; background:#f5f5f5; }
  &.mdp-s-1 { color:$primary; background:$primary-light; }
  &.mdp-s-2 { color:$success; background:#f6ffed; }
  &.mdp-s-3 { color:#64748b; background:#eef2f6; }
  &.is-loading {
    opacity:.66;
    animation:mdp-status-pulse .9s ease-in-out infinite;
  }
}
.mdp-cancel-mark {
  font-size:13px; font-weight:800; line-height:1;
}
.doing-pulse-sm {
  width:6px; height:6px; border-radius:50%; background:$primary; display:inline-block;
  animation:pulse-dot 1.6s ease-out infinite;
}

.mdp-footer {
  display:flex; align-items:center; gap:8px;
  padding:12px; border-top:1px solid $border; flex-shrink:0;
  background:#fafbfc;
}
.mdp-btn-new {
  flex:1; display:inline-flex; align-items:center; justify-content:center; gap:5px;
  font-size:12px; font-weight:600; padding:8px 0;
  background:$primary; color:#fff; border:none; border-radius:$rs; cursor:pointer;
  transition:all .15s;
  &:hover { background: darken($primary, 8%); }
}
.mdp-btn-detail {
  flex:1; display:inline-flex; align-items:center; justify-content:center; gap:4px;
  font-size:12px; font-weight:600; padding:8px 0;
  background:$surface; color:$ink2; border:1px solid $border; border-radius:$rs; cursor:pointer;
  transition:all .15s;
  &:hover { border-color:$primary; color:$primary; background:$primary-light; }
}

/* 月视图响应式 */
@media(max-width:900px){
  .month-overview { padding:6px 12px 10px; gap:6px; }
  .mo-calendar-stage { padding:0 2px 24px; }
  .mo-calendar-panel { width:100%; height:min(100%, clamp(510px, calc(100vh - 216px), 700px)); padding:8px 8px 6px; }
  .mo-grid { grid-template-rows:repeat(var(--mo-row-count, 6), minmax(68px, 1fr)); }
  .mo-cell { min-height:68px; }
  .mo-ev-title { font-size:12px; }
  .mo-footer { gap:6px 8px; padding:5px 0 0; }
  .mo-legend { display:none; }
  .mo-week-header span { font-size:10px; letter-spacing:1px; }
  .mo-footer-track { width:80px; }
  .mo-footer-stats { gap:10px; }
}
@media(max-width:600px){
  .month-overview { padding:4px 6px 8px; gap:5px; }
  .mo-calendar-stage { align-items:stretch; padding:0 0 10px; }
  .mo-calendar-panel { width:100%; height:100%; padding:6px 6px 4px; border-radius:14px; }
  .mo-grid { grid-template-rows:repeat(var(--mo-row-count, 6), minmax(52px, 1fr)); }
  .mo-cell { min-height:52px; }
  .mo-cell-hd { padding:5px 5px 2px; }
  .mo-day-num { font-size:12px; }
  .mo-ev-title { font-size:12px; }
  .mo-footer { flex-direction:column; align-items:flex-start; gap:6px; padding:6px 0 0; }
  .mo-footer-progress { width:100%; }
  .mo-footer-track { flex:1; }
}
@keyframes mo-today-flash {
  0%, 100% { box-shadow:inset 4px 0 0 #ff4d4f, 0 0 0 2px #ff4d4f, 0 0 14px rgba(255,77,79,.45); }
  50% { box-shadow:inset 4px 0 0 #ff4d4f, 0 0 0 2.5px #ff4d4f, 0 0 24px rgba(255,77,79,.7), 0 0 48px rgba(255,77,79,.25); }
}
</style>
