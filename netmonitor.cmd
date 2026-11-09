<# : batch part (PowerShell reads this block as a comment)
@echo off
setlocal
set "NETMONITOR_SCRIPT=%~f0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Invoke-Expression ([IO.File]::ReadAllText($env:NETMONITOR_SCRIPT))"
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%" == "0" (
    echo.
    echo NetMonitor stopped with exit code %EXIT_CODE%.
    pause
)
endlocal & exit /b %EXIT_CODE%
#>

# =====================================================================================
#  NetMonitor - one-click setup and launch, no administrator rights required.
#  This single file can be copied anywhere (e.g. from a USB stick) and run.
#    Git    portable Git (%USERPROFILE%\portable-git) + user.name / user.email
#    Repo   clone or update the project (%USERPROFILE%\2-IUT-Monitoring-application)
#    MySQL  portable MySQL server (%USERPROFILE%\portable-mysql, 127.0.0.1:3307)
#    JDK    portable JDK 21 (%USERPROFILE%\portable-jdk21)
#    App    NetMonitor (JavaFX); the MySQL server is stopped when the app is closed
# =====================================================================================

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$GitUserName  = 'Oleh Liubchenko'
$GitUserEmail = 'dev@liubquanti.click'
$RepoUrl      = 'https://github.com/liubquanti-edu/2-IUT-Monitoring-application.git'
$ProjectDir   = Join-Path $env:USERPROFILE '2-IUT-Monitoring-application'
$GitRoot      = Join-Path $env:USERPROFILE 'portable-git'
$JdkRoot      = Join-Path $env:USERPROFILE 'portable-jdk21'
$MySqlRoot    = Join-Path $env:USERPROFILE 'portable-mysql'
$MySqlVersion = '8.4.6'
$MySqlUrl     = "https://downloads.mysql.com/archives/get/p/23/file/mysql-$MySqlVersion-winx64.zip"
$MySqlHome    = Join-Path $MySqlRoot "mysql-$MySqlVersion-winx64"
$MySqlData    = Join-Path $MySqlRoot 'data'
$MySqlIni     = Join-Path $MySqlRoot 'my.ini'
$MySqlLog     = Join-Path $MySqlRoot 'mysql.err'
$MySqlPort    = 3307
$DbName       = 'netmonitor'
$DbUser       = 'netmonitor'
$DbPassword   = 'netmonitor'

# ----------------------------------------------------------------------------- status display
$Steps = [ordered]@{ Git = 'IN QUEUE'; Repo = 'IN QUEUE'; MySQL = 'IN QUEUE'; JDK = 'IN QUEUE'; App = 'IN QUEUE' }
$Log = New-Object System.Collections.Generic.List[string]

# -NoClear keeps what is already on screen (e.g. Maven errors) and prints the table below it.
function Show-Status([switch]$NoClear) {
    if (-not $NoClear) { try { Clear-Host } catch { } }
    Write-Host ''
    Write-Host '  NetMonitor' -ForegroundColor Cyan
    Write-Host '  ------------------------'
    foreach ($name in $Steps.Keys) {
        $state = $Steps[$name]
        $color = switch ($state) {
            'DONE'       { 'Green' }
            'IN PROCESS' { 'Yellow' }
            'RUNNING'    { 'Yellow' }
            'ERROR'      { 'Red' }
            default      { 'DarkGray' }
        }
        Write-Host ('  {0,-8}' -f $name) -NoNewline
        Write-Host $state -ForegroundColor $color
    }
    Write-Host '  ------------------------'
    foreach ($line in $Log) {
        $color = if ($line -like 'ERROR*') { 'Red' } elseif ($line -like 'WARNING*') { 'Yellow' } else { 'Gray' }
        Write-Host "  $line" -ForegroundColor $color
    }
}

function Set-Step([string]$name, [string]$state) {
    $Steps[$name] = $state
    Show-Status
}

function Write-Info([string]$text) {
    $Log.Add($text)
    Write-Host "  $text" -ForegroundColor Gray
}

# ----------------------------------------------------------------------------- helpers
# Quotes arguments containing spaces (Start-Process does not do it in Windows PowerShell 5.1).
function Join-Arguments([string[]]$arguments) {
    ($arguments | ForEach-Object { if ($_ -match '\s') { '"' + $_ + '"' } else { $_ } }) -join ' '
}

