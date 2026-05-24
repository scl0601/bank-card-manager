Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ProjectRoot = 'C:\Users\Administrator\CodeBuddy\20260327094619'
$EnvId = 'dev-4g1sv3870175b971'
$CloudRunService = 'bank-admin-backend'
$CloudRunPort = 7878
$Jdk21Home = 'C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'
$FrontendDir = Join-Path $ProjectRoot 'frontend'
$BackendDir = Join-Path $ProjectRoot 'backend'
$FrontendDist = Join-Path $FrontendDir 'dist'
$BackendApiDocsUrl = 'https://bank-admin-backend-239413-10-1411764939.sh.run.tcloudbase.com/v3/api-docs'
$FrontendUrls = @(
  'https://bankaiscl.top/',
  'https://dev-4g1sv3870175b971-1411764939.tcloudbaseapp.com/'
)
$RequiredBackendFields = @('topUserId', 'topUserName', 'creditCardCount', 'debitCardCount')

function Write-Step {
  param([Parameter(Mandatory = $true)][string]$Message)
  Write-Host ''
  Write-Host "==> $Message" -ForegroundColor Cyan
}

function Require-Command {
  param([Parameter(Mandatory = $true)][string]$Name)
  $command = Get-Command $Name -ErrorAction SilentlyContinue
  if (-not $command) {
    throw "Required command not found: $Name"
  }
  return $command.Source
}

function Join-ProcessArguments {
  param([Parameter(Mandatory = $true)][string[]]$Arguments)
  $escaped = foreach ($arg in $Arguments) {
    if ($arg -match '[\s"]') {
      '"' + ($arg -replace '"', '\"') + '"'
    } else {
      $arg
    }
  }
  return ($escaped -join ' ')
}

function Invoke-Checked {
  param(
    [Parameter(Mandatory = $true)][string]$FilePath,
    [Parameter(Mandatory = $true)][string[]]$Arguments,
    [Parameter(Mandatory = $true)][string]$WorkingDirectory,
    [string]$StandardInput
  )

  Push-Location $WorkingDirectory
  try {
    if ($null -ne $StandardInput) {
      $StandardInput | & $FilePath @Arguments
    } else {
      & $FilePath @Arguments
    }
    $exitCode = $LASTEXITCODE
  } finally {
    Pop-Location
  }

  if ($exitCode -ne 0) {
    throw "Command failed with exit code $exitCode`: $FilePath $($Arguments -join ' ')"
  }
}

function Get-JavaVersionText {
  $psi = [System.Diagnostics.ProcessStartInfo]::new()
  $psi.FileName = 'java.exe'
  $psi.Arguments = '-version'
  $psi.UseShellExecute = $false
  $psi.RedirectStandardOutput = $true
  $psi.RedirectStandardError = $true
  $process = [System.Diagnostics.Process]::Start($psi)
  $stdout = $process.StandardOutput.ReadToEnd()
  $stderr = $process.StandardError.ReadToEnd()
  $process.WaitForExit()
  return (($stderr + ' ' + $stdout) -replace '\s+', ' ').Trim()
}

function Get-FrontendIndexAsset {
  param([Parameter(Mandatory = $true)][string]$Url)
  $cacheBustUrl = $Url
  if ($cacheBustUrl.Contains('?')) {
    $cacheBustUrl = "${cacheBustUrl}&v=$([DateTimeOffset]::Now.ToUnixTimeSeconds())"
  } else {
    $cacheBustUrl = "${cacheBustUrl}?v=$([DateTimeOffset]::Now.ToUnixTimeSeconds())"
  }
  $response = Invoke-WebRequest -UseBasicParsing -Uri $cacheBustUrl -Headers @{
    'Cache-Control' = 'no-cache'
    'Pragma' = 'no-cache'
  }
  $match = [regex]::Match($response.Content, 'assets/index-[A-Za-z0-9_-]+\.js')
  if (-not $match.Success) {
    throw "Could not find frontend index asset in $Url"
  }
  return $match.Value
}

function Test-BackendSchema {
  $url = "${BackendApiDocsUrl}?ts=$([DateTimeOffset]::Now.ToUnixTimeMilliseconds())"
  $json = (Invoke-WebRequest -UseBasicParsing -Uri $url -Headers @{
    'Cache-Control' = 'no-cache'
    'Pragma' = 'no-cache'
  }).Content | ConvertFrom-Json
  $schemas = $json.components.schemas
  $bankCard = ($schemas.PSObject.Properties | Where-Object { $_.Name -match 'BankCardVO' } | Select-Object -First 1).Value
  $bankDist = ($schemas.PSObject.Properties | Where-Object { $_.Name -match 'BankDistVO' } | Select-Object -First 1).Value
  if ($null -eq $bankCard -or $null -eq $bankDist) {
    return $false
  }
  $bankCardProps = @($bankCard.properties.PSObject.Properties.Name)
  $bankDistProps = @($bankDist.properties.PSObject.Properties.Name)
  return (
    $bankCardProps -contains 'topUserId' -and
    $bankCardProps -contains 'topUserName' -and
    $bankDistProps -contains 'creditCardCount' -and
    $bankDistProps -contains 'debitCardCount'
  )
}

