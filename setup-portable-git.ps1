$ErrorActionPreference = 'Stop'

$gitRoot = Join-Path $env:USERPROFILE 'portable-git'
$gitExe = Join-Path $gitRoot 'cmd\git.exe'

try {
    if (-not (Test-Path $gitExe)) {
        Write-Host 'Portable Git was not found. Downloading it now...'
        New-Item -ItemType Directory -Force -Path $gitRoot | Out-Null

        $release = Invoke-RestMethod -Uri 'https://api.github.com/repos/git-for-windows/git/releases/latest'
        $asset = $release.assets |
            Where-Object { $_.name -match '^PortableGit-.*64-bit\.7z\.exe$' } |
            Select-Object -First 1

        if ($null -eq $asset) {
            throw 'The latest Portable Git download was not found.'
        }

        $archive = Join-Path $env:TEMP $asset.name
        Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $archive
        $extractor = Start-Process -FilePath $archive -ArgumentList @('-y', "-o$gitRoot") -Wait -PassThru
        Remove-Item $archive -Force -ErrorAction SilentlyContinue

        if ($extractor.ExitCode -ne 0) {
            throw "Portable Git extraction failed with exit code $($extractor.ExitCode)."
        }
    }

    if (-not (Test-Path $gitExe)) {
        throw "Git was not found after extraction in $gitRoot."
    }

    $name = Read-Host 'Git user.name'
    while ([string]::IsNullOrWhiteSpace($name)) {
        $name = Read-Host 'Git user.name cannot be empty'
    }

    $email = Read-Host 'Git user.email'
    while ([string]::IsNullOrWhiteSpace($email)) {
        $email = Read-Host 'Git user.email cannot be empty'
    }

    & $gitExe config --global user.name $name
    & $gitExe config --global user.email $email

    $gitCommandPath = Split-Path $gitExe -Parent
    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    $pathEntries = @()
    if (-not [string]::IsNullOrWhiteSpace($userPath)) {
        $pathEntries = $userPath -split ';' | Where-Object { $_ }
    }

    if ($pathEntries -notcontains $gitCommandPath) {
        [Environment]::SetEnvironmentVariable('Path', "$gitCommandPath;$userPath", 'User')
    }

    $env:Path = "$gitCommandPath;$env:Path"
    Write-Host ''
    Write-Host 'Portable Git is ready:'
    & $gitExe --version
    Write-Host "Configured name: $(& $gitExe config --global user.name)"
    Write-Host "Configured email: $(& $gitExe config --global user.email)"
    Write-Host ''
    Write-Host 'Restart VS Code so it can find Git in the updated PATH.'
}
catch {
    Write-Error $_.Exception.Message
    exit 1
}

Read-Host 'Press Enter to close'
