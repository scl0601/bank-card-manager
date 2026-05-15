# 银行卡管理后台系统

## 项目简介

内部使用的银行卡管理系统，用于管理持卡人、银行卡、流水、账单等信息。

## 技术栈

### 前端
- Vue 3 + TypeScript
- Vite 5
- Element Plus
- Pinia + Vue Router
- ECharts
- Axios

### 后端
- Java 17+（已用 JDK 21 验证）/ Spring Boot 3.3.0
- Spring Security + JWT
- MyBatis-Plus
- MySQL 8
- Knife4j (OpenAPI)

## 部署信息

### 环境配置
- **CloudBase 环境 ID**: dev-4g1sv3870175b971
- **数据库名**: bank_admin
- **数据库地址**: sh-cynosdbmysql-grp-81wbjz68.sql.tencentcdb.com:20721

### 访问地址

#### 前端（静态托管）
- **URL**: https://dev-4g1sv3870175b971-1411764939.tcloudbaseapp.com/
- **部署时间**: 2026-05-11 20:03
- **状态**: ✅ 已部署

#### 后端（云托管容器）
- **自定义域名**: https://api.bankaiscl.top (推荐)
- **默认域名**: https://bank-admin-backend-239413-10-1411764939.sh.run.tcloudbase.com
- **端口**: 7878
- **服务名**: bank-admin-backend
- **类型**: 容器型 (Container)
- **规格**: 1核 CPU / 2GB 内存
- **实例数**: 最小 1 个，最大 2 个
- **版本**: bank-admin-backend-037
- **状态**: ✅ 运行中 (normal)
- **更新时间**: 2026-05-11 20:17

#### API 文档 (Knife4j)
- **URL**: https://bank-admin-backend-239413-10-1411764939.sh.run.tcloudbase.com/doc.html

#### MySQL 数据库
- **控制台**: https://tcb.cloud.tencent.com/dev?envId=dev-4g1sv3870175b971#/db/mysql/table/default/
- **库名**: bank_admin
- **字符集**: utf8mb4
- **排序规则**: utf8mb4_unicode_ci

### 数据表清单（共 10 张）

| 表名 | 说明 | 状态 |
|------|------|------|
| bank_sys_user | 系统用户表 | ✅ |
| card_user | 用户表（两级层级） | ✅ |
| bank_card | 银行卡表 | ✅ |
| card_transaction | 流水记录表 | ✅ |
| card_bill | 账单表 | ✅ |
| reminder_task | 提醒任务表 | ✅ |
| operation_log | 操作日志表 | ✅ |
| book_category | 记账分类表 | ✅ |
| personal_book | 个人记账表 | ✅ |
| calendar_event | 日程事项表 | ✅ |


## CloudBase 控制台入口

- **总览**: https://tcb.cloud.tencent.com/dev?envId=dev-4g1sv3870175b971#/overview
- **云托管**: https://tcb.cloud.tencent.com/dev?envId=dev-4g1sv3870175b971#/platform-run
- **静态托管**: https://tcb.cloud.tencent.com/dev?envId=dev-4g1sv3870175b971#/static-hosting
- **数据库**: https://tcb.cloud.tencent.com/dev?envId=dev-4g1sv3870175b971#/db/mysql
- **环境设置**: https://tcb.cloud.tencent.com/dev?envId=dev-4g1sv3870175b971#/env

## 本地开发

详细环境说明见：[`DEV_SETUP.md`](./DEV_SETUP.md)

### 前端
```bash
cd frontend
npm install
npm run dev
```

### 后端
```bash
start-backend.bat
```

或手动指定 JDK 17+ 后启动：

```bash
cd backend
mvn clean package -DskipTests
java -jar target/bank-admin-1.0.0.jar
```

### 重置数据库
```bash
reset-db.bat
```

## 部署记录

| 时间 | 操作 | 状态 |
|------|------|------|
| 2026-05-11 20:00 | 初始化 MySQL 数据库 (bank_admin) | ✅ 完成 |
| 2026-05-11 20:01 | 部署后端到 CloudRun | ✅ 完成 |
| 2026-05-11 20:03 | 构建前端代码 | ✅ 成功 (14.96s) |
| 2026-05-11 20:03 | 上传前端到静态托管 (45个文件) | ✅ 完成 |

---

**部署完成时间**: 2026-05-11 20:03 (CST)
**部署工具**: CodeBuddy + CloudBase
