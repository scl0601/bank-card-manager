# 银行卡管理后台系统 - 版本迭代记录

> 本文档记录系统的每次迭代更新，包含功能新增、优化改进、问题修复等内容。


---

## v1.6.0 (2026-04-09)

### ✨ 功能新增
- **日历计划模块（全新模块）**
  - 日程事项的完整 CRUD 管理（新建/编辑/删除/状态变更）
  - 月视图日历：展示当月所有日期，标记有日程的日期（分类颜色圆点），支持上月/下月切换和年份月份快速跳转
  - 日视图列表：展示选中日期的所有日程事项，支持按状态快速筛选（全部/待办/进行中/已完成）
  - 日程分类体系：工作 / 生活 / 学习 / 健康 / 其他，每种分类对应独立颜色标识
  - 优先级管理：低 / 中 / 高三级优先级，通过左侧边框颜色区分
  - 状态流转：待办 → 进行中 → 已完成，支持一键标记完成/还原为待办
  - 提前提醒设置（按分钟数配置）

#### 前端页面功能
- **CalendarView 主页面**（左右双栏布局）
  - 左栏：
    - 统计卡片区：待办/进行中/已完成三项环形进度图 + 数量统计 + 进度条
    - 月度日历网格：42格日历（6行×7列），支持今天高亮、周末标红、法定节假日标注、日期圆点标记
    - 年份月份快速选择器（弹出面板）
    - 分类筛选下拉 + 图例说明
    - 快捷键提示面板
  - 右栏：
    - 选中日期信息头（大号日期数字 + 星期 + 相对时间描述）
    - 状态快速筛选按钮组
    - 日程卡片列表：时间轴 + 标题 + 分类标签 + 时长 + 备注 + 优先级标签
    - 已完成折叠区域（可展开/收起）
    - 底部本月完成率进度条
  - 交互功能：
    - 键盘快捷键：←→ 切换日期、↑↓ 切换月份、N 新建、T 回到今天、B 批量模式、Ctrl+F 搜索
    - 双击日历格子快速创建当日日程
    - 单击日程标题进入行内编辑模式
    - 批量选择与操作（批量标记完成 / 批量删除）
    - 事件排序上移/下移
    - 关键词搜索（标题/备注模糊匹配）
    - 导出当天/当月 CSV 文件
    - 打印当天日程（新窗口打印预览）
    - 加载骨架屏 + 错误重试机制

- **EventDrawer 抽屉组件**
  - 新建/编辑日程的侧边抽屉表单
  - 表单字段：标题、描述内容、日期、开始时间、结束时间、分类、优先级、状态、提前提醒分钟数、备注

#### 数据库变更
- 新增 `calendar_event` 日程事项表
  - 字段：title / description / eventDate(LocalDate) / startTime(LocalTime) / endTime(LocalTime) / category(0~4) / priority(0~2) / status(0~3) / remindBeforeMin / remark
  - 包含标准审计字段（is_deleted / create_by / create_time / update_by / update_time）

#### 后端接口
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/calendar/events/month` | 获取月度事项列表（日历标记用） |
| GET | `/api/calendar/events/day` | 获取指定日期的事项列表 |
| GET | `/api/calendar/events/{id}` | 获取事项详情 |
| POST | `/api/calendar/events` | 新增日程事项 |
| PUT | `/api/calendar/events/{id}` | 编辑日程事项 |
| DELETE | `/api/calendar/events/{id}` | 删除日程事项 |
| PUT | `/api/calendar/events/{id}/status` | 更新事项状态 |
| GET | `/api/calendar/events/stats/month` | 获取月度统计数据 |

#### 后端模块结构（calendar）
```
module/calendar/
├── controller/CalendarEventController.java
├── service/CalendarEventService.java
├── serviceImpl/CalendarEventServiceImpl.java
├── mapper/CalendarEventMapper.java
├── entity/CalendarEvent.java
├── dto/
│   ├── CalendarEventQueryDTO.java
│   └── CalendarEventSaveDTO.java
└── vo/
    ├── CalendarEventVO.java
    └── CalendarStatsVO.java
