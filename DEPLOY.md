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

## Preflight checklist

- Confirm `SPRING_PROFILES_ACTIVE=prod`.
- Confirm `JWT_SECRET`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
  and `SPRING_DATASOURCE_PASSWORD` are configured in CloudBase.
- If AI is enabled, confirm the Tencent AI secret id/key values are real values,
  not placeholder examples.
- Confirm `APP_CORS_ALLOWED_ORIGINS` contains only the production frontend and
  API domains that should send credentialed requests.
- Confirm `APP_SCHEMA_PATCH_ENABLED=false` and
  `APP_BOOTSTRAP_USERS_ENABLED=false` before normal production deployment.
- Run `npm run test`, `npm run build`, and `mvn test` before publishing.
- Run SQL files such as `backend/src/main/resources/sql/patch-performance-indexes.sql`
  manually during a database maintenance window only; deployment scripts do not
  apply them automatically.

## Rollback notes

- Frontend rollback: redeploy the previous static hosting build artifact.
- Backend rollback: switch CloudBase CloudRun back to the previous image/version.
- Database rollback: index patches are additive; if an added index causes an
  unexpected issue, drop only that index after checking current query load.
- Keep `APP_SCHEMA_PATCH_ENABLED=false` during rollback unless a controlled
  schema maintenance step explicitly requires it.

## Windows

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy-cloudbase.ps1
```

The PowerShell wrapper is specific to this project path:

`C:\Users\Administrator\CodeBuddy\20260327094619`

Do not copy either deployment script to another project without editing its
CloudBase environment, service name, paths, and verification fields.
