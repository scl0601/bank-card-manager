<template>
  <div class="calendar-page" tabindex="0" @keydown="handleKeydown" ref="pageRef">
    <div class="page-header">
      <div class="header-left">
        <span class="page-title-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
        </span>
        <div class="header-title-group">
          <h1 class="page-title">日历计划</h1>
          <div class="header-meta">
            <span class="meta-year">{{ currentYear }} 年</span>
            <span class="meta-divider">·</span>
            <span class="header-badge"><span class="badge-dot"></span>{{ stats.todoCount + stats.doingCount + stats.doneCount }} 个日程</span>
          </div>
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
          <button class="icon-btn" title="导出/打印">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="export-day">导出当天 CSV</el-dropdown-item>
              <el-dropdown-item command="export-month">导出本月 CSV</el-dropdown-item>
              <el-dropdown-item command="print" divided>打印当天日程</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button type="primary" @click="openDrawer(null)">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="margin-right:5px"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新建日程
        </el-button>
      </div>
    </div>

    <div class="main-body">
      <div class="left-panel">
        <div class="stats-row">
          <div class="stat-card stat-todo" @click="setQuickFilter('todo')" :class="{ 'stat-active': quickFilter === 'todo' }">
            <div class="stat-card-top">
              <div class="stat-icon-wrap"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg></div>
              <div class="stat-ring"><svg width="38" height="38" viewBox="0 0 38 38"><circle cx="19" cy="19" r="15" fill="none" stroke="#f5e6c8" stroke-width="3"/><circle cx="19" cy="19" r="15" fill="none" stroke="#fa8c16" stroke-width="3" stroke-dasharray="94.2" :stroke-dashoffset="94.2*(1-statPercent('todo')/100)" stroke-linecap="round" transform="rotate(-90 19 19)" style="transition:stroke-dashoffset .6s"/><text x="19" y="23" text-anchor="middle" font-size="9" font-weight="700" fill="#fa8c16">{{ statPercent('todo') }}%</text></svg></div>
            </div>
            <div class="stat-info"><em>{{ stats.todoCount }}</em><i>待办事项</i></div>
            <div class="stat-bar"><div class="stat-bar-inner" :style="{ width: statPercent('todo') + '%' }"></div></div>
          </div>
          <div class="stat-card stat-doing" @click="setQuickFilter('doing')" :class="{ 'stat-active': quickFilter === 'doing' }">
            <div class="stat-card-top">
              <div class="stat-icon-wrap"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg></div>
              <div class="stat-ring"><svg width="38" height="38" viewBox="0 0 38 38"><circle cx="19" cy="19" r="15" fill="none" stroke="#c8dcf5" stroke-width="3"/><circle cx="19" cy="19" r="15" fill="none" stroke="#1677ff" stroke-width="3" stroke-dasharray="94.2" :stroke-dashoffset="94.2*(1-statPercent('doing')/100)" stroke-linecap="round" transform="rotate(-90 19 19)" style="transition:stroke-dashoffset .6s"/><text x="19" y="23" text-anchor="middle" font-size="9" font-weight="700" fill="#1677ff">{{ statPercent('doing') }}%</text></svg></div>
            </div>
            <div class="stat-info"><em>{{ stats.doingCount }}</em><i>进行中</i></div>
            <div class="stat-bar"><div class="stat-bar-inner" :style="{ width: statPercent('doing') + '%' }"></div></div>
          </div>
          <div class="stat-card stat-done" @click="setQuickFilter('done')" :class="{ 'stat-active': quickFilter === 'done' }">
            <div class="stat-card-top">
              <div class="stat-icon-wrap"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg></div>
              <div class="stat-ring"><svg width="38" height="38" viewBox="0 0 38 38"><circle cx="19" cy="19" r="15" fill="none" stroke="#c8f0d0" stroke-width="3"/><circle cx="19" cy="19" r="15" fill="none" stroke="#52c41a" stroke-width="3" stroke-dasharray="94.2" :stroke-dashoffset="94.2*(1-statPercent('done')/100)" stroke-linecap="round" transform="rotate(-90 19 19)" style="transition:stroke-dashoffset .6s"/><text x="19" y="23" text-anchor="middle" font-size="9" font-weight="700" fill="#52c41a">{{ statPercent('done') }}%</text></svg></div>
            </div>
            <div class="stat-info"><em>{{ stats.doneCount }}</em><i>已完成</i></div>
            <div class="stat-bar"><div class="stat-bar-inner" :style="{ width: statPercent('done') + '%' }"></div></div>
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
            <div class="cal-day-grid">
              <div v-for="(cell,ci) in allCells" :key="cell.key||ci"
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
            <el-select v-model="filterCategory" placeholder="分类筛选" clearable size="small" style="width:108px" @change="onFilterChange">
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
            <div class="quick-filters">
              <button :class="['qf-btn',{active:quickFilter==='all'}]" @click="setQuickFilter('all')">全部</button>
              <button :class="['qf-btn',{active:quickFilter==='todo'}]" @click="setQuickFilter('todo')">待办</button>
              <button :class="['qf-btn',{active:quickFilter==='doing'}]" @click="setQuickFilter('doing')">进行中</button>
              <button :class="['qf-btn',{active:quickFilter==='done'}]" @click="setQuickFilter('done')">已完成</button>
            </div>
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
            <!-- 待办/进行中 -->
            <transition-group v-if="filteredPendingEvents.length" name="ev-list" tag="div" class="ev-group">
              <div v-for="(ev,idx) in filteredPendingEvents" :key="ev.id"
                :class="['ev-card',`pri-${ev.priority}`,{'is-doing':ev.status===EVENT_STATUS_VALUE.DOING,'ev-selected':selectedIds.has(ev.id),'batch-mode':batchMode}]"
                @click="batchMode?toggleSelect(ev.id):openDrawer(ev)">
                <div class="ev-left">
                  <template v-if="batchMode">
                    <span class="ev-batch-check">
                      <span :class="['batch-cb',{checked:selectedIds.has(ev.id)}]">
                        <svg v-if="selectedIds.has(ev.id)" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                      </span>
                    </span>
                  </template>
                  <template v-else>
                    <span class="ev-time">{{ formatTime(ev.startTime) }}</span>
                    <label class="ev-check" @click.stop title="标记完成">
                      <input type="checkbox" :checked="false" @change="onToggleDone(ev)" />
                      <span class="check-box"></span>
                    </label>
                  </template>
                </div>
                <div class="ev-main">
                  <div class="ev-title-row">
                    <i class="ev-cat-bar" :style="{background:categoryColor[ev.category]}"></i>
                    <span v-if="inlineEditId!==ev.id||batchMode" class="ev-tit" @dblclick.stop="!batchMode&&startInlineEdit(ev)">{{ ev.title }}</span>
                    <input v-else class="ev-tit-input" v-model="inlineEditTitle" @blur="submitInlineEdit(ev)" @keydown.enter.stop="submitInlineEdit(ev)" @keydown.esc.stop="cancelInlineEdit" @click.stop />
                    <el-tag v-if="ev.priority===2" size="small" type="danger" effect="dark" round>紧急</el-tag>
                    <el-tag v-else-if="ev.priority===1" size="small" type="warning" effect="plain" round>重要</el-tag>
                    <span v-if="ev.status===EVENT_STATUS_VALUE.DOING" class="doing-pulse"></span>
                  </div>
                  <div class="ev-meta-row">
                    <span class="ev-cat-tag" :style="{background:categoryColorLight[ev.category],color:categoryColor[ev.category]}">{{ EVENT_CATEGORY_MAP[ev.category] }}</span>
                    <span v-if="ev.endTime" class="ev-duration"><svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>{{ formatTime(ev.startTime) }}–{{ formatTime(ev.endTime) }}</span>
                    <span v-if="ev.remark" class="ev-remark">{{ ev.remark }}</span>
                  </div>
                </div>
                <div v-if="!batchMode" class="ev-right">
                  <!-- [C] 排序按钮 -->
                  <div class="sort-btns">
                    <button class="sort-btn" :disabled="idx===0" @click.stop="moveEventUp(idx)" title="上移">
                      <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="18 15 12 9 6 15"/></svg>
                    </button>
                    <button class="sort-btn" :disabled="idx===filteredPendingEvents.length-1" @click.stop="moveEventDown(idx)" title="下移">
                      <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
                    </button>
                  </div>
                  <el-dropdown trigger="click" @command="(cmd:string)=>handleEvAction(cmd,ev)">
                    <button class="ev-action-btn more-btn" @click.stop>
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="5" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="12" cy="19" r="1"/></svg>
                    </button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="edit">编辑</el-dropdown-item>
                        <el-dropdown-item command="doing" v-if="ev.status===EVENT_STATUS_VALUE.TODO">开始进行</el-dropdown-item>
                        <el-dropdown-item command="toggle" divided>{{ ev.status===EVENT_STATUS_VALUE.DONE?'标记未完成':'标记完成' }}</el-dropdown-item>
                        <el-dropdown-item command="delete" divided style="color:#f53f3f">删除</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
            </transition-group>

            <!-- 已完成折叠 -->
            <template v-if="filteredDoneEvents.length">
              <div class="section-divider section-divider-toggle" @click="doneCollapsed=!doneCollapsed">
                <span>
                  <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" :style="{display:'inline-block',verticalAlign:'middle',marginRight:'3px',transform:doneCollapsed?'rotate(-90deg)':'rotate(0deg)',transition:'transform .2s'}"><polyline points="6 9 12 15 18 9"/></svg>
                  已完成 · {{ filteredDoneEvents.length }}
                </span>
              </div>
              <transition name="done-fold">
                <div v-show="!doneCollapsed" class="ev-group ev-group-done">
                  <div v-for="ev in filteredDoneEvents" :key="ev.id"
                    :class="['ev-card','ev-card-done',{'ev-selected':selectedIds.has(ev.id),'batch-mode':batchMode}]"
                    @click="batchMode?toggleSelect(ev.id):openDrawer(ev)">
                    <div class="ev-left">
                      <template v-if="batchMode">
                        <span class="ev-batch-check"><span :class="['batch-cb',{checked:selectedIds.has(ev.id)}]"><svg v-if="selectedIds.has(ev.id)" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg></span></span>
                      </template>
                      <template v-else>
                        <span class="ev-time muted">{{ formatTime(ev.startTime) }}</span>
                        <label class="ev-check checked" @click.stop><input type="checkbox" checked @change="onUndo(ev)" /><span class="check-box"></span></label>
                      </template>
                    </div>
                    <div class="ev-main">
                      <div class="ev-title-row">
                        <i class="ev-cat-bar faded" :style="{background:categoryColor[ev.category]}"></i>
                        <span class="ev-tit strike">{{ ev.title }}</span>
                      </div>
                      <div class="ev-meta-row"><span class="ev-cat-tag done-tag">{{ EVENT_CATEGORY_MAP[ev.category] }}</span></div>
                    </div>
                    <div v-if="!batchMode" class="ev-right">
                      <el-popconfirm title="确定删除该日程？" confirm-button-text="确定" cancel-button-text="取消" @confirm="onDelete(ev.id)">
                        <template #reference><button class="ev-action-btn ev-del" @click.stop><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg></button></template>
                      </el-popconfirm>
                    </div>
                  </div>
                </div>
              </transition>
            </template>

            <div v-if="events.length>0&&!filteredPendingEvents.length&&!filteredDoneEvents.length" class="rp-empty rp-filter-empty">
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
          <div class="foot-progress-wrap" v-if="totalEvents>0">
            <div class="foot-progress-labels">
              <span class="foot-progress-title">本月完成率</span>
              <span class="foot-progress-pct">{{ statPercent('done') }}%</span>
            </div>
            <div class="foot-progress-track"><div class="foot-progress-fill" :style="{width:statPercent('done')+'%'}"></div></div>
            <div class="foot-progress-counts">
              <span><em class="clr-warning">{{ stats.todoCount }}</em> 待办</span>
              <span><em class="clr-primary">{{ stats.doingCount }}</em> 进行中</span>
              <span><em class="clr-success">{{ stats.doneCount }}</em> 已完成</span>
            </div>
          </div>
          <span v-else class="foot-hint">按 <kbd>N</kbd> 快捷新建 · 双击日期快速创建</span>
        </div>
      </div>
    </div>

    <EventDrawer v-model:visible="drawerVisible" :event-id="editingId" :default-date="defaultNewDate" @saved="onSaved" />

    <!-- [E] 打印预览弹窗 -->
    <el-dialog v-model="printPreviewVisible" title="打印预览" width="600px" class="print-dialog">
      <div class="print-preview" id="print-area">
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
      </div>
      <template #footer>
        <el-button @click="printPreviewVisible=false">关闭</el-button>
        <el-button type="primary" @click="doPrint">打印</el-button>
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
const filterCategory = ref<number | undefined>(undefined)
const filterStatus = ref<number | undefined>(undefined)
const keyword = ref('')
const searchFocused = ref(false)
const monthLoading = ref(false)
const listLoading = ref(false)
const monthChanging = ref(false)
const loadError = ref(false)
const quickFilter = ref<'all'|'todo'|'doing'|'done'>('all')
const editingId = ref<number | null>(null)
const drawerVisible = ref(false)
const defaultNewDate = ref<string>('')
const pageRef = ref<HTMLElement | null>(null)
const weekDays = ['一','二','三','四','五','六','日']
const monthData = ref<any[]>([])
const events = ref<any[]>([])
const stats = ref({ todoCount: 0, doingCount: 0, doneCount: 0 })