```

#### 字典常量扩展
- `dict.ts` 新增日程相关常量：
  - `EVENT_CATEGORY_VALUE/OPTIONS/MAP/TAG_TYPE` — 日程分类（工作/生活/学习/健康/其他）
  - `EVENT_PRIORITY_VALUE/OPTIONS/MAP/TAG_TYPE` — 优先级（低/中/高）
  - `EVENT_STATUS_VALUE/OPTIONS/MAP/TAG_TYPE` — 状态（待办/进行中/已完成/已取消）

#### 菜单与路由
- 侧边栏新增「日历计划」菜单项（Calendar 图标）
- 路由注册 `/calendar` → `CalendarView.vue`

---

## v1.5.0 (2026-04-08)

### ✨ 功能新增
- **个人记账模块（全新模块）**
  - 日常收支记录管理，按天记录收入和支出
  - 可选关联银行卡，与现有银行卡体系打通
  - 记账分类体系：一级分类 + 二级分类树形结构
  - 预置默认分类数据：
    - 收入类（5个）：工资、奖金、兼职、投资理财、其他收入
    - 支出类一级（9个）：餐饮、交通、购物、居住、医疗、教育、娱乐、人情、其他支出
    - 支出类二级（15个）：早餐/午餐/晚餐/零食饮料、公交地铁/打车/停车费、日用品/服装鞋包/电子产品等

- **记账列表页**
  - 顶部统计卡片区：本月收入 / 本月支出 / 本月结余
  - 筛选区：记账类型（收入/支出）+ 级联分类选择器 + 日期范围
  - 表格区：日期、类型标签、分类、金额（红绿色区分正负）、描述、关联银行卡、操作列
  - "记一笔"快速记账弹窗：类型单选 → 分类下拉（按类型联动过滤）→ 金额 → 日期 → 描述 → 可选关联银行卡
  - Excel 导出功能
  - 批量删除

- **分类管理**
  - 内嵌侧边抽屉式管理（不占用独立菜单）
  - 树形表格展示分类层级结构
  - 分类的新增/编辑/删除操作
  - 删除保护：有子分类或有关联记录时不可删除

#### 数据库变更
- 新增 `book_category` 记账分类表（含 name/icon/type/sortOrder/parentId/status 字段）
- 新增 `personal_book` 个人记账表（含 bookDate/bookType/amount/categoryId/description/cardId 字段）

#### 后端接口
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/books/page` | 分页查询记账记录 |
| POST | `/api/books` | 新增记账 |
| PUT | `/api/books` | 编辑记账 |
| DELETE | `/api/books/{id}` | 删除记账 |
| DELETE | `/api/books/batch` | 批量删除 |
| GET | `/api/books/export` | 导出Excel |
| GET | `/api/books/summary` | 月度收支汇总统计 |
| GET | `/api/books/categories` | 查询分类列表（树形） |
| POST | `/api/books/categories` | 新增分类 |
| PUT | `/api/books/categories` | 编辑分类 |
| DELETE | `/api/books/categories/{id}` | 删除分类 |

#### 菜单更新
- 侧边栏新增"个人记账"菜单项（Wallet 图标），位于「流水管理」和「账单管理」之间

---

## v1.4.0 (2026-04-08)

### ⚡ 体验优化
- **列表页筛选条件自动搜索（移除手动查询按钮）**
  - `usePageTable` composable 新增 `autoSearch` 和 `debounceMs` 选项
  - 筛选条件变化时通过 `watch` + 防抖（400ms）自动触发查询，无需手动点「查询」按钮
  - 输入框打字停顿后、下拉选择/日期切换后均自动搜索
  - 翻页操作不受影响，重置按钮正常可用
  - 内部 `_isInternalChange` 标记防止 reset/handleSearch 触发重复请求

- **SearchBar 组件增强**
  - 新增 `hideSearchButton` prop，为 true 时隐藏「查询」按钮，仅保留「重置」和额外操作按钮

- **涉及页面**
  - 持卡人管理（OwnerList）
  - 银行卡管理（CardList）
  - 流水管理（TransactionList）
  - 账单管理（BillList）
  - 提醒中心（ReminderList）
  - 系统日志（LogList）

---

## v1.3.1 (2026-04-07)

### ⚡ 代码优化
- **数据字典常量重构 - 前后端枚举值对齐**
  - 重构 `constants/dict.ts`：所有枚举值从字符串改为整数值（与后端实体字段类型一致）
  - 新增 `*_VALUE` 常量对象用于代码中引用整数值
  - 更新 `*_MAP` 和 `*_TAG_TYPE` 的 key 类型为 `number`

- **列表页统一使用字典常量**
  - `OwnerList.vue` - 持卡人状态下拉和标签
  - `CardList.vue` - 卡片类型/状态下拉和标签
  - `TransactionList.vue` - 交易类型下拉和标签
  - `BillList.vue` - 账单状态下拉和标签
  - `ReminderList.vue` - 提醒类型/状态下拉和标签
  - `LogList.vue` - 操作结果下拉和标签

### 🐛 Bug 修复
- **修复 ReminderList.vue 和 LogList.vue 的 resetQuery 问题**
  - 问题：这两个页面覆写了 `resetQuery`，导致表单重置逻辑不完整
  - 解决：改为调用 `usePageTable` 提供的 `resetQuery`，并在 `handleReset` 中额外清空日期范围

