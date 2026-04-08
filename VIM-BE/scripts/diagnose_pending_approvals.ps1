# Call staging pending APIs from Windows (PowerShell).
# Get JWT: browser -> DevTools -> Application -> Local Storage -> token (or Network tab on any API request -> Authorization header).
#
# Example:
#   $env:BASE = "https://staging.example.com/velocity"
#   $env:TOKEN = "Bearer eyJhbG..."
#   .\diagnose_pending_approvals.ps1

$Base = if ($env:BASE) { $env:BASE.TrimEnd('/') } else { "http://localhost:8080/velocity" }
$Token = $env:TOKEN

$headers = @{}
if ($Token) { $headers["Authorization"] = $Token }

Write-Host "GET $Base/getAllApplicationsPendingApproval"
try {
    $r = Invoke-WebRequest -Uri "$Base/getAllApplicationsPendingApproval" -Headers $headers -UseBasicParsing -ErrorAction Stop
    Write-Host "Status:" $r.StatusCode
    Write-Host ($r.Content.Substring(0, [Math]::Min(500, $r.Content.Length)))
} catch {
    Write-Host "Failed:" $_.Exception.Message
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host $reader.ReadToEnd()
    }
}

Write-Host "`nGET $Base/getApplicationsPendingApproval?departmentHeadUserId=1"
try {
    $r2 = Invoke-WebRequest -Uri "$Base/getApplicationsPendingApproval?departmentHeadUserId=1" -Headers $headers -UseBasicParsing -ErrorAction Stop
    Write-Host "Status:" $r2.StatusCode
} catch {
    Write-Host "Failed:" $_.Exception.Message
}
