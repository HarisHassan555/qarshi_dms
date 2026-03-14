$filePath = "VIM-BE\src\main\java\com\bezkoder\spring\login\sa\dal\daoimpl\CfgTblCustomFormApplicationDAO.java"
$content = Get-Content $filePath -Raw

# Fix remaining prevDeptId references that should be targetDeptId
$content = $content -replace 'prevDeptId', 'targetDeptId'

Set-Content -Path $filePath -Value $content -NoNewline
Write-Host "Fixed compilation errors"