const doneCollapsed = ref(true)
const showMonthPicker = ref(false)
const mpYear = ref(today.getFullYear())
const monthPickerRef = ref<HTMLElement | null>(null)
const inlineEditId = ref<number | null>(null)
const inlineEditTitle = ref('')

// [C] 批量模式
const batchMode = ref(false)
const selectedIds = ref<Set<number>>(new Set())

// [E] 打印
const printPreviewVisible = ref(false)
const nowStr = ref('')
const STATUS_LABEL: Record<number,string> = { 0:'待办', 1:'进行中', 2:'已完成', 3:'已取消' }

const categoryColor: Record<string,string> = {
  0:'#1677ff',1:'#52c41a',2:'#fa8c16',3:'#f5222d',4:'#8c8c8c',5:'#722ed1',
}
const categoryColorLight: Record<string,string> = {
  0:'#e6f4ff',1:'#f6ffed',2:'#fff7e6',3:'#fff1f0',4:'#fafafa',5:'#f9f0ff',
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
const totalEvents = computed(()=>stats.value.todoCount+stats.value.doingCount+stats.value.doneCount)
function statPercent(type:'todo'|'doing'|'done'):number {
  const total=totalEvents.value; if(!total)return 0
  const val=type==='todo'?stats.value.todoCount:type==='doing'?stats.value.doingCount:stats.value.doneCount
  return Math.round((val/total)*100)
}
const filteredEvents = computed(()=>{
  let list=events.value
  if(quickFilter.value==='todo') list=list.filter(e=>e.status===EVENT_STATUS_VALUE.TODO)
  else if(quickFilter.value==='doing') list=list.filter(e=>e.status===EVENT_STATUS_VALUE.DOING)
  else if(quickFilter.value==='done') list=list.filter(e=>e.status===EVENT_STATUS_VALUE.DONE)
  if(keyword.value.trim()){
    const kw=keyword.value.trim().toLowerCase()
    list=list.filter((e:any)=>(e.title||'').toLowerCase().includes(kw)||(e.remark||'').toLowerCase().includes(kw))
  }
  if(filterCategory.value!==undefined&&filterCategory.value!==null)
    list=list.filter((e:any)=>e.category===filterCategory.value)
  return list
})
const filteredPendingEvents = computed(()=>
  filteredEvents.value.filter((e:any)=>e.status===EVENT_STATUS_VALUE.TODO||e.status===EVENT_STATUS_VALUE.DOING)
)
const filteredDoneEvents = computed(()=>
  filteredEvents.value.filter((e:any)=>e.status===EVENT_STATUS_VALUE.DONE)
)
const isAllSelected = computed(()=>{
  const all=[...filteredPendingEvents.value,...filteredDoneEvents.value]
  return all.length>0&&all.every(e=>selectedIds.value.has(e.id))
})
const isSomeSelected = computed(()=>selectedIds.value.size>0)

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
    const dots=[...new Set(dayEvents.map((e:any)=>e.category))]
    cells.push({day:d,date:dateKey,isToday:dateKey===todayStr,dots,eventCount:dayEvents.length,isWeekend:isWeekend(y,m,d),holidayName:getHolidayName(m,d),key:dateKey})
  }
  const remaining=42-cells.length
  for(let d=1;d<=remaining;d++) cells.push({day:d,isOther:true,dots:[],key:`next-${d}`})
  const rows:any[][]=[]
  for(let i=0;i<6;i++) rows.push(cells.slice(i*7,(i+1)*7))
  return rows
})
const allCells = computed(()=>calendarRows.value.flat())