---

## v1.3.0 (2026-04-07)

### ✨ 功能新增
- **首页看板全面增强**
  - 新增快捷操作区（添加持卡人/银行卡、记流水、查看账单、提醒中心）
  - 新增卡片类型分布饼图（信用卡 vs 借记卡）
  - 新增银行分布横向柱状图（按银行统计卡片数量）
  - 新增本月净额展示（收入 - 支出）
  - 近7日收支趋势图增加圆角样式和自适应 Y 轴标签
  - 最近操作日志增加"查看全部"跳转链接

- **后端接口增强**
  - `DashboardVO` 新增 `bankDistribution` 字段（银行分布统计数据）
  - `DashboardServiceImpl` 新增银行卡片数量分组统计逻辑

### ⚡ 代码优化
- 移除 Dashboard 页面中重复定义的 `formatAmount` 函数，改用 `@/utils/formatters`
- 使用 ECharts `PieChart` 组件，避免额外安装依赖
- 统一使用项目现有的样式变量和卡片样式

### 🐛 Bug 修复
- **修复 TypeScript 无法识别 .vue 文件模块**
  - 问题：导入 `.vue` 文件时提示 `Cannot find module './index.vue' or its corresponding type declarations`
  - 解决：在 `vite-env.d.ts` 中添加 `.vue` 模块类型声明

---

## v1.2.0 (2026-04-07)

### ⚡ 代码优化
- **列表页全面重构 - 使用通用组件替换重复代码**
  - 重构 `OwnerList.vue` - 持卡人管理（191行 → 135行，-29%）
  - 重构 `CardList.vue` - 银行卡管理（247行 → 240行，-3%）
  - 重构 `TransactionList.vue` - 流水管理（217行 → 175行，-19%）
  - 重构 `BillList.vue` - 账单管理（204行 → 155行，-24%）
  - 重构 `ReminderList.vue` - 提醒中心（407行 → 210行，-48%）
  - 重构 `LogList.vue` - 系统日志（139行 → 100行，-28%）
  
- **使用的通用组件**
  - `SearchBar` - 统一搜索栏样式
  - `PageTable` - 统一分页表格（含分页器）
  - `CrudDialog` - 统一 CRUD 弹窗
  - `StatusTag` - 统一状态标签映射
  - `AmountDisplay` - 统一金额格式化显示
  - `CardNumberDisplay` - 统一卡号脱敏显示
  - `ExportButton` - 统一导出按钮

- **使用的组合式函数**
  - `usePageTable` - 统一分页查询逻辑（含 pageNum/pageSize → current/size 适配）
  - `useExport` - 统一 Excel 导出逻辑
  - `formatTime` - 统一时间格式化（替代各页面本地函数）

- **重构收益**
  - 总计减少约 300 行重复代码
  - 所有页面统一使用通用组件，样式一致
  - 分页/查询/导出逻辑集中管理，便于后续维护

---

## v1.1.0 (2026-04-07)

### ⚡ 代码优化
- **前端通用组件库建设**
  - 新增 `utils/formatters.ts` - 格式化工具函数（金额、日期、脱敏等）
  - 新增 `constants/dict.ts` - 数据字典常量（状态枚举、选项配置）
  - 新增 `composables/usePageTable.ts` - 分页表格组合式函数
  - 新增 `composables/useCrudDialog.ts` - CRUD对话框组合式函数
  - 新增 `composables/useExport.ts` - 导出功能组合式函数
  - 新增 `composables/useDateRange.ts` - 日期范围组合式函数
  - 新增 `components/SearchBar` - 搜索栏组件
  - 新增 `components/PageTable` - 分页表格组件
  - 新增 `components/AmountDisplay` - 金额显示组件（带颜色和格式化）
  - 新增 `components/CardNumberDisplay` - 卡号脱敏显示组件
  - 新增 `components/StatusTag` - 状态标签组件
  - 新增 `components/CrudDialog` - CRUD对话框组件
  - 新增 `components/ExportButton` - 导出按钮组件
  - 全局注册所有通用组件到 main.ts
  
- **预期收益**
  - 消除约 800 行重复代码
  - 每个列表页代码量减少约 50%
  - 提升代码可维护性和一致性

---

## v1.0.1 (2026-04-07)

### 🐛 Bug 修复
- **修复 Dashboard 时间格式化越界异常**
  - 问题：首页统计看板在处理时间范围时，`dayjs` 格式化可能导致数组越界
  - 解决：优化时间边界判断逻辑，增加空值校验
  
- **修复导出功能 Blob 响应处理问题**
  - 问题：Excel 导出时 Blob 响应类型处理不当，导致文件损坏
  - 解决：统一 `request.ts` 中 Blob 响应的处理方式，确保 responseType 正确设置

