@echo off
setlocal

REM ============================================================
REM ExportJavaKotlin.cmd
REM
REM Creates a ZIP containing every .java and .kt file beneath
REM this script's directory, preserving repo-relative paths.
REM
REM Skips common generated/cache folders:
REM   .git, .gradle, .idea, build, out, target, bin
REM ============================================================

set "REPOROOT=%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference = 'Stop';" ^
  "$root = [System.IO.Path]::GetFullPath('%REPOROOT%');" ^
  "$stamp = Get-Date -Format 'yyyyMMdd-HHmmss';" ^
  "$zip = Join-Path $root ('JavaKotlinSource_' + $stamp + '.zip');" ^
  "$stage = Join-Path ([System.IO.Path]::GetTempPath()) ('JavaKotlinSource_' + [guid]::NewGuid().ToString('N'));" ^
  "$excluded = @('.git','.gradle','.idea','build','out','target','bin');" ^
  "New-Item -ItemType Directory -Path $stage -Force | Out-Null;" ^
  "try {" ^
  "  $files = Get-ChildItem -LiteralPath $root -Recurse -File | Where-Object {" ^
  "    ($_.Extension -ieq '.java' -or $_.Extension -ieq '.kt' -or $_.Extension -ieq '.png') -and" ^
  "    -not ($_.FullName.Substring($root.Length).TrimStart('\').Split('\') | Where-Object { $excluded -contains $_ })" ^
  "  };" ^
  "  if (-not $files) { throw 'No .java or .kt files were found.' };" ^
  "  foreach ($file in $files) {" ^
  "    $relative = $file.FullName.Substring($root.Length).TrimStart('\');" ^
  "    $dest = Join-Path $stage $relative;" ^
  "    $destDir = Split-Path -Parent $dest;" ^
  "    New-Item -ItemType Directory -Path $destDir -Force | Out-Null;" ^
  "    Copy-Item -LiteralPath $file.FullName -Destination $dest -Force;" ^
  "  };" ^
  "  Compress-Archive -Path (Join-Path $stage '*') -DestinationPath $zip -CompressionLevel Optimal -Force;" ^
  "  Write-Host '';" ^
  "  Write-Host ('Created: ' + $zip) -ForegroundColor Green;" ^
  "  Write-Host ('Files:   ' + $files.Count) -ForegroundColor Cyan;" ^
  "} finally {" ^
  "  if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }" ^
  "}"

if errorlevel 1 (
    echo.
    echo Export failed.
    pause
    exit /b 1
)

echo.
pause
endlocal
