[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern("^[a-z0-9_-]+$")]
    [string]$Name,

    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$World,

    [string[]]$GradleProperty = @(),

    [ValidateRange(1, 65535)]
    [int]$RconPort = 25575,

    [ValidateNotNullOrEmpty()]
    [string]$RconPassword = "magneticraft-task6-smoke",

    [ValidateRange(30, 900)]
    [int]$StartupTimeoutSeconds = 300,

    [ValidateRange(30, 600)]
    [int]$ShutdownTimeoutSeconds = 180
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$reportDirectory = Join-Path $projectRoot "build/reports"
$standardOutput = Join-Path $reportDirectory "migration-server-$Name.out.log"
$standardError = Join-Path $reportDirectory "migration-server-$Name.err.log"
$serverLog = Join-Path $reportDirectory "migration-server-$Name.log"
$gradleWrapper = Join-Path $projectRoot "gradlew.bat"

New-Item -ItemType Directory -Path $reportDirectory -Force | Out-Null
Remove-Item -LiteralPath $standardOutput, $standardError -Force -ErrorAction SilentlyContinue

function Read-ExactBytes {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.Stream]$Stream,

        [Parameter(Mandatory = $true)]
        [int]$Count
    )

    $buffer = [byte[]]::new($Count)
    $offset = 0
    while ($offset -lt $Count) {
        $read = $Stream.Read($buffer, $offset, $Count - $offset)
        if ($read -le 0) {
            throw "RCON stream closed before the packet completed"
        }
        $offset += $read
    }
    return ,$buffer
}

function Send-RconPacket {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.Stream]$Stream,

        [Parameter(Mandatory = $true)]
        [int]$RequestId,

        [Parameter(Mandatory = $true)]
        [int]$Type,

        [Parameter(Mandatory = $true)]
        [AllowEmptyString()]
        [string]$Body
    )

    $payload = [System.Text.Encoding]::UTF8.GetBytes($Body)
    $packetLength = 10 + $payload.Length
    $packet = [byte[]]::new(4 + $packetLength)
    [System.BitConverter]::GetBytes([int]$packetLength).CopyTo($packet, 0)
    [System.BitConverter]::GetBytes($RequestId).CopyTo($packet, 4)
    [System.BitConverter]::GetBytes($Type).CopyTo($packet, 8)
    $payload.CopyTo($packet, 12)
    $Stream.Write($packet, 0, $packet.Length)
    $Stream.Flush()
}

function Read-RconPacket {
    param(
        [Parameter(Mandatory = $true)]
        [System.IO.Stream]$Stream
    )

    $lengthBytes = Read-ExactBytes -Stream $Stream -Count 4
    $length = [System.BitConverter]::ToInt32($lengthBytes, 0)
    if ($length -lt 10) {
        throw "Invalid RCON packet length: $length"
    }

    $data = Read-ExactBytes -Stream $Stream -Count $length
    return [pscustomobject]@{
        Id = [System.BitConverter]::ToInt32($data, 0)
        Type = [System.BitConverter]::ToInt32($data, 4)
        Body = [System.Text.Encoding]::UTF8.GetString($data, 8, $length - 10)
    }
}

function Open-RconConnection {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $client.ReceiveTimeout = 5000
        $client.SendTimeout = 5000
        $client.Connect("127.0.0.1", $RconPort)
        $stream = $client.GetStream()
        Send-RconPacket -Stream $stream -RequestId 1 -Type 3 -Body $RconPassword
        $response = Read-RconPacket -Stream $stream
        if ($response.Id -eq -1) {
            throw "RCON authentication failed"
        }
        return [pscustomobject]@{
            Client = $client
            Stream = $stream
        }
    } catch {
        $client.Dispose()
        throw
    }
}

function Invoke-RconCommand {
    param(
        [Parameter(Mandatory = $true)]
        $Connection,

        [Parameter(Mandatory = $true)]
        [string]$Command
    )

    Send-RconPacket -Stream $Connection.Stream -RequestId 2 -Type 2 -Body $Command
    return (Read-RconPacket -Stream $Connection.Stream).Body
}

