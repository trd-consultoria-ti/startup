$apiKey = 'job-hunter-evolution-2026'
$baseUrl = 'http://localhost:8080'

Write-Host '=== Evolution API — WhatsApp Test ===' -ForegroundColor Cyan

# 1. List bots
Write-Host ''
$body = @{ instanceName = 'thiago-job-hunter' } | ConvertTo-Json
try {
  $bots = Invoke-RestMethod -Uri "$baseUrl/anthropic/find" -Method Post -Headers @{ apikey = $apiKey } -ContentType 'application/json' -Body $body
  $bots | ConvertTo-Json -Depth 5
} catch {
  Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# 2. Fetch settings
Write-Host ''
try {
  $settings = Invoke-RestMethod -Uri "$baseUrl/anthropic/fetchSettings" -Method Post -Headers @{ apikey = $apiKey } -ContentType 'application/json' -Body $body
  $settings | ConvertTo-Json -Depth 5
} catch {
  Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# 3. Reminders stats
Write-Host ''
try {
  $stats = Invoke-RestMethod -Uri "$baseUrl/anthropic/reminders/stats" -Headers @{ apikey = $apiKey }
  $stats | ConvertTo-Json -Depth 3
} catch {
  Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# 4. Agent status
Write-Host ''
try {
  $status = Invoke-RestMethod -Uri "$baseUrl/anthropic/agents/status" -Headers @{ apikey = $apiKey }
  $status | ConvertTo-Json -Depth 3
} catch {
  Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# 5. Agent metrics
Write-Host ''
try {
  $metrics = Invoke-RestMethod -Uri "$baseUrl/anthropic/metrics" -Headers @{ apikey = $apiKey }
  $metrics | ConvertTo-Json -Depth 3
} catch {
  Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# 6. Traces
Write-Host ''
try {
  $traces = Invoke-RestMethod -Uri "$baseUrl/anthropic/traces" -Headers @{ apikey = $apiKey }
  $traces | ConvertTo-Json -Depth 3
} catch {
  Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host '=== Done ===' -ForegroundColor Cyan
