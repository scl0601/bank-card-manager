# Deployment

## macOS

From this repository on macOS, run:

```bash
./deploy-cloudbase.sh
```

The script builds the frontend, builds the backend with Java 21+, deploys
`bank-admin-backend` to CloudBase CloudRun, refreshes static hosting, and
verifies the public frontend domains plus backend OpenAPI schema.

Required local tools:

- Node.js and npm
- Java 21+
- Maven
- CloudBase CLI (`cloudbase`)

On this macOS machine, `deploy-cloudbase.sh` prepends the known local Node 22,
Maven 3.9.16, and Java 21 paths before checking commands, so deployment does
not depend on the shell default Java 8 / Node 16 toolchain.

Before deploying, make sure production secrets are configured in CloudBase
environment variables. Do not commit database credentials, JWT secrets, or
Tencent Cloud keys to Git. Production schema patching is disabled by default;
set `APP_SCHEMA_PATCH_ENABLED=true` only during a controlled database
maintenance window, then turn it back off.

## Windows

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy-cloudbase.ps1
```

The PowerShell wrapper is specific to this project path:

`C:\Users\Administrator\CodeBuddy\20260327094619`

Do not copy either deployment script to another project without editing its
CloudBase environment, service name, paths, and verification fields.
