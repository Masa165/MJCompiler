$ErrorActionPreference = "Stop"

Write-Host "=== Building MicroJava Compiler ===" -ForegroundColor Cyan

# Project root = parent directory of scripts/
$Root = Split-Path -Parent $PSScriptRoot

$Src = Join-Path $Root "src"
$Spec = Join-Path $Src "spec"
$Package = Join-Path $Src "rs\ac\bg\etf\pp1"
$Bin = Join-Path $Root "bin"
$Lib = Join-Path $Root "lib"

# Libraries
$JFlex = Join-Path $Lib "JFlex.jar"
$Cup = Join-Path $Lib "cup_v10k.jar"
$Log4j = Join-Path $Lib "log4j-1.2.17.jar"
$SymbolTable = Join-Path $Lib "symboltable.jar"
$Runtime = Join-Path $Lib "mj-runtime.jar"

Write-Host "`n[1/4] Generating lexer with JFlex..." -ForegroundColor Yellow

java -jar $JFlex `
    -d $Package `
    (Join-Path $Spec "mjlexer.flex")

if ($LASTEXITCODE -ne 0) {
    throw "JFlex generation failed."
}

Write-Host "`n[2/4] Generating parser and AST with CUP..." -ForegroundColor Yellow

java -jar $Cup `
    -destdir $Package `
    -ast src.rs.ac.bg.etf.pp1.ast `
    -expect 5 `
    -buildtree `
    (Join-Path $Spec "mjparser.cup")

if ($LASTEXITCODE -ne 0) {
    throw "CUP generation failed."
}

Write-Host "`n[3/4] Fixing generated package references..." -ForegroundColor Yellow

Get-ChildItem -Path $Src -Recurse -Filter *.java |
    ForEach-Object {
        $content = Get-Content $_.FullName -Raw

        $content = $content -replace `
            'src\.rs\.ac\.bg\.etf\.pp1\.ast', `
            'rs.ac.bg.etf.pp1.ast'

        Set-Content $_.FullName $content -NoNewline
    }

Write-Host "`n[4/4] Compiling Java sources..." -ForegroundColor Yellow

# Create bin/ if it doesn't exist
New-Item -ItemType Directory -Force -Path $Bin | Out-Null

# Collect all Java source files
$JavaFiles = Get-ChildItem -Path $Src -Recurse -Filter *.java |
    ForEach-Object { $_.FullName }

$Classpath = "$Cup;$Log4j;$SymbolTable;$Runtime"

javac `
    -cp $Classpath `
    -d $Bin `
    $JavaFiles

if ($LASTEXITCODE -ne 0) {
    throw "Java compilation failed."
}

Write-Host "`n=== BUILD SUCCESSFUL ===" -ForegroundColor Green
Write-Host "Compiled classes are in: bin/"