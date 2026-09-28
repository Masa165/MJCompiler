param(
    [Parameter(Mandatory = $true)]
    [string]$TestFile
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot

$Lib = Join-Path $Root "lib"
$Bin = Join-Path $Root "bin"

$Cup = Join-Path $Lib "cup_v10k.jar"
$Log4j = Join-Path $Lib "log4j-1.2.17.jar"
$SymbolTable = Join-Path $Lib "symboltable.jar"
$Runtime = Join-Path $Lib "mj-runtime.jar"

# Convert test path to absolute path
$InputFile = Join-Path $Root $TestFile

if (-not (Test-Path $InputFile)) {
    throw "Test file not found: $InputFile"
}

# Create .obj path next to the .mj file
$OutputFile = [System.IO.Path]::ChangeExtension($InputFile, ".obj")

Write-Host "=== Running MicroJava Test ===" -ForegroundColor Cyan
Write-Host "Input : $InputFile"
Write-Host "Output: $OutputFile"

# Build compiler
Write-Host "`n[1/2] Building compiler..." -ForegroundColor Yellow

& (Join-Path $PSScriptRoot "build.ps1")

if ($LASTEXITCODE -ne 0) {
    throw "Build failed."
}

# Compile MicroJava source
Write-Host "`n[2/2] Compiling and running test..." -ForegroundColor Yellow

$Classpath = "$Bin;$Cup;$Log4j;$SymbolTable;$Runtime"

java `
    -cp $Classpath `
    rs.ac.bg.etf.pp1.Compiler `
    $InputFile `
    $OutputFile

if ($LASTEXITCODE -ne 0) {
    throw "MicroJava compilation failed."
}

Write-Host "`n=== Program Output ===" -ForegroundColor Green

java `
    -cp $Runtime `
    rs.etf.pp1.mj.runtime.Run `
    $OutputFile

if ($LASTEXITCODE -ne 0) {
    throw "MicroJava program execution failed."
}

Write-Host "`n=== TEST FINISHED ===" -ForegroundColor Green