Write-Step 'Checking project and tools'
if (-not (Test-Path $ProjectRoot)) { throw "Project root not found: $ProjectRoot" }
if (-not (Test-Path $Jdk21Home)) { throw "JDK 21 home not found: $Jdk21Home" }
$CloudBaseCmd = Require-Command 'cloudbase.cmd'
$NodeExe = Require-Command 'node.exe'
$NpmCmd = Require-Command 'npm.cmd'
$MvnCmd = Require-Command 'mvn.cmd'

$env:JAVA_HOME = $Jdk21Home
$env:Path = "$Jdk21Home\bin;$env:Path"

Write-Host "Project: $ProjectRoot"
Write-Host "CloudBase env: $EnvId"
Write-Host "CloudRun service: $CloudRunService"
Write-Host "Node: $NodeExe"
Write-Host "NPM: $NpmCmd"
Write-Host "Maven: $MvnCmd"
Write-Host "Java: $(Get-JavaVersionText)"

Write-Step 'Building frontend'
Invoke-Checked -FilePath $NpmCmd -Arguments @('run', 'build') -WorkingDirectory $FrontendDir

if (-not (Test-Path (Join-Path $FrontendDist 'index.html'))) {
  throw "Frontend build output missing: $FrontendDist"
}
$localIndexHtml = Get-Content -Raw (Join-Path $FrontendDist 'index.html')
$localIndexMatch = [regex]::Match($localIndexHtml, 'assets/index-[A-Za-z0-9_-]+\.js')
if (-not $localIndexMatch.Success) {
  throw 'Could not find local frontend index asset after build.'
}
$localIndexAsset = $localIndexMatch.Value
Write-Host "Local frontend index asset: $localIndexAsset"

Write-Step 'Building backend'
Invoke-Checked -FilePath $MvnCmd -Arguments @('clean', 'package', '-DskipTests') -WorkingDirectory $BackendDir

if (-not (Test-Path (Join-Path $BackendDir 'target\bank-admin-1.0.0.jar'))) {
  throw 'Backend jar was not created.'
}

Write-Step 'Deploying backend to CloudRun'
Invoke-Checked `
  -FilePath $CloudBaseCmd `
  -Arguments @('cloudrun', 'deploy', '-s', $CloudRunService, '--source', '.', '--port', [string]$CloudRunPort, '--force', '-e', $EnvId) `
  -WorkingDirectory $BackendDir `
  -StandardInput "`nY"

Write-Step 'Refreshing static hosting assets'
Invoke-Checked `
  -FilePath $CloudBaseCmd `
  -Arguments @('hosting', 'delete', 'assets', '--dir', '-e', $EnvId) `
  -WorkingDirectory $FrontendDir `
  -StandardInput ''

Write-Step 'Deploying frontend static files'
Invoke-Checked `
  -FilePath $CloudBaseCmd `
  -Arguments @('hosting', 'deploy', 'dist', '/', '-e', $EnvId) `
  -WorkingDirectory $FrontendDir

Write-Step 'Verifying frontend domains'
foreach ($url in $FrontendUrls) {
  $onlineAsset = Get-FrontendIndexAsset -Url $url
  Write-Host "$url -> $onlineAsset"
  if ($onlineAsset -ne $localIndexAsset) {
    throw "Frontend index asset mismatch for $url. Expected $localIndexAsset, got $onlineAsset"
  }
}

Write-Step 'Waiting for backend schema'
$deadline = (Get-Date).AddMinutes(8)
$schemaReady = $false
do {
  if (Test-BackendSchema) {
    $schemaReady = $true
    break
  }
  Write-Host "$(Get-Date -Format HH:mm:ss) backend schema not updated yet; waiting..."
  Start-Sleep -Seconds 20
} while ((Get-Date) -lt $deadline)

if (-not $schemaReady) {
  throw "Backend schema did not expose required fields within timeout: $($RequiredBackendFields -join ', ')"
}

Write-Step 'Checking CloudRun status'
Invoke-Checked `
  -FilePath $CloudBaseCmd `
  -Arguments @('cloudrun', 'list', '-e', $EnvId, '--serviceName', $CloudRunService) `
  -WorkingDirectory $BackendDir

Write-Step 'Deployment complete'
Write-Host "Frontend index asset: $localIndexAsset" -ForegroundColor Green
Write-Host "Backend schema contains: $($RequiredBackendFields -join ', ')" -ForegroundColor Green
Write-Host 'Done.' -ForegroundColor Green
