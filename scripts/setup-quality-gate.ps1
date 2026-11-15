# Створює кастомний Quality Gate "Shop-Quality-Gate" і прив'язує його до проєкту (Windows / PowerShell).
# Використання:  $env:SONAR_TOKEN="squ_xxx"; .\scripts\setup-quality-gate.ps1
$ErrorActionPreference = "Continue"
if (-not $env:SONAR_TOKEN) { throw "Set `$env:SONAR_TOKEN" }
$hostUrl = if ($env:SONAR_HOST_URL) { $env:SONAR_HOST_URL } else { "http://localhost:9000" }
$gate = "Shop-Quality-Gate"
$project = "shop-quality"
$auth = @{ Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$($env:SONAR_TOKEN):")) }

function Invoke-Sonar([string]$path, [hashtable]$body) {
    try {
        $r = Invoke-RestMethod -Method Post -Uri "$hostUrl/api/$path" -Headers $auth -Body $body
        Write-Host "OK  $path $($body | ConvertTo-Json -Compress)" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "ERR $path $($body | ConvertTo-Json -Compress): $($_.ErrorDetails.Message)" -ForegroundColor Yellow
        return $false
    }
}
function Add-Condition($metric, $op, $err) {
    Invoke-Sonar "qualitygates/create_condition" @{ gateName = $gate; metric = $metric; op = $op; error = $err }
}

Invoke-Sonar "projects/create" @{ name = "Shop Quality"; project = $project } | Out-Null
Invoke-Sonar "qualitygates/create" @{ name = $gate } | Out-Null

# Категорія 1: New Code
if (-not (Add-Condition "new_bugs" "GT" "0")) { Add-Condition "new_reliability_rating" "GT" "1" | Out-Null }
Add-Condition "new_security_hotspots_reviewed" "LT" "100" | Out-Null
# Категорія 2: Overall Code
Add-Condition "duplicated_lines_density" "GT" "3" | Out-Null
Add-Condition "sqale_rating" "GT" "1" | Out-Null

Invoke-Sonar "qualitygates/select" @{ gateName = $gate; projectKey = $project } | Out-Null
Invoke-Sonar "new_code_periods/set" @{ project = $project; type = "PREVIOUS_VERSION" } | Out-Null

Invoke-RestMethod -Uri "$hostUrl/api/qualitygates/show?name=$gate" -Headers $auth | ConvertTo-Json -Depth 5
