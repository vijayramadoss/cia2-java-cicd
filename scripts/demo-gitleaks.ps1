# Demo: the Gitleaks pre-commit hook blocks a commit that contains a secret.
# Run from the project folder:   .\scripts\demo-gitleaks.ps1
# Nothing is committed - the fake file is removed at the end.

$ErrorActionPreference = "Continue"
$env:Path += ";$env:LOCALAPPDATA\Microsoft\WinGet\Packages\Gitleaks.Gitleaks_Microsoft.Winget.Source_8wekyb3d8bbwe"

# The fake AWS key is built from two parts so this script itself does not trip Gitleaks.
$fakeKey = "AKIA" + "Z7Q3EXAMPLE4QWRT"
"aws_access_key_id = $fakeKey" | Out-File -Encoding ascii demo-secret.txt

Write-Host ""
Write-Host "1) Created demo-secret.txt containing a fake AWS access key" -ForegroundColor Cyan
Get-Content demo-secret.txt

Write-Host ""
Write-Host "2) Staging it and trying to commit ..." -ForegroundColor Cyan
git add demo-secret.txt
git commit -m "test: try to commit a secret"

Write-Host ""
if ($LASTEXITCODE -ne 0) {
    Write-Host "RESULT: commit was BLOCKED by the Gitleaks pre-commit hook" -ForegroundColor Green
} else {
    Write-Host "RESULT: commit went through - undoing it" -ForegroundColor Red
    git reset --soft HEAD~1
}

Write-Host ""
Write-Host "3) Cleaning up (nothing was committed)" -ForegroundColor Cyan
git reset -q demo-secret.txt
Remove-Item demo-secret.txt
git status --short
