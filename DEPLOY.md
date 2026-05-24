# Deployment

This deployment wrapper is specific to this project:

`C:\Users\Administrator\CodeBuddy\20260327094619`

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy-cloudbase.ps1
```

The script builds the frontend, builds the backend with JDK 21, deploys `bank-admin-backend` to CloudBase CloudRun, refreshes static hosting, and verifies the public frontend domains plus backend OpenAPI schema.

Do not copy this script to another project without editing its hardcoded CloudBase environment, service name, paths, and verification fields.
