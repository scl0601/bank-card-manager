@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ============================================
REM 项目冗余清理脚本 (Windows 版本)
REM 生成时间: 2026-04-24
REM 用途: 安全清理项目中的冗余文件和编译产物
REM ============================================

echo ========================================
echo   项目冗余清理脚本 (Windows)
echo ========================================
echo.

REM 检查是否在正确的目录
if not exist "backend\pom.xml" (
    echo [错误] 请在项目根目录执行此脚本
    pause
    exit /b 1
)

if not exist "frontend\package.json" (
    echo [错误] 请在项目根目录执行此脚本
    pause
    exit /b 1
)

echo [警告] 此操作将删除以下内容:
echo   - 空文件和废弃代码 (11个文件)
echo   - 编译产物 (backend\target, frontend\dist)
echo   - 构建日志 (4个文件)
echo   - 临时 worktree 目录 (3个)
echo.
set /p confirm="确认继续? (Y/N): "
if /i not "%confirm%"=="Y" (
    echo 已取消清理操作
    pause
    exit /b 0
)

echo.
echo [开始清理...]
echo.

set DELETED_FILES=0
set DELETED_DIRS=0

REM ============================================
REM 第一步: 删除空文件和废弃代码
REM ============================================

echo [1/4] 清理空文件和废弃代码...

REM 后端 CardSupplier 模块
if exist "backend\src\main\java\com\bank\admin\module\card\controller\CardSupplierController.java" (
    del /f /q "backend\src\main\java\com\bank\admin\module\card\controller\CardSupplierController.java"
    echo   [OK] 已删除: CardSupplierController.java
    set /a DELETED_FILES+=1
)

if exist "backend\src\main\java\com\bank\admin\module\card\dto\CardSupplierQueryDTO.java" (
    del /f /q "backend\src\main\java\com\bank\admin\module\card\dto\CardSupplierQueryDTO.java"
    echo   [OK] 已删除: CardSupplierQueryDTO.java
    set /a DELETED_FILES+=1
)

if exist "backend\src\main\java\com\bank\admin\module\card\dto\CardSupplierSaveDTO.java" (
    del /f /q "backend\src\main\java\com\bank\admin\module\card\dto\CardSupplierSaveDTO.java"
    echo   [OK] 已删除: CardSupplierSaveDTO.java
    set /a DELETED_FILES+=1
)

if exist "backend\src\main\java\com\bank\admin\module\card\entity\CardSupplier.java" (
    del /f /q "backend\src\main\java\com\bank\admin\module\card\entity\CardSupplier.java"
    echo   [OK] 已删除: CardSupplier.java
    set /a DELETED_FILES+=1
)

if exist "backend\src\main\java\com\bank\admin\module\card\mapper\CardSupplierMapper.java" (
    del /f /q "backend\src\main\java\com\bank\admin\module\card\mapper\CardSupplierMapper.java"
    echo   [OK] 已删除: CardSupplierMapper.java
    set /a DELETED_FILES+=1
)

if exist "backend\src\main\java\com\bank\admin\module\card\service\CardSupplierService.java" (
    del /f /q "backend\src\main\java\com\bank\admin\module\card\service\CardSupplierService.java"
    echo   [OK] 已删除: CardSupplierService.java
    set /a DELETED_FILES+=1
)

if exist "backend\src\main\java\com\bank\admin\module\card\service\impl\CardSupplierServiceImpl.java" (
    del /f /q "backend\src\main\java\com\bank\admin\module\card\service\impl\CardSupplierServiceImpl.java"
    echo   [OK] 已删除: CardSupplierServiceImpl.java
    set /a DELETED_FILES+=1
)

if exist "backend\src\main\java\com\bank\admin\module\card\vo\CardSupplierVO.java" (
    del /f /q "backend\src\main\java\com\bank\admin\module\card\vo\CardSupplierVO.java"
    echo   [OK] 已删除: CardSupplierVO.java
    set /a DELETED_FILES+=1
)

REM 前端 owner 模块
if exist "frontend\src\api\owner.ts" (
    del /f /q "frontend\src\api\owner.ts"
    echo   [OK] 已删除: owner.ts
    set /a DELETED_FILES+=1
)