# Runs a console program silently and returns its exit code and output.
function Invoke-Tool([string]$exe, [string[]]$arguments, [string]$inputFile) {
    $out = [IO.Path]::GetTempFileName()
    $err = [IO.Path]::GetTempFileName()
    try {
        $params = @{
            FilePath = $exe; ArgumentList = (Join-Arguments $arguments); Wait = $true; PassThru = $true
            NoNewWindow = $true; RedirectStandardOutput = $out; RedirectStandardError = $err
        }
        if ($inputFile) { $params.RedirectStandardInput = $inputFile }
        $process = Start-Process @params
        return [pscustomobject]@{
            ExitCode = $process.ExitCode
            Output   = ((Get-Content $out -Raw) + (Get-Content $err -Raw))
        }
    }
    finally {
        Remove-Item $out, $err -Force -ErrorAction SilentlyContinue
    }
}

function Get-File([string]$url, [string]$target) {
    $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
    if ($curl) {
        & $curl.Source -L --fail --progress-bar -o $target $url
        if ($LASTEXITCODE -ne 0) { throw "Download failed: $url" }
    }
    else {
        Invoke-WebRequest -Uri $url -OutFile $target -UseBasicParsing
    }
}

function Expand-Zip([string]$zip, [string]$destination) {
    New-Item -ItemType Directory -Force -Path $destination | Out-Null
    $tar = Join-Path $env:SystemRoot 'System32\tar.exe'
    if (Test-Path $tar) {
        & $tar -xf $zip -C $destination
        if ($LASTEXITCODE -ne 0) { throw "Could not extract $zip" }
    }
    else {
        Expand-Archive -Path $zip -DestinationPath $destination -Force
    }
}

function Add-ToUserPath([string]$directory) {
    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    $entries = @()
    if (-not [string]::IsNullOrWhiteSpace($userPath)) { $entries = $userPath -split ';' | Where-Object { $_ } }
    if ($entries -notcontains $directory) {
        [Environment]::SetEnvironmentVariable('Path', (@($directory) + $entries) -join ';', 'User')
    }
    $env:Path = "$directory;$env:Path"
}

# ----------------------------------------------------------------------------- Git
function Install-Git {
    $gitExe = Join-Path $GitRoot 'cmd\git.exe'
    if (Test-Path $gitExe) { return $gitExe }

    $existing = Get-Command git.exe -ErrorAction SilentlyContinue
    if ($existing) { return $existing.Source }

    Write-Info 'Portable Git not found, downloading...'
    New-Item -ItemType Directory -Force -Path $GitRoot | Out-Null
    $release = Invoke-RestMethod -Uri 'https://api.github.com/repos/git-for-windows/git/releases/latest'
    $asset = $release.assets | Where-Object { $_.name -match '^PortableGit-.*64-bit\.7z\.exe$' } | Select-Object -First 1
    if ($null -eq $asset) { throw 'The latest Portable Git download was not found.' }

    $archive = Join-Path $env:TEMP $asset.name
    Get-File $asset.browser_download_url $archive
    Write-Info 'Extracting Git...'
    $extractor = Start-Process -FilePath $archive -ArgumentList @('-y', (Join-Arguments @("-o$GitRoot"))) -Wait -PassThru
    Remove-Item $archive -Force -ErrorAction SilentlyContinue
    if ($extractor.ExitCode -ne 0) { throw "Portable Git extraction failed with exit code $($extractor.ExitCode)." }
    if (-not (Test-Path $gitExe)) { throw "Git was not found after extraction in $GitRoot." }

    Add-ToUserPath (Split-Path $gitExe -Parent)
    return $gitExe
}

function Set-GitIdentity([string]$gitExe) {
    & $gitExe config --global user.name $GitUserName
    & $gitExe config --global user.email $GitUserEmail
    if ($LASTEXITCODE -ne 0) { throw 'Could not write the Git configuration.' }
    Write-Info "Git user: $GitUserName <$GitUserEmail>"
}

# ----------------------------------------------------------------------------- Repo
function Update-Repository([string]$gitExe) {
    if (Test-Path (Join-Path $ProjectDir '.git')) {
        Write-Info "Updating $ProjectDir ..."
        $pull = Invoke-Tool $gitExe @('-C', $ProjectDir, 'pull', '--ff-only')
        if ($pull.ExitCode -ne 0) {
            # local changes or no network: keep working with the existing copy
            Write-Info 'WARNING: git pull failed, using the local copy as it is.'
        }
    }
    else {
        if ((Test-Path $ProjectDir) -and (Get-ChildItem $ProjectDir -Force | Select-Object -First 1)) {
            throw "$ProjectDir already exists and is not a Git repository. Move or delete it, then run again."
        }
        Write-Info "Cloning $RepoUrl ..."
        $clone = Invoke-Tool $gitExe @('clone', $RepoUrl, $ProjectDir)
        if ($clone.ExitCode -ne 0) { throw "git clone failed:`n$($clone.Output)" }
    }
    if (-not (Test-Path (Join-Path $ProjectDir 'pom.xml'))) { throw "pom.xml not found in $ProjectDir." }
    Write-Info "Project: $ProjectDir"
}

