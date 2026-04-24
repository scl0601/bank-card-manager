# 项目冗余清理 - 快速执行指南

## 📋 执行前检查清单

- [ ] 确保代码已提交到 Git
- [ ] 确保有数据库备份
- [ ] 在开发环境先测试
- [ ] 通知团队成员

## 🚀 快速执行步骤

### Windows 用户

```cmd
# 1. 打开命令提示符（以管理员身份运行）
# 2. 进入项目根目录
cd C:\Users\Administrator\CodeBuddy\20260327094619

# 3. 执行清理脚本
cleanup.bat

# 4. 验证编译
cd backend
mvn compile
cd ..

cd frontend
npm run build
cd ..

# 5. 提交更改
git add -A
git commit -m "chore: 清理冗余文件和编译产物"
```

### Linux/Mac 用户

```bash
# 1. 打开终端
# 2. 进入项目根目录
cd /path/to/project

# 3. 添加执行权限
chmod +x cleanup.sh

# 4. 执行清理脚本
./cleanup.sh

# 5. 验证编译
cd backend && mvn compile && cd ..
cd frontend && npm run build && cd ..

# 6. 提交更改
git add -A
git commit -m "chore: 清理冗余文件和编译产物"
```

## 🗄️ 数据库清理（可选）

```bash
# 1. 备份数据库
mysqldump -u root -p bank_admin > backup_$(date +%Y%m%d_%H%M%S).sql

# 2. 执行清理 SQL
mysql -u root -p bank_admin < database_cleanup.sql

# 3. 验证数据完整性
mysql -u root -p bank_admin -e "SELECT COUNT(*) FROM card_bill;"
```

## 📊 预期清理效果

| 项目 | 清理前 | 清理后 | 优化 |
|------|--------|--------|------|
| 源码文件数 | 190+ | 179 | -11 |
| 编译产物 | 4.4M | 0 | -4.4M |
| 日志文件 | 4 个 | 0 | -100KB |
| 总体积 | ~240M | ~235M | -5M |

## ⚠️ 注意事项

1. **不要在生产环境直接执行数据库清理**
2. **清理后务必验证编译和功能**
3. **如遇问题可通过 Git 回滚**
4. **node_modules 不会被清理（开发依赖）**

## 🔄 回滚方法

如果清理后出现问题：

```bash
# 回滚 Git 更改
git reset --hard HEAD~1

# 或者恢复特定文件
git checkout HEAD~1 -- path/to/file

# 恢复数据库
mysql -u root -p bank_admin < backup_20260424.sql
```

## 📞 问题排查

### 问题 1: 编译失败

```bash
# 清理 Maven 缓存
cd backend
mvn clean install -U

# 清理 npm 缓存
cd frontend
rm -rf node_modules package-lock.json
npm install
```

### 问题 2: Git 提交冲突

```bash
# 查看状态
git status

# 重新添加文件
git add -A
git commit -m "chore: 清理冗余文件"
```

### 问题 3: 数据库连接失败

```bash
# 检查数据库服务
# Windows
net start MySQL80

# Linux
sudo systemctl start mysql
```

## 📝 清理内容详细列表

### 删除的文件（11个）

**后端 CardSupplier 模块（8个空文件）**
- CardSupplierController.java
- CardSupplierQueryDTO.java
- CardSupplierSaveDTO.java
- CardSupplier.java
- CardSupplierMapper.java
- CardSupplierService.java
- CardSupplierServiceImpl.java
- CardSupplierVO.java

**前端 Owner 模块（2个空文件）**
- frontend/src/api/owner.ts
- frontend/src/views/owner/OwnerList.vue

**SQL 文件（1个空文件）**
- backend/src/main/resources/sql/patch-missing-tables.sql

### 删除的目录（4个）

- frontend/src/views/owner/
- .claude/worktrees/agent-a5a3586e/
- .claude/worktrees/agent-ac0ec6e7/
- .claude/worktrees/agent-ae05df54/

### 清理的编译产物

- backend/target/ (~1.7M)
- frontend/dist/ (~2.7M)

### 清理的日志文件（4个）

- build-ok.log
- build.log
- frontend/build-latest.log
- frontend/build.log

## ✅ 验证清单

清理完成后，请验证以下功能：

- [ ] 后端编译成功 (`mvn compile`)
- [ ] 前端编译成功 (`npm run build`)
- [ ] 后端启动成功 (`mvn spring-boot:run`)
- [ ] 前端启动成功 (`npm run dev`)
- [ ] 登录功能正常
- [ ] 银行卡管理功能正常
- [ ] 账单管理功能正常
- [ ] 流水记录功能正常
- [ ] 数据导出功能正常

## 📈 后续优化建议

1. **定期清理编译产物**
   ```bash
   # 添加到 .gitignore
   *.log
   build*.log
   ```

2. **配置自动清理脚本**
   ```json
   // package.json
   "scripts": {
     "clean": "rm -rf dist && rm -f build*.log",
     "prebuild": "npm run clean"
   }
   ```

3. **定期检查依赖更新**
   ```bash
   cd backend && mvn versions:display-dependency-updates
   cd frontend && npm outdated
   ```

4. **定期数据库优化**
   ```sql
   -- 每月执行一次
   OPTIMIZE TABLE card_bill, bank_card, card_transaction;
   ```

## 📚 相关文档

- 详细清理报告: `CLEANUP_REPORT.md`
- 清理脚本 (Windows): `cleanup.bat`
- 清理脚本 (Linux/Mac): `cleanup.sh`
- 数据库清理 SQL: `database_cleanup.sql`

---

**最后更新**: 2026-04-24  
**维护者**: Claude Code  
**项目**: 银行卡管理后台系统
