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
| **前端应用** | [https://dev-4g1sv3870175b971-1411764939.tcloudbaseapp.com/?v=202604270713](https://dev-4g1sv3870175b971-1411764939.tcloudbaseapp.com/?v=202604270713) | ✅ 已上线 |
| **后端 API** | `https://bank-admin-backend-239413-10-1411764939.sh.run.tcloudbase.com` | ✅ 运行中 |

---

## 部署信息

### CloudBase 环境
- **环境ID**: dev-4g1sv3870175b971
- **区域**: 上海 (ap-shanghai)
- **套餐**: 个人版

### 云托管服务配置
- **服务名称**: bank-admin-backend
- **当前线上版本**: bank-admin-backend-018
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

### 2026-04-27
- 将当前最新前后端代码重新部署到 CloudBase
- 后端云托管服务 `bank-admin-backend` 已成功运行在版本 `bank-admin-backend-018`，当前流量占比 `100%`
- 前端重新执行生产构建并上传最新 `dist` 到静态网站托管
- 更新 README 中的前端缓存刷新访问参数为 `?v=202604270713`
- 后端 API 线上域名保持不变，可继续通过现有地址访问

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



