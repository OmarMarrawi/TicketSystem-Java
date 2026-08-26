<#
.SYNOPSIS
    Baut und deployed das Ticket-System Projekt.
.DESCRIPTION
    Stoppt Payara, kompiliert mit Maven, kopiert die WAR in den Deploy-Ordner
    und startet Payara Micro neu. Prueft automatisch, ob die App bereit ist.
.PARAMETER SkipBuild
    Ueberspringt den Maven-Build (nur Deploy/Restart).
.PARAMETER NoStart
    Baut und deployed, startet Payara aber nicht.
#>
param(
    [switch]$SkipBuild,
    [switch]$NoStart
)

$ErrorActionPreference = "Stop"

# --- Pfade (fuer interaktive & Script-Aufruf) ---
if ($PSScriptRoot) {
    $ProjectDir = $PSScriptRoot
} elseif ($MyInvocation.MyCommand.Path) {
    $ProjectDir = Split-Path $MyInvocation.MyCommand.Path -Parent
} else {
    # Fallback: bekannter Projektpfad
    $ProjectDir = "$env:USERPROFILE\Desktop\Ticket-System Java\TicketSystem-Java"
}
$JDK         = "$env:USERPROFILE\AppData\Local\Temp\opencode\jdk8\jdk8u502-b07"
$Maven       = "$env:USERPROFILE\AppData\Local\Temp\opencode\maven\apache-maven-3.8.8"
$PayaraJar   = "$env:USERPROFILE\AppData\Local\Temp\opencode\payara-micro.jar"
$DeployDir   = "$env:USERPROFILE\AppData\Local\Temp\opencode\deploy"
$WarFile     = "$ProjectDir\target\ticket-system.war"
$DeployWar   = "$DeployDir\ROOT.war"
$ErrLog      = "$env:USERPROFILE\AppData\Local\Temp\opencode\payara-err.log"
$OutLog      = "$env:USERPROFILE\AppData\Local\Temp\opencode\payara-out.log"
$Port        = 8080

# --- Farben ---
function Write-Step  { param($msg) Write-Host "`n>> $msg" -ForegroundColor Cyan }
function Write-OK    { param($msg) Write-Host "   OK: $msg" -ForegroundColor Green }
function Write-Fail  { param($msg) Write-Host "   FEHLER: $msg" -ForegroundColor Red }

Write-Host "============================================" -ForegroundColor Yellow
Write-Host "   Ticket-System Build & Deploy"            -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow

# --- 1. Payara stoppen ---
Write-Step "Payara stoppen..."
$killed = 0
Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -like "*payara*" } |
    ForEach-Object {
        Write-Host "   PID $($_.ProcessId) wird gestoppt..."
        Stop-Process -Id $_.ProcessId -Force
        $killed++
    }
if ($killed -eq 0) { Write-OK "Keine laufende Payara-Instanz gefunden" }
else               { Write-OK "$killed Prozess(e) gestoppt" }
Start-Sleep -Seconds 3