# ----------------------------------------------------------------------------- MySQL
$MySqlClientArgs = @('-u', 'root', '-h', '127.0.0.1', '-P', "$MySqlPort")

function Test-MySqlRunning {
    $mysqladmin = Join-Path $MySqlHome 'bin\mysqladmin.exe'
    return (Invoke-Tool $mysqladmin ($MySqlClientArgs + @('--connect-timeout=2', 'ping'))).ExitCode -eq 0
}

function Install-MySql {
    $mysqld = Join-Path $MySqlHome 'bin\mysqld.exe'
    if (-not (Test-Path $mysqld)) {
        Write-Info "Portable MySQL $MySqlVersion not found, downloading (about 250 MB)..."
        $zip = Join-Path $env:TEMP "mysql-$MySqlVersion-winx64.zip"
        Get-File $MySqlUrl $zip
        Write-Info 'Extracting MySQL...'
        Expand-Zip $zip $MySqlRoot
        Remove-Item $zip -Force -ErrorAction SilentlyContinue
        if (-not (Test-Path $mysqld)) { throw "mysqld.exe was not found after extraction in $MySqlHome." }
    }

    $system32 = Join-Path $env:SystemRoot 'System32'
    foreach ($dll in 'vcruntime140.dll', 'vcruntime140_1.dll', 'msvcp140.dll') {
        if (-not (Test-Path (Join-Path $system32 $dll)) -and -not (Test-Path (Join-Path $MySqlHome "bin\$dll"))) {
            Write-Info "WARNING: $dll is missing (Visual C++ runtime). Copy it into $MySqlHome\bin."
        }
    }

    # Paths with forward slashes: MySQL option files treat backslashes as escapes.
    $ini = @(
        '[mysqld]'
        "basedir=$($MySqlHome -replace '\\', '/')"
        "datadir=$($MySqlData -replace '\\', '/')"
        "log-error=$($MySqlLog -replace '\\', '/')"
        "port=$MySqlPort"
        'bind-address=127.0.0.1'
        'mysqlx=OFF'
        'character-set-server=utf8mb4'
    )
    Set-Content -Path $MySqlIni -Value $ini -Encoding ASCII

    if (-not (Test-Path (Join-Path $MySqlData 'mysql'))) {
        Write-Info 'Initializing the MySQL data directory (first launch)...'
        Remove-Item $MySqlData -Recurse -Force -ErrorAction SilentlyContinue
        $init = Invoke-Tool $mysqld @("--defaults-file=$MySqlIni", '--initialize-insecure')
        if ($init.ExitCode -ne 0) {
            Remove-Item $MySqlData -Recurse -Force -ErrorAction SilentlyContinue
            throw "MySQL initialization failed. See $MySqlLog`n$($init.Output)"
        }
    }
}

# Returns $true if this script started the server (and must therefore stop it).
function Start-MySql {
    if (Test-MySqlRunning) {
        Write-Info "MySQL is already running on port $MySqlPort."
        return $false
    }
    Write-Info 'Starting the MySQL server...'
    $mysqld = Join-Path $MySqlHome 'bin\mysqld.exe'
    $process = Start-Process -FilePath $mysqld -ArgumentList (Join-Arguments @("--defaults-file=$MySqlIni")) `
        -WindowStyle Hidden -PassThru
    $deadline = (Get-Date).AddSeconds(60)
    while (-not (Test-MySqlRunning)) {
        if ($process.HasExited -or (Get-Date) -gt $deadline) {
            $log = if (Test-Path $MySqlLog) { (Get-Content $MySqlLog -Tail 15) -join "`n" } else { '' }
            throw "The MySQL server did not start. Last lines of ${MySqlLog}:`n$log"
        }
        Start-Sleep -Milliseconds 500
    }
    Write-Info "MySQL server running on 127.0.0.1:$MySqlPort."
    return $true
}