function formatDate(d:Date):string {
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}
function formatMonth():string{ return `${currentYear.value}-${String(currentMonth.value).padStart(2,'0')}` }
function formatTime(t:string):string{ if(!t)return''; return t.length>=5?t.slice(0,5):t }
function isSelected(cell:any):boolean{ return !cell.isOther&&cell.date===selectedDate.value }

async function selectDate(cell:any){
  if(!cell.date)return
  selectedDate.value=cell.date
  const d=new Date(cell.date+'T00:00:00')
  if(d.getFullYear()!==currentYear.value||d.getMonth()+1!==currentMonth.value){
    currentYear.value=d.getFullYear(); currentMonth.value=d.getMonth()+1
    await loadMonthData()
  }
  await loadDayEvents()
}
async function loadMonthData(){
  monthLoading.value=true
  try{ monthData.value=(await getMonthEventsApi(formatMonth()) as any).data||[] }catch{}finally{ monthLoading.value=false }
}
async function loadDayEvents(){
  listLoading.value=true; loadError.value=false
  try{
    const params:any={eventDate:selectedDate.value}
    if(filterCategory.value!=null)params.category=filterCategory.value
    if(filterStatus.value!=null)params.status=filterStatus.value
    events.value=(await getDayEventsApi(params) as any).data||[]
  }catch{ loadError.value=true }finally{ listLoading.value=false }
}
async function loadStats(){
  try{ const res:any=await getCalendarStatsApi(formatMonth()); if(res.data)stats.value=res.data }catch{}
}
function retryLoad(){ refreshAll() }
async function prevMonth(){
  if(monthChanging.value)return
  monthChanging.value=true
  if(currentMonth.value===1){currentMonth.value=12;currentYear.value--}else{currentMonth.value--}
  await refreshAll(); setTimeout(()=>{monthChanging.value=false},250)
}
async function nextMonth(){
  if(monthChanging.value)return
  monthChanging.value=true
  if(currentMonth.value===12){currentMonth.value=1;currentYear.value++}else{currentMonth.value++}
  await refreshAll(); setTimeout(()=>{monthChanging.value=false},250)
}
async function goToday(){
  const t=new Date()
  currentYear.value=t.getFullYear(); currentMonth.value=t.getMonth()+1
  selectedDate.value=formatDate(t)
  await refreshAll()
}
function moveDay(dir:number){
  const d=new Date(selectedDate.value+'T00:00:00'); d.setDate(d.getDate()+dir)
  selectedDate.value=formatDate(d)
  if(d.getFullYear()!==currentYear.value||d.getMonth()+1!==currentMonth.value){
    currentYear.value=d.getFullYear(); currentMonth.value=d.getMonth()+1; refreshAll()
  }else{ loadDayEvents() }
}
function setQuickFilter(val:'all'|'todo'|'doing'|'done'){
  quickFilter.value=val
  filterStatus.value=val==='todo'?EVENT_STATUS_VALUE.TODO:val==='doing'?EVENT_STATUS_VALUE.DOING:val==='done'?EVENT_STATUS_VALUE.DONE:undefined
}
function onFilterChange(){}
function resetFilters(){ quickFilter.value='all'; filterCategory.value=undefined; filterStatus.value=undefined; keyword.value='' }
function clearSearch(){ keyword.value='' }
function openDrawer(event:any,date?:string){
  if(event){editingId.value=event.id;defaultNewDate.value=''}
  else{editingId.value=null;defaultNewDate.value=date||selectedDate.value}
  drawerVisible.value=true
}
async function onToggleDone(ev:any){
  try{await updateEventStatusApi(ev.id,EVENT_STATUS_VALUE.DONE);ElMessage.success('已标记完成');refreshAll()}catch{}
}
async function onUndo(ev:any){
  try{await updateEventStatusApi(ev.id,EVENT_STATUS_VALUE.TODO);ElMessage.success('已还原为待办');refreshAll()}catch{}
}
async function onDelete(id:number){
  try{await deleteEventApi(id);ElMessage.success('已删除');refreshAll()}catch{}
}
async function handleEvAction(cmd:string,ev:any){
  if(cmd==='edit'){openDrawer(ev);return}
  if(cmd==='doing'){try{await updateEventStatusApi(ev.id,EVENT_STATUS_VALUE.DOING);ElMessage.success('已开始进行');refreshAll()}catch{};return}
  if(cmd==='toggle'){ev.status===EVENT_STATUS_VALUE.DONE?await onUndo(ev):await onToggleDone(ev);return}
  if(cmd==='delete'){await onDelete(ev.id)}
}
function onSaved(){ refreshAll() }
async function refreshAll(){ await Promise.all([loadMonthData(),loadDayEvents(),loadStats()]) }

