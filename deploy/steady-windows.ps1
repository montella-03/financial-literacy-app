param([switch]$OpenBrowser)
$ErrorActionPreference = 'Stop'
$homeDir = $PSScriptRoot
$sid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
$mutex = New-Object System.Threading.Mutex($false, ('Local\Steady-' + $sid))
$locked = $false
$child = $null
try {
    try { $locked = $mutex.WaitOne(0) } catch [System.Threading.AbandonedMutexException] { $locked = $true }
    if ($locked) {
        $settings = Get-Content (Join-Path $homeDir 'steady.json') -Raw | ConvertFrom-Json
        foreach ($property in $settings.PSObject.Properties) {
            [Environment]::SetEnvironmentVariable($property.Name, [string]$property.Value, 'Process')
        }
        $java = (Get-Content (Join-Path $homeDir 'java-path.txt') -Raw).Trim()
        $child = Start-Process $java -ArgumentList @('-Xms128m', '-Xmx512m', '-jar', ('"' + (Join-Path $homeDir 'steady.jar') + '"')) -WorkingDirectory $homeDir -WindowStyle Hidden -RedirectStandardOutput (Join-Path $homeDir 'stdout.log') -RedirectStandardError (Join-Path $homeDir 'stderr.log') -PassThru
    }
    if ($OpenBrowser) {
        for ($attempt = 0; $attempt -lt 90; $attempt++) {
            if (Test-Path (Join-Path $homeDir 'stop')) { break }
            try {
                Invoke-WebRequest 'http://127.0.0.1:8808/login' -UseBasicParsing -TimeoutSec 2 | Out-Null
                Start-Process 'http://localhost:8808'
                break
            } catch { Start-Sleep -Seconds 1 }
        }
    }
    if ($child) {
        while (!$child.HasExited) {
            if (Test-Path (Join-Path $homeDir 'stop')) { break }
            Start-Sleep -Seconds 1
            $child.Refresh()
        }
    }
} catch {
    $_ | Out-String | Add-Content (Join-Path $homeDir 'launcher.log')
} finally {
    if ($child -and !$child.HasExited) { $child.Kill(); $child.WaitForExit() }
    if ($locked) {
        Remove-Item (Join-Path $homeDir 'stop') -ErrorAction SilentlyContinue
        $mutex.ReleaseMutex()
    }
    $mutex.Dispose()
}
