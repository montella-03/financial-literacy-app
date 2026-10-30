# Install the production JAR for the current Windows user; no administrator needed.
$ErrorActionPreference = 'Stop'
if ($env:OS -ne 'Windows_NT') { throw 'This installer requires Windows.' }
$projectDir = Split-Path $PSScriptRoot -Parent
$jar = Join-Path $projectDir 'target\financial-literacy-app-0.0.1-SNAPSHOT.jar'
if (!(Test-Path $jar)) { throw 'Build first: mvn -Pproduction package' }
$java = (Get-Command java.exe -ErrorAction Stop).Source
$homeDir = Join-Path $env:LOCALAPPDATA 'Steady'
New-Item -ItemType Directory -Force -Path $homeDir | Out-Null

# Restrict configuration, logs and application files to this user and SYSTEM.
$sid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
$acl = New-Object System.Security.AccessControl.DirectorySecurity
$acl.SetOwner($sid)
$acl.SetAccessRuleProtection($true, $false)
foreach ($identity in @($sid, [System.Security.Principal.SecurityIdentifier]'S-1-5-18')) {
    $rule = New-Object System.Security.AccessControl.FileSystemAccessRule($identity, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
    $acl.AddAccessRule($rule)
}
Set-Acl -Path $homeDir -AclObject $acl
$configPath = Join-Path $homeDir 'steady.json'
if (!(Test-Path $configPath)) {
    $source = Get-Content (Join-Path $projectDir 'src\main\resources\application.yml') -Raw
    $settings = @{}
    foreach ($key in @('POSTGRES_HOST', 'POSTGRES_PORT', 'POSTGRES_DB_NAME', 'POSTGRES_USER', 'POSTGRES_PASSWORD', 'POSTGRES_SSLMODE')) {
        $value = [Environment]::GetEnvironmentVariable($key)
        if ($null -eq $value) {
            $match = [regex]::Match($source, '\$\{' + $key + ':([^}]*)\}')
            if (!$match.Success) { throw "Missing database setting: $key" }
            $value = $match.Groups[1].Value
        }
        $settings[$key] = $value
    }
    $settings.SERVER_ADDRESS = '127.0.0.1'
    $settings.SERVER_PORT = '8808'
    $settings.VAADIN_LAUNCH_BROWSER = 'false'
    $settings | ConvertTo-Json | Set-Content $configPath -Encoding UTF8
}

# Ask the installed launcher to stop its own Java child before replacing the JAR.
$mutexName = 'Local\Steady-' + $sid.Value
$mutex = New-Object System.Threading.Mutex($false, $mutexName)
$locked = $false
try {
    try { $locked = $mutex.WaitOne(0) } catch [System.Threading.AbandonedMutexException] { $locked = $true }
    if (!$locked) {
        New-Item -ItemType File -Force (Join-Path $homeDir 'stop') | Out-Null
        try { $locked = $mutex.WaitOne(30000) } catch [System.Threading.AbandonedMutexException] { $locked = $true }
        if (!$locked) { throw 'Steady did not stop within 30 seconds. Close it before reinstalling.' }
    }
    Remove-Item (Join-Path $homeDir 'stop') -ErrorAction SilentlyContinue
    Copy-Item $jar (Join-Path $homeDir 'steady.jar') -Force
    $java | Set-Content (Join-Path $homeDir 'java-path.txt') -Encoding UTF8
    Copy-Item (Join-Path $projectDir 'deploy\steady-windows.ps1') (Join-Path $homeDir 'steady.ps1') -Force

    $shell = New-Object -ComObject WScript.Shell
    $powershell = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
    $arguments = '-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "' + (Join-Path $homeDir 'steady.ps1') + '"'
    $startup = $shell.CreateShortcut((Join-Path ([Environment]::GetFolderPath('Startup')) 'Steady.lnk'))
    $startup.TargetPath = $powershell
    $startup.Arguments = $arguments
    $startup.WorkingDirectory = $homeDir
    $startup.Save()
    $menu = $shell.CreateShortcut((Join-Path ([Environment]::GetFolderPath('Programs')) 'Steady.lnk'))
    $menu.TargetPath = $powershell
    $menu.Arguments = $arguments + ' -OpenBrowser'
    $menu.WorkingDirectory = $homeDir
    $menu.Save()
} finally {
    if ($locked) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}
Start-Process $powershell -ArgumentList $arguments -WindowStyle Hidden
Write-Host 'Installed. Steady starts at Windows sign-in. Java 21+ is required.'
Write-Host 'Open Steady from the Start menu or visit http://localhost:8808.'
Write-Host "Configuration and logs: $homeDir"