---

## v1.0.0 (2026-03-27)

### 🎉 正式发布 - MVP 版本

#### 核心功能模块
- **登录与权限模块**
  - JWT Token 认证机制
  - Spring Security 权限控制
  - 登录日志记录

- **持卡人管理模块**
  - 持卡人信息 CRUD
  - 分页查询、条件筛选
  - 批量导入导出

- **银行卡管理模块**
  - 银行卡信息 CRUD
  - 卡号 AES 加密存储
  - 卡号脱敏展示（仅显示后四位）
  - 卡片状态管理（正常/冻结/注销）

- **流水管理模块**
  - 每日流水记录
  - 流水类型区分（消费/还款/取现/转账）
  - Excel 批量导入导出
  - 流水统计分析

- **账单管理模块**
  - 信用卡账单生成
  - 还款日管理
  - 账单状态跟踪
  - 逾期标记

- **提醒中心模块**
  - 还款日提前提醒
  - 逾期预警
  - 定时任务调度
  - 提醒状态管理

- **首页看板模块**
  - ECharts 统计图表
  - 持卡人/银行卡数量统计
  - 本月消费/还款汇总
  - 即将到期账单提醒

- **系统日志模块**
  - 操作日志记录
  - 登录日志记录
  - 日志查询与筛选

#### 技术架构
- **前端**: Vue 3 + TypeScript + Vite + Element Plus + Pinia + ECharts
- **后端**: Java 21 + Spring Boot 3.5 + MyBatis-Plus + Spring Security
- **数据库**: MySQL 8

---

## v0.9.0 (2026-03-27)

### ✨ 功能新增
- 首页看板统计图表开发
- 集成 ECharts 图表库
- 实现多维度数据可视化展示

---

## v0.8.0 (2026-03-27)

### ✨ 功能新增
- 提醒中心模块开发
- 定时任务调度实现
- 还款日提前提醒功能
- 逾期自动标记

---

## v0.7.0 (2026-03-27)

### ✨ 功能新增
- 账单管理模块开发
- 账单生成逻辑
- 还款日管理
- 账单状态流转

---

## v0.6.0 (2026-03-27)

### ✨ 功能新增
- 流水管理模块开发
- 每日流水记录管理
- 流水类型区分
- Excel 导入导出功能

---

## v0.5.0 (2026-03-27)

### ✨ 功能新增
- 银行卡管理模块开发
- 卡号 AES 加密存储实现
- 卡号脱敏展示逻辑
- 卡片状态管理

---

## v0.4.0 (2026-03-27)

### ✨ 功能新增
- 持卡人管理模块 CRUD 完成
- 分页查询接口
- 条件筛选功能

---

## v0.3.0 (2026-03-27)

### ✨ 功能新增
- 登录模块开发
- JWT Token 认证机制实现
- Spring Security 权限框架集成
- 登录日志记录

---

## v0.2.0 (2026-03-27)

### ✨ 功能新增
- 数据库逻辑设计完成
- 建表 SQL 脚本编写
  - `sys_user` 系统用户表
  - `sys_role` 角色表
  - `sys_menu` 菜单表
  - `card_holder` 持卡人表
  - `bank_card` 银行卡表
  - `transaction` 流水表
  - `bill` 账单表
  - `reminder` 提醒表
  - `sys_log` 系统日志表

---

## v0.1.0 (2026-03-27)

### 🚀 项目初始化

#### 前端骨架
- Vue 3 + Vite 项目初始化
- TypeScript 配置
- 目录结构规划
  - `views/` 页面目录
  - `components/` 通用组件目录
  - `api/` 接口封装目录
  - `store/modules/` 状态管理目录
  - `styles/` 样式目录
- Element Plus UI 框架集成
- Vue Router 路由配置
- Axios 请求封装

#### 后端骨架
- Spring Boot 3.5 项目初始化
- MyBatis-Plus 集成
- 模块化包结构设计
  - `controller/` 控制器层
  - `service/` 服务层
  - `mapper/` 数据访问层
  - `entity/` 实体类
  - `dto/` 数据传输对象
  - `vo/` 视图对象
- 全局异常处理
- 统一返回结果封装
- Knife4j 接口文档集成

---

## 版本说明

| 标识 | 含义 |
|------|------|
| 🚀 初始化 | 项目/模块初始化 |
| ✨ 功能 | 新功能开发 |
| 🎉 发布 | 版本发布 |
| 🐛 修复 | Bug 修复 |
| ⚡ 优化 | 性能/代码优化 |
| 🔒 安全 | 安全相关更新 |
| 📝 文档 | 文档更新 |

---

> 维护者：amtf_fzby  
> 最后更新：2026-04-09
