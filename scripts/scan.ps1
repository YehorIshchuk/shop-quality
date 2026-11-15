# Збирає проєкт, проганяє тести з JaCoCo і відправляє аналіз у SonarQube (Windows / PowerShell).
# Використання:  $env:SONAR_TOKEN="squ_xxx"; .\scripts\scan.ps1
$ErrorActionPreference = "Stop"
if (-not $env:SONAR_TOKEN) { throw "Set `$env:SONAR_TOKEN (My Account -> Security -> Generate token)" }
$hostUrl = if ($env:SONAR_HOST_URL) { $env:SONAR_HOST_URL } else { "http://localhost:9000" }

mvn -B clean verify sonar:sonar `
  "-Dsonar.host.url=$hostUrl" `
  "-Dsonar.token=$($env:SONAR_TOKEN)" `
  "-Dsonar.qualitygate.wait=true"
if ($LASTEXITCODE -ne 0) { Write-Host "BUILD BLOCKED: Quality Gate FAILED or build error" -ForegroundColor Red; exit $LASTEXITCODE }
Write-Host "Quality Gate PASSED" -ForegroundColor Green