function toggleMonthPicker(){
  if(!showMonthPicker.value)mpYear.value=currentYear.value
  showMonthPicker.value=!showMonthPicker.value
}
async function jumpToMonth(y:number,m:number){
  showMonthPicker.value=false; currentYear.value=y; currentMonth.value=m; await refreshAll()
}
function handleDocClick(e:MouseEvent){
  if(!showMonthPicker.value)return
  const popup=monthPickerRef.value
  const trigger=pageRef.value?.querySelector('.cal-month-btn')
  if(popup&&!popup.contains(e.target as Node)&&trigger&&!trigger.contains(e.target as Node))
    showMonthPicker.value=false
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
  const all=[...filteredPendingEvents.value,...filteredDoneEvents.value]
  if(isAllSelected.value) selectedIds.value=new Set()
  else selectedIds.value=new Set(all.map(e=>e.id))
}
async function batchMarkDone(){
  if(selectedIds.value.size===0)return
  try{
    await ElMessageBox.confirm(`确定将 ${selectedIds.value.size} 项日程标记为完成？`,'批量操作',{confirmButtonText:'确定',cancelButtonText:'取消',type:'info'})
    const ids=[...selectedIds.value]
    await Promise.all(ids.map(id=>updateEventStatusApi(id,EVENT_STATUS_VALUE.DONE)))
    ElMessage.success(`已将 ${ids.length} 项标记为完成`)
    exitBatchMode(); refreshAll()
  }catch{}
}
async function batchDelete(){
  if(selectedIds.value.size===0)return
  try{
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.size} 项日程？此操作不可撤销。`,'批量删除',{confirmButtonText:'确定删除',cancelButtonText:'取消',type:'warning'})
    const ids=[...selectedIds.value]
    await Promise.all(ids.map(id=>deleteEventApi(id)))
    ElMessage.success(`已删除 ${ids.length} 项日程`)
    exitBatchMode(); refreshAll()
  }catch{}
}

// ---- [C] 排序上移/下移 ----
function moveEventUp(idx:number){
  if(idx<=0)return
  const list=events.value
  const pending=filteredPendingEvents.value
  const ai=list.findIndex(e=>e.id===pending[idx].id)
  const bi=list.findIndex(e=>e.id===pending[idx-1].id)
  if(ai>=0&&bi>=0){ const tmp=list[ai];list[ai]=list[bi];list[bi]=tmp; events.value=[...list] }
}
function moveEventDown(idx:number){
  if(idx>=filteredPendingEvents.value.length-1)return
  const list=events.value
  const pending=filteredPendingEvents.value
  const ai=list.findIndex(e=>e.id===pending[idx].id)
  const bi=list.findIndex(e=>e.id===pending[idx+1].id)
  if(ai>=0&&bi>=0){ const tmp=list[ai];list[ai]=list[bi];list[bi]=tmp; events.value=[...list] }
}

// ---- [E] 导出 & 打印 ----
function handleExportCmd(cmd:string){
  if(cmd==='export-day') exportCSV(events.value,`日程_${selectedDate.value}`)
  else if(cmd==='export-month') exportCSV(monthData.value,`日程_${formatMonth()}`)
  else if(cmd==='print') openPrintPreview()
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
function openPrintPreview(){
  nowStr.value=new Date().toLocaleString('zh-CN')
  printPreviewVisible.value=true
}
function doPrint(){
  const area=document.getElementById('print-area')
  if(!area)return
  const win=window.open('','_blank','width=700,height=900')
  if(!win){ElMessage.warning('请允许浏览器弹出窗口');return}
  win.document.write(`<!DOCTYPE html><html><head><meta charset="utf-8"><title>日程打印</title><style>
    body{font-family:'PingFang SC','Microsoft YaHei',sans-serif;padding:32px 40px;color:#1d2129;font-size:13px}
    .pp-header{border-bottom:2px solid #1677ff;padding-bottom:12px;margin-bottom:20px}
    .pp-title{font-size:20px;font-weight:700;color:#1677ff;margin-bottom:4px}
    .pp-date{font-size:12px;color:#86909c}
    .pp-row{display:flex;align-items:center;gap:10px;padding:9px 0;border-bottom:1px solid #f0f2f5}
    .pp-num{width:20px;font-size:11px;color:#c9cdd4;flex-shrink:0;text-align:right}
    .pp-cat-dot{width:8px;height:8px;border-radius:50%;flex-shrink:0}
    .pp-tit{flex:1;font-size:13px;font-weight:600}
    .pp-time{font-size:11px;color:#86909c;white-space:nowrap}
    .pp-status{font-size:11px;padding:1px 8px;border-radius:10px;white-space:nowrap}
    .pp-s-0{background:#fff7e6;color:#fa8c16}.pp-s-1{background:#e6f4ff;color:#1677ff}.pp-s-2{background:#f6ffed;color:#52c41a}
    .pp-footer{margin-top:20px;font-size:11px;color:#c9cdd4;text-align:right}
    .pp-empty{text-align:center;color:#c9cdd4;padding:40px 0}
    @media print{body{padding:16px 20px}}
  </style></head><body>${area.innerHTML}</body></html>`)
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
  else if(k==='escape'){e.preventDefault();drawerVisible.value=false;showMonthPicker.value=false;exitBatchMode()}
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
$primary:       #1677ff;
$primary-light: #e6f4ff;
$ink:           #1d2129;
$ink2:          #4e5969;
$sub:           #86909c;
$faint:         #c9cdd4;
$border:        #e5e6eb;
$surface:       #ffffff;
$bg:            #f0f2f5;
$danger:        #f5222d;
$warning:       #fa8c16;
$success:       #52c41a;
$rs:            8px;
$rs-lg:         12px;
$shadow-sm:     0 1px 4px rgba(0,0,0,.06);
$shadow-md:     0 4px 16px rgba(0,0,0,.08);
$shadow-lg:     0 8px 32px rgba(0,0,0,.1);

.calendar-page {
  display:flex; flex-direction:column;
  margin:-20px; width:calc(100% + 40px); height:calc(100% + 40px);
  min-height:0; background:$bg; overflow:hidden; outline:none; box-sizing:border-box;
}
.page-header {
  display:flex; align-items:center; justify-content:space-between;
  padding:16px 28px; background:$surface; border-bottom:1px solid $border;
  flex-shrink:0; box-shadow:0 1px 0 rgba(0,0,0,.03);
}
.header-left { display:flex; align-items:center; gap:12px; }
.header-title-group { display:flex; flex-direction:column; gap:3px; }
.page-title-icon {
  width:32px; height:32px; border-radius:$rs;
  background:$primary-light; color:$primary;
  display:flex; align-items:center; justify-content:center; flex-shrink:0;
}
.page-title { margin:0; font-size:18px; font-weight:700; color:$ink; letter-spacing:-.3px; }
.header-meta { display:flex; align-items:center; gap:6px; }
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
  width:32px; height:32px; border:1px solid $border; background:$surface;
  border-radius:$rs; cursor:pointer; display:flex; align-items:center; justify-content:center;
  color:$sub; transition:all .18s; flex-shrink:0;
  &:hover { border-color:$primary; color:$primary; background:$primary-light; }
  &.icon-btn-active { border-color:$primary; color:$primary; background:$primary-light; }
}
.main-body { display:flex; flex:1; overflow:hidden; min-height:0; }

.left-panel {
  flex:1.1; min-width:380px; max-width:640px;
  display:flex; flex-direction:column; gap:12px;
  padding:16px 20px; overflow-y:auto; overflow-x:hidden;
  &::-webkit-scrollbar { width:4px; }
  &::-webkit-scrollbar-thumb { background:#d0d3da; border-radius:4px; }
  &::-webkit-scrollbar-track { background:transparent; }
}

/* 统计卡片 */
.stats-row { display:grid; grid-template-columns:repeat(3,1fr); gap:10px; }
.stat-card {
  background:$surface; border-radius:$rs-lg; padding:14px 14px 10px;
  display:flex; flex-direction:column; gap:8px;
  box-shadow:$shadow-sm; border:1.5px solid transparent;
  cursor:pointer; transition:all .2s; position:relative; overflow:hidden;
  &:hover { box-shadow:$shadow-md; transform:translateY(-2px); }
  &.stat-active { border-color:currentColor; }
}
.stat-card-top { display:flex; align-items:center; justify-content:space-between; }
.stat-icon-wrap { width:36px; height:36px; border-radius:$rs; display:flex; align-items:center; justify-content:center; flex-shrink:0; }
.stat-ring { flex-shrink:0; line-height:0; }
.stat-todo {
  color:$warning;
  .stat-icon-wrap { background:#fff7e6; }
  &.stat-active { border-color:$warning; background:#fffbf0; }
  .stat-bar-inner { background:$warning; }
}
.stat-doing {
  color:$primary;
  .stat-icon-wrap { background:$primary-light; }
  &.stat-active { border-color:$primary; background:#f0f7ff; }
  .stat-bar-inner { background:$primary; }
}
.stat-done {
  color:$success;
  .stat-icon-wrap { background:#f6ffed; }
  &.stat-active { border-color:$success; background:#f6ffed; }
  .stat-bar-inner { background:$success; }
}
.stat-info {
  em { display:block; font-style:normal; font-size:28px; font-weight:800; line-height:1; color:$ink; }
  i { display:block; font-style:normal; font-size:11px; color:$sub; margin-top:2px; }
}
.stat-bar {
  height:3px; background:$border; border-radius:2px; overflow:hidden;
  .stat-bar-inner { height:100%; border-radius:2px; transition:width .6s cubic-bezier(.4,0,.2,1); min-width:4px; }
}

/* 日历卡片 */
.cal-card {
  background:$surface; border-radius:$rs-lg; padding:16px 14px 12px;
  box-shadow:$shadow-sm; border:1px solid rgba(0,0,0,.04); flex:1;
  display:flex; flex-direction:column; min-height:0; position:relative;
}
.cal-nav { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; flex-shrink:0; }
.cal-nav-center { display:flex; align-items:center; gap:10px; flex:1; justify-content:center; position:relative; }
.nav-btn {
  width:30px; height:30px; border:1px solid $border; background:$surface;
  border-radius:$rs; cursor:pointer; display:flex; align-items:center; justify-content:center;
  color:$sub; transition:all .18s; flex-shrink:0;
  &:hover:not(:disabled) { border-color:$primary; color:$primary; background:$primary-light; }
  &:disabled { opacity:.3; cursor:not-allowed; }
}
.cal-month {
  font-size:16px; font-weight:700; color:$ink; letter-spacing:.5px;
  em { font-style:normal; font-size:13px; color:$sub; font-weight:400; }
}
.cal-month-btn {
  display:inline-flex; align-items:center; gap:4px;
  cursor:pointer; padding:4px 10px; border-radius:$rs; transition:all .15s; user-select:none;
  &:hover { background:$primary-light; color:$primary; em { color:rgba($primary,.7); } .mp-arrow { color:$primary; } }
  .mp-arrow { color:$faint; transition:all .2s; &.open { transform:rotate(180deg); } }
}
.month-picker-popup {
  position:absolute; top:calc(100% + 8px); left:50%; transform:translateX(-50%);
  background:$surface; border:1px solid $border; border-radius:$rs-lg;
  box-shadow:$shadow-lg; padding:14px; z-index:200; width:216px;
  animation:popup-in .15s ease;
}
@keyframes popup-in {
  from { opacity:0; transform:translateX(-50%) translateY(-6px); }
  to   { opacity:1; transform:translateX(-50%) translateY(0); }
}
.mp-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:10px; }
.mp-yr-label { font-size:14px; font-weight:700; color:$ink; }
.mp-yr-btn {
  width:26px; height:26px; border:1px solid $border; background:$surface;
  border-radius:6px; cursor:pointer; display:flex; align-items:center; justify-content:center;
  color:$sub; transition:all .15s;
  &:hover { border-color:$primary; color:$primary; background:$primary-light; }
}
.mp-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:5px; }
.mp-m-btn {
  font-size:12.5px; font-weight:500; padding:6px 0; border-radius:6px;
  border:1px solid transparent; background:transparent; cursor:pointer;
  color:$ink2; text-align:center; transition:all .15s;
  &:hover { background:$primary-light; color:$primary; }
  &.active { background:$primary; color:#fff; border-color:$primary; font-weight:700; }
}
.today-btn {
  font-size:12px; font-weight:600; color:$primary;
  background:$primary-light; border:none; border-radius:6px;
  padding:4px 12px; cursor:pointer; transition:all .18s;
  &:hover { background:$primary; color:#fff; }
}
.month-fade-enter-active,.month-fade-leave-active { transition:opacity .18s,transform .18s; }
.month-fade-enter-from { opacity:0; transform:translateX(8px); }
.month-fade-leave-to { opacity:0; transform:translateX(-8px); }

.cal-grid-wrap { flex:1; display:flex; flex-direction:column; min-height:0; }
.cal-week-row {
  display:grid; grid-template-columns:repeat(7,1fr); margin-bottom:4px;
  span { text-align:center; font-size:11.5px; font-weight:600; color:$sub; padding:4px 0 8px; letter-spacing:2px;
    &.wk-end { color:$danger; } }
}
.cal-day-grid { display:grid; grid-template-columns:repeat(7,1fr); grid-auto-rows:1fr; flex:1; gap:3px; }
.day-cell {
  border-radius:7px; cursor:pointer; transition:all .15s;
  position:relative; border:1.5px solid transparent; min-height:48px;
  .cell-inner { width:100%; height:100%; display:flex; flex-direction:column; align-items:center; justify-content:flex-start; padding:5px 2px 4px; }
  &:hover { background:$primary-light; border-color:rgba($primary,.2); }
  &.active { background:$primary-light; border-color:rgba($primary,.4); .cell-num { color:$primary; font-weight:700; } }
  &.today {
    background:rgba($primary,.04); border-color:rgba($primary,.18);
    .cell-num { display:inline-flex; align-items:center; justify-content:center; width:28px; height:28px; border-radius:50%; background:$primary; color:#fff !important; font-weight:700; font-size:13px; box-shadow:0 0 0 4px rgba($primary,.14),0 2px 8px rgba($primary,.4); }
  }
  &.other { opacity:.28; pointer-events:none; }
  &.weekend .cell-num { color:$danger; font-weight:600; }
  &.holiday .cell-num { color:#cf382a; font-weight:700; }
  &.has-events { background:rgba($primary,.03); }
}
.cell-hd { display:flex; flex-direction:column; align-items:center; gap:2px; width:100%; justify-content:center; min-height:32px; flex-shrink:0; }
.cell-num { font-size:14px; font-weight:500; color:$ink; line-height:1.2; }
.cell-hd small {
  font-size:9px; color:#cf382a; background:#ffe4df; border-radius:3px; padding:0 4px; line-height:15px; font-weight:700;
  white-space:nowrap; max-width:30px; overflow:hidden; text-overflow:ellipsis; display:block;
}
.cell-dots {
  display:flex; justify-content:center; gap:2px; padding-top:2px;
  i { width:5px; height:5px; border-radius:50%; display:inline-block; flex-shrink:0;
    &.dot-more { width:auto; height:auto; font-size:8px; color:$sub; background:none !important; font-style:normal; line-height:1; font-weight:700; } }
}
.cal-loading { position:absolute; inset:0; background:rgba(255,255,255,.8); display:flex; align-items:center; justify-content:center; z-index:10; border-radius:$rs; }
.loading-spinner { width:28px; height:28px; border:3px solid $border; border-top-color:$primary; border-radius:50%; animation:spin .6s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }

.toolbar { display:flex; align-items:center; gap:8px; flex-shrink:0; flex-wrap:wrap; }
.toolbar-right { display:flex; align-items:center; gap:8px; margin-left:auto; }
.legend { display:flex; align-items:center; gap:6px; flex-wrap:wrap; }
.leg-item {
  display:flex; align-items:center; gap:3px; font-size:11px; color:$sub; white-space:nowrap;
  i { width:7px; height:7px; border-radius:50%; display:inline-block; flex-shrink:0; }
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

.right-panel { flex:1.2; display:flex; flex-direction:column; min-width:340px; background:$surface; border-left:1px solid $border; overflow:hidden; }
.rp-head {
  display:flex; align-items:center; justify-content:space-between;
  padding:12px 20px; border-bottom:1px solid $border; flex-shrink:0;
  background:linear-gradient(to bottom,#fafbfc 0%,$surface 100%);
  gap:12px; flex-wrap:wrap;
}
.rp-date-info { display:flex; align-items:center; gap:12px; }
.rp-date-main { display:flex; align-items:center; gap:10px; }
.rp-day-num { font-size:36px; font-weight:800; color:$ink; line-height:1; font-variant-numeric:tabular-nums; letter-spacing:-1px; }
.rp-date-sub { display:flex; flex-direction:column; gap:3px; }
.rp-date-text { font-size:13px; font-weight:600; color:$ink2; }
.rp-rel-date {
  font-size:11px; font-weight:600; padding:1px 7px; border-radius:10px; display:inline-block; width:fit-content;
  &.rel-today { background:#e6f4ff; color:$primary; }
  &.rel-future { background:#f6ffed; color:$success; }
  &.rel-past { background:$bg; color:$sub; }
}
.rp-head-right { display:flex; align-items:center; gap:8px; }
.quick-filters { display:flex; gap:2px; background:$bg; padding:3px; border-radius:$rs; border:1px solid $border; }
.qf-btn {
  font-size:11.5px; font-weight:500; padding:4px 10px;
  border:none; border-radius:6px; background:transparent; cursor:pointer; color:$sub; transition:all .15s; white-space:nowrap;
  &:hover { color:$ink; background:$surface; }
  &.active { background:$primary; color:#fff; box-shadow:0 2px 6px rgba($primary,.35); }
}
.rp-nav-btns { display:flex; gap:5px; }
.rp-arrow {
  width:28px; height:28px; border:1px solid $border; background:$surface;
  border-radius:$rs; cursor:pointer; display:flex; align-items:center; justify-content:center;
  color:$sub; transition:all .18s;
  &:hover { border-color:$primary; color:$primary; background:$primary-light; }
}

/* [C] 批量操作栏 */
.batch-bar {
  display:flex; align-items:center; justify-content:space-between;
  padding:8px 20px; background:#fffbe6; border-bottom:1px solid #ffe58f;
  flex-shrink:0; gap:12px;
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
.batch-count { font-size:12.5px; color:$ink2; font-weight:600; }
.batch-actions { display:flex; align-items:center; gap:6px; }
.batch-action-btn {
  display:inline-flex; align-items:center; gap:4px;
  font-size:12px; font-weight:600; padding:5px 12px; border-radius:6px;
  border:1px solid transparent; cursor:pointer; transition:all .15s;
  &:disabled { opacity:.4; cursor:not-allowed; }
}
.batch-done-btn { background:#f6ffed; color:$success; border-color:#b7eb8f; &:hover:not(:disabled) { background:$success; color:#fff; } }
.batch-del-btn { background:#fff1f0; color:$danger; border-color:#ffa39e; &:hover:not(:disabled) { background:$danger; color:#fff; } }
.batch-cancel-btn { background:$bg; color:$sub; border-color:$border; &:hover { color:$ink; background:$surface; } }
.batch-bar-slide-enter-active,.batch-bar-slide-leave-active { transition:all .22s ease; overflow:hidden; }
.batch-bar-slide-enter-from,.batch-bar-slide-leave-to { opacity:0; max-height:0; padding-top:0; padding-bottom:0; }
.batch-bar-slide-enter-to,.batch-bar-slide-leave-from { max-height:60px; }

.rp-body {
  flex:1; overflow-y:auto; padding:10px 0;
  &::-webkit-scrollbar { width:4px; }
  &::-webkit-scrollbar-thumb { background:#dde0e6; border-radius:4px; }
  &::-webkit-scrollbar-track { background:transparent; }
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
.ev-group { padding:0 12px; display:flex; flex-direction:column; gap:4px; }
.ev-group-done { opacity:.85; }
.ev-card {
  display:flex; align-items:stretch; gap:8px;
  padding:11px 12px; border-radius:$rs; cursor:pointer;
  transition:all .15s; border:1px solid transparent; background:$surface; position:relative;
  &:hover { background:#f7f8fa; border-color:$border; box-shadow:$shadow-sm; }
  &.pri-0 { border-left:3px solid $faint; padding-left:10px; }
  &.pri-1 { border-left:3px solid $warning; padding-left:10px; }
  &.pri-2 { border-left:3px solid $danger; padding-left:10px; background:#fffafa; &:hover { background:#fff5f5; border-color:rgba($danger,.15); } }
  &.is-doing { background:linear-gradient(to right,#f0f7ff 0%,$surface 100%); border-left-color:$primary; &:hover { background:#e8f3ff; } }
  /* [C] 批量选中态 */
  &.ev-selected { background:$primary-light !important; border-color:rgba($primary,.3) !important; }
  &.batch-mode { cursor:pointer; }
}
.ev-card-done { .ev-tit { text-decoration:line-through; color:$faint; font-weight:400; } }

/* [C] 批量复选框 */
.ev-batch-check { display:flex; align-items:center; justify-content:center; width:36px; flex-shrink:0; }
.batch-cb {
  width:17px; height:17px; border-radius:5px; border:1.8px solid #c0c4cc;
  display:flex; align-items:center; justify-content:center; background:$surface;
  transition:all .15s; flex-shrink:0;
  &.checked { background:$primary; border-color:$primary; color:#fff; }
}

/* [C] 排序按钮 */
.sort-btns { display:flex; flex-direction:column; gap:1px; opacity:0; transition:opacity .15s; }
.ev-card:hover .sort-btns { opacity:1; }
.sort-btn {
  width:20px; height:16px; border:none; background:transparent; cursor:pointer;
  display:flex; align-items:center; justify-content:center; color:$faint; border-radius:3px; padding:0;
  &:hover:not(:disabled) { color:$primary; background:$primary-light; }
  &:disabled { opacity:.2; cursor:not-allowed; }
}

.section-divider {
  padding:12px 12px 5px; text-align:center; position:relative;
  &::before { content:''; position:absolute; left:12px; right:12px; top:50%; height:1px; background:$border; }
  span { position:relative; background:$surface; padding:0 10px; font-size:11px; font-weight:600; color:$faint; letter-spacing:1px; }
}
.section-divider-toggle { cursor:pointer; &:hover span { color:$ink2; } }
.done-fold-enter-active,.done-fold-leave-active { transition:opacity .22s,max-height .28s ease; max-height:1000px; overflow:hidden; }
.done-fold-enter-from,.done-fold-leave-to { opacity:0; max-height:0; }

.ev-left { display:flex; align-items:center; gap:6px; flex-shrink:0; padding-top:1px; }
.ev-time {
  width:44px; text-align:right; font-size:12px; color:$ink2;
  font-variant-numeric:tabular-nums; font-weight:600; letter-spacing:-.3px;
  &.muted { color:$faint; }
}
.ev-check {
  flex-shrink:0; position:relative; cursor:pointer;
  input[type="checkbox"] { position:absolute; opacity:0; width:0; height:0; }
  .check-box { display:block; width:17px; height:17px; border:1.8px solid #c0c4cc; border-radius:5px; transition:all .18s; position:relative; background:$surface; }
  &:hover .check-box { border-color:$primary; }
  &.checked .check-box { background:$success; border-color:$success;
    &::after { content:''; position:absolute; left:4.5px; top:1.5px; width:4.5px; height:8.5px; border:solid #fff; border-width:0 2px 2px 0; transform:rotate(45deg); } }
}
.ev-main { flex:1; min-width:0; display:flex; flex-direction:column; gap:4px; }
.ev-title-row { display:flex; align-items:center; gap:6px; flex-wrap:nowrap; }
.ev-cat-bar { width:3px; height:14px; border-radius:2px; flex-shrink:0; &.faded { opacity:.35; } }
.ev-tit {
  font-size:13.5px; font-weight:600; color:$ink;
  white-space:nowrap; overflow:hidden; text-overflow:ellipsis; min-width:0; flex:1; cursor:text;
  &.strike { text-decoration:line-through; color:$faint; font-weight:400; }
}
.ev-tit-input {
  flex:1; min-width:0; font-size:13.5px; font-weight:600; color:$ink;
  border:1.5px solid $primary; border-radius:5px; outline:none;
  padding:2px 7px; background:$primary-light; box-shadow:0 0 0 3px rgba($primary,.1);
}
.ev-meta-row { display:flex; align-items:center; gap:6px; flex-wrap:wrap; }
.ev-cat-tag { display:inline-block; font-size:10.5px; font-weight:600; padding:1px 7px; border-radius:4px; white-space:nowrap;
  &.done-tag { background:$bg !important; color:$sub !important; } }
.ev-duration { display:flex; align-items:center; gap:3px; font-size:11px; color:$sub; svg { flex-shrink:0; } }
.ev-remark { font-size:11.5px; color:$sub; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; max-width:180px; }

.doing-pulse {
  width:7px; height:7px; border-radius:50%; background:$primary;
  flex-shrink:0; display:inline-block; animation:pulse-dot 1.6s ease-out infinite;
}
@keyframes pulse-dot {
  0%   { box-shadow:0 0 0 0 rgba(22,119,255,.55); }
  70%  { box-shadow:0 0 0 7px rgba(22,119,255,0); }
  100% { box-shadow:0 0 0 0 rgba(22,119,255,0); }
}
.ev-right { flex-shrink:0; display:flex; align-items:center; gap:4px; }
.ev-action-btn {
  width:28px; height:28px; border:none; background:transparent;
  border-radius:7px; cursor:pointer; display:flex; align-items:center; justify-content:center;
  color:$sub; transition:all .15s;
  &:hover { background:#eff0f2; color:$ink; }
  &.ev-del:hover { background:#fff1f0; color:$danger; }
  &.more-btn { opacity:0; }
}
.ev-card:hover .more-btn { opacity:1; }

.rp-empty { display:flex; flex-direction:column; align-items:center; justify-content:center; padding:40px 20px; }
.rp-filter-empty { gap:10px; p { font-size:13px; } }
.empty-illus { margin-bottom:16px; line-height:0; }
.empty-title { font-size:14.5px; font-weight:600; color:$ink2; margin:0 0 6px; }
.empty-hint {
  font-size:12px; color:$faint; text-align:center; line-height:1.7; margin:0;
  kbd { display:inline-block; font-family:inherit; font-size:10px; font-weight:600; background:$surface; border:1px solid $border; border-radius:3px; padding:1px 5px; color:$ink2; }
}

.rp-foot { padding:12px 20px; border-top:1px solid $border; flex-shrink:0; background:#fafbfc; }
.foot-hint {
  font-size:12px; color:$faint;
  kbd { display:inline-block; font-family:inherit; font-size:10px; font-weight:600; background:$surface; border:1px solid $border; border-radius:3px; padding:1px 4px; color:$ink2; box-shadow:0 1px 2px rgba(0,0,0,.06); }
}
.foot-progress-wrap { display:flex; flex-direction:column; gap:6px; }
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
    &.clr-warning { color:$warning; } &.clr-primary { color:$primary; } &.clr-success { color:$success; } }
}

/* [E] 打印预览 */
.print-preview { padding:20px 24px; }
.pp-header { border-bottom:2px solid $primary; padding-bottom:12px; margin-bottom:16px; }
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
    &.pp-s-2 { background:#f6ffed; color:$success; } }
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
  .left-panel { min-width:340px; padding:14px 16px; }
  .toolbar .legend { display:none; }
  .search-box { width:170px; }
  .kbd-hints-wrap { display:none; }
}
@media (max-width:900px) {
  .main-body { flex-direction:column; }
  .left-panel { min-width:unset; max-height:52vh; border-bottom:1px solid $border; padding:12px 14px; }
  .right-panel { min-width:unset; }
  .header-actions .search-box { display:none; }
  .rp-day-num { font-size:28px; }
  .rp-head { flex-wrap:wrap; gap:8px; }
  .quick-filters { order:3; }
}
</style>
