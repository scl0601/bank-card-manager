@echo off
chcp 65001 >nul

:: ====== 自动探测 JAVA_HOME ======
if defined JAVA_HOME goto :java_ok

:: 常见 JDK 路径按优先级探测
for %%p in (
  "C:\Program Files\Eclipse Adoptium\jdk-*"
  "C:\Program Files\Java\jdk-*"
  "C:\Program Files (x86)\Java\jre-*"
  "C:\Program Files\Java\jre-*"
  "C:\Program Files\JetBrains\IntelliJ IDEA*\jbr"
  "C:\Program Files\Zulu\zulu-*"
) do (
  for /d %%d in (%%p) do (
    if exist "%%d\bin\java.exe" (
      set "JAVA_HOME=%%d"
      goto :java_ok
    )
  )
)

:: 尝试从注册表读取（适用于 Oracle JDK）
for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\JavaSoft\Java Development Kit" /v CurrentVersion 2^>nul') do (
  for /f "tokens=2*" %%i in ('reg query "HKLM\SOFTWARE\JavaSoft\Java Development Kit\%%b" /v JavaHome 2^>nul') do (
    if exist "%%j\bin\java.exe" set "JAVA_HOME=%%j"
  )
)

:java_ok
if not defined JAVA_HOME (
  echo [ERROR] 未找到 JDK，请安装 JDK 或设置 JAVA_HOME 环境变量
  pause
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo [INFO] JAVA_HOME=%JAVA_HOME%
java -version

:: ====== 自动探测 Maven ======
if defined MAVEN_HOME (
  set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
  goto :maven_ok
)

set "MAVEN_CMD=mvn.cmd"
where mvn.cmd >nul 2>&1 && goto :maven_ok

:: 探测 IDE 内置 Maven
for %%p in (
  "C:\Program Files\JetBrains\IntelliJ IDEA*\plugins\maven\lib\maven3\bin\mvn.cmd"
) do (
  for %%f in (%%p) do (
    if exist "%%f" set "MAVEN_CMD=%%f" && goto :maven_ok
  )
)

:maven_ok
if not exist "%MAVEN_CMD%" (
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