$arguments = @("runServer", "--no-daemon", "-Pserver_world=$World")
$arguments += $GradleProperty | ForEach-Object {
    if ($_ -match "^-P") { $_ } else { "-P$_" }
}

$process = $null
$connection = $null
try {
    $process = Start-Process -FilePath $gradleWrapper -ArgumentList $arguments `
        -WorkingDirectory $projectRoot `
        -RedirectStandardOutput $standardOutput `
        -RedirectStandardError $standardError `
        -WindowStyle Hidden `
        -PassThru

    $deadline = [DateTime]::UtcNow.AddSeconds($StartupTimeoutSeconds)
    while ([DateTime]::UtcNow -lt $deadline) {
        if ($process.HasExited) {
            throw "Server wrapper exited before startup completed"
        }
        if ((Test-Path -LiteralPath $standardOutput) -and
            (Select-String -LiteralPath $standardOutput -Pattern "Done \(" -Quiet)) {
            break
        }
        Start-Sleep -Seconds 1
    }
    if (-not (Test-Path -LiteralPath $standardOutput) -or
        -not (Select-String -LiteralPath $standardOutput -Pattern "Done \(" -Quiet)) {
        throw "Timed out waiting for the dedicated server to start"
    }

    $connection = Open-RconConnection
    Write-Output ("LIST=" + (Invoke-RconCommand -Connection $connection -Command "list"))
    Write-Output ("SAVE=" + (Invoke-RconCommand -Connection $connection -Command "save-all flush"))
    try {
        Write-Output ("STOP=" + (Invoke-RconCommand -Connection $connection -Command "stop"))
    } catch {
        Write-Output "STOP=connection closed during normal shutdown"
    }

    if (-not $process.WaitForExit($ShutdownTimeoutSeconds * 1000)) {
        throw "Server wrapper did not exit after the stop command"
    }
    if (-not (Select-String -LiteralPath $standardOutput -Pattern "BUILD SUCCESSFUL" -Quiet)) {
        throw "Gradle did not report BUILD SUCCESSFUL"
    }

    $latestLog = Join-Path $projectRoot "run/logs/latest.log"
    if (-not (Test-Path -LiteralPath $latestLog)) {
        throw "Dedicated server did not produce run/logs/latest.log"
    }
    Copy-Item -LiteralPath $latestLog -Destination $serverLog -Force
    Write-Output "SERVER_SMOKE_OK=$Name"
} catch {
    Write-Output ("SERVER_SMOKE_ERROR=" + $_.Exception.Message)
    if (Test-Path -LiteralPath $standardOutput) {
        Get-Content -LiteralPath $standardOutput -Tail 80
    }
    if (Test-Path -LiteralPath $standardError) {
        Get-Content -LiteralPath $standardError -Tail 80
    }
    throw
} finally {
    if ($null -ne $connection) {
        $connection.Stream.Dispose()
        $connection.Client.Dispose()
    }
    if ($null -ne $process -and -not $process.HasExited) {
        $cleanupConnection = $null
        try {
            $cleanupConnection = Open-RconConnection
            Invoke-RconCommand -Connection $cleanupConnection -Command "save-all flush" | Out-Null
            Invoke-RconCommand -Connection $cleanupConnection -Command "stop" | Out-Null
            $process.WaitForExit(30000) | Out-Null
        } catch {
            Write-Output ("SERVER_SMOKE_CLEANUP_WARNING=" + $_.Exception.Message)
        } finally {
            if ($null -ne $cleanupConnection) {
                $cleanupConnection.Stream.Dispose()
                $cleanupConnection.Client.Dispose()
            }
        }
    }
    if ($null -ne $process -and -not $process.HasExited) {
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
        Get-CimInstance Win32_Process | Where-Object {
            $_.Name -match '^java(w)?\.exe$' -and
            $_.CommandLine -like "*forgeserveruserdev*" -and
            $_.CommandLine -like "*$World*"
        } | ForEach-Object {
            Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        }
    }
}
