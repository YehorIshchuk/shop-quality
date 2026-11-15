# SonarQube Web API через Invoke-RestMethod (Windows). Сирі JSON -> reports\sonar-raw\ps\
if (-not $env:SONAR_TOKEN) { throw "Set `$env:SONAR_TOKEN" }
$hostUrl = if ($env:SONAR_HOST_URL) { $env:SONAR_HOST_URL } else { "http://localhost:9000" }
$auth = @{ Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$($env:SONAR_TOKEN):")) }
$p = "shop-quality"
$out = "reports\sonar-raw\ps"; New-Item -ItemType Directory -Force $out | Out-Null
$m = "complexity,cognitive_complexity,ncloc,functions,code_smells,sqale_index"

foreach ($f in "domain/Order", "infrastructure/FileOrderRepository", "dto/OrderDto") {
    $uri = "$hostUrl/api/measures/component?component=${p}:src/main/java/com/kira/shop/$f.java&metricKeys=$m"
    Write-Host "GET $uri" -ForegroundColor Cyan
    $r = Invoke-RestMethod -Uri $uri -Headers $auth
    $r | ConvertTo-Json -Depth 6 | Tee-Object -FilePath "$out\$(Split-Path $f -Leaf).json"
}
try {
    Invoke-RestMethod -Uri "$hostUrl/api/measures/component?component=${p}:src/main/java/com/kira/shop/domain/Order.java&metricKeys=class_complexity,coupling_between_objects,lcom4" -Headers $auth
} catch {
    Write-Host "CK legacy keys -> $($_.ErrorDetails.Message)" -ForegroundColor Yellow
    $_.ErrorDetails.Message | Out-File "$out\ck-legacy-keys.json"
}