# --- 2. Maven Build ---
if (-not $SkipBuild) {
    Write-Step "Maven Build..."

    # Pruefen ob Java/Maven existieren
    $javaExe = "$JDK\bin\java.exe"
    $mvnCmd  = "$Maven\bin\mvn.cmd"

    if (-not (Test-Path $javaExe)) {
        Write-Host "   Java nicht gefunden - versuche JDK8 zu entpacken..." -ForegroundColor Yellow
        $zip = "$env:USERPROFILE\AppData\Local\Temp\opencode\jdk8.zip"
        if (Test-Path $zip) {
            $dest = "$env:USERPROFILE\AppData\Local\Temp\opencode\jdk8"
            Expand-Archive -Path $zip -DestinationPath $dest -Force
            if (-not (Test-Path $javaExe)) {
                Write-Fail "JDK8 entpackt aber java.exe immer noch nicht unter: $javaExe"
                exit 1
            }
            Write-OK "JDK8 entpackt"
        } else {
            Write-Fail "JDK8.zip nicht gefunden unter: $zip"
            Write-Host "   Bitte JDK manuell installieren oder JDK8.zip bereitstellen." -ForegroundColor Yellow
            exit 1
        }
    }

    if (-not (Test-Path $mvnCmd)) {
        Write-Host "   Maven nicht gefunden - versuche zu entpacken..." -ForegroundColor Yellow
        $zip = "$env:USERPROFILE\AppData\Local\Temp\opencode\maven.zip"
        if (Test-Path $zip) {
            $dest = "$env:USERPROFILE\AppData\Local\Temp\opencode\maven"
            Expand-Archive -Path $zip -DestinationPath $dest -Force
            if (-not (Test-Path $mvnCmd)) {
                Write-Fail "Maven entpackt aber mvn.cmd immer noch nicht unter: $mvnCmd"
                exit 1
            }
            Write-OK "Maven entpackt"
        } else {
            Write-Fail "Maven.zip nicht gefunden unter: $zip"
            exit 1
        }
    }

    $env:JAVA_HOME = $JDK
    Push-Location $ProjectDir
    $buildArgs = "clean package -DskipTests"
    & cmd /c "set `"JAVA_HOME=$JDK`" && `"$mvnCmd`" $buildArgs"
    $exitCode = $LASTEXITCODE
    Pop-Location

    if ($exitCode -ne 0) {
        Write-Fail "Maven Build fehlgeschlagen (Exit-Code: $exitCode)!"
        exit 1
    }

    if (-not (Test-Path $WarFile)) {
        Write-Fail "WAR-Datei nicht gefunden: $WarFile"
        exit 1
    }

    Write-OK "Build erfolgreich"
} else {
    Write-Step "Build uebersprungen (-SkipBuild)"
    if (-not (Test-Path $WarFile)) {
        Write-Fail "Keine WAR-Datei vorhanden: $WarFile"
        exit 1
    }
}

# --- 3. WAR deployn ---
Write-Step "WAR in Deploy-Ordner kopieren..."
if (-not (Test-Path $DeployDir)) { New-Item -ItemType Directory -Path $DeployDir -Force | Out-Null }
Copy-Item $WarFile $DeployWar -Force
$size = [math]::Round((Get-Item $DeployWar).Length / 1MB, 1)
Write-OK "ROOT.war kopiert ($size MB)"

# --- 4. Payara starten ---
if ($NoStart) {
    Write-Step "Start uebersprungen (-NoStart)"
    Write-Host "`nFertig!" -ForegroundColor Yellow
    exit 0
}

Write-Step "Payara Micro starten (Port $Port)..."
$java = "$JDK\bin\java.exe"
Start-Process -FilePath $java `
    -ArgumentList @("-jar", $PayaraJar, "--deploymentDir", $DeployDir, "--port", $Port, "--nocluster") `
    -RedirectStandardOutput $OutLog `
    -RedirectStandardError  $ErrLog `
    -WindowStyle Hidden

# --- 5. Warten bis bereit ---
Write-Host "   Warte auf Deploy" -NoNewline
$deadline = (Get-Date).AddSeconds(120)
$ready = $false
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 3
    Write-Host "." -NoNewline
    if (Select-String -Path $ErrLog -Pattern "ready in" -ErrorAction SilentlyContinue) {
        $ready = $true
        break
    }
}
Start-Sleep -Seconds 5
Write-Host ""

if ($ready) {
    Write-OK "Payara ist bereit!"
} else {
    Write-Fail "Timeout (120s) - Payara ist moeglicherweise noch am Starten"
}

# --- 6. Health Check ---
Write-Step "Health Check..."
try {
    $resp = Invoke-WebRequest -Uri "http://localhost:$Port/login.xhtml" -UseBasicParsing -TimeoutSec 10
    Write-OK "Login-Page erreichbar (HTTP $($resp.StatusCode))"
} catch {
    Write-Fail "Login-Page nicht erreichbar: $($_.Exception.Message)"
}

# --- Fertig ---
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "   App: http://localhost:$Port"                -ForegroundColor Green
Write-Host "   Login: admin@bvl.bund.de / admin"           -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
