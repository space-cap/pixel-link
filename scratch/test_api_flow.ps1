# PowerShell API Key & Short Link API Integration Test Script
# Safe ASCII-only script to avoid Windows locales encoding errors

$serverUrl = "http://localhost:8090"
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "Pixel-Link API Key & External API Verification" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# 1. API Key Request
Write-Host "[1/3] Requesting new API Key..." -ForegroundColor Yellow
$keyBody = @{
    userId = "test-user"
    name = "PowerShell-Verification-Key"
} | ConvertTo-Json

try {
    $keyResponse = Invoke-RestMethod -Uri "$serverUrl/api/dashboard/keys" -Method Post -ContentType "application/json;charset=UTF-8" -Body $keyBody
    $apiKey = $keyResponse.apiKey
    $apiKeyId = $keyResponse.id
    Write-Host "API Key Issued Successfully! -> $apiKey (Key ID: $apiKeyId)" -ForegroundColor Green
} catch {
    Write-Error "API Key Issuance Failed: $_"
    Exit
}

# 2. Call Short Url creation API using X-API-KEY header
Write-Host ""
Write-Host "[2/3] Calling External Link API..." -ForegroundColor Yellow
$linkBody = @{
    defaultTargetUrl = "https://github.com/space-cap/pixel-link"
    title = "Pixel Link GitHub"
    description = "SaaS link routing solution repository"
    isAdEnabled = $true
    adTimerSeconds = 5
} | ConvertTo-Json

$headers = @{
    "X-API-KEY" = $apiKey
}

try {
    $linkResponse = Invoke-RestMethod -Uri "$serverUrl/api/v1/links" -Method Post -ContentType "application/json;charset=UTF-8" -Headers $headers -Body $linkBody
    Write-Host "Short Link Created Successfully!" -ForegroundColor Green
    Write-Host "  - Short Code: $($linkResponse.code)"
    Write-Host "  - Short URL: $($linkResponse.shortUrl)"
    Write-Host "  - Original URL: $($linkResponse.originalUrl)"
    Write-Host "  - Ad Enabled: $($linkResponse.isAdEnabled)"
} catch {
    Write-Error "Short Link Creation Failed: $_"
}

# 3. Revoke the API Key
Write-Host ""
Write-Host "[3/3] Revoking the temporary API Key..." -ForegroundColor Yellow
try {
    $null = Invoke-RestMethod -Uri "$serverUrl/api/dashboard/keys/$apiKeyId" -Method Delete
    Write-Host "API Key Revoked Successfully! (Test environment cleaned)" -ForegroundColor Green
} catch {
    Write-Warning "API Key Revocation Failed: $_"
}

Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "API Integration Verification Done" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