if exist "frontend\src\views\owner\OwnerList.vue" (
    del /f /q "frontend\src\views\owner\OwnerList.vue"
    echo   [OK] 已删除: OwnerList.vue
    set /a DELETED_FILES+=1
)

if exist "frontend\src\views\owner" (
    rmdir /s /q "frontend\src\views\owner"
    echo   [OK] 已删除目录: frontend\src\views\owner
    set /a DELETED_DIRS+=1
)

REM 空 SQL 文件
if exist "backend\src\main\resources\sql\patch-missing-tables.sql" (
    del /f /q "backend\src\main\resources\sql\patch-missing-tables.sql"
    echo   [OK] 已删除: patch-missing-tables.sql
    set /a DELETED_FILES+=1
)

REM 临时 worktree 目录
if exist ".claude\worktrees\agent-a5a3586e" (
    rmdir /s /q ".claude\worktrees\agent-a5a3586e"
    echo   [OK] 已删除目录: .claude\worktrees\agent-a5a3586e
    set /a DELETED_DIRS+=1
)

if exist ".claude\worktrees\agent-ac0ec6e7" (
    rmdir /s /q ".claude\worktrees\agent-ac0ec6e7"
    echo   [OK] 已删除目录: .claude\worktrees\agent-ac0ec6e7
    set /a DELETED_DIRS+=1
)

if exist ".claude\worktrees\agent-ae05df54" (
    rmdir /s /q ".claude\worktrees\agent-ae05df54"
    echo   [OK] 已删除目录: .claude\worktrees\agent-ae05df54
    set /a DELETED_DIRS+=1
)

echo   [完成] 删除 !DELETED_FILES! 个文件, !DELETED_DIRS! 个目录
echo.

REM ============================================
REM 第二步: 清理构建日志
REM ============================================

echo [2/4] 清理构建日志...

set LOG_COUNT=0

if exist "build-ok.log" (
    del /f /q "build-ok.log"
    echo   [OK] 已删除: build-ok.log
    set /a LOG_COUNT+=1
)

if exist "build.log" (
    del /f /q "build.log"
    echo   [OK] 已删除: build.log
    set /a LOG_COUNT+=1
)

if exist "frontend\build-latest.log" (
    del /f /q "frontend\build-latest.log"
    echo   [OK] 已删除: frontend\build-latest.log
    set /a LOG_COUNT+=1
)

if exist "frontend\build.log" (
    del /f /q "frontend\build.log"
    echo   [OK] 已删除: frontend\build.log
    set /a LOG_COUNT+=1
)

echo   [完成] 删除 !LOG_COUNT! 个日志文件
echo.

REM ============================================
REM 第三步: 清理后端编译产物
REM ============================================

echo [3/4] 清理后端编译产物...

if exist "backend\target" (
    echo   正在清理 backend\target...
    cd backend
    where mvn >nul 2>&1
    if !errorlevel! equ 0 (
        call mvn clean -q
        echo   [OK] Maven clean 执行成功
    ) else (
        cd ..
        rmdir /s /q "backend\target"
        echo   [OK] 手动删除 target 目录
    )
    if exist "backend" cd ..
) else (
    echo   [信息] target 目录不存在，跳过
)

echo   [完成] 后端编译产物已清理
echo.

REM ============================================
REM 第四步: 清理前端编译产物
REM ============================================

echo [4/4] 清理前端编译产物...

if exist "frontend\dist" (
    rmdir /s /q "frontend\dist"
    echo   [OK] 已删除 frontend\dist
) else (
    echo   [信息] dist 目录不存在，跳过
)

echo   [完成] 前端编译产物已清理
echo.

REM ============================================
REM 清理完成总结
REM ============================================

echo ========================================
echo   清理完成！
echo ========================================
echo.
echo 清理统计:
set /a TOTAL_FILES=!DELETED_FILES!+!LOG_COUNT!
echo   - 删除文件: !TOTAL_FILES! 个
echo   - 删除目录: !DELETED_DIRS! 个
echo   - 清理编译产物: backend\target + frontend\dist
echo.
echo [建议后续操作]
echo   1. 验证编译: cd backend ^&^& mvn compile
echo   2. 验证前端: cd frontend ^&^& npm run build
echo   3. 提交更改: git add -A ^&^& git commit -m "chore: 清理冗余文件"
echo.
echo [OK] 项目清理成功完成
echo.
pause