function Initialize-Database {
    $sql = @"
CREATE DATABASE IF NOT EXISTS $DbName CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS '$DbUser'@'localhost' IDENTIFIED BY '$DbPassword';
CREATE USER IF NOT EXISTS '$DbUser'@'127.0.0.1' IDENTIFIED BY '$DbPassword';
GRANT ALL PRIVILEGES ON $DbName.* TO '$DbUser'@'localhost';
GRANT ALL PRIVILEGES ON $DbName.* TO '$DbUser'@'127.0.0.1';
"@
    $file = [IO.Path]::GetTempFileName()
    try {
        Set-Content -Path $file -Value $sql -Encoding ASCII
        $result = Invoke-Tool (Join-Path $MySqlHome 'bin\mysql.exe') $MySqlClientArgs $file
        if ($result.ExitCode -ne 0) { throw "Could not create the '$DbName' database:`n$($result.Output)" }
    }
    finally {
        Remove-Item $file -Force -ErrorAction SilentlyContinue
    }
    Write-Info "Database '$DbName' ready (user '$DbUser')."
}

function Stop-MySql {
    Write-Info 'Stopping the MySQL server...'
    $result = Invoke-Tool (Join-Path $MySqlHome 'bin\mysqladmin.exe') ($MySqlClientArgs + @('shutdown'))
    if ($result.ExitCode -ne 0) { Write-Info "WARNING: could not stop MySQL: $($result.Output)" }
    else { Write-Info 'MySQL server stopped.' }
}

# ----------------------------------------------------------------------------- JDK
function Install-Jdk {
    $jdk = Get-ChildItem -Path $JdkRoot -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $jdk) {
        Write-Info 'Portable JDK 21 not found, downloading...'
        $zip = Join-Path $env:TEMP 'netmonitor-jdk21.zip'
        Get-File 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse' $zip
        Write-Info 'Extracting JDK...'
        Expand-Zip $zip $JdkRoot
        Remove-Item $zip -Force -ErrorAction SilentlyContinue
        $jdk = Get-ChildItem -Path $JdkRoot -Directory -Filter 'jdk-*' | Select-Object -First 1
    }
    if (-not $jdk -or -not (Test-Path (Join-Path $jdk.FullName 'bin\java.exe'))) {
        throw "No valid JDK found in $JdkRoot. Download one from https://adoptium.net/temurin/releases/"
    }
    return $jdk.FullName
}

# ----------------------------------------------------------------------------- main
$current = $null
$mysqlStartedHere = $false
$exitCode = 0
try {
    Show-Status

    $current = 'Git'; Set-Step $current 'IN PROCESS'
    $git = Install-Git
    Write-Info "Git: $git"
    Set-GitIdentity $git
    Set-Step $current 'DONE'

    $current = 'Repo'; Set-Step $current 'IN PROCESS'
    Update-Repository $git
    Set-Step $current 'DONE'

    $current = 'MySQL'; Set-Step $current 'IN PROCESS'
    Install-MySql
    $mysqlStartedHere = Start-MySql
    Initialize-Database
    Set-Step $current 'DONE'

    $current = 'JDK'; Set-Step $current 'IN PROCESS'
    $env:JAVA_HOME = Install-Jdk
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    Write-Info "JDK: $env:JAVA_HOME"
    Set-Step $current 'DONE'

    $current = 'App'; Set-Step $current 'IN PROCESS'
    Write-Info 'Building and starting NetMonitor (the first build downloads dependencies)...'
    $env:NETMONITOR_DB_URL = "jdbc:mysql://127.0.0.1:$MySqlPort/$DbName" +
        '?sslMode=DISABLED&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true'
    $env:NETMONITOR_DB_USER = $DbUser
    $env:NETMONITOR_DB_PASSWORD = $DbPassword
    Push-Location $ProjectDir
    try {
        & (Join-Path $ProjectDir 'mvnw.cmd') -q -f (Join-Path $ProjectDir 'pom.xml') javafx:run
        $exitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
    if ($exitCode -ne 0) { throw "NetMonitor exited with code $exitCode." }
    Write-Info 'NetMonitor closed.'
    Set-Step $current 'DONE'
}
catch {
    if ($current) { $Steps[$current] = 'ERROR' }
    $Log.Add("ERROR: $($_.Exception.Message)")
    Show-Status -NoClear
    if ($exitCode -eq 0) { $exitCode = 1 }
}
finally {
    if ($mysqlStartedHere) { Stop-MySql }
}
exit $exitCode
