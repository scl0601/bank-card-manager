#!/bin/bash

# ============================================
# 项目冗余清理脚本
# 生成时间: 2026-04-24
# 用途: 安全清理项目中的冗余文件和编译产物
# ============================================

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 计数器
DELETED_FILES=0
DELETED_DIRS=0
FREED_SPACE=0

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  项目冗余清理脚本${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 检查是否在正确的目录
if [ ! -f "backend/pom.xml" ] || [ ! -f "frontend/package.json" ]; then
    echo -e "${RED}错误: 请在项目根目录执行此脚本${NC}"
    exit 1
fi

echo -e "${YELLOW}⚠️  警告: 此操作将删除以下内容:${NC}"
echo "  - 空文件和废弃代码 (11个文件)"
echo "  - 编译产物 (backend/target, frontend/dist)"
echo "  - 构建日志 (4个文件)"
echo "  - 临时 worktree 目录 (3个)"
echo ""
read -p "确认继续? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}已取消清理操作${NC}"
    exit 0
fi

echo ""
echo -e "${GREEN}开始清理...${NC}"
echo ""

# ============================================
# 第一步: 删除空文件和废弃代码
# ============================================

echo -e "${BLUE}[1/4] 清理空文件和废弃代码...${NC}"

# 后端 CardSupplier 模块 (8个空文件)
FILES_TO_DELETE=(
    "backend/src/main/java/com/bank/admin/module/card/controller/CardSupplierController.java"
    "backend/src/main/java/com/bank/admin/module/card/dto/CardSupplierQueryDTO.java"
    "backend/src/main/java/com/bank/admin/module/card/dto/CardSupplierSaveDTO.java"
    "backend/src/main/java/com/bank/admin/module/card/entity/CardSupplier.java"
    "backend/src/main/java/com/bank/admin/module/card/mapper/CardSupplierMapper.java"
    "backend/src/main/java/com/bank/admin/module/card/service/CardSupplierService.java"
    "backend/src/main/java/com/bank/admin/module/card/service/impl/CardSupplierServiceImpl.java"
    "backend/src/main/java/com/bank/admin/module/card/vo/CardSupplierVO.java"
    "frontend/src/api/owner.ts"
    "frontend/src/views/owner/OwnerList.vue"
    "backend/src/main/resources/sql/patch-missing-tables.sql"
)

for file in "${FILES_TO_DELETE[@]}"; do
    if [ -f "$file" ]; then
        rm -f "$file"
        echo "  ✓ 已删除: $file"
        ((DELETED_FILES++))
    fi
done

# 删除空目录
DIRS_TO_DELETE=(
    "frontend/src/views/owner"
    ".claude/worktrees/agent-a5a3586e"
    ".claude/worktrees/agent-ac0ec6e7"
    ".claude/worktrees/agent-ae05df54"
)

for dir in "${DIRS_TO_DELETE[@]}"; do
    if [ -d "$dir" ]; then
        rm -rf "$dir"
        echo "  ✓ 已删除目录: $dir"
        ((DELETED_DIRS++))
    fi
done

echo -e "${GREEN}  完成: 删除 $DELETED_FILES 个文件, $DELETED_DIRS 个目录${NC}"
echo ""

# ============================================
# 第二步: 清理构建日志
# ============================================

echo -e "${BLUE}[2/4] 清理构建日志...${NC}"

LOG_FILES=(
    "build-ok.log"
    "build.log"
    "frontend/build-latest.log"
    "frontend/build.log"
)

LOG_COUNT=0
for log in "${LOG_FILES[@]}"; do
    if [ -f "$log" ]; then
        rm -f "$log"
        echo "  ✓ 已删除: $log"
        ((LOG_COUNT++))
    fi
done

echo -e "${GREEN}  完成: 删除 $LOG_COUNT 个日志文件${NC}"
echo ""

# ============================================
# 第三步: 清理后端编译产物
# ============================================

echo -e "${BLUE}[3/4] 清理后端编译产物...${NC}"

if [ -d "backend/target" ]; then
    TARGET_SIZE=$(du -sh backend/target 2>/dev/null | cut -f1)
    echo "  当前 target 目录大小: $TARGET_SIZE"

    cd backend
    if command -v mvn &> /dev/null; then
        mvn clean -q
        echo "  ✓ Maven clean 执行成功"
    else
        cd ..
        rm -rf backend/target
        echo "  ✓ 手动删除 target 目录"
    fi
    cd ..
else
    echo "  ℹ target 目录不存在，跳过"
fi

echo -e "${GREEN}  完成: 后端编译产物已清理${NC}"
echo ""

# ============================================
# 第四步: 清理前端编译产物
# ============================================

echo -e "${BLUE}[4/4] 清理前端编译产物...${NC}"

if [ -d "frontend/dist" ]; then
    DIST_SIZE=$(du -sh frontend/dist 2>/dev/null | cut -f1)
    echo "  当前 dist 目录大小: $DIST_SIZE"
    rm -rf frontend/dist
    echo "  ✓ 已删除 frontend/dist"
else
    echo "  ℹ dist 目录不存在，跳过"
fi

echo -e "${GREEN}  完成: 前端编译产物已清理${NC}"
echo ""

# ============================================
# 清理完成总结
# ============================================

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  清理完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "清理统计:"
echo "  - 删除文件: $((DELETED_FILES + LOG_COUNT)) 个"
echo "  - 删除目录: $DELETED_DIRS 个"
echo "  - 清理编译产物: backend/target + frontend/dist"
echo ""
echo -e "${YELLOW}建议后续操作:${NC}"
echo "  1. 验证编译: cd backend && mvn compile"
echo "  2. 验证前端: cd frontend && npm run build"
echo "  3. 提交更改: git add -A && git commit -m 'chore: 清理冗余文件'"
echo ""
echo -e "${GREEN}✓ 项目清理成功完成${NC}"
