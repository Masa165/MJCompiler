$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot

$Lib = Join-Path $Root "lib"
$Tests = Join-Path $Root "tests"

$Runtime = Join-Path $Lib "mj-runtime.jar"
$ObjectFile = Join-Path $Tests "test.obj"

Write-Host "=== MicroJava Bytecode Disassembler ===" -ForegroundColor Cyan

if (-not (Test-Path $ObjectFile)) {
    Write-Host "test.obj does not exist. Building and compiling test.mj first..." -ForegroundColor Yellow

    & (Join-Path $PSScriptRoot "run.ps1") "tests\test.mj"

    if ($LASTEXITCODE -ne 0) {
        throw "Could not generate test.obj."
    }
}

Write-Host "`nDisassembly of ${ObjectFile}:`n" -ForegroundColor Yellow

java `
    -cp $Runtime `
    rs.etf.pp1.mj.runtime.disasm `
    $ObjectFile

if ($LASTEXITCODE -ne 0) {
    throw "Disassembly failed."
}

Write-Host "`n=== DISASSEMBLY COMPLETE ===" -ForegroundColor Green