$ErrorActionPreference = "Continue"

Write-Host "Starting AMHS/SWIM Gateway Test Tool Installation..." -ForegroundColor Cyan

# 1. Check and Install JDK (11 or higher)
Write-Host "Checking for Java Development Kit (JDK)..." -ForegroundColor Cyan
try {
    $javaVersion = java -version 2>&1
    if ($javaVersion -match 'version "(\d+)') {
        $version = [int]$matches[1]
        Write-Host "Java is already installed (Version $version)." -ForegroundColor Green
    } else {
        Write-Host "Java is already installed." -ForegroundColor Green
    }
} catch {
    Write-Host "Java not found. Installing OpenJDK 11..." -ForegroundColor Yellow
    # Using winget for unattended install
    winget install -e --id Microsoft.OpenJDK.11 --accept-package-agreements --accept-source-agreements
    Write-Host "Note: You may need to restart your terminal for Java to be fully available in PATH." -ForegroundColor Yellow
}

# 2. Check and Install Maven (3.6 or higher)
Write-Host "`nChecking for Apache Maven..." -ForegroundColor Cyan
try {
    $mvnVersion = mvn -version 2>&1
    Write-Host "Maven is already installed." -ForegroundColor Green
} catch {
    Write-Host "Maven not found. Installing Apache Maven..." -ForegroundColor Yellow
    winget install -e --id Apache.Maven --accept-package-agreements --accept-source-agreements
    Write-Host "Note: You may need to restart your terminal for Maven to be fully available in PATH." -ForegroundColor Yellow
}

# 3. Setting up lib directory for JARs
$libDir = Join-Path $PWD "lib"
Write-Host "`nSetting up dependencies in '$libDir'..." -ForegroundColor Cyan
if (-not (Test-Path $libDir)) {
    New-Item -ItemType Directory -Path $libDir | Out-Null
    Write-Host "Created lib/ directory." -ForegroundColor Green
} else {
    Write-Host "lib/ directory already exists." -ForegroundColor Green
}

# 4. Locate and Download JARs
Write-Host "`nLocating and Downloading Solace and Isode JAR files..." -ForegroundColor Cyan

# 4.1 Solace JCSMP
$solaceJar = Join-Path $libDir "sol-jcsmp-10.1.0.jar"
if (-not (Test-Path $solaceJar)) {
    Write-Host "Downloading Solace JCSMP JAR from Maven Central..."
    try {
        Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/com/solacesystems/sol-jcsmp/10.1.0/sol-jcsmp-10.1.0.jar" -OutFile $solaceJar
        Write-Host "Successfully downloaded '$solaceJar'." -ForegroundColor Green
    } catch {
        Write-Host "Failed to download Solace JAR: $_" -ForegroundColor Red
    }
} else {
    Write-Host "Solace JAR already exists. Skipping." -ForegroundColor Green
}

# 4.2 Isode X.400 Gateway API
$isodeX400Jar = Join-Path $libDir "isode-x400-api.jar"
if (-not (Test-Path $isodeX400Jar)) {
    Write-Host "Setting up Isode X.400 API JAR..."
    # Note: Isode JARs are proprietary and typically require a license/login to download.
    # Below is a placeholder for demonstration. The user needs to supply the actual proprietary JAR.
    # Alternatively, you could host them internally and update the $isodeUrl below.
    $isodeUrl = $null 
    if ($isodeUrl) {
        Invoke-WebRequest -Uri $isodeUrl -OutFile $isodeX400Jar
        Write-Host "Successfully downloaded Isode X.400 JAR." -ForegroundColor Green
    } else {
        Write-Host "Warning: Isode X.400 JAR needs to be provided manually. It is proprietary software." -ForegroundColor Yellow
        New-Item -ItemType File -Path $isodeX400Jar -Force | Out-Null
        Write-Host "Created placeholder at '$isodeX400Jar'. Please replace with actual JAR." -ForegroundColor DarkYellow
    }
} else {
    Write-Host "Isode X.400 JAR already exists. Skipping." -ForegroundColor Green
}

# 4.3 Isode Directory API
$isodeDirJar = Join-Path $libDir "isode-directory-api.jar"
if (-not (Test-Path $isodeDirJar)) {
    Write-Host "Setting up Isode Directory API JAR..."
    $isodeDirUrl = $null
    if ($isodeDirUrl) {
        Invoke-WebRequest -Uri $isodeDirUrl -OutFile $isodeDirJar
    } else {
        Write-Host "Warning: Isode Directory JAR needs to be provided manually. It is proprietary software." -ForegroundColor Yellow
        New-Item -ItemType File -Path $isodeDirJar -Force | Out-Null
        Write-Host "Created placeholder at '$isodeDirJar'. Please replace with actual JAR." -ForegroundColor DarkYellow
    }
} else {
    Write-Host "Isode Directory JAR already exists. Skipping." -ForegroundColor Green
}

# 5. Execute the remaining installation / build script
Write-Host "`nExecuting remaining installation script..." -ForegroundColor Cyan
try {
    # Check if the session sees mvn
    $mvnAvailable = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnAvailable) {
        Write-Host "Running Maven build..."
        mvn clean install
    } else {
        # Fallback to bash if they have Git Bash, WSL etc.
        $bashAvailable = Get-Command bash -ErrorAction SilentlyContinue
        if ($bashAvailable -and (Test-Path "./scripts/build.sh")) {
            Write-Host "mvn not found in this session. Attempting to run ./scripts/build.sh via bash..." -ForegroundColor Yellow
            bash ./scripts/build.sh
        } else {
            Write-Host "Could not find 'mvn' in current path." -ForegroundColor Yellow
            Write-Host "Since JDK and Maven may have just been installed, please restart your terminal to load the new PATH, then run 'mvn clean install'." -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "Failed to execute installation script: $_" -ForegroundColor Red
}

Write-Host "`nInstallation workflow complete." -ForegroundColor Green
