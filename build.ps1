param(
    [string]$JavaHome
)

$ErrorActionPreference = 'Stop'
$javaCompilerPath = $null

if ($JavaHome) {
    $javaCompilerPath = Join-Path $JavaHome 'bin\javac.exe'
    if (-not (Test-Path -LiteralPath $javaCompilerPath)) {
        throw "javac.exe was not found in $JavaHome. Use a JDK, version 17 or later."
    }
} elseif ($env:JAVA_HOME) {
    $configuredCompilerPath = Join-Path $env:JAVA_HOME 'bin\javac.exe'
    if (Test-Path -LiteralPath $configuredCompilerPath) {
        $javaCompilerPath = $configuredCompilerPath
    }
}

if (-not $javaCompilerPath) {
    $compilerCommand = Get-Command javac -ErrorAction SilentlyContinue
    if ($compilerCommand) {
        $javaCompilerPath = $compilerCommand.Source
    }
}

if (-not $javaCompilerPath) {
    throw 'Java JDK 17 or later is required. Set JAVA_HOME, add its bin folder to PATH, or pass -JavaHome <JDK directory>.'
}

$classOutputDirectory = Join-Path $PSScriptRoot 'build\classes'
$sourceListFile = Join-Path $PSScriptRoot 'build\sources.txt'
$sourceDirectory = Join-Path $PSScriptRoot 'src'

New-Item -ItemType Directory -Path $classOutputDirectory -Force | Out-Null
$sourceFiles = Get-ChildItem -LiteralPath $sourceDirectory -Recurse -File -Filter '*.java' |
    Sort-Object FullName

$quotedSourcePaths = @(
    $sourceFiles | ForEach-Object {
        $sourcePath = $_.FullName.Replace('\', '/')
        '"' + $sourcePath + '"'
    }
)
$utf8WithoutBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllLines($sourceListFile, [string[]]$quotedSourcePaths, $utf8WithoutBom)

$compilerArguments = @(
    '-J-Dfile.encoding=UTF-8'
    '--release', '17'
    '-encoding', 'UTF-8'
    '-d', $classOutputDirectory
    ('@' + $sourceListFile)
)

& $javaCompilerPath @compilerArguments
if ($LASTEXITCODE -ne 0) {
    throw "Java compilation failed (exit code $LASTEXITCODE)."
}

Write-Output "Compiled $($sourceFiles.Count) Java source files for Java 17."
