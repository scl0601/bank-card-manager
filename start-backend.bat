@echo off
chcp 65001 >nul
setlocal EnableExtensions EnableDelayedExpansion

:: ====== 自动探测 JAVA_HOME ======
:: 不盲信系统 JAVA_HOME；优先选择本机 JDK 17+，避免 Maven 默认走 Java 8 导致构建失败。

set "JAVA_HOME="

:: 常见 JDK 路径按优先级探测
for %%p in (
  "C:\Program Files\Microsoft\jdk-*"
  "C:\Program Files\Eclipse Adoptium\jdk-*"
  "C:\Program Files\Java\jdk-*"
  "C:\Program Files\JetBrains\IntelliJ IDEA*\jbr"
  "C:\Program Files\Zulu\zulu-*"
) do (
  for /d %%d in (%%p) do (
    if exist "%%d\bin\java.exe" (
      set "CANDIDATE_JAVA_HOME=%%d"
      call :check_java_candidate
      if defined JAVA_HOME goto :java_ok
    )
  )
)

:: 尝试从注册表读取（适用于 Oracle JDK）
for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\JavaSoft\Java Development Kit" /v CurrentVersion 2^>nul') do (
  for /f "tokens=2*" %%i in ('reg query "HKLM\SOFTWARE\JavaSoft\Java Development Kit\%%b" /v JavaHome 2^>nul') do (
    if exist "%%j\bin\java.exe" (
      set "CANDIDATE_JAVA_HOME=%%j"
      call :check_java_candidate
      if defined JAVA_HOME goto :java_ok
    )
  )
)

:java_ok
if not defined JAVA_HOME (
  echo [ERROR] 未找到 JDK 17+，请安装 JDK 17/21 或设置 JAVA_HOME
  pause
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 %JAVA_TOOL_OPTIONS%"
echo [INFO] JAVA_HOME=%JAVA_HOME%
java -version

:: ====== 自动探测 Maven ======
if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" (
  set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
  goto :maven_ok
)

where mvn.cmd >nul 2>&1 && (
  set "MAVEN_CMD=mvn.cmd"
  goto :maven_ok
)

:: 探测 IDE 内置 Maven
for %%p in (
  "C:\Program Files\JetBrains\IntelliJ IDEA*\plugins\maven\lib\maven3\bin\mvn.cmd"
) do (
  for %%f in (%%p) do (
    if exist "%%f" set "MAVEN_CMD=%%f" && goto :maven_ok
  )
)

:maven_ok
if not defined MAVEN_CMD (
  echo [ERROR] 未找到 Maven，请安装 Maven 或设置 MAVEN_HOME 环境变量
  pause
  exit /b 1
)

echo [INFO] MAVEN_CMD=%MAVEN_CMD%

:: ====== 启动后端 ======
set "SPRING_PROFILES_ACTIVE=dev"
cd /d "%~dp0backend"
"%MAVEN_CMD%" spring-boot:run
pause
exit /b %ERRORLEVEL%

:check_java_candidate
for /f "tokens=2 delims=\"" %%v in ('"%CANDIDATE_JAVA_HOME%\bin\java.exe" -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VERSION=%%v"
for /f "tokens=1 delims=." %%m in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%m"
if "%JAVA_MAJOR%"=="1" for /f "tokens=2 delims=." %%m in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%m"
if not defined JAVA_MAJOR exit /b 0
if %JAVA_MAJOR% GEQ 17 set "JAVA_HOME=%CANDIDATE_JAVA_HOME%"
exit /b 0
