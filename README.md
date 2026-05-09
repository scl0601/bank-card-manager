# 银行卡管理后台系统

一个企业级内部使用的银行卡管理后台系统，用于管理持卡人、银行卡、流水、账单等信息。

## 技术栈

### 前端
- Vue 3 + TypeScript
- Vite 5
- Element Plus
- Pinia (状态管理)
- Vue Router (路由)
- Axios (HTTP请求)
- ECharts (图表)

### 后端
- Java 17 / Spring Boot 3.3
- Spring Security (安全认证)
- MyBatis-Plus (ORM)
- MySQL 8 (数据库)
- Hibernate Validator (参数校验)
- Knife4j / OpenAPI (接口文档)

### 部署环境
- **CloudBase**: 腾讯云开发平台
- **静态托管**: 前端部署到 CloudBase 静态网站托管
- **云托管**: 后端部署到 CloudBase 云托管容器
- **数据库**: 腾讯云 CynosDB MySQL

---

## 🌐 外网访问地址

| 服务 | 访问地址 | 状态 |
|------|----------|------|
| **前端应用** | [https://dev-4g1sv3870175b971-1411764939.tcloudbaseapp.com/?v=2026100042](https://dev-4g1sv3870175b971-1411764939.tcloudbaseapp.com/?v=2026100042) | ✅ 已上线 |
| **后端 API** | `https://bank-admin-backend-239413-10-1411764939.sh.run.tcloudbase.com` | ✅ 运行中 |

---

## 部署信息

### CloudBase 环境
- **环境ID**: dev-4g1sv3870175b971
- **区域**: 上海 (ap-shanghai)
- **套餐**: 个人版

### 云托管服务配置
- **服务名称**: bank-admin-backend
- **当前线上版本**: bank-admin-backend-033 (运行中)
- **服务类型**: 容器型 (Container)
- **CPU**: 1 核
- **内存**: 2 GB
- **最小实例数**: 1
- **最大实例数**: 2
- **端口**: 7878
- **访问类型**: OA / PUBLIC / MINIAPP

### 静态托管配置
- **域名**: dev-4g1sv3870175b971-1411764939.tcloudbaseapp.com
- **状态**: 在线

---

## 本地开发

### 环境要求
- Node.js >= 18
- Java >= 17
- Maven >= 3.8
- MySQL >= 8.0

### 启动后端
```bash
cd backend
mvn spring-boot:run
```
后端运行在: http://localhost:7878

### 启动前端
```bash
cd frontend
npm install
npm run dev
```
前端运行在: http://localhost:5173

---

## 项目结构

```
├── backend/                 # 后端项目 (Spring Boot)
│   ├── src/main/java/com/bank/admin/
│   │   ├── common/          # 公共模块 (异常、响应、工具类)
│   │   ├── config/          # 配置类
│   │   ├── module/          # 业务模块
│   │   │   ├── auth/        # 认证模块
│   │   │   ├── owner/       # 持卡人模块
│   │   │   ├── card/        # 银行卡模块
│   │   │   ├── transaction/ # 流水模块
│   │   │   ├── bill/        # 账单模块
│   │   │   ├── reminder/    # 提醒模块
│   │   │   ├── dashboard/   # 首页看板
│   │   │   └── log/         # 日志模块
│   │   └── security/        # 安全模块
│   └── pom.xml
│
├── frontend/                # 前端项目 (Vue 3)
│   ├── src/
│   │   ├── api/             # API 接口封装
│   │   ├── assets/          # 静态资源
│   │   ├── components/      # 通用组件
│   │   ├── layouts/         # 布局组件
│   │   ├── router/          # 路由配置
│   │   ├── store/modules/   # Pinia 状态管理
│   │   ├── styles/          # 全局样式
│   │   ├── utils/           # 工具函数
│   │   └── views/           # 页面视图
│   ├── .env.development     # 开发环境变量
│   ├── .env.production      # 生产环境变量
│   └── vite.config.ts
│
├── Dockerfile               # 后端容器构建文件
├── start-backend.bat        # Windows 快速启动脚本
└── reset-db.bat             # 数据库重置脚本
```

---

## 功能模块

1. **登录与权限** - JWT认证、角色权限控制
2. **持卡人管理** - 持卡人信息CRUD、关联银行卡
3. **银行卡管理** - 银行卡信息、卡号脱敏、状态管理
4. **流水管理** - 收支记录、分类统计、导出功能
5. **账单管理** - 账单生成、还款日提醒、还款记录
6. **提醒中心** - 到期提醒、逾期标记、通知推送
7. **首页看板** - 统计概览、数据可视化图表
8. **操作日志** - 操作审计、日志查询

---

## 更新日志

### 2026-05-10 (00:42 更新)
- **修复**: 账单列表筛选月份改为按还款日筛选而非账单日 (3498edd)
- **修改文件**: `frontend/src/views/bill/BillList.vue`
- **修改内容**:
  1. 筛选器 placeholder 从 "筛选账单月份" 改为 "筛选还款月份"
  2. `syncBillMonthQuery()` 逻辑：将值传给 `query.repayMonth`（后端按 `DATE_FORMAT(repay_date, '%Y-%m')` 查询），而非之前的 `startBillMonth/endBillMonth`
- 后端已支持 `repayMonth` 参数，无需改后端代码
- 代码已推送到 GitHub 远程仓库
- 前端44个文件已上传到静态托管（纯前端改动，无需重新部署后端）
- 更新前端缓存刷新参数为 `?v=2026100042`

### 2026-05-10 (00:38 更新) - 补丁
- **修复**: 修正 `ServletRequestAttributes` 的 import 路径
  - 错误: `org.springframework.web.servlet.ServletRequestAttributes` (不存在)
  - 正确: `org.springframework.web.context.request.ServletRequestAttributes` (来自 spring-web)
- **部署**: 前端已上传 + 后端 v034 部署中（CloudRun 构建较慢，可能已排队）
- **代码**: 已提交 (a9fec41) 并推送到 GitHub

### 2026-05-10 (00:38 更新)
- **修复**: 账单明细保存失败 - MetaObjectHandlerConfig 增加 `_openid` 自动填充 (7b5b36f)
- **根因分析**:
  - `bill_detail` 表有 `_openid NOT NULL` 约束
  - `MetaObjectHandlerConfig` 的 `insertFill()` 没有处理 `_openid` 字段
  - 新增明细时 INSERT 缺少 `_openid` 值，导致 SQL 报错保存失败
- **修复方案**:
  - 在 `insertFill()` 中增加 `_openid` 字段自动填充逻辑
  - 优先从请求头 `x-wx-openid` 获取，无则填默认值 `"system"`
  - 使用 `hasGetter("_openid")` 判断，仅对含该字段的实体生效
- 代码已推送到 GitHub 远程仓库
- 前端44个文件已上传到静态托管
- 后端云托管 v033 已部署成功（状态 normal）
- 更新前端缓存刷新参数为 `?v=2026100038`

### 2026-05-10 (00:24 更新)
- **部署最新代码**: 银行卡管理 + 账单模块优化 (3c7df35)
- **问题修复**:
  - `CardBillServiceImpl` 新增 `normalizeBillStatus()` 状态校验，防止非法 status 值导致保存失败
  - 之前"账单新增明细一直保存不了"的原因：后端对状态值做了严格校验，前后端版本不一致时容易触发异常
  - 本次部署已确保前后端代码同步
- **前端变更**: BillList.vue 筛选区优化、状态徽章样式重构、CardUserList/ProfitStatsView/CardList UI调整
- 无数据库变更（纯业务代码更新）
- 前端44个文件已上传到静态托管
- 后端云托管 `bank-admin-backend` v032 部署中
- 更新前端缓存刷新参数为 `?v=2026100024`

### 2026-05-08 (12:57 更新)
- **部署最新代码**: 银行卡管理页面 + 利润统计优化 (fbd729d)
- 本次无数据库变更（纯业务代码更新）
- 涉及文件：利润统计页面、前端环境配置
- 代码已推送到 GitHub 远程仓库
- 前端44个文件已上传到静态托管
- 后端云托管 `bank-admin-backend` v031 部署中
- 更新前端缓存刷新参数为 `?v=2026081257`

### 2026-05-07 (21:47 更新)
- **部署最新代码**: 银行卡管理页面 + 账单模块优化 (b2f1743)
- **数据库补丁**:
  - `card_bill` 表新增 `fee_paid_amount` 字段（手续费已支付金额）
  - `card_bill` 表新增 `fee_pay_time` 字段（最近手续费支付时间）
  - `card_bill` 表新增 `fee_pay_method` 字段（支付方式：wechat/alipay/cash/other）
- 前端44个文件已上传到静态托管
- 后端云托管 `bank-admin-backend` v030 部署中
- 更新前端缓存刷新参数为 `?v=202605072147`

### 2026-05-06 (19:50 更新)
- **部署最新代码**: 银行卡管理页面优化 (bf7ea76)
- 本次无数据库变更（纯业务代码更新）
- 涉及文件：银行卡管理页面、账单列表、利润统计、操作日志切面、数据库补丁启动器等
- 前端44个文件已上传到静态托管
- 后端云托管 `bank-admin-backend` v029 部署中
- 更新前端缓存刷新参数为 `?v=202605061950`

### 2026-05-05 (20:34 更新)
- **部署最新代码**: 账单明细模块 + 银行卡页面优化 (0f44d79)
- **数据库补丁**:
  - 新建 `bill_detail` 账单明细表（POS刷卡流水/客户还款记录）
  - `card_bill` 表新增 `other_fee_amount` 字段（其他费用）
  - `card_bill` 表已有 `fee_rate`、`fee_amount`、`fee_paid` 字段，跳过
- 前端重新构建并上传到静态托管（44个文件）
- 后端云托管 `bank-admin-backend` v028 部署中
- 更新前端缓存刷新参数为 `?v=202605052034`

### 2026-04-29 (14:40 更新)
- **部署最新代码**: 银行卡管理页面优化 (55db456)
- **数据库补丁**:
  - `bank_card.repay_method` 注释更新，新增 `bankapp银行APP` 和 `none无` 选项
  - `bank_card.verified` 字段已移除（不再使用该字段）
- 前端重新构建并上传到静态托管（44个文件）
- 后端云托管 `bank-admin-backend` v027 已发布
- 更新前端缓存刷新参数为 `?v=202604291440`

### 2026-04-29 (09:59 更新)
- **部署最新代码**: 银行卡管理 + 账单模块优化 (5ace8ea)
- 本次无数据库变更（纯业务代码更新）
- 涉及文件：CardBillController/Service/ServiceImpl、前端 BillList/CardList/bill API
- 前端重新构建并上传到静态托管（44个文件）
- 后端云托管 `bank-admin-backend` v026 已发布
- 更新前端缓存刷新参数为 `?v=202604290959`

### 2026-04-29 (00:23 更新)
- **部署最新代码**: 银行卡管理页面 + 账单模块优化 (44dba83)
- **数据库补丁**:
  - `card_bill` 表新增 `verified` 字段（当月账单是否已核实）
  - `card_bill` 表新增 `expense_verified` 字段（本月支出明细是否已核实）
- 前端重新构建并上传到静态托管（44个文件全部更新）
- 后端云托管服务 `bank-admin-backend` 已触发新的容器部署 (v025)
- 更新前端缓存刷新访问参数为 `?v=202604290023`

### 2026-04-28 (19:58 更新)
- **部署最新代码**: 银行卡管理页面多次优化后的最新提交 (44185a3)
- **数据库补丁执行**:
  - `bank_card` 表删除废弃字段：`owner_relation`、`owner_name`、`total_limit`
  - `bank_card` 表更新 `repay_method` 默认值（空值→cloudpay, invoice→other）
  - `bank_card` 表更新 `verified` 默认值（NULL→0）
  - 新建 `repay_month_bill` 代还月度账单条目表
  - `card_user.fee_rate` 字段已确认存在，跳过
- 前端重新构建并上传到静态托管（44个文件全部更新）
- 后端云托管服务 `bank-admin-backend` 已触发新的容器部署 (v024)
- 更新前端缓存刷新访问参数为 `?v=202604281958`

### 2026-04-27 (21:20 更新)
- **紧急修复**: 发现线上代码落后于本地最新提交
- 前端重新构建并上传到静态托管（44个文件全部更新）
- 后端云托管服务 `bank-admin-backend` 已触发新的容器部署 (v023)
- 本次部署包含最新的"银行卡管理页面优化"提交内容
- 更新前端缓存刷新访问参数为 `?v=202604272121`

### 2026-04-27
- 部署最新代码到 CloudBase
- 修复线上 bank_card 表多余字段（card_no, owner_id, used_amount）导致 INSERT 失败的问题
- 修复前端页面缓存导致新增用户/银行卡后数据不刷新的问题
- 前端重新构建并上传到静态托管，访问地址更新
- 后端云托管服务 `bank-admin-backend` 已重新触发容器部署

### 2026-04-17
- 再次将当前最新前后端代码重新部署到 CloudBase
- 后端云托管服务 `bank-admin-backend` 已重新触发容器部署，控制台更新时间为 `2026-04-17 15:33:26`
- 前端重新构建并上传到静态网站托管
- 线上执行 `feedback-tables.sql`，补齐 `user_feedback`、`user_feedback_attachment`、`user_feedback_process_log` 三张反馈表
- 将上传文件相关的 `multipart` 配置同步到 `application-prod.yml`
- 修复反馈附件下载接口的前端 TypeScript 返回类型，确保本次前端构建成功
- 更新 README 中的前端缓存刷新访问参数为 `?v=202604171535`
- 后端 API 线上域名保持不变，可继续通过现有地址访问
- 当前前后端测试域名仍会先返回 CloudBase 风险提醒页，并提示“当前访问量已达上限”，需后续在控制台继续处理测试域名访问限制

### 2026-04-16
- 重新将当前最新前后端代码部署到 CloudBase
- 后端云托管服务 `bank-admin-backend` 成功发布至版本 `bank-admin-backend-013`
- 前端重新构建并上传到静态网站托管
- 更新 README 中的前端缓存刷新访问参数为 `?v=202604161539`
- 线上验证发现测试域名当前返回 CloudBase 风险提醒页，提示“当前访问量已达上限”，需后续在控制台继续排查域名访问限制
- 后端 API 线上域名保持不变，可继续通过现有地址访问


### 2026-04-15
- 部署到腾讯云 CloudBase 外网
- 后端部署至云托管容器
- 前端部署至静态网站托管
- 修复前端生产环境接口兜底并重新发布静态资源
- 调整后端 prod 配置与容器 profile，重新部署云托管服务
- 修复登录链路：登录请求不再附带旧 token，JWT 过滤器跳过 `/api/auth/login`
- 回测确认 CloudBase 外网登录与静态站点访问均正常
- 重新发布当前最新版本到 CloudBase（后端云托管 + 前端静态托管）
- 为云托管服务补充 AI 环境变量配置，外网场景可正常调用 AI 能力



