[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Launcher,

    [Parameter(Mandatory = $true)]
    [string]$LegalDirectory,

    [ValidateRange(1024, 65535)]
    [int]$Port = 18911
)

$ErrorActionPreference = "Stop"
$smokeDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("heapscout-package-smoke-" + [guid]::NewGuid())
$standardOutput = Join-Path $smokeDirectory "heapscout.stdout.log"
$standardError = Join-Path $smokeDirectory "heapscout.stderr.log"
$baseUrl = "http://127.0.0.1:$Port"
$heapScoutProcess = $null

function Write-HeapScoutLog {
    if (Test-Path -LiteralPath $standardOutput -PathType Leaf) {
        Write-Error "HeapScout standard output:`n$(Get-Content -LiteralPath $standardOutput -Raw)" -ErrorAction Continue
    }
    if (Test-Path -LiteralPath $standardError -PathType Leaf) {
        Write-Error "HeapScout standard error:`n$(Get-Content -LiteralPath $standardError -Raw)" -ErrorAction Continue
    }
}

try {
    if (-not (Test-Path -LiteralPath $Launcher -PathType Leaf)) {
        throw "Launcher is missing: $Launcher"
    }
    if (-not (Test-Path -LiteralPath (Join-Path $LegalDirectory "LICENSE") -PathType Leaf)) {
        throw "LICENSE is missing from the application image"
    }
    if (-not (Test-Path -LiteralPath (Join-Path $LegalDirectory "THIRD_PARTY_NOTICES.md") -PathType Leaf)) {
        throw "THIRD_PARTY_NOTICES.md is missing from the application image"
    }

    New-Item -ItemType Directory -Path $smokeDirectory | Out-Null
    $heapScoutProcess = Start-Process `
        -FilePath $Launcher `
        -ArgumentList @("--heapscout.open-browser=false", "--server.port=$Port") `
        -RedirectStandardOutput $standardOutput `
        -RedirectStandardError $standardError `
        -PassThru

    $readyResponse = $null
    for ($attempt = 1; $attempt -le 60; $attempt++) {
        $heapScoutProcess.Refresh()
        if ($heapScoutProcess.HasExited) {
            throw "Packaged process exited before the API became ready"
        }

        try {
            $candidate = Invoke-WebRequest -Uri "$baseUrl/api/dumps" -TimeoutSec 2
            if ($candidate.StatusCode -eq 200) {
                $readyResponse = $candidate
                break
            }
        } catch {
            # Connection failures are expected while the packaged JVM starts.
        }
        Start-Sleep -Seconds 1
    }

    if ($null -eq $readyResponse) {
        throw "API did not become ready within 60 seconds"
    }
    if ($readyResponse.Content.Trim() -ne "[]") {
        throw "Fresh package returned an unexpected job list"
    }

    $uiResponse = Invoke-WebRequest -Uri "$baseUrl/" -TimeoutSec 5
    if ($uiResponse.StatusCode -ne 200 -or $uiResponse.Content -notmatch "<title>HeapScout</title>") {
        throw "Bundled UI verification failed"
    }

    $rebindStatus = & curl.exe `
        --silent `
        --output NUL `
        --write-out "%{http_code}" `
        --header "Host: attacker.example" `
        --max-time 5 `
        "$baseUrl/api/dumps"
    if ($LASTEXITCODE -ne 0 -or $rebindStatus.Trim() -ne "421") {
        throw "Non-loopback Host was not rejected with HTTP 421"
    }

    Write-Output "Package smoke test passed: $Launcher"
} catch {
    Write-HeapScoutLog
    throw
} finally {
    if ($null -ne $heapScoutProcess) {
        $heapScoutProcess.Refresh()
        if (-not $heapScoutProcess.HasExited) {
            Stop-Process -Id $heapScoutProcess.Id -Force
            $heapScoutProcess.WaitForExit()
        }
    }
    if (Test-Path -LiteralPath $smokeDirectory -PathType Container) {
        Remove-Item -LiteralPath $smokeDirectory -Recurse -Force
    }
}
