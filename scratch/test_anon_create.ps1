# 1. 비로그인 상태에서 /info/faq 검증
$anonRes = Invoke-WebRequest -Uri "http://localhost:8090/info/faq?lang=ko" -MaximumRedirection 0 -ErrorAction SilentlyContinue
$anonHtml = $anonRes.Content

# HTML 일부 출력하여 한글 깨짐이나 다국어 바인딩 결과 확인
Write-Host "--- ANON HTML PREVIEW ---"
Write-Host ($anonHtml.Substring(0, [Math]::Min(1500, $anonHtml.Length)))
Write-Host "-------------------------"

# 2. 로그인 세션 획득
$loginUrl = "http://localhost:8090/app/login/process"
$loginBody = @{
    username = "test-verified"
    password = "pass1234"
}
$loginRes = Invoke-WebRequest -Uri $loginUrl -Method Post -Body $loginBody -SessionVariable sess -MaximumRedirection 0 -ErrorAction SilentlyContinue

# 3. 로그인 상태에서 /info/faq 검증
$memberRes = Invoke-WebRequest -Uri "http://localhost:8090/info/faq?lang=ko" -WebSession $sess
$memberHtml = $memberRes.Content

Write-Host "--- MEMBER HTML PREVIEW ---"
Write-Host ($memberHtml.Substring(0, [Math]::Min(1500, $memberHtml.Length)))
Write-Host "---------------------------"
