param(
    [string]$JavaHome,
    [switch]$Verify,
    [switch]$Synthetic,
    [switch]$Quadratic,
    [switch]$Compare,
    [switch]$Real,
    [string]$DataPath
)

$ErrorActionPreference = 'Stop'
$requestedModes = @(
    $Verify.IsPresent
    $Synthetic.IsPresent
    $Quadratic.IsPresent
    $Compare.IsPresent
    $Real.IsPresent
)

$selectedModeCount = 0
foreach ($modeIsSelected in $requestedModes) {
    if ($modeIsSelected) {
        $selectedModeCount++
    }
}

if ($selectedModeCount -gt 1) {
    throw 'Choose only one demonstration mode.'
}
if ($DataPath -and -not $Real) {
    throw '-DataPath is only supported with -Real.'
}

& (Join-Path $PSScriptRoot 'build.ps1') -JavaHome $JavaHome

if ($JavaHome) {
    $javaRuntimePath = Join-Path $JavaHome 'bin\java.exe'
} elseif ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
    $javaRuntimePath = Join-Path $env:JAVA_HOME 'bin\java.exe'
} else {
    $compilerCommand = Get-Command javac -ErrorAction Stop
    $javaRuntimePath = Join-Path (Split-Path -Parent $compilerCommand.Source) 'java.exe'
}

$javaRuntimePath = (Resolve-Path -LiteralPath $javaRuntimePath -ErrorAction Stop).Path
$programArguments = @()

if ($Verify) {
    $programArguments = @('--verify')
} elseif ($Synthetic) {
    $programArguments = @('--synthetic')
} elseif ($Quadratic) {
    $programArguments = @('--quadratic')
} elseif ($Compare) {
    $programArguments = @('--compare')
} elseif ($Real) {
    $programArguments = @('--real')
    if ($DataPath) {
        $absoluteDataPath = (Resolve-Path -LiteralPath $DataPath -ErrorAction Stop).Path
        $programArguments += $absoluteDataPath
    }
}

$classOutputDirectory = Join-Path $PSScriptRoot 'build\classes'
$mainClass = 'auk.sdt202.team4.app.RetentionRadarApp'
$runtimeArguments = @(
    '-Dfile.encoding=UTF-8'
    '-cp', $classOutputDirectory
    $mainClass
) + $programArguments

Push-Location -LiteralPath $PSScriptRoot
try {
    & $javaRuntimePath @runtimeArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Program failed (exit code $LASTEXITCODE)."
    }
} finally {
    Pop-Location
}
