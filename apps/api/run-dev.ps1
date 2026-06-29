# This script loads environment variables from the root .env file and starts the Spring Boot backend.

# 1. Ensure Java 21 and Maven paths are set up (handling fresh installations before terminal restart)
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    # Check Chocolatey Maven path
    $chocoMavenPath = "C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.16\bin"
    if (Test-Path $chocoMavenPath) {
        $env:Path = "$chocoMavenPath;" + $env:Path
        Write-Host "Added Maven to session Path: $chocoMavenPath" -ForegroundColor Yellow
    }
}

# Check Java version and locate Temurin 21 if current Java is Java 8 or missing
$needsJavaUpgrade = $true
try {
    $javaVerStr = & java -version 2>&1 | Out-String
    if ($javaVerStr -match 'version "(21|17|22|23|24|25)"' -or $javaVerStr -match 'openjdk version "(21|17|22|23|24|25)"') {
        $needsJavaUpgrade = $false
    }
} catch {
    # Java not found
}

if ($needsJavaUpgrade) {
    # Check default Temurin 21 path
    $temurin21Path = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
    if (Test-Path $temurin21Path) {
        $env:JAVA_HOME = $temurin21Path
        $env:Path = (Join-Path $temurin21Path "bin") + ";" + $env:Path
        Write-Host "Set JAVA_HOME to JDK 21: $temurin21Path" -ForegroundColor Yellow
    }
}

# 2. Locate the .env file (check root and local directory)
$envPath = Join-Path $PSScriptRoot "..\..\.env"
if (-not (Test-Path $envPath)) {
    $envPath = Join-Path $PSScriptRoot ".env"
}

if (-not (Test-Path $envPath)) {
    Write-Error "Could not find .env file! Please copy .env.example to .env and fill in the values."
    exit 1
}

Write-Host "Loading environment variables from $envPath..." -ForegroundColor Cyan

# 3. Parse and set environment variables
Get-Content $envPath | ForEach-Object {
    $line = $_.Trim()
    # Skip comments and empty lines
    if ($line -and -not $line.StartsWith("#")) {
        $index = $line.IndexOf("=")
        if ($index -gt 0) {
            $key = $line.Substring(0, $index).Trim()
            $value = $line.Substring($index + 1).Trim()
            
            # Remove optional wrapping quotes
            if ($value.StartsWith('"') -and $value.EndsWith('"')) {
                $value = $value.Substring(1, $value.Length - 2)
            } elseif ($value.StartsWith("'") -and $value.EndsWith("'")) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            
            # Set variable in the current process
            [System.Environment]::SetEnvironmentVariable($key, $value, [System.EnvironmentVariableTarget]::Process)
        }
    }
}

# 4. Check Java & Maven Version for verification
try {
    $javaVerStr = & java -version 2>&1 | Out-String
    if ($javaVerStr -match 'version "([^"]+)"' -or $javaVerStr -match 'openjdk version "([^"]+)"') {
        Write-Host "Using Java version: $($Matches[1])" -ForegroundColor Green
    }
    $mvnVerStr = & mvn -v 2>&1 | Out-String
    if ($mvnVerStr -match 'Apache Maven ([^\s]+)') {
        Write-Host "Using Maven version: $($Matches[1])" -ForegroundColor Green
    }
} catch {
    Write-Warning "Could not verify Java or Maven installation."
}

# 5. Start the application
Write-Host "Starting Spring Boot API..." -ForegroundColor Green
mvn spring-boot:run
