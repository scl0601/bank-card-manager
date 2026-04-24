@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ============================================
REM Git 提交清理脚本 (Windows 版本)
REM 生成时间: 2026-04-24
REM 用途: 自动提交清理后的更改
REM ============================================

echo ========================================
echo   Git 提交清理脚本 (Windows)
echo ========================================
echo.

REM 检查是否在 Git 仓库中
if not exist ".git" (
    echo [错误] 当前目录不是 Git 仓库
    pause
    exit /b 1
)

REM 检查 Git 状态
echo [检查 Git 状态...]
git status --short
echo.

echo [准备提交以下更改:]
echo   - 删除的空文件和废弃代码
echo   - 更新的 .gitignore 文件
echo   - 清理的编译产物
echo.

set /p confirm="确认提交这些更改? (Y/N): "
if /i not "%confirm%"=="Y" (
    echo 已取消提交操作
    pause
    exit /b 0
)

echo.
echo [开始提交...]
echo.

REM 暂存所有更改
echo [1/3] 暂存更改...
git add -A
if !errorlevel! neq 0 (
    echo [错误] 暂存更改失败
    pause
    exit /b 1
)
echo   [OK] 已暂存所有更改
echo.

REM 创建提交
echo [2/3] 创建提交...
git commit -m "chore: 清理冗余文件和编译产物" -m "" -m "- 删除未使用的 CardSupplier 模块（8个空文件）" -m "- 删除废弃的 owner 模块（2个空文件）" -m "- 删除空 SQL 补丁文件 patch-missing-tables.sql" -m "- 清理构建日志文件（4个）" -m "- 移除临时 worktree 目录（3个）" -m "- 更新 .gitignore 添加构建日志和临时文件规则" -m "- 清理编译产物目录 backend\target 和 frontend\dist" -m "" -m "清理效果:" -m "- 删除文件: 11 个" -m "- 删除目录: 4 个" -m "- 优化体积: ~4.6M" -m "" -m "详细信息请查看 CLEANUP_REPORT.md"

if !errorlevel! neq 0 (
    echo [错误] 提交创建失败
    pause
    exit /b 1
)
echo   [OK] 提交创建成功
echo.

REM 显示提交信息
echo [3/3] 提交信息:
git log -1 --stat
echo.

echo ========================================
echo   提交完成！
echo ========================================
echo.
echo [后续操作]

REM 获取当前分支名
for /f "tokens=*" %%i in ('git branch --show-current') do set CURRENT_BRANCH=%%i

echo   1. 推送到远程: git push origin !CURRENT_BRANCH!
echo   2. 验证编译: cd backend ^&^& mvn compile
echo   3. 验证前端: cd frontend ^&^& npm run build
echo.
echo [OK] Git 提交成功完成
echo.
pause
