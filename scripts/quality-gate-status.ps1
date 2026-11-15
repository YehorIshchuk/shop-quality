# Статус Quality Gate і ключові метрики (Windows / PowerShell).
if (-not $env:SONAR_TOKEN) { throw "Set `$env:SONAR_TOKEN" }
$hostUrl = if ($env:SONAR_HOST_URL) { $env:SONAR_HOST_URL } else { "http://localhost:9000" }
$auth = @{ Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$($env:SONAR_TOKEN):")) }
$p = "shop-quality"
(Invoke-RestMethod -Uri "$hostUrl/api/qualitygates/project_status?projectKey=$p" -Headers $auth).projectStatus | ConvertTo-Json -Depth 5
$m = "bugs,vulnerabilities,security_hotspots,code_smells,sqale_index,sqale_debt_ratio,duplicated_lines_density,complexity,cognitive_complexity,coverage,ncloc"
(Invoke-RestMethod -Uri "$hostUrl/api/measures/component?component=$p&metricKeys=$m" -Headers $auth).component.measures | Format-Table metric, value
