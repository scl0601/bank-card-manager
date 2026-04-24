#!/bin/bash

# ============================================
# Git 提交清理脚本
# 生成时间: 2026-04-24
# 用途: 自动提交清理后的更改
# ============================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Git 提交清理脚本${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 检查是否在 Git 仓库中
if [ ! -d ".git" ]; then
    echo -e "${RED}错误: 当前目录不是 Git 仓库${NC}"
    exit 1
fi

# 检查 Git 状态
echo -e "${BLUE}检查 Git 状态...${NC}"
git status --short

echo ""
echo -e "${YELLOW}准备提交以下更改:${NC}"
echo "  - 删除的空文件和废弃代码"
echo "  - 更新的 .gitignore 文件"
echo "  - 清理的编译产物"
echo ""

read -p "确认提交这些更改? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}已取消提交操作${NC}"
    exit 0
fi

echo ""
echo -e "${GREEN}开始提交...${NC}"
echo ""

# 暂存所有更改
echo -e "${BLUE}[1/3] 暂存更改...${NC}"
git add -A
echo -e "${GREEN}  ✓ 已暂存所有更改${NC}"
echo ""

# 创建提交
echo -e "${BLUE}[2/3] 创建提交...${NC}"
git commit -m "chore: 清理冗余文件和编译产物

- 删除未使用的 CardSupplier 模块（8个空文件）
- 删除废弃的 owner 模块（2个空文件）
- 删除空 SQL 补丁文件 patch-missing-tables.sql
- 清理构建日志文件（4个）
- 移除临时 worktree 目录（3个）
- 更新 .gitignore 添加构建日志和临时文件规则
- 清理编译产物目录 backend/target 和 frontend/dist

清理效果:
- 删除文件: 11 个
- 删除目录: 4 个
- 优化体积: ~4.6M

详细信息请查看 CLEANUP_REPORT.md"

echo -e "${GREEN}  ✓ 提交创建成功${NC}"
echo ""

# 显示提交信息
echo -e "${BLUE}[3/3] 提交信息:${NC}"
git log -1 --stat
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  提交完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${YELLOW}后续操作:${NC}"
echo "  1. 推送到远程: git push origin $(git branch --show-current)"
echo "  2. 验证编译: cd backend && mvn compile"
echo "  3. 验证前端: cd frontend && npm run build"
echo ""
echo -e "${GREEN}✓ Git 提交成功完成${NC}"
