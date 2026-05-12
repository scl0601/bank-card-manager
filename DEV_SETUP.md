# 本地开发环境说明

## 环境要求

- JDK 17+（推荐 JDK 21；本机可用：`C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot`）
- Maven 3.8+
- Node.js 18+（当前项目已验证 Node 24 可构建）
- MySQL 8

## 后端启动

优先使用根目录脚本：

```bat
start-backend.bat
```

脚本会自动选择 JDK 17+，避免系统默认 Java 8 导致 Spring Boot 3 编译/启动失败。

手动启动：

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:JAVA_TOOL_OPTIONS='-Dfile.encoding=UTF-8'
$env:SPRING_PROFILES_ACTIVE='dev'
cd backend
mvn spring-boot:run
```

后端地址：

```text
http://localhost:7878
```

## 前端启动

```bash
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
cd frontend
npm run build
```

后端：

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd backend
mvn clean package -DskipTests
```
