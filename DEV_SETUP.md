# 本地开发环境说明

## 环境要求

- JDK 17+（推荐 JDK 21）
- Maven 3.8+
- Node.js 20.19+ 或 22+（当前 macOS 已验证 Node 22 可构建）
- MySQL 8

> 当前仓库内 `.tools/apache-maven-3.9.9` 只包含 `boot/` 和 `lib/`，没有 `bin/mvn.cmd`。本机已验证可使用系统 Maven：`C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.15\bin\mvn.cmd`。

## macOS 工具链

当前 macOS 已安装可用的 Node 22 和 CloudBase CLI：

```bash
export PATH="/Users/shichenlong/shichenlong-work/tools/node-v22/current/bin:$PATH"
node --version
npm --version
cloudbase --version
```

如果需要补装 Java 21 和 Maven，可优先使用 Homebrew：

```bash
brew install openjdk@21 maven
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

当前 macOS 默认 `java`/`mvn` 仍可能指向 Java 8；后端启动和打包前必须确认 `java -version` 是 21。

## 后端启动

优先使用根目录脚本：

```bat
start-backend.bat
```

脚本会自动选择 JDK 17+，避免系统默认 Java 8 导致 Spring Boot 3 编译/启动失败。

Windows 手动启动：

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:JAVA_TOOL_OPTIONS='-Dfile.encoding=UTF-8'
$env:SPRING_PROFILES_ACTIVE='dev'
cd backend
mvn spring-boot:run
```

macOS 手动启动：

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="/Users/shichenlong/shichenlong-work/tools/node-v22/current/bin:$JAVA_HOME/bin:$PATH"
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"
export SPRING_PROFILES_ACTIVE=dev
cd backend
mvn spring-boot:run
```

开发环境默认启用数据库结构补丁和内置账号引导：

```yaml
app.schema-patch.enabled: true
app.bootstrap-users.enabled: true
```

生产环境默认关闭二者，并要求显式配置数据库连接和 `JWT_SECRET`。如需临时维护生产库，请通过环境变量打开并在维护完成后关闭：

```powershell
$env:APP_SCHEMA_PATCH_ENABLED='true'
$env:APP_BOOTSTRAP_USERS_ENABLED='false'
```

后端地址：

```text
http://localhost:7878
```

## 前端启动

```bash
export PATH="/Users/shichenlong/shichenlong-work/tools/node-v22/current/bin:$PATH"
cd frontend
npm install
npm run dev
```

前端地址：

```text
http://localhost:5173/
```

## 构建验证

前端：

```bash
export PATH="/Users/shichenlong/shichenlong-work/tools/node-v22/current/bin:$PATH"
cd frontend
npm run build
```

Windows 后端：

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd backend
mvn test
mvn clean package -DskipTests
```

macOS 后端：

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
cd backend
mvn test
mvn clean package -DskipTests
```
