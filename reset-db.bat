@echo off
chcp 65001 >nul
set MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe

echo ========================================
echo 银行卡管理后台系统 - 数据库初始化
echo ========================================

echo.
echo [1/3] 删除旧数据库...
"%MYSQL%" -u root -proot -e "DROP DATABASE IF EXISTS bank_admin"

echo [2/3] 创建数据库和表结构...
"%MYSQL%" -u root -proot < "%~dp0backend\src\main\resources\sql\init.sql"

echo [3/3] 导入测试数据...
"%MYSQL%" -u root -proot bank_admin < "%~dp0backend\src\main\resources\sql\test_data.sql"

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo 数据库初始化成功！
    echo ========================================
    echo.
    echo 默认管理员账号:
    echo   用户名: admin
    echo   密码: admin123
    echo.
    echo 测试账号 (密码均为 123456):
    echo   zhangsan - 操作员
    echo   lisi     - 查看员
    echo   wangwu   - 查看员 (已禁用)
    echo ========================================
) else (
    echo.
    echo [错误] 数据库初始化失败
)

pause
