@echo off
chcp 65001 >nul
set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2022.3.3\jbr"
set "MAVEN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2022.3.3\plugins\maven\lib\maven3\bin\mvn.cmd"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d c:\Users\Administrator\CodeBuddy\20260327094619\backend
"%MAVEN_CMD%" spring-boot:run